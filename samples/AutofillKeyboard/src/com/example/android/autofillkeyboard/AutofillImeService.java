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

import android.inputmethodservice.InputMethodService;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;

/** The {@link InputMethodService} implementation for Autofill keyboard. */
public class AutofillImeService extends InputMethodService {

    private InputView mInputView;
    private Decoder mDecoder;

    @Override
    public View onCreateInputView() {
        mInputView = (InputView) LayoutInflater.from(this).inflate(R.layout.input_view, null);
        return mInputView;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        mDecoder = new Decoder(getCurrentInputConnection());
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        mInputView.removeAllViews();
        Keyboard keyboard = Keyboard.qwerty(this);
        mInputView.addView(keyboard.inflateKeyboardView(LayoutInflater.from(this), mInputView));
    }

    @Override
    public void onComputeInsets(Insets outInsets) {
        super.onComputeInsets(outInsets);
        if (mInputView != null) {
            outInsets.contentTopInsets += mInputView.getTopInsets();
        }
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_CONTENT;
    }

    void handle(String data) {
        mDecoder.decode(data);
    }
}
