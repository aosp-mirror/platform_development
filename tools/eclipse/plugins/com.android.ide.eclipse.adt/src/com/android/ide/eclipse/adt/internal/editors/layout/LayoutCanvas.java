/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.internal.editors.layout;

import com.android.layoutlib.api.ILayoutResult;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;

/**
 * Displays the image rendered by the {@link GraphicalEditorPart} and handles
 * the interaction with the widgets.
 * <p/>
 *
 * @since GLE2
 *
 * TODO list:
 * - make sure it is scrollable (Canvas derives from Scrollable, so prolly just setting bounds.)
 * - handle selection (will need the model, aka the root node)/
 * - handle drop target (from palette)/
 * - handle drag'n'drop (internal, for moving/duplicating)/
 * - handle context menu (depending on selection)/
 * - selection synchronization with the outline (both ways)/
 * - preserve selection during editor input change if applicable (e.g. when changing configuration.)
 */
public class LayoutCanvas extends Canvas {

    private Image mImage;

    public LayoutCanvas(Composite parent, int style) {
        super(parent, style);

        addPaintListener(new PaintListener() {
            public void paintControl(PaintEvent e) {
                paint(e);
            }
        });
    }

    public void setResult(ILayoutResult result) {
        if (result.getSuccess() == ILayoutResult.SUCCESS) {
            setImage(result.getImage());
        }
    }

    private void setImage(BufferedImage awtImage) {
        // Convert the AWT image into an SWT image.
        int width = awtImage.getWidth();
        int height = awtImage.getHeight();

        Raster raster = awtImage.getData(new java.awt.Rectangle(width, height));
        int[] imageDataBuffer = ((DataBufferInt)raster.getDataBuffer()).getData();

        ImageData imageData = new ImageData(width, height, 32,
                new PaletteData(0x00FF0000, 0x0000FF00, 0x000000FF));

        imageData.setPixels(0, 0, imageDataBuffer.length, imageDataBuffer, 0);

        mImage = new Image(getDisplay(), imageData);

        redraw();
    }

    private void paint(PaintEvent e) {
        if (mImage != null) {
            GC gc = e.gc;
            gc.drawImage(mImage, 0, 0);
        }
    }

}
