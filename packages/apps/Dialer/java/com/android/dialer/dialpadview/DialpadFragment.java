/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.dialer.dialpadview;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Trace;
import android.os.UserManager;
import android.provider.Contacts.People;
import android.provider.Contacts.Phones;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.Contacts.PhonesColumns;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.FloatingActionButton;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.contacts.common.dialog.CallSubjectDialog;
import com.android.contacts.common.util.StopWatch;
import com.android.dialer.animation.AnimUtils;
import com.android.dialer.app.DialtactsActivity;
import com.android.dialer.app.fastdial.FastDialUtils;
import com.android.dialer.callintent.CallInitiationType;
import com.android.dialer.callintent.CallIntentBuilder;
import com.android.dialer.common.Assert;
import com.android.dialer.common.FragmentUtils;
import com.android.dialer.common.LogUtil;
import com.android.dialer.common.concurrent.DialerExecutor;
import com.android.dialer.common.concurrent.DialerExecutor.Worker;
import com.android.dialer.common.concurrent.DialerExecutorComponent;
import com.android.dialer.common.concurrent.ThreadUtil;
import com.android.dialer.location.GeoUtil;
import com.android.dialer.logging.UiAction;
import com.android.dialer.oem.MotorolaUtils;
import com.android.dialer.performancereport.PerformanceReport;
import com.android.dialer.phonenumberutil.PhoneNumberHelper;
import com.android.dialer.precall.PreCall;
import com.android.dialer.proguard.UsedByReflection;
import com.android.dialer.sprd.util.IpDialingUtils;
import com.android.dialer.telecom.TelecomUtil;
import com.android.dialer.util.CallUtil;
import com.android.dialer.util.DialerUtils;
import com.android.dialer.util.PermissionsUtil;
import com.android.dialer.util.ViewUtil;
import com.android.dialer.widget.FloatingActionButtonController;
import com.google.common.base.Ascii;
import com.google.common.base.Optional;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.support.annotation.NonNull;
import android.telephony.SubscriptionInfo;
import android.util.Log;
import android.widget.ImageButton;
import com.android.contacts.common.widget.FloatingActionButtonContainerController;
import com.android.incallui.InCallPresenter;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import java.util.HashMap;
import com.android.incallui.call.CallList;
import android.telecom.TelecomManager;
import com.android.incallui.call.DialerCall;
import android.widget.Toast;
import static android.Manifest.permission.READ_PHONE_STATE;
import android.telephony.SubscriptionManager;
import com.android.dialer.calllogutils.PhoneAccountUtils;
import com.android.dialer.util.DialerUtils;
import com.android.ims.internal.ImsManagerEx;
import com.android.ims.internal.IImsServiceEx;
import com.android.ims.internal.IImsRegisterListener;
import com.android.ims.ImsManager;
import android.telephony.CarrierConfigManager;
import android.telephony.CarrierConfigManagerEx;

import com.android.incallui.sprd.InCallUiUtils;
import java.util.Iterator;

import com.android.contacts.common.widget.SelectPhoneAccountDialogFragment.SelectPhoneAccountListener;
import android.text.InputFilter;

/** Fragment that displays a twelve-key phone dialpad. */
public class DialpadFragment extends Fragment
    implements View.OnClickListener,
        View.OnLongClickListener,
        View.OnKeyListener,
        AdapterView.OnItemClickListener,
        TextWatcher,
        PopupMenu.OnMenuItemClickListener,
        DialpadKeyButton.OnPressedListener,CallList.Listener {

  private static final String TAG = "DialpadFragment";
  private static final String EMPTY_NUMBER = "";
  private static final char PAUSE = ',';
  private static final char WAIT = ';';
  /** The length of DTMF tones in milliseconds */
  private static final int TONE_LENGTH_MS = 150;

  private static final int TONE_LENGTH_INFINITE = -1;
  /** The DTMF tone volume relative to other sounds in the stream */
  private static final int TONE_RELATIVE_VOLUME = 80;
  /** Stream type used to play the DTMF tones off call, and mapped to the volume control keys */
  private static final int DIAL_TONE_STREAM_TYPE = AudioManager.STREAM_DTMF;
  /** Identifier for the "Add Call" intent extra. */
  private static final String ADD_CALL_MODE_KEY = "add_call_mode";
  /**
   * Identifier for intent extra for sending an empty Flash message for CDMA networks. This message
   * is used by the network to simulate a press/depress of the "hookswitch" of a landline phone. Aka
   * "empty flash".
   *
   * <p>TODO: Using an intent extra to tell the phone to send this flash is a temporary measure. To
   * be replaced with an Telephony/TelecomManager call in the future. TODO: Keep in sync with the
   * string defined in OutgoingCallBroadcaster.java in Phone app until this is replaced with the
   * Telephony/Telecom API.
   */
  private static final String EXTRA_SEND_EMPTY_FLASH = "com.android.phone.extra.SEND_EMPTY_FLASH";

  private static final String PREF_DIGITS_FILLED_BY_INTENT = "pref_digits_filled_by_intent";
  private static final String PREF_IS_DIALPAD_SLIDE_OUT = "pref_is_dialpad_slide_out";

  private static Optional<String> currentCountryIsoForTesting = Optional.absent();

  private final Object toneGeneratorLock = new Object();
  /** Set of dialpad keys that are currently being pressed */
  private final HashSet<View> pressedDialpadKeys = new HashSet<>(12);

  private OnDialpadQueryChangedListener dialpadQueryListener;
  private DialpadView dialpadView;
  private EditText digits;
  private int dialpadSlideInDuration;
  /** Remembers if we need to clear digits field when the screen is completely gone. */
  private boolean clearDigitsOnStop;

  private View overflowMenuButton;
  private PopupMenu overflowPopupMenu;
  private View delete;
  private ToneGenerator toneGenerator;
  private FloatingActionButtonController floatingActionButtonController;
  private FloatingActionButton floatingActionButton;
  private ListView dialpadChooser;
  private DialpadChooserAdapter dialpadChooserAdapter;
  /** Regular expression prohibiting manual phone call. Can be empty, which means "no rule". */
  private String prohibitedPhoneNumberRegexp;

  private PseudoEmergencyAnimator pseudoEmergencyAnimator;
  private String lastNumberDialed = EMPTY_NUMBER;

  // determines if we want to playback local DTMF tones.
  private boolean dTMFToneEnabled;
  private CallStateReceiver callStateReceiver;
  private boolean wasEmptyBeforeTextChange;
  /**
   * This field is set to true while processing an incoming DIAL intent, in order to make sure that
   * SpecialCharSequenceMgr actions can be triggered by user input but *not* by a tel: URI passed by
   * some other app. It will be set to false when all digits are cleared.
   */
  private boolean digitsFilledByIntent;

  private boolean startedFromNewIntent = false;
  private boolean firstLaunch = false;
  private boolean animate = false;

  private boolean isLayoutRtl;
  private boolean isLandscape;

  private DialerExecutor<String> initPhoneNumberFormattingTextWatcherExecutor;
  private boolean isDialpadSlideUp;

  /* SPRD: add for bug731278 @{ */
  private FloatingActionButtonContainerController mFloatingActionFirstButtonController;
  private FloatingActionButtonContainerController mFloatingActionButtonSencondaryController;
  private ImageView mFirstSimIconView;
  private TextView mFirstSimDisplayName;
  private ImageView mSecSimIconView;
  private TextView mSecSimDisplayName;
  private static final int INDEX_SIM1 = 0;
  private static final int INDEX_SIM2 = 1;
  private static final int CARRIER_NAME_LENGTH = 4;
  private static final int TEXT_SIZE = 12;
  private boolean mIsDoubleSim = false;
  private boolean mIsDualVoLTEActive = false;
  private HashMap<Integer,String> mCarrierName = new HashMap<Integer,String>();
  List<SubscriptionInfo> mSubscriptions = null;
  private TelecomManager mTelecomManager;
  private boolean mIsFirstSimIncall;
  private boolean mIsSecSimIncall;
  private SimStateChangedReceiver mSimStateChangedReceiver;
  /* @} */
  /* SPRD: Add for VoLTE@{*/
  private boolean mIsVideoEnable;
  private boolean mIsImsListenerRegistered;
  private IImsServiceEx mIImsServiceEx;
  private PopupMenu mPopupMenu;
  private static final int MENU_MAKE_VIDEO_CALL  = 100;
  private static final int MENU_MAKE_MULTE_CALL = 101;
  public static final String ADD_MULTI_CALL = "addMultiCall";
  private static final String MULTI_PICK_CONTACTS_ACTION = "com.android.contacts.action.MULTI_TAB_PICK";
  private static final int MAX_CONTACTS_NUMBER = 5;
  private static final int MIN_CONTACTS_NUMBER = 2;
  private static final int REQUEST_CODE_PICK = 99;
  public static final int RESULT_OK = -1;
  /* @} */

  /** SPRD: IP dialing feature for bug886817 @{ */
  private static final int MENU_IP_DIAL = 102;
  public static final String SUB_ID_EXTRA =
          "com.android.phone.settings.SubscriptionInfoHelper.SubscriptionId";
  private static final String PHONE_PACKAGE_NAME = "com.android.dialer";
  private static final String IP_NUMBER_LIST_ACTIVITY =
          "com.android.dialer.app.ipdial.IpNumberListActivity";
  /** @} */

  /** SPRD: bug902308  Add character limits in the search box @{*/
  private static final int SEARCH_INPUT_MAX = 100;
  /** @} */

  /**UNISOC: add for the bug998527 @{*/
  private static final int INPUT_MAX_LENGTH = 50;
  /** @} */

  //UNISOC:modify for bug 931236
  private boolean mIsSupportMultiCall;

  /**
   * Determines whether an add call operation is requested.
   *
   * @param intent The intent.
   * @return {@literal true} if add call operation was requested. {@literal false} otherwise.
   */
  public static boolean isAddCallMode(Intent intent) {
    if (intent == null) {
      return false;
    }
    final String action = intent.getAction();
    if (Intent.ACTION_DIAL.equals(action) || Intent.ACTION_VIEW.equals(action)) {
      // see if we are "adding a call" from the InCallScreen; false by default.
      return intent.getBooleanExtra(ADD_CALL_MODE_KEY, false);
    } else {
      return false;
    }
  }

  /**
   * Format the provided string of digits into one that represents a properly formatted phone
   * number.
   *
   * @param dialString String of characters to format
   * @param normalizedNumber the E164 format number whose country code is used if the given
   *     phoneNumber doesn't have the country code.
   * @param countryIso The country code representing the format to use if the provided normalized
   *     number is null or invalid.
   * @return the provided string of digits as a formatted phone number, retaining any post-dial
   *     portion of the string.
   */
  String getFormattedDigits(String dialString, String normalizedNumber, String countryIso) {
    String number = PhoneNumberUtils.extractNetworkPortion(dialString);
    // Also retrieve the post dial portion of the provided data, so that the entire dial
    // string can be reconstituted later.
    final String postDial = PhoneNumberUtils.extractPostDialPortion(dialString);

    if (TextUtils.isEmpty(number)) {
      return postDial;
    }

    number = PhoneNumberHelper.formatNumber(getContext(), number, normalizedNumber, countryIso);

    if (TextUtils.isEmpty(postDial)) {
      return number;
    }

    return number.concat(postDial);
  }

  /**
   * Returns true of the newDigit parameter can be added at the current selection point, otherwise
   * returns false. Only prevents input of WAIT and PAUSE digits at an unsupported position. Fails
   * early if start == -1 or start is larger than end.
   */
  @VisibleForTesting
  /* package */ static boolean canAddDigit(CharSequence digits, int start, int end, char newDigit) {
    if (newDigit != WAIT && newDigit != PAUSE) {
      throw new IllegalArgumentException(
          "Should not be called for anything other than PAUSE & WAIT");
    }

    // False if no selection, or selection is reversed (end < start)
    if (start == -1 || end < start) {
      return false;
    }

    // unsupported selection-out-of-bounds state
    if (start > digits.length() || end > digits.length()) {
      return false;
    }

    // Special digit cannot be the first digit
    if (start == 0) {
      return false;
    }

    if (newDigit == WAIT) {
      // preceding char is ';' (WAIT)
      if (digits.charAt(start - 1) == WAIT) {
        return false;
      }

      // next char is ';' (WAIT)
      if ((digits.length() > end) && (digits.charAt(end) == WAIT)) {
        return false;
      }
    }

    return true;
  }

  private TelephonyManager getTelephonyManager() {
    return (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
  }

  @Override
  public Context getContext() {
    return getActivity();
  }

  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    wasEmptyBeforeTextChange = TextUtils.isEmpty(s);
  }

  @Override
  public void onTextChanged(CharSequence input, int start, int before, int changeCount) {
    /** SPRD: bug902308 & 987889 Add character limits in the search box @{*/
      if (input.length() > SEARCH_INPUT_MAX) {
        Log.d(TAG,"the length of input phonenumber is: " + input.length());
        Toast.makeText(getContext(),R.string.beyond_query_character_limit,Toast.LENGTH_SHORT).show();
        input = input.toString().substring(0,SEARCH_INPUT_MAX);
      }
    /** @} */

    if (wasEmptyBeforeTextChange != TextUtils.isEmpty(input)) {
      final Activity activity = getActivity();
      if (activity != null) {
        activity.invalidateOptionsMenu();
        updateMenuOverflowButton(wasEmptyBeforeTextChange);
      }
    }

    // DTMF Tones do not need to be played here any longer -
    // the DTMF dialer handles that functionality now.
  }

  @Override
  public void afterTextChanged(Editable input) {
    /** SPRD: bug902308 Add character limits in the search box @{*/
    if (input.length() > SEARCH_INPUT_MAX) {
      input.delete(SEARCH_INPUT_MAX,input.length());
    }
    /** @} */

    // When DTMF dialpad buttons are being pressed, we delay SpecialCharSequenceMgr sequence,
    // since some of SpecialCharSequenceMgr's behavior is too abrupt for the "touch-down"
    // behavior.
    // SPRD: modify for bug709722
    if (!digitsFilledByIntent && getActivity() != null
        && SpecialCharSequenceMgr.handleChars(getActivity(), input.toString(), digits)) {
      // A special sequence was entered, clear the digits
      digits.getText().clear();
    }

    if (isDigitsEmpty()) {
      digitsFilledByIntent = false;
      digits.setCursorVisible(false);
    }

    if (dialpadQueryListener != null) {
      dialpadQueryListener.onDialpadQueryChanged(digits.getText().toString());
    }

    updateDeleteButtonEnabledState();
  }

  @Override
  public void onCreate(Bundle state) {
    Trace.beginSection(TAG + " onCreate");
    LogUtil.enterBlock("DialpadFragment.onCreate");
    super.onCreate(state);

    firstLaunch = state == null;

    prohibitedPhoneNumberRegexp =
        getResources().getString(R.string.config_prohibited_phone_number_regexp);

    if (state != null) {
      digitsFilledByIntent = state.getBoolean(PREF_DIGITS_FILLED_BY_INTENT);
      isDialpadSlideUp = state.getBoolean(PREF_IS_DIALPAD_SLIDE_OUT);
    }

    dialpadSlideInDuration = getResources().getInteger(R.integer.dialpad_slide_in_duration);

    if (callStateReceiver == null) {
      IntentFilter callStateIntentFilter =
          new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
      callStateReceiver = new CallStateReceiver();
      getActivity().registerReceiver(callStateReceiver, callStateIntentFilter);
    }

    initPhoneNumberFormattingTextWatcherExecutor =
        DialerExecutorComponent.get(getContext())
            .dialerExecutorFactory()
            .createUiTaskBuilder(
                getFragmentManager(),
                "DialpadFragment.initPhoneNumberFormattingTextWatcher",
                new InitPhoneNumberFormattingTextWatcherWorker())
            .onSuccess(watcher -> dialpadView.getDigits().addTextChangedListener(watcher))
            .build();
    /* SPRD: add for bug731278 & 761688 & 782352 @{ */
    mIsDualVoLTEActive = ImsManagerEx.isDualVoLTEActive();
    Log.d(TAG, "isDualVoLTEActive " + mIsDualVoLTEActive);
    final boolean hasReadPhoneStatePermission =
            PermissionsUtil.hasPermission(getActivity(), READ_PHONE_STATE);
    if (hasReadPhoneStatePermission && mIsDualVoLTEActive) {
      mSubscriptions = SubscriptionManager.from(
              getActivity()).getActiveSubscriptionInfoList();
      mTelecomManager =
              (TelecomManager) getActivity().getSystemService(Context.TELECOM_SERVICE);
      PhoneAccountHandle phoneAccountHandle = TelecomUtil.getDefaultOutgoingPhoneAccount(
              getActivity(), PhoneAccount.SCHEME_TEL);
      if ((mSubscriptions != null) && (mSubscriptions.size() > 1)
              && (phoneAccountHandle == null)) {
        mIsDoubleSim = true;
        mIsFirstSimIncall = false;
        mIsSecSimIncall = false;
        CallList.getInstance().addListener(this);
        IntentFilter intentFilter
                = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        mSimStateChangedReceiver = new SimStateChangedReceiver();
        getActivity().registerReceiver(mSimStateChangedReceiver, intentFilter);
      }
    }
    /* @} */
    tryRegisterImsListener();//SRPD: Add for VoLTE
    Trace.endSection();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
    Trace.beginSection(TAG + " onCreateView");
    LogUtil.enterBlock("DialpadFragment.onCreateView");
    Trace.beginSection(TAG + " inflate view");
    /* SPRD: add for bug731278 @{ */
    View fragmentRView = inflater.inflate(R.layout.dialpad_fragment, container,
            false);
    if (mIsDoubleSim) {
      fragmentRView = inflater.inflate(R.layout.dialpad_fragment_double_sim,
              container, false);
      for (int i = 0; i < mSubscriptions.size(); i++) {
        int phoneId = mSubscriptions.get(i).getSimSlotIndex();
        String carrierName = mSubscriptions.get(i).getDisplayName().toString();
        mCarrierName.put(phoneId, carrierName);
      }
    }
    final View fragmentView = fragmentRView;
    /* @} */
    Trace.endSection();
    Trace.beginSection(TAG + " buildLayer");
    fragmentView.buildLayer();
    Trace.endSection();

    Trace.beginSection(TAG + " setup views");

    dialpadView = fragmentView.findViewById(R.id.dialpad_view);
    dialpadView.setCanDigitsBeEdited(true);
    digits = dialpadView.getDigits();
    digits.setKeyListener(UnicodeDialerKeyListener.INSTANCE);
    digits.setOnClickListener(this);
    digits.setOnKeyListener(this);
    digits.setOnLongClickListener(this);
    digits.addTextChangedListener(this);
    digits.setElegantTextHeight(false);
    /**UNISOC: add for the bug998527 @{*/
    digits.setFilters(new InputFilter[] { new InputFilter.LengthFilter(
            INPUT_MAX_LENGTH) });
    /** @}*/

    initPhoneNumberFormattingTextWatcherExecutor.executeSerial(getCurrentCountryIso());

    // Check for the presence of the keypad
    View oneButton = fragmentView.findViewById(R.id.one);
    if (oneButton != null) {
      configureKeypadListeners(fragmentView);
    }

    delete = dialpadView.getDeleteButton();

    if (delete != null) {
      delete.setOnClickListener(this);
      delete.setOnLongClickListener(this);
    }

    fragmentView
        .findViewById(R.id.spacer)
        .setOnTouchListener(
            (v, event) -> {
              if (isDigitsEmpty()) {
                if (getActivity() != null) {
                  LogUtil.i("DialpadFragment.onCreateView", "dialpad spacer touched");
                  return FragmentUtils.getParentUnsafe(this, HostInterface.class)
                      .onDialpadSpacerTouchWithEmptyQuery();
                }
                return true;
              }
              return false;
            });

    digits.setCursorVisible(false);

    // Set up the "dialpad chooser" UI; see showDialpadChooser().
    dialpadChooser = fragmentView.findViewById(R.id.dialpadChooser);
    dialpadChooser.setOnItemClickListener(this);

    /* SPRD: add for bug731278 @{ */
    if (mIsDoubleSim) {
      final View floatingActionButtonContainer =
              fragmentView.findViewById(R.id.dialpad_floating_action_button_container);
      final ImageButton floatingActionButton =
              (ImageButton) fragmentView.findViewById(R.id.dialpad_floating_action_button);
      floatingActionButton.setOnClickListener(this);
      mFloatingActionFirstButtonController =
              new FloatingActionButtonContainerController(
                      getActivity(), floatingActionButtonContainer, floatingActionButton);
      final View floatingActionButtonSecondaryContainer =
              fragmentView.findViewById(
                      R.id.dialpad_floating_action_button_secondary_container);
      final ImageButton floatingActionSecondaryButton =
              (ImageButton) fragmentView.findViewById(
                      R.id.dialpad_floating_action_secondary_button);
      floatingActionSecondaryButton.setOnClickListener(this);
      mFloatingActionButtonSencondaryController
              = new FloatingActionButtonContainerController(getActivity(),
              floatingActionButtonSecondaryContainer,
              floatingActionSecondaryButton);
      mFirstSimIconView =
              (ImageView) fragmentView.findViewById(R.id.dialpad_floating_action_icon);
      mFirstSimDisplayName =
              (TextView) fragmentView.findViewById(R.id.dialpad_floating_action_name);
      mSecSimIconView = (ImageView) fragmentView.findViewById(
              R.id.dialpad_floating_action_secondary_icon);
      mSecSimDisplayName = (TextView) fragmentView.findViewById(
              R.id.dialpad_floating_action_secondary_name);
      String firstSimDisplayName = mCarrierName.get(INDEX_SIM1);
      String secSimDisplayName = mCarrierName.get(INDEX_SIM2);
      mFirstSimDisplayName.setText(firstSimDisplayName == null ? "" : firstSimDisplayName);
      mSecSimDisplayName.setText(secSimDisplayName == null ? "" : secSimDisplayName);
      if (firstSimDisplayName.length() > CARRIER_NAME_LENGTH) {
        mFirstSimDisplayName.setTextSize(TEXT_SIZE);
      }
      if (secSimDisplayName.length() > CARRIER_NAME_LENGTH) {
        mSecSimDisplayName.setTextSize(TEXT_SIZE);
      }
    } else {
      FloatingActionButton floatingActionButton =
              (FloatingActionButton) fragmentView.findViewById(R.id.dialpad_floating_action_button);
      floatingActionButton.setOnClickListener(this);
      floatingActionButtonController =
              new FloatingActionButtonController(getActivity(), floatingActionButton);
    }
    /* @} */
    Trace.endSection();
    Trace.endSection();
    return fragmentView;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    isLayoutRtl = ViewUtil.isRtl();
    isLandscape =
        getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
  }

  private String getCurrentCountryIso() {
    if (currentCountryIsoForTesting.isPresent()) {
      return currentCountryIsoForTesting.get();
    }

    return GeoUtil.getCurrentCountryIso(getActivity());
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  public static void setCurrentCountryIsoForTesting(String countryCode) {
    currentCountryIsoForTesting = Optional.of(countryCode);
  }

  private boolean isLayoutReady() {
    return digits != null;
  }

  public EditText getDigitsWidget() {
    return digits;
  }

  /** @return true when {@link #digits} is actually filled by the Intent. */
  private boolean fillDigitsIfNecessary(Intent intent) {
    // Only fills digits from an intent if it is a new intent.
    // Otherwise falls back to the previously used number.
    if (!firstLaunch && !startedFromNewIntent) {
      return false;
    }

    final String action = intent.getAction();
    if (Intent.ACTION_DIAL.equals(action) || Intent.ACTION_VIEW.equals(action)) {
      Uri uri = intent.getData();
      if (uri != null) {
        if (PhoneAccount.SCHEME_TEL.equals(uri.getScheme())) {
          // Put the requested number into the input area
          String data = uri.getSchemeSpecificPart();
          // Remember it is filled via Intent.
          digitsFilledByIntent = true;
          final String converted =
              PhoneNumberUtils.convertKeypadLettersToDigits(
                  PhoneNumberUtils.replaceUnicodeDigits(data));
          setFormattedDigits(converted, null);
          return true;
        } else {
          if (!PermissionsUtil.hasContactsReadPermissions(getActivity())) {
            return false;
          }
          String type = intent.getType();
          if (People.CONTENT_ITEM_TYPE.equals(type) || Phones.CONTENT_ITEM_TYPE.equals(type)) {
            // Query the phone number
            Cursor c =
                getActivity()
                    .getContentResolver()
                    .query(
                        intent.getData(),
                        new String[] {PhonesColumns.NUMBER, PhonesColumns.NUMBER_KEY},
                        null,
                        null,
                        null);
            if (c != null) {
              try {
                if (c.moveToFirst()) {
                  // Remember it is filled via Intent.
                  digitsFilledByIntent = true;
                  // Put the number into the input area
                  setFormattedDigits(c.getString(0), c.getString(1));
                  return true;
                }
              } finally {
                c.close();
              }
            }
          }
        }
      }
    }
    return false;
  }

  /**
   * Checks the given Intent and changes dialpad's UI state.
   *
   * <p>There are three modes:
   *
   * <ul>
   *   <li>Empty Dialpad shown via "Add Call" in the in call ui
   *   <li>Dialpad (digits filled), shown by {@link Intent#ACTION_DIAL} with a number.
   *   <li>Return to call view, shown when a call is ongoing without {@link Intent#ACTION_DIAL}
   * </ul>
   *
   * For example, if the user...
   *
   * <ul>
   *   <li>clicks a number in gmail, this method will show the dialpad filled with the number,
   *       regardless of whether a call is ongoing.
   *   <li>places a call, presses home and opens dialer, this method will show the return to call
   *       prompt to confirm what they want to do.
   * </ul>
   */
  private void configureScreenFromIntent(@NonNull Intent intent) {
    LogUtil.i("DialpadFragment.configureScreenFromIntent", "action: %s", intent.getAction());
    if (!isLayoutReady()) {
      // This happens typically when parent's Activity#onNewIntent() is called while
      // Fragment#onCreateView() isn't called yet, and thus we cannot configure Views at
      // this point. onViewCreate() should call this method after preparing layouts, so
      // just ignore this call now.
      LogUtil.i(
          "DialpadFragment.configureScreenFromIntent",
          "Screen configuration is requested before onCreateView() is called. Ignored");
      return;
    }

    // If "Add call" was selected, show the dialpad instead of the dialpad chooser prompt
    if (isAddCallMode(intent)) {
      LogUtil.i("DialpadFragment.configureScreenFromIntent", "Add call mode");
      showDialpadChooser(false);
      setStartedFromNewIntent(true);
      return;
    }

    // Don't show the chooser when called via onNewIntent() and phone number is present.
    // i.e. User clicks a telephone link from gmail for example.
    // In this case, we want to show the dialpad with the phone number.
    boolean digitsFilled = fillDigitsIfNecessary(intent);
    if (!(startedFromNewIntent && digitsFilled) && isPhoneInUse()) {
      // If there's already an active call, bring up an intermediate UI to
      // make the user confirm what they really want to do.
      LogUtil.i("DialpadFragment.configureScreenFromIntent", "Dialpad chooser mode");
      showDialpadChooser(true);
      setStartedFromNewIntent(false);
      return;
    }

    LogUtil.i("DialpadFragment.configureScreenFromIntent", "Nothing to show");
    showDialpadChooser(false);
    setStartedFromNewIntent(false);
  }

  public void setStartedFromNewIntent(boolean value) {
    startedFromNewIntent = value;
  }

  public void clearCallRateInformation() {
    setCallRateInformation(null, null);
  }

  public void setCallRateInformation(String countryName, String displayRate) {
    dialpadView.setCallRateInformation(countryName, displayRate);
  }

  /** Sets formatted digits to digits field. */
  private void setFormattedDigits(String data, String normalizedNumber) {
    final String formatted = getFormattedDigits(data, normalizedNumber, getCurrentCountryIso());
    if (!TextUtils.isEmpty(formatted)) {
      Editable digits = this.digits.getText();
      digits.replace(0, digits.length(), formatted);
      // for some reason this isn't getting called in the digits.replace call above..
      // but in any case, this will make sure the background drawable looks right
      afterTextChanged(digits);
    }
  }

  private void configureKeypadListeners(View fragmentView) {
    final int[] buttonIds =
        new int[] {
          R.id.one,
          R.id.two,
          R.id.three,
          R.id.four,
          R.id.five,
          R.id.six,
          R.id.seven,
          R.id.eight,
          R.id.nine,
          R.id.star,
          R.id.zero,
          R.id.pound
        };

    DialpadKeyButton dialpadKey;

    for (int buttonId : buttonIds) {
      dialpadKey = fragmentView.findViewById(buttonId);
      dialpadKey.setOnPressedListener(this);
      /* SPRD: Bug#694810, FAST DIAL feature @{ */
      dialpadKey.setOnLongClickListener(this);
      /* @} */
      /* SPRD: FEATURE_WAIT_PAUSE_ON_LONG_CLICK @{ */
      if (buttonId == R.id.pound) {
          dialpadKey.setOnClickListener(this);
        }
      /* @} */
    }

    // Long-pressing one button will initiate Voicemail.
    final DialpadKeyButton one = fragmentView.findViewById(R.id.one);
    one.setOnLongClickListener(this);

    // Long-pressing zero button will enter '+' instead.
    final DialpadKeyButton zero = fragmentView.findViewById(R.id.zero);
    zero.setOnLongClickListener(this);
  }

  @Override
  public void onStart() {
    LogUtil.i("DialpadFragment.onStart", "first launch: %b", firstLaunch);
    Trace.beginSection(TAG + " onStart");
    super.onStart();
    // if the mToneGenerator creation fails, just continue without it.  It is
    // a local audio signal, and is not as important as the dtmf tone itself.
    final long start = System.currentTimeMillis();
    synchronized (toneGeneratorLock) {
      if (toneGenerator == null) {
        try {
          toneGenerator = new ToneGenerator(DIAL_TONE_STREAM_TYPE, TONE_RELATIVE_VOLUME);
        } catch (RuntimeException e) {
          LogUtil.e(
              "DialpadFragment.onStart",
              "Exception caught while creating local tone generator: " + e);
          toneGenerator = null;
        }
      }
    }
    final long total = System.currentTimeMillis() - start;
    if (total > 50) {
      LogUtil.i("DialpadFragment.onStart", "Time for ToneGenerator creation: " + total);
    }
    Trace.endSection();
  }

  @Override
  public void onResume() {
    LogUtil.enterBlock("DialpadFragment.onResume");
    Trace.beginSection(TAG + " onResume");
    super.onResume();

    Resources res = getResources();
    int iconId = R.drawable.quantum_ic_call_vd_theme_24;
    if (MotorolaUtils.isWifiCallingAvailable(getContext())) {
      iconId = R.drawable.ic_wifi_calling;
    }
    /* SPRD: add for bug731278 @{ */
    if (floatingActionButtonController != null) {
        floatingActionButtonController.changeIcon(
                getContext(), iconId, res.getString(R.string.description_dial_button));
    }
    /* @} */
    dialpadQueryListener = FragmentUtils.getParentUnsafe(this, OnDialpadQueryChangedListener.class);

    final StopWatch stopWatch = StopWatch.start("Dialpad.onResume");

    // Query the last dialed number. Do it first because hitting
    // the DB is 'slow'. This call is asynchronous.
    queryLastOutgoingCall();

    stopWatch.lap("qloc");

    final ContentResolver contentResolver = getActivity().getContentResolver();

    // retrieve the DTMF tone play back setting.
    dTMFToneEnabled =
        Settings.System.getInt(contentResolver, Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1;

    stopWatch.lap("dtwd");

    stopWatch.lap("hptc");

    pressedDialpadKeys.clear();

    configureScreenFromIntent(getActivity().getIntent());

    stopWatch.lap("fdin");

    if (!isPhoneInUse()) {
      LogUtil.i("DialpadFragment.onResume", "phone not in use");
      // A sanity-check: the "dialpad chooser" UI should not be visible if the phone is idle.
      showDialpadChooser(false);
    }

    stopWatch.lap("hnt");

    updateDeleteButtonEnabledState();

    stopWatch.lap("bes");

    stopWatch.stopAndLog(TAG, 50);

    // Populate the overflow menu in onResume instead of onCreate, so that if the SMS activity
    // is disabled while Dialer is paused, the "Send a text message" option can be correctly
    // removed when resumed.
    overflowMenuButton = dialpadView.getOverflowMenuButton();
    overflowPopupMenu = buildOptionsMenu(overflowMenuButton);
    overflowMenuButton.setOnTouchListener(overflowPopupMenu.getDragToOpenListener());
    overflowMenuButton.setOnClickListener(this);
    overflowMenuButton.setVisibility(isDigitsEmpty() ? View.INVISIBLE : View.VISIBLE);

    if (firstLaunch) {
      // The onHiddenChanged callback does not get called the first time the fragment is
      // attached, so call it ourselves here.
      onHiddenChanged(false);
    }

    firstLaunch = false;
    Trace.endSection();
  }

  @Override
  public void onPause() {
    super.onPause();

    /* SPRD: add for bug 892976 @{ */
    if (mPopupMenu != null) {
      mPopupMenu.dismiss();
    }
    /* @} */
    // Make sure we don't leave this activity with a tone still playing.
    stopTone();
    pressedDialpadKeys.clear();

    // TODO: I wonder if we should not check if the AsyncTask that
    // lookup the last dialed number has completed.
    lastNumberDialed = EMPTY_NUMBER; // Since we are going to query again, free stale number.

    SpecialCharSequenceMgr.cleanup();
    overflowPopupMenu.dismiss();
  }

  @Override
  public void onStop() {
    LogUtil.enterBlock("DialpadFragment.onStop");
    super.onStop();

    synchronized (toneGeneratorLock) {
      if (toneGenerator != null) {
        toneGenerator.release();
        toneGenerator = null;
      }
    }

    if (clearDigitsOnStop) {
      clearDigitsOnStop = false;
      clearDialpad();
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(PREF_DIGITS_FILLED_BY_INTENT, digitsFilledByIntent);
    outState.putBoolean(PREF_IS_DIALPAD_SLIDE_OUT, isDialpadSlideUp);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (pseudoEmergencyAnimator != null) {
      pseudoEmergencyAnimator.destroy();
      pseudoEmergencyAnimator = null;
    }
    getActivity().unregisterReceiver(callStateReceiver);
    /* SPRD: add for bug731278 @{ */
    if (mIsDoubleSim) {
      if (mSimStateChangedReceiver != null) {
        getActivity().unregisterReceiver(mSimStateChangedReceiver);
        mSimStateChangedReceiver = null;
      }
      CallList.getInstance().removeListener(this);
    }
    /* @} */
    /*SPRD: add for VoLTE 712380 {@*/
    if (getActivity() != null  && mIImsServiceEx != null && PermissionsUtil.hasPermission(getActivity(), READ_PHONE_STATE)) {
      try {
        if (mIsImsListenerRegistered) {
          mIsImsListenerRegistered = false;
          mIImsServiceEx.unregisterforImsRegisterStateChanged(mImsUtListenerExBinder);
        }
      } catch (RemoteException e) {
        LogUtil.e("DialpadFragment.onDestroy:", "e: " + e);
      }
    }
    /* @} */
  }

  private void keyPressed(int keyCode) {
    if (getView() == null || getView().getTranslationY() != 0) {
      return;
    }
    switch (keyCode) {
      case KeyEvent.KEYCODE_1:
        playTone(ToneGenerator.TONE_DTMF_1, TONE_LENGTH_INFINITE);
        break;
      case KeyEvent.KEYCODE_2:
        playTone(ToneGenerator.TONE_DTMF_2, TONE_LENGTH_INFINITE);
        break;
      case KeyEvent.KEYCODE_3:
        playTone(ToneGenerator.TONE_DTMF_3, TONE_LENGTH_INFINITE);
        break;
      case KeyEvent.KEYCODE_4:
        playTone(ToneGenerator.TONE_DTMF_4, TONE_LENGTH_INFINITE);
        break;
      case KeyEvent.KEYCODE_5:
        playTone(ToneGenerator.TONE_DTMF_5, TONE_LENGTH_INFINITE);
        break;
      case KeyEvent.KEYCODE_6:
        playTone(ToneGenerator.TONE_DTMF_6, TONE_LENGTH_INFINITE);
        break;
      case KeyEvent.KEYCODE_7:
        playTone(ToneGenerator.TONE_DTMF_7, TONE_LENGTH_INFINITE);
        break;
      case KeyEvent.KEYCODE_8:
        playTone(ToneGenerator.TONE_DTMF_8, TONE_LENGTH_INFINITE);
        break;
      case KeyEvent.KEYCODE_9:
        playTone(ToneGenerator.TONE_DTMF_9, TONE_LENGTH_INFINITE);
        break;
      case KeyEvent.KEYCODE_0:
        playTone(ToneGenerator.TONE_DTMF_0, TONE_LENGTH_INFINITE);
        break;
      case KeyEvent.KEYCODE_POUND:
        playTone(ToneGenerator.TONE_DTMF_P, TONE_LENGTH_INFINITE);
        break;
      case KeyEvent.KEYCODE_STAR:
        playTone(ToneGenerator.TONE_DTMF_S, TONE_LENGTH_INFINITE);
        break;
      default:
        break;
    }

    getView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
    digits.onKeyDown(keyCode, event);

    // If the cursor is at the end of the text we hide it.
    final int length = digits.length();
    if (length == digits.getSelectionStart() && length == digits.getSelectionEnd()) {
      digits.setCursorVisible(false);
    }
  }

  @Override
  public boolean onKey(View view, int keyCode, KeyEvent event) {
    if (view.getId() == R.id.digits) {
      if (keyCode == KeyEvent.KEYCODE_ENTER) {
        handleDialButtonPressed();
        return true;
      }
    }
    return false;
  }

  /**
   * When a key is pressed, we start playing DTMF tone, do vibration, and enter the digit
   * immediately. When a key is released, we stop the tone. Note that the "key press" event will be
   * delivered by the system with certain amount of delay, it won't be synced with user's actual
   * "touch-down" behavior.
   */
  @Override
  public void onPressed(View view, boolean pressed) {
    if (pressed) {
      int resId = view.getId();
      if (resId == R.id.one) {
        keyPressed(KeyEvent.KEYCODE_1);
      } else if (resId == R.id.two) {
        keyPressed(KeyEvent.KEYCODE_2);
      } else if (resId == R.id.three) {
        keyPressed(KeyEvent.KEYCODE_3);
      } else if (resId == R.id.four) {
        keyPressed(KeyEvent.KEYCODE_4);
      } else if (resId == R.id.five) {
        keyPressed(KeyEvent.KEYCODE_5);
      } else if (resId == R.id.six) {
        keyPressed(KeyEvent.KEYCODE_6);
      } else if (resId == R.id.seven) {
        keyPressed(KeyEvent.KEYCODE_7);
      } else if (resId == R.id.eight) {
        keyPressed(KeyEvent.KEYCODE_8);
      } else if (resId == R.id.nine) {
        keyPressed(KeyEvent.KEYCODE_9);
      } else if (resId == R.id.zero) {
        keyPressed(KeyEvent.KEYCODE_0);
      } else if (resId == R.id.pound) {
        keyPressed(KeyEvent.KEYCODE_POUND);
      } else if (resId == R.id.star) {
        keyPressed(KeyEvent.KEYCODE_STAR);
      } else {
        LogUtil.e(
            "DialpadFragment.onPressed", "Unexpected onTouch(ACTION_DOWN) event from: " + view);
      }
      pressedDialpadKeys.add(view);
    } else {
      pressedDialpadKeys.remove(view);
      if (pressedDialpadKeys.isEmpty()) {
        stopTone();
      }
    }
  }

  /**
   * Called by the containing Activity to tell this Fragment to build an overflow options menu for
   * display by the container when appropriate.
   *
   * @param invoker the View that invoked the options menu, to act as an anchor location.
   */
  private PopupMenu buildOptionsMenu(View invoker) {
      if(mPopupMenu == null) {
          mPopupMenu = new PopupMenu(getActivity(), invoker) {
                @Override
                public void show() {
                  final Menu menu = getMenu();

                  boolean enable = !isDigitsEmpty();
                  for (int i = 0; i < menu.size(); i++) {
                    MenuItem item = menu.getItem(i);
                    item.setEnabled(enable);
                    if (item.getItemId() == R.id.menu_call_with_note) {
                      item.setVisible(CallUtil.isCallWithSubjectSupported(getContext()));
                    }
                  }
                  super.show();
                }
              };
      }
      mPopupMenu.getMenu().clear();
      mPopupMenu.inflate(R.menu.dialpad_options);
      /* SPRD: bug#890160, check permission for Diapad @{ */
      final boolean hasReadReadPhoneStatePermission = PermissionsUtil.hasPermission(getActivity(), READ_PHONE_STATE);
      if (hasReadReadPhoneStatePermission) {
          //Modify for 3GVT for bug576512@{
          mPopupMenu.getMenu().add(0, MENU_MAKE_VIDEO_CALL, 0, R.string.make_video_call_menu);
          mPopupMenu.getMenu()
              .findItem(MENU_MAKE_VIDEO_CALL)
             /** SPRD: bug895158 Card One Unicom Card II moves, the model closes the “Video Call” function,
              * the dial pad enters the number, and the option still has the “Start Video Call” option.@{*/
              .setVisible(CallUtil.isVideoEnabled(getActivity()));
             /** @} */
          /**UNISOC:modify for bug 931236 @{*/
          CarrierConfigManager configManager =
                  (CarrierConfigManager) getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
          int primeSubId = SubscriptionManager.getDefaultDataSubscriptionId();
          if (configManager.getConfigForDefaultPhone() != null) {
              mIsSupportMultiCall = configManager.getConfigForSubId(primeSubId).getBoolean(
              CarrierConfigManagerEx.KEY_CARRIER_SUPPORTS_MULTI_CALL);
              Log.d("DialPadFragment", "mIsSupportMultiCall" + mIsSupportMultiCall);
          }
          mPopupMenu.getMenu().add(0, MENU_MAKE_MULTE_CALL, 0,
                  R.string.dialpad_multi_call_menu);
          mPopupMenu.getMenu().findItem(MENU_MAKE_MULTE_CALL).setVisible(mIsVideoEnable && mIsSupportMultiCall);
          /**@}*/
          /** SPRD: IP dialing feature for bug886817 @{ */
          /**UNISOC:970074,983956 cmcc,cucc show IP feature  @{*/
          boolean shouldShowIP = getResources().getBoolean(com.android.internal.R.bool.ip_dial_enabled_bool);
          Log.d(TAG,"shouldShowIP:"+shouldShowIP);
          if (shouldShowIP) {
             mPopupMenu.getMenu().add(0, MENU_IP_DIAL, 0, R.string.ip_dial_menu);
             mPopupMenu.setOnMenuItemClickListener(this);
          }
          /** @}*/
      }
      /*@}*/

      mPopupMenu.setOnMenuItemClickListener(this);
      return mPopupMenu;
  }

  @Override
  public void onClick(View view) {
    int resId = view.getId();
    if (resId == R.id.dialpad_floating_action_button) {
      view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
      /* SPRD: add for bug731278 & 782352 & 897080 @{ */
      if (mIsDualVoLTEActive) {
          if (mIsSecSimIncall && !ImsManagerEx.isReadyForDualActiveCall()) {
              Toast.makeText(getActivity(),
                      R.string.toast_not_dual_volte_regist, Toast.LENGTH_SHORT).show();
              return;
          }
          handleDialButtonPressed(INDEX_SIM1);
        } else {
          handleDialButtonPressed();
        }
    } else if (resId == R.id.dialpad_floating_action_secondary_button) {
      view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
      if (mIsFirstSimIncall && !ImsManagerEx.isReadyForDualActiveCall()) {
          Toast.makeText(getActivity(),
                  R.string.toast_not_dual_volte_regist, Toast.LENGTH_SHORT).show();
          return;
      }
      handleDialButtonPressed(INDEX_SIM2);
      /* @} */
    } else if (resId == R.id.deleteButton) {
      keyPressed(KeyEvent.KEYCODE_DEL);
    } else if (resId == R.id.digits) {
      if (!isDigitsEmpty()) {
        digits.setCursorVisible(true);
      }
    } else if (resId == R.id.dialpad_overflow) {
      overflowPopupMenu.show();
      /* SPRD: FEATURE_WAIT_PAUSE_ON_LONG_CLICK @{ */
    } else if (resId == R.id.pound) {
      if (!digitsFilledByIntent && getActivity() != null && digits != null
           && digits.getText() != null) {
          SpecialCharSequenceMgr.handleAdnEntry(getActivity(), digits.getText()
              .toString(), digits);
      }
      /* @} */
    } else {
      LogUtil.w("DialpadFragment.onClick", "Unexpected event from: " + view);
    }
  }

  @Override
  public boolean onLongClick(View view) {
    final Editable digits = this.digits.getText();
    final int id = view.getId();
    if (id == R.id.deleteButton) {
      digits.clear();
      return true;
    } else if (id == R.id.one) {
      /**
       * SPRD: bug892901 Porting AOB for calling voice mail on Airplane mode @{
       * First judge airplane, if airplane mode on , just return.
       * Voice mail is unavailable maybe because Airplane mode is turned on.
       * Check the current status and show the most appropriate error message.
       */
      if (getActivity() != null) {
        final boolean isAirplaneModeOn =
                Settings.System.getInt(getActivity().getContentResolver(),
                        Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        TelephonyManager telephonyManager = (TelephonyManager)
                getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        boolean isImsRegistered = telephonyManager.isImsRegistered();
        Log.d(TAG, "onLongClick: long click one, isAirplaneModeOn =" + isAirplaneModeOn
                + " isImsRegistered =" + isImsRegistered);
        // UNISOC: add for bug915593
        if (isAirplaneModeOn && !mIsVideoEnable) {
          Toast.makeText(getActivity(), R.string.dialog_voicemail_airplane_mode_message,
                  Toast.LENGTH_LONG).show();
          return true;
        }
      }
      /** @} */

      if (isDigitsEmpty() || TextUtils.equals(this.digits.getText(), "1")) {
        // We'll try to initiate voicemail and thus we want to remove irrelevant string.
        removePreviousDigitIfPossible('1');

        List<PhoneAccountHandle> subscriptionAccountHandles =
            TelecomUtil.getSubscriptionPhoneAccounts(getActivity());
        boolean hasUserSelectedDefault =
            subscriptionAccountHandles.contains(
                TelecomUtil.getDefaultOutgoingPhoneAccount(
                    getActivity(), PhoneAccount.SCHEME_VOICEMAIL));
        // unisoc: modify for bug920980
        boolean needsAccountDisambiguation =
            subscriptionAccountHandles.size() > 1 && !hasUserSelectedDefault && !isPhoneInUse();

        if (needsAccountDisambiguation || isVoicemailAvailable()) {
          // On a multi-SIM phone, if the user has not selected a default
          // subscription, initiate a call to voicemail so they can select an account
          // from the "Call with" dialog.
          callVoicemail();
        } else if (getActivity() != null) {
          // Voicemail is unavailable maybe because Airplane mode is turned on.
          // Check the current status and show the most appropprivate boolean isVoicemailAvailable() {riate error message.
          /* UNISOC: add for FEATURE bug897077 @{ */
          DialogFragment dialogFragment =
                  ErrorDialogFragment.newInstance(R.string.dialog_voicemail_not_ready_message);
          dialogFragment.show(getFragmentManager(), "voicemail_not_ready");
          /* @} */
        }
        return true;
      }
      return false;
    } else if (id == R.id.zero) {
      if (pressedDialpadKeys.contains(view)) {
        // If the zero key is currently pressed, then the long press occurred by touch
        // (and not via other means like certain accessibility input methods).
        // Remove the '0' that was input when the key was first pressed.
        removePreviousDigitIfPossible('0');
      }
      keyPressed(KeyEvent.KEYCODE_PLUS);
      stopTone();
      pressedDialpadKeys.remove(view);
      return true;
    } else if (id == R.id.digits) {
      this.digits.setCursorVisible(true);
      return false;
      /* SPRD: FEATURE_WAIT_PAUSE_ON_LONG_CLICK @{ */
    } else if (id == R.id.star) {
      if (this.digits.getSelectionStart() > 1) {
        // Remove tentative input ('*') done by onTouch().
        removePreviousDigitIfPossible('*');
        keyPressed(KeyEvent.KEYCODE_COMMA);
        // Stop tone immediately
        stopTone();
        pressedDialpadKeys.remove(view);
      }
      return true;
    } else if (id == R.id.pound) {
      if (this.digits.getSelectionStart() > 1) {
        // Remove tentative input ('#') done by onTouch().
        removePreviousDigitIfPossibleForPound();
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        updateDialString(WAIT);

        // Stop tone immediately
        stopTone();
        pressedDialpadKeys.remove(view);
      }
      return true;
     /* @} */
    } else {
        /* UNISOC: FAST DIAL FEATURE @{ */
        /* SPRD:BUG 896033 PreCall for start fastdial @{*/
        String fastPhoneNumber = FastDialUtils.getFastDialPhoneNumber(this, digits, id);
        if (fastPhoneNumber != null) {
          /** UNISOC:BUG 951075 DUT always ask for SIM (SIM 1 & SIM 2) to make call while making
            * emergency call from fast dial. @{*/
          if (!PhoneNumberUtils.isEmergencyNumber(fastPhoneNumber)) {
            PreCall.start(
                    getContext(), new CallIntentBuilder(fastPhoneNumber, CallInitiationType.Type.DIALPAD));
          } else {
            final Intent intent =
                    new CallIntentBuilder(fastPhoneNumber, CallInitiationType.Type.DIALPAD).build();
            DialerUtils.startActivityWithErrorToast(getActivity(), intent);
          }
          /** @} */
          digits.clear();
          return true;
        }
        /* @} */
        /* @} */
    }
    return false;
  }

  /**
   * Remove the digit just before the current position of the cursor, iff the following conditions
   * are true: 1) The cursor is not positioned at index 0. 2) The digit before the current cursor
   * position matches the current digit.
   *
   * @param digit to remove from the digits view.
   */
  private void removePreviousDigitIfPossible(char digit) {
    final int currentPosition = digits.getSelectionStart();
    if (currentPosition > 0 && digit == digits.getText().charAt(currentPosition - 1)) {
      digits.setSelection(currentPosition);
      digits.getText().delete(currentPosition - 1, currentPosition);
    }
  }

  public void callVoicemail() {
    PreCall.start(
        getContext(), CallIntentBuilder.forVoicemail(null, CallInitiationType.Type.DIALPAD));
    hideAndClearDialpad();
  }

  private void hideAndClearDialpad() {
    LogUtil.enterBlock("DialpadFragment.hideAndClearDialpad");
    FragmentUtils.getParentUnsafe(this, DialpadListener.class).onCallPlacedFromDialpad();
  }

  /**
   * In most cases, when the dial button is pressed, there is a number in digits area. Pack it in
   * the intent, start the outgoing call broadcast as a separate task and finish this activity.
   *
   * <p>When there is no digit and the phone is CDMA and off hook, we're sending a blank flash for
   * CDMA. CDMA networks use Flash messages when special processing needs to be done, mainly for
   * 3-way or call waiting scenarios. Presumably, here we're in a special 3-way scenario where the
   * network needs a blank flash before being able to add the new participant. (This is not the case
   * with all 3-way calls, just certain CDMA infrastructures.)
   *
   * <p>Otherwise, there is no digit, display the last dialed number. Don't finish since the user
   * may want to edit it. The user needs to press the dial button again, to dial it (general case
   * described above).
   */
  private void handleDialButtonPressed() {
    if (isDigitsEmpty()) { // No number entered.
      // No real call made, so treat it as a click
      PerformanceReport.recordClick(UiAction.Type.PRESS_CALL_BUTTON_WITHOUT_CALLING);
      handleDialButtonClickWithEmptyDigits();
    } else {
      final String number = digits.getText().toString();

      // "persist.radio.otaspdial" is a temporary hack needed for one carrier's automated
      // test equipment.
      // TODO: clean it up.
      if (number != null
          && !TextUtils.isEmpty(prohibitedPhoneNumberRegexp)
          && number.matches(prohibitedPhoneNumberRegexp)) {
        PerformanceReport.recordClick(UiAction.Type.PRESS_CALL_BUTTON_WITHOUT_CALLING);
        LogUtil.i(
            "DialpadFragment.handleDialButtonPressed",
            "The phone number is prohibited explicitly by a rule.");
        if (getActivity() != null) {
          DialogFragment dialogFragment =
              ErrorDialogFragment.newInstance(R.string.dialog_phone_call_prohibited_message);
          dialogFragment.show(getFragmentManager(), "phone_prohibited_dialog");
        }

        // Clear the digits just in case.
        clearDialpad();
      } else {
        /** SPRD: add for bug 900020 add Toast if airplane mode is open
         * bug 915561 SharkLE9.0 prompts select SIM card to make outgoing call while dialing
         * Emergency number 112 in airplane mode.@{ */
        final boolean isAirplaneModeOn = Settings.System.getInt(
                getActivity().getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        //UNISOC: Add for bug915593
        if (isAirplaneModeOn && !mIsVideoEnable) {
          if (!PhoneNumberUtils.isEmergencyNumber(number)) {
            Toast.makeText(getActivity(), R.string.dialog_make_call_airplane_mode_message,
                    Toast.LENGTH_LONG).show();
          } else {
            final Intent intent = new CallIntentBuilder(number, CallInitiationType.Type.DIALPAD).build();
            DialerUtils.startActivityWithErrorToast(getActivity(), intent);
            hideAndClearDialpad();
          }
        } else {
          if (!PhoneNumberUtils.isEmergencyNumber(number)) {
            PreCall.start(getContext(), new CallIntentBuilder(number, CallInitiationType.Type.DIALPAD));
          } else {
            final Intent intent = new CallIntentBuilder(number, CallInitiationType.Type.DIALPAD).build();
            DialerUtils.startActivityWithErrorToast(getActivity(), intent);
          }
          hideAndClearDialpad();
        }
        /**@}*/
      }
    }
  }

  /* SPRD: add for bug731278 @{ */
  private void handleDialButtonPressed(int phoneId) {
    List<PhoneAccountHandle> subscriptionAccountHandles =
            PhoneAccountUtils.getSubscriptionPhoneAccounts(getActivity());
    PhoneAccountHandle account;
    int size = 0;
    if (subscriptionAccountHandles != null) {
      size = subscriptionAccountHandles.size();
    }
    Log.i(TAG, "handleDialButtonPressed->phoneId:" + phoneId + " size:" + size);
    if (subscriptionAccountHandles != null && size > 1 && phoneId < size && mIsDoubleSim) {
      account = subscriptionAccountHandles.get(phoneId);
    } else {
      handleDialButtonPressed();
      return;
    }
    if (isDigitsEmpty()) {
      handleDialButtonClickWithEmptyDigits();
    } else {
      final String number = digits.getText().toString();
      if (number != null
              && !TextUtils.isEmpty(prohibitedPhoneNumberRegexp)
              && number.matches(prohibitedPhoneNumberRegexp)) {
        Log.i(TAG, "The phone number is prohibited explicitly by a rule.");
        if (getActivity() != null) {
          DialogFragment dialogFragment = ErrorDialogFragment.newInstance(
                  R.string.dialog_phone_call_prohibited_message);
          dialogFragment.show(getFragmentManager(), "phone_prohibited_dialog");
        }
        clearDialpad();
      } else {
        final Intent intent = new CallIntentBuilder(number, CallInitiationType.Type.DIALPAD)
                .setPhoneAccountHandle(account)
                .build();
        DialerUtils.startActivityWithErrorToast(getActivity(), intent);
        clearDialpad();
      }
    }
  }
  /* @} */

  public void clearDialpad() {
    if (digits != null) {
      digits.getText().clear();
    }
  }

  private void handleDialButtonClickWithEmptyDigits() {
    if (phoneIsCdma() && isPhoneInUse()) {
      // TODO: Move this logic into services/Telephony
      //
      // This is really CDMA specific. On GSM is it possible
      // to be off hook and wanted to add a 3rd party using
      // the redial feature.
      startActivity(newFlashIntent());
    } else {
      if (!TextUtils.isEmpty(lastNumberDialed)) {
        // Dialpad will be filled with last called number,
        // but we don't want to record it as user action
        PerformanceReport.setIgnoreActionOnce(UiAction.Type.TEXT_CHANGE_WITH_INPUT);

        // Recall the last number dialed.
        digits.setText(lastNumberDialed);

        // ...and move the cursor to the end of the digits string,
        // so you'll be able to delete digits using the Delete
        // button (just as if you had typed the number manually.)
        //
        // Note we use mDigits.getText().length() here, not
        // mLastNumberDialed.length(), since the EditText widget now
        // contains a *formatted* version of mLastNumberDialed (due to
        // mTextWatcher) and its length may have changed.
        digits.setSelection(digits.getText().length());
      } else {
        // There's no "last number dialed" or the
        // background query is still running. There's
        // nothing useful for the Dial button to do in
        // this case.  Note: with a soft dial button, this
        // can never happens since the dial button is
        // disabled under these conditons.
        playTone(ToneGenerator.TONE_PROP_NACK);
      }
    }
  }

  /** Plays the specified tone for TONE_LENGTH_MS milliseconds. */
  private void playTone(int tone) {
    playTone(tone, TONE_LENGTH_MS);
  }

  /**
   * Play the specified tone for the specified milliseconds
   *
   * <p>The tone is played locally, using the audio stream for phone calls. Tones are played only if
   * the "Audible touch tones" user preference is checked, and are NOT played if the device is in
   * silent mode.
   *
   * <p>The tone length can be -1, meaning "keep playing the tone." If the caller does so, it should
   * call stopTone() afterward.
   *
   * @param tone a tone code from {@link ToneGenerator}
   * @param durationMs tone length.
   */
  private void playTone(int tone, int durationMs) {
    // if local tone playback is disabled, just return.
    if (!dTMFToneEnabled) {
      return;
    }

    // Also do nothing if the phone is in silent mode.
    // We need to re-check the ringer mode for *every* playTone()
    // call, rather than keeping a local flag that's updated in
    // onResume(), since it's possible to toggle silent mode without
    // leaving the current activity (via the ENDCALL-longpress menu.)
    AudioManager audioManager =
        (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
    int ringerMode = audioManager.getRingerMode();
    if ((ringerMode == AudioManager.RINGER_MODE_SILENT)
        || (ringerMode == AudioManager.RINGER_MODE_VIBRATE)) {
      return;
    }

    synchronized (toneGeneratorLock) {
      if (toneGenerator == null) {
        LogUtil.w("DialpadFragment.playTone", "mToneGenerator == null, tone: " + tone);
        return;
      }

      // Start the new tone (will stop any playing tone)
      toneGenerator.startTone(tone, durationMs);
    }
  }

  /** Stop the tone if it is played. */
  private void stopTone() {
    // if local tone playback is disabled, just return.
    if (!dTMFToneEnabled) {
      return;
    }
    synchronized (toneGeneratorLock) {
      if (toneGenerator == null) {
        LogUtil.w("DialpadFragment.stopTone", "mToneGenerator == null");
        return;
      }
      toneGenerator.stopTone();
    }
  }

  /**
   * Brings up the "dialpad chooser" UI in place of the usual Dialer elements (the textfield/button
   * and the dialpad underneath).
   *
   * <p>We show this UI if the user brings up the Dialer while a call is already in progress, since
   * there's a good chance we got here accidentally (and the user really wanted the in-call dialpad
   * instead). So in this situation we display an intermediate UI that lets the user explicitly
   * choose between the in-call dialpad ("Use touch tone keypad") and the regular Dialer ("Add
   * call"). (Or, the option "Return to call in progress" just goes back to the in-call UI with no
   * dialpad at all.)
   *
   * @param enabled If true, show the "dialpad chooser" instead of the regular Dialer UI
   */
  private void showDialpadChooser(boolean enabled) {
    if (getActivity() == null) {
      return;
    }
    // Check if onCreateView() is already called by checking one of View objects.
    if (!isLayoutReady()) {
      return;
    }

    if (enabled) {
      LogUtil.i("DialpadFragment.showDialpadChooser", "Showing dialpad chooser!");
      if (dialpadView != null) {
        dialpadView.setVisibility(View.GONE);
      }

      if (overflowPopupMenu != null) {
        overflowPopupMenu.dismiss();
      }

      /* SPRD: add for bug731278 @{ */
      if (floatingActionButtonController != null) {
        floatingActionButtonController.setVisible(false);
      }
      if (mFloatingActionFirstButtonController != null) {
        mFloatingActionFirstButtonController.setVisible(false);
      }
      if (mFloatingActionButtonSencondaryController != null) {
        mFloatingActionButtonSencondaryController.setVisible(false);
      }
      /* @} */
      dialpadChooser.setVisibility(View.VISIBLE);

      // Instantiate the DialpadChooserAdapter and hook it up to the
      // ListView.  We do this only once.
      if (dialpadChooserAdapter == null) {
        dialpadChooserAdapter = new DialpadChooserAdapter(getActivity());
      }
      dialpadChooser.setAdapter(dialpadChooserAdapter);
    } else {
      LogUtil.i("DialpadFragment.showDialpadChooser", "Displaying normal Dialer UI.");
      if (dialpadView != null) {
        LogUtil.i("DialpadFragment.showDialpadChooser", "mDialpadView not null");
        dialpadView.setVisibility(View.VISIBLE);
        /* SPRD: add for bug731278 @{ */
        if (floatingActionButtonController != null) {
            floatingActionButtonController.scaleIn();
        }
        /* @} */
      } else {
        LogUtil.i("DialpadFragment.showDialpadChooser", "mDialpadView null");
        digits.setVisibility(View.VISIBLE);
      }

      /* SPRD: add for bug731278 @{ */
      // mFloatingActionButtonController must also be 'scaled in', in order to be visible after
      // 'scaleOut()' hidden method.
      if (floatingActionButtonController != null && !floatingActionButtonController.isVisible()) {
          // Just call 'scaleIn()' method if the mFloatingActionButtonController was not already
          // previously visible.
          floatingActionButtonController.scaleIn();
        }
        if (mFloatingActionFirstButtonController != null
                && !mFloatingActionFirstButtonController.isVisible()) {
          mFloatingActionFirstButtonController.scaleIn(0);
        }
        if (mFloatingActionButtonSencondaryController != null
                && !mFloatingActionButtonSencondaryController.isVisible()) {
          mFloatingActionButtonSencondaryController.scaleIn(0);
        }
        /* @} */
      dialpadChooser.setVisibility(View.GONE);
    }
  }

  /** @return true if we're currently showing the "dialpad chooser" UI. */
  private boolean isDialpadChooserVisible() {
    return dialpadChooser.getVisibility() == View.VISIBLE;
  }

  /** Handle clicks from the dialpad chooser. */
  @Override
  public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
    DialpadChooserAdapter.ChoiceItem item =
        (DialpadChooserAdapter.ChoiceItem) parent.getItemAtPosition(position);
    int itemId = item.id;
    if (itemId == DialpadChooserAdapter.DIALPAD_CHOICE_USE_DTMF_DIALPAD) {
      // Fire off an intent to go back to the in-call UI
      // with the dialpad visible.
      returnToInCallScreen(true);
    } else if (itemId == DialpadChooserAdapter.DIALPAD_CHOICE_RETURN_TO_CALL) {
      // Fire off an intent to go back to the in-call UI
      // (with the dialpad hidden).
      returnToInCallScreen(false);
    } else if (itemId == DialpadChooserAdapter.DIALPAD_CHOICE_ADD_NEW_CALL) {
      // Ok, guess the user really did want to be here (in the
      // regular Dialer) after all.  Bring back the normal Dialer UI.
      showDialpadChooser(false);
    } else {
      LogUtil.w("DialpadFragment.onItemClick", "Unexpected itemId: " + itemId);
    }
  }

  /**
   * Returns to the in-call UI (where there's presumably a call in progress) in response to the user
   * selecting "use touch tone keypad" or "return to call" from the dialpad chooser.
   */
  private void returnToInCallScreen(boolean showDialpad) {
    TelecomUtil.showInCallScreen(getActivity(), showDialpad);

    // Finally, finish() ourselves so that we don't stay on the
    // activity stack.
    // Note that we do this whether or not the showCallScreenWithDialpad()
    // call above had any effect or not!  (That call is a no-op if the
    // phone is idle, which can happen if the current call ends while
    // the dialpad chooser is up.  In this case we can't show the
    // InCallScreen, and there's no point staying here in the Dialer,
    // so we just take the user back where he came from...)
    getActivity().finish();
  }

  /**
   * @return true if the phone is "in use", meaning that at least one line is active (ie. off hook
   *     or ringing or dialing, or on hold).
   */
  private boolean isPhoneInUse() {
    return getContext() != null
        && TelecomUtil.isInManagedCall(getContext())
        && FragmentUtils.getParentUnsafe(this, HostInterface.class).shouldShowDialpadChooser();
  }

  /** @return true if the phone is a CDMA phone type */
  private boolean phoneIsCdma() {
    return getTelephonyManager().getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA;
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    int resId = item.getItemId();
    if (resId == R.id.menu_2s_pause) {
      updateDialString(PAUSE);
      return true;
    } else if (resId == R.id.menu_add_wait) {
      updateDialString(WAIT);
      return true;
    } else if (resId == R.id.menu_call_with_note) {
      CallSubjectDialog.start(getActivity(), digits.getText().toString());
      hideAndClearDialpad();
      return true;
      /* SPRD: add for VoLTE@{*/
    } else if (resId == MENU_MAKE_VIDEO_CALL) {
        /* SPRD: add for bug634770 @{ */
      if (digits != null) {
          String number = digits.getText().toString();
          final Intent intent = new CallIntentBuilder(number, CallInitiationType.Type.DIALPAD).setIsVideoCall(true).build();
          if (getActivity() != null) {
              DialerUtils.startActivityWithErrorToast(getActivity(), intent);
              hideAndClearDialpad(); //SPRD: modify for bug791048
          }
        /* @} */
        }
      return true;
    } else if (resId == MENU_MAKE_MULTE_CALL) {
      if (!isPhoneInUse()) {
          if (getTelephonyManager().isWifiCallingAvailable()){// SPRD: add for bug764072
              Toast.makeText(getContext(), R.string.vowifi_conf_do_not_support, Toast.LENGTH_SHORT).show();
          } else {
              Intent intentPick = new Intent(MULTI_PICK_CONTACTS_ACTION).
              putExtra("checked_limit_count", MAX_CONTACTS_NUMBER).
              putExtra("checked_min_limit_count", MIN_CONTACTS_NUMBER).
              putExtra("cascading", new Intent(MULTI_PICK_CONTACTS_ACTION).setType(Phone.CONTENT_ITEM_TYPE)).
              putExtra("multi", ADD_MULTI_CALL);
              DialerUtils.startActivityWithErrorToast(getActivity(), intentPick);
          }
      } else {
           //add for SPRD:617749
           Toast.makeText(getContext(), R.string.cound_not_make_mutil_call, Toast.LENGTH_SHORT).show();
      }
       return true;
       /* @} */
    }else if (resId == MENU_IP_DIAL) {
      /** SPRD: IP dialing feature for bug886817 @{ */
      Context context = getActivity();
      if (digits != null && context != null) {
        String number = digits.getText().toString();
        /* SPRD: add for 900020 @{ */
        final boolean isAirplaneModeOn = Settings.System.getInt(context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        if (isAirplaneModeOn && !PhoneNumberUtils.isEmergencyNumber(number)) {
          Toast.makeText(context, R.string.dialog_make_call_airplane_mode_message,
                  Toast.LENGTH_LONG).show();
          return true;
        }
        /* @{ */
        List<PhoneAccountHandle> subscriptionAccountHandles =
                PhoneAccountUtils.getSubscriptionPhoneAccounts(context);
        PhoneAccountHandle defaultPhoneAccountHandle = TelecomUtil
                .getDefaultOutgoingPhoneAccount(context,
                        PhoneAccount.SCHEME_TEL);
        boolean hasUserSelectedDefault = subscriptionAccountHandles
                .contains(defaultPhoneAccountHandle);
        /* SPRD: modify for bug598787 @{ */
        int activeSimCount = SubscriptionManager.from(context)
                .getActiveSubscriptionInfoCount();
        if (activeSimCount <= 0) {
          Toast.makeText(context, R.string.no_available_sim, Toast.LENGTH_SHORT).show();
          return true;
        }
        /* SPRD: add for bug613018 @{ */
        UserManager userManager = (UserManager) context.getSystemService(
                Context.USER_SERVICE);
        if (userManager != null && !userManager.isSystemUser()) {
          Toast.makeText(context, R.string.ip_dial_in_guest_mode,
                  Toast.LENGTH_SHORT).show();
          return true;
        }
        /* @} */
        if (isPhoneInUse()) {
          handleIpDial(context, DialerUtils.getCallingPhoneAccountHandle(context), number);
        } else if (subscriptionAccountHandles.size() <= 1 || hasUserSelectedDefault) {
          /* @} */
          handleIpDial(context, defaultPhoneAccountHandle, number);
        } else {
          SelectPhoneAccountListener ipDialCallback =
                  new HandleDialAccountSelectedCallback(context, number, false);
          DialerUtils.showSelectPhoneAccountDialog(context, subscriptionAccountHandles,
                  ipDialCallback);
        }
      }
      /** @} */
      return true;
    } else {
      return false;
    }
  }

  /** SPRD: IP dialing feature for bug886817 @{ */
  public class HandleDialAccountSelectedCallback extends SelectPhoneAccountListener {
    final private Context mContext;
    final private String mNumber;
    final private boolean misVoicemail;

    public HandleDialAccountSelectedCallback(Context context, String number, boolean isVoiceMail) {
      mContext = context;
      mNumber = number;
      misVoicemail = isVoiceMail;
    }

    @Override
    public void onPhoneAccountSelected(PhoneAccountHandle selectedAccountHandle, boolean setDefault, String callId) {
      super.onPhoneAccountSelected(selectedAccountHandle, setDefault, callId);
      if (misVoicemail) {
        //DialerUtils.callVoicemail(mContext, selectedAccountHandle);comment this line temporarily
      } else {
        handleIpDial(mContext, selectedAccountHandle, mNumber);
      }
    }
  }

  public void handleIpDial(Context context, PhoneAccountHandle phoneAccountHandle, String number) {
    /* SPRD: modify for bug598787 @{ */
    if (phoneAccountHandle == null) {
      return;
    }
    /* @} */
    IpDialingUtils ipUtils = new IpDialingUtils(context);
    for (String prefix : IpDialingUtils.EXCLUDE_PREFIX) {
      if (number.startsWith(prefix)) {
        number = number.substring(prefix.length());
        break;
      }
    }
    TelecomManager telecomManager = (TelecomManager) context
            .getSystemService(Context.TELECOM_SERVICE);
    PhoneAccount phoneAccount = telecomManager.getPhoneAccount(phoneAccountHandle);
    TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    int subId = tm.getSubIdForPhoneAccount(phoneAccount);
    String ipPrefixNum = ipUtils.getIpDialNumber(subId);

    if (!TextUtils.isEmpty(ipPrefixNum)) {
      number = ipPrefixNum + number;
      // append ip prefix-num, also as we will pass it to
      // Telecomm, so dial number can be handled in Telecom.
      final Intent intent = CallUtil.getCallIntent(number);
      intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle);
      intent.putExtra(IpDialingUtils.EXTRA_IS_IP_DIAL, true);
      intent.putExtra(IpDialingUtils.EXTRA_IP_PRFIX_NUM, ipPrefixNum);
      DialerUtils.startActivityWithErrorToast(context, intent);
    } else {
      /* SPRD:BUG 895579 modify the package name @{ */
      final Intent ipListIntent = new Intent();
      ipListIntent.putExtra(SUB_ID_EXTRA, subId);
      ipListIntent.setAction(Intent.ACTION_MAIN);//android.intent.action.MAIN
      ipListIntent.addCategory(Intent.CATEGORY_DEVELOPMENT_PREFERENCE);
      ipListIntent.setClassName(PHONE_PACKAGE_NAME, IP_NUMBER_LIST_ACTIVITY);
      DialerUtils.startActivityWithErrorToast(context, ipListIntent);
      /* @} */
    }
    /** SPRD: modify for bug957943 @{ */
//    if (context != null && context instanceof DialtactsActivity) {
//      ((DialtactsActivity) context).hideDialpadFragment(false, true);
//    }
    /** @} */
  }
  /** @} */

  /**
   * Updates the dial string (mDigits) after inserting a Pause character (,) or Wait character (;).
   */
  private void updateDialString(char newDigit) {
    if (newDigit != WAIT && newDigit != PAUSE) {
      throw new IllegalArgumentException("Not expected for anything other than PAUSE & WAIT");
    }

    int selectionStart;
    int selectionEnd;

    // SpannableStringBuilder editable_text = new SpannableStringBuilder(mDigits.getText());
    int anchor = this.digits.getSelectionStart();
    int point = this.digits.getSelectionEnd();

    selectionStart = Math.min(anchor, point);
    selectionEnd = Math.max(anchor, point);

    if (selectionStart == -1) {
      selectionStart = selectionEnd = this.digits.length();
    }

    Editable digits = this.digits.getText();

    if (canAddDigit(digits, selectionStart, selectionEnd, newDigit)) {
      digits.replace(selectionStart, selectionEnd, Character.toString(newDigit));

      if (selectionStart != selectionEnd) {
        // Unselect: back to a regular cursor, just pass the character inserted.
        this.digits.setSelection(selectionStart + 1);
      }
    }
  }

  /** Update the enabledness of the "Dial" and "Backspace" buttons if applicable. */
  private void updateDeleteButtonEnabledState() {
    if (getActivity() == null) {
      return;
    }
    final boolean digitsNotEmpty = !isDigitsEmpty();
    delete.setEnabled(digitsNotEmpty);
  }

  /**
   * Handle transitions for the menu button depending on the state of the digits edit text.
   * Transition out when going from digits to no digits and transition in when the first digit is
   * pressed.
   *
   * @param transitionIn True if transitioning in, False if transitioning out
   */
  private void updateMenuOverflowButton(boolean transitionIn) {
    overflowMenuButton = dialpadView.getOverflowMenuButton();
    if (transitionIn) {
      AnimUtils.fadeIn(overflowMenuButton, AnimUtils.DEFAULT_DURATION);
    } else {
      AnimUtils.fadeOut(overflowMenuButton, AnimUtils.DEFAULT_DURATION);
    }
  }

  /**
   * Check if voicemail is enabled/accessible.
   *
   * @return true if voicemail is enabled and accessible. Note that this can be false "temporarily"
   *     after the app boot.
   */
  private boolean isVoicemailAvailable() {
    try {
      PhoneAccountHandle defaultUserSelectedAccount =
          TelecomUtil.getDefaultOutgoingPhoneAccount(getActivity(), PhoneAccount.SCHEME_VOICEMAIL);
      if (defaultUserSelectedAccount == null) {
        // In a single-SIM phone, there is no default outgoing phone account selected by
        // the user, so just call TelephonyManager#getVoicemailNumber directly.
        /* unisoc: add for bug920980 @{ */
        List<PhoneAccountHandle> phoneAccoutHandle =
                TelecomUtil.getCallCapablePhoneAccounts(getActivity());
        for (Iterator<PhoneAccountHandle> i = phoneAccoutHandle.iterator(); i.hasNext(); ) {
          PhoneAccountHandle phoneAccountHandle = i.next();
          int slotId = InCallUiUtils.getPhoneIdByAccountHandle(getActivity(), phoneAccountHandle);

          android.util.Log.d(TAG, " Get call state for phone "
                  + slotId
                  + " with state is "
                  + TelephonyManager.getDefault()
                  .getCallStateForSlot(slotId));

          if (TelephonyManager.getDefault().getCallStateForSlot(slotId)
                  != TelephonyManager.CALL_STATE_IDLE) {
            return !TextUtils.isEmpty(TelecomUtil.getVoicemailNumber(
                    getActivity(), phoneAccountHandle));
          }
        }
        /* @} */
        return !TextUtils.isEmpty(getTelephonyManager().getVoiceMailNumber());
      } else {
        return !TextUtils.isEmpty(
            TelecomUtil.getVoicemailNumber(getActivity(), defaultUserSelectedAccount));
      }
    } catch (SecurityException se) {
      // Possibly no READ_PHONE_STATE privilege.
      LogUtil.w(
          "DialpadFragment.isVoicemailAvailable",
          "SecurityException is thrown. Maybe privilege isn't sufficient.");
    }
    return false;
  }

  /** @return true if the widget with the phone number digits is empty. */
  public boolean isDigitsEmpty() {
    return digits.length() == 0;
  }

  /**
   * Starts the asyn query to get the last dialed/outgoing number. When the background query
   * finishes, mLastNumberDialed is set to the last dialed number or an empty string if none exists
   * yet.
   */
  private void queryLastOutgoingCall() {
    lastNumberDialed = EMPTY_NUMBER;
    if (!PermissionsUtil.hasCallLogReadPermissions(getContext())) {
      return;
    }
    FragmentUtils.getParentUnsafe(this, DialpadListener.class)
        .getLastOutgoingCall(
            number -> {
              // TODO: Filter out emergency numbers if the carrier does not want redial for these.

              // If the fragment has already been detached since the last time we called
              // queryLastOutgoingCall in onResume there is no point doing anything here.
              if (getActivity() == null) {
                return;
              }
              lastNumberDialed = number;
              updateDeleteButtonEnabledState();
            });
  }

  private Intent newFlashIntent() {
    Intent intent = new CallIntentBuilder(EMPTY_NUMBER, CallInitiationType.Type.DIALPAD).build();
    intent.putExtra(EXTRA_SEND_EMPTY_FLASH, true);
    return intent;
  }

  @Override
  public void onHiddenChanged(boolean hidden) {
    super.onHiddenChanged(hidden);
    if (getActivity() == null || getView() == null) {
      return;
    }
    if (!hidden && !isDialpadChooserVisible()) {
      if (animate) {
        dialpadView.animateShow();
      }
      /* SPRD: add for bug731278 @{ */
      if (floatingActionButtonController != null) {
        floatingActionButtonController.setVisible(false);
        floatingActionButtonController.scaleIn();
      }
      if (mFloatingActionFirstButtonController != null) {
        mFloatingActionFirstButtonController.setVisible(false);
        mFloatingActionFirstButtonController.scaleIn(
                animate ? dialpadSlideInDuration : 0);
      }
      if (mFloatingActionButtonSencondaryController != null) {
        mFloatingActionButtonSencondaryController.setVisible(false);
        mFloatingActionButtonSencondaryController.scaleIn(
                animate ? dialpadSlideInDuration : 0);
      }
      /* @} */
      FragmentUtils.getParentUnsafe(this, DialpadListener.class).onDialpadShown();
      digits.requestFocus();
    }
    if (hidden) {
        if (animate) {
          /* SPRD: add for bug731278 @{ */
          if (floatingActionButtonController != null) {
            floatingActionButtonController.scaleOut();
          }
          if (mFloatingActionFirstButtonController != null) {
            mFloatingActionFirstButtonController.scaleOut();
          }
          if (mFloatingActionButtonSencondaryController != null) {
            mFloatingActionButtonSencondaryController.scaleOut();
          }
          /* @} */
        } else {
          /* SPRD: add for bug731278 @{ */
          if (floatingActionButtonController != null) {
            floatingActionButtonController.setVisible(false);
          }
          if (mFloatingActionFirstButtonController != null) {
            mFloatingActionFirstButtonController.setVisible(false);
          }
          if (mFloatingActionButtonSencondaryController != null) {
            mFloatingActionButtonSencondaryController.setVisible(false);
          }
          /* @} */
        }
      }
  }

  public boolean getAnimate() {
    return animate;
  }

  public void setAnimate(boolean value) {
    animate = value;
  }

  public void setYFraction(float yFraction) {
    ((DialpadSlidingRelativeLayout) getView()).setYFraction(yFraction);
  }

  public int getDialpadHeight() {
    if (dialpadView == null) {
      return 0;
    }
    return dialpadView.getHeight();
  }

  public void process_quote_emergency_unquote(String query) {
    if (PseudoEmergencyAnimator.PSEUDO_EMERGENCY_NUMBER.equals(query)) {
      if (pseudoEmergencyAnimator == null) {
        pseudoEmergencyAnimator =
            new PseudoEmergencyAnimator(
                new PseudoEmergencyAnimator.ViewProvider() {
                  @Override
                  public View getFab() {
                    return floatingActionButton;
                  }

                  @Override
                  public Context getContext() {
                    return DialpadFragment.this.getContext();
                  }
                });
      }
      pseudoEmergencyAnimator.start();
    } else {
      if (pseudoEmergencyAnimator != null) {
        pseudoEmergencyAnimator.end();
      }
    }
  }

  /** Animate the dialpad down off the screen. */
  public void slideDown(boolean animate, AnimationListener listener) {
    Assert.checkArgument(isDialpadSlideUp);
    isDialpadSlideUp = false;
    int animation;
    if (isLandscape) {
      animation = isLayoutRtl ? R.anim.dialpad_slide_out_left : R.anim.dialpad_slide_out_right;
    } else {
      animation = R.anim.dialpad_slide_out_bottom;
    }
    Animation slideDown = AnimationUtils.loadAnimation(getContext(), animation);
    slideDown.setInterpolator(AnimUtils.EASE_OUT);
    slideDown.setAnimationListener(listener);
    slideDown.setDuration(animate ? dialpadSlideInDuration : 0);
    getView().startAnimation(slideDown);
  }

  /** Animate the dialpad up onto the screen. */
  public void slideUp(boolean animate) {
    Assert.checkArgument(!isDialpadSlideUp);
    isDialpadSlideUp = true;
    int animation;
    if (isLandscape) {
      animation = isLayoutRtl ? R.anim.dialpad_slide_in_left : R.anim.dialpad_slide_in_right;
    } else {
      animation = R.anim.dialpad_slide_in_bottom;
    }
    Animation slideUp = AnimationUtils.loadAnimation(getContext(), animation);
    slideUp.setInterpolator(AnimUtils.EASE_IN);
    slideUp.setDuration(animate ? dialpadSlideInDuration : 0);
    getView().startAnimation(slideUp);
  }

  public boolean isDialpadSlideUp() {
    return isDialpadSlideUp;
  }

  /** Returns the text in the dialpad */
  public String getQuery() {
    return digits.getText().toString();
  }

  public interface OnDialpadQueryChangedListener {

    void onDialpadQueryChanged(String query);
  }

  public interface HostInterface {

    /**
     * Notifies the parent activity that the space above the dialpad has been tapped with no query
     * in the dialpad present. In most situations this will cause the dialpad to be dismissed,
     * unless there happens to be content showing.
     */
    boolean onDialpadSpacerTouchWithEmptyQuery();

    /** Returns true if this fragment's parent want the dialpad to show the dialpad chooser. */
    boolean shouldShowDialpadChooser();
  }

  /**
   * LinearLayout with getter and setter methods for the translationY property using floats, for
   * animation purposes.
   */
  public static class DialpadSlidingRelativeLayout extends RelativeLayout {

    public DialpadSlidingRelativeLayout(Context context) {
      super(context);
    }

    public DialpadSlidingRelativeLayout(Context context, AttributeSet attrs) {
      super(context, attrs);
    }

    public DialpadSlidingRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
      super(context, attrs, defStyle);
    }

    @UsedByReflection(value = "dialpad_fragment.xml")
    public float getYFraction() {
      final int height = getHeight();
      if (height == 0) {
        return 0;
      }
      return getTranslationY() / height;
    }

    @UsedByReflection(value = "dialpad_fragment.xml")
    public void setYFraction(float yFraction) {
      setTranslationY(yFraction * getHeight());
    }
  }

  public static class ErrorDialogFragment extends DialogFragment {

    private static final String ARG_TITLE_RES_ID = "argTitleResId";
    private static final String ARG_MESSAGE_RES_ID = "argMessageResId";
    private int titleResId;
    private int messageResId;

    public static ErrorDialogFragment newInstance(int messageResId) {
      return newInstance(0, messageResId);
    }

    public static ErrorDialogFragment newInstance(int titleResId, int messageResId) {
      final ErrorDialogFragment fragment = new ErrorDialogFragment();
      final Bundle args = new Bundle();
      args.putInt(ARG_TITLE_RES_ID, titleResId);
      args.putInt(ARG_MESSAGE_RES_ID, messageResId);
      fragment.setArguments(args);
      return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      titleResId = getArguments().getInt(ARG_TITLE_RES_ID);
      messageResId = getArguments().getInt(ARG_MESSAGE_RES_ID);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      if (titleResId != 0) {
        builder.setTitle(titleResId);
      }
      if (messageResId != 0) {
        builder.setMessage(messageResId);
      }
      builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dismiss());
      return builder.create();
    }
  }

  /**
   * Simple list adapter, binding to an icon + text label for each item in the "dialpad chooser"
   * list.
   */
  private static class DialpadChooserAdapter extends BaseAdapter {

    // IDs for the possible "choices":
    static final int DIALPAD_CHOICE_USE_DTMF_DIALPAD = 101;
    static final int DIALPAD_CHOICE_RETURN_TO_CALL = 102;
    static final int DIALPAD_CHOICE_ADD_NEW_CALL = 103;
    private static final int NUM_ITEMS = 3;
    private LayoutInflater inflater;
    private ChoiceItem[] choiceItems = new ChoiceItem[NUM_ITEMS];

    DialpadChooserAdapter(Context context) {
      // Cache the LayoutInflate to avoid asking for a new one each time.
      inflater = LayoutInflater.from(context);

      // Initialize the possible choices.
      // TODO: could this be specified entirely in XML?

      // - "Use touch tone keypad"
      choiceItems[0] =
          new ChoiceItem(
              context.getString(R.string.dialer_useDtmfDialpad),
              BitmapFactory.decodeResource(
                  context.getResources(), R.drawable.ic_dialer_fork_tt_keypad),
              DIALPAD_CHOICE_USE_DTMF_DIALPAD);

      // - "Return to call in progress"
      choiceItems[1] =
          new ChoiceItem(
              context.getString(R.string.dialer_returnToInCallScreen),
              BitmapFactory.decodeResource(
                  context.getResources(), R.drawable.ic_dialer_fork_current_call),
              DIALPAD_CHOICE_RETURN_TO_CALL);

      // - "Add call"
      choiceItems[2] =
          new ChoiceItem(
              context.getString(R.string.dialer_addAnotherCall),
              BitmapFactory.decodeResource(
                  context.getResources(), R.drawable.ic_dialer_fork_add_call),
              DIALPAD_CHOICE_ADD_NEW_CALL);
    }

    @Override
    public int getCount() {
      return NUM_ITEMS;
    }

    /** Return the ChoiceItem for a given position. */
    @Override
    public Object getItem(int position) {
      return choiceItems[position];
    }

    /** Return a unique ID for each possible choice. */
    @Override
    public long getItemId(int position) {
      return position;
    }

    /** Make a view for each row. */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      // When convertView is non-null, we can reuse it (there's no need
      // to reinflate it.)
      if (convertView == null) {
        convertView = inflater.inflate(R.layout.dialpad_chooser_list_item, null);
      }

      TextView text = convertView.findViewById(R.id.text);
      text.setText(choiceItems[position].text);

      ImageView icon = convertView.findViewById(R.id.icon);
      icon.setImageBitmap(choiceItems[position].icon);

      return convertView;
    }

    // Simple struct for a single "choice" item.
    static class ChoiceItem {

      String text;
      Bitmap icon;
      int id;

      ChoiceItem(String s, Bitmap b, int i) {
        text = s;
        icon = b;
        id = i;
      }
    }
  }

  private class CallStateReceiver extends BroadcastReceiver {

    /**
     * Receive call state changes so that we can take down the "dialpad chooser" if the phone
     * becomes idle while the chooser UI is visible.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
      String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
      if ((TextUtils.equals(state, TelephonyManager.EXTRA_STATE_IDLE)
              || TextUtils.equals(state, TelephonyManager.EXTRA_STATE_OFFHOOK))
          && isDialpadChooserVisible()) {
        // Note there's a race condition in the UI here: the
        // dialpad chooser could conceivably disappear (on its
        // own) at the exact moment the user was trying to select
        // one of the choices, which would be confusing.  (But at
        // least that's better than leaving the dialpad chooser
        // onscreen, but useless...)
        LogUtil.i("CallStateReceiver.onReceive", "hiding dialpad chooser, state: %s", state);
        showDialpadChooser(false);
      }
    }
  }

  /* SPRD: FEATURE_WAIT_PAUSE_ON_LONG_CLICK @{ */
  private void removePreviousDigitIfPossibleForPound() {
    final int currentPosition = digits.getSelectionStart();
    if (currentPosition > 0
        && (digits.getText().toString().endsWith("#") || currentPosition < digits
            .getText().toString().length())) {
        digits.setSelection(currentPosition);
        digits.getText().delete(currentPosition - 1, currentPosition);
    }
  }
  /* @} */

  /** Listener for dialpad's parent. */
  public interface DialpadListener {
    void getLastOutgoingCall(LastOutgoingCallCallback callback);

    void onDialpadShown();

    void onCallPlacedFromDialpad();
  }

  /** Callback for async lookup of the last number dialed. */
  public interface LastOutgoingCallCallback {

    void lastOutgoingCall(String number);
  }

  /**
   * A worker that helps formatting the phone number as the user types it in.
   *
   * <p>Input: the ISO 3166-1 two-letter country code of the country the user is in.
   *
   * <p>Output: an instance of {@link DialerPhoneNumberFormattingTextWatcher}. Note: It is unusual
   * to return a non-data value from a worker. But {@link DialerPhoneNumberFormattingTextWatcher}
   * depends on libphonenumber API, which cannot be initialized on the main thread.
   */
  private static class InitPhoneNumberFormattingTextWatcherWorker
      implements Worker<String, DialerPhoneNumberFormattingTextWatcher> {

    @Nullable
    @Override
    public DialerPhoneNumberFormattingTextWatcher doInBackground(@Nullable String countryCode) {
      return new DialerPhoneNumberFormattingTextWatcher(countryCode);
    }
  }

  /**
   * An extension of Android telephony's {@link PhoneNumberFormattingTextWatcher}. This watcher
   * skips formatting Argentina mobile numbers for domestic calls.
   *
   * <p>As of Nov. 28, 2017, the as-you-type-formatting provided by libphonenumber's
   * AsYouTypeFormatter (which {@link PhoneNumberFormattingTextWatcher} depends on) can't correctly
   * format Argentina mobile numbers for domestic calls (a bug). We temporarily disable the
   * formatting for such numbers until libphonenumber is fixed (which will come as early as the next
   * Android release).
   */
  @VisibleForTesting
  public static class DialerPhoneNumberFormattingTextWatcher
      extends PhoneNumberFormattingTextWatcher {
    private static final Pattern AR_DOMESTIC_CALL_MOBILE_NUMBER_PATTERN;

    // This static initialization block builds a pattern for domestic calls to Argentina mobile
    // numbers:
    // (1) Local calls: 15 <local number>
    // (2) Long distance calls: <area code> 15 <local number>
    // See https://en.wikipedia.org/wiki/Telephone_numbers_in_Argentina for detailed explanations.
    static {
      String regex =
          "0?("
              + "  ("
              + "   11|"
              + "   2("
              + "     2("
              + "       02?|"
              + "       [13]|"
              + "       2[13-79]|"
              + "       4[1-6]|"
              + "       5[2457]|"
              + "       6[124-8]|"
              + "       7[1-4]|"
              + "       8[13-6]|"
              + "       9[1267]"
              + "     )|"
              + "     3("
              + "       02?|"
              + "       1[467]|"
              + "       2[03-6]|"
              + "       3[13-8]|"
              + "       [49][2-6]|"
              + "       5[2-8]|"
              + "       [67]"
              + "     )|"
              + "     4("
              + "       7[3-578]|"
              + "       9"
              + "     )|"
              + "     6("
              + "       [0136]|"
              + "       2[24-6]|"
              + "       4[6-8]?|"
              + "       5[15-8]"
              + "     )|"
              + "     80|"
              + "     9("
              + "       0[1-3]|"
              + "       [19]|"
              + "       2\\d|"
              + "       3[1-6]|"
              + "       4[02568]?|"
              + "       5[2-4]|"
              + "       6[2-46]|"
              + "       72?|"
              + "       8[23]?"
              + "     )"
              + "   )|"
              + "   3("
              + "     3("
              + "       2[79]|"
              + "       6|"
              + "       8[2578]"
              + "     )|"
              + "     4("
              + "       0[0-24-9]|"
              + "       [12]|"
              + "       3[5-8]?|"
              + "       4[24-7]|"
              + "       5[4-68]?|"
              + "       6[02-9]|"
              + "       7[126]|"
              + "       8[2379]?|"
              + "       9[1-36-8]"
              + "     )|"
              + "     5("
              + "       1|"
              + "       2[1245]|"
              + "       3[237]?|"
              + "       4[1-46-9]|"
              + "       6[2-4]|"
              + "       7[1-6]|"
              + "       8[2-5]?"
              + "     )|"
              + "     6[24]|"
              + "     7("
              + "       [069]|"
              + "       1[1568]|"
              + "       2[15]|"
              + "       3[145]|"
              + "       4[13]|"
              + "       5[14-8]|"
              + "       7[2-57]|"
              + "       8[126]"
              + "     )|"
              + "     8("
              + "       [01]|"
              + "       2[15-7]|"
              + "       3[2578]?|"
              + "       4[13-6]|"
              + "       5[4-8]?|"
              + "       6[1-357-9]|"
              + "       7[36-8]?|"
              + "       8[5-8]?|"
              + "       9[124]"
              + "     )"
              + "   )"
              + " )?15"
              + ").*";
      AR_DOMESTIC_CALL_MOBILE_NUMBER_PATTERN = Pattern.compile(regex.replaceAll("\\s+", ""));
    }

    private final String countryCode;

    DialerPhoneNumberFormattingTextWatcher(String countryCode) {
      super(countryCode);
      this.countryCode = countryCode;
    }

    @Override
    public synchronized void afterTextChanged(Editable s) {
      // When the country code is NOT "AR", Android telephony's PhoneNumberFormattingTextWatcher can
      // correctly handle the input so we will let it do its job.
      if (!Ascii.toUpperCase(countryCode).equals("AR")) {
        super.afterTextChanged(s);
        return;
      }

      // When the country code is "AR", PhoneNumberFormattingTextWatcher can also format the input
      // correctly if the number is NOT for a domestic call to a mobile phone.
      String rawNumber = getRawNumber(s);
      Matcher matcher = AR_DOMESTIC_CALL_MOBILE_NUMBER_PATTERN.matcher(rawNumber);
      if (!matcher.matches()) {
        super.afterTextChanged(s);
        return;
      }

      // As modifying the input will trigger another call to afterTextChanged(Editable), we must
      // check whether the input's format has already been removed and return if it has
      // been to avoid infinite recursion.
      if (rawNumber.contentEquals(s)) {
        return;
      }

      // If we reach this point, the country code must be "AR" and variable "s" represents a number
      // for a domestic call to a mobile phone. "s" is incorrectly formatted by Android telephony's
      // PhoneNumberFormattingTextWatcher so we remove its format by replacing it with the raw
      // number.
      s.replace(0, s.length(), rawNumber);

      // Make sure the cursor is at the end of the text.
      Selection.setSelection(s, s.length());

      PhoneNumberUtils.addTtsSpan(s, 0 /* start */, s.length() /* endExclusive */);
    }

    private static String getRawNumber(Editable s) {
      StringBuilder rawNumberBuilder = new StringBuilder();

      for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        if (PhoneNumberUtils.isNonSeparator(c)) {
          rawNumberBuilder.append(c);
        }
      }

      return rawNumberBuilder.toString();
    }
  }
  /* SPRD: add for bug731278 @{ */
  @Override
  public void onCallListChange(CallList callList) {
    if (callList != null) {
      int subId = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
      if (callList.getActiveCall() != null
              && callList.getActiveCall().getAccountHandle() != null) {
        PhoneAccount account = mTelecomManager.getPhoneAccount(
                callList.getActiveCall().getAccountHandle());
        subId = getTelephonyManager().getSubIdForPhoneAccount(account);
        int phoneId = SubscriptionManager.getPhoneId(subId);
        updateSimIcon(phoneId);
      } else {
        if (mIsFirstSimIncall) {
          if (mFirstSimIconView != null) {
            mFirstSimIconView.setImageResource(R.drawable.fab_ic_call_sim1);
            mIsFirstSimIncall = false;
          }
        } else if (mIsSecSimIncall) {
          if (mSecSimIconView != null) {
            mSecSimIconView.setImageResource(R.drawable.fab_ic_call_sim2);
            mIsSecSimIncall = false;
          }
        }
      }
    }
  }

  @Override
  public void onIncomingCall(DialerCall call) {
  }

  @Override
  public void onUpgradeToVideo(DialerCall call) {
  }

  @Override
  public void onSessionModificationStateChange(DialerCall call) {
  }

  @Override
  public void onDisconnect(DialerCall call) {
  }

  @Override
  public void onWiFiToLteHandover(DialerCall call) {
  }

  @Override
  public void onHandoverToWifiFailed(DialerCall call) {
  }

  @Override
  public void onInternationalCallOnWifi(@NonNull DialerCall call) {
  }

  private void updateSimIcon(int phoneId) {
    switch (phoneId) {
      case INDEX_SIM1:
        if (mFirstSimIconView != null) {
          mFirstSimIconView.setImageResource(R.drawable.fab_ic_call_sim1_oncall);
          mIsFirstSimIncall = true;
        }
        break;
      case INDEX_SIM2:
        if (mSecSimIconView != null) {
          mSecSimIconView.setImageResource(R.drawable.fab_ic_call_sim2_oncall);
          mIsSecSimIncall = true;
        }
        break;
      default:
        break;
    }
  }
  /* @} */
  /* SPRD: add for bug672502 @{ */
  private class SimStateChangedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      int slotId = intent.getIntExtra(PhoneConstants.PHONE_KEY,
              SubscriptionManager.INVALID_SIM_SLOT_INDEX);
      if (slotId == SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
        return;
      }
      if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
        String simStatus = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
        if ((IccCardConstants.INTENT_VALUE_ICC_LOADED).equals(simStatus)) {
          List<SubscriptionInfo> subscriptionInfos = SubscriptionManager.from(
                  getActivity()).getActiveSubscriptionInfoList();
          if ((subscriptionInfos == null)
                  || ((subscriptionInfos != null) && (subscriptionInfos.size() < 2))) {
            return;
          }
          Log.d(TAG, "sim " + slotId
                  + ", subscription info: " + subscriptionInfos.get(slotId).toString());
          switch (slotId) {
            case INDEX_SIM1:
              if (mFirstSimDisplayName != null) {
                mFirstSimDisplayName.setText(
                        subscriptionInfos.get(INDEX_SIM1).getDisplayName());
              }
              break;
            case INDEX_SIM2:
              if (mSecSimDisplayName != null) {
                mSecSimDisplayName.setText(
                        subscriptionInfos.get(INDEX_SIM2).getDisplayName());
              }
              break;
            default:
              break;
          }
        }
      }
    }
  }
  
  /* SPRD: Add for VoLTE@{*/
  private synchronized void tryRegisterImsListener() {
      if(getActivity() != null && PermissionsUtil.hasPhonePermissions(getActivity())) {
          mIImsServiceEx = ImsManagerEx.getIImsServiceEx();
          if(mIImsServiceEx != null){
              try{
                  if(!mIsImsListenerRegistered){
                      mIsImsListenerRegistered = true;
                      mIImsServiceEx.registerforImsRegisterStateChanged(mImsUtListenerExBinder);
                  }
              }catch(RemoteException e){
                  LogUtil.e(
                          "DialpadFragment.tryRegisterImsListener",
                          "regiseterforImsException:" + e);
              }
          }
      }
  }

  private final IImsRegisterListener.Stub mImsUtListenerExBinder = new IImsRegisterListener.Stub(){
      @Override
      public void imsRegisterStateChange(boolean isRegistered){
          final MenuItem videoCallItem = (mPopupMenu == null)? null:mPopupMenu.getMenu().findItem(MENU_MAKE_VIDEO_CALL);
          final MenuItem groupCallItem = (mPopupMenu == null) ? null : mPopupMenu.getMenu().
                  findItem(MENU_MAKE_MULTE_CALL);
          LogUtil.i("DialpadFragment.imsRegisterStateChange", "isRegistered: " + isRegistered + ", videoCallItem: " + videoCallItem
                  + " groupCallItem: "+ groupCallItem);
          if(mIsVideoEnable != isRegistered){
              mIsVideoEnable = isRegistered;
          }

          if (getActivity() != null) {
              if (videoCallItem != null) {
                /** SPRD: bug895158 Card One Unicom Card II moves, the model closes the “Video Call” function,
                 * the dial pad enters the number, and the option still has the “Start Video Call” option.@{*/
                  videoCallItem.setVisible(CallUtil.isVideoEnabled(getActivity()));
                /** @} */
              }

              if (groupCallItem != null) {
                  //UNISOC:modify for bug 931236
                  groupCallItem.setVisible(mIsVideoEnable && mIsSupportMultiCall);
              }
          }
      }
  };
  /* @} */

  /* UNISOC: add for bug904221 @{ */
  public PopupMenu getOverflowPopupMenu() {
    return overflowPopupMenu;
  }
  /* @} */

}
