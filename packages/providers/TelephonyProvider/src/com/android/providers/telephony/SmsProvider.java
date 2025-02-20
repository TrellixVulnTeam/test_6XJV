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

package com.android.providers.telephony;

import android.annotation.NonNull;
import android.app.AppOpsManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.UserHandle;
import android.provider.Contacts;
import android.provider.Telephony;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.provider.TelephonyEx.SmsEx;
import android.provider.TelephonyEx.MmsEx;
import android.provider.Telephony.TextBasedSmsColumns;
import android.provider.Telephony.Threads;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.android.internal.telephony.TelephonyIntents;
import com.android.providers.telephony.ext.simmessage.ProvidersReceiver;
//fanzr add
//489223 begin
import com.android.providers.telephony.ext.Interface.IDatabaseOperate;
import com.android.providers.telephony.ext.simmessage.ICCMessageManager;
import com.android.providers.telephony.ext.simmessage.Status;
import android.telephony.TelephonyManager;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Context;
import android.content.Intent;
import com.android.providers.telephony.MmsSmsDatabaseHelper;
//489223 end

public class SmsProvider extends ContentProvider {
    private static final Uri NOTIFICATION_URI = Uri.parse("content://sms");
    private static final Uri ICC_URI = Uri.parse("content://sms/icc");
    static final String TABLE_SMS = "sms";
    static final String TABLE_RAW = "raw";
    private static final String TABLE_SR_PENDING = "sr_pending";
    private static final String TABLE_WORDS = "words";
    static final String VIEW_SMS_RESTRICTED = "sms_restricted";

    private static final Integer ONE = Integer.valueOf(1);

    private static final String[] CONTACT_QUERY_PROJECTION =
            new String[] { Contacts.Phones.PERSON_ID };
    private static final int PERSON_ID_COLUMN = 0;

    /** Delete any raw messages or message segments marked deleted that are older than an hour. */
    static final long RAW_MESSAGE_EXPIRE_AGE_MS = (long) (60 * 60 * 1000);
    //489223 begin
    private static final int INDEX_SMS_ID = 1;
    private static final int INDEX_SMS_THREAD_ID = 2;
    private static final int INDEX_SMS_ADDRESS = 3;
    private static final int INDEX_SMS_PERSON = 4;
    private static final int INDEX_SMS_DATE = 5;
    private static final int INDEX_SMS_DATE_SENT = 6;
    private static final int INDEX_SMS_PROTOCOL = 7;
    private static final int INDEX_SMS_READ = 8;
    private static final int INDEX_SMS_STATUS = 9;
    private static final int INDEX_SMS_TYPE = 10;
    private static final int INDEX_SMS_REPLY_PATH_PRESENT = 11;
    private static final int INDEX_SMS_SUBJECT = 12;
    private static final int INDEX_SMS_BODY = 13;
    private static final int INDEX_SMS_SERVICE_CENTER = 14;
    private static final int INDEX_SMS_LOCKED = 15;
    private static final int INDEX_SMS_SUBID = 16;
    private static final int INDEX_SMS_ERRORCODE = 17;
    private static final int INDEX_SMS_CREATOR = 18;
    private static final int INDEX_SMS_SEEN = 19;
    //489223 end

    /**
     * These are the columns that are available when reading SMS
     * messages from the ICC.  Columns whose names begin with "is_"
     * have either "true" or "false" as their values.
     */
    private final static String[] ICC_COLUMNS = new String[] {
        // N.B.: These columns must appear in the same order as the
        // calls to add appear in convertIccToSms.
        "service_center_address",       // getServiceCenterAddress
        "address",                      // getDisplayOriginatingAddress
        "message_class",                // getMessageClass
        "body",                         // getDisplayMessageBody
        "date",                         // getTimestampMillis
        "status",                       // getStatusOnIcc
        "index_on_icc",                 // getIndexOnIcc
        "is_status_report",             // isStatusReportMessage
        "transport_type",               // Always "sms".
        "type",                         // Always MESSAGE_TYPE_ALL.
        "locked",                       // Always 0 (false).
        "error_code",                   // Always 0
        "_id"
    };

    @Override
    public boolean onCreate() {
        setAppOps(AppOpsManager.OP_READ_SMS, AppOpsManager.OP_WRITE_SMS);
        // So we have two database files. One in de, one in ce. Here only "raw" table is in
        // mDeOpenHelper, other tables are all in mCeOpenHelper.
        mDeOpenHelper = MmsSmsDatabaseHelper.getInstanceForDe(getContext());
        mCeOpenHelper = MmsSmsDatabaseHelper.getInstanceForCe(getContext());
        TelephonyBackupAgent.DeferredSmsMmsRestoreService.startIfFilesExist(getContext());
        //489223 begin
        Log.d(TAG, " first init iccmanager");
        ICCMessageManager.getInstance().setContext(getContext());
        SQLiteDatabase db = MmsSmsDatabaseHelper.getInstance(getContext()).getReadableDatabase();
        ICCMessageManager.getInstance().CreateStructure(db);
        ICCMessageManager.getInstance().initDelayHandler();
        registeReceiverForSimsms();
        //489223 end
        return true;
    }

    private void registeReceiverForSimsms() {
        Log.d(TAG, " registerReceiver SIMStateChangedReceiver");
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        filter.addAction("android.intent.action.BOOT_COMPLETED");
        filter.setPriority(Integer.MAX_VALUE);
        getContext().registerReceiver(new ProvidersReceiver(), filter);
    }

    /**
     * Return the proper view of "sms" table for the current access status.
     *
     * @param accessRestricted If the access is restricted
     * @return the table/view name of the "sms" data
     */
    public static String getSmsTable(boolean accessRestricted) {
        return accessRestricted ? VIEW_SMS_RESTRICTED : TABLE_SMS;
    }

    @Override
    public Cursor query(Uri url, String[] projectionIn, String selection,
            String[] selectionArgs, String sort) {
        // First check if a restricted view of the "sms" table should be used based on the
        // caller's identity. Only system, phone or the default sms app can have full access
        // of sms data. For other apps, we present a restricted view which only contains sent
        // or received messages.
        final boolean accessRestricted = ProviderUtil.isAccessRestricted(
                getContext(), getCallingPackage(), Binder.getCallingUid());
        final String smsTable = getSmsTable(accessRestricted);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        // Generate the body of the query.
        int match = sURLMatcher.match(url);
        SQLiteDatabase db = getReadableDatabase(match);
        //489223 begin
        Log.d(TAG, "Query url=" + url + ", match=" + match);
        //489223 end
        switch (match) {
            case SMS_ALL:
                constructQueryForBox(qb, Sms.MESSAGE_TYPE_ALL, smsTable);
                break;

            case SMS_UNDELIVERED:
                constructQueryForUndelivered(qb, smsTable);
                break;

            case SMS_FAILED:
                constructQueryForBox(qb, Sms.MESSAGE_TYPE_FAILED, smsTable);
                break;

            case SMS_QUEUED:
                constructQueryForBox(qb, Sms.MESSAGE_TYPE_QUEUED, smsTable);
                break;

            case SMS_INBOX:
                constructQueryForBox(qb, Sms.MESSAGE_TYPE_INBOX, smsTable);
                break;

            case SMS_SENT:
                constructQueryForBox(qb, Sms.MESSAGE_TYPE_SENT, smsTable);
                break;

            case SMS_DRAFT:
                constructQueryForBox(qb, Sms.MESSAGE_TYPE_DRAFT, smsTable);
                break;

            case SMS_OUTBOX:
                constructQueryForBox(qb, Sms.MESSAGE_TYPE_OUTBOX, smsTable);
                break;
            case SMS_ALARM:
				//613227
                constructQueryForBox(qb, SmsEx.MESSAGE_TYPE_ALARM, smsTable);
                
                break;

            case SMS_ALL_ID:
                qb.setTables(smsTable);
                qb.appendWhere("(_id = " + url.getPathSegments().get(0) + ")");
                break;

            case SMS_INBOX_ID:
            case SMS_FAILED_ID:
            case SMS_SENT_ID:
            case SMS_DRAFT_ID:
            case SMS_OUTBOX_ID:
            case SMS_ALARM_ID:
                qb.setTables(smsTable);
                qb.appendWhere("(_id = " + url.getPathSegments().get(1) + ")");
                break;

            case SMS_CONVERSATIONS_ID:
                int threadID;

                try {
                    threadID = Integer.parseInt(url.getPathSegments().get(1));
                    if (Log.isLoggable(TAG, Log.VERBOSE)) {
                        Log.d(TAG, "query conversations: threadID=" + threadID);
                    }
                }
                catch (Exception ex) {
                    Log.e(TAG,
                          "Bad conversation thread id: "
                          + url.getPathSegments().get(1));
                    return null;
                }

                qb.setTables(smsTable);
                qb.appendWhere("thread_id = " + threadID);
                break;

            case SMS_CONVERSATIONS:
                qb.setTables(smsTable + ", "
                        + "(SELECT thread_id AS group_thread_id, "
                        + "MAX(date) AS group_date, "
                        + "COUNT(*) AS msg_count "
                        + "FROM " + smsTable + " "
                        + "GROUP BY thread_id) AS groups");
                qb.appendWhere(smsTable + ".thread_id=groups.group_thread_id"
                        + " AND " + smsTable + ".date=groups.group_date");
                final HashMap<String, String> projectionMap = new HashMap<>();
                projectionMap.put(Sms.Conversations.SNIPPET,
                        smsTable + ".body AS snippet");
                projectionMap.put(Sms.Conversations.THREAD_ID,
                        smsTable + ".thread_id AS thread_id");
                projectionMap.put(Sms.Conversations.MESSAGE_COUNT,
                        "groups.msg_count AS msg_count");
                projectionMap.put("delta", null);
                qb.setProjectionMap(projectionMap);
                break;

            case SMS_RAW_MESSAGE:
                // before querying purge old entries with deleted = 1
                purgeDeletedMessagesInRawTable(db);
                qb.setTables("raw");
                break;

            case SMS_STATUS_PENDING:
                qb.setTables("sr_pending");
                break;

            case SMS_ATTACHMENT:
                qb.setTables("attachments");
                break;

            case SMS_ATTACHMENT_ID:
                qb.setTables("attachments");
                qb.appendWhere(
                        "(sms_id = " + url.getPathSegments().get(1) + ")");
                break;

            case SMS_QUERY_THREAD_ID:
                qb.setTables("canonical_addresses");
                if (projectionIn == null) {
                    projectionIn = sIDProjection;
                }
                break;

            case SMS_STATUS_ID:
                qb.setTables(smsTable);
                qb.appendWhere("(_id = " + url.getPathSegments().get(1) + ")");
                break;
            //489223 begin
/*
            case SMS_ALL_ICC:
                return getAllMessagesFromIcc();

            case SMS_ICC:
                String messageIndexString = url.getPathSegments().get(1);

                return getSingleMessageFromIcc(messageIndexString);
*/
            default:
                Log.d(TAG, "Enter default [" + url + "]");
                if (GetPlus() != null) {
                    Log.d(TAG, "Start Plus Query");
                    return GetPlus().query(url, projectionIn, selection, selectionArgs, sort);
                } else {
                    Log.d(TAG, "Invalid request: " + url);
                    //return null;
                }
                //Log.e(TAG, "Invalid request: " + url);
                //489223 eng
                return null;
        }

        String orderBy = null;

        if (!TextUtils.isEmpty(sort)) {
            orderBy = sort;
        } else if (qb.getTables().equals(smsTable)) {
            orderBy = Sms.DEFAULT_SORT_ORDER;
        }

        Cursor ret = qb.query(db, projectionIn, selection, selectionArgs,
                              null, null, orderBy);

        // TODO: Since the URLs are a mess, always use content://sms
        ret.setNotificationUri(getContext().getContentResolver(),
                NOTIFICATION_URI);
        return ret;
    }

    private void purgeDeletedMessagesInRawTable(SQLiteDatabase db) {
        long oldTimestamp = System.currentTimeMillis() - RAW_MESSAGE_EXPIRE_AGE_MS;
        int num = db.delete(TABLE_RAW, "deleted = 1 AND date < " + oldTimestamp, null);
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.d(TAG, "purgeDeletedMessagesInRawTable: num rows older than " + oldTimestamp +
                    " purged: " + num);
        }
    }

    private SQLiteOpenHelper getDBOpenHelper(int match) {
        // Raw table is stored on de database. Other tables are stored in ce database.
        if (match == SMS_RAW_MESSAGE || match == SMS_RAW_MESSAGE_PERMANENT_DELETE) {
            return mDeOpenHelper;
        }
        return mCeOpenHelper;
    }

    private Object[] convertIccToSms(SmsMessage message, int id) {
        // N.B.: These calls must appear in the same order as the
        // columns appear in ICC_COLUMNS.
        Object[] row = new Object[13];
        row[0] = message.getServiceCenterAddress();
        row[1] = message.getDisplayOriginatingAddress();
        row[2] = String.valueOf(message.getMessageClass());
        row[3] = message.getDisplayMessageBody();
        row[4] = message.getTimestampMillis();
        row[5] = Sms.STATUS_NONE;
        row[6] = message.getIndexOnIcc();
        row[7] = message.isStatusReportMessage();
        row[8] = "sms";
        row[9] = TextBasedSmsColumns.MESSAGE_TYPE_ALL;
        row[10] = 0;      // locked
        row[11] = 0;      // error_code
        row[12] = id;
        return row;
    }

    /**
     * Return a Cursor containing just one message from the ICC.
     */
    private Cursor getSingleMessageFromIcc(String messageIndexString) {
        int messageIndex = -1;
        try {
            Integer.parseInt(messageIndexString);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Bad SMS ICC ID: " + messageIndexString);
        }
        ArrayList<SmsMessage> messages;
        final SmsManager smsManager = SmsManager.getDefault();
        // Use phone id to avoid AppOps uid mismatch in telephony
        long token = Binder.clearCallingIdentity();
        try {
            messages = smsManager.getAllMessagesFromIcc();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
        if (messages == null) {
            throw new IllegalArgumentException("ICC message not retrieved");
        }
        final SmsMessage message = messages.get(messageIndex);
        if (message == null) {
            throw new IllegalArgumentException(
                    "Message not retrieved. ID: " + messageIndexString);
        }
        MatrixCursor cursor = new MatrixCursor(ICC_COLUMNS, 1);
        cursor.addRow(convertIccToSms(message, 0));
        return withIccNotificationUri(cursor);
    }

    /**
     * Return a Cursor listing all the messages stored on the ICC.
     */
    private Cursor getAllMessagesFromIcc() {
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<SmsMessage> messages;

        // use phone app permissions to avoid UID mismatch in AppOpsManager.noteOp() call
        long token = Binder.clearCallingIdentity();
        try {
            messages = smsManager.getAllMessagesFromIcc();
        } finally {
            Binder.restoreCallingIdentity(token);
        }

        final int count = messages.size();
        MatrixCursor cursor = new MatrixCursor(ICC_COLUMNS, count);
        for (int i = 0; i < count; i++) {
            SmsMessage message = messages.get(i);
            if (message != null) {
                cursor.addRow(convertIccToSms(message, i));
            }
        }
        return withIccNotificationUri(cursor);
    }

    private Cursor withIccNotificationUri(Cursor cursor) {
        cursor.setNotificationUri(getContext().getContentResolver(), ICC_URI);
        return cursor;
    }

    private void constructQueryForBox(SQLiteQueryBuilder qb, int type, String smsTable) {
        qb.setTables(smsTable);

        if (type != Sms.MESSAGE_TYPE_ALL) {
            qb.appendWhere("type=" + type);
        }
    }

    private void constructQueryForUndelivered(SQLiteQueryBuilder qb, String smsTable) {
        qb.setTables(smsTable);

        qb.appendWhere("(type=" + Sms.MESSAGE_TYPE_OUTBOX +
                       " OR type=" + Sms.MESSAGE_TYPE_FAILED +
                       " OR type=" + Sms.MESSAGE_TYPE_QUEUED + ")");
    }

    @Override
    public String getType(Uri url) {
        switch (url.getPathSegments().size()) {
        case 0:
            return VND_ANDROID_DIR_SMS;
            case 1:
                try {
                    Integer.parseInt(url.getPathSegments().get(0));
                    return VND_ANDROID_SMS;
                } catch (NumberFormatException ex) {
                    return VND_ANDROID_DIR_SMS;
                }
            case 2:
                // TODO: What about "threadID"?
                if (url.getPathSegments().get(0).equals("conversations")) {
                    return VND_ANDROID_SMSCHAT;
                } else {
                    return VND_ANDROID_SMS;
                }
        }
        return null;
    }

    @Override
    public int bulkInsert(@NonNull Uri url, @NonNull ContentValues[] values) {
        final int callerUid = Binder.getCallingUid();
        final String callerPkg = getCallingPackage();
        long token = Binder.clearCallingIdentity();
        try {
            int messagesInserted = 0;
            for (ContentValues initialValues : values) {
                Uri insertUri = insertInner(url, initialValues, callerUid, callerPkg);
                if (insertUri != null) {
                    messagesInserted++;
                }
            }

            // The raw table is used by the telephony layer for storing an sms before
            // sending out a notification that an sms has arrived. We don't want to notify
            // the default sms app of changes to this table.
            final boolean notifyIfNotDefault = sURLMatcher.match(url) != SMS_RAW_MESSAGE;
            notifyChange(notifyIfNotDefault, url, callerPkg);
            return messagesInserted;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    //489223 begin
    private static final String[] forIccProjection = new String[]{
            "_id",
            "sub_id",
            "service_center",
            Telephony.Sms.DATE,
            Telephony.Sms.BODY,
            "address",
            "read",
            "type"
    };

    private Cursor findOutSmsCursor(Uri url, int match) {//for bug 844673

        final String smsTable = TABLE_SMS;
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(smsTable);
        qb.appendWhere("(_id = " + url.getPathSegments().get(0) + ")");
        SQLiteDatabase db = getDBOpenHelper(match).getReadableDatabase();//for bug 844673
        String orderBy = null;
        orderBy = Sms.DEFAULT_SORT_ORDER;
        Log.d(TAG, "get(0):[" + url.getPathSegments().get(0) + "]");
        Cursor ret = qb.query(db, forIccProjection, "_id=" + url.getPathSegments().get(0), null,
                null, null, orderBy);
        return ret;

    }

    //489223 end
    @Override
    public Uri insert(Uri url, ContentValues initialValues) {
        final int callerUid = Binder.getCallingUid();
        final String callerPkg = getCallingPackage();
        long token = Binder.clearCallingIdentity();
        try {
            Uri insertUri = insertInner(url, initialValues, callerUid, callerPkg);

            // The raw table is used by the telephony layer for storing an sms before
            // sending out a notification that an sms has arrived. We don't want to notify
            // the default sms app of changes to this table.
            final boolean notifyIfNotDefault = sURLMatcher.match(url) != SMS_RAW_MESSAGE;
            notifyChange(notifyIfNotDefault, insertUri, callerPkg);
            return insertUri;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private Uri insertInner(Uri url, ContentValues initialValues, int callerUid, String callerPkg) {
        ContentValues values;
        long rowID;
        int type = Sms.MESSAGE_TYPE_ALL;

        int match = sURLMatcher.match(url);
        String table = TABLE_SMS;
        boolean notifyIfNotDefault = true;
        //489223 begin
        Log.d(TAG, "insertInner url=" + url + ", match=" + match);
        //489223 end
        switch (match) {
            case SMS_ALL:
                Integer typeObj = initialValues.getAsInteger(Sms.TYPE);
                if (typeObj != null) {
                    type = typeObj.intValue();
                    Log.d(TAG, "insertInner type=" + type);
                } else {
                    // default to inbox
                    type = Sms.MESSAGE_TYPE_INBOX;
                }
                break;

            case SMS_INBOX:
                type = Sms.MESSAGE_TYPE_INBOX;
                break;

            case SMS_FAILED:
                type = Sms.MESSAGE_TYPE_FAILED;
                break;

            case SMS_QUEUED:
                type = Sms.MESSAGE_TYPE_QUEUED;
                break;

            case SMS_SENT:
                type = Sms.MESSAGE_TYPE_SENT;
                break;

            case SMS_DRAFT:
                type = Sms.MESSAGE_TYPE_DRAFT;
                break;

            case SMS_OUTBOX:
                type = Sms.MESSAGE_TYPE_OUTBOX;
                break;
            case SMS_ALARM:
				//613227
                type = SmsEx.MESSAGE_TYPE_ALARM;
                break;

            case SMS_RAW_MESSAGE:
                table = "raw";
                // The raw table is used by the telephony layer for storing an sms before
                // sending out a notification that an sms has arrived. We don't want to notify
                // the default sms app of changes to this table.
                notifyIfNotDefault = false;
                break;

            case SMS_STATUS_PENDING:
                table = "sr_pending";
                break;

            case SMS_ATTACHMENT:
                table = "attachments";
                break;

            case SMS_NEW_THREAD_ID:
                table = "canonical_addresses";
                break;
            //489223 begin
            case SMS_ALL_ID:

                Cursor sms_cursor = findOutSmsCursor(url, match);//for bug 844673
                if (sms_cursor != null) {
                    Log.d(TAG, "sms_cursor count[:" + sms_cursor.getCount() + "]");
                    if (sms_cursor.moveToFirst()) {
                        Log.d(TAG, "sms_cursor subid:[" + sms_cursor.getString(1) + "]");
                        Log.d(TAG, "sms_cursor service center address:[" + sms_cursor.getString(2) + "]");
                        Log.d(TAG, "sms_cursor date:[" + sms_cursor.getString(3) + "]");
                        Log.d(TAG, "sms_cursor body:[" + sms_cursor.getString(4) + "]");
                        Log.d(TAG, "sms_cursor address:[" + sms_cursor.getString(5) + "]");
                        int read = sms_cursor.getInt(6);//read
                        int smsType = sms_cursor.getInt(7);//type
                        int status = 0;
                        Log.d(TAG, "sms_cursor smsType:[" + smsType + "]");
                        if (smsType == Sms.MESSAGE_TYPE_SENT) {
                            status = SmsManager.STATUS_ON_ICC_SENT;
                        } else if (smsType == Sms.MESSAGE_TYPE_INBOX) {
                                status = SmsManager.STATUS_ON_ICC_READ;
                        }
                        initialValues.put(ICCMessageManager.SERVICE_CENTER, sms_cursor.getString(2));
                        initialValues.put(ICCMessageManager.DATE, sms_cursor.getString(3));
                        initialValues.put(ICCMessageManager.BODY, sms_cursor.getString(4));
                        initialValues.put(ICCMessageManager.ADDRESS, sms_cursor.getString(5));
                        initialValues.put(ICCMessageManager.STATUS, status);
                    }
                } else {
                    Log.e(TAG, "sms_cursor is null ");
                }
                if (sms_cursor != null) {
                    sms_cursor.close();
                }
            default:

                if (GetPlus() != null) {
                    Log.d(TAG, "GetPlus().insert :[" + url + "]");
                    return GetPlus().insert(url, initialValues);
                } else {
                    Log.e(TAG, "Invalid request: " + url);
                }
                //Log.e(TAG, "Invalid request: " + url);
                //489223 end
                return null;
        }

        SQLiteDatabase db = getWritableDatabase(match);

        if (table.equals(TABLE_SMS)) {
            boolean addDate = false;
            boolean addType = false;

            // Make sure that the date and type are set
            if (initialValues == null) {
                values = new ContentValues(1);
                addDate = true;
                addType = true;
            } else {
                values = new ContentValues(initialValues);

                if (!initialValues.containsKey(Sms.DATE)) {
                    addDate = true;
                }

                if (!initialValues.containsKey(Sms.TYPE)) {
                    addType = true;
                }
            }

            if (addDate) {
                values.put(Sms.DATE, new Long(System.currentTimeMillis()));
            }

            if (addType && (type != Sms.MESSAGE_TYPE_ALL)) {
                values.put(Sms.TYPE, Integer.valueOf(type));
            }

            // thread_id
            Long threadId = values.getAsLong(Sms.THREAD_ID);
            String address = values.getAsString(Sms.ADDRESS);

            if (((threadId == null) || (threadId == 0)) && (!TextUtils.isEmpty(address))) {
                values.put(Sms.THREAD_ID, Threads.getOrCreateThreadId(
                                   getContext(), address));
            }

            // If this message is going in as a draft, it should replace any
            // other draft messages in the thread.  Just delete all draft
            // messages with this thread ID.  We could add an OR REPLACE to
            // the insert below, but we'd have to query to find the old _id
            // to produce a conflict anyway.
            if (values.getAsInteger(Sms.TYPE) == Sms.MESSAGE_TYPE_DRAFT) {
                db.delete(TABLE_SMS, "thread_id=? AND type=?",
                        new String[] { values.getAsString(Sms.THREAD_ID),
                                       Integer.toString(Sms.MESSAGE_TYPE_DRAFT) });
            }

            if (type == Sms.MESSAGE_TYPE_INBOX) {
                // Look up the person if not already filled in.
                if ((values.getAsLong(Sms.PERSON) == null) && (!TextUtils.isEmpty(address))) {
                    Cursor cursor = null;
                    Uri uri = Uri.withAppendedPath(Contacts.Phones.CONTENT_FILTER_URL,
                            Uri.encode(address));
                    try {
                        cursor = getContext().getContentResolver().query(
                                uri,
                                CONTACT_QUERY_PROJECTION,
                                null, null, null);

                        if (cursor.moveToFirst()) {
                            Long id = Long.valueOf(cursor.getLong(PERSON_ID_COLUMN));
                            values.put(Sms.PERSON, id);
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "insert: query contact uri " + uri + " caught ", ex);
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }
            } else {
                // Mark all non-inbox messages read.
                values.put(Sms.READ, ONE);
            }
            if (ProviderUtil.shouldSetCreator(values, callerUid)) {
                // Only SYSTEM or PHONE can set CREATOR
                // If caller is not SYSTEM or PHONE, or SYSTEM or PHONE does not set CREATOR
                // set CREATOR using the truth on caller.
                // Note: Inferring package name from UID may include unrelated package names
                values.put(Sms.CREATOR, callerPkg);
            }
        } else {
            if (initialValues == null) {
                values = new ContentValues(1);
            } else {
                values = initialValues;
            }
        }

        rowID = db.insert(table, "body", values);

        // Don't use a trigger for updating the words table because of a bug
        // in FTS3.  The bug is such that the call to get the last inserted
        // row is incorrect.
        if (table == TABLE_SMS) {
            // Update the words table with a corresponding row.  The words table
            // allows us to search for words quickly, without scanning the whole
            // table;
            ContentValues cv = new ContentValues();
            cv.put(Telephony.MmsSms.WordsTable.ID, rowID);
            cv.put(Telephony.MmsSms.WordsTable.INDEXED_TEXT, values.getAsString("body"));
            cv.put(Telephony.MmsSms.WordsTable.SOURCE_ROW_ID, rowID);
            cv.put(Telephony.MmsSms.WordsTable.TABLE_ID, 1);
            db.insert(TABLE_WORDS, Telephony.MmsSms.WordsTable.INDEXED_TEXT, cv);
        }
        if (rowID > 0) {
            Uri uri;
        //762709 begin
            if (table == TABLE_SMS) {
                uri = Uri.withAppendedPath(url, ""+rowID);
            } else {
                uri = Uri.withAppendedPath(url,table + "/" + rowID );
            }
            //if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.d(TAG, "insert " + uri + " succeeded");
            //}
        //762709 end
            return uri;
        } else {
            Log.e(TAG, "insert: failed!");
        }

        return null;
    }

    @Override
    public int delete(Uri url, String where, String[] whereArgs) {
        int count;
        int match = sURLMatcher.match(url);
        SQLiteDatabase db = getWritableDatabase(match);
        //489223 begin
        Log.d(TAG, "match:[" + match + "] where:[" + where + "]");
        //489233 end

        boolean notifyIfNotDefault = true;
        switch (match) {
            case SMS_ALL:
                count = db.delete(TABLE_SMS, where, whereArgs);
                if (count != 0) {
                    // Don't update threads unless something changed.
                    MmsSmsDatabaseHelper.updateThreads(db, where, whereArgs);
                }
                break;

            case SMS_ALL_ID:
                try {
                    int message_id = Integer.parseInt(url.getPathSegments().get(0));
                    count = MmsSmsDatabaseHelper.deleteOneSms(db, message_id);
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                        "Bad message id: " + url.getPathSegments().get(0));
                }
                break;

            case SMS_CONVERSATIONS_ID:
                int threadID;

                try {
                    threadID = Integer.parseInt(url.getPathSegments().get(1));
                } catch (Exception ex) {
                    throw new IllegalArgumentException(
                            "Bad conversation thread id: "
                            + url.getPathSegments().get(1));
                }

                // delete the messages from the sms table
                where = DatabaseUtils.concatenateWhere("thread_id=" + threadID, where);
                count = db.delete(TABLE_SMS, where, whereArgs);
                MmsSmsDatabaseHelper.updateThread(db, threadID);
                break;

            case SMS_RAW_MESSAGE:
                ContentValues cv = new ContentValues();
                cv.put("deleted", 1);
                count = db.update(TABLE_RAW, cv, where, whereArgs);
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.d(TAG, "delete: num rows marked deleted in raw table: " + count);
                }
                notifyIfNotDefault = false;
                break;

            case SMS_RAW_MESSAGE_PERMANENT_DELETE:
                count = db.delete(TABLE_RAW, where, whereArgs);
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.d(TAG, "delete: num rows permanently deleted in raw table: " + count);
                }
                notifyIfNotDefault = false;
                break;

            case SMS_STATUS_PENDING:
                count = db.delete("sr_pending", where, whereArgs);
                break;
            //489223 begin
/*
            case SMS_ICC:
                String messageIndexString = url.getPathSegments().get(1);

                return deleteMessageFromIcc(messageIndexString);
                */
            default:
                if (GetPlus() != null) {
                    Log.d(TAG, "going to GetPlus().delete");
                    return GetPlus().delete(url, where, whereArgs);
                }
            //489223 end
                throw new IllegalArgumentException("Unknown URL");
        }

        if (count > 0) {
            notifyChange(notifyIfNotDefault, url, getCallingPackage());
        }
        return count;
    }

    /**
     * Delete the message at index from ICC.  Return true iff
     * successful.
     */
    private int deleteMessageFromIcc(String messageIndexString) {
        SmsManager smsManager = SmsManager.getDefault();
        // Use phone id to avoid AppOps uid mismatch in telephony
        long token = Binder.clearCallingIdentity();
        try {
            return smsManager.deleteMessageFromIcc(
                    Integer.parseInt(messageIndexString))
                    ? 1 : 0;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Bad SMS ICC ID: " + messageIndexString);
        } finally {
            ContentResolver cr = getContext().getContentResolver();
            cr.notifyChange(ICC_URI, null, true, UserHandle.USER_ALL);

            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public int update(Uri url, ContentValues values, String where, String[] whereArgs) {
        final int callerUid = Binder.getCallingUid();
        final String callerPkg = getCallingPackage();
        int count = 0;
        String table = TABLE_SMS;
        String extraWhere = null;
        boolean notifyIfNotDefault = true;
        int match = sURLMatcher.match(url);
        SQLiteDatabase db = getWritableDatabase(match);
        //489223 begin
        Log.d(TAG, "update url=" + url);
        //489223 end
        switch (match) {
            case SMS_RAW_MESSAGE:
                table = TABLE_RAW;
                notifyIfNotDefault = false;
                break;

            case SMS_STATUS_PENDING:
                table = TABLE_SR_PENDING;
                break;

            case SMS_ALL:
            case SMS_FAILED:
            case SMS_QUEUED:
            case SMS_INBOX:
            case SMS_SENT:
            case SMS_DRAFT:
            case SMS_OUTBOX:
			//613227
            case SMS_ALARM:
            case SMS_CONVERSATIONS:
                break;

            case SMS_ALL_ID:
                extraWhere = "_id=" + url.getPathSegments().get(0);
                break;

            case SMS_INBOX_ID:
            case SMS_FAILED_ID:
            case SMS_SENT_ID:
            case SMS_DRAFT_ID:
            case SMS_OUTBOX_ID:
            case SMS_ALARM_ID:
                extraWhere = "_id=" + url.getPathSegments().get(1);
                break;

            case SMS_CONVERSATIONS_ID: {
                String threadId = url.getPathSegments().get(1);

                try {
                    Integer.parseInt(threadId);
                } catch (Exception ex) {
                    Log.e(TAG, "Bad conversation thread id: " + threadId);
                    break;
                }

                extraWhere = "thread_id=" + threadId;
                break;
            }

            case SMS_STATUS_ID:
                extraWhere = "_id=" + url.getPathSegments().get(1);
                break;

            default:
                //489223 begin
                if (GetPlus() != null) {
                    return GetPlus().update(url, values, where, whereArgs);
                } else {
                    throw new UnsupportedOperationException(
                            "URI " + url + " not supported");
                }
                //489223 end
        }

        if (table.equals(TABLE_SMS) && ProviderUtil.shouldRemoveCreator(values, callerUid)) {
            // CREATOR should not be changed by non-SYSTEM/PHONE apps
            Log.w(TAG, callerPkg + " tries to update CREATOR");
            values.remove(Sms.CREATOR);
        }

        where = DatabaseUtils.concatenateWhere(where, extraWhere);
        /* Modify by SPRD for bug 543275 Start android N update start*/
        try {
            count = db.update(table, values, where, whereArgs);
        } catch (SQLiteException e) {
            Log.e(TAG, "Exception occured when perform update: " + e.getMessage());
        }
        /* Modify by SPRD for bug 543275 End android N update end*/
        if (count > 0) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.d(TAG, "update " + url + " succeeded");
            }
            notifyChange(notifyIfNotDefault, url, callerPkg);
        }
        return count;
    }

    //489223 begin
    protected IDatabaseOperate GetPlus() {
        return ICCMessageManager.getInstance();
    }

    public void SetPlus(IDatabaseOperate iplus) {
        mIplus = iplus;
    }

    private IDatabaseOperate mIplus = null; //new com.android.providers.telephony.ext.simmessage.ICCMessageManager();
    //489223 end

    private void notifyChange(boolean notifyIfNotDefault, Uri uri, final String callingPackage) {
        final Context context = getContext();
        ContentResolver cr = context.getContentResolver();
        if (uri != null) {
            cr.notifyChange(uri, null, true, UserHandle.USER_ALL);
        }
        cr.notifyChange(MmsSms.CONTENT_URI, null, true, UserHandle.USER_ALL);
        cr.notifyChange(Uri.parse("content://mms-sms/conversations/"), null, true,
                UserHandle.USER_ALL);
        if (notifyIfNotDefault) {
            ProviderUtil.notifyIfNotDefaultSmsApp(uri, callingPackage, context);
        }
    }

    // Db open helper for tables stored in CE(Credential Encrypted) storage.
    @VisibleForTesting
    public SQLiteOpenHelper mCeOpenHelper;
    // Db open helper for tables stored in DE(Device Encrypted) storage. It's currently only used
    // to store raw table.
    @VisibleForTesting
    public SQLiteOpenHelper mDeOpenHelper;

    private final static String TAG = "SmsProvider";
    private final static String VND_ANDROID_SMS = "vnd.android.cursor.item/sms";
    private final static String VND_ANDROID_SMSCHAT =
            "vnd.android.cursor.item/sms-chat";
    private final static String VND_ANDROID_DIR_SMS =
            "vnd.android.cursor.dir/sms";

    private static final String[] sIDProjection = new String[] { "_id" };

    private static final int SMS_ALL = 0;
    private static final int SMS_ALL_ID = 1;
    private static final int SMS_INBOX = 2;
    private static final int SMS_INBOX_ID = 3;
    private static final int SMS_SENT = 4;
    private static final int SMS_SENT_ID = 5;
    private static final int SMS_DRAFT = 6;
    private static final int SMS_DRAFT_ID = 7;
    private static final int SMS_OUTBOX = 8;
    private static final int SMS_OUTBOX_ID = 9;
    private static final int SMS_CONVERSATIONS = 10;
    private static final int SMS_CONVERSATIONS_ID = 11;
    private static final int SMS_RAW_MESSAGE = 15;
    private static final int SMS_ATTACHMENT = 16;
    private static final int SMS_ATTACHMENT_ID = 17;
    private static final int SMS_NEW_THREAD_ID = 18;
    private static final int SMS_QUERY_THREAD_ID = 19;
    private static final int SMS_STATUS_ID = 20;
    private static final int SMS_STATUS_PENDING = 21;
    private static final int SMS_ALL_ICC = 22;
    private static final int SMS_ICC = 23;
    private static final int SMS_FAILED = 24;
    private static final int SMS_FAILED_ID = 25;
    private static final int SMS_QUEUED = 26;
    private static final int SMS_UNDELIVERED = 27;
    private static final int SMS_RAW_MESSAGE_PERMANENT_DELETE = 28;
	//613227
    private static final int SMS_ALARM = 30;
    private static final int SMS_ALARM_ID = 31;
    private static final UriMatcher sURLMatcher =
            new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURLMatcher.addURI("sms", null, SMS_ALL);
        sURLMatcher.addURI("sms", "#", SMS_ALL_ID);
        sURLMatcher.addURI("sms", "inbox", SMS_INBOX);
        sURLMatcher.addURI("sms", "inbox/#", SMS_INBOX_ID);
        sURLMatcher.addURI("sms", "sent", SMS_SENT);
        sURLMatcher.addURI("sms", "sent/#", SMS_SENT_ID);
        sURLMatcher.addURI("sms", "draft", SMS_DRAFT);
        sURLMatcher.addURI("sms", "draft/#", SMS_DRAFT_ID);
        sURLMatcher.addURI("sms", "outbox", SMS_OUTBOX);
        sURLMatcher.addURI("sms", "outbox/#", SMS_OUTBOX_ID);
        sURLMatcher.addURI("sms", "undelivered", SMS_UNDELIVERED);
        sURLMatcher.addURI("sms", "failed", SMS_FAILED);
        sURLMatcher.addURI("sms", "failed/#", SMS_FAILED_ID);
        sURLMatcher.addURI("sms", "queued", SMS_QUEUED);
        sURLMatcher.addURI("sms", "conversations", SMS_CONVERSATIONS);
        sURLMatcher.addURI("sms", "conversations/*", SMS_CONVERSATIONS_ID);
        sURLMatcher.addURI("sms", "raw", SMS_RAW_MESSAGE);
        sURLMatcher.addURI("sms", "raw/permanentDelete", SMS_RAW_MESSAGE_PERMANENT_DELETE);
        sURLMatcher.addURI("sms", "attachments", SMS_ATTACHMENT);
        sURLMatcher.addURI("sms", "attachments/#", SMS_ATTACHMENT_ID);
        sURLMatcher.addURI("sms", "threadID", SMS_NEW_THREAD_ID);
        sURLMatcher.addURI("sms", "threadID/*", SMS_QUERY_THREAD_ID);
        sURLMatcher.addURI("sms", "status/#", SMS_STATUS_ID);
        sURLMatcher.addURI("sms", "sr_pending", SMS_STATUS_PENDING);
        sURLMatcher.addURI("sms", "icc", SMS_ALL_ICC);
        sURLMatcher.addURI("sms", "icc/#", SMS_ICC);
        //we keep these for not breaking old applications
        sURLMatcher.addURI("sms", "sim", SMS_ALL_ICC);
        sURLMatcher.addURI("sms", "sim/#", SMS_ICC);
        sURLMatcher.addURI("sms", "alarm", SMS_ALARM);
        sURLMatcher.addURI("sms", "alarm/#", SMS_ALARM_ID);
    }

    /**
     * These methods can be overridden in a subclass for testing SmsProvider using an
     * in-memory database.
     */
    SQLiteDatabase getReadableDatabase(int match) {
        return getDBOpenHelper(match).getReadableDatabase();
    }

    SQLiteDatabase getWritableDatabase(int match) {
        return  getDBOpenHelper(match).getWritableDatabase();
    }
}
