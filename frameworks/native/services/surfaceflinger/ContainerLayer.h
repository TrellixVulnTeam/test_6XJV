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
#pragma once

#include <sys/types.h>

#include <cstdint>

#include "Layer.h"

namespace android {

class ContainerLayer : public Layer {
public:
    ContainerLayer(SurfaceFlinger* flinger, const sp<Client>& client, const String8& name,
                   uint32_t w, uint32_t h, uint32_t flags);
    virtual ~ContainerLayer() = default;

    const char* getTypeId() const override { return "ContainerLayer"; }
    void onDraw(const RenderArea& renderArea, const Region& clip,
                bool useIdentityTransform) const override;
    bool isVisible() const override { return false; }

    void setPerFrameData(const sp<const DisplayDevice>& displayDevice) override;
};

} // namespace android
