/*
 * Copyright (C) 2007 The Android Open Source Project
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

#ifndef RECOVERY_COMMON_H
#define RECOVERY_COMMON_H

#include <stdio.h>
#include <stdarg.h>

#include <string>
#include "device.h"

// Not using the command-line defined macro here because this header could be included by
// device-specific recovery libraries. We static assert the value consistency in recovery.cpp.
static constexpr int kRecoveryApiVersion = 3;

class RecoveryUI;

extern RecoveryUI* ui;
extern bool modified_flash;

// The current stage, e.g. "1/2".
extern std::string stage;

// The reason argument provided in "--reason=".
extern const char* reason;

extern const char *SDCARD_ROOT;

extern Device* device;

// fopen a file, mounting volumes and making parent dirs as necessary.
FILE* fopen_path(const char *path, const char *mode);

void ui_print(const char* format, ...);

bool is_ro_debuggable();

bool reboot(const std::string& command);

bool repart_yes_no(Device* device);
#endif  // RECOVERY_COMMON_H
