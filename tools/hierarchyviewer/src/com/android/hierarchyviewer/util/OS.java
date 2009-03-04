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

package com.android.hierarchyviewer.util;

public class OS {
    private static boolean macOs;
    private static boolean leopard;
    private static boolean linux;
    private static boolean windows;

    static {
        String osName = System.getProperty("os.name");
        macOs = "Mac OS X".startsWith(osName);
        linux = "Linux".startsWith(osName);
        windows = "Windows".startsWith(osName);

        String version = System.getProperty("os.version");
        final String[] parts = version.split("\\.");
        leopard = Integer.parseInt(parts[0]) >= 10 && Integer.parseInt(parts[1]) >= 5;
    }

    public static boolean isMacOsX() {
        return macOs;
    }

    public static boolean isLeopardOrLater() {
        return leopard;
    }

    public static boolean isLinux() {
        return linux;
    }

    public static boolean isWindows() {
        return windows;
    }
}
