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

package com.android.incallui.answer.impl;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialogFragment;
import android.telephony.SmsMessage;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.EditText;

import com.android.dialer.common.FragmentUtils;
import com.android.incallui.incalluilock.InCallUiLock;

/**
 * Shows the dialog for users to enter a custom message when rejecting a call with an SMS message.
 */
public class CreateCustomSmsDialogFragment extends AppCompatDialogFragment {

  private static final String ARG_ENTERED_TEXT = "enteredText";

  private EditText editText;
  private InCallUiLock inCallUiLock;

  public static CreateCustomSmsDialogFragment newInstance() {
    return new CreateCustomSmsDialogFragment();
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    super.onCreateDialog(savedInstanceState);
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    View view = View.inflate(builder.getContext(), R.layout.fragment_custom_sms_dialog, null);
    editText = (EditText) view.findViewById(R.id.custom_sms_input);
    // UNISOC: fix aob for bug895922 910932
    editText.setFilters(new InputFilter[] { new SmsLengthFilter() });
    if (savedInstanceState != null) {
      editText.setText(savedInstanceState.getCharSequence(ARG_ENTERED_TEXT));
    }

    inCallUiLock =
        FragmentUtils.getParentUnsafe(
                CreateCustomSmsDialogFragment.this, CreateCustomSmsHolder.class)
            .acquireInCallUiLock("CreateCustomSmsDialogFragment");
    builder
        .setCancelable(true)
        .setView(view)
        .setPositiveButton(
            R.string.call_incoming_custom_message_send,
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                FragmentUtils.getParentUnsafe(
                        CreateCustomSmsDialogFragment.this, CreateCustomSmsHolder.class)
                    .customSmsCreated(editText.getText().toString().trim());
                dismiss();
              }
            })
        .setNegativeButton(
            R.string.call_incoming_custom_message_cancel,
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                dismiss();
              }
            })
        .setOnCancelListener(
            new OnCancelListener() {
              @Override
              public void onCancel(DialogInterface dialogInterface) {
                dismiss();
              }
            })
        .setTitle(R.string.call_incoming_respond_via_sms_custom_message);
    final AlertDialog customMessagePopup = builder.create();
    customMessagePopup.setOnShowListener(
        new OnShowListener() {
          @Override
          public void onShow(DialogInterface dialogInterface) {
            ((AlertDialog) dialogInterface)
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setEnabled(false);
          }
        });
    editText.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
              String s = editText.getText().toString();
          }

          @Override
          public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

          @Override
          public void afterTextChanged(Editable editable) {
            Button sendButton = customMessagePopup.getButton(DialogInterface.BUTTON_POSITIVE);
            sendButton.setEnabled(editable != null && editable.toString().trim().length() != 0);
          }
        });
    //UNISOC: add for bug895921
    customMessagePopup.getWindow().clearFlags(LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    customMessagePopup.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    customMessagePopup.getWindow().addFlags(LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    return customMessagePopup;
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putCharSequence(ARG_ENTERED_TEXT, editText.getText());
  }

  @Override
  public void onDismiss(DialogInterface dialogInterface) {
    super.onDismiss(dialogInterface);
    inCallUiLock.release();
    FragmentUtils.getParentUnsafe(this, CreateCustomSmsHolder.class).customSmsDismissed();
  }

  /** Call back for {@link CreateCustomSmsDialogFragment} */
  public interface CreateCustomSmsHolder {

    InCallUiLock acquireInCallUiLock(String tag);

    void customSmsCreated(@NonNull CharSequence text);

    void customSmsDismissed();
  }

  // UNISOC: add for bug910932
  private static class SmsLengthFilter implements InputFilter{

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                                   int dstart, int dend) {
            String sourceSubStr = source.subSequence(start, end).toString();
            String orignalStart = dest.subSequence(0, dstart).toString() + dest.subSequence(dend, dest.length()).toString();

            String s = orignalStart + sourceSubStr;
            int[] params = SmsMessage.calculateLength(s, false);
            int endIndex = start + 1;
            if (params[0] == 1) {
                return null;
            } else if (!containsEmoji(source.subSequence(start, end).toString())){
                for (; endIndex <= end; ++endIndex) {
                    int[] ps = SmsMessage.calculateLength(orignalStart + source.subSequence(start, endIndex).toString(), false);
                    if (ps[0] > 1) {
                        endIndex--;
                        break;
                    }
                }
                if (endIndex == start) {
                    return "";
                } else {
                    return source.subSequence(start, endIndex);
                }
            } else {
                return "";
            }
        }

        private boolean containsEmoji(String source) {
            int len = source.length();
            for (int i = 0; i < len; i++) {
                char codePoint = source.charAt(i);
                if (!isEmojiCharacter(codePoint)) {
                    return true;
                }
            }
            return false;
        }

        private  boolean isEmojiCharacter(char codePoint) {
            return (codePoint == 0x0) || (codePoint == 0x9) || (codePoint == 0xA) ||
                    (codePoint == 0xD) || ((codePoint >= 0x20) && (codePoint <= 0xD7FF)) ||
                    ((codePoint >= 0xE000) && (codePoint <= 0xFFFD)) || ((codePoint >= 0x10000)
                    && (codePoint <= 0x10FFFF));
        }
    }
}
