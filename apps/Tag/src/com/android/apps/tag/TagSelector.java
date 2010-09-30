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
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/**
 * A minimal "Hello, World!" application.
 */
public class TagSelector extends Activity {
    private static final int DIALOG = 1;
    private static final String[] elements = { "hello world" };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final TextView tv = new TextView(this);
        tv.setText("hello world");
        tv.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tv.setText("clicked!");
                showAlert();
            }
        });

        setContentView(tv);

        // Set the layout for this activity.  You can find it
        // in res/layout/hello_activity.xml
        // setContentView(R.layout.hello_activity);
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle bundle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Welcome ");
        builder.setCancelable(true);
        builder.setItems(elements, null);
        return builder.create();
    }


    private void showAlert() {
        showDialog(DIALOG);
    }
}

