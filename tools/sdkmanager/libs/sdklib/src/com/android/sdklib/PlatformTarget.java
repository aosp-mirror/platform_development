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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a platform target in the SDK.
 */
final class PlatformTarget implements IAndroidTarget {
    /** String used to get a hash to the platform target */
    private final static String PLATFORM_HASH = "android-%s";

    private final static String PLATFORM_VENDOR = "Android Open Source Project";

    private final static String PLATFORM_NAME = "Android %s";
    private final static String PLATFORM_NAME_PREVIEW = "Android %s (Preview)";

    private final String mLocation;
    private final String mName;
    private final AndroidVersion mVersion;
    private final String mVersionName;
    private final int mRevision;
    private final Map<String, String> mProperties;
    private final Map<Integer, String> mPaths = new HashMap<Integer, String>();
    private String[] mSkins;


    PlatformTarget(String location, Map<String, String> properties,
            int apiLevel, String codeName, String versionName, int revision) {
        if (location.endsWith(File.separator) == false) {
            location = location + File.separator;
        }
        mLocation = location;
        mProperties = Collections.unmodifiableMap(properties);
        mVersion = new AndroidVersion(apiLevel, codeName);
        mVersionName = versionName;
        mRevision = revision;

        if (mVersion.isPreview()) {
            mName =  String.format(PLATFORM_NAME_PREVIEW, mVersionName);
        } else {
            mName = String.format(PLATFORM_NAME, mVersionName);
        }

        // pre-build the path to the platform components
        mPaths.put(ANDROID_JAR, mLocation + SdkConstants.FN_FRAMEWORK_LIBRARY);
        mPaths.put(SOURCES, mLocation + SdkConstants.FD_ANDROID_SOURCES);
        mPaths.put(ANDROID_AIDL, mLocation + SdkConstants.FN_FRAMEWORK_AIDL);
        mPaths.put(IMAGES, mLocation + SdkConstants.OS_IMAGES_FOLDER);
        mPaths.put(SAMPLES, mLocation + SdkConstants.OS_PLATFORM_SAMPLES_FOLDER);
        mPaths.put(SKINS, mLocation + SdkConstants.OS_SKINS_FOLDER);
        mPaths.put(TEMPLATES, mLocation + SdkConstants.OS_PLATFORM_TEMPLATES_FOLDER);
        mPaths.put(DATA, mLocation + SdkConstants.OS_PLATFORM_DATA_FOLDER);
        mPaths.put(ATTRIBUTES, mLocation + SdkConstants.OS_PLATFORM_ATTRS_XML);
        mPaths.put(MANIFEST_ATTRIBUTES, mLocation + SdkConstants.OS_PLATFORM_ATTRS_MANIFEST_XML);
        mPaths.put(RESOURCES, mLocation + SdkConstants.OS_PLATFORM_RESOURCES_FOLDER);
        mPaths.put(FONTS, mLocation + SdkConstants.OS_PLATFORM_FONTS_FOLDER);
        mPaths.put(LAYOUT_LIB, mLocation + SdkConstants.OS_PLATFORM_DATA_FOLDER +
                SdkConstants.FN_LAYOUTLIB_JAR);
        mPaths.put(WIDGETS, mLocation + SdkConstants.OS_PLATFORM_DATA_FOLDER +
                SdkConstants.FN_WIDGETS);
        mPaths.put(ACTIONS_ACTIVITY, mLocation + SdkConstants.OS_PLATFORM_DATA_FOLDER +
                SdkConstants.FN_INTENT_ACTIONS_ACTIVITY);
        mPaths.put(ACTIONS_BROADCAST, mLocation + SdkConstants.OS_PLATFORM_DATA_FOLDER +
                SdkConstants.FN_INTENT_ACTIONS_BROADCAST);
        mPaths.put(ACTIONS_SERVICE, mLocation + SdkConstants.OS_PLATFORM_DATA_FOLDER +
                SdkConstants.FN_INTENT_ACTIONS_SERVICE);
        mPaths.put(CATEGORIES, mLocation + SdkConstants.OS_PLATFORM_DATA_FOLDER +
                SdkConstants.FN_INTENT_CATEGORIES);
        mPaths.put(AAPT, mLocation + SdkConstants.OS_SDK_TOOLS_FOLDER + SdkConstants.FN_AAPT);
        mPaths.put(AIDL, mLocation + SdkConstants.OS_SDK_TOOLS_FOLDER + SdkConstants.FN_AIDL);
        mPaths.put(DX, mLocation + SdkConstants.OS_SDK_TOOLS_FOLDER + SdkConstants.FN_DX);
        mPaths.put(DX_JAR, mLocation + SdkConstants.OS_SDK_TOOLS_LIB_FOLDER +
                SdkConstants.FN_DX_JAR);
    }

    public String getLocation() {
        return mLocation;
    }

    /*
     * (non-Javadoc)
     *
     * For Platform, the vendor name is always "Android".
     *
     * @see com.android.sdklib.IAndroidTarget#getVendor()
     */
    public String getVendor() {
        return PLATFORM_VENDOR;
    }

    public String getName() {
        return mName;
    }

    public String getFullName() {
        return mName;
    }

    public String getClasspathName() {
        return mName;
    }

    /*
     * (non-Javadoc)
     *
     * Description for the Android platform is dynamically generated.
     *
     * @see com.android.sdklib.IAndroidTarget#getDescription()
     */
    public String getDescription() {
        return String.format("Standard Android platform %s", mVersionName);
    }

    public AndroidVersion getVersion() {
        return mVersion;
    }

    public String getVersionName() {
        return mVersionName;
    }

    public int getRevision() {
        return mRevision;
    }

    public boolean isPlatform() {
        return true;
    }

    public IAndroidTarget getParent() {
        return null;
    }

    public String getPath(int pathId) {
        return mPaths.get(pathId);
    }

    public String[] getSkins() {
        return mSkins;
    }

    public String getDefaultSkin() {
        // at this time, this is the default skin for all the platform.
        return "HVGA";
    }

    /**
     * Always returns null, as a standard platform ha no optional libraries.
     *
     * {@inheritDoc}
     * @see com.android.sdklib.IAndroidTarget#getOptionalLibraries()
     */
    public IOptionalLibrary[] getOptionalLibraries() {
        return null;
    }

    /**
     * Currently always return a fixed list with "android.test.runner" in it.
     * <p/>
     * TODO change the fixed library list to be build-dependent later.
     * {@inheritDoc}
     */
    public String[] getPlatformLibraries() {
        return new String[] { SdkConstants.ANDROID_TEST_RUNNER_LIB };
    }

    /**
     * The platform has no USB Vendor Id: always return {@link IAndroidTarget#NO_USB_ID}.
     * {@inheritDoc}
     */
    public int getUsbVendorId() {
        return NO_USB_ID;
    }

    public boolean isCompatibleBaseFor(IAndroidTarget target) {
        // basic test
        if (target == this) {
            return true;
        }

        // if the platform has a codename (ie it's a preview of an upcoming platform), then
        // both platforms must be exactly identical.
        if (mVersion.getCodename() != null) {
            return mVersion.equals(target.getVersion());
        }

        // target is compatible wit the receiver as long as its api version number is greater or
        // equal.
        return target.getVersion().getApiLevel() >= mVersion.getApiLevel();
    }

    public String hashString() {
        return String.format(PLATFORM_HASH, mVersion.getApiString());
    }

    @Override
    public int hashCode() {
        return hashString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PlatformTarget) {
            PlatformTarget platform = (PlatformTarget)obj;

            return mVersion.equals(platform.getVersion());
        }

        return false;
    }

    /*
     * Always return -1 if the object we compare to is an addon.
     * Otherwise, compare api level.
     * (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(IAndroidTarget target) {
        if (target.isPlatform() == false) {
            return -1;
        }

        int apiDiff = mVersion.getApiLevel() - target.getVersion().getApiLevel();

        if (mVersion.getCodename() != null && apiDiff == 0) {
            if (target.getVersionName() == null) {
                return +1; // preview showed last
            }
            return mVersion.getCodename().compareTo(target.getVersion().getCodename());
        }

        return apiDiff;
    }

    // ---- platform only methods.

    public String getProperty(String name) {
        return mProperties.get(name);
    }

    public Map<String, String> getProperties() {
        return mProperties; // mProperties is unmodifiable.
    }

    void setSkins(String[] skins) {
        mSkins = skins;
    }
}
