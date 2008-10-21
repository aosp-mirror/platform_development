/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ddmuilib;

import com.android.ddmlib.Log;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class ImageHelper {

    /**
     * Loads an image from a resource. This method used a class to locate the
     * resources, and then load the filename from /images inside the resources.<br>
     * Extra parameters allows for creation of a replacement image of the
     * loading failed.
     *
     * @param loader the image loader used.
     * @param display the Display object
     * @param fileName the file name
     * @param width optional width to create replacement Image. If -1, null be
     *            be returned if the loading fails.
     * @param height optional height to create replacement Image. If -1, null be
     *            be returned if the loading fails.
     * @param phColor optional color to create replacement Image. If null, Blue
     *            color will be used.
     * @return a new Image or null if the loading failed and the optional
     *         replacement size was -1
     */
    public static Image loadImage(IImageLoader loader, Display display,
            String fileName, int width, int height, Color phColor) {

        Image img = null;
        if (loader != null) {
            img = loader.loadImage(fileName, display);
        }

        if (img == null) {
            Log.w("ddms", "Couldn't load " + fileName);
            // if we had the extra parameter to create replacement image then we
            // create and return it.
            if (width != -1 && height != -1) {
                return createPlaceHolderArt(display, width, height,
                        phColor != null ? phColor : display
                                .getSystemColor(SWT.COLOR_BLUE));
            }

            // otherwise, just return null
            return null;
        }

        return img;
    }

    /**
     * Create place-holder art with the specified color.
     */
    public static Image createPlaceHolderArt(Display display, int width,
            int height, Color color) {
        Image img = new Image(display, width, height);
        GC gc = new GC(img);
        gc.setForeground(color);
        gc.drawLine(0, 0, width, height);
        gc.drawLine(0, height - 1, width, -1);
        gc.dispose();
        return img;
    }

}
