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

    /**
     * Reads the Apk Configurations from a {@link ProjectProperties} file and returns them as a map.
     * <p/>If there are no defined configurations, the returned map will be empty.
     */
    public static Map<String, String> getConfigs(ProjectProperties properties) {
        HashMap<String, String> configMap = new HashMap<String, String>();

        // get the list of configs.
        String configList = properties.getProperty(ProjectProperties.PROPERTY_CONFIGS);
        if (configList != null) {
            // this is a comma separated list
            String[] configs = configList.split(","); //$NON-NLS-1$
            
            // read the value of each config and store it in a map
            
            for (String config : configs) {
                String configValue = properties.getProperty(config);
                if (configValue != null) {
                    configMap.put(config, configValue);
                }
            }
        }
        
        return configMap;
    }
    
    /**
     * Writes the Apk Configurations from a given map into a {@link ProjectProperties}.
     * @return true if the {@link ProjectProperties} contained Apk Configuration that were not
     * present in the map. 
     */
    public static boolean setConfigs(ProjectProperties properties, Map<String, String> configMap) {
        // load the current configs, in order to remove the value properties for each of them
        // in case a config was removed.
        
        // get the list of configs.
        String configList = properties.getProperty(ProjectProperties.PROPERTY_CONFIGS);
        
        // this is a comma separated list
        String[] configs = configList.split(","); //$NON-NLS-1$
        
        boolean hasRemovedConfig = false;
        
        for (String config : configs) {
            if (configMap.containsKey(config) == false) {
                hasRemovedConfig = true;
                properties.removeProperty(config);
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
            properties.setProperty(entry.getKey(), entry.getValue());
        }
        properties.setProperty(ProjectProperties.PROPERTY_CONFIGS, sb.toString());
        
        return hasRemovedConfig;
    }
}
