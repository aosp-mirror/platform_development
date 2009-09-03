/*
 * Copyright (C) 2008 The Android Open Source Project
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
 * Resource Qualifier for keyboard state.
 */
public final class KeyboardStateQualifier extends ResourceQualifier {

    public static final String NAME = "Keyboard State";

    private KeyboardState mValue = null;

    /**
     * Screen Orientation enum.
     */
    public static enum KeyboardState {
        EXPOSED("keysexposed", "Exposed"), //$NON-NLS-1$
        HIDDEN("keyshidden", "Hidden"); //$NON-NLS-1$

        private String mValue;
        private String mDisplayValue;

        private KeyboardState(String value, String displayValue) {
            mValue = value;
            mDisplayValue = displayValue;
        }

        /**
         * Returns the enum for matching the provided qualifier value.
         * @param value The qualifier value.
         * @return the enum for the qualifier value or null if no matching was found.
         */
        static KeyboardState getEnum(String value) {
            for (KeyboardState orient : values()) {
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

        public static int getIndex(KeyboardState value) {
            int i = 0;
            for (KeyboardState input : values()) {
                if (value == input) {
                    return i;
                }

                i++;
            }

            return -1;
        }

        public static KeyboardState getByIndex(int index) {
            int i = 0;
            for (KeyboardState value : values()) {
                if (i == index) {
                    return value;
                }
                i++;
            }
            return null;
        }
    }

    public KeyboardStateQualifier() {
        // pass
    }

    public KeyboardStateQualifier(KeyboardState value) {
        mValue = value;
    }

    public KeyboardState getValue() {
        return mValue;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getShortName() {
        return "Keyboard";
    }

    @Override
    public Image getIcon() {
        return IconFactory.getInstance().getIcon("keyboard"); //$NON-NLS-1$
    }

    @Override
    public boolean isValid() {
        return mValue != null;
    }

    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        KeyboardState orientation = KeyboardState.getEnum(value);
        if (orientation != null) {
            KeyboardStateQualifier qualifier = new KeyboardStateQualifier();
            qualifier.mValue = orientation;
            config.setKeyboardStateQualifier(qualifier);
            return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object qualifier) {
        if (qualifier instanceof KeyboardStateQualifier) {
            return mValue == ((KeyboardStateQualifier)qualifier).mValue;
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
