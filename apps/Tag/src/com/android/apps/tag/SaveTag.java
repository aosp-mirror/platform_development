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
import android.widget.Toast;

/**
 * @author nnk@google.com (Nick Kralevich)
 */
public class SaveTag extends Activity implements DialogInterface.OnClickListener {

    @Override
    protected void onStart() {
        super.onStart();
        showDialog(1);
        String s = getIntent().getExtras().toString();
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
}
