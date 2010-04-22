/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.monkeyrunner;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  MonkeyRunner is a host side application to control a monkey instance on a
 *  device. MonkeyRunner provides some useful helper functions to control the
 *  device as well as various other methods to help script tests.  This class bootstraps
 *  MonkeyRunner.
 */
public class MonkeyRunnerStarter {
    private static final Logger LOG = Logger.getLogger(MonkeyRunnerStarter.class.getName());
    // delay between key events
    private static final int KEY_INPUT_DELAY = 1000;

    private final MonkeyManager monkeyManager;
    private final File scriptFile;

    public MonkeyRunnerStarter(MonkeyManager monkeyManager,
            File scriptFile) {
        this.monkeyManager = monkeyManager;
        this.scriptFile = scriptFile;
    }

    private void run() throws IOException {
        MonkeyRunner.MONKEY_MANANGER = monkeyManager;
        MonkeyRunner.start_script();
        ScriptRunner.run(scriptFile.getAbsolutePath());
        MonkeyRunner.end_script();
        monkeyManager.close();
    }

    public static void main(String[] args) throws IOException {
        // haven't figure out how to get below INFO...bad parent.  Pass -v INFO to turn on logging
        LOG.setLevel(Level.parse("WARNING"));
        MonkeyRunningOptions options = MonkeyRunningOptions.processOptions(args);

        if (options == null) {
            return;
        }

        MonkeyRunnerStarter runner = new MonkeyRunnerStarter(initAdbConnection(),
                options.getScriptFile());
        runner.run();
        System.exit(0);
    }

    /**
     * Initialize an adb session with a device connected to the host
     *
     * @return a monkey manager.
     */
    private static MonkeyManager initAdbConnection() {
        String adbLocation = "adb";
        boolean device = false;
        boolean emulator = false;
        String serial = null;

        AndroidDebugBridge.init(false /* debugger support */);

        AndroidDebugBridge bridge = AndroidDebugBridge.createBridge(
                adbLocation, true /* forceNewBridge */);

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
                return null;
            }
        }

        // now get the devices
        IDevice[] devices = bridge.getDevices();

        if (devices.length == 0) {
            printAndExit("No devices found!", true /* terminate */);
        }

        IDevice monkeyDevice = null;

        if (emulator || device) {
            for (IDevice d : devices) {
                // this test works because emulator and device can't both be true at the same
                // time.
                if (d.isEmulator() == emulator) {
                    // if we already found a valid target, we print an error and return.
                    if (monkeyDevice != null) {
                        if (emulator) {
                            printAndExit("Error: more than one emulator launched!",
                                    true /* terminate */);
                        } else {
                            printAndExit("Error: more than one device connected!",
                                    true /* terminate */);
                        }
                    }
                    monkeyDevice = d;
                }
            }
        } else if (serial != null) {
            for (IDevice d : devices) {
                if (serial.equals(d.getSerialNumber())) {
                    monkeyDevice = d;
                    break;
                }
            }
        } else {
            if (devices.length > 1) {
                printAndExit("Error: more than one emulator or device available!",
                        true /* terminate */);
            }
            monkeyDevice = devices[0];
        }

        return new MonkeyManager(monkeyDevice);
    }

    private static void printAndExit(String message, boolean terminate) {
        System.out.println(message);
        if (terminate) {
            AndroidDebugBridge.terminate();
        }
        System.exit(1);
    }
}