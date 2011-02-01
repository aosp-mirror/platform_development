/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.customlocale2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * Dialog to ask the user for a new locale. <p/> Returns the locale code (e.g.
 * "en_US") via an intent with a "locale" extra string and a "select" extra
 * boolean.
 */
public class NewLocaleDialog extends Activity implements View.OnClickListener {

    public static final String INTENT_EXTRA_LOCALE = "locale";
    public static final String INTENT_EXTRA_SELECT = "select";

    private static final String TAG = "NewLocale";
    private static final boolean DEBUG = true;
    private Button mButtonAdd;
    private Button mButtonAddSelect;
    private EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.new_locale);

        mEditText = (EditText) findViewById(R.id.value);

        mButtonAdd = (Button) findViewById(R.id.add);
        mButtonAdd.setOnClickListener(this);

        mButtonAddSelect = (Button) findViewById(R.id.add_and_select);
        mButtonAddSelect.setOnClickListener(this);
    }

    public void onClick(View v) {
        String locale = mEditText.getText().toString();
        boolean select = v == mButtonAddSelect;

        if (DEBUG) {
            Log.d(TAG, "New Locale: " + locale + (select ? " + select" : ""));
        }

        Intent data = new Intent(NewLocaleDialog.this, NewLocaleDialog.class);
        data.putExtra(INTENT_EXTRA_LOCALE, locale);
        data.putExtra(INTENT_EXTRA_SELECT, select);
        setResult(RESULT_OK, data);

        finish();
    }
}
