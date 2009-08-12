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
 * Resource Qualifier for Screen Orientation.
 */
public final class ScreenOrientationQualifier extends ResourceQualifier {

    public static final String NAME = "Screen Orientation";

    private ScreenOrientation mValue = null;

    /**
     * Screen Orientation enum.
     */
    public static enum ScreenOrientation {
        PORTRAIT("port", "Portrait"), //$NON-NLS-1$
        LANDSCAPE("land", "Landscape"), //$NON-NLS-1$
        SQUARE("square", "Square"); //$NON-NLS-1$

        private String mValue;
        private String mDisplayValue;

        private ScreenOrientation(String value, String displayValue) {
            mValue = value;
            mDisplayValue = displayValue;
        }

        /**
         * Returns the enum for matching the provided qualifier value.
         * @param value The qualifier value.
         * @return the enum for the qualifier value or null if no matching was found.
         */
        static ScreenOrientation getEnum(String value) {
            for (ScreenOrientation orient : values()) {
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

        public static int getIndex(ScreenOrientation orientation) {
            int i = 0;
            for (ScreenOrientation orient : values()) {
                if (orient == orientation) {
                    return i;
                }

                i++;
            }

            return -1;
        }

        public static ScreenOrientation getByIndex(int index) {
            int i = 0;
            for (ScreenOrientation orient : values()) {
                if (i == index) {
                    return orient;
                }
                i++;
            }

            return null;
        }
    }

    public ScreenOrientationQualifier() {
    }

    public ScreenOrientationQualifier(ScreenOrientation value) {
        mValue = value;
    }

    public ScreenOrientation getValue() {
        return mValue;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getShortName() {
        return "Orientation";
    }

    @Override
    public Image getIcon() {
        return IconFactory.getInstance().getIcon("orientation"); //$NON-NLS-1$
    }

    @Override
    public boolean isValid() {
        return mValue != null;
    }

    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        ScreenOrientation orientation = ScreenOrientation.getEnum(value);
        if (orientation != null) {
            ScreenOrientationQualifier qualifier = new ScreenOrientationQualifier(orientation);
            config.setScreenOrientationQualifier(qualifier);
            return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object qualifier) {
        if (qualifier instanceof ScreenOrientationQualifier) {
            return mValue == ((ScreenOrientationQualifier)qualifier).mValue;
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
