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
import android.widget.Toast;
import com.trustedlogic.trustednfc.android.NdefMessage;
import com.trustedlogic.trustednfc.android.NdefRecord;
import com.trustedlogic.trustednfc.android.NfcManager;

/**
 * This class doesn't work.  Sorry.  Think of this class as pseudo
 * code for now.
 */
public class TagBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(NfcManager.NDEF_TAG_DISCOVERED_ACTION)) {
            NdefMessage msg = intent.getParcelableExtra(NfcManager.NDEF_MESSAGE_EXTRA);
            Toast.makeText(context, "got a new message", Toast.LENGTH_SHORT).show();
            insertIntoDb(msg);
        }
    }

    private void insertIntoDb(NdefMessage msg) {
        for (NdefRecord record : msg.getRecords()) {
            insertIntoRecordDb(record.getType(), record.getPayload());
        }
    }

    private void insertIntoRecordDb(byte[] type, byte[] payload) {
        // do something...
    }

}
