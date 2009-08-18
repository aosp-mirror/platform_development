/*
 * Copyright (C) 2007 The Android Open Source Project
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

/**
 * Constant definition class.<br>
 * <br>
 * Most constants have a prefix defining the content.
 * <ul>
 * <li><code>OS_</code> OS path constant. These paths are different depending on the platform.</li>
 * <li><code>FN_</code> File name constant.</li>
 * <li><code>FD_</code> Folder name constant.</li>
 * </ul>
 *
 */
public final class SdkConstants {
    public final static int PLATFORM_UNKNOWN = 0;
    public final static int PLATFORM_LINUX = 1;
    public final static int PLATFORM_WINDOWS = 2;
    public final static int PLATFORM_DARWIN = 3;

    /**
     * Returns current platform, one of {@link #PLATFORM_WINDOWS}, {@link #PLATFORM_DARWIN},
     * {@link #PLATFORM_LINUX} or {@link #PLATFORM_UNKNOWN}.
     */
    public final static int CURRENT_PLATFORM = currentPlatform();

    /**
     * Charset for the ini file handled by the SDK.
     */
    public final static String INI_CHARSET = "UTF-8";

    /** An SDK Project's AndroidManifest.xml file */
    public static final String FN_ANDROID_MANIFEST_XML= "AndroidManifest.xml";
    /** An SDK Project's build.xml file */
    public final static String FN_BUILD_XML = "build.xml";

    /** Name of the framework library, i.e. "android.jar" */
    public static final String FN_FRAMEWORK_LIBRARY = "android.jar";
    /** Name of the layout attributes, i.e. "attrs.xml" */
    public static final String FN_ATTRS_XML = "attrs.xml";
    /** Name of the layout attributes, i.e. "attrs_manifest.xml" */
    public static final String FN_ATTRS_MANIFEST_XML = "attrs_manifest.xml";
    /** framework aidl import file */
    public static final String FN_FRAMEWORK_AIDL = "framework.aidl";
    /** layoutlib.jar file */
    public static final String FN_LAYOUTLIB_JAR = "layoutlib.jar";
    /** widget list file */
    public static final String FN_WIDGETS = "widgets.txt";
    /** Intent activity actions list file */
    public static final String FN_INTENT_ACTIONS_ACTIVITY = "activity_actions.txt";
    /** Intent broadcast actions list file */
    public static final String FN_INTENT_ACTIONS_BROADCAST = "broadcast_actions.txt";
    /** Intent service actions list file */
    public static final String FN_INTENT_ACTIONS_SERVICE = "service_actions.txt";
    /** Intent category list file */
    public static final String FN_INTENT_CATEGORIES = "categories.txt";

    /** platform build property file */
    public final static String FN_BUILD_PROP = "build.prop";
    /** plugin properties file */
    public final static String FN_PLUGIN_PROP = "plugin.prop";
    /** add-on manifest file */
    public final static String FN_MANIFEST_INI = "manifest.ini";
    /** hardware properties definition file */
    public final static String FN_HARDWARE_INI = "hardware-properties.ini";

    /** Skin layout file */
    public final static String FN_SKIN_LAYOUT = "layout";//$NON-NLS-1$

    /** dex.jar file */
    public static final String FN_DX_JAR = "dx.jar"; //$NON-NLS-1$

    /** dx executable (with extension for the current OS)  */
    public final static String FN_DX = (CURRENT_PLATFORM == PLATFORM_WINDOWS) ?
            "dx.bat" : "dx"; //$NON-NLS-1$ //$NON-NLS-2$

    /** aapt executable (with extension for the current OS)  */
    public final static String FN_AAPT = (CURRENT_PLATFORM == PLATFORM_WINDOWS) ?
            "aapt.exe" : "aapt"; //$NON-NLS-1$ //$NON-NLS-2$

    /** aidl executable (with extension for the current OS)  */
    public final static String FN_AIDL = (CURRENT_PLATFORM == PLATFORM_WINDOWS) ?
            "aidl.exe" : "aidl"; //$NON-NLS-1$ //$NON-NLS-2$

    /** adb executable (with extension for the current OS)  */
    public final static String FN_ADB = (CURRENT_PLATFORM == PLATFORM_WINDOWS) ?
            "adb.exe" : "adb"; //$NON-NLS-1$ //$NON-NLS-2$

    /** emulator executable (with extension for the current OS) */
    public final static String FN_EMULATOR = (CURRENT_PLATFORM == PLATFORM_WINDOWS) ?
            "emulator.exe" : "emulator"; //$NON-NLS-1$ //$NON-NLS-2$

    /** zipalign executable (with extension for the current OS)  */
    public final static String FN_ZIPALIGN = (CURRENT_PLATFORM == PLATFORM_WINDOWS) ?
            "zipalign.exe" : "zipalign"; //$NON-NLS-1$ //$NON-NLS-2$

    /* Folder Names for Android Projects . */

    /** Resources folder name, i.e. "res". */
    public final static String FD_RESOURCES = "res"; //$NON-NLS-1$
    /** Assets folder name, i.e. "assets" */
    public final static String FD_ASSETS = "assets"; //$NON-NLS-1$
    /** Default source folder name, i.e. "src" */
    public final static String FD_SOURCES = "src"; //$NON-NLS-1$
    /** Default generated source folder name, i.e. "gen" */
    public final static String FD_GEN_SOURCES = "gen"; //$NON-NLS-1$
    /** Default native library folder name inside the project, i.e. "libs"
     * While the folder inside the .apk is "lib", we call that one libs because
     * that's what we use in ant for both .jar and .so and we need to make the 2 development ways
     * compatible. */
    public final static String FD_NATIVE_LIBS = "libs"; //$NON-NLS-1$
    /** Native lib folder inside the APK: "lib" */
    public final static String FD_APK_NATIVE_LIBS = "lib"; //$NON-NLS-1$
    /** Default output folder name, i.e. "bin" */
    public final static String FD_OUTPUT = "bin"; //$NON-NLS-1$
    /** Default anim resource folder name, i.e. "anim" */
    public final static String FD_ANIM = "anim"; //$NON-NLS-1$
    /** Default color resource folder name, i.e. "color" */
    public final static String FD_COLOR = "color"; //$NON-NLS-1$
    /** Default drawable resource folder name, i.e. "drawable" */
    public final static String FD_DRAWABLE = "drawable"; //$NON-NLS-1$
    /** Default layout resource folder name, i.e. "layout" */
    public final static String FD_LAYOUT = "layout"; //$NON-NLS-1$
    /** Default menu resource folder name, i.e. "menu" */
    public final static String FD_MENU = "menu"; //$NON-NLS-1$
    /** Default values resource folder name, i.e. "values" */
    public final static String FD_VALUES = "values"; //$NON-NLS-1$
    /** Default xml resource folder name, i.e. "xml" */
    public final static String FD_XML = "xml"; //$NON-NLS-1$
    /** Default raw resource folder name, i.e. "raw" */
    public final static String FD_RAW = "raw"; //$NON-NLS-1$

    /* Folder Names for the Android SDK */

    /** Name of the SDK platforms folder. */
    public final static String FD_PLATFORMS = "platforms";
    /** Name of the SDK addons folder. */
    public final static String FD_ADDONS = "add-ons";
    /** Name of the SDK tools folder. */
    public final static String FD_TOOLS = "tools";
    /** Name of the SDK tools/lib folder. */
    public final static String FD_LIB = "lib";
    /** Name of the SDK docs folder. */
    public final static String FD_DOCS = "docs";
    /** Name of the doc folder containing API reference doc (javadoc) */
    public static final String FD_DOCS_REFERENCE = "reference";
    /** Name of the SDK images folder. */
    public final static String FD_IMAGES = "images";
    /** Name of the SDK skins folder. */
    public final static String FD_SKINS = "skins";
    /** Name of the SDK samples folder. */
    public final static String FD_SAMPLES = "samples";
    /** Name of the SDK templates folder, i.e. "templates" */
    public final static String FD_TEMPLATES = "templates";
    /** Name of the SDK data folder, i.e. "data" */
    public final static String FD_DATA = "data";
    /** Name of the SDK resources folder, i.e. "res" */
    public final static String FD_RES = "res";
    /** Name of the SDK font folder, i.e. "fonts" */
    public final static String FD_FONTS = "fonts";
    /** Name of the android sources directory */
    public static final String FD_ANDROID_SOURCES = "sources";
    /** Name of the addon libs folder. */
    public final static String FD_ADDON_LIBS = "libs";

    /** Namespace for the resource XML, i.e. "http://schemas.android.com/apk/res/android" */
    public final static String NS_RESOURCES = "http://schemas.android.com/apk/res/android";

    /** The name of the uses-library that provides "android.test.runner" */
    public final static String ANDROID_TEST_RUNNER_LIB = "android.test.runner";

    /* Folder path relative to the SDK root */
    /** Path of the documentation directory relative to the sdk folder.
     *  This is an OS path, ending with a separator. */
    public final static String OS_SDK_DOCS_FOLDER = FD_DOCS + File.separator;

    /** Path of the tools directory relative to the sdk folder, or to a platform folder.
     *  This is an OS path, ending with a separator. */
    public final static String OS_SDK_TOOLS_FOLDER = FD_TOOLS + File.separator;

    /** Path of the lib directory relative to the sdk folder, or to a platform folder.
     *  This is an OS path, ending with a separator. */
    public final static String OS_SDK_TOOLS_LIB_FOLDER =
            OS_SDK_TOOLS_FOLDER + FD_LIB + File.separator;

    /* Folder paths relative to a platform or add-on folder */

    /** Path of the images directory relative to a platform or addon folder.
     *  This is an OS path, ending with a separator. */
    public final static String OS_IMAGES_FOLDER = FD_IMAGES + File.separator;

    /** Path of the skin directory relative to a platform or addon folder.
     *  This is an OS path, ending with a separator. */
    public final static String OS_SKINS_FOLDER = FD_SKINS + File.separator;

    /* Folder paths relative to a Platform folder */

    /** Path of the data directory relative to a platform folder.
     *  This is an OS path, ending with a separator. */
    public final static String OS_PLATFORM_DATA_FOLDER = FD_DATA + File.separator;

    /** Path of the samples directory relative to a platform folder.
     *  This is an OS path, ending with a separator. */
    public final static String OS_PLATFORM_SAMPLES_FOLDER = FD_SAMPLES + File.separator;

    /** Path of the resources directory relative to a platform folder.
     *  This is an OS path, ending with a separator. */
    public final static String OS_PLATFORM_RESOURCES_FOLDER =
            OS_PLATFORM_DATA_FOLDER + FD_RES + File.separator;

    /** Path of the fonts directory relative to a platform folder.
     *  This is an OS path, ending with a separator. */
    public final static String OS_PLATFORM_FONTS_FOLDER =
            OS_PLATFORM_DATA_FOLDER + FD_FONTS + File.separator;

    /** Path of the android source directory relative to a platform folder.
     *  This is an OS path, ending with a separator. */
    public final static String OS_PLATFORM_SOURCES_FOLDER = FD_ANDROID_SOURCES + File.separator;

    /** Path of the android templates directory relative to a platform folder.
     *  This is an OS path, ending with a separator. */
    public final static String OS_PLATFORM_TEMPLATES_FOLDER = FD_TEMPLATES + File.separator;

    /** Path of the attrs.xml file relative to a platform folder. */
    public final static String OS_PLATFORM_ATTRS_XML =
            OS_PLATFORM_RESOURCES_FOLDER + FD_VALUES + File.separator + FN_ATTRS_XML;

    /** Path of the attrs_manifest.xml file relative to a platform folder. */
    public final static String OS_PLATFORM_ATTRS_MANIFEST_XML =
            OS_PLATFORM_RESOURCES_FOLDER + FD_VALUES + File.separator + FN_ATTRS_MANIFEST_XML;

    /** Path of the layoutlib.jar file relative to a platform folder. */
    public final static String OS_PLATFORM_LAYOUTLIB_JAR =
            OS_PLATFORM_DATA_FOLDER + FN_LAYOUTLIB_JAR;

    /* Folder paths relative to a addon folder */

    /** Path of the images directory relative to a folder folder.
     *  This is an OS path, ending with a separator. */
    public final static String OS_ADDON_LIBS_FOLDER = FD_ADDON_LIBS + File.separator;


    /** Skin default **/
    public final static String SKIN_DEFAULT = "default";

    /** Returns the appropriate name for the 'android' command, which is 'android.bat' for
     * Windows and 'android' for all other platforms. */
    public static String androidCmdName() {
        String os = System.getProperty("os.name");
        String cmd = "android";
        if (os.startsWith("Windows")) {
            cmd += ".bat";
        }
        return cmd;
    }

    /** Returns the appropriate name for the 'mksdcard' command, which is 'mksdcard.exe' for
     * Windows and 'mkdsdcard' for all other platforms. */
    public static String mkSdCardCmdName() {
        String os = System.getProperty("os.name");
        String cmd = "mksdcard";
        if (os.startsWith("Windows")) {
            cmd += ".exe";
        }
        return cmd;
    }

    /**
     * Returns current platform
     *
     * @return one of {@link #PLATFORM_WINDOWS}, {@link #PLATFORM_DARWIN},
     * {@link #PLATFORM_LINUX} or {@link #PLATFORM_UNKNOWN}.
     */
    public static int currentPlatform() {
        String os = System.getProperty("os.name");          //$NON-NLS-1$
        if (os.startsWith("Mac OS")) {                      //$NON-NLS-1$
            return PLATFORM_DARWIN;
        } else if (os.startsWith("Windows")) {              //$NON-NLS-1$
            return PLATFORM_WINDOWS;
        } else if (os.startsWith("Linux")) {                //$NON-NLS-1$
            return PLATFORM_LINUX;
        }

        return PLATFORM_UNKNOWN;
    }
}
