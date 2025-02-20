/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.voicemail;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.android.dialer.common.LogUtil;

/** Receives android.provider.Telephony.SECRET_CODE */
public class VoicemailSecretCodeReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    /* UNISOC: modify for bug918932 @{ */
    if(intent.getData() == null) {
      return;
    }
    /* @} */
    String host = intent.getData().getHost();
    if (!VoicemailClient.VOICEMAIL_SECRET_CODE.equals(host)) {
      return;
    }
    LogUtil.i("VoicemailSecretCodeReceiver.onReceive", "secret code received");
    VoicemailComponent.get(context).getVoicemailClient().showConfigUi(context);
  }
}
