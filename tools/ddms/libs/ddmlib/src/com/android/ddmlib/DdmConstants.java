/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.ddmlib;

public final class DdmConstants {

    public final static int PLATFORM_UNKNOWN = 0;
    public final static int PLATFORM_LINUX = 1;
    public final static int PLATFORM_WINDOWS = 2;
    public final static int PLATFORM_DARWIN = 3;

    /**
     * Returns current platform, one of {@link #PLATFORM_WINDOWS}, {@link #PLATFORM_DARWIN},
     * {@link #PLATFORM_LINUX} or {@link #PLATFORM_UNKNOWN}.
     */
    public final static int CURRENT_PLATFORM = currentPlatform();

    /** hprof-conv executable (with extension for the current OS)  */
    public final static String FN_HPROF_CONVERTER = (CURRENT_PLATFORM == PLATFORM_WINDOWS) ?
            "hprof-conv.exe" : "hprof-conv"; //$NON-NLS-1$ //$NON-NLS-2$

    /** traceview executable (with extension for the current OS)  */
    public final static String FN_TRACEVIEW = (CURRENT_PLATFORM == PLATFORM_WINDOWS) ?
            "traceview.bat" : "traceview"; //$NON-NLS-1$ //$NON-NLS-2$

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
