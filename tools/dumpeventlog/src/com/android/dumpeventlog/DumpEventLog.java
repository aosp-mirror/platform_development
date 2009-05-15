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

package com.android.dumpeventlog;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;
import com.android.ddmlib.Log.ILogOutput;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmlib.log.LogReceiver;
import com.android.ddmlib.log.LogReceiver.ILogListener;
import com.android.ddmlib.log.LogReceiver.LogEntry;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Connects to a device using ddmlib and dumps its event log as long as the device is connected.
 */
public class DumpEventLog {

    /**
     * Custom {@link ILogListener} to receive and save the event log raw output.
     */
    private static class LogWriter implements ILogListener {
        private FileOutputStream mOutputStream;
        private LogReceiver mReceiver;

        public LogWriter(String filePath) throws IOException {
            mOutputStream = new FileOutputStream(filePath);
        }

        public void newData(byte[] data, int offset, int length) {
            try {
                mOutputStream.write(data, offset, length);
            } catch (IOException e) {
                if (mReceiver != null) {
                    mReceiver.cancel();
                }
                System.out.println(e);
            }
        }

        public void newEntry(LogEntry entry) {
            // pass
        }

        public void setReceiver(LogReceiver receiver) {
            mReceiver = receiver;
        }

        public void done() throws IOException {
            mOutputStream.close();
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: dumpeventlog <device s/n> <filepath>");
            return;
        }

        // redirect the log output to /dev/null
        Log.setLogOutput(new ILogOutput() {
            public void printAndPromptLog(LogLevel logLevel, String tag, String message) {
                // pass
            }

            public void printLog(LogLevel logLevel, String tag, String message) {
                // pass
            }
        });

        // init the lib
        AndroidDebugBridge.init(false /* debugger support */);

        try {
            AndroidDebugBridge bridge = AndroidDebugBridge.createBridge();

            // we can't just ask for the device list right away, as the internal thread getting
            // them from ADB may not be done getting the first list.
            // Since we don't really want getDevices() to be blocking, we wait here manually.
            int count = 0;
            while (bridge.hasInitialDeviceList() == false) {
                try {
                    Thread.sleep(100);
                    count++;
                } catch (InterruptedException e) {
                    // pass
                }

                // let's not wait > 10 sec.
                if (count > 100) {
                    System.err.println("Timeout getting device list!");
                    return;
                }
            }

            // now get the devices
            IDevice[] devices = bridge.getDevices();

            for (IDevice device : devices) {
                if (device.getSerialNumber().equals(args[0])) {
                    try {
                        grabLogFrom(device, args[1]);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                }
            }

            System.err.println("Could not find " + args[0]);
        } finally {
            AndroidDebugBridge.terminate();
        }
    }

    private static void grabLogFrom(IDevice device, String filePath) throws IOException {
        LogWriter writer = new LogWriter(filePath);
        LogReceiver receiver = new LogReceiver(writer);
        writer.setReceiver(receiver);

        device.runEventLogService(receiver);

        writer.done();
    }
}
