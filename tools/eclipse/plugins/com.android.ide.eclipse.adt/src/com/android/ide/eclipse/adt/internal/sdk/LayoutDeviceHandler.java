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

import com.android.ide.eclipse.adt.internal.resources.configurations.CountryCodeQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;
import com.android.ide.eclipse.adt.internal.resources.configurations.KeyboardStateQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.NavigationMethodQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.NetworkCodeQualifier;
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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link DefaultHandler} implementation to parse Layout Device XML file.
 * @see LayoutDevicesXsd
 * @see Layout-configs.xsd
 */
class LayoutDeviceHandler extends DefaultHandler {
    /*
     * The handler does most of the work in startElement and endElement.
     * In startElement, it'll create DeviceConfiguration on <device>, as well as
     * FolderConfiguration instances on <default> and <config>.
     * Those objects are then filled as new nodes are discovered.
     *
     * For the qualifier values, the qualifier is created and added to the current config
     * on the endElement, by using the content found in characters().
     */

    private List<LayoutDevice> mDevices = new ArrayList<LayoutDevice>();

    private LayoutDevice mCurrentDevice;
    private FolderConfiguration mDefaultConfig;
    private FolderConfiguration mCurrentConfig;
    private final StringBuilder mStringAccumulator = new StringBuilder();

    private String mSize1, mSize2;

    public List<LayoutDevice> getDevices() {
        return mDevices;
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes)
            throws SAXException {
        if (LayoutDevicesXsd.NODE_DEVICE.equals(localName)) {
            // get the deviceName, will not be null since we validated the XML.
            String deviceName = attributes.getValue("", LayoutDevicesXsd.ATTR_NAME);

            // create a device and add it to the list
            mCurrentDevice = new LayoutDevice(deviceName);
            mDevices.add(mCurrentDevice);
        } else if (LayoutDevicesXsd.NODE_DEFAULT.equals(localName)) {
            // create a new default config
            mDefaultConfig = mCurrentConfig = new FolderConfiguration();
        } else if (LayoutDevicesXsd.NODE_CONFIG.equals(localName)) {
            // create a new config
            mCurrentConfig = new FolderConfiguration();

            // init with default config if applicable
            if (mDefaultConfig != null) {
                mCurrentConfig.set(mDefaultConfig);
            }

            // get the name of the config
            String deviceName = attributes.getValue("", LayoutDevicesXsd.ATTR_NAME);

            // give it to the current device.
            mCurrentDevice.addConfig(deviceName, mCurrentConfig);
        } else if (LayoutDevicesXsd.NODE_SCREEN_DIMENSION.equals(localName)) {
            mSize1 = mSize2 = null;
        }

        mStringAccumulator.setLength(0);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        mStringAccumulator.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
        if (LayoutDevicesXsd.NODE_DEVICE.equals(localName)) {
            mCurrentDevice = null;
            mDefaultConfig = null;
        } else if (LayoutDevicesXsd.NODE_CONFIG.equals(localName)) {
            mCurrentConfig = null;
        } else if (LayoutDevicesXsd.NODE_COUNTRY_CODE.equals(localName)) {
            CountryCodeQualifier ccq = new CountryCodeQualifier(
                    Integer.parseInt(mStringAccumulator.toString()));
            mCurrentConfig.setCountryCodeQualifier(ccq);
        } else if (LayoutDevicesXsd.NODE_NETWORK_CODE.equals(localName)) {
            NetworkCodeQualifier ncq = new NetworkCodeQualifier(
                    Integer.parseInt(mStringAccumulator.toString()));
            mCurrentConfig.setNetworkCodeQualifier(ncq);
        } else if (LayoutDevicesXsd.NODE_SCREEN_SIZE.equals(localName)) {
            ScreenSizeQualifier ssq = new ScreenSizeQualifier(
                    ScreenSize.getEnum(mStringAccumulator.toString()));
            mCurrentConfig.setScreenSizeQualifier(ssq);
        } else if (LayoutDevicesXsd.NODE_SCREEN_RATIO.equals(localName)) {
            ScreenRatioQualifier srq = new ScreenRatioQualifier(
                    ScreenRatio.getEnum(mStringAccumulator.toString()));
            mCurrentConfig.setScreenRatioQualifier(srq);
        } else if (LayoutDevicesXsd.NODE_SCREEN_ORIENTATION.equals(localName)) {
            ScreenOrientationQualifier soq = new ScreenOrientationQualifier(
                    ScreenOrientation.getEnum(mStringAccumulator.toString()));
            mCurrentConfig.setScreenOrientationQualifier(soq);
        } else if (LayoutDevicesXsd.NODE_PIXEL_DENSITY.equals(localName)) {
            PixelDensityQualifier pdq = new PixelDensityQualifier(
                    Density.getEnum(mStringAccumulator.toString()));
            mCurrentConfig.setPixelDensityQualifier(pdq);
        } else if (LayoutDevicesXsd.NODE_TOUCH_TYPE.equals(localName)) {
            TouchScreenQualifier tsq = new TouchScreenQualifier(
                    TouchScreenType.getEnum(mStringAccumulator.toString()));
            mCurrentConfig.setTouchTypeQualifier(tsq);
        } else if (LayoutDevicesXsd.NODE_KEYBOARD_STATE.equals(localName)) {
            KeyboardStateQualifier ksq = new KeyboardStateQualifier(
                    KeyboardState.getEnum(mStringAccumulator.toString()));
            mCurrentConfig.setKeyboardStateQualifier(ksq);
        } else if (LayoutDevicesXsd.NODE_TEXT_INPUT_METHOD.equals(localName)) {
            TextInputMethodQualifier timq = new TextInputMethodQualifier(
                    TextInputMethod.getEnum(mStringAccumulator.toString()));
            mCurrentConfig.setTextInputMethodQualifier(timq);
        } else if (LayoutDevicesXsd.NODE_NAV_METHOD.equals(localName)) {
            NavigationMethodQualifier nmq = new NavigationMethodQualifier(
                    NavigationMethod.getEnum(mStringAccumulator.toString()));
            mCurrentConfig.setNavigationMethodQualifier(nmq);
        } else if (LayoutDevicesXsd.NODE_SCREEN_DIMENSION.equals(localName)) {
            ScreenDimensionQualifier qual = ScreenDimensionQualifier.getQualifier(mSize1, mSize2);
            if (qual != null) {
                mCurrentConfig.setScreenDimensionQualifier(qual);
            }
        } else if (LayoutDevicesXsd.NODE_XDPI.equals(localName)) {
            mCurrentDevice.setXDpi(Float.parseFloat(mStringAccumulator.toString()));
        } else if (LayoutDevicesXsd.NODE_YDPI.equals(localName)) {
            mCurrentDevice.setYDpi(Float.parseFloat(mStringAccumulator.toString()));
        } else if (LayoutDevicesXsd.NODE_SIZE.equals(localName)) {
            if (mSize1 == null) {
                mSize1 = mStringAccumulator.toString();
            } else if (mSize2 == null) {
                mSize2 = mStringAccumulator.toString();
            }
        }
    }
}
