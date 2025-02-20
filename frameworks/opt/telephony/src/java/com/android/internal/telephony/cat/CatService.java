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

package com.android.internal.telephony.cat;

import static com.android.internal.telephony.cat.CatCmdMessage.SetupEventListConstants
        .IDLE_SCREEN_AVAILABLE_EVENT;
import static com.android.internal.telephony.cat.CatCmdMessage.SetupEventListConstants
        .LANGUAGE_SELECTION_EVENT;
import static com.android.internal.telephony.cat.CatCmdMessage.SetupEventListConstants
        .USER_ACTIVITY_EVENT;
import static com.android.internal.telephony.cat.CatCmdMessage.SetupEventListConstants
        .CALL_CONNECTED_EVENT;
import static com.android.internal.telephony.cat.CatCmdMessage.SetupEventListConstants
        .CALL_DISCONNECTED_EVENT;
import static com.android.internal.telephony.cat.CatCmdMessage.SetupEventListConstants
        .CHANNEL_STATUS_EVENT;
import static com.android.internal.telephony.cat.CatCmdMessage.SetupEventListConstants
        .DATA_AVAILABLE_EVENT;
import static com.android.internal.telephony.cat.CatCmdMessage.SetupEventListConstants
        .LOCATION_STATUS_EVENT;
import static com.android.internal.telephony.cat.CatCmdMessage.SetupEventListConstants
        .MT_CALL_EVENT;

import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.backup.BackupManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.LocaleList;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccRefreshResponse;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccProfile;
import com.android.sprd.telephony.RadioInteractor;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class RilMessage {
    int mId;
    Object mData;
    ResultCode mResCode;

    RilMessage(int msgId, String rawData) {
        mId = msgId;
        mData = rawData;
    }

    RilMessage(RilMessage other) {
        mId = other.mId;
        mData = other.mData;
        mResCode = other.mResCode;
    }
}

/**
 * Class that implements SIM Toolkit Telephony Service. Interacts with the RIL
 * and application.
 *
 * {@hide}
 */
public class CatService extends Handler implements AppInterface {
    private static final boolean DBG = false;

    // Class members
    private static IccRecords mIccRecords;
    private static UiccCardApplication mUiccApplication;

    // Service members.
    // Protects singleton instance lazy initialization.
    private static final Object sInstanceLock = new Object();
    private static CatService[] sInstance = null;
    private CommandsInterface mCmdIf;
    private Context mContext;
    private CatCmdMessage mCurrntCmd = null;
    private CatCmdMessage mMenuCmd = null;

    private RilMessageDecoder mMsgDecoder = null;
    private boolean mStkAppInstalled = false;

    private UiccController mUiccController;
    private CardState mCardState = CardState.CARDSTATE_ABSENT;

    // Service constants.
    protected static final int MSG_ID_SESSION_END              = 1;
    protected static final int MSG_ID_PROACTIVE_COMMAND        = 2;
    protected static final int MSG_ID_EVENT_NOTIFY             = 3;
    protected static final int MSG_ID_CALL_SETUP               = 4;
    static final int MSG_ID_REFRESH                  = 5;
    static final int MSG_ID_RESPONSE                 = 6;
    static final int MSG_ID_SIM_READY                = 7;

    protected static final int MSG_ID_ICC_CHANGED    = 8;
    protected static final int MSG_ID_ALPHA_NOTIFY   = 9;

    static final int MSG_ID_RIL_MSG_DECODED          = 10;

    // Events to signal SIM presence or absent in the device.
    private static final int MSG_ID_ICC_RECORDS_LOADED       = 20;

    //Events to signal SIM REFRESH notificatations
    private static final int MSG_ID_ICC_REFRESH  = 30;

    /* UNISOC:for Stk Feature @{ */
    private static final int MSG_ID_SEND_SECOND_DTMF = 50;
    private static final int MSG_ID_SEND_SERIAL_DTMF = 51;
    private static final int DTMF_INTERVAL = 2500;

    private static final int MSG_ID_SETUPMENU_DELAY = 60;
    private static final int SETUPMENU_INTERVAL = 500;
    /* UNISOC:for Stk Feature @{ */

    private static final int DEV_ID_KEYPAD      = 0x01;
    private static final int DEV_ID_DISPLAY     = 0x02;
    private static final int DEV_ID_UICC        = 0x81;
    private static final int DEV_ID_TERMINAL    = 0x82;
    private static final int DEV_ID_NETWORK     = 0x83;

    static final String STK_DEFAULT = "Default Message";

    private HandlerThread mHandlerThread;
    private int mSlotId;
    /* UNISOC: for REFRESH function @{ */
    private RadioInteractor mRadioInteractor;
    private boolean mRefreshReset = false;
    private boolean mIccRefreshRecieved = false;
    private boolean mUiccResetCmd = false;
    /* UNISOC: @} */

    /* For multisim catservice should not be singleton */
    private CatService(CommandsInterface ci, UiccCardApplication ca, IccRecords ir,
            Context context, IccFileHandler fh, UiccProfile uiccProfile, int slotId) {
        if (ci == null || ca == null || ir == null || context == null || fh == null
                || uiccProfile == null) {
            throw new NullPointerException(
                    "Service: Input parameters must not be null");
        }
        mCmdIf = ci;
        mContext = context;
        mSlotId = slotId;
        mHandlerThread = new HandlerThread("Cat Telephony service" + slotId);
        mHandlerThread.start();

        // Get the RilMessagesDecoder for decoding the messages.
        mMsgDecoder = RilMessageDecoder.getInstance(this, fh, slotId);
        if (null == mMsgDecoder) {
            CatLog.d(this, "Null RilMessageDecoder instance");
            return;
        }
        mMsgDecoder.start();

        // Register ril events handling.
        mCmdIf.setOnCatSessionEnd(this, MSG_ID_SESSION_END, null);
        mCmdIf.setOnCatProactiveCmd(this, MSG_ID_PROACTIVE_COMMAND, null);
        mCmdIf.setOnCatEvent(this, MSG_ID_EVENT_NOTIFY, null);
        mCmdIf.setOnCatCallSetUp(this, MSG_ID_CALL_SETUP, null);
        //mCmdIf.setOnSimRefresh(this, MSG_ID_REFRESH, null);

        mCmdIf.registerForIccRefresh(this, MSG_ID_ICC_REFRESH, null);
        mCmdIf.setOnCatCcAlphaNotify(this, MSG_ID_ALPHA_NOTIFY, null);

        mIccRecords = ir;
        mUiccApplication = ca;

        // Register for SIM ready event.
        mIccRecords.registerForRecordsLoaded(this, MSG_ID_ICC_RECORDS_LOADED, null);
        CatLog.d(this, "registerForRecordsLoaded slotid=" + mSlotId + " instance:" + this);


        mUiccController = UiccController.getInstance();
        mUiccController.registerForIccChanged(this, MSG_ID_ICC_CHANGED, null);
        mRadioInteractor = new RadioInteractor(mContext);

        // Check if STK application is available
        mStkAppInstalled = isStkAppInstalled();

        CatLog.d(this, "Running CAT service on Slotid: " + mSlotId +
                ". STK app installed:" + mStkAppInstalled);
    }

    /**
     * Used for instantiating the Service from the Card.
     *
     * @param ci CommandsInterface object
     * @param context phone app context
     * @param ic Icc card
     * @param slotId to know the index of card
     * @return The only Service object in the system
     */
    public static CatService getInstance(CommandsInterface ci,
            Context context, UiccProfile uiccProfile, int slotId) {
        UiccCardApplication ca = null;
        IccFileHandler fh = null;
        IccRecords ir = null;
        if (uiccProfile != null) {
            /* Since Cat is not tied to any application, but rather is Uicc application
             * in itself - just get first FileHandler and IccRecords object
             */
            ca = uiccProfile.getApplicationIndex(0);
            if (ca != null) {
                fh = ca.getIccFileHandler();
                ir = ca.getIccRecords();
            }
        }

        synchronized (sInstanceLock) {
            if (sInstance == null) {
                int simCount = TelephonyManager.getDefault().getSimCount();
                sInstance = new CatService[simCount];
                for (int i = 0; i < simCount; i++) {
                    sInstance[i] = null;
                }
            }

            if (slotId >= sInstance.length) {
                return null;
            }

            if (sInstance[slotId] == null) {
                if (ci == null || ca == null || ir == null || context == null || fh == null
                        || uiccProfile == null) {
                    return null;
                }

                sInstance[slotId] = new CatService(ci, ca, ir, context, fh, uiccProfile, slotId);
            } else if ((ir != null) && (mIccRecords != ir)) {
                if (mIccRecords != null) {
                    mIccRecords.unregisterForRecordsLoaded(sInstance[slotId]);
                }

                mIccRecords = ir;
                mUiccApplication = ca;

                mIccRecords.registerForRecordsLoaded(sInstance[slotId],
                        MSG_ID_ICC_RECORDS_LOADED, null);
                CatLog.d(sInstance[slotId], "registerForRecordsLoaded slotid=" + slotId
                        + " instance:" + sInstance[slotId]);
            }
            return sInstance[slotId];
        }
    }

    public void dispose() {
        if (!mUiccResetCmd && (mRadioInteractor != null && mRadioInteractor.getRealSimSatus(mSlotId) != 0)) {
            CatLog.d(this, "not uicc reset and sim disable but Sim is not absent");
            return;
        }
        synchronized (sInstanceLock) {
            CatLog.d(this, "Disposing CatService object");
            mIccRecords.unregisterForRecordsLoaded(this);

            // Clean up stk icon if dispose is called
            broadcastCardStateAndIccRefreshResp(CardState.CARDSTATE_ABSENT, null);

            mCmdIf.unSetOnCatSessionEnd(this);
            mCmdIf.unSetOnCatProactiveCmd(this);
            mCmdIf.unSetOnCatEvent(this);
            mCmdIf.unSetOnCatCallSetUp(this);
            mCmdIf.unSetOnCatCcAlphaNotify(this);

            mCmdIf.unregisterForIccRefresh(this);
            if (mUiccController != null) {
                mUiccController.unregisterForIccChanged(this);
                mUiccController = null;
            }
            mMsgDecoder.dispose();
            mMsgDecoder = null;
            mHandlerThread.quit();
            mHandlerThread = null;
            removeCallbacksAndMessages(null);
            mUiccResetCmd = false;
            if (sInstance != null) {
                if (SubscriptionManager.isValidSlotIndex(mSlotId)) {
                    sInstance[mSlotId] = null;
                } else {
                    CatLog.d(this, "error: invaild slot id: " + mSlotId);
                }
            }
        }
    }

    @Override
    protected void finalize() {
        CatLog.d(this, "Service finalized");
    }

    private void handleRilMsg(RilMessage rilMsg) {
        if (rilMsg == null) {
            return;
        }

        // dispatch messages
        CommandParams cmdParams = null;
        switch (rilMsg.mId) {
        case MSG_ID_EVENT_NOTIFY:
            if (rilMsg.mResCode == ResultCode.OK) {
                cmdParams = (CommandParams) rilMsg.mData;
                if (cmdParams != null) {
                    handleCommand(cmdParams, false);
                }
            }
            break;
        case MSG_ID_PROACTIVE_COMMAND:
            try {
                cmdParams = (CommandParams) rilMsg.mData;
            } catch (ClassCastException e) {
                // for error handling : cast exception
                CatLog.d(this, "Fail to parse proactive command");
                // Don't send Terminal Resp if command detail is not available
                CommandDetails cmdDet = (CommandDetails)rilMsg.mData;
                if (cmdDet != null) {
                    sendTerminalResponse(cmdDet, ResultCode.CMD_DATA_NOT_UNDERSTOOD,
                            false, 0x00, null);
                } else if (mCurrntCmd != null) {
                    sendTerminalResponse(mCurrntCmd.mCmdDet, ResultCode.CMD_DATA_NOT_UNDERSTOOD,
                            false, 0x00, null);
                }
                break;
            }
            if (cmdParams != null) {
                if (rilMsg.mResCode == ResultCode.OK) {
                    handleCommand(cmdParams, true);
                } else {
                    // for proactive commands that couldn't be decoded
                    // successfully respond with the code generated by the
                    // message decoder.
                    sendTerminalResponse(cmdParams.mCmdDet, rilMsg.mResCode,
                            false, 0, null);
                }
            }
            break;
        case MSG_ID_REFRESH:
            cmdParams = (CommandParams) rilMsg.mData;
            if (cmdParams != null) {
                handleCommand(cmdParams, false);
            }
            break;
        case MSG_ID_SESSION_END:
            handleSessionEnd();
            break;
        case MSG_ID_CALL_SETUP:
            // prior event notify command supplied all the information
            // needed for set up call processing.
            break;
        }
    }

    /**
     * This function validates the events in SETUP_EVENT_LIST which are currently
     * supported by the Android framework. In case of SETUP_EVENT_LIST has NULL events
     * or no events, all the events need to be reset.
     */
    private boolean isSupportedSetupEventCommand(CatCmdMessage cmdMsg) {
        boolean flag = true;

        for (int eventVal: cmdMsg.getSetEventList().eventList) {
            CatLog.d(this,"Event: " + eventVal);
            switch (eventVal) {
                /* Currently android is supporting only the below events in SetupEventList
                 * Language Selection.  */
                case IDLE_SCREEN_AVAILABLE_EVENT:
                case LANGUAGE_SELECTION_EVENT:
                case USER_ACTIVITY_EVENT:
                case MT_CALL_EVENT:
                case CALL_CONNECTED_EVENT:
                case CALL_DISCONNECTED_EVENT:
                case LOCATION_STATUS_EVENT:
                    flag = true;
                    break;
                default:
                    flag = false;
            }
        }
        return flag;
    }

    /**
     * Handles RIL_UNSOL_STK_EVENT_NOTIFY or RIL_UNSOL_STK_PROACTIVE_COMMAND command
     * from RIL.
     * Sends valid proactive command data to the application using intents.
     * RIL_REQUEST_STK_SEND_TERMINAL_RESPONSE will be send back if the command is
     * from RIL_UNSOL_STK_PROACTIVE_COMMAND.
     */
    private void handleCommand(CommandParams cmdParams, boolean isProactiveCmd) {
        CatLog.d(this, cmdParams.getCommandType().name());

        // Log all proactive commands.
        if (isProactiveCmd) {
            if (mUiccController != null) {
                mUiccController.addCardLog("ProactiveCommand mSlotId=" + mSlotId +
                        " cmdParams=" + cmdParams);
            }
        }

        CharSequence message;
        ResultCode resultCode;
        CatCmdMessage cmdMsg = new CatCmdMessage(cmdParams);
        switch (cmdParams.getCommandType()) {
            case SET_UP_MENU:
                if (removeMenu(cmdMsg.getMenu())) {
                    mMenuCmd = null;
                } else {
                    mMenuCmd = cmdMsg;
                }
                resultCode = cmdParams.mLoadIconFailed ? ResultCode.PRFRMD_ICON_NOT_DISPLAYED
                                                                            : ResultCode.OK;
                sendTerminalResponse(cmdParams.mCmdDet, resultCode, false, 0, null);
                break;
            case DISPLAY_TEXT:
                /* UNISOC: USAT 27.22.4.1.1/2 DISPLAY TEXT normal priority @{ */
                if (cmdMsg.geTextMessage().isHighPriority == false) {
                    CatLog.d(this,  "<" + mSlotId + ">" + "[stk] DISPLAY_TEXT is normal Priority");
                    boolean display_flag = isCurrentCanDisplayText();
                    CatLog.d(this,  "<" + mSlotId + ">" + "[stkapp]display_flag = " + display_flag);
                    if (!display_flag) {
                        sendTerminalResponse(cmdParams.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS,
                                true, AddinfoMeProblem.SCREEN_BUSY.value(), null);
                        return;
                    }
                }
                /* @} */
                break;
            case REFRESH:
                // ME side only handles refresh commands which meant to remove IDLE
                // MODE TEXT.
                //cmdParams.mCmdDet.typeOfCommand = CommandType.SET_UP_IDLE_MODE_TEXT.value();
                // Add here for REFRESH function
                mCurrntCmd = cmdMsg;
                /*@{ Bug757405:  when the refresh happen with type session reset and application reset in SIM card,
                 * AP should send the TR with speical result value (PRFRMD_NAA_NOT_ACTIVE(0x08)) and do nothing.
                 * cp will not report MSG_ID_ICC_REFRESH */
                if (mUiccApplication != null) {
                    if (mUiccApplication.getType() == AppType.APPTYPE_SIM
                            && (mCurrntCmd.mCmdDet.commandQualifier == CommandParamsFactory.REFRESH_NAA_APP_RESET
                            || mCurrntCmd.mCmdDet.commandQualifier == CommandParamsFactory.REFRESH_NAA_SESSION_RESET)) {
                        CatLog.d(this, "the type is session reset or application reset in SIM card ");
                        sendTerminalResponse(cmdParams.mCmdDet, ResultCode.PRFRMD_NAA_NOT_ACTIVE, false, 0, null);
                        return;
                    }
                }
                /* @} */
                if (mCurrntCmd.mCmdDet.commandQualifier == CommandParamsFactory.REFRESH_UICC_RESET) {
                    mUiccResetCmd = true;
                }
                CatLog.d(this, "mIccRefreshRecieved = " + mIccRefreshRecieved);
                if (mIccRefreshRecieved && mCurrntCmd.mCmdDet.commandQualifier
                        != CommandParamsFactory.REFRESH_UICC_RESET) {
                    handleRefreshCmdResponse(true);
                }
                break;
            case SET_UP_IDLE_MODE_TEXT:
                //resultCode = cmdParams.mLoadIconFailed ? ResultCode.PRFRMD_ICON_NOT_DISPLAYED
                //                                                           : ResultCode.OK;
                //sendTerminalResponse(cmdParams.mCmdDet,resultCode, false, 0, null);
                /* UNISOC: Modify for STK case 27.22.4.22.2/4 @{ */
                CatLog.d(this,  "<" + mSlotId+ ">" +
                        "icon = "                 + ((DisplayTextParams)cmdParams).mTextMsg.icon +
                        " iconSelfExplanatory = " + ((DisplayTextParams)cmdParams).mTextMsg.iconSelfExplanatory +
                        " text = "                + ((DisplayTextParams)cmdParams).mTextMsg.text);
                if(((DisplayTextParams)cmdParams).mTextMsg.icon != null
                        && ((DisplayTextParams)cmdParams).mTextMsg.iconSelfExplanatory == false
                        && ((DisplayTextParams)cmdParams).mTextMsg.text == null){
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.CMD_DATA_NOT_UNDERSTOOD, false,0, null);
                    return;
                } else {
                    resultCode = cmdParams.mLoadIconFailed ? ResultCode.PRFRMD_ICON_NOT_DISPLAYED
                            : ResultCode.OK;
                    sendTerminalResponse(cmdParams.mCmdDet, resultCode, false,0, null);
                }
                /* @} */
                break;
            case SET_UP_EVENT_LIST:
                if (isSupportedSetupEventCommand(cmdMsg)) {
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, null);
                } else {
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.BEYOND_TERMINAL_CAPABILITY,
                            false, 0, null);
                }
                break;
            case PROVIDE_LOCAL_INFORMATION:
                ResponseData resp;
                switch (cmdParams.mCmdDet.commandQualifier) {
                    case CommandParamsFactory.DTTZ_SETTING:
                        resp = new DTTZResponseData(null);
                        sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, resp);
                        break;
                    case CommandParamsFactory.LANGUAGE_SETTING:
                        resp = new LanguageResponseData(Locale.getDefault().getLanguage());
                        sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, resp);
                        break;
                    default:
                        sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, null);
                }
                // No need to start STK app here.
                return;
            case LAUNCH_BROWSER:
                if ((((LaunchBrowserParams) cmdParams).mConfirmMsg.text != null)
                        && (((LaunchBrowserParams) cmdParams).mConfirmMsg.text.equals(STK_DEFAULT))) {
                    message = mContext.getText(com.android.internal.R.string.launchBrowserDefault);
                    ((LaunchBrowserParams) cmdParams).mConfirmMsg.text = message.toString();
                }
                break;
            case SELECT_ITEM:
            case GET_INPUT:
            case GET_INKEY:
                break;
            case SEND_DTMF:
                /* UNISOC: for DTMF function @{ */
                DtmfMessage dtmfMessage = cmdMsg.getDtmfMessage();
                retrieveDtmfString(cmdParams, dtmfMessage.mdtmfString);
                /* @} */
                break;
            case SEND_SMS:
            case SEND_SS:
            case SEND_USSD:
                if ((((DisplayTextParams)cmdParams).mTextMsg.text != null)
                        && (((DisplayTextParams)cmdParams).mTextMsg.text.equals(STK_DEFAULT))) {
                    message = mContext.getText(com.android.internal.R.string.sending);
                    ((DisplayTextParams)cmdParams).mTextMsg.text = message.toString();
                }
                break;
            case PLAY_TONE:
                break;
            case SET_UP_CALL:
                if ((((CallSetupParams) cmdParams).mConfirmMsg.text != null)
                        && (((CallSetupParams) cmdParams).mConfirmMsg.text.equals(STK_DEFAULT))) {
                    message = mContext.getText(com.android.internal.R.string.SetupCallDefault);
                    ((CallSetupParams) cmdParams).mConfirmMsg.text = message.toString();
                }
                break;
            case LANGUAGE_NOTIFICATION:
                String language = ((LanguageParams) cmdParams).mLanguage;
                ResultCode result = ResultCode.OK;
                if (language != null && language.length() > 0) {
                    try {
                        changeLanguage(language);
                    } catch (RemoteException e) {
                        result = ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS;
                    }
                }
                sendTerminalResponse(cmdParams.mCmdDet, result, false, 0, null);
                return;
            case OPEN_CHANNEL:
            case RECEIVE_DATA:
            case SEND_DATA:
                BIPClientParams cmd = (BIPClientParams) cmdParams;
                /* Per 3GPP specification 102.223,
                 * if the alpha identifier is not provided by the UICC,
                 * the terminal MAY give information to the user
                 * noAlphaUsrCnf defines if you need to show user confirmation or not
                 */
                boolean noAlphaUsrCnf = false;
                try {
                    noAlphaUsrCnf = mContext.getResources().getBoolean(
                            com.android.internal.R.bool.config_stkNoAlphaUsrCnf);
                } catch (NotFoundException e) {
                    noAlphaUsrCnf = false;
                }
                if ((cmd.mTextMsg.text == null) && (cmd.mHasAlphaId || noAlphaUsrCnf)) {
                    CatLog.d(this, "cmd " + cmdParams.getCommandType() + " with null alpha id");
                    // If alpha length is zero, we just respond with OK.
                    if (isProactiveCmd) {
                        sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, null);
                    } else if (cmdParams.getCommandType() == CommandType.OPEN_CHANNEL) {
                        mCmdIf.handleCallSetupRequestFromSim(true, null);
                    }
                    return;
                }
                // Respond with permanent failure to avoid retry if STK app is not present.
                if (!mStkAppInstalled) {
                    CatLog.d(this, "No STK application found.");
                    if (isProactiveCmd) {
                        sendTerminalResponse(cmdParams.mCmdDet,
                                             ResultCode.BEYOND_TERMINAL_CAPABILITY,
                                             false, 0, null);
                        return;
                    }
                }
                /*
                 * CLOSE_CHANNEL, RECEIVE_DATA and SEND_DATA can be delivered by
                 * either PROACTIVE_COMMAND or EVENT_NOTIFY.
                 * If PROACTIVE_COMMAND is used for those commands, send terminal
                 * response here.
                 */
                if (isProactiveCmd &&
                    ((cmdParams.getCommandType() == CommandType.CLOSE_CHANNEL) ||
                     (cmdParams.getCommandType() == CommandType.RECEIVE_DATA) ||
                     (cmdParams.getCommandType() == CommandType.SEND_DATA))) {
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, null);
                }
                break;
            case CLOSE_CHANNEL:
                break;
            default:
                CatLog.d(this, "Unsupported command");
                return;
        }
        mCurrntCmd = cmdMsg;
        broadcastCatCmdIntent(cmdMsg);
    }


    private void broadcastCatCmdIntent(CatCmdMessage cmdMsg) {
        CatLog.d(this, "broadcastCatCmdIntent isStkAppInstalled: " + isStkAppInstalled());
        if (isStkAppInstalled()) {
            Intent intent = new Intent(AppInterface.CAT_CMD_ACTION);
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            intent.putExtra("STK CMD", cmdMsg);
            intent.putExtra("SLOT_ID", mSlotId);
            intent.setComponent(AppInterface.getDefaultSTKApplication());
            CatLog.d(this, "Sending CmdMsg: " + cmdMsg + " on slotid:" + mSlotId);
            mContext.sendBroadcast(intent, AppInterface.STK_PERMISSION);
        } else {
            broadcastCatCmdIntentDelay(cmdMsg);
        }
    }

    /**
     * Handles RIL_UNSOL_STK_SESSION_END unsolicited command from RIL.
     *
     */
    private void handleSessionEnd() {
        CatLog.d(this, "SESSION END on "+ mSlotId);

        mCurrntCmd = mMenuCmd;
        Intent intent = new Intent(AppInterface.CAT_SESSION_END_ACTION);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.putExtra("SLOT_ID", mSlotId);
        intent.setComponent(AppInterface.getDefaultSTKApplication());
        mContext.sendBroadcast(intent, AppInterface.STK_PERMISSION);
    }


    private void sendTerminalResponse(CommandDetails cmdDet,
            ResultCode resultCode, boolean includeAdditionalInfo,
            int additionalInfo, ResponseData resp) {

        if (cmdDet == null) {
            return;
        }
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        Input cmdInput = null;
        if (mCurrntCmd != null) {
            cmdInput = mCurrntCmd.geInput();
        }

        // command details
        int tag = ComprehensionTlvTag.COMMAND_DETAILS.value();
        if (cmdDet.compRequired) {
            tag |= 0x80;
        }
        buf.write(tag);
        buf.write(0x03); // length
        buf.write(cmdDet.commandNumber);
        buf.write(cmdDet.typeOfCommand);
        buf.write(cmdDet.commandQualifier);

        // device identities
        // According to TS102.223/TS31.111 section 6.8 Structure of
        // TERMINAL RESPONSE, "For all SIMPLE-TLV objects with Min=N,
        // the ME should set the CR(comprehension required) flag to
        // comprehension not required.(CR=0)"
        // Since DEVICE_IDENTITIES and DURATION TLVs have Min=N,
        // the CR flag is not set.
        //tag = ComprehensionTlvTag.DEVICE_IDENTITIES.value();
        tag = 0x80 | ComprehensionTlvTag.DEVICE_IDENTITIES.value();
        buf.write(tag);
        buf.write(0x02); // length
        buf.write(DEV_ID_TERMINAL); // source device id
        buf.write(DEV_ID_UICC); // destination device id

        // result
        tag = ComprehensionTlvTag.RESULT.value();
        if (cmdDet.compRequired) {
            tag |= 0x80;
        }
        buf.write(tag);
        int length = includeAdditionalInfo ? 2 : 1;
        buf.write(length);
        buf.write(resultCode.value());

        // additional info
        if (includeAdditionalInfo) {
            buf.write(additionalInfo);
        }

        // Fill optional data for each corresponding command
        if (resp != null) {
            resp.format(buf);
        } else {
            encodeOptionalTags(cmdDet, resultCode, cmdInput, buf);
        }

        byte[] rawData = buf.toByteArray();
        String hexString = IccUtils.bytesToHexString(rawData);
        if (DBG) {
            CatLog.d(this, "TERMINAL RESPONSE: " + hexString);
        }

        mCmdIf.sendTerminalResponse(hexString, null);
    }

    private void encodeOptionalTags(CommandDetails cmdDet,
            ResultCode resultCode, Input cmdInput, ByteArrayOutputStream buf) {
        CommandType cmdType = AppInterface.CommandType.fromInt(cmdDet.typeOfCommand);
        if (cmdType != null) {
            switch (cmdType) {
                case GET_INPUT:
                case GET_INKEY:
                    // Please refer to the clause 6.8.21 of ETSI 102.223.
                    // The terminal shall supply the command execution duration
                    // when it issues TERMINAL RESPONSE for GET INKEY command with variable timeout.
                    // GET INPUT command should also be handled in the same manner.
                    if ((resultCode.value() == ResultCode.NO_RESPONSE_FROM_USER.value()) &&
                        (cmdInput != null) && (cmdInput.duration != null)) {
                        getInKeyResponse(buf, cmdInput);
                    }
                    break;
                case PROVIDE_LOCAL_INFORMATION:
                    if ((cmdDet.commandQualifier == CommandParamsFactory.LANGUAGE_SETTING) &&
                        (resultCode.value() == ResultCode.OK.value())) {
                        getPliResponse(buf);
                    }
                    break;
                default:
                    CatLog.d(this, "encodeOptionalTags() Unsupported Cmd details=" + cmdDet);
                    break;
            }
        } else {
            CatLog.d(this, "encodeOptionalTags() bad Cmd details=" + cmdDet);
        }
    }

    private void getInKeyResponse(ByteArrayOutputStream buf, Input cmdInput) {
        int tag = ComprehensionTlvTag.DURATION.value();

        buf.write(tag);
        buf.write(0x02); // length
        buf.write(cmdInput.duration.timeUnit.SECOND.value()); // Time (Unit,Seconds)
        buf.write(cmdInput.duration.timeInterval); // Time Duration
    }

    private void getPliResponse(ByteArrayOutputStream buf) {
        // Locale Language Setting
        final String lang = Locale.getDefault().getLanguage();

        if (lang != null) {
            // tag
            int tag = ComprehensionTlvTag.LANGUAGE.value();
            buf.write(tag);
            ResponseData.writeLength(buf, lang.length());
            buf.write(lang.getBytes(), 0, lang.length());
        }
    }

    private void sendMenuSelection(int menuId, boolean helpRequired) {

        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        // tag
        int tag = BerTlv.BER_MENU_SELECTION_TAG;
        buf.write(tag);

        // length
        buf.write(0x00); // place holder

        // device identities
        tag = 0x80 | ComprehensionTlvTag.DEVICE_IDENTITIES.value();
        buf.write(tag);
        buf.write(0x02); // length
        buf.write(DEV_ID_KEYPAD); // source device id
        buf.write(DEV_ID_UICC); // destination device id

        // item identifier
        tag = 0x80 | ComprehensionTlvTag.ITEM_ID.value();
        buf.write(tag);
        buf.write(0x01); // length
        buf.write(menuId); // menu identifier chosen

        // help request
        if (helpRequired) {
            tag = ComprehensionTlvTag.HELP_REQUEST.value();
            buf.write(tag);
            buf.write(0x00); // length
        }

        byte[] rawData = buf.toByteArray();

        // write real length
        int len = rawData.length - 2; // minus (tag + length)
        rawData[1] = (byte) len;

        String hexString = IccUtils.bytesToHexString(rawData);

        mCmdIf.sendEnvelope(hexString, null);
    }

    private void eventDownload(int event, int sourceId, int destinationId,
            byte[] additionalInfo, boolean oneShot) {

        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        // tag
        int tag = BerTlv.BER_EVENT_DOWNLOAD_TAG;
        buf.write(tag);

        // length
        buf.write(0x00); // place holder, assume length < 128.

        // event list
        tag = 0x80 | ComprehensionTlvTag.EVENT_LIST.value();
        buf.write(tag);
        buf.write(0x01); // length
        buf.write(event); // event value

        // device identities
        tag = 0x80 | ComprehensionTlvTag.DEVICE_IDENTITIES.value();
        buf.write(tag);
        buf.write(0x02); // length
        buf.write(sourceId); // source device id
        buf.write(destinationId); // destination device id

        /*
         * Check for type of event download to be sent to UICC - Browser
         * termination,Idle screen available, User activity, Language selection
         * etc as mentioned under ETSI TS 102 223 section 7.5
         */

        /*
         * Currently the below events are supported:
         * Language Selection Event.
         * Other event download commands should be encoded similar way
         */
        /* TODO: eventDownload should be extended for other Envelope Commands */
        switch (event) {
            case IDLE_SCREEN_AVAILABLE_EVENT:
                CatLog.d(sInstance, " Sending Idle Screen Available event download to ICC");
                break;
            case LANGUAGE_SELECTION_EVENT:
                CatLog.d(sInstance, " Sending Language Selection event download to ICC");
                tag = 0x80 | ComprehensionTlvTag.LANGUAGE.value();
                buf.write(tag);
                // Language length should be 2 byte
                buf.write(0x02);
                break;
            case USER_ACTIVITY_EVENT:
                break;
            default:
                break;
        }

        // additional information
        if (additionalInfo != null) {
            for (byte b : additionalInfo) {
                buf.write(b);
            }
        }

        byte[] rawData = buf.toByteArray();

        // write real length
        int len = rawData.length - 2; // minus (tag + length)
        rawData[1] = (byte) len;

        String hexString = IccUtils.bytesToHexString(rawData);

        CatLog.d(this, "ENVELOPE COMMAND: " + hexString);

        mCmdIf.sendEnvelope(hexString, null);
    }

    /**
     * Used by application to get an AppInterface object.
     *
     * @return The only Service object in the system
     */
    //TODO Need to take care for MSIM
    public static AppInterface getInstance() {
        int slotId = PhoneConstants.DEFAULT_CARD_INDEX;
        SubscriptionController sControl = SubscriptionController.getInstance();
        if (sControl != null) {
            slotId = sControl.getSlotIndex(sControl.getDefaultSubId());
        }
        return getInstance(null, null, null, slotId);
    }

    /**
     * Used by application to get an AppInterface object.
     *
     * @return The only Service object in the system
     */
    public static AppInterface getInstance(int slotId) {
        return getInstance(null, null, null, slotId);
    }

    @Override
    public void handleMessage(Message msg) {
        CatLog.d(this, "handleMessage[" + msg.what + "]");

        switch (msg.what) {
        case MSG_ID_SESSION_END:
        case MSG_ID_PROACTIVE_COMMAND:
        case MSG_ID_EVENT_NOTIFY:
        case MSG_ID_REFRESH:
            CatLog.d(this, "ril message arrived,slotid:" + mSlotId);
            String data = null;
            if (msg.obj != null) {
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar != null && ar.result != null) {
                    try {
                        data = (String) ar.result;
                    } catch (ClassCastException e) {
                        break;
                    }
                }
            }
            mMsgDecoder.sendStartDecodingMessageParams(new RilMessage(msg.what, data));
            break;
        case MSG_ID_CALL_SETUP:
            mMsgDecoder.sendStartDecodingMessageParams(new RilMessage(msg.what, null));
            break;
        case MSG_ID_ICC_RECORDS_LOADED:
            break;
        case MSG_ID_RIL_MSG_DECODED:
            handleRilMsg((RilMessage) msg.obj);
            break;
        case MSG_ID_RESPONSE:
            handleCmdResponse((CatResponseMessage) msg.obj);
            break;
        case MSG_ID_ICC_CHANGED:
            CatLog.d(this, "MSG_ID_ICC_CHANGED");
            updateIccAvailability();
            break;
        case MSG_ID_ICC_REFRESH:
            if (msg.obj != null) {
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar != null && ar.result != null) {
                    /* UNISOC: for REFRESH function @{ */
                    mIccRefreshRecieved = true;
                    IccRefreshResponse response = (IccRefreshResponse) ar.result;
                    CatLog.d(this, "<" + mSlotId + ">" + "MSG_ID_ICC_REFRESH result = "
                            + response.refreshResult);
                    if (IccRefreshResponse.REFRESH_RESULT_FILE_UPDATE == response.refreshResult
                            || IccRefreshResponse.REFRESH_RESULT_INIT == response.refreshResult
                            || IccRefreshResponse.REFRESH_RESULT_RESET == response.refreshResult) {
                        if (IccRefreshResponse.REFRESH_RESULT_RESET == response.refreshResult) {
                            mRefreshReset = true;
                        }
                        handleRefreshCmdResponse(true);// Success
                    } else {
                        handleRefreshCmdResponse(false);// Fail
                    }
                    /* @} */
                    broadcastCardStateAndIccRefreshResp(CardState.CARDSTATE_PRESENT,
                                  (IccRefreshResponse) ar.result);
                } else {
                    CatLog.d(this,"Icc REFRESH with exception: " + ar.exception);
                }
            } else {
                CatLog.d(this, "IccRefresh Message is null");
            }
            break;
        case MSG_ID_ALPHA_NOTIFY:
            CatLog.d(this, "Received CAT CC Alpha message from card");
            if (msg.obj != null) {
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar != null && ar.result != null) {
                    String alphaStr = (String)ar.result;
                    broadcastAlphaMessage(retrieveAlphaString(alphaStr));
                } else {
                    CatLog.d(this, "CAT Alpha message: ar.result is null");
                }
            } else {
                CatLog.d(this, "CAT Alpha message: msg.obj is null");
            }
            break;
        case MSG_ID_SEND_SECOND_DTMF:
            CommandParams cmdParams = (CommandParams) msg.obj;
            String str = msg.getData().getString("dtmf");
            retrieveDtmfString(cmdParams, str);
            break;
        case MSG_ID_SEND_SERIAL_DTMF:
            AsyncResult dtmfAr = (AsyncResult) msg.obj;
            Message msgAr = (Message) dtmfAr.userObj;
            CommandParams cmdParamAr = (CommandParams) msgAr.obj;
            String arStr = msgAr.getData().getString("dtmf");
            retrieveDtmfString(cmdParamAr, arStr);
            break;
        case MSG_ID_SETUPMENU_DELAY:
            CatCmdMessage cmdMsg = (CatCmdMessage)msg.obj;
            broadcastCatCmdIntent(cmdMsg) ;
            break;
        default:
            throw new AssertionError("Unrecognized CAT command: " + msg.what);
        }
    }

    /**
     ** This function sends a CARD status (ABSENT, PRESENT, REFRESH) to STK_APP.
     ** This is triggered during ICC_REFRESH or CARD STATE changes. In case
     ** REFRESH, additional information is sent in 'refresh_result'
     **
     **/
    private void  broadcastCardStateAndIccRefreshResp(CardState cardState,
            IccRefreshResponse iccRefreshState) {
        Intent intent = new Intent(AppInterface.CAT_ICC_STATUS_CHANGE);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        boolean cardPresent = (cardState == CardState.CARDSTATE_PRESENT);

        if (iccRefreshState != null) {
            //This case is when MSG_ID_ICC_REFRESH is received.
            intent.putExtra(AppInterface.REFRESH_RESULT, iccRefreshState.refreshResult);
            CatLog.d(this, "Sending IccResult with Result: "
                    + iccRefreshState.refreshResult);
        }

        // This sends an intent with CARD_ABSENT (0 - false) /CARD_PRESENT (1 - true).
        intent.putExtra(AppInterface.CARD_STATUS, cardPresent);
        intent.setComponent(AppInterface.getDefaultSTKApplication());
        intent.putExtra("SLOT_ID", mSlotId);
        CatLog.d(this, "Sending Card Status: "
                + cardState + " " + "cardPresent: " + cardPresent +  "SLOT_ID: " +  mSlotId);
        mContext.sendBroadcast(intent, AppInterface.STK_PERMISSION);
    }

    private void broadcastAlphaMessage(String alphaString) {
        CatLog.d(this, "Broadcasting CAT Alpha message from card: " + alphaString);
        Intent intent = new Intent(AppInterface.CAT_ALPHA_NOTIFY_ACTION);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.putExtra(AppInterface.ALPHA_STRING, alphaString);
        intent.putExtra("SLOT_ID", mSlotId);
        intent.setComponent(AppInterface.getDefaultSTKApplication());
        mContext.sendBroadcast(intent, AppInterface.STK_PERMISSION);
    }

    @Override
    public synchronized void onCmdResponse(CatResponseMessage resMsg) {
        if (resMsg == null) {
            return;
        }
        // queue a response message.
        Message msg = obtainMessage(MSG_ID_RESPONSE, resMsg);
        msg.sendToTarget();
    }

    private boolean validateResponse(CatResponseMessage resMsg) {
        boolean validResponse = false;
        if ((resMsg.mCmdDet.typeOfCommand == CommandType.SET_UP_EVENT_LIST.value())
                || (resMsg.mCmdDet.typeOfCommand == CommandType.SET_UP_MENU.value())) {
            CatLog.d(this, "CmdType: " + resMsg.mCmdDet.typeOfCommand);
            validResponse = true;
        } else if (mCurrntCmd != null) {
            validResponse = resMsg.mCmdDet.compareTo(mCurrntCmd.mCmdDet);
            CatLog.d(this, "isResponse for last valid cmd: " + validResponse);
        }
        return validResponse;
    }

    private boolean removeMenu(Menu menu) {
        try {
            if (menu.items.size() == 1 && menu.items.get(0) == null) {
                return true;
            }
        } catch (NullPointerException e) {
            CatLog.d(this, "Unable to get Menu's items size");
            return true;
        }
        return false;
    }

    private void handleCmdResponse(CatResponseMessage resMsg) {
        // Make sure the response details match the last valid command. An invalid
        // response is a one that doesn't have a corresponding proactive command
        // and sending it can "confuse" the baseband/ril.
        // One reason for out of order responses can be UI glitches. For example,
        // if the application launch an activity, and that activity is stored
        // by the framework inside the history stack. That activity will be
        // available for relaunch using the latest application dialog
        // (long press on the home button). Relaunching that activity can send
        // the same command's result again to the CatService and can cause it to
        // get out of sync with the SIM. This can happen in case of
        // non-interactive type Setup Event List and SETUP_MENU proactive commands.
        // Stk framework would have already sent Terminal Response to Setup Event
        // List and SETUP_MENU proactive commands. After sometime Stk app will send
        // Envelope Command/Event Download. In which case, the response details doesn't
        // match with last valid command (which are not related).
        // However, we should allow Stk framework to send the message to ICC.
        if (!validateResponse(resMsg)) {
            return;
        }
        ResponseData resp = null;
        boolean helpRequired = false;
        CommandDetails cmdDet = resMsg.getCmdDetails();
        AppInterface.CommandType type = AppInterface.CommandType.fromInt(cmdDet.typeOfCommand);

        switch (resMsg.mResCode) {
        case HELP_INFO_REQUIRED:
            helpRequired = true;
            // fall through
        case OK:
        case PRFRMD_WITH_PARTIAL_COMPREHENSION:
        case PRFRMD_WITH_MISSING_INFO:
        case PRFRMD_WITH_ADDITIONAL_EFS_READ:
        case PRFRMD_ICON_NOT_DISPLAYED:
        case PRFRMD_MODIFIED_BY_NAA:
        case PRFRMD_LIMITED_SERVICE:
        case PRFRMD_WITH_MODIFICATION:
        case PRFRMD_NAA_NOT_ACTIVE:
        case PRFRMD_TONE_NOT_PLAYED:
        case LAUNCH_BROWSER_ERROR:
        case TERMINAL_CRNTLY_UNABLE_TO_PROCESS:
            switch (type) {
            case SET_UP_MENU:
                helpRequired = resMsg.mResCode == ResultCode.HELP_INFO_REQUIRED;
                sendMenuSelection(resMsg.mUsersMenuSelection, helpRequired);
                return;
            case SELECT_ITEM:
                resp = new SelectItemResponseData(resMsg.mUsersMenuSelection);
                break;
            case GET_INPUT:
            case GET_INKEY:
                Input input = mCurrntCmd.geInput();
                if (!input.yesNo) {
                    // when help is requested there is no need to send the text
                    // string object.
                    if (!helpRequired) {
                        resp = new GetInkeyInputResponseData(resMsg.mUsersInput,
                                input.ucs2, input.packed);
                    }
                } else {
                    resp = new GetInkeyInputResponseData(
                            resMsg.mUsersYesNoSelection);
                }
                break;
            case DISPLAY_TEXT:
                if (resMsg.mResCode == ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS) {
                    // For screenbusy case there will be addtional information in the terminal
                    // response. And the value of the additional information byte is 0x01.
                    resMsg.setAdditionalInfo(0x01);
                } else {
                    resMsg.mIncludeAdditionalInfo = false;
                    resMsg.mAdditionalInfo = 0;
                }
                break;
            case LAUNCH_BROWSER:
                if (resMsg.mResCode == ResultCode.LAUNCH_BROWSER_ERROR) {
                    // Additional info for Default URL unavailable.
                    resMsg.setAdditionalInfo(0x04);
                } else {
                    resMsg.mIncludeAdditionalInfo = false;
                    resMsg.mAdditionalInfo = 0;
                }
                break;
            // 3GPP TS.102.223: Open Channel alpha confirmation should not send TR
            case OPEN_CHANNEL:
            case SET_UP_CALL:
                mCmdIf.handleCallSetupRequestFromSim(resMsg.mUsersConfirm, null);
                // No need to send terminal response for SET UP CALL. The user's
                // confirmation result is send back using a dedicated ril message
                // invoked by the CommandInterface call above.
                mCurrntCmd = null;
                return;
            case SET_UP_EVENT_LIST:
                if (IDLE_SCREEN_AVAILABLE_EVENT == resMsg.mEventValue) {
                    eventDownload(resMsg.mEventValue, DEV_ID_DISPLAY, DEV_ID_UICC,
                            resMsg.mAddedInfo, false);
                 } else {
                    /* UNISOC: for EVENTDOWNLOAD function @{ */
                    if (resMsg.mAddedInfo == null) {
                        byte[] addedInfo = null;
                        ByteArrayOutputStream buf = new ByteArrayOutputStream();
                        int tag;
                        switch (resMsg.mEventValue) {
                            case DATA_AVAILABLE_EVENT:
                            case CHANNEL_STATUS_EVENT:
                            case USER_ACTIVITY_EVENT:
                                break;
                            default:
                                CatLog.d(this, "<" + mSlotId + ">" + "unknown event");
                                return;
                        }
                        eventDownload(resMsg.mEventValue, DEV_ID_TERMINAL, DEV_ID_UICC,
                                addedInfo, false);
                    } else {
                        eventDownload(resMsg.mEventValue, DEV_ID_TERMINAL, DEV_ID_UICC,
                                resMsg.mAddedInfo, false);
                    }
                }
                /* @} */
                // No need to send the terminal response after event download.
                return;
            default:
                break;
            }
            break;
        case BACKWARD_MOVE_BY_USER:
        case USER_NOT_ACCEPT:
            // if the user dismissed the alert dialog for a
            // setup call/open channel, consider that as the user
            // rejecting the call. Use dedicated API for this, rather than
            // sending a terminal response.
            if (type == CommandType.SET_UP_CALL || type == CommandType.OPEN_CHANNEL) {
                mCmdIf.handleCallSetupRequestFromSim(false, null);
                mCurrntCmd = null;
                return;
            } else {
                resp = null;
            }
            break;
        case NO_RESPONSE_FROM_USER:
            // No need to send terminal response for SET UP CALL on user timeout,
            // instead use dedicated API
            if (type == CommandType.SET_UP_CALL) {
                mCmdIf.handleCallSetupRequestFromSim(false, null);
                mCurrntCmd = null;
                return;
            }
        case UICC_SESSION_TERM_BY_USER:
            resp = null;
            break;
        default:
            return;
        }
        sendTerminalResponse(cmdDet, resMsg.mResCode, resMsg.mIncludeAdditionalInfo,
                resMsg.mAdditionalInfo, resp);
        mCurrntCmd = null;
    }

    private boolean isStkAppInstalled() {
        Intent intent = new Intent(AppInterface.CAT_CMD_ACTION);
        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> broadcastReceivers =
                            pm.queryBroadcastReceivers(intent, PackageManager.GET_META_DATA);
        int numReceiver = broadcastReceivers == null ? 0 : broadcastReceivers.size();

        return (numReceiver > 0);
    }

    public void update(CommandsInterface ci,
            Context context, UiccProfile uiccProfile) {
        UiccCardApplication ca = null;
        IccRecords ir = null;

        if (uiccProfile != null) {
            /* Since Cat is not tied to any application, but rather is Uicc application
             * in itself - just get first FileHandler and IccRecords object
             */
            ca = uiccProfile.getApplicationIndex(0);
            if (ca != null) {
                ir = ca.getIccRecords();
            }
        }

        synchronized (sInstanceLock) {
            if ((ir != null) && (mIccRecords != ir)) {
                if (mIccRecords != null) {
                    mIccRecords.unregisterForRecordsLoaded(this);
                }

                CatLog.d(this,
                        "Reinitialize the Service with SIMRecords and UiccCardApplication");
                mIccRecords = ir;
                mUiccApplication = ca;

                // re-Register for SIM ready event.
                mIccRecords.registerForRecordsLoaded(this, MSG_ID_ICC_RECORDS_LOADED, null);
                CatLog.d(this, "registerForRecordsLoaded slotid=" + mSlotId + " instance:" + this);
            }
        }
    }

    void updateIccAvailability() {
        if (null == mUiccController) {
            return;
        }

        CardState newState = CardState.CARDSTATE_ABSENT;
        UiccCard newCard = mUiccController.getUiccCard(mSlotId);
        if (newCard != null) {
            newState = newCard.getCardState();
        }
        CardState oldState = mCardState;
        mCardState = newState;
        CatLog.d(this,"New Card State = " + newState + " " + "Old Card State = " + oldState);
        if (oldState == CardState.CARDSTATE_PRESENT &&
                newState != CardState.CARDSTATE_PRESENT) {
            if (!mUiccResetCmd && (mRadioInteractor != null && mRadioInteractor.getRealSimSatus(mSlotId) != 0)) {
                CatLog.d(this, "not uicc reset and sim disable but Sim is not absent");
                return;
            }
            broadcastCardStateAndIccRefreshResp(newState, null);
        } else if (oldState != CardState.CARDSTATE_PRESENT &&
                newState == CardState.CARDSTATE_PRESENT) {
            // Card moved to PRESENT STATE.
            mCmdIf.reportStkServiceIsRunning(null);
        }
    }

    private void changeLanguage(String language) throws RemoteException {
        IActivityManager am = ActivityManagerNative.getDefault();
        Configuration config = am.getConfiguration();
        config.setLocales(new LocaleList(new Locale(language), LocaleList.getDefault()));
        config.userSetLocale = true;
        am.updatePersistentConfiguration(config);
        BackupManager.dataChanged("com.android.providers.settings");
    }

    /* UNISOC: for REFRESH function @{ */
    private void handleRefreshCmdResponse(boolean result) {
        CatLog.d(this, "<" + mSlotId + ">" + "handleRefreshCmdResponse enter" + " result = "
                + result);
        if (mCurrntCmd == null || (mCurrntCmd != null
                && AppInterface.CommandType.fromInt(mCurrntCmd.getCmdDet().typeOfCommand) != AppInterface.CommandType.REFRESH)) {
            CatLog.d(this, "<" + mSlotId + ">" + "handleRefreshCmdResponse mCurrntCmd is NULL");
            mRefreshReset = false;
            return;
        }
        CommandDetails cmdDet = mCurrntCmd.getCmdDet();

        ResultCode resCode = ResultCode.OK;
        if (result) {
            resCode = ResultCode.OK;
            if (!mRefreshReset && mCurrntCmd.mCmdDet.commandQualifier != CommandParamsFactory.REFRESH_UICC_RESET) {
                //uicc reset no need to send TR
                sendTerminalResponse(cmdDet, resCode, false, 0, null);
            }
            mRefreshReset = false;
        } else {
            resCode = ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS;
            sendTerminalResponse(cmdDet, resCode, true,
                    AddinfoMeProblem.SCREEN_BUSY.value(), null);
        }
        mIccRefreshRecieved = false;
        mCurrntCmd = null;
    }

    private void broadcastCatCmdIntentDelay(CatCmdMessage cmdMsg) {
        Message msg = this.obtainMessage(MSG_ID_SETUPMENU_DELAY);
        msg.obj = cmdMsg;
        this.sendMessageDelayed(msg, SETUPMENU_INTERVAL);
    }
    /* @｝ */
    /* UNISOC: for DTMF function @{ */
    private void retrieveDtmfString(CommandParams cmdParams, String dtmf) {
        if (phoneIsIdle()) {
            sendTerminalResponse(cmdParams.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS,
                    true, AddinfoMeProblem.NOT_IN_SPEECH_CALL.value(), null);
        } else {
            String dtmfTemp = new String(dtmf);
            CatLog.d(this, "dtmfTemp = " + dtmfTemp);

            if (dtmfTemp != null && dtmfTemp.length() > 0) {
                String firstStr = dtmfTemp.substring(0, 1);
                Message msg = new Message();
                Bundle bundle = new Bundle();
                bundle.putString("dtmf", dtmf.substring(1, dtmf.length()));
                msg.what = MSG_ID_SEND_SECOND_DTMF;
                msg.obj = cmdParams;
                msg.setData(bundle);

                if (firstStr.equals("P")) {
                    this.sendMessageDelayed(msg, DTMF_INTERVAL);
                    return;
                } else {
                    mCmdIf.sendDtmf(firstStr.charAt(0), obtainMessage(MSG_ID_SEND_SERIAL_DTMF, msg));
                }
            } else {
                sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, null);
            }
        }
    }

    public boolean isIdleBySubId(int subId) {
        final Phone phone = PhoneFactory.getPhone(subId);
        if (phone != null) {
            return (phone.getState() == PhoneConstants.State.IDLE);
        } else {
            return false;
        }
    }

    private boolean phoneIsIdle() {
        boolean isIdle = true;
        for (int i = 0; i < TelephonyManager.getDefault().getSimCount(); i++) {
            isIdle = isIdleBySubId(i);
            if (false == isIdle) {
                return isIdle;
            }
        }
        return isIdle;
    }
    /* @｝ */
    /* UNISOC: USAT 27.22.4.1.1/2 DISPLAY TEXT normal priority @{ */
    private boolean isCurrentCanDisplayText() {
        try {
            List<RunningTaskInfo> mRunningTaskInfoList = (List<RunningTaskInfo>)ActivityManagerNative.getDefault().getTasks(1);
            if(null == mRunningTaskInfoList || mRunningTaskInfoList.isEmpty()){
                CatLog.d(this, "<" + mSlotId+ ">" + "mRunningTaskInfoList is NULL!");
                return false;
            }
            int mListSize = mRunningTaskInfoList.size();
            CatLog.d(this, "<" + mSlotId+ ">" + "[stk]isCurrentCanDisplayText trace mListSize = " + mListSize);
            if(mListSize > 0) {
                ComponentName cn = mRunningTaskInfoList.get(0).topActivity;
                CatLog.d(this, "<" + mSlotId + ">" + "[stk]isCurrentCanDisplayText cn is " + cn);
                boolean result = ((cn.getClassName().indexOf("com.android.stk") != -1))
                        || isHome(cn);
                return result;
            }
        } catch (RemoteException e) {
            CatLog.d(this, "<" + mSlotId+ ">" + "[stk]isCurrentCanDisplayText exception");
        }
        return false;
    }

    private List<String> getHomes() {
        List<String> names = new ArrayList<String>();
        PackageManager packageManager = mContext.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(
                intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo ri : resolveInfo) {
            names.add(ri.activityInfo.packageName);
            System.out.println(ri.activityInfo.packageName);
        }
        return names;
    }

    public boolean isHome(ComponentName m) {
        String packagename = m.getPackageName();
        List<String> name = getHomes();
        for (String i : name) {
            if (packagename.equals(i))
                return true;
        }
        return false;
    }
    /* @｝ */

    private String retrieveAlphaString(String alphaString) {
        byte[] rawValue = IccUtils.hexStringToBytes(alphaString);
        int valueIndex = 0;
        int length = rawValue.length;
        if (length == 0) {
            CatLog.d(this, "retrieveAlphaString rawValue is null");
            return null;
        }
        try {
            return IccUtils.adnStringFieldToString(rawValue, valueIndex,
                    length);
        } catch (IndexOutOfBoundsException e) {
            CatLog.d(this, "IndexOutOfBoundsException e = " + e);
            return null;
        }
    }
}

