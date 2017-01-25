/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.example.android.pushapithirdpartytwo;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorDescription;
import android.accounts.OnAccountsUpdateListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_PICK_ACCOUNT = 0;
    private static final String TAG = "PushApiTestAppTwo";
    private static AccountManager am;
    private static OnAccountsUpdateListener mListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        am = AccountManager.get(getApplicationContext());
        mListener = new OnAccountsUpdateListener() {
            @Override
            public void onAccountsUpdated(Account[] accounts) {
                Log.i(TAG, "onAccountsUpdated is called:");
                if (accounts != null) {
                    for (Account account : accounts) {
                        Log.i(TAG, "visible account: " + account);
                    }
                }
            }
        };
        am.addOnAccountsUpdatedListener(mListener, null, false,
                new String[] {"com.example.android.pushapiauthenticator"});
        final TextView visibleAccounts = (TextView) findViewById(R.id.visibleaccounts);
        final Button getVisibleAccounts = (Button) findViewById(R.id.getvisibleaccounts);
        final Toast notifOn =
                Toast.makeText(getApplicationContext(), "Notifs Turned On!", Toast.LENGTH_SHORT);
        final Toast notifOff =
                Toast.makeText(getApplicationContext(), "Notifs Turned Off!", Toast.LENGTH_SHORT);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Welcome to Test App 2.\nPlease make sure you have:\n\n1. Test App 1\n"
                + "\n2. Auth App \n\ninstalled for the demo. These applications together provide"
                + " tests, use cases, and proof of concept of Account Discovery API!\n")
                .setTitle("WELCOME")
                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do nothing
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
        getVisibleAccounts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Account[] accountsAccessedByAuthApp = am.getAccounts();
                StringBuilder masterString = new StringBuilder();
                for (int i = 0; i < accountsAccessedByAuthApp.length; i++) {
                    masterString.append(accountsAccessedByAuthApp[i].name + ", "
                            + accountsAccessedByAuthApp[i].type + "\n");
                }
                if (masterString.length() > 0) {
                    visibleAccounts.setText(masterString);
                } else {
                    visibleAccounts.setText("----");
                }

                Intent intent = AccountManager.newChooseAccountIntent(null, null, null, null, null,
                        null, null); // Show all accounts
                startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
            }
        });
    }

    @Override
    protected void onDestroy() {
        am.removeOnAccountsUpdatedListener(mListener);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
            // Receiving a result from the AccountPicker
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE),
                        Toast.LENGTH_LONG).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "No account was chosen", Toast.LENGTH_LONG).show();
            }
        }
    }
}
