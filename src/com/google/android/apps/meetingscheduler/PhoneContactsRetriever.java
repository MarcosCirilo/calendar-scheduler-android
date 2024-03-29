/*
 * Copyright (c) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.apps.meetingscheduler;

import android.accounts.Account;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Get the contacts from the phone for the selected account.
 * 
 * @author Alain Vongsouvanh (alainv@google.com)
 */
public class PhoneContactsRetriever implements AttendeeRetriever {

  private Activity activity;
  private Account account;

  public PhoneContactsRetriever(Activity activity, Account account) {
    this.activity = activity;
    this.account = account;
  }

  @Override
  public List<Attendee> getPossibleAttendees() {
    List<Attendee> result = new ArrayList<Attendee>();
    ContentResolver cr = activity.getContentResolver();
    Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, new String[] { Contacts._ID,
        Contacts.DISPLAY_NAME, Contacts.IN_VISIBLE_GROUP }, Contacts.IN_VISIBLE_GROUP + " = 1",
        null, null);

    try {
      if (cursor.getCount() > 0) {
        while (cursor.moveToNext()) {
          long id = cursor.getLong(cursor.getColumnIndex(Contacts._ID));
          String email = getEmail(cr, id);

          if (email != null) {
            String name = cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME));
            String imageUri = getPhotoUri(activity, cr, id);

            result.add(new Attendee(name + " (" + email + ")", email, imageUri));
          }
        }
      } else
        Log.e(MeetingSchedulerConstants.TAG, "No contacts found.");
    } finally {
      cursor.close();
    }
    Attendee current = getCurrentUser();
    current.selected = true;
    result.add(current);

    return result;
  }

  @Override
  public Attendee getCurrentUser() {
    return new Attendee("Me (" + account.name + ")", account.name, null);
  }

  /**
   * Get the correct email address to use for the current contact.
   * 
   * @param cr
   * @param id
   * @return
   */
  private String getEmail(ContentResolver cr, long id) {
    Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, new String[] {
        Email.DATA, Email.IS_PRIMARY }, Email.CONTACT_ID + " = '" + id + "'", null,
        Email.IS_PRIMARY + " DESC");
    String result = null;

    if (cursor != null) {
      try {
        if (cursor.getCount() > 0) {
          while (cursor.moveToNext()) {
            String email = cursor.getString(cursor.getColumnIndex(Email.DATA));

            if (!email.contains("@"))
              continue;
            // Get the first same-domain account.
            if (isSameDomain(account.name, email))
              return email;
            // Else, get the first gmail address.
            else if (isSameDomain("@gmail.com", email) && result == null)
              result = email;
          }
          // If none of the above has been found, use the first email address.
          if (result == null) {
            if (cursor.moveToFirst()) {
              result = cursor.getString(cursor.getColumnIndex(Email.DATA));
            }
          }
        }
      } finally {
        cursor.close();
      }
    }

    return result;
  }

  /**
   * Check if two emails are of the same domain.
   * 
   * @param lhs
   * @param rhs
   * @return
   */
  private boolean isSameDomain(String lhs, String rhs) {
    return lhs.substring(lhs.indexOf('@')).equalsIgnoreCase(rhs.substring(rhs.indexOf('@')));
  }

  /**
   * Get the contact's Photo URI if it exists.
   * 
   * @param cr
   * @param id
   * @return The contact's Photo URI.
   */
  private String getPhotoUri(Context context, ContentResolver cr, long id) {
    Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, id);
    Uri photoUri = Uri.withAppendedPath(contactUri, Contacts.Photo.CONTENT_DIRECTORY);

    // TODO: Do not try to open the file to check if it exists.
    try {
      AssetFileDescriptor descriptor = context.getContentResolver().openAssetFileDescriptor(
          photoUri, "r");

      descriptor.close();
      return photoUri.toString();
    } catch (Exception e) {
      // The contact has no pictures.
      return null;
    }
  }

}
