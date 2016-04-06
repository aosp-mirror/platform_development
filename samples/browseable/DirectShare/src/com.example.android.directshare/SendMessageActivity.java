/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.directshare;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Provides the UI for sharing a text with a {@link Contact}.
 */
public class SendMessageActivity extends Activity {

    /**
     * The request code for {@link SelectContactActivity}. This is used when the user doesn't select
     * any of Direct Share icons.
     */
    private static final int REQUEST_SELECT_CONTACT = 1;

    /**
     * The text to share.
     */
    private String mBody;

    /**
     * The ID of the contact to share the text with.
     */
    private int mContactId;

    // View references.
    private TextView mTextContactName;
    private TextView mTextMessageBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send_message);
        setTitle(R.string.sending_message);
        // View references.
        mTextContactName = (TextView) findViewById(R.id.contact_name);
        mTextMessageBody = (TextView) findViewById(R.id.message_body);
        // Resolve the share Intent.
        boolean resolved = resolveIntent(getIntent());
        if (!resolved) {
            finish();
            return;
        }
        // Bind event handlers.
        findViewById(R.id.send).setOnClickListener(mOnClickListener);
        // Set up the UI.
        prepareUi();
        // The contact ID will not be passed on when the user clicks on the app icon rather than any
        // of the Direct Share icons. In this case, we show another dialog for selecting a contact.
        if (mContactId == Contact.INVALID_ID) {
            selectContact();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SELECT_CONTACT:
                if (resultCode == RESULT_OK) {
                    mContactId = data.getIntExtra(Contact.ID, Contact.INVALID_ID);
                }
                // Give up sharing the send_message if the user didn't choose a contact.
                if (mContactId == Contact.INVALID_ID) {
                    finish();
                    return;
                }
                prepareUi();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Resolves the passed {@link Intent}. This method can only resolve intents for sharing a plain
     * text. {@link #mBody} and {@link #mContactId} are modified accordingly.
     *
     * @param intent The {@link Intent}.
     * @return True if the {@code intent} is resolved properly.
     */
    private boolean resolveIntent(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction()) &&
                "text/plain".equals(intent.getType())) {
            mBody = intent.getStringExtra(Intent.EXTRA_TEXT);
            mContactId = intent.getIntExtra(Contact.ID, Contact.INVALID_ID);
            return true;
        }
        return false;
    }

    /**
     * Sets up the UI.
     */
    private void prepareUi() {
        if (mContactId != Contact.INVALID_ID) {
            Contact contact = Contact.byId(mContactId);
            ContactViewBinder.bind(contact, mTextContactName);
        }
        mTextMessageBody.setText(mBody);
    }

    /**
     * Delegates selection of a {@Contact} to {@link SelectContactActivity}.
     */
    private void selectContact() {
        Intent intent = new Intent(this, SelectContactActivity.class);
        intent.setAction(SelectContactActivity.ACTION_SELECT_CONTACT);
        startActivityForResult(intent, REQUEST_SELECT_CONTACT);
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.send:
                    send();
                    break;
            }
        }
    };

    /**
     * Pretends to send the text to the contact. This only shows a dummy message.
     */
    private void send() {
        Toast.makeText(this,
                getString(R.string.message_sent, mBody, Contact.byId(mContactId).getName()),
                Toast.LENGTH_SHORT).show();
        finish();
    }

}
