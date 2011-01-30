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
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.widget.TextView;

import java.nio.charset.Charset;
import java.util.Locale;

/**
 * An example of how to use the NFC foreground NDEF push APIs.
 */
public class ForegroundNdefPush extends Activity {
    private NfcAdapter mAdapter;
    private TextView mText;
    private NdefMessage mMessage;

    public static NdefRecord newTextRecord(String text, Locale locale, boolean encodeInUtf8) {
        byte[] langBytes = locale.getLanguage().getBytes(Charset.forName("US-ASCII"));

        Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset.forName("UTF-16");
        byte[] textBytes = text.getBytes(utfEncoding);

        int utfBit = encodeInUtf8 ? 0 : (1 << 7);
        char status = (char) (utfBit + langBytes.length);

        byte[] data = new byte[1 + langBytes.length + textBytes.length]; 
        data[0] = (byte) status;
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);

        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], data);
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        mAdapter = NfcAdapter.getDefaultAdapter(this);

        setContentView(R.layout.foreground_dispatch);
        mText = (TextView) findViewById(R.id.text);
        if (mAdapter != null) {
            mText.setText("Tap another Android phone with NFC to push 'NDEF Push Sample'");
        } else {
            mText.setText("This phone is not NFC enabled.");
        }

        // Create an NDEF message with some sample text
        mMessage = new NdefMessage(
                new NdefRecord[] { newTextRecord("NDEF Push Sample", Locale.ENGLISH, true)});        
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter != null) mAdapter.enableForegroundNdefPush(this, mMessage);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAdapter != null) mAdapter.disableForegroundNdefPush(this);
    }
}
