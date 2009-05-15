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

package com.android.ddmuilib;

import com.android.ddmlib.Client;
import com.android.ddmlib.IDevice;

/**
 * A Panel that requires {@link Device}/{@link Client} selection notifications.
 */
public abstract class SelectionDependentPanel extends Panel {
    private IDevice mCurrentDevice = null;
    private Client mCurrentClient = null;

    /**
     * Returns the current {@link Device}.
     * @return the current device or null if none are selected.
     */
    protected final IDevice getCurrentDevice() {
        return mCurrentDevice;
    }

    /**
     * Returns the current {@link Client}.
     * @return the current client or null if none are selected.
     */
    protected final Client getCurrentClient() {
        return mCurrentClient;
    }

    /**
     * Sent when a new device is selected.
     * @param selectedDevice the selected device.
     */
    public final void deviceSelected(IDevice selectedDevice) {
        if (selectedDevice != mCurrentDevice) {
            mCurrentDevice = selectedDevice;
            deviceSelected();
        }
    }

    /**
     * Sent when a new client is selected.
     * @param selectedClient the selected client.
     */
    public final void clientSelected(Client selectedClient) {
        if (selectedClient != mCurrentClient) {
            mCurrentClient = selectedClient;
            clientSelected();
        }
    }

    /**
     * Sent when a new device is selected. The new device can be accessed
     * with {@link #getCurrentDevice()}.
     */
    public abstract void deviceSelected();

    /**
     * Sent when a new client is selected. The new client can be accessed
     * with {@link #getCurrentClient()}.
     */
    public abstract void clientSelected();
}
