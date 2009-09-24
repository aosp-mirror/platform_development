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

package com.android.screenshot;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;
import com.android.ddmlib.RawImage;
import com.android.ddmlib.Log.ILogOutput;
import com.android.ddmlib.Log.LogLevel;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Connects to a device using ddmlib and dumps its event log as long as the device is connected.
 */
public class Screenshot {

    public static void main(String[] args) {
        boolean device = false;
        boolean emulator = false;
        String serial = null;
        String filepath = null;
        boolean landscape = false;

        if (args.length == 0) {
            printUsageAndQuit();
        }

        // parse command line parameters.
        int index = 0;
        do {
            String argument = args[index++];

            if ("-d".equals(argument)) {
                if (emulator || serial != null) {
                    printAndExit("-d conflicts with -e and -s", false /* terminate */);
                }
                device = true;
            } else if ("-e".equals(argument)) {
                if (device || serial != null) {
                    printAndExit("-e conflicts with -d and -s", false /* terminate */);
                }
                emulator = true;
            } else if ("-s".equals(argument)) {
                // quick check on the next argument.
                if (index == args.length) {
                    printAndExit("Missing serial number after -s", false /* terminate */);
                }

                if (device || emulator) {
                    printAndExit("-s conflicts with -d and -e", false /* terminate */);
                }

                serial = args[index++];
            } else if ("-l".equals(argument)) {
                landscape = true;
            } else {
                // get the filepath and break.
                filepath = argument;

                // should not be any other device.
                if (index < args.length) {
                    printAndExit("Too many arguments!", false /* terminate */);
                }
            }
        } while (index < args.length);

        if (filepath == null) {
            printUsageAndQuit();
        }

        Log.setLogOutput(new ILogOutput() {
            public void printAndPromptLog(LogLevel logLevel, String tag, String message) {
                System.err.println(logLevel.getStringValue() + ":" + tag + ":" + message);
            }

            public void printLog(LogLevel logLevel, String tag, String message) {
                System.err.println(logLevel.getStringValue() + ":" + tag + ":" + message);
            }
        });

        // init the lib
        // [try to] ensure ADB is running
        String adbLocation = System.getProperty("com.android.screenshot.bindir"); //$NON-NLS-1$
        if (adbLocation != null && adbLocation.length() != 0) {
            adbLocation += File.separator + "adb"; //$NON-NLS-1$
        } else {
            adbLocation = "adb"; //$NON-NLS-1$
        }

        AndroidDebugBridge.init(false /* debugger support */);

        try {
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
                    return;
                }
            }

            // now get the devices
            IDevice[] devices = bridge.getDevices();

            if (devices.length == 0) {
                printAndExit("No devices found!", true /* terminate */);
            }

            IDevice target = null;

            if (emulator || device) {
                for (IDevice d : devices) {
                    // this test works because emulator and device can't both be true at the same
                    // time.
                    if (d.isEmulator() == emulator) {
                        // if we already found a valid target, we print an error and return.
                        if (target != null) {
                            if (emulator) {
                                printAndExit("Error: more than one emulator launched!",
                                        true /* terminate */);
                            } else {
                                printAndExit("Error: more than one device connected!",true /* terminate */);
                            }
                        }
                        target = d;
                    }
                }
            } else if (serial != null) {
                for (IDevice d : devices) {
                    if (serial.equals(d.getSerialNumber())) {
                        target = d;
                        break;
                    }
                }
            } else {
                if (devices.length > 1) {
                    printAndExit("Error: more than one emulator or device available!",
                            true /* terminate */);
                }
                target = devices[0];
            }

            if (target != null) {
                try {
                    System.out.println("Taking screenshot from: " + target.getSerialNumber());
                    getDeviceImage(target, filepath, landscape);
                    System.out.println("Success.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                printAndExit("Could not find matching device/emulator.", true /* terminate */);
            }
        } finally {
            AndroidDebugBridge.terminate();
        }
    }

    /*
     * Grab an image from an ADB-connected device.
     */
    private static void getDeviceImage(IDevice device, String filepath, boolean landscape)
            throws IOException {
        RawImage rawImage;

        try {
            rawImage = device.getScreenshot();
        }
        catch (IOException ioe) {
            printAndExit("Unable to get frame buffer: " + ioe.getMessage(), true /* terminate */);
            return;
        }

        // device/adb not available?
        if (rawImage == null)
            return;

        if (landscape) {
            rawImage = rawImage.getRotated();
        }

        // convert raw data to an Image
        BufferedImage image = new BufferedImage(rawImage.width, rawImage.height,
                BufferedImage.TYPE_INT_ARGB);

        int index = 0;
        int IndexInc = rawImage.bpp >> 3;
        for (int y = 0 ; y < rawImage.height ; y++) {
            for (int x = 0 ; x < rawImage.width ; x++) {
                int value = rawImage.getARGB(index);
                index += IndexInc;
                image.setRGB(x, y, value);
            }
        }

        if (!ImageIO.write(image, "png", new File(filepath))) {
            throw new IOException("Failed to find png writer");
        }
    }

    private static void printUsageAndQuit() {
        // 80 cols marker:  01234567890123456789012345678901234567890123456789012345678901234567890123456789
        System.out.println("Usage: screenshot2 [-d | -e | -s SERIAL] [-l] OUT_FILE");
        System.out.println("");
        System.out.println("    -d      Uses the first device found.");
        System.out.println("    -e      Uses the first emulator found.");
        System.out.println("    -s      Targets the device by serial number.");
        System.out.println("");
        System.out.println("    -l      Rotate images for landscape mode.");
        System.out.println("");

        System.exit(1);
    }

    private static void printAndExit(String message, boolean terminate) {
        System.out.println(message);
        if (terminate) {
            AndroidDebugBridge.terminate();
        }
        System.exit(1);
    }
}
