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
package com.android.settings.network;

import static android.content.Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.os.UserHandle.myUserId;
import static android.os.UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS;

import static com.android.settingslib.RestrictedLockUtils.hasBaseUserRestriction;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.Utils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;

import java.util.List;


public class MobilePlanPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnCreate, OnDestroy, OnSaveInstanceState {

    public interface MobilePlanPreferenceHost {
        void showMobilePlanMessageDialog();
        // UNISOC: Add for Bug1013381
        void dismissMobilePlanMessageDialog();
    }

    public static final int MANAGE_MOBILE_PLAN_DIALOG_ID = 1;

    private static final String TAG = "MobilePlanPrefContr";
    private static final String KEY_MANAGE_MOBILE_PLAN = "manage_mobile_plan";
    private static final String SAVED_MANAGE_MOBILE_PLAN_MSG = "mManageMobilePlanMessage";

    private final UserManager mUserManager;
    private final boolean mIsSecondaryUser;
    private final MobilePlanPreferenceHost mHost;

    private ConnectivityManager mCm;
    private TelephonyManager mTm;
    // UNISOC: Add for Bug1013381
    private Context mContext;
    private SubscriptionManager mSubMgr;
    private int mDefaultSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    private String mMobilePlanDialogMessage;

    public MobilePlanPreferenceController(Context context,
            MobilePlanPreferenceHost host) {
        super(context);
        mHost = host;
        mCm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mTm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mIsSecondaryUser = !mUserManager.isAdminUser();
        // UNISOC: Add for Bug1013381
        mContext = context;
        mSubMgr = SubscriptionManager.from(context);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (mHost != null && KEY_MANAGE_MOBILE_PLAN.equals(preference.getKey())) {
            mMobilePlanDialogMessage = null;
            onManageMobilePlanClick();
        }
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mMobilePlanDialogMessage = savedInstanceState.getString(SAVED_MANAGE_MOBILE_PLAN_MSG);
        }
        Log.d(TAG, "onCreate: mMobilePlanDialogMessage=" + mMobilePlanDialogMessage);
        // UNISOC: Add for Bug1013381
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyIntents.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED);
        filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        mContext.registerReceiver(mReceiver,filter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (!TextUtils.isEmpty(mMobilePlanDialogMessage)) {
            outState.putString(SAVED_MANAGE_MOBILE_PLAN_MSG, mMobilePlanDialogMessage);
        }
    }

    /* UNISOC: Add for Bug1013381 @{ */
    @Override
    public void onDestroy(){
        mContext.unregisterReceiver(mReceiver);
        mDefaultSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }
    /* @} */

    public String getMobilePlanDialogMessage() {
        return mMobilePlanDialogMessage;
    }

    public void setMobilePlanDialogMessage(String messasge) {
        mMobilePlanDialogMessage = messasge;
    }

    @Override
    public boolean isAvailable() {
        final boolean isPrefAllowedOnDevice = mContext.getResources().getBoolean(
                com.android.settings.R.bool.config_show_mobile_plan);
        final boolean isPrefAllowedForUser = !mIsSecondaryUser
                && !Utils.isWifiOnly(mContext)
                && !hasBaseUserRestriction(mContext, DISALLOW_CONFIG_MOBILE_NETWORKS, myUserId());
        return isPrefAllowedForUser && isPrefAllowedOnDevice;
    }
    @Override
    public String getPreferenceKey() {
        return KEY_MANAGE_MOBILE_PLAN;
    }

    private void onManageMobilePlanClick() {
        Resources resources = mContext.getResources();
        NetworkInfo ni = mCm.getActiveNetworkInfo();
        if (mTm.hasIccCard() && (ni != null)) {
            // UNISOC: Add for Bug1013381
            mDefaultSubId = mSubMgr.getDefaultSubscriptionId();
            // Check for carrier apps that can handle provisioning first
            Intent provisioningIntent = new Intent(Intent.ACTION_CARRIER_SETUP);
            List<String> carrierPackages =
                    mTm.getCarrierPackageNamesForIntent(provisioningIntent);
            if (carrierPackages != null && !carrierPackages.isEmpty()) {
                if (carrierPackages.size() != 1) {
                    Log.w(TAG, "Multiple matching carrier apps found, launching the first.");
                }
                provisioningIntent.setPackage(carrierPackages.get(0));
                mContext.startActivity(provisioningIntent);
                return;
            }

            // Get provisioning URL
            String url = mCm.getMobileProvisioningUrl();
            if (!TextUtils.isEmpty(url)) {
                Intent intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN,
                        Intent.CATEGORY_APP_BROWSER);
                intent.setData(Uri.parse(url));
                intent.setFlags(FLAG_ACTIVITY_BROUGHT_TO_FRONT | FLAG_ACTIVITY_NEW_TASK);
                try {
                    mContext.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.w(TAG, "onManageMobilePlanClick: startActivity failed" + e);
                }
            } else {
                // No provisioning URL
                String operatorName = mTm.getSimOperatorName();
                if (TextUtils.isEmpty(operatorName)) {
                    // Use NetworkOperatorName as second choice in case there is no
                    // SPN (Service Provider Name on the SIM). Such as with T-mobile.
                    operatorName = mTm.getNetworkOperatorName();
                    if (TextUtils.isEmpty(operatorName)) {
                        mMobilePlanDialogMessage =
                                resources.getString(R.string.mobile_unknown_sim_operator);
                    } else {
                        mMobilePlanDialogMessage = resources.getString(
                                R.string.mobile_no_provisioning_url, operatorName);
                    }
                } else {
                    mMobilePlanDialogMessage =
                            resources.getString(R.string.mobile_no_provisioning_url, operatorName);
                }
            }
        } else if (mTm.hasIccCard() == false) {
            // No sim card
            mMobilePlanDialogMessage = resources.getString(R.string.mobile_insert_sim_card);
        } else {
            // NetworkInfo is null, there is no connection
            mMobilePlanDialogMessage = resources.getString(R.string.mobile_connect_to_internet);
        }
        if (!TextUtils.isEmpty(mMobilePlanDialogMessage)) {
            Log.d(TAG, "onManageMobilePlanClick: message=" + mMobilePlanDialogMessage);
            if (mHost != null) {
                mHost.showMobilePlanMessageDialog();
            } else {
                Log.d(TAG, "Missing host fragment, cannot show message dialog.");
            }
        }
    }

    /* UNISOC: Bug1013381 Dismiss dialog when sim removed or default voice subId change. @{*/
    private final BroadcastReceiver mReceiver = new BroadcastReceiver (){
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: intent=" + intent);
            final String action = intent.getAction();
            switch (action) {
            case TelephonyIntents.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED:
                int voiceSubId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,-1);
                if (voiceSubId != mDefaultSubId && mHost != null){
                    mHost.dismissMobilePlanMessageDialog();
                }
                break;
            case TelephonyIntents.ACTION_SIM_STATE_CHANGED:
                String state = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                int subId = intent.getIntExtra(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,-1);
                if (subId == mDefaultSubId && state.equals(IccCardConstants.INTENT_VALUE_ICC_ABSENT)
                        && mHost != null){
                    mHost.dismissMobilePlanMessageDialog();
                }
                break;
            default:
                break;
            }
        }
    };
    /* @} */
}
