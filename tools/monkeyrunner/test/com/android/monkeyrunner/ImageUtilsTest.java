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
import com.android.monkeyrunner.CaptureRawAndConvertedImage.MonkeyRunnerRawImage;

import junit.framework.TestCase;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import javax.imageio.ImageIO;

public class ImageUtilsTest extends TestCase {
    private static BufferedImage createBufferedImage(String name) throws IOException {
        InputStream is = ImageUtilsTest.class.getResourceAsStream(name);
        BufferedImage img =  ImageIO.read(is);
        is.close();
        return img;
    }

    private static RawImage createRawImage(String name) throws IOException, ClassNotFoundException {
        ObjectInputStream is =
            new ObjectInputStream(ImageUtilsTest.class.getResourceAsStream(name));
        CaptureRawAndConvertedImage.MonkeyRunnerRawImage wrapper = (MonkeyRunnerRawImage) is.readObject();
        is.close();
        return wrapper.toRawImage();
    }

    /**
     * Check that the two images will draw the same (ie. have the same pixels).  This is different
     * that BufferedImage.equals(), which also wants to check that they have the same ColorModel
     * and other parameters.
     *
     * @param i1 the first image
     * @param i2 the second image
     * @return true if both images will draw the same (ie. have same pixels).
     */
    private static boolean checkImagesHaveSamePixels(BufferedImage i1, BufferedImage i2) {
        if (i1.getWidth() != i2.getWidth()) {
            return false;
        }
        if (i1.getHeight() != i2.getHeight()) {
            return false;
        }

        for (int y = 0; y < i1.getHeight(); y++) {
            for (int x = 0; x < i1.getWidth(); x++) {
                int p1 = i1.getRGB(x, y);
                int p2 = i2.getRGB(x, y);
                if (p1 != p2) {
                    WritableRaster r1 = i1.getRaster();
                    WritableRaster r2 = i2.getRaster();
                    return false;
                }
            }
        }

        return true;
    }

    public void testImageConversionOld() throws IOException, ClassNotFoundException {
        RawImage rawImage = createRawImage("image1.raw");
        BufferedImage convertedImage = ImageUtils.convertImage(rawImage);
        BufferedImage correctConvertedImage = createBufferedImage("image1.png");

        assertTrue(checkImagesHaveSamePixels(convertedImage, correctConvertedImage));
    }

    public void testImageConversionNew() throws IOException, ClassNotFoundException {
        RawImage rawImage = createRawImage("image2.raw");
        BufferedImage convertedImage = ImageUtils.convertImage(rawImage);
        BufferedImage correctConvertedImage = createBufferedImage("image2.png");

        assertTrue(checkImagesHaveSamePixels(convertedImage, correctConvertedImage));
    }
}
