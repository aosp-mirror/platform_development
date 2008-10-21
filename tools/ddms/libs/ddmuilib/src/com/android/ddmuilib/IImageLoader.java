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

/**
 * Interface defining an image loader. jar app/lib and plugin have different packaging method
 * so each implementation will be different.
 * The implementation should implement at least one of the methods, and preferably both if possible.
 *
 */
public interface IImageLoader {

    /**
     * Load an image from the resource from a filename
     * @param filename
     * @param display
     */
    public Image loadImage(String filename, Display display);

    /**
     * Load an ImageDescriptor from the resource from a filename
     * @param filename
     * @param display
     */
    public ImageDescriptor loadDescriptor(String filename, Display display);

}
