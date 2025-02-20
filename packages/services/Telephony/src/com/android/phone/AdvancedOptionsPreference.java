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
package com.android.phone;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

/**
 * Customized preference class representing the "Advanced" button that expands to fields that
 * are hidden by default.
 */
public class AdvancedOptionsPreference extends Preference {
    public AdvancedOptionsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindView(View view) {
        /* UNISOC: bug 902092 @{ */
        setIcon(R.drawable.ic_expand_more);
        setTitle(R.string.advanced_options_title);
        TextView summary = view.findViewById(android.R.id.summary);
        summary.setMaxLines(1);

        super.onBindView(view);
        /* @} */
    }
}
