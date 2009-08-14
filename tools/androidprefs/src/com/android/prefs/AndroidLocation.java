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

package com.android.prefs;

import java.io.File;

/**
 * Manages the location of the android files (including emulator files, ddms config, debug keystore)
 */
public final class AndroidLocation {
    /**
     * Virtual Device folder inside the path returned by {@link #getFolder()}
     */
    public static final String FOLDER_AVD = "avd";

    /**
     * Throw when the location of the android folder couldn't be found.
     */
    public static final class AndroidLocationException extends Exception {
        private static final long serialVersionUID = 1L;

        public AndroidLocationException(String string) {
            super(string);
        }
    }

    private static String sPrefsLocation = null;

    /**
     * Returns the folder used to store android related files.
     * @return an OS specific path, terminated by a separator.
     * @throws AndroidLocationException
     */
    public final static String getFolder() throws AndroidLocationException {
        if (sPrefsLocation == null) {
            String home = findValidPath("ANDROID_SDK_HOME", "user.home", "HOME");

            // if the above failed, we throw an exception.
            if (home == null) {
                throw new AndroidLocationException(
                        "Unable to get the home directory. Make sure the user.home property is set up");
            } else {
                sPrefsLocation = home + File.separator + ".android" + File.separator;
            }
        }

        // make sure the folder exists!
        File f = new File(sPrefsLocation);
        if (f.exists() == false) {
            try {
                f.mkdir();
            } catch (SecurityException e) {
                AndroidLocationException e2 = new AndroidLocationException(String.format(
                        "Unable to create folder '%1$s'. " +
                        "This is the path of preference folder expected by the Android tools.",
                        sPrefsLocation));
                e2.initCause(e);
                throw e2;
            }
        } else if (f.isFile()) {
            throw new AndroidLocationException(sPrefsLocation +
                    " is not a directory! " +
                    "This is the path of preference folder expected by the Android tools.");
        }

        return sPrefsLocation;
    }

    /**
     * Checks a list of system properties and/or system environment variables for validity, and
     * existing director, and returns the first one.
     * @param names
     * @return the content of the first property/variable.
     */
    private static String findValidPath(String... names) {
        for (String name : names) {
            String path;
            if (name.indexOf('.') != -1) {
                path = System.getProperty(name);
            } else {
                path = System.getenv(name);
            }

            if (path != null) {
                File f = new File(path);
                if (f.isDirectory()) {
                    return path;
                }
            }
        }

        return null;
    }
}
