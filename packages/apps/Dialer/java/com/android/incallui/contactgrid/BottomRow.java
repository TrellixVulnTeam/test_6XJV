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
import android.support.annotation.Nullable;
import android.telephony.PhoneNumberUtils;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import com.android.incallui.call.DialerCall.State;
import com.android.incallui.incall.protocol.PrimaryCallState;
import com.android.incallui.incall.protocol.PrimaryInfo;
import com.android.incallui.sprd.plugin.voiceclearcode.VoiceClearCodeHelper;
import com.android.incallui.sprd.plugin.CallerAddress.CallerAddressHelper;

/**
 * Gets the content of the bottom row. For example:
 *
 * <ul>
 *   <li>Mobile +1 (650) 253-0000
 *   <li>[HD attempting icon]/[HD icon] 00:15
 *   <li>Call ended
 *   <li>Hanging up
 * </ul>
 */
public class BottomRow {

  /** Content of the bottom row. */
  public static class Info {

    @Nullable public final CharSequence label;
    public final boolean isTimerVisible;
    public final boolean isWorkIconVisible;
    public final boolean isHdAttemptingIconVisible;
    public final boolean isHdIconVisible;
    public final boolean isForwardIconVisible;
    public final boolean isSpamIconVisible;
    public final boolean shouldPopulateAccessibilityEvent;

    public Info(
        @Nullable CharSequence label,
        boolean isTimerVisible,
        boolean isWorkIconVisible,
        boolean isHdAttemptingIconVisible,
        boolean isHdIconVisible,
        boolean isForwardIconVisible,
        boolean isSpamIconVisible,
        boolean shouldPopulateAccessibilityEvent) {
      this.label = label;
      this.isTimerVisible = isTimerVisible;
      this.isWorkIconVisible = isWorkIconVisible;
      this.isHdAttemptingIconVisible = isHdAttemptingIconVisible;
      this.isHdIconVisible = isHdIconVisible;
      this.isForwardIconVisible = isForwardIconVisible;
      this.isSpamIconVisible = isSpamIconVisible;
      this.shouldPopulateAccessibilityEvent = shouldPopulateAccessibilityEvent;
    }
  }

  private BottomRow() {}

  public static Info getInfo(Context context, PrimaryCallState state, PrimaryInfo primaryInfo) {
    CharSequence label;
    boolean isForwardIconVisible = state.isForwardedNumber();
    boolean isWorkIconVisible = state.isWorkCall();
    boolean isHdIconVisible = state.isHdAudioCall() && !isForwardIconVisible;
    boolean isHdAttemptingIconVisible = state.isHdAttempting();
    boolean isSpamIconVisible = false;
    boolean shouldPopulateAccessibilityEvent = true;

    /* SPRD FEATURE: FL1000060550 Continue call timer when primary call on hold.
    boolean isTimerVisible = state.state() == State.ACTIVE; */
    boolean isTimerVisible;
    if (context.getResources().getBoolean(R.bool.config_is_show_calltimer_when_call_onhold)) {
      isTimerVisible = state.state() == State.ACTIVE || state.state() == State.ONHOLD;
    } else {
      isTimerVisible = state.state() == State.ACTIVE;
    }

    if (isIncoming(state) && primaryInfo.isSpam()) {
      label = context.getString(R.string.contact_grid_incoming_suspected_spam);
      isSpamIconVisible = true;
      isHdIconVisible = false;
    } else if (state.state() == State.DISCONNECTING) {
      // While in the DISCONNECTING state we display a "Hanging up" message in order to make the UI
      // feel more responsive.  (In GSM it's normal to see a delay of a couple of seconds while
      // negotiating the disconnect with the network, so the "Hanging up" state at least lets the
      // user know that we're doing something.  This state is currently not used with CDMA.)
      label = context.getString(R.string.incall_hanging_up);
      /* add for bug900292 */
      isHdIconVisible = false;
      isHdAttemptingIconVisible = false;
    } else if (state.state() == State.DISCONNECTED) {
      label = state.disconnectCause().getLabel();
      if (TextUtils.isEmpty(label)) {
        label = context.getString(R.string.incall_call_ended);
      } else {
        /* SPRD Feature Porting: Voice Clear Code Feature. @{ */
        VoiceClearCodeHelper callFailCauseHelper = VoiceClearCodeHelper
                .getInstance(context);
        if (callFailCauseHelper.isVoiceClearCodeLabel(label.toString())) {
          label = context.getString(R.string.incall_call_ended);
        }
      }
      /* add for bug900292 */
      isHdIconVisible = false;
      isHdAttemptingIconVisible = false;
    } else {
      /* SPRD Feature Porting: Display caller address for phone number feature. @{ */
      if (CallerAddressHelper.getsInstance(context).isSupportCallerAddress() && !PhoneNumberUtils.isEmergencyNumber(primaryInfo.number())) {
        label = "";
      } else {
        label = getLabelForPhoneNumber(primaryInfo, state);
        shouldPopulateAccessibilityEvent = primaryInfo.nameIsNumber();
      }
      /* @} */
    }

    return new Info(
        label,
        isTimerVisible,
        isWorkIconVisible,
        isHdAttemptingIconVisible,
        isHdIconVisible,
        isForwardIconVisible,
        isSpamIconVisible,
        shouldPopulateAccessibilityEvent);
  }

  private static CharSequence getLabelForPhoneNumber(PrimaryInfo primaryInfo, PrimaryCallState state) {
    if (primaryInfo.location() != null) {
      return primaryInfo.location();
    }
    //UNISOC: add for bug903504
    if (!primaryInfo.nameIsNumber() && !TextUtils.isEmpty(primaryInfo.number())
            && (!state.isConference() || state.state() == State.INCOMING) && state.state() != State.IDLE) {
      CharSequence spannedNumber = spanDisplayNumber(primaryInfo.number());
      if (primaryInfo.label() == null) {
        return spannedNumber;
      } else {
        return TextUtils.concat(primaryInfo.label(), " ", spannedNumber);
      }
    }
    return null;
  }

  private static CharSequence spanDisplayNumber(String displayNumber) {
    return PhoneNumberUtils.createTtsSpannable(
        BidiFormatter.getInstance().unicodeWrap(displayNumber, TextDirectionHeuristics.LTR));
  }

  private static boolean isIncoming(PrimaryCallState state) {
    return state.state() == State.INCOMING || state.state() == State.CALL_WAITING;
  }
}
