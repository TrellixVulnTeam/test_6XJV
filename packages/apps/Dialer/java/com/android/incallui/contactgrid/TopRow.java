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

package com.android.incallui.contactgrid;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import com.android.dialer.common.Assert;
import com.android.dialer.common.LogUtil;
import com.android.incallui.call.CallList;
import com.android.incallui.call.DialerCall;
import com.android.incallui.call.DialerCall.State;
import com.android.incallui.incall.protocol.PrimaryCallState;
import com.android.incallui.incall.protocol.PrimaryInfo;
import com.android.incallui.sprd.InCallUiUtils;
import com.android.incallui.videotech.utils.SessionModificationState;
import com.android.incallui.videotech.utils.VideoUtils;

/**
 * Gets the content of the top row. For example:
 *
 * <ul>
 *   <li>Captain Holt ON HOLD
 *   <li>Calling...
 *   <li>[Wi-Fi icon] Calling via Starbucks Wi-Fi
 *   <li>[Wi-Fi icon] Starbucks Wi-Fi
 *   <li>Call from
 * </ul>
 */
public class TopRow {

  /** Content of the top row. */
  public static class Info {

    @Nullable public final CharSequence label;
    @Nullable public final Drawable icon;
    public final boolean labelIsSingleLine;

    public Info(@Nullable CharSequence label, @Nullable Drawable icon, boolean labelIsSingleLine) {
      this.label = label;
      this.icon = icon;
      this.labelIsSingleLine = labelIsSingleLine;
    }
  }

  private TopRow() {}

  public static Info getInfo(Context context, PrimaryCallState state, PrimaryInfo primaryInfo) {
    CharSequence label = null;
    Drawable icon = state.connectionIcon();
    boolean labelIsSingleLine = true;

    if (state.isWifi() && icon == null) {
      icon = context.getDrawable(R.drawable.quantum_ic_network_wifi_vd_theme_24);
    }

    if (state.state() == State.INCOMING || state.state() == State.CALL_WAITING) {
      // Call from
      // [Wi-Fi icon] Video call from
      // Hey Jake, pick up!
      if (!TextUtils.isEmpty(state.callSubject())) {
        label = state.callSubject();
        labelIsSingleLine = false;
      } else {
        label = getLabelForIncoming(context, state);
        // Show phone number if it's not displayed in name (center row) or location field (bottom
        // row).
        if (shouldShowNumber(primaryInfo, true /* isIncoming */) && !state.isConference()) {
          label = TextUtils.concat(label, " ", spanDisplayNumber(primaryInfo.number()));
        }
      }

      /* SPRD Feature Porting: show main/vice card feature. @{ */
      String accountlabel = InCallUiUtils.getPhoneAccountLabel(
              CallList.getInstance().getFirstCall(), context);
      label = getLabelWithSlotInfo(context, primaryInfo,
              R.string.contact_grid_incoming_via_template, accountlabel); // UNISOC: modify for bug923017
      /* @} */

    } else if (VideoUtils.hasSentVideoUpgradeRequest(state.sessionModificationState())
        || VideoUtils.hasReceivedVideoUpgradeRequest(state.sessionModificationState())) {
      label = getLabelForVideoRequest(context, state);
    } else if (state.state() == State.PULLING) {
      label = context.getString(R.string.incall_transferring);
    } else if (state.state() == State.DIALING || state.state() == State.CONNECTING) {
      // [Wi-Fi icon] Calling via Google Guest
      // Calling...
      /* SPRD Feature Porting: show main/vice card feature.
       * @orig
      label = getLabelForDialing(context, state); */
      DialerCall firstCall = CallList.getInstance().getFirstCall();
      // UNISOC: modify for bug900303
      if (firstCall != null && (primaryInfo.subId() != -1) && !firstCall.isEmergencyCall()) {//add for bug940820
        String accountLabel = InCallUiUtils.getPhoneAccountLabel(firstCall, context);
        label = getLabelWithSlotInfo(context, primaryInfo,R.string.incall_calling_via_template,accountLabel);//modify for bug913838
      } else {
        label = getLabelForDialing(context, state, firstCall.isEmergencyCall());
      }
      /* @} */
    } else if (state.state() == State.ACTIVE && state.isRemotelyHeld()) {
      label = context.getString(R.string.incall_remotely_held);
      // SPRD Feature Porting: show main/vice card feature.
      DialerCall firstCall = CallList.getInstance().getFirstCall();
      if (firstCall != null && !firstCall.isEmergencyCall()) {
        label = InCallUiUtils.getSlotInfoBySubId(context, primaryInfo.subId()) + label;
      }
    } else if (state.state() == State.ACTIVE
        && shouldShowNumber(primaryInfo, false /* isIncoming */) && !state.isConference()) {
      label = spanDisplayNumber(primaryInfo.number());
    } else if (state.state() == State.CALL_PENDING && !TextUtils.isEmpty(state.customLabel())) {
      label = state.customLabel();
    } else if (state.state() == State.ACTIVE) {
      // SPRD Feature Porting: show main/vice card feature.
      DialerCall firstCall = CallList.getInstance().getFirstCall();
      if (firstCall != null && !firstCall.isEmergencyCall()) {
        String accountLabel = InCallUiUtils.getPhoneAccountLabel(
                CallList.getInstance().getFirstCall(), context);
        if(primaryInfo.subId() > 0){ //modify for bug913838
          label = InCallUiUtils.getSlotInfoBySubId(context, primaryInfo.subId()) + accountLabel;
        }else {
          label = InCallUiUtils.getSlotInfoByCall(context, firstCall)+ accountLabel;
        }
      }
      // UNISOC: add for bug900490
    } else if (state.state() == State.ONHOLD) {
      DialerCall firstCall = CallList.getInstance().getActiveOrBackgroundCall();
      if (firstCall != null && !firstCall.isEmergencyCall()) {
        // UNISOC: modify for bug907744
        label = InCallUiUtils.getPhoneAccountLabel(firstCall, context);
        if (primaryInfo.subId() > 0) {
          label = InCallUiUtils.getSlotInfoBySubId(context, primaryInfo.subId()) + label;
        } else {
          label = InCallUiUtils.getSlotInfoByCall(context, firstCall) + label;
        }
      }
    } else {
      // Video calling...
      // [Wi-Fi icon] Starbucks Wi-Fi
      DialerCall firstCall = CallList.getInstance().getFirstCall();
      if(firstCall != null && !firstCall.isEmergencyCall()) {//add for bug940820,bug946437
        label = getConnectionLabel(state);
      }
    }

    return new Info(label, icon, labelIsSingleLine);
  }

  private static CharSequence spanDisplayNumber(String displayNumber) {
    return PhoneNumberUtils.createTtsSpannable(
        BidiFormatter.getInstance().unicodeWrap(displayNumber, TextDirectionHeuristics.LTR));
  }

  private static boolean shouldShowNumber(PrimaryInfo primaryInfo, boolean isIncoming) {
    if (primaryInfo.nameIsNumber()) {
      return false;
    }
    // Don't show number since it's already shown in bottom row of incoming screen if there is no
    // location info.
    if (primaryInfo.location() == null && isIncoming) {
      return false;
    }
    if (primaryInfo.isLocalContact() && !isIncoming) {
      return false;
    }
    if (TextUtils.isEmpty(primaryInfo.number())) {
      return false;
    }
    return true;
  }

  private static CharSequence getLabelForIncoming(Context context, PrimaryCallState state) {
    if (state.isVideoCall()) {
      return getLabelForIncomingVideo(context, state.sessionModificationState(), state.isWifi());
    } else if (state.isWifi() && !TextUtils.isEmpty(state.connectionLabel())) {
      return state.connectionLabel();
    } else if (isAccount(state)) {
      return context.getString(
          R.string.contact_grid_incoming_via_template, state.connectionLabel());
    } else if (state.isWorkCall()) {
      return context.getString(R.string.contact_grid_incoming_work_call);
    } else {
      return context.getString(R.string.contact_grid_incoming_voice_call);
    }
  }

  private static CharSequence getLabelForIncomingVideo(
      Context context, @SessionModificationState int sessionModificationState, boolean isWifi) {
    if (sessionModificationState == SessionModificationState.RECEIVED_UPGRADE_TO_VIDEO_REQUEST) {
      if (isWifi) {
        return context.getString(R.string.contact_grid_incoming_wifi_video_call);
      } else {
        return context.getString(R.string.contact_grid_incoming_video_call);
      }
    } else {
      if (isWifi) {
        return context.getString(R.string.contact_grid_incoming_wifi_video_call);
      } else {
        return context.getString(R.string.contact_grid_incoming_video_call);
      }
    }
  }

  private static CharSequence getLabelForDialing(Context context, PrimaryCallState state, boolean isEmergency) { //modify for bug940820
    if (!TextUtils.isEmpty(state.connectionLabel()) && !state.isWifi()&&!isEmergency) {
      return context.getString(R.string.incall_calling_via_template, state.connectionLabel());
    } else {
      if (state.isVideoCall()) {
        if (state.isWifi()) {
          return context.getString(R.string.incall_wifi_video_call_requesting);
        } else {
          return context.getString(R.string.incall_video_call_requesting);
        }
      }

      if (state.isAssistedDialed() && state.assistedDialingExtras() != null) {
        LogUtil.i("TopRow.getLabelForDialing", "using assisted dialing label.");
        String countryCode =
            String.valueOf(state.assistedDialingExtras().transformedNumberCountryCallingCode());
        return context.getString(
            R.string.incall_connecting_assited_dialed,
            countryCode,
            state.assistedDialingExtras().userHomeCountryCode());
      }
      return context.getString(R.string.incall_connecting);
    }
  }

  private static CharSequence getConnectionLabel(PrimaryCallState state) {
    if (!TextUtils.isEmpty(state.connectionLabel())
        && (isAccount(state) || state.isWifi() || state.isConference())) {
      // We normally don't show a "call state label" at all when active
      // (but we can use the call state label to display the provider name).
      return state.connectionLabel();
    } else {
      return null;
    }
  }

  private static CharSequence getLabelForVideoRequest(Context context, PrimaryCallState state) {
    switch (state.sessionModificationState()) {
      case SessionModificationState.WAITING_FOR_UPGRADE_TO_VIDEO_RESPONSE:
        return context.getString(R.string.incall_video_call_upgrade_request);
      case SessionModificationState.REQUEST_FAILED:
      case SessionModificationState.UPGRADE_TO_VIDEO_REQUEST_FAILED:
        return context.getString(R.string.incall_video_call_request_failed);
      case SessionModificationState.REQUEST_REJECTED:
        return context.getString(R.string.incall_video_call_request_rejected);
      case SessionModificationState.UPGRADE_TO_VIDEO_REQUEST_TIMED_OUT:
        return context.getString(R.string.incall_video_call_request_timed_out);
      case SessionModificationState.RECEIVED_UPGRADE_TO_VIDEO_REQUEST:
        return getLabelForIncomingVideo(context, state.sessionModificationState(), state.isWifi());
      case SessionModificationState.NO_REQUEST:
      default:
        Assert.fail();
        return null;
    }
  }

  private static boolean isAccount(PrimaryCallState state) {
    return !TextUtils.isEmpty(state.connectionLabel()) && TextUtils.isEmpty(state.gatewayNumber());
  }

  //add for bug913838
  private static CharSequence getLabelWithSlotInfo(Context context, PrimaryInfo primaryInfo,
                                                   int templateId, String connectionLabel) {
    if (primaryInfo.subId() > 0) {
      return context.getString(templateId, InCallUiUtils.getSlotInfoBySubId(
                            context, primaryInfo.subId()) + connectionLabel);
      } else {
      return context.getString(templateId, InCallUiUtils.getSlotInfoByCall(
                            context, CallList.getInstance().getFirstCall()) + connectionLabel);
      }
  }
}
