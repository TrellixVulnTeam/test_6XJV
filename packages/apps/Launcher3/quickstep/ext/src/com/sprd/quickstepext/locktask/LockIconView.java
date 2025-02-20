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
package com.sprd.quickstepext.locktask;

import android.content.Context;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.util.AttributeSet;

import com.android.launcher3.R;

public class LockIconView extends LinearLayout {
    private ImageView mIconView;

    public LockIconView(Context context) {
        this(context,null);
    }

    public LockIconView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public LockIconView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mIconView = findViewById(R.id.lockicon);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setLockState(boolean isLocked) {
        if (isLocked) {
            mIconView.setImageResource(R.drawable.recents_lock_icon);
        } else {
            mIconView.setImageResource(R.drawable.recents_unlock_icon);
        }
    }
}
