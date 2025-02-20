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
 * limitations under the License.
 */

package com.android.dialer.contactsfragment;

import android.content.Context;
import android.content.CursorLoader;
import android.provider.ContactsContract;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import com.android.contacts.common.preference.ContactsPreferences;

/** Cursor Loader for {@link ContactsFragment}. */
final class ContactsCursorLoader extends CursorLoader {

  public static final int CONTACT_ID = 0;
  public static final int CONTACT_DISPLAY_NAME = 1;
  public static final int CONTACT_PHOTO_ID = 2;
  public static final int CONTACT_PHOTO_URI = 3;
  public static final int CONTACT_LOOKUP_KEY = 4;
  /**
   * SPRD:add for show fdn,sim,sdn icon in dialer contactslist tab feature 840676.
   * @{
   */
  public static final int CONTACT_SYNC1_KEY = 5;
  public static final int CONTACT_SYNC2_KEY = 6;
  public static final int  CONTACT_ACCOUNT_TYPE = 7;
  public static final int CONTACT_ACCOUNT_NAME = 8;
  /**
   * @}
   */

  public static final String[] CONTACTS_PROJECTION_DISPLAY_NAME_PRIMARY =
      new String[] {
        Contacts._ID, // 0
        Contacts.DISPLAY_NAME_PRIMARY, // 1
        Contacts.PHOTO_ID, // 2
        Contacts.PHOTO_THUMBNAIL_URI, // 3
        Contacts.LOOKUP_KEY, // 4
        /**
        * SPRD:add for show fdn,sim,sdn icon in dialer contactslist tab feature 840676.
        * @{
        */
        ContactsContract.RawContacts.SYNC1,
        ContactsContract.RawContacts.SYNC2,
        Contacts.DISPLAY_ACCOUNT_TYPE,
        Contacts.DISPLAY_ACCOUNT_NAME
        /**
        * @}
        */
      };

  public static final String[] CONTACTS_PROJECTION_DISPLAY_NAME_ALTERNATIVE =
      new String[] {
        Contacts._ID, // 0
        Contacts.DISPLAY_NAME_ALTERNATIVE, // 1
        Contacts.PHOTO_ID, // 2
        Contacts.PHOTO_THUMBNAIL_URI, // 3
        Contacts.LOOKUP_KEY, // 4
        /**
         * SPRD:add for show fdn,sim,sdn icon in dialer contactslist tab feature 840676.
         * @{
         */
        ContactsContract.RawContacts.SYNC1,
        ContactsContract.RawContacts.SYNC2,
        Contacts.DISPLAY_ACCOUNT_TYPE,
        Contacts.DISPLAY_ACCOUNT_NAME
        /**
        * @}
        */
      };

  ContactsCursorLoader(Context context, boolean hasPhoneNumbers) {
    super(
        context,
        buildUri(""),
        getProjection(context),
        getWhere(context, hasPhoneNumbers),
        null,
        getSortKey(context) + " ASC");
  }

  private static String[] getProjection(Context context) {
    ContactsPreferences contactsPrefs = new ContactsPreferences(context);
    boolean displayOrderPrimary =
        (contactsPrefs.getDisplayOrder() == ContactsPreferences.DISPLAY_ORDER_PRIMARY);
    return displayOrderPrimary
        ? CONTACTS_PROJECTION_DISPLAY_NAME_PRIMARY
        : CONTACTS_PROJECTION_DISPLAY_NAME_ALTERNATIVE;
  }

  private static String getWhere(Context context, boolean hasPhoneNumbers) {
    String where = getProjection(context)[CONTACT_DISPLAY_NAME] + " IS NOT NULL";
    if (hasPhoneNumbers) {
      where += " AND " + Contacts.HAS_PHONE_NUMBER + "=1";
    }
    return where;
  }

  private static String getSortKey(Context context) {
    ContactsPreferences contactsPrefs = new ContactsPreferences(context);
    boolean sortOrderPrimary =
        (contactsPrefs.getSortOrder() == ContactsPreferences.SORT_ORDER_PRIMARY);
    return sortOrderPrimary ? Contacts.SORT_KEY_PRIMARY : Contacts.SORT_KEY_ALTERNATIVE;
  }

  /** Update cursor loader to filter contacts based on the provided query. */
  public void setQuery(String query) {
    setUri(buildUri(query));
  }

  private static Uri buildUri(String query) {
    Uri.Builder baseUri;
    if (TextUtils.isEmpty(query)) {
      baseUri = Contacts.CONTENT_URI.buildUpon();
    } else {
      baseUri = Contacts.CONTENT_FILTER_URI.buildUpon().appendPath(query);
    }
    return baseUri.appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true").build();
  }
}
