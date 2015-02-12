/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.apis.content;

import com.example.android.apis.R;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * Simple example of using an UndoManager for editing text in a TextView.
 */
public class TextUndoActivity extends Activity {
    // Characters allowed as input in the credit card field.
    private static final String CREDIT_CARD_CHARS = "0123456789 ";

    EditText mDefaultText;
    EditText mLengthLimitText;
    EditText mCreditCardText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.text_undo);

        mDefaultText = (EditText) findViewById(R.id.default_text);
        ((Button) findViewById(R.id.set_text)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDefaultText.setText("some text");
            }
        });
        ((Button) findViewById(R.id.append_text)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDefaultText.append(" append");
            }
        });
        ((Button) findViewById(R.id.insert_text)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Editable editable = mDefaultText.getText();
                editable.insert(0, "insert ");
            }
        });

        mLengthLimitText = (EditText) findViewById(R.id.length_limit_text);
        mLengthLimitText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(4) });

        mCreditCardText = (EditText) findViewById(R.id.credit_card_text);
        mCreditCardText.setKeyListener(DigitsKeyListener.getInstance(CREDIT_CARD_CHARS));
        mCreditCardText.addTextChangedListener(new CreditCardTextWatcher());
     }

    /**
     * A simple credit card input formatter that adds spaces every 4 characters.
     */
    private static class CreditCardTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            String original = s.toString();
            String formatted = addSpaces(getNumbers(original));
            // This is an ugly way to avoid infinite recursion, but it's common in app code.
            if (!formatted.equals(original)) {
                s.replace(0, s.length(), formatted);
            }
        }

        /**
         * @return Returns a string with a space added every 4 characters.
         */
        private static String addSpaces(CharSequence str) {
            StringBuilder builder = new StringBuilder();
            int len = str.length();
            for (int i = 0; i < len; i += 4) {
                if (i + 4 < len) {
                    builder.append(str.subSequence(i, i + 4));
                    builder.append(' ');
                } else {
                    // Don't put a space after the end.
                    builder.append(str.subSequence(i, len));
                }
            }
            return builder.toString();
        }

        /**
         * @return Returns a string containing only the digits from a character sequence.
         */
        private static String getNumbers(CharSequence cc) {
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0, count = cc.length(); i < count; ++i) {
                char c = cc.charAt(i);
                if (isNumber(c)) {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private static boolean isNumber(char c) {
            return c >= '0' && c <= '9';
        }

    }
}
