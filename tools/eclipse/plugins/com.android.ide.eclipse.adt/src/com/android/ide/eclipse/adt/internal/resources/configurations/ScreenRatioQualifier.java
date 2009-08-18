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

public class ScreenRatioQualifier extends ResourceQualifier {

    public static final String NAME = "Screen Ratio";

    private ScreenRatio mValue = null;

    /**
     * Screen Orientation enum.
     */
    public static enum ScreenRatio {
        NOTLONG("notlong", "Not Long"), //$NON-NLS-1$
        LONG("long", "Long"); //$NON-NLS-1$

        private String mValue;
        private String mDisplayValue;

        private ScreenRatio(String value, String displayValue) {
            mValue = value;
            mDisplayValue = displayValue;
        }

        /**
         * Returns the enum for matching the provided qualifier value.
         * @param value The qualifier value.
         * @return the enum for the qualifier value or null if no matching was found.
         */
        static ScreenRatio getEnum(String value) {
            for (ScreenRatio orient : values()) {
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

        public static int getIndex(ScreenRatio orientation) {
            int i = 0;
            for (ScreenRatio orient : values()) {
                if (orient == orientation) {
                    return i;
                }

                i++;
            }

            return -1;
        }

        public static ScreenRatio getByIndex(int index) {
            int i = 0;
            for (ScreenRatio orient : values()) {
                if (i == index) {
                    return orient;
                }
                i++;
            }

            return null;
        }
    }

    public ScreenRatioQualifier() {
    }

    public ScreenRatioQualifier(ScreenRatio value) {
        mValue = value;
    }

    public ScreenRatio getValue() {
        return mValue;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getShortName() {
        return "Ratio";
    }

    @Override
    public Image getIcon() {
        return IconFactory.getInstance().getIcon("ratio"); //$NON-NLS-1$
    }

    @Override
    public boolean isValid() {
        return mValue != null;
    }

    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        ScreenRatio size = ScreenRatio.getEnum(value);
        if (size != null) {
            ScreenRatioQualifier qualifier = new ScreenRatioQualifier(size);
            config.setScreenRatioQualifier(qualifier);
            return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object qualifier) {
        if (qualifier instanceof ScreenRatioQualifier) {
            return mValue == ((ScreenRatioQualifier)qualifier).mValue;
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
