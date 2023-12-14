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

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.VANILLA_ICE_CREAM;

import android.companion.virtual.VirtualDeviceManager.VirtualDevice;
import android.companion.virtual.flags.Flags;
import android.hardware.display.VirtualDisplayConfig;

public class VdmCompat {

    // Hidden DisplayManager.VIRTUAL_DISPLAY_FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS.
    private static final int VIRTUAL_DISPLAY_FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS = 1 << 9;

    private VdmCompat() {}

    static VirtualDisplayConfig.Builder setHomeSupported(
            VirtualDisplayConfig.Builder builder, int flags) {
        if (SDK_INT >= VANILLA_ICE_CREAM && Flags.vdmCustomHome()) {
            return builder.setHomeSupported(true);
        } else {
            return builder.setFlags(flags | VIRTUAL_DISPLAY_FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS);
        }
    }

    static void setDisplayImePolicy(VirtualDevice virtualDevice, int displayId, int policy) {
        if (SDK_INT >= VANILLA_ICE_CREAM) {
            virtualDevice.setDisplayImePolicy(displayId, policy);
        }
    }
}
