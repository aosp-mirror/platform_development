/*
 * Copyright (C) 2008 The Android Open Source Project
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

import com.android.sdkstats.SdkStatsService;

import org.osgi.framework.Version;

/**
 * Helper class to access the ping usage stat server.
 */
public class SdkStatsHelper {

    /**
     * Pings the usage start server.
     * @param pluginName the name of the plugin to appear in the stats
     * @param pluginVersion the {@link Version} of the plugin.
     */
    public static void pingUsageServer(String pluginName, Version pluginVersion) {
        String versionString = String.format("%1$d.%2$d.%3$d", pluginVersion.getMajor(),
                pluginVersion.getMinor(), pluginVersion.getMicro());

        SdkStatsService.ping(pluginName, versionString);
    }
}
