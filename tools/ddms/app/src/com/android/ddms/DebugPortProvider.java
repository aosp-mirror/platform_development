/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ddms;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.DebugPortManager.IDebugPortProvider;

import org.eclipse.jface.preference.IPreferenceStore;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * DDMS implementation of the IDebugPortProvider interface.
 * This class handles saving/loading the list of static debug port from
 * the preference store and provides the port number to the Device Monitor.
 */
public class DebugPortProvider implements IDebugPortProvider {

    private static DebugPortProvider sThis  = new DebugPortProvider();

    /** Preference name for the static port list. */
    public static final String PREFS_STATIC_PORT_LIST = "android.staticPortList"; //$NON-NLS-1$

    /**
     * Mapping device serial numbers to maps. The embedded maps are mapping application names to
     * debugger ports.
     */
    private Map<String, Map<String, Integer>> mMap;

    public static DebugPortProvider getInstance() {
        return sThis;
    }

    private DebugPortProvider() {
        computePortList();
    }

    /**
         * Returns a static debug port for the specified application running on the
         * specified {@link IDevice}.
         * @param device The device the application is running on.
         * @param appName The application name, as defined in the
         *  AndroidManifest.xml package attribute.
         * @return The static debug port or {@link #NO_STATIC_PORT} if there is none setup.
     *
     * @see IDebugPortProvider#getPort(IDevice, String)
     */
    public int getPort(IDevice device, String appName) {
        if (mMap != null) {
            Map<String, Integer> deviceMap = mMap.get(device.getSerialNumber());
            if (deviceMap != null) {
                Integer i = deviceMap.get(appName);
                if (i != null) {
                    return i.intValue();
                }
            }
        }
        return IDebugPortProvider.NO_STATIC_PORT;
    }

    /**
     * Returns the map of Static debugger ports. The map links device serial numbers to
     * a map linking application name to debugger ports.
     */
    public Map<String, Map<String, Integer>> getPortList() {
        return mMap;
    }

    /**
     * Create the map member from the values contained in the Preference Store.
     */
    private void computePortList() {
        mMap = new HashMap<String, Map<String, Integer>>();

        // get the prefs store
        IPreferenceStore store = PrefsDialog.getStore();
        String value = store.getString(PREFS_STATIC_PORT_LIST);

        if (value != null && value.length() > 0) {
            // format is
            // port1|port2|port3|...
            // where port# is
            // appPackageName:appPortNumber:device-serial-number
            String[] portSegments = value.split("\\|");  //$NON-NLS-1$
            for (String seg : portSegments) {
                String[] entry = seg.split(":");  //$NON-NLS-1$

                // backward compatibility support. if we have only 2 entry, we default
                // to the first emulator.
                String deviceName = null;
                if (entry.length == 3) {
                    deviceName = entry[2];
                } else {
                    deviceName = IDevice.FIRST_EMULATOR_SN;
                }

                // get the device map
                Map<String, Integer> deviceMap = mMap.get(deviceName);
                if (deviceMap == null) {
                    deviceMap = new HashMap<String, Integer>();
                    mMap.put(deviceName, deviceMap);
                }

                deviceMap.put(entry[0], Integer.valueOf(entry[1]));
            }
        }
    }

    /**
     * Sets new [device, app, port] values.
     * The values are also sync'ed in the preference store.
     * @param map The map containing the new values.
     */
    public void setPortList(Map<String, Map<String,Integer>> map) {
        // update the member map.
        mMap.clear();
        mMap.putAll(map);

        // create the value to store in the preference store.
        // see format definition in getPortList
        StringBuilder sb = new StringBuilder();

        Set<String> deviceKeys = map.keySet();
        for (String deviceKey : deviceKeys) {
            Map<String, Integer> deviceMap = map.get(deviceKey);
            if (deviceMap != null) {
                Set<String> appKeys = deviceMap.keySet();

                for (String appKey : appKeys) {
                    Integer port = deviceMap.get(appKey);
                    if (port != null) {
                        sb.append(appKey).append(':').append(port.intValue()).append(':').
                            append(deviceKey).append('|');
                    }
                }
            }
        }

        String value = sb.toString();

        // get the prefs store.
        IPreferenceStore store = PrefsDialog.getStore();

        // and give it the new value.
        store.setValue(PREFS_STATIC_PORT_LIST, value);
    }
}
