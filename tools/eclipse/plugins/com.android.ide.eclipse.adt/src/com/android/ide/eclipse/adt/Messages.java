
package com.android.ide.eclipse.adt;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "com.android.ide.eclipse.adt.messages"; //$NON-NLS-1$

    public static String AdtPlugin_Android_SDK_Content_Loader;

    public static String AdtPlugin_Android_SDK_Resource_Parser;

    public static String AdtPlugin_Failed_To_Parse_s;

    public static String AdtPlugin_Failed_To_Start_s;

    public static String AdtPlugin_Parsing_Resources;

    public static String Could_Not_Find;

    public static String Could_Not_Find_Folder;

    public static String Could_Not_Find_Folder_In_SDK;

    public static String Dialog_Title_SDK_Location;

    public static String Error_Check_Prefs;

    public static String SDK_Not_Setup;

    public static String VersionCheck_Plugin_Too_Old;

    public static String VersionCheck_Plugin_Version_Failed;

    public static String VersionCheck_SDK_Build_Too_Low;

    public static String VersionCheck_SDK_Milestone_Too_Low;

    public static String VersionCheck_Unable_To_Parse_Version_s;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
