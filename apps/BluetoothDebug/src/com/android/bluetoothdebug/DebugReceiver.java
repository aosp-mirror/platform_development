/*
 * Copyright 2009, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.bluetoothdebug;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothDevicePicker;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Prints Bluetooth intents to logcat. For example:
 * BTDEBUG : a.b.device.a.FOUND
 * BTDEBUG :       a.b.device.e.DEVICE = 00:18:13:F2:CC:33
 * BTDEBUG :       a.b.device.e.RSSI = -35
 * BTDEBUG :       a.b.device.e.CLASS = 200404
 * BTDEBUG : a.b.adapter.a.DISCOVERY_FINISHED
 * BTDEBUG : a.b.device.a.BOND_STATE_CHANGED
 * BTDEBUG :       a.b.device.e.DEVICE = 00:18:13:F2:CC:33
 * BTDEBUG :       a.b.device.e.BOND_STATE = 11
 * BTDEBUG :       a.b.device.e.PREVIOUS_BOND_STATE = 10
 */
public class DebugReceiver extends BroadcastReceiver {
    private static final String TAG = "BTDEBUG";

    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, shorten(intent.getAction()));

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;
        for (String extra : bundle.keySet()) {
            Log.d(TAG, "\t" + shorten(extra) + " = " + bundle.get(extra));
        }
    }

    // shorten string to shorthand
    // android.bluetooth.device.extra.DEVICE -> a.b.device.e.DEVICE
    private static String shorten(String action) {
        return action.replace("android", "a")
                     .replace("bluetooth", "b")
                     .replace("extra", "e")
                     .replace("action", "a");
    }

}
