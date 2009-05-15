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

package com.android.hierarchyviewer.scene;

import com.android.ddmlib.IDevice;
import com.android.hierarchyviewer.device.Window;
import com.android.hierarchyviewer.device.DeviceBridge;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ViewManager {
    public static void invalidate(IDevice device, Window window, String params) {
        sendCommand("INVALIDATE", device, window, params);
    }

    public static void requestLayout(IDevice device, Window window, String params) {
        sendCommand("REQUEST_LAYOUT", device, window, params);
    }

    private static void sendCommand(String command, IDevice device, Window window, String params) {
        Socket socket = null;
        BufferedWriter out = null;

        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("127.0.0.1",
                    DeviceBridge.getDeviceLocalPort(device)));

            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            out.write(command + " " + window.encode() + " " + params);
            out.newLine();
            out.flush();
        } catch (IOException e) {
            // Empty
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
