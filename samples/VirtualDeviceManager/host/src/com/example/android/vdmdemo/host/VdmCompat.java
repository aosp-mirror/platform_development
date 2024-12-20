/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.example.android.vdmdemo.host;

import static android.Manifest.permission.ADD_MIRROR_DISPLAY;

import android.companion.AssociationRequest;
import android.companion.virtual.VirtualDeviceManager.VirtualDevice;
import android.companion.virtualdevice.flags.Flags;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.display.VirtualDisplayConfig;
import android.hardware.input.InputManager;
import android.os.Build;
import android.view.InputDevice;

import androidx.core.os.BuildCompat;

public class VdmCompat {

    // Hidden DisplayManager.VIRTUAL_DISPLAY_FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS.
    private static final int VIRTUAL_DISPLAY_FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS = 1 << 9;

    private VdmCompat() {}

    static VirtualDisplayConfig.Builder setHomeSupported(
            VirtualDisplayConfig.Builder builder, int flags) {
        if (BuildCompat.isAtLeastV()) {
            return builder.setHomeSupported(true);
        } else {
            return builder.setFlags(flags | VIRTUAL_DISPLAY_FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS);
        }
    }

    static void setDisplayImePolicy(VirtualDevice virtualDevice, int displayId, int policy) {
        if (BuildCompat.isAtLeastV()) {
            virtualDevice.setDisplayImePolicy(displayId, policy);
        }
    }

    static boolean canCreateVirtualMouse(Context context) {
        if (BuildCompat.isAtLeastV()) {
            return true;
        }
        InputManager inputManager = context.getSystemService(InputManager.class);
        for (int inputDeviceId : inputManager.getInputDeviceIds()) {
            InputDevice inputDevice = inputManager.getInputDevice(inputDeviceId);
            String inputDeviceName = inputDevice.getName();
            if (inputDeviceName != null && inputDeviceName.startsWith("vdmdemo-mouse")) {
                return false;
            }
        }
        return true;
    }

    static boolean isMirrorDisplaySupported(Context context,
            PreferenceController preferenceController) {
        if (!preferenceController.getBoolean(R.string.internal_pref_mirror_displays_supported)) {
            return false;
        }
        if (isAtLeastB() && Flags.enableLimitedVdmRole()) {
            return context.checkCallingOrSelfPermission(ADD_MIRROR_DISPLAY)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return preferenceController.getString(R.string.pref_device_profile)
                .equals(AssociationRequest.DEVICE_PROFILE_APP_STREAMING);
    }

    // TODO(b/379277747): replace with BuildCompat.isAtLeastB once available.
    static boolean isAtLeastB() {
        return Build.VERSION.CODENAME.equals("Baklava")
                || Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA;
    }
}
