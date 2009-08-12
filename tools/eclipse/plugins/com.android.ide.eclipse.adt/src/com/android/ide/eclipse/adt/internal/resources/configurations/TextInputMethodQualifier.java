/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.internal.resources.configurations;

import com.android.ide.eclipse.adt.internal.editors.IconFactory;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.swt.graphics.Image;




/**
 * Resource Qualifier for Text Input Method.
 */
public final class TextInputMethodQualifier extends ResourceQualifier {

    public static final String NAME = "Text Input Method";

    private TextInputMethod mValue;

    /**
     * Screen Orientation enum.
     */
    public static enum TextInputMethod {
        NOKEY("nokeys", "No Keys"), //$NON-NLS-1$
        QWERTY("qwerty", "Qwerty"), //$NON-NLS-1$
        TWELVEKEYS("12key", "12 Key"); //$NON-NLS-1$

        private String mValue;
        private String mDisplayValue;

        private TextInputMethod(String value, String displayValue) {
            mValue = value;
            mDisplayValue = displayValue;
        }

        /**
         * Returns the enum for matching the provided qualifier value.
         * @param value The qualifier value.
         * @return the enum for the qualifier value or null if no matching was found.
         */
        static TextInputMethod getEnum(String value) {
            for (TextInputMethod orient : values()) {
                if (orient.mValue.equals(value)) {
                    return orient;
                }
            }

            return null;
        }

        public String getValue() {
            return mValue;
        }

        public String getDisplayValue() {
            return mDisplayValue;
        }

        public static int getIndex(TextInputMethod value) {
            int i = 0;
            for (TextInputMethod input : values()) {
                if (value == input) {
                    return i;
                }

                i++;
            }

            return -1;
        }

        public static TextInputMethod getByIndex(int index) {
            int i = 0;
            for (TextInputMethod value : values()) {
                if (i == index) {
                    return value;
                }
                i++;
            }
            return null;
        }
    }

    public TextInputMethodQualifier() {
        // pass
    }

    public TextInputMethodQualifier(TextInputMethod value) {
        mValue = value;
    }

    public TextInputMethod getValue() {
        return mValue;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getShortName() {
        return "Text Input";
    }

    @Override
    public Image getIcon() {
        return IconFactory.getInstance().getIcon("text_input"); //$NON-NLS-1$
    }

    @Override
    public boolean isValid() {
        return mValue != null;
    }

    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        TextInputMethod method = TextInputMethod.getEnum(value);
        if (method != null) {
            TextInputMethodQualifier qualifier = new TextInputMethodQualifier();
            qualifier.mValue = method;
            config.setTextInputMethodQualifier(qualifier);
            return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object qualifier) {
        if (qualifier instanceof TextInputMethodQualifier) {
            return mValue == ((TextInputMethodQualifier)qualifier).mValue;
        }

        return false;
    }

    @Override
    public int hashCode() {
        if (mValue != null) {
            return mValue.hashCode();
        }

        return 0;
    }

    /**
     * Returns the string used to represent this qualifier in the folder name.
     */
    @Override
    public String getFolderSegment(IAndroidTarget target) {
        if (mValue != null) {
            return mValue.getValue();
        }

        return ""; //$NON-NLS-1$
    }

    @Override
    public String getStringValue() {
        if (mValue != null) {
            return mValue.getDisplayValue();
        }

        return ""; //$NON-NLS-1$
    }
}
