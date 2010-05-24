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
package com.android.monkeyrunner.adb.image;

import com.android.ddmlib.RawImage;
import com.android.monkeyrunner.MonkeyDevice;
import com.android.monkeyrunner.adb.AdbBackend;
import com.android.monkeyrunner.adb.AdbMonkeyImage;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Utility program to capture raw and converted images from a device and write them to a file.
 * This is used to generate the test data for ImageUtilsTest.
 */
public class CaptureRawAndConvertedImage {
    public static class MonkeyRunnerRawImage implements Serializable {
        public int version;
        public int bpp;
        public int size;
        public int width;
        public int height;
        public int red_offset;
        public int red_length;
        public int blue_offset;
        public int blue_length;
        public int green_offset;
        public int green_length;
        public int alpha_offset;
        public int alpha_length;

        public byte[] data;

        public MonkeyRunnerRawImage(RawImage rawImage) {
            version = rawImage.version;
            bpp = rawImage.bpp;
            size = rawImage.size;
            width = rawImage.width;
            height = rawImage.height;
            red_offset = rawImage.red_offset;
            red_length = rawImage.red_length;
            blue_offset = rawImage.blue_offset;
            blue_length = rawImage.blue_length;
            green_offset = rawImage.green_offset;
            green_length = rawImage.green_length;
            alpha_offset = rawImage.alpha_offset;
            alpha_length = rawImage.alpha_length;

            data = rawImage.data;
        }

        public RawImage toRawImage() {
            RawImage rawImage = new RawImage();

            rawImage.version = version;
            rawImage.bpp = bpp;
            rawImage.size = size;
            rawImage.width = width;
            rawImage.height = height;
            rawImage.red_offset = red_offset;
            rawImage.red_length = red_length;
            rawImage.blue_offset = blue_offset;
            rawImage.blue_length = blue_length;
            rawImage.green_offset = green_offset;
            rawImage.green_length = green_length;
            rawImage.alpha_offset = alpha_offset;
            rawImage.alpha_length = alpha_length;

            rawImage.data = data;
            return rawImage;
        }
    }

    private static void writeOutImage(RawImage screenshot, String name) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(name));
        out.writeObject(new MonkeyRunnerRawImage(screenshot));
        out.close();
    }

    public static void main(String[] args) throws IOException {
        AdbBackend backend = new AdbBackend();
        MonkeyDevice device = backend.waitForConnection();
        AdbMonkeyImage snapshot = (AdbMonkeyImage) device.takeSnapshot();

        // write out to a file
        snapshot.writeToFile("output.png", "png");
        writeOutImage(snapshot.getRawImage(), "output.raw");
        System.exit(0);
    }
}
