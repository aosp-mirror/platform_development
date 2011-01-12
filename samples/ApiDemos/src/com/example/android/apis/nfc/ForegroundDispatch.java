/*
 * Copyright (C) 2011 The Android Open Source Project
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
 * limitations under the License
 */

package com.example.android.apis.nfc;

import com.example.android.apis.R;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

/**
 * An example of how to use the NFC foreground dispatch APIs.
 */
public class ForegroundDispatch extends Activity {
    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mFilters;
    private TextView mText;
    private int mCount = 0;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        setContentView(R.layout.foreground_dispatch);
        mText = (TextView) findViewById(R.id.text);
        mText.setText("Scan a tag");

        mAdapter = NfcAdapter.getDefaultAdapter(this);

        // Create a generic PendingIntent that will be deliver to this activity. The NFC stack
        // will fill in the intent with the details of the discovered tag before delivering to
        // this activity.
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // Setup intent filters for all types of NFC dispatches to intercept all discovered tags
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECHNOLOGY_DISCOVERED);
        tech.addDataScheme("vnd.android.nfc"); 
        mFilters = new IntentFilter[] {
                ndef,
                tech,
                new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters);
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.i("Foreground dispatch", "Discovered tag with intent: " + intent);
        mText.setText("Discovered tag " + ++mCount + " with intent: " + intent);
    }

    @Override
    public void onPause() {
        super.onPause();
        mAdapter.disableForegroundDispatch(this);
    }
}
