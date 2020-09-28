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

import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;

/** Decodes key data and applies changes to {@link InputConnection}. */
final class Decoder {

    private final InputConnection mInputConnection;

    Decoder(InputConnection inputConnection) {
        this.mInputConnection = inputConnection;
    }

    void decodeAndApply(String data) {
        if ("DEL".equals(data)) {
            mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
        } else if ("ENT".equals(data)) {
            mInputConnection.sendKeyEvent(
                    new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
        } else if ("SPA".equals(data)) {
            mInputConnection.sendKeyEvent(
                    new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE));
        } else {
            mInputConnection.commitText(data, 1);
        }
    }

    boolean isEmpty() {
        if (mInputConnection.getTextBeforeCursor(1, 0).length() == 0
                && mInputConnection.getTextAfterCursor(1, 0).length() == 0) {
            return true;
        }
        return false;
    }
}
