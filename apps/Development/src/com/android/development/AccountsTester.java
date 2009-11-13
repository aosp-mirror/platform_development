/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.development;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.accounts.*;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Handler;
import android.view.*;
import android.widget.*;
import android.widget.ArrayAdapter;
import android.util.Log;
import android.text.TextUtils;

import java.io.IOException;

public class AccountsTester extends Activity implements OnAccountsUpdateListener {
    private static final String TAG = "AccountsTester";
    private Spinner mAccountTypesSpinner;
    private ListView mAccountsListView;
    private ListView mAuthenticatorsListView;
    private AccountManager mAccountManager;
    private String mLongPressedAccount = null;
    private static final String COM_GOOGLE = "com.google";
    private AuthenticatorDescription[] mAuthenticatorDescs;

    private static final int GET_AUTH_TOKEN_DIALOG_ID = 1;
    private static final int UPDATE_CREDENTIALS_DIALOG_ID = 2;
    private static final int INVALIDATE_AUTH_TOKEN_DIALOG_ID = 3;
    private EditText mDesiredAuthTokenTypeEditText;
    private EditText mDesiredFeaturesEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAccountManager = AccountManager.get(this);
        setContentView(R.layout.accounts_tester);
        ButtonClickListener buttonClickListener = new ButtonClickListener();

        mAccountTypesSpinner = (Spinner) findViewById(R.id.accounts_tester_account_types_spinner);
        mAccountsListView = (ListView) findViewById(R.id.accounts_tester_accounts_list);
        mAuthenticatorsListView = (ListView) findViewById(R.id.accounts_tester_authenticators_list);
        registerForContextMenu(mAccountsListView);
        getAuthenticatorTypes();
        findViewById(R.id.accounts_tester_get_all_accounts).setOnClickListener(buttonClickListener);
        findViewById(R.id.accounts_tester_get_accounts_by_type).setOnClickListener(
                buttonClickListener);
        findViewById(R.id.accounts_tester_add_account).setOnClickListener(buttonClickListener);
        findViewById(R.id.accounts_tester_edit_properties).setOnClickListener(buttonClickListener);
        mDesiredAuthTokenTypeEditText =
                (EditText) findViewById(R.id.accounts_tester_desired_authtokentype);
        mDesiredFeaturesEditText = (EditText) findViewById(R.id.accounts_tester_desired_features);
    }

    private static class AuthenticatorsArrayAdapter extends ArrayAdapter<AuthenticatorDescription> {
        protected LayoutInflater mInflater;
        private static final int mResource = R.layout.authenticators_list_item;

        public AuthenticatorsArrayAdapter(Context context, AuthenticatorDescription[] items) {
            super(context, mResource, items);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        static class ViewHolder {
            TextView label;
            ImageView icon;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // A ViewHolder keeps references to children views to avoid unneccessary calls
            // to findViewById() on each row.
            ViewHolder holder;

            // When convertView is not null, we can reuse it directly, there is no need
            // to reinflate it. We only inflate a new View when the convertView supplied
            // by ListView is null.
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.authenticators_list_item, null);

                // Creates a ViewHolder and store references to the two children views
                // we want to bind data to.
                holder = new ViewHolder();
                holder.label = (TextView) convertView.findViewById(
                        R.id.accounts_tester_authenticator_label);
                holder.icon = (ImageView) convertView.findViewById(
                        R.id.accounts_tester_authenticator_icon);

                convertView.setTag(holder);
            } else {
                // Get the ViewHolder back to get fast access to the TextView
                // and the ImageView.
                holder = (ViewHolder) convertView.getTag();
            }

            final AuthenticatorDescription desc = getItem(position);
            final String packageName = desc.packageName;
            try {
                final Context authContext = getContext().createPackageContext(packageName, 0);

                // Set text field
                holder.label.setText(authContext.getString(desc.labelId));

                // Set resource icon
                holder.icon.setImageDrawable(authContext.getResources().getDrawable(desc.iconId));
            } catch (PackageManager.NameNotFoundException e) {
                Log.d(TAG, "error getting the Package Context for " + packageName, e);
            }

            return convertView;
        }
    }

    private void getAuthenticatorTypes() {
        mAuthenticatorDescs = mAccountManager.getAuthenticatorTypes();
        String[] names = new String[mAuthenticatorDescs.length];
        for (int i = 0; i < mAuthenticatorDescs.length; i++) {
            Context authContext;
            try {
                authContext = createPackageContext(mAuthenticatorDescs[i].packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                continue;
            }
            names[i] = authContext.getString(mAuthenticatorDescs[i].labelId);
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(AccountsTester.this,
                android.R.layout.simple_spinner_item, names);
        mAccountTypesSpinner.setAdapter(adapter);

        mAuthenticatorsListView.setAdapter(new AuthenticatorsArrayAdapter(
                AccountsTester.this, mAuthenticatorDescs));
    }

    public void onAccountsUpdated(Account[] accounts) {
        Log.d(TAG, "onAccountsUpdated: \n  " + TextUtils.join("\n  ", accounts));
        String[] accountNames = new String[accounts.length];
        for (int i = 0; i < accounts.length; i++) {
            accountNames[i] = accounts[i].name;
        }
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(AccountsTester.this,
                android.R.layout.simple_list_item_1, accountNames);
        mAccountsListView.setAdapter(adapter);
    }

    protected void onStart() {
        super.onStart();
        final Handler mainHandler = new Handler(getMainLooper());
        mAccountManager.addOnAccountsUpdatedListener(this, mainHandler,
                true /* updateImmediately */);
    }

    protected void onStop() {
        super.onStop();
        mAccountManager.removeOnAccountsUpdatedListener(this);
    }

    class ButtonClickListener implements View.OnClickListener {
        public void onClick(View v) {
            if (R.id.accounts_tester_get_all_accounts == v.getId()) {
                onAccountsUpdated(mAccountManager.getAccounts());
            } else if (R.id.accounts_tester_get_accounts_by_type == v.getId()) {
                String type = getSelectedAuthenticator().type;
                onAccountsUpdated(mAccountManager.getAccountsByType(type));
            } else if (R.id.accounts_tester_add_account == v.getId()) {
                AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {
                    public void run(AccountManagerFuture<Bundle> future) {
                        try {
                            Bundle bundle = future.getResult();
                            bundle.keySet();
                            Log.d(TAG, "account added: " + bundle);
                        } catch (OperationCanceledException e) {
                            Log.d(TAG, "addAccount was canceled");
                        } catch (IOException e) {
                            Log.d(TAG, "addAccount failed: " + e);
                        } catch (AuthenticatorException e) {
                            Log.d(TAG, "addAccount failed: " + e);
                        }
                    }
                };
                String authTokenType = mDesiredAuthTokenTypeEditText.getText().toString();
                if (TextUtils.isEmpty(authTokenType)) {
                    authTokenType = null;
                }
                String featureString = mDesiredFeaturesEditText.getText().toString();
                String[] requiredFeatures = TextUtils.split(featureString, " ");
                if (requiredFeatures.length == 0) {
                    requiredFeatures = null;
                }
                mAccountManager.addAccount(getSelectedAuthenticator().type,
                        authTokenType, requiredFeatures, null /* options */,
                        AccountsTester.this, callback, null /* handler */);
            } else if (R.id.accounts_tester_edit_properties == v.getId()) {
                mAccountManager.editProperties(getSelectedAuthenticator().type,
                        AccountsTester.this, new EditPropertiesCallback(), null /* handler */);
            } else {
                // unknown button
            }
        }

        private class EditPropertiesCallback implements AccountManagerCallback<Bundle> {
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    Bundle bundle = future.getResult();
                    bundle.keySet();
                    Log.d(TAG, "editProperties succeeded: " + bundle);
                } catch (OperationCanceledException e) {
                    Log.d(TAG, "editProperties was canceled");
                } catch (IOException e) {
                    Log.d(TAG, "editProperties failed: ", e);
                } catch (AuthenticatorException e) {
                    Log.d(TAG, "editProperties failed: ", e);
                }
            }
        }
    }

    private AuthenticatorDescription getSelectedAuthenticator() {
        return mAuthenticatorDescs[mAccountTypesSpinner.getSelectedItemPosition()];
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {
        menu.setHeaderTitle(R.string.accounts_tester_account_context_menu_title);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.account_list_context_menu, menu);
        mLongPressedAccount = ((TextView)info.targetView).getText().toString();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.accounts_tester_remove_account) {
            final Account account = new Account(mLongPressedAccount, COM_GOOGLE);
            mAccountManager.removeAccount(account, new AccountManagerCallback<Boolean>() {
                public void run(AccountManagerFuture<Boolean> future) {
                    try {
                        Log.d(TAG, "removeAccount(" + account + ") = " + future.getResult());
                    } catch (OperationCanceledException e) {
                    } catch (IOException e) {
                    } catch (AuthenticatorException e) {
                    }
                }
            }, null /* handler */);
        } else if (item.getItemId() == R.id.accounts_tester_get_auth_token) {
            showDialog(GET_AUTH_TOKEN_DIALOG_ID);
        } else if (item.getItemId() == R.id.accounts_tester_invalidate_auth_token) {
            showDialog(INVALIDATE_AUTH_TOKEN_DIALOG_ID);
        } else if (item.getItemId() == R.id.accounts_tester_update_credentials) {
            showDialog(UPDATE_CREDENTIALS_DIALOG_ID);
        } else if (item.getItemId() == R.id.accounts_tester_confirm_credentials) {
            mAccountManager.confirmCredentials(new Account(mLongPressedAccount, COM_GOOGLE), null,
                    AccountsTester.this, new ConfirmCredentialsCallback(), null /* handler */);
        }
        return true;
    }

    @Override
    protected Dialog onCreateDialog(final int id) {
        if (id == GET_AUTH_TOKEN_DIALOG_ID || id == INVALIDATE_AUTH_TOKEN_DIALOG_ID
                || id == UPDATE_CREDENTIALS_DIALOG_ID) {
            final View view = LayoutInflater.from(this).inflate(R.layout.get_auth_token_view, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setPositiveButton(R.string.accounts_tester_do_get_auth_token,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            EditText value = (EditText) view.findViewById(
                                    R.id.accounts_tester_auth_token_type);

                            String authTokenType = value.getText().toString();
                            AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {
                                public void run(AccountManagerFuture<Bundle> future) {
                                    try {
                                        Bundle bundle = future.getResult();
                                        bundle.keySet();
                                        Log.d(TAG, "dialog " + id + " success: " + bundle);
                                    } catch (OperationCanceledException e) {
                                        Log.d(TAG, "dialog " + id + " canceled");
                                    } catch (IOException e) {
                                        Log.d(TAG, "dialog " + id + " failed: " + e);
                                    } catch (AuthenticatorException e) {
                                        Log.d(TAG, "dialog " + id + " failed: " + e);
                                    }
                                }
                            };
                            final Account account = new Account(mLongPressedAccount,
                                    COM_GOOGLE);
                            if (id == GET_AUTH_TOKEN_DIALOG_ID) {
                                mAccountManager.getAuthToken(account, authTokenType,
                                        null /* loginOptions */, AccountsTester.this,
                                        callback, null /* handler */);
                            } else if (id == INVALIDATE_AUTH_TOKEN_DIALOG_ID) {
                                mAccountManager.getAuthToken(account, authTokenType, false,
                                        new GetAndInvalidateAuthTokenCallback(), null);
                            } else {
                                mAccountManager.updateCredentials(
                                        account,
                                        authTokenType, null /* loginOptions */,
                                        AccountsTester.this, callback, null /* handler */);
                            }
                        }
            });
            builder.setView(view);
            return builder.create();
        }
        return super.onCreateDialog(id);
    }

    AccountManagerCallback<Bundle> newAccountsCallback(String type, String[] features) {
        return new GetAccountsCallback(type, features);
    }

    class GetAccountsCallback implements AccountManagerCallback<Bundle> {
        final String[] mFeatures;
        final String mAccountType;

        public GetAccountsCallback(String type, String[] features) {
            mFeatures = features;
            mAccountType = type;
        }

        public void run(AccountManagerFuture<Bundle> future) {
            Log.d(TAG, "GetAccountsCallback: type " + mAccountType
                    + ", features "
                    + (mFeatures == null ? "none" : TextUtils.join(",", mFeatures)));
            try {
                Bundle result = future.getResult();
                Parcelable[] accounts = result.getParcelableArray(AccountManager.KEY_ACCOUNTS);
                Log.d(TAG, "found " + accounts.length + " accounts");
                for (Parcelable account : accounts) {
                    Log.d(TAG, "  " + account);
                }
            } catch (OperationCanceledException e) {
                Log.d(TAG, "failure", e);
            } catch (IOException e) {
                Log.d(TAG, "failure", e);
            } catch (AuthenticatorException e) {
                Log.d(TAG, "failure", e);
            }
        }
    }

    AccountManagerCallback<Bundle> newAuthTokensCallback(String type, String authTokenType, String[] features) {
        return new GetAuthTokenCallback(type, authTokenType, features);
    }

    class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {
        final String[] mFeatures;
        final String mAccountType;
        final String mAuthTokenType;

        public GetAuthTokenCallback(String type, String authTokenType, String[] features) {
            mFeatures = features;
            mAccountType = type;
            mAuthTokenType = authTokenType;
        }

        public void run(AccountManagerFuture<Bundle> future) {
            Log.d(TAG, "GetAuthTokenCallback: type " + mAccountType
                    + ", features "
                    + (mFeatures == null ? "none" : TextUtils.join(",", mFeatures)));
            try {
                Bundle result = future.getResult();
                result.keySet();
                Log.d(TAG, "  result: " + result);
            } catch (OperationCanceledException e) {
                Log.d(TAG, "failure", e);
            } catch (IOException e) {
                Log.d(TAG, "failure", e);
            } catch (AuthenticatorException e) {
                Log.d(TAG, "failure", e);
            }
        }
    }

    private class GetAndInvalidateAuthTokenCallback implements AccountManagerCallback<Bundle> {
        public void run(AccountManagerFuture<Bundle> future) {
            try {
                Bundle bundle = future.getResult();
                String authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                mAccountManager.invalidateAuthToken(COM_GOOGLE, authToken);
            } catch (OperationCanceledException e) {
                Log.d(TAG, "invalidate: interrupted while getting authToken");
            } catch (IOException e) {
                Log.d(TAG, "invalidate: error getting authToken", e);
            } catch (AuthenticatorException e) {
                Log.d(TAG, "invalidate: error getting authToken", e);
            }
        }
    }

    private static class ConfirmCredentialsCallback implements AccountManagerCallback<Bundle> {
        public void run(AccountManagerFuture<Bundle> future) {
            try {
                Bundle bundle = future.getResult();
                bundle.keySet();
                Log.d(TAG, "confirmCredentials success: " + bundle);
            } catch (OperationCanceledException e) {
                Log.d(TAG, "confirmCredentials canceled");
            } catch (AuthenticatorException e) {
                Log.d(TAG, "confirmCredentials failed: " + e);
            } catch (IOException e) {
                Log.d(TAG, "confirmCredentials failed: " + e);
            }
        }
    }
}
