LOCAL_PATH:= $(call my-dir)

##################################################################
# WRAPPER LIBRARY
##################################################################

include $(CLEAR_VARS)

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/include \
    frameworks/av/services/audiopolicy/engineconfigurable/include \
    frameworks/av/services/audiopolicy/engineconfigurable/interface \
    frameworks/av/services/audiopolicy/utilities/convert \

LOCAL_SRC_FILES:= ParameterManagerWrapper.cpp

LOCAL_SHARED_LIBRARIES := \
    libparameter \
    libmedia_helper

LOCAL_MULTILIB := $(AUDIOSERVER_MULTILIB)

LOCAL_MODULE:= libaudiopolicypfwwrapper
LOCAL_EXPORT_C_INCLUDE_DIRS := $(LOCAL_PATH)/include

LOCAL_MODULE_TAGS := optional
LOCAL_CFLAGS := -Wall -Werror -Wextra

include $(BUILD_STATIC_LIBRARY)

##################################################################
# CONFIGURATION FILE
##################################################################

ifneq ($(USE_CONFIGURABLE_AUDIO_POLICY),1)
# specific management of audio_policy_criteria.conf
include $(CLEAR_VARS)
LOCAL_MODULE := audio_policy_criteria.conf
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)
LOCAL_SRC_FILES := config/$(LOCAL_MODULE)
include $(BUILD_PREBUILT)
endif
