/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.smssample;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony.Sms.Inbox;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

/**
 * The main Activity that provides a sample of a few things:
 *   -detecting if this app is the default SMS app and then showing/hiding UI and enabling/disabling
 *    functionality. the UI that is shown has a button to prompt the user to set this app as the
 *    default.
 *   -a simple query to the SMS content provider to show a list of SMS messages in the inbox. even
 *    though the query uses KitKat APIs this query should still work on earlier versions of Android
 *    as the contract class and ContentProvider were still around (with essentially the same
 *    structure) but were private.
 *   -being triggered from another application when creating a new SMS. a good example is creating
 *    a new SMS from the system People application. although nothing is done with the incoming
 *    Intent in this case (just a Toast is displayed)
 *
 *  Obviously this is far from a full implementation and should just be used as a sample of how
 *  an app could be set up to correctly integrate with the new Android 4.4 KitKat APIs while
 *  running normally on earlier Android versions.
 */
public class MainActivity extends FragmentActivity implements LoaderCallbacks<Cursor> {
    private RelativeLayout mSetDefaultSmsLayout;
    private Button mSendSmsButton;
    private EditText mSendSmsEditText;
    private SimpleCursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find some views
        mSetDefaultSmsLayout = (RelativeLayout) findViewById(R.id.set_default_sms_layout);
        mSendSmsEditText = (EditText) findViewById(R.id.send_sms_edittext);
        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setEmptyView(findViewById(android.R.id.empty));
        mSendSmsButton = (Button) findViewById(R.id.send_sms_button);
        mSendSmsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                sendSms(mSendSmsEditText.getText().toString());
            }
        });

        // Create adapter and set it to our ListView
        final String[] fromFields = new String[] {
                SmsQuery.PROJECTION[SmsQuery.ADDRESS], SmsQuery.PROJECTION[SmsQuery.BODY] };
        final int[] toViews = new int[] { android.R.id.text1, android.R.id.text2 };
        mAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, null,
                fromFields, toViews, 0);
        listView.setAdapter(mAdapter);

        // Placeholder to process incoming SEND/SENDTO intents
        String intentAction = getIntent() == null ? null : getIntent().getAction();
        if (!TextUtils.isEmpty(intentAction) && (Intent.ACTION_SENDTO.equals(intentAction)
                || Intent.ACTION_SEND.equals(intentAction))) {
            // TODO: Handle incoming SEND and SENDTO intents by pre-populating UI components
            Toast.makeText(this, "Handle SEND and SENDTO intents: " + getIntent().getDataString(),
                    Toast.LENGTH_SHORT).show();
        }

        // Simple query to show the most recent SMS messages in the inbox
        getSupportLoaderManager().initLoader(SmsQuery.TOKEN, null, this);
    }

    /**
     * Dummy sendSms method, would need the "to" address to actually send a message :)
     */
    private void sendSms(String smsText) {
        if (!TextUtils.isEmpty(smsText)) {
            if (Utils.isDefaultSmsApp(this)) {
                // TODO: Use SmsManager to send SMS and then record the message in the system SMS
                // ContentProvider
                Toast.makeText(this, "Sending text message: " + smsText, Toast.LENGTH_SHORT).show();
            } else {
                // TODO: Notify the user the app is not default and provide a way to trigger
                // Utils.setDefaultSmsApp() so they can set it.
                Toast.makeText(this, "Not default", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Only do these checks/changes on KitKat+, the "mSetDefaultSmsLayout" has its visibility
        // set to "gone" in the xml layout so it won't show at all on earlier Android versions.
        if (Utils.hasKitKat()) {
            if (Utils.isDefaultSmsApp(this)) {
                // This app is the default, remove the "make this app the default" layout and
                // enable message sending components.
                mSetDefaultSmsLayout.setVisibility(View.GONE);
                mSendSmsEditText.setHint(R.string.sms_send_new_hint);
                mSendSmsEditText.setEnabled(true);
                mSendSmsButton.setEnabled(true);
            } else {
                // Not the default, show the "make this app the default" layout and disable
                // message sending components.
                mSetDefaultSmsLayout.setVisibility(View.VISIBLE);
                mSendSmsEditText.setText("");
                mSendSmsEditText.setHint(R.string.sms_send_disabled);
                mSendSmsEditText.setEnabled(false);
                mSendSmsButton.setEnabled(false);

                Button button = (Button) findViewById(R.id.set_default_sms_button);
                button.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Utils.setDefaultSmsApp(MainActivity.this);
                    }
                });
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        if (i == SmsQuery.TOKEN) {
            // This will fetch all SMS messages in the inbox, ordered by date desc
            return new CursorLoader(this, SmsQuery.CONTENT_URI, SmsQuery.PROJECTION, null, null,
                    SmsQuery.SORT_ORDER);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursorLoader.getId() == SmsQuery.TOKEN && cursor != null) {
            // Standard swap cursor in when load is done
            mAdapter.swapCursor(cursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        // Standard swap cursor to null when loader is reset
        mAdapter.swapCursor(null);
    }

    /**
     * A basic SmsQuery on android.provider.Telephony.Sms.Inbox
     */
    private interface SmsQuery {
        int TOKEN = 1;

        static final Uri CONTENT_URI = Inbox.CONTENT_URI;

        static final String[] PROJECTION = {
                Inbox._ID,
                Inbox.ADDRESS,
                Inbox.BODY,
        };

        static final String SORT_ORDER = Inbox.DEFAULT_SORT_ORDER;

        int ID = 0;
        int ADDRESS = 1;
        int BODY = 2;
    }
}
