/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef SAMPLE_ITERATOR_H_

#define SAMPLE_ITERATOR_H_

#include <utils/Vector.h>

namespace android {

class SampleTable;

struct SampleIterator {
    explicit SampleIterator(SampleTable *table);

    status_t seekTo(uint32_t sampleIndex);

    uint32_t getChunkIndex() const { return mCurrentChunkIndex; }
    uint32_t getDescIndex() const { return mChunkDesc; }
    off64_t getSampleOffset() const { return mCurrentSampleOffset; }
    size_t getSampleSize() const { return mCurrentSampleSize; }
    uint64_t getSampleTime() const { return mCurrentSampleTime; }
    uint32_t getSampleDuration() const { return mCurrentSampleDuration; }

    status_t getSampleSizeDirect(
            uint32_t sampleIndex, size_t *size);

private:
    SampleTable *mTable;

    bool mInitialized;

    uint32_t mSampleToChunkIndex;
    uint32_t mFirstChunk;
    uint32_t mFirstChunkSampleIndex;
    uint32_t mStopChunk;
    uint32_t mStopChunkSampleIndex;
    uint32_t mSamplesPerChunk;
    uint32_t mChunkDesc;

    uint32_t mCurrentChunkIndex;
    off64_t mCurrentChunkOffset;
    Vector<size_t> mCurrentChunkSampleSizes;

    uint32_t mTimeToSampleIndex;
    uint32_t mTTSSampleIndex;
    uint64_t mTTSSampleTime;
    uint32_t mTTSCount;
    uint32_t mTTSDuration;

    uint32_t mCurrentSampleIndex;
    off64_t mCurrentSampleOffset;
    size_t mCurrentSampleSize;
    uint64_t mCurrentSampleTime;
    uint32_t mCurrentSampleDuration;
    uint32_t mCurSimpleSizeIndex;

    void reset();
    status_t findChunkRange(uint32_t sampleIndex);
    status_t getChunkOffset(uint32_t chunk, off64_t *offset);
    status_t findSampleTimeAndDuration(uint32_t sampleIndex, uint64_t *time, uint32_t *duration);

    SampleIterator(const SampleIterator &);
    SampleIterator &operator=(const SampleIterator &);
};

}  // namespace android

#endif  // SAMPLE_ITERATOR_H_
