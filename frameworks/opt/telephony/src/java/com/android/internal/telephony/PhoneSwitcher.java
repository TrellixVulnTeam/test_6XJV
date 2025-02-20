/*
* Copyright (C) 2015 The Android Open Source Project
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

package com.android.internal.telephony;

import static android.telephony.SubscriptionManager.INVALID_PHONE_INDEX;
import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.MatchAllNetworkSpecifier;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.StringNetworkSpecifier;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.telephony.PhoneStateListener;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.LocalLog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.PhoneConstants.State;
import com.android.internal.telephony.dataconnection.DcRequest;
import com.android.internal.util.IndentingPrintWriter;
import com.android.ims.internal.ImsManagerEx;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Utility singleton to monitor subscription changes and incoming NetworkRequests
 * and determine which phone/phones are active.
 *
 * Manages the ALLOW_DATA calls to modems and notifies phones about changes to
 * the active phones.  Note we don't wait for data attach (which may not happen anyway).
 */
public class PhoneSwitcher extends Handler {
    private final static String LOG_TAG = "PhoneSwitcher";
    private final static boolean VDBG = false;

    private final int mMaxActivePhones;
    private final List<DcRequest> mPrioritizedDcRequests = new ArrayList<DcRequest>();
    private final RegistrantList[] mActivePhoneRegistrants;
    private final SubscriptionController mSubscriptionController;
    private final int[] mPhoneSubscriptions;
    private final CommandsInterface[] mCommandsInterfaces;
    private final Context mContext;
    private final PhoneState[] mPhoneStates;
    private final int mNumPhones;
    private final Phone[] mPhones;
    private final LocalLog mLocalLog;
    private int mDefaultDataSubscription;
    private PhoneStateListener phoneListener = null;
    private int lastCallState = TelephonyManager.CALL_STATE_IDLE;
    private final List<DcRequest> mPendingReq = new ArrayList<DcRequest>();
    private HashMap<Phone, State> mPhoneCallStates = new HashMap<Phone, State>();
    private int mOldDataCardSubId = -1;
    private int mSubIdForReleaseRequest;
    private boolean mDualLteModem = false;

    private final static int EVENT_DEFAULT_SUBSCRIPTION_CHANGED = 101;
    private final static int EVENT_SUBSCRIPTION_CHANGED         = 102;
    private final static int EVENT_REQUEST_NETWORK              = 103;
    private final static int EVENT_RELEASE_NETWORK              = 104;
    private final static int EVENT_EMERGENCY_TOGGLE             = 105;
    private final static int EVENT_RESEND_DATA_ALLOWED          = 106;
    private final static int EVENT_DATA_ATTACHED                = 107;
    private final static int EVENT_RADIO_ON                     = 108;

    private final static int MAX_LOCAL_LOG_LINES = 30;

    @VisibleForTesting
    public PhoneSwitcher(Looper looper) {
        super(looper);
        mMaxActivePhones = 0;
        mSubscriptionController = null;
        mPhoneSubscriptions = null;
        mCommandsInterfaces = null;
        mContext = null;
        mPhoneStates = null;
        mPhones = null;
        mLocalLog = null;
        mActivePhoneRegistrants = null;
        mNumPhones = 0;
    }

    public PhoneSwitcher(int maxActivePhones, int numPhones, Context context,
            SubscriptionController subscriptionController, Looper looper, ITelephonyRegistry tr,
            CommandsInterface[] cis, Phone[] phones) {
        super(looper);
        mContext = context;
        mNumPhones = numPhones;
        mPhones = phones;
        mPhoneSubscriptions = new int[numPhones];
        mMaxActivePhones = maxActivePhones;
        mLocalLog = new LocalLog(MAX_LOCAL_LOG_LINES);

        mSubscriptionController = subscriptionController;

        mActivePhoneRegistrants = new RegistrantList[numPhones];
        mPhoneStates = new PhoneState[numPhones];
        for (int i = 0; i < numPhones; i++) {
            mActivePhoneRegistrants[i] = new RegistrantList();
            mPhoneStates[i] = new PhoneState();
            if (mPhones[i] != null) {
                mPhones[i].registerForEmergencyCallToggle(this, EVENT_EMERGENCY_TOGGLE, null);
            }
            mPhones[i].getServiceStateTracker().registerForDataConnectionAttached(this,
                    EVENT_DATA_ATTACHED, null);
        }
        mDualLteModem = ImsManagerEx.isDualLteModem();
        mCommandsInterfaces = cis;

        try {
            tr.addOnSubscriptionsChangedListener(context.getOpPackageName(),
                    mSubscriptionsChangedListener);
        } catch (RemoteException e) {
        }

        mContext.registerReceiver(mDefaultDataChangedReceiver,
                new IntentFilter(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED));

        NetworkCapabilities netCap = new NetworkCapabilities();
        netCap.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_MMS);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_SUPL);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_DUN);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_FOTA);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_IMS);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_CBS);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_IA);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_RCS);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_XCAP);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_EIMS);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        netCap.setNetworkSpecifier(new MatchAllNetworkSpecifier());

        NetworkFactory networkFactory = new PhoneSwitcherNetworkRequestListener(looper, context,
                netCap, this);
        // we want to see all requests
        networkFactory.setScoreFilter(101);
        networkFactory.register();
        /* UNISOC: Bug 615971 Don't process network request during call@{ */
        phoneListener = new PhoneStateListener(){
            public void onCallStateChanged(int state, String incomingNumber) {
                Phone[] phones = PhoneFactory.getPhones();
                for (Phone phone : phones) {
                    onVoiceCallStateChange(phone);
                }
            }
        };
        TelephonyManager mTm = TelephonyManager.from(mContext);
        mTm.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        /* @} */
        log("PhoneSwitcher started");
    }

    private final BroadcastReceiver mDefaultDataChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Message msg = PhoneSwitcher.this.obtainMessage(EVENT_DEFAULT_SUBSCRIPTION_CHANGED);
            msg.sendToTarget();
        }
    };

    private final IOnSubscriptionsChangedListener mSubscriptionsChangedListener =
            new IOnSubscriptionsChangedListener.Stub() {
        @Override
        public void onSubscriptionsChanged() {
            Message msg = PhoneSwitcher.this.obtainMessage(EVENT_SUBSCRIPTION_CHANGED);
            msg.sendToTarget();
        }
    };

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_SUBSCRIPTION_CHANGED: {
                onEvaluate(REQUESTS_UNCHANGED, "subChanged");
                break;
            }
            case EVENT_DEFAULT_SUBSCRIPTION_CHANGED: {
                onEvaluate(REQUESTS_UNCHANGED, "defaultChanged");
                break;
            }
            case EVENT_REQUEST_NETWORK: {
                onRequestNetwork((NetworkRequest)msg.obj);
                break;
            }
            case EVENT_RELEASE_NETWORK: {
                onReleaseNetwork((NetworkRequest)msg.obj);
                break;
            }
            case EVENT_EMERGENCY_TOGGLE: {
                onEvaluate(REQUESTS_CHANGED, "emergencyToggle");
                break;
            }
            case EVENT_RESEND_DATA_ALLOWED: {
                onResendDataAllowed(msg);
                break;
            }
            case EVENT_DATA_ATTACHED: {
                onDataconnectionAttached();
                break;
            }
        }
    }

    private boolean isEmergency() {
        for (Phone p : mPhones) {
            if (p == null) continue;
            if (p.isInEcm() || p.isInEmergencyCall()) return true;
        }
        return false;
    }

    private static class PhoneSwitcherNetworkRequestListener extends NetworkFactory {
        private final PhoneSwitcher mPhoneSwitcher;
        public PhoneSwitcherNetworkRequestListener (Looper l, Context c,
                NetworkCapabilities nc, PhoneSwitcher ps) {
            super(l, c, "PhoneSwitcherNetworkRequstListener", nc);
            mPhoneSwitcher = ps;
        }

        @Override
        protected void needNetworkFor(NetworkRequest networkRequest, int score) {
            if (VDBG) log("needNetworkFor " + networkRequest + ", " + score);
            Message msg = mPhoneSwitcher.obtainMessage(EVENT_REQUEST_NETWORK);
            msg.obj = networkRequest;
            msg.sendToTarget();
        }

        @Override
        protected void releaseNetworkFor(NetworkRequest networkRequest) {
            if (VDBG) log("releaseNetworkFor " + networkRequest);
            Message msg = mPhoneSwitcher.obtainMessage(EVENT_RELEASE_NETWORK);
            msg.obj = networkRequest;
            msg.sendToTarget();
        }
    }

    private void onRequestNetwork(NetworkRequest networkRequest) {
        final DcRequest dcRequest = new DcRequest(networkRequest, mContext);
        if (mPrioritizedDcRequests.contains(dcRequest) == false) {
            /* UNISOC:
             * Bug 615971 Don't process network request
             * during call Bug 644789 change to here @{ */
            int phoneIdForRequest = phoneIdForRequest(networkRequest);
            if (SubscriptionManager.isValidPhoneId(phoneIdForRequest)
                    && isOtherPhoneCalling(phoneIdForRequest)) {
                if (mPendingReq.contains(dcRequest) == false) {
                    mPendingReq.add(dcRequest);
                } else {
                    log ("mPendingReq already have request" + dcRequest);
                }
                return;
            }
            /* @} */
            mPrioritizedDcRequests.add(dcRequest);
            Collections.sort(mPrioritizedDcRequests);
             /*UNISOC:
              * for bug630092 do not set up data call on vice sim card
              * during volte call @{*/
            int defaultDataSubId = mSubscriptionController.getDefaultDataSubId();
            int subIdForequest = subIdForRequest(networkRequest);
            if (!mDualLteModem && ImsManagerEx.isDualVoLTEActive() &&
                    SubscriptionManager.isValidSubscriptionId(defaultDataSubId) &&
                    SubscriptionManager.isValidSubscriptionId(subIdForequest)) {
                if (subIdForequest != defaultDataSubId) {
                    if (!isPhoneStateIdle(phoneIdForRequest)) {//whether have volte call
                        if (mPendingReq.contains(dcRequest) == false) {
                            mPendingReq.add(dcRequest);
                        } else {
                            log ("mPendingReq already have request" + dcRequest);
                        }
                        return;
                    }
                    if (isMmsRequest(networkRequest) &&
                            needSwitchPrimaryCardForMMS(subIdForequest, defaultDataSubId)) {
                        log("need change primary card for mms");
                        if (mPendingReq.contains(dcRequest) == false) {
                            log ("pending request!");
                            mPendingReq.add(dcRequest);
                        }
                        onSwitchPrimaryCard(subIdForequest, defaultDataSubId);
                        return;
                    }
                }
            }
            /* @} */
            /*UNISOC: for 731286 @{*/
            if (isMmsRequest(networkRequest)) {
                SystemProperties.set("vendor.ril.mms.active", "1");
            }
            /* @} */
            onEvaluate(REQUESTS_CHANGED, "netRequest");
        }
    }

    private void onReleaseNetwork(NetworkRequest networkRequest) {
        final DcRequest dcRequest = new DcRequest(networkRequest, mContext);
        int subIdForequest = subIdForRequest(networkRequest);
        mPendingReq.remove(dcRequest);
        /*UNISOC: for 731286 @{*/
        if (isMmsRequest(networkRequest)) {
            SystemProperties.set("vendor.ril.mms.active", "0");
        }
        /* @} */
        boolean isAllPhoneStateIdle = true;
        for (int i = 0; i < mNumPhones; i++) {
            if (!isPhoneStateIdle(i)) {
                isAllPhoneStateIdle = false;
            }
        }
        if (mPrioritizedDcRequests.remove(dcRequest)) {
            if (ImsManagerEx.isDualVoLTEActive() &&
                    isMmsRequest(networkRequest) && !isAllPhoneStateIdle) {
                log("do not switch primary card during calling");
                mSubIdForReleaseRequest = subIdForequest;
                return;
            }
            onEvaluate(REQUESTS_CHANGED, "netReleased");
        }
        if (ImsManagerEx.isDualVoLTEActive() &&
                isMmsRequest(networkRequest) &&
                SubscriptionManager.isValidSubscriptionId(mOldDataCardSubId)) {
            if (mOldDataCardSubId != subIdForequest){
                onSwitchPrimaryCard(mOldDataCardSubId, -1);
            }
        }
    }

    private static final boolean REQUESTS_CHANGED   = true;
    private static final boolean REQUESTS_UNCHANGED = false;
    /**
     * Re-evaluate things.
     * Do nothing if nothing's changed.
     *
     * Otherwise, go through the requests in priority order adding their phone
     * until we've added up to the max allowed.  Then go through shutting down
     * phones that aren't in the active phone list.  Finally, activate all
     * phones in the active phone list.
     */
    private void onEvaluate(boolean requestsChanged, String reason) {
        StringBuilder sb = new StringBuilder(reason);
        if (isEmergency()) {
            log("onEvalute aborted due to Emergency");
            return;
        }

        boolean diffDetected = requestsChanged;
        final int dataSub = mSubscriptionController.getDefaultDataSubId();
        if (dataSub != mDefaultDataSubscription) {
            sb.append(" default ").append(mDefaultDataSubscription).append("->").append(dataSub);
            mDefaultDataSubscription = dataSub;
            diffDetected = true;

        }

        for (int i = 0; i < mNumPhones; i++) {
            int sub = mSubscriptionController.getSubIdUsingPhoneId(i);
            if (sub != mPhoneSubscriptions[i]) {
                sb.append(" phone[").append(i).append("] ").append(mPhoneSubscriptions[i]);
                sb.append("->").append(sub);
                mPhoneSubscriptions[i] = sub;
                diffDetected = true;
            }
        }

        if (diffDetected) {
            log("evaluating due to " + sb.toString());

            List<Integer> newActivePhones = new ArrayList<Integer>();

            for (DcRequest dcRequest : mPrioritizedDcRequests) {
                int phoneIdForRequest = phoneIdForRequest(dcRequest.networkRequest);
                if (phoneIdForRequest == INVALID_PHONE_INDEX) continue;
                if (newActivePhones.contains(phoneIdForRequest)) continue;
                newActivePhones.add(phoneIdForRequest);
                if (newActivePhones.size() >= mMaxActivePhones) break;
            }

            if (VDBG) {
                log("default subId = " + mDefaultDataSubscription);
                for (int i = 0; i < mNumPhones; i++) {
                    log(" phone[" + i + "] using sub[" + mPhoneSubscriptions[i] + "]");
                }
                log(" newActivePhones:");
                for (Integer i : newActivePhones) log("  " + i);
            }

            for (int phoneId = 0; phoneId < mNumPhones; phoneId++) {
                if (newActivePhones.contains(phoneId) == false) {
                    deactivate(phoneId);
                }
            }

            // only activate phones up to the limit
            for (int phoneId : newActivePhones) {
                activate(phoneId);
            }
        }
    }

    private static class PhoneState {
        public volatile boolean active = false;
        public long lastRequested = 0;
    }

    private void deactivate(int phoneId) {
        PhoneState state = mPhoneStates[phoneId];
        if (state.active == false) return;
        state.active = false;
        log("deactivate " + phoneId);
        state.lastRequested = System.currentTimeMillis();
        // Skip ALLOW_DATA for single SIM device
        if (mNumPhones > 1) {
            mCommandsInterfaces[phoneId].setDataAllowed(false, null);
        }
        mActivePhoneRegistrants[phoneId].notifyRegistrants();
    }

    private void activate(int phoneId) {
        PhoneState state = mPhoneStates[phoneId];
        if (state.active == true) return;
        state.active = true;
        log("activate " + phoneId);
        state.lastRequested = System.currentTimeMillis();
        // Skip ALLOW_DATA for single SIM device
        if (mNumPhones > 1) {
            mCommandsInterfaces[phoneId].setDataAllowed(true, null);
        }
        mActivePhoneRegistrants[phoneId].notifyRegistrants();
    }

    // used when the modem may have been rebooted and we want to resend
    // setDataAllowed
    public void resendDataAllowed(int phoneId) {
        validatePhoneId(phoneId);
        Message msg = obtainMessage(EVENT_RESEND_DATA_ALLOWED);
        msg.arg1 = phoneId;
        msg.sendToTarget();
    }

    private void onResendDataAllowed(Message msg) {
        final int phoneId = msg.arg1;
        // Skip ALLOW_DATA for single SIM device
        if (mNumPhones > 1) {
            mCommandsInterfaces[phoneId].setDataAllowed(mPhoneStates[phoneId].active, null);
        }
    }

    private int phoneIdForRequest(NetworkRequest netRequest) {
        NetworkSpecifier specifier = netRequest.networkCapabilities.getNetworkSpecifier();
        int subId;

        if (specifier == null) {
            subId = mDefaultDataSubscription;
        } else if (specifier instanceof StringNetworkSpecifier) {
            try {
                subId = Integer.parseInt(((StringNetworkSpecifier) specifier).specifier);
            } catch (NumberFormatException e) {
                Rlog.e(LOG_TAG, "NumberFormatException on "
                        + ((StringNetworkSpecifier) specifier).specifier);
                subId = INVALID_SUBSCRIPTION_ID;
            }
        } else {
            subId = INVALID_SUBSCRIPTION_ID;
        }

        int phoneId = INVALID_PHONE_INDEX;
        if (subId == INVALID_SUBSCRIPTION_ID) return phoneId;

        for (int i = 0 ; i < mNumPhones; i++) {
            if (mPhoneSubscriptions[i] == subId) {
                phoneId = i;
                break;
            }
        }
        return phoneId;
    }

    public boolean isPhoneActive(int phoneId) {
        validatePhoneId(phoneId);
        return mPhoneStates[phoneId].active;
    }

    public void registerForActivePhoneSwitch(int phoneId, Handler h, int what, Object o) {
        validatePhoneId(phoneId);
        Registrant r = new Registrant(h, what, o);
        mActivePhoneRegistrants[phoneId].add(r);
        r.notifyRegistrant();
    }

    public void unregisterForActivePhoneSwitch(int phoneId, Handler h) {
        validatePhoneId(phoneId);
        mActivePhoneRegistrants[phoneId].remove(h);
    }

    private void validatePhoneId(int phoneId) {
        if (phoneId < 0 || phoneId >= mNumPhones) {
            throw new IllegalArgumentException("Invalid PhoneId");
        }
    }
    /* UNISOC: Bug 615971 Don't process network request during call @{ */
    private boolean isOtherPhoneCalling(int requestPhoneId) {
        for (Phone p : mPhones) {
            if (p.getState() != PhoneConstants.State.IDLE && p.getPhoneId() != requestPhoneId) {
                Rlog.d(LOG_TAG, "Phone " + p.getPhoneId() + " is in call");
                return true;
            }
        }
        return false;
    }

    public void onVoiceCallStateChange(Phone p) {
        State state = p.getState();
        State oldState = mPhoneCallStates.get(p);
        log("old state is " + oldState + " new state is " + state );
        if (oldState != state) {
            mPhoneCallStates.put(p, state);
            if (oldState == State.IDLE) {
                onVoiceCallStarted(p);
            } else if (state == State.IDLE) {
                onVoiceCallEnded(p);
            }
        }
    }

    private void onVoiceCallStarted(Phone phone) {
        //TO DO
    }

    private void onVoiceCallEnded(Phone phone) {
        processSwitchPriamryCardAfterCall();
        log("on call end, eval pending request");
        evalPendingRequest();
    }

    private void evalPendingRequest(){
        Collections.sort(mPendingReq);
        for(int i=0; i<mPendingReq.size(); i++) {
            onRequestNetwork(mPendingReq.get(i).networkRequest);
        }
    }
    /* @} */

    boolean isMmsRequest(NetworkRequest networkRequest){
        return networkRequest.networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_MMS);
    }

    private void processSwitchPriamryCardAfterCall(){
        boolean needSwitch = false;
        DcRequest request = mPrioritizedDcRequests.get(0);
        int requestSubId = subIdForRequest(request.networkRequest);
        if (!(isMmsRequest(request.networkRequest) && requestSubId == mSubIdForReleaseRequest)) {
            needSwitch = true;
        }
        if (ImsManagerEx.isDualVoLTEActive() &&
                needSwitch &&
                SubscriptionManager.isValidSubscriptionId(mOldDataCardSubId)) {
            if (mOldDataCardSubId != mSubIdForReleaseRequest){
                log("on call end,switch primary card after release mms request");
                onSwitchPrimaryCard(mOldDataCardSubId, -1);
            }
        }
    }
    /*UNISOC: add for mms business for DSDA @{*/
    private boolean needSwitchPrimaryCardForMMS(int requestSubId, int defaultDataSubId){
        boolean ret = false;
        int requestPhoneId = mSubscriptionController.getPhoneId(requestSubId);
        int defaultDataPhoneId = mSubscriptionController.getPhoneId(defaultDataSubId);
        if (SubscriptionManager.isValidPhoneId(defaultDataPhoneId)) {
            ServiceState serviceState = mPhones[defaultDataPhoneId].getServiceState();
            boolean priImsEnabled = SystemProperties.getBoolean("gsm.ims.enable" + defaultDataPhoneId, false);
            boolean secImsEnabled = SystemProperties.getBoolean("gsm.ims.enable" + requestPhoneId, false);
            if (serviceState.getDataRegState() == ServiceState.STATE_IN_SERVICE &&
                    serviceState.getRilDataRadioTechnology() == ServiceState.RIL_RADIO_TECHNOLOGY_LTE &&
                    priImsEnabled && secImsEnabled) {
                ret = true;
            } else {
                log("radioTec = " + serviceState.getRilDataRadioTechnology() + ", priImsRegistered = " +
                        priImsEnabled + ", secImsRegistered = " + secImsEnabled);
            }
        }
        return ret;
    }

    private void onSwitchPrimaryCard(int requestSubId, int oldDataCardSubId) {
        log("requestSubId = " + requestSubId + ", oldDataCardSubId = " + oldDataCardSubId);
        mSubscriptionController.setDefaultDataSubId(requestSubId);
        mOldDataCardSubId = oldDataCardSubId;
    }

    private void onDataconnectionAttached() {
        if (!mDualLteModem && ImsManagerEx.isDualVoLTEActive()) {
            evalPendingRequest();
        }
    }

    private int subIdForRequest(NetworkRequest netRequest){
        NetworkSpecifier specifier = netRequest.networkCapabilities.getNetworkSpecifier();
        int subId;
        if (specifier == null) {
            subId = mDefaultDataSubscription;
        } else if (specifier instanceof StringNetworkSpecifier) {
            try {
                subId = Integer.parseInt(((StringNetworkSpecifier) specifier).specifier);
            } catch (NumberFormatException e) {
                Rlog.e(LOG_TAG, "NumberFormatException on "
                        + ((StringNetworkSpecifier) specifier).specifier);
                subId = INVALID_SUBSCRIPTION_ID;
            }
        } else {
            subId = INVALID_SUBSCRIPTION_ID;
        }
        return subId;
    }

    private boolean isPhoneStateIdle(int phoneId){
        for (Phone p : mPhones) {
            if (p.getPhoneId() == phoneId && p.getState() != PhoneConstants.State.IDLE){
                log("Phone " + phoneId + " is in call");
                return false;
            }
        }
        return true;
    }
    /* @} */
    private void log(String l) {
        Rlog.d(LOG_TAG, l);
        mLocalLog.log(l);
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        final IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        pw.println("PhoneSwitcher:");
        Calendar c = Calendar.getInstance();
        for (int i = 0; i < mNumPhones; i++) {
            PhoneState ps = mPhoneStates[i];
            c.setTimeInMillis(ps.lastRequested);
            pw.println("PhoneId(" + i + ") active=" + ps.active + ", lastRequest=" +
                    (ps.lastRequested == 0 ? "never" :
                     String.format("%tm-%td %tH:%tM:%tS.%tL", c, c, c, c, c, c)));
        }
        pw.increaseIndent();
        mLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
    }
}
