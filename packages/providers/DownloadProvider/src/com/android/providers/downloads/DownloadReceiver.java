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

package com.android.providers.downloads;

import static android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED;
import static android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION;

import static com.android.providers.downloads.Helpers.getAsyncHandler;
import static com.android.providers.downloads.Helpers.getDownloadNotifier;
import static com.android.providers.downloads.Helpers.getInt;
import static com.android.providers.downloads.Helpers.getString;
import static com.android.providers.downloads.Helpers.getSystemFacade;

import android.app.DownloadManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Downloads;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import java.util.List;
import android.widget.Toast;
import android.net.NetworkInfo;
import android.net.ConnectivityManager;
/*
 * for downloadprovider_DRM
 *@{
 */
import com.android.providers.downloadsplugin.DownloadsDRMUtils;
/*@}*/

/**
 * Receives system broadcasts (boot, network connectivity)
 */
public class DownloadReceiver extends BroadcastReceiver {
    private static final String TAG = "DownloadManager:DownloadReceiver";
    private static final boolean DEBUG = true;
    /**
     * Intent extra included with {@link Constants#ACTION_CANCEL} intents,
     * indicating the IDs (as array of long) of the downloads that were
     * canceled.
     */
    public static final String EXTRA_CANCELED_DOWNLOAD_IDS =
            "com.android.providers.downloads.extra.CANCELED_DOWNLOAD_IDS";

    /**
     * Intent extra included with {@link Constants#ACTION_CANCEL} intents,
     * indicating the tag of the notification corresponding to the download(s)
     * that were canceled; this notification must be canceled.
     */
    public static final String EXTRA_CANCELED_DOWNLOAD_NOTIFICATION_TAG =
            "com.android.providers.downloads.extra.CANCELED_DOWNLOAD_NOTIFICATION_TAG";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        if (DEBUG) Log.d(TAG, "onReceive action="+action);
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
            final PendingResult result = goAsync();
            getAsyncHandler().post(new Runnable() {
                @Override
                public void run() {
                    handleBootCompleted(context);
                    handleMediaMount(context, intent.getData());
                    result.finish();
                }
            });
        } else if (Intent.ACTION_UID_REMOVED.equals(action)) {
            final PendingResult result = goAsync();
            getAsyncHandler().post(new Runnable() {
                @Override
                public void run() {
                    handleUidRemoved(context, intent);
                    result.finish();
                }
            });

        } else if (Constants.ACTION_OPEN.equals(action)
                || Constants.ACTION_LIST.equals(action)
                || Constants.ACTION_HIDE.equals(action)) {

            final PendingResult result = goAsync();
            if (result == null) {
                // TODO: remove this once test is refactored
                handleNotificationBroadcast(context, intent);
            } else {
                getAsyncHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        handleNotificationBroadcast(context, intent);
                        result.finish();
                    }
                });
            }
        } else if (Constants.ACTION_CANCEL.equals(action)) {
            long[] downloadIds = intent.getLongArrayExtra(
                    DownloadReceiver.EXTRA_CANCELED_DOWNLOAD_IDS);
            DownloadManager manager = (DownloadManager) context.getSystemService(
                    Context.DOWNLOAD_SERVICE);
            manager.remove(downloadIds);

            String notifTag = intent.getStringExtra(
                    DownloadReceiver.EXTRA_CANCELED_DOWNLOAD_NOTIFICATION_TAG);
            NotificationManager notifManager = (NotificationManager) context.getSystemService(
                    Context.NOTIFICATION_SERVICE);
            notifManager.cancel(notifTag, 0);
        }
        /*
         * for downloadprovider_DRM
         *@{
         */
        else if (action.equals(DownloadsDRMUtils.DELETE_DOWNLOAD_INFORMATION)){
            DownloadsDRMUtils.getInstance(context).deleteDatabase(context, intent);
        }
        /*@}*/
    }

    private void handleBootCompleted(Context context) {
        if (DownloadManager.isDownloadAlertEnabled()) {
            Helpers.resetUnfinishedDownload(context);
        }
        // Show any relevant notifications for completed downloads
        getDownloadNotifier(context).update();

        // Schedule all downloads that are ready
        final ContentResolver resolver = context.getContentResolver();
        try (Cursor cursor = resolver.query(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, null, null,
                null, null)) {
            final DownloadInfo.Reader reader = new DownloadInfo.Reader(resolver, cursor);
            final DownloadInfo info = new DownloadInfo(context);
            while (cursor.moveToNext()) {
                reader.updateFromDatabase(info);
                Helpers.scheduleJob(context, info);
            }
        }

        // Schedule idle pass to clean up orphaned files
        DownloadIdleService.scheduleIdlePass(context);
    }

    private void handleMediaMount(Context context, Uri mountUri) {
        if (mountUri != null) {
            String volumePath = mountUri.getPath();
            if (Helpers.isWritePermissionNeeded(volumePath)) {
                boolean hasPermission = isVolumeHasWritePermission(context, volumePath);
                if (DEBUG) Log.d(TAG, "handleMediaMount, volume="+volumePath+"  hasPermission="+hasPermission);
                if (!hasPermission) {
                    final Intent intent = new Intent(Constants.ACTION_DOWNLOAD_REQUEST_WRITE_PERMISSION);
                    intent.putExtra(Constants.VOLUME_PATH, volumePath);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            }
        }
    }

    private boolean isVolumeHasWritePermission(Context context, String volumePath){
        Uri volumeUri = Uri.parse(Helpers.getUriPermission(volumePath));
        List<UriPermission> permissions = context.getContentResolver().getPersistedUriPermissions();
        for(UriPermission permission: permissions){
            if(volumeUri.equals(permission.getUri())){
                return true;
            }
        }
        return false;
    }

    private void handleUidRemoved(Context context, Intent intent) {
        final ContentResolver resolver = context.getContentResolver();
        final int uid = intent.getIntExtra(Intent.EXTRA_UID, -1);

        // First, disown any downloads that live in shared storage
        final ContentValues values = new ContentValues();
        values.putNull(Constants.UID);
        final int disowned = resolver.update(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, values,
                Constants.UID + "=" + uid + " AND " + Downloads.Impl.COLUMN_DESTINATION + " IN ("
                        + Downloads.Impl.DESTINATION_EXTERNAL + ","
                        + Downloads.Impl.DESTINATION_FILE_URI + ")",
                null);

        // Finally, delete any remaining downloads owned by UID
        final int deleted = resolver.delete(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                Constants.UID + "=" + uid, null);

        if ((disowned + deleted) > 0) {
            Slog.d(TAG, "Disowned " + disowned + " and deleted " + deleted
                    + " downloads owned by UID " + uid);
        }
    }

    /**
     * Handle any broadcast related to a system notification.
     */
    private void handleNotificationBroadcast(Context context, Intent intent) {
        final String action = intent.getAction();
        /*
         * for downloadprovider_DRM
         *@{
         */
        if(DownloadsDRMUtils.getInstance(context).handleDRMNotificationBroadcast(context, intent)) {
            Log.d(TAG, "handleNotificationBroadcast : DRM");
            return;
        }
        /*@}*/
        if (Constants.ACTION_LIST.equals(action)) {
            final long[] ids = intent.getLongArrayExtra(
                    DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS);
            sendNotificationClickedIntent(context, ids);

        } else if (Constants.ACTION_OPEN.equals(action)) {
            final long id = ContentUris.parseId(intent.getData());
            openDownload(context, id);
            hideNotification(context, id);

        } else if (Constants.ACTION_HIDE.equals(action)) {
            final long id = ContentUris.parseId(intent.getData());
            hideNotification(context, id);
        }
    }

    /**
     * Mark the given {@link DownloadManager#COLUMN_ID} as being acknowledged by
     * user so it's not renewed later.
     */
    private void hideNotification(Context context, long id) {
        final int status;
        final int visibility;

        final Uri uri = ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, id);
        final Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                status = getInt(cursor, Downloads.Impl.COLUMN_STATUS);
                visibility = getInt(cursor, Downloads.Impl.COLUMN_VISIBILITY);
            } else {
                Log.w(TAG, "Missing details for download " + id);
                return;
            }
        } finally {
            cursor.close();
        }

        if (Downloads.Impl.isStatusCompleted(status) &&
                (visibility == VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                || visibility == VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION)) {
            final ContentValues values = new ContentValues();
            values.put(Downloads.Impl.COLUMN_VISIBILITY,
                    Downloads.Impl.VISIBILITY_VISIBLE);
            context.getContentResolver().update(uri, values, null, null);
        }
    }

    /**
     * Start activity to display the file represented by the given
     * {@link DownloadManager#COLUMN_ID}.
     */
    private void openDownload(Context context, long id) {
        if (!OpenHelper.startViewIntent(context, id, Intent.FLAG_ACTIVITY_NEW_TASK)) {
            Toast.makeText(context, R.string.download_no_application_title, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    /**
     * Notify the owner of a running download that its notification was clicked.
     */
    private void sendNotificationClickedIntent(Context context, long[] ids) {
        final String packageName;
        final String clazz;
        final boolean isPublicApi;

        final Uri uri = ContentUris.withAppendedId(
                Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, ids[0]);
        final Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                packageName = getString(cursor, Downloads.Impl.COLUMN_NOTIFICATION_PACKAGE);
                clazz = getString(cursor, Downloads.Impl.COLUMN_NOTIFICATION_CLASS);
                isPublicApi = getInt(cursor, Downloads.Impl.COLUMN_IS_PUBLIC_API) != 0;
            } else {
                Log.w(TAG, "Missing details for download " + ids[0]);
                return;
            }
        } finally {
            cursor.close();
        }

        if (TextUtils.isEmpty(packageName)) {
            Log.w(TAG, "Missing package; skipping broadcast");
            return;
        }

        Intent appIntent = null;
        if (isPublicApi) {
            appIntent = new Intent(DownloadManager.ACTION_NOTIFICATION_CLICKED);
            appIntent.setPackage(packageName);
            appIntent.putExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS, ids);

        } else { // legacy behavior
            if (TextUtils.isEmpty(clazz)) {
                Log.w(TAG, "Missing class; skipping broadcast");
                return;
            }

            appIntent = new Intent(DownloadManager.ACTION_NOTIFICATION_CLICKED);
            appIntent.setClassName(packageName, clazz);
            appIntent.putExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS, ids);

            if (ids.length == 1) {
                appIntent.setData(uri);
            } else {
                appIntent.setData(Downloads.Impl.CONTENT_URI);
            }
        }

        getSystemFacade(context).sendBroadcast(appIntent);
    }
}
