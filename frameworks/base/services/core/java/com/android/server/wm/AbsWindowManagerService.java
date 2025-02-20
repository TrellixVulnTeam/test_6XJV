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

package com.android.server.wm;

import android.view.IWindowManager;
/** {@hide} */
public abstract class AbsWindowManagerService extends IWindowManager.Stub {
	/* SPRD: for Super Resolution @{ */
    public static final String ACTION_CHANGE_DISPLAY_CONFIG = "sprd.action.change_display_config";
    public void setForcedDisplaySizeDensity(int width, int height, int density) {
    }
    int computeInitialDisplayDensity(DisplayContent displayContent, int defaultDensity) {
        return defaultDensity;
    }
    void dumpCurrentWindow(){
    }
    /* }@ */
}
