/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.sdksetup;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.input.InputManager;
import android.hardware.input.KeyboardLayout;
import android.location.LocationManager;
import android.provider.Settings;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.view.InputDevice;

/**
 * Entry point for SDK SetupWizard.
 *
 */
public class DefaultActivity extends Activity {

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Edit Settings only for Emulator
        if (Build.IS_EMULATOR) {
            // Set physical keyboard layout based on the system property set by emulator host.
            String layoutName = SystemProperties.get("qemu.keyboard_layout");
            String deviceName = "qwerty2";
            InputDevice device = getKeyboardDevice(deviceName);
            if (device != null && !layoutName.isEmpty()) {
                setKeyboardLayout(device, layoutName);
            }
            // Add a persistent setting to allow other apps to know the device has been provisioned.
            Settings.Global.putInt(getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 1);

            Settings.Secure.putInt(getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 1);

            // Disables a dialog shown on adb install execution.
            Settings.Global.putInt(getContentResolver(), Settings.Global.PACKAGE_VERIFIER_INCLUDE_ADB, 0);

            // Enable the GPS.
            // Not needed since this SDK will contain the Settings app.
            Settings.Secure.putString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED,
                    LocationManager.GPS_PROVIDER);

            // enable install from non market
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 1);

            Settings.Global.putInt(getContentResolver(), Settings.Global.ADB_ENABLED, 1);

            TelephonyManager mTelephony = getApplicationContext().getSystemService(TelephonyManager.class);
            mTelephony.setPreferredNetworkTypeBitmask(TelephonyManager.NETWORK_TYPE_BITMASK_NR);
        }

        // remove this activity from the package manager.
        PackageManager pm = getPackageManager();
        ComponentName name = new ComponentName(this, DefaultActivity.class);
        pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);

        // terminate the activity.
        finish();
    }

    private InputDevice getKeyboardDevice(String keyboardDeviceName) {
        int[] deviceIds = InputDevice.getDeviceIds();

        for (int deviceId : deviceIds) {
            InputDevice inputDevice = InputDevice.getDevice(deviceId);
            if (inputDevice != null
                    && inputDevice.supportsSource(InputDevice.SOURCE_KEYBOARD)
                    && inputDevice.getName().equals(keyboardDeviceName)) {
                return inputDevice;
            }
        }
        return null;
    }

    private void setKeyboardLayout(InputDevice keyboardDevice, String layoutName) {
        InputManager im = InputManager.getInstance();

        KeyboardLayout[] keyboardLayouts =
                im.getKeyboardLayoutsForInputDevice(keyboardDevice.getIdentifier());

        for (KeyboardLayout keyboardLayout : keyboardLayouts) {
            if (keyboardLayout.getDescriptor().endsWith(layoutName)) {
                im.setCurrentKeyboardLayoutForInputDevice(
                        keyboardDevice.getIdentifier(), keyboardLayout.getDescriptor());
                return;
            }
        }
    }
}

