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
 * Resource Qualifier for Navigation Method.
 */
public final class NavigationMethodQualifier extends ResourceQualifier {

    public static final String NAME = "Navigation Method";

    private NavigationMethod mValue;

    /**
     * Navigation Method enum.
     */
    public static enum NavigationMethod {
        DPAD("dpad", "D-pad"), //$NON-NLS-1$
        TRACKBALL("trackball", "Trackball"), //$NON-NLS-1$
        WHEEL("wheel", "Wheel"), //$NON-NLS-1$
        NONAV("nonav", "No Navigation"); //$NON-NLS-1$

        private String mValue;
        private String mDisplay;

        private NavigationMethod(String value, String display) {
            mValue = value;
            mDisplay = display;
        }

        /**
         * Returns the enum for matching the provided qualifier value.
         * @param value The qualifier value.
         * @return the enum for the qualifier value or null if no matching was found.
         */
        static NavigationMethod getEnum(String value) {
            for (NavigationMethod orient : values()) {
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
            return mDisplay;
        }

        public static int getIndex(NavigationMethod value) {
            int i = 0;
            for (NavigationMethod nav : values()) {
                if (nav == value) {
                    return i;
                }

                i++;
            }

            return -1;
        }

        public static NavigationMethod getByIndex(int index) {
            int i = 0;
            for (NavigationMethod value : values()) {
                if (i == index) {
                    return value;
                }
                i++;
            }
            return null;
        }
    }

    public NavigationMethodQualifier() {
        // pass
    }

    public NavigationMethodQualifier(NavigationMethod value) {
        mValue = value;
    }

    public NavigationMethod getValue() {
        return mValue;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getShortName() {
        return "Navigation";
    }


    @Override
    public Image getIcon() {
        return IconFactory.getInstance().getIcon("navpad"); //$NON-NLS-1$
    }

    @Override
    public boolean isValid() {
        return mValue != null;
    }

    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        NavigationMethod method = NavigationMethod.getEnum(value);
        if (method != null) {
            NavigationMethodQualifier qualifier = new NavigationMethodQualifier();
            qualifier.mValue = method;
            config.setNavigationMethodQualifier(qualifier);
            return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object qualifier) {
        if (qualifier instanceof NavigationMethodQualifier) {
            return mValue == ((NavigationMethodQualifier)qualifier).mValue;
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
