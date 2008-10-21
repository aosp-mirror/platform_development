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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import java.io.InputStream;

/**
 * Image loader for an normal standalone app.
 */
public class ImageLoader implements IImageLoader {

    /** class used as reference to get the reources */
    private Class<?> mClass;

    /**
     * Creates a loader for a specific class. The class allows java to figure
     * out which .jar file to search for the image.
     *
     * @param theClass
     */
    public ImageLoader(Class<?> theClass) {
        mClass = theClass;
    }

    public ImageDescriptor loadDescriptor(String filename, Display display) {
        // we don't support ImageDescriptor
        return null;
    }

    public Image loadImage(String filename, Display display) {

        String tmp = "/images/" + filename;
        InputStream imageStream = mClass.getResourceAsStream(tmp);

        if (imageStream != null) {
            Image img = new Image(display, imageStream);
            if (img == null)
                throw new NullPointerException("couldn't load " + tmp);
            return img;
        }

        return null;
    }
}
