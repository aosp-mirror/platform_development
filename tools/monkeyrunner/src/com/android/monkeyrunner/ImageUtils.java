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

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Hashtable;
/**
 * Useful image related functions.
 */
public class ImageUtils {
    // Utility class
    private ImageUtils() { }

    private static Hashtable<?,?> EMPTY_HASH = new Hashtable();
    private static int[] BAND_OFFSETS_32 = { 0, 1, 2, 3 };
    private static int[] BAND_OFFSETS_16 = { 0, 1 };

    /**
     * Convert a raw image into a buffered image.
     *
     * @param rawImage the raw image to convert
     * @param image the old image to (possibly) recycle
     * @return the converted image
     */
    public static BufferedImage convertImage(RawImage rawImage, BufferedImage image) {
        switch (rawImage.bpp) {
            case 16:
                return rawImage16toARGB(image, rawImage);
            case 32:
                return rawImage32toARGB(rawImage);
        }
        return null;
    }

    /**
     * Convert a raw image into a buffered image.
     *
     * @param rawImage the image to convert.
     * @return the converted image.
     */
    public static BufferedImage convertImage(RawImage rawImage) {
        return convertImage(rawImage, null);
    }

    static int getMask(int length) {
        int res = 0;
        for (int i = 0 ; i < length ; i++) {
            res = (res << 1) + 1;
        }

        return res;
    }

    private static BufferedImage rawImage32toARGB(RawImage rawImage) {
        // Do as much as we can to not make an extra copy of the data.  This is just a bunch of
        // classes that wrap's the raw byte array of the image data.
        DataBufferByte dataBuffer = new DataBufferByte(rawImage.data, rawImage.size);

        PixelInterleavedSampleModel sampleModel =
            new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, rawImage.width, rawImage.height,
                    4, rawImage.width * 4, BAND_OFFSETS_32);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer,
                new Point(0, 0));
        return new BufferedImage(new ThirtyTwoBitColorModel(rawImage), raster, false, EMPTY_HASH);
    }

    private static BufferedImage rawImage16toARGB(BufferedImage image, RawImage rawImage) {
        // Do as much as we can to not make an extra copy of the data.  This is just a bunch of
        // classes that wrap's the raw byte array of the image data.
        DataBufferByte dataBuffer = new DataBufferByte(rawImage.data, rawImage.size);

        PixelInterleavedSampleModel sampleModel =
            new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, rawImage.width, rawImage.height,
                    2, rawImage.width * 2, BAND_OFFSETS_16);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer,
                new Point(0, 0));
        return new BufferedImage(new SixteenBitColorModel(rawImage), raster, false, EMPTY_HASH);
    }
}
