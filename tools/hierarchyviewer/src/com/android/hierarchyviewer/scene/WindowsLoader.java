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
import com.android.hierarchyviewer.device.DeviceBridge;
import com.android.hierarchyviewer.device.Window;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

public class WindowsLoader {
    public static Window[] loadWindows(IDevice device) {
        Socket socket = null;
        BufferedReader in = null;
        BufferedWriter out = null;

        try {
            ArrayList<Window> windows = new ArrayList<Window>();

            socket = new Socket();
            socket.connect(new InetSocketAddress("127.0.0.1",
                    DeviceBridge.getDeviceLocalPort(device)));

            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.write("LIST");
            out.newLine();
            out.flush();

            String line;
            while ((line = in.readLine()) != null) {
                if ("DONE.".equalsIgnoreCase(line)) {
                    break;
                }

                int index = line.indexOf(' ');
                if (index != -1) {
                    Window w = new Window(line.substring(index + 1),
                            Integer.parseInt(line.substring(0, index), 16));
                    windows.add(w);
                }
            }

            return windows.toArray(new Window[windows.size()]);
        } catch (IOException e) {
            // Empty
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return new Window[0];
    }
}
