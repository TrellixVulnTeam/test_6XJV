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

package com.android.incallui;

import android.app.LowmemoryUtils;
import android.app.ActivityManager;
import android.app.ActivityManager.AppTask;
import android.app.ActivityManager.TaskDescription;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.Bundle;
import android.os.Environment;
import android.os.Trace;
import android.os.PowerManager;//UNISOC :Add for bug914943
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.ColorUtils;
import android.telecom.CallAudioState;
import android.telecom.PhoneAccountHandle;
import android.telecom.VideoProfile;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.Toast;
import com.android.contacts.common.widget.SelectPhoneAccountDialogFragment;
import com.android.dialer.animation.AnimUtils;
import com.android.dialer.animation.AnimationListenerAdapter;
import com.android.dialer.common.Assert;
import com.android.dialer.common.LogUtil;
import com.android.dialer.common.concurrent.ThreadUtil;
import com.android.dialer.compat.ActivityCompat;
import com.android.dialer.compat.CompatUtils;
import com.android.dialer.configprovider.ConfigProviderBindings;
import com.android.dialer.logging.Logger;
import com.android.dialer.logging.ScreenEvent;
import com.android.dialer.metrics.Metrics;
import com.android.dialer.metrics.MetricsComponent;
import com.android.dialer.util.PermissionsUtil;
import com.android.dialer.util.ViewUtil;
import com.android.ims.internal.ImsManagerEx;
import com.android.incallui.answer.bindings.AnswerBindings;
import com.android.incallui.answer.impl.AnswerFragment;
import com.android.incallui.answer.protocol.AnswerScreen;
import com.android.incallui.answer.protocol.AnswerScreenDelegate;
import com.android.incallui.answer.protocol.AnswerScreenDelegateFactory;
import com.android.incallui.answerproximitysensor.PseudoScreenState;
import com.android.incallui.audiomode.AudioModeProvider;
import com.android.incallui.call.CallList;
import com.android.incallui.call.DialerCall;
import com.android.incallui.call.DialerCall.State;
import com.android.incallui.call.TelecomAdapter;
import com.android.incallui.callpending.CallPendingActivity;
import com.android.incallui.disconnectdialog.DisconnectMessage;
import com.android.incallui.incall.bindings.InCallBindings;
import com.android.incallui.incall.protocol.InCallButtonUiDelegate;
import com.android.incallui.incall.protocol.InCallButtonUiDelegateFactory;
import com.android.incallui.incall.protocol.InCallScreen;
import com.android.incallui.incall.protocol.InCallScreenDelegate;
import com.android.incallui.incall.protocol.InCallScreenDelegateFactory;
import com.android.incallui.incalluilock.InCallUiLock;
import com.android.incallui.rtt.bindings.RttBindings;
import com.android.incallui.rtt.protocol.RttCallScreen;
import com.android.incallui.rtt.protocol.RttCallScreenDelegate;
import com.android.incallui.rtt.protocol.RttCallScreenDelegateFactory;
import com.android.incallui.sprd.PhoneRecorderHelper;
import com.android.incallui.telecomeventui.InternationalCallOnWifiDialogFragment;
import com.android.incallui.video.bindings.VideoBindings;
import com.android.incallui.video.protocol.VideoCallScreen;
import com.android.incallui.video.protocol.VideoCallScreenDelegate;
import com.android.incallui.video.protocol.VideoCallScreenDelegateFactory;
import com.google.common.base.Optional;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;  //write external-storage
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;   //eard external-storage
import static android.Manifest.permission.RECORD_AUDIO;           
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.UPDATE_DEVICE_STATS;

/** Version of {@link InCallActivity} that shows the new UI */
public class InCallActivity extends TransactionSafeFragmentActivity
    implements AnswerScreenDelegateFactory,
        InCallScreenDelegateFactory,
        InCallButtonUiDelegateFactory,
        VideoCallScreenDelegateFactory,
        RttCallScreenDelegateFactory,
        PseudoScreenState.StateChangedListener {

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({
    DIALPAD_REQUEST_NONE,
    DIALPAD_REQUEST_SHOW,
    DIALPAD_REQUEST_HIDE,
  })
  @interface DialpadRequestType {}

  private static final int DIALPAD_REQUEST_NONE = 1;
  private static final int DIALPAD_REQUEST_SHOW = 2;
  private static final int DIALPAD_REQUEST_HIDE = 3;

  //SPRD: add rejectmessage action in the notification.
  private static final String SHOW_REJECT_MESSAGE_DIALOG = "InCallActivity.reject_message_dialog";

  private static Optional<Integer> audioRouteForTesting = Optional.absent();

  private final InternationalCallOnWifiCallback internationalCallOnWifiCallback =
      new InternationalCallOnWifiCallback();
  private final SelectPhoneAccountListener selectPhoneAccountListener =
      new SelectPhoneAccountListener();

  private Animation dialpadSlideInAnimation;
  private Animation dialpadSlideOutAnimation;
  private Dialog errorDialog;
  private GradientDrawable backgroundDrawable;
  private InCallOrientationEventListener inCallOrientationEventListener;
  private View pseudoBlackScreenOverlay;
  private SelectPhoneAccountDialogFragment selectPhoneAccountDialogFragment;
  private String dtmfTextToPrepopulate;
  private String showPostCharWaitDialogCallId;
  private String showPostCharWaitDialogChars;
  private boolean allowOrientationChange;
  private boolean animateDialpadOnShow;
  private boolean didShowAnswerScreen;
  private boolean didShowInCallScreen;
  private boolean didShowVideoCallScreen;
  private boolean didShowRttCallScreen;
  private boolean dismissKeyguard;
  private boolean isInShowMainInCallFragment;
  private boolean isRecreating; // whether the activity is going to be recreated
  private boolean isVisible;
  private boolean needDismissPendingDialogs;
  private boolean showPostCharWaitDialogOnResume;
  private boolean touchDownWhenPseudoScreenOff;
  //SPRD: Bug787856 add rejectmessage action in the notification.
  private boolean showRejectMessageDialog;
  private int[] backgroundDrawableColors;
  @DialpadRequestType private int showDialpadRequest = DIALPAD_REQUEST_NONE;

    /* SPRD Feature Porting: Add for call recorder feature. @{ */
  private PhoneRecorderHelper mRecorderHelper;
  private static final int MICROPHONE_AND_STORAGE_PERMISSION_REQUEST_CODE = 1;
  private PhoneRecorderHelper.State mRecorderState;
  private String[] mPermissions = {WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE,
          RECORD_AUDIO, READ_PHONE_STATE};
  /* @} */
  private PowerManager mPowerManager;//UNISOC:add for bug914943
  private PowerManager.WakeLock mCpuWakeLock;//UNISOC:add for bug914943
  private AlertDialog mAlert = null;//UNISOC:add for bug895363

  public static Intent getIntent(
      Context context, boolean showDialpad, boolean newOutgoingCall, boolean isForFullScreen) {
    Intent intent = new Intent(Intent.ACTION_MAIN, null);
    intent.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.setClass(context, InCallActivity.class);
    if (showDialpad) {
      intent.putExtra(IntentExtraNames.SHOW_DIALPAD, true);
    }
    intent.putExtra(IntentExtraNames.NEW_OUTGOING_CALL, newOutgoingCall);
    intent.putExtra(IntentExtraNames.FOR_FULL_SCREEN, isForFullScreen);
    return intent;
  }

  /* SPRD: add rejectmessage action in the notification. @{ */
  public static Intent getShowRejectSMStIntent(
          Context context, boolean showRejectMessage) {
    Intent intent = new Intent(Intent.ACTION_MAIN, null);
    intent.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_NEW_TASK);

    intent.setClass(context, InCallActivity.class);
    intent.putExtra(SHOW_REJECT_MESSAGE_DIALOG, showRejectMessage);
    return intent;
  }
  /* @} */


  @Override
  protected void onResumeFragments() {
    super.onResumeFragments();
    if (needDismissPendingDialogs) {
      dismissPendingDialogs();
    }
  }

  @Override
  protected void onCreate(Bundle bundle) {
    Trace.beginSection("InCallActivity.onCreate");
    super.onCreate(bundle);

    if (bundle != null) {
      didShowAnswerScreen = bundle.getBoolean(KeysForSavedInstance.DID_SHOW_ANSWER_SCREEN);
      didShowInCallScreen = bundle.getBoolean(KeysForSavedInstance.DID_SHOW_IN_CALL_SCREEN);
      didShowVideoCallScreen = bundle.getBoolean(KeysForSavedInstance.DID_SHOW_VIDEO_CALL_SCREEN);
      didShowRttCallScreen = bundle.getBoolean(KeysForSavedInstance.DID_SHOW_RTT_CALL_SCREEN);
    }

    setWindowFlags();
    setContentView(R.layout.incall_screen);
    internalResolveIntent(getIntent());

    boolean isLandscape =
        getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    boolean isRtl = ViewUtil.isRtl();
    if (isLandscape) {
      dialpadSlideInAnimation =
          AnimationUtils.loadAnimation(
              this, isRtl ? R.anim.dialpad_slide_in_left : R.anim.dialpad_slide_in_right);
      dialpadSlideOutAnimation =
          AnimationUtils.loadAnimation(
              this, isRtl ? R.anim.dialpad_slide_out_left : R.anim.dialpad_slide_out_right);
    } else {
      dialpadSlideInAnimation = AnimationUtils.loadAnimation(this, R.anim.dialpad_slide_in_bottom);
      dialpadSlideOutAnimation =
          AnimationUtils.loadAnimation(this, R.anim.dialpad_slide_out_bottom);
    }
    dialpadSlideInAnimation.setInterpolator(AnimUtils.EASE_IN);
    dialpadSlideOutAnimation.setInterpolator(AnimUtils.EASE_OUT);
    dialpadSlideOutAnimation.setAnimationListener(
        new AnimationListenerAdapter() {
          @Override
          public void onAnimationEnd(Animation animation) {
            hideDialpadFragment();
          }
        });

    if (bundle != null && showDialpadRequest == DIALPAD_REQUEST_NONE) {
      // If the dialpad was shown before, set related variables so that it can be shown and
      // populated with the previous DTMF text during onResume().
      if (bundle.containsKey(IntentExtraNames.SHOW_DIALPAD)) {
        boolean showDialpad = bundle.getBoolean(IntentExtraNames.SHOW_DIALPAD);
        showDialpadRequest = showDialpad ? DIALPAD_REQUEST_SHOW : DIALPAD_REQUEST_HIDE;
        animateDialpadOnShow = false;
      }
      dtmfTextToPrepopulate = bundle.getString(KeysForSavedInstance.DIALPAD_TEXT);

      SelectPhoneAccountDialogFragment selectPhoneAccountDialogFragment =
          (SelectPhoneAccountDialogFragment)
              getFragmentManager().findFragmentByTag(Tags.SELECT_ACCOUNT_FRAGMENT);
      if (selectPhoneAccountDialogFragment != null) {
        selectPhoneAccountDialogFragment.setListener(selectPhoneAccountListener);
      }
    }

    InternationalCallOnWifiDialogFragment existingInternationalCallOnWifiDialogFragment =
        (InternationalCallOnWifiDialogFragment)
            getSupportFragmentManager().findFragmentByTag(Tags.INTERNATIONAL_CALL_ON_WIFI);
    if (existingInternationalCallOnWifiDialogFragment != null) {
      existingInternationalCallOnWifiDialogFragment.setCallback(internationalCallOnWifiCallback);
    }

    inCallOrientationEventListener = new InCallOrientationEventListener(this);

    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

    pseudoBlackScreenOverlay = findViewById(R.id.psuedo_black_screen_overlay);
    sendBroadcast(CallPendingActivity.getFinishBroadcast());
    Trace.endSection();
    MetricsComponent.get(this)
        .metrics()
        .stopTimer(Metrics.ON_CALL_ADDED_TO_ON_INCALL_UI_SHOWN_INCOMING);
    MetricsComponent.get(this)
        .metrics()
        .stopTimer(Metrics.ON_CALL_ADDED_TO_ON_INCALL_UI_SHOWN_OUTGOING);

     /* SPRD Feature Porting: Add for call recorder feature. @{ */
    mRecorderHelper = PhoneRecorderHelper.getInstance(getApplicationContext());
    mRecorderHelper.setOnStateChangedListener(mRecorderStateChangedListener);
    mRecorderHelper.registerExternalStorageStateListener(getApplicationContext());
    /* @} */
    // UNISOC: add for bug 914943. Recreate InCallActivity Only when screen is not on.
    mPowerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);

  }

  private void setWindowFlags() {
    // Allow the activity to be shown when the screen is locked and filter out touch events that are
    // "too fat".
    int flags =
        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            | WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES;

    // When the audio stream is not via Bluetooth, turn on the screen once the activity is shown.
    // When the audio stream is via Bluetooth, turn on the screen only for an incoming call.
    final int audioRoute = getAudioRoute();
    if (audioRoute != CallAudioState.ROUTE_BLUETOOTH
        || CallList.getInstance().getIncomingCall() != null) {
      flags |= WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
    }

    getWindow().addFlags(flags);
  }

  private static int getAudioRoute() {
    if (audioRouteForTesting.isPresent()) {
      return audioRouteForTesting.get();
    }

    return AudioModeProvider.getInstance().getAudioState().getRoute();
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  public static void setAudioRouteForTesting(int audioRoute) {
    audioRouteForTesting = Optional.of(audioRoute);
  }

  private void internalResolveIntent(Intent intent) {
    if (!intent.getAction().equals(Intent.ACTION_MAIN)) {
      return;
    }

    if (intent.hasExtra(IntentExtraNames.SHOW_DIALPAD)) {
      // IntentExtraNames.SHOW_DIALPAD can be used to specify whether the DTMF dialpad should be
      // initially visible.  If the extra is absent, leave the dialpad in its previous state.
      boolean showDialpad = intent.getBooleanExtra(IntentExtraNames.SHOW_DIALPAD, false);
      relaunchedFromDialer(showDialpad);
    }

    /* SPRD: add rejectmessage action in the notification. @{ */
    if (intent.hasExtra(SHOW_REJECT_MESSAGE_DIALOG)) {
      showRejectMessageDialog = intent.getBooleanExtra(SHOW_REJECT_MESSAGE_DIALOG, false);
    }
    /* @} */

    DialerCall outgoingCall = CallList.getInstance().getOutgoingCall();
    if (outgoingCall == null) {
      outgoingCall = CallList.getInstance().getPendingOutgoingCall();
    }
    if (intent.getBooleanExtra(IntentExtraNames.NEW_OUTGOING_CALL, false)) {
      intent.removeExtra(IntentExtraNames.NEW_OUTGOING_CALL);

      // InCallActivity is responsible for disconnecting a new outgoing call if there is no way of
      // making it (i.e. no valid call capable accounts).
      // If the version is not MSIM compatible, ignore this code.
      if (CompatUtils.isMSIMCompatible()
          && InCallPresenter.isCallWithNoValidAccounts(outgoingCall)) {
        LogUtil.i(
            "InCallActivity.internalResolveIntent", "Call with no valid accounts, disconnecting");
        outgoingCall.disconnect();
      }

      dismissKeyguard(true);
    }

    if (showPhoneAccountSelectionDialog()) {
      hideMainInCallFragment();
    }
  }

  /**
   * When relaunching from the dialer app, {@code showDialpad} indicates whether the dialpad should
   * be shown on launch.
   *
   * @param showDialpad {@code true} to indicate the dialpad should be shown on launch, and {@code
   *     false} to indicate no change should be made to the dialpad visibility.
   */
  private void relaunchedFromDialer(boolean showDialpad) {
    showDialpadRequest = showDialpad ? DIALPAD_REQUEST_SHOW : DIALPAD_REQUEST_NONE;
    animateDialpadOnShow = true;

    if (showDialpadRequest == DIALPAD_REQUEST_SHOW) {
      // If there's only one line in use, AND it's on hold, then we're sure the user
      // wants to use the dialpad toward the exact line, so un-hold the holding line.
      DialerCall call = CallList.getInstance().getActiveOrBackgroundCall();
      if (call != null && call.getState() == State.ONHOLD) {
        call.unhold();
      }
    }
  }

  /**
   * Show a phone account selection dialog if there is a call waiting for phone account selection.
   *
   * @return true if the dialog was shown.
   */
  private boolean showPhoneAccountSelectionDialog() {
    DialerCall waitingForAccountCall = CallList.getInstance().getWaitingForAccountCall();
    if (waitingForAccountCall == null) {
      return false;
    }

    Bundle extras = waitingForAccountCall.getIntentExtras();
    List<PhoneAccountHandle> phoneAccountHandles =
        extras == null
            ? new ArrayList<>()
            : extras.getParcelableArrayList(android.telecom.Call.AVAILABLE_PHONE_ACCOUNTS);

    selectPhoneAccountDialogFragment =
        SelectPhoneAccountDialogFragment.newInstance(
            // UNISOC: modify for bug907127
            R.string.pre_call_select_phone_account,
            true /* canSetDefault */,
            0 /* setDefaultResId */,
            phoneAccountHandles,
            selectPhoneAccountListener,
            waitingForAccountCall.getId(),
            null /* hints */);
    selectPhoneAccountDialogFragment.show(getFragmentManager(), Tags.SELECT_ACCOUNT_FRAGMENT);
    return true;
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    LogUtil.enterBlock("InCallActivity.onSaveInstanceState");

    // TODO: DialpadFragment should handle this as part of its own state
    out.putBoolean(IntentExtraNames.SHOW_DIALPAD, isDialpadVisible());
    DialpadFragment dialpadFragment = getDialpadFragment();
    if (dialpadFragment != null) {
      out.putString(KeysForSavedInstance.DIALPAD_TEXT, dialpadFragment.getDtmfText());
    }

    out.putBoolean(KeysForSavedInstance.DID_SHOW_ANSWER_SCREEN, didShowAnswerScreen);
    out.putBoolean(KeysForSavedInstance.DID_SHOW_IN_CALL_SCREEN, didShowInCallScreen);
    out.putBoolean(KeysForSavedInstance.DID_SHOW_VIDEO_CALL_SCREEN, didShowVideoCallScreen);
    out.putBoolean(KeysForSavedInstance.DID_SHOW_RTT_CALL_SCREEN, didShowRttCallScreen);

    super.onSaveInstanceState(out);
    isVisible = false;
  }

  @Override
  protected void onStart() {
    Trace.beginSection("InCallActivity.onStart");
    super.onStart();

    isVisible = true;
    showMainInCallFragment();

    InCallPresenter.getInstance().setActivity(this);
    enableInCallOrientationEventListener(
        getRequestedOrientation()
            == InCallOrientationEventListener.ACTIVITY_PREFERENCE_ALLOW_ROTATION);
    InCallPresenter.getInstance().onActivityStarted();

    if (!isRecreating) {
      InCallPresenter.getInstance().onUiShowing(true);
    }

    if (ActivityCompat.isInMultiWindowMode(this)
        && !getResources().getBoolean(R.bool.incall_dialpad_allowed)) {
      // Hide the dialpad because there may not be enough room
      showDialpadFragment(false, false);
    }

    /* add for bug904816 @{ */
    AnswerScreen answerScreen = getAnswerScreen();
    if(answerScreen != null && (answerScreen.isVideoCall() || answerScreen.isVideoUpgradeRequest())){
      answerScreen.onVideoCallIsFront();
    }
    /* @} */

    Trace.endSection();
  }

  @Override
  protected void onResume() {
    Trace.beginSection("InCallActivity.onResume");
    super.onResume();

    if (!InCallPresenter.getInstance().isReadyForTearDown()) {
      updateTaskDescription();
      InCallPresenter.getInstance().updateNotification();
    }

    // If there is a pending request to show or hide the dialpad, handle that now.
    if (showDialpadRequest != DIALPAD_REQUEST_NONE) {
      if (showDialpadRequest == DIALPAD_REQUEST_SHOW) {
        // Exit fullscreen so that the user has access to the dialpad hide/show button.
        // This is important when showing the dialpad from within dialer.
        InCallPresenter.getInstance().setFullScreen(false /* isFullScreen */, true /* force */);

        showDialpadFragment(true /* show */, animateDialpadOnShow /* animate */);
        animateDialpadOnShow = false;

        DialpadFragment dialpadFragment = getDialpadFragment();
        if (dialpadFragment != null) {
          dialpadFragment.setDtmfText(dtmfTextToPrepopulate);
          dtmfTextToPrepopulate = null;
        }
      } else {
        LogUtil.i("InCallActivity.onResume", "Force-hide the dialpad");
        if (getDialpadFragment() != null) {
          showDialpadFragment(false /* show */, false /* animate */);
        }
      }
      showDialpadRequest = DIALPAD_REQUEST_NONE;
    }
    updateNavigationBar(isDialpadVisible());


    /* SPRD: add rejectmessage action in the notification. @{ */
    if (showRejectMessageDialog) {
      if (getAnswerScreen() != null && getAnswerScreen().getAnswerScreenFragment() != null) {
        AnswerFragment answerFragment = (AnswerFragment)getAnswerScreen().getAnswerScreenFragment();
        answerFragment.showMessageMenu();
      } else {
        Log.v(this, "onResume : has not click on the statusbar reject message action");
      }
      showRejectMessageDialog = false;
    }
    /* @} */

    if (showPostCharWaitDialogOnResume) {
      showDialogForPostCharWait(showPostCharWaitDialogCallId, showPostCharWaitDialogChars);
    }

    CallList.getInstance()
        .onInCallUiShown(getIntent().getBooleanExtra(IntentExtraNames.FOR_FULL_SCREEN, false));

    PseudoScreenState pseudoScreenState = InCallPresenter.getInstance().getPseudoScreenState();
    pseudoScreenState.addListener(this);
    onPseudoScreenStateChanged(pseudoScreenState.isOn());
    Trace.endSection();
    // add 1 sec delay to get memory snapshot so that dialer wont react slowly on resume.
    ThreadUtil.postDelayedOnUiThread(
        () ->
            MetricsComponent.get(this)
                .metrics()
                .recordMemory(Metrics.INCALL_ACTIVITY_ON_RESUME_MEMORY_EVENT_NAME),
        1000);

    /* SPRD Feature Porting: Add for call recorder feature. @{ */
    mRecorderHelper.notifyCurrentState();
    /* @} */
  }

  @Override
  protected void onPause() {
    Trace.beginSection("InCallActivity.onPause");
    super.onPause();

    // UNISOC: add for bug895487
    if (errorDialog != null && !this.isFinishing()) {
      LogUtil.v("InCallActivity.onPause", "dismiss dialog");
      errorDialog.dismiss();
    }
    DialpadFragment dialpadFragment = getDialpadFragment();
    if (dialpadFragment != null) {
      dialpadFragment.onDialerKeyUp(null);
    }

    InCallPresenter.getInstance().updateNotification();

    InCallPresenter.getInstance().getPseudoScreenState().removeListener(this);
    Trace.endSection();
  }

  @Override
  protected void onStop() {
    Trace.beginSection("InCallActivity.onStop");
    isVisible = false;
    // UNISOC: modify for bug951500
    InCallPresenter.getInstance().updateIsChangingConfigurations();
    super.onStop();

    // Disconnects the call waiting for a phone account when the activity is hidden (e.g., after the
    // user presses the home button).
    // Without this the pending call will get stuck on phone account selection and new calls can't
    // be created.
    // Skip this when the screen is locked since the activity may complete its current life cycle
    // and restart.
    if (!isRecreating && !getSystemService(KeyguardManager.class).isKeyguardLocked()) {
      DialerCall waitingForAccountCall = CallList.getInstance().getWaitingForAccountCall();
      if (waitingForAccountCall != null) {
        waitingForAccountCall.disconnect();
      }
    }

    enableInCallOrientationEventListener(false);
    InCallPresenter.getInstance().onActivityStopped();
    if (!isRecreating) {
      InCallPresenter.getInstance().onUiShowing(false);
      if (errorDialog != null) {
        errorDialog.dismiss();
      }
    }

    if (isFinishing()) {
      InCallPresenter.getInstance().unsetActivity(this);
    }

    /* SPRD: bug904816 @{ */
    AnswerScreen answerScreen = getAnswerScreen();
    if(answerScreen != null && (answerScreen.isVideoCall() || answerScreen.isVideoUpgradeRequest())){
      answerScreen.onVideoCallIsBack();
    }
    /* @} */

    Trace.endSection();
  }

  @Override
  protected void onDestroy() {
    Trace.beginSection("InCallActivity.onDestroy");
    super.onDestroy();

    InCallPresenter.getInstance().unsetActivity(this);
    InCallPresenter.getInstance().updateIsChangingConfigurations();

        /* SPRD Feature Porting: Add for call record feature. @{ */
    InCallPresenter.getInstance().stopRecorderForDisconnect();
    //SPRD: add for bug711372
    if (mRecorderHelper != null) {
      mRecorderHelper.unRegisterExternalStorageStateListener(getApplicationContext());
    }
    /* @} */
    Trace.endSection();
  }

  @Override
  public void finish() {
    if (shouldCloseActivityOnFinish()) {
      // When user select incall ui from recents after the call is disconnected, it tries to launch
      // a new InCallActivity but InCallPresenter is already teared down at this point, which causes
      // crash.
      // By calling finishAndRemoveTask() instead of finish() the task associated with
      // InCallActivity is cleared completely. So system won't try to create a new InCallActivity in
      // this case.
      //
      // Calling finish won't clear the task and normally when an activity finishes it shouldn't
      // clear the task since there could be parent activity in the same task that's still alive.
      // But InCallActivity is special since it's singleInstance which means it's root activity and
      // only instance of activity in the task. So it should be safe to also remove task when
      // finishing.
      // It's also necessary in the sense of it's excluded from recents. So whenever the activity
      // finishes, the task should also be removed since it doesn't make sense to go back to it in
      // anyway anymore.
      super.finishAndRemoveTask();
    }
  }

  private boolean shouldCloseActivityOnFinish() {
    if (!isVisible) {
      LogUtil.i(
          "InCallActivity.shouldCloseActivityOnFinish",
          "allowing activity to be closed because it's not visible");
      return true;
    }

    if (InCallPresenter.getInstance().isInCallUiLocked()) {
      LogUtil.i(
          "InCallActivity.shouldCloseActivityOnFinish",
          "in call ui is locked, not closing activity");
      return false;
    }

    LogUtil.i(
        "InCallActivity.shouldCloseActivityOnFinish",
        "activity is visible and has no locks, allowing activity to close");
    return true;
  }

  @Override
  protected void onNewIntent(Intent intent) {
    LogUtil.enterBlock("InCallActivity.onNewIntent");

    // If the screen is off, we need to make sure it gets turned on for incoming calls.
    // This normally works just fine thanks to FLAG_TURN_SCREEN_ON but that only works
    // when the activity is first created. Therefore, to ensure the screen is turned on
    // for the call waiting case, we recreate() the current activity. There should be no jank from
    // this since the screen is already off and will remain so until our new activity is up.
    // UNISOC: Add for bug914943. Recreate InCallActivity Only when screen is not on.
    if (!isVisible && !mPowerManager.isScreenOn()) {
      onNewIntent(intent, true /* isRecreating */);
      LogUtil.i("InCallActivity.onNewIntent", "Restarting InCallActivity to force screen on.");
      //recreate();
    if (!mPowerManager.isInteractive()) {
        mCpuWakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "InCallActivity");
        if (mCpuWakeLock != null && !mCpuWakeLock.isHeld()) {
          LogUtil.i("InCallActivity.onNewIntent", "Acquiring full wake lock to force screen on.");
          mCpuWakeLock.acquire();
        }
      }
      //  Releasing full wake lock after
      if (mCpuWakeLock != null && mCpuWakeLock.isHeld()) {
        mCpuWakeLock.release();
        LogUtil.i("InCallActivity.onNewIntent", "Releasing full wake lock.");
      }
      /*@}*/
    } else {
      onNewIntent(intent, false /* isRecreating */);
    }
  }

  @VisibleForTesting
  void onNewIntent(Intent intent, boolean isRecreating) {
    this.isRecreating = isRecreating;

    // We're being re-launched with a new Intent.  Since it's possible for a single InCallActivity
    // instance to persist indefinitely (even if we finish() ourselves), this sequence can
    // happen any time the InCallActivity needs to be displayed.

    // Stash away the new intent so that we can get it in the future by calling getIntent().
    // Otherwise getIntent() will return the original Intent from when we first got created.
    setIntent(intent);

    // Activities are always paused before receiving a new intent, so we can count on our onResume()
    // method being called next.

    // Just like in onCreate(), handle the intent.
    // Skip if InCallActivity is going to be recreated since this will be called in onCreate().
    if (!isRecreating) {
      internalResolveIntent(intent);
    }
  }

  @Override
  public void onBackPressed() {
    LogUtil.enterBlock("InCallActivity.onBackPressed");

    if (!isVisible) {
      return;
    }

    if (!getCallCardFragmentVisible()) {
      return;
    }

    DialpadFragment dialpadFragment = getDialpadFragment();
    if (dialpadFragment != null && dialpadFragment.isVisible()) {
      showDialpadFragment(false /* show */, true /* animate */);
      return;
    }

    if (CallList.getInstance().getIncomingCall() != null) {
      LogUtil.i(
          "InCallActivity.onBackPressed",
          "Ignore the press of the back key when an incoming call is ringing");
      return;
    }

    // Nothing special to do. Fall back to the default behavior.
    super.onBackPressed();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    LogUtil.i("InCallActivity.onOptionsItemSelected", "item: " + item);
    if (item.getItemId() == android.R.id.home) {
      onBackPressed();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    DialpadFragment dialpadFragment = getDialpadFragment();
    if (dialpadFragment != null
        && dialpadFragment.isVisible()
        && dialpadFragment.onDialerKeyUp(event)) {
      return true;
    }

    if (keyCode == KeyEvent.KEYCODE_CALL) {
      // Always consume KEYCODE_CALL to ensure the PhoneWindow won't do anything with it.
      return true;
    }

    return super.onKeyUp(keyCode, event);
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    switch (keyCode) {
      case KeyEvent.KEYCODE_CALL:
        if (!InCallPresenter.getInstance().handleCallKey()) {
          LogUtil.e(
              "InCallActivity.onKeyDown",
              "InCallPresenter should always handle KEYCODE_CALL in onKeyDown");
        }
        // Always consume KEYCODE_CALL to ensure the PhoneWindow won't do anything with it.
        return true;

        // Note that KEYCODE_ENDCALL isn't handled here as the standard system-wide handling of it
        // is exactly what's needed, namely
        // (1) "hang up" if there's an active call, or
        // (2) "don't answer" if there's an incoming call.
        // (See PhoneWindowManager for implementation details.)

      case KeyEvent.KEYCODE_CAMERA:
        // Consume KEYCODE_CAMERA since it's easy to accidentally press the camera button.
        return true;

      case KeyEvent.KEYCODE_VOLUME_UP:
      case KeyEvent.KEYCODE_VOLUME_DOWN:
      case KeyEvent.KEYCODE_VOLUME_MUTE:
        // Ringer silencing handled by PhoneWindowManager.
        break;

      case KeyEvent.KEYCODE_MUTE:
        TelecomAdapter.getInstance()
            .mute(!AudioModeProvider.getInstance().getAudioState().isMuted());
        return true;

      case KeyEvent.KEYCODE_SLASH:
        // When verbose logging is enabled, dump the view for debugging/testing purposes.
        if (LogUtil.isVerboseEnabled()) {
          View decorView = getWindow().getDecorView();
          LogUtil.v("InCallActivity.onKeyDown", "View dump:\n%s", decorView);
          return true;
        }
        break;

      case KeyEvent.KEYCODE_EQUALS:
        break;

      default: // fall out
    }

    // Pass other key events to DialpadFragment's "onDialerKeyDown" method in case the user types
    // in DTMF (Dual-tone multi-frequency signaling) code.
    DialpadFragment dialpadFragment = getDialpadFragment();
    if (dialpadFragment != null
        && dialpadFragment.isVisible()
        && dialpadFragment.onDialerKeyDown(event)) {
      return true;
    }

    return super.onKeyDown(keyCode, event);
  }

  public boolean isInCallScreenAnimating() {
    return false;
  }

  public void showConferenceFragment(boolean show) {
    if (show) {
      startActivity(new Intent(this, ManageConferenceActivity.class));
    }
  }

  //UNISOC:add for bug895363
  public void setAlert(AlertDialog alertDialog) {
    mAlert = alertDialog;
  }

  public void showDialpadFragment(boolean show, boolean animate) {
    if (show == isDialpadVisible()) {
      return;
    }

    FragmentManager dialpadFragmentManager = getDialpadFragmentManager();
    if (dialpadFragmentManager == null) {
      LogUtil.i("InCallActivity.showDialpadFragment", "Unable to obtain a FragmentManager");
      return;
    }

    if (!animate) {
      if (show) {
        showDialpadFragment();
      } else {
        hideDialpadFragment();
      }
    } else {
      if (show) {
        showDialpadFragment();
        getDialpadFragment().animateShowDialpad();
      }
      getDialpadFragment()
          .getView()
          .startAnimation(show ? dialpadSlideInAnimation : dialpadSlideOutAnimation);
    }

    ProximitySensor sensor = InCallPresenter.getInstance().getProximitySensor();
    if (sensor != null) {
      sensor.onDialpadVisible(show);
    }
    showDialpadRequest = DIALPAD_REQUEST_NONE;

    // Note:  onInCallScreenDialpadVisibilityChange is called here to ensure that the dialpad FAB
    // repositions itself.
    /* SPRD: modify for bug888126 @{ */
    InCallScreen inCallScreen = getInCallScreen();
    if (inCallScreen != null && inCallScreen.getInCallScreenFragment().isVisible()) {
      inCallScreen.onInCallScreenDialpadVisibilityChange(show);
    }
    /* @} */
  }

  private void showDialpadFragment() {
    FragmentManager dialpadFragmentManager = getDialpadFragmentManager();
    if (dialpadFragmentManager == null) {
      return;
    }

    FragmentTransaction transaction = dialpadFragmentManager.beginTransaction();
    DialpadFragment dialpadFragment = getDialpadFragment();
    if (dialpadFragment == null) {
      transaction.add(getDialpadContainerId(), new DialpadFragment(), Tags.DIALPAD_FRAGMENT);
    } else {
      transaction.show(dialpadFragment);
      dialpadFragment.setUserVisibleHint(true);
    }
    transaction.commitAllowingStateLoss();
    dialpadFragmentManager.executePendingTransactions();

    Logger.get(this).logScreenView(ScreenEvent.Type.INCALL_DIALPAD, this);
    updateNavigationBar(true /* isDialpadVisible */);
  }

  private void hideDialpadFragment() {
    FragmentManager dialpadFragmentManager = getDialpadFragmentManager();
    if (dialpadFragmentManager == null) {
      return;
    }

    DialpadFragment dialpadFragment = getDialpadFragment();
    if (dialpadFragment != null) {
      FragmentTransaction transaction = dialpadFragmentManager.beginTransaction();
      transaction.hide(dialpadFragment);
      transaction.commitAllowingStateLoss();
      dialpadFragmentManager.executePendingTransactions();
      dialpadFragment.setUserVisibleHint(false);
    }
    updateNavigationBar(false /* isDialpadVisible */);
  }

  public boolean isDialpadVisible() {
    DialpadFragment dialpadFragment = getDialpadFragment();
    return dialpadFragment != null
        && dialpadFragment.isAdded()
        && !dialpadFragment.isHidden()
        && dialpadFragment.getView() != null
        && dialpadFragment.getUserVisibleHint();
  }

  /** Returns the {@link DialpadFragment} that's shown by this activity, or {@code null} */
  @Nullable
  private DialpadFragment getDialpadFragment() {
    FragmentManager fragmentManager = getDialpadFragmentManager();
    if (fragmentManager == null) {
      return null;
    }
    return (DialpadFragment) fragmentManager.findFragmentByTag(Tags.DIALPAD_FRAGMENT);
  }

  public void onForegroundCallChanged(DialerCall newForegroundCall) {
    updateTaskDescription();

    if (newForegroundCall == null || !didShowAnswerScreen) {
      LogUtil.v("InCallActivity.onForegroundCallChanged", "resetting background color");
      updateWindowBackgroundColor(0 /* progress */);
    }
  }

  private void updateTaskDescription() {
    int color =
        getResources().getBoolean(R.bool.is_layout_landscape)
            ? ResourcesCompat.getColor(
                getResources(), R.color.statusbar_background_color, getTheme())
            : InCallPresenter.getInstance().getThemeColorManager().getSecondaryColor();
    setTaskDescription(
        new TaskDescription(
            getResources().getString(R.string.notification_ongoing_call), null /* icon */, color));
  }

  public void updateWindowBackgroundColor(@FloatRange(from = -1f, to = 1.0f) float progress) {
    ThemeColorManager themeColorManager = InCallPresenter.getInstance().getThemeColorManager();
    @ColorInt int top;
    @ColorInt int middle;
    @ColorInt int bottom;
    @ColorInt int gray = 0x66000000;

    if (ActivityCompat.isInMultiWindowMode(this)) {
      top = themeColorManager.getBackgroundColorSolid();
      middle = themeColorManager.getBackgroundColorSolid();
      bottom = themeColorManager.getBackgroundColorSolid();
    } else {
      top = themeColorManager.getBackgroundColorTop();
      middle = themeColorManager.getBackgroundColorMiddle();
      bottom = themeColorManager.getBackgroundColorBottom();
    }

    if (progress < 0) {
      float correctedProgress = Math.abs(progress);
      top = ColorUtils.blendARGB(top, gray, correctedProgress);
      middle = ColorUtils.blendARGB(middle, gray, correctedProgress);
      bottom = ColorUtils.blendARGB(bottom, gray, correctedProgress);
    }

    boolean backgroundDirty = false;
    if (backgroundDrawable == null) {
      backgroundDrawableColors = new int[] {top, middle, bottom};
      backgroundDrawable = new GradientDrawable(Orientation.TOP_BOTTOM, backgroundDrawableColors);
      backgroundDirty = true;
    } else {
      if (backgroundDrawableColors[0] != top) {
        backgroundDrawableColors[0] = top;
        backgroundDirty = true;
      }
      if (backgroundDrawableColors[1] != middle) {
        backgroundDrawableColors[1] = middle;
        backgroundDirty = true;
      }
      if (backgroundDrawableColors[2] != bottom) {
        backgroundDrawableColors[2] = bottom;
        backgroundDirty = true;
      }
      if (backgroundDirty) {
        backgroundDrawable.setColors(backgroundDrawableColors);
      }
    }

    if (backgroundDirty) {
      getWindow().setBackgroundDrawable(backgroundDrawable);
    }
  }

  public boolean isVisible() {
    return isVisible;
  }

  public boolean getCallCardFragmentVisible() {
    return didShowInCallScreen || didShowVideoCallScreen;
  }

  public void dismissKeyguard(boolean dismiss) {
    if (dismissKeyguard == dismiss) {
      return;
    }

    dismissKeyguard = dismiss;
    if (dismiss) {
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    } else {
      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    }
  }

  public void showDialogForPostCharWait(String callId, String chars) {
    if (isVisible) {
      PostCharDialogFragment fragment = new PostCharDialogFragment(callId, chars);
      fragment.show(getSupportFragmentManager(), Tags.POST_CHAR_DIALOG_FRAGMENT);

      showPostCharWaitDialogOnResume = false;
      showPostCharWaitDialogCallId = null;
      showPostCharWaitDialogChars = null;
    } else {
      showPostCharWaitDialogOnResume = true;
      showPostCharWaitDialogCallId = callId;
      showPostCharWaitDialogChars = chars;
    }
  }

  public void showDialogOrToastForDisconnectedCall(DisconnectMessage disconnectMessage) {
    LogUtil.i(
        "InCallActivity.showDialogOrToastForDisconnectedCall",
        "disconnect cause: %s",
        disconnectMessage);

    if (disconnectMessage.dialog == null || isFinishing()) {
      return;
    }

    dismissPendingDialogs();

    // Show a toast if the app is in background when a dialog can't be visible.
    if (!isVisible()) {
      Toast.makeText(getApplicationContext(), disconnectMessage.toastMessage, Toast.LENGTH_LONG)
          .show();
      return;
    }

    // Show the dialog.
    errorDialog = disconnectMessage.dialog;
    InCallUiLock lock = InCallPresenter.getInstance().acquireInCallUiLock("showErrorDialog");
    disconnectMessage.dialog.setOnDismissListener(
        dialogInterface -> {
          lock.release();
          onDialogDismissed();
        });
    disconnectMessage.dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    disconnectMessage.dialog.show();
  }

  private void onDialogDismissed() {
    errorDialog = null;
    CallList.getInstance().onErrorDialogDismissed();
  }

  public void dismissPendingDialogs() {
    LogUtil.enterBlock("InCallActivity.dismissPendingDialogs");

    if (!isVisible) {
      // Defer the dismissing action as the activity is not visible and onSaveInstanceState may have
      // been called.
      LogUtil.i(
          "InCallActivity.dismissPendingDialogs", "defer actions since activity is not visible");
      needDismissPendingDialogs = true;
      return;
    }

    // Dismiss the error dialog
    if (errorDialog != null) {
      errorDialog.dismiss();
      errorDialog = null;
    }

    // Dismiss the phone account selection dialog
    if (selectPhoneAccountDialogFragment != null) {
      selectPhoneAccountDialogFragment.dismiss();
      selectPhoneAccountDialogFragment = null;
    }

    // Dismiss the dialog for international call on WiFi
    InternationalCallOnWifiDialogFragment internationalCallOnWifiFragment =
        (InternationalCallOnWifiDialogFragment)
            getSupportFragmentManager().findFragmentByTag(Tags.INTERNATIONAL_CALL_ON_WIFI);
    if (internationalCallOnWifiFragment != null) {
      internationalCallOnWifiFragment.dismiss();
    }

    // Dismiss the answer screen
    AnswerScreen answerScreen = getAnswerScreen();
    if (answerScreen != null) {
      answerScreen.dismissPendingDialogs();
    }

    needDismissPendingDialogs = false;
  }

  /* UNISOC: modify for bug926625 @{ */
  public boolean IsAnswer() {
    return didShowAnswerScreen;
  }
  /* @} */

  private void enableInCallOrientationEventListener(boolean enable) {
    if (enable) {
      inCallOrientationEventListener.enable(true /* notifyDeviceOrientationChange */);
    } else {
      inCallOrientationEventListener.disable();
    }
  }

  public void setExcludeFromRecents(boolean exclude) {
    int taskId = getTaskId();

    List<AppTask> tasks = getSystemService(ActivityManager.class).getAppTasks();
    for (AppTask task : tasks) {
      try {
        if (task.getTaskInfo().id == taskId) {
          task.setExcludeFromRecents(exclude);
        }
      } catch (RuntimeException e) {
        LogUtil.e("InCallActivity.setExcludeFromRecents", "RuntimeException:\n%s", e);
      }
    }
  }

  @Nullable
  public FragmentManager getDialpadFragmentManager() {
    InCallScreen inCallScreen = getInCallScreen();
    // unisoc: add for bug895177
    DialerCall call = CallList.getInstance().getFirstCall();
    if (call == null) {
      call = CallList.getInstance().getBackgroundCall();
    }
    if (inCallScreen != null && inCallScreen.getInCallScreenFragment().isAdded() &&
            call !=null && !call.isVideoCall() && !VideoProfile.isVideo(call.getVideoState())) {
      return inCallScreen.getInCallScreenFragment().getChildFragmentManager();
    }
    /* SPRD: modify for bug888126 @{ */
    VideoCallScreen videoCallScreen = getVideoCallScreen();
    if(videoCallScreen != null && videoCallScreen.getVideoCallScreenFragment().isAdded()){
      return videoCallScreen.getVideoCallScreenFragment().getChildFragmentManager();
    }
    /* @} */
    return null;
  }

  public int getDialpadContainerId() {
    InCallScreen inCallScreen = getInCallScreen();
    // unisoc: add for bug895177
    DialerCall call = CallList.getInstance().getFirstCall();
    if (call == null) {
      call = CallList.getInstance().getBackgroundCall();
    }
    /* SPRD: modify for bug888126 @{ */
    VideoCallScreen videoCallScreen = getVideoCallScreen();
    if(inCallScreen != null && inCallScreen.getInCallScreenFragment().isAdded() &&
            call !=null && !call.isVideoCall() && !VideoProfile.isVideo(call.getVideoState())){
      return inCallScreen.getAnswerAndDialpadContainerResourceId();
    }else if(videoCallScreen != null && videoCallScreen.getVideoCallScreenFragment().isAdded()){
      return videoCallScreen.getAnswerAndDialpadContainerResourceId();
    }
    /* @} */
    return 0;
  }

  @Override
  public AnswerScreenDelegate newAnswerScreenDelegate(AnswerScreen answerScreen) {
    DialerCall call = CallList.getInstance().getCallById(answerScreen.getCallId());
    if (call == null) {
      // This is a work around for a bug where we attempt to create a new delegate after the call
      // has already been removed. An example of when this can happen is:
      // 1. incoming video call in landscape mode
      // 2. remote party hangs up
      // 3. activity switches from landscape to portrait
      // At step #3 the answer fragment will try to create a new answer delegate but the call won't
      // exist. In this case we'll simply return a stub delegate that does nothing. This is ok
      // because this new state is transient and the activity will be destroyed soon.
      LogUtil.i("InCallActivity.onPrimaryCallStateChanged", "call doesn't exist, using stub");
      return new AnswerScreenPresenterStub();
    } else {
      return new AnswerScreenPresenter(
          this, answerScreen, CallList.getInstance().getCallById(answerScreen.getCallId()));
    }
  }

  @Override
  public InCallScreenDelegate newInCallScreenDelegate() {
    return new CallCardPresenter(this);
  }

  @Override
  public InCallButtonUiDelegate newInCallButtonUiDelegate() {
    return new CallButtonPresenter(this);
  }

  @Override
  public VideoCallScreenDelegate newVideoCallScreenDelegate(VideoCallScreen videoCallScreen) {
    DialerCall dialerCall = CallList.getInstance().getCallById(videoCallScreen.getCallId());
    if (dialerCall != null && dialerCall.getVideoTech().shouldUseSurfaceView()) {
      return dialerCall.getVideoTech().createVideoCallScreenDelegate(this, videoCallScreen);
    }
    return new VideoCallPresenter();
  }

  public void onPrimaryCallStateChanged() {
    Trace.beginSection("InCallActivity.onPrimaryCallStateChanged");
    showMainInCallFragment();
    Trace.endSection();
  }

  public void showToastForWiFiToLteHandover(DialerCall call) {
    if (call.hasShownWiFiToLteHandoverToast()) {
      return;
    }

    Toast.makeText(this, R.string.video_call_wifi_to_lte_handover_toast, Toast.LENGTH_LONG).show();
    call.setHasShownWiFiToLteHandoverToast();
  }

  public void showDialogOrToastForWifiHandoverFailure(DialerCall call) {
    if (call.showWifiHandoverAlertAsToast()) {
      Toast.makeText(this, R.string.video_call_lte_to_wifi_failed_message, Toast.LENGTH_SHORT)
          .show();
      return;
    }

    dismissPendingDialogs();

    AlertDialog.Builder builder =
        new AlertDialog.Builder(this).setTitle(R.string.video_call_lte_to_wifi_failed_title);

    // This allows us to use the theme of the dialog instead of the activity
    View dialogCheckBoxView =
        View.inflate(builder.getContext(), R.layout.video_call_lte_to_wifi_failed, null /* root */);
    CheckBox wifiHandoverFailureCheckbox =
        (CheckBox) dialogCheckBoxView.findViewById(R.id.video_call_lte_to_wifi_failed_checkbox);
    wifiHandoverFailureCheckbox.setChecked(false);

    InCallUiLock lock = InCallPresenter.getInstance().acquireInCallUiLock("WifiFailedDialog");
    errorDialog =
        builder
            .setView(dialogCheckBoxView)
            .setMessage(R.string.video_call_lte_to_wifi_failed_message)
            .setOnCancelListener(dialogInterface -> onDialogDismissed())
            .setPositiveButton(
                android.R.string.ok,
                (dialogInterface, id) -> {
                  call.setDoNotShowDialogForHandoffToWifiFailure(
                      wifiHandoverFailureCheckbox.isChecked());
                  dialogInterface.cancel();
                  onDialogDismissed();
                })
            .setOnDismissListener(dialogInterface -> lock.release())
            .create();
    errorDialog.show();
  }

  public void showDialogForInternationalCallOnWifi(@NonNull DialerCall call) {
    if (!InternationalCallOnWifiDialogFragment.shouldShow(this)) {
      LogUtil.i(
          "InCallActivity.showDialogForInternationalCallOnWifi",
          "InternationalCallOnWifiDialogFragment.shouldShow returned false");
      return;
    }

    InternationalCallOnWifiDialogFragment fragment =
        InternationalCallOnWifiDialogFragment.newInstance(
            call.getId(), internationalCallOnWifiCallback);
    fragment.show(getSupportFragmentManager(), Tags.INTERNATIONAL_CALL_ON_WIFI);
  }

  @Override
  public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
    super.onMultiWindowModeChanged(isInMultiWindowMode);
    updateNavigationBar(isDialpadVisible());
  }

  private void updateNavigationBar(boolean isDialpadVisible) {
    if (ActivityCompat.isInMultiWindowMode(this)) {
      return;
    }

    View navigationBarBackground = getWindow().findViewById(R.id.navigation_bar_background);
    if (navigationBarBackground != null) {
      navigationBarBackground.setVisibility(isDialpadVisible ? View.VISIBLE : View.GONE);
    }
  }

  public void setAllowOrientationChange(boolean allowOrientationChange) {
    if (this.allowOrientationChange == allowOrientationChange) {
      return;
    }
    this.allowOrientationChange = allowOrientationChange;
    if (!allowOrientationChange) {
      setRequestedOrientation(InCallOrientationEventListener.ACTIVITY_PREFERENCE_DISALLOW_ROTATION);
    } else {
      setRequestedOrientation(InCallOrientationEventListener.ACTIVITY_PREFERENCE_ALLOW_ROTATION_FULL);//UNISOC:modify for bug900297
    }
    enableInCallOrientationEventListener(allowOrientationChange);
  }

  public void hideMainInCallFragment() {
    LogUtil.enterBlock("InCallActivity.hideMainInCallFragment");
    if (getCallCardFragmentVisible()) {
      FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
      hideInCallScreenFragment(transaction);
      hideVideoCallScreenFragment(transaction);
      transaction.commitAllowingStateLoss();
      getSupportFragmentManager().executePendingTransactions();
    }
  }

  private void showMainInCallFragment() {
    Trace.beginSection("InCallActivity.showMainInCallFragment");
    // If the activity's onStart method hasn't been called yet then defer doing any work.
    if (!isVisible) {
      LogUtil.i("InCallActivity.showMainInCallFragment", "not visible yet/anymore");
      DialerCall call = CallList.getInstance().getFirstCall();//add for bug969474, modify for bug916332
      if(call != null && call.isVideoCall() && call.getVideoTech() != null && call.getVideoTech().isPaused()) {
          InCallPresenter.getInstance().updateNotification();
      }
      Trace.endSection();
      return;
    }
    //UNISOC:add for bug895363
    if (mAlert != null) {
      mAlert.dismiss();
      mAlert = null ;
    }

    // Don't let this be reentrant.
    if (isInShowMainInCallFragment) {
      LogUtil.i("InCallActivity.showMainInCallFragment", "already in method, bailing");
      Trace.endSection();
      return;
    }

    isInShowMainInCallFragment = true;
    ShouldShowUiResult shouldShowAnswerUi = getShouldShowAnswerUi();
    ShouldShowUiResult shouldShowVideoUi = getShouldShowVideoUi();
    ShouldShowUiResult shouldShowRttUi = getShouldShowRttUi();
    LogUtil.i(
        "InCallActivity.showMainInCallFragment",
        "shouldShowAnswerUi: %b, shouldShowRttUi: %b, shouldShowVideoUi: %b "
            + "didShowAnswerScreen: %b, didShowInCallScreen: %b, didShowRttCallScreen: %b, "
            + "didShowVideoCallScreen: %b",
        shouldShowAnswerUi.shouldShow,
        shouldShowRttUi.shouldShow,
        shouldShowVideoUi.shouldShow,
        didShowAnswerScreen,
        didShowInCallScreen,
        didShowRttCallScreen,
        didShowVideoCallScreen);
    // Only video call ui allows orientation change.
    DialerCall call = CallList.getInstance().getFirstCall();//modify for bug908219
    setAllowOrientationChange(shouldShowVideoUi.shouldShow && (call != null && !call.isRingToneOnAudioCall()));

    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    boolean didChange;
    if (shouldShowAnswerUi.shouldShow) {
      /* SPRD: DSDA. @{*/
      if (ImsManagerEx.isDualVoLTERegistered()) {
        if (CallList.getInstance().getFirstCallWithState(DialerCall.State.DIALING) != null) {
          didChange = false;
        } else if (CallList.getInstance().getRingingCallSize() >1) {
          didChange = showInCallScreenFragment(transaction);
        } else {
          didChange = hideInCallScreenFragment(transaction);
        }
      } else {
        didChange = hideInCallScreenFragment(transaction);
      }
      /* @} */
      didChange = hideInCallScreenFragment(transaction);
      didChange |= hideVideoCallScreenFragment(transaction);
      didChange |= hideRttCallScreenFragment(transaction);
      didChange |= showAnswerScreenFragment(transaction, shouldShowAnswerUi.call);
    } else if (shouldShowVideoUi.shouldShow) {
      didChange = hideInCallScreenFragment(transaction);
      didChange |= showVideoCallScreenFragment(transaction, shouldShowVideoUi.call);
      didChange |= hideRttCallScreenFragment(transaction);
      didChange |= hideAnswerScreenFragment(transaction);
    } else if (shouldShowRttUi.shouldShow) {
      didChange = hideInCallScreenFragment(transaction);
      didChange |= hideVideoCallScreenFragment(transaction);
      didChange |= hideAnswerScreenFragment(transaction);
      didChange |= showRttCallScreenFragment(transaction, shouldShowRttUi.call);
    } else {
      didChange = showInCallScreenFragment(transaction);
      didChange |= hideVideoCallScreenFragment(transaction);
      didChange |= hideRttCallScreenFragment(transaction);
      didChange |= hideAnswerScreenFragment(transaction);
    }

    if (didChange) {
      Trace.beginSection("InCallActivity.commitTransaction");
      transaction.commitNow();
      Trace.endSection();
      Logger.get(this).logScreenView(ScreenEvent.Type.INCALL, this);
    }
    isInShowMainInCallFragment = false;
    Trace.endSection();
  }

  private ShouldShowUiResult getShouldShowAnswerUi() {
    DialerCall call = CallList.getInstance().getIncomingCall();
    if (call != null) {
      LogUtil.i("InCallActivity.getShouldShowAnswerUi", "found incoming call");
      TelecomAdapter.getInstance().stopForegroundNotification();//UNISOC:add for bug916926
      /* SPRD: DSDA. @{*/
      boolean shouldShowAnswerUi = true;
      if (ImsManagerEx.isDualVoLTERegistered()
              && CallList.getInstance().getUserPrimaryCall() != null
              && CallList.getInstance().getUserPrimaryCall().getState() == DialerCall.State.DIALING) {
        shouldShowAnswerUi = false;
      }
      /* @} */
      return new ShouldShowUiResult(shouldShowAnswerUi, call);
    }

    call = CallList.getInstance().getVideoUpgradeRequestCall();
    if (call != null) {
      LogUtil.i("InCallActivity.getShouldShowAnswerUi", "found video upgrade request");
      return new ShouldShowUiResult(true, call);
    }

    // Check if we're showing the answer screen and the call is disconnected. If this condition is
    // true then we won't switch from the answer UI to the in call UI. This prevents flicker when
    // the user rejects an incoming call.
    call = CallList.getInstance().getFirstCall();
    if (call == null) {
      call = CallList.getInstance().getBackgroundCall();
    }
    if (didShowAnswerScreen && (call == null || call.getState() == State.DISCONNECTED)) {
      LogUtil.i("InCallActivity.getShouldShowAnswerUi", "found disconnecting incoming call");
      return new ShouldShowUiResult(true, call);
    }

    return new ShouldShowUiResult(false, null);
  }

  private static ShouldShowUiResult getShouldShowVideoUi() {
    DialerCall call = CallList.getInstance().getFirstCall();
    if (call == null) {
      LogUtil.i("InCallActivity.getShouldShowVideoUi", "null call");
      return new ShouldShowUiResult(false, null);
    }

    if (call.isVideoCall()) {
      LogUtil.i("InCallActivity.getShouldShowVideoUi", "found video call");
      return new ShouldShowUiResult(true, call);
    }

    if (call.hasSentVideoUpgradeRequest() || call.hasReceivedVideoUpgradeRequest()) {
      LogUtil.i("InCallActivity.getShouldShowVideoUi", "upgrading to video");
      return new ShouldShowUiResult(true, call);
    }

    return new ShouldShowUiResult(false, null);
  }

  private static ShouldShowUiResult getShouldShowRttUi() {
    DialerCall call = CallList.getInstance().getFirstCall();
    if (call == null) {
      LogUtil.i("InCallActivity.getShouldShowRttUi", "null call");
      return new ShouldShowUiResult(false, null);
    }

    if (call.isRttCall()) {
      LogUtil.i("InCallActivity.getShouldShowRttUi", "found rtt call");
      return new ShouldShowUiResult(true, call);
    }

    if (call.hasSentRttUpgradeRequest()) {
      LogUtil.i("InCallActivity.getShouldShowRttUi", "upgrading to rtt");
      return new ShouldShowUiResult(true, call);
    }

    return new ShouldShowUiResult(false, null);
  }

  private boolean showAnswerScreenFragment(FragmentTransaction transaction, DialerCall call) {
    // When rejecting a call the active call can become null in which case we should continue
    // showing the answer screen.
    if (didShowAnswerScreen && call == null) {
      return false;
    }

    Assert.checkArgument(call != null, "didShowAnswerScreen was false but call was still null");

    boolean isVideoUpgradeRequest = call.hasReceivedVideoUpgradeRequest();

    // Check if we're already showing an answer screen for this call.
    if (didShowAnswerScreen) {
      AnswerScreen answerScreen = getAnswerScreen();
      if (answerScreen.getCallId().equals(call.getId())
          && answerScreen.isVideoCall() == call.isVideoCall()
          && answerScreen.isVideoUpgradeRequest() == isVideoUpgradeRequest
          && !answerScreen.isActionTimeout()) {
        LogUtil.d(
            "InCallActivity.showAnswerScreenFragment",
            "answer fragment exists for same call and has NOT been accepted/rejected/timed out");
        return false;
      }
      if (answerScreen.isActionTimeout()) {
        LogUtil.i(
            "InCallActivity.showAnswerScreenFragment",
            "answer fragment exists but has been accepted/rejected and timed out");
      } else {
        LogUtil.i(
            "InCallActivity.showAnswerScreenFragment",
            "answer fragment exists but arguments do not match");
      }
      hideAnswerScreenFragment(transaction);
    }

    // Show a new answer screen.
    AnswerScreen answerScreen =
        AnswerBindings.createAnswerScreen(
            call.getId(),
            call.isRttCall(),
            call.isVideoCall(),
            isVideoUpgradeRequest,
            call.getVideoTech().isSelfManagedCamera(),
            shouldAllowAnswerAndRelease(call),
            CallList.getInstance().getBackgroundCall() != null);
    transaction.add(R.id.main, answerScreen.getAnswerScreenFragment(), Tags.ANSWER_SCREEN);

    Logger.get(this).logScreenView(ScreenEvent.Type.INCOMING_CALL, this);
    didShowAnswerScreen = true;
    return true;
  }

  private boolean shouldAllowAnswerAndRelease(DialerCall call) {
    if (CallList.getInstance().getActiveCall() == null) {
      LogUtil.i("InCallActivity.shouldAllowAnswerAndRelease", "no active call");
      return false;
    }
    if (getSystemService(TelephonyManager.class).getPhoneType()
        == TelephonyManager.PHONE_TYPE_CDMA) {
      LogUtil.i("InCallActivity.shouldAllowAnswerAndRelease", "PHONE_TYPE_CDMA not supported");
      return false;
    }
    if (/*call.isVideoCall() || */call.hasReceivedVideoUpgradeRequest()) { // UNISOC: modify for bug982369
      LogUtil.i("InCallActivity.shouldAllowAnswerAndRelease", "video call");
      return false;
    }
    if (!ConfigProviderBindings.get(this)
        .getBoolean(ConfigNames.ANSWER_AND_RELEASE_ENABLED, true)) {
      LogUtil.i("InCallActivity.shouldAllowAnswerAndRelease", "disabled by config");
      return false;
    }

    return true;
  }

  private boolean hideAnswerScreenFragment(FragmentTransaction transaction) {
    if (!didShowAnswerScreen) {
      return false;
    }
    AnswerScreen answerScreen = getAnswerScreen();
    if (answerScreen != null) {
      transaction.remove(answerScreen.getAnswerScreenFragment());
    }

    didShowAnswerScreen = false;
    return true;
  }

  private boolean showInCallScreenFragment(FragmentTransaction transaction) {
    if (didShowInCallScreen) {
      return false;
    }
    InCallScreen inCallScreen = InCallBindings.createInCallScreen();
    transaction.add(R.id.main, inCallScreen.getInCallScreenFragment(), Tags.IN_CALL_SCREEN);
    Logger.get(this).logScreenView(ScreenEvent.Type.INCALL, this);
    didShowInCallScreen = true;
    return true;
  }

  private boolean hideInCallScreenFragment(FragmentTransaction transaction) {
    if (!didShowInCallScreen) {
      return false;
    }
    InCallScreen inCallScreen = getInCallScreen();
    if (inCallScreen != null) {
      transaction.remove(inCallScreen.getInCallScreenFragment());
    }
    didShowInCallScreen = false;
    return true;
  }

  private boolean showRttCallScreenFragment(FragmentTransaction transaction, DialerCall call) {
    if (didShowRttCallScreen) {
      // This shouldn't happen since only one RTT call is allow at same time.
      if (!getRttCallScreen().getCallId().equals(call.getId())) {
        LogUtil.e("InCallActivity.showRttCallScreenFragment", "RTT call id doesn't match");
      }
      return false;
    }
    RttCallScreen rttCallScreen = RttBindings.createRttCallScreen(call.getId());
    transaction.add(R.id.main, rttCallScreen.getRttCallScreenFragment(), Tags.RTT_CALL_SCREEN);
    Logger.get(this).logScreenView(ScreenEvent.Type.INCALL, this);
    didShowRttCallScreen = true;
    return true;
  }

  private boolean hideRttCallScreenFragment(FragmentTransaction transaction) {
    if (!didShowRttCallScreen) {
      return false;
    }
    RttCallScreen rttCallScreen = getRttCallScreen();
    if (rttCallScreen != null) {
      transaction.remove(rttCallScreen.getRttCallScreenFragment());
    }
    didShowRttCallScreen = false;
    return true;
  }

  private boolean showVideoCallScreenFragment(FragmentTransaction transaction, DialerCall call) {
    if (didShowVideoCallScreen) {
      VideoCallScreen videoCallScreen = getVideoCallScreen();
      if (videoCallScreen.getCallId().equals(call.getId())) {
        return false;
      }
      LogUtil.i(
          "InCallActivity.showVideoCallScreenFragment",
          "video call fragment exists but arguments do not match");
      hideVideoCallScreenFragment(transaction);
    }

    LogUtil.i("InCallActivity.showVideoCallScreenFragment", "call: %s", call);

    VideoCallScreen videoCallScreen =
        VideoBindings.createVideoCallScreen(
            call.getId(), call.getVideoTech().shouldUseSurfaceView());
    transaction.add(
        R.id.main, videoCallScreen.getVideoCallScreenFragment(), Tags.VIDEO_CALL_SCREEN);

    Logger.get(this).logScreenView(ScreenEvent.Type.INCALL, this);
    didShowVideoCallScreen = true;
    return true;
  }

  private boolean hideVideoCallScreenFragment(FragmentTransaction transaction) {
    if (!didShowVideoCallScreen) {
      return false;
    }
    VideoCallScreen videoCallScreen = getVideoCallScreen();
    if (videoCallScreen != null) {
      transaction.remove(videoCallScreen.getVideoCallScreenFragment());
    }
    didShowVideoCallScreen = false;
    return true;
  }

  private AnswerScreen getAnswerScreen() {
    return (AnswerScreen) getSupportFragmentManager().findFragmentByTag(Tags.ANSWER_SCREEN);
  }

  private InCallScreen getInCallScreen() {
    return (InCallScreen) getSupportFragmentManager().findFragmentByTag(Tags.IN_CALL_SCREEN);
  }

  private VideoCallScreen getVideoCallScreen() {
    return (VideoCallScreen) getSupportFragmentManager().findFragmentByTag(Tags.VIDEO_CALL_SCREEN);
  }
  //SPRD:add for bug711670,call record for video
  private InCallScreen getCurrentInCallScreen(){
    if(didShowVideoCallScreen){
      return (InCallScreen) getSupportFragmentManager().findFragmentByTag(Tags.VIDEO_CALL_SCREEN);
    }else {
      return (InCallScreen) getSupportFragmentManager().findFragmentByTag(Tags.IN_CALL_SCREEN);
    }
  }




  private RttCallScreen getRttCallScreen() {
    return (RttCallScreen) getSupportFragmentManager().findFragmentByTag(Tags.RTT_CALL_SCREEN);
  }

  @Override
  public void onPseudoScreenStateChanged(boolean isOn) {
    LogUtil.i("InCallActivity.onPseudoScreenStateChanged", "isOn: " + isOn);
    pseudoBlackScreenOverlay.setVisibility(isOn ? View.GONE : View.VISIBLE);
  }

  /**
   * For some touch related issue, turning off the screen can be faked by drawing a black view over
   * the activity. All touch events started when the screen is "off" is rejected.
   *
   * @see PseudoScreenState
   */
  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {
    // Reject any gesture that started when the screen is in the fake off state.
    if (touchDownWhenPseudoScreenOff) {
      if (event.getAction() == MotionEvent.ACTION_UP) {
        touchDownWhenPseudoScreenOff = false;
      }
      return true;
    }
    // Reject all touch event when the screen is in the fake off state.
    if (!InCallPresenter.getInstance().getPseudoScreenState().isOn()) {
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
        touchDownWhenPseudoScreenOff = true;
        LogUtil.i("InCallActivity.dispatchTouchEvent", "touchDownWhenPseudoScreenOff");
      }
      return true;
    }
    return super.dispatchTouchEvent(event);
  }

  @Override
  public RttCallScreenDelegate newRttCallScreenDelegate(RttCallScreen videoCallScreen) {
    return new RttCallPresenter();
  }

  private static class ShouldShowUiResult {
    public final boolean shouldShow;
    public final DialerCall call;

    ShouldShowUiResult(boolean shouldShow, DialerCall call) {
      this.shouldShow = shouldShow;
      this.call = call;
    }
  }

  private static final class IntentExtraNames {
    static final String FOR_FULL_SCREEN = "InCallActivity.for_full_screen_intent";
    static final String NEW_OUTGOING_CALL = "InCallActivity.new_outgoing_call";
    static final String SHOW_DIALPAD = "InCallActivity.show_dialpad";
  }

  private static final class KeysForSavedInstance {
    static final String DIALPAD_TEXT = "InCallActivity.dialpad_text";
    static final String DID_SHOW_ANSWER_SCREEN = "did_show_answer_screen";
    static final String DID_SHOW_IN_CALL_SCREEN = "did_show_in_call_screen";
    static final String DID_SHOW_VIDEO_CALL_SCREEN = "did_show_video_call_screen";
    static final String DID_SHOW_RTT_CALL_SCREEN = "did_show_rtt_call_screen";
  }

  /** Request codes for pending intents. */
  public static final class PendingIntentRequestCodes {
    static final int NON_FULL_SCREEN = 0;
    static final int FULL_SCREEN = 1;
    static final int BUBBLE = 2;
  }

  private static final class Tags {
    static final String ANSWER_SCREEN = "tag_answer_screen";
    static final String DIALPAD_FRAGMENT = "tag_dialpad_fragment";
    static final String IN_CALL_SCREEN = "tag_in_call_screen";
    static final String INTERNATIONAL_CALL_ON_WIFI = "tag_international_call_on_wifi";
    static final String SELECT_ACCOUNT_FRAGMENT = "tag_select_account_fragment";
    static final String VIDEO_CALL_SCREEN = "tag_video_call_screen";
    static final String RTT_CALL_SCREEN = "tag_rtt_call_screen";
    static final String POST_CHAR_DIALOG_FRAGMENT = "tag_post_char_dialog_fragment";
  }

  private static final class ConfigNames {
    static final String ANSWER_AND_RELEASE_ENABLED = "answer_and_release_enabled";
  }

  private static final class InternationalCallOnWifiCallback
      implements InternationalCallOnWifiDialogFragment.Callback {
    private static final String TAG = InternationalCallOnWifiCallback.class.getCanonicalName();

    @Override
    public void continueCall(@NonNull String callId) {
      LogUtil.i(TAG, "Continuing call with ID: %s", callId);
    }

    @Override
    public void cancelCall(@NonNull String callId) {
      DialerCall call = CallList.getInstance().getCallById(callId);
      if (call == null) {
        LogUtil.i(TAG, "Call destroyed before the dialog is closed");
        return;
      }

      LogUtil.i(TAG, "Disconnecting international call on WiFi");
      call.disconnect();
    }
  }

  private static final class SelectPhoneAccountListener
      extends SelectPhoneAccountDialogFragment.SelectPhoneAccountListener {
    private static final String TAG = SelectPhoneAccountListener.class.getCanonicalName();

    @Override
    public void onPhoneAccountSelected(
        PhoneAccountHandle selectedAccountHandle, boolean setDefault, String callId) {
      DialerCall call = CallList.getInstance().getCallById(callId);
      LogUtil.i(TAG, "Phone account select with call:\n%s", call);

      if (call != null) {
        call.phoneAccountSelected(selectedAccountHandle, setDefault);
      }
    }

    @Override
    public void onDialogDismissed(String callId) {
      DialerCall call = CallList.getInstance().getCallById(callId);
      LogUtil.i(TAG, "Disconnecting call:\n%s" + call);

      if (call != null) {
        call.disconnect();
      }
      else {//UNISOC:add for bug961041
        DialerCall waitingForAccountCall = CallList.getInstance().getWaitingForAccountCall();
        if (waitingForAccountCall != null) {
          LogUtil.i(TAG, "disconnect waitingForAccountCall");
          waitingForAccountCall.disconnect();
        }
      }
    }
  }


  /** SPRD Feature Porting: Add for call recorder feature. @{ */
  private PhoneRecorderHelper.OnStateChangedListener mRecorderStateChangedListener =
          new PhoneRecorderHelper.OnStateChangedListener() {
            public void onTimeChanged(long time) {
              PhoneRecorderHelper.State state = getRecorderState();
              InCallScreen inCallScreen = getCurrentInCallScreen();
              if (inCallScreen != null) {
                // UNISOC: modify for bug921220
                if (time == 0 || (state != null && !state.isActive())) {
                  inCallScreen.setRecordTime(getString(R.string.call_recording_setting_title));
                } else {
                  inCallScreen.setRecord(true);
                  inCallScreen.setRecordTime(DateUtils.formatElapsedTime(time / 1000));
                }
              }
            }

            public void onStateChanged(PhoneRecorderHelper.State state) {
              setRecorderState(state);
              if (!state.isActive()) {
                InCallScreen inCallScreen = getCurrentInCallScreen();
                if (inCallScreen != null) {
                  inCallScreen.setRecordTime(getString(R.string.call_recording_setting_title));
                }
              }
            }

            @Override
            public void onShowMessage(int type, String msg) {
              String res = null;
              boolean resetIcon = false;
              switch (type) {
                case PhoneRecorderHelper.TYPE_ERROR_SD_NOT_EXIST:
                  res = getString(R.string.no_sd_card);
                  resetIcon = true;
                  break;
                case PhoneRecorderHelper.TYPE_ERROR_SD_FULL:
                  res = getString(R.string.storage_is_full);
                  resetIcon = true;
                  break;
                case PhoneRecorderHelper.TYPE_ERROR_SD_ACCESS:
                  res = getString(R.string.sdcard_access_error);
                  resetIcon = true;
                  break;
                case PhoneRecorderHelper.TYPE_ERROR_IN_RECORD:
                  res = getString(R.string.used_by_other_applications);
                  resetIcon = true;
                  break;
                case PhoneRecorderHelper.TYPE_ERROR_INTERNAL:
                  resetIcon = true;
                  break;
                case PhoneRecorderHelper.TYPE_MSG_PATH:
                case PhoneRecorderHelper.TYPE_SAVE_FAIL:
                  res = msg;
                  resetIcon = true;
                  break;
                case PhoneRecorderHelper.TYPE_NO_AVAILABLE_STORAGE:
                  res = getString(R.string.no_available_storage);
                  resetIcon = true;
                case PhoneRecorderHelper.TYPE_ERROR_FILE_INIT://add for bug932735
                  res = getString(R.string.record_failed_error);
                  resetIcon = true;
                  break;
              }
              if (resetIcon) {
                InCallScreen inCallScreen = getCurrentInCallScreen();
                if (inCallScreen != null) {
                  inCallScreen.setRecord(false);
                }
              }

              LogUtil.i("InCallActivity onShowMessage", " toast message: " + res);
              if (!TextUtils.isEmpty(res)) {
                Toast.makeText(InCallActivity.this, res, Toast.LENGTH_LONG).show();
              }
            }
          };

  // Modify permissions judgements for call recording
  // There are many permissions needed for recording, we need make sure dialer have all of these
  // permissions, if we grant part of these permissions,just request for the rest permissions.
  public void recordClick() {
    List<String> requestPermissionsList = new ArrayList<>();
    for (int i = 0; i < mPermissions.length; i++) {
      if (!PermissionsUtil.hasPermission(this, mPermissions[i])) {
        requestPermissionsList.add(mPermissions[i]);
      }
    }
    String[] requestPermissions = requestPermissionsList.toArray(
            new String[requestPermissionsList.size()]);

    if (requestPermissions.length == 0) {
      InCallPresenter.getInstance().toggleRecorder();
    } else {
      requestPermissions(requestPermissions, MICROPHONE_AND_STORAGE_PERMISSION_REQUEST_CODE);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    if (requestCode == MICROPHONE_AND_STORAGE_PERMISSION_REQUEST_CODE) {
      // toggleRecorder since we were missing the permission before this.
      boolean isPermissionGranted = false;
      // Only if all requested permissions granted, can we toggleRecorder.
      if (grantResults.length > 0) {
        // grantResults's length greater than 0 means user has make a decision
        isPermissionGranted = true;
      }
      for (int i = 0; i < grantResults.length; i++) {
        isPermissionGranted = isPermissionGranted && grantResults[i] == PackageManager.PERMISSION_GRANTED;
      }
      if (isPermissionGranted) {
        InCallPresenter.getInstance().toggleRecorder();
      } else {
        InCallScreen inCallScreen = getCurrentInCallScreen();
        if (inCallScreen != null) {
          inCallScreen.setRecord(false);
        }
        Toast.makeText(this, R.string.permission_no_record, Toast.LENGTH_LONG).show();
      }
    }
  }

  public void setRecorderState(PhoneRecorderHelper.State state) {
    mRecorderState = state;
  }

  public PhoneRecorderHelper.State getRecorderState() {
    return mRecorderState;
  }
  /* @} */
  /** kill-stop mechanism BEGIN */
  @Override
  protected void onPostResume() {
    super.onPostResume();
    // UNISOC: add for bug918127
    if (PermissionsUtil.hasPermission(this, UPDATE_DEVICE_STATS)) {
      LowmemoryUtils.killStopFrontApp(LowmemoryUtils.CANCEL_KILL_STOP_TIMEOUT);
    }
    LogUtil.i("InCallActivity.onPostResume","killStopFrontApp : CANCEL_KILL_STOP_TIMEOUT");
  }
  /** @} */
}
