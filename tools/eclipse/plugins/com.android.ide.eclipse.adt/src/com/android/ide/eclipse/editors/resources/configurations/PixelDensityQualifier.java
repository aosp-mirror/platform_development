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

package com.android.ide.eclipse.editors.resources.configurations;

import com.android.ide.eclipse.editors.IconFactory;

import org.eclipse.swt.graphics.Image;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resource Qualifier for Screen Pixel Density.
 */
public final class PixelDensityQualifier extends ResourceQualifier {
    /** Default pixel density value. This means the property is not set. */
    private final static int DEFAULT_DENSITY = -1;

    private final static Pattern sPixelDensityPattern = Pattern.compile("^(\\d+)dpi$");//$NON-NLS-1$

    public static final String NAME = "Pixel Density";

    private int mValue = DEFAULT_DENSITY;
    
    /**
     * Creates and returns a qualifier from the given folder segment. If the segment is incorrect,
     * <code>null</code> is returned.
     * @param folderSegment the folder segment from which to create a qualifier.
     * @return a new {@link CountryCodeQualifier} object or <code>null</code>
     */
    public static PixelDensityQualifier getQualifier(String folderSegment) {
        Matcher m = sPixelDensityPattern.matcher(folderSegment);
        if (m.matches()) {
            String v = m.group(1);

            int density = -1;
            try {
                density = Integer.parseInt(v);
            } catch (NumberFormatException e) {
                // looks like the string we extracted wasn't a valid number.
                return null;
            }
            
            PixelDensityQualifier qualifier = new PixelDensityQualifier();
            qualifier.mValue = density;
            
            return qualifier;
        }
        return null;
    }

    /**
     * Returns the folder name segment for the given value. This is equivalent to calling
     * {@link #toString()} on a {@link NetworkCodeQualifier} object.
     * @param value the value of the qualifier, as returned by {@link #getValue()}.
     */
    public static String getFolderSegment(int value) {
        if (value != DEFAULT_DENSITY) {
            return String.format("%1$ddpi", value); //$NON-NLS-1$
        }
        
        return ""; //$NON-NLS-1$
    }

    public int getValue() {
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
        return mValue != DEFAULT_DENSITY;
    }

    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        PixelDensityQualifier qualifier = getQualifier(value);
        if (qualifier != null) {
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
        return mValue;
    }
    
    /**
     * Returns the string used to represent this qualifier in the folder name.
     */
    @Override
    public String toString() {
        return getFolderSegment(mValue);
    }

    @Override
    public String getStringValue() {
        if (mValue != DEFAULT_DENSITY) {
            return String.format("%1$d dpi", mValue);
        }
        
        return ""; //$NON-NLS-1$
    }
}
