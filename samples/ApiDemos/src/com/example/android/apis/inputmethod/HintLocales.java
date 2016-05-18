/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.example.android.apis.inputmethod;

import android.app.Activity;
import android.os.Bundle;
import android.os.LocaleList;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import static android.widget.LinearLayout.VERTICAL;

/**
 * Provide some {@link EditText} with specifying
 * {@link android.view.inputmethod.EditorInfo#hintLocales} so that IME developers can test their
 * IMEs.
 */
public class HintLocales extends Activity {

    /**
     * Creates a new instance of {@link EditText} that is configured to specify the given
     * {@link LocaleList} to {@link android.view.inputmethod.EditorInfo#hintLocales} so that
     * developers can locally test how the current input method behaves for the given hint locales.
     *
     * <p><b>Note:</b> {@link android.view.inputmethod.EditorInfo#hintLocales} is just a hint for
     * the input method. IME developers can decide how to use it.</p>
     *
     * @return A new instance of {@link EditText}, which specifies
     * {@link android.view.inputmethod.EditorInfo#hintLocales} with the given {@link LocaleList}.
     * @param hintLocales an ordered list of locales to be specified to
     * {@link android.view.inputmethod.EditorInfo#hintLocales}.
     * @see android.view.inputmethod.EditorInfo#hintLocales
     * @see TextView#setImeHintLocales(LocaleList)
     * @see LocaleList
     */
    @NonNull
    private EditText createEditTextWithImeHintLocales(@Nullable LocaleList hintLocales) {
        final EditText exitText = new EditText(this);
        if (hintLocales == null) {
            exitText.setHint("EditorInfo#hintLocales: (null)");
        } else {
            exitText.setHint("EditorInfo#hintLocales: " + hintLocales.toLanguageTags());
        }
        // Both null and non-null locale list are supported.
        exitText.setImeHintLocales(hintLocales);
        return exitText;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(VERTICAL);

        // Test EditorInfo#hintLocales = null. This is the default behavior and should be the same
        // to the behavior in Android M and prior.
        layout.addView(createEditTextWithImeHintLocales(null));

        // This gives a hint that the application is confident that the user wants to input text
        // for "en-US" in this text field.  Note that IME developers can decide how to use this
        // hint.
        layout.addView(createEditTextWithImeHintLocales(LocaleList.forLanguageTags("en-US")));

        // Likewise, this gives a hint as a list of locales in the order of likelihood.
        layout.addView(createEditTextWithImeHintLocales(
                LocaleList.forLanguageTags("en-GB,en-US,en")));

        // Being able to support 3-letter language code correctly is really important.
        layout.addView(createEditTextWithImeHintLocales(LocaleList.forLanguageTags("fil-ph")));

        // Likewise, test some more locales.
        layout.addView(createEditTextWithImeHintLocales(LocaleList.forLanguageTags("fr")));
        layout.addView(createEditTextWithImeHintLocales(LocaleList.forLanguageTags("zh_CN")));
        layout.addView(createEditTextWithImeHintLocales(LocaleList.forLanguageTags("ja")));

        // Test more complex BCP 47 language tag.  Here the subtag starts with "x-" is a private-use
        // subtags.
        layout.addView(createEditTextWithImeHintLocales(
                LocaleList.forLanguageTags("ryu-Kana-JP-x-android")));

        setContentView(layout);
    }
}
