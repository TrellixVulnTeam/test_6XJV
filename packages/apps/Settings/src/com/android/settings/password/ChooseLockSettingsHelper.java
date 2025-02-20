/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.password;

import android.annotation.Nullable;
import android.app.Activity;
import android.app.Fragment;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.UserManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.setupwizardlib.util.WizardManagerHelper;

public final class ChooseLockSettingsHelper {

    public static final String EXTRA_KEY_TYPE = "type";
    public static final String EXTRA_KEY_PASSWORD = "password";
    public static final String EXTRA_KEY_RETURN_CREDENTIALS = "return_credentials";
    public static final String EXTRA_KEY_HAS_CHALLENGE = "has_challenge";
    public static final String EXTRA_KEY_CHALLENGE = "challenge";
    public static final String EXTRA_KEY_CHALLENGE_TOKEN = "hw_auth_token";
    public static final String EXTRA_KEY_FOR_FINGERPRINT = "for_fingerprint";
    public static final String EXTRA_KEY_FOR_FACE = "for_face";
    public static final String EXTRA_KEY_FOR_CHANGE_CRED_REQUIRED_FOR_BOOT = "for_cred_req_boot";

    /**
     * When invoked via {@link ConfirmLockPassword.InternalActivity}, this flag
     * controls if we relax the enforcement of
     * {@link Utils#enforceSameOwner(android.content.Context, int)}.
     */
    public static final String EXTRA_ALLOW_ANY_USER = "allow_any_user";

    @VisibleForTesting LockPatternUtils mLockPatternUtils;
    private Activity mActivity;
    private Fragment mFragment;

    public ChooseLockSettingsHelper(Activity activity) {
        mActivity = activity;
        mLockPatternUtils = new LockPatternUtils(activity);
    }

    public ChooseLockSettingsHelper(Activity activity, Fragment fragment) {
        this(activity);
        mFragment = fragment;
    }

    public LockPatternUtils utils() {
        return mLockPatternUtils;
    }

    /**
     * If a pattern, password or PIN exists, prompt the user before allowing them to change it.
     *
     * @param title title of the confirmation screen; shown in the action bar
     * @return true if one exists and we launched an activity to confirm it
     * @see Activity#onActivityResult(int, int, android.content.Intent)
     */
    public boolean launchConfirmationActivity(int request, CharSequence title) {
        return launchConfirmationActivity(request, title, null, null, false, false);
    }

    /**
     * If a pattern, password or PIN exists, prompt the user before allowing them to change it.
     *
     * @param title title of the confirmation screen; shown in the action bar
     * @param returnCredentials if true, put credentials into intent. Note that if this is true,
     *                          this can only be called internally.
     * @return true if one exists and we launched an activity to confirm it
     * @see Activity#onActivityResult(int, int, android.content.Intent)
     */
    public boolean launchConfirmationActivity(int request, CharSequence title, boolean returnCredentials) {
        return launchConfirmationActivity(request, title, null, null, returnCredentials, false);
    }

    /**
     * If a pattern, password or PIN exists, prompt the user before allowing them to change it.
     *
     * @param title title of the confirmation screen; shown in the action bar
     * @param returnCredentials if true, put credentials into intent. Note that if this is true,
     *                          this can only be called internally.
     * @param userId The userId for whom the lock should be confirmed.
     * @return true if one exists and we launched an activity to confirm it
     * @see Activity#onActivityResult(int, int, android.content.Intent)
     */
    public boolean launchConfirmationActivity(int request, CharSequence title,
            boolean returnCredentials, int userId) {
        return launchConfirmationActivity(request, title, null, null,
                returnCredentials, false, false, 0, Utils.enforceSameOwner(mActivity, userId));
    }

    /**
     * If a pattern, password or PIN exists, prompt the user before allowing them to change it.
     *
     * @param title title of the confirmation screen; shown in the action bar
     * @param header header of the confirmation screen; shown as large text
     * @param description description of the confirmation screen
     * @param returnCredentials if true, put credentials into intent. Note that if this is true,
     *                          this can only be called internally.
     * @param external specifies whether this activity is launched externally, meaning that it will
     *                 get a dark theme, allow fingerprint authentication and it will forward
     *                 activity result.
     * @return true if one exists and we launched an activity to confirm it
     * @see Activity#onActivityResult(int, int, android.content.Intent)
     */
    boolean launchConfirmationActivity(int request, @Nullable CharSequence title,
            @Nullable CharSequence header, @Nullable CharSequence description,
            boolean returnCredentials, boolean external) {
        return launchConfirmationActivity(request, title, header, description,
                returnCredentials, external, false, 0, Utils.getCredentialOwnerUserId(mActivity));
    }

    /**
     * If a pattern, password or PIN exists, prompt the user before allowing them to change it.
     *
     * @param title title of the confirmation screen; shown in the action bar
     * @param header header of the confirmation screen; shown as large text
     * @param description description of the confirmation screen
     * @param returnCredentials if true, put credentials into intent. Note that if this is true,
     *                          this can only be called internally.
     * @param external specifies whether this activity is launched externally, meaning that it will
     *                 get a dark theme, allow fingerprint authentication and it will forward
     *                 activity result.
     * @param userId The userId for whom the lock should be confirmed.
     * @return true if one exists and we launched an activity to confirm it
     * @see Activity#onActivityResult(int, int, android.content.Intent)
     */
    boolean launchConfirmationActivity(int request, @Nullable CharSequence title,
            @Nullable CharSequence header, @Nullable CharSequence description,
            boolean returnCredentials, boolean external, int userId) {
        return launchConfirmationActivity(request, title, header, description,
                returnCredentials, external, false, 0, Utils.enforceSameOwner(mActivity, userId));
    }

    /**
     * If a pattern, password or PIN exists, prompt the user before allowing them to change it.
     *
     * @param title title of the confirmation screen; shown in the action bar
     * @param header header of the confirmation screen; shown as large text
     * @param description description of the confirmation screen
     * @param challenge a challenge to be verified against the device credential.
     * @return true if one exists and we launched an activity to confirm it
     * @see Activity#onActivityResult(int, int, android.content.Intent)
     */
    public boolean launchConfirmationActivity(int request, @Nullable CharSequence title,
            @Nullable CharSequence header, @Nullable CharSequence description,
            long challenge) {
        return launchConfirmationActivity(request, title, header, description,
                true, false, true, challenge, Utils.getCredentialOwnerUserId(mActivity));
    }

    /**
     * If a pattern, password or PIN exists, prompt the user before allowing them to change it.
     *
     * @param title title of the confirmation screen; shown in the action bar
     * @param header header of the confirmation screen; shown as large text
     * @param description description of the confirmation screen
     * @param challenge a challenge to be verified against the device credential.
     * @param userId The userId for whom the lock should be confirmed.
     * @return true if one exists and we launched an activity to confirm it
     * @see Activity#onActivityResult(int, int, android.content.Intent)
     */
    public boolean launchConfirmationActivity(int request, @Nullable CharSequence title,
            @Nullable CharSequence header, @Nullable CharSequence description,
            long challenge, int userId) {
        return launchConfirmationActivity(request, title, header, description,
                true, false, true, challenge, Utils.enforceSameOwner(mActivity, userId));
    }

    /**
     * If a pattern, password or PIN exists, prompt the user before allowing them to change it.
     *
     * @param title title of the confirmation screen; shown in the action bar
     * @param header header of the confirmation screen; shown as large text
     * @param description description of the confirmation screen
     * @param external specifies whether this activity is launched externally, meaning that it will
     *                 get a dark theme, allow fingerprint authentication and it will forward
     *                 activity result.
     * @param challenge a challenge to be verified against the device credential.
     * @param userId The userId for whom the lock should be confirmed.
     * @return true if one exists and we launched an activity to confirm it
     * @see Activity#onActivityResult(int, int, android.content.Intent)
     */
    public boolean launchConfirmationActivityWithExternalAndChallenge(int request,
            @Nullable CharSequence title, @Nullable CharSequence header,
            @Nullable CharSequence description, boolean external, long challenge, int userId) {
        return launchConfirmationActivity(request, title, header, description, false,
                external, true, challenge, Utils.enforceSameOwner(mActivity, userId));
    }

    /**
     * Variant that allows you to prompt for credentials of any user, including
     * those which aren't associated with the current user. As an example, this
     * is useful when unlocking the storage for secondary users.
     */
    public boolean launchConfirmationActivityForAnyUser(int request,
            @Nullable CharSequence title, @Nullable CharSequence header,
            @Nullable CharSequence description, int userId) {
        final Bundle extras = new Bundle();
        extras.putBoolean(EXTRA_ALLOW_ANY_USER, true);
        return launchConfirmationActivity(request, title, header, description, false,
                false, true, 0, userId, extras);
    }

    private boolean launchConfirmationActivity(int request, @Nullable CharSequence title,
            @Nullable CharSequence header, @Nullable CharSequence description,
            boolean returnCredentials, boolean external, boolean hasChallenge,
            long challenge, int userId) {
        return launchConfirmationActivity(request, title, header, description, returnCredentials,
                external, hasChallenge, challenge, userId, null /* alternateButton */, null);
    }

    private boolean launchConfirmationActivity(int request, @Nullable CharSequence title,
            @Nullable CharSequence header, @Nullable CharSequence description,
            boolean returnCredentials, boolean external, boolean hasChallenge,
            long challenge, int userId, Bundle extras) {
        return launchConfirmationActivity(request, title, header, description, returnCredentials,
                external, hasChallenge, challenge, userId, null /* alternateButton */, extras);
    }

    public boolean launchFrpConfirmationActivity(int request, @Nullable CharSequence header,
            @Nullable CharSequence description, @Nullable CharSequence alternateButton) {
        return launchConfirmationActivity(request, null /* title */, header, description,
                false /* returnCredentials */, true /* external */, false /* hasChallenge */,
                0 /* challenge */, LockPatternUtils.USER_FRP, alternateButton, null);
    }

    private boolean launchConfirmationActivity(int request, @Nullable CharSequence title,
            @Nullable CharSequence header, @Nullable CharSequence description,
            boolean returnCredentials, boolean external, boolean hasChallenge,
            long challenge, int userId, @Nullable CharSequence alternateButton, Bundle extras) {
        final int effectiveUserId = UserManager.get(mActivity).getCredentialOwnerProfile(userId);
        boolean launched = false;

        switch (mLockPatternUtils.getKeyguardStoredPasswordQuality(effectiveUserId)) {
            case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                launched = launchConfirmationActivity(request, title, header, description,
                        returnCredentials || hasChallenge
                                ? ConfirmLockPattern.InternalActivity.class
                                : ConfirmLockPattern.class, returnCredentials, external,
                                hasChallenge, challenge, userId, alternateButton, extras);
                break;
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
            case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                launched = launchConfirmationActivity(request, title, header, description,
                        returnCredentials || hasChallenge
                                ? ConfirmLockPassword.InternalActivity.class
                                : ConfirmLockPassword.class, returnCredentials, external,
                                hasChallenge, challenge, userId, alternateButton, extras);
                break;
        }
        return launched;
    }

    private boolean launchConfirmationActivity(int request, CharSequence title, CharSequence header,
            CharSequence message, Class<?> activityClass, boolean returnCredentials,
            boolean external, boolean hasChallenge, long challenge,
            int userId, @Nullable CharSequence alternateButton, Bundle extras) {
        final Intent intent = new Intent();
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.TITLE_TEXT, title);
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.HEADER_TEXT, header);
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.DETAILS_TEXT, message);
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.ALLOW_FP_AUTHENTICATION, external);
        // TODO: Remove dark theme and show_cancel_button options since they are no longer used
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.DARK_THEME, false);
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.SHOW_CANCEL_BUTTON, false);
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.SHOW_WHEN_LOCKED, external);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_RETURN_CREDENTIALS, returnCredentials);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, hasChallenge);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, challenge);
        // we should never have a drawer when confirming device credentials.
        intent.putExtra(SettingsActivity.EXTRA_HIDE_DRAWER, true);
        intent.putExtra(Intent.EXTRA_USER_ID, userId);
        intent.putExtra(KeyguardManager.EXTRA_ALTERNATE_BUTTON_LABEL, alternateButton);
        if (extras != null) {
            intent.putExtras(extras);
        }
        intent.setClassName(ConfirmDeviceCredentialBaseFragment.PACKAGE, activityClass.getName());
        if (external) {
            intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            if (mFragment != null) {
                copyOptionalExtras(mFragment.getActivity().getIntent(), intent);
                mFragment.startActivity(intent);
            } else {
                copyOptionalExtras(mActivity.getIntent(), intent);
                mActivity.startActivity(intent);
            }
        } else {
            if (mFragment != null) {
                copyInternalExtras(mFragment.getActivity().getIntent(), intent);
                mFragment.startActivityForResult(intent, request);
            } else {
                copyInternalExtras(mActivity.getIntent(), intent);
                mActivity.startActivityForResult(intent, request);
            }
        }
        return true;
    }

    private void copyOptionalExtras(Intent inIntent, Intent outIntent) {
        IntentSender intentSender = inIntent.getParcelableExtra(Intent.EXTRA_INTENT);
        if (intentSender != null) {
            outIntent.putExtra(Intent.EXTRA_INTENT, intentSender);
        }
        int taskId = inIntent.getIntExtra(Intent.EXTRA_TASK_ID, -1);
        if (taskId != -1) {
            outIntent.putExtra(Intent.EXTRA_TASK_ID, taskId);
        }
        // If we will launch another activity once credentials are confirmed, exclude from recents.
        // This is a workaround to a framework bug where affinity is incorrect for activities
        // that are started from a no display activity, as is ConfirmDeviceCredentialActivity.
        // TODO: Remove once that bug is fixed.
        if (intentSender != null || taskId != -1) {
            outIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            outIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        }
    }

    private void copyInternalExtras(Intent inIntent, Intent outIntent) {
        String theme = inIntent.getStringExtra(WizardManagerHelper.EXTRA_THEME);
        if (theme != null) {
            outIntent.putExtra(WizardManagerHelper.EXTRA_THEME, theme);
        }
    }
}
