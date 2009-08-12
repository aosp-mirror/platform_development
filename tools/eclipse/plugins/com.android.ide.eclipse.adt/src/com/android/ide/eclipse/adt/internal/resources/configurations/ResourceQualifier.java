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

import com.android.sdklib.IAndroidTarget;

import org.eclipse.swt.graphics.Image;

/**
 * Base class for resource qualifiers.
 * <p/>The resource qualifier classes are designed as immutable.
 */
public abstract class ResourceQualifier implements Comparable<ResourceQualifier> {

    /**
     * Returns the human readable name of the qualifier.
     */
    public abstract String getName();

    /**
     * Returns a shorter human readable name for the qualifier.
     * @see #getName()
     */
    public abstract String getShortName();

    /**
     * Returns the icon for the qualifier.
     */
    public abstract Image getIcon();

    /**
     * Returns whether the qualifier has a valid filter value.
     */
    public abstract boolean isValid();

    /**
     * Check if the value is valid for this qualifier, and if so sets the value
     * into a Folder Configuration.
     * @param value The value to check and set. Must not be null.
     * @param config The folder configuration to receive the value. Must not be null.
     * @return true if the value was valid and was set.
     */
    public abstract boolean checkAndSet(String value, FolderConfiguration config);

    /**
     * Returns a string formated to be used in a folder name.
     * <p/>This is declared as abstract to force children classes to implement it.
     */
    public abstract String getFolderSegment(IAndroidTarget target);

    @Override
    public String toString() {
        return getFolderSegment(null);
    }

    /**
     * Returns a string formatted for display purpose.
     */
    public abstract String getStringValue();

    /**
     * Returns <code>true</code> if both objects are equal.
     * <p/>This is declared as abstract to force children classes to implement it.
     */
    @Override
    public abstract boolean equals(Object object);

    /**
     * Returns a hash code value for the object.
     * <p/>This is declared as abstract to force children classes to implement it.
     */
    @Override
    public abstract int hashCode();

    public final int compareTo(ResourceQualifier o) {
        return toString().compareTo(o.toString());
    }
}
