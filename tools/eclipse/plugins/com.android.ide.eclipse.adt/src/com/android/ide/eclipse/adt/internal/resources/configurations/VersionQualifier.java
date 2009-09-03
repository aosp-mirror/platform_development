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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resource Qualifier for Platform Version.
 */
public final class VersionQualifier extends ResourceQualifier {
    /** Default pixel density value. This means the property is not set. */
    private final static int DEFAULT_VERSION = -1;

    private final static Pattern sCountryCodePattern = Pattern.compile("^v(\\d+)$");//$NON-NLS-1$

    private int mVersion = DEFAULT_VERSION;

    public static final String NAME = "Platform Version";

    /**
     * Creates and returns a qualifier from the given folder segment. If the segment is incorrect,
     * <code>null</code> is returned.
     * @param segment the folder segment from which to create a qualifier.
     * @return a new {@link VersionQualifier} object or <code>null</code>
     */
    public static VersionQualifier getQualifier(String segment) {
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

            VersionQualifier qualifier = new VersionQualifier();
            qualifier.mVersion = code;
            return qualifier;
        }

        return null;
    }

    /**
     * Returns the folder name segment for the given value. This is equivalent to calling
     * {@link #toString()} on a {@link VersionQualifier} object.
     * @param version the value of the qualifier, as returned by {@link #getVersion()}.
     */
    public static String getFolderSegment(int version) {
        if (version != DEFAULT_VERSION) {
            return String.format("v%1$d", version); //$NON-NLS-1$
        }

        return ""; //$NON-NLS-1$
    }

    public int getVersion() {
        return mVersion;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getShortName() {
        return "Version";
    }

    @Override
    public Image getIcon() {
        return IconFactory.getInstance().getIcon("version"); //$NON-NLS-1$
    }

    @Override
    public boolean isValid() {
        return mVersion != DEFAULT_VERSION;
    }

    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        VersionQualifier qualifier = getQualifier(value);
        if (qualifier != null) {
            config.setVersionQualifier(qualifier);
            return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object qualifier) {
        if (qualifier instanceof VersionQualifier) {
            return mVersion == ((VersionQualifier)qualifier).mVersion;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return mVersion;
    }

    /**
     * Returns the string used to represent this qualifier in the folder name.
     */
    @Override
    public String getFolderSegment(IAndroidTarget target) {
        if (target == null) {
            // Default behavior (when target==null) is qualifier is supported
            return getFolderSegment(mVersion);
        }

        AndroidVersion version = target.getVersion();
        if (version.getApiLevel() >= 3) {
            return getFolderSegment(mVersion);
        }

        return ""; //$NON-NLS-1$
    }

    @Override
    public String getStringValue() {
        if (mVersion != DEFAULT_VERSION) {
            return String.format("API %1$d", mVersion);
        }

        return ""; //$NON-NLS-1$
    }
}
