/*
 * Copyright (C) 2007 The Android Open Source Project
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

import com.google.android.googleapps.GoogleLoginCredentialsResult;
import com.google.android.googlelogin.GoogleLoginServiceConstants;
import com.google.android.googleapps.IGoogleLoginService;
import com.google.android.googleapps.LoginData;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

/**
 * Using a LogTextBox to display a scrollable text area
 * to which text is appended.
 *
 */
public class GLSTester extends Activity {

    private static final String TAG = "GLSTester";

    private LogTextBox mText;

    private IGoogleLoginService mGls = null;
    private ServiceConnection mConnection = null;

    CheckBox mDoNotify = null;
    CheckBox mRunIntent = null;
    Spinner mService = null;
    EditText mUsernameEdit = null;

    private class Listener implements View.OnClickListener {
        public Listener() {
        }

        public void onClick(View v) {
            if (mGls == null) {
                mText.append("mGls is null\n");
                return;
            }
            try {
                String service = (String) mService.getSelectedItem();
                mText.append("service: " + service + "\n");

                String account = mUsernameEdit.getText().toString();
                if (account.length() == 0) {
                    account = null;
                }
                mText.append("account: " + account + "\n");
                GoogleLoginCredentialsResult result =
                    mGls.blockingGetCredentials(account, service, mDoNotify.isChecked());
                mText.append("result account: " + result.getAccount() + "\n");
                mText.append("result string: " + result.getCredentialsString() + "\n");
                Intent intent = result.getCredentialsIntent();
                mText.append("result intent: " + intent + "\n");

                if (intent != null && mRunIntent.isChecked()) {
                    startActivityForResult(intent, 0);
                }
            } catch (RemoteException e) {
                mText.append("caught exception " + e + "\n");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mText.append("resultCode: " + resultCode + "\n");
        if (data != null) {
            mText.append("account: " +
                         data.getStringExtra(GoogleLoginServiceConstants.AUTH_ACCOUNT_KEY) + "\n");
            mText.append("authtoken: " +
                         data.getStringExtra(GoogleLoginServiceConstants.AUTHTOKEN_KEY) + "\n");
        } else {
            mText.append("intent is null");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Open a connection to the Google Login Service.  Return the account.
        mConnection = new ServiceConnection() {
                public void onServiceConnected(ComponentName className, IBinder service) {
                    mGls = IGoogleLoginService.Stub.asInterface(service);
                }
                public void onServiceDisconnected(ComponentName className) {
                    mGls = null;
                }
            };

        bindService(GoogleLoginServiceConstants.SERVICE_INTENT,
                    mConnection, Context.BIND_AUTO_CREATE);

        setContentView(R.layout.gls_tester);

        mText = (LogTextBox) findViewById(R.id.text);
        mText.append("Hello, world!\n");
        Log.v(TAG, "hello, world!");

        mDoNotify = (CheckBox) findViewById(R.id.do_notification);
        mRunIntent = (CheckBox) findViewById(R.id.run_intent);
        mRunIntent.setChecked(true);

        mService = (Spinner) findViewById(R.id.service_spinner);

        mUsernameEdit = (EditText) findViewById(R.id.username_edit);

        Button b;
        b = (Button) findViewById(R.id.require_google);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    String account = mGls.getAccount(GoogleLoginServiceConstants.REQUIRE_GOOGLE);
                    mText.append("REQUIRE_GOOGLE gave: " + account + "\n");
                    mUsernameEdit.setText(account == null ? "" : account);
                } catch (RemoteException e) {
                    mText.append("exception: " + e + "\n");
                }
            } });

        b = (Button) findViewById(R.id.prefer_hosted);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    String account = mGls.getAccount(GoogleLoginServiceConstants.PREFER_HOSTED);
                    mText.append("PREFER_HOSTED gave: " + account + "\n");
                    mUsernameEdit.setText(account == null ? "" : account);
                } catch (RemoteException e) {
                    mText.append("exception: " + e + "\n");
                }
            } });


        b = (Button) findViewById(R.id.get_accounts);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    String[] accounts = mGls.getAccounts();
                    mText.append("account list: (" + accounts.length + " entries)\n");
                    for (String username: accounts) {
                        mText.append("  " + username + "\n");
                    }
                } catch (RemoteException e) {
                    mText.append("exception: " + e + "\n");
                }
            } });

        b = (Button) findViewById(R.id.clear);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mText.setText("");
            } });

        b = (Button) findViewById(R.id.go);
        b.setOnClickListener(new Listener());

        b = (Button) findViewById(R.id.wipe_passwords);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mText.setText("wiping passwords:\n");
                try {
                    String[] accounts = mGls.getAccounts();
                    LoginData ld = new LoginData();
                    for (String username: accounts) {
                        ld.mUsername = username;
                        mGls.updatePassword(ld);
                        mText.append("  " + username + "\n");
                    }
                    mText.append("done.\n");
                } catch (RemoteException e) {
                    mText.append("caught exception " + e + "\n");
                }
            } });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
    }
}
