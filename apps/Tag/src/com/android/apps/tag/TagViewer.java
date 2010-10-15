/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.apps.tag;

import com.android.apps.tag.message.NdefMessageParser;
import com.android.apps.tag.message.ParsedNdefMessage;
import com.android.apps.tag.record.ParsedNdefRecord;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefTag;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * An {@link Activity} which handles a broadcast of a new tag that the device just discovered.
 */
public class TagViewer extends Activity implements OnClickListener, Handler.Callback {
    static final String TAG = "SaveTag";    
    static final String EXTRA_TAG_DB_ID = "db_id";
    static final String EXTRA_MESSAGE = "msg";

    /** This activity will finish itself in this amount of time if the user doesn't do anything. */
    static final int ACTIVITY_TIMEOUT_MS = 10 * 1000;

    long mTagDatabaseId;
    ImageView mIcon;
    TextView mTitle;
    CheckBox mStar;
    Button mDeleteButton;
    Button mCancelButton;
    NdefMessage[] mMessagesToSave = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DIM_BEHIND
        );

        setContentView(R.layout.tag_viewer);

        mTitle = (TextView) findViewById(R.id.title);
        mIcon = (ImageView) findViewById(R.id.icon);
        mStar = (CheckBox) findViewById(R.id.star);
        mDeleteButton = (Button) findViewById(R.id.btn_delete);
        mCancelButton = (Button) findViewById(R.id.btn_cancel);

        mDeleteButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);
        mIcon.setImageResource(R.drawable.ic_launcher_nfc);

        Intent intent = getIntent();
        NdefMessage[] msgs = null;
        NdefTag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            // Maybe it came from the database? 
            mTagDatabaseId = intent.getLongExtra(EXTRA_TAG_DB_ID, -1);
            NdefMessage msg = intent.getParcelableExtra(EXTRA_MESSAGE);
            if (msg != null) {
                msgs = new NdefMessage[] { msg };
            }

            // Hide the text about saving the tag, it's already in the database
            findViewById(R.id.cancel_help_text).setVisibility(View.GONE);
        } else {
            msgs = tag.getNdefMessages();
            mDeleteButton.setVisibility(View.GONE);

            // Set a timer on this activity since it wasn't created by the user
            new Handler(this).sendEmptyMessageDelayed(0, ACTIVITY_TIMEOUT_MS);
            
            // Save the messages that were just scanned
            mMessagesToSave = msgs;
        }

        if (msgs == null || msgs.length == 0) {
            Log.e(TAG, "No NDEF messages");
            finish();
            return;
        }

        Context contentContext = new ContextThemeWrapper(this, android.R.style.Theme_Light); 
        LayoutInflater inflater = LayoutInflater.from(contentContext);
        LinearLayout list = (LinearLayout) findViewById(R.id.list);

        buildTagViews(list, inflater, msgs);

        if (TextUtils.isEmpty(getTitle())) {
            // There isn't a snippet for this tag, use a default title
            setTitle(R.string.tag_unknown);
        }
    }

    private void buildTagViews(LinearLayout list, LayoutInflater inflater, NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) {
            return;
        }

        // Build the views from the logical records in the messages
        NdefMessage msg = msgs[0];

        // Set the title to be the snippet of the message
        ParsedNdefMessage parsedMsg = NdefMessageParser.parse(msg);
        setTitle(parsedMsg.getSnippet(this, Locale.getDefault()));

        // Build views for all of the sub records
        for (ParsedNdefRecord record : parsedMsg.getRecords()) {
            list.addView(record.getView(this, inflater, list));
            inflater.inflate(R.layout.tag_divider, list, true);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle.setText(title);
    }

    @Override
    public void onClick(View view) {
        if (view == mDeleteButton) {
            Intent save = new Intent(this, TagService.class);
            save.putExtra(TagService.EXTRA_DELETE_ID, mTagDatabaseId);
            startService(save);
            finish();
        } else if (view == mCancelButton) {
            mMessagesToSave = null;
            finish();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mMessagesToSave != null) {
            saveMessages(mMessagesToSave);
        }
    }

    void saveMessages(NdefMessage[] msgs) {
        Intent save = new Intent(this, TagService.class);
        save.putExtra(TagService.EXTRA_SAVE_MSGS, msgs);
        startService(save);
    }

    @Override
    public boolean handleMessage(Message msg) {
        finish();
        return true;
    }
}
