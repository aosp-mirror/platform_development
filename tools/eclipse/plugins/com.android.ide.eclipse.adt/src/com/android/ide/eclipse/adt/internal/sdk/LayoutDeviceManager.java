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

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;
import com.android.sdklib.SdkConstants;

import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;

/**
 * Manages the layout devices.
 * They can come from 3 sources: built-in, add-ons, user.
 */
public class LayoutDeviceManager {

    /**
     * A SAX error handler that captures the errors and warnings.
     * This allows us to capture *all* errors and just not get an exception on the first one.
     */
    private static class CaptureErrorHandler implements ErrorHandler {

        private final String mSourceLocation;

        private boolean mFoundError = false;

        CaptureErrorHandler(String sourceLocation) {
            mSourceLocation = sourceLocation;
        }

        public boolean foundError() {
            return mFoundError;
        }

        /**
         * @throws SAXException
         */
        public void error(SAXParseException ex) throws SAXException {
            mFoundError = true;
            AdtPlugin.log(ex, "Error validating %1$s", mSourceLocation);
        }

        /**
         * @throws SAXException
         */
        public void fatalError(SAXParseException ex) throws SAXException {
            mFoundError = true;
            AdtPlugin.log(ex, "Error validating %1$s", mSourceLocation);
        }

        /**
         * @throws SAXException
         */
        public void warning(SAXParseException ex) throws SAXException {
            // ignore those for now.
        }
    }

    private final SAXParserFactory mParserFactory;

    private List<LayoutDevice> mDefaultLayoutDevices =
        new ArrayList<LayoutDevice>();
    private List<LayoutDevice> mAddOnLayoutDevices =
        new ArrayList<LayoutDevice>();
    private final List<LayoutDevice> mUserLayoutDevices =
        new ArrayList<LayoutDevice>();
    private List<LayoutDevice> mLayoutDevices;

    LayoutDeviceManager() {
        mParserFactory = SAXParserFactory.newInstance();
        mParserFactory.setNamespaceAware(true);
    }

    public List<LayoutDevice> getCombinedList() {
        return mLayoutDevices;
    }

    public List<LayoutDevice> getDefaultLayoutDevices() {
        return mDefaultLayoutDevices;
    }

    public List<LayoutDevice> getAddOnLayoutDevice() {
        return mAddOnLayoutDevices;
    }

    public List<LayoutDevice> getUserLayoutDevices() {
        return mUserLayoutDevices;
    }

    public LayoutDevice getUserLayoutDevice(String name) {
        for (LayoutDevice d : mUserLayoutDevices) {
            if (d.getName().equals(name)) {
                return d;
            }
        }

        return null;
    }

    public LayoutDevice addUserDevice(String name, float xdpi, float ydpi) {
        LayoutDevice d = new LayoutDevice(name);
        d.setXDpi(xdpi);
        d.setYDpi(ydpi);
        mUserLayoutDevices.add(d);
        combineLayoutDevices();

        return d;
    }

    public void removeUserDevice(LayoutDevice device) {
        if (mUserLayoutDevices.remove(device)) {
            combineLayoutDevices();
        }
    }

    /**
     * Replaces a device with a new one with new name and/or x/y dpi, and return the new device.
     * If the name and dpi values are identical the given device is returned an nothing is done
     * @param device the {@link LayoutDevice} to replace
     * @param newName the new name.
     * @param newXDpi the new X dpi value
     * @param newYDpi the new Y dpi value.
     * @return the new LayoutDevice
     */
    public LayoutDevice replaceUserDevice(LayoutDevice device, String newName,
            float newXDpi, float newYDpi) {
        if (device.getName().equals(newName) && device.getXDpi() == newXDpi &&
                device.getYDpi() == newYDpi) {
            return device;
        }

        // else create a new device
        LayoutDevice newDevice = new LayoutDevice(newName);
        newDevice.setXDpi(newXDpi);
        newDevice.setYDpi(newYDpi);

        // and get the Folderconfiguration
        Map<String, FolderConfiguration> configs = device.getConfigs();
        newDevice.addConfigs(configs);

        // replace the old device with the new
        mUserLayoutDevices.remove(device);
        mUserLayoutDevices.add(newDevice);
        combineLayoutDevices();

        return newDevice;
    }


    /**
     * Adds or replaces a configuration in a given {@link LayoutDevice}.
     * @param device the device to modify
     * @param configName the configuration name to add or replace
     * @param config the configuration to set
     */
    public void addUserConfiguration(LayoutDevice device, String configName,
            FolderConfiguration config) {
        // check that the device does belong to the user list.
        // the main goal is to make sure that this does not belong to the default/addon list.
        if (mUserLayoutDevices.contains(device)) {
            device.addConfig(configName, config);
        }
    }

    /**
     * Replaces a configuration in a given {@link LayoutDevice}.
     * @param device the device to modify
     * @param oldConfigName the name of the config to replace. If null, the new config is simply
     * added.
     * @param newConfigName the configuration name to add or replace
     * @param config the configuration to set
     */
    public void replaceUserConfiguration(LayoutDevice device, String oldConfigName,
            String newConfigName, FolderConfiguration config) {
        // check that the device does belong to the user list.
        // the main goal is to make sure that this does not belong to the default/addon list.
        if (mUserLayoutDevices.contains(device)) {
            // if the old and new config name are different, remove the old one
            if (oldConfigName != null && oldConfigName.equals(newConfigName) == false) {
                device.removeConfig(oldConfigName);
            }

            // and then add the new one
            device.addConfig(newConfigName, config);
        }
    }

    /**
     * Removes a configuration from a given user {@link LayoutDevice}
     * @param device the device to modify
     * @param configName the name of the config to remove
     */
    public void removeUserConfiguration(LayoutDevice device, String configName) {
        // check that the device does belong to the user list.
        // the main goal is to make sure that this does not belong to the default/addon list.
        if (mUserLayoutDevices.contains(device)) {
            device.removeConfig(configName);
        }
    }

    void load(String sdkOsLocation) {
        // load the default devices
        loadDefaultLayoutDevices(sdkOsLocation);

        // load the user devices;
    }

    void parseAddOnLayoutDevice(File deviceXml) {
        parseLayoutDevices(deviceXml, mAddOnLayoutDevices);
    }

    void sealAddonLayoutDevices() {
        mAddOnLayoutDevices = Collections.unmodifiableList(mAddOnLayoutDevices);

        combineLayoutDevices();
    }

    /**
     * Does the actual parsing of a devices.xml file.
     */
    private void parseLayoutDevices(File deviceXml, List<LayoutDevice> list) {
        // first we validate the XML
        try {
            Source source = new StreamSource(new FileReader(deviceXml));

            CaptureErrorHandler errorHandler = new CaptureErrorHandler(deviceXml.getAbsolutePath());

            Validator validator = LayoutConfigsXsd.getValidator(errorHandler);
            validator.validate(source);

            if (errorHandler.foundError() == false) {
                // do the actual parsing
                LayoutDeviceHandler handler = new LayoutDeviceHandler();

                SAXParser parser = mParserFactory.newSAXParser();
                parser.parse(new InputSource(new FileInputStream(deviceXml)), handler);

                // get the parsed devices
                list.addAll(handler.getDevices());
            }
        } catch (SAXException e) {
            AdtPlugin.log(e, "Error parsing %1$s", deviceXml.getAbsoluteFile());
        } catch (FileNotFoundException e) {
            // this shouldn't happen as we check above.
        } catch (IOException e) {
            AdtPlugin.log(e, "Error reading %1$s", deviceXml.getAbsoluteFile());
        } catch (ParserConfigurationException e) {
            AdtPlugin.log(e, "Error parsing %1$s", deviceXml.getAbsoluteFile());
        }
    }

    /**
     * Creates some built-it layout devices.
     */
    private void loadDefaultLayoutDevices(String sdkOsLocation) {
        ArrayList<LayoutDevice> list = new ArrayList<LayoutDevice>();
        File toolsFolder = new File(sdkOsLocation, SdkConstants.OS_SDK_TOOLS_LIB_FOLDER);
        if (toolsFolder.isDirectory()) {
            File deviceXml = new File(toolsFolder, SdkConstants.FN_DEVICES_XML);
            if (deviceXml.isFile()) {
                parseLayoutDevices(deviceXml, list);
            }
        }
        mDefaultLayoutDevices = Collections.unmodifiableList(list);
    }

    private void combineLayoutDevices() {
        ArrayList<LayoutDevice> list = new ArrayList<LayoutDevice>();
        list.addAll(mDefaultLayoutDevices);
        list.addAll(mAddOnLayoutDevices);
        list.addAll(mUserLayoutDevices);

        mLayoutDevices = Collections.unmodifiableList(list);
    }

}
