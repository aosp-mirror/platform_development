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

import com.android.ddmlib.RawImage;

import java.awt.image.BufferedImage;
/**
 * Useful image related functions.
 */
public class ImageUtils {
    // Utility class
    private ImageUtils() { }

    public static BufferedImage convertImage(RawImage rawImage, BufferedImage image) {
        if (image == null || rawImage.width != image.getWidth() ||
                rawImage.height != image.getHeight()) {
            image = new BufferedImage(rawImage.width, rawImage.height,
                    BufferedImage.TYPE_INT_ARGB);
        }

        switch (rawImage.bpp) {
            case 16:
                rawImage16toARGB(image, rawImage);
                break;
            case 32:
                rawImage32toARGB(image, rawImage);
                break;
        }

        return image;
    }

    public static BufferedImage convertImage(RawImage rawImage) {
        return convertImage(rawImage, null);
    }

    private static int getMask(int length) {
        int res = 0;
        for (int i = 0 ; i < length ; i++) {
            res = (res << 1) + 1;
        }

        return res;
    }

    private static void rawImage32toARGB(BufferedImage image, RawImage rawImage) {
        int[] scanline = new int[rawImage.width];
        byte[] buffer = rawImage.data;
        int index = 0;

        final int redOffset = rawImage.red_offset;
        final int redLength = rawImage.red_length;
        final int redMask = getMask(redLength);
        final int greenOffset = rawImage.green_offset;
        final int greenLength = rawImage.green_length;
        final int greenMask = getMask(greenLength);
        final int blueOffset = rawImage.blue_offset;
        final int blueLength = rawImage.blue_length;
        final int blueMask = getMask(blueLength);
        final int alphaLength = rawImage.alpha_length;
        final int alphaOffset = rawImage.alpha_offset;
        final int alphaMask = getMask(alphaLength);

        for (int y = 0 ; y < rawImage.height ; y++) {
            for (int x = 0 ; x < rawImage.width ; x++) {
                int value = buffer[index++] & 0x00FF;
                value |= (buffer[index++] & 0x00FF) << 8;
                value |= (buffer[index++] & 0x00FF) << 16;
                value |= (buffer[index++] & 0x00FF) << 24;

                int r = ((value >>> redOffset) & redMask) << (8 - redLength);
                int g = ((value >>> greenOffset) & greenMask) << (8 - greenLength);
                int b = ((value >>> blueOffset) & blueMask) << (8 - blueLength);
                int a = 0xFF;

                if (alphaLength != 0) {
                    a = ((value >>> alphaOffset) & alphaMask) << (8 - alphaLength);
                }

                scanline[x] = a << 24 | r << 16 | g << 8 | b;
            }

            image.setRGB(0, y, rawImage.width, 1, scanline,
                    0, rawImage.width);
        }
    }

    private static void rawImage16toARGB(BufferedImage image, RawImage rawImage) {
        int[] scanline = new int[rawImage.width];
        byte[] buffer = rawImage.data;
        int index = 0;

        for (int y = 0 ; y < rawImage.height ; y++) {
            for (int x = 0 ; x < rawImage.width ; x++) {
                int value = buffer[index++] & 0x00FF;
                value |= (buffer[index++] << 8) & 0x0FF00;

                int r = ((value >> 11) & 0x01F) << 3;
                int g = ((value >> 5) & 0x03F) << 2;
                int b = ((value     ) & 0x01F) << 3;

                scanline[x] = 0xFF << 24 | r << 16 | g << 8 | b;
            }

            image.setRGB(0, y, rawImage.width, 1, scanline,
                    0, rawImage.width);
        }
    }
}
