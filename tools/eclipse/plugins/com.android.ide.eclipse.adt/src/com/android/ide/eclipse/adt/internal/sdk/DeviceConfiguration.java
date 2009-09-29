/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.ide.eclipse.adt.internal.sdk;

import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DeviceConfiguration {

    private final String mName;

    private Map<String, FolderConfiguration> mMap =
        new HashMap<String, FolderConfiguration>();
    private float mXDpi = Float.NaN;
    private float mYDpi = Float.NaN;

    DeviceConfiguration(String name) {
        mName = name;
    }

    void addConfig(String name, FolderConfiguration config) {
        mMap.put(name, config);
    }

    void seal() {
        mMap = Collections.unmodifiableMap(mMap);
    }

    void setXDpi(float xdpi) {
        mXDpi = xdpi;
    }

    void setYDpi(float ydpi) {
        mYDpi = ydpi;
    }

    public String getName() {
        return mName;
    }

    public Map<String, FolderConfiguration> getConfigs() {
        return mMap;
    }

    /**
     * Returns the dpi of the Device screen in X.
     * @return the dpi of screen or {@link Float#NaN} if it's not set.
     */
    public float getXDpi() {
        return mXDpi;
    }

    /**
     * Returns the dpi of the Device screen in Y.
     * @return the dpi of screen or {@link Float#NaN} if it's not set.
     */
    public float getYDpi() {
        return mYDpi;
    }
 }
