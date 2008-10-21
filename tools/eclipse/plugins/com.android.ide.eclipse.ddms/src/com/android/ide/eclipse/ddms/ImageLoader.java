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
package com.android.ide.eclipse.ddms;

import com.android.ddmuilib.IImageLoader;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Implementation of the IImageLoader interface for the eclipse plugin.
 */
public class ImageLoader implements IImageLoader  {

    private URL mBaseUrl;

    public ImageLoader(Plugin plugin) {
        mBaseUrl = plugin.getBundle().getEntry("/"); // $NON-NLS-1$
    }

    /**
     * default method. only need a filename. the 2 interface methods call this one.
     * @param filename the filename of the image to load. The filename is searched for under /icons.
     * @return
     */
    public ImageDescriptor loadDescriptor(String filename) {
        try {
            URL newUrl = new URL(mBaseUrl, "/icons/" + filename); // $NON-NLS-1$
            return ImageDescriptor.createFromURL(newUrl);
        } catch (MalformedURLException e) {
            // we'll just return null;
        }
        return null;
    }

    public ImageDescriptor loadDescriptor(String filename, Display display) {
        return loadDescriptor(filename);
    }


    public Image loadImage(String filename, Display display) {
        ImageDescriptor descriptor = loadDescriptor(filename);
        if (descriptor !=null) {
            return descriptor.createImage();
        }
        return null;
    }

}
