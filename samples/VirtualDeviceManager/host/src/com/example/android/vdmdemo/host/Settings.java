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

import javax.inject.Inject;
import javax.inject.Singleton;

/** Settings known to the VDM Demo Host application */
@Singleton
final class Settings {
    public boolean displayRotationEnabled = true;
    public boolean sensorsEnabled = true;
    public boolean audioEnabled = true;
    public boolean includeInRecents = false;
    public boolean crossDeviceClipboardEnabled = false;
    public boolean alwaysUnlocked = true;
    public boolean deviceStreaming = false;
    public boolean showPointerIcon = true;
    public boolean immersiveMode = false;
    public boolean customHome = false;

    /**
     * When enabled, the encoder output of the host will be stored in:
     * /sdcard/Download/vdmdemo_encoder_output_[displayId].h264
     *
     * <p>After pulling this file to your machine this can be played back with:
     * {@code ffplay -f h264 vdmdemo_encoder_output_[displayId].h264}
     */
    boolean recordEncoderOutput = false;

    @Inject
    Settings() {}
}
