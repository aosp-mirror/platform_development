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

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefTag;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * An {@link Activity} which handles a broadcast of a new tag that the device just discovered.
 */
public class TagViewer extends Activity {
    static final String TAG = "SaveTag";    
    static final String EXTRA_TAG_DB_ID = "db_id";
    static final String EXTRA_MESSAGE = "msg";

    long mTagDatabaseId;

    @Override
    protected void onStart() {
        super.onStart();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DIM_BEHIND
        );

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
        } else {
            msgs = tag.getNdefMessages();
            // TODO use a service to avoid the process getting reaped during saving 
            new SaveTagTask().execute(msgs);
        }

        if (msgs == null || msgs.length == 0) {
            Log.e(TAG, "No NDEF messages");
            finish();
            return;
        }

        
        LayoutInflater inflater = LayoutInflater.from(
                new ContextThemeWrapper(this, android.R.style.Theme_Light));
        LinearLayout list = (LinearLayout) inflater.inflate(R.layout.tag_viewer_list, null, false);
        // TODO figure out why the background isn't white, the CTW should force that...
        list.setBackgroundColor(Color.WHITE);
        setContentView(list);
        buildTagViews(list, inflater, msgs);
    }

    private void buildTagViews(LinearLayout list, LayoutInflater inflater, NdefMessage[] msgs) {
        // The body of the dialog should use the light theme

        // Build the views from the logical records in the messages
        boolean first = true;
        for (NdefMessage msg : msgs) {
            Iterable<Object> objects = NdefUtil.getObjects(msg);
            for (Object object : objects) {
                if (!first) {
                    list.addView(inflater.inflate(R.layout.tag_divider, list, false));
                    first = false;
                }

                if (object instanceof String) {
                    TextView text = (TextView) inflater.inflate(R.layout.tag_text, list, false);
                    text.setText((CharSequence) object);
                    list.addView(text);
                } else if (object instanceof Uri) {
                    TextView text = (TextView) inflater.inflate(R.layout.tag_text, list, false);
                    text.setText(object.toString());
                    list.addView(text);
                } else if (object instanceof SmartPoster) {
                    TextView text = (TextView) inflater.inflate(R.layout.tag_text, list, false);
                    SmartPoster poster = (SmartPoster) object;
                    text.setText(poster.getTitle());
                    list.addView(text);
                }
            }
        }
    }
    
    final class SaveTagTask extends AsyncTask<NdefMessage, Void, Void> {
        @Override
        public Void doInBackground(NdefMessage... msgs) {
            TagDBHelper helper = TagDBHelper.getInstance(TagViewer.this);
            SQLiteDatabase db = helper.getWritableDatabase();
            db.beginTransaction();
            try {
                for (NdefMessage msg : msgs) {
                    helper.insertNdefMessage(db, msg, false);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            return null;
        }
    }
}
