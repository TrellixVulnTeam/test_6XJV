/*
 * Copyright (C) 2018 The Android Open Source Project
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

#ifndef AUDIO_HW_H
#define AUDIO_HW_H

#include <pthread.h>

#include <cutils/hashmap.h>
#include <hardware/audio.h>
#include <tinyalsa/asoundlib.h>

#include "audio_vbuffer.h"

struct generic_audio_device {
  struct audio_hw_device device;  // Constant after init
  pthread_mutex_t lock;
  bool master_mute;             // Proteced by this->lock
  bool mic_mute;                // Proteced by this->lock
  struct mixer *mixer;          // Proteced by this->lock
  Hashmap *out_bus_stream_map;  // Extended field. Constant after init
};

struct generic_stream_out {
  struct audio_stream_out stream;  // Constant after init
  pthread_mutex_t lock;
  struct generic_audio_device *dev;  // Constant after init
  audio_devices_t device;            // Protected by this->lock
  struct audio_config req_config;    // Constant after init
  struct pcm_config pcm_config;      // Constant after init
  audio_vbuffer_t buffer;            // Constant after init
  const char *bus_address;           // Extended field. Constant after init
  struct audio_gain gain_stage;      // Constant after init
  float amplitude_ratio;             // Protected by this->lock

  // Time & Position Keeping
  bool standby;                    // Protected by this->lock
  uint64_t underrun_position;      // Protected by this->lock
  struct timespec underrun_time;   // Protected by this->lock
  uint64_t last_write_time_us;     // Protected by this->lock
  uint64_t frames_total_buffered;  // Protected by this->lock
  uint64_t frames_written;         // Protected by this->lock
  uint64_t frames_rendered;        // Protected by this->lock

  // Worker
  pthread_t worker_thread;     // Constant after init
  pthread_cond_t worker_wake;  // Protected by this->lock
  bool worker_standby;         // Protected by this->lock
  bool worker_exit;            // Protected by this->lock
};

struct generic_stream_in {
  struct audio_stream_in stream;  // Constant after init
  pthread_mutex_t lock;
  struct generic_audio_device *dev;  // Constant after init
  audio_devices_t device;            // Protected by this->lock
  struct audio_config req_config;    // Constant after init
  struct pcm *pcm;                   // Protected by this->lock
  struct pcm_config pcm_config;      // Constant after init
  int16_t *stereo_to_mono_buf;       // Protected by this->lock
  size_t stereo_to_mono_buf_size;    // Protected by this->lock
  audio_vbuffer_t buffer;            // Protected by this->lock
  const char *bus_address;           // Extended field. Constant after init

  // Time & Position Keeping
  bool standby;                       // Protected by this->lock
  int64_t standby_position;           // Protected by this->lock
  struct timespec standby_exit_time;  // Protected by this->lock
  int64_t standby_frames_read;        // Protected by this->lock

  // Worker
  pthread_t worker_thread;     // Constant after init
  pthread_cond_t worker_wake;  // Protected by this->lock
  bool worker_standby;         // Protected by this->lock
  bool worker_exit;            // Protected by this->lock
};

#endif  // AUDIO_HW_H
