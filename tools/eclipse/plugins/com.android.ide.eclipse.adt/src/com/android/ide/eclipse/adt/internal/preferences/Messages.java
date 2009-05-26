
package com.android.ide.eclipse.adt.internal.preferences;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "com.android.ide.eclipse.adt.internal.preferences.messages"; //$NON-NLS-1$

    public static String AndroidPreferencePage_ERROR_Reserved_Char;

    public static String AndroidPreferencePage_SDK_Location_;

    public static String AndroidPreferencePage_Title;

    public static String BuildPreferencePage_Auto_Refresh_Resources_on_Build;

    public static String BuildPreferencePage_Build_Output;

    public static String BuildPreferencePage_Custom_Keystore;

    public static String BuildPreferencePage_Default_KeyStore;

    public static String BuildPreferencePage_Normal;

    public static String BuildPreferencePage_Silent;

    public static String BuildPreferencePage_Title;

    public static String BuildPreferencePage_Verbose;

    public static String LaunchPreferencePage_Default_Emu_Options;

    public static String LaunchPreferencePage_Default_HOME_Package;

    public static String LaunchPreferencePage_Title;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
