/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.nfcprovisioning;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.support.v4.content.AsyncTaskLoader;

import com.example.android.common.logger.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * Loads default values for NFC provisioning.
 * <p/>
 * This loader first tries to load values from a config file in SD card. Then it fills in missing
 * values using constants and settings on the programming device.
 */
public class ProvisioningValuesLoader extends AsyncTaskLoader<Map<String, String>> {

    private static final String FILENAME = "nfcprovisioning.txt";
    private static final String TAG = "LoadProvisioningValuesTask";

    private Map<String, String> mValues;

    public ProvisioningValuesLoader(Context context) {
        super(context);
    }

    @Override
    public Map<String, String> loadInBackground() {
        HashMap<String, String> values = new HashMap<>();
        loadFromDisk(values);
        loadSystemValues(values);
        return values;
    }

    @Override
    public void deliverResult(Map<String, String> values) {
        if (isReset()) {
            return;
        }
        mValues = values;
        super.deliverResult(values);
    }

    @Override
    protected void onStartLoading() {
        if (mValues != null) {
            deliverResult(mValues);
        }
        if (takeContentChanged() || mValues == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        mValues = null;
    }

    private void loadFromDisk(HashMap<String, String> values) {
        File directory = Environment.getExternalStorageDirectory();
        File file = new File(directory, FILENAME);
        if (!file.exists()) {
            return;
        }
        Log.d(TAG, "Loading the config file...");
        try {
            loadFromFile(values, file);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error loading data from " + file, e);
        }
    }

    private void loadFromFile(HashMap<String, String> values, File file) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while (null != (line = reader.readLine())) {
                if (line.startsWith("#")) {
                    continue;
                }
                int position = line.indexOf("=");
                if (position < 0) { // Not found
                    continue;
                }
                String key = line.substring(0, position);
                String value = line.substring(position + 1);
                values.put(key, value);
                Log.d(TAG, key + "=" + value);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private void loadSystemValues(HashMap<String, String> values) {
        Context context = getContext();
        putIfMissing(values, DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME,
                "com.example.android.deviceowner");
        putIfMissing(values, DevicePolicyManager.EXTRA_PROVISIONING_LOCALE,
                context.getResources().getConfiguration().locale.toString());
        putIfMissing(values, DevicePolicyManager.EXTRA_PROVISIONING_TIME_ZONE,
                TimeZone.getDefault().getID());
        if (!values.containsKey(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_SSID)) {
            WifiManager wifiManager = (WifiManager) context
                    .getSystemService(Activity.WIFI_SERVICE);
            WifiInfo info = wifiManager.getConnectionInfo();
            values.put(DevicePolicyManager.EXTRA_PROVISIONING_WIFI_SSID, trimSsid(info.getSSID()));
        }
    }

    /**
     * {@link WifiInfo#getSSID} returns the WiFi SSID surrounded by double quotation marks. This
     * method removes them if wifiSsid contains them.
     */
    private static String trimSsid(String wifiSsid) {
        int head = wifiSsid.startsWith("\"") ? 1 : 0;
        int tail = wifiSsid.endsWith("\"") ? 1 : 0;
        return wifiSsid.substring(head, wifiSsid.length() - tail);
    }

    private static <Key, Value> void putIfMissing(HashMap<Key, Value> map, Key key, Value value) {
        if (!map.containsKey(key)) {
            map.put(key, value);
        }
    }

}
