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
import com.android.ide.eclipse.adt.internal.resources.configurations.KeyboardStateQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.NavigationMethodQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.PixelDensityQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenDimensionQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenOrientationQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenRatioQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenSizeQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.TextInputMethodQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.TouchScreenQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.KeyboardStateQualifier.KeyboardState;
import com.android.ide.eclipse.adt.internal.resources.configurations.NavigationMethodQualifier.NavigationMethod;
import com.android.ide.eclipse.adt.internal.resources.configurations.PixelDensityQualifier.Density;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenOrientationQualifier.ScreenOrientation;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenRatioQualifier.ScreenRatio;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenSizeQualifier.ScreenSize;
import com.android.ide.eclipse.adt.internal.resources.configurations.TextInputMethodQualifier.TextInputMethod;
import com.android.ide.eclipse.adt.internal.resources.configurations.TouchScreenQualifier.TouchScreenType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DeviceConfiguration {

    private final String mName;
    private Map<String, FolderConfiguration> mMap =
        new HashMap<String, FolderConfiguration>();

    DeviceConfiguration(String name) {
        mName = name;
    }

    void addConfig(String name, FolderConfiguration config) {
        mMap.put(name, config);
    }

    void seal() {
        mMap = Collections.unmodifiableMap(mMap);
    }

    public String getName() {
        return mName;
    }

    public Map<String, FolderConfiguration> getConfigs() {
        return mMap;
    }

    /**
     * temp method returning some hard-coded devices.
     * TODO: load devices from the SDK and add-ons and remove this method.
     */
    public static DeviceConfiguration[] getDevices() {
        DeviceConfiguration adp1 = new DeviceConfiguration("ADP1");
        // default config
        FolderConfiguration defConfig = new FolderConfiguration();
        defConfig.addQualifier(new ScreenSizeQualifier(ScreenSize.NORMAL));
        defConfig.addQualifier(new ScreenRatioQualifier(ScreenRatio.NOTLONG));
        defConfig.addQualifier(new PixelDensityQualifier(Density.MEDIUM));
        defConfig.addQualifier(new TouchScreenQualifier(TouchScreenType.FINGER));
        defConfig.addQualifier(new TextInputMethodQualifier(TextInputMethod.QWERTY));
        defConfig.addQualifier(new NavigationMethodQualifier(NavigationMethod.TRACKBALL));
        defConfig.addQualifier(new ScreenDimensionQualifier(480, 320));

        // specific configs
        FolderConfiguration closedLand = new FolderConfiguration();
        closedLand.set(defConfig);
        closedLand.addQualifier(new ScreenOrientationQualifier(ScreenOrientation.LANDSCAPE));
        closedLand.addQualifier(new KeyboardStateQualifier(KeyboardState.HIDDEN));
        adp1.addConfig("Closed, landscape", closedLand);

        FolderConfiguration closedPort = new FolderConfiguration();
        closedPort.set(defConfig);
        closedPort.addQualifier(new ScreenOrientationQualifier(ScreenOrientation.PORTRAIT));
        closedPort.addQualifier(new KeyboardStateQualifier(KeyboardState.HIDDEN));
        adp1.addConfig("Closed, portrait", closedPort);

        FolderConfiguration opened = new FolderConfiguration();
        opened.set(defConfig);
        opened.addQualifier(new ScreenOrientationQualifier(ScreenOrientation.LANDSCAPE));
        opened.addQualifier(new KeyboardStateQualifier(KeyboardState.EXPOSED));
        adp1.addConfig("Opened", opened);

        DeviceConfiguration ion = new DeviceConfiguration("Ion");
        // default config
        defConfig = new FolderConfiguration();
        defConfig.addQualifier(new ScreenSizeQualifier(ScreenSize.NORMAL));
        defConfig.addQualifier(new ScreenRatioQualifier(ScreenRatio.NOTLONG));
        defConfig.addQualifier(new PixelDensityQualifier(Density.MEDIUM));
        defConfig.addQualifier(new TouchScreenQualifier(TouchScreenType.FINGER));
        defConfig.addQualifier(new KeyboardStateQualifier(KeyboardState.EXPOSED));
        defConfig.addQualifier(new TextInputMethodQualifier(TextInputMethod.NOKEY));
        defConfig.addQualifier(new NavigationMethodQualifier(NavigationMethod.TRACKBALL));
        defConfig.addQualifier(new ScreenDimensionQualifier(480, 320));

        // specific configs
        FolderConfiguration landscape = new FolderConfiguration();
        landscape.set(defConfig);
        landscape.addQualifier(new ScreenOrientationQualifier(ScreenOrientation.LANDSCAPE));
        ion.addConfig("Landscape", landscape);

        FolderConfiguration portrait = new FolderConfiguration();
        portrait.set(defConfig);
        portrait.addQualifier(new ScreenOrientationQualifier(ScreenOrientation.PORTRAIT));
        ion.addConfig("Portrait", portrait);

        return new DeviceConfiguration[] { adp1, ion };

    }
}
