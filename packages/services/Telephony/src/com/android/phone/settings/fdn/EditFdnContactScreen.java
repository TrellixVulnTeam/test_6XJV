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

package com.android.phone.settings.fdn;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.view.Window.PROGRESS_VISIBILITY_OFF;
import static android.view.Window.PROGRESS_VISIBILITY_ON;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.provider.Contacts.PeopleColumns;
import android.provider.Contacts.PhonesColumns;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.CarrierConfigManagerEx;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DialerKeyListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.phone.PhoneGlobals;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.unisoc.phone.callsettings.IccUriUtils;
import com.unisoc.phone.callsettings.ActivityContainer;
import com.android.internal.telephony.EncodeException;
import com.android.internal.telephony.GsmAlphabetEx;

/**
 * Activity to let the user add or edit an FDN contact.
 */
public class EditFdnContactScreen extends Activity {
    private static final String LOG_TAG = PhoneGlobals.LOG_TAG;
    private static final boolean DBG = true;

    // Menu item codes
    private static final int MENU_IMPORT = 1;
    private static final int MENU_DELETE = 2;

    private static final String INTENT_EXTRA_NAME = "name";
    private static final String INTENT_EXTRA_NUMBER = "number";

    private static final int PIN2_REQUEST_CODE = 100;

    private SubscriptionInfoHelper mSubscriptionInfoHelper;

    private String mName;
    private String mNumber;
    private String mPin2;
    private boolean mAddContact;
    private QueryHandler mQueryHandler;

    private EditText mNameField;
    private EditText mNumberField;
    private LinearLayout mPinFieldContainer;
    private Button mButton;

    /* SPRD: delete for function FDN support. @{
    ** origin
    private Handler mHandler = new Handler();
    **/
    private static final int EVENT_PIN2_ENTRY_COMPLETE = PIN2_REQUEST_CODE + 1;
    private static final String FDN_UPDATE_ACTION = "android.callsettings.action.FDN_LIST_CHANGED";
    private static final int RESULT_PIN2_TIMEOUT = PIN2_REQUEST_CODE + 2;
    private Phone mPhone;
    private int mRemainPin2Times = IccUriUtils.MAX_INPUT_TIMES;
    private static final long HANDLER_TIME = 2000;
    /* }@ */

    // UNISOC: add for FEATURE bug889286
    private int mFdnNumberLimitLength;
    // UNISOC: add for BUG 925034
    private int configNumberLimitLength;

    // SPRD: add for Bug 617999
    private static final String INTENT_EXTRA_SUB_ID = "subid";

    /**
     * Constants used in importing from contacts
     */
    /** request code when invoking subactivity */
    private static final int CONTACTS_PICKER_CODE = 200;
    /** projection for phone number query */
    private static final String[] NUM_PROJECTION = new String[] {CommonDataKinds.Phone.DISPLAY_NAME,
            CommonDataKinds.Phone.NUMBER};
    /** static intent to invoke phone number picker */
    private static final Intent CONTACT_IMPORT_INTENT;
    static {
        /* UNISOC: add for FEATURE bug900312 @{ */
        CONTACT_IMPORT_INTENT = new Intent(Intent.ACTION_PICK);
        CONTACT_IMPORT_INTENT.setType(CommonDataKinds.Phone.CONTENT_TYPE);
        /* @} */
    }
    /** flag to track saving state */
    private boolean mDataBusy;
    private Context mContext;

    // UNISOC: modify by BUG 954072
    private ActivityContainer mActivityContainer;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        resolveIntent();
        mContext = this;
        CarrierConfigManager carrierConfig = (CarrierConfigManager) mContext.getSystemService(
                Context.CARRIER_CONFIG_SERVICE);

        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.edit_fdn_contact_screen);
        setupView();
        setTitle(mAddContact ? R.string.add_fdn_contact : R.string.edit_fdn_contact);

        /* SPRD: function FDN support. @{
        ** origin
        displayProgress(false);
        **/
        mDataBusy = false;
        int phoneId = mPhone.getPhoneId();
        mRemainPin2Times = IccUriUtils.getInstance().getPIN2RemainTimes(getBaseContext(), phoneId);
        /* }@ */
        /* UNISOC: modify for BUG 925034 @{ */
        if (carrierConfig != null) {
            PersistableBundle config = carrierConfig.getConfigForPhoneId(phoneId);
            if (config != null) {
                configNumberLimitLength = config.getInt(
                        CarrierConfigManagerEx.KEY_FDN_NUMBER_LENGTH, 20);
            }
        }
        if (configNumberLimitLength != 20) {
            mFdnNumberLimitLength = configNumberLimitLength;
            mNumberField.setFilters(new InputFilter[]{
                    new InputFilter.LengthFilter(mFdnNumberLimitLength)});
        } else {
            // UNISOC: add for FEATURE bug889286
            mFdnNumberLimitLength = mContext.getResources().getInteger(
                    R.integer.config_valid_number_length);
        }
        /* @} */
        /* UNISOC: modify by BUG 954072 @{ */
        mActivityContainer = ActivityContainer.getInstance();
        mActivityContainer.setApplication(getApplication());
        mActivityContainer.addActivity(this, phoneId);
    }


    protected void onDestroy() {
        super.onDestroy();
        if (mActivityContainer != null) {
            mActivityContainer.removeActivity(this);
        }
    }
    /* @} */

    /**
     * We now want to bring up the pin request screen AFTER the
     * contact information is displayed, to help with user
     * experience.
     *
     * Also, process the results from the contact picker.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (DBG) log("onActivityResult request:" + requestCode + " result:" + resultCode);

        switch (requestCode) {
            case PIN2_REQUEST_CODE:
                Bundle extras = (intent != null) ? intent.getExtras() : null;
                if (extras != null) {
                    mPin2 = extras.getString("pin2");
                    /* SPRD: function FDN support. @{
                    ** origin
                    if (mAddContact) {
                        addContact();
                    } else {
                        updateContact();
                    }
                    **/
                    checkPin2(mPin2);
                    /* }@ */
                } else if (resultCode != RESULT_OK) {
                    // if they cancelled, then we just cancel too.
                    if (DBG) log("onActivityResult: cancelled.");
                    finish();
                }
                break;

            // look for the data associated with this number, and update
            // the display with it.
            case CONTACTS_PICKER_CODE:
                if (resultCode != RESULT_OK) {
                    if (DBG) log("onActivityResult: cancelled.");
                    return;
                }
                Cursor cursor = null;
                try {
                    cursor = getContentResolver().query(intent.getData(),
                        NUM_PROJECTION, null, null, null);
                    if ((cursor == null) || (!cursor.moveToFirst())) {
                        Log.w(LOG_TAG,"onActivityResult: bad contact data, no results found.");
                        return;
                    }
                    mNameField.setText(cursor.getString(0));
                    mNumberField.setText(cursor.getString(1));
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                break;
        }
    }

    /**
     * Overridden to display the import and delete commands.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        Resources r = getResources();

        // Added the icons to the context menu
        menu.add(0, MENU_IMPORT, 0, r.getString(R.string.importToFDNfromContacts))
                .setIcon(R.drawable.ic_menu_contact);
        /* SPRD: modify by BUG 741131 @{ */
        if (!mAddContact) {
            menu.add(0, MENU_DELETE, 0, r.getString(R.string.menu_delete))
                .setIcon(android.R.drawable.ic_menu_delete);
        }
        /* @} */
        return true;
    }

    /**
     * Allow the menu to be opened ONLY if we're not busy.
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean result = super.onPrepareOptionsMenu(menu);
        return mDataBusy ? false : result;
    }

    /**
     * Overridden to allow for handling of delete and import.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_IMPORT:
                startActivityForResult(CONTACT_IMPORT_INTENT, CONTACTS_PICKER_CODE);
                return true;

            case MENU_DELETE:
                deleteSelected();
                return true;

            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void resolveIntent() {
        Intent intent = getIntent();

        mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, intent);
        /* SPRD: function FDN support. @{ */
        if (mPhone == null) {
            mPhone = mSubscriptionInfoHelper.getPhone();
        }
        /* }@ */
        mName = intent.getStringExtra(INTENT_EXTRA_NAME);
        mNumber = intent.getStringExtra(INTENT_EXTRA_NUMBER);

        mAddContact = TextUtils.isEmpty(mNumber);
    }

    /**
     * We have multiple layouts, one to indicate that the user needs to
     * open the keyboard to enter information (if the keybord is hidden).
     * So, we need to make sure that the layout here matches that in the
     * layout file.
     */
    private void setupView() {
        mNameField = (EditText) findViewById(R.id.fdn_name);
        if (mNameField != null) {
            mNameField.setOnFocusChangeListener(mOnFocusChangeHandler);
            mNameField.setOnClickListener(mClicked);
            mNameField.addTextChangedListener(mTextWatcher);
            /* UNISOC: add for FEATURE bug903647 @{ */
            mNameField.setFocusable(true);
            mNameField.setFocusableInTouchMode(true);
            mNameField.requestFocus();
            /* @} */
        }

        mNumberField = (EditText) findViewById(R.id.fdn_number);
        if (mNumberField != null) {
            mNumberField.setTextDirection(View.TEXT_DIRECTION_LTR);
            mNumberField.setKeyListener(DialerKeyListener.getInstance());
            mNumberField.setOnFocusChangeListener(mOnFocusChangeHandler);
            mNumberField.setOnClickListener(mClicked);
            mNumberField.addTextChangedListener(mTextWatcher);
        }

        if (!mAddContact) {
            if (mNameField != null) {
                mNameField.setText(mName);
                // UNISOC: add for FEATURE bug903647
                mNameField.setSelection(mNameField.getText().toString().length());
            }
            if (mNumberField != null) {
                mNumberField.setText(mNumber);
            }
        }

        mButton = (Button) findViewById(R.id.button);
        if (mButton != null) {
            mButton.setOnClickListener(mClicked);
            setButtonEnabled();
        }

        mPinFieldContainer = (LinearLayout) findViewById(R.id.pinc);

    }

    private String getNameFromTextField() {
        return mNameField.getText().toString();
    }

    private String getNumberFromTextField() {
        return mNumberField.getText().toString();
    }

    /**
     * Enable Save button if text has been added to both name and number
     */
    private void setButtonEnabled() {
        if (mButton != null && mNameField != null && mNumberField != null) {
            mButton.setEnabled(mNameField.length() > 0 && mNumberField.length() > 0);
        }
    }

    /**
      * @param number is voice mail number
      * @return true if number length is less than 20-digit limit
      *
      * TODO: Fix this logic.
      */
     private boolean isValidNumber(String number) {
         return (number.length() <= 20) && (number.length() > 0);
     }

     /**
      * SPRD: add for bug 516070 @{
      *
      * @param name
      * @return true if name length is less than 14-digit limit
      */
     private boolean isValidName(String name) {
         //add for unisoc 969193
         int nameLength = getGsmAlphabetBytes(name);
         return (nameLength <= 14);
     }

    //add for unisoc 969193
    public  int  getGsmAlphabetBytes(String record) {
        byte[] bytes = new byte[0];
        int length = 0;
        if (record == null) {
            record = "";
        }
        try {
            bytes = GsmAlphabetEx.stringToGsmAlphaSS(record);
            length = bytes != null ? bytes.length : 0;
        } catch (EncodeException e) {
            try {
                if (record != null) {
                    bytes = record.getBytes("utf-16be");
                }
                //unisoc 973530 add length 1 for 0x80
                length = bytes == null ? 0 :bytes.length +1;
            } catch (Exception e1) {
                e1.printStackTrace();
           }
        }
        return length;
    }

     /* SPRD: function FDN support. @{ */
     private boolean isValidNumberOrTag(String number, String tag) {
         String numberPlus = number;
         if (TextUtils.isEmpty(number) && TextUtils.isEmpty(tag)) {
             return false;
         }
          if (!TextUtils.isEmpty(number)) {
            if (number.charAt(0) == '+') {
                numberPlus = number.substring(1);
            }
        }
        Log.d(LOG_TAG, "isValidNumberOrTag number:"+numberPlus);
         if (!containDigit(numberPlus)) {
             return false;
         }
         return true;
     }

     private boolean containDigit(String content) {
         boolean flag = false;
         /* UNISOC: add for bug908175 & bug884565 & 925031@{ */
         content = content.replaceAll("\\s*", "");
         Pattern p = Pattern.compile("[\\+]?[0-9\\p{P}+&&[^;,]]+");
         /* @} */
         Matcher m = p.matcher(content);
         if (m.matches()) {
             flag = true;
         }
         return flag;
     }
     /* }@ */

    private void addContact() {
        if (DBG) log("addContact");

        final String name = getNameFromTextField();
        final String number = PhoneNumberUtils.convertAndStrip(getNumberFromTextField());

        /* SPRD: add for bug 516070 @{ */
        if (!isValidName(name)) {
            handleResult(false, false, true);
            return;
        }
        /* @} */

        /* SPRD: add for bug689671 @{ */
        if (!isValidNumber(mContext, number)) {
            handleResult(false, true, false);
            return;
        }
        /* @} */
        /* SPRD: function FDN support. @{ */
        if (mSubscriptionInfoHelper.getPhone().getSubId() < 0) {
            log("addContact, Ignore add action for invliad subId(" +
                    mSubscriptionInfoHelper.getSubId() + ")");
            return;
        }
        /* }@ */

        Uri uri = FdnList.getContentUri(mSubscriptionInfoHelper);

        ContentValues bundle = new ContentValues(3);
        bundle.put("tag", getNameFromTextField());
        bundle.put("number", number);
        bundle.put("pin2", mPin2);

        mQueryHandler = new QueryHandler(getContentResolver());
        mQueryHandler.startInsert(0, null, uri, bundle);
        displayProgress(true);
        showStatus(getResources().getText(R.string.adding_fdn_contact));
    }

    private void updateContact() {
        if (DBG) log("updateContact");

        final String name = getNameFromTextField();
        final String number = PhoneNumberUtils.convertAndStrip(getNumberFromTextField());

        /* SPRD: add for bug 516070 @{ */
        if (!isValidName(name)) {
            handleResult(false, false, true);
            return;
        }
        /* @} */

        /* SPRD: add for bug689671 @{ */
        if (!isValidNumber(mContext, number)) {
            handleResult(false, true, false);
            return;
        }
        /* @} */
        Uri uri = FdnList.getContentUri(mSubscriptionInfoHelper);

        ContentValues bundle = new ContentValues();
        bundle.put("tag", mName);
        bundle.put("number", mNumber);
        bundle.put("newTag", name);
        bundle.put("newNumber", number);
        bundle.put("pin2", mPin2);

        mQueryHandler = new QueryHandler(getContentResolver());
        mQueryHandler.startUpdate(0, null, uri, bundle, null, null);
        displayProgress(true);
        showStatus(getResources().getText(R.string.updating_fdn_contact));
    }

    /**
     * Handle the delete command, based upon the state of the Activity.
     */
    private void deleteSelected() {
        // delete ONLY if this is NOT a new contact.
        if (!mAddContact) {
            Intent intent = mSubscriptionInfoHelper.getIntent(DeleteFdnContactScreen.class);
            intent.putExtra(INTENT_EXTRA_NAME, mName);
            intent.putExtra(INTENT_EXTRA_NUMBER, mNumber);
            startActivity(intent);
        }
        finish();
    }

    private void authenticatePin2() {
        Intent intent = new Intent();
        intent.setClass(this, GetPin2Screen.class);
        intent.setData(FdnList.getContentUri(mSubscriptionInfoHelper));
        /* SPRD: function FDN support. @{ */
        intent.putExtra("times", mRemainPin2Times);
        /* }@ */
        startActivityForResult(intent, PIN2_REQUEST_CODE);
    }

    private void displayProgress(boolean flag) {
        // indicate we are busy.
        mDataBusy = flag;
        getWindow().setFeatureInt(
                Window.FEATURE_INDETERMINATE_PROGRESS,
                mDataBusy ? PROGRESS_VISIBILITY_ON : PROGRESS_VISIBILITY_OFF);
        // make sure we don't allow calls to save when we're
        // not ready for them.
        mButton.setClickable(!mDataBusy);
    }

    /**
     * Removed the status field, with preference to displaying a toast
     * to match the rest of settings UI.
     */
    private void showStatus(CharSequence statusMsg) {
        if (statusMsg != null) {
            Toast.makeText(this, statusMsg, Toast.LENGTH_LONG)
                    .show();
        }
    }

    /* SPRD: add for bug 516070 @{ */
    private void handleResult(boolean success, boolean invalidNumber, boolean invalidName) {
        if (success) {
            if (DBG) {
                log("handleResult: success!");
            }
            showStatus(getResources().getText(mAddContact ?
                    R.string.fdn_contact_added : R.string.fdn_contact_updated));
        } else {
            if (DBG) {
                log("handleResult: failed!");
            }
            if (invalidNumber) {
                // UNISOC: add for FEATURE bug889286
                showStatus(getResources().getString(R.string.fdn_invalid_number,
                        mFdnNumberLimitLength));
            } else if (invalidName) {
                showStatus(getResources().getText(R.string.fdn_invalid_name));
            } else {
               /* SPRD: function FDN support. @{
               ** origin
               if (PhoneFactory.getDefaultPhone().getIccCard().getIccPin2Blocked()) {
                    showStatus(getResources().getText(R.string.fdn_enable_puk2_requested));
               } else if (PhoneFactory.getDefaultPhone().getIccCard().getIccPuk2Blocked()) {
                    showStatus(getResources().getText(R.string.puk2_blocked));
               }
               **/
                if (mPhone.getIccCard().getIccPin2Blocked()) {
                    showStatus(getResources().getText(R.string.fdn_enable_puk2_requested));
                } else if (mPhone.getIccCard().getIccPuk2Blocked()) {
                    showStatus(getResources().getText(R.string.puk2_blocked));
                }
               /* }@ */
                else {
                    // There's no way to know whether the failure is due to incorrect PIN2 or
                    // an inappropriate phone number.
                    showStatus(getResources().getText(R.string.pin2_or_fdn_invalid));
                }
            }
        }
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 2000);
    }
    /* @} */

    private void handleResult(boolean success, boolean invalidNumber) {
        if (success) {
            if (DBG) log("handleResult: success!");
            showStatus(getResources().getText(mAddContact ?
                    R.string.fdn_contact_added : R.string.fdn_contact_updated));
        } else {
            if (DBG) log("handleResult: failed!");
            if (invalidNumber) {
                // UNISOC: add for FEATURE bug889286
                showStatus(getResources().getString(R.string.fdn_invalid_number,
                        mFdnNumberLimitLength));
            } else {
               if (PhoneFactory.getDefaultPhone().getIccCard().getIccPin2Blocked()) {
                    showStatus(getResources().getText(R.string.fdn_enable_puk2_requested));
                } else if (PhoneFactory.getDefaultPhone().getIccCard().getIccPuk2Blocked()) {
                    showStatus(getResources().getText(R.string.puk2_blocked));
                } else {
                    // There's no way to know whether the failure is due to incorrect PIN2 or
                    // an inappropriate phone number.
                    showStatus(getResources().getText(R.string.pin2_or_fdn_invalid));
                }
            }
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 2000);

    }

    private final View.OnClickListener mClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mPinFieldContainer.getVisibility() != View.VISIBLE) {
                return;
            }

            if (v == mNameField) {
                //add for unisoc 1017627
                mButton.requestFocus();
            } else if (v == mNumberField) {
                mButton.requestFocus();
            } else if (v == mButton) {
                final String number = PhoneNumberUtils.convertAndStrip(getNumberFromTextField());

                /* UNISOC: add for FEATURE bug900501 @{ */
                if (TextUtils.isEmpty(number)) {
                    showToast(R.string.number_empty);
                    return;
                } else if (!isValidNumberOrTag(number.replaceAll(" ",""), "tag")) {
                    showToast(R.string.callFailed_unobtainable_number);
                    return;
                }
                /* @} */

                if (!isValidNumber(mContext, number)) {
                    handleResult(false, true);
                    return;
                }
                // Authenticate the pin AFTER the contact information
                // is entered, and if we're not busy.
                if (!mDataBusy) {
                    authenticatePin2();
                }
            }
        }
    };

    /* SPRD: FDN_FEATURE. @{ */
    private boolean isAirplaneModeOn(Context context) {
        if (context == null) {
            return true;
        }
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) != 0;
    }

    private int counter = 0;

    private int countStr(String str1, char str2) {
        if (str1.indexOf(str2) == -1) {
            return 0;
        } else if (str1.indexOf(str2) != -1) {
            counter++;
            countStr(str1.substring(str1.indexOf(str2) + 1), str2);
            return counter;
        }
        return 0;
    }
    /* }@ */

    private final View.OnFocusChangeListener mOnFocusChangeHandler =
            new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                TextView textView = (TextView) v;
                Selection.selectAll((Spannable) textView.getText());
            }
        }
    };

    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable arg0) {}

        @Override
        public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}

        @Override
        public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            setButtonEnabled();
        }
    };

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor c) {
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            if (DBG) log("onInsertComplete");
            displayProgress(false);
            handleResult(uri != null, false, false);
            // SPRD: add for Bug 617999
            sendBroadcast();
        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
            if (DBG) log("onUpdateComplete");
            displayProgress(false);
            handleResult(result > 0, false, false);
            // SPRD: add for Bug 617999
            sendBroadcast();
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
        }
    }

    /* SPRD: add for Bug 617999 @{ */
    private void sendBroadcast() {
        Intent intent = new Intent(FDN_UPDATE_ACTION);
        intent.putExtra(INTENT_EXTRA_SUB_ID, mSubscriptionInfoHelper.getSubId());
        // SPRD: add for Bug712193
        intent.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        sendBroadcast(intent);
    }
    /* @{ */

    /* SPRD: function FDN support. @{ */
    protected void checkPin2(String pin2) {
        log("checkPin2: pin2 = " + pin2);

        if (IccUriUtils.validatePin(pin2, false)) {
            // get the relevant data for the icc call
            boolean isEnabled = mPhone.getIccCard().getIccFdnEnabled();
            log("toggleFDNEnable  isEnabled" + isEnabled);

            Message onComplete = mHandler.obtainMessage(EVENT_PIN2_ENTRY_COMPLETE);
            // make fdn request
            // SPRD: modify for bug586544
            mPhone.getIccCard().supplyPin2(pin2, onComplete);
        } else {
            // throw up error if the pin is invalid.
            showStatus(getResources().getText(R.string.invalidPin2));
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                // when we are enabling FDN, either we are unsuccessful and
                // display a toast, or just update the UI.
                case EVENT_PIN2_ENTRY_COMPLETE:
                    log(" EVENT_PIN2_ENTRY_COMPLETE");
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        displayProgress(false);
                        mRemainPin2Times -= 1;
                        log(" EVENT_PIN2_ENTRY_COMPLETE remainTimesPIN2:" + mRemainPin2Times);
                        if (mRemainPin2Times > 0) {
                            showStatus(getResources().getText(R.string.pin2_invalid));
                            authenticatePin2();
                        } else {
                            showStatus(getResources().getText(R.string.puk2_requested));
                            setResult(RESULT_PIN2_TIMEOUT);
                            finish();
                        }
                    } else {
                        if (mAddContact) {
                            addContact();
                        } else {
                            updateContact();
                        }
                    }
                    break;
            }
        }
    };
    /* }@ */

    private boolean isValidNumber(Context context, String number) {
        /* UNISOC: add for FEATURE bug889286 @{ */
        if (TextUtils.isEmpty(number)) {
            return false;
        } else {
            return (number.length() <= mFdnNumberLimitLength);
        }
        /* @} */
    }

    private void log(String msg) {
        Log.d(LOG_TAG, "[EditFdnContact] " + msg);
    }

    /* UNISOC: add for FEATURE bug900501 @{ */
    public void showToast(int stingId) {
        Toast.makeText(EditFdnContactScreen.this, stingId, Toast.LENGTH_LONG).show();
    }
    /* @} */
}
