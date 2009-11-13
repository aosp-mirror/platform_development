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

package com.android.sdklib.internal.project;


/**
 * Helper class to read and write Apk Configuration into a {@link ProjectProperties} file.
 */
public class ApkConfigurationHelper {
    /**
     * Reads the project settings from a {@link ProjectProperties} file and returns them as a
     * {@link ApkSettings} object.
     */
    public static ApkSettings getSettings(ProjectProperties properties) {
        ApkSettings apkSettings = new ApkSettings();

        boolean splitByDensity = Boolean.parseBoolean(properties.getProperty(
                ProjectProperties.PROPERTY_SPLIT_BY_DENSITY));
        apkSettings.setSplitByDensity(splitByDensity);


        return apkSettings;
    }

    /**
     * Sets the content of a {@link ApkSettings} into a {@link ProjectProperties}.
     * @param properties the {@link ProjectProperties} in which to store the settings.
     * @param settings the project settings to store.
     */
    public static void setProperties(ProjectProperties properties, ApkSettings settings) {
        properties.setProperty(ProjectProperties.PROPERTY_SPLIT_BY_DENSITY,
                Boolean.toString(settings.isSplitByDpi()));
    }
}
