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

import java.util.regex.Pattern;

/**
 * Resource Qualifier for Language.
 */
public final class LanguageQualifier extends ResourceQualifier {
    private final static Pattern sLanguagePattern = Pattern.compile("^[a-z]{2}$"); //$NON-NLS-1$

    public static final String NAME = "Language";

    private String mValue;

    /**
     * Creates and returns a qualifier from the given folder segment. If the segment is incorrect,
     * <code>null</code> is returned.
     * @param segment the folder segment from which to create a qualifier.
     * @return a new {@link LanguageQualifier} object or <code>null</code>
     */
    public static LanguageQualifier getQualifier(String segment) {
        if (sLanguagePattern.matcher(segment).matches()) {
            LanguageQualifier qualifier = new LanguageQualifier();
            qualifier.mValue = segment;

            return qualifier;
        }
        return null;
    }

    /**
     * Returns the folder name segment for the given value. This is equivalent to calling
     * {@link #toString()} on a {@link LanguageQualifier} object.
     * @param value the value of the qualifier, as returned by {@link #getValue()}.
     */
    public static String getFolderSegment(String value) {
        String segment = value.toLowerCase();
        if (sLanguagePattern.matcher(segment).matches()) {
            return segment;
        }

        return null;
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
        return IconFactory.getInstance().getIcon("language"); //$NON-NLS-1$
    }

    @Override
    public boolean isValid() {
        return mValue != null;
    }

    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        LanguageQualifier qualifier = getQualifier(value);
        if (qualifier != null) {
            config.setLanguageQualifier(qualifier);
            return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object qualifier) {
        if (qualifier instanceof LanguageQualifier) {
            if (mValue == null) {
                return ((LanguageQualifier)qualifier).mValue == null;
            }
            return mValue.equals(((LanguageQualifier)qualifier).mValue);
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
            return getFolderSegment(mValue);
        }

        return ""; //$NON-NLS-1$
    }

    @Override
    public String getStringValue() {
        if (mValue != null) {
            return mValue;
        }

        return ""; //$NON-NLS-1$
    }
}
