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
 * limitations under the License
 */

package com.android.incallui.incall.protocol;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Ids for buttons in the in call UI. */
@Retention(RetentionPolicy.SOURCE)
@IntDef({
  InCallButtonIds.BUTTON_AUDIO,
  InCallButtonIds.BUTTON_MUTE,
  InCallButtonIds.BUTTON_DIALPAD,
  InCallButtonIds.BUTTON_HOLD,
  InCallButtonIds.BUTTON_SWAP,
  InCallButtonIds.BUTTON_UPGRADE_TO_VIDEO,
  InCallButtonIds.BUTTON_SWITCH_CAMERA,
  InCallButtonIds.BUTTON_DOWNGRADE_TO_AUDIO,
  InCallButtonIds.BUTTON_ADD_CALL,
  InCallButtonIds.BUTTON_MERGE,
  InCallButtonIds.BUTTON_PAUSE_VIDEO,
  InCallButtonIds.BUTTON_MANAGE_VIDEO_CONFERENCE,
  InCallButtonIds.BUTTON_MANAGE_VOICE_CONFERENCE,
  InCallButtonIds.BUTTON_SWITCH_TO_SECONDARY,
  InCallButtonIds.BUTTON_SWAP_SIM,
  InCallButtonIds.BUTTON_RECORD,
  InCallButtonIds.BUTTON_SEND_MESSAGE,
  InCallButtonIds.BUTTON_HANGUP_ALL,
  InCallButtonIds.BUTTON_ECT,
  InCallButtonIds.BUTTON_INVITE,
  InCallButtonIds.BUTTON_CHANGE_VIDEO_TYPE,
  InCallButtonIds.BUTTON_COUNT,
})
public @interface InCallButtonIds {

  int BUTTON_AUDIO = 0;
  int BUTTON_MUTE = 1;
  int BUTTON_DIALPAD = 2;
  int BUTTON_HOLD = 3;
  int BUTTON_SWAP = 4;
  int BUTTON_UPGRADE_TO_VIDEO = 5;
  int BUTTON_SWITCH_CAMERA = 6;
  int BUTTON_DOWNGRADE_TO_AUDIO = 7;
  int BUTTON_ADD_CALL = 8;
  int BUTTON_MERGE = 9;
  int BUTTON_PAUSE_VIDEO = 10;
  int BUTTON_MANAGE_VIDEO_CONFERENCE = 11;
  int BUTTON_MANAGE_VOICE_CONFERENCE = 12;
  int BUTTON_SWITCH_TO_SECONDARY = 13;
  int BUTTON_SWAP_SIM = 14;
  int BUTTON_RECORD = 15; // SPRD Feature Porting: Add for call recorder feature
  int BUTTON_SEND_MESSAGE = 16; // SPRD Feature Porting:: Add for send message feature.
  int BUTTON_HANGUP_ALL = 17; // SPRD Feature Porting: Add for hangup all feature
  int BUTTON_ECT = 18; // SPRD Feature Porting: Add for Explicit Call Transfer.
  int BUTTON_INVITE = 19; // SPRD Feature Porting: Add for call invite feature
  int BUTTON_CHANGE_VIDEO_TYPE = 20; // SPRD Feature Porting: Add for change video type feature.
  int BUTTON_COUNT = 21;
}
