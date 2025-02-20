/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.launcher3.folder;

import android.graphics.drawable.Drawable;

/**
 * Manages the parameters used to draw a Folder preview item.
 */
public class PreviewItemDrawingParams {
    float transX;
    float transY;
    float scale;
    float overlayAlpha;
    FolderPreviewItemAnim anim;
    public boolean hidden;
    Drawable drawable;

    public PreviewItemDrawingParams(float transX, float transY, float scale, float overlayAlpha) {
        this.transX = transX;
        this.transY = transY;
        this.scale = scale;
        this.overlayAlpha = overlayAlpha;
    }

    public void update(float transX, float transY, float scale) {
        // We ensure the update will not interfere with an animation on the layout params
        // If the final values differ, we cancel the animation.
        if (anim != null) {
            if (anim.finalTransX == transX || anim.finalTransY == transY
                    || anim.finalScale == scale) {
                return;
            }
            anim.cancel();
        }

        this.transX = transX;
        this.transY = transY;
        this.scale = scale;
    }
}
