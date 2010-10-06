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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;
import com.trustedlogic.trustednfc.android.NdefMessage;
import com.trustedlogic.trustednfc.android.NfcManager;


/**
 * @author nnk@google.com (Nick Kralevich)
 */
public class SaveTag extends Activity implements DialogInterface.OnClickListener {

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

        showDialog(1);
        NdefMessage msg = getIntent().getParcelableExtra(NfcManager.NDEF_MESSAGE_EXTRA);

        String s = toHexString(msg.toByteArray());
        Log.d("SaveTag", s);
        Toast.makeText(this.getBaseContext(), "SaveTag: " + s, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        return new AlertDialog.Builder(this)
                .setTitle("Welcome! T2000 Festival")
                .setPositiveButton("Save", this)
                .setNegativeButton("Cancel", this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        dismissDialog(1);
    }

    private static final char[] hexDigits = "0123456789abcdef".toCharArray();

    private static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(3 * bytes.length);
        for (byte b : bytes) {
            sb.append("(byte) 0x").append(hexDigits[(b >> 4) & 0xf]).append(hexDigits[b & 0xf]).append(", ");
        }
        return sb.toString();
    }
}
