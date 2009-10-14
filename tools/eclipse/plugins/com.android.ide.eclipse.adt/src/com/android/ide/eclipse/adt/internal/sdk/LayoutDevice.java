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

/**
 * Class representing a layout device.
 *
 * A Layout device is a collection of {@link FolderConfiguration} that can be used to render Android
 * layout files.
 *
 * It also contains a single xdpi/ydpi that is independent of the {@link FolderConfiguration}.
 *
 * If the device is meant to represent a true device, then most of the FolderConfigurations' content
 * should be identical, with only a few qualifiers (orientation, keyboard state) that would differ.
 * However it is simpler to reuse the FolderConfiguration class (with the non changing qualifiers
 * duplicated in each configuration) as it's what's being used by the rendering library.
 *
 * To create, edit and delete LayoutDevice objects, see {@link LayoutDeviceManager}.
 * The class is not technically immutable but behaves as such outside of its package.
 */
public class LayoutDevice {

    private final String mName;

    /** editable map of the config */
    private Map<String, FolderConfiguration> mEditMap = new HashMap<String, FolderConfiguration>();
    /** unmodifiable map returned by {@link #getConfigs()}. */
    private Map<String, FolderConfiguration> mMap;
    private float mXDpi = Float.NaN;
    private float mYDpi = Float.NaN;

    LayoutDevice(String name) {
        mName = name;
    }

    void addConfig(String name, FolderConfiguration config) {
        mEditMap.put(name, config);
        _seal();
    }

    void addConfigs(Map<String, FolderConfiguration> configs) {
        mEditMap.putAll(configs);
        _seal();
    }

    void removeConfig(String name) {
        mEditMap.remove(name);
        _seal();
    }

    /**
     * Adds config to the LayoutDevice. This is to be used to add plenty of configurations.
     * It must be followed by {@link #_seal()}.
     * @param name the name of the config
     * @param config the config.
     */
    void _addConfig(String name, FolderConfiguration config) {
        mEditMap.put(name, config);
    }

    void _seal() {
        mMap = Collections.unmodifiableMap(mEditMap);
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
