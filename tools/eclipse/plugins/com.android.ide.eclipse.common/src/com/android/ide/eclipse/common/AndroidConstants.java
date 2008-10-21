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

package com.android.ide.eclipse.common;

import java.io.File;

/**
 * Constant definition class.<br>
 * <br>
 * Most constants have a prefix defining the content.
 * <ul>
 * <li><code>WS_</code> Workspace path constant. Those are absolute paths,
 * from the project root.</li>
 * <li><code>OS_</code> OS path constant. These paths are different depending on the platform.</li>
 * <li><code>FN_</code> File name constant.</li>
 * <li><code>FD_</code> Folder name constant.</li>
 * <li><code>MARKER_</code> Resource Marker Ids constant.</li>
 * <li><code>EXT_</code> File extension constant. This does NOT include a dot.</li>
 * <li><code>DOT_</code> File extension constant. This start with a dot.</li>
 * <li><code>RE_</code> Regexp constant.</li>
 * <li><code>NS_</code> Namespace constant.</li>
 * <li><code>CLASS_</code> Fully qualified class name.</li>
 * </ul>
 *
 */
public class AndroidConstants {
    /** The Editors Plugin ID */
    public static final String EDITORS_PLUGIN_ID = "com.android.ide.eclipse.editors"; // $NON-NLS-1$

    /** Nature of android projects */
    public final static String NATURE = "com.android.ide.eclipse.adt.AndroidNature"; //$NON-NLS-1$

    public final static int PLATFORM_UNKNOWN = 0;
    public final static int PLATFORM_LINUX = 1;
    public final static int PLATFORM_WINDOWS = 2;
    public final static int PLATFORM_DARWIN = 3;

    /**
     * Returns current platform, one of {@link #PLATFORM_WINDOWS}, {@link #PLATFORM_DARWIN},
     * {@link #PLATFORM_LINUX} or {@link #PLATFORM_UNKNOWN}.
     */
    public final static int CURRENT_PLATFORM = currentPlatform();

    /** Separator for workspace path, i.e. "/". */
    public final static String WS_SEP = "/"; //$NON-NLS-1$
    /** Separator character for workspace path, i.e. '/'. */
    public final static char WS_SEP_CHAR = '/';

    /** Extension of the Application package Files, i.e. "apk". */
    public final static String EXT_ANDROID_PACKAGE = "apk"; //$NON-NLS-1$
    /** Extension of java files, i.e. "java" */
    public final static String EXT_JAVA = "java"; //$NON-NLS-1$
    /** Extension of compiled java files, i.e. "class" */
    public final static String EXT_CLASS = "class"; //$NON-NLS-1$
    /** Extension of xml files, i.e. "xml" */
    public final static String EXT_XML = "xml"; //$NON-NLS-1$
    /** Extension of jar files, i.e. "jar" */
    public final static String EXT_JAR = "jar"; //$NON-NLS-1$
    /** Extension of aidl files, i.e. "aidl" */
    public final static String EXT_AIDL = "aidl"; //$NON-NLS-1$

    private final static String DOT = "."; //$NON-NLS-1$

    /** Dot-Extension of the Application package Files, i.e. ".apk". */
    public final static String DOT_ANDROID_PACKAGE = DOT + EXT_ANDROID_PACKAGE;
    /** Dot-Extension of java files, i.e. ".java" */
    public final static String DOT_JAVA = DOT + EXT_JAVA;
    /** Dot-Extension of compiled java files, i.e. ".class" */
    public final static String DOT_CLASS = DOT + EXT_CLASS;
    /** Dot-Extension of xml files, i.e. ".xml" */
    public final static String DOT_XML = DOT + EXT_XML;
    /** Dot-Extension of jar files, i.e. ".jar" */
    public final static String DOT_JAR = DOT + EXT_JAR;
    /** Dot-Extension of aidl files, i.e. ".aidl" */
    public final static String DOT_AIDL = DOT + EXT_AIDL;

    /** Name of the manifest file, i.e. "AndroidManifest.xml". */
    public static final String FN_ANDROID_MANIFEST = "AndroidManifest.xml"; //$NON-NLS-1$

    /** Name of the framework library, i.e. "android.jar" */
    public static final String FN_FRAMEWORK_LIBRARY = "android.jar"; //$NON-NLS-1$
    /** Name of the layout attributes, i.e. "attrs.xml" */
    public static final String FN_ATTRS_XML = "attrs.xml"; //$NON-NLS-1$
    /** Name of the layout attributes, i.e. "attrs_manifest.xml" */
    public static final String FN_ATTRS_MANIFEST_XML = "attrs_manifest.xml"; //$NON-NLS-1$
    /** framework aidl import file */
    public static final String FN_FRAMEWORK_AIDL = "framework.aidl"; //$NON-NLS-1$
    /** layoutlib.jar file */
    public static final String FN_LAYOUTLIB_JAR = "layoutlib.jar"; //$NON-NLS-1$
    /** dex.jar file */
    public static final String FN_DX_JAR = "dx.jar"; //$NON-NLS-1$
    /** Name of the android sources directory */
    public static final String FD_ANDROID_SOURCES = "sources"; //$NON-NLS-1$

    /** Resource java class  filename, i.e. "R.java" */
    public final static String FN_RESOURCE_CLASS = "R.java"; //$NON-NLS-1$
    /** Resource class file  filename, i.e. "R.class" */
    public final static String FN_COMPILED_RESOURCE_CLASS = "R.class"; //$NON-NLS-1$
    /** Manifest java class filename, i.e. "Manifest.java" */
    public final static String FN_MANIFEST_CLASS = "Manifest.java"; //$NON-NLS-1$
    /** Dex conversion output filname, i.e. "classes.dex" */
    public final static String FN_CLASSES_DEX = "classes.dex"; //$NON-NLS-1$
    /** Temporary packaged resources file name, i.e. "resources.ap_" */
    public final static String FN_RESOURCES_AP_ = "resources.ap_"; //$NON-NLS-1$
    /** build properties file */
    public final static String FN_BUILD_PROP = "build.prop"; //$NON-NLS-1$
    /** plugin properties file */
    public final static String FN_PLUGIN_PROP = "plugin.prop"; //$NON-NLS-1$

    public final static String FN_ADB = (CURRENT_PLATFORM == PLATFORM_WINDOWS) ?
            "adb.exe" : "adb"; //$NON-NLS-1$ //$NON-NLS-2$

    public final static String FN_AAPT = (CURRENT_PLATFORM == PLATFORM_WINDOWS) ?
            "aapt.exe" : "aapt"; //$NON-NLS-1$ //$NON-NLS-2$

    public final static String FN_AIDL = (CURRENT_PLATFORM == PLATFORM_WINDOWS) ?
            "aidl.exe" : "aidl"; //$NON-NLS-1$ //$NON-NLS-2$

    public final static String FN_EMULATOR = (CURRENT_PLATFORM == PLATFORM_WINDOWS) ?
            "emulator.exe" : "emulator"; //$NON-NLS-1$ //$NON-NLS-2$

    public final static String FN_TRACEVIEW = (CURRENT_PLATFORM == PLATFORM_WINDOWS) ?
            "traceview.exe" : "traceview"; //$NON-NLS-1$ //$NON-NLS-2$

    /** Skin layout file */
    public final static String FN_LAYOUT = "layout";//$NON-NLS-1$

    /** Resources folder name, i.e. "res". */
    public final static String FD_RESOURCES = "res"; //$NON-NLS-1$
    /** Assets folder name, i.e. "assets" */
    public final static String FD_ASSETS = "assets"; //$NON-NLS-1$
    /** Default source folder name, i.e. "src" */
    public final static String FD_SOURCES = "src"; //$NON-NLS-1$
    /** Default bin folder name, i.e. "bin" */
    public final static String FD_BINARIES = "bin"; //$NON-NLS-1$
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
    /** Name of the tools folder. */
    public final static String FD_TOOLS = "tools"; //$NON-NLS-1$
    /** Name of the libs folder. */
    public final static String FD_LIBS = "lib"; //$NON-NLS-1$
    /** Name of the docs folder. */
    public final static String FD_DOCS = "docs"; //$NON-NLS-1$
    /** Name of the images folder. */
    public final static String FD_IMAGES = "images"; //$NON-NLS-1$
    /** Name of the skins folder. */
    public final static String FD_SKINS = "skins"; //$NON-NLS-1$
    /** Name of the samples folder. */
    public final static String FD_SAMPLES = "samples"; //$NON-NLS-1$
    /** Name of the folder containing the default framework resources. */
    public final static String FD_DEFAULT_RES = "default"; //$NON-NLS-1$
    /** SDK font folder name, i.e. "fonts" */
    public final static String FD_FONTS = "fonts"; //$NON-NLS-1$

    /** Absolute path of the workspace root, i.e. "/" */
    public final static String WS_ROOT = WS_SEP;

    /** Absolute path of the resource folder, eg "/res".<br> This is a workspace path. */
    public final static String WS_RESOURCES = WS_SEP + FD_RESOURCES;

    /** Absolute path of the resource folder, eg "/assets".<br> This is a workspace path. */
    public final static String WS_ASSETS = WS_SEP + FD_ASSETS;

    /** Leaf of the javaDoc folder. Does not start with a separator. */
    public final static String WS_JAVADOC_FOLDER_LEAF = FD_DOCS + "/reference"; //$NON-NLS-1$

    /** Path of the documentation directory relative to the sdk folder.
     *  This is an OS path, ending with a separator. */
    public final static String OS_SDK_DOCS_FOLDER = FD_DOCS + File.separator;

    /** Path of the tools directory relative to the sdk folder.
     *  This is an OS path, ending with a separator. */
    public final static String OS_SDK_TOOLS_FOLDER = FD_TOOLS + File.separator;

    /** Path of the samples directory relative to the sdk folder.
     *  This is an OS path, ending with a separator. */
    public final static String OS_SDK_SAMPLES_FOLDER = FD_SAMPLES + File.separator;

    /** Path of the lib directory relative to the sdk folder.
     *  This is an OS path, ending with a separator. */
    public final static String OS_SDK_LIBS_FOLDER =
        OS_SDK_TOOLS_FOLDER + FD_LIBS + File.separator;

    /** Path of the resources directory relative to the sdk folder.
     *  This is an OS path, ending with a separator. */
    public final static String OS_SDK_RESOURCES_FOLDER =
        OS_SDK_LIBS_FOLDER + FD_RESOURCES + File.separator;

    /** Path of the resources directory relative to the sdk folder.
     *  This is an OS path, ending with a separator. */
    public final static String OS_SDK_FONTS_FOLDER =
        OS_SDK_LIBS_FOLDER + FD_FONTS + File.separator;

    /** Path of the skin directory relative to the sdk folder.
     *  This is an OS path, ending with a separator. */
    public final static String OS_SDK_SKINS_FOLDER =
        OS_SDK_LIBS_FOLDER + FD_IMAGES + File.separator + FD_SKINS + File.separator;

    /** Path of the attrs.xml file relative to the sdk folder. */
    public final static String OS_SDK_ATTRS_XML =
        OS_SDK_RESOURCES_FOLDER + File.separator + FD_DEFAULT_RES + File.separator +
        FD_VALUES + File.separator + FN_ATTRS_XML;

    /** Path of the attrs_manifest.xml file relative to the sdk folder. */
    public final static String OS_SDK_ATTRS_MANIFEST_XML =
        OS_SDK_RESOURCES_FOLDER + File.separator + FD_DEFAULT_RES + File.separator +
        FD_VALUES + File.separator + FN_ATTRS_MANIFEST_XML;

    /** Path of the layoutlib.jar file relative to the sdk folder. */
    public final static String OS_SDK_LIBS_LAYOUTLIB_JAR =
        OS_SDK_LIBS_FOLDER + FN_LAYOUTLIB_JAR;

    /** Path of the dx.jar file relative to the sdk folder. */
    public final static String OS_SDK_LIBS_DX_JAR =
        OS_SDK_LIBS_FOLDER + FN_DX_JAR;

    /** Regexp for single dot */
    public final static String RE_DOT = "\\."; //$NON-NLS-1$
    /** Regexp for java extension, i.e. "\.java$" */
    public final static String RE_JAVA_EXT = "\\.java$"; //$NON-NLS-1$
    /** Regexp for aidl extension, i.e. "\.aidl$" */
    public final static String RE_AIDL_EXT = "\\.aidl$"; //$NON-NLS-1$

    /** Namespace for the resource XML, i.e. "http://schemas.android.com/apk/res/android" */
    public final static String NS_RESOURCES = "http://schemas.android.com/apk/res/android"; //$NON-NLS-1$

    /** Namespace pattern for the custom resource XML, i.e. "http://schemas.android.com/apk/res/%s" */
    public final static String NS_CUSTOM_RESOURCES = "http://schemas.android.com/apk/res/%1$s"; //$NON-NLS-1$

    /** aapt marker error. */
    public final static String MARKER_AAPT = CommonPlugin.PLUGIN_ID + ".aaptProblem"; //$NON-NLS-1$

    /** XML marker error. */
    public final static String MARKER_XML = CommonPlugin.PLUGIN_ID + ".xmlProblem"; //$NON-NLS-1$

    /** aidl marker error. */
    public final static String MARKER_AIDL = CommonPlugin.PLUGIN_ID + ".aidlProblem"; //$NON-NLS-1$
    
    /** android marker error */
    public final static String MARKER_ANDROID = CommonPlugin.PLUGIN_ID + ".androidProblem"; //$NON-NLS-1$
    
    /** Name for the "type" marker attribute */
    public final static String MARKER_ATTR_TYPE = "android.type"; //$NON-NLS-1$
    /** Name for the "class" marker attribute */
    public final static String MARKER_ATTR_CLASS = "android.class"; //$NON-NLS-1$
    /** activity value for marker attribute "type" */
    public final static String MARKER_ATTR_TYPE_ACTIVITY = "activity"; //$NON-NLS-1$
    /** service value for marker attribute "type" */
    public final static String MARKER_ATTR_TYPE_SERVICE = "service"; //$NON-NLS-1$
    /** receiver value for marker attribute "type" */
    public final static String MARKER_ATTR_TYPE_RECEIVER = "receiver"; //$NON-NLS-1$
    /** provider value for marker attribute "type" */
    public final static String MARKER_ATTR_TYPE_PROVIDER = "provider"; //$NON-NLS-1$

    public final static String CLASS_ACTIVITY = "android.app.Activity"; //$NON-NLS-1$ 
    public final static String CLASS_SERVICE = "android.app.Service"; //$NON-NLS-1$ 
    public final static String CLASS_BROADCASTRECEIVER = "android.content.BroadcastReceiver"; //$NON-NLS-1$ 
    public final static String CLASS_CONTENTPROVIDER = "android.content.ContentProvider"; //$NON-NLS-1$
    public final static String CLASS_INSTRUMENTATION = "android.app.Instrumentation"; //$NON-NLS-1$
    public final static String CLASS_BUNDLE = "android.os.Bundle"; //$NON-NLS-1$
    public final static String CLASS_R = "android.R"; //$NON-NLS-1$
    public final static String CLASS_MANIFEST_PERMISSION = "android.Manifest$permission"; //$NON-NLS-1$
    public final static String CLASS_INTENT = "android.content.Intent"; //$NON-NLS-1$
    public final static String CLASS_CONTEXT = "android.content.Context"; //$NON-NLS-1$
    public final static String CLASS_VIEW = "android.view.View"; //$NON-NLS-1$
    public final static String CLASS_VIEWGROUP = "android.view.ViewGroup"; //$NON-NLS-1$
    public final static String CLASS_LAYOUTPARAMS = "LayoutParams"; //$NON-NLS-1$
    public final static String CLASS_VIEWGROUP_LAYOUTPARAMS =
        CLASS_VIEWGROUP + "$" + CLASS_LAYOUTPARAMS; //$NON-NLS-1$
    public final static String CLASS_FRAMELAYOUT = "FrameLayout"; //$NON-NLS-1$
    public final static String CLASS_PREFERENCE = "android.preference.Preference"; //$NON-NLS-1$
    public final static String CLASS_PREFERENCE_SCREEN = "PreferenceScreen"; //$NON-NLS-1$
    public final static String CLASS_PREFERENCES =
        "android.preference." + CLASS_PREFERENCE_SCREEN; //$NON-NLS-1$
    public final static String CLASS_PREFERENCEGROUP = "android.preference.PreferenceGroup"; //$NON-NLS-1$
    
    public final static String CLASS_BRIDGE = "com.android.layoutlib.bridge.Bridge"; //$NON-NLS-1$

    /**
     * Prefered compiler level, i.e. "1.5".
     */
    public final static String COMPILER_COMPLIANCE_PREFERRED = "1.5"; //$NON-NLS-1$
    /**
     * List of valid compiler level, i.e. "1.5" and "1.6"
     */
    public final static String[] COMPILER_COMPLIANCE = {
        "1.5", //$NON-NLS-1$
        "1.6", //$NON-NLS-1$
    };

    /** The base URL where to find the Android class & manifest documentation */
    public static final String CODESITE_BASE_URL = "http://code.google.com/android";  //$NON-NLS-1$
    
    /**
     * Returns current platform
     * 
     * @return one of {@link #PLATFORM_WINDOWS}, {@link #PLATFORM_DARWIN},
     * {@link #PLATFORM_LINUX} or {@link #PLATFORM_UNKNOWN}.
     */
    private static int currentPlatform() {
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
