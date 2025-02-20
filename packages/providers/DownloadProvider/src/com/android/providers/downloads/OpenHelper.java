/*
 * Copyright (C) 2012 The Android Open Source Project
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

import static android.app.DownloadManager.COLUMN_LOCAL_FILENAME;
import static android.app.DownloadManager.COLUMN_MEDIA_TYPE;
import static android.app.DownloadManager.COLUMN_URI;
import static android.provider.Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI;

import static com.android.providers.downloads.Constants.TAG;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.database.Cursor;
import android.net.Uri;
import android.os.Process;
import android.provider.DocumentsContract;
import android.provider.Downloads.Impl.RequestHeaders;
import android.util.Log;

import java.io.File;
/*
 * for downloadprovider_DRM
 *@{
 */
import com.android.providers.downloadsplugin.OpenHelperDRMUtil;
/*@}*/

public class OpenHelper {
    /**
     * Build and start an {@link Intent} to view the download with given ID,
     * handling subtleties around installing packages.
     */
    public static boolean startViewIntent(Context context, long id, int intentFlags) {
        final Intent intent = OpenHelper.buildViewIntent(context, id);
        if (intent == null) {
            Log.w(TAG, "No intent built for " + id);
            return false;
        }

        intent.addFlags(intentFlags);
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "Failed to start " + intent + ": " + e);
            return false;
        }
    }

    /**
     * Build an {@link Intent} to view the download with given ID, handling
     * subtleties around installing packages.
     */
    /*
     * for downloadprovider_DRM
     * original code
     private static Intent buildViewIntent(Context context, long id) {
     *@{
     */
    public static Intent buildViewIntent(Context context, long id) {
    /*@}*/
        final DownloadManager downManager = (DownloadManager) context.getSystemService(
                Context.DOWNLOAD_SERVICE);
        downManager.setAccessAllDownloads(true);
        downManager.setAccessFilename(true);

        final Cursor cursor = downManager.query(new DownloadManager.Query().setFilterById(id));
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }

            final Uri localUri = getCursorUri(cursor, DownloadManager.COLUMN_LOCAL_URI);
            final File file = getCursorFile(cursor, DownloadManager.COLUMN_LOCAL_FILENAME);
            /*
             * for downloadprovider_DRM
             * original code
             String mimeType = getCursorString(cursor, COLUMN_MEDIA_TYPE);
             mimeType = DownloadDrmHelper.getOriginalMimeType(context, file, mimeType);

             final Uri documentUri = DocumentsContract.buildDocumentUri(
                     Constants.STORAGE_AUTHORITY, String.valueOf(id));

             final Intent intent = new Intent(Intent.ACTION_VIEW);
             *@{
             */
            String originalMimeType = getCursorString(cursor, DownloadManager.COLUMN_MEDIA_TYPE);
            if ((file != null) && (!file.exists())){
                return null;
            }
            String mimeType = OpenHelperDRMUtil.getInstance(context).getDRMPluginMimeType(context, file, originalMimeType, cursor);
            if ((mimeType != null) && (mimeType.equals("image/jpg"))){
                mimeType = "image/jpeg";
            }
            //modify for flv video support in GMS version
            if ((mimeType != null) && ((mimeType.equals("video/flv") || mimeType.equals("video/x-flv")))){
                mimeType = "video/*";
            }
            final Uri documentUri = DocumentsContract.buildDocumentUri(
                    Constants.STORAGE_AUTHORITY, String.valueOf(id));

            final Intent intent = OpenHelperDRMUtil.getInstance(context).setDRMPluginIntent(file, originalMimeType);
            /*@}*/
            if ("application/vnd.android.package-archive".equals(mimeType) || "application/vnd.android".equals(mimeType)) {
                // PackageInstaller doesn't like content URIs, so open file
                mimeType = "application/vnd.android.package-archive";
                intent.setDataAndType(localUri, mimeType);

                // Also splice in details about where it came from
                final Uri remoteUri = getCursorUri(cursor, DownloadManager.COLUMN_URI);
                intent.putExtra(Intent.EXTRA_ORIGINATING_URI, remoteUri);
                intent.putExtra(Intent.EXTRA_REFERRER, getRefererUri(context, id));
                intent.putExtra(Intent.EXTRA_ORIGINATING_UID, getOriginatingUid(context, id));
            /*
             * for downloadprovider_DRM
             *@{
             */
            } else if ("application/vnd.oma.drm.content".equals(originalMimeType)) {
                intent.setDataAndType(localUri, mimeType);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            /*@}*/
            } else {
                intent.setDataAndType(documentUri, mimeType);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }

            return intent;
        } finally {
            cursor.close();
        }
    }

    private static Uri getRefererUri(Context context, long id) {
        final Uri headersUri = Uri.withAppendedPath(
                ContentUris.withAppendedId(ALL_DOWNLOADS_CONTENT_URI, id),
                RequestHeaders.URI_SEGMENT);
        final Cursor headers = context.getContentResolver()
                .query(headersUri, null, null, null, null);
        try {
            while (headers.moveToNext()) {
                final String header = getCursorString(headers, RequestHeaders.COLUMN_HEADER);
                if ("Referer".equalsIgnoreCase(header)) {
                    return getCursorUri(headers, RequestHeaders.COLUMN_VALUE);
                }
            }
        } finally {
            headers.close();
        }
        return null;
    }

    private static int getOriginatingUid(Context context, long id) {
        final Uri uri = ContentUris.withAppendedId(ALL_DOWNLOADS_CONTENT_URI, id);
        final Cursor cursor = context.getContentResolver().query(uri, new String[]{Constants.UID},
                null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    final int uid = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.UID));
                    if (uid != Process.myUid()) {
                        return uid;
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return PackageInstaller.SessionParams.UID_UNKNOWN;
    }

    private static String getCursorString(Cursor cursor, String column) {
        return cursor.getString(cursor.getColumnIndexOrThrow(column));
    }

    private static Uri getCursorUri(Cursor cursor, String column) {
        return Uri.parse(getCursorString(cursor, column));
    }

    private static File getCursorFile(Cursor cursor, String column) {
        return new File(cursor.getString(cursor.getColumnIndexOrThrow(column)));
    }
}
