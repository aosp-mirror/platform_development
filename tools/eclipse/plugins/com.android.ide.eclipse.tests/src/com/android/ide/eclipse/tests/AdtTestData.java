/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License"); you
 * may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.ide.eclipse.tests;

import com.android.ide.eclipse.adt.AndroidConstants;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Helper class for retrieving test data
 * <p/>
 * All tests which need to retrieve paths to test data files should go through this class.
 */
public class AdtTestData {

    /** singleton instance */
    private static AdtTestData sInstance = null;
    private static final Logger sLogger = Logger.getLogger(AdtTestData.class.getName());

    /** The absolute file path to the plugin's contents. */
    private String mOsRootDataPath;

    private AdtTestData() {
        // can set test_data env variable to override default behavior of
        // finding data using class loader
        // useful when running in plugin environment, where test data is inside
        // bundled jar, and must be extracted to temp filesystem location to be
        // accessed normally
        mOsRootDataPath = System.getProperty("test_data");
        if (mOsRootDataPath == null) {
            sLogger.info("Cannot find test_data environment variable, init to class loader");
            URL url = this.getClass().getClassLoader().getResource(".");  //$NON-NLS-1$

            if (Platform.isRunning()) {
                sLogger.info("Running as an Eclipse Plug-in JUnit test, using FileLocator");
                try {
                    mOsRootDataPath = FileLocator.resolve(url).getFile();
                } catch (IOException e) {
                    sLogger.warning("IOException while using FileLocator, reverting to url");
                    mOsRootDataPath = url.getFile();
                }
            } else {
                sLogger.info("Running as an plain JUnit test, using url as-is");
                mOsRootDataPath = url.getFile();
            }
        }

        if (mOsRootDataPath.equals(AndroidConstants.WS_SEP)) {
            sLogger.warning("Resource data not found using class loader!, Defaulting to no path");
        }

        if (!mOsRootDataPath.endsWith(File.separator)) {
            sLogger.info("Fixing test_data env variable (does not end with path separator)");
            mOsRootDataPath = mOsRootDataPath.concat(File.separator);
        }
    }

    /** Get the singleton instance of AdtTestData */
    public static AdtTestData getInstance() {
        if (sInstance == null) {
            sInstance = new AdtTestData();
        }
        return sInstance;
    }

    /**
     * Returns the absolute file path to a file located in this plugin.
     *
     * @param osRelativePath {@link String} path to file contained in plugin. Must
     * use path separators appropriate to host OS
     *
     * @return absolute OS path to test file
     */
    public String getTestFilePath(String osRelativePath) {
        return mOsRootDataPath + osRelativePath;
    }
}
