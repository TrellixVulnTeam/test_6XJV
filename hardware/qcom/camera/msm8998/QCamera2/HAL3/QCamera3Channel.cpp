/* Copyright (c) 2012-2016, The Linux Foundation. All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are
* met:
*     * Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above
*       copyright notice, this list of conditions and the following
*       disclaimer in the documentation and/or other materials provided
*       with the distribution.
*     * Neither the name of The Linux Foundation nor the names of its
*       contributors may be used to endorse or promote products derived
*       from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
* ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
* BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
* BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
* WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
* OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
* IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*
*/


#define LOG_TAG "QCamera3Channel"

// To remove
#include <cutils/properties.h>

// System dependencies
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include "hardware/gralloc.h"
#include <utils/Timers.h>
#include <sys/stat.h>

// Camera dependencies
#include "QCamera3Channel.h"
#include "QCamera3HWI.h"
#include "QCameraTrace.h"
#include "QCameraFormat.h"
extern "C" {
#include "mm_camera_dbg.h"
}

using namespace android;

namespace qcamera {
#define IS_BUFFER_ERROR(x) (((x) & V4L2_BUF_FLAG_ERROR) == V4L2_BUF_FLAG_ERROR)

/*===========================================================================
 * FUNCTION   : QCamera3Channel
 *
 * DESCRIPTION: constrcutor of QCamera3Channel
 *
 * PARAMETERS :
 *   @cam_handle : camera handle
 *   @cam_ops    : ptr to camera ops table
 *
 * RETURN     : none
 *==========================================================================*/
QCamera3Channel::QCamera3Channel(uint32_t cam_handle,
                               uint32_t channel_handle,
                               mm_camera_ops_t *cam_ops,
                               channel_cb_routine cb_routine,
                               channel_cb_buffer_err cb_buffer_err,
                               cam_padding_info_t *paddingInfo,
                               cam_feature_mask_t postprocess_mask,
                               void *userData, uint32_t numBuffers)
{
    m_camHandle = cam_handle;
    m_handle = channel_handle;
    m_camOps = cam_ops;
    m_bIsActive = false;
    m_bUBWCenable = true;

    m_numStreams = 0;
    memset(mStreams, 0, sizeof(mStreams));
    mUserData = userData;

    mStreamInfoBuf = NULL;
    mChannelCB = cb_routine;
    mChannelCbBufErr = cb_buffer_err;
    mPaddingInfo = *paddingInfo;
    mPaddingInfo.offset_info.offset_x = 0;
    mPaddingInfo.offset_info.offset_y = 0;

    mPostProcMask = postprocess_mask;

    mIsType = IS_TYPE_NONE;
    mNumBuffers = numBuffers;
    mPerFrameMapUnmapEnable = true;
    mDumpFrmCnt = 0;
    mNRMode = 0;

    mYUVDump = property_get_int32("persist.camera.dumpimg", 0);
    mMapStreamBuffers = mYUVDump;
}

/*===========================================================================
 * FUNCTION   : ~QCamera3Channel
 *
 * DESCRIPTION: destructor of QCamera3Channel
 *
 * PARAMETERS : none
 *
 * RETURN     : none
 *==========================================================================*/
QCamera3Channel::~QCamera3Channel()
{
}

/*===========================================================================
 * FUNCTION   : destroy
 *
 * DESCRIPTION: internal destructor of QCamera3Channel called by the subclasses
 *              this destructor will call pure virtual functions.  stop will eventuall call
 *              QCamera3Stream::putBufs.  The putBufs function will
 *              call QCamera3Channel::putStreamBufs which is pure virtual
 *
 * PARAMETERS : none
 *
 * RETURN     : none
 *==========================================================================*/
void QCamera3Channel::destroy()
{
    if (m_bIsActive)
        stop();

    for (uint32_t i = 0; i < m_numStreams; i++) {
        if (mStreams[i] != NULL) {
            delete mStreams[i];
            mStreams[i] = 0;
        }
    }
    m_numStreams = 0;
}

/*===========================================================================
 * FUNCTION   : addStream
 *
 * DESCRIPTION: add a stream into channel
 *
 * PARAMETERS :
 *   @streamType     : stream type
 *   @streamFormat   : stream format
 *   @streamDim      : stream dimension
 *   @streamRotation : rotation of the stream
 *   @minStreamBufNum : minimal buffer count for particular stream type
 *   @postprocessMask : post-proccess feature mask
 *   @isType         : type of image stabilization required on the stream
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3Channel::addStream(cam_stream_type_t streamType,
                                  cam_format_t streamFormat,
                                  cam_dimension_t streamDim,
                                  cam_rotation_t streamRotation,
                                  uint8_t minStreamBufNum,
                                  cam_feature_mask_t postprocessMask,
                                  cam_is_type_t isType,
                                  uint32_t batchSize)
{
    int32_t rc = NO_ERROR;

    if (m_numStreams >= 1) {
        LOGE("Only one stream per channel supported in v3 Hal");
        return BAD_VALUE;
    }

    if (m_numStreams >= MAX_STREAM_NUM_IN_BUNDLE) {
        LOGE("stream number (%d) exceeds max limit (%d)",
               m_numStreams, MAX_STREAM_NUM_IN_BUNDLE);
        return BAD_VALUE;
    }
    QCamera3Stream *pStream = new QCamera3Stream(m_camHandle,
                                               m_handle,
                                               m_camOps,
                                               &mPaddingInfo,
                                               this,
                                               mMapStreamBuffers);
    if (pStream == NULL) {
        LOGE("No mem for Stream");
        return NO_MEMORY;
    }
    LOGD("batch size is %d", batchSize);

    rc = pStream->init(streamType, streamFormat, streamDim, streamRotation,
            NULL, minStreamBufNum, postprocessMask, isType, batchSize,
            streamCbRoutine, this);
    if (rc == 0) {
        mStreams[m_numStreams] = pStream;
        m_numStreams++;
    } else {
        delete pStream;
    }
    return rc;
}

/*===========================================================================
 * FUNCTION   : start
 *
 * DESCRIPTION: start channel, which will start all streams belong to this channel
 *
 * PARAMETERS :
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3Channel::start()
{
    ATRACE_CAMSCOPE_CALL(CAMSCOPE_HAL3_CH_START);
    int32_t rc = NO_ERROR;

    if (m_numStreams > 1) {
        LOGW("bundle not supported");
    } else if (m_numStreams == 0) {
        return NO_INIT;
    }

    if(m_bIsActive) {
        LOGW("Attempt to start active channel");
        return rc;
    }

    for (uint32_t i = 0; i < m_numStreams; i++) {
        if (mStreams[i] != NULL) {
            mStreams[i]->start();
        }
    }

    m_bIsActive = true;

    return rc;
}

/*===========================================================================
 * FUNCTION   : stop
 *
 * DESCRIPTION: stop a channel, which will stop all streams belong to this channel
 *
 * PARAMETERS : none
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3Channel::stop()
{
    ATRACE_CAMSCOPE_CALL(CAMSCOPE_HAL3_CH_STOP);
    int32_t rc = NO_ERROR;
    if(!m_bIsActive) {
        LOGE("Attempt to stop inactive channel");
        return rc;
    }

    for (uint32_t i = 0; i < m_numStreams; i++) {
        if (mStreams[i] != NULL) {
            mStreams[i]->stop();
        }
    }

    m_bIsActive = false;
    return rc;
}

/*===========================================================================
 * FUNCTION   : setBatchSize
 *
 * DESCRIPTION: Set batch size for the channel. This is a dummy implementation
 *              for the base class
 *
 * PARAMETERS :
 *   @batchSize  : Number of image buffers in a batch
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success always
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3Channel::setBatchSize(uint32_t batchSize)
{
    LOGD("Dummy method. batchSize: %d unused ", batchSize);
    return NO_ERROR;
}

/*===========================================================================
 * FUNCTION   : queueBatchBuf
 *
 * DESCRIPTION: This is a dummy implementation for the base class
 *
 * PARAMETERS :
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success always
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3Channel::queueBatchBuf()
{
    LOGD("Dummy method. Unused ");
    return NO_ERROR;
}

/*===========================================================================
 * FUNCTION   : setPerFrameMapUnmap
 *
 * DESCRIPTION: Sets internal enable flag
 *
 * PARAMETERS :
 *  @enable : Bool value for the enable flag
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success always
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3Channel::setPerFrameMapUnmap(bool enable)
{
    mPerFrameMapUnmapEnable = enable;
    return NO_ERROR;
}

/*===========================================================================
 * FUNCTION   : flush
 *
 * DESCRIPTION: flush a channel
 *
 * PARAMETERS : none
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3Channel::flush()
{
    ATRACE_CAMSCOPE_CALL(CAMSCOPE_HAL3_CH_FLUSH);
    return NO_ERROR;
}

/*===========================================================================
 * FUNCTION   : bufDone
 *
 * DESCRIPTION: return a stream buf back to kernel
 *
 * PARAMETERS :
 *   @recvd_frame  : stream buf frame to be returned
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3Channel::bufDone(mm_camera_super_buf_t *recvd_frame)
{
    int32_t rc = NO_ERROR;
    for (uint32_t i = 0; i < recvd_frame->num_bufs; i++) {
         if (recvd_frame->bufs[i] != NULL) {
             for (uint32_t j = 0; j < m_numStreams; j++) {
                 if (mStreams[j] != NULL &&
                     mStreams[j]->getMyHandle() == recvd_frame->bufs[i]->stream_id) {
                     rc = mStreams[j]->bufDone(recvd_frame->bufs[i]->buf_idx);
                     break; // break loop j
                 }
             }
         }
    }

    return rc;
}

int32_t QCamera3Channel::setBundleInfo(const cam_bundle_config_t &bundleInfo)
{
    int32_t rc = NO_ERROR;
    cam_stream_parm_buffer_t param;
    memset(&param, 0, sizeof(cam_stream_parm_buffer_t));
    param.type = CAM_STREAM_PARAM_TYPE_SET_BUNDLE_INFO;
    param.bundleInfo = bundleInfo;
    if (m_numStreams > 0 && mStreams[0]) {
        rc = mStreams[0]->setParameter(param);
        if (rc != NO_ERROR) {
            LOGE("stream setParameter for set bundle failed");
        }
    }
    return rc;
}

/*===========================================================================
 * FUNCTION   : getStreamTypeMask
 *
 * DESCRIPTION: Get bit mask of all stream types in this channel
 *
 * PARAMETERS : None
 *
 * RETURN     : Bit mask of all stream types in this channel
 *==========================================================================*/
uint32_t QCamera3Channel::getStreamTypeMask()
{
    uint32_t mask = 0;
    for (uint32_t i = 0; i < m_numStreams; i++) {
       mask |= (1U << mStreams[i]->getMyType());
    }
    return mask;
}

/*===========================================================================
 * FUNCTION   : getStreamID
 *
 * DESCRIPTION: Get StreamID of requested stream type
 *
 * PARAMETERS : streamMask
 *
 * RETURN     : Stream ID
 *==========================================================================*/
uint32_t QCamera3Channel::getStreamID(uint32_t streamMask)
{
    uint32_t streamID = 0;
    for (uint32_t i = 0; i < m_numStreams; i++) {
        if (streamMask == (uint32_t )(0x1 << mStreams[i]->getMyType())) {
            streamID = mStreams[i]->getMyServerID();
            break;
        }
    }
    return streamID;
}

/*===========================================================================
 * FUNCTION   : getStreamByHandle
 *
 * DESCRIPTION: return stream object by stream handle
 *
 * PARAMETERS :
 *   @streamHandle : stream handle
 *
 * RETURN     : stream object. NULL if not found
 *==========================================================================*/
QCamera3Stream *QCamera3Channel::getStreamByHandle(uint32_t streamHandle)
{
    for (uint32_t i = 0; i < m_numStreams; i++) {
        if (mStreams[i] != NULL && mStreams[i]->getMyHandle() == streamHandle) {
            return mStreams[i];
        }
    }
    return NULL;
}

/*===========================================================================
 * FUNCTION   : getStreamByIndex
 *
 * DESCRIPTION: return stream object by index
 *
 * PARAMETERS :
 *   @streamHandle : stream handle
 *
 * RETURN     : stream object. NULL if not found
 *==========================================================================*/
QCamera3Stream *QCamera3Channel::getStreamByIndex(uint32_t index)
{
    if (index < m_numStreams) {
        return mStreams[index];
    }
    return NULL;
}

/*===========================================================================
 * FUNCTION   : streamCbRoutine
 *
 * DESCRIPTION: callback routine for stream
 *
 * PARAMETERS :
 *   @streamHandle : stream handle
 *
 * RETURN     : stream object. NULL if not found
 *==========================================================================*/
void QCamera3Channel::streamCbRoutine(mm_camera_super_buf_t *super_frame,
                QCamera3Stream *stream, void *userdata)
{
    QCamera3Channel *channel = (QCamera3Channel *)userdata;
    if (channel == NULL) {
        LOGE("invalid channel pointer");
        return;
    }
    channel->streamCbRoutine(super_frame, stream);
}

/*===========================================================================
 * FUNCTION   : dumpYUV
 *
 * DESCRIPTION: function to dump the YUV data from ISP/pproc
 *
 * PARAMETERS :
 *   @frame   : frame to be dumped
 *   @dim     : dimension of the stream
 *   @offset  : offset of the data
 *   @name    : 1 if it is ISP output/pproc input, 2 if it is pproc output
 *
 * RETURN  :
 *==========================================================================*/
void QCamera3Channel::dumpYUV(mm_camera_buf_def_t *frame, cam_dimension_t dim,
        cam_frame_len_offset_t offset, uint8_t dump_type)
{
    char buf[FILENAME_MAX];
    memset(buf, 0, sizeof(buf));
    static int counter = 0;
    if (mYUVDump & dump_type) {
        mFrmNum = ((mYUVDump & 0xffff0000) >> 16);
        if (mFrmNum == 0) {
            mFrmNum = 10;
        }
        if (mFrmNum > 256) {
            mFrmNum = 256;
        }
        mSkipMode = ((mYUVDump & 0x0000ff00) >> 8);
        if (mSkipMode == 0) {
            mSkipMode = 1;
        }
        if (mDumpSkipCnt == 0) {
            mDumpSkipCnt = 1;
        }
        if (mDumpSkipCnt % mSkipMode == 0) {
            if (mDumpFrmCnt < mFrmNum) {
                /* Note that the image dimension will be the unrotated stream dimension.
                * If you feel that the image would have been rotated during reprocess
                * then swap the dimensions while opening the file
                * */
                switch (dump_type) {
                    case QCAMERA_DUMP_FRM_PREVIEW:
                        snprintf(buf, sizeof(buf), QCAMERA_DUMP_FRM_LOCATION"p_%d_%d_%dx%d.yuv",
                            counter, frame->frame_idx, dim.width, dim.height);
                    break;
                    case QCAMERA_DUMP_FRM_VIDEO:
                        snprintf(buf, sizeof(buf), QCAMERA_DUMP_FRM_LOCATION"v_%d_%d_%dx%d.yuv",
                            counter, frame->frame_idx, dim.width, dim.height);
                    break;
                    case QCAMERA_DUMP_FRM_INPUT_JPEG:
                        snprintf(buf, sizeof(buf), QCAMERA_DUMP_FRM_LOCATION"s_%d_%d_%dx%d.yuv",
                            counter, frame->frame_idx, dim.width, dim.height);
                    break;
                    case QCAMERA_DUMP_FRM_INPUT_REPROCESS:
                        snprintf(buf, sizeof(buf), QCAMERA_DUMP_FRM_LOCATION"ir_%d_%d_%dx%d.yuv",
                            counter, frame->frame_idx, dim.width, dim.height);
                    break;
                    case QCAMERA_DUMP_FRM_CALLBACK:
                        snprintf(buf, sizeof(buf), QCAMERA_DUMP_FRM_LOCATION"c_%d_%d_%dx%d.yuv",
                            counter, frame->frame_idx, dim.width, dim.height);
                    break;
                    case QCAMERA_DUMP_FRM_OUTPUT_JPEG:
                        snprintf(buf, sizeof(buf), QCAMERA_DUMP_FRM_LOCATION"j_%d_%d_%dx%d.jpg",
                            counter, frame->frame_idx, dim.width, dim.height);
                    break;
                    default :
                        LOGE("dumping not enabled for stream type %d",dump_type);
                    break;
                }
                counter++;
                int file_fd = open(buf, O_RDWR | O_CREAT, 0777);
                ssize_t written_len = 0;
                if (file_fd >= 0) {
                    void *data = NULL;
                    fchmod(file_fd, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH);
                    if( dump_type == QCAMERA_DUMP_FRM_OUTPUT_JPEG ) {
                        written_len = write(file_fd, frame->buffer, frame->frame_len);
                    }
                    else {
                        for (uint32_t i = 0; i < offset.num_planes; i++) {
                            uint32_t index = offset.mp[i].offset;
                            if (i > 0) {
                                index += offset.mp[i-1].len;
                            }
                            for (int j = 0; j < offset.mp[i].height; j++) {
                                data = (void *)((uint8_t *)frame->buffer + index);
                                written_len += write(file_fd, data,
                                        (size_t)offset.mp[i].width);
                                index += (uint32_t)offset.mp[i].stride;
                            }
                        }
                    }
                    LOGH("written number of bytes %ld\n", written_len);
                    mDumpFrmCnt++;
                    frame->cache_flags |= CPU_HAS_READ;
                    close(file_fd);
                } else {
                    LOGE("failed to open file to dump image");
                }
            }
        } else {
            mDumpSkipCnt++;
        }
    }
}

/*===========================================================================
 * FUNCTION   : isUBWCEnabled
 *
 * DESCRIPTION: Function to get UBWC hardware support.
 *
 * PARAMETERS : None
 *
 * RETURN     : TRUE -- UBWC format supported
 *              FALSE -- UBWC is not supported.
 *==========================================================================*/
bool QCamera3Channel::isUBWCEnabled()
{
#ifdef UBWC_PRESENT
    char value[PROPERTY_VALUE_MAX];
    int prop_value = 0;
    memset(value, 0, sizeof(value));
    property_get("debug.gralloc.gfx_ubwc_disable", value, "0");
    prop_value = atoi(value);
    if (prop_value) {
        return FALSE;
    }

    //Disable UBWC if Eztune is enabled
    //EzTune process CPP output frame and cannot understand UBWC.
    memset(value, 0, sizeof(value));
    property_get("persist.camera.eztune.enable", value, "0");
    prop_value = atoi(value);
    if (prop_value) {
        return FALSE;
    }
    return TRUE;
#else
    return FALSE;
#endif
}

/*===========================================================================
 * FUNCTION   : setUBWCEnabled
 *
 * DESCRIPTION: set UBWC enable
 *
 * PARAMETERS : UBWC enable value
 *
 * RETURN     : none
 *
 *==========================================================================*/
void QCamera3Channel::setUBWCEnabled(bool val)
{
    m_bUBWCenable = val;
}

/*===========================================================================
 * FUNCTION   : getStreamDefaultFormat
 *
 * DESCRIPTION: return default buffer format for the stream
 *
 * PARAMETERS : type : Stream type
 *
 ** RETURN    : format for stream type
 *
 *==========================================================================*/
cam_format_t QCamera3Channel::getStreamDefaultFormat(cam_stream_type_t type,
        uint32_t width, uint32_t height, bool forcePreviewUBWC, cam_is_type_t isType)
{
    cam_format_t streamFormat;

    switch (type) {
    case CAM_STREAM_TYPE_PREVIEW:
        if (isUBWCEnabled()) {

            char prop[PROPERTY_VALUE_MAX];
            int pFormat;
            memset(prop, 0, sizeof(prop));
            property_get("persist.camera.preview.ubwc", prop, "1");
            pFormat = atoi(prop);

            // When goog_zoom is linked to the preview stream, disable ubwc to preview
            property_get("persist.camera.gzoom.at", prop, "0");
            bool is_goog_zoom_preview_enabled = ((atoi(prop) & 2) > 0) && isType == IS_TYPE_EIS_3_0;

            if (pFormat == 1 && forcePreviewUBWC && !is_goog_zoom_preview_enabled) {
                streamFormat = CAM_FORMAT_YUV_420_NV12_UBWC;
            } else {
                /* Changed to macro to ensure format sent to gralloc for preview
                is also changed if the preview format is changed at camera HAL */
                streamFormat = PREVIEW_STREAM_FORMAT;
            }
        } else {
            /* Changed to macro to ensure format sent to gralloc for preview
            is also changed if the preview format is changed at camera HAL */
            streamFormat = PREVIEW_STREAM_FORMAT;
        }
        break;
    case CAM_STREAM_TYPE_VIDEO:
    {
        /* Disable UBWC for smaller video resolutions due to CPP downscale
            limits. Refer cpp_hw_params.h::CPP_DOWNSCALE_LIMIT_UBWC */
        if (isUBWCEnabled() && (width >= 640) && (height >= 480)) {
            // When goog_zoom is linked to the video stream, disable ubwc to video
            char prop[PROPERTY_VALUE_MAX];
            property_get("persist.camera.gzoom.at", prop, "0");
            bool is_goog_zoom_video_enabled = ((atoi(prop) & 1) > 0) && isType == IS_TYPE_EIS_3_0;

            property_get("persist.camera.gzoom.4k", prop, "0");
            bool is_goog_zoom_4k_enabled = (atoi(prop) > 0);
            bool is_4k_video = (width >= 3840 && height >= 2160);

            if ((QCameraCommon::isVideoUBWCEnabled()) && (!is_goog_zoom_video_enabled
                    || (is_4k_video && !is_goog_zoom_4k_enabled))) {
                streamFormat = CAM_FORMAT_YUV_420_NV12_UBWC;
            } else {
                streamFormat = CAM_FORMAT_YUV_420_NV12_VENUS;
            }
        } else {
#if VENUS_PRESENT
        streamFormat = CAM_FORMAT_YUV_420_NV12_VENUS;
#else
        streamFormat = CAM_FORMAT_YUV_420_NV12;
#endif
        }
        break;
    }
    case CAM_STREAM_TYPE_SNAPSHOT:
        streamFormat = CAM_FORMAT_YUV_420_NV21;
        break;
    case CAM_STREAM_TYPE_CALLBACK:
        /* Changed to macro to ensure format sent to gralloc for callback
        is also changed if the preview format is changed at camera HAL */
        streamFormat = CALLBACK_STREAM_FORMAT;
        break;
    case CAM_STREAM_TYPE_RAW:
        streamFormat = CAM_FORMAT_BAYER_MIPI_RAW_10BPP_GBRG;
        break;
    default:
        streamFormat = CAM_FORMAT_YUV_420_NV21;
        break;
    }
    return streamFormat;
}


/* QCamera3ProcessingChannel methods */

/*===========================================================================
 * FUNCTION   : QCamera3ProcessingChannel
 *
 * DESCRIPTION: constructor of QCamera3ProcessingChannel
 *
 * PARAMETERS :
 *   @cam_handle : camera handle
 *   @cam_ops    : ptr to camera ops table
 *   @cb_routine : callback routine to frame aggregator
 *   @paddingInfo: stream padding info
 *   @userData   : HWI handle
 *   @stream     : camera3_stream_t structure
 *   @stream_type: Channel stream type
 *   @postprocess_mask: the postprocess mask for streams of this channel
 *   @metadataChannel: handle to the metadataChannel
 *   @numBuffers : number of max dequeued buffers
 * RETURN     : none
 *==========================================================================*/
QCamera3ProcessingChannel::QCamera3ProcessingChannel(uint32_t cam_handle,
        uint32_t channel_handle,
        mm_camera_ops_t *cam_ops,
        channel_cb_routine cb_routine,
        channel_cb_buffer_err cb_buffer_err,
        cam_padding_info_t *paddingInfo,
        void *userData,
        camera3_stream_t *stream,
        cam_stream_type_t stream_type,
        cam_feature_mask_t postprocess_mask,
        QCamera3Channel *metadataChannel,
        uint32_t numBuffers) :
            QCamera3Channel(cam_handle, channel_handle, cam_ops, cb_routine,
                    cb_buffer_err, paddingInfo, postprocess_mask, userData, numBuffers),
            m_postprocessor(this),
            mFrameCount(0),
            mLastFrameCount(0),
            mLastFpsTime(0),
            mMemory(numBuffers),
            mCamera3Stream(stream),
            mNumBufs(CAM_MAX_NUM_BUFS_PER_STREAM),
            mStreamType(stream_type),
            mPostProcStarted(false),
            mReprocessType(REPROCESS_TYPE_NONE),
            mInputBufferConfig(false),
            m_pMetaChannel(metadataChannel),
            mMetaFrame(NULL),
            mOfflineMemory(0),
            mOfflineMetaMemory(numBuffers + (MAX_REPROCESS_PIPELINE_STAGES - 1))
{
    char prop[PROPERTY_VALUE_MAX];
    property_get("persist.debug.sf.showfps", prop, "0");
    mDebugFPS = (uint8_t) atoi(prop);

    int32_t rc = m_postprocessor.init(&mMemory);
    if (rc != 0) {
        LOGE("Init Postprocessor failed");
    }
}

/*===========================================================================
 * FUNCTION   : ~QCamera3ProcessingChannel
 *
 * DESCRIPTION: destructor of QCamera3ProcessingChannel
 *
 * PARAMETERS : none
 *
 * RETURN     : none
 *==========================================================================*/
QCamera3ProcessingChannel::~QCamera3ProcessingChannel()
{
    destroy();

    int32_t rc = m_postprocessor.deinit();
    if (rc != 0) {
        LOGE("De-init Postprocessor failed");
    }

    if (0 < mOfflineMetaMemory.getCnt()) {
        mOfflineMetaMemory.deallocate();
    }
    if (0 < mOfflineMemory.getCnt()) {
        mOfflineMemory.unregisterBuffers();
    }

}

/*===========================================================================
 * FUNCTION   : streamCbRoutine
 *
 * DESCRIPTION:
 *
 * PARAMETERS :
 * @super_frame : the super frame with filled buffer
 * @stream      : stream on which the buffer was requested and filled
 *
 * RETURN     : none
 *==========================================================================*/
void QCamera3ProcessingChannel::streamCbRoutine(mm_camera_super_buf_t *super_frame,
        QCamera3Stream *stream)
{
    if (mStreamType == CAM_STREAM_TYPE_PREVIEW) {
        KPI_ATRACE_CAMSCOPE_CALL(CAMSCOPE_HAL3_PREVIEW_STRM_CB);
    } else {
        ATRACE_CAMSCOPE_CALL(CAMSCOPE_HAL3_PROC_CH_STRM_CB);
    }
    //FIXME Q Buf back in case of error?
    uint8_t frameIndex;
    buffer_handle_t *resultBuffer;
    int32_t resultFrameNumber;
    camera3_stream_buffer_t result;
    cam_dimension_t dim;
    cam_frame_len_offset_t offset;

    memset(&dim, 0, sizeof(dim));
    memset(&offset, 0, sizeof(cam_frame_len_offset_t));
    if (checkStreamCbErrors(super_frame, stream) != NO_ERROR) {
        LOGE("Error with the stream callback");
        return;
    }

    frameIndex = (uint8_t)super_frame->bufs[0]->buf_idx;
    if(frameIndex >= mNumBufs) {
         LOGE("Error, Invalid index for buffer");
         stream->bufDone(frameIndex);
         return;
    }

    if (mDebugFPS) {
        showDebugFPS(stream->getMyType());
    }
    stream->getFrameDimension(dim);
    stream->getFrameOffset(offset);
    if (stream->getMyType() == CAM_STREAM_TYPE_PREVIEW) {
        dumpYUV(super_frame->bufs[0], dim, offset, QCAMERA_DUMP_FRM_PREVIEW);
    } else if (stream->getMyType() == CAM_STREAM_TYPE_VIDEO) {
        dumpYUV(super_frame->bufs[0], dim, offset, QCAMERA_DUMP_FRM_VIDEO);
    } else if (stream->getMyType() == CAM_STREAM_TYPE_CALLBACK) {
        dumpYUV(super_frame->bufs[0], dim, offset, QCAMERA_DUMP_FRM_CALLBACK);
    }

    do {

       //Use below data to issue framework callback
       resultBuffer = (buffer_handle_t *)mMemory.getBufferHandle(frameIndex);
       resultFrameNumber = mMemory.getFrameNumber(frameIndex);
       uint32_t oldestBufIndex;
       int32_t lowestFrameNumber = mMemory.getOldestFrameNumber(oldestBufIndex);
       QCamera3HardwareInterface* hal_obj = (QCamera3HardwareInterface*)mUserData;
       if ((lowestFrameNumber != -1 ) && (lowestFrameNumber < resultFrameNumber) &&
            hal_obj->mOpMode != CAMERA3_STREAM_CONFIGURATION_CONSTRAINED_HIGH_SPEED_MODE) {
           LOGE("Error buffer dropped for framenumber:%d with bufidx:%d",
                   lowestFrameNumber, oldestBufIndex);
           if (mOutOfSequenceBuffers.empty()) {
              stream->cancelBuffer(oldestBufIndex);
           }

           //push in order!
           auto itr = mOutOfSequenceBuffers.begin();
           for (; itr != mOutOfSequenceBuffers.end(); itr++) {
               mm_camera_super_buf_t *super_buf = *itr;
               uint32_t buf_idx = super_buf->bufs[0]->buf_idx;
               int32_t frame_num = mMemory.getFrameNumber(buf_idx);
               if (resultFrameNumber < frame_num) {
                   LOGE("Out of order frame!! set buffer status error flag!");
                   mOutOfSequenceBuffers.insert(itr, super_frame);
                   super_buf->bufs[0]->flags |= V4L2_BUF_FLAG_ERROR;
                   break;
               }
           }

           if (itr == mOutOfSequenceBuffers.end()) {
               LOGE("Add the frame to the end of mOutOfSequenceBuffers");
               // add the buffer
               mOutOfSequenceBuffers.push_back(super_frame);
           }
           return;
       }

       if(hal_obj->mStreamConfig == true) {
          switch (stream->getMyType()) {
              case CAM_STREAM_TYPE_PREVIEW:
                  LOGH("[KPI Perf] : PROFILE_FIRST_PREVIEW_FRAME");
                  break;
              case CAM_STREAM_TYPE_VIDEO:
                  LOGH("[KPI Perf] : PROFILE_FIRST_VIDEO_FRAME");
                  break;
              default:
                  break;
          }
          hal_obj->mStreamConfig = false;
       }

       result.stream = mCamera3Stream;
       result.buffer = resultBuffer;
       if (IS_BUFFER_ERROR(super_frame->bufs[0]->flags)) {
           result.status = CAMERA3_BUFFER_STATUS_ERROR;
           LOGW("CAMERA3_BUFFER_STATUS_ERROR for stream_type: %d",
                   mStreams[0]->getMyType());
           mChannelCbBufErr(this, resultFrameNumber, CAMERA3_BUFFER_STATUS_ERROR, mUserData);
       } else {
           result.status = CAMERA3_BUFFER_STATUS_OK;
       }
       result.acquire_fence = -1;
       result.release_fence = -1;
       if(mPerFrameMapUnmapEnable) {
           int32_t rc = stream->bufRelease(frameIndex);
           if (NO_ERROR != rc) {
               LOGE("Error %d releasing stream buffer %d",
                        rc, frameIndex);
           }

           rc = mMemory.unregisterBuffer(frameIndex);
           if (NO_ERROR != rc) {
               LOGE("Error %d unregistering stream buffer %d",
                        rc, frameIndex);
           }
       }

       if (0 <= resultFrameNumber) {
           if (mChannelCB) {
               mChannelCB(NULL, &result, (uint32_t)resultFrameNumber, false, mUserData);
           }
       } else {
           LOGE("Bad frame number");
       }
       free(super_frame);
       super_frame = NULL;
       if (mOutOfSequenceBuffers.empty()) {
          break;
       } else {
            auto itr = mOutOfSequenceBuffers.begin();
            super_frame = *itr;
            frameIndex = super_frame->bufs[0]->buf_idx;
            resultFrameNumber = mMemory.getFrameNumber(frameIndex);
            lowestFrameNumber = mMemory.getOldestFrameNumber(oldestBufIndex);
            LOGE("Attempting to recover next frame: result Frame#: %d, resultIdx: %d, "
                    "Lowest Frame#: %d, oldestBufIndex: %d",
                    resultFrameNumber, frameIndex, lowestFrameNumber, oldestBufIndex);
            if ((lowestFrameNumber != -1) && (lowestFrameNumber < resultFrameNumber)) {
                LOGE("Multiple frame dropped requesting cancel for frame %d, idx:%d",
                        lowestFrameNumber, oldestBufIndex);
                stream->cancelBuffer(oldestBufIndex);
                return;
             } else if (lowestFrameNumber == resultFrameNumber) {
                LOGE("Time to flush out head of list continue loop with this new super frame");
                itr = mOutOfSequenceBuffers.erase(itr);
             } else {
                LOGE("Unexpected condition head of list is not the lowest frame number");
                itr = mOutOfSequenceBuffers.erase(itr);
             }
          }
    } while (1);
    return;
}

/*===========================================================================
 * FUNCTION   : putStreamBufs
 *
 * DESCRIPTION: release the buffers allocated to the stream
 *
 * PARAMETERS : NONE
 *
 * RETURN     : NONE
 *==========================================================================*/
void QCamera3YUVChannel::putStreamBufs()
{
    QCamera3ProcessingChannel::putStreamBufs();

    // Free allocated heap buffer.
    mMemory.deallocate();
    // Clear free heap buffer list.
    mFreeHeapBufferList.clear();
    // Clear offlinePpInfoList
    mOfflinePpInfoList.clear();
}

/*===========================================================================
 * FUNCTION   : timeoutFrame
 *
 * DESCRIPTION: Method to indicate to channel that a given frame has take too
 *              long to be generated
 *
 * PARAMETERS : framenumber indicating the framenumber of the buffer timingout
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3ProcessingChannel::timeoutFrame(uint32_t frameNumber)
{
    int32_t bufIdx;

    bufIdx = mMemory.getBufferIndex(frameNumber);

    if (bufIdx < 0) {
        LOGE("%s: Buffer not found for frame:%d", __func__, frameNumber);
        return -1;
    }

    mStreams[0]->timeoutFrame(bufIdx);
    return NO_ERROR;
}

/*===========================================================================
 * FUNCTION   : postprocFail
 *
 * DESCRIPTION: notify clients about failing post-process requests.
 *
 * PARAMETERS :
 * @ppBuffer  : pointer to the pp buffer.
 *
 * RETURN     : 0 on success
 *              -EINVAL on invalid input
 *==========================================================================*/
int32_t QCamera3ProcessingChannel::postprocFail(qcamera_hal3_pp_buffer_t *ppBuffer) {
    if (ppBuffer == nullptr) {
        return BAD_VALUE;
    }

    if (ppBuffer->output == nullptr) {
        return BAD_VALUE;
    }

    camera3_stream_buffer_t result = {};
    result.buffer = ppBuffer->output;

    LOGE("Input frame number: %d dropped!", ppBuffer->frameNumber);
    result.stream = mCamera3Stream;
    result.status = CAMERA3_BUFFER_STATUS_ERROR;
    result.acquire_fence = -1;
    result.release_fence = -1;
    if (mChannelCB) {
        mChannelCB(NULL, &result, ppBuffer->frameNumber, false, mUserData);
    }

    return OK;
}

/*===========================================================================
 * FUNCTION   : request
 *
 * DESCRIPTION: handle the request - either with an input buffer or a direct
 *              output request
 *
 * PARAMETERS :
 * @buffer          : pointer to the output buffer
 * @frameNumber     : frame number of the request
 * @pInputBuffer    : pointer to input buffer if an input request
 * @metadata        : parameters associated with the request
 * @internalreq      : boolean to indicate if this is purely internal request
 *                    needing internal buffer allocation
 * @meteringonly    : boolean indicating metering only frame subset of internal
 *                    not consumed by postprocessor
 *
 * RETURN     : 0 on a success start of capture
 *              -EINVAL on invalid input
 *              -ENODEV on serious error
 *==========================================================================*/
int32_t QCamera3ProcessingChannel::request(buffer_handle_t *buffer,
        uint32_t frameNumber,
        camera3_stream_buffer_t* pInputBuffer,
        metadata_buffer_t* metadata,
        int &indexUsed,
        __unused bool internalRequest = false,
        __unused bool meteringOnly = false)
{
    int32_t rc = NO_ERROR;
    int index;

    if (NULL == buffer || NULL == metadata) {
        LOGE("Invalid buffer/metadata in channel request");
        return BAD_VALUE;
    }

    if (pInputBuffer) {
        //need to send to reprocessing
        LOGD("Got a request with input buffer, output streamType = %d", mStreamType);
        reprocess_config_t reproc_cfg;
        cam_dimension_t dim;
        memset(&reproc_cfg, 0, sizeof(reprocess_config_t));
        memset(&dim, 0, sizeof(dim));
        setReprocConfig(reproc_cfg, pInputBuffer, metadata, mStreamFormat, dim);
        startPostProc(reproc_cfg);

        qcamera_fwk_input_pp_data_t *src_frame = NULL;
        src_frame = (qcamera_fwk_input_pp_data_t *)calloc(1,
                sizeof(qcamera_fwk_input_pp_data_t));
        if (src_frame == NULL) {
            LOGE("No memory for src frame");
            return NO_MEMORY;
        }
        rc = setFwkInputPPData(src_frame, pInputBuffer, &reproc_cfg, metadata, buffer, frameNumber);
        if (NO_ERROR != rc) {
            LOGE("Error %d while setting framework input PP data", rc);
            free(src_frame);
            return rc;
        }
        LOGH("Post-process started");
        m_postprocessor.processData(src_frame);
    } else {
        index = mMemory.getMatchBufIndex((void*)buffer);
        if(index < 0) {
            rc = registerBuffer(buffer, mIsType);
            if (NO_ERROR != rc) {
                LOGE("On-the-fly buffer registration failed %d",
                         rc);
                return rc;
            }

            index = mMemory.getMatchBufIndex((void*)buffer);
            if (index < 0) {
                LOGE("Could not find object among registered buffers");
                return DEAD_OBJECT;
            }
        }
        rc = mMemory.markFrameNumber(index, frameNumber);
        if(rc != NO_ERROR) {
            LOGE("Error marking frame number:%d for index %d", frameNumber,
                index);
            return rc;
        }
        if (m_bIsActive) {
            rc = mStreams[0]->bufDone(index);
            if(rc != NO_ERROR) {
                LOGE("Failed to Q new buffer to stream");
                mMemory.markFrameNumber(index, -1);
                return rc;
            }
        }
        indexUsed = index;
    }
    return rc;
}

/*===========================================================================
 * FUNCTION   : initialize
 *
 * DESCRIPTION:
 *
 * PARAMETERS : isType : type of image stabilization on the buffer
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3ProcessingChannel::initialize(__unused cam_is_type_t isType)
{
    int32_t rc = NO_ERROR;
    rc = mOfflineMetaMemory.allocateAll(sizeof(metadata_buffer_t));
    if (rc == NO_ERROR) {
        Mutex::Autolock lock(mFreeOfflineMetaBuffersLock);
        mFreeOfflineMetaBuffersList.clear();
        for (uint32_t i = 0; i < mNumBuffers + (MAX_REPROCESS_PIPELINE_STAGES - 1);
                i++) {
            mFreeOfflineMetaBuffersList.push_back(i);
        }
    } else {
        LOGE("Could not allocate offline meta buffers for input reprocess");
    }
    mOutOfSequenceBuffers.clear();
    return rc;
}

/*===========================================================================
 * FUNCTION   : registerBuffer
 *
 * DESCRIPTION: register streaming buffer to the channel object
 *
 * PARAMETERS :
 *   @buffer     : buffer to be registered
 *   @isType     : image stabilization type on the stream
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3ProcessingChannel::registerBuffer(buffer_handle_t *buffer,
        cam_is_type_t isType)
{
    ATRACE_CAMSCOPE_CALL(CAMSCOPE_HAL3_PROC_CH_REG_BUF);
    int rc = 0;
    mIsType = isType;
    cam_stream_type_t streamType;

    if ((uint32_t)mMemory.getCnt() > (mNumBufs - 1)) {
        LOGE("Trying to register more buffers than initially requested");
        return BAD_VALUE;
    }

    if (0 == m_numStreams) {
        rc = initialize(mIsType);
        if (rc != NO_ERROR) {
            LOGE("Couldn't initialize camera stream %d", rc);
            return rc;
        }
    }

    streamType = mStreams[0]->getMyType();
    rc = mMemory.registerBuffer(buffer, streamType);
    if (ALREADY_EXISTS == rc) {
        return NO_ERROR;
    } else if (NO_ERROR != rc) {
        LOGE("Buffer %p couldn't be registered %d", buffer, rc);
        return rc;
    }

    return rc;
}

int32_t QCamera3ProcessingChannel::registerBufferAndGetBufDef(buffer_handle_t *buffer,
        mm_camera_buf_def_t *frame)
{
    if (buffer == nullptr || frame == nullptr) {
        ALOGE("%s: buffer and frame cannot be nullptr.", __FUNCTION__);
        return BAD_VALUE;
    }

    status_t rc;

    // Get the buffer index.
    int index = mMemory.getMatchBufIndex((void*)buffer);
    if(index < 0) {
        // Register the buffer if it was not registered.
        rc = registerBuffer(buffer, mIsType);
        if (rc != OK) {
            ALOGE("%s: Regitering buffer failed: %s (%d)", __FUNCTION__, strerror(-rc), rc);
            return rc;
        }

        index = mMemory.getMatchBufIndex((void*)buffer);
        if (index < 0) {
            ALOGE("%s: Could not find object among registered buffers", __FUNCTION__);
            return DEAD_OBJECT;
        }
    }

    cam_frame_len_offset_t offset = {};
    mStreams[0]->getFrameOffset(offset);

    // Get the buffer def.
    rc = mMemory.getBufDef(offset, *frame, index, mMapStreamBuffers);
    if (rc != 0) {
        ALOGE("%s: Getting a frame failed: %s (%d).", __FUNCTION__, strerror(-rc), rc);
        return rc;
    }

    // Set the frame's stream ID because it's not set in getBufDef.
    frame->stream_id = mStreams[0]->getMyHandle();
    return 0;
}

void QCamera3ProcessingChannel::unregisterBuffer(mm_camera_buf_def_t *frame)
{
    if (frame == nullptr) {
        ALOGE("%s: frame is nullptr", __FUNCTION__);
        return;
    }

    mMemory.unregisterBuffer(frame->buf_idx);
}

/*===========================================================================
 * FUNCTION   : setFwkInputPPData
 *
 * DESCRIPTION: fill out the framework src frame information for reprocessing
 *
 * PARAMETERS :
 *   @src_frame         : input pp data to be filled out
 *   @pInputBuffer      : input buffer for reprocessing
 *   @reproc_cfg        : pointer to the reprocess config
 *   @metadata          : pointer to the metadata buffer
 *   @output_buffer     : output buffer for reprocessing; could be NULL if not
 *                        framework allocated
 *   @frameNumber       : frame number of the request
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3ProcessingChannel::setFwkInputPPData(qcamera_fwk_input_pp_data_t *src_frame,
        camera3_stream_buffer_t *pInputBuffer, reprocess_config_t *reproc_cfg,
        metadata_buffer_t *metadata, buffer_handle_t *output_buffer,
        uint32_t frameNumber)
{
    int32_t rc = NO_ERROR;
    int input_index = mOfflineMemory.getMatchBufIndex((void*)pInputBuffer->buffer);
    if(input_index < 0) {
        rc = mOfflineMemory.registerBuffer(pInputBuffer->buffer, mStreamType);
        if (NO_ERROR != rc) {
            LOGE("On-the-fly input buffer registration failed %d",
                     rc);
            return rc;
        }
        input_index = mOfflineMemory.getMatchBufIndex((void*)pInputBuffer->buffer);
        if (input_index < 0) {
            LOGE("Could not find object among registered buffers");
            return DEAD_OBJECT;
        }
    }
    mOfflineMemory.markFrameNumber(input_index, frameNumber);

    src_frame->src_frame = *pInputBuffer;
    rc = mOfflineMemory.getBufDef(reproc_cfg->input_stream_plane_info.plane_info,
            src_frame->input_buffer, input_index, mMapStreamBuffers);
    if (rc != 0) {
        return rc;
    }
    dumpYUV(&src_frame->input_buffer, reproc_cfg->input_stream_dim,
            reproc_cfg->input_stream_plane_info.plane_info, QCAMERA_DUMP_FRM_INPUT_REPROCESS);
    cam_dimension_t dim = {sizeof(metadata_buffer_t), 1};
    cam_stream_buf_plane_info_t meta_planes;
    rc = mm_stream_calc_offset_metadata(&dim, &mPaddingInfo, &meta_planes);
    if (rc != 0) {
        LOGE("Metadata stream plane info calculation failed!");
        return rc;
    }
    uint32_t metaBufIdx;
    {
        Mutex::Autolock lock(mFreeOfflineMetaBuffersLock);
        if (mFreeOfflineMetaBuffersList.empty()) {
            LOGE("mFreeOfflineMetaBuffersList is null. Fatal");
            return BAD_VALUE;
        }

        metaBufIdx = *(mFreeOfflineMetaBuffersList.begin());
        mFreeOfflineMetaBuffersList.erase(mFreeOfflineMetaBuffersList.begin());
        LOGD("erasing %d, mFreeOfflineMetaBuffersList.size %d", metaBufIdx,
                mFreeOfflineMetaBuffersList.size());
    }

    mOfflineMetaMemory.markFrameNumber(metaBufIdx, frameNumber);

    mm_camera_buf_def_t meta_buf;
    cam_frame_len_offset_t offset = meta_planes.plane_info;
    rc = mOfflineMetaMemory.getBufDef(offset, meta_buf, metaBufIdx, true /*virtualAddr*/);
    if (NO_ERROR != rc) {
        return rc;
    }
    memcpy(meta_buf.buffer, metadata, sizeof(metadata_buffer_t));
    src_frame->metadata_buffer = meta_buf;
    src_frame->reproc_config = *reproc_cfg;
    src_frame->output_buffer = output_buffer;
    src_frame->frameNumber = frameNumber;
    return rc;
}

/*===========================================================================
 * FUNCTION   : checkStreamCbErrors
 *
 * DESCRIPTION: check the stream callback for errors
 *
 * PARAMETERS :
 *   @super_frame : the super frame with filled buffer
 *   @stream      : stream on which the buffer was requested and filled
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3ProcessingChannel::checkStreamCbErrors(mm_camera_super_buf_t *super_frame,
        QCamera3Stream *stream)
{
    if (NULL == stream) {
        LOGE("Invalid stream");
        return BAD_VALUE;
    }

    if(NULL == super_frame) {
         LOGE("Invalid Super buffer");
         return BAD_VALUE;
    }

    if(super_frame->num_bufs != 1) {
         LOGE("Multiple streams are not supported");
         return BAD_VALUE;
    }
    if(NULL == super_frame->bufs[0]) {
         LOGE("Error, Super buffer frame does not contain valid buffer");
         return BAD_VALUE;
    }
    return NO_ERROR;
}

/*===========================================================================
 * FUNCTION   : getStreamSize
 *
 * DESCRIPTION: get the size from the camera3_stream_t for the channel
 *
 * PARAMETERS :
 *   @dim     : Return the size of the stream
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3ProcessingChannel::getStreamSize(cam_dimension_t &dim)
{
    if (mCamera3Stream) {
        dim.width = mCamera3Stream->width;
        dim.height = mCamera3Stream->height;
        return NO_ERROR;
    } else {
        return BAD_VALUE;
    }
}

/*===========================================================================
 * FUNCTION   : getStreamBufs
 *
 * DESCRIPTION: get the buffers allocated to the stream
 *
 * PARAMETERS :
 * @len       : buffer length
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
QCamera3StreamMem* QCamera3ProcessingChannel::getStreamBufs(uint32_t /*len*/)
{
    KPI_ATRACE_CAMSCOPE_CALL(CAMSCOPE_HAL3_GETSTREAMBUFS);
    return &mMemory;
}

/*===========================================================================
 * FUNCTION   : putStreamBufs
 *
 * DESCRIPTION: release the buffers allocated to the stream
 *
 * PARAMETERS : NONE
 *
 * RETURN     : NONE
 *==========================================================================*/
void QCamera3ProcessingChannel::putStreamBufs()
{
    mMemory.unregisterBuffers();

    /* Reclaim all the offline metabuffers and push them to free list */
    {
        Mutex::Autolock lock(mFreeOfflineMetaBuffersLock);
        mFreeOfflineMetaBuffersList.clear();
        for (uint32_t i = 0; i < mOfflineMetaMemory.getCnt(); i++) {
            mFreeOfflineMetaBuffersList.push_back(i);
        }
    }
}


/*===========================================================================
 * FUNCTION   : stop
 *
 * DESCRIPTION: stop processing channel, which will stop all streams within,
 *              including the reprocessing channel in postprocessor.
 *
 * PARAMETERS : none
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3ProcessingChannel::stop()
{
    if (mStreamType == CAM_STREAM_TYPE_PREVIEW) {
        KPI_ATRACE_CAMSCOPE_CALL(CAMSCOPE_HAL3_STOP_PREVIEW);
    }
    int32_t rc = NO_ERROR;
    if(!m_bIsActive) {
        LOGE("Attempt to stop inactive channel");
        return rc;
    }

    m_postprocessor.stop();
    mPostProcStarted = false;
    rc |= QCamera3Channel::stop();
    return rc;
}

/*===========================================================================
 * FUNCTION   : startPostProc
 *
 * DESCRIPTION: figure out if the postprocessor needs to be restarted and if yes
 *              start it
 *
 * PARAMETERS :
 * @inputBufExists : whether there is an input buffer for post processing
 * @config         : reprocessing configuration
 * @metadata       : metadata associated with the reprocessing request
 *
 * RETURN     : NONE
 *==========================================================================*/
void QCamera3ProcessingChannel::startPostProc(const reprocess_config_t &config)
{
    if (mPostProcStarted) {
        if (config.reprocess_type != mReprocessType) {
            // If the reprocess type doesn't match, stop and start with the new type
            m_postprocessor.stop();
            mPostProcStarted = false;
        } else {
            // Return if reprocess type is the same.
            return;
        }
    }

    m_postprocessor.start(config);
    mPostProcStarted = true;
    mReprocessType = config.reprocess_type;
}

/*===========================================================================
 * FUNCTION   : queueReprocMetadata
 *
 * DESCRIPTION: queue the reprocess metadata to the postprocessor
 *
 * PARAMETERS : metadata : the metadata corresponding to the pp frame
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3ProcessingChannel::queueReprocMetadata(mm_camera_super_buf_t *metadata)
{
    return m_postprocessor.processPPMetadata(metadata);
}

/*===========================================================================
 * FUNCTION : metadataBufDone
 *
 * DESCRIPTION: Buffer done method for a metadata buffer
 *
 * PARAMETERS :
 * @recvd_frame : received metadata frame
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3ProcessingChannel::metadataBufDone(mm_camera_super_buf_t *recvd_frame)
{
    int32_t rc = NO_ERROR;;
    if ((NULL == m_pMetaChannel) || (NULL == recvd_frame)) {
        LOGE("Metadata channel or metadata buffer invalid");
        return BAD_VALUE;
    }

    rc = ((QCamera3MetadataChannel*)m_pMetaChannel)->bufDone(recvd_frame);

    return rc;
}

/*===========================================================================
 * FUNCTION : translateStreamTypeAndFormat
 *
 * DESCRIPTION: translates the framework stream format into HAL stream type
 *              and format
 *
 * PARAMETERS :
 * @streamType   : translated stream type
 * @streamFormat : translated stream format
 * @stream       : fwk stream
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3ProcessingChannel::translateStreamTypeAndFormat(camera3_stream_t *stream,
        cam_stream_type_t &streamType, cam_format_t &streamFormat)
{
    switch (stream->format) {
        case HAL_PIXEL_FORMAT_YCbCr_420_888:
            if(stream->stream_type == CAMERA3_STREAM_INPUT){
                streamType = CAM_STREAM_TYPE_SNAPSHOT;
                streamFormat = getStreamDefaultFormat(CAM_STREAM_TYPE_SNAPSHOT,
                        stream->width, stream->height, m_bUBWCenable, mIsType);
            } else {
                streamType = CAM_STREAM_TYPE_CALLBACK;
                streamFormat = getStreamDefaultFormat(CAM_STREAM_TYPE_CALLBACK,
                        stream->width, stream->height, m_bUBWCenable, mIsType);
            }
            break;
        case HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED:
            if (stream->usage & GRALLOC_USAGE_HW_VIDEO_ENCODER) {
                streamType = CAM_STREAM_TYPE_VIDEO;
                streamFormat = getStreamDefaultFormat(CAM_STREAM_TYPE_VIDEO,
                        stream->width, stream->height, m_bUBWCenable, mIsType);
            } else if(stream->stream_type == CAMERA3_STREAM_INPUT ||
                    stream->stream_type == CAMERA3_STREAM_BIDIRECTIONAL ||
                    IS_USAGE_ZSL(stream->usage)){
                streamType = CAM_STREAM_TYPE_SNAPSHOT;
                streamFormat = getStreamDefaultFormat(CAM_STREAM_TYPE_SNAPSHOT,
                        stream->width, stream->height, m_bUBWCenable, mIsType);
            } else {
                streamType = CAM_STREAM_TYPE_PREVIEW;
                streamFormat = getStreamDefaultFormat(CAM_STREAM_TYPE_PREVIEW,
                        stream->width, stream->height, m_bUBWCenable, mIsType);
            }
            break;
        case HAL_PIXEL_FORMAT_RAW_OPAQUE:
        case HAL_PIXEL_FORMAT_RAW16:
        case HAL_PIXEL_FORMAT_RAW10:
            streamType = CAM_STREAM_TYPE_RAW;
            if ((HAL_DATASPACE_DEPTH == stream->data_space) &&
                    (HAL_PIXEL_FORMAT_RAW16 == stream->format)) {
                streamFormat = CAM_FORMAT_META_RAW_10BIT;
            } else {
                streamFormat = CAM_FORMAT_BAYER_MIPI_RAW_10BPP_GBRG;
            }
            break;
        default:
            return -EINVAL;
    }
    LOGD("fwk_format = %d, streamType = %d, streamFormat = %d",
            stream->format, streamType, streamFormat);
    return NO_ERROR;
}

/*===========================================================================
 * FUNCTION : setReprocConfig
 *
 * DESCRIPTION: sets the reprocessing parameters for the input buffer
 *
 * PARAMETERS :
 * @reproc_cfg : the configuration to be set
 * @pInputBuffer : pointer to the input buffer
 * @metadata : pointer to the reprocessing metadata buffer
 * @streamFormat : format of the input stream
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3ProcessingChannel::setReprocConfig(reprocess_config_t &reproc_cfg,
        camera3_stream_buffer_t *pInputBuffer,
        __unused metadata_buffer_t *metadata,
        cam_format_t streamFormat, cam_dimension_t dim)
{
    int32_t rc = 0;
    reproc_cfg.padding = &mPaddingInfo;
    //to ensure a big enough buffer size set the height and width
    //padding to max(height padding, width padding)
    if (reproc_cfg.padding->height_padding > reproc_cfg.padding->width_padding) {
       reproc_cfg.padding->width_padding = reproc_cfg.padding->height_padding;
    } else {
       reproc_cfg.padding->height_padding = reproc_cfg.padding->width_padding;
    }
    if (NULL != pInputBuffer) {
        reproc_cfg.input_stream_dim.width = (int32_t)pInputBuffer->stream->width;
        reproc_cfg.input_stream_dim.height = (int32_t)pInputBuffer->stream->height;
    } else {
        reproc_cfg.input_stream_dim.width = (int32_t)dim.width;
        reproc_cfg.input_stream_dim.height = (int32_t)dim.height;
    }
    reproc_cfg.src_channel = this;
    reproc_cfg.output_stream_dim.width = mCamera3Stream->width;
    reproc_cfg.output_stream_dim.height = mCamera3Stream->height;
    reproc_cfg.reprocess_type = getReprocessType();

    //offset calculation
    if (NULL != pInputBuffer) {
        rc = translateStreamTypeAndFormat(pInputBuffer->stream,
                reproc_cfg.stream_type, reproc_cfg.stream_format);
        if (rc != NO_ERROR) {
            LOGE("Stream format %d is not supported",
                    pInputBuffer->stream->format);
            return rc;
        }
    } else {
        reproc_cfg.stream_type = mStreamType;
        reproc_cfg.stream_format = streamFormat;
    }

    switch (reproc_cfg.stream_type) {
        case CAM_STREAM_TYPE_PREVIEW:
            if (getStreamByIndex(0) == NULL) {
                LOGE("Could not find stream");
                rc = -1;
                break;
            }
            rc = mm_stream_calc_offset_preview(
                    getStreamByIndex(0)->getStreamInfo(),
                    &reproc_cfg.input_stream_dim,
                    reproc_cfg.padding,
                    &reproc_cfg.input_stream_plane_info);
            break;
        case CAM_STREAM_TYPE_VIDEO:
            rc = mm_stream_calc_offset_video(reproc_cfg.stream_format,
                    &reproc_cfg.input_stream_dim,
                    &reproc_cfg.input_stream_plane_info);
            break;
        case CAM_STREAM_TYPE_RAW:
            rc = mm_stream_calc_offset_raw(reproc_cfg.stream_format,
                    &reproc_cfg.input_stream_dim,
                    reproc_cfg.padding, &reproc_cfg.input_stream_plane_info);
            break;
        case CAM_STREAM_TYPE_SNAPSHOT:
        case CAM_STREAM_TYPE_CALLBACK:
        default:
            rc = mm_stream_calc_offset_snapshot(streamFormat, &reproc_cfg.input_stream_dim,
                    reproc_cfg.padding, &reproc_cfg.input_stream_plane_info);
            break;
    }
    if (rc != 0) {
        LOGE("Stream %d plane info calculation failed!", mStreamType);
        return rc;
    }

    IF_META_AVAILABLE(cam_hdr_param_t, hdr_info, CAM_INTF_PARM_HAL_BRACKETING_HDR, metadata) {
        reproc_cfg.hdr_param = *hdr_info;
    }

    return rc;
}

/*===========================================================================
 * FUNCTION   : reprocessCbRoutine
 *
 * DESCRIPTION: callback function for the reprocessed frame. This frame now
 *              should be returned to the framework
 *
 * PARAMETERS :
 * @resultBuffer      : buffer containing the reprocessed data
 * @resultFrameNumber : frame number on which the buffer was requested
 *
 * RETURN     : NONE
 *
 *==========================================================================*/
void QCamera3ProcessingChannel::reprocessCbRoutine(buffer_handle_t *resultBuffer,
        uint32_t resultFrameNumber)
{
    ATRACE_CAMSCOPE_CALL(CAMSCOPE_HAL3_PROC_CH_REPROC_CB);
    int rc = NO_ERROR;

    rc = releaseOfflineMemory(resultFrameNumber);
    if (NO_ERROR != rc) {
        LOGE("Error releasing offline memory %d", rc);
    }
    /* Since reprocessing is done, send the callback to release the input buffer */
    if (mChannelCB) {
        mChannelCB(NULL, NULL, resultFrameNumber, true, mUserData);
    }
    issueChannelCb(resultBuffer, resultFrameNumber);

    return;
}

/*===========================================================================
 * FUNCTION   : issueChannelCb
 *
 * DESCRIPTION: function to set the result and issue channel callback
 *
 * PARAMETERS :
 * @resultBuffer      : buffer containing the data
 * @resultFrameNumber : frame number on which the buffer was requested
 *
 * RETURN     : NONE
 *
 *
 *==========================================================================*/
void QCamera3ProcessingChannel::issueChannelCb(buffer_handle_t *resultBuffer,
        uint32_t resultFrameNumber)
{
    camera3_stream_buffer_t result;
    //Use below data to issue framework callback
    result.stream = mCamera3Stream;
    result.buffer = resultBuffer;
    result.status = CAMERA3_BUFFER_STATUS_OK;
    result.acquire_fence = -1;
    result.release_fence = -1;

    if (mChannelCB) {
        mChannelCB(NULL, &result, resultFrameNumber, false, mUserData);
    }
}

/*===========================================================================
 * FUNCTION   : showDebugFPS
 *
 * DESCRIPTION: Function to log the fps for preview, video, callback and raw
 *              streams
 *
 * PARAMETERS : Stream type
 *
 * RETURN  : None
 *==========================================================================*/
void QCamera3ProcessingChannel::showDebugFPS(int32_t streamType)
{
    double fps = 0;
    mFrameCount++;
    nsecs_t now = systemTime();
    nsecs_t diff = now - mLastFpsTime;
    if (diff > ms2ns(250)) {
        fps = (((double)(mFrameCount - mLastFrameCount)) *
                (double)(s2ns(1))) / (double)diff;
        switch(streamType) {
            case CAM_STREAM_TYPE_PREVIEW:
                LOGH("PROFILE_PREVIEW_FRAMES_PER_SECOND : %.4f: mFrameCount=%d",
                         fps, mFrameCount);
                break;
            case CAM_STREAM_TYPE_VIDEO:
                LOGH("PROFILE_VIDEO_FRAMES_PER_SECOND : %.4f",
                         fps);
                break;
            case CAM_STREAM_TYPE_CALLBACK:
                LOGH("PROFILE_CALLBACK_FRAMES_PER_SECOND : %.4f",
                         fps);
                break;
            case CAM_STREAM_TYPE_RAW:
                LOGH("PROFILE_RAW_FRAMES_PER_SECOND : %.4f",
                         fps);
                break;
            default:
                LOGH("logging not supported for the stream");
                break;
        }
        mLastFpsTime = now;
        mLastFrameCount = mFrameCount;
    }
}

/*===========================================================================
 * FUNCTION   : releaseOfflineMemory
 *
 * DESCRIPTION: function to clean up the offline memory used for input reprocess
 *
 * PARAMETERS :
 * @resultFrameNumber : frame number on which the buffer was requested
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              non-zero failure code
 *
 *
 *==========================================================================*/
int32_t QCamera3ProcessingChannel::releaseOfflineMemory(uint32_t resultFrameNumber)
{
    int32_t rc = NO_ERROR;
    int32_t inputBufIndex =
            mOfflineMemory.getGrallocBufferIndex(resultFrameNumber);
    if (0 <= inputBufIndex) {
        rc = mOfflineMemory.unregisterBuffer(inputBufIndex);
    } else {
        LOGW("Could not find offline input buffer, resultFrameNumber %d",
                 resultFrameNumber);
    }
    if (rc != NO_ERROR) {
        LOGE("Failed to unregister offline input buffer");
    }

    int32_t metaBufIndex =
            mOfflineMetaMemory.getHeapBufferIndex(resultFrameNumber);
    if (0 <= metaBufIndex) {
        Mutex::Autolock lock(mFreeOfflineMetaBuffersLock);
        mFreeOfflineMetaBuffersList.push_back((uint32_t)metaBufIndex);
    } else {
        LOGW("Could not find offline meta buffer, resultFrameNumber %d",
                resultFrameNumber);
    }

    return rc;
}

/* Regular Channel methods */
/*===========================================================================
 * FUNCTION   : QCamera3RegularChannel
 *
 * DESCRIPTION: constructor of QCamera3RegularChannel
 *
 * PARAMETERS :
 *   @cam_handle : camera handle
 *   @cam_ops    : ptr to camera ops table
 *   @cb_routine : callback routine to frame aggregator
 *   @stream     : camera3_stream_t structure
 *   @stream_type: Channel stream type
 *   @postprocess_mask: feature mask for postprocessing
 *   @metadataChannel : metadata channel for the session
 *   @numBuffers : number of max dequeued buffers
 *
 * RETURN     : none
 *==========================================================================*/
QCamera3RegularChannel::QCamera3RegularChannel(uint32_t cam_handle,
        uint32_t channel_handle,
        mm_camera_ops_t *cam_ops,
        channel_cb_routine cb_routine,
        channel_cb_buffer_err cb_buffer_err,
        cam_padding_info_t *paddingInfo,
        void *userData,
        camera3_stream_t *stream,
        cam_stream_type_t stream_type,
        cam_feature_mask_t postprocess_mask,
        QCamera3Channel *metadataChannel,
        uint32_t numBuffers) :
            QCamera3ProcessingChannel(cam_handle, channel_handle, cam_ops,
                    cb_routine, cb_buffer_err, paddingInfo, userData, stream, stream_type,
                    postprocess_mask, metadataChannel, numBuffers),
            mBatchSize(0),
            mRotation(ROTATE_0)
{
}

/*===========================================================================
 * FUNCTION   : ~QCamera3RegularChannel
 *
 * DESCRIPTION: destructor of QCamera3RegularChannel
 *
 * PARAMETERS : none
 *
 * RETURN     : none
 *==========================================================================*/
QCamera3RegularChannel::~QCamera3RegularChannel()
{
    destroy();
}

/*===========================================================================
 * FUNCTION   : initialize
 *
 * DESCRIPTION: Initialize and add camera channel & stream
 *
 * PARAMETERS :
 *    @isType : type of image stabilization required on this stream
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/

int32_t QCamera3RegularChannel::initialize(cam_is_type_t isType)
{
    ATRACE_CAMSCOPE_CALL(CAMSCOPE_HAL3_REG_CH_INIT);
    int32_t rc = NO_ERROR;

    cam_dimension_t streamDim;

    if (NULL == mCamera3Stream) {
        LOGE("Camera stream uninitialized");
        return NO_INIT;
    }

    if (1 <= m_numStreams) {
        // Only one stream per channel supported in v3 Hal
        return NO_ERROR;
    }

    mIsType  = isType;

    rc = translateStreamTypeAndFormat(mCamera3Stream, mStreamType,
            mStreamFormat);
    if (rc != NO_ERROR) {
        return -EINVAL;
    }


    if ((mStreamType == CAM_STREAM_TYPE_VIDEO) ||
            (mStreamType == CAM_STREAM_TYPE_PREVIEW)) {
        if ((mCamera3Stream->rotation != CAMERA3_STREAM_ROTATION_0) &&
                ((mPostProcMask & CAM_QCOM_FEATURE_ROTATION) == 0)) {
            LOGE("attempting rotation %d when rotation is disabled",
                    mCamera3Stream->rotation);
            return -EINVAL;
        }

        switch (mCamera3Stream->rotation) {
            case CAMERA3_STREAM_ROTATION_0:
                mRotation = ROTATE_0;
                break;
            case CAMERA3_STREAM_ROTATION_90: {
                mRotation = ROTATE_90;
                break;
            }
            case CAMERA3_STREAM_ROTATION_180:
                mRotation = ROTATE_180;
                break;
            case CAMERA3_STREAM_ROTATION_270: {
                mRotation = ROTATE_270;
                break;
            }
            default:
                LOGE("Unknown rotation: %d",
                         mCamera3Stream->rotation);
            return -EINVAL;
        }

        // Camera3/HAL3 spec expecting counter clockwise rotation but CPP HW is
        // doing Clockwise rotation and so swap it.
        if (mRotation == ROTATE_90) {
            mRotation = ROTATE_270;
        } else if (mRotation == ROTATE_270) {
            mRotation = ROTATE_90;
        }

    } else if (mCamera3Stream->rotation != CAMERA3_STREAM_ROTATION_0) {
        LOGE("Rotation %d is not supported by stream type %d",
                mCamera3Stream->rotation,
                mStreamType);
        return -EINVAL;
    }

    streamDim.width = mCamera3Stream->width;
    streamDim.height = mCamera3Stream->height;

    LOGD("batch size is %d", mBatchSize);
    rc = QCamera3Channel::addStream(mStreamType,
            mStreamFormat,
            streamDim,
            mRotation,
            mNumBufs,
            mPostProcMask,
            mIsType,
            mBatchSize);

    return rc;
}

/*===========================================================================
 * FUNCTION   : setBatchSize
 *
 * DESCRIPTION: Set batch size for the channel.
 *
 * PARAMETERS :
 *   @batchSize  : Number of image buffers in a batch
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success always
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3RegularChannel::setBatchSize(uint32_t batchSize)
{
    int32_t rc = NO_ERROR;

    mBatchSize = batchSize;
    LOGD("Batch size set: %d", mBatchSize);
    return rc;
}

/*===========================================================================
 * FUNCTION   : getStreamTypeMask
 *
 * DESCRIPTION: Get bit mask of all stream types in this channel.
 *              If stream is not initialized, then generate mask based on
 *              local streamType
 *
 * PARAMETERS : None
 *
 * RETURN     : Bit mask of all stream types in this channel
 *==========================================================================*/
uint32_t QCamera3RegularChannel::getStreamTypeMask()
{
    if (mStreams[0]) {
        return QCamera3Channel::getStreamTypeMask();
    } else {
        return (1U << mStreamType);
    }
}

/*===========================================================================
 * FUNCTION   : queueBatchBuf
 *
 * DESCRIPTION: queue batch container to downstream
 *
 * PARAMETERS :
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success always
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3RegularChannel::queueBatchBuf()
{
    int32_t rc = NO_ERROR;

    if (mStreams[0]) {
        rc = mStreams[0]->queueBatchBuf();
    }
    if (rc != NO_ERROR) {
        LOGE("stream->queueBatchContainer failed");
    }
    return rc;
}

/*===========================================================================
 * FUNCTION   : request
 *
 * DESCRIPTION: process a request from camera service. Stream on if ncessary.
 *
 * PARAMETERS :
 *   @buffer  : buffer to be filled for this request
 *
 * RETURN     : 0 on a success start of capture
 *              -EINVAL on invalid input
 *              -ENODEV on serious error
 *==========================================================================*/
int32_t QCamera3RegularChannel::request(buffer_handle_t *buffer, uint32_t frameNumber, int &indexUsed)
{
    ATRACE_CAMSCOPE_CALL(CAMSCOPE_HAL3_REG_CH_REQ);
    //FIX ME: Return buffer back in case of failures below.

    int32_t rc = NO_ERROR;
    int index;

    if (NULL == buffer) {
        LOGE("Invalid buffer in channel request");
        return BAD_VALUE;
    }

    index = mMemory.getMatchBufIndex((void*)buffer);
    if(index < 0) {
        rc = registerBuffer(buffer, mIsType);
        if (NO_ERROR != rc) {
            LOGE("On-the-fly buffer registration failed %d",
                     rc);
            return rc;
        }

        index = mMemory.getMatchBufIndex((void*)buffer);
        if (index < 0) {
            LOGE("Could not find object among registered buffers");
            return DEAD_OBJECT;
        }
    }

    rc = mMemory.markFrameNumber((uint32_t)index, frameNumber);
    if(rc != NO_ERROR) {
        LOGE("Failed to mark FrameNumber:%d,idx:%d",frameNumber,index);
        return rc;
    }
    if (m_bIsActive) {
        rc = mStreams[0]->bufDone((uint32_t)index);
        if(rc != NO_ERROR) {
            LOGE("Failed to Q new buffer to stream");
            mMemory.markFrameNumber(index, -1);
            return rc;
        }
    }

    indexUsed = index;
    return rc;
}

/*===========================================================================
 * FUNCTION   : getReprocessType
 *
 * DESCRIPTION: get the type of reprocess output supported by this channel
 *
 * PARAMETERS : NONE
 *
 * RETURN     : reprocess_type_t : type of reprocess
 *==========================================================================*/
reprocess_type_t QCamera3RegularChannel::getReprocessType()
{
    return REPROCESS_TYPE_PRIVATE;
}


QCamera3MetadataChannel::QCamera3MetadataChannel(uint32_t cam_handle,
                    uint32_t channel_handle,
                    mm_camera_ops_t *cam_ops,
                    channel_cb_routine cb_routine,
                    channel_cb_buffer_err cb_buffer_err,
                    cam_padding_info_t *paddingInfo,
                    cam_feature_mask_t postprocess_mask,
                    void *userData, uint32_t numBuffers) :
                        QCamera3Channel(cam_handle, channel_handle, cam_ops,
                                cb_routine, cb_buffer_err, paddingInfo, postprocess_mask,
                                userData, numBuffers),
                        mMemory(NULL), mDepthDataPresent(false)
{
    mMapStreamBuffers = true;
}

QCamera3MetadataChannel::~QCamera3MetadataChannel()
{
    destroy();

    if (mMemory) {
        mMemory->deallocate();
        delete mMemory;
        mMemory = NULL;
    }
}

int32_t QCamera3MetadataChannel::initialize(cam_is_type_t isType)
{
    ATRACE_CAMSCOPE_CALL(CAMSCOPE_HAL3_METADATA_CH_INIT);
    int32_t rc;
    cam_dimension_t streamDim;

    if (mMemory || m_numStreams > 0) {
        LOGE("metadata channel already initialized");
        return -EINVAL;
    }

    streamDim.width = (int32_t)sizeof(metadata_buffer_t),
    streamDim.height = 1;

    mIsType = isType;
    rc = QCamera3Channel::addStream(CAM_STREAM_TYPE_METADATA, CAM_FORMAT_MAX,
            streamDim, ROTATE_0, (uint8_t)mNumBuffers, mPostProcMask, mIsType);
    if (rc < 0) {
        LOGE("addStream failed");
    }
    return rc;
}

int32_t QCamera3MetadataChannel::request(buffer_handle_t * /*buffer*/,
                                                uint32_t /*frameNumber*/,
                                                int&  /*indexUsed*/)
{
    if (!m_bIsActive) {
        return start();
    }
    else
        return 0;
}

void QCamera3MetadataChannel::streamCbRoutine(
                        mm_camera_super_buf_t *super_frame,
                        QCamera3Stream * /*stream*/)
{
    ATRACE_NAME("metadata_stream_cb_routine");
    uint32_t requestNumber = 0;
    if (super_frame == NULL || super_frame->num_bufs != 1) {
        LOGE("super_frame is not valid");
        return;
    }
    if (mChannelCB) {
        mChannelCB(super_frame, NULL, requestNumber, false, mUserData);
    }
}

QCamera3StreamMem* QCamera3MetadataChannel::getStreamBufs(uint32_t len)
{
    int rc;
    if (len < sizeof(metadata_buffer_t)) {
        LOGE("Metadata buffer size less than structure %d vs %d",
                len,
                sizeof(metadata_buffer_t));
        return NULL;
    }
    mMemory = new QCamera3StreamMem(MIN_STREAMING_BUFFER_NUM);
    if (!mMemory) {
        LOGE("unable to create metadata memory");
        return NULL;
    }
    rc = mMemory->allocateAll(len);
    if (rc < 0) {
        LOGE("unable to allocate metadata memory");
        delete mMemory;
        mMemory = NULL;
        return NULL;
    }
    clear_metadata_buffer((metadata_buffer_t*)mMemory->getPtr(0));

    for (uint32_t i = 0; i < mMemory->getCnt(); i++) {
        if (mMemory->valid(i)) {
            metadata_buffer_t *metadata_buffer_t =
                    static_cast<::metadata_buffer_t *> (mMemory->getPtr(i));
            metadata_buffer_t->depth_data.depth_data = nullptr;
            if (mDepthDataPresent) {
                metadata_buffer_t->depth_data.depth_data =
                        new uint8_t[PD_DATA_SIZE];
            }
        } else {
            LOGE("Invalid meta buffer at index: %d", i);
        }
    }

    return mMemory;
}

void QCamera3MetadataChannel::putStreamBufs()
{
    for (uint32_t i = 0; i < mMemory->getCnt(); i++) {
        if (mMemory->valid(i)) {
            metadata_buffer_t *metadata_buffer_t =
                    static_cast<::metadata_buffer_t *> (mMemory->getPtr(i));
            if (nullptr != metadata_buffer_t->depth_data.depth_data) {
                delete [] metadata_buffer_t->depth_data.depth_data;
                metadata_buffer_t->depth_data.depth_data = nullptr;
            }
        } else {
            LOGE("Invalid meta buffer at index: %d", i);
        }
    }

    mMemory->deallocate();
    delete mMemory;
    mMemory = NULL;
}
/*************************************************************************************/
// RAW Channel related functions
QCamera3RawChannel::QCamera3RawChannel(uint32_t cam_handle,
                    uint32_t channel_handle,
                    mm_camera_ops_t *cam_ops,
                    channel_cb_routine cb_routine,
                    channel_cb_buffer_err cb_buffer_err,
                    cam_padding_info_t *paddingInfo,
                    void *userData,
                    camera3_stream_t *stream,
                    cam_feature_mask_t postprocess_mask,
                    QCamera3Channel *metadataChannel,
                    bool raw_16, uint32_t numBuffers) :
                        QCamera3RegularChannel(cam_handle, channel_handle, cam_ops,
                                cb_routine, cb_buffer_err, paddingInfo, userData, stream,
                                CAM_STREAM_TYPE_RAW, postprocess_mask,
                                metadataChannel, numBuffers),
                        mIsRaw16(raw_16)
{
    char prop[PROPERTY_VALUE_MAX];
    property_get("persist.camera.raw.debug.dump", prop, "0");
    mRawDump = atoi(prop);
    mMapStreamBuffers = (mRawDump || mIsRaw16);
}

QCamera3RawChannel::~QCamera3RawChannel()
{
}

/*===========================================================================
 * FUNCTION   : initialize
 *
 * DESCRIPTION: Initialize and add camera channel & stream
 *
 * PARAMETERS :
 * @isType    : image stabilization type on the stream
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/

int32_t QCamera3RawChannel::initialize(cam_is_type_t isType)
{
    return QCamera3RegularChannel::initialize(isType);
}

void QCamera3RawChannel::streamCbRoutine(
                        mm_camera_super_buf_t *super_frame,
                        QCamera3Stream * stream)
{
    ATRACE_CAMSCOPE_CALL(CAMSCOPE_HAL3_RAW_CH_STRM_CB);
    /* Move this back down once verified */
    if (mRawDump)
        dumpRawSnapshot(super_frame->bufs[0]);

    if (mIsRaw16) {
        cam_format_t streamFormat = getStreamDefaultFormat(CAM_STREAM_TYPE_RAW,
                mCamera3Stream->width, mCamera3Stream->height, m_bUBWCenable, mIsType);
        if (streamFormat == CAM_FORMAT_BAYER_MIPI_RAW_10BPP_GBRG)
            convertMipiToRaw16(super_frame->bufs[0]);
        else
            convertLegacyToRaw16(super_frame->bufs[0]);

        //Make sure cache coherence because extra processing is done
        mMemory.cleanCache(super_frame->bufs[0]->buf_idx);
    }

    QCamera3RegularChannel::streamCbRoutine(super_frame, stream);
    return;
}

void QCamera3RawChannel::dumpRawSnapshot(mm_camera_buf_def_t *frame)
{
   QCamera3Stream *stream = getStreamByIndex(0);
   if (stream != NULL) {
       char buf[FILENAME_MAX];
       memset(buf, 0, sizeof(buf));
       cam_dimension_t dim;
       memset(&dim, 0, sizeof(dim));
       stream->getFrameDimension(dim);

       cam_frame_len_offset_t offset;
       memset(&offset, 0, sizeof(cam_frame_len_offset_t));
       stream->getFrameOffset(offset);
       snprintf(buf, sizeof(buf), QCAMERA_DUMP_FRM_LOCATION"r_%d_%dx%d.raw",
                frame->frame_idx, offset.mp[0].stride, offset.mp[0].scanline);

       int file_fd = open(buf, O_RDWR| O_CREAT, 0644);
       if (file_fd >= 0) {
          ssize_t written_len = write(file_fd, frame->buffer, frame->frame_len);
          LOGD("written number of bytes %zd", written_len);
          frame->cache_flags |= CPU_HAS_READ;
          close(file_fd);
       } else {
          LOGE("failed to open file to dump image");
       }
   } else {
       LOGE("Could not find stream");
   }

}

void QCamera3RawChannel::convertLegacyToRaw16(mm_camera_buf_def_t *frame)
{
    // Convert image buffer from Opaque raw format to RAW16 format
    // 10bit Opaque raw is stored in the format of:
    // 0000 - p5 - p4 - p3 - p2 - p1 - p0
    // where p0 to p5 are 6 pixels (each is 10bit)_and most significant
    // 4 bits are 0s. Each 64bit word contains 6 pixels.

  QCamera3Stream *stream = getStreamByIndex(0);
  if (stream != NULL) {
      cam_dimension_t dim;
      memset(&dim, 0, sizeof(dim));
      stream->getFrameDimension(dim);

      cam_frame_len_offset_t offset;
      memset(&offset, 0, sizeof(cam_frame_len_offset_t));
      stream->getFrameOffset(offset);

      uint32_t raw16_stride = ((uint32_t)dim.width + 15U) & ~15U;
      uint16_t* raw16_buffer = (uint16_t *)frame->buffer;

      // In-place format conversion.
      // Raw16 format always occupy more memory than opaque raw10.
      // Convert to Raw16 by iterating through all pixels from bottom-right
      // to top-left of the image.
      // One special notes:
      // 1. Cross-platform raw16's stride is 16 pixels.
      // 2. Opaque raw10's stride is 6 pixels, and aligned to 16 bytes.
      for (int32_t ys = dim.height - 1; ys >= 0; ys--) {
          uint32_t y = (uint32_t)ys;
          uint64_t* row_start = (uint64_t *)frame->buffer +
                  y * (uint32_t)offset.mp[0].stride_in_bytes / 8;
          for (int32_t xs = dim.width - 1; xs >= 0; xs--) {
              uint32_t x = (uint32_t)xs;
              uint16_t raw16_pixel = 0x3FF & (row_start[x/6] >> (10*(x%6)));
              raw16_buffer[y*raw16_stride+x] = raw16_pixel;
          }
      }
  } else {
      LOGE("Could not find stream");
  }

}

void QCamera3RawChannel::convertMipiToRaw16(mm_camera_buf_def_t *frame)
{
    // Convert image buffer from mipi10 raw format to RAW16 format
    // mipi10 opaque raw is stored in the format of:
    // P3(1:0) P2(1:0) P1(1:0) P0(1:0) P3(9:2) P2(9:2) P1(9:2) P0(9:2)
    // 4 pixels occupy 5 bytes, no padding needed

    QCamera3Stream *stream = getStreamByIndex(0);
    if (stream != NULL) {
        cam_dimension_t dim;
        memset(&dim, 0, sizeof(dim));
        stream->getFrameDimension(dim);

        cam_frame_len_offset_t offset;
        memset(&offset, 0, sizeof(cam_frame_len_offset_t));
        stream->getFrameOffset(offset);

        uint32_t raw16_stride = ((uint32_t)dim.width + 15U) & ~15U;
        uint16_t* raw16_buffer = (uint16_t *)frame->buffer;
        uint8_t first_quintuple[5];
        memcpy(first_quintuple, raw16_buffer, sizeof(first_quintuple));

        // In-place format conversion.
        // Raw16 format always occupy more memory than opaque raw10.
        // Convert to Raw16 by iterating through all pixels from bottom-right
        // to top-left of the image.
        // One special notes:
        // 1. Cross-platform raw16's stride is 16 pixels.
        // 2. mipi raw10's stride is 4 pixels, and aligned to 16 bytes.
        for (int32_t ys = dim.height - 1; ys >= 0; ys--) {
            uint32_t y = (uint32_t)ys;
            uint8_t* row_start = (uint8_t *)frame->buffer +
                    y * (uint32_t)offset.mp[0].stride_in_bytes;
            for (int32_t xs = dim.width - 1; xs >= 0; xs--) {
                uint32_t x = (uint32_t)xs;
                uint8_t upper_8bit = row_start[5*(x/4)+x%4];
                uint8_t lower_2bit = ((row_start[5*(x/4)+4] >> ((x%4) << 1)) & 0x3);
                uint16_t raw16_pixel =
                        (uint16_t)(((uint16_t)upper_8bit)<<2 |
                        (uint16_t)lower_2bit);
                raw16_buffer[y*raw16_stride+x] = raw16_pixel;
            }
        }

        // Re-convert the first 2 pixels of the buffer because the loop above messes
        // them up by reading the first quintuple while modifying it.
        raw16_buffer[0] = ((uint16_t)first_quintuple[0]<<2) | (first_quintuple[4] & 0x3);
        raw16_buffer[1] = ((uint16_t)first_quintuple[1]<<2) | ((first_quintuple[4] >> 2) & 0x3);

    } else {
        LOGE("Could not find stream");
    }

}

/*===========================================================================
 * FUNCTION   : getReprocessType
 *
 * DESCRIPTION: get the type of reprocess output supported by this channel
 *
 * PARAMETERS : NONE
 *
 * RETURN     : reprocess_type_t : type of reprocess
 *==========================================================================*/
reprocess_type_t QCamera3RawChannel::getReprocessType()
{
    return REPROCESS_TYPE_RAW;
}


/*************************************************************************************/
// RAW Dump Channel related functions

/*===========================================================================
 * FUNCTION   : QCamera3RawDumpChannel
 *
 * DESCRIPTION: Constructor for RawDumpChannel
 *
 * PARAMETERS :
 *   @cam_handle    : Handle for Camera
 *   @cam_ops       : Function pointer table
 *   @rawDumpSize   : Dimensions for the Raw stream
 *   @paddinginfo   : Padding information for stream
 *   @userData      : Cookie for parent
 *   @pp mask       : PP feature mask for this stream
 *   @numBuffers    : number of max dequeued buffers
 *
 * RETURN           : NA
 *==========================================================================*/
QCamera3RawDumpChannel::QCamera3RawDumpChannel(uint32_t cam_handle,
                    uint32_t channel_handle,
                    mm_camera_ops_t *cam_ops,
                    cam_dimension_t rawDumpSize,
                    cam_padding_info_t *paddingInfo,
                    void *userData,
                    cam_feature_mask_t postprocess_mask, uint32_t numBuffers) :
                        QCamera3Channel(cam_handle, channel_handle, cam_ops, NULL,
                                NULL, paddingInfo, postprocess_mask,
                                userData, numBuffers),
                        mDim(rawDumpSize),
                        mMemory(NULL)
{
    char prop[PROPERTY_VALUE_MAX];
    property_get("persist.camera.raw.dump", prop, "0");
    mRawDump = atoi(prop);
}

/*===========================================================================
 * FUNCTION   : QCamera3RawDumpChannel
 *
 * DESCRIPTION: Destructor for RawDumpChannel
 *
 * PARAMETERS :
 *
 * RETURN           : NA
 *==========================================================================*/

QCamera3RawDumpChannel::~QCamera3RawDumpChannel()
{
    destroy();
}

/*===========================================================================
 * FUNCTION   : dumpRawSnapshot
 *
 * DESCRIPTION: Helper function to dump Raw frames
 *
 * PARAMETERS :
 *  @frame      : stream buf frame to be dumped
 *
 *  RETURN      : NA
 *==========================================================================*/
void QCamera3RawDumpChannel::dumpRawSnapshot(mm_camera_buf_def_t *frame)
{
    QCamera3Stream *stream = getStreamByIndex(0);
    if (stream != NULL) {
        char buf[FILENAME_MAX];
        struct timeval tv;
        struct tm timeinfo_data;
        struct tm *timeinfo;

        cam_dimension_t dim;
        memset(&dim, 0, sizeof(dim));
        stream->getFrameDimension(dim);

        cam_frame_len_offset_t offset;
        memset(&offset, 0, sizeof(cam_frame_len_offset_t));
        stream->getFrameOffset(offset);

        gettimeofday(&tv, NULL);
        timeinfo = localtime_r(&tv.tv_sec, &timeinfo_data);

        if (NULL != timeinfo) {
            memset(buf, 0, sizeof(buf));
            snprintf(buf, sizeof(buf),
                    QCAMERA_DUMP_FRM_LOCATION
                    "%04d-%02d-%02d-%02d-%02d-%02d-%06ld_%d_%dx%d.raw",
                    timeinfo->tm_year + 1900, timeinfo->tm_mon + 1,
                    timeinfo->tm_mday, timeinfo->tm_hour,
                    timeinfo->tm_min, timeinfo->tm_sec,tv.tv_usec,
                    frame->frame_idx, dim.width, dim.height);

            int file_fd = open(buf, O_RDWR| O_CREAT, 0777);
            if (file_fd >= 0) {
                ssize_t written_len =
                        write(file_fd, frame->buffer, offset.frame_len);
                LOGD("written number of bytes %zd", written_len);
                frame->cache_flags |= CPU_HAS_READ;
                close(file_fd);
            } else {
                LOGE("failed to open file to dump image");
            }
        } else {
            LOGE("localtime_r() error");
        }
    } else {
        LOGE("Could not find stream");
    }

}

/*===========================================================================
 * FUNCTION   : streamCbRoutine
 *
 * DESCRIPTION: Callback routine invoked for each frame generated for
 *              Rawdump channel
 *
 * PARAMETERS :
 *   @super_frame  : stream buf frame generated
 *   @stream       : Underlying Stream object cookie
 *
 * RETURN          : NA
 *==========================================================================*/
void QCamera3RawDumpChannel::streamCbRoutine(mm_camera_super_buf_t *super_frame,
                                                __unused QCamera3Stream *stream)
{
    LOGD("E");
    if (super_frame == NULL || super_frame->num_bufs != 1) {
        LOGE("super_frame is not valid");
        return;
    }

    if (mRawDump)
        dumpRawSnapshot(super_frame->bufs[0]);

    bufDone(super_frame);
    free(super_frame);
}

/*===========================================================================
 * FUNCTION   : getStreamBufs
 *
 * DESCRIPTION: Callback function provided to interface to get buffers.
 *
 * PARAMETERS :
 *   @len       : Length of each buffer to be allocated
 *
 * RETURN     : NULL on buffer allocation failure
 *              QCamera3StreamMem object on sucess
 *==========================================================================*/
QCamera3StreamMem* QCamera3RawDumpChannel::getStreamBufs(uint32_t len)
{
    int rc;
    mMemory = new QCamera3StreamMem(mNumBuffers);

    if (!mMemory) {
        LOGE("unable to create heap memory");
        return NULL;
    }
    rc = mMemory->allocateAll((size_t)len);
    if (rc < 0) {
        LOGE("unable to allocate heap memory");
        delete mMemory;
        mMemory = NULL;
        return NULL;
    }
    return mMemory;
}

/*===========================================================================
 * FUNCTION   : putStreamBufs
 *
 * DESCRIPTION: Callback function provided to interface to return buffers.
 *              Although no handles are actually returned, implicitl assumption
 *              that interface will no longer use buffers and channel can
 *              deallocated if necessary.
 *
 * PARAMETERS : NA
 *
 * RETURN     : NA
 *==========================================================================*/
void QCamera3RawDumpChannel::putStreamBufs()
{
    mMemory->deallocate();
    delete mMemory;
    mMemory = NULL;
}

/*===========================================================================
 * FUNCTION : request
 *
 * DESCRIPTION: Request function used as trigger
 *
 * PARAMETERS :
 * @recvd_frame : buffer- this will be NULL since this is internal channel
 * @frameNumber : Undefined again since this is internal stream
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3RawDumpChannel::request(buffer_handle_t * /*buffer*/,
                                                uint32_t /*frameNumber*/,
                                                int & /*indexUsed*/)
{
    if (!m_bIsActive) {
        return QCamera3Channel::start();
    }
    else
        return 0;
}

/*===========================================================================
 * FUNCTION : intialize
 *
 * DESCRIPTION: Initializes channel params and creates underlying stream
 *
 * PARAMETERS :
 *    @isType : type of image stabilization required on this stream
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3RawDumpChannel::initialize(cam_is_type_t isType)
{
    int32_t rc;

    mIsType = isType;
    rc = QCamera3Channel::addStream(CAM_STREAM_TYPE_RAW,
        CAM_FORMAT_BAYER_MIPI_RAW_10BPP_GBRG, mDim, ROTATE_0, (uint8_t)mNumBuffers,
        mPostProcMask, mIsType);
    if (rc < 0) {
        LOGE("addStream failed");
    }
    return rc;
}

/*************************************************************************************/
// HDR+ RAW Source Channel related functions
QCamera3HdrPlusRawSrcChannel::QCamera3HdrPlusRawSrcChannel(uint32_t cam_handle,
                    uint32_t channel_handle,
                    mm_camera_ops_t *cam_ops,
                    cam_dimension_t rawDumpSize,
                    cam_padding_info_t *paddingInfo,
                    void *userData,
                    cam_feature_mask_t postprocess_mask,
                    std::shared_ptr<HdrPlusClient> hdrPlusClient,
                    uint32_t hdrPlusStreamId,
                    uint32_t numBuffers) :
    QCamera3RawDumpChannel(cam_handle, channel_handle, cam_ops, rawDumpSize, paddingInfo, userData,
                    postprocess_mask, numBuffers),
    mHdrPlusClient(hdrPlusClient),
    mHdrPlusStreamId(hdrPlusStreamId)
{

}

QCamera3HdrPlusRawSrcChannel::~QCamera3HdrPlusRawSrcChannel()
{
}

void QCamera3HdrPlusRawSrcChannel::streamCbRoutine(mm_camera_super_buf_t *super_frame,
                            __unused QCamera3Stream *stream)
{
    if (super_frame == NULL || super_frame->num_bufs != 1) {
        LOGE("super_frame is not valid");
        return;
    }

    // Send RAW buffer to HDR+ service
    sendRawToHdrPlusService(super_frame->bufs[0]);

    bufDone(super_frame);
    free(super_frame);
}

void QCamera3HdrPlusRawSrcChannel::sendRawToHdrPlusService(mm_camera_buf_def_t *frame)
{
    QCamera3Stream *stream = getStreamByIndex(0);
    if (stream == nullptr) {
        LOGE("%s: Could not find stream.", __FUNCTION__);
        return;
    }

    cam_frame_len_offset_t offset = {};
    stream->getFrameOffset(offset);

    pbcamera::StreamBuffer buffer;
    buffer.streamId = mHdrPlusStreamId;
    buffer.data = frame->buffer;
    buffer.dataSize = offset.frame_len;

    // Use the frame timestamp as mock Easel timestamp.
    int64_t mockEaselTimestampNs = (int64_t)frame->ts.tv_sec * 1000000000 + frame->ts.tv_nsec;
    mHdrPlusClient->notifyInputBuffer(buffer, mockEaselTimestampNs);
}

/*************************************************************************************/

/* QCamera3YUVChannel methods */

/*===========================================================================
 * FUNCTION   : QCamera3YUVChannel
 *
 * DESCRIPTION: constructor of QCamera3YUVChannel
 *
 * PARAMETERS :
 *   @cam_handle : camera handle
 *   @cam_ops    : ptr to camera ops table
 *   @cb_routine : callback routine to frame aggregator
 *   @paddingInfo : padding information for the stream
 *   @stream     : camera3_stream_t structure
 *   @stream_type: Channel stream type
 *   @postprocess_mask: the postprocess mask for streams of this channel
 *   @metadataChannel: handle to the metadataChannel
 * RETURN     : none
 *==========================================================================*/
QCamera3YUVChannel::QCamera3YUVChannel(uint32_t cam_handle,
        uint32_t channel_handle,
        mm_camera_ops_t *cam_ops,
        channel_cb_routine cb_routine,
        channel_cb_buffer_err cb_buf_err,
        cam_padding_info_t *paddingInfo,
        void *userData,
        camera3_stream_t *stream,
        cam_stream_type_t stream_type,
        cam_feature_mask_t postprocess_mask,
        QCamera3Channel *metadataChannel) :
            QCamera3ProcessingChannel(cam_handle, channel_handle, cam_ops,
                    cb_routine, cb_buf_err, paddingInfo, userData, stream, stream_type,
                    postprocess_mask, metadataChannel)
{

    mBypass = (postprocess_mask == CAM_QCOM_FEATURE_NONE);
    mFrameLen = 0;
    mEdgeMode.edge_mode = CAM_EDGE_MODE_OFF;
    mEdgeMode.sharpness = 0;
    mNoiseRedMode = CAM_NOISE_REDUCTION_MODE_OFF;
    memset(&mCropRegion, 0, sizeof(mCropRegion));
}

/*===========================================================================
 * FUNCTION   : ~QCamera3YUVChannel
 *
 * DESCRIPTION: destructor of QCamera3YUVChannel
 *
 * PARAMETERS : none
 *
 *
 * RETURN     : none
 *==========================================================================*/
QCamera3YUVChannel::~QCamera3YUVChannel()
{
   // Deallocation of heap buffers allocated in mMemory is freed
   // automatically by its destructor
}

/*===========================================================================
 * FUNCTION   : initialize
 *
 * DESCRIPTION: Initialize and add camera channel & stream
 *
 * PARAMETERS :
 * @isType    : the image stabilization type
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3YUVChannel::initialize(cam_is_type_t isType)
{
    ATRACE_CAMSCOPE_CALL(CAMSCOPE_HAL3_YUV_CH_INIT);
    int32_t rc = NO_ERROR;
    cam_dimension_t streamDim;

    if (NULL == mCamera3Stream) {
        LOGE("Camera stream uninitialized");
        return NO_INIT;
    }

    if (1 <= m_numStreams) {
        // Only one stream per channel supported in v3 Hal
        return NO_ERROR;
    }

    mIsType  = isType;
    mStreamFormat = getStreamDefaultFormat(CAM_STREAM_TYPE_CALLBACK,
            mCamera3Stream->width, mCamera3Stream->height, m_bUBWCenable, mIsType);
    streamDim.width = mCamera3Stream->width;
    streamDim.height = mCamera3Stream->height;

    rc = QCamera3Channel::addStream(mStreamType,
            mStreamFormat,
            streamDim,
            ROTATE_0,
            mNumBufs,
            mPostProcMask,
            mIsType);
    if (rc < 0) {
        LOGE("addStream failed");
        return rc;
    }

    cam_stream_buf_plane_info_t buf_planes;
    cam_padding_info_t paddingInfo = mPaddingInfo;

    memset(&buf_planes, 0, sizeof(buf_planes));
    //to ensure a big enough buffer size set the height and width
    //padding to max(height padding, width padding)
    paddingInfo.width_padding = MAX(paddingInfo.width_padding, paddingInfo.height_padding);
    paddingInfo.height_padding = paddingInfo.width_padding;

    rc = mm_stream_calc_offset_snapshot(mStreamFormat, &streamDim, &paddingInfo,
            &buf_planes);
    if (rc < 0) {
        LOGE("mm_stream_calc_offset_preview failed");
        return rc;
    }

    mFrameLen = buf_planes.plane_info.frame_len;

    if (NO_ERROR != rc) {
        LOGE("Initialize failed, rc = %d", rc);
        return rc;
    }

    /* initialize offline meta memory for input reprocess */
    rc = QCamera3ProcessingChannel::initialize(isType);
    if (NO_ERROR != rc) {
        LOGE("Processing Channel initialize failed, rc = %d",
                 rc);
    }

    return rc;
}

/*===========================================================================
 * FUNCTION   : request
 *
 * DESCRIPTION: entry function for a request on a YUV stream. This function
 *              has the logic to service a request based on its type
 *
 * PARAMETERS :
 * @buffer          : pointer to the output buffer
 * @frameNumber     : frame number of the request
 * @pInputBuffer    : pointer to input buffer if an input request
 * @metadata        : parameters associated with the request
 * @internalreq      : boolean to indicate if this is purely internal request
 *                    needing internal buffer allocation
 * @meteringonly    : boolean indicating metering only frame subset of internal
 *                    not consumed by postprocessor
 *
 * RETURN     : 0 on a success start of capture
 *              -EINVAL on invalid input
 *              -ENODEV on serious error
 *==========================================================================*/
int32_t QCamera3YUVChannel::request(buffer_handle_t *buffer,
        uint32_t frameNumber,
        camera3_stream_buffer_t* pInputBuffer,
        metadata_buffer_t* metadata, bool &needMetadata,
        int &indexUsed,
        __unused bool internalRequest = false,
        __unused bool meteringOnly = false)
{
    int32_t rc = NO_ERROR;
    Mutex::Autolock lock(mOfflinePpLock);

    LOGD("pInputBuffer is %p frame number %d", pInputBuffer, frameNumber);
    if (NULL == buffer || NULL == metadata) {
        LOGE("Invalid buffer/metadata in channel request");
        return BAD_VALUE;
    }

    PpInfo ppInfo;
    memset(&ppInfo, 0, sizeof(ppInfo));
    ppInfo.frameNumber = frameNumber;
    ppInfo.offlinePpFlag = false;
    if (mBypass && !pInputBuffer ) {
        ppInfo.offlinePpFlag = needsFramePostprocessing(metadata);
        ppInfo.output = buffer;
        mOfflinePpInfoList.push_back(ppInfo);
    }

    LOGD("offlinePpFlag is %d", ppInfo.offlinePpFlag);
    needMetadata = ppInfo.offlinePpFlag;
    if (!ppInfo.offlinePpFlag) {
        // regular request
        return QCamera3ProcessingChannel::request(buffer, frameNumber,
                pInputBuffer, metadata, indexUsed);
    } else {

        //we need to send this frame through the CPP
        //Allocate heap memory, then buf done on the buffer
        uint32_t bufIdx;
        if (mFreeHeapBufferList.empty()) {
            rc = mMemory.allocateOne(mFrameLen);
            if (rc < 0) {
                LOGE("Failed allocating heap buffer. Fatal");
                return BAD_VALUE;
            } else {
                bufIdx = (uint32_t)rc;
            }
        } else {
            bufIdx = *(mFreeHeapBufferList.begin());
            mFreeHeapBufferList.erase(mFreeHeapBufferList.begin());
        }

        /* Configure and start postproc if necessary */
        reprocess_config_t reproc_cfg;
        cam_dimension_t dim;
        memset(&reproc_cfg, 0, sizeof(reprocess_config_t));
        memset(&dim, 0, sizeof(dim));
        mStreams[0]->getFrameDimension(dim);
        setReprocConfig(reproc_cfg, NULL, metadata, mStreamFormat, dim);

        // Start postprocessor without input buffer
        startPostProc(reproc_cfg);

        LOGD("erasing %d", bufIdx);

        mMemory.markFrameNumber(bufIdx, frameNumber);
        indexUsed = bufIdx;
        if (m_bIsActive) {
            mStreams[0]->bufDone(bufIdx);
        }

    }
    return rc;
}

/*===========================================================================
 * FUNCTION   : postprocFail
 *
 * DESCRIPTION: notify clients about failing post-process requests.
 *
 * PARAMETERS :
 * @ppBuffer  : pointer to the pp buffer.
 *
 * RETURN     : 0 on success
 *              -EINVAL on invalid input
 *==========================================================================*/
int32_t QCamera3YUVChannel::postprocFail(qcamera_hal3_pp_buffer_t *ppBuffer) {
    if (ppBuffer == nullptr) {
        return BAD_VALUE;
    }

    {
        List<PpInfo>::iterator ppInfo;

        Mutex::Autolock lock(mOfflinePpLock);
        for (ppInfo = mOfflinePpInfoList.begin();
                ppInfo != mOfflinePpInfoList.end(); ppInfo++) {
            if (ppInfo->frameNumber == ppBuffer->frameNumber) {
                break;
            }
        }

        if (ppInfo == mOfflinePpInfoList.end()) {
            LOGE("Offline reprocess info for frame number: %d not found!", ppBuffer->frameNumber);
            return BAD_VALUE;
        }

        LOGE("Failed YUV post-process on frame number: %d removing from offline queue!",
                ppBuffer->frameNumber);
        mOfflinePpInfoList.erase(ppInfo);
    }

    int32_t bufferIndex = mMemory.getHeapBufferIndex(ppBuffer->frameNumber);
    if (bufferIndex < 0) {
        LOGE("Fatal %d: no buffer index for frame number %d", bufferIndex, ppBuffer->frameNumber);
        return BAD_VALUE;
    } else {
        mMemory.markFrameNumber(bufferIndex, -1);
        mFreeHeapBufferList.push_back(bufferIndex);
    }

    return QCamera3ProcessingChannel::postprocFail(ppBuffer);
}

/*===========================================================================
 * FUNCTION   : streamCbRoutine
 *
 * DESCRIPTION:
 *
 * PARAMETERS :
 * @super_frame : the super frame with filled buffer
 * @stream      : stream on which the buffer was requested and filled
 *
 * RETURN     : none
 *==========================================================================*/
void QCamera3YUVChannel::streamCbRoutine(mm_camera_super_buf_t *super_frame,
        QCamera3Stream *stream)
{
    ATRACE_CAMSCOPE_CALL(CAMSCOPE_HAL3_YUV_CH_STRM_CB);
    uint8_t frameIndex;
    int32_t resultFrameNumber;
    bool droppedInputPPBuffer = false;

    if (checkStreamCbErrors(super_frame, stream) != NO_ERROR) {
        LOGE("Error with the stream callback");
        return;
    }

    frameIndex = (uint8_t)super_frame->bufs[0]->buf_idx;
    if(frameIndex >= mNumBufs) {
         LOGE("Error, Invalid index for buffer");
         stream->bufDone(frameIndex);
         return;
    }

    if (mBypass) {
        {
            List<PpInfo>::iterator ppInfo;

            Mutex::Autolock lock(mOfflinePpLock);
            resultFrameNumber = mMemory.getFrameNumber(frameIndex);
            for (ppInfo = mOfflinePpInfoList.begin();
                    ppInfo != mOfflinePpInfoList.end(); ppInfo++) {
                if (ppInfo->frameNumber == (uint32_t)resultFrameNumber) {
                    break;
                }
            }
            LOGD("frame index %d, frame number %d", frameIndex,
                    resultFrameNumber);
            //check the reprocessing required flag against the frame number
            if (ppInfo == mOfflinePpInfoList.end()) {
                LOGE("Error, request for frame number is a reprocess.");
                stream->bufDone(frameIndex);
                return;
            }

            if (ppInfo->offlinePpFlag) {
                if (ppInfo != mOfflinePpInfoList.begin() &&
                        IS_BUFFER_ERROR(super_frame->bufs[0]->flags)) {
                    droppedInputPPBuffer = true;
                    mOfflinePpInfoList.erase(ppInfo);
                } else {
                    mm_camera_super_buf_t *frame =
                        (mm_camera_super_buf_t *)malloc(sizeof(
                                    mm_camera_super_buf_t));
                    if (frame == NULL) {
                        LOGE("Error allocating memory to save received_frame structure.");
                        if(stream) {
                            stream->bufDone(frameIndex);
                        }
                        return;
                    }

                    *frame = *super_frame;
                    m_postprocessor.processData(frame, ppInfo->output,
                            resultFrameNumber);
                    free(super_frame);
                    return;
                }
            } else {
                if (ppInfo != mOfflinePpInfoList.begin()) {
                    // There is pending reprocess buffer, cache current buffer
                    if (ppInfo->callback_buffer != NULL) {
                        LOGE("Fatal: cached callback_buffer is already present");
                    }
                    ppInfo->callback_buffer = super_frame;
                    return;
                } else {
                    mOfflinePpInfoList.erase(ppInfo);
                }
            }
        }

        if (IS_BUFFER_ERROR(super_frame->bufs[0]->flags)) {
            mChannelCbBufErr(this, resultFrameNumber,
                            CAMERA3_BUFFER_STATUS_ERROR, mUserData);
            if (droppedInputPPBuffer) {
                camera3_stream_buffer_t result = {};
                result.buffer = (buffer_handle_t *)mMemory.getBufferHandle(frameIndex);
                int32_t bufferIndex =
                    mMemory.getHeapBufferIndex(resultFrameNumber);
                if (bufferIndex < 0) {
                    LOGE("Fatal %d: no buffer index for frame number %d",
                            bufferIndex, resultFrameNumber);
                } else {
                    mMemory.markFrameNumber(bufferIndex, -1);
                    mFreeHeapBufferList.push_back(bufferIndex);
                }

                LOGE("Input frame number: %d dropped!", resultFrameNumber);
                result.stream = mCamera3Stream;
                result.status = CAMERA3_BUFFER_STATUS_ERROR;
                result.acquire_fence = -1;
                result.release_fence = -1;
                if (mChannelCB) {
                    mChannelCB(NULL, &result, (uint32_t)resultFrameNumber, false, mUserData);
                }
                free(super_frame);

                return;
            }
        }
    }

    QCamera3ProcessingChannel::streamCbRoutine(super_frame, stream);

    /* return any pending buffers that were received out of order earlier */
    while((super_frame = getNextPendingCbBuffer())) {
        QCamera3ProcessingChannel::streamCbRoutine(super_frame, stream);
    }

    return;
}

/*===========================================================================
 * FUNCTION   : getNextPendingCbBuffer
 *
 * DESCRIPTION: Returns the callback_buffer from the first entry of
 *              mOfflinePpInfoList
 *
 * PARAMETERS : none
 *
 * RETURN     : callback_buffer
 *==========================================================================*/
mm_camera_super_buf_t* QCamera3YUVChannel::getNextPendingCbBuffer() {
    mm_camera_super_buf_t* super_frame = NULL;
    if (mOfflinePpInfoList.size()) {
        if ((super_frame = mOfflinePpInfoList.begin()->callback_buffer)) {
            mOfflinePpInfoList.erase(mOfflinePpInfoList.begin());
        }
    }
    return super_frame;
}

/*===========================================================================
 * FUNCTION   : reprocessCbRoutine
 *
 * DESCRIPTION: callback function for the reprocessed frame. This frame now
 *              should be returned to the framework. This same callback is
 *              used during input reprocessing or offline postprocessing
 *
 * PARAMETERS :
 * @resultBuffer      : buffer containing the reprocessed data
 * @resultFrameNumber : frame number on which the buffer was requested
 *
 * RETURN     : NONE
 *
 *==========================================================================*/
void QCamera3YUVChannel::reprocessCbRoutine(buffer_handle_t *resultBuffer,
        uint32_t resultFrameNumber)
{
    LOGD("E: frame number %d", resultFrameNumber);
    Vector<mm_camera_super_buf_t *> pendingCbs;

    /* release the input buffer and input metadata buffer if used */
    if (0 > mMemory.getHeapBufferIndex(resultFrameNumber)) {
        /* mOfflineMemory and mOfflineMetaMemory used only for input reprocessing */
        int32_t rc = releaseOfflineMemory(resultFrameNumber);
        if (NO_ERROR != rc) {
            LOGE("Error releasing offline memory rc = %d", rc);
        }
        /* Since reprocessing is done, send the callback to release the input buffer */
        if (mChannelCB) {
            mChannelCB(NULL, NULL, resultFrameNumber, true, mUserData);
        }
    }

    if (mBypass) {
        int32_t rc = handleOfflinePpCallback(resultFrameNumber, pendingCbs);
        if (rc != NO_ERROR) {
            return;
        }
    }

    issueChannelCb(resultBuffer, resultFrameNumber);

    // Call all pending callbacks to return buffers
    for (size_t i = 0; i < pendingCbs.size(); i++) {
        QCamera3ProcessingChannel::streamCbRoutine(
                pendingCbs[i], mStreams[0]);
    }

}

/*===========================================================================
 * FUNCTION   : needsFramePostprocessing
 *
 * DESCRIPTION:
 *
 * PARAMETERS :
 *
 * RETURN     :
 *  TRUE if frame needs to be postprocessed
 *  FALSE is frame does not need to be postprocessed
 *
 *==========================================================================*/
bool QCamera3YUVChannel::needsFramePostprocessing(metadata_buffer_t *meta)
{
    bool ppNeeded = false;

    //sharpness
    IF_META_AVAILABLE(cam_edge_application_t, edgeMode,
            CAM_INTF_META_EDGE_MODE, meta) {
        mEdgeMode = *edgeMode;
    }

    //wnr
    IF_META_AVAILABLE(uint32_t, noiseRedMode,
            CAM_INTF_META_NOISE_REDUCTION_MODE, meta) {
        mNoiseRedMode = *noiseRedMode;
    }

    //crop region
    IF_META_AVAILABLE(cam_crop_region_t, scalerCropRegion,
            CAM_INTF_META_SCALER_CROP_REGION, meta) {
        mCropRegion = *scalerCropRegion;
    }

    if ((CAM_EDGE_MODE_OFF != mEdgeMode.edge_mode) &&
            (CAM_EDGE_MODE_ZERO_SHUTTER_LAG != mEdgeMode.edge_mode)) {
        ppNeeded = true;
    }
    if ((CAM_NOISE_REDUCTION_MODE_ZERO_SHUTTER_LAG != mNoiseRedMode) &&
            (CAM_NOISE_REDUCTION_MODE_OFF != mNoiseRedMode) &&
            (CAM_NOISE_REDUCTION_MODE_MINIMAL != mNoiseRedMode)) {
        ppNeeded = true;
    }
    if ((mCropRegion.width < (int32_t)mCamera3Stream->width) ||
            (mCropRegion.height < (int32_t)mCamera3Stream->height)) {
        ppNeeded = true;
    }

    return ppNeeded;
}

/*===========================================================================
 * FUNCTION   : handleOfflinePpCallback
 *
 * DESCRIPTION: callback function for the reprocessed frame from offline
 *              postprocessing.
 *
 * PARAMETERS :
 * @resultFrameNumber : frame number on which the buffer was requested
 * @pendingCbs        : pending buffers to be returned first
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3YUVChannel::handleOfflinePpCallback(uint32_t resultFrameNumber,
            Vector<mm_camera_super_buf_t *>& pendingCbs)
{
    Mutex::Autolock lock(mOfflinePpLock);
    List<PpInfo>::iterator ppInfo;

    for (ppInfo = mOfflinePpInfoList.begin();
            ppInfo != mOfflinePpInfoList.end(); ppInfo++) {
        if (ppInfo->frameNumber == resultFrameNumber) {
            break;
        }
    }

    if (ppInfo == mOfflinePpInfoList.end()) {
        LOGI("Request of frame number %d is reprocessing",
                 resultFrameNumber);
        return NO_ERROR;
    } else if (ppInfo != mOfflinePpInfoList.begin()) {
        LOGE("callback for frame number %d should be head of list",
                 resultFrameNumber);
        return BAD_VALUE;
    }

    if (ppInfo->offlinePpFlag) {
        // Need to get the input buffer frame index from the
        // mMemory object and add that to the free heap buffers list.
        int32_t bufferIndex =
                mMemory.getHeapBufferIndex(resultFrameNumber);
        if (bufferIndex < 0) {
            LOGE("Fatal %d: no buffer index for frame number %d",
                     bufferIndex, resultFrameNumber);
            return BAD_VALUE;
        }
        mMemory.markFrameNumber(bufferIndex, -1);
        mFreeHeapBufferList.push_back(bufferIndex);
        //Move heap buffer into free pool and invalidate the frame number
        ppInfo = mOfflinePpInfoList.erase(ppInfo);

        /* return any pending buffers that were received out of order
            earlier */
        mm_camera_super_buf_t* super_frame;
        while((super_frame = getNextPendingCbBuffer())) {
            pendingCbs.push_back(super_frame);
        }
    } else {
        LOGE("Fatal: request of frame number %d doesn't need"
                " offline postprocessing. However there is"
                " reprocessing callback.",
                resultFrameNumber);
        return BAD_VALUE;
    }

    return NO_ERROR;
}

/*===========================================================================
 * FUNCTION   : getReprocessType
 *
 * DESCRIPTION: get the type of reprocess output supported by this channel
 *
 * PARAMETERS : NONE
 *
 * RETURN     : reprocess_type_t : type of reprocess
 *==========================================================================*/
reprocess_type_t QCamera3YUVChannel::getReprocessType()
{
    return REPROCESS_TYPE_YUV;
}

/* QCamera3PicChannel methods */

/*===========================================================================
 * FUNCTION   : jpegEvtHandle
 *
 * DESCRIPTION: Function registerd to mm-jpeg-interface to handle jpeg events.
                Construct result payload and call mChannelCb to deliver buffer
                to framework.
 *
 * PARAMETERS :
 *   @status    : status of jpeg job
 *   @client_hdl: jpeg client handle
 *   @jobId     : jpeg job Id
 *   @p_ouput   : ptr to jpeg output result struct
 *   @userdata  : user data ptr
 *
 * RETURN     : none
 *==========================================================================*/
void QCamera3PicChannel::jpegEvtHandle(jpeg_job_status_t status,
                                              uint32_t /*client_hdl*/,
                                              uint32_t jobId,
                                              mm_jpeg_output_t *p_output,
                                              void *userdata)
{
    ATRACE_CAMSCOPE_CALL(CAMSCOPE_HAL3_PIC_CH_JPEG_EVT_HANDLE);
    buffer_handle_t *resultBuffer = NULL;
    buffer_handle_t *jpegBufferHandle = NULL;
    int resultStatus = CAMERA3_BUFFER_STATUS_OK;
    camera3_stream_buffer_t result;
    camera3_jpeg_blob_t jpegHeader;

    QCamera3PicChannel *obj = (QCamera3PicChannel *)userdata;
    if (obj) {
        //Construct payload for process_capture_result. Call mChannelCb

        qcamera_hal3_jpeg_data_t *job = obj->m_postprocessor.findJpegJobByJobId(jobId);

        if ((job == NULL) || (status == JPEG_JOB_STATUS_ERROR)) {
            LOGE("Error in jobId: (%d) with status: %d", jobId, status);
            resultStatus = CAMERA3_BUFFER_STATUS_ERROR;
        }

        if (NULL != job) {
            uint32_t bufIdx = (uint32_t)job->jpeg_settings->out_buf_index;
            LOGD("jpeg out_buf_index: %d", bufIdx);

            //Construct jpeg transient header of type camera3_jpeg_blob_t
            //Append at the end of jpeg image of buf_filled_len size

            jpegHeader.jpeg_blob_id = CAMERA3_JPEG_BLOB_ID;
            if (JPEG_JOB_STATUS_DONE == status) {
                jpegHeader.jpeg_size = (uint32_t)p_output->buf_filled_len;
                char* jpeg_buf = (char *)p_output->buf_vaddr;
                cam_frame_len_offset_t offset;
                memset(&offset, 0, sizeof(cam_frame_len_offset_t));
                mm_camera_buf_def_t *jpeg_dump_buffer = NULL;
                cam_dimension_t dim;
                dim.width = obj->mCamera3Stream->width;
                dim.height = obj->mCamera3Stream->height;
                jpeg_dump_buffer = (mm_camera_buf_def_t *)malloc(sizeof(mm_camera_buf_def_t));
                if(!jpeg_dump_buffer) {
                    LOGE("Could not allocate jpeg dump buffer");
                } else {
                    jpeg_dump_buffer->buffer = p_output->buf_vaddr;
                    jpeg_dump_buffer->frame_len = p_output->buf_filled_len;
                    jpeg_dump_buffer->frame_idx = obj->mMemory.getFrameNumber(bufIdx);
                    obj->dumpYUV(jpeg_dump_buffer, dim, offset, QCAMERA_DUMP_FRM_OUTPUT_JPEG);
                    free(jpeg_dump_buffer);
                }

                ssize_t maxJpegSize = -1;

                // Gralloc buffer may have additional padding for 4K page size
                // Follow size guidelines based on spec since framework relies
                // on that to reach end of buffer and with it the header

                //Handle same as resultBuffer, but for readablity
                jpegBufferHandle =
                        (buffer_handle_t *)obj->mMemory.getBufferHandle(bufIdx);

                if (NULL != jpegBufferHandle) {
                    maxJpegSize = ((private_handle_t*)(*jpegBufferHandle))->width;
                    if (maxJpegSize > obj->mMemory.getSize(bufIdx)) {
                        maxJpegSize = obj->mMemory.getSize(bufIdx);
                    }

                    size_t jpeg_eof_offset =
                            (size_t)(maxJpegSize - (ssize_t)sizeof(jpegHeader));
                    char *jpeg_eof = &jpeg_buf[jpeg_eof_offset];
                    memcpy(jpeg_eof, &jpegHeader, sizeof(jpegHeader));
                    obj->mMemory.cleanInvalidateCache(bufIdx);
                } else {
                    LOGE("JPEG buffer not found and index: %d",
                            bufIdx);
                    resultStatus = CAMERA3_BUFFER_STATUS_ERROR;
                }
            }

            ////Use below data to issue framework callback
            resultBuffer =
                    (buffer_handle_t *)obj->mMemory.getBufferHandle(bufIdx);
            int32_t resultFrameNumber = obj->mMemory.getFrameNumber(bufIdx);
            int32_t rc = obj->mMemory.unregisterBuffer(bufIdx);
            if (NO_ERROR != rc) {
                LOGE("Error %d unregistering stream buffer %d",
                     rc, bufIdx);
            }

            result.stream = obj->mCamera3Stream;
            result.buffer = resultBuffer;
            result.status = resultStatus;
            result.acquire_fence = -1;
            result.release_fence = -1;

            // Release any snapshot buffers before calling
            // the user callback. The callback can potentially
            // unblock pending requests to snapshot stream.
            int32_t snapshotIdx = -1;
            mm_camera_super_buf_t* src_frame = NULL;

            if (job->src_reproc_frame)
                src_frame = job->src_reproc_frame;
            else
                src_frame = job->src_frame;

            if (src_frame) {
                if (obj->mStreams[0]->getMyHandle() ==
                        src_frame->bufs[0]->stream_id) {
                    snapshotIdx = (int32_t)src_frame->bufs[0]->buf_idx;
                    if (0 <= snapshotIdx) {
                        Mutex::Autolock lock(obj->mFreeBuffersLock);
                        obj->mFreeBufferList.push_back((uint32_t)snapshotIdx);
                    }
                }
            }

            LOGI("Issue Jpeg Callback frameNumber = %d status = %d",
                    resultFrameNumber, resultStatus);
            ATRACE_ASYNC_END("SNAPSHOT", resultFrameNumber);
            if (obj->mChannelCB) {
                obj->mChannelCB(NULL,
                        &result,
                        (uint32_t)resultFrameNumber,
                        false,
                        obj->mUserData);
            }

            // release internal data for jpeg job
            if ((NULL != job->fwk_frame) || (NULL != job->fwk_src_buffer)) {
                /* unregister offline input buffer */
                int32_t inputBufIndex =
                        obj->mOfflineMemory.getGrallocBufferIndex((uint32_t)resultFrameNumber);
                if (0 <= inputBufIndex) {
                    rc = obj->mOfflineMemory.unregisterBuffer(inputBufIndex);
                } else {
                    LOGE("could not find the input buf index, frame number %d",
                             resultFrameNumber);
                }
                if (NO_ERROR != rc) {
                    LOGE("Error %d unregistering input buffer %d",
                             rc, bufIdx);
                }

                /* unregister offline meta buffer */
                int32_t metaBufIndex =
                        obj->mOfflineMetaMemory.getHeapBufferIndex((uint32_t)resultFrameNumber);
                if (0 <= metaBufIndex) {
                    Mutex::Autolock lock(obj->mFreeOfflineMetaBuffersLock);
                    obj->mFreeOfflineMetaBuffersList.push_back((uint32_t)metaBufIndex);
                } else {
                    LOGE("could not find the input meta buf index, frame number %d",
                             resultFrameNumber);
                }
            }
            obj->m_postprocessor.releaseOfflineBuffers(false);
            obj->m_postprocessor.releaseJpegJobData(job);
            free(job);
        }

        return;
        // }
    } else {
        LOGE("Null userdata in jpeg callback");
    }
}

QCamera3PicChannel::QCamera3PicChannel(uint32_t cam_handle,
                    uint32_t channel_handle,
                    mm_camera_ops_t *cam_ops,
                    channel_cb_routine cb_routine,
                    channel_cb_buffer_err cb_buf_err,
                    cam_padding_info_t *paddingInfo,
                    void *userData,
                    camera3_stream_t *stream,
                    cam_feature_mask_t postprocess_mask,
                    __unused bool is4KVideo,
                    bool isInputStreamConfigured,
                    QCamera3Channel *metadataChannel,
                    uint32_t numBuffers) :
                        QCamera3ProcessingChannel(cam_handle, channel_handle,
                                cam_ops, cb_routine, cb_buf_err, paddingInfo, userData,
                                stream, CAM_STREAM_TYPE_SNAPSHOT,
                                postprocess_mask, metadataChannel, numBuffers),
                        mNumSnapshotBufs(0),
                        mInputBufferHint(isInputStreamConfigured),
                        mYuvMemory(NULL),
                        mFrameLen(0)
{
    QCamera3HardwareInterface* hal_obj = (QCamera3HardwareInterface*)mUserData;
    m_max_pic_dim = hal_obj->calcMaxJpegDim();
    mYuvWidth = stream->width;
    mYuvHeight = stream->height;
    mStreamType = CAM_STREAM_TYPE_SNAPSHOT;
    // Use same pixelformat for 4K video case
    mStreamFormat = getStreamDefaultFormat(CAM_STREAM_TYPE_SNAPSHOT,
            stream->width, stream->height, m_bUBWCenable, IS_TYPE_NONE);
    int32_t rc = m_postprocessor.initJpeg(jpegEvtHandle, &m_max_pic_dim, this);
    if (rc != 0) {
        LOGE("Init Postprocessor failed");
    }
}

/*===========================================================================
 * FUNCTION   : flush
 *
 * DESCRIPTION: flush pic channel, which will stop all processing within, including
 *              the reprocessing channel in postprocessor and YUV stream.
 *
 * PARAMETERS : none
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3PicChannel::flush()
{
    int32_t rc = NO_ERROR;
    if(!m_bIsActive) {
        LOGE("Attempt to flush inactive channel");
        return NO_INIT;
    }

    rc = m_postprocessor.flush();
    if (rc == 0) {
        LOGE("Postprocessor flush failed, rc = %d", rc);
        return rc;
    }

    if (0 < mOfflineMetaMemory.getCnt()) {
        mOfflineMetaMemory.deallocate();
    }
    if (0 < mOfflineMemory.getCnt()) {
        mOfflineMemory.unregisterBuffers();
    }
    Mutex::Autolock lock(mFreeBuffersLock);
    mFreeBufferList.clear();

    for (uint32_t i = 0; i < mCamera3Stream->max_buffers; i++) {
        mFreeBufferList.push_back(i);
    }
    return rc;
}


QCamera3PicChannel::~QCamera3PicChannel()
{
    if (mYuvMemory != nullptr) {
        mYuvMemory->deallocate();
        delete mYuvMemory;
        mYuvMemory = nullptr;
    }
}

/*===========================================================================
 * FUNCTION : metadataBufDone
 *
 * DESCRIPTION: Buffer done method for a metadata buffer
 *
 * PARAMETERS :
 * @recvd_frame : received metadata frame
 *
 * RETURN     : int32_t type of status
 *              OK  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3PicChannel::metadataBufDone(mm_camera_super_buf_t *recvd_frame)
{
    // Check if this is an external metadata
    if (recvd_frame != nullptr && recvd_frame->num_bufs == 1) {
        Mutex::Autolock lock(mPendingExternalMetadataLock);
        auto iter = mPendingExternalMetadata.begin();
        while (iter != mPendingExternalMetadata.end()) {
            if (iter->get() == recvd_frame->bufs[0]->buffer) {
                // Remove the metadata allocated externally.
                mPendingExternalMetadata.erase(iter);
                return OK;
            }

            iter++;
        }
    }

    // If this is not an external metadata, return the metadata.
    return QCamera3ProcessingChannel::metadataBufDone(recvd_frame);
}

int32_t QCamera3PicChannel::initialize(cam_is_type_t isType)
{
    int32_t rc = NO_ERROR;
    cam_dimension_t streamDim;
    cam_stream_type_t streamType;
    cam_format_t streamFormat;

    if (NULL == mCamera3Stream) {
        LOGE("Camera stream uninitialized");
        return NO_INIT;
    }

    if (1 <= m_numStreams) {
        // Only one stream per channel supported in v3 Hal
        return NO_ERROR;
    }

    mIsType = isType;
    streamType = mStreamType;
    streamFormat = mStreamFormat;
    streamDim.width = (int32_t)mYuvWidth;
    streamDim.height = (int32_t)mYuvHeight;

    mNumSnapshotBufs = mCamera3Stream->max_buffers;
    rc = QCamera3Channel::addStream(streamType, streamFormat, streamDim,
            ROTATE_0, (uint8_t)mCamera3Stream->max_buffers, mPostProcMask,
            mIsType);

    if (NO_ERROR != rc) {
        LOGE("Initialize failed, rc = %d", rc);
        return rc;
    }

    mYuvMemory = new QCamera3StreamMem(mCamera3Stream->max_buffers);
    if (!mYuvMemory) {
        LOGE("unable to create YUV buffers");
        return NO_MEMORY;
    }
    cam_stream_buf_plane_info_t buf_planes;
    cam_padding_info_t paddingInfo = mPaddingInfo;

    memset(&buf_planes, 0, sizeof(buf_planes));
    //to ensure a big enough buffer size set the height and width
    //padding to max(height padding, width padding)
    paddingInfo.width_padding = MAX(paddingInfo.width_padding, paddingInfo.height_padding);
    paddingInfo.height_padding = paddingInfo.width_padding;

    rc = mm_stream_calc_offset_snapshot(mStreamFormat, &streamDim, &paddingInfo,
            &buf_planes);
    if (rc < 0) {
        LOGE("mm_stream_calc_offset_preview failed");
        return rc;
    }
    mFrameLen = buf_planes.plane_info.frame_len;

    /* initialize offline meta memory for input reprocess */
    rc = QCamera3ProcessingChannel::initialize(isType);
    if (NO_ERROR != rc) {
        LOGE("Processing Channel initialize failed, rc = %d",
                 rc);
    }

    return rc;
}

/*===========================================================================
 * FUNCTION   : request
 *
 * DESCRIPTION: handle the request - either with an input buffer or a direct
 *              output request
 *
 * PARAMETERS :
 * @buffer       : pointer to the output buffer
 * @frameNumber  : frame number of the request
 * @pInputBuffer : pointer to input buffer if an input request
 * @metadata     : parameters associated with the request
 * @internalreq      : boolean to indicate if this is purely internal request
 *                    needing internal buffer allocation
 * @meteringonly    : boolean indicating metering only frame subset of internal
 *                    not consumed by postprocessor
 *
 * RETURN     : 0 on a success start of capture
 *              -EINVAL on invalid input
 *              -ENODEV on serious error
 *==========================================================================*/
int32_t QCamera3PicChannel::request(buffer_handle_t *buffer,
        uint32_t frameNumber,
        camera3_stream_buffer_t *pInputBuffer,
        metadata_buffer_t *metadata, int &indexUsed,
        bool internalRequest, bool meteringOnly)
{
    ATRACE_CAMSCOPE_CALL(CAMSCOPE_HAL3_PIC_CH_REQ);
    //FIX ME: Return buffer back in case of failures below.

    int32_t rc = NO_ERROR;

    reprocess_config_t reproc_cfg;
    cam_dimension_t dim;
    memset(&reproc_cfg, 0, sizeof(reprocess_config_t));
    //make sure to set the correct input stream dim in case of YUV size override
    //and recalculate the plane info
    dim.width = (int32_t)mYuvWidth;
    dim.height = (int32_t)mYuvHeight;

    setReprocConfig(reproc_cfg, pInputBuffer, metadata, mStreamFormat, dim);

    // Start postprocessor
    startPostProc(reproc_cfg);

    if (!internalRequest) {
        int index = mMemory.getMatchBufIndex((void*)buffer);

        if(index < 0) {
            rc = registerBuffer(buffer, mIsType);
            if (NO_ERROR != rc) {
                LOGE("On-the-fly buffer registration failed %d",
                         rc);
                return rc;
            }

            index = mMemory.getMatchBufIndex((void*)buffer);
            if (index < 0) {
                LOGE("Could not find object among registered buffers");
                return DEAD_OBJECT;
            }
        }
        LOGD("buffer index %d, frameNumber: %u", index, frameNumber);

        rc = mMemory.markFrameNumber((uint32_t)index, frameNumber);

        // Queue jpeg settings
        rc = queueJpegSetting((uint32_t)index, metadata);

    } else {
        LOGD("Internal request @ Picchannel");
    }


    if (pInputBuffer == NULL) {
        Mutex::Autolock lock(mFreeBuffersLock);
        uint32_t bufIdx;
        if (mFreeBufferList.empty()) {
            rc = mYuvMemory->allocateOne(mFrameLen, /*isCached*/false);
            if (rc < 0) {
                LOGE("Failed to allocate heap buffer. Fatal");
                return rc;
            } else {
                bufIdx = (uint32_t)rc;
            }
        } else {
            List<uint32_t>::iterator it = mFreeBufferList.begin();
            bufIdx = *it;
            mFreeBufferList.erase(it);
        }
        if (meteringOnly) {
            mYuvMemory->markFrameNumber(bufIdx, 0xFFFFFFFF);
        } else {
            mYuvMemory->markFrameNumber(bufIdx, frameNumber);
        }
        if (m_bIsActive) {
            mStreams[0]->bufDone(bufIdx);
        }
        indexUsed = bufIdx;
    } else {
        qcamera_fwk_input_pp_data_t *src_frame = NULL;
        src_frame = (qcamera_fwk_input_pp_data_t *)calloc(1,
                sizeof(qcamera_fwk_input_pp_data_t));
        if (src_frame == NULL) {
            LOGE("No memory for src frame");
            return NO_MEMORY;
        }
        rc = setFwkInputPPData(src_frame, pInputBuffer, &reproc_cfg, metadata,
                NULL /*fwk output buffer*/, frameNumber);
        if (NO_ERROR != rc) {
            LOGE("Error %d while setting framework input PP data", rc);
            free(src_frame);
            return rc;
        }
        LOGH("Post-process started");
        m_postprocessor.processData(src_frame);
    }
    return rc;
}



/*===========================================================================
 * FUNCTION   : dataNotifyCB
 *
 * DESCRIPTION: Channel Level callback used for super buffer data notify.
 *              This function is registered with mm-camera-interface to handle
 *              data notify
 *
 * PARAMETERS :
 *   @recvd_frame   : stream frame received
 *   userdata       : user data ptr
 *
 * RETURN     : none
 *==========================================================================*/
void QCamera3PicChannel::dataNotifyCB(mm_camera_super_buf_t *recvd_frame,
                                 void *userdata)
{
    ATRACE_CAMSCOPE_CALL(CAMSCOPE_HAL3_PIC_CH_DATA_NOTIFY_CB);
    LOGD("E\n");
    QCamera3PicChannel *channel = (QCamera3PicChannel *)userdata;

    if (channel == NULL) {
        LOGE("invalid channel pointer");
        return;
    }

    if(channel->m_numStreams != 1) {
        LOGE("Error: Bug: This callback assumes one stream per channel");
        return;
    }


    if(channel->mStreams[0] == NULL) {
        LOGE("Error: Invalid Stream object");
        return;
    }

    channel->QCamera3PicChannel::streamCbRoutine(recvd_frame, channel->mStreams[0]);

    LOGD("X\n");
    return;
}

/*===========================================================================
 * FUNCTION   : streamCbRoutine
 *
 * DESCRIPTION:
 *
 * PARAMETERS :
 * @super_frame : the super frame with filled buffer
 * @stream      : stream on which the buffer was requested and filled
 *
 * RETURN     : none
 *==========================================================================*/
void QCamera3PicChannel::streamCbRoutine(mm_camera_super_buf_t *super_frame,
                            QCamera3Stream *stream)
{
    KPI_ATRACE_CAMSCOPE_CALL(CAMSCOPE_HAL3_CAPTURE_CH_CB);
    //TODO
    //Used only for getting YUV. Jpeg callback will be sent back from channel
    //directly to HWI. Refer to func jpegEvtHandle

    //Got the yuv callback. Calling yuv callback handler in PostProc
    uint8_t frameIndex;
    mm_camera_super_buf_t* frame = NULL;
    cam_dimension_t dim;
    cam_frame_len_offset_t offset;

    memset(&dim, 0, sizeof(dim));
    memset(&offset, 0, sizeof(cam_frame_len_offset_t));

    if (checkStreamCbErrors(super_frame, stream) != NO_ERROR) {
        LOGE("Error with the stream callback");
        return;
    }

    frameIndex = (uint8_t)super_frame->bufs[0]->buf_idx;
    LOGD("recvd buf_idx: %u for further processing",
         (uint32_t)frameIndex);
    if(frameIndex >= mNumSnapshotBufs) {
         LOGE("Error, Invalid index for buffer");
         if(stream) {
             Mutex::Autolock lock(mFreeBuffersLock);
             mFreeBufferList.push_back(frameIndex);
             stream->bufDone(frameIndex);
         }
         return;
    }

    if ((uint32_t)mYuvMemory->getFrameNumber(frameIndex) == EMPTY_FRAMEWORK_FRAME_NUMBER) {
        LOGD("Internal Request recycle frame");
        Mutex::Autolock lock(mFreeBuffersLock);
        mFreeBufferList.push_back(frameIndex);
        return;
    }

    frame = (mm_camera_super_buf_t *)malloc(sizeof(mm_camera_super_buf_t));
    if (frame == NULL) {
       LOGE("Error allocating memory to save received_frame structure.");
       if(stream) {
           Mutex::Autolock lock(mFreeBuffersLock);
           mFreeBufferList.push_back(frameIndex);
           stream->bufDone(frameIndex);
       }
       return;
    }
    *frame = *super_frame;
    stream->getFrameDimension(dim);
    stream->getFrameOffset(offset);
    dumpYUV(frame->bufs[0], dim, offset, QCAMERA_DUMP_FRM_INPUT_REPROCESS);

    if (IS_BUFFER_ERROR(super_frame->bufs[0]->flags)) {
        mChannelCbBufErr(this, mYuvMemory->getFrameNumber(frameIndex),
                CAMERA3_BUFFER_STATUS_ERROR, mUserData);
    }

    m_postprocessor.processData(frame);
    free(super_frame);
    return;
}

QCamera3StreamMem* QCamera3PicChannel::getStreamBufs(uint32_t /*len*/)
{
    return mYuvMemory;
}

void QCamera3PicChannel::putStreamBufs()
{
    QCamera3ProcessingChannel::putStreamBufs();
    Mutex::Autolock lock(mFreeBuffersLock);
    mFreeBufferList.clear();

    if (nullptr != mYuvMemory) {
        uint32_t count = mYuvMemory->getCnt();
        for (uint32_t i = 0; i < count; i++) {
            mFreeBufferList.push_back(i);
        }
    }
}

int32_t QCamera3PicChannel::queueJpegSetting(uint32_t index, metadata_buffer_t *metadata)
{
    QCamera3HardwareInterface* hal_obj = (QCamera3HardwareInterface*)mUserData;
    jpeg_settings_t *settings =
            (jpeg_settings_t *)malloc(sizeof(jpeg_settings_t));

    if (!settings) {
        LOGE("out of memory allocating jpeg_settings");
        return -ENOMEM;
    }

    memset(settings, 0, sizeof(jpeg_settings_t));

    settings->out_buf_index = index;

    settings->jpeg_orientation = 0;
    IF_META_AVAILABLE(int32_t, orientation, CAM_INTF_META_JPEG_ORIENTATION, metadata) {
        settings->jpeg_orientation = *orientation;
    }

    settings->jpeg_quality = 85;
    IF_META_AVAILABLE(uint32_t, quality1, CAM_INTF_META_JPEG_QUALITY, metadata) {
        settings->jpeg_quality = (uint8_t) *quality1;
    }

    IF_META_AVAILABLE(uint32_t, quality2, CAM_INTF_META_JPEG_THUMB_QUALITY, metadata) {
        settings->jpeg_thumb_quality = (uint8_t) *quality2;
    }

    IF_META_AVAILABLE(cam_dimension_t, dimension, CAM_INTF_META_JPEG_THUMB_SIZE, metadata) {
        settings->thumbnail_size = *dimension;
    }

    settings->gps_timestamp_valid = 0;
    IF_META_AVAILABLE(int64_t, timestamp, CAM_INTF_META_JPEG_GPS_TIMESTAMP, metadata) {
        settings->gps_timestamp = *timestamp;
        settings->gps_timestamp_valid = 1;
    }

    settings->gps_coordinates_valid = 0;
    IF_META_AVAILABLE(double, coordinates, CAM_INTF_META_JPEG_GPS_COORDINATES, metadata) {
        memcpy(settings->gps_coordinates, coordinates, 3*sizeof(double));
        settings->gps_coordinates_valid = 1;
    }

    IF_META_AVAILABLE(uint8_t, proc_methods, CAM_INTF_META_JPEG_GPS_PROC_METHODS, metadata) {
        memset(settings->gps_processing_method, 0,
                sizeof(settings->gps_processing_method));
        strlcpy(settings->gps_processing_method, (const char *)proc_methods,
                sizeof(settings->gps_processing_method));
    }

    settings->hdr_snapshot = 0;
    IF_META_AVAILABLE(cam_hdr_param_t, hdr_info, CAM_INTF_PARM_HAL_BRACKETING_HDR, metadata) {
        if (hdr_info->hdr_enable) {
            settings->hdr_snapshot = 1;
        }
    }


    // Image description
    const char *eepromVersion = hal_obj->getEepromVersionInfo();
    const uint32_t *ldafCalib = hal_obj->getLdafCalib();
    const char *easelFwVersion = hal_obj->getEaselFwVersion();
    if ((eepromVersion && strlen(eepromVersion)) ||
            ldafCalib || easelFwVersion) {
        uint32_t len = 0;
        settings->image_desc_valid = true;
        if (eepromVersion && strlen(eepromVersion)) {
            len = snprintf(settings->image_desc, sizeof(settings->image_desc),
                    "%s", eepromVersion);
        }
        if (ldafCalib) {
            len += snprintf(settings->image_desc + len,
                    sizeof(settings->image_desc) - len, "L:%u-%u",
                    ldafCalib[0], ldafCalib[1]);
        }
        if (easelFwVersion) {
            ALOGD("%s: Easel FW version %s", __FUNCTION__, easelFwVersion);
            if (len > 0 && len < sizeof(settings->image_desc)) {
                settings->image_desc[len] = ',';
                len++;
            }
            len += snprintf(settings->image_desc + len,
                            sizeof(settings->image_desc) - len, "E-ver:%s", easelFwVersion);
        }
    }

    return m_postprocessor.processJpegSettingData(settings);
}


void QCamera3PicChannel::overrideYuvSize(uint32_t width, uint32_t height)
{
   mYuvWidth = width;
   mYuvHeight = height;
}

/*===========================================================================
 * FUNCTION   : getReprocessType
 *
 * DESCRIPTION: get the type of reprocess output supported by this channel
 *
 * PARAMETERS : NONE
 *
 * RETURN     : reprocess_type_t : type of reprocess
 *==========================================================================*/
reprocess_type_t QCamera3PicChannel::getReprocessType()
{
    /* a picture channel could either use the postprocessor for reprocess+jpeg
       or only for reprocess */
    reprocess_type_t expectedReprocess;
    if (mPostProcMask == CAM_QCOM_FEATURE_NONE || mInputBufferHint) {
        expectedReprocess = REPROCESS_TYPE_JPEG;
    } else {
        expectedReprocess = REPROCESS_TYPE_NONE;
    }
    LOGH("expectedReprocess from Pic Channel is %d", expectedReprocess);
    return expectedReprocess;
}


/*===========================================================================
 * FUNCTION   : timeoutFrame
 *
 * DESCRIPTION: Method to indicate to channel that a given frame has take too
 *              long to be generated
 *
 * PARAMETERS : framenumber indicating the framenumber of the buffer timingout
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3PicChannel::timeoutFrame(uint32_t frameNumber)
{
    int32_t bufIdx;

    bufIdx = mYuvMemory->getBufferIndex(frameNumber);

    if (bufIdx < 0) {
        LOGE("%s: Buffer not found for frame:%d", __func__, frameNumber);
        return -1;
    }

    mStreams[0]->timeoutFrame(bufIdx);

    return NO_ERROR;
}

int32_t QCamera3PicChannel::getYuvBufferForRequest(mm_camera_buf_def_t *frame,
        uint32_t frameNumber)
{
    uint32_t bufIdx;
    status_t rc;

    Mutex::Autolock lock(mFreeBuffersLock);

    // Get an available YUV buffer.
    if (mFreeBufferList.empty()) {
        // Allocate a buffer if no one is available.
        rc = mYuvMemory->allocateOne(mFrameLen, /*isCached*/false);
        if (rc < 0) {
            LOGE("Failed to allocate heap buffer. Fatal");
            return rc;
        } else {
            bufIdx = (uint32_t)rc;
        }
    } else {
        List<uint32_t>::iterator it = mFreeBufferList.begin();
        bufIdx = *it;
        mFreeBufferList.erase(it);
    }

    mYuvMemory->markFrameNumber(bufIdx, frameNumber);

    cam_frame_len_offset_t offset = {};
    mStreams[0]->getFrameOffset(offset);

    // Get a buffer from YUV memory.
    rc = mYuvMemory->getBufDef(offset, *frame, bufIdx, mMapStreamBuffers);
    if (rc != 0) {
        ALOGE("%s: Getting a frame failed: %s (%d).", __FUNCTION__, strerror(-rc), rc);
        return rc;
    }

    // Set the frame's stream ID because it's not set in getBufDef.
    frame->stream_id = mStreams[0]->getMyHandle();
    return 0;
}

int32_t QCamera3PicChannel::returnYuvBuffer(mm_camera_buf_def_t *frame)
{
    Mutex::Autolock lock(mFreeBuffersLock);
    mFreeBufferList.push_back(frame->buf_idx);
    return 0;
}

int32_t QCamera3PicChannel::returnYuvBufferAndEncode(mm_camera_buf_def_t *frame,
        buffer_handle_t *outBuffer, uint32_t frameNumber,
        std::shared_ptr<metadata_buffer_t> metadata, mm_camera_buf_def_t *metaFrame)
{
    int32_t rc = OK;

    // Picture stream must have been started before any request comes in.
    if (!m_bIsActive) {
        LOGE("Channel not started!!");
        return NO_INIT;
    }

    // Set up reprocess configuration
    reprocess_config_t reproc_cfg = {};
    cam_dimension_t dim;
    dim.width = (int32_t)mYuvWidth;
    dim.height = (int32_t)mYuvHeight;
    setReprocConfig(reproc_cfg, nullptr, metadata.get(), mStreamFormat, dim);

    // Override reprocess type to just JPEG encoding without reprocessing.
    reproc_cfg.reprocess_type = REPROCESS_TYPE_NONE;

    // Get the index of the output jpeg buffer.
    int index = mMemory.getMatchBufIndex((void*)outBuffer);
    if(index < 0) {
        rc = registerBuffer(outBuffer, mIsType);
        if (OK != rc) {
            LOGE("On-the-fly buffer registration failed %d",
                     rc);
            return rc;
        }

        index = mMemory.getMatchBufIndex((void*)outBuffer);
        if (index < 0) {
            LOGE("Could not find object among registered buffers");
            return DEAD_OBJECT;
        }
    }

    rc = mMemory.markFrameNumber((uint32_t)index, frameNumber);
    if (rc != OK) {
        ALOGE("%s: Marking frame number (%u) for jpeg buffer (%d) failed: %s (%d)", __FUNCTION__,
                frameNumber, index, strerror(-rc), rc);
        return rc;
    }

    // Start postprocessor
    startPostProc(reproc_cfg);

    // Queue jpeg settings
    rc = queueJpegSetting((uint32_t)index, metadata.get());
    if (rc != OK) {
        ALOGE("%s: Queueing Jpeg setting for frame number (%u) buffer index (%d) failed: %s (%d)",
                __FUNCTION__, frameNumber, index, strerror(-rc), rc);
        return rc;
    }

    // Allocate a buffer for the YUV input. It will be freed in QCamera3PostProc.
    mm_camera_super_buf_t *src_frame =
            (mm_camera_super_buf_t *)calloc(1, sizeof(mm_camera_super_buf_t));
    if (src_frame == nullptr) {
        LOGE("%s: No memory for src frame", __FUNCTION__);
        return NO_MEMORY;
    }
    src_frame->camera_handle = m_camHandle;
    src_frame->ch_id = getMyHandle();
    src_frame->num_bufs = 1;
    src_frame->bufs[0] = frame;

    // Start processing the YUV buffer.
    ALOGD("%s: %d: Post-process started", __FUNCTION__, __LINE__);
    rc = m_postprocessor.processData(src_frame);
    if (rc != OK) {
        ALOGE("%s: Post processing frame (frame number: %u, jpeg buffer: %d) failed: %s (%d)",
            __FUNCTION__, frameNumber, index, strerror(-rc), rc);
        return rc;
    }

    // Allocate a buffer for the metadata. It will be freed in QCamera3PostProc.
    mm_camera_super_buf_t *metadataBuf =
            (mm_camera_super_buf_t *)calloc(1, sizeof(mm_camera_super_buf_t));
    if (metadata == nullptr) {
        LOGE("%s: No memory for metadata", __FUNCTION__);
        return NO_MEMORY;
    }
    metadataBuf->camera_handle = m_camHandle;
    metadataBuf->ch_id = getMyHandle();
    metadataBuf->num_bufs = 1;
    metadataBuf->bufs[0] = metaFrame;
    metadataBuf->bufs[0]->buffer = metadata.get();

    // Start processing the metadata
    rc = m_postprocessor.processPPMetadata(metadataBuf);
    if (rc != OK) {
        ALOGE("%s: Post processing metadata (frame number: %u, jpeg buffer: %d) failed: %s (%d)",
                __FUNCTION__, frameNumber, index, strerror(-rc), rc);
        return rc;
    }

    // Queue the external metadata.
    {
        Mutex::Autolock lock(mPendingExternalMetadataLock);
        mPendingExternalMetadata.push_back(metadata);
    }

    return OK;
}

/*===========================================================================
 * FUNCTION   : QCamera3ReprocessChannel
 *
 * DESCRIPTION: constructor of QCamera3ReprocessChannel
 *
 * PARAMETERS :
 *   @cam_handle : camera handle
 *   @cam_ops    : ptr to camera ops table
 *   @pp_mask    : post-proccess feature mask
 *
 * RETURN     : none
 *==========================================================================*/
QCamera3ReprocessChannel::QCamera3ReprocessChannel(uint32_t cam_handle,
                                                 uint32_t channel_handle,
                                                 mm_camera_ops_t *cam_ops,
                                                 channel_cb_routine cb_routine,
                                                 channel_cb_buffer_err cb_buf_err,
                                                 cam_padding_info_t *paddingInfo,
                                                 cam_feature_mask_t postprocess_mask,
                                                 void *userData, void *ch_hdl) :
    /* In case of framework reprocessing, pproc and jpeg operations could be
     * parallelized by allowing 1 extra buffer for reprocessing output:
     * ch_hdl->getNumBuffers() + 1 */
    QCamera3Channel(cam_handle, channel_handle, cam_ops, cb_routine, cb_buf_err, paddingInfo,
                    postprocess_mask, userData,
                    ((QCamera3ProcessingChannel *)ch_hdl)->getNumBuffers()
                              + (MAX_REPROCESS_PIPELINE_STAGES - 1)),
    inputChHandle(ch_hdl),
    mOfflineBuffersIndex(-1),
    mFrameLen(0),
    mReprocessType(REPROCESS_TYPE_NONE),
    m_pSrcChannel(NULL),
    m_pMetaChannel(NULL),
    mMemory(NULL),
    mGrallocMemory(0),
    mReprocessPerfMode(false)
{
    memset(mSrcStreamHandles, 0, sizeof(mSrcStreamHandles));
    mOfflineBuffersIndex = mNumBuffers -1;
    mOfflineMetaIndex = (int32_t) (2*mNumBuffers -1);
}


/*===========================================================================
 * FUNCTION   : QCamera3ReprocessChannel
 *
 * DESCRIPTION: constructor of QCamera3ReprocessChannel
 *
 * PARAMETERS :
 *   @cam_handle : camera handle
 *   @cam_ops    : ptr to camera ops table
 *   @pp_mask    : post-proccess feature mask
 *
 * RETURN     : none
 *==========================================================================*/
int32_t QCamera3ReprocessChannel::initialize(cam_is_type_t isType)
{
    int32_t rc = NO_ERROR;
    mm_camera_channel_attr_t attr;

    memset(&attr, 0, sizeof(mm_camera_channel_attr_t));
    attr.notify_mode = MM_CAMERA_SUPER_BUF_NOTIFY_CONTINUOUS;
    attr.max_unmatched_frames = 1;

    m_handle = m_camOps->add_channel(m_camHandle,
                                      &attr,
                                      NULL,
                                      this);
    if (m_handle == 0) {
        LOGE("Add channel failed");
        return UNKNOWN_ERROR;
    }

    mIsType = isType;
    return rc;
}

/*===========================================================================
 * FUNCTION   : registerBuffer
 *
 * DESCRIPTION: register streaming buffer to the channel object
 *
 * PARAMETERS :
 *   @buffer     : buffer to be registered
 *   @isType     : the image stabilization type for the buffer
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3ReprocessChannel::registerBuffer(buffer_handle_t *buffer,
        cam_is_type_t isType)
{
    ATRACE_CAMSCOPE_CALL(CAMSCOPE_HAL3_REPROC_CH_REG_BUF);
    int rc = 0;
    mIsType = isType;
    cam_stream_type_t streamType;

    if (buffer == NULL) {
        LOGE("Error: Cannot register a NULL buffer");
        return BAD_VALUE;
    }

    if ((uint32_t)mGrallocMemory.getCnt() > (mNumBuffers - 1)) {
        LOGE("Trying to register more buffers than initially requested");
        return BAD_VALUE;
    }

    if (0 == m_numStreams) {
        rc = initialize(mIsType);
        if (rc != NO_ERROR) {
            LOGE("Couldn't initialize camera stream %d",
                     rc);
            return rc;
        }
    }

    streamType = mStreams[0]->getMyType();
    rc = mGrallocMemory.registerBuffer(buffer, streamType);
    if (ALREADY_EXISTS == rc) {
        return NO_ERROR;
    } else if (NO_ERROR != rc) {
        LOGE("Buffer %p couldn't be registered %d", buffer, rc);
        return rc;
    }

    return rc;
}

/*===========================================================================
 * FUNCTION   : QCamera3ReprocessChannel
 *
 * DESCRIPTION: constructor of QCamera3ReprocessChannel
 *
 * PARAMETERS :
 *   @cam_handle : camera handle
 *   @cam_ops    : ptr to camera ops table
 *   @pp_mask    : post-proccess feature mask
 *
 * RETURN     : none
 *==========================================================================*/
void QCamera3ReprocessChannel::streamCbRoutine(mm_camera_super_buf_t *super_frame,
                                  QCamera3Stream *stream)
{
    //Got the pproc data callback. Now send to jpeg encoding
    uint8_t frameIndex;
    uint32_t resultFrameNumber;
    ATRACE_CALL();
    mm_camera_super_buf_t* frame = NULL;
    QCamera3ProcessingChannel *obj = (QCamera3ProcessingChannel *)inputChHandle;
    cam_dimension_t dim;
    cam_frame_len_offset_t offset;

    memset(&dim, 0, sizeof(dim));
    memset(&offset, 0, sizeof(cam_frame_len_offset_t));
    if(!super_frame) {
         LOGE("Invalid Super buffer");
         return;
    }

    if(super_frame->num_bufs != 1) {
         LOGE("Multiple streams are not supported");
         return;
    }
    if(super_frame->bufs[0] == NULL ) {
         LOGE("Error, Super buffer frame does not contain valid buffer");
         return;
    }
    frameIndex = (uint8_t)super_frame->bufs[0]->buf_idx;


    if (mReprocessType == REPROCESS_TYPE_JPEG) {
        resultFrameNumber =  mMemory->getFrameNumber(frameIndex);
        frame = (mm_camera_super_buf_t *)malloc(sizeof(mm_camera_super_buf_t));
        if (frame == NULL) {
           LOGE("Error allocating memory to save received_frame structure.");
           if(stream) {
               stream->bufDone(frameIndex);
           }
           return;
        }
        LOGI("bufIndex: %u recvd from post proc",
                 (uint32_t)frameIndex);
        *frame = *super_frame;

        stream->getFrameDimension(dim);
        stream->getFrameOffset(offset);
        dumpYUV(frame->bufs[0], dim, offset, QCAMERA_DUMP_FRM_INPUT_JPEG);
        // Release offline buffers.
        int32_t rc = obj->releaseOfflineMemory(resultFrameNumber);
        if (NO_ERROR != rc) {
            LOGE("Error releasing offline memory %d", rc);
        }
        /* Since reprocessing is done, send the callback to release the input buffer */
        if (mChannelCB) {
            mChannelCB(NULL, NULL, resultFrameNumber, true, mUserData);
        }
        obj->m_postprocessor.processPPData(frame);
    } else {
        buffer_handle_t *resultBuffer;
        frameIndex = (uint8_t)super_frame->bufs[0]->buf_idx;
        resultBuffer = (buffer_handle_t *)mGrallocMemory.getBufferHandle(frameIndex);
        resultFrameNumber = mGrallocMemory.getFrameNumber(frameIndex);
        int32_t rc = stream->bufRelease(frameIndex);
        if (NO_ERROR != rc) {
            LOGE("Error %d releasing stream buffer %d",
                     rc, frameIndex);
        }
        rc = mGrallocMemory.unregisterBuffer(frameIndex);
        if (NO_ERROR != rc) {
            LOGE("Error %d unregistering stream buffer %d",
                     rc, frameIndex);
        }
        obj->reprocessCbRoutine(resultBuffer, resultFrameNumber);

        obj->m_postprocessor.releaseOfflineBuffers(false);
        qcamera_hal3_pp_data_t *pp_job = obj->m_postprocessor.dequeuePPJob(resultFrameNumber);
        if (pp_job != NULL) {
            obj->m_postprocessor.releasePPJobData(pp_job);
        }
        free(pp_job);
        resetToCamPerfNormal(resultFrameNumber);
    }
    free(super_frame);
    return;
}

/*===========================================================================
 * FUNCTION   : resetToCamPerfNormal
 *
 * DESCRIPTION: Set the perf mode to normal if all the priority frames
 *              have been reprocessed
 *
 * PARAMETERS :
 *      @frameNumber: Frame number of the reprocess completed frame
 *
 * RETURN     : QCamera3StreamMem *
 *==========================================================================*/
int32_t QCamera3ReprocessChannel::resetToCamPerfNormal(uint32_t frameNumber)
{
    int32_t rc = NO_ERROR;
    bool resetToPerfNormal = false;
    {
        Mutex::Autolock lock(mPriorityFramesLock);
        /* remove the priority frame number from the list */
        for (size_t i = 0; i < mPriorityFrames.size(); i++) {
            if (mPriorityFrames[i] == frameNumber) {
                mPriorityFrames.removeAt(i);
            }
        }
        /* reset the perf mode if pending priority frame list is empty */
        if (mReprocessPerfMode && mPriorityFrames.empty()) {
            resetToPerfNormal = true;
        }
    }
    if (resetToPerfNormal) {
        QCamera3Stream *pStream = mStreams[0];
        cam_stream_parm_buffer_t param;
        memset(&param, 0, sizeof(cam_stream_parm_buffer_t));

        param.type = CAM_STREAM_PARAM_TYPE_REQUEST_OPS_MODE;
        param.perf_mode = CAM_PERF_NORMAL;
        rc = pStream->setParameter(param);
        {
            Mutex::Autolock lock(mPriorityFramesLock);
            mReprocessPerfMode = false;
        }
    }
    return rc;
}

/*===========================================================================
 * FUNCTION   : getStreamBufs
 *
 * DESCRIPTION: register the buffers of the reprocess channel
 *
 * PARAMETERS : none
 *
 * RETURN     : QCamera3StreamMem *
 *==========================================================================*/
QCamera3StreamMem* QCamera3ReprocessChannel::getStreamBufs(uint32_t len)
{
    if (mReprocessType == REPROCESS_TYPE_JPEG) {
        mMemory = new QCamera3StreamMem(mNumBuffers);
        if (!mMemory) {
            LOGE("unable to create reproc memory");
            return NULL;
        }
        mFrameLen = len;
        return mMemory;
    }
    return &mGrallocMemory;
}

/*===========================================================================
 * FUNCTION   : putStreamBufs
 *
 * DESCRIPTION: release the reprocess channel buffers
 *
 * PARAMETERS : none
 *
 * RETURN     :
 *==========================================================================*/
void QCamera3ReprocessChannel::putStreamBufs()
{
   if (mReprocessType == REPROCESS_TYPE_JPEG) {
       mMemory->deallocate();
       delete mMemory;
       mMemory = NULL;
       mFreeBufferList.clear();
   } else {
       mGrallocMemory.unregisterBuffers();
   }
}

/*===========================================================================
 * FUNCTION   : ~QCamera3ReprocessChannel
 *
 * DESCRIPTION: destructor of QCamera3ReprocessChannel
 *
 * PARAMETERS : none
 *
 * RETURN     : none
 *==========================================================================*/
QCamera3ReprocessChannel::~QCamera3ReprocessChannel()
{
    destroy();

    if (m_handle) {
        m_camOps->delete_channel(m_camHandle, m_handle);
        LOGD("deleting channel %d", m_handle);
        m_handle = 0;
    }
}

/*===========================================================================
 * FUNCTION   : start
 *
 * DESCRIPTION: start reprocess channel.
 *
 * PARAMETERS :
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3ReprocessChannel::start()
{
    ATRACE_CAMSCOPE_CALL(CAMSCOPE_HAL3_REPROC_CH_START);
    int32_t rc = NO_ERROR;

    rc = QCamera3Channel::start();

    if (rc == NO_ERROR) {
       rc = m_camOps->start_channel(m_camHandle, m_handle, /*start_sensor_streaming*/true);

       // Check failure
       if (rc != NO_ERROR) {
           LOGE("start_channel failed %d", rc);
           QCamera3Channel::stop();
       }
    }
    return rc;
}

/*===========================================================================
 * FUNCTION   : stop
 *
 * DESCRIPTION: stop reprocess channel.
 *
 * PARAMETERS : none
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3ReprocessChannel::stop()
{
    ATRACE_CAMSCOPE_CALL(CAMSCOPE_HAL3_REPROC_CH_STOP);
    int32_t rc = NO_ERROR;

    rc = QCamera3Channel::stop();
    rc |= m_camOps->stop_channel(m_camHandle, m_handle, /*stop_channel_immediately*/false);
    // Unmapping the buffers
    unmapOfflineBuffers(true);
    return rc;
}

/*===========================================================================
 * FUNCTION   : getStreamBySrcHandle
 *
 * DESCRIPTION: find reprocess stream by its source stream handle
 *
 * PARAMETERS :
 *   @srcHandle : source stream handle
 *
 * RETURN     : ptr to reprocess stream if found. NULL if not found
 *==========================================================================*/
QCamera3Stream * QCamera3ReprocessChannel::getStreamBySrcHandle(uint32_t srcHandle)
{
    QCamera3Stream *pStream = NULL;

    for (uint32_t i = 0; i < m_numStreams; i++) {
        if (mSrcStreamHandles[i] == srcHandle) {
            pStream = mStreams[i];
            break;
        }
    }
    return pStream;
}

/*===========================================================================
 * FUNCTION   : getSrcStreamBySrcHandle
 *
 * DESCRIPTION: find source stream by source stream handle
 *
 * PARAMETERS :
 *   @srcHandle : source stream handle
 *
 * RETURN     : ptr to reprocess stream if found. NULL if not found
 *==========================================================================*/
QCamera3Stream * QCamera3ReprocessChannel::getSrcStreamBySrcHandle(uint32_t srcHandle)
{
    QCamera3Stream *pStream = NULL;

    if (NULL == m_pSrcChannel) {
        return NULL;
    }

    for (uint32_t i = 0; i < m_numStreams; i++) {
        if (mSrcStreamHandles[i] == srcHandle) {
            pStream = m_pSrcChannel->getStreamByIndex(i);
            break;
        }
    }
    return pStream;
}

/*===========================================================================
 * FUNCTION   : unmapOfflineBuffers
 *
 * DESCRIPTION: Unmaps offline buffers
 *
 * PARAMETERS : none
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3ReprocessChannel::unmapOfflineBuffers(bool all)
{
    int rc = NO_ERROR;
    Mutex::Autolock l(mOfflineBuffersLock);
    if (!mOfflineBuffers.empty()) {
        QCamera3Stream *stream = NULL;
        List<OfflineBuffer>::iterator it = mOfflineBuffers.begin();
        for (; it != mOfflineBuffers.end(); it++) {
           stream = (*it).stream;
           if (NULL != stream) {
               rc = stream->unmapBuf((*it).type,
                                     (*it).index,
                                        -1);
               if (NO_ERROR != rc) {
                   LOGE("Error during offline buffer unmap %d",
                          rc);
               }
               LOGD("Unmapped buffer with index %d", (*it).index);
           }
           if (!all) {
               mOfflineBuffers.erase(it);
               break;
           }
        }
        if (all) {
           mOfflineBuffers.clear();
        }
    }

    if (!mOfflineMetaBuffers.empty()) {
        QCamera3Stream *stream = NULL;
        List<OfflineBuffer>::iterator it = mOfflineMetaBuffers.begin();
        for (; it != mOfflineMetaBuffers.end(); it++) {
           stream = (*it).stream;
           if (NULL != stream) {
               rc = stream->unmapBuf((*it).type,
                                     (*it).index,
                                        -1);
               if (NO_ERROR != rc) {
                   LOGE("Error during offline buffer unmap %d",
                          rc);
               }
               LOGD("Unmapped meta buffer with index %d", (*it).index);
           }
           if (!all) {
               mOfflineMetaBuffers.erase(it);
               break;
           }
        }
        if (all) {
           mOfflineMetaBuffers.clear();
        }
    }
    return rc;
}

/*===========================================================================
 * FUNCTION   : bufDone
 *
 * DESCRIPTION: Return reprocess stream buffer to free buffer list.
 *              Note that this function doesn't queue buffer back to kernel.
 *              It's up to doReprocessOffline to do that instead.
 * PARAMETERS :
 *   @recvd_frame  : stream buf frame to be returned
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3ReprocessChannel::bufDone(mm_camera_super_buf_t *recvd_frame)
{
    int rc = NO_ERROR;
    if (recvd_frame && recvd_frame->num_bufs == 1) {
        Mutex::Autolock lock(mFreeBuffersLock);
        uint32_t buf_idx = recvd_frame->bufs[0]->buf_idx;
        mFreeBufferList.push_back(buf_idx);

    } else {
        LOGE("Fatal. Not supposed to be here");
        rc = BAD_VALUE;
    }

    return rc;
}

/*===========================================================================
 * FUNCTION   : overrideMetadata
 *
 * DESCRIPTION: Override metadata entry such as rotation, crop, and CDS info.
 *
 * PARAMETERS :
 *   @frame     : input frame from source stream
 *   meta_buffer: metadata buffer
 *   @metadata  : corresponding metadata
 *   @fwk_frame :
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3ReprocessChannel::overrideMetadata(qcamera_hal3_pp_buffer_t *pp_buffer,
        mm_camera_buf_def_t *meta_buffer, jpeg_settings_t *jpeg_settings,
        qcamera_fwk_input_pp_data_t &fwk_frame)
{
    int32_t rc = NO_ERROR;
    QCamera3HardwareInterface* hal_obj = (QCamera3HardwareInterface*)mUserData;
    if ((NULL == meta_buffer) || (NULL == pp_buffer) || (NULL == pp_buffer->input) ||
            (NULL == hal_obj)) {
        return BAD_VALUE;
    }

    metadata_buffer_t *meta = (metadata_buffer_t *)meta_buffer->buffer;
    mm_camera_super_buf_t *frame = pp_buffer->input;
    if (NULL == meta) {
        return BAD_VALUE;
    }

    for (uint32_t i = 0; i < frame->num_bufs; i++) {
        QCamera3Stream *pStream = getStreamBySrcHandle(frame->bufs[i]->stream_id);
        QCamera3Stream *pSrcStream = getSrcStreamBySrcHandle(frame->bufs[i]->stream_id);

        if (pStream != NULL && pSrcStream != NULL) {
            if (jpeg_settings) {
                // Find rotation info for reprocess stream
                cam_rotation_info_t rotation_info;
                memset(&rotation_info, 0, sizeof(rotation_info));
                if (jpeg_settings->jpeg_orientation == 0) {
                   rotation_info.rotation = ROTATE_0;
                } else if (jpeg_settings->jpeg_orientation == 90) {
                   rotation_info.rotation = ROTATE_90;
                } else if (jpeg_settings->jpeg_orientation == 180) {
                   rotation_info.rotation = ROTATE_180;
                } else if (jpeg_settings->jpeg_orientation == 270) {
                   rotation_info.rotation = ROTATE_270;
                }

                rotation_info.device_rotation = ROTATE_0;
                rotation_info.streamId = mStreams[0]->getMyServerID();
                ADD_SET_PARAM_ENTRY_TO_BATCH(meta, CAM_INTF_PARM_ROTATION, rotation_info);
            }

            // Find and insert crop info for reprocess stream
            IF_META_AVAILABLE(cam_crop_data_t, crop_data, CAM_INTF_META_CROP_DATA, meta) {
                if (MAX_NUM_STREAMS > crop_data->num_of_streams) {
                    for (int j = 0; j < crop_data->num_of_streams; j++) {
                        if (crop_data->crop_info[j].stream_id ==
                                pSrcStream->getMyServerID()) {

                            // Store crop/roi information for offline reprocess
                            // in the reprocess stream slot
                            crop_data->crop_info[crop_data->num_of_streams].crop =
                                    crop_data->crop_info[j].crop;
                            crop_data->crop_info[crop_data->num_of_streams].roi_map =
                                    crop_data->crop_info[j].roi_map;
                            crop_data->crop_info[crop_data->num_of_streams].stream_id =
                                    mStreams[0]->getMyServerID();
                            crop_data->num_of_streams++;

                            LOGD("Reprocess stream server id: %d",
                                     mStreams[0]->getMyServerID());
                            LOGD("Found offline reprocess crop %dx%d %dx%d",
                                    crop_data->crop_info[j].crop.left,
                                    crop_data->crop_info[j].crop.top,
                                    crop_data->crop_info[j].crop.width,
                                    crop_data->crop_info[j].crop.height);
                            LOGD("Found offline reprocess roimap %dx%d %dx%d",
                                    crop_data->crop_info[j].roi_map.left,
                                    crop_data->crop_info[j].roi_map.top,
                                    crop_data->crop_info[j].roi_map.width,
                                    crop_data->crop_info[j].roi_map.height);

                            break;
                        }
                    }
                } else {
                    LOGE("No space to add reprocess stream crop/roi information");
                }
            }

            IF_META_AVAILABLE(cam_cds_data_t, cdsInfo, CAM_INTF_META_CDS_DATA, meta) {
                uint8_t cnt = cdsInfo->num_of_streams;
                if (cnt <= MAX_NUM_STREAMS) {
                    cam_stream_cds_info_t repro_cds_info;
                    memset(&repro_cds_info, 0, sizeof(repro_cds_info));
                    repro_cds_info.stream_id = mStreams[0]->getMyServerID();
                    for (size_t i = 0; i < cnt; i++) {
                        if (cdsInfo->cds_info[i].stream_id ==
                                pSrcStream->getMyServerID()) {
                            repro_cds_info.cds_enable =
                                    cdsInfo->cds_info[i].cds_enable;
                            break;
                        }
                    }
                    cdsInfo->num_of_streams = 1;
                    cdsInfo->cds_info[0] = repro_cds_info;
                } else {
                    LOGE("No space to add reprocess stream cds information");
                }
            }

            fwk_frame.input_buffer = *frame->bufs[i];
            fwk_frame.metadata_buffer = *meta_buffer;
            fwk_frame.output_buffer = pp_buffer->output;
            break;
        } else {
            LOGE("Source/Re-process streams are invalid");
            rc |= BAD_VALUE;
        }
    }

    return rc;
}

/*===========================================================================
* FUNCTION : overrideFwkMetadata
*
* DESCRIPTION: Override frameworks metadata such as rotation, crop, and CDS data.
*
* PARAMETERS :
* @frame : input frame for reprocessing
*
* RETURN : int32_t type of status
* NO_ERROR -- success
* none-zero failure code
*==========================================================================*/
int32_t QCamera3ReprocessChannel::overrideFwkMetadata(
        qcamera_fwk_input_pp_data_t *frame)
{
    if (NULL == frame) {
        LOGE("Incorrect input frame");
        return BAD_VALUE;
    }

    if (NULL == frame->metadata_buffer.buffer) {
        LOGE("No metadata available");
        return BAD_VALUE;
    }
    metadata_buffer_t *meta = (metadata_buffer_t *) frame->metadata_buffer.buffer;

    // Not doing rotation at all for YUV to YUV reprocess
    if (mReprocessType != REPROCESS_TYPE_JPEG) {
        LOGD("Override rotation to 0 for channel reprocess type %d",
                mReprocessType);
        cam_rotation_info_t rotation_info;
        memset(&rotation_info, 0, sizeof(rotation_info));
        rotation_info.rotation = ROTATE_0;
        rotation_info.streamId = mStreams[0]->getMyServerID();
        ADD_SET_PARAM_ENTRY_TO_BATCH(meta, CAM_INTF_PARM_ROTATION, rotation_info);
    }

    // Find and insert crop info for reprocess stream
    IF_META_AVAILABLE(cam_crop_data_t, crop_data, CAM_INTF_META_CROP_DATA, meta) {
        if (1 == crop_data->num_of_streams) {
            // Store crop/roi information for offline reprocess
            // in the reprocess stream slot
            crop_data->crop_info[crop_data->num_of_streams].crop =
                    crop_data->crop_info[0].crop;
            crop_data->crop_info[crop_data->num_of_streams].roi_map =
                    crop_data->crop_info[0].roi_map;
            crop_data->crop_info[crop_data->num_of_streams].stream_id =
                    mStreams[0]->getMyServerID();
            crop_data->num_of_streams++;

            LOGD("Reprocess stream server id: %d",
                     mStreams[0]->getMyServerID());
            LOGD("Found offline reprocess crop %dx%d %dx%d",
                    crop_data->crop_info[0].crop.left,
                    crop_data->crop_info[0].crop.top,
                    crop_data->crop_info[0].crop.width,
                    crop_data->crop_info[0].crop.height);
            LOGD("Found offline reprocess roi map %dx%d %dx%d",
                    crop_data->crop_info[0].roi_map.left,
                    crop_data->crop_info[0].roi_map.top,
                    crop_data->crop_info[0].roi_map.width,
                    crop_data->crop_info[0].roi_map.height);
        } else {
            LOGE("Incorrect number of offline crop data entries %d",
                    crop_data->num_of_streams);
            return BAD_VALUE;
        }
    } else {
        LOGW("Crop data not present");
    }

    IF_META_AVAILABLE(cam_cds_data_t, cdsInfo, CAM_INTF_META_CDS_DATA, meta) {
        if (1 == cdsInfo->num_of_streams) {
            cdsInfo->cds_info[0].stream_id = mStreams[0]->getMyServerID();
        } else {
            LOGE("Incorrect number of offline cds info entries %d",
                     cdsInfo->num_of_streams);
            return BAD_VALUE;
        }
    }

    return NO_ERROR;
}

/*===========================================================================
 * FUNCTION   : doReprocessOffline
 *
 * DESCRIPTION: request to do a reprocess on the frame
 *
 * PARAMETERS :
 *   @frame     : input frame for reprocessing
 *   @isPriorityFrame: Hint that this frame is of priority, equivalent to
 *              real time, even though it is processed in offline mechanism
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
 int32_t  QCamera3ReprocessChannel::doReprocessOffline(
        qcamera_fwk_input_pp_data_t *frame, bool isPriorityFrame)
{
    int32_t rc = 0;
    int index;
    OfflineBuffer mappedBuffer;
    ATRACE_CALL();

    if (m_numStreams < 1) {
        LOGE("No reprocess stream is created");
        return -1;
    }

    if (NULL == frame) {
        LOGE("Incorrect input frame");
        return BAD_VALUE;
    }

    if (NULL == frame->metadata_buffer.buffer) {
        LOGE("No metadata available");
        return BAD_VALUE;
    }

    if (0 > frame->input_buffer.fd) {
        LOGE("No input buffer available");
        return BAD_VALUE;
    }

    if ((0 == m_numStreams) || (NULL == mStreams[0])) {
        LOGE("Reprocess stream not initialized!");
        return NO_INIT;
    }

    QCamera3Stream *pStream = mStreams[0];

    //qbuf the output buffer if it was allocated by the framework
    if (mReprocessType != REPROCESS_TYPE_JPEG && frame->output_buffer != NULL) {
        index = mGrallocMemory.getMatchBufIndex((void*)frame->output_buffer);
        if(index < 0) {
            rc = registerBuffer(frame->output_buffer, mIsType);
            if (NO_ERROR != rc) {
                LOGE("On-the-fly buffer registration failed %d",
                         rc);
                return rc;
            }

            index = mGrallocMemory.getMatchBufIndex((void*)frame->output_buffer);
            if (index < 0) {
                LOGE("Could not find object among registered buffers");
                return DEAD_OBJECT;
            }
        }
        rc = mGrallocMemory.markFrameNumber(index, frame->frameNumber);
        if(rc != NO_ERROR) {
            LOGE("Failed to mark frame#:%d, index:%d",frame->frameNumber,index);
            return rc;
        }
        if(!m_bIsActive) {
            rc = start();
            if (NO_ERROR != rc) {
                return rc;
            }
        } else {
            rc = pStream->bufDone(index);
            if(rc != NO_ERROR) {
                LOGE("Failed to Q new buffer to stream %d", rc);
                mGrallocMemory.markFrameNumber(index, -1);
                return rc;
            }
        }

    } else if (mReprocessType == REPROCESS_TYPE_JPEG) {
        Mutex::Autolock lock(mFreeBuffersLock);
        uint32_t bufIdx;
        if (mFreeBufferList.empty()) {
            rc = mMemory->allocateOne(mFrameLen);
            if (rc < 0) {
                LOGE("Failed allocating heap buffer. Fatal");
                return BAD_VALUE;
            } else {
                bufIdx = (uint32_t)rc;
            }
        } else {
            bufIdx = *(mFreeBufferList.begin());
            mFreeBufferList.erase(mFreeBufferList.begin());
        }

        mMemory->markFrameNumber(bufIdx, frame->frameNumber);
        rc = pStream->bufDone(bufIdx);
        if (rc != NO_ERROR) {
            LOGE("Failed to queue new buffer to stream");
            return rc;
        }
    }

    int32_t max_idx = (int32_t) (mNumBuffers - 1);
    //loop back the indices if max burst count reached
    if (mOfflineBuffersIndex == max_idx) {
       mOfflineBuffersIndex = -1;
    }
    uint32_t buf_idx = (uint32_t)(mOfflineBuffersIndex + 1);

    //Do cache ops before sending for reprocess
    if (mMemory != NULL) {
        mMemory->cleanInvalidateCache(buf_idx);
    }

    rc = pStream->mapBuf(
            CAM_MAPPING_BUF_TYPE_OFFLINE_INPUT_BUF,
            buf_idx, -1,
            frame->input_buffer.fd, frame->input_buffer.buffer,
            frame->input_buffer.frame_len);
    if (NO_ERROR == rc) {
        Mutex::Autolock l(mOfflineBuffersLock);
        mappedBuffer.index = buf_idx;
        mappedBuffer.stream = pStream;
        mappedBuffer.type = CAM_MAPPING_BUF_TYPE_OFFLINE_INPUT_BUF;
        mOfflineBuffers.push_back(mappedBuffer);
        mOfflineBuffersIndex = (int32_t)buf_idx;
        LOGD("Mapped buffer with index %d", mOfflineBuffersIndex);
    }

    max_idx = (int32_t) ((mNumBuffers * 2) - 1);
    //loop back the indices if max burst count reached
    if (mOfflineMetaIndex == max_idx) {
       mOfflineMetaIndex = (int32_t) (mNumBuffers - 1);
    }
    uint32_t meta_buf_idx = (uint32_t)(mOfflineMetaIndex + 1);
    rc |= pStream->mapBuf(
            CAM_MAPPING_BUF_TYPE_OFFLINE_META_BUF,
            meta_buf_idx, -1,
            frame->metadata_buffer.fd, frame->metadata_buffer.buffer,
            frame->metadata_buffer.frame_len);
    if (NO_ERROR == rc) {
        Mutex::Autolock l(mOfflineBuffersLock);
        mappedBuffer.index = meta_buf_idx;
        mappedBuffer.stream = pStream;
        mappedBuffer.type = CAM_MAPPING_BUF_TYPE_OFFLINE_META_BUF;
        mOfflineMetaBuffers.push_back(mappedBuffer);
        mOfflineMetaIndex = (int32_t)meta_buf_idx;
        LOGD("Mapped meta buffer with index %d", mOfflineMetaIndex);
    }

    if (rc == NO_ERROR) {
        cam_stream_parm_buffer_t param;
        uint32_t numPendingPriorityFrames = 0;

        if(isPriorityFrame && (mReprocessType != REPROCESS_TYPE_JPEG)) {
            Mutex::Autolock lock(mPriorityFramesLock);
            /* read the length before pushing the frame number to check if
             * vector is empty */
            numPendingPriorityFrames = mPriorityFrames.size();
            mPriorityFrames.push(frame->frameNumber);
        }

        if(isPriorityFrame && !numPendingPriorityFrames &&
            (mReprocessType != REPROCESS_TYPE_JPEG)) {
            memset(&param, 0, sizeof(cam_stream_parm_buffer_t));
            param.type = CAM_STREAM_PARAM_TYPE_REQUEST_OPS_MODE;
            param.perf_mode = CAM_PERF_HIGH_PERFORMANCE;
            rc = pStream->setParameter(param);
            if (rc != NO_ERROR) {
                LOGE("%s: setParameter for CAM_PERF_HIGH_PERFORMANCE failed",
                    __func__);
            }
            {
                Mutex::Autolock lock(mPriorityFramesLock);
                mReprocessPerfMode = true;
            }
        }

        memset(&param, 0, sizeof(cam_stream_parm_buffer_t));
        param.type = CAM_STREAM_PARAM_TYPE_DO_REPROCESS;
        param.reprocess.buf_index = buf_idx;
        param.reprocess.frame_idx = frame->input_buffer.frame_idx;
        param.reprocess.meta_present = 1;
        param.reprocess.meta_buf_index = meta_buf_idx;

        LOGI("Offline reprocessing id = %d buf Id = %d meta index = %d",
                    param.reprocess.frame_idx, param.reprocess.buf_index,
                    param.reprocess.meta_buf_index);
        rc = pStream->setParameter(param);
        if (rc != NO_ERROR) {
            LOGE("stream setParameter for reprocess failed");
            resetToCamPerfNormal(frame->frameNumber);
        }
    } else {
        LOGE("Input buffer memory map failed: %d", rc);
    }

    return rc;
}

/*===========================================================================
 * FUNCTION   : doReprocess
 *
 * DESCRIPTION: request to do a reprocess on the frame
 *
 * PARAMETERS :
 *   @buf_fd     : fd to the input buffer that needs reprocess
 *   @buffer     : Buffer ptr
 *   @buf_lenght : length of the input buffer
 *   @ret_val    : result of reprocess.
 *                 Example: Could be faceID in case of register face image.
 *   @meta_frame : metadata frame.
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3ReprocessChannel::doReprocess(int buf_fd, void *buffer, size_t buf_length,
        int32_t &ret_val, mm_camera_super_buf_t *meta_frame)
{
    int32_t rc = 0;
    if (m_numStreams < 1) {
        LOGE("No reprocess stream is created");
        return -1;
    }
    if (meta_frame == NULL) {
        LOGE("Did not get corresponding metadata in time");
        return -1;
    }

    uint8_t buf_idx = 0;
    for (uint32_t i = 0; i < m_numStreams; i++) {
        rc = mStreams[i]->mapBuf(CAM_MAPPING_BUF_TYPE_OFFLINE_INPUT_BUF,
                                 buf_idx, -1,
                                 buf_fd, buffer, buf_length);

        //Do cache ops before sending for reprocess
        if (mMemory != NULL) {
            mMemory->cleanInvalidateCache(buf_idx);
        }

        if (rc == NO_ERROR) {
            cam_stream_parm_buffer_t param;
            memset(&param, 0, sizeof(cam_stream_parm_buffer_t));
            param.type = CAM_STREAM_PARAM_TYPE_DO_REPROCESS;
            param.reprocess.buf_index = buf_idx;
            param.reprocess.meta_present = 1;
            param.reprocess.meta_stream_handle = m_pMetaChannel->mStreams[0]->getMyServerID();
            param.reprocess.meta_buf_index = meta_frame->bufs[0]->buf_idx;

            LOGI("Online reprocessing id = %d buf Id = %d meta index = %d",
                    param.reprocess.frame_idx, param.reprocess.buf_index,
                    param.reprocess.meta_buf_index);
            rc = mStreams[i]->setParameter(param);
            if (rc == NO_ERROR) {
                ret_val = param.reprocess.ret_val;
            }
            mStreams[i]->unmapBuf(CAM_MAPPING_BUF_TYPE_OFFLINE_INPUT_BUF,
                                  buf_idx, -1);
        }
    }
    return rc;
}

/*===========================================================================
 * FUNCTION   : addReprocStreamsFromSource
 *
 * DESCRIPTION: add reprocess streams from input source channel
 *
 * PARAMETERS :
 *   @config         : pp feature configuration
 *   @src_config     : source reprocess configuration
 *   @isType         : type of image stabilization required on this stream
 *   @pMetaChannel   : ptr to metadata channel to get corresp. metadata
 *
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3ReprocessChannel::addReprocStreamsFromSource(cam_pp_feature_config_t &pp_config,
        const reprocess_config_t &src_config , cam_is_type_t is_type,
        QCamera3Channel *pMetaChannel)
{
    int32_t rc = 0;
    cam_stream_reproc_config_t reprocess_config;
    cam_stream_type_t streamType;

    cam_dimension_t streamDim = src_config.output_stream_dim;

    if (NULL != src_config.src_channel) {
        QCamera3Stream *pSrcStream = src_config.src_channel->getStreamByIndex(0);
        if (pSrcStream == NULL) {
           LOGE("source channel doesn't have a stream");
           return BAD_VALUE;
        }
        mSrcStreamHandles[m_numStreams] = pSrcStream->getMyHandle();
    }

    streamType = CAM_STREAM_TYPE_OFFLINE_PROC;
    reprocess_config.pp_type = CAM_OFFLINE_REPROCESS_TYPE;

    reprocess_config.offline.input_fmt = src_config.stream_format;
    reprocess_config.offline.input_dim = src_config.input_stream_dim;
    reprocess_config.offline.input_buf_planes.plane_info =
            src_config.input_stream_plane_info.plane_info;
    reprocess_config.offline.num_of_bufs = (uint8_t)mNumBuffers;
    reprocess_config.offline.input_type = src_config.stream_type;

    reprocess_config.pp_feature_config = pp_config;
    QCamera3Stream *pStream = new QCamera3Stream(m_camHandle,
            m_handle,
            m_camOps,
            &mPaddingInfo,
            (QCamera3Channel*)this,
            false/*mapStreamBuffers*/);
    if (pStream == NULL) {
        LOGE("No mem for Stream");
        return NO_MEMORY;
    }

    rc = pStream->init(streamType, src_config.stream_format,
            streamDim, ROTATE_0, &reprocess_config,
            (uint8_t)mNumBuffers,
            reprocess_config.pp_feature_config.feature_mask,
            is_type,
            0,/* batchSize */
            QCamera3Channel::streamCbRoutine, this);

    if (rc == 0) {
        mStreams[m_numStreams] = pStream;
        m_numStreams++;
    } else {
        LOGE("failed to create reprocess stream");
        delete pStream;
    }

    if (rc == NO_ERROR) {
        m_pSrcChannel = src_config.src_channel;
        m_pMetaChannel = pMetaChannel;
        mReprocessType = src_config.reprocess_type;
        LOGD("mReprocessType is %d", mReprocessType);
    }
    mm_camera_req_buf_t buf;
    memset(&buf, 0x0, sizeof(buf));
    buf.type = MM_CAMERA_REQ_SUPER_BUF;
    buf.num_buf_requested = 1;
    if(m_camOps->request_super_buf(m_camHandle,m_handle, &buf) < 0) {
        LOGE("Request for super buffer failed");
    }
    return rc;
}

/* QCamera3SupportChannel methods */

cam_dimension_t QCamera3SupportChannel::kDim = {640, 480};

QCamera3SupportChannel::QCamera3SupportChannel(uint32_t cam_handle,
                    uint32_t channel_handle,
                    mm_camera_ops_t *cam_ops,
                    cam_padding_info_t *paddingInfo,
                    cam_feature_mask_t postprocess_mask,
                    cam_stream_type_t streamType,
                    cam_dimension_t *dim,
                    cam_format_t streamFormat,
                    uint8_t hw_analysis_supported,
                    cam_color_filter_arrangement_t color_arrangement,
                    void *userData, uint32_t numBuffers) :
                        QCamera3Channel(cam_handle, channel_handle, cam_ops,
                                NULL, NULL, paddingInfo, postprocess_mask,
                                userData, numBuffers),
                        mMemory(NULL),
                        mHwAnalysisSupported(hw_analysis_supported),
                        mColorArrangement(color_arrangement)
{
    memcpy(&mDim, dim, sizeof(cam_dimension_t));
    mStreamType = streamType;
    mStreamFormat = streamFormat;
}

QCamera3SupportChannel::~QCamera3SupportChannel()
{
    destroy();

    if (mMemory) {
        mMemory->deallocate();
        delete mMemory;
        mMemory = NULL;
    }
}

int32_t QCamera3SupportChannel::initialize(cam_is_type_t isType)
{
    int32_t rc;

    if (mMemory || m_numStreams > 0) {
        LOGE("metadata channel already initialized");
        return -EINVAL;
    }

    mIsType = isType;
   // Make Analysis same as Preview format
   if (!mHwAnalysisSupported && mStreamType == CAM_STREAM_TYPE_ANALYSIS &&
           mColorArrangement != CAM_FILTER_ARRANGEMENT_Y) {
        mStreamFormat = getStreamDefaultFormat(CAM_STREAM_TYPE_PREVIEW,
                mDim.width, mDim.height, m_bUBWCenable, mIsType);
   }

    rc = QCamera3Channel::addStream(mStreamType,
            mStreamFormat, mDim, ROTATE_0, MIN_STREAMING_BUFFER_NUM,
            mPostProcMask, mIsType);
    if (rc < 0) {
        LOGE("addStream failed");
    }
    return rc;
}

int32_t QCamera3SupportChannel::request(buffer_handle_t * /*buffer*/,
                                                uint32_t /*frameNumber*/,
                                                int & /*indexUsed*/)
{
    return NO_ERROR;
}

void QCamera3SupportChannel::streamCbRoutine(
                        mm_camera_super_buf_t *super_frame,
                        QCamera3Stream * /*stream*/)
{
    if (super_frame == NULL || super_frame->num_bufs != 1) {
        LOGE("super_frame is not valid");
        return;
    }
    bufDone(super_frame);
    free(super_frame);
}

QCamera3StreamMem* QCamera3SupportChannel::getStreamBufs(uint32_t len)
{
    int rc;
    mMemory = new QCamera3StreamMem(mNumBuffers);
    if (!mMemory) {
        LOGE("unable to create heap memory");
        return NULL;
    }
    rc = mMemory->allocateAll(len);
    if (rc < 0) {
        LOGE("unable to allocate heap memory");
        delete mMemory;
        mMemory = NULL;
        return NULL;
    }
    return mMemory;
}

void QCamera3SupportChannel::putStreamBufs()
{
    mMemory->deallocate();
    delete mMemory;
    mMemory = NULL;
}

QCamera3DepthChannel::~QCamera3DepthChannel() {
    unmapAllBuffers();
}

/*===========================================================================
 * FUNCTION   : mapBuffer
 *
 * DESCRIPTION: Maps stream depth buffer
 *
 * PARAMETERS :
 *   @buffer       : Depth buffer
 *   @frameNumber  : Frame number
 *
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3DepthChannel::mapBuffer(buffer_handle_t *buffer,
        uint32_t frameNumber) {
    int32_t rc = NO_ERROR;

    int32_t index = mGrallocMem.getMatchBufIndex((void*)buffer);
    if (0 > index) {
        rc = mGrallocMem.registerBuffer(buffer, CAM_STREAM_TYPE_DEFAULT);
        if (NO_ERROR != rc) {
            LOGE("Buffer registration failed %d", rc);
            return rc;
        }

        index = mGrallocMem.getMatchBufIndex((void*)buffer);
        if (index < 0) {
            LOGE("Could not find object among registered buffers");
            return DEAD_OBJECT;
        }
    } else {
        LOGE("Buffer: %p is already present at index: %d!", buffer, index);
        return ALREADY_EXISTS;
    }

    rc = mGrallocMem.markFrameNumber((uint32_t)index, frameNumber);

    return rc;
}

/*===========================================================================
 * FUNCTION   : populateDepthData
 *
 * DESCRIPTION: Copies the incoming depth data in the respective depth buffer
 *
 * PARAMETERS :
 *   @data         : Incoming Depth data
 *   @frameNumber  : Frame number of incoming depth data
 *
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3DepthChannel::populateDepthData(const cam_depth_data_t &data,
        uint32_t frameNumber) {
    if (nullptr == mStream) {
        LOGE("Invalid depth stream!");
        return BAD_VALUE;
    }

    ssize_t length = data.length;
    int32_t index = mGrallocMem.getBufferIndex(frameNumber);
    if (0 > index) {
        LOGE("Frame number: %u not present!");
        return BAD_VALUE;
    }

    void *dst = mGrallocMem.getPtr(index);
    if (nullptr == dst) {
        LOGE("Invalid mapped buffer");
        return BAD_VALUE;
    }

    camera3_jpeg_blob_t jpegHeader;
    ssize_t headerSize = sizeof jpegHeader;
    buffer_handle_t *blobBufferHandle = static_cast<buffer_handle_t *>
            (mGrallocMem.getBufferHandle(index));
    ssize_t maxBlobSize;
    if (nullptr != blobBufferHandle) {
        maxBlobSize = ((private_handle_t*)(*blobBufferHandle))->width;
    } else {
        LOGE("Couldn't query buffer handle!");
        return BAD_VALUE;
    }

    if ((length + headerSize) > maxBlobSize) {
        LOGE("Depth buffer size mismatch expected: %d actual: %d",
                (length + headerSize), maxBlobSize);
        return BAD_VALUE;
    }

    if (0 < length) {
        memcpy(dst, data.depth_data, length);
    }

    memset(&jpegHeader, 0, headerSize);
    jpegHeader.jpeg_blob_id = CAMERA3_JPEG_BLOB_ID;
    jpegHeader.jpeg_size = length;
    size_t jpeg_eof_offset = static_cast<size_t> (maxBlobSize - headerSize);
    uint8_t *jpegBuffer = static_cast<uint8_t *> (dst);
    uint8_t *jpegEOF = &jpegBuffer[jpeg_eof_offset];
    memcpy(jpegEOF, &jpegHeader, headerSize);

    return NO_ERROR;
}

/*===========================================================================
 * FUNCTION   : getOldestFrame
 *
 * DESCRIPTION: Return oldest mapped buffer
 *
 * PARAMETERS :
 *   @frameNumber         : Sets oldest frame number if present
 *
 *
 * RETURN     : buffer_handle_t pointer
 *              NULL in case of error
 *==========================================================================*/
buffer_handle_t *QCamera3DepthChannel::getOldestFrame(uint32_t &frameNumber) {
    uint32_t oldestIndex = UINT32_MAX;
    int32_t frameNumberResult = mGrallocMem.getOldestFrameNumber(oldestIndex);
    if (0 > frameNumberResult) {
        LOGD("Invalid frame number!");
        return nullptr;
    }
    frameNumber = static_cast<uint32_t> (frameNumberResult);

    buffer_handle_t *ret = static_cast<buffer_handle_t *>
            (mGrallocMem.getBufferHandle(oldestIndex));
    if (nullptr == ret) {
        LOGE("Invalid buffer handle!");
        return nullptr;
    }

    return ret;
}

/*===========================================================================
 * FUNCTION   : unmapBuffer
 *
 * DESCRIPTION: Unmap a single buffer
 *
 * PARAMETERS :
 *   @frameNumber         : Frame number of buffer that should get unmapped
 *
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3DepthChannel::unmapBuffer(uint32_t frameNumber) {
    int32_t index = mGrallocMem.getBufferIndex(frameNumber);
    if (0 > index) {
        LOGE("Frame number: %u not present!", frameNumber);
        return BAD_VALUE;
    }

    return mGrallocMem.unregisterBuffer(index);
}

/*===========================================================================
 * FUNCTION   : unmapAllBuffers
 *
 * DESCRIPTION: This will unmap all buffers
 *
 * PARAMETERS :
 *
 * RETURN     : int32_t type of status
 *              NO_ERROR  -- success
 *              none-zero failure code
 *==========================================================================*/
int32_t QCamera3DepthChannel::unmapAllBuffers() {
    mGrallocMem.unregisterBuffers();

    return NO_ERROR;
}

}; // namespace qcamera
