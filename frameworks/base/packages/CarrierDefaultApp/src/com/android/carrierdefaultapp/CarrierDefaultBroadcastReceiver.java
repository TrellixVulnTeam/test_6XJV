/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.carrierdefaultapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;

public class CarrierDefaultBroadcastReceiver extends BroadcastReceiver{
    private static final String TAG = CarrierDefaultBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        // SPRD: bug/797640 Modify for intentfuzzer-broadcast-null intent fuzzing.
        if (intent == null || intent.getAction() == null) {
            return;
        }

        Log.d(TAG, "onReceive intent: " + intent.getAction());
        if (ProvisionObserver.isDeferredForProvision(context, intent)) {
            Log.d(TAG, "skip carrier actions during provisioning");
            return;
        }
        if (Intent.ACTION_LOCALE_CHANGED.equals(intent.getAction())) {
            CarrierActionUtils.createNotificationChannels(context);
            return;
        }
        List<Integer> actionList = CustomConfigLoader.loadCarrierActionList(context, intent);
        for (int actionIdx : actionList) {
            Log.d(TAG, "apply carrier action idx: " + actionIdx);
            CarrierActionUtils.applyCarrierAction(actionIdx, intent, context);
        }
    }
}
