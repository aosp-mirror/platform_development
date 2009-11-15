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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

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
 * - handle drop target (from palette).
 * - handle drag'n'drop (internal, for moving/duplicating).
 * - handle context menu (depending on selection).
 * - selection synchronization with the outline (both ways).
 */
public class LayoutCanvas extends Canvas {

    /**
     * Margin around the rendered image. Should be enough space to display the layout
     * width and height pseudo widgets.
     */
    private static final int IMAGE_MARGIN = 5;
    /**
     * Minimal size of the selection, in case an empty view or layout is selected.
     */
    private static final int SELECTION_MIN_SIZE = 6;

    private ILayoutResult mLastValidResult;
    private ViewInfo mLastValidViewInfoRoot;

    /** Current background image. Null when there's no image. */
    private Image mImage;

    private final LinkedList<Selection> mSelections = new LinkedList<Selection>();

    /** Selection border color. Do not dispose, it's a system color. */
    private Color mSelectionFgColor;

    /** Selection name font. Do not dispose, it's a system font. */
    private Font mSelectionFont;

    /** Pixel height of the font displaying the selection name. Initially set to 0 and only
     * initialized in onPaint() when we have a GC. */
    private int mSelectionFontHeight;

    /** Current hover view info. Null when no mouse hover. */
    private ViewInfo mHoverViewInfo;

    /** Current mouse hover border rectangle. Null when there's no mouse hover. */
    private Rectangle mHoverRect;

    /** Hover border color. Must be disposed, it's NOT a system color. */
    private Color mHoverFgColor;

    private AlternateSelection mAltSelection;

    /**
     * True when the last {@link #setResult(ILayoutResult)} provided a valid {@link ILayoutResult}
     * in which case it is also available in {@link #mLastValidResult}.
     * When false this means the canvas is displaying an out-dated result image & bounds and some
     * features should be disabled accordingly such a drag'n'drop.
     */
    private boolean mIsResultValid;


    public LayoutCanvas(Composite parent, int style) {
        super(parent, style | SWT.DOUBLE_BUFFERED);

        Display d = getDisplay();
        mSelectionFgColor = d.getSystemColor(SWT.COLOR_RED);
        mHoverFgColor     = new Color(d, 0xFF, 0x99, 0x00); // orange
        mSelectionFont    = d.getSystemFont();

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

    @Override
    public void dispose() {
        super.dispose();
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
            mLastValidViewInfoRoot = new ViewInfo(result.getRootView());
            setImage(result.getImage());

            // Check if the selection is still the same (based on the object keys)
            // and eventually recompute their bounds.
            for (ListIterator<Selection> it = mSelections.listIterator(); it.hasNext(); ) {
                Selection s = it.next();

                // Check the if the selected object still exists
                Object key = s.getViewInfo().getKey();
                ViewInfo vi = findViewInfoKey(key, mLastValidViewInfoRoot);

                // Remove the previous selection -- if the selected object still exists
                // we need to recompute its bounds in case it moved so we'll insert a new one
                // at the same place.
                it.remove();
                if (vi != null) {
                    it.add(new Selection(vi));
                }
            }

            // remove the current alternate selection views
            mAltSelection = null;
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

    /**
     * Paints the canvas in response to paint events.
     */
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

        for (Selection s : mSelections) {
            drawSelection(gc, s);
        }
    }

    private void drawSelection(GC gc, Selection s) {
        Rectangle r = s.getRect();

        gc.setForeground(mSelectionFgColor);
        gc.setLineStyle(SWT.LINE_SOLID);
        gc.drawRectangle(s.mRect);

        String name = s.getName();

        if (name != null) {
            int xs = r.x + 2;
            int ys = r.y - mSelectionFontHeight;
            if (ys < 0) {
                ys = r.y + r.height;
            }
            gc.drawString(name, xs, ys, true /*transparent*/);
        }
    }

    /**
     * Hover on top of a known child.
     */
    private void onMouseMove(MouseEvent e) {
        if (mLastValidResult != null) {
            ViewInfo root = mLastValidViewInfoRoot;
            ViewInfo vi = findViewInfoAt(e.x - IMAGE_MARGIN, e.y - IMAGE_MARGIN, root);

            // We don't hover on the root since it's not a widget per see and it is always there.
            if (vi == root) {
                vi = null;
            }

            boolean needsUpdate = vi != mHoverViewInfo;
            mHoverViewInfo = vi;

            if (vi == null) {
                mHoverRect = null;
            } else {
                Rectangle r = vi.getSelectionRect();
                mHoverRect = new Rectangle(r.x + IMAGE_MARGIN, r.y + IMAGE_MARGIN,
                                           r.width, r.height);
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
     * <p/>
     * Shift key is used to toggle in multi-selection.
     * Alt key is used to cycle selection through objects at the same level than the one
     * pointed at (i.e. click on an object then alt-click to cycle).
     */
    private void onMouseUp(MouseEvent e) {
        if (mLastValidResult != null) {

            boolean isShift = (e.stateMask & SWT.SHIFT) != 0;
            boolean isAlt   = (e.stateMask & SWT.ALT)   != 0;

            int x = e.x - IMAGE_MARGIN;
            int y = e.y - IMAGE_MARGIN;
            ViewInfo vi = findViewInfoAt(x, y, mLastValidViewInfoRoot);

            if (isShift && !isAlt) {
                // Case where shift is pressed: pointed object is toggled.

                // reset alternate selection if any
                mAltSelection = null;

                // If nothing has been found at the cursor, assume it might be a user error
                // and avoid clearing the existing selection.

                if (vi != null) {
                    // toggle this selection on-off: remove it if already selected
                    if (deselect(vi)) {
                        redraw();
                        return;
                    }

                    // otherwise add it.
                    mSelections.add(new Selection(vi));
                    redraw();
                }

            } else if (isAlt) {
                // Case where alt is pressed: select or cycle the object pointed at.

                // Note: if shift and alt are pressed, shift is ignored. The alternate selection
                // mechanism does not reset the current multiple selection unless they intersect.

                // We need to remember the "origin" of the alternate selection, to be
                // able to continue cycling through it later. If there's no alternate selection,
                // create one. If there's one but not for the same origin object, create a new
                // one too.
                if (mAltSelection == null || mAltSelection.getOriginatingView() != vi) {
                    mAltSelection = new AlternateSelection(vi, findAltViewInfoAt(
                                                    x, y, mLastValidViewInfoRoot, null));

                    // deselect them all, in case they were partially selected
                    deselectAll(mAltSelection.getAltViews());

                    // select the current one
                    ViewInfo vi2 = mAltSelection.getCurrent();
                    if (vi2 != null) {
                        mSelections.addFirst(new Selection(vi2));
                    }
                } else {
                    // We're trying to cycle through the current alternate selection.
                    // First remove the current object.
                    ViewInfo vi2 = mAltSelection.getCurrent();
                    deselect(vi2);

                    // Now select the next one.
                    vi2 = mAltSelection.getNext();
                    if (vi2 != null) {
                        mSelections.addFirst(new Selection(vi2));
                    }
                }
                redraw();

            } else {
                // Case where no modifier is pressed: either select or reset the selection.

                // reset alternate selection if any
                mAltSelection = null;

                // reset (multi)selection if any
                if (mSelections.size() > 0) {
                    if (mSelections.size() == 1 && mSelections.getFirst().getViewInfo() == vi) {
                        // Selection remains the same, don't touch it.
                        return;
                    }
                    mSelections.clear();
                }

                if (vi != null) {
                    mSelections.add(new Selection(vi));
                }
                redraw();
            }
        }
    }

    /** Deselects a view info. Returns true if the object was actually selected. */
    private boolean deselect(ViewInfo viewInfo) {
        if (viewInfo == null) {
            return false;
        }

        for (ListIterator<Selection> it = mSelections.listIterator(); it.hasNext(); ) {
            Selection s = it.next();
            if (viewInfo == s.getViewInfo()) {
                it.remove();
                return true;
            }
        }

        return false;
    }

    /** Deselects multiple view infos, */
    private void deselectAll(List<ViewInfo> viewInfos) {
        for (ListIterator<Selection> it = mSelections.listIterator(); it.hasNext(); ) {
            Selection s = it.next();
            if (viewInfos.contains(s.getViewInfo())) {
                it.remove();
            }
        }
    }

    private void onDoubleClick(MouseEvent e) {
        // pass, not used yet.
    }

    /**
     * Tries to find a child with the same view key in the view info sub-tree.
     * Returns null if not found.
     */
    private ViewInfo findViewInfoKey(Object viewKey, ViewInfo viewInfo) {
        if (viewInfo.getKey() == viewKey) {
            return viewInfo;
        }

        // try to find a matching child
        for (ViewInfo child : viewInfo.getChildren()) {
            ViewInfo v = findViewInfoKey(viewKey, child);
            if (v != null) {
                return v;
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
    private ViewInfo findViewInfoAt(int x, int y, ViewInfo viewInfo) {
        Rectangle r = viewInfo.getSelectionRect();
        if (r.contains(x, y)) {

            // try to find a matching child first
            for (ViewInfo child : viewInfo.getChildren()) {
                ViewInfo v = findViewInfoAt(x, y, child);
                if (v != null) {
                    return v;
                }
            }

            // if no children matched, this is the view that we're looking for
            return viewInfo;
        }

        return null;
    }

    private ArrayList<ViewInfo> findAltViewInfoAt(
            int x, int y, ViewInfo parent, ArrayList<ViewInfo> outList) {
        Rectangle r;

        if (outList == null) {
            outList = new ArrayList<ViewInfo>();

            // add the parent root only once
            r = parent.getSelectionRect();
            if (r.contains(x, y)) {
                outList.add(parent);
            }
        }

        if (parent.getChildren().size() > 0) {
            // then add all children that match the position
            for (ViewInfo child : parent.getChildren()) {
                r = child.getSelectionRect();
                if (r.contains(x, y)) {
                    outList.add(child);
                }
            }

            // finally recurse in the children
            for (ViewInfo child : parent.getChildren()) {
                r = child.getSelectionRect();
                if (r.contains(x, y)) {
                    findAltViewInfoAt(x, y, child, outList);
                }
            }
        }

        return outList;
    }

    /**
     * Maps a {@link ILayoutViewInfo} in a structure more adapted to our needs.
     * The only large difference is that we keep both the original bounds of the view info
     * and we pre-compute the selection bounds which are absolute to the rendered image (where
     * as the original bounds are relative to the parent view.)
     * <p/>
     * Each view also know its parent, which should be handy later.
     * <p/>
     * We can't alter {@link ILayoutViewInfo} as it is part of the LayoutBridge and needs to
     * have a fixed API.
     */
    private static class ViewInfo {
        private final Rectangle mRealRect;
        private final Rectangle mSelectionRect;
        private final String mName;
        private final Object mKey;
        private final ViewInfo mParent;
        private final ArrayList<ViewInfo> mChildren = new ArrayList<ViewInfo>();

        /**
         * Constructs a {@link ViewInfo} hierarchy based on a given {@link ILayoutViewInfo}
         * hierarchy. This call is recursives and builds a full tree.
         *
         * @param viewInfo The root of the {@link ILayoutViewInfo} hierarchy.
         */
        public ViewInfo(ILayoutViewInfo viewInfo) {
            this(viewInfo, null /*parent*/, 0 /*parentX*/, 0 /*parentY*/);
        }

        private ViewInfo(ILayoutViewInfo viewInfo, ViewInfo parent, int parentX, int parentY) {
            mParent = parent;
            mKey  = viewInfo.getViewKey();
            mName = viewInfo.getName();

            int x = viewInfo.getLeft();
            int y = viewInfo.getTop();
            int w = viewInfo.getRight() - x;
            int h = viewInfo.getBottom() - y;

            mRealRect = new Rectangle(x, y, w, h);

            if (parent != null) {
                x += parentX;
                y += parentY;
            }

            if (viewInfo.getChildren() != null) {
                for (ILayoutViewInfo child : viewInfo.getChildren()) {
                    mChildren.add(new ViewInfo(child, this, x, y));
                }
            }

            // adjust selection bounds for views which are too small to select

            if (w < SELECTION_MIN_SIZE) {
                int d = (SELECTION_MIN_SIZE - w) / 2;
                x -= d;
                w += SELECTION_MIN_SIZE - w;
            }

            if (h < SELECTION_MIN_SIZE) {
                int d = (SELECTION_MIN_SIZE - h) / 2;
                y -= d;
                h += SELECTION_MIN_SIZE - h;
            }

            mSelectionRect = new Rectangle(x, y, w - 1, h - 1);
        }

        /** Returns the original {@link ILayoutResult} bounds, relative to the parent. */
        public Rectangle getRealRect() {
            return mRealRect;
        }

        /*
        * Returns the absolute selection bounds of the view info as a rectangle.
        * The selection bounds will always have a size greater or equal to
        * {@link #SELECTION_MIN_SIZE}.
        * The width/height is inclusive (i.e. width = right-left-1).
        * This is in absolute "screen" coordinates (relative to the rendered bitmap).
        */
        public Rectangle getSelectionRect() {
            return mSelectionRect;
        }

        /**
         * Returns the view key. Could be null, although unlikely.
         * @see ILayoutViewInfo#getViewKey()
         */
        public Object getKey() {
            return mKey;
        }

        /**
         * Returns the parent {@link ViewInfo}.
         * It is null for the root and non-null for children.
         */
        public ViewInfo getParent() {
            return mParent;
        }

        /**
         * Returns the list of children of this {@link ViewInfo}.
         * The list is never null. It can be empty.
         * By contract, this.getChildren().get(0..n-1).getParent() == this.
         */
        public ArrayList<ViewInfo> getChildren() {
            return mChildren;
        }

        /**
         * Returns the name of the {@link ViewInfo}.
         * Could be null, although unlikely.
         * Experience shows this is the full qualified Java name of the View.
         * @see ILayoutViewInfo#getName()
         */
        public String getName() {
            return mName;
        }
    }

    /**
     * Represents one selection.
     */
    private static class Selection {
        /** Current selected view info. Cannot be null. */
        private final ViewInfo mViewInfo;

        /** Current selection border rectangle. Cannot be null. */
        private final Rectangle mRect;

        /** The name displayed over the selection, typically the widget class name. Can be null. */
        private final String mName;

        /**
         * Creates a new {@link Selection} object.
         * @param viewInfo The view info being selected. Must not be null.
         */
        public Selection(ViewInfo viewInfo) {

            assert viewInfo != null;

            mViewInfo = viewInfo;

            if (viewInfo == null) {
                mRect = null;
            } else {
                Rectangle r = viewInfo.getSelectionRect();
                mRect = new Rectangle(r.x + IMAGE_MARGIN, r.y + IMAGE_MARGIN, r.width, r.height);
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

            mName = name;
        }

        /**
         * Returns the selected view info. Cannot be null.
         */
        public ViewInfo getViewInfo() {
            return mViewInfo;
        }

        /**
         * Returns the selection border rectangle.
         * Cannot be null.
         */
        public Rectangle getRect() {
            return mRect;
        }

        /**
         * The name displayed over the selection, typically the widget class name.
         * Can be null.
         */
        public String getName() {
            return mName;
        }
    }

    /**
     * Information for the current alternate selection, i.e. the possible selected items
     * that are located at the same x/y as the original view, either sibling or parents.
     */
    private static class AlternateSelection {
        private final ViewInfo mOriginatingView;
        private final List<ViewInfo> mAltViews;
        private int mIndex;

        /**
         * Creates a new alternate selection based on the given originating view and the
         * given list of alternate views. Both cannot be null.
         */
        public AlternateSelection(ViewInfo originatingView, List<ViewInfo> altViews) {
            assert originatingView != null;
            assert altViews != null;
            mOriginatingView = originatingView;
            mAltViews = altViews;
            mIndex = altViews.size() - 1;
        }

        /** Returns the list of alternate views. Cannot be null. */
        public List<ViewInfo> getAltViews() {
            return mAltViews;
        }

        /** Returns the originating view. Cannot be null. */
        public ViewInfo getOriginatingView() {
            return mOriginatingView;
        }

        /**
         * Returns the current alternate view to select.
         * Initially this is the top-most view.
         */
        public ViewInfo getCurrent() {
            return mIndex >= 0 ? mAltViews.get(mIndex) : null;
        }

        /**
         * Changes the current view to be the next one and then returns it.
         * This loops through the alternate views.
         */
        public ViewInfo getNext() {
            if (mIndex == 0) {
                mIndex = mAltViews.size() - 1;
            } else if (mIndex > 0) {
                mIndex--;
            }

            return getCurrent();
        }
    }


}
