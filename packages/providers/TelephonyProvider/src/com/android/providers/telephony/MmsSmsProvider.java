/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.app.AppOpsManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.provider.Telephony.CanonicalAddressesColumns;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Conversations;
import android.provider.Telephony.Threads;
import android.provider.Telephony.ThreadsColumns;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.mms.pdu.PduHeaders;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
//bug 495194 : add for search feature begin android N update start
import android.os.Handler;
import android.os.Message;
import java.util.ArrayList;
import android.content.ContentResolver;
import com.android.providers.telephony.CharEscapeUtil;
import com.android.providers.telephony.SearchSyncHelper;
//bug 495194 : add for search feature end android N update end

/**
 * This class provides the ability to query the MMS and SMS databases
 * at the same time, mixing messages from both in a single thread
 * (A.K.A. conversation).
 *
 * A virtual column, MmsSms.TYPE_DISCRIMINATOR_COLUMN, may be
 * requested in the projection for a query.  Its value is either "mms"
 * or "sms", depending on whether the message represented by the row
 * is an MMS message or an SMS message, respectively.
 *
 * This class also provides the ability to find out what addresses
 * participated in a particular thread.  It doesn't support updates
 * for either of these.
 *
 * This class provides a way to allocate and retrieve thread IDs.
 * This is done atomically through a query.  There is no insert URI
 * for this.
 *
 * Finally, this class provides a way to delete or update all messages
 * in a thread.
 */
public class MmsSmsProvider extends ContentProvider {
    private static final UriMatcher URI_MATCHER =
            new UriMatcher(UriMatcher.NO_MATCH);
    private static final String LOG_TAG = "MmsSmsProvider";
    private static final boolean DEBUG = false;
    // bug 495194 : add for search feature begin android N update start
    private final String URI_CONVERSATION = "content://com.android.messaging.datamodel.MessagingContentProvider/conversation";
    // bug 495194 : add for search feature end android N update end
    private static final String NO_DELETES_INSERTS_OR_UPDATES =
            "MmsSmsProvider does not support deletes, inserts, or updates for this URI.";
    private static final int URI_CONVERSATIONS                     = 0;
    private static final int URI_CONVERSATIONS_MESSAGES            = 1;
    private static final int URI_CONVERSATIONS_RECIPIENTS          = 2;
    private static final int URI_MESSAGES_BY_PHONE                 = 3;
    private static final int URI_THREAD_ID                         = 4;
    private static final int URI_CANONICAL_ADDRESS                 = 5;
    private static final int URI_PENDING_MSG                       = 6;
    private static final int URI_COMPLETE_CONVERSATIONS            = 7;
    private static final int URI_UNDELIVERED_MSG                   = 8;
    private static final int URI_CONVERSATIONS_SUBJECT             = 9;
    private static final int URI_NOTIFICATIONS                     = 10;
    private static final int URI_OBSOLETE_THREADS                  = 11;
    private static final int URI_DRAFT                             = 12;
    private static final int URI_CANONICAL_ADDRESSES               = 13;
    private static final int URI_SEARCH                            = 14;
    private static final int URI_SEARCH_SUGGEST                    = 15;
    private static final int URI_FIRST_LOCKED_MESSAGE_ALL          = 16;
    private static final int URI_FIRST_LOCKED_MESSAGE_BY_THREAD_ID = 17;
    private static final int URI_MESSAGE_ID_TO_THREAD              = 18;
	//android N update start
    private static final int URI_SMS_MMS_SEARCH                    = 19;
    // bug 495194 : add for search feature begin
    private static final int URI_CREATE_TEMP_TABLE                 = 20;
    private static final int URI_DROP_TEMP_TABLE                   = 21;
    //add for bug 552039 begin
    private static final int URI_UPDATE_CONVERSATION_DRAFT         = 22;
    //add for bug 552039 end
    // bug 495194 : add for search feature end
    //sprd: fix for telcel bug 557685 begin
    private static final int URI_FILTER_FDN_CONTACTS               = 23;
    private static final int URI_FILTER_lOCAL_CONTACTS             = 24;
    private static final int URI_FILTER_CUFUSE_FDN_CONTACTS        = 25;
    //sprd: fix for telcel bug 557685 end
    //add for bug 566254 begin
    private static final int URI_PART                              = 26;
    //add for bug 566254 end
    private static final int URI_FILTER_FDN_CONTACTS_DATA          = 27;
	//android N update end
    /**
     * the name of the table that is used to store the queue of
     * messages(both MMS and SMS) to be sent/downloaded.
     */
    public static final String TABLE_PENDING_MSG = "pending_msgs";

    /**
     * the name of the table that is used to store the canonical addresses for both SMS and MMS.
     */
    private static final String TABLE_CANONICAL_ADDRESSES = "canonical_addresses";

    /**
     * the name of the table that is used to store the conversation threads.
     */
    static final String TABLE_THREADS = "threads";

    // These constants are used to construct union queries across the
    // MMS and SMS base tables.

    // These are the columns that appear in both the MMS ("pdu") and
    // SMS ("sms") message tables.
    private static final String[] MMS_SMS_COLUMNS =
            { BaseColumns._ID, Mms.DATE, Mms.DATE_SENT, Mms.READ, Mms.THREAD_ID, Mms.LOCKED,
                    Mms.SUBSCRIPTION_ID };

    // These are the columns that appear only in the MMS message
    // table.
    private static final String[] MMS_ONLY_COLUMNS = {
        Mms.CONTENT_CLASS, Mms.CONTENT_LOCATION, Mms.CONTENT_TYPE,
        Mms.DELIVERY_REPORT, Mms.EXPIRY, Mms.MESSAGE_CLASS, Mms.MESSAGE_ID,
        Mms.MESSAGE_SIZE, Mms.MESSAGE_TYPE, Mms.MESSAGE_BOX, Mms.PRIORITY,
        Mms.READ_STATUS, Mms.RESPONSE_STATUS, Mms.RESPONSE_TEXT,
        Mms.RETRIEVE_STATUS, Mms.RETRIEVE_TEXT_CHARSET, Mms.REPORT_ALLOWED,
        Mms.READ_REPORT, Mms.STATUS, Mms.SUBJECT, Mms.SUBJECT_CHARSET,
        Mms.TRANSACTION_ID, Mms.MMS_VERSION, Mms.TEXT_ONLY };

    // These are the columns that appear only in the SMS message
    // table.
    private static final String[] SMS_ONLY_COLUMNS =
            { "address", "body", "person", "reply_path_present",
              "service_center", "status", "subject", "type", "error_code" };

    // These are all the columns that appear in the "threads" table.
    private static final String[] THREADS_COLUMNS = {
        BaseColumns._ID,
        ThreadsColumns.DATE,
        ThreadsColumns.RECIPIENT_IDS,
        ThreadsColumns.MESSAGE_COUNT
    };

    private static final String[] CANONICAL_ADDRESSES_COLUMNS_1 =
            new String[] { CanonicalAddressesColumns.ADDRESS };

    private static final String[] CANONICAL_ADDRESSES_COLUMNS_2 =
            new String[] { CanonicalAddressesColumns._ID,
                    CanonicalAddressesColumns.ADDRESS };

    // These are all the columns that appear in the MMS and SMS
    // message tables.
    private static final String[] UNION_COLUMNS =
            new String[MMS_SMS_COLUMNS.length
                       + MMS_ONLY_COLUMNS.length
                       + SMS_ONLY_COLUMNS.length];

    // These are all the columns that appear in the MMS table.
    private static final Set<String> MMS_COLUMNS = new HashSet<String>();

    // These are all the columns that appear in the SMS table.
    private static final Set<String> SMS_COLUMNS = new HashSet<String>();

    private static final String VND_ANDROID_DIR_MMS_SMS =
            "vnd.android-dir/mms-sms";

    private static final String[] ID_PROJECTION = { BaseColumns._ID };

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final String[] SEARCH_STRING = new String[1];
    private static final String SEARCH_QUERY = "SELECT snippet(words, '', ' ', '', 1, 1) as " +
            "snippet FROM words WHERE index_text MATCH ? ORDER BY snippet LIMIT 50;";

    private static final String SMS_CONVERSATION_CONSTRAINT = "(" +
            Sms.TYPE + " != " + Sms.MESSAGE_TYPE_DRAFT + ")";

    private static final String MMS_CONVERSATION_CONSTRAINT = "(" +
            Mms.MESSAGE_BOX + " != " + Mms.MESSAGE_BOX_DRAFTS + " AND (" +
            Mms.MESSAGE_TYPE + " = " + PduHeaders.MESSAGE_TYPE_SEND_REQ + " OR " +
            Mms.MESSAGE_TYPE + " = " + PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF + " OR " +
            Mms.MESSAGE_TYPE + " = " + PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND + "))";

    private static String getTextSearchQuery(String smsTable, String pduTable) {
        // Search on the words table but return the rows from the corresponding sms table
        final String smsQuery = "SELECT "
                + smsTable + "._id AS _id,"
                + "thread_id,"
                + "address,"
                + "body,"
                + "date,"
                + "date_sent,"
                + "index_text,"
                + "words._id "
                + "FROM " + smsTable + ",words "
                + "WHERE (index_text MATCH ? "
                + "AND " + smsTable + "._id=words.source_id "
                + "AND words.table_to_use=1)";

        // Search on the words table but return the rows from the corresponding parts table
        final String mmsQuery = "SELECT "
                + pduTable + "._id,"
                + "thread_id,"
                + "addr.address,"
                + "part.text AS body,"
                + pduTable + ".date,"
                + pduTable + ".date_sent,"
                + "index_text,"
                + "words._id "
                + "FROM " + pduTable + ",part,addr,words "
                + "WHERE ((part.mid=" + pduTable + "._id) "
                + "AND (addr.msg_id=" + pduTable + "._id) "
                + "AND (addr.type=" + PduHeaders.TO + ") "
                + "AND (part.ct='text/plain') "
                + "AND (index_text MATCH ?) "
                + "AND (part._id = words.source_id) "
                + "AND (words.table_to_use=2))";

        // This code queries the sms and mms tables and returns a unified result set
        // of text matches.  We query the sms table which is pretty simple.  We also
        // query the pdu, part and addr table to get the mms result.  Note we're
        // using a UNION so we have to have the same number of result columns from
        // both queries.
        return smsQuery + " UNION " + mmsQuery + " "
                + "GROUP BY thread_id "
                + "ORDER BY thread_id ASC, date DESC";
    }
    // bug 495194 : add for search feature begin android N update start
    //add for bug 543691 begin
    private static final String SMS_QUERY = "SELECT sms._id as _id,temp_conversation.conversation_id as conv_id, sms.thread_id as thread_id,temp_conversation.recipient_name as name,temp_conversation.recipient_address as address,sms.body as body, null as sub, 'sms' as msg_type, sms.type as box_type"
            + " FROM sms,words,threads,temp_conversation WHERE ( "
            + " sms._id=words.source_id  AND words.table_to_use=1  AND sms.thread_id = threads._id AND temp_conversation.sms_thread_id = threads._id AND "
            + " (index_text LIKE ? OR temp_conversation.recipient_address LIKE ? OR temp_conversation.recipient_name LIKE ?))";
    /* SPRD: Add this for implement the func of searching by contacts' name. @{ */
    /*private static final String MMS_QUERY2 = "SELECT pdu._id as _id, pdu.thread_id as thread_id, temp_conversation.recipient_name as recipient_name,address, null as body, pdu.sub as sub, 'mms' as msg_type, pdu.msg_box as box_type"
            + " FROM pdu,threads,sms,temp_conversation WHERE ((pdu.thread_id = threads._id ) AND threads._id = temp_conversation.sms_thread_id "
            + " AND pdu.m_type != "+PduHeaders.MESSAGE_TYPE_DELIVERY_IND
            + " AND (address LIKE ? OR temp_conversation.recipient_name LIKE ?  OR pdu.sub LIKE ?)) ";*/
    private static final String MMS_QUERY2 = "SELECT pdu._id as _id,temp_conversation.conversation_id as conv_id, pdu.thread_id as thread_id, temp_conversation.recipient_name as name,temp_conversation.recipient_address as address, temp_conversation.snippet_text as body, pdu.sub as sub, 'mms' as msg_type, pdu.msg_box as box_type"
            + " FROM pdu,threads,temp_conversation WHERE ((pdu.thread_id = threads._id ) AND threads._id = temp_conversation.sms_thread_id "
            + " AND pdu.m_type != "+PduHeaders.MESSAGE_TYPE_DELIVERY_IND
            + " AND (temp_conversation.recipient_address LIKE ? OR temp_conversation.recipient_name LIKE ?  OR pdu.sub LIKE ?)) ";

    private static final String SMS_QUERY2 = "SELECT sms._id as _id,temp_conversation.conversation_id as conv_id, thread_id,temp_conversation.recipient_name as name,temp_conversation.recipient_address as address,sms.body as body, null as sub, 'sms' as msg_type, sms.type as box_type"
            + " FROM sms,words,threads,temp_conversation WHERE ( "
            + " sms._id=words.source_id  AND words.table_to_use=1  AND sms.thread_id = threads._id AND threads._id = temp_conversation.sms_thread_id AND  "
            + " (index_text MATCH ?)"
            + " AND (temp_conversation.recipient_address LIKE ? OR temp_conversation.recipient_name LIKE ?))";

    private static final String MMS_QUERY3 = "SELECT pdu._id as _id,temp_conversation.conversation_id as conv_id, pdu.thread_id as thread_id,temp_conversation.recipient_name as name,temp_conversation.recipient_address as address,part.text as body, null as sub, 'mms' as msg_type, pdu.msg_box as box_type"
            + " FROM pdu,part,addr,words,threads,temp_conversation "
            + " WHERE ((part.mid=pdu._id) AND (addr.msg_id=pdu._id) AND (pdu.thread_id = threads._id ) AND "
            + " ((addr.type= "
            + PduHeaders.TO
            + " AND pdu.msg_box>1) OR (addr.type= "
            + PduHeaders.FROM
            + " AND pdu.msg_box=1))"
            + " AND (part.ct='text/plain')  AND (part._id = words.source_id) "
            + " AND (words.table_to_use=2)) "
            + " AND (index_text MATCH ?) "
            + " AND (pdu.m_type != " + PduHeaders.MESSAGE_TYPE_DELIVERY_IND + ")"
            + " AND (temp_conversation.recipient_address LIKE ? OR temp_conversation.recipient_name LIKE ?)";
    /* @} */
    private static final String MMS_QUERY = "SELECT pdu._id as _id,temp_conversation.conversation_id as conv_id, pdu.thread_id as thread_id,temp_conversation.recipient_name as name,temp_conversation.recipient_address as address,part.text as body, null as sub, 'mms' as msg_type, pdu.msg_box as box_type"
            + " FROM threads, pdu,part,addr,words,temp_conversation "
            + " WHERE ((pdu.thread_id = threads._id ) AND (threads._id = temp_conversation.sms_thread_id) AND (part.mid=pdu._id) AND (addr.msg_id=pdu._id) AND  "
            + " ((addr.type= "
            + PduHeaders.FROM
            + " AND pdu.msg_box>1) OR (addr.type= "
            + PduHeaders.TO
            + " AND pdu.msg_box=1))"
            + " AND (part.ct='text/plain')  AND (part._id = words.source_id) "
            + " AND (words.table_to_use=2) AND pdu.m_type != "
            + PduHeaders.MESSAGE_TYPE_DELIVERY_IND + " AND (index_text LIKE ?  OR temp_conversation.recipient_address LIKE ? OR temp_conversation.recipient_name LIKE ?) ) ";
    //add for bug 552039 begin
    private static final String DRAFT_QUERY = "SELECT conversation_id as _id, conversation_id as conv_id,sms_thread_id as thread_id,recipient_name as name,recipient_address as address, draft_snippet_text as body, draft_subject_text as sub, 'draft' as msg_type, null as box_type"
            + " FROM temp_conversation WHERE ( "
            + " (temp_conversation.draft_snippet_text LIKE ? OR temp_conversation.draft_subject_text LIKE ?))";
    //add for bug 552039 end
    //add for bug 552039 begin
    private static final String SMS_MMS_QUERY = SMS_QUERY + " UNION "
            + SMS_QUERY2 + " UNION " + " SELECT * FROM (  " + MMS_QUERY + " UNION "
            + MMS_QUERY2 + " UNION "+ MMS_QUERY3 + " UNION "+ DRAFT_QUERY +" ) GROUP BY _id";

    private  static  final String escString = " escape '/' ";

    private static final String SMS_MMS_QUERY_ESC = SMS_QUERY + escString + " UNION "
            + SMS_QUERY2 + escString + " UNION   SELECT * FROM (  " + MMS_QUERY+escString + " UNION "
            + MMS_QUERY2 + escString + " UNION " + MMS_QUERY3+ escString + " UNION  " + DRAFT_QUERY  + escString + " ) GROUP BY _id " ;

    //add for bug 552039 end
    //add for bug 543691 end
    // bug 495194 : add for search feature end  android N update end
    private static final String AUTHORITY = "mms-sms";

    static {
        URI_MATCHER.addURI(AUTHORITY, "conversations", URI_CONVERSATIONS);
        URI_MATCHER.addURI(AUTHORITY, "complete-conversations", URI_COMPLETE_CONVERSATIONS);

        // In these patterns, "#" is the thread ID.
        URI_MATCHER.addURI(
                AUTHORITY, "conversations/#", URI_CONVERSATIONS_MESSAGES);
        URI_MATCHER.addURI(
                AUTHORITY, "conversations/#/recipients",
                URI_CONVERSATIONS_RECIPIENTS);

        URI_MATCHER.addURI(
                AUTHORITY, "conversations/#/subject",
                URI_CONVERSATIONS_SUBJECT);

        // URI for deleting obsolete threads.
        URI_MATCHER.addURI(AUTHORITY, "conversations/obsolete", URI_OBSOLETE_THREADS);

        URI_MATCHER.addURI(
                AUTHORITY, "messages/byphone/*",
                URI_MESSAGES_BY_PHONE);

        // In this pattern, two query parameter names are expected:
        // "subject" and "recipient."  Multiple "recipient" parameters
        // may be present.
        URI_MATCHER.addURI(AUTHORITY, "threadID", URI_THREAD_ID);

        // Use this pattern to query the canonical address by given ID.
        URI_MATCHER.addURI(AUTHORITY, "canonical-address/#", URI_CANONICAL_ADDRESS);

        // Use this pattern to query all canonical addresses.
        URI_MATCHER.addURI(AUTHORITY, "canonical-addresses", URI_CANONICAL_ADDRESSES);

        URI_MATCHER.addURI(AUTHORITY, "search", URI_SEARCH);
        URI_MATCHER.addURI(AUTHORITY, "searchSuggest", URI_SEARCH_SUGGEST);

        // In this pattern, two query parameters may be supplied:
        // "protocol" and "message." For example:
        //   content://mms-sms/pending?
        //       -> Return all pending messages;
        //   content://mms-sms/pending?protocol=sms
        //       -> Only return pending SMs;
        //   content://mms-sms/pending?protocol=mms&message=1
        //       -> Return the the pending MM which ID equals '1'.
        //
        URI_MATCHER.addURI(AUTHORITY, "pending", URI_PENDING_MSG);

        // Use this pattern to get a list of undelivered messages.
        URI_MATCHER.addURI(AUTHORITY, "undelivered", URI_UNDELIVERED_MSG);

        // Use this pattern to see what delivery status reports (for
        // both MMS and SMS) have not been delivered to the user.
        URI_MATCHER.addURI(AUTHORITY, "notifications", URI_NOTIFICATIONS);

        URI_MATCHER.addURI(AUTHORITY, "draft", URI_DRAFT);

        URI_MATCHER.addURI(AUTHORITY, "locked", URI_FIRST_LOCKED_MESSAGE_ALL);

        URI_MATCHER.addURI(AUTHORITY, "locked/#", URI_FIRST_LOCKED_MESSAGE_BY_THREAD_ID);

        URI_MATCHER.addURI(AUTHORITY, "messageIdToThread", URI_MESSAGE_ID_TO_THREAD);
        URI_MATCHER.addURI(AUTHORITY, "sqlite_sequence", URI_SMS_MMS_SEARCH);
        // bug 495194 : add for search feature begin android N update start
        URI_MATCHER.addURI(AUTHORITY, "createTempTable", URI_CREATE_TEMP_TABLE);
        URI_MATCHER.addURI(AUTHORITY, "dropTempTable", URI_DROP_TEMP_TABLE);
        //add for bug 552039 begin
        URI_MATCHER.addURI(AUTHORITY, "updateConvDraft",URI_UPDATE_CONVERSATION_DRAFT);
        //add for bug 552039 end
        // bug 495194 : add for search feature end

        //sprd: fix for telcel bug 557685 begin
        URI_MATCHER.addURI(AUTHORITY, "contacts/fdn/",URI_FILTER_FDN_CONTACTS_DATA);
        URI_MATCHER.addURI(AUTHORITY, "contacts/fdn/#",URI_FILTER_FDN_CONTACTS);
        URI_MATCHER.addURI(AUTHORITY, "contacts/contacts/#",URI_FILTER_lOCAL_CONTACTS);
        URI_MATCHER.addURI(AUTHORITY, "contacts/fdnContacts/#",URI_FILTER_CUFUSE_FDN_CONTACTS);
        //sprd: fix for telcel bug 557685 end
        //add for bug 566254 begin
        URI_MATCHER.addURI(AUTHORITY, "part",URI_PART);
        //add for bug 566254 end android N update end
        initializeColumnSets();
    }

    private SQLiteOpenHelper mOpenHelper;

    private boolean mUseStrictPhoneNumberComparation;

    private static final String METHOD_IS_RESTORING = "is_restoring";
    private static final String IS_RESTORING_KEY = "restoring";

    @Override
    public boolean onCreate() {
        setAppOps(AppOpsManager.OP_READ_SMS, AppOpsManager.OP_WRITE_SMS);
        mOpenHelper = MmsSmsDatabaseHelper.getInstanceForCe(getContext());
        mUseStrictPhoneNumberComparation =
            getContext().getResources().getBoolean(
                    com.android.internal.R.bool.config_use_strict_phone_number_comparation);
        TelephonyBackupAgent.DeferredSmsMmsRestoreService.startIfFilesExist(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        // First check if restricted views of the "sms" and "pdu" tables should be used based on the
        // caller's identity. Only system, phone or the default sms app can have full access
        // of sms/mms data. For other apps, we present a restricted view which only contains sent
        // or received messages, without wap pushes.
        final boolean accessRestricted = ProviderUtil.isAccessRestricted(
                getContext(), getCallingPackage(), Binder.getCallingUid());
        final String pduTable = MmsProvider.getPduTable(accessRestricted);
        final String smsTable = SmsProvider.getSmsTable(accessRestricted);

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor cursor = null;
        final int match = URI_MATCHER.match(uri);
        switch (match) {
            case URI_COMPLETE_CONVERSATIONS:
                cursor = getCompleteConversations(projection, selection, sortOrder, smsTable,
                        pduTable);
                break;
            case URI_CONVERSATIONS:
                String simple = uri.getQueryParameter("simple");
                if ((simple != null) && simple.equals("true")) {
                    String threadType = uri.getQueryParameter("thread_type");
                    if (!TextUtils.isEmpty(threadType)) {
                        selection = concatSelections(
                                selection, Threads.TYPE + "=" + threadType);
                    }
                    cursor = getSimpleConversations(
                            projection, selection, selectionArgs, sortOrder);
                } else {
                    cursor = getConversations(
                            projection, selection, sortOrder, smsTable, pduTable);
                }
                break;
            case URI_CONVERSATIONS_MESSAGES:
                cursor = getConversationMessages(uri.getPathSegments().get(1), projection,
                        selection, sortOrder, smsTable, pduTable);
                break;
            case URI_CONVERSATIONS_RECIPIENTS:
                cursor = getConversationById(
                        uri.getPathSegments().get(1), projection, selection,
                        selectionArgs, sortOrder);
                break;
            case URI_CONVERSATIONS_SUBJECT:
                cursor = getConversationById(
                        uri.getPathSegments().get(1), projection, selection,
                        selectionArgs, sortOrder);
                break;
            case URI_MESSAGES_BY_PHONE:
                cursor = getMessagesByPhoneNumber(
                        uri.getPathSegments().get(2), projection, selection, sortOrder, smsTable,
                        pduTable);
                break;
            case URI_THREAD_ID:
                List<String> recipients = uri.getQueryParameters("recipient");

                cursor = getThreadId(recipients);
                break;
            case URI_CANONICAL_ADDRESS: {
                String extraSelection = "_id=" + uri.getPathSegments().get(1);
                String finalSelection = TextUtils.isEmpty(selection)
                        ? extraSelection : extraSelection + " AND " + selection;
                cursor = db.query(TABLE_CANONICAL_ADDRESSES,
                        CANONICAL_ADDRESSES_COLUMNS_1,
                        finalSelection,
                        selectionArgs,
                        null, null,
                        sortOrder);
                break;
            }
            case URI_CANONICAL_ADDRESSES:
                cursor = db.query(TABLE_CANONICAL_ADDRESSES,
                        CANONICAL_ADDRESSES_COLUMNS_2,
                        selection,
                        selectionArgs,
                        null, null,
                        sortOrder);
                break;
            case URI_SEARCH_SUGGEST: {
                SEARCH_STRING[0] = uri.getQueryParameter("pattern") + '*' ;

                // find the words which match the pattern using the snippet function.  The
                // snippet function parameters mainly describe how to format the result.
                // See http://www.sqlite.org/fts3.html#section_4_2 for details.
                if (       sortOrder != null
                        || selection != null
                        || selectionArgs != null
                        || projection != null) {
                    throw new IllegalArgumentException(
                            "do not specify sortOrder, selection, selectionArgs, or projection" +
                            "with this query");
                }

                /*
                 * SPRD: Modify for bug#283281. orig:cursor =
                 * db.rawQuery(SEARCH_QUERY, SEARCH_STRING);
                 * @{
                 */
                try {
                    String searchString = uri.getQueryParameter("pattern");
                    //add for bug 543676 begin
                    searchString = CharEscapeUtil.charEscaseEncode(searchString);
                    System.out.println("after CharEscapeUtil,searchString = ["+searchString+"]");
                    //add for bug 543676 end
                    String sql;
                    if ("".equals(searchString)) {
                        sql = String
                                .format("SELECT snippet(words, '', ' ', '', 1, 1) as snippet FROM words WHERE index_text MATCH '%s*' ORDER BY snippet LIMIT 50;",
                                        searchString);
                    } else {
                        /*
                         * the sql porting from 5.1 version sql =
                         * " SELECT index_text as snippet FROM words WHERE index_text LIKE '%"
                         * + searchString + "%' UNION " +
                         * " SELECT recipient_names as snippet FROM threads WHERE recipient_names LIKE '%"
                         * + searchString + "%' UNION " +
                         * " SELECT recipient_addresses as snippet FROM threads WHERE recipient_addresses LIKE '%"
                         * + searchString.replaceAll(" ", "") + "%' " +
                         * " ORDER BY snippet LIMIT 50;";
                         */
                        //add for bug 543676 begin
                        //add for bug 551694 begin
                        // add for bug 855055 begin
                        sql = " SELECT index_text as snippet FROM words WHERE index_text LIKE '%"
                                + searchString
                                + "%' escape '/' UNION "
                                + " SELECT recipient_name as snippet FROM temp_conversation WHERE sort_timestamp >0 AND recipient_name LIKE '%"
                                + searchString
                                + "%' escape '/' UNION "
                                + " SELECT recipient_address as snippet FROM  temp_conversation WHERE sort_timestamp >0 AND recipient_address LIKE '%"
                                + searchString.replaceAll(" ", "")
                                + "%' escape '/' UNION"
                                + " SELECT sub as snippet FROM pdu WHERE sub LIKE '%"
                                + searchString
                                //add for bug 552039 begin
                                + "%' escape '/' UNION"
                                + " SELECT draft_snippet_text as snippet FROM temp_conversation WHERE sort_timestamp >0 AND draft_snippet_text LIKE '%"
                                + searchString
                                + "%' escape '/' UNION"
                                + " SELECT draft_subject_text as snippet FROM temp_conversation WHERE sort_timestamp >0 AND draft_subject_text LIKE '%"
                                + searchString
                                //add for bug 552039 end
                                + "%' escape '/'"
                                + " ORDER BY snippet LIMIT 50;";
                        // add for bug 855055 end
                        //add for bug 551694 end
                        //add for bug 543676 end
                    }
                    System.out.println("the searchString = [" + searchString + "] "
                            + ", the sql = [" + sql + "]");
                    /* @} */
                    cursor = db.rawQuery(sql, null);
                } catch (Exception e) {
                    System.out.println("Exception occur in URI_SEARCH_SUGGEST, exception is : "
                            + e.toString());
                }//android N update end
                break;
            }
            case URI_MESSAGE_ID_TO_THREAD: {
                // Given a message ID and an indicator for SMS vs. MMS return
                // the thread id of the corresponding thread.
                try {
                    long id = Long.parseLong(uri.getQueryParameter("row_id"));
                    switch (Integer.parseInt(uri.getQueryParameter("table_to_use"))) {
                        case 1:  // sms
                            cursor = db.query(
                                smsTable,
                                new String[] { "thread_id" },
                                "_id=?",
                                new String[] { String.valueOf(id) },
                                null,
                                null,
                                null);
                            break;
                        case 2:  // mms
                            String mmsQuery = "SELECT thread_id "
                                    + "FROM " + pduTable + ",part "
                                    + "WHERE ((part.mid=" + pduTable + "._id) "
                                    + "AND " + "(part._id=?))";
                            cursor = db.rawQuery(mmsQuery, new String[] { String.valueOf(id) });
                            break;
                    }
                } catch (NumberFormatException ex) {
                    // ignore... return empty cursor
                }
                break;
            }
            case URI_SEARCH: {
                if (       sortOrder != null
                        || selection != null
                        || selectionArgs != null
                        || projection != null) {
                    throw new IllegalArgumentException(
                            "do not specify sortOrder, selection, selectionArgs, or projection" +
                            "with this query");
                }
                //add Bug 809309 start
                 /*String searchString = uri.getQueryParameter("pattern") + "*";

                 try {
                     cursor = db.rawQuery(getTextSearchQuery(smsTable, pduTable),
                             new String[] { searchString, searchString });
                 } catch (Exception ex) {
                     Log.e(LOG_TAG, "got exception: " + ex.toString());
                 }
                 break;*/
                 // bug 495194 : add for search feature begin
                 /* SPRD: Modify for search by contacts' name. @{ */
                // String searchString = "%" + uri.getQueryParameter("pattern") + "%";
                //  String strMatch = uri.getQueryParameter("pattern") + "*";
                String searchString = uri.getQueryParameter("pattern");
                String searchString_esc = CharEscapeUtil.charEscaseEncode(searchString);
                String strMatch = searchString + "*";
                searchString = "%" +searchString_esc + "%";
                //String strMatch = searchString_esc + "*";

                 System.out.println("the searchString = [" + searchString + "] "
                         + ", the strMatch = [" + strMatch + "]");
                 try {;
                     //add for bug 552039 begin
                     //bug 837318  begin
                     String szSQLPattern = " SELECT * FROM ("+
                             " SELECT * FROM  SMS_MMS_VIEW  WHERE index_text LIKE '%s' escape '/' " +
                             " UNION " +
                             " SELECT * FROM  SMS_MMS_VIEW  WHERE address LIKE '%s' escape '/' " +
                             " UNION " +
                             " SELECT * FROM  SMS_MMS_VIEW  WHERE name LIKE '%s' escape '/' " +
                             " UNION " +
                             " SELECT * FROM  SMS_MMS_VIEW  WHERE sub LIKE '%s' escape '/' " +
                             " UNION " +
                             " SELECT * FROM  SMS_MMS_VIEW  WHERE body LIKE '%s' escape '/' " +
                             " )  group by  _id  ";
                     //bug 837318  end

                     String szSQL = String.format(szSQLPattern,  searchString, searchString, searchString,  searchString, searchString );
                     //Log.d(LOG_TAG," SMS_MMS_QUERY_ESC::::::"+szSQL);
                     cursor = db.rawQuery(szSQL, null);
                     //add Bug 809309 end
                     //add for bug 552039 end
                 } catch (Exception ex) {
                     Log.e(LOG_TAG, "got exception: " + ex.toString());
                 }
                 /* @} */
                 // bug 495194 : add for search feature end android N update end
                break;
            }
            case URI_PENDING_MSG: {
                String protoName = uri.getQueryParameter("protocol");
                String msgId = uri.getQueryParameter("message");
                int proto = TextUtils.isEmpty(protoName) ? -1
                        : (protoName.equals("sms") ? MmsSms.SMS_PROTO : MmsSms.MMS_PROTO);

                String extraSelection = (proto != -1) ?
                        (PendingMessages.PROTO_TYPE + "=" + proto) : " 0=0 ";
                if (!TextUtils.isEmpty(msgId)) {
                    extraSelection += " AND " + PendingMessages.MSG_ID + "=" + msgId;
                }

                String finalSelection = TextUtils.isEmpty(selection)
                        ? extraSelection : ("(" + extraSelection + ") AND " + selection);
                String finalOrder = TextUtils.isEmpty(sortOrder)
                        ? PendingMessages.DUE_TIME : sortOrder;
                cursor = db.query(TABLE_PENDING_MSG, null,
                        finalSelection, selectionArgs, null, null, finalOrder);
                break;
            }
            case URI_UNDELIVERED_MSG: {
                cursor = getUndeliveredMessages(projection, selection,
                        selectionArgs, sortOrder, smsTable, pduTable);
                break;
            }
            case URI_DRAFT: {
                cursor = getDraftThread(projection, selection, sortOrder, smsTable, pduTable);
                break;
            }
            case URI_FIRST_LOCKED_MESSAGE_BY_THREAD_ID: {
                long threadId;
                try {
                    threadId = Long.parseLong(uri.getLastPathSegment());
                } catch (NumberFormatException e) {
                    Log.e(LOG_TAG, "Thread ID must be a long.");
                    break;
                }
                cursor = getFirstLockedMessage(projection, "thread_id=" + Long.toString(threadId),
                        sortOrder, smsTable, pduTable);
                break;
            }
            case URI_FIRST_LOCKED_MESSAGE_ALL: {
                cursor = getFirstLockedMessage(
                        projection, selection, sortOrder, smsTable, pduTable);
                break;
            }
           case URI_SMS_MMS_SEARCH: {//android N update start
                System.out.println("jessica add URI_SMS_MMS_SEARCH selection = " + selection
                                   + "selectionArgs = " + selectionArgs);
                if (sortOrder != null
                              || projection != null) {
                   throw new IllegalArgumentException(
                         "do not specify sortOrder, or projection" +
                          "with this query");
                 }


               try {
                   cursor = db.rawQuery(selection,selectionArgs);
                } catch (Exception ex) {
                    Log.e(LOG_TAG, "got exception: " + ex.toString());
                }
               break;
             }
            // bug 495194 : add for search feature begin
           case URI_CREATE_TEMP_TABLE: {
               System.out.println("case URI_CREATE_TEMP_TABLE");
               try {
                   SearchSyncHelper.getInstance(getContext(), mOpenHelper.getWritableDatabase()).syncRecord(1000);
               } catch (Exception e) {
                   System.out.println("case URI_CREATE_TEMP_TABLE Exception : " + e.toString());
               }
               break;
           }
           case URI_DROP_TEMP_TABLE: {
               System.out.println("case URI_DROP_TEMP_TABLE");
               try {
                   SQLiteDatabase dropDb = mOpenHelper.getWritableDatabase();
                   String sqlDelete = "drop table temp_conversation;";
                   dropDb.execSQL(sqlDelete);
               } catch (Exception e) {
                   System.out.println("case URI_DROP_TEMP_TABLE Exception : " + e.toString());
               }
               break;
           }
           //add for bug 552039 begin
           case URI_UPDATE_CONVERSATION_DRAFT: {
               System.out.println("case URI_UPDATE_CONVERSATION_DRAFT");
               try {
                   List<String> convId = uri.getQueryParameters("convId");
                   SearchSyncHelper.getInstance(getContext(), mOpenHelper.getWritableDatabase()).updateConvDraft(convId.get(0));
               } catch (Exception e) {
                   System.out.println("case URI_UPDATE_CONVERSATION_DRAFT Exception : " + e.toString());
               }
               break;
           }
           //add for bug 552039 end
           // bug 495194 : add for search feature end

           // sprd FdnContacts 5/29 begin
           /*case URI_FILTER_FDN_CONTACTS:{
               System.out.println("URI_FILTER_FDN_CONTACTS");
               String number = (uri.getLastPathSegment()).toString().trim();
               try {

                   SQLiteDatabase fdnDb = mOpenHelper.getWritableDatabase();
                   String fdnViewTable = MmsSmsDatabaseHelper.getFdnViewTable();
                   cursor = fdnDb.query(fdnViewTable, projection, "data1 like ?", new String[]{"%"+number+"%"}, null, null, sortOrder);
               } catch (Exception e) {
                   System.out.println("case URI_FILTER_FDN_CONTACTS Exception : " + e.toString());
               }

               break;
           }
           case URI_FILTER_lOCAL_CONTACTS:{
               System.out.println("URI_FILTER_lOCAL_CONTACTS");
               String number = (uri.getLastPathSegment()).toString().trim();
               try {
                   String selectionsprd = "data1"+ "=?";
                   SQLiteDatabase fdnDb = mOpenHelper.getWritableDatabase();
                   String contactsViewTable = MmsSmsDatabaseHelper.getLocalContactsViewTable();
                   cursor = fdnDb.query(contactsViewTable, projection, "data1 like ?", new String[]{"%"+number+"%"}, null, null, sortOrder);
               } catch (Exception e) {
                   System.out.println("case URI_FILTER_lOCAL_CONTACTS Exception : " + e.toString());
               }
               break;
           }
           case URI_FILTER_CUFUSE_FDN_CONTACTS:{
               System.out.println("URI_FILTER_CUFUSE_FDN_CONTACTS");
               String number = (uri.getLastPathSegment()).toString().trim();
               try {
                   String selectionsprd = "data1"+ "=?";
                   SQLiteDatabase fdnDb = mOpenHelper.getWritableDatabase();
                   String confuseViewTable = MmsSmsDatabaseHelper.getConfuseContactsViewTable();
                   cursor = fdnDb.query(confuseViewTable, projection, "data1 like ?", new String[]{"%"+number+"%"}, null, null, sortOrder);
               } catch (Exception e) {
                   System.out.println("case URI_FILTER_CUFUSE_FDN_CONTACTS Exception : " + e.toString());
               }

               break;
           }
           // sprd 570184 start
           case URI_FILTER_FDN_CONTACTS_DATA: {
               System.out.println("URI_FILTER_FDN_CONTACTS_DATA");
               try {
                SQLiteDatabase fdnDb = mOpenHelper.getWritableDatabase();
                cursor = fdnDb.query(MmsSmsDatabaseHelper.getFdnViewTable(),
                        projection, null, null, null, null, sortOrder);
               } catch (Exception e) {
                 System.out
                        .println("case URI_FILTER_FDN_CONTACTS_DATA Exception : "
                                + e.toString());
               }
              break;
           }*/
           // sprd FdnContacts 5/29 begin
           // sprd 570184 end
           //add for bug 566254 begin
           case URI_PART: {
               System.out.println("case URI_PART");
               try {
                   //Integer pduId = Integer.valueOf(uri.getQueryParameter("pduId"));
                   Integer pduId = -1;
                   if (selectionArgs!= null && selectionArgs.length != 0) {
                       pduId = Integer.valueOf(selectionArgs[0]);
                   }
                   String sqlStr = String.format("select * from part where mid = %d",pduId);
                   System.out.println("sqlStr = ["+sqlStr+"]");
                   cursor = db.rawQuery(sqlStr, null);
               } catch (Exception e) {
                   System.out.println("case URI_PART Exception : " + e.toString());
               }
               break;
           }
           //add for bug 566254 end android N update end
             default:
                throw new IllegalStateException("Unrecognized URI:" + uri);
        }

        if (cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), MmsSms.CONTENT_URI);
        }
        return cursor;
    }

    /**
     * Return the canonical address ID for this address.
     */
    private long getSingleAddressId(String address) {
        boolean isEmail = Mms.isEmailAddress(address);
        boolean isPhoneNumber = Mms.isPhoneNumber(address);

        // We lowercase all email addresses, but not addresses that aren't numbers, because
        // that would incorrectly turn an address such as "My Vodafone" into "my vodafone"
        // and the thread title would be incorrect when displayed in the UI.
        String refinedAddress = isEmail ? address.toLowerCase() : address;

        String selection = "address=?";
        String[] selectionArgs;
        long retVal = -1L;

        if (!isPhoneNumber) {
            selectionArgs = new String[] { refinedAddress };
        } else {
            selection += " OR PHONE_NUMBERS_EQUAL(address, ?, " +
                        (mUseStrictPhoneNumberComparation ? 1 : 0) + ")";
            selectionArgs = new String[] { refinedAddress, refinedAddress };
        }

        Cursor cursor = null;

        try {
            SQLiteDatabase db = mOpenHelper.getReadableDatabase();
            cursor = db.query(
                    "canonical_addresses", ID_PROJECTION,
                    selection, selectionArgs, null, null, null);

            if (cursor.getCount() == 0) {
                ContentValues contentValues = new ContentValues(1);
                contentValues.put(CanonicalAddressesColumns.ADDRESS, refinedAddress);

                db = mOpenHelper.getWritableDatabase();
                retVal = db.insert("canonical_addresses",
                        CanonicalAddressesColumns.ADDRESS, contentValues);

                Log.d(LOG_TAG, "getSingleAddressId: insert new canonical_address for " +
                        /*address*/ "xxxxxx" + ", _id=" + retVal);

                return retVal;
            }

            if (cursor.moveToFirst()) {
                retVal = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return retVal;
    }

    /**
     * Return the canonical address IDs for these addresses.
     */
    private Set<Long> getAddressIds(List<String> addresses) {
        Set<Long> result = new HashSet<Long>(addresses.size());

        for (String address : addresses) {
            if (!address.equals(PduHeaders.FROM_INSERT_ADDRESS_TOKEN_STR)) {
                long id = getSingleAddressId(address);
                if (id != -1L) {
                    result.add(id);
                } else {
                    Log.e(LOG_TAG, "getAddressIds: address ID not found for " + address);
                }
            }
        }
        return result;
    }

    /**
     * Return a sorted array of the given Set of Longs.
     */
    private long[] getSortedSet(Set<Long> numbers) {
        int size = numbers.size();
        long[] result = new long[size];
        int i = 0;

        for (Long number : numbers) {
            result[i++] = number;
        }

        if (size > 1) {
            Arrays.sort(result);
        }

        return result;
    }

    /**
     * Return a String of the numbers in the given array, in order,
     * separated by spaces.
     */
    private String getSpaceSeparatedNumbers(long[] numbers) {
        int size = numbers.length;
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < size; i++) {
            if (i != 0) {
                buffer.append(' ');
            }
            buffer.append(numbers[i]);
        }
        return buffer.toString();
    }

    /**
     * Insert a record for a new thread.
     */
    private void insertThread(String recipientIds, int numberOfRecipients) {
        ContentValues values = new ContentValues(4);

        long date = System.currentTimeMillis();
        values.put(ThreadsColumns.DATE, date - date % 1000);
        values.put(ThreadsColumns.RECIPIENT_IDS, recipientIds);
        if (numberOfRecipients > 1) {
            values.put(Threads.TYPE, Threads.BROADCAST_THREAD);
        }
        values.put(ThreadsColumns.MESSAGE_COUNT, 0);

        long result = mOpenHelper.getWritableDatabase().insert(TABLE_THREADS, null, values);
        Log.d(LOG_TAG, "insertThread: created new thread_id " + result +
                " for recipientIds " + /*recipientIds*/ "xxxxxxx");

        getContext().getContentResolver().notifyChange(MmsSms.CONTENT_URI, null, true,
                UserHandle.USER_ALL);
        // bug 495194 : add for search feature begin android N update start
        SearchSyncHelper.getInstance(getContext(), mOpenHelper.getWritableDatabase()).insertRecord((int) result);
        // bug 495194 : add for search feature end android N update end
    }

    private static final String THREAD_QUERY =
            "SELECT _id FROM threads " + "WHERE recipient_ids=?";

    /**
     * Return the thread ID for this list of
     * recipients IDs.  If no thread exists with this ID, create
     * one and return it.  Callers should always use
     * Threads.getThreadId to access this information.
     */
    private synchronized Cursor getThreadId(List<String> recipients) {
        Set<Long> addressIds = getAddressIds(recipients);
        String recipientIds = "";

        if (addressIds.size() == 0) {
            Log.e(LOG_TAG, "getThreadId: NO receipients specified -- NOT creating thread",
                    new Exception());
            return null;
        } else if (addressIds.size() == 1) {
            // optimize for size==1, which should be most of the cases
            for (Long addressId : addressIds) {
                recipientIds = Long.toString(addressId);
            }
        } else {
            recipientIds = getSpaceSeparatedNumbers(getSortedSet(addressIds));
        }

        if (Log.isLoggable(LOG_TAG, Log.VERBOSE)) {
            Log.d(LOG_TAG, "getThreadId: recipientIds (selectionArgs) =" +
                    /*recipientIds*/ "xxxxxxx");
        }

        String[] selectionArgs = new String[] { recipientIds };

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        db.beginTransaction();
        Cursor cursor = null;
        try {
            // Find the thread with the given recipients
            cursor = db.rawQuery(THREAD_QUERY, selectionArgs);

            if (cursor.getCount() == 0) {
                // No thread with those recipients exists, so create the thread.
                cursor.close();

                Log.d(LOG_TAG, "getThreadId: create new thread_id for recipients " +
                        /*recipients*/ "xxxxxxxx");
                insertThread(recipientIds, recipients.size());

                // The thread was just created, now find it and return it.
                cursor = db.rawQuery(THREAD_QUERY, selectionArgs);
            }
            db.setTransactionSuccessful();
        } catch (Throwable ex) {
            Log.e(LOG_TAG, ex.getMessage(), ex);
        } finally {
            db.endTransaction();
        }

        if (cursor != null && cursor.getCount() > 1) {
            Log.w(LOG_TAG, "getThreadId: why is cursorCount=" + cursor.getCount());
        }
        return cursor;
    }

    private static String concatSelections(String selection1, String selection2) {
        if (TextUtils.isEmpty(selection1)) {
            return selection2;
        } else if (TextUtils.isEmpty(selection2)) {
            return selection1;
        } else {
            return selection1 + " AND " + selection2;
        }
    }

    /**
     * If a null projection is given, return the union of all columns
     * in both the MMS and SMS messages tables.  Otherwise, return the
     * given projection.
     */
    private static String[] handleNullMessageProjection(
            String[] projection) {
        return projection == null ? UNION_COLUMNS : projection;
    }

    /**
     * If a null projection is given, return the set of all columns in
     * the threads table.  Otherwise, return the given projection.
     */
    private static String[] handleNullThreadsProjection(
            String[] projection) {
        return projection == null ? THREADS_COLUMNS : projection;
    }

    /**
     * If a null sort order is given, return "normalized_date ASC".
     * Otherwise, return the given sort order.
     */
    private static String handleNullSortOrder (String sortOrder) {
        return sortOrder == null ? "normalized_date ASC" : sortOrder;
    }

    /**
     * Return existing threads in the database.
     */
    private Cursor getSimpleConversations(String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        return mOpenHelper.getReadableDatabase().query(TABLE_THREADS, projection,
                selection, selectionArgs, null, null, " date DESC");
    }

    /**
     * Return the thread which has draft in both MMS and SMS.
     *
     * Use this query:
     *
     *   SELECT ...
     *     FROM (SELECT _id, thread_id, ...
     *             FROM pdu
     *             WHERE msg_box = 3 AND ...
     *           UNION
     *           SELECT _id, thread_id, ...
     *             FROM sms
     *             WHERE type = 3 AND ...
     *          )
     *   ;
     */
    private Cursor getDraftThread(String[] projection, String selection,
            String sortOrder, String smsTable, String pduTable) {
        String[] innerProjection = new String[] {BaseColumns._ID, Conversations.THREAD_ID};
        SQLiteQueryBuilder mmsQueryBuilder = new SQLiteQueryBuilder();
        SQLiteQueryBuilder smsQueryBuilder = new SQLiteQueryBuilder();

        mmsQueryBuilder.setTables(pduTable);
        smsQueryBuilder.setTables(smsTable);

        String mmsSubQuery = mmsQueryBuilder.buildUnionSubQuery(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN, innerProjection,
                MMS_COLUMNS, 1, "mms",
                concatSelections(selection, Mms.MESSAGE_BOX + "=" + Mms.MESSAGE_BOX_DRAFTS),
                null, null);
        String smsSubQuery = smsQueryBuilder.buildUnionSubQuery(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN, innerProjection,
                SMS_COLUMNS, 1, "sms",
                concatSelections(selection, Sms.TYPE + "=" + Sms.MESSAGE_TYPE_DRAFT),
                null, null);
        SQLiteQueryBuilder unionQueryBuilder = new SQLiteQueryBuilder();

        unionQueryBuilder.setDistinct(true);

        String unionQuery = unionQueryBuilder.buildUnionQuery(
                new String[] { mmsSubQuery, smsSubQuery }, null, null);

        SQLiteQueryBuilder outerQueryBuilder = new SQLiteQueryBuilder();

        outerQueryBuilder.setTables("(" + unionQuery + ")");

        String outerQuery = outerQueryBuilder.buildQuery(
                projection, null, null, null, sortOrder, null);

        return mOpenHelper.getReadableDatabase().rawQuery(outerQuery, EMPTY_STRING_ARRAY);
    }

    /**
     * Return the most recent message in each conversation in both MMS
     * and SMS.
     *
     * Use this query:
     *
     *   SELECT ...
     *     FROM (SELECT thread_id AS tid, date * 1000 AS normalized_date, ...
     *             FROM pdu
     *             WHERE msg_box != 3 AND ...
     *             GROUP BY thread_id
     *             HAVING date = MAX(date)
     *           UNION
     *           SELECT thread_id AS tid, date AS normalized_date, ...
     *             FROM sms
     *             WHERE ...
     *             GROUP BY thread_id
     *             HAVING date = MAX(date))
     *     GROUP BY tid
     *     HAVING normalized_date = MAX(normalized_date);
     *
     * The msg_box != 3 comparisons ensure that we don't include draft
     * messages.
     */
    private Cursor getConversations(String[] projection, String selection,
            String sortOrder, String smsTable, String pduTable) {
        SQLiteQueryBuilder mmsQueryBuilder = new SQLiteQueryBuilder();
        SQLiteQueryBuilder smsQueryBuilder = new SQLiteQueryBuilder();

        mmsQueryBuilder.setTables(pduTable);
        smsQueryBuilder.setTables(smsTable);

        String[] columns = handleNullMessageProjection(projection);
        String[] innerMmsProjection = makeProjectionWithDateAndThreadId(
                UNION_COLUMNS, 1000);
        String[] innerSmsProjection = makeProjectionWithDateAndThreadId(
                UNION_COLUMNS, 1);
        String mmsSubQuery = mmsQueryBuilder.buildUnionSubQuery(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN, innerMmsProjection,
                MMS_COLUMNS, 1, "mms",
                concatSelections(selection, MMS_CONVERSATION_CONSTRAINT),
                "thread_id", "date = MAX(date)");
        String smsSubQuery = smsQueryBuilder.buildUnionSubQuery(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN, innerSmsProjection,
                SMS_COLUMNS, 1, "sms",
                concatSelections(selection, SMS_CONVERSATION_CONSTRAINT),
                "thread_id", "date = MAX(date)");
        SQLiteQueryBuilder unionQueryBuilder = new SQLiteQueryBuilder();

        unionQueryBuilder.setDistinct(true);

        String unionQuery = unionQueryBuilder.buildUnionQuery(
                new String[] { mmsSubQuery, smsSubQuery }, null, null);

        SQLiteQueryBuilder outerQueryBuilder = new SQLiteQueryBuilder();

        outerQueryBuilder.setTables("(" + unionQuery + ")");

        String outerQuery = outerQueryBuilder.buildQuery(
                columns, null, "tid",
                "normalized_date = MAX(normalized_date)", sortOrder, null);

        return mOpenHelper.getReadableDatabase().rawQuery(outerQuery, EMPTY_STRING_ARRAY);
    }

    /**
     * Return the first locked message found in the union of MMS
     * and SMS messages.
     *
     * Use this query:
     *
     *  SELECT _id FROM pdu GROUP BY _id HAVING locked=1 UNION SELECT _id FROM sms GROUP
     *      BY _id HAVING locked=1 LIMIT 1
     *
     * We limit by 1 because we're only interested in knowing if
     * there is *any* locked message, not the actual messages themselves.
     */
    private Cursor getFirstLockedMessage(String[] projection, String selection,
            String sortOrder, String smsTable, String pduTable) {
        SQLiteQueryBuilder mmsQueryBuilder = new SQLiteQueryBuilder();
        SQLiteQueryBuilder smsQueryBuilder = new SQLiteQueryBuilder();

        mmsQueryBuilder.setTables(pduTable);
        smsQueryBuilder.setTables(smsTable);

        String[] idColumn = new String[] { BaseColumns._ID };

        // NOTE: buildUnionSubQuery *ignores* selectionArgs
        String mmsSubQuery = mmsQueryBuilder.buildUnionSubQuery(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN, idColumn,
                null, 1, "mms",
                selection,
                BaseColumns._ID, "locked=1");

        String smsSubQuery = smsQueryBuilder.buildUnionSubQuery(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN, idColumn,
                null, 1, "sms",
                selection,
                BaseColumns._ID, "locked=1");

        SQLiteQueryBuilder unionQueryBuilder = new SQLiteQueryBuilder();

        unionQueryBuilder.setDistinct(true);

        String unionQuery = unionQueryBuilder.buildUnionQuery(
                new String[] { mmsSubQuery, smsSubQuery }, null, "1");

        Cursor cursor = mOpenHelper.getReadableDatabase().rawQuery(unionQuery, EMPTY_STRING_ARRAY);

        if (DEBUG) {
            Log.v("MmsSmsProvider", "getFirstLockedMessage query: " + unionQuery);
            Log.v("MmsSmsProvider", "cursor count: " + cursor.getCount());
        }
        return cursor;
    }

    /**
     * Return every message in each conversation in both MMS
     * and SMS.
     */
    private Cursor getCompleteConversations(String[] projection,
            String selection, String sortOrder, String smsTable, String pduTable) {
        String unionQuery = buildConversationQuery(projection, selection, sortOrder, smsTable,
                pduTable);

        return mOpenHelper.getReadableDatabase().rawQuery(unionQuery, EMPTY_STRING_ARRAY);
    }

    /**
     * Add normalized date and thread_id to the list of columns for an
     * inner projection.  This is necessary so that the outer query
     * can have access to these columns even if the caller hasn't
     * requested them in the result.
     */
    private String[] makeProjectionWithDateAndThreadId(
            String[] projection, int dateMultiple) {
        int projectionSize = projection.length;
        String[] result = new String[projectionSize + 2];

        result[0] = "thread_id AS tid";
        result[1] = "date * " + dateMultiple + " AS normalized_date";
        for (int i = 0; i < projectionSize; i++) {
            result[i + 2] = projection[i];
        }
        return result;
    }

    /**
     * Return the union of MMS and SMS messages for this thread ID.
     */
    private Cursor getConversationMessages(
            String threadIdString, String[] projection, String selection,
            String sortOrder, String smsTable, String pduTable) {
        try {
            Long.parseLong(threadIdString);
        } catch (NumberFormatException exception) {
            Log.e(LOG_TAG, "Thread ID must be a Long.");
            return null;
        }

        String finalSelection = concatSelections(
                selection, "thread_id = " + threadIdString);
        String unionQuery = buildConversationQuery(projection, finalSelection, sortOrder, smsTable,
                pduTable);

        return mOpenHelper.getReadableDatabase().rawQuery(unionQuery, EMPTY_STRING_ARRAY);
    }

    /**
     * Return the union of MMS and SMS messages whose recipients
     * included this phone number.
     *
     * Use this query:
     *
     * SELECT ...
     *   FROM pdu, (SELECT msg_id AS address_msg_id
     *              FROM addr
     *              WHERE (address='<phoneNumber>' OR
     *              PHONE_NUMBERS_EQUAL(addr.address, '<phoneNumber>', 1/0)))
     *             AS matching_addresses
     *   WHERE pdu._id = matching_addresses.address_msg_id
     * UNION
     * SELECT ...
     *   FROM sms
     *   WHERE (address='<phoneNumber>' OR PHONE_NUMBERS_EQUAL(sms.address, '<phoneNumber>', 1/0));
     */
    private Cursor getMessagesByPhoneNumber(
            String phoneNumber, String[] projection, String selection,
            String sortOrder, String smsTable, String pduTable) {
        String escapedPhoneNumber = DatabaseUtils.sqlEscapeString(phoneNumber);
        String finalMmsSelection =
                concatSelections(
                        selection,
                        pduTable + "._id = matching_addresses.address_msg_id");
        String finalSmsSelection =
                concatSelections(
                        selection,
                        "(address=" + escapedPhoneNumber + " OR PHONE_NUMBERS_EQUAL(address, " +
                        escapedPhoneNumber +
                        (mUseStrictPhoneNumberComparation ? ", 1))" : ", 0))"));
        SQLiteQueryBuilder mmsQueryBuilder = new SQLiteQueryBuilder();
        SQLiteQueryBuilder smsQueryBuilder = new SQLiteQueryBuilder();

        mmsQueryBuilder.setDistinct(true);
        smsQueryBuilder.setDistinct(true);
        mmsQueryBuilder.setTables(
                pduTable +
                ", (SELECT msg_id AS address_msg_id " +
                "FROM addr WHERE (address=" + escapedPhoneNumber +
                " OR PHONE_NUMBERS_EQUAL(addr.address, " +
                escapedPhoneNumber +
                (mUseStrictPhoneNumberComparation ? ", 1))) " : ", 0))) ") +
                "AS matching_addresses");
        smsQueryBuilder.setTables(smsTable);

        String[] columns = handleNullMessageProjection(projection);
        String mmsSubQuery = mmsQueryBuilder.buildUnionSubQuery(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN, columns, MMS_COLUMNS,
                0, "mms", finalMmsSelection, null, null);
        String smsSubQuery = smsQueryBuilder.buildUnionSubQuery(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN, columns, SMS_COLUMNS,
                0, "sms", finalSmsSelection, null, null);
        SQLiteQueryBuilder unionQueryBuilder = new SQLiteQueryBuilder();

        unionQueryBuilder.setDistinct(true);

        String unionQuery = unionQueryBuilder.buildUnionQuery(
                new String[] { mmsSubQuery, smsSubQuery }, sortOrder, null);

        return mOpenHelper.getReadableDatabase().rawQuery(unionQuery, EMPTY_STRING_ARRAY);
    }

    /**
     * Return the conversation of certain thread ID.
     */
    private Cursor getConversationById(
            String threadIdString, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        try {
            Long.parseLong(threadIdString);
        } catch (NumberFormatException exception) {
            Log.e(LOG_TAG, "Thread ID must be a Long.");
            return null;
        }

        String extraSelection = "_id=" + threadIdString;
        String finalSelection = concatSelections(selection, extraSelection);
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        String[] columns = handleNullThreadsProjection(projection);

        queryBuilder.setDistinct(true);
        queryBuilder.setTables(TABLE_THREADS);
        return queryBuilder.query(
                mOpenHelper.getReadableDatabase(), columns, finalSelection,
                selectionArgs, sortOrder, null, null);
    }

    private static String joinPduAndPendingMsgTables(String pduTable) {
        return pduTable + " LEFT JOIN " + TABLE_PENDING_MSG
                + " ON " + pduTable + "._id = pending_msgs.msg_id";
    }

    private static String[] createMmsProjection(String[] old, String pduTable) {
        String[] newProjection = new String[old.length];
        for (int i = 0; i < old.length; i++) {
            if (old[i].equals(BaseColumns._ID)) {
                newProjection[i] = pduTable + "._id";
            } else {
                newProjection[i] = old[i];
            }
        }
        return newProjection;
    }

    private Cursor getUndeliveredMessages(
            String[] projection, String selection, String[] selectionArgs,
            String sortOrder, String smsTable, String pduTable) {
        String[] mmsProjection = createMmsProjection(projection, pduTable);

        SQLiteQueryBuilder mmsQueryBuilder = new SQLiteQueryBuilder();
        SQLiteQueryBuilder smsQueryBuilder = new SQLiteQueryBuilder();

        mmsQueryBuilder.setTables(joinPduAndPendingMsgTables(pduTable));
        smsQueryBuilder.setTables(smsTable);

        String finalMmsSelection = concatSelections(
                selection, Mms.MESSAGE_BOX + " = " + Mms.MESSAGE_BOX_OUTBOX);
        String finalSmsSelection = concatSelections(
                selection, "(" + Sms.TYPE + " = " + Sms.MESSAGE_TYPE_OUTBOX
                + " OR " + Sms.TYPE + " = " + Sms.MESSAGE_TYPE_FAILED
                + " OR " + Sms.TYPE + " = " + Sms.MESSAGE_TYPE_QUEUED + ")");

        String[] smsColumns = handleNullMessageProjection(projection);
        String[] mmsColumns = handleNullMessageProjection(mmsProjection);
        String[] innerMmsProjection = makeProjectionWithDateAndThreadId(
                mmsColumns, 1000);
        String[] innerSmsProjection = makeProjectionWithDateAndThreadId(
                smsColumns, 1);

        Set<String> columnsPresentInTable = new HashSet<String>(MMS_COLUMNS);
        columnsPresentInTable.add(pduTable + "._id");
        columnsPresentInTable.add(PendingMessages.ERROR_TYPE);
        String mmsSubQuery = mmsQueryBuilder.buildUnionSubQuery(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN, innerMmsProjection,
                columnsPresentInTable, 1, "mms", finalMmsSelection,
                null, null);
        String smsSubQuery = smsQueryBuilder.buildUnionSubQuery(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN, innerSmsProjection,
                SMS_COLUMNS, 1, "sms", finalSmsSelection,
                null, null);
        SQLiteQueryBuilder unionQueryBuilder = new SQLiteQueryBuilder();

        unionQueryBuilder.setDistinct(true);

        String unionQuery = unionQueryBuilder.buildUnionQuery(
                new String[] { smsSubQuery, mmsSubQuery }, null, null);

        SQLiteQueryBuilder outerQueryBuilder = new SQLiteQueryBuilder();

        outerQueryBuilder.setTables("(" + unionQuery + ")");

        String outerQuery = outerQueryBuilder.buildQuery(
                smsColumns, null, null, null, sortOrder, null);

        return mOpenHelper.getReadableDatabase().rawQuery(outerQuery, EMPTY_STRING_ARRAY);
    }

    /**
     * Add normalized date to the list of columns for an inner
     * projection.
     */
    private static String[] makeProjectionWithNormalizedDate(
            String[] projection, int dateMultiple) {
        int projectionSize = projection.length;
        String[] result = new String[projectionSize + 1];

        result[0] = "date * " + dateMultiple + " AS normalized_date";
        System.arraycopy(projection, 0, result, 1, projectionSize);
        return result;
    }

    private static String buildConversationQuery(String[] projection,
            String selection, String sortOrder, String smsTable, String pduTable) {
        String[] mmsProjection = createMmsProjection(projection, pduTable);

        SQLiteQueryBuilder mmsQueryBuilder = new SQLiteQueryBuilder();
        SQLiteQueryBuilder smsQueryBuilder = new SQLiteQueryBuilder();

        mmsQueryBuilder.setDistinct(true);
        smsQueryBuilder.setDistinct(true);
        mmsQueryBuilder.setTables(joinPduAndPendingMsgTables(pduTable));
        smsQueryBuilder.setTables(smsTable);

        String[] smsColumns = handleNullMessageProjection(projection);
        String[] mmsColumns = handleNullMessageProjection(mmsProjection);
        String[] innerMmsProjection = makeProjectionWithNormalizedDate(mmsColumns, 1000);
        String[] innerSmsProjection = makeProjectionWithNormalizedDate(smsColumns, 1);

        Set<String> columnsPresentInTable = new HashSet<String>(MMS_COLUMNS);
        columnsPresentInTable.add(pduTable + "._id");
        columnsPresentInTable.add(PendingMessages.ERROR_TYPE);

        String mmsSelection = concatSelections(selection,
                                Mms.MESSAGE_BOX + " != " + Mms.MESSAGE_BOX_DRAFTS);
        String mmsSubQuery = mmsQueryBuilder.buildUnionSubQuery(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN, innerMmsProjection,
                columnsPresentInTable, 0, "mms",
                concatSelections(mmsSelection, MMS_CONVERSATION_CONSTRAINT),
                null, null);
        String smsSubQuery = smsQueryBuilder.buildUnionSubQuery(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN, innerSmsProjection, SMS_COLUMNS,
                0, "sms", concatSelections(selection, SMS_CONVERSATION_CONSTRAINT),
                null, null);
        SQLiteQueryBuilder unionQueryBuilder = new SQLiteQueryBuilder();

        unionQueryBuilder.setDistinct(true);

        String unionQuery = unionQueryBuilder.buildUnionQuery(
                new String[] { smsSubQuery, mmsSubQuery },
                handleNullSortOrder(sortOrder), null);

        SQLiteQueryBuilder outerQueryBuilder = new SQLiteQueryBuilder();

        outerQueryBuilder.setTables("(" + unionQuery + ")");

        return outerQueryBuilder.buildQuery(
                smsColumns, null, null, null, sortOrder, null);
    }

    @Override
    public String getType(Uri uri) {
        return VND_ANDROID_DIR_MMS_SMS;
    }

    @Override
    public int delete(Uri uri, String selection,
            String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Context context = getContext();
        int affectedRows = 0;
		//android N update start
        System.out.println(" jessica URI_MATCHER.match(uri)==== " + URI_MATCHER.match(uri));
        switch(URI_MATCHER.match(uri)) {
            case URI_CONVERSATIONS_MESSAGES:
                long threadId;
                System.out.println(" jessica uri.toString()==== " + uri.toString());
                try {
                    threadId = Long.parseLong(uri.getLastPathSegment());
                } catch (NumberFormatException e) {
                    Log.e(LOG_TAG, "Thread ID must be a long.");
                    break;
                }
                affectedRows = deleteConversation(uri, selection, selectionArgs);
                MmsSmsDatabaseHelper.updateThread(db, threadId);
                break;
            case URI_CONVERSATIONS:
                 System.out.println(" jessica URI_CONVERSATIONS uri.toString()==== " + uri.toString());
                 System.out.println("jessica out URI_CONVERSATIONS selectionArgs = " + selectionArgs);
                 String[] szRet = getSelectCondition(selectionArgs);
                 System.out.println("jessica URI_CONVERSATIONS deleteConversation selection = " + selection);
                 if(szRet == null){
                 System.out.println("jessica out URI_CONVERSATIONS no sms or mms is deleted !!!!");
                        break;
                //affectedRows = MmsProvider.deleteMessages(context, db,
                //                        selection, selectionArgs, uri)
                //        + db.delete("sms", selection, selectionArgs);
                // Intentionally don't pass the selection variable to updateAllThreads.
                // When we pass in "locked=0" there, the thread will get excluded from
                // the selection and not get updated.
                }else{
                  if(szRet[1] != null){
                    affectedRows = MmsProvider.deleteMessages(getContext(), db, selection,
                                          new String[]{szRet[1]}, uri);
                  }
                  if(szRet[0] != null){
                     affectedRows =  affectedRows + db.delete("sms", selection, new String[]{szRet[0]});
                   }
                }//android N update end
                MmsSmsDatabaseHelper.updateAllThreads(db, null, null);
                break;
            case URI_OBSOLETE_THREADS:
                affectedRows = db.delete(TABLE_THREADS,
                        "_id NOT IN (SELECT DISTINCT thread_id FROM sms where thread_id NOT NULL " +
                        "UNION SELECT DISTINCT thread_id FROM pdu where thread_id NOT NULL)", null);
                break;
            default:
                throw new UnsupportedOperationException(NO_DELETES_INSERTS_OR_UPDATES + uri);
        }

        if (affectedRows > 0) {
            context.getContentResolver().notifyChange(MmsSms.CONTENT_URI, null, true,
                    UserHandle.USER_ALL);
        }
        return affectedRows;
    }

    //android N update start
    private void  SetConditionToArray(String[] szRet, String szCondition)
    {
      if( szCondition == null)
        {
            return;
        }

       String szKey = szCondition.trim();
       if(szKey==null || szKey.length() <=0)
         {
            return;
         }
       System.out.println("jessica enter SetConditionToArray szRet[0] = " + szRet[0] + " szRet[1] = " + szRet[1]);
       String[] szConditionArray = new String[2];
       szConditionArray = szCondition.split("=");
       szKey = szConditionArray[0].trim();
       if(szKey.equals("maxSms")){
         szRet[0] = szConditionArray[1].trim();
       } else if(szKey.equals("maxMms")){
         szRet[1] = szConditionArray[1].trim();
       }else{
             return;
       }
       System.out.println("jessica SetConditionToArray szRet[0] = " + szRet[0] + " szRet[1] = " + szRet[1]);
    }
    private String[] getSelectCondition(String[] szInCondition){
      /*
        * 0 : SMS
        * 1 :  MMS
      */
      if(szInCondition == null || szInCondition[0] == null )
        {
            return null;
        }
       String szCondition = szInCondition[0];
       String[] szRet = new String[2];
       szRet[0] = null;
       szRet[1] = null;
       boolean allMax = false;
       if( szCondition == null)
         {
           return null;
        }
      char szChar;
      for(int index=0; index < szInCondition[0].length(); index++)
        {
         szChar = szInCondition[0].charAt(index);
         if(szChar == '&'){
             allMax = true;
              break;
          }
       }
      if(!allMax){
         System.out.println("jessica getSelectCondition szCondition= " + szCondition);
         SetConditionToArray(szRet, szCondition);
      }else{
         String[] szConditionArray = new String[2];
         szConditionArray = szCondition.split("&");
         if(szConditionArray[0] != null && szConditionArray[1] != null)
           {
             System.out.println("jessica getSelectCondition szConditionArray[0]= " + szConditionArray[0]
                           + " szConditionArray[1] = " + szConditionArray[1]);
             SetConditionToArray(szRet, szConditionArray[0]);
             SetConditionToArray(szRet, szConditionArray[1]);
           }
      }
        if( szRet[0] == null && szRet[1] == null){
            return null;
         }else {
            return szRet;
         }
    }//android N update end

    /**
     * Delete the conversation with the given thread ID.
     */
    private int deleteConversation(Uri uri, String selection, String[] selectionArgs) {
        String threadId = uri.getLastPathSegment();

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String finalSelection = concatSelections(selection, "thread_id = " + threadId);
		//android N update start
        System.out.println("jessica deleteConversation selection = " + selection);
        // bug 495194 : add for search feature begin
        SearchSyncHelper.getInstance(getContext(), db).deleteRecord((Integer.valueOf(threadId)).intValue());
        // bug 495194 : add for search feature end
		//android N update end
        return MmsProvider.deleteMessages(getContext(), db, finalSelection,
                                          selectionArgs, uri)
                + db.delete("sms", finalSelection, selectionArgs);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (URI_MATCHER.match(uri) == URI_PENDING_MSG) {
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            long rowId = db.insert(TABLE_PENDING_MSG, null, values);
            return Uri.parse(uri + "/" + rowId);
        }
        throw new UnsupportedOperationException(NO_DELETES_INSERTS_OR_UPDATES + uri);
    }

    @Override
    public int update(Uri uri, ContentValues values,
            String selection, String[] selectionArgs) {
        final int callerUid = Binder.getCallingUid();
        final String callerPkg = getCallingPackage();
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int affectedRows = 0;
        try {   // Add by SPRD for bug 543275 android N update start
            switch(URI_MATCHER.match(uri)) {
                case URI_CONVERSATIONS_MESSAGES:
                    String threadIdString = uri.getPathSegments().get(1);
                    affectedRows = updateConversation(threadIdString, values,
                            selection, selectionArgs, callerUid, callerPkg);
                    break;

            case URI_PENDING_MSG:
                affectedRows = db.update(TABLE_PENDING_MSG, values, selection, null);
                break;

            case URI_CANONICAL_ADDRESS: {
                String extraSelection = "_id=" + uri.getPathSegments().get(1);
                String finalSelection = TextUtils.isEmpty(selection)
                        ? extraSelection : extraSelection + " AND " + selection;

                affectedRows = db.update(TABLE_CANONICAL_ADDRESSES, values, finalSelection, null);
                break;
            }

            case URI_CONVERSATIONS: {
                final ContentValues finalValues = new ContentValues(1);
                if (values.containsKey(Threads.ARCHIVED)) {
                    // Only allow update archived
                    finalValues.put(Threads.ARCHIVED, values.getAsBoolean(Threads.ARCHIVED));
                }
                affectedRows = db.update(TABLE_THREADS, finalValues, selection, selectionArgs);
                break;
            }

                default:
                    throw new UnsupportedOperationException(
                            NO_DELETES_INSERTS_OR_UPDATES + uri);
            }
        /* Add by SPRD for bug 543275 Start */
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Exception occured when perform update: " + e.getMessage());
        }
        /* Add by SPRD for bug 543275 End */
		//android N update end


        if (affectedRows > 0) {
            getContext().getContentResolver().notifyChange(
                    MmsSms.CONTENT_URI, null, true, UserHandle.USER_ALL);
        }
        return affectedRows;
    }

    private int updateConversation(String threadIdString, ContentValues values, String selection,
            String[] selectionArgs, int callerUid, String callerPkg) {
        try {
            Long.parseLong(threadIdString);
        } catch (NumberFormatException exception) {
            Log.e(LOG_TAG, "Thread ID must be a Long.");
            return 0;

        }
        if (ProviderUtil.shouldRemoveCreator(values, callerUid)) {
            // CREATOR should not be changed by non-SYSTEM/PHONE apps
            Log.w(LOG_TAG, callerPkg + " tries to update CREATOR");
            // Sms.CREATOR and Mms.CREATOR are same. But let's do this
            // twice in case the names may differ in the future
            values.remove(Sms.CREATOR);
            values.remove(Mms.CREATOR);
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String finalSelection = concatSelections(selection, "thread_id=" + threadIdString);
        return db.update(MmsProvider.TABLE_PDU, values, finalSelection, selectionArgs)
                + db.update("sms", values, finalSelection, selectionArgs);
    }

    /**
     * Construct Sets of Strings containing exactly the columns
     * present in each table.  We will use this when constructing
     * UNION queries across the MMS and SMS tables.
     */
    private static void initializeColumnSets() {
        int commonColumnCount = MMS_SMS_COLUMNS.length;
        int mmsOnlyColumnCount = MMS_ONLY_COLUMNS.length;
        int smsOnlyColumnCount = SMS_ONLY_COLUMNS.length;
        Set<String> unionColumns = new HashSet<String>();

        for (int i = 0; i < commonColumnCount; i++) {
            MMS_COLUMNS.add(MMS_SMS_COLUMNS[i]);
            SMS_COLUMNS.add(MMS_SMS_COLUMNS[i]);
            unionColumns.add(MMS_SMS_COLUMNS[i]);
        }
        for (int i = 0; i < mmsOnlyColumnCount; i++) {
            MMS_COLUMNS.add(MMS_ONLY_COLUMNS[i]);
            unionColumns.add(MMS_ONLY_COLUMNS[i]);
        }
        for (int i = 0; i < smsOnlyColumnCount; i++) {
            SMS_COLUMNS.add(SMS_ONLY_COLUMNS[i]);
            unionColumns.add(SMS_ONLY_COLUMNS[i]);
        }

        int i = 0;
        for (String columnName : unionColumns) {
            UNION_COLUMNS[i++] = columnName;
        }
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        // Dump default SMS app
        String defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(getContext());
        if (TextUtils.isEmpty(defaultSmsApp)) {
            defaultSmsApp = "None";
        }
        writer.println("Default SMS app: " + defaultSmsApp);
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (METHOD_IS_RESTORING.equals(method)) {
            Bundle result = new Bundle();
            result.putBoolean(IS_RESTORING_KEY, TelephonyBackupAgent.getIsRestoring());
            return result;
        }
        Log.w(LOG_TAG, "Ignored unsupported " + method + " call");
        return null;
    }
}
