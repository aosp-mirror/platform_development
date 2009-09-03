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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resource Qualifier for Region.
 */
public final class RegionQualifier extends ResourceQualifier {
    private final static Pattern sRegionPattern = Pattern.compile("^r([A-Z]{2})$"); //$NON-NLS-1$

    public static final String NAME = "Region";

    private String mValue;

    /**
     * Creates and returns a qualifier from the given folder segment. If the segment is incorrect,
     * <code>null</code> is returned.
     * @param segment the folder segment from which to create a qualifier.
     * @return a new {@link RegionQualifier} object or <code>null</code>
     */
    public static RegionQualifier getQualifier(String segment) {
        Matcher m = sRegionPattern.matcher(segment);
        if (m.matches()) {
            RegionQualifier qualifier = new RegionQualifier();
            qualifier.mValue = m.group(1);

            return qualifier;
        }
        return null;
    }

    /**
     * Returns the folder name segment for the given value. This is equivalent to calling
     * {@link #toString()} on a {@link RegionQualifier} object.
     * @param value the value of the qualifier, as returned by {@link #getValue()}.
     */
    public static String getFolderSegment(String value) {
        if (value != null) {
            String segment = "r" + value.toUpperCase(); //$NON-NLS-1$
            if (sRegionPattern.matcher(segment).matches()) {
                return segment;
            }
        }

        return "";  //$NON-NLS-1$
    }

    public String getValue() {
        if (mValue != null) {
            return mValue;
        }

        return ""; //$NON-NLS-1$
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
        return IconFactory.getInstance().getIcon("region"); //$NON-NLS-1$
    }

    @Override
    public boolean isValid() {
        return mValue != null;
    }

    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        RegionQualifier qualifier = getQualifier(value);
        if (qualifier != null) {
            config.setRegionQualifier(qualifier);
            return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object qualifier) {
        if (qualifier instanceof RegionQualifier) {
            if (mValue == null) {
                return ((RegionQualifier)qualifier).mValue == null;
            }
            return mValue.equals(((RegionQualifier)qualifier).mValue);
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
        return getFolderSegment(mValue);
    }

    @Override
    public String getStringValue() {
        if (mValue != null) {
            return mValue;
        }

        return ""; //$NON-NLS-1$
    }
}
