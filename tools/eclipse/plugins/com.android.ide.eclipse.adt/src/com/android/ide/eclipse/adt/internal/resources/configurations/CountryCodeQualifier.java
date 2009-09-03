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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resource Qualifier for Mobile Country Code.
 */
public final class CountryCodeQualifier extends ResourceQualifier {
    /** Default pixel density value. This means the property is not set. */
    private final static int DEFAULT_CODE = -1;

    private final static Pattern sCountryCodePattern = Pattern.compile("^mcc(\\d{3})$");//$NON-NLS-1$

    private int mCode = DEFAULT_CODE;

    public static final String NAME = "Mobile Country Code";

    /**
     * Creates and returns a qualifier from the given folder segment. If the segment is incorrect,
     * <code>null</code> is returned.
     * @param segment the folder segment from which to create a qualifier.
     * @return a new {@link CountryCodeQualifier} object or <code>null</code>
     */
    public static CountryCodeQualifier getQualifier(String segment) {
        Matcher m = sCountryCodePattern.matcher(segment);
        if (m.matches()) {
            String v = m.group(1);

            int code = -1;
            try {
                code = Integer.parseInt(v);
            } catch (NumberFormatException e) {
                // looks like the string we extracted wasn't a valid number.
                return null;
            }

            CountryCodeQualifier qualifier = new CountryCodeQualifier();
            qualifier.mCode = code;
            return qualifier;
        }

        return null;
    }

    /**
     * Returns the folder name segment for the given value. This is equivalent to calling
     * {@link #toString()} on a {@link CountryCodeQualifier} object.
     * @param code the value of the qualifier, as returned by {@link #getCode()}.
     */
    public static String getFolderSegment(int code) {
        if (code != DEFAULT_CODE && code >= 100 && code <=999) { // code is 3 digit.) {
            return String.format("mcc%1$d", code); //$NON-NLS-1$
        }

        return ""; //$NON-NLS-1$
    }

    public int getCode() {
        return mCode;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getShortName() {
        return "Country Code";
    }

    @Override
    public Image getIcon() {
        return IconFactory.getInstance().getIcon("mcc"); //$NON-NLS-1$
    }

    @Override
    public boolean isValid() {
        return mCode != DEFAULT_CODE;
    }

    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        CountryCodeQualifier qualifier = getQualifier(value);
        if (qualifier != null) {
            config.setCountryCodeQualifier(qualifier);
            return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object qualifier) {
        if (qualifier instanceof CountryCodeQualifier) {
            return mCode == ((CountryCodeQualifier)qualifier).mCode;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return mCode;
    }

    /**
     * Returns the string used to represent this qualifier in the folder name.
     */
    @Override
    public String getFolderSegment(IAndroidTarget target) {
        return getFolderSegment(mCode);
    }

    @Override
    public String getStringValue() {
        if (mCode != DEFAULT_CODE) {
            return String.format("MCC %1$d", mCode);
        }

        return ""; //$NON-NLS-1$
    }
}
