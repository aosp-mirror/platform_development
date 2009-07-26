/*
 * Copyright (C) 2009 The Android Open Source Project
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

import java.net.Socket;
import java.net.InetSocketAddress;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ProfilesLoader {
    public static double[] loadProfiles(IDevice device, Window window, String params) {
        Socket socket = null;
        BufferedReader in = null;
        BufferedWriter out = null;

        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("127.0.0.1",
                    DeviceBridge.getDeviceLocalPort(device)));

            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.write("PROFILE " + window.encode() + " " + params);
            out.newLine();
            out.flush();

            String response = in.readLine();
            String[] data = response.split(" ");

            double[] profiles = new double[data.length];
            for (int i = 0; i < data.length; i++) {
                profiles[i] = (Long.parseLong(data[i]) / 1000.0) / 1000.0; // convert to ms
            }
            return profiles;
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

        return null;
    }
}
