/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.example.android.autofillkeyboard;

import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/** Controls the visible virtual keyboard view. */
final class Keyboard {

    private static final int NUM_STATES = 4;
    private static final int STATE_SHIFT = 1;
    private static final int STATE_SYMBOL = 2;

    private final AutofillImeService mAutofillImeService;
    private final int mViewResId;
    private final SparseArray<String> mKeyMapping;
    private View mKeyboardView;
    private int mState;

    private Keyboard(AutofillImeService autofillImeService, int viewResId,
            SparseArray<String> keyMapping) {
        this.mAutofillImeService = autofillImeService;
        this.mViewResId = viewResId;
        this.mKeyMapping = keyMapping;
        this.mState = 0;
    }

    private static String getLabel(String data) {
        if ("SHI".equals(data)) {
            return "↑";
        } else if ("DEL".equals(data)) {
            return "←";
        } else if ("SYM".equals(data)) {
            return "?123";
        } else if ("SPA".equals(data)) {
            return "[            ]";
        } else if ("ENT".equals(data)) {
            return "↩";
        } else {
            return data;
        }
    }

    static Keyboard qwerty(AutofillImeService autofillImeService) {
        SparseArray<String> keyMapping = new SparseArray<>();
        keyMapping.put(R.id.key_pos_0_0, "qQ1\u007E");
        keyMapping.put(R.id.key_pos_0_1, "wW2\u0060");
        keyMapping.put(R.id.key_pos_0_2, "eE3\u007C");
        keyMapping.put(R.id.key_pos_0_3, "rR4\u2022");
        keyMapping.put(R.id.key_pos_0_4, "tT5\u221A");
        keyMapping.put(R.id.key_pos_0_5, "yY6\u03C0");
        keyMapping.put(R.id.key_pos_0_6, "uU7\u00F7");
        keyMapping.put(R.id.key_pos_0_7, "iI8\u00D7");
        keyMapping.put(R.id.key_pos_0_8, "oO9\u00B6");
        keyMapping.put(R.id.key_pos_0_9, "pP0\u2206");
        keyMapping.put(R.id.key_pos_1_0, "aA@\u00A3");
        keyMapping.put(R.id.key_pos_1_1, "sS#\u00A2");
        keyMapping.put(R.id.key_pos_1_2, "dD$\u20AC");
        keyMapping.put(R.id.key_pos_1_3, "fF_\u00A5");
        keyMapping.put(R.id.key_pos_1_4, "gG&\u005E");
        keyMapping.put(R.id.key_pos_1_5, "hH-=");
        keyMapping.put(R.id.key_pos_1_6, "jJ+{");
        keyMapping.put(R.id.key_pos_1_7, "kK(}");
        keyMapping.put(R.id.key_pos_1_8, "lL)\\");
        keyMapping.put(R.id.key_pos_2_0, "zZ*%");
        keyMapping.put(R.id.key_pos_2_1, "xX\"\u00A9");
        keyMapping.put(R.id.key_pos_2_2, "cC'\u00AE");
        keyMapping.put(R.id.key_pos_2_3, "vV:\u2122");
        keyMapping.put(R.id.key_pos_2_4, "bB;\u2713");
        keyMapping.put(R.id.key_pos_2_5, "nN![");
        keyMapping.put(R.id.key_pos_2_6, "mM?]");
        keyMapping.put(R.id.key_pos_bottom_0, ",,,<");
        keyMapping.put(R.id.key_pos_bottom_1, "...>");
        keyMapping.put(R.id.key_pos_shift, "SHI");
        keyMapping.put(R.id.key_pos_del, "DEL");
        keyMapping.put(R.id.key_pos_symbol, "SYM");
        keyMapping.put(R.id.key_pos_space, "SPA");
        keyMapping.put(R.id.key_pos_enter, "ENT");
        return new Keyboard(autofillImeService, R.layout.keyboard_10_9_9, keyMapping);
    }

    View inflateKeyboardView(LayoutInflater inflater, InputView inputView) {
        mKeyboardView = inflater.inflate(mViewResId, inputView, false);
        mapKeys();
        return mKeyboardView;
    }

    void reset() {
        mState = 0;
        mapKeys();
    }

    private void mapKeys() {
        for (int i = 0; i < mKeyMapping.size(); i++) {
            TextView softkey = mKeyboardView.findViewById(mKeyMapping.keyAt(i));
            String rawData = mKeyMapping.valueAt(i);
            String data = rawData.length() != NUM_STATES ? rawData : rawData.substring(mState,
                    mState + 1);
            softkey.setText(getLabel(data));
            softkey.setOnClickListener(v -> handle(data));
        }
    }

    private void handle(String data) {
        if ("SHI".equals(data)) {
            // Toggle STATE_SHIFT.
            mState = mState ^ STATE_SHIFT;
            mapKeys();
        } else if ("SYM".equals(data)) {
            // Toggle STATE_SYMBOL and clear STATE_SHIFT.
            mState = (mState ^ STATE_SYMBOL) & ~STATE_SHIFT;
            mapKeys();
        } else {
            mAutofillImeService.handle(data);
        }
    }
}
