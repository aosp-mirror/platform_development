/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.sdklib;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Represents an add-on target in the SDK.
 * An add-on extends a standard {@link PlatformTarget}.
 */
final class AddOnTarget implements IAndroidTarget {
    /**
     * String to compute hash for add-on targets.
     * Format is vendor:name:apiVersion
     * */
    private final static String ADD_ON_FORMAT = "%s:%s:%d"; //$NON-NLS-1$
    
    private final static class OptionalLibrary implements IOptionalLibrary {
        private final String mJarName;
        private final String mJarPath;
        private final String mName;

        OptionalLibrary(String jarName, String jarPath, String name) {
            mJarName = jarName;
            mJarPath = jarPath;
            mName = name;
        }

        public String getJarName() {
            return mJarName;
        }

        public String getJarPath() {
            return mJarPath;
        }

        public String getName() {
            return mName;
        }
    }
    
    private final String mLocation;
    private final PlatformTarget mBasePlatform;
    private final String mName;
    private final String mVendor;
    private final String mDescription;
    private String[] mSkins;
    private IOptionalLibrary[] mLibraries;

    /**
     * Creates a new add-on
     * @param location the OS path location of the add-on
     * @param name the name of the add-on
     * @param vendor the vendor name of the add-on
     * @param description the add-on description
     * @param libMap A map containing the optional libraries. The map key is the fully-qualified
     * library name. The value is the .jar filename
     * @param basePlatform the platform the add-on is extending.
     */
    AddOnTarget(String location, String name, String vendor, String description,
            Map<String, String> libMap, PlatformTarget basePlatform) {
        if (location.endsWith(File.separator) == false) {
            location = location + File.separator;
        }

        mLocation = location;
        mName = name;
        mVendor = vendor;
        mDescription = description;
        mBasePlatform = basePlatform;
        
        // handle the optional libraries.
        mLibraries = new IOptionalLibrary[libMap.size()];
        int index = 0;
        for (Entry<String, String> entry : libMap.entrySet()) {
            mLibraries[index++] = new OptionalLibrary(entry.getValue(),
                    mLocation + SdkConstants.OS_ADDON_LIBS_FOLDER + entry.getValue(),
                    entry.getKey());
        }
    }
    
    public String getLocation() {
        return mLocation;
    }
    
    public String getName() {
        return mName;
    }
    
    public String getVendor() {
        return mVendor;
    }
    
    public String getFullName() {
        return String.format("%1$s (%2$s)", mName, mVendor);
    }
    
    public String getDescription() {
        return mDescription;
    }

    public String getApiVersionName() {
        // this is always defined by the base platform
        return mBasePlatform.getApiVersionName();
    }

    public int getApiVersionNumber() {
        // this is always defined by the base platform
        return mBasePlatform.getApiVersionNumber();
    }
    
    public boolean isPlatform() {
        return false;
    }
    
    public String getPath(int pathId) {
        switch (pathId) {
            case IMAGES:
                return mLocation + SdkConstants.OS_IMAGES_FOLDER;
            case SKINS:
                return mLocation + SdkConstants.OS_SKINS_FOLDER;
            default :
                return mBasePlatform.getPath(pathId);
        }
    }

    public String[] getSkins() {
        return mSkins;
    }

    public IOptionalLibrary[] getOptionalLibraries() {
        return mLibraries;
    }
    
    public boolean isCompatibleBaseFor(IAndroidTarget target) {
        // basic test
        if (target == this) {
            return true;
        }

        // if the receiver has no optional library, then anything with api version number >= to
        // the receiver is compatible.
        if (mLibraries.length == 0) {
            return target.getApiVersionNumber() >= getApiVersionNumber();
        }

        // Otherwise, target is only compatible if the vendor and name are equals with the api
        // number greater or equal (ie target is a newer version of this add-on).
        if (target.isPlatform() == false) {
            return (mVendor.equals(target.getVendor()) && mName.equals(target.getName()) &&
                    target.getApiVersionNumber() >= getApiVersionNumber());
        }

        return false;
    }
    
    public String hashString() {
        return String.format(ADD_ON_FORMAT, mVendor, mName, mBasePlatform.getApiVersionNumber());
    }
    
    @Override
    public int hashCode() {
        return hashString().hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AddOnTarget) {
            AddOnTarget addon = (AddOnTarget)obj;
            
            return mVendor.equals(addon.mVendor) && mName.equals(addon.mName) &&
                mBasePlatform.getApiVersionNumber() == addon.mBasePlatform.getApiVersionNumber();
        }

        return super.equals(obj);
    }
    
    /*
     * Always return +1 if the object we compare to is a platform.
     * Otherwise, do vendor then name then api version comparison.
     * (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(IAndroidTarget target) {
        if (target.isPlatform()) {
            return +1;
        }
        
        // vendor
        int value = mVendor.compareTo(target.getVendor());

        // name
        if (value == 0) {
            value = mName.compareTo(target.getName());
        }
        
        // api version
        if (value == 0) {
            value = getApiVersionNumber() - target.getApiVersionNumber();
        }
        
        return value;
    }

    
    // ---- local methods.


    public void setSkins(String[] skins) {
        mSkins = skins;
    }
}
