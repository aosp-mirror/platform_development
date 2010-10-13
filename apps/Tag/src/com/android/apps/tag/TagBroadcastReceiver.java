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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefTag;

import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;

/**
 * When we receive a new NDEF tag, start the activity to
 * process the tag.
 */
public class TagBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(NfcAdapter.ACTION_NDEF_TAG_DISCOVERED)) {
            NdefTag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Intent i = new Intent(context, SaveTag.class)
                    .putExtra(NfcAdapter.EXTRA_TAG, tag)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}
