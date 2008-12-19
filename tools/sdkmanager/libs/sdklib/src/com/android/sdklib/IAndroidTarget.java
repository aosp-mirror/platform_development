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
 * A version of Android that application can target when building. 
 */
public interface IAndroidTarget extends Comparable<IAndroidTarget> {
    
    public static int ANDROID_JAR         = 1;
    public static int ANDROID_AIDL        = 2;
    public static int IMAGES              = 3;
    public static int SAMPLES             = 4;
    public static int SKINS               = 5;
    public static int TEMPLATES           = 6;
    public static int DATA                = 7;
    public static int ATTRIBUTES          = 8;
    public static int MANIFEST_ATTRIBUTES = 9;
    public static int LAYOUT_LIB          = 10;
    public static int RESOURCES           = 11;
    public static int FONTS               = 12;
    public static int WIDGETS             = 13;
    public static int ACTIONS_ACTIVITY    = 14;
    public static int ACTIONS_BROADCAST   = 15;
    public static int ACTIONS_SERVICE     = 16;
    public static int CATEGORIES          = 17;
    public static int SOURCES             = 18;
    
    public interface IOptionalLibrary {
        String getName();
        String getJarName();
        String getJarPath();
    }

    /**
     * Returns the name of the vendor of the target.
     */
    String getVendor();

    /**
     * Returns the name of the target.
     */
    String getName();
    
    /**
     * Returns the description of the target.
     */
    String getDescription();
    
    /**
     * Returns the api version as an integer.
     */
    int getApiVersionNumber();

    /**
     * Returns the platform version as a readable string.
     */
    String getApiVersionName();
    
    /**
     * Returns true if the target is a standard Android platform.
     */
    boolean isPlatform();
    
    /**
     * Returns the path of a platform component.
     * @param pathId the id representing the path to return. Any of the constants defined in the
     * {@link ITargetDataProvider} interface can be used.
     */
    String getPath(int pathId);
    
    /**
     * Returns the available skins for this target.
     */
    String[] getSkins();
    
    /**
     * Returns the available optional libraries for this target.
     * @return an array of optional libraries or <code>null</code> if there is none.
     */
    IOptionalLibrary[] getOptionalLibraries();
    
    /**
     * Returns a string able to uniquely identify a target.
     * Typically the target will encode information such as api level, whether it's a platform
     * or add-on, and if it's an add-on vendor and add-on name.
     */
    String hashString();
}
