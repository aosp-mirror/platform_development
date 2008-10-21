
package com.android.ide.eclipse.common;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "com.android.ide.eclipse.common.messages"; //$NON-NLS-1$

    public static String Console_Data_Project_Tag;

    public static String Console_Date_Tag;


    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
