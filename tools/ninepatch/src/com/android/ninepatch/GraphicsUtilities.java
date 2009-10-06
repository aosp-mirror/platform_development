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

package com.android.ninepatch;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics;
import java.awt.Transparency;
import java.net.URL;
import java.io.IOException;
import java.io.InputStream;

public class GraphicsUtilities {
    public static BufferedImage loadCompatibleImage(URL resource) throws IOException {
        BufferedImage image = ImageIO.read(resource);
        return toCompatibleImage(image);
    }

    public static BufferedImage loadCompatibleImage(InputStream stream) throws IOException {
        BufferedImage image = ImageIO.read(stream);
        return toCompatibleImage(image);
    }

    public static BufferedImage createCompatibleImage(int width, int height) {
        return getGraphicsConfiguration().createCompatibleImage(width, height);
    }

    public static BufferedImage toCompatibleImage(BufferedImage image) {
        if (isHeadless()) {
            return image;
        }

        if (image.getColorModel().equals(getGraphicsConfiguration().getColorModel())) {
            return image;
        }

        BufferedImage compatibleImage = getGraphicsConfiguration().createCompatibleImage(
                    image.getWidth(), image.getHeight(), image.getTransparency());
        Graphics g = compatibleImage.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return compatibleImage;
    }

    public static BufferedImage createCompatibleImage(BufferedImage image, int width, int height) {
        return getGraphicsConfiguration().createCompatibleImage(width, height,
                                                   image.getTransparency());
    }

    private static GraphicsConfiguration getGraphicsConfiguration() {
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        return environment.getDefaultScreenDevice().getDefaultConfiguration();
    }

    private static boolean isHeadless() {
        return GraphicsEnvironment.isHeadless();
    }

    public static BufferedImage createTranslucentCompatibleImage(int width, int height) {
        return getGraphicsConfiguration().createCompatibleImage(width, height,
                Transparency.TRANSLUCENT);
    }

    public static int[] getPixels(BufferedImage img, int x, int y, int w, int h, int[] pixels) {
        if (w == 0 || h == 0) {
            return new int[0];
        }

        if (pixels == null) {
            pixels = new int[w * h];
        } else if (pixels.length < w * h) {
            throw new IllegalArgumentException("Pixels array must have a length >= w * h");
        }

        int imageType = img.getType();
        if (imageType == BufferedImage.TYPE_INT_ARGB || imageType == BufferedImage.TYPE_INT_RGB) {
            Raster raster = img.getRaster();
            return (int[]) raster.getDataElements(x, y, w, h, pixels);
        }

        // Unmanages the image
        return img.getRGB(x, y, w, h, pixels, 0, w);
    }
}
