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
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.util.Log;

/**
 * Choose which account to upload track information to.
 * 
 * This class is from the project My Tracks {@link http://mytracks.appspot.com/}
 * and has been modified to run on this project.
 * 
 * @author Sandor Dornbush
 */
public class AccountChooser {

  /**
   * The last selected account.
   */
  private int selectedAccountIndex = -1;
  private Account selectedAccount = null;

  /**
   * Singleton instance.
   */
  private static AccountChooser instance = null;

  /**
   * An interface for receiving updates once the user has selected the account.
   */
  public interface AccountHandler {
    /**
     * Handle the account being selected.
     * 
     * @param account The selected account or null if none could be found
     */
    public void handleAccountSelected(Account account);
  }

  /**
   * Get the singleton instance of AccountChooser.
   * 
   * @return The instance of AccountChooser.
   */
  public static AccountChooser getInstance() {
    if (instance == null)
      instance = new AccountChooser();
    return instance;
  }

  /**
   * Private Constructor to have a single instance in the application.
   */
  private AccountChooser() {
  }

  /**
   * Reset selected account value.
   */
  public void Reset() {
    selectedAccount = null;
    selectedAccountIndex = -1;
  }

  /**
   * Chooses the best account to upload to. If no account is found the user will
   * be alerted. If only one account is found that will be used. If multiple
   * accounts are found the user will be allowed to choose.
   * 
   * @param context The parent activity
   * @param oldAccount TODO
   * @param handler The handler to be notified when an account has been selected
   */
  public void chooseAccount(final Context context, String oldAccount, final AccountHandler handler) {
    final Account[] accounts = AccountManager.get(context).getAccountsByType(
        MeetingSchedulerConstants.ACCOUNT_TYPE);
    if (accounts.length < 1) {
      alertNoAccounts(context, handler);
      return;
    }
    if (accounts.length == 1) {
      handler.handleAccountSelected(accounts[0]);
      return;
    }

    if (selectedAccount != null) {
      handler.handleAccountSelected(selectedAccount);
      return;
    }

    if (oldAccount != null) {
      for (Account account : accounts) {
        if (account.name.equals(oldAccount)) {
          selectedAccount = account;
          handler.handleAccountSelected(selectedAccount);
          return;
        }
      }
    }

    // Let the user choose.
    Log.e(MeetingSchedulerConstants.TAG, "Multiple matching accounts found.");

    String[] choices = new String[accounts.length];
    for (int i = 0; i < accounts.length; i++) {
      choices[i] = accounts[i].name;
    }

    final AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(R.string.choose_account_title);
    builder.setItems(choices, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        selectedAccountIndex = which;
        selectedAccount = accounts[selectedAccountIndex];
        handler.handleAccountSelected(selectedAccount);
      }
    });
    builder.setOnCancelListener(new OnCancelListener() {
      @Override
      public void onCancel(DialogInterface dialog) {
        handler.handleAccountSelected(null);
      }
    });
    builder.show();
  }

  /**
   * Puts up a dialog alerting the user that no suitable account was found.
   */
  private void alertNoAccounts(final Context context, final AccountHandler handler) {
    Log.e(MeetingSchedulerConstants.TAG, "No matching accounts found.");
    final AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(R.string.no_account_found_title);
    builder.setMessage(R.string.no_account_found);
    builder.setCancelable(true);
    builder.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        handler.handleAccountSelected(null);
      }
    });
    builder.show();
  }
}
