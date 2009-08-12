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
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.swt.graphics.Image;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resource Qualifier for Screen Pixel Density.
 */
public final class PixelDensityQualifier extends ResourceQualifier {
    private final static Pattern sDensityLegacyPattern = Pattern.compile("^(\\d+)dpi$");//$NON-NLS-1$

    public static final String NAME = "Pixel Density";

    private Density mValue = Density.MEDIUM;

    /**
     * Screen Orientation enum.
     */
    public static enum Density {
        HIGH("hdpi", 240, "High Density"), //$NON-NLS-1$
        MEDIUM("mdpi", 160, "Medium Density"), //$NON-NLS-1$
        LOW("ldpi", 120, "Low Density"), //$NON-NLS-1$
        NODPI("nodpi", -1, "No Density"); //$NON-NLS-1$

        private final String mValue;
        private final String mDisplayValue;
        private final int mDpiValue;

        private Density(String value, int dpiValue, String displayValue) {
            mValue = value;
            mDpiValue = dpiValue;
            mDisplayValue = displayValue;
        }

        /**
         * Returns the enum for matching the provided qualifier value.
         * @param value The qualifier value.
         * @return the enum for the qualifier value or null if no matching was found.
         */
        static Density getEnum(String value) {
            for (Density orient : values()) {
                if (orient.mValue.equals(value)) {
                    return orient;
                }
            }

            return null;
        }

        static Density getLegacyEnum(String value) {
            Matcher m = sDensityLegacyPattern.matcher(value);
            if (m.matches()) {
                String v = m.group(1);

                try {
                    int density = Integer.parseInt(v);
                    for (Density orient : values()) {
                        if (orient.mDpiValue == density) {
                            return orient;
                        }
                    }
                } catch (NumberFormatException e) {
                    // looks like the string we extracted wasn't a valid number
                    // which really shouldn't happen since the regexp would have failed.
                }
            }
            return null;
        }

        public String getValue() {
            return mValue;
        }

        public int getDpiValue() {
            return mDpiValue;
        }

        public String getLegacyValue() {
            if (this != NODPI) {
                return String.format("%1$ddpi", mDpiValue);
            }

            return "";
        }

        public String getDisplayValue() {
            return mDisplayValue;
        }

        public static int getIndex(Density value) {
            int i = 0;
            for (Density input : values()) {
                if (value == input) {
                    return i;
                }

                i++;
            }

            return -1;
        }

        public static Density getByIndex(int index) {
            int i = 0;
            for (Density value : values()) {
                if (i == index) {
                    return value;
                }
                i++;
            }
            return null;
        }
    }

    public PixelDensityQualifier() {
        // pass
    }

    public PixelDensityQualifier(Density value) {
        mValue = value;
    }

    public Density getValue() {
        return mValue;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getShortName() {
        return NAME;
    }

    @Override
    public Image getIcon() {
        return IconFactory.getInstance().getIcon("dpi"); //$NON-NLS-1$
    }

    @Override
    public boolean isValid() {
        return mValue != null;
    }

    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        Density density = Density.getEnum(value);
        if (density == null) {
            density = Density.getLegacyEnum(value);
        }

        if (density != null) {
            PixelDensityQualifier qualifier = new PixelDensityQualifier();
            qualifier.mValue = density;
            config.setPixelDensityQualifier(qualifier);
            return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object qualifier) {
        if (qualifier instanceof PixelDensityQualifier) {
            return mValue == ((PixelDensityQualifier)qualifier).mValue;
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
            if (target != null) {
                AndroidVersion version = target.getVersion();
                if (version.getApiLevel() <= 3 && version.getCodename() == null) {
                    return mValue.getLegacyValue();
                }
            }
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
