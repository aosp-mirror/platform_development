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

package com.android.sdklib.project;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Helper class to read and write Apk Configuration into a {@link ProjectProperties} file.
 */
public class ApkConfigurationHelper {
    /** Prefix for property names for config definition. This prevents having config named
     * after other valid properties such as "target". */
    final static String CONFIG_PREFIX = "apk-config-";

    /**
     * Reads the Apk Configurations from a {@link ProjectProperties} file and returns them as a map.
     * <p/>If there are no defined configurations, the returned map will be empty.
     * @return a map of apk configurations. The map contains (name, filter) where name is
     * the name of the configuration (a-zA-Z0-9 only), and filter is the comma separated list of
     * resource configuration to include in the apk (see aapt -c) 
     */
    public static Map<String, String> getConfigs(ProjectProperties properties) {
        HashMap<String, String> configMap = new HashMap<String, String>();

        // get the list of configs.
        String configList = properties.getProperty(ProjectProperties.PROPERTY_APK_CONFIGS);
        if (configList != null) {
            // this is a comma separated list
            String[] configs = configList.split(","); //$NON-NLS-1$
            
            // read the value of each config and store it in a map
            for (String config : configs) {
                config = config.trim();
                String configValue = properties.getProperty(CONFIG_PREFIX + config);
                if (configValue != null) {
                    configMap.put(config, configValue);
                }
            }
        }
        
        return configMap;
    }
    
    /**
     * Writes the Apk Configurations from a given map into a {@link ProjectProperties}.
     * @param properties the {@link ProjectProperties} in which to store the apk configurations. 
     * @param configMap a map of apk configurations. The map contains (name, filter) where name is
     * the name of the configuration (a-zA-Z0-9 only), and filter is the comma separated list of
     * resource configuration to include in the apk (see aapt -c) 
     * @return true if the {@link ProjectProperties} contained Apk Configuration that were not
     * present in the map. 
     */
    public static boolean setConfigs(ProjectProperties properties, Map<String, String> configMap) {
        // load the current configs, in order to remove the value properties for each of them
        // in case a config was removed.
        
        // get the list of configs.
        String configList = properties.getProperty(ProjectProperties.PROPERTY_APK_CONFIGS);

        boolean hasRemovedConfig = false;

        if (configList != null) {
            // this is a comma separated list
            String[] configs = configList.split(","); //$NON-NLS-1$
            
            for (String config : configs) {
                config = config.trim();
                if (configMap.containsKey(config) == false) {
                    hasRemovedConfig = true;
                    properties.removeProperty(CONFIG_PREFIX + config);
                }
            }
        }
        
        // now add the properties.
        Set<Entry<String, String>> entrySet = configMap.entrySet();
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> entry : entrySet) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(entry.getKey());
            properties.setProperty(CONFIG_PREFIX + entry.getKey(), entry.getValue());
        }
        properties.setProperty(ProjectProperties.PROPERTY_APK_CONFIGS, sb.toString());
        
        return hasRemovedConfig;
    }
}
