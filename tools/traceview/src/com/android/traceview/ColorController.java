/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.traceview;

import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ColorController {
    private static final int[] systemColors = { SWT.COLOR_BLUE, SWT.COLOR_RED,
        SWT.COLOR_GREEN, SWT.COLOR_CYAN, SWT.COLOR_MAGENTA, SWT.COLOR_DARK_BLUE,
        SWT.COLOR_DARK_RED, SWT.COLOR_DARK_GREEN, SWT.COLOR_DARK_YELLOW,
        SWT.COLOR_DARK_CYAN, SWT.COLOR_DARK_MAGENTA, SWT.COLOR_BLACK };

    private static RGB[] rgbColors = { new RGB(90, 90, 255), // blue
            new RGB(0, 240, 0), // green
            new RGB(255, 0, 0), // red
            new RGB(0, 255, 255), // cyan
            new RGB(255, 80, 255), // magenta
            new RGB(200, 200, 0), // yellow
            new RGB(40, 0, 200), // dark blue
            new RGB(150, 255, 150), // light green
            new RGB(150, 0, 0), // dark red
            new RGB(30, 150, 150), // dark cyan
            new RGB(200, 200, 255), // light blue
            new RGB(0, 120, 0), // dark green
            new RGB(255, 150, 150), // light red
            new RGB(140, 80, 140), // dark magenta
            new RGB(150, 100, 50), // brown
            new RGB(70, 70, 70), // dark grey
    };

    private static HashMap<Integer, Color> colorCache = new HashMap<Integer, Color>();
    private static HashMap<Integer, Image> imageCache = new HashMap<Integer, Image>();

    public ColorController() {
    }

    public static Color requestColor(Display display, RGB rgb) {
        return requestColor(display, rgb.red, rgb.green, rgb.blue);
    }

    public static Image requestColorSquare(Display display, RGB rgb) {
        return requestColorSquare(display, rgb.red, rgb.green, rgb.blue);
    }

    public static Color requestColor(Display display, int red, int green, int blue) {
        int key = (red << 16) | (green << 8) | blue;
        Color color = colorCache.get(key);
        if (color == null) {
            color = new Color(display, red, green, blue);
            colorCache.put(key, color);
        }
        return color;
    }

    public static Image requestColorSquare(Display display, int red, int green, int blue) {
        int key = (red << 16) | (green << 8) | blue;
        Image image = imageCache.get(key);
        if (image == null) {
            image = new Image(display, 8, 14);
            GC gc = new GC(image);
            Color color = requestColor(display, red, green, blue);
            gc.setBackground(color);
            gc.fillRectangle(image.getBounds());
            gc.dispose();
            imageCache.put(key, image);
        }
        return image;
    }

    public static void assignMethodColors(Display display, MethodData[] methods) {
        int nextColorIndex = 0;
        for (MethodData md : methods) {
            RGB rgb = rgbColors[nextColorIndex];
            if (++nextColorIndex == rgbColors.length)
                nextColorIndex = 0;
            Color color = requestColor(display, rgb);
            Image image = requestColorSquare(display, rgb);
            md.setColor(color);
            md.setImage(image);

            // Compute and set a faded color
            int fadedRed = 150 + rgb.red / 4;
            int fadedGreen = 150 + rgb.green / 4;
            int fadedBlue = 150 + rgb.blue / 4;
            RGB faded = new RGB(fadedRed, fadedGreen, fadedBlue);
            color = requestColor(display, faded);
            image = requestColorSquare(display, faded);
            md.setFadedColor(color);
            md.setFadedImage(image);
        }
    }
}
