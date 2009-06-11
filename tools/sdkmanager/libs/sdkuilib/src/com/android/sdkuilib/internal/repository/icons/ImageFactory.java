/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.sdkuilib.internal.repository.icons;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;


/**
 * An utility class to serve {@link Image} correspond to the various icons
 * present in this package and dispose of them correctly at the end.
 */
public class ImageFactory {

    private final Display mDisplay;
    private final HashMap<String, Image> mImages = new HashMap<String, Image>();

    public ImageFactory(Display display) {
        mDisplay = display;
    }

    /**
     * Loads an image given its filename (with its extension).
     * Might return null if the image cannot be loaded.
     */
    public Image getImage(String imageName) {

        Image image = mImages.get(imageName);
        if (image != null) {
            return image;
        }

        InputStream stream = getClass().getResourceAsStream(imageName);
        if (stream != null) {
            try {
                ImageData imgData = new ImageData(stream);
                image = new Image(mDisplay, imgData, imgData.getTransparencyMask());
            } catch (SWTException e) {
                // ignore
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        // Store the image in the hash, even if this failed. If it fails now, it will fail later.
        mImages.put(imageName, image);

        return image;
    }

    /**
     * Dispose all the images created by this factory so far.
     */
    public void dispose() {
        Iterator<Image> it = mImages.values().iterator();
        while(it.hasNext()) {
            Image img = it.next();
            if (img != null) {
                img.dispose();
            }
            it.remove();
        }
    }

}
