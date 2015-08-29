/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.directshare;

import android.widget.TextView;

/**
 * A simple utility to bind a {@link TextView} with a {@link Contact}.
 */
public class ContactViewBinder {

    /**
     * Binds the {@code textView} with the specified {@code contact}.
     *
     * @param contact  The contact.
     * @param textView The TextView.
     */
    public static void bind(Contact contact, TextView textView) {
        textView.setText(contact.getName());
        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(contact.getIcon(), 0, 0, 0);
    }

}
