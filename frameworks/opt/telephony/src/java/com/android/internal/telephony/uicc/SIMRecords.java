/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.internal.telephony.uicc;

import android.content.Context;
import android.content.res.Resources;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Message;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;

import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.SmsConstants;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TeleUtils;
import com.android.internal.telephony.gsm.SimTlv;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.TelephonyIntentsEx;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * {@hide}
 */
public class SIMRecords extends IccRecords {
    protected static final String LOG_TAG = "SIMRecords";

    private static final boolean CRASH_RIL = false;

    private static final boolean VDBG = false;

    // ***** Instance Variables

    VoiceMailConstants mVmConfig;

    // ***** Cached SIM State; cleared on channel close

    private int mCallForwardingStatus = CALL_FORWARDING_STATUS_UNKNOWN;//UNISOC: add for bug950781


    /**
     * States only used by getSpnFsm FSM
     */
    private GetSpnFsmState mSpnState;

    /** CPHS service information (See CPHS 4.2 B.3.1.1)
     *  It will be set in onSimReady if reading GET_CPHS_INFO successfully
     *  mCphsInfo[0] is CPHS Phase
     *  mCphsInfo[1] and mCphsInfo[2] is CPHS Service Table
     */
    private byte[] mCphsInfo = null;
    boolean mCspPlmnEnabled = true;

    byte[] mEfMWIS = null;
    byte[] mEfCPHS_MWI =null;
    byte[] mEfCff = null;
    byte[] mEfCfis = null;

    byte[] mEfLi = null;
    byte[] mEfPl = null;

    int mSpnDisplayCondition;
    // Numeric network codes listed in TS 51.011 EF[SPDI]
    ArrayList<String> mSpdiNetworks = null;

    UsimServiceTable mUsimServiceTable;

    // SPRD: Add cache of original SPN value.
    String mOriginalSpn = null;

    @Override
    public String toString() {
        return "SimRecords: " + super.toString()
                + " mVmConfig" + mVmConfig
                + " callForwardingEnabled=" + mCallForwardingStatus
                + " spnState=" + mSpnState
                + " mCphsInfo=" + mCphsInfo
                + " mCspPlmnEnabled=" + mCspPlmnEnabled
                + " efMWIS=" + mEfMWIS
                + " efCPHS_MWI=" + mEfCPHS_MWI
                + " mEfCff=" + mEfCff
                + " mEfCfis=" + mEfCfis
                + " getOperatorNumeric=" + getOperatorNumeric();
    }

    // ***** Constants

    // From TS 51.011 EF[SPDI] section
    static final int TAG_SPDI = 0xA3;
    static final int TAG_SPDI_PLMN_LIST = 0x80;

    // Full Name IEI from TS 24.008
    static final int TAG_FULL_NETWORK_NAME = 0x43;

    // Short Name IEI from TS 24.008
    static final int TAG_SHORT_NETWORK_NAME = 0x45;

    // active CFF from CPHS 4.2 B.4.5
    static final int CFF_UNCONDITIONAL_ACTIVE = 0x0a;
    static final int CFF_UNCONDITIONAL_DEACTIVE = 0x05;
    static final int CFF_LINE1_MASK = 0x0f;
    static final int CFF_LINE1_RESET = 0xf0;

    // CPHS Service Table (See CPHS 4.2 B.3.1)
    private static final int CPHS_SST_MBN_MASK = 0x30;
    private static final int CPHS_SST_MBN_ENABLED = 0x30;

    // EF_CFIS related constants
    // Spec reference TS 51.011 section 10.3.46.
    private static final int CFIS_BCD_NUMBER_LENGTH_OFFSET = 2;
    private static final int CFIS_TON_NPI_OFFSET = 3;
    private static final int CFIS_ADN_CAPABILITY_ID_OFFSET = 14;
    private static final int CFIS_ADN_EXTENSION_ID_OFFSET = 15;

    // ***** Event Constants
    private static final int SIM_RECORD_EVENT_BASE = 0x00;
    private static final int EVENT_GET_IMSI_DONE = 3 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_ICCID_DONE = 4 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_MBI_DONE = 5 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_MBDN_DONE = 6 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_MWIS_DONE = 7 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_VOICE_MAIL_INDICATOR_CPHS_DONE = 8 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_AD_DONE = 9 + SIM_RECORD_EVENT_BASE; // Admin data on SIM
    private static final int EVENT_GET_MSISDN_DONE = 10 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_CPHS_MAILBOX_DONE = 11 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_SPN_DONE = 12 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_SPDI_DONE = 13 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_UPDATE_DONE = 14 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_PNN_DONE = 15 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_SST_DONE = 17 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_ALL_SMS_DONE = 18 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_MARK_SMS_READ_DONE = 19 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_SET_MBDN_DONE = 20 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_SMS_ON_SIM = 21 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_SMS_DONE = 22 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_CFF_DONE = 24 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_SET_CPHS_MAILBOX_DONE = 25 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_INFO_CPHS_DONE = 26 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_SET_MSISDN_DONE = 30 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_CFIS_DONE = 32 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_CSP_CPHS_DONE = 33 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_GID1_DONE = 34 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_GID2_DONE = 36 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_PLMN_W_ACT_DONE = 37 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_OPLMN_W_ACT_DONE = 38 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_HPLMN_W_ACT_DONE = 39 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_EHPLMN_DONE = 40 + SIM_RECORD_EVENT_BASE;
    private static final int EVENT_GET_FPLMN_DONE = 41 + SIM_RECORD_EVENT_BASE;

    // TODO: Possibly move these to IccRecords.java
    private static final int SYSTEM_EVENT_BASE = 0x100;
    private static final int EVENT_APP_LOCKED = 2 + SYSTEM_EVENT_BASE;
    private static final int EVENT_APP_NETWORK_LOCKED = 3 + SYSTEM_EVENT_BASE;


    // Lookup table for carriers known to produce SIMs which incorrectly indicate MNC length.

    private static final String[] MCCMNC_CODES_HAVING_3DIGITS_MNC = {
        "302370", "302720", "310260",
        "405025", "405026", "405027", "405028", "405029", "405030", "405031", "405032",
        "405033", "405034", "405035", "405036", "405037", "405038", "405039", "405040",
        "405041", "405042", "405043", "405044", "405045", "405046", "405047", "405750",
        "405751", "405752", "405753", "405754", "405755", "405756", "405799", "405800",
        "405801", "405802", "405803", "405804", "405805", "405806", "405807", "405808",
        "405809", "405810", "405811", "405812", "405813", "405814", "405815", "405816",
        "405817", "405818", "405819", "405820", "405821", "405822", "405823", "405824",
        "405825", "405826", "405827", "405828", "405829", "405830", "405831", "405832",
        "405833", "405834", "405835", "405836", "405837", "405838", "405839", "405840",
        "405841", "405842", "405843", "405844", "405845", "405846", "405847", "405848",
        "405849", "405850", "405851", "405852", "405853", "405854", "405855", "405856",
        "405857", "405858", "405859", "405860", "405861", "405862", "405863", "405864",
        "405865", "405866", "405867", "405868", "405869", "405870", "405871", "405872",
        "405873", "405874", "405875", "405876", "405877", "405878", "405879", "405880",
        "405881", "405882", "405883", "405884", "405885", "405886", "405908", "405909",
        "405910", "405911", "405912", "405913", "405914", "405915", "405916", "405917",
        "405918", "405919", "405920", "405921", "405922", "405923", "405924", "405925",
        "405926", "405927", "405928", "405929", "405930", "405931", "405932", "502142",
        "502143", "502145", "502146", "502147", "502148"
    };

    // ***** Constructor

    public SIMRecords(UiccCardApplication app, Context c, CommandsInterface ci) {
        super(app, c, ci);

        /* SPRD: Add for bug693265, AndroidP porting for USIM/SIM phonebook @{ */
        //mAdnCache = new AdnRecordCache(mFh);
        mAdnCache = new AdnRecordCacheEx(mFh);
        /* @} */

        mVmConfig = new VoiceMailConstants();

        mRecordsRequested = false;  // No load request is made till SIM ready
        mLockedRecordsReqReason = LOCKED_RECORDS_REQ_REASON_NONE;

        // recordsToLoad is set to 0 because no requests are made yet
        mRecordsToLoad = 0;

        mCi.setOnSmsOnSim(this, EVENT_SMS_ON_SIM, null);

        // Start off by setting empty state
        resetRecords();
        mParentApp.registerForReady(this, EVENT_APP_READY, null);
        mParentApp.registerForLocked(this, EVENT_APP_LOCKED, null);
        mParentApp.registerForNetworkLocked(this, EVENT_APP_NETWORK_LOCKED, null);
        if (DBG) log("SIMRecords X ctor this=" + this);
    }

    @Override
    public void dispose() {
        if (DBG) log("Disposing SIMRecords this=" + this);
        //Unregister for all events
        mCi.unSetOnSmsOnSim(this);
        mParentApp.unregisterForReady(this);
        mParentApp.unregisterForLocked(this);
        mParentApp.unregisterForNetworkLocked(this);
        resetRecords();
        super.dispose();
    }

    @Override
    protected void finalize() {
        if (DBG) log("finalized");
    }

    protected void resetRecords() {
        mImsi = null;
        mMsisdn = null;
        mVoiceMailNum = null;
        mMncLength = UNINITIALIZED;
        log("setting0 mMncLength" + mMncLength);
        mIccId = null;
        mFullIccId = null;
        // -1 means no EF_SPN found; treat accordingly.
        mSpnDisplayCondition = -1;
        mEfMWIS = null;
        mEfCPHS_MWI = null;
        mSpdiNetworks = null;
        mPnnHomeName = null;
        mGid1 = null;
        mGid2 = null;
        mPlmnActRecords = null;
        mOplmnActRecords = null;
        mHplmnActRecords = null;
        mFplmns = null;
        mEhplmns = null;

        /* SPRD: Add for bug693265, AndroidP porting for USIM/SIM phonebook @{ */
        //mAdnCache.reset();
        /* @} */

        log("SIMRecords: onRadioOffOrNotAvailable set 'gsm.sim.operator.numeric' to operator=null");
        log("update icc_operator_numeric=" + null);
        mTelephonyManager.setSimOperatorNumericForPhone(mParentApp.getPhoneId(), "");
        mTelephonyManager.setSimOperatorNameForPhone(mParentApp.getPhoneId(), "");
        mTelephonyManager.setSimCountryIsoForPhone(mParentApp.getPhoneId(), "");

        // recordsRequested is set to false indicating that the SIM
        // read requests made so far are not valid. This is set to
        // true only when fresh set of read requests are made.
        mRecordsRequested = false;
        mLockedRecordsReqReason = LOCKED_RECORDS_REQ_REASON_NONE;
        mLoaded.set(false);
    }

    //***** Public Methods

    @Override
    public String getMsisdnNumber() {
        return mMsisdn;
    }

    @Override
    public UsimServiceTable getUsimServiceTable() {
        return mUsimServiceTable;
    }

    private int getExtFromEf(int ef) {
        int ext;
        switch (ef) {
            case EF_MSISDN:
                /* For USIM apps use EXT5. (TS 31.102 Section 4.2.37) */
                if (mParentApp.getType() == AppType.APPTYPE_USIM) {
                    ext = EF_EXT5;
                } else {
                    ext = EF_EXT1;
                }
                break;
            default:
                ext = EF_EXT1;
        }
        return ext;
    }

    /**
     * Set subscriber number to SIM record
     *
     * The subscriber number is stored in EF_MSISDN (TS 51.011)
     *
     * When the operation is complete, onComplete will be sent to its handler
     *
     * @param alphaTag alpha-tagging of the dailing nubmer (up to 10 characters)
     * @param number dialing number (up to 20 digits)
     *        if the number starts with '+', then set to international TOA
     * @param onComplete
     *        onComplete.obj will be an AsyncResult
     *        ((AsyncResult)onComplete.obj).exception == null on success
     *        ((AsyncResult)onComplete.obj).exception != null on fail
     */
    @Override
    public void setMsisdnNumber(String alphaTag, String number,
            Message onComplete) {

        // If the SIM card is locked by PIN, we will set EF_MSISDN fail.
        // In that case, msisdn and msisdnTag should not be update.
        mNewMsisdn = number;
        mNewMsisdnTag = alphaTag;

        if(DBG) log("Set MSISDN: " + mNewMsisdnTag + " " + /*mNewMsisdn*/
                Rlog.pii(LOG_TAG, mNewMsisdn));

        AdnRecord adn = new AdnRecord(mNewMsisdnTag, mNewMsisdn);

        new AdnRecordLoader(mFh).updateEF(adn, EF_MSISDN, getExtFromEf(EF_MSISDN), 1, null,
                obtainMessage(EVENT_SET_MSISDN_DONE, onComplete));
    }

    @Override
    public String getMsisdnAlphaTag() {
        return mMsisdnTag;
    }

    @Override
    public String getVoiceMailNumber() {
        return mVoiceMailNum;
    }

    /**
     * Set voice mail number to SIM record
     *
     * The voice mail number can be stored either in EF_MBDN (TS 51.011) or
     * EF_MAILBOX_CPHS (CPHS 4.2)
     *
     * If EF_MBDN is available, store the voice mail number to EF_MBDN
     *
     * If EF_MAILBOX_CPHS is enabled, store the voice mail number to EF_CHPS
     *
     * So the voice mail number will be stored in both EFs if both are available
     *
     * Return error only if both EF_MBDN and EF_MAILBOX_CPHS fail.
     *
     * When the operation is complete, onComplete will be sent to its handler
     *
     * @param alphaTag alpha-tagging of the dailing nubmer (upto 10 characters)
     * @param voiceNumber dailing nubmer (upto 20 digits)
     *        if the number is start with '+', then set to international TOA
     * @param onComplete
     *        onComplete.obj will be an AsyncResult
     *        ((AsyncResult)onComplete.obj).exception == null on success
     *        ((AsyncResult)onComplete.obj).exception != null on fail
     */
    @Override
    public void setVoiceMailNumber(String alphaTag, String voiceNumber,
            Message onComplete) {
        if (mIsVoiceMailFixed) {
            AsyncResult.forMessage((onComplete)).exception =
                    new IccVmFixedException("Voicemail number is fixed by operator");
            onComplete.sendToTarget();
            return;
        }

        mNewVoiceMailNum = voiceNumber;
        mNewVoiceMailTag = alphaTag;

        AdnRecord adn = new AdnRecord(mNewVoiceMailTag, mNewVoiceMailNum);

        if (mMailboxIndex != 0 && mMailboxIndex != 0xff) {

            new AdnRecordLoader(mFh).updateEF(adn, EF_MBDN, EF_EXT6,
                    mMailboxIndex, null,
                    obtainMessage(EVENT_SET_MBDN_DONE, onComplete));

        } else if (isCphsMailboxEnabled()) {

            new AdnRecordLoader(mFh).updateEF(adn, EF_MAILBOX_CPHS,
                    EF_EXT1, 1, null,
                    obtainMessage(EVENT_SET_CPHS_MAILBOX_DONE, onComplete));

        } else {
            AsyncResult.forMessage((onComplete)).exception =
                    new IccVmNotSupportedException("Update SIM voice mailbox error");
            onComplete.sendToTarget();
        }
    }

    @Override
    public String getVoiceMailAlphaTag()
    {
        return mVoiceMailTag;
    }

    /**
     * Sets the SIM voice message waiting indicator records
     * @param line GSM Subscriber Profile Number, one-based. Only '1' is supported
     * @param countWaiting The number of messages waiting, if known. Use
     *                     -1 to indicate that an unknown number of
     *                      messages are waiting
     */
    @Override
    public void
    setVoiceMessageWaiting(int line, int countWaiting) {
        if (line != 1) {
            // only profile 1 is supported
            return;
        }

        try {
            if (mEfMWIS != null) {
                // TS 51.011 10.3.45

                // lsb of byte 0 is 'voicemail' status
                mEfMWIS[0] = (byte)((mEfMWIS[0] & 0xfe)
                                    | (countWaiting == 0 ? 0 : 1));

                // byte 1 is the number of voice messages waiting
                if (countWaiting < 0) {
                    // The spec does not define what this should be
                    // if we don't know the count
                    mEfMWIS[1] = 0;
                } else {
                    mEfMWIS[1] = (byte) countWaiting;
                }

                mFh.updateEFLinearFixed(
                    EF_MWIS, 1, mEfMWIS, null,
                    obtainMessage (EVENT_UPDATE_DONE, EF_MWIS, 0));
            }

            if (mEfCPHS_MWI != null) {
                    // Refer CPHS4_2.WW6 B4.2.3
                mEfCPHS_MWI[0] = (byte)((mEfCPHS_MWI[0] & 0xf0)
                            | (countWaiting == 0 ? 0x5 : 0xa));
                mFh.updateEFTransparent(
                    EF_VOICE_MAIL_INDICATOR_CPHS, mEfCPHS_MWI,
                    obtainMessage (EVENT_UPDATE_DONE, EF_VOICE_MAIL_INDICATOR_CPHS));
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            logw("Error saving voice mail state to SIM. Probably malformed SIM record", ex);
        }
    }

    // Validate data is not null and not empty.
    private boolean validEfCfis(byte[] data) {
        if (data != null) {
            if (data[0] < 1 || data[0] > 4) {
                // The MSP (Multiple Subscriber Profile) byte should be between
                // 1 and 4 according to ETSI TS 131 102 v11.3.0 section 4.2.64.
                logw("MSP byte: " + data[0] + " is not between 1 and 4", null);
            }
            // empty EF_CFIS should be considered as call forward disabled
            for (byte b : data) {
                if (b != (byte) 0xFF) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getVoiceMessageCount() {
        boolean voiceMailWaiting = false;
        int countVoiceMessages = DEFAULT_VOICE_MESSAGE_COUNT;
        if (mEfMWIS != null) {
            // Use this data if the EF[MWIS] exists and
            // has been loaded
            // Refer TS 51.011 Section 10.3.45 for the content description
            voiceMailWaiting = ((mEfMWIS[0] & 0x01) != 0);
            countVoiceMessages = mEfMWIS[1] & 0xff;

            if (voiceMailWaiting && (countVoiceMessages == 0 || countVoiceMessages == 0xff)) {
                // Unknown count = -1
                countVoiceMessages = UNKNOWN_VOICE_MESSAGE_COUNT;
            }
            if (DBG) log(" VoiceMessageCount from SIM MWIS = " + countVoiceMessages);
        } else if (mEfCPHS_MWI != null) {
            // use voice mail count from CPHS
            int indicator = (int) (mEfCPHS_MWI[0] & 0xf);

            // Refer CPHS4_2.WW6 B4.2.3
            if (indicator == 0xA) {
                // Unknown count = -1
                countVoiceMessages = UNKNOWN_VOICE_MESSAGE_COUNT;
            } else if (indicator == 0x5) {
                countVoiceMessages = 0;
            }
            if (DBG) log(" VoiceMessageCount from SIM CPHS = " + countVoiceMessages);
        }
        return countVoiceMessages;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getVoiceCallForwardingFlag() {
        return mCallForwardingStatus;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVoiceCallForwardingFlag(int line, boolean enable, String dialNumber) {

        if (line != 1) return; // only line 1 is supported

        mCallForwardingStatus = enable ? CALL_FORWARDING_STATUS_ENABLED :
                CALL_FORWARDING_STATUS_DISABLED;

        mRecordsEventsRegistrants.notifyResult(EVENT_CFI);

        try {
            if (validEfCfis(mEfCfis)) {
                // lsb is of byte f1 is voice status
                if (enable) {
                    mEfCfis[1] |= 1;
                } else {
                    mEfCfis[1] &= 0xfe;
                }

                log("setVoiceCallForwardingFlag: enable=" + enable
                        + " mEfCfis=" + IccUtils.bytesToHexString(mEfCfis));

                // Update dialNumber if not empty and CFU is enabled.
                // Spec reference for EF_CFIS contents, TS 51.011 section 10.3.46.
                if (enable && !TextUtils.isEmpty(dialNumber)) {
                    logv("EF_CFIS: updating cf number, " + Rlog.pii(LOG_TAG, dialNumber));
                    byte[] bcdNumber = PhoneNumberUtils.numberToCalledPartyBCD(
                            dialNumber, PhoneNumberUtils.BCD_EXTENDED_TYPE_EF_ADN);

                    System.arraycopy(bcdNumber, 0, mEfCfis, CFIS_TON_NPI_OFFSET, bcdNumber.length);

                    mEfCfis[CFIS_BCD_NUMBER_LENGTH_OFFSET] = (byte) (bcdNumber.length);
                    mEfCfis[CFIS_ADN_CAPABILITY_ID_OFFSET] = (byte) 0xFF;
                    mEfCfis[CFIS_ADN_EXTENSION_ID_OFFSET] = (byte) 0xFF;
                }

                mFh.updateEFLinearFixed(
                        EF_CFIS, 1, mEfCfis, null,
                        obtainMessage (EVENT_UPDATE_DONE, EF_CFIS));
            } else {
                log("setVoiceCallForwardingFlag: ignoring enable=" + enable
                        + " invalid mEfCfis=" + IccUtils.bytesToHexString(mEfCfis));
            }

            if (mEfCff != null) {
                if (enable) {
                    mEfCff[0] = (byte) ((mEfCff[0] & CFF_LINE1_RESET)
                            | CFF_UNCONDITIONAL_ACTIVE);
                } else {
                    mEfCff[0] = (byte) ((mEfCff[0] & CFF_LINE1_RESET)
                            | CFF_UNCONDITIONAL_DEACTIVE);
                }

                mFh.updateEFTransparent(
                        EF_CFF_CPHS, mEfCff,
                        obtainMessage (EVENT_UPDATE_DONE, EF_CFF_CPHS));
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            logw("Error saving call forwarding flag to SIM. "
                            + "Probably malformed SIM record", ex);

        }
    }

    /**
     * Called by STK Service when REFRESH is received.
     * @param fileChanged indicates whether any files changed
     * @param fileList if non-null, a list of EF files that changed
     */
    @Override
    public void onRefresh(boolean fileChanged, int[] fileList) {
        if (fileChanged) {
            // A future optimization would be to inspect fileList and
            // only reload those files that we care about.  For now,
            // just re-fetch all SIM records that we cache.
            /* UNISOC:Loading ICCID by CRSM takes too long@{*/
            //fetchSimRecords();
            loadIccId();
            /* @} */
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOperatorNumeric() {
        String imsi = getIMSI();
        if (imsi == null) {
            log("getOperatorNumeric: IMSI == null");
            return null;
        }
        if (mMncLength == UNINITIALIZED || mMncLength == UNKNOWN) {
            log("getSIMOperatorNumeric: bad mncLength");
            return null;
        }

        // Length = length of MCC + length of MNC
        // length of mcc = 3 (TS 23.003 Section 2.2)
        if (imsi.length() >= 3 + mMncLength) {
            return imsi.substring(0, 3 + mMncLength);
        } else {
            return null;
        }
    }

    // ***** Overridden from Handler
    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar;
        AdnRecord adn;

        byte data[];

        boolean isRecordLoadResponse = false;

        if (mDestroyed.get()) {
            loge("Received message " + msg + "[" + msg.what + "] " +
                    " while being destroyed. Ignoring.");
            return;
        }

        try {
            switch (msg.what) {
                case EVENT_APP_READY:
                    onReady();
                    break;

                case EVENT_APP_LOCKED:
                case EVENT_APP_NETWORK_LOCKED:
                    onLocked(msg.what);
                    break;

                /* IO events */
                case EVENT_GET_IMSI_DONE:
                    isRecordLoadResponse = true;

                    ar = (AsyncResult) msg.obj;

                    if (ar.exception != null) {
                        loge("Exception querying IMSI, Exception:" + ar.exception);
                        break;
                    }

                    mImsi = (String) ar.result;

                    // IMSI (MCC+MNC+MSIN) is at least 6 digits, but not more
                    // than 15 (and usually 15).
                    if (mImsi != null && (mImsi.length() < 6 || mImsi.length() > 15)) {
                        loge("invalid IMSI " + mImsi);
                        mImsi = null;
                    }

                    log("IMSI: mMncLength=" + mMncLength);

                    if (mImsi != null && mImsi.length() >= 6) {
                        log("IMSI: " + mImsi.substring(0, 6)
                                + Rlog.pii(LOG_TAG, mImsi.substring(6)));
                    }

                    //Modify for Bug916935,when imsi contains "a~f",set invalid.
                    if (mImsi != null && !mImsi.substring(0, 6).matches("[0-9]*")) {
                        loge("invalid IMSI ");
                        mImsi = null;
                    }

                    String imsi = getIMSI();

                    if (((mMncLength == UNKNOWN) || (mMncLength == 2))
                            && ((imsi != null) && (imsi.length() >= 6))) {
                        String mccmncCode = imsi.substring(0, 6);
                        for (String mccmnc : MCCMNC_CODES_HAVING_3DIGITS_MNC) {
                            if (mccmnc.equals(mccmncCode)) {
                                mMncLength = 3;
                                log("IMSI: setting1 mMncLength=" + mMncLength);
                                break;
                            }
                        }
                    }

                    if (mMncLength == UNKNOWN) {
                        // the SIM has told us all it knows, but it didn't know the mnc length.
                        // guess using the mcc
                        try {
                            int mcc = Integer.parseInt(imsi.substring(0, 3));
                            mMncLength = MccTable.smallestDigitsMccForMnc(mcc);
                            log("setting2 mMncLength=" + mMncLength);
                        } catch (NumberFormatException e) {
                            mMncLength = UNKNOWN;
                            loge("Corrupt IMSI! setting3 mMncLength=" + mMncLength);
                        }
                    }

                    if (mMncLength != UNKNOWN && mMncLength != UNINITIALIZED
                            && imsi.length() >= 3 + mMncLength) {
                        log("update mccmnc=" + imsi.substring(0, 3 + mMncLength));
                        // finally have both the imsi and the mncLength and
                        // can parse the imsi properly
                        MccTable.updateMccMncConfiguration(mContext,
                                imsi.substring(0, 3 + mMncLength), false);
                    }
                    mImsiReadyRegistrants.notifyRegistrants();
                    break;

                case EVENT_GET_MBI_DONE:
                    boolean isValidMbdn;
                    isRecordLoadResponse = true;

                    ar = (AsyncResult) msg.obj;
                    data = (byte[]) ar.result;

                    isValidMbdn = false;
                    if (ar.exception == null) {
                        // Refer TS 51.011 Section 10.3.44 for content details
                        log("EF_MBI: " + IccUtils.bytesToHexString(data));

                        // Voice mail record number stored first
                        mMailboxIndex = data[0] & 0xff;

                        // check if dailing numbe id valid
                        if (mMailboxIndex != 0 && mMailboxIndex != 0xff) {
                            log("Got valid mailbox number for MBDN");
                            isValidMbdn = true;
                        }
                    }

                    // one more record to load
                    mRecordsToLoad += 1;

                    if (isValidMbdn) {
                        // Note: MBDN was not included in NUM_OF_SIM_RECORDS_LOADED
                        new AdnRecordLoader(mFh).loadFromEF(EF_MBDN, EF_EXT6,
                                mMailboxIndex, obtainMessage(EVENT_GET_MBDN_DONE));
                    } else {
                        // If this EF not present, try mailbox as in CPHS standard
                        // CPHS (CPHS4_2.WW6) is a european standard.
                        new AdnRecordLoader(mFh).loadFromEF(EF_MAILBOX_CPHS,
                                EF_EXT1, 1,
                                obtainMessage(EVENT_GET_CPHS_MAILBOX_DONE));
                    }

                    break;
                case EVENT_GET_CPHS_MAILBOX_DONE:
                case EVENT_GET_MBDN_DONE:
                    //Resetting the voice mail number and voice mail tag to null
                    //as these should be updated from the data read from EF_MBDN.
                    //If they are not reset, incase of invalid data/exception these
                    //variables are retaining their previous values and are
                    //causing invalid voice mailbox info display to user.
                    mVoiceMailNum = null;
                    mVoiceMailTag = null;
                    isRecordLoadResponse = true;

                    ar = (AsyncResult) msg.obj;

                    if (ar.exception != null) {

                        log("Invalid or missing EF"
                                + ((msg.what == EVENT_GET_CPHS_MAILBOX_DONE)
                                    ? "[MAILBOX]" : "[MBDN]"));

                        // Bug #645770 fall back to CPHS
                        // FIXME should use SST to decide

                        if (msg.what == EVENT_GET_MBDN_DONE) {
                            //load CPHS on fail...
                            // FIXME right now, only load line1's CPHS voice mail entry

                            mRecordsToLoad += 1;
                            new AdnRecordLoader(mFh).loadFromEF(
                                    EF_MAILBOX_CPHS, EF_EXT1, 1,
                                    obtainMessage(EVENT_GET_CPHS_MAILBOX_DONE));
                        }
                        break;
                    }

                    adn = (AdnRecord) ar.result;

                    log("VM: " + adn
                            + ((msg.what == EVENT_GET_CPHS_MAILBOX_DONE)
                                ? " EF[MAILBOX]" : " EF[MBDN]"));

                    if (adn.isEmpty() && msg.what == EVENT_GET_MBDN_DONE) {
                        // Bug #645770 fall back to CPHS
                        // FIXME should use SST to decide
                        // FIXME right now, only load line1's CPHS voice mail entry
                        mRecordsToLoad += 1;
                        new AdnRecordLoader(mFh).loadFromEF(
                                EF_MAILBOX_CPHS, EF_EXT1, 1,
                                obtainMessage(EVENT_GET_CPHS_MAILBOX_DONE));

                        break;
                    }

                    mVoiceMailNum = adn.getNumber();
                    mVoiceMailTag = adn.getAlphaTag();
                    Resources resource = Resources.getSystem();
                    if (resource.getBoolean(com.android.internal.R.bool.config_modifyForIncorrectVociemailtag)
                            && !TextUtils.isEmpty(mVoiceMailTag) && mVoiceMailTag.matches("@*")) {
                        mVoiceMailTag = "";
                    }
                    break;

                case EVENT_GET_MSISDN_DONE:
                    isRecordLoadResponse = true;

                    ar = (AsyncResult) msg.obj;

                    if (ar.exception != null) {
                        log("Invalid or missing EF[MSISDN]");
                        break;
                    }

                    adn = (AdnRecord) ar.result;

                    mMsisdn = adn.getNumber();
                    mMsisdnTag = adn.getAlphaTag();

                    log("MSISDN: " + /*mMsisdn*/ Rlog.pii(LOG_TAG, mMsisdn));
                    break;

                case EVENT_SET_MSISDN_DONE:
                    isRecordLoadResponse = false;
                    ar = (AsyncResult) msg.obj;

                    if (ar.exception == null) {
                        mMsisdn = mNewMsisdn;
                        mMsisdnTag = mNewMsisdnTag;
                        log("Success to update EF[MSISDN]");
                    }

                    if (ar.userObj != null) {
                        AsyncResult.forMessage(((Message) ar.userObj)).exception = ar.exception;
                        ((Message) ar.userObj).sendToTarget();
                    }
                    break;

                case EVENT_GET_MWIS_DONE:
                    isRecordLoadResponse = true;

                    ar = (AsyncResult) msg.obj;
                    data = (byte[]) ar.result;

                    if (DBG) log("EF_MWIS : " + IccUtils.bytesToHexString(data));

                    if (ar.exception != null) {
                        if (DBG) log("EVENT_GET_MWIS_DONE exception = " + ar.exception);
                        break;
                    }

                    if ((data[0] & 0xff) == 0xff) {
                        if (DBG) log("SIMRecords: Uninitialized record MWIS");
                        break;
                    }

                    mEfMWIS = data;
                    break;

                case EVENT_GET_VOICE_MAIL_INDICATOR_CPHS_DONE:
                    isRecordLoadResponse = true;

                    ar = (AsyncResult) msg.obj;
                    data = (byte[]) ar.result;

                    if (DBG) log("EF_CPHS_MWI: " + IccUtils.bytesToHexString(data));

                    if (ar.exception != null) {
                        if (DBG) {
                            log("EVENT_GET_VOICE_MAIL_INDICATOR_CPHS_DONE exception = "
                                    + ar.exception);
                        }
                        break;
                    }

                    mEfCPHS_MWI = data;
                    break;

                case EVENT_GET_ICCID_DONE:
                    isRecordLoadResponse = true;

                    ar = (AsyncResult) msg.obj;
                    data = (byte[]) ar.result;

                    if (ar.exception != null) {
                        //Add invalid ICCID cache
                        log("Get iccid occur exception, set default value");
                        mFullIccId = "00000000000000000000";
                        mIccId = mFullIccId;
                        break;
                    }
                    /* UNISOC:When IccId has been fetched,go to create SubscriptionInfo @{*/
                    mFullIccId = IccUtils.bchToString(data, 0, data.length);
                    mIccId = mFullIccId;
                    mIccIdLoadedRegistrants.notifyRegistrants();
                    if (mParentApp.getState() ==
                            IccCardApplicationStatus.AppState.APPSTATE_READY) {
                        fetchSimRecords();
                    }
                    /* @} */
                    log("iccid: " + SubscriptionInfo.givePrintableIccid(mFullIccId));
                    break;

                case EVENT_GET_AD_DONE:
                    try {
                        isRecordLoadResponse = true;

                        if (mCarrierTestOverride.isInTestMode() && getIMSI() != null) {
                            imsi = getIMSI();
                            try {
                                int mcc = Integer.parseInt(imsi.substring(0, 3));
                                mMncLength = MccTable.smallestDigitsMccForMnc(mcc);
                                log("[TestMode] mMncLength=" + mMncLength);
                            } catch (NumberFormatException e) {
                                mMncLength = UNKNOWN;
                                loge("[TestMode] Corrupt IMSI! mMncLength=" + mMncLength);
                            }
                        } else {
                            ar = (AsyncResult) msg.obj;
                            data = (byte[]) ar.result;

                            if (ar.exception != null) {
                                break;
                            }

                            log("EF_AD: " + IccUtils.bytesToHexString(data));

                            if (data.length < 3) {
                                log("Corrupt AD data on SIM");
                                break;
                            }

                            if (data.length == 3) {
                                log("MNC length not present in EF_AD");
                                break;
                            }

                            mMncLength = data[3] & 0xf;
                            log("setting4 mMncLength=" + mMncLength);
                        }

                        if (mMncLength == 0xf) {
                            mMncLength = UNKNOWN;
                            log("setting5 mMncLength=" + mMncLength);
                        } else if (mMncLength != 2 && mMncLength != 3) {
                            mMncLength = UNINITIALIZED;
                            log("setting5 mMncLength=" + mMncLength);
                        }
                    } finally {

                        // IMSI could be a value reading from Sim or a fake IMSI if in the test mode
                        imsi = getIMSI();

                        if (((mMncLength == UNINITIALIZED) || (mMncLength == UNKNOWN)
                                    || (mMncLength == 2)) && ((imsi != null)
                                    && (imsi.length() >= 6))) {
                            String mccmncCode = imsi.substring(0, 6);
                            log("mccmncCode=" + mccmncCode);
                            for (String mccmnc : MCCMNC_CODES_HAVING_3DIGITS_MNC) {
                                if (mccmnc.equals(mccmncCode)) {
                                    mMncLength = 3;
                                    log("setting6 mMncLength=" + mMncLength);
                                    break;
                                }
                            }
                        }

                        if (mMncLength == UNKNOWN || mMncLength == UNINITIALIZED) {
                            if (imsi != null) {
                                try {
                                    int mcc = Integer.parseInt(imsi.substring(0, 3));

                                    mMncLength = MccTable.smallestDigitsMccForMnc(mcc);
                                    log("setting7 mMncLength=" + mMncLength);
                                } catch (NumberFormatException e) {
                                    mMncLength = UNKNOWN;
                                    loge("Corrupt IMSI! setting8 mMncLength=" + mMncLength);
                                }
                            } else {
                                // Indicate we got this info, but it didn't contain the length.
                                mMncLength = UNKNOWN;
                                log("MNC length not present in EF_AD setting9 "
                                        + "mMncLength=" + mMncLength);
                            }
                        }
                        if (imsi != null && mMncLength != UNKNOWN
                                && imsi.length() >= 3 + mMncLength) {
                            // finally have both imsi and the length of the mnc and can parse
                            // the imsi properly
                            log("update mccmnc=" + imsi.substring(0, 3 + mMncLength));
                            MccTable.updateMccMncConfiguration(mContext,
                                    imsi.substring(0, 3 + mMncLength), false);
                        }
                    }
                    break;

                case EVENT_GET_SPN_DONE:
                    isRecordLoadResponse = true;
                    ar = (AsyncResult) msg.obj;
                    getSpnFsm(false, ar);
                    break;

                case EVENT_GET_CFF_DONE:
                    isRecordLoadResponse = true;

                    ar = (AsyncResult) msg.obj;
                    data = (byte[]) ar.result;

                    if (ar.exception != null) {
                        mEfCff = null;
                    } else {
                        log("EF_CFF_CPHS: " + IccUtils.bytesToHexString(data));
                        mEfCff = data;
                    }

                    break;

                case EVENT_GET_SPDI_DONE:
                    isRecordLoadResponse = true;

                    ar = (AsyncResult) msg.obj;
                    data = (byte[]) ar.result;

                    if (ar.exception != null) {
                        break;
                    }

                    parseEfSpdi(data);
                    break;

                case EVENT_UPDATE_DONE:
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        logw("update failed. ", ar.exception);
                    }
                    break;

                case EVENT_GET_PNN_DONE:
                    isRecordLoadResponse = true;

                    ar = (AsyncResult) msg.obj;
                    data = (byte[]) ar.result;

                    if (ar.exception != null) {
                        break;
                    }

                    SimTlv tlv = new SimTlv(data, 0, data.length);

                    for (; tlv.isValidObject(); tlv.nextObject()) {
                        if (tlv.getTag() == TAG_FULL_NETWORK_NAME) {
                            mPnnHomeName = IccUtils.networkNameToString(
                                    tlv.getData(), 0, tlv.getData().length);
                            log("PNN: " + mPnnHomeName);
                            break;
                        }
                    }
                    break;

                case EVENT_GET_ALL_SMS_DONE:
                    isRecordLoadResponse = true;

                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        break;
                    }

                    handleSmses((ArrayList<byte []>) ar.result);
                    break;

                case EVENT_MARK_SMS_READ_DONE:
                    Rlog.i("ENF", "marked read: sms " + msg.arg1);
                    break;


                case EVENT_SMS_ON_SIM:
                    isRecordLoadResponse = false;

                    ar = (AsyncResult) msg.obj;

                    Integer index = (Integer) ar.result;

                    if (ar.exception != null || index == null) {
                        loge("Error on SMS_ON_SIM with exp "
                                + ar.exception + " index " + index);
                    } else {
                        log("READ EF_SMS RECORD index=" + index);
                        mFh.loadEFLinearFixed(EF_SMS, index, obtainMessage(EVENT_GET_SMS_DONE));
                    }
                    break;

                case EVENT_GET_SMS_DONE:
                    isRecordLoadResponse = false;
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        handleSms((byte[]) ar.result);
                    } else {
                        loge("Error on GET_SMS with exp " + ar.exception);
                    }
                    break;
                case EVENT_GET_SST_DONE:
                    isRecordLoadResponse = true;

                    ar = (AsyncResult) msg.obj;
                    data = (byte[]) ar.result;

                    if (ar.exception != null) {
                        break;
                    }

                    mUsimServiceTable = new UsimServiceTable(data);
                    if (DBG) log("SST: " + mUsimServiceTable);
                    break;

                case EVENT_GET_INFO_CPHS_DONE:
                    isRecordLoadResponse = true;

                    ar = (AsyncResult) msg.obj;

                    if (ar.exception != null) {
                        break;
                    }

                    mCphsInfo = (byte[]) ar.result;

                    if (DBG) log("iCPHS: " + IccUtils.bytesToHexString(mCphsInfo));
                    break;

                case EVENT_SET_MBDN_DONE:
                    isRecordLoadResponse = false;
                    ar = (AsyncResult) msg.obj;

                    if (DBG) log("EVENT_SET_MBDN_DONE ex:" + ar.exception);
                    if (ar.exception == null) {
                        mVoiceMailNum = mNewVoiceMailNum;
                        mVoiceMailTag = mNewVoiceMailTag;
                    }

                    if (isCphsMailboxEnabled()) {
                        adn = new AdnRecord(mVoiceMailTag, mVoiceMailNum);
                        Message onCphsCompleted = (Message) ar.userObj;

                        /* write to cphs mailbox whenever it is available but
                        * we only need notify caller once if both updating are
                        * successful.
                        *
                        * so if set_mbdn successful, notify caller here and set
                        * onCphsCompleted to null
                        */
                        if (ar.exception == null && ar.userObj != null) {
                            AsyncResult.forMessage(((Message) ar.userObj)).exception = null;
                            ((Message) ar.userObj).sendToTarget();

                            if (DBG) log("Callback with MBDN successful.");

                            onCphsCompleted = null;
                        }

                        new AdnRecordLoader(mFh)
                                .updateEF(adn, EF_MAILBOX_CPHS, EF_EXT1, 1, null,
                                obtainMessage(EVENT_SET_CPHS_MAILBOX_DONE,
                                        onCphsCompleted));
                    } else {
                        if (ar.userObj != null) {
                            CarrierConfigManager configLoader = (CarrierConfigManager)
                                    mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
                            if (ar.exception != null && configLoader != null
                                    && configLoader.getConfig().getBoolean(
                                    CarrierConfigManager.KEY_EDITABLE_VOICEMAIL_NUMBER_BOOL)) {
                                // GsmCdmaPhone will store vm number on device
                                // when IccVmNotSupportedException occurred
                                AsyncResult.forMessage(((Message) ar.userObj)).exception =
                                        new IccVmNotSupportedException(
                                            "Update SIM voice mailbox error");
                            } else {
                                AsyncResult.forMessage(((Message) ar.userObj))
                                    .exception = ar.exception;
                            }
                            ((Message) ar.userObj).sendToTarget();
                        }
                    }
                    break;
                case EVENT_SET_CPHS_MAILBOX_DONE:
                    isRecordLoadResponse = false;
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        mVoiceMailNum = mNewVoiceMailNum;
                        mVoiceMailTag = mNewVoiceMailTag;
                    } else {
                        if (DBG) log("Set CPHS MailBox with exception: " + ar.exception);
                    }
                    if (ar.userObj != null) {
                        if (DBG) log("Callback with CPHS MB successful.");
                        AsyncResult.forMessage(((Message) ar.userObj)).exception
                                = ar.exception;
                        ((Message) ar.userObj).sendToTarget();
                    }
                    break;
                case EVENT_GET_CFIS_DONE:
                    isRecordLoadResponse = true;

                    ar = (AsyncResult) msg.obj;
                    data = (byte[]) ar.result;

                    if (ar.exception != null) {
                        mEfCfis = null;
                    } else {
                        log("EF_CFIS: " + IccUtils.bytesToHexString(data));
                        mEfCfis = data;
                    }

                    break;

                case EVENT_GET_CSP_CPHS_DONE:
                    isRecordLoadResponse = true;

                    ar = (AsyncResult) msg.obj;

                    if (ar.exception != null) {
                        loge("Exception in fetching EF_CSP data " + ar.exception);
                        break;
                    }

                    data = (byte[]) ar.result;

                    log("EF_CSP: " + IccUtils.bytesToHexString(data));
                    handleEfCspData(data);
                    break;

                case EVENT_GET_GID1_DONE:
                    isRecordLoadResponse = true;

                    ar = (AsyncResult) msg.obj;
                    data = (byte[]) ar.result;

                    if (ar.exception != null) {
                        loge("Exception in get GID1 " + ar.exception);
                        mGid1 = null;
                        break;
                    }

                    mGid1 = IccUtils.bytesToHexString(data);

                    log("GID1: " + mGid1);

                    break;

                case EVENT_GET_GID2_DONE:
                    isRecordLoadResponse = true;
                    ar = (AsyncResult) msg.obj;
                    data = (byte[]) ar.result;

                    if (ar.exception != null) {
                        loge("Exception in get GID2 " + ar.exception);
                        mGid2 = null;
                        break;
                    }

                    mGid2 = IccUtils.bytesToHexString(data);

                    log("GID2: " + mGid2);

                    break;

                case EVENT_GET_PLMN_W_ACT_DONE:
                    isRecordLoadResponse = true;
                    ar = (AsyncResult) msg.obj;
                    data = (byte[]) ar.result;

                    if (ar.exception != null || data == null) {
                        loge("Failed getting User PLMN with Access Tech Records: " + ar.exception);
                        break;
                    } else {
                        log("Received a PlmnActRecord, raw=" + IccUtils.bytesToHexString(data));
                        mPlmnActRecords = PlmnActRecord.getRecords(data);
                        if (VDBG) log("PlmnActRecords=" + Arrays.toString(mPlmnActRecords));
                    }
                    break;

                case EVENT_GET_OPLMN_W_ACT_DONE:
                    isRecordLoadResponse = true;
                    ar = (AsyncResult) msg.obj;
                    data = (byte[]) ar.result;

                    if (ar.exception != null || data == null) {
                        loge("Failed getting Operator PLMN with Access Tech Records: "
                                + ar.exception);
                        break;
                    } else {
                        log("Received a PlmnActRecord, raw=" + IccUtils.bytesToHexString(data));
                        mOplmnActRecords = PlmnActRecord.getRecords(data);
                        if (VDBG) log("OplmnActRecord[]=" + Arrays.toString(mOplmnActRecords));
                    }
                    break;

                case EVENT_GET_HPLMN_W_ACT_DONE:
                    isRecordLoadResponse = true;
                    ar = (AsyncResult) msg.obj;
                    data = (byte[]) ar.result;

                    if (ar.exception != null || data == null) {
                        loge("Failed getting Home PLMN with Access Tech Records: " + ar.exception);
                        break;
                    } else {
                        log("Received a PlmnActRecord, raw=" + IccUtils.bytesToHexString(data));
                        mHplmnActRecords = PlmnActRecord.getRecords(data);
                        log("HplmnActRecord[]=" + Arrays.toString(mHplmnActRecords));
                    }
                    break;

                case EVENT_GET_EHPLMN_DONE:
                    isRecordLoadResponse = true;
                    ar = (AsyncResult) msg.obj;
                    data = (byte[]) ar.result;
                    if (ar.exception != null || data == null) {
                        loge("Failed getting Equivalent Home PLMNs: " + ar.exception);
                        break;
                    } else {
                        mEhplmns = parseBcdPlmnList(data, "Equivalent Home");
                    }
                    break;

                case EVENT_GET_FPLMN_DONE:
                    isRecordLoadResponse = true;
                    ar = (AsyncResult) msg.obj;
                    data = (byte[]) ar.result;
                    if (ar.exception != null || data == null) {
                        loge("Failed getting Forbidden PLMNs: " + ar.exception);
                        break;
                    } else {
                        mFplmns = parseBcdPlmnList(data, "Forbidden");
                    }
                    if (msg.arg1 == HANDLER_ACTION_SEND_RESPONSE) {
                        if (VDBG) logv("getForbiddenPlmns(): send async response");
                        isRecordLoadResponse = false;
                        Message response = retrievePendingResponseMessage(msg.arg2);
                        if (response != null) {
                            // UNISOC Modify for bug 934273:
                            // If FPLMN is invalid mFplmns will be set to null, response.sendToTarget cannot be called due to NPE.
                            // AsyncTask.onPostExecute in NetworkSelectListPreference.loadNetworksList cannot be called.
                            // This causes progress dialog keeps showing but PLMN search not performing.
                            AsyncResult.forMessage(
                                    response, mFplmns == null ? new String[0] :
                                            Arrays.copyOf(mFplmns, mFplmns.length), null);
                            response.sendToTarget();
                        } else {
                            loge("Failed to retrieve a response message for FPLMN");
                            break;
                        }
                    }
                    break;

                default:
                    super.handleMessage(msg);   // IccRecords handles generic record load responses
            }
        } catch (RuntimeException exc) {
            // I don't want these exceptions to be fatal
            logw("Exception parsing SIM record", exc);
        } finally {
            // Count up record load responses even if they are fails
            if (isRecordLoadResponse) {
                onRecordLoaded();
            }
        }
    }

    private class EfPlLoaded implements IccRecordLoaded {
        public String getEfName() {
            return "EF_PL";
        }

        public void onRecordLoaded(AsyncResult ar) {
            mEfPl = (byte[]) ar.result;
            if (DBG) log("EF_PL=" + IccUtils.bytesToHexString(mEfPl));
        }
    }

    private class EfUsimLiLoaded implements IccRecordLoaded {
        public String getEfName() {
            return "EF_LI";
        }

        public void onRecordLoaded(AsyncResult ar) {
            mEfLi = (byte[]) ar.result;
            if (DBG) log("EF_LI=" + IccUtils.bytesToHexString(mEfLi));
        }
    }

    @Override
    protected void handleFileUpdate(int efid) {
        switch(efid) {
            case EF_MBDN:
                mRecordsToLoad++;
                new AdnRecordLoader(mFh).loadFromEF(EF_MBDN, EF_EXT6,
                        mMailboxIndex, obtainMessage(EVENT_GET_MBDN_DONE));
                break;
            case EF_MAILBOX_CPHS:
                mRecordsToLoad++;
                new AdnRecordLoader(mFh).loadFromEF(EF_MAILBOX_CPHS, EF_EXT1,
                        1, obtainMessage(EVENT_GET_CPHS_MAILBOX_DONE));
                break;
            case EF_CSP_CPHS:
                mRecordsToLoad++;
                log("[CSP] SIM Refresh for EF_CSP_CPHS");
                mFh.loadEFTransparent(EF_CSP_CPHS,
                        obtainMessage(EVENT_GET_CSP_CPHS_DONE));
                break;
            case EF_FDN:
                if (DBG) log("SIM Refresh called for EF_FDN");
                mParentApp.queryFdn();
                mAdnCache.reset();
                break;
            case EF_MSISDN:
                mRecordsToLoad++;
                log("SIM Refresh called for EF_MSISDN");
                new AdnRecordLoader(mFh).loadFromEF(EF_MSISDN, getExtFromEf(EF_MSISDN), 1,
                        obtainMessage(EVENT_GET_MSISDN_DONE));
                break;
            case EF_CFIS:
            case EF_CFF_CPHS:
                log("SIM Refresh called for EF_CFIS or EF_CFF_CPHS");
                loadCallForwardingRecords();
                break;
            default:
                // For now, fetch all records if this is not a
                // voicemail number.
                // TODO: Handle other cases, instead of fetching all.
                mAdnCache.reset();
                fetchSimRecords();
                log("onRefresh handleSimRefresh with SIM_FILE_UPDATED");
                Intent intent = new Intent(TelephonyIntentsEx.ACTION_STK_REFRESH_SIM_CONTACTS);
                intent.putExtra("phone_id", mParentApp.getPhoneId());
                intent.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
                mContext.sendBroadcast(intent);
                break;
        }
    }

    /**
     * Dispatch 3GPP format message to registrant ({@code GsmCdmaPhone}) to pass to the 3GPP SMS
     * dispatcher for delivery.
     */
    private int dispatchGsmMessage(SmsMessage message) {
        mNewSmsRegistrants.notifyResult(message);
        return 0;
    }

    private void handleSms(byte[] ba) {
        if (ba[0] != 0)
            Rlog.d("ENF", "status : " + ba[0]);

        // 3GPP TS 51.011 v5.0.0 (20011-12)  10.5.3
        // 3 == "received by MS from network; message to be read"
        if (ba[0] == 3) {
            int n = ba.length;

            // Note: Data may include trailing FF's.  That's OK; message
            // should still parse correctly.
            byte[] pdu = new byte[n - 1];
            System.arraycopy(ba, 1, pdu, 0, n - 1);
            SmsMessage message = SmsMessage.createFromPdu(pdu, SmsConstants.FORMAT_3GPP);

            dispatchGsmMessage(message);
        }
    }


    private void handleSmses(ArrayList<byte[]> messages) {
        int count = messages.size();

        for (int i = 0; i < count; i++) {
            byte[] ba = messages.get(i);

            if (ba[0] != 0)
                Rlog.i("ENF", "status " + i + ": " + ba[0]);

            // 3GPP TS 51.011 v5.0.0 (20011-12)  10.5.3
            // 3 == "received by MS from network; message to be read"

            if (ba[0] == 3) {
                int n = ba.length;

                // Note: Data may include trailing FF's.  That's OK; message
                // should still parse correctly.
                byte[] pdu = new byte[n - 1];
                System.arraycopy(ba, 1, pdu, 0, n - 1);
                SmsMessage message = SmsMessage.createFromPdu(pdu, SmsConstants.FORMAT_3GPP);

                dispatchGsmMessage(message);

                // 3GPP TS 51.011 v5.0.0 (20011-12)  10.5.3
                // 1 == "received by MS from network; message read"

                ba[0] = 1;

                if (false) { // FIXME: writing seems to crash RdoServD
                    mFh.updateEFLinearFixed(EF_SMS,
                            i, ba, null, obtainMessage(EVENT_MARK_SMS_READ_DONE, i));
                }
            }
        }
    }

    @Override
    protected void onRecordLoaded() {
        // One record loaded successfully or failed, In either case
        // we need to update the recordsToLoad count
        mRecordsToLoad -= 1;
        if (DBG) log("onRecordLoaded " + mRecordsToLoad + " requested: " + mRecordsRequested);

        if (getRecordsLoaded()) {
            onAllRecordsLoaded();
        } else if (getLockedRecordsLoaded() || getNetworkLockedRecordsLoaded()) {
            onLockedAllRecordsLoaded();
        } else if (mRecordsToLoad < 0) {
            loge("recordsToLoad <0, programmer error suspected");
            mRecordsToLoad = 0;
        }
    }

    private void setVoiceCallForwardingFlagFromSimRecords() {
        if (validEfCfis(mEfCfis)) {
            // Refer TS 51.011 Section 10.3.46 for the content description
            mCallForwardingStatus = (mEfCfis[1] & 0x01);
            log("EF_CFIS: callForwardingEnabled=" + mCallForwardingStatus);
        } else if (mEfCff != null) {
            mCallForwardingStatus =
                    ((mEfCff[0] & CFF_LINE1_MASK) == CFF_UNCONDITIONAL_ACTIVE) ?
                            CALL_FORWARDING_STATUS_ENABLED : CALL_FORWARDING_STATUS_DISABLED;
            log("EF_CFF: callForwardingEnabled=" + mCallForwardingStatus);
        } else {
            mCallForwardingStatus = CALL_FORWARDING_STATUS_UNKNOWN;
            log("EF_CFIS and EF_CFF not valid. callForwardingEnabled=" + mCallForwardingStatus);
        }
    }

    private void setSimLanguageFromEF() {
        Resources resource = Resources.getSystem();
        if (resource.getBoolean(com.android.internal.R.bool.config_use_sim_language_file)) {
            setSimLanguage(mEfLi, mEfPl);
        } else {
            if (DBG) log ("Not using EF LI/EF PL");
        }
    }

    private void onLockedAllRecordsLoaded() {
        setSimLanguageFromEF();
        if (mLockedRecordsReqReason == LOCKED_RECORDS_REQ_REASON_LOCKED) {
            mLockedRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
        } else if (mLockedRecordsReqReason == LOCKED_RECORDS_REQ_REASON_NETWORK_LOCKED) {
            mNetworkLockedRecordsLoadedRegistrants.notifyRegistrants(
                    new AsyncResult(null, null, null));
        } else {
            loge("onLockedAllRecordsLoaded: unexpected mLockedRecordsReqReason "
                    + mLockedRecordsReqReason);
        }
    }

    @Override
    protected void onAllRecordsLoaded() {
        if (DBG) log("record load complete");

        setSimLanguageFromEF();
        setVoiceCallForwardingFlagFromSimRecords();

        // Some fields require more than one SIM record to set

        String operator = getOperatorNumeric();
        if (!TextUtils.isEmpty(operator)) {
            log("onAllRecordsLoaded set 'gsm.sim.operator.numeric' to operator='" +
                    operator + "'");
            mTelephonyManager.setSimOperatorNumericForPhone(
                    mParentApp.getPhoneId(), operator);
        } else {
            log("onAllRecordsLoaded empty 'gsm.sim.operator.numeric' skipping");
        }

        String imsi = getIMSI();

        if (!TextUtils.isEmpty(imsi) && imsi.length() >= 3) {
            log("onAllRecordsLoaded set mcc imsi" + (VDBG ? ("=" + imsi) : ""));
            mTelephonyManager.setSimCountryIsoForPhone(
                    mParentApp.getPhoneId(), MccTable.countryCodeForMcc(
                    Integer.parseInt(imsi.substring(0, 3))));
        } else {
            log("onAllRecordsLoaded empty imsi skipping setting mcc");
        }

        setVoiceMailByCountry(operator);
        mLoaded.set(true);
        mRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
    }

    //***** Private methods

    private void setVoiceMailByCountry (String spn) {
        if (mVmConfig.containsCarrier(spn)) {
            mIsVoiceMailFixed = true;
            mVoiceMailNum = mVmConfig.getVoiceMailNumber(spn);
            mVoiceMailTag = mVmConfig.getVoiceMailTag(spn);
        }
    }

    /**
     * String[] of forbidden PLMNs will be sent to the Message's handler
     * in the result field of an AsyncResult in the response.obj.
     */
    public void getForbiddenPlmns(Message response) {
        int key = storePendingResponseMessage(response);
        mFh.loadEFTransparent(EF_FPLMN, obtainMessage(
                    EVENT_GET_FPLMN_DONE, HANDLER_ACTION_SEND_RESPONSE, key));
    }

    @Override
    public void onReady() {
        /* UNISOC:Loading ICCID by CRSM takes too long@{*/
        //fetchSimRecords();
        loadIccId();
    }

    private void loadIccId() {
        mFh.loadEFTransparent(EF_ICCID, obtainMessage(EVENT_GET_ICCID_DONE));
        mRecordsToLoad++;
    }
    /* @} */

    private void onLocked(int msg) {
        if (DBG) log("only fetch EF_LI, EF_PL and EF_ICCID in locked state");
        mRecordsRequested = false;
        mLoaded.set(false);
        mLockedRecordsReqReason = msg == EVENT_APP_LOCKED ? LOCKED_RECORDS_REQ_REASON_LOCKED :
                LOCKED_RECORDS_REQ_REASON_NETWORK_LOCKED;

        loadEfLiAndEfPl();
        mFh.loadEFTransparent(EF_ICCID, obtainMessage(EVENT_GET_ICCID_DONE));
        mRecordsToLoad++;
    }

    private void loadEfLiAndEfPl() {
        if (mParentApp.getType() == AppType.APPTYPE_USIM) {
            mFh.loadEFTransparent(EF_LI,
                    obtainMessage(EVENT_GET_ICC_RECORD_DONE, new EfUsimLiLoaded()));
            mRecordsToLoad++;

            mFh.loadEFTransparent(EF_PL,
                    obtainMessage(EVENT_GET_ICC_RECORD_DONE, new EfPlLoaded()));
            mRecordsToLoad++;
        }
    }

    private void loadCallForwardingRecords() {
        mRecordsRequested = true;
        mFh.loadEFLinearFixed(EF_CFIS, 1, obtainMessage(EVENT_GET_CFIS_DONE));
        mRecordsToLoad++;
        mFh.loadEFTransparent(EF_CFF_CPHS, obtainMessage(EVENT_GET_CFF_DONE));
        mRecordsToLoad++;
    }

    protected void fetchSimRecords() {
        mRecordsRequested = true;

        if (DBG) log("fetchSimRecords " + mRecordsToLoad);

        mCi.getIMSIForApp(mParentApp.getAid(), obtainMessage(EVENT_GET_IMSI_DONE));
        mRecordsToLoad++;

        //UNISOC:Loading ICCID by CRSM takes too long
        //mFh.loadEFTransparent(EF_ICCID, obtainMessage(EVENT_GET_ICCID_DONE));
        //mRecordsToLoad++;

        // FIXME should examine EF[MSISDN]'s capability configuration
        // to determine which is the voice/data/fax line
        new AdnRecordLoader(mFh).loadFromEF(EF_MSISDN, getExtFromEf(EF_MSISDN), 1,
                    obtainMessage(EVENT_GET_MSISDN_DONE));
        mRecordsToLoad++;

        // Record number is subscriber profile
        mFh.loadEFLinearFixed(EF_MBI, 1, obtainMessage(EVENT_GET_MBI_DONE));
        mRecordsToLoad++;

        mFh.loadEFTransparent(EF_AD, obtainMessage(EVENT_GET_AD_DONE));
        mRecordsToLoad++;

        // Record number is subscriber profile
        mFh.loadEFLinearFixed(EF_MWIS, 1, obtainMessage(EVENT_GET_MWIS_DONE));
        mRecordsToLoad++;


        // Also load CPHS-style voice mail indicator, which stores
        // the same info as EF[MWIS]. If both exist, both are updated
        // but the EF[MWIS] data is preferred
        // Please note this must be loaded after EF[MWIS]
        mFh.loadEFTransparent(
                EF_VOICE_MAIL_INDICATOR_CPHS,
                obtainMessage(EVENT_GET_VOICE_MAIL_INDICATOR_CPHS_DONE));
        mRecordsToLoad++;

        // Same goes for Call Forward Status indicator: fetch both
        // EF[CFIS] and CPHS-EF, with EF[CFIS] preferred.
        loadCallForwardingRecords();

        getSpnFsm(true, null);

        mFh.loadEFTransparent(EF_SPDI, obtainMessage(EVENT_GET_SPDI_DONE));
        mRecordsToLoad++;

        mFh.loadEFLinearFixed(EF_PNN, 1, obtainMessage(EVENT_GET_PNN_DONE));
        mRecordsToLoad++;

        mFh.loadEFTransparent(EF_SST, obtainMessage(EVENT_GET_SST_DONE));
        mRecordsToLoad++;

        mFh.loadEFTransparent(EF_INFO_CPHS, obtainMessage(EVENT_GET_INFO_CPHS_DONE));
        mRecordsToLoad++;

        mFh.loadEFTransparent(EF_CSP_CPHS,obtainMessage(EVENT_GET_CSP_CPHS_DONE));
        mRecordsToLoad++;

        mFh.loadEFTransparent(EF_GID1, obtainMessage(EVENT_GET_GID1_DONE));
        mRecordsToLoad++;

        mFh.loadEFTransparent(EF_GID2, obtainMessage(EVENT_GET_GID2_DONE));
        mRecordsToLoad++;

        mFh.loadEFTransparent(EF_PLMN_W_ACT, obtainMessage(EVENT_GET_PLMN_W_ACT_DONE));
        mRecordsToLoad++;

        mFh.loadEFTransparent(EF_OPLMN_W_ACT, obtainMessage(EVENT_GET_OPLMN_W_ACT_DONE));
        mRecordsToLoad++;

        mFh.loadEFTransparent(EF_HPLMN_W_ACT, obtainMessage(EVENT_GET_HPLMN_W_ACT_DONE));
        mRecordsToLoad++;

        mFh.loadEFTransparent(EF_EHPLMN, obtainMessage(EVENT_GET_EHPLMN_DONE));
        mRecordsToLoad++;

        mFh.loadEFTransparent(EF_FPLMN, obtainMessage(
                    EVENT_GET_FPLMN_DONE, HANDLER_ACTION_NONE, -1));
        mRecordsToLoad++;

        loadEfLiAndEfPl();

        // XXX should seek instead of examining them all
        if (false) { // XXX
            mFh.loadEFLinearFixedAll(EF_SMS, obtainMessage(EVENT_GET_ALL_SMS_DONE));
            mRecordsToLoad++;
        }

        if (CRASH_RIL) {
            String sms = "0107912160130310f20404d0110041007030208054832b0120"
                         + "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
                         + "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
                         + "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
                         + "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
                         + "ffffffffffffffffffffffffffffff";
            byte[] ba = IccUtils.hexStringToBytes(sms);

            mFh.updateEFLinearFixed(EF_SMS, 1, ba, null,
                            obtainMessage(EVENT_MARK_SMS_READ_DONE, 1));
        }
        if (DBG) log("fetchSimRecords " + mRecordsToLoad + " requested: " + mRecordsRequested);
    }

    /**
     * Returns the SpnDisplayRule based on settings on the SIM and the
     * current service state. See TS 22.101 Annex A and TS 51.011 10.3.11
     * for details.
     *
     * If the SPN is not found on the SIM or is empty, the rule is
     * always PLMN_ONLY.
     *
     * @param serviceState Service state
     * @return the display rule
     *
     * @see #SPN_RULE_SHOW_SPN
     * @see #SPN_RULE_SHOW_PLMN
     */
    @Override
    public int getDisplayRule(ServiceState serviceState) {
        int rule;

        if (mParentApp != null && mParentApp.getUiccProfile() != null
                && mParentApp.getUiccProfile().getOperatorBrandOverride() != null) {
        // If the operator has been overridden, treat it as the SPN file on the SIM did not exist.
            rule = SPN_RULE_SHOW_PLMN;
        } else if (TextUtils.isEmpty(getServiceProviderName()) || mSpnDisplayCondition == -1) {
            // No EF_SPN content was found on the SIM, or not yet loaded.  Just show ONS.
            rule = SPN_RULE_SHOW_PLMN;
        } else if (useRoamingFromServiceState() ? !serviceState.getRoaming()
                : isOnMatchingPlmn(serviceState.getOperatorNumeric())) {
            rule = SPN_RULE_SHOW_SPN;
            if ((mSpnDisplayCondition & 0x01) == 0x01) {
                // ONS required when registered to HPLMN or PLMN in EF_SPDI
                rule |= SPN_RULE_SHOW_PLMN;
            }
        } else {
            rule = SPN_RULE_SHOW_PLMN;
            if ((mSpnDisplayCondition & 0x02) == 0x00) {
                // SPN required if not registered to HPLMN or PLMN in EF_SPDI
                rule |= SPN_RULE_SHOW_SPN;
            }
        }
        return rule;
    }

    private boolean useRoamingFromServiceState() {
        CarrierConfigManager configManager = (CarrierConfigManager)
                mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configManager != null) {
            PersistableBundle b = configManager.getConfigForSubId(
                    SubscriptionController.getInstance().getSubIdUsingPhoneId(
                    mParentApp.getPhoneId()));
            if (b != null && b.getBoolean(CarrierConfigManager
                    .KEY_SPN_DISPLAY_RULE_USE_ROAMING_FROM_SERVICE_STATE_BOOL)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if plmn is HPLMN or on the spdiNetworks list.
     */
    private boolean isOnMatchingPlmn(String plmn) {
        if (plmn == null) return false;

        if (plmn.equals(getOperatorNumeric())) {
            return true;
        }

        if (mSpdiNetworks != null) {
            for (String spdiNet : mSpdiNetworks) {
                if (plmn.equals(spdiNet)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * States of Get SPN Finite State Machine which only used by getSpnFsm()
     */
    private enum GetSpnFsmState {
        IDLE,               // No initialized
        INIT,               // Start FSM
        READ_SPN_3GPP,      // Load EF_SPN firstly
        READ_SPN_CPHS,      // Load EF_SPN_CPHS secondly
        READ_SPN_SHORT_CPHS // Load EF_SPN_SHORT_CPHS last
    }

    /**
     * Finite State Machine to load Service Provider Name , which can be stored
     * in either EF_SPN (3GPP), EF_SPN_CPHS, or EF_SPN_SHORT_CPHS (CPHS4.2)
     *
     * After starting, FSM will search SPN EFs in order and stop after finding
     * the first valid SPN
     *
     * If the FSM gets restart while waiting for one of
     * SPN EFs results (i.e. a SIM refresh occurs after issuing
     * read EF_CPHS_SPN), it will re-initialize only after
     * receiving and discarding the unfinished SPN EF result.
     *
     * @param start set true only for initialize loading
     * @param ar the AsyncResult from loadEFTransparent
     *        ar.exception holds exception in error
     *        ar.result is byte[] for data in success
     */
    private void getSpnFsm(boolean start, AsyncResult ar) {
        byte[] data;

        if (start) {
            // Check previous state to see if there is outstanding
            // SPN read
            if (mSpnState == GetSpnFsmState.READ_SPN_3GPP
                    || mSpnState == GetSpnFsmState.READ_SPN_CPHS
                    || mSpnState == GetSpnFsmState.READ_SPN_SHORT_CPHS
                    || mSpnState == GetSpnFsmState.INIT) {
                // Set INIT then return so the INIT code
                // will run when the outstanding read done.
                mSpnState = GetSpnFsmState.INIT;
                return;
            } else {
                mSpnState = GetSpnFsmState.INIT;
            }
        }

        switch(mSpnState){
            case INIT:
                setServiceProviderName(null);

                mFh.loadEFTransparent(EF_SPN,
                        obtainMessage(EVENT_GET_SPN_DONE));
                mRecordsToLoad++;

                mSpnState = GetSpnFsmState.READ_SPN_3GPP;
                break;
            case READ_SPN_3GPP:
                if (ar != null && ar.exception == null) {
                    data = (byte[]) ar.result;
                    mSpnDisplayCondition = 0xff & data[0];

                    setServiceProviderName(IccUtils.adnStringFieldToString(
                                data, 1, data.length - 1));
                    // for card double-check and brand override
                    // we have to do this:
                    final String spn = getServiceProviderName();
                    // SPRD: save original SPN value.
                    mOriginalSpn = spn;

                    if (spn == null || spn.length() == 0) {
                        mSpnState = GetSpnFsmState.READ_SPN_CPHS;
                    } else {
                        if (DBG) log("Load EF_SPN: " + spn
                                + " spnDisplayCondition: " + mSpnDisplayCondition);
                        mTelephonyManager.setSimOperatorNameForPhone(
                                mParentApp.getPhoneId(), spn);

                        mSpnState = GetSpnFsmState.IDLE;
                    }
                } else {
                    mSpnState = GetSpnFsmState.READ_SPN_CPHS;
                }

                if (mSpnState == GetSpnFsmState.READ_SPN_CPHS) {
                    mFh.loadEFTransparent( EF_SPN_CPHS,
                            obtainMessage(EVENT_GET_SPN_DONE));
                    mRecordsToLoad++;

                    // See TS 51.011 10.3.11.  Basically, default to
                    // show PLMN always, and SPN also if roaming.
                    mSpnDisplayCondition = -1;
                }
                break;
            case READ_SPN_CPHS:
                if (ar != null && ar.exception == null) {
                    data = (byte[]) ar.result;

                    // XXX UNISOC: Some operators may write invalid data in EF_SPN_CPHS file, ignore.
                    String ons = IccUtils.adnStringFieldToString(data, 0, data.length);
                    if (!TeleUtils.isInvalidONS(ons)) {
                        setServiceProviderName(ons);
                    }
                    // for card double-check and brand override
                    // we have to do this:
                    final String spn = getServiceProviderName();

                    if (spn == null || spn.length() == 0) {
                        mSpnState = GetSpnFsmState.READ_SPN_SHORT_CPHS;
                    } else {
                        // Display CPHS Operator Name only when not roaming
                        mSpnDisplayCondition = 2;

                        if (DBG) log("Load EF_SPN_CPHS: " + spn);
                        mTelephonyManager.setSimOperatorNameForPhone(
                                mParentApp.getPhoneId(), spn);

                        mSpnState = GetSpnFsmState.IDLE;
                    }
                } else {
                    mSpnState = GetSpnFsmState.READ_SPN_SHORT_CPHS;
                }

                if (mSpnState == GetSpnFsmState.READ_SPN_SHORT_CPHS) {
                    mFh.loadEFTransparent(
                            EF_SPN_SHORT_CPHS, obtainMessage(EVENT_GET_SPN_DONE));
                    mRecordsToLoad++;
                }
                break;
            case READ_SPN_SHORT_CPHS:
                if (ar != null && ar.exception == null) {
                    data = (byte[]) ar.result;

                    setServiceProviderName(IccUtils.adnStringFieldToString(
                                data, 0, data.length));
                    // for card double-check and brand override
                    // we have to do this:
                    final String spn = getServiceProviderName();

                    if (spn == null || spn.length() == 0) {
                        if (DBG) log("No SPN loaded in either CHPS or 3GPP");
                    } else {
                        // Display CPHS Operator Name only when not roaming
                        mSpnDisplayCondition = 2;

                        if (DBG) log("Load EF_SPN_SHORT_CPHS: " + spn);
                        mTelephonyManager.setSimOperatorNameForPhone(
                                mParentApp.getPhoneId(), spn);
                    }
                } else {
                    setServiceProviderName(null);
                    if (DBG) log("No SPN loaded in either CHPS or 3GPP");
                }

                mSpnState = GetSpnFsmState.IDLE;
                break;
            default:
                mSpnState = GetSpnFsmState.IDLE;
        }
    }

    // SPRD: Add cache of original SPN value.
    public String getOriginalServiceProviderName() {
        return mOriginalSpn;
    }

    /**
     * Parse TS 51.011 EF[SPDI] record
     * This record contains the list of numeric network IDs that
     * are treated specially when determining SPN display
     */
    private void
    parseEfSpdi(byte[] data) {
        SimTlv tlv = new SimTlv(data, 0, data.length);

        byte[] plmnEntries = null;

        for ( ; tlv.isValidObject() ; tlv.nextObject()) {
            // Skip SPDI tag, if existant
            if (tlv.getTag() == TAG_SPDI) {
              tlv = new SimTlv(tlv.getData(), 0, tlv.getData().length);
            }
            // There should only be one TAG_SPDI_PLMN_LIST
            if (tlv.getTag() == TAG_SPDI_PLMN_LIST) {
                plmnEntries = tlv.getData();
                break;
            }
        }

        if (plmnEntries == null) {
            return;
        }

        mSpdiNetworks = new ArrayList<String>(plmnEntries.length / 3);

        for (int i = 0 ; i + 2 < plmnEntries.length ; i += 3) {
            String plmnCode;
            plmnCode = IccUtils.bcdPlmnToString(plmnEntries, i);

            // Valid operator codes are 5 or 6 digits
            if (plmnCode != null && plmnCode.length() >= 5) {
                log("EF_SPDI network: " + plmnCode);
                mSpdiNetworks.add(plmnCode);
            }
        }
    }

    /**
     * convert a byte array of packed plmns to an array of strings
     */
    private String[] parseBcdPlmnList(byte[] data, String description) {
        final int packedBcdPlmnLenBytes = 3;
        log("Received " + description + " PLMNs, raw=" + IccUtils.bytesToHexString(data));
        if (data.length == 0 || (data.length % packedBcdPlmnLenBytes) != 0) {
            loge("Received invalid " + description + " PLMN list");
            return null;
        }
        int numPlmns = data.length / packedBcdPlmnLenBytes;
        int numValidPlmns = 0;
        String[] parsed = new String[numPlmns];
        for (int i = 0; i < numPlmns; i++) {
            parsed[numValidPlmns] = IccUtils.bcdPlmnToString(data, i * packedBcdPlmnLenBytes);
            // we count the valid (non empty) records and only increment if valid
            if (!TextUtils.isEmpty(parsed[numValidPlmns])) numValidPlmns++;
        }
        String[] ret = Arrays.copyOf(parsed, numValidPlmns);
        if (VDBG) logv(description + " PLMNs: " + Arrays.toString(ret));
        return ret;
    }

    /**
     * check to see if Mailbox Number is allocated and activated in CPHS SST
     */
    private boolean isCphsMailboxEnabled() {
        if (mCphsInfo == null)  return false;
        return ((mCphsInfo[1] & CPHS_SST_MBN_MASK) == CPHS_SST_MBN_ENABLED );
    }

    @Override
    protected void log(String s) {
        Rlog.d(LOG_TAG, "[SIMRecords] " + s);
    }

    @Override
    protected void loge(String s) {
        Rlog.e(LOG_TAG, "[SIMRecords] " + s);
    }

    protected void logw(String s, Throwable tr) {
        Rlog.w(LOG_TAG, "[SIMRecords] " + s, tr);
    }

    protected void logv(String s) {
        Rlog.v(LOG_TAG, "[SIMRecords] " + s);
    }

    /**
     * Return true if "Restriction of menu options for manual PLMN selection"
     * bit is set or EF_CSP data is unavailable, return false otherwise.
     */
    @Override
    public boolean isCspPlmnEnabled() {
        return mCspPlmnEnabled;
    }

    /**
     * Parse EF_CSP data and check if
     * "Restriction of menu options for manual PLMN selection" is
     * Enabled/Disabled
     *
     * @param data EF_CSP hex data.
     */
    private void handleEfCspData(byte[] data) {
        // As per spec CPHS4_2.WW6, CPHS B.4.7.1, EF_CSP contains CPHS defined
        // 18 bytes (i.e 9 service groups info) and additional data specific to
        // operator. The valueAddedServicesGroup is not part of standard
        // services. This is operator specific and can be programmed any where.
        // Normally this is programmed as 10th service after the standard
        // services.
        int usedCspGroups = data.length / 2;
        // This is the "Service Group Number" of "Value Added Services Group".
        byte valueAddedServicesGroup = (byte)0xC0;

        mCspPlmnEnabled = true;
        for (int i = 0; i < usedCspGroups; i++) {
             if (data[2 * i] == valueAddedServicesGroup) {
                 log("[CSP] found ValueAddedServicesGroup, value " + data[(2 * i) + 1]);
                 if ((data[(2 * i) + 1] & 0x80) == 0x80) {
                     // Bit 8 is for
                     // "Restriction of menu options for manual PLMN selection".
                     // Operator Selection menu should be enabled.
                     mCspPlmnEnabled = true;
                 } else {
                     mCspPlmnEnabled = false;
                     // Operator Selection menu should be disabled.
                     // Operator Selection Mode should be set to Automatic.
                     log("[CSP] Set Automatic Network Selection");
                     mNetworkSelectionModeAutomaticRegistrants.notifyRegistrants();
                 }
                 return;
             }
        }

        log("[CSP] Value Added Service Group (0xC0), not found!");
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("SIMRecords: " + this);
        pw.println(" extends:");
        super.dump(fd, pw, args);
        pw.println(" mVmConfig=" + mVmConfig);
        pw.println(" mCallForwardingStatus=" + mCallForwardingStatus);
        pw.println(" mSpnState=" + mSpnState);
        pw.println(" mCphsInfo=" + mCphsInfo);
        pw.println(" mCspPlmnEnabled=" + mCspPlmnEnabled);
        pw.println(" mEfMWIS[]=" + Arrays.toString(mEfMWIS));
        pw.println(" mEfCPHS_MWI[]=" + Arrays.toString(mEfCPHS_MWI));
        pw.println(" mEfCff[]=" + Arrays.toString(mEfCff));
        pw.println(" mEfCfis[]=" + Arrays.toString(mEfCfis));
        pw.println(" mSpnDisplayCondition=" + mSpnDisplayCondition);
        pw.println(" mSpdiNetworks[]=" + mSpdiNetworks);
        pw.println(" mUsimServiceTable=" + mUsimServiceTable);
        pw.println(" mGid1=" + mGid1);
        if (mCarrierTestOverride.isInTestMode()) {
            pw.println(" mFakeGid1=" + mCarrierTestOverride.getFakeGid1());
        }
        pw.println(" mGid2=" + mGid2);
        if (mCarrierTestOverride.isInTestMode()) {
            pw.println(" mFakeGid2=" + mCarrierTestOverride.getFakeGid2());
        }
        pw.println(" mPnnHomeName=" + mPnnHomeName);
        if (mCarrierTestOverride.isInTestMode()) {
            pw.println(" mFakePnnHomeName=" + mCarrierTestOverride.getFakePnnHomeName());
        }
        pw.println(" mPlmnActRecords[]=" + Arrays.toString(mPlmnActRecords));
        pw.println(" mOplmnActRecords[]=" + Arrays.toString(mOplmnActRecords));
        pw.println(" mHplmnActRecords[]=" + Arrays.toString(mHplmnActRecords));
        pw.println(" mFplmns[]=" + Arrays.toString(mFplmns));
        pw.println(" mEhplmns[]=" + Arrays.toString(mEhplmns));
        pw.flush();
    }
}
