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
import com.android.layoutlib.api.ILayoutResult.ILayoutViewInfo;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

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
 * - gray on error, keep select but disable d'n'd.
 * - make sure it is scrollable (Canvas derives from Scrollable, so prolly just setting bounds.)
 * - handle selection (will need the model, aka the root node).
 * - handle drop target (from palette).
 * - handle drag'n'drop (internal, for moving/duplicating).
 * - handle context menu (depending on selection).
 * - selection synchronization with the outline (both ways).
 * - preserve selection during editor input change if applicable (e.g. when changing configuration.)
 */
public class LayoutCanvas extends Canvas {

    private static final int IMAGE_MARGIN = 5;
    private static final int SELECTION_MARGIN = 2;

    private ILayoutResult mLastValidResult;

    /** Current background image. Null when there's no image. */
    private Image mImage;

    /** Current selected view info. Null when none is selected. */
    private ILayoutViewInfo mSelectionViewInfo;
    /** Current selection border rectangle. Null when there's no selection. */
    private Rectangle mSelectionRect;
    /** The name displayed over the selection, typically the widget class name. */
    private String mSelectionName;
    /** Selection border color. Do not dispose, it's a system color. */
    private Color mSelectionFgColor;
    /** Selection name font. Do not dispose, it's a system font. */
    private Font mSelectionFont;
    /** Pixel height of the font displaying the selection name. Initially set to 0 and only
     * initialized in onPaint() when we have a GC. */
    private int mSelectionFontHeight;

    /** Current hover view info. Null when no mouse hover. */
    private ILayoutViewInfo mHoverViewInfo;
    /** Current mouse hover border rectangle. Null when there's no mouse hover. */
    private Rectangle mHoverRect;
    /** Hover border color. Do not dispose, it's a system color. */
    private Color mHoverFgColor;

    private boolean mIsResultValid;



    public LayoutCanvas(Composite parent, int style) {
        super(parent, style | SWT.DOUBLE_BUFFERED);

        Display d = getDisplay();
        mSelectionFgColor = d.getSystemColor(SWT.COLOR_RED);
        mHoverFgColor = mSelectionFgColor;

        mSelectionFont = d.getSystemFont();

        addPaintListener(new PaintListener() {
            public void paintControl(PaintEvent e) {
                onPaint(e);
            }
        });

        addMouseMoveListener(new MouseMoveListener() {
            public void mouseMove(MouseEvent e) {
                onMouseMove(e);
            }
        });

        addMouseListener(new MouseListener() {
            public void mouseUp(MouseEvent e) {
                onMouseUp(e);
            }

            public void mouseDown(MouseEvent e) {
                onMouseDown(e);
            }

            public void mouseDoubleClick(MouseEvent e) {
                onDoubleClick(e);
            }
        });
    }

    /**
     * Sets the result of the layout rendering. The result object indicates if the layout
     * rendering succeeded. If it did, it contains a bitmap and the objects rectangles.
     *
     * Implementation detail: the bridge's computeLayout() method already returns a newly
     * allocated ILayourResult. That means we can keep this result and hold on to it
     * when it is valid.
     *
     * @param result The new rendering result, either valid or not.
     */
    public void setResult(ILayoutResult result) {

        // disable any hover
        mHoverRect = null;

        mIsResultValid = (result != null && result.getSuccess() == ILayoutResult.SUCCESS);

        if (mIsResultValid && result != null) {
            mLastValidResult = result;
            setImage(result.getImage());

            // Check if the selection is still the same (based on its key)
            // and eventually recompute its bounds.
            if (mSelectionViewInfo != null) {
                ILayoutViewInfo vi = findViewInfoKey(
                        mSelectionViewInfo.getViewKey(),
                        result.getRootView());
                setSelection(vi);
            }
        }

        redraw();
    }

    //---

    /**
     * Sets the image of the last *successful* rendering.
     * Converts the AWT image into an SWT image.
     */
    private void setImage(BufferedImage awtImage) {
        int width = awtImage.getWidth();
        int height = awtImage.getHeight();

        Raster raster = awtImage.getData(new java.awt.Rectangle(width, height));
        int[] imageDataBuffer = ((DataBufferInt)raster.getDataBuffer()).getData();

        ImageData imageData = new ImageData(width, height, 32,
                new PaletteData(0x00FF0000, 0x0000FF00, 0x000000FF));

        imageData.setPixels(0, 0, imageDataBuffer.length, imageDataBuffer, 0);

        mImage = new Image(getDisplay(), imageData);
    }

    private void onPaint(PaintEvent e) {
        GC gc = e.gc;

        if (mImage != null) {
            if (!mIsResultValid) {
                gc.setAlpha(128);
            }

            gc.drawImage(mImage, IMAGE_MARGIN, IMAGE_MARGIN);

            if (!mIsResultValid) {
                gc.setAlpha(255);
            }
        }

        if (mHoverRect != null) {
            gc.setForeground(mHoverFgColor);
            gc.setLineStyle(SWT.LINE_DOT);
            gc.drawRectangle(mHoverRect);
        }

        // initialize the selection font height once. We need the GC to do that.
        if (mSelectionFontHeight == 0) {
            gc.setFont(mSelectionFont);
            FontMetrics fm = gc.getFontMetrics();
            mSelectionFontHeight = fm.getHeight();
        }

        if (mSelectionRect != null) {
            gc.setForeground(mSelectionFgColor);
            gc.setLineStyle(SWT.LINE_SOLID);
            gc.drawRectangle(mSelectionRect);

            if (mSelectionName != null) {
                int x = mSelectionRect.x + 2;
                int y = mSelectionRect.y - mSelectionFontHeight;
                if (y < 0) {
                    y = mSelectionRect.y + mSelectionRect.height;
                }
                gc.drawString(mSelectionName, x, y, true /*transparent*/);
            }
        }
    }

    /**
     * Hover on top of a known child.
     */
    private void onMouseMove(MouseEvent e) {
        if (mLastValidResult != null) {
            ILayoutViewInfo root = mLastValidResult.getRootView();
            ILayoutViewInfo vi = findViewInfoAt(e.x - IMAGE_MARGIN, e.y - IMAGE_MARGIN, root);

            // We don't hover on the root since it's not a widget per see and it is always there.
            if (vi == root) {
                vi = null;
            }

            boolean needsUpdate = vi != mHoverViewInfo;
            mHoverViewInfo = vi;

            mHoverRect = vi == null ? null : getViewInfoRect(vi);
            if (mHoverRect != null) {
                mHoverRect.x += IMAGE_MARGIN;
                mHoverRect.y += IMAGE_MARGIN;
            }

            if (needsUpdate) {
                redraw();
            }
        }
    }

    private void onMouseDown(MouseEvent e) {
        // pass, not used yet.
    }

    /**
     * Performs selection on mouse up (not mouse down).
     */
    private void onMouseUp(MouseEvent e) {
        if (mLastValidResult != null) {
            ILayoutViewInfo vi = findViewInfoAt(e.x - IMAGE_MARGIN, e.y - IMAGE_MARGIN,
                    mLastValidResult.getRootView());
            setSelection(vi);
        }
    }

    private void onDoubleClick(MouseEvent e) {
        // pass, not used yet.
    }

    private void setSelection(ILayoutViewInfo viewInfo) {
        boolean needsUpdate = viewInfo != mSelectionViewInfo;
        mSelectionViewInfo = viewInfo;

        mSelectionRect = viewInfo == null ? null : getViewInfoRect(viewInfo);
        if (mSelectionRect != null) {
            mSelectionRect.x += IMAGE_MARGIN;
            mSelectionRect.y += IMAGE_MARGIN;
        }

        String name = viewInfo == null ? null : viewInfo.getName();
        if (name != null) {
            // The name is typically a fully-qualified class name. Let's make it a tad shorter.

            if (name.startsWith("android.")) {                                      // $NON-NLS-1$
                // For android classes, convert android.foo.Name to android...Name
                int first = name.indexOf('.');
                int last = name.lastIndexOf('.');
                if (last > first) {
                    name = name.substring(0, first) + ".." + name.substring(last);   // $NON-NLS-1$
                }
            } else {
                // For custom non-android classes, it's best to keep the 2 first segments of
                // the namespace, e.g. we want to get something like com.example...MyClass
                int first = name.indexOf('.');
                first = name.indexOf('.', first + 1);
                int last = name.lastIndexOf('.');
                if (last > first) {
                    name = name.substring(0, first) + ".." + name.substring(last);   // $NON-NLS-1$
                }
            }
        }
        mSelectionName = name;

        if (needsUpdate) {
            redraw();
        }
    }

    /**
     * Tries to find a child with the same view key in the view info sub-tree.
     * Returns null if not found.
     */
    private ILayoutViewInfo findViewInfoKey(Object viewKey, ILayoutViewInfo viewInfo) {
        if (viewInfo.getViewKey() == viewKey) {
            return viewInfo;
        }

        // try to find a matching child
        if (viewInfo.getChildren() != null) {
            for (ILayoutViewInfo child : viewInfo.getChildren()) {
                ILayoutViewInfo v = findViewInfoKey(viewKey, child);
                if (v != null) {
                    return v;
                }
            }
        }

        return null;
    }

    /**
     * Tries to find the inner most child matching the given x,y coordinates in the view
     * info sub-tree. This uses the potentially-expanded selection bounds.
     *
     * Returns null if not found.
     */
    private ILayoutViewInfo findViewInfoAt(int x, int y, ILayoutViewInfo viewInfo) {
        Rectangle r = getViewInfoRect(viewInfo);
        if (r.contains(x, y)) {

            // try to find a matching child first
            if (viewInfo.getChildren() != null) {
                for (ILayoutViewInfo child : viewInfo.getChildren()) {
                    ILayoutViewInfo v = findViewInfoAt(x, y, child);
                    if (v != null) {
                        return v;
                    }
                }
            }

            // if no children matched, this is the view that we're looking for
            return viewInfo;
        }

        return null;
    }

    /**
     * Returns the bounds of the view info as a rectangle.
     * In case the view has a null width or null height, it is expanded using
     * {@link #SELECTION_MARGIN}.
     */
    private Rectangle getViewInfoRect(ILayoutViewInfo viewInfo) {
        int x = viewInfo.getLeft();
        int y = viewInfo.getTop();
        int w = viewInfo.getRight() - x;
        int h = viewInfo.getBottom() - y;

        if (w == 0) {
            x -= SELECTION_MARGIN;
            w += 2 * SELECTION_MARGIN;
        }
        if (h == 0) {
            y -= SELECTION_MARGIN;
            h += 2* SELECTION_MARGIN;
        }

        return new Rectangle(x, y, w, h);
    }
}
