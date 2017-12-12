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

package com.example.android.app.admin.whitelistedaccount;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

public class MyAuthenticator extends Service {
    private static final String TAG = "TestAuthenticator";

    private static final String ACCOUNT_TYPE = "com.example.android.app.admin.whitelistedaccount";

    private static final String ACCOUNT_FEATURE_DEVICE_OR_PROFILE_OWNER_ALLOWED =
            "android.account.DEVICE_OR_PROFILE_OWNER_ALLOWED";

    private static Authenticator sInstance;

    @Override
    public IBinder onBind(Intent intent) {
        if (sInstance == null) {
            sInstance = new Authenticator(getApplicationContext());

        }
        return sInstance.getIBinder();
    }

    public static boolean setUpAccount(Context context) {
        final AccountManager am = AccountManager.get(context);
        if (am.getAccountsByType(ACCOUNT_TYPE).length > 0) {
            return false; // Already set up.
        }

        // Add a new account.
        final Account account = new Account(
                context.getResources().getString(R.string.account_name), ACCOUNT_TYPE);
        am.addAccountExplicitly(account, null, null);
        return true;
    }

    public static class Authenticator extends AbstractAccountAuthenticator {

        private final Context mContxet;

        public Authenticator(Context context) {
            super(context);
            mContxet = context;
        }

        @Override
        public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
                String authTokenType, String[] requiredFeatures, Bundle options)
                throws NetworkErrorException {
            return new Bundle();
        }

        @Override
        public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
            return new Bundle();
        }

        @Override
        public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
                String authTokenType, Bundle options) throws NetworkErrorException {
            return new Bundle();
        }

        @Override
        public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
                Bundle options) throws NetworkErrorException {
            return new Bundle();
        }

        @Override
        public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
                String authTokenType, Bundle options) throws NetworkErrorException {
            return new Bundle();
        }

        @Override
        public String getAuthTokenLabel(String authTokenType) {
            return "token_label";
        }

        @Override
        public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
                String[] features) throws NetworkErrorException {

            boolean hasAll = false;

            if ((features != null) && (features.length == 1)
                    && ACCOUNT_FEATURE_DEVICE_OR_PROFILE_OWNER_ALLOWED.equals(features[0])) {
                hasAll = true;
            }

            Bundle result = new Bundle();
            result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, hasAll);
            return result;
        }
    }
}
