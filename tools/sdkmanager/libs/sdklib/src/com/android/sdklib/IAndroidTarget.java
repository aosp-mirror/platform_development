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



/**
 * A version of Android that applications can target when building.
 */
public interface IAndroidTarget extends Comparable<IAndroidTarget> {

    /** OS Path to the "android.jar" file. */
    public final static int ANDROID_JAR         = 1;
    /** OS Path to the "framework.aidl" file. */
    public final static int ANDROID_AIDL        = 2;
    /** OS Path to "images" folder which contains the emulator system images. */
    public final static int IMAGES              = 3;
    /** OS Path to the "samples" folder which contains sample projects. */
    public final static int SAMPLES             = 4;
    /** OS Path to the "skins" folder which contains the emulator skins. */
    public final static int SKINS               = 5;
    /** OS Path to the "templates" folder which contains the templates for new projects. */
    public final static int TEMPLATES           = 6;
    /** OS Path to the "data" folder which contains data & libraries for the SDK tools. */
    public final static int DATA                = 7;
    /** OS Path to the "attrs.xml" file. */
    public final static int ATTRIBUTES          = 8;
    /** OS Path to the "attrs_manifest.xml" file. */
    public final static int MANIFEST_ATTRIBUTES = 9;
    /** OS Path to the "data/layoutlib.jar" library. */
    public final static int LAYOUT_LIB          = 10;
    /** OS Path to the "data/res" folder. */
    public final static int RESOURCES           = 11;
    /** OS Path to the "data/fonts" folder. */
    public final static int FONTS               = 12;
    /** OS Path to the "data/widgets.txt" file. */
    public final static int WIDGETS             = 13;
    /** OS Path to the "data/activity_actions.txt" file. */
    public final static int ACTIONS_ACTIVITY    = 14;
    /** OS Path to the "data/broadcast_actions.txt" file. */
    public final static int ACTIONS_BROADCAST   = 15;
    /** OS Path to the "data/service_actions.txt" file. */
    public final static int ACTIONS_SERVICE     = 16;
    /** OS Path to the "data/categories.txt" file. */
    public final static int CATEGORIES          = 17;
    /** OS Path to the "sources" folder. */
    public final static int SOURCES             = 18;
    /** OS Path to the target specific docs */
    public final static int DOCS                = 19;
    /** OS Path to the target's version of the aapt tool. */
    public final static int AAPT                = 20;
    /** OS Path to the target's version of the aidl tool. */
    public final static int AIDL                = 21;
    /** OS Path to the target's version of the dx too. */
    public final static int DX                  = 22;
    /** OS Path to the target's version of the dx.jar file. */
    public final static int DX_JAR              = 23;

    /**
     * Return value for {@link #getUsbVendorId()} meaning no USB vendor IDs are defined by the
     * Android target.
     */
    public final static int NO_USB_ID = 0;

    /** An optional library provided by an Android Target */
    public interface IOptionalLibrary {
        /** The name of the library, as used in the manifest (&lt;uses-library&gt;). */
        String getName();
        /** The file name of the jar file. */
        String getJarName();
        /** Absolute OS path to the jar file. */
        String getJarPath();
        /** Description of the library. */
        String getDescription();
    }

    /**
     * Returns the target location.
     */
    String getLocation();

    /**
     * Returns the name of the vendor of the target.
     */
    String getVendor();

    /**
     * Returns the name of the target.
     */
    String getName();

    /**
     * Returns the full name of the target, possibly including vendor name.
     */
    String getFullName();

    /**
     * Returns the name to be displayed when representing all the libraries this target contains.
     */
    String getClasspathName();

    /**
     * Returns the description of the target.
     */
    String getDescription();

    /**
     * Returns the version of the target. This is guaranteed to be non-null.
     */
    AndroidVersion getVersion();

    /**
     * Returns the platform version as a readable string.
     */
    public String getVersionName();

    /** Returns the revision number for the target. */
    int getRevision();

    /**
     * Returns true if the target is a standard Android platform.
     */
    boolean isPlatform();

    /**
     * Returns the parent target. This is likely to only be non <code>null</code> if
     * {@link #isPlatform()} returns <code>false</code>
     */
    IAndroidTarget getParent();

    /**
     * Returns the path of a platform component.
     * @param pathId the id representing the path to return. Any of the constants defined in the
     * {@link IAndroidTarget} interface can be used.
     */
    String getPath(int pathId);

    /**
     * Returns the available skins for this target.
     */
    String[] getSkins();

    /**
     * Returns the default skin for this target.
     */
    String getDefaultSkin();

    /**
     * Returns the available optional libraries for this target.
     * @return an array of optional libraries or <code>null</code> if there is none.
     */
    IOptionalLibrary[] getOptionalLibraries();

    /**
     * Returns the list of libraries available for a given platform.
     *
     * @return an array of libraries provided by the platform or <code>null</code> if there is none.
     */
    String[] getPlatformLibraries();

    /**
     * Returns the USB Vendor ID for the vendor of this target.
     * <p/>If the target defines no USB Vendor ID, then the method return 0.
     */
    int getUsbVendorId();

    /**
     * Returns whether the given target is compatible with the receiver.
     * <p/>A target is considered compatible if applications developed for the receiver can run on
     * the given target.
     *
     * @param target the IAndroidTarget to test.
     */
    boolean isCompatibleBaseFor(IAndroidTarget target);

    /**
     * Returns a string able to uniquely identify a target.
     * Typically the target will encode information such as api level, whether it's a platform
     * or add-on, and if it's an add-on vendor and add-on name.
     */
    String hashString();
}
