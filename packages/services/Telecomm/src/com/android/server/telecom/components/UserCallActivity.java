/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.server.telecom.components;

import com.android.server.telecom.CallIntentProcessor;
import com.android.server.telecom.TelecomSystem;
import com.android.server.telecom.R;
import android.telecom.VideoProfile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.ServiceManager;
import android.telecom.Log;
import android.telecom.TelecomManager;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.telecom.VideoProfile;
import android.content.IntentFilter;
import android.os.Build;
import android.provider.Settings;
import android.view.WindowManager;


// TODO: Needed for move to system service: import com.android.internal.R;

/**
 * Activity that handles system CALL actions and forwards them to {@link CallIntentProcessor}.
 * Handles all three CALL action types: CALL, CALL_PRIVILEGED, and CALL_EMERGENCY.
 *
 * Pre-L, the only way apps were were allowed to make outgoing emergency calls was the
 * ACTION_CALL_PRIVILEGED action (which requires the system only CALL_PRIVILEGED permission).
 *
 * In L, any app that has the CALL_PRIVILEGED permission can continue to make outgoing emergency
 * calls via ACTION_CALL_PRIVILEGED.
 *
 * In addition, the default dialer (identified via
 * {@link TelecomManager#getDefaultPhoneApp()} will also be granted the ability to
 * make emergency outgoing calls using the CALL action. In order to do this, it must call
 * startActivityForResult on the CALL intent to allow its package name to be passed to
 * {@link UserCallActivity}. Calling startActivity will continue to work on all non-emergency
 * numbers just like it did pre-L.
 */
public class UserCallActivity extends Activity implements TelecomSystem.Component {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "UserCallActivity");
        wakelock.acquire();

        Log.startSession("UCA.oC");
        try {
            // TODO: Figure out if there is something to restore from bundle.
            // See OutgoingCallBroadcaster in services/Telephony for more.
            Intent intent = getIntent();
            verifyCallAction(intent);
            final UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
            final UserHandle userHandle = new UserHandle(userManager.getUserHandle());
            // Once control flow has passed to this activity, it is no longer guaranteed that we can
            // accurately determine whether the calling package has the CALL_PHONE runtime permission.
            // At this point in time we trust that the ActivityManager has already performed this
            // validation before starting this activity.
            // Create a new instance of intent to avoid modifying the
            // ActivityThread.ActivityClientRecord#intent directly.
            // Modifying directly may be a potential risk when relaunching this activity.
            int videoState = intent.getIntExtra( TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE,
                                  VideoProfile.STATE_AUDIO_ONLY);
            boolean isLowBattery = isBatteryLow(this);
            Log.i(this,"isLowBattery=%d, isVideo=%d.",isLowBattery,VideoProfile.isVideo(videoState));
            if(VideoProfile.isVideo(videoState) && isLowBattery ) {
                showLowBatteryDialDialog(this, intent, userHandle);
            } else {
                new UserCallIntentProcessor(this, userHandle).processIntent(new Intent(intent),
                        getCallingPackage(), true /* hasCallAppOp*/, false /* isLocalInvocation */);
            }
        } finally {
            Log.endSession();
            wakelock.release();
        }
        Log.i(this, "onCreate done");
        finish();
    }

    private void verifyCallAction(Intent intent) {
        if (getClass().getName().equals(intent.getComponent().getClassName())) {
            // If we were launched directly from the CallActivity, not one of its more privileged
            // aliases, then make sure that only the non-privileged actions are allowed.
            if (!Intent.ACTION_CALL.equals(intent.getAction())) {
                Log.w(this, "Attempt to deliver non-CALL action; forcing to CALL");
                intent.setAction(Intent.ACTION_CALL);
            }
        }
    }

    private void showLowBatteryDialDialog(Context context, final Intent intent, UserHandle userHandle) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.low_battery_warning_title);
        builder.setMessage(R.string.low_battery_warning_message);
        builder.setPositiveButton(R.string.low_battery_continue_video_call,
                new android.content.DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        new UserCallIntentProcessor(context, userHandle).processIntent(new Intent(intent),
                                getCallingPackage(), true /* hasCallAppOp*/, false /* isLocalInvocation */);
                    }
                });
        builder.setNegativeButton(R.string.low_battery_convert_to_voice_call,
                new android.content.DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        intent.putExtra(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE,
                                VideoProfile.STATE_AUDIO_ONLY);
                        new UserCallIntentProcessor(context, userHandle).processIntent(new Intent(intent),
                                getCallingPackage(), true /* hasCallAppOp*/, false /* isLocalInvocation */);
                    }
                });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        dialog.show();
    }

    private boolean isBatteryLow(Context context) {
        Log.d(this, "in isBatteryLow ");
        boolean isShowLowBattery = context.getResources().getBoolean(com.android.internal.R.bool.config_show_low_battery_dialog);
        if (isShowLowBattery) {
            Intent batteryInfoIntent = context.registerReceiver(null, new IntentFilter(
                    Intent.ACTION_BATTERY_CHANGED));
            int current = batteryInfoIntent.getIntExtra("level", 0);
            int total = batteryInfoIntent.getIntExtra("scale", 0);
            if (current * 1.0 / total <= 0.15) {
                Log.d(this, "in isBattery Low 15%");
                return true;
            } else {
                Log.d(this, "in isBatteryLow high 15%");
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public TelecomSystem getTelecomSystem() {
        return TelecomSystem.getInstance();
    }
}
