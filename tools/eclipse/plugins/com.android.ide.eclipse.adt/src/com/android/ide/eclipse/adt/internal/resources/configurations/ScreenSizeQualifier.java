/*
 * Copyright (C) 2009 The Android Open Source Project
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
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.swt.graphics.Image;

/**
 * Resource Qualifier for Screen Size. Size can be "small", "normal", and "large"
 */
public class ScreenSizeQualifier extends ResourceQualifier {

    public static final String NAME = "Screen Size";

    private ScreenSize mValue = null;

    /**
     * Screen Orientation enum.
     */
    public static enum ScreenSize {
        SMALL("small", "Small"), //$NON-NLS-1$
        NORMAL("normal", "Normal"), //$NON-NLS-1$
        LARGE("large", "Large"); //$NON-NLS-1$

        private String mValue;
        private String mDisplayValue;

        private ScreenSize(String value, String displayValue) {
            mValue = value;
            mDisplayValue = displayValue;
        }

        /**
         * Returns the enum for matching the provided qualifier value.
         * @param value The qualifier value.
         * @return the enum for the qualifier value or null if no matching was found.
         */
        static ScreenSize getEnum(String value) {
            for (ScreenSize orient : values()) {
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

        public static int getIndex(ScreenSize orientation) {
            int i = 0;
            for (ScreenSize orient : values()) {
                if (orient == orientation) {
                    return i;
                }

                i++;
            }

            return -1;
        }

        public static ScreenSize getByIndex(int index) {
            int i = 0;
            for (ScreenSize orient : values()) {
                if (i == index) {
                    return orient;
                }
                i++;
            }

            return null;
        }
    }

    public ScreenSizeQualifier() {
    }

    public ScreenSizeQualifier(ScreenSize value) {
        mValue = value;
    }

    public ScreenSize getValue() {
        return mValue;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getShortName() {
        return "Size";
    }

    @Override
    public Image getIcon() {
        return IconFactory.getInstance().getIcon("size"); //$NON-NLS-1$
    }

    @Override
    public boolean isValid() {
        return mValue != null;
    }

    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        ScreenSize size = ScreenSize.getEnum(value);
        if (size != null) {
            ScreenSizeQualifier qualifier = new ScreenSizeQualifier(size);
            config.setScreenSizeQualifier(qualifier);
            return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object qualifier) {
        if (qualifier instanceof ScreenSizeQualifier) {
            return mValue == ((ScreenSizeQualifier)qualifier).mValue;
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
            if (target == null) {
                // Default behavior (when target==null) is qualifier is supported
                return mValue.getValue();
            }

            AndroidVersion version = target.getVersion();
            if (version.getApiLevel() >= 4 ||
                    (version.getApiLevel() == 3 && "Donut".equals(version.getCodename()))) {
                return mValue.getValue();
            }
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
