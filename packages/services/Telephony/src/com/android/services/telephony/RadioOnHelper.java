/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.services.telephony;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.WindowManager;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import com.android.phone.R;

/**
 * Helper class that implements special behavior related to emergency calls or making phone calls
 * when the radio is in the POWER_OFF STATE. Specifically, this class handles the case of the user
 * trying to dial an emergency number while the radio is off (i.e. the device is in airplane mode)
 * or a normal number while the radio is off (because of the device is on Bluetooth), by turning the
 * radio back on, waiting for it to come up, and then retrying the call.
 */
public class RadioOnHelper implements RadioOnStateListener.Callback {

    private final Context mContext;
    private RadioOnStateListener.Callback mCallback;
    private List<RadioOnStateListener> mListeners;
    private List<RadioOnStateListener> mInProgressListeners;
    private boolean mIsRadioOnCallingEnabled;

    public RadioOnHelper(Context context) {
        mContext = context;
        mInProgressListeners = new ArrayList<>(2);
    }

    private void setupListeners() {
        if (mListeners != null) {
            return;
        }
        mListeners = new ArrayList<>(2);
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            mListeners.add(new RadioOnStateListener());
        }
    }
    /**
     * Starts the "turn on radio" sequence. This is the (single) external API of the
     * RadioOnHelper class.
     *
     * This method kicks off the following sequence:
     * - Power on the radio for each Phone
     * - Listen for radio events telling us the radio has come up.
     * - Retry if we've gone a significant amount of time without any response from the radio.
     * - Finally, clean up any leftover state.
     *
     * This method is safe to call from any thread, since it simply posts a message to the
     * RadioOnHelper's handler (thus ensuring that the rest of the sequence is entirely
     * serialized, and runs on the main looper.)
     */
    public void triggerRadioOnAndListen(RadioOnStateListener.Callback callback) {
        setupListeners();
        mCallback = callback;
        mInProgressListeners.clear();
        mIsRadioOnCallingEnabled = false;
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            Phone phone = PhoneFactory.getPhone(i);
            if (phone == null) {
                continue;
            }

            mInProgressListeners.add(mListeners.get(i));
            mListeners.get(i).waitForRadioOn(phone, this);
        }

        powerOnRadio();
    }
    /**
     * Attempt to power on the radio (i.e. take the device out of airplane mode). We'll eventually
     * get an onServiceStateChanged() callback when the radio successfully comes up.
     */
    private void powerOnRadio() {
        Log.d(this, "powerOnRadio().");

        // If airplane mode is on, we turn it off the same way that the Settings activity turns it
        // off.
        if (Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) > 0) {
            Log.d(this, "==> Turning off airplane mode.");
            /* UNISOC:Telstra requirement. @{ */
            if (mContext.getResources().getBoolean(R.bool.config_tip_turn_off_airplane_mode)) {
                turnOffAirplaneModeForEcall();
                return;
            }
            /* @} */
            // Change the system setting
            Settings.Global.putInt(mContext.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0);

            // Post the broadcast intend for change in airplane mode
            // TODO: We really should not be in charge of sending this broadcast.
            // If changing the setting is sufficient to trigger all of the rest of the logic,
            // then that should also trigger the broadcast intent.
            Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", false);
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    /**
     * This method is called from multiple Listeners on the Main Looper.
     * Synchronization is not necessary.
     */
    @Override
    public void onComplete(RadioOnStateListener listener, boolean isRadioReady) {
        mIsRadioOnCallingEnabled |= isRadioReady;
        mInProgressListeners.remove(listener);
        if (mCallback != null && mInProgressListeners.isEmpty()) {
            mCallback.onComplete(null, mIsRadioOnCallingEnabled);
        }
    }

    @Override
    public boolean isOkToCall(Phone phone, int serviceState) {
        return (mCallback == null) ? false : mCallback.isOkToCall(phone, serviceState);
    }

    /* UNISOC:Telstra requirement. @{ */
    private void turnOffAirplaneModeForEcall() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.emergency_call);
        builder.setMessage(R.string.turn_off_airplane_mode_for_emergency_call);
        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(this, "==> turnOffAirplaneModeForEcall.");

                        // Change the system setting
                        Settings.Global.putInt(mContext.getContentResolver(),
                                Settings.Global.AIRPLANE_MODE_ON, 0);

                        // Post the broadcast intend for change in airplane mode
                        // TODO: We really should not be in charge of sending this broadcast.
                        // If changing the setting is sufficent to trigger all of the rest of
                        // the logic,
                        // then that should also trigger the broadcast intent.
                        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                        intent.putExtra("state", false);
                        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
    }
    /* @} */
}
