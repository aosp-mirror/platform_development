/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.ide.eclipse.editors.layout.parts;

import com.android.ide.eclipse.editors.layout.parts.UiLayoutEditPart.HighlightInfo;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;

/**
 * The figure used to draw the feedback on a layout.
 * <p/>
 * By default the figure is transparent and empty.
 * The base {@link ElementFigure} knows how to draw the selection border.
 * This figure knows how to draw the drop feedback.
 */
class LayoutFigure extends ElementFigure {

    private HighlightInfo mHighlightInfo;
    
    public LayoutFigure() {
        super();
    }

    public void setHighlighInfo(HighlightInfo highlightInfo) {
        mHighlightInfo = highlightInfo;
        repaint();
    }

    /**
     * Paints the "border" for this figure.
     * <p/>
     * The parent {@link Figure#paint(Graphics)} calls {@link #paintFigure(Graphics)} then
     * {@link #paintClientArea(Graphics)} then {@link #paintBorder(Graphics)}. Here we thus
     * draw the actual highlight border but also the highlight anchor lines and points so that
     * we can make sure they are all drawn on top of the border. 
     * <p/>
     * Note: This method doesn't really need to restore its graphic state. The parent
     * Figure will do it for us.
     * <p/>
     * 
     * @param graphics The Graphics object used for painting
     */
    @Override
    protected void paintBorder(Graphics graphics) {
        super.paintBorder(graphics);

        if (mHighlightInfo == null) {
            return;
        }

        // Draw the border. We want other highlighting to be drawn on top of the border.
        if (mHighlightInfo.drawDropBorder) {
            graphics.setLineWidth(3);
            graphics.setLineStyle(SWT.LINE_SOLID);
            graphics.setForegroundColor(ColorConstants.green);
            graphics.drawRectangle(getInnerBounds().getCopy().shrink(1, 1));
        }

        Rectangle bounds = getBounds();
        int bx = bounds.x;
        int by = bounds.y;
        int w = bounds.width;
        int h = bounds.height;

        // Draw frames of target child parts, if any
        if (mHighlightInfo.childParts != null) {
            graphics.setLineWidth(2);
            graphics.setLineStyle(SWT.LINE_DOT);
            graphics.setForegroundColor(ColorConstants.lightBlue);
            for (UiElementEditPart part : mHighlightInfo.childParts) {
                if (part != null) {
                    graphics.drawRectangle(part.getBounds().getCopy().translate(bx, by));
                }
            }
        }

        // Draw the target line, if any
        if (mHighlightInfo.linePoints != null) {
            int x1 = mHighlightInfo.linePoints[0].x;
            int y1 = mHighlightInfo.linePoints[0].y;
            int x2 = mHighlightInfo.linePoints[1].x;
            int y2 = mHighlightInfo.linePoints[1].y;
            
            // if the line is right to the edge, draw it one pixel more inside so that the
            // full 2-pixel width be visible.
            if (x1 <= 0) x1++;
            if (x2 <= 0) x2++;
            if (y1 <= 0) y1++;
            if (y2 <= 0) y2++;

            if (x1 >= w - 1) x1--;
            if (x2 >= w - 1) x2--;
            if (y1 >= h - 1) y1--;
            if (y2 >= h - 1) y2--;
            
            x1 += bx;
            x2 += bx;
            y1 += by;
            y2 += by;
            
            graphics.setLineWidth(2);
            graphics.setLineStyle(SWT.LINE_DASH);
            graphics.setLineCap(SWT.CAP_ROUND);
            graphics.setForegroundColor(ColorConstants.orange);
            graphics.drawLine(x1, y1, x2, y2);
        }

        // Draw the anchor point, if any
        if (mHighlightInfo.anchorPoint != null) {
            int x = mHighlightInfo.anchorPoint.x;
            int y = mHighlightInfo.anchorPoint.y;

            // If the point is right on the edge, draw it one pixel inside so that it
            // matches the highlight line. It makes it slightly more visible that way.
            if (x <= 0) x++;
            if (y <= 0) y++;
            if (x >= w - 1) x--;
            if (y >= h - 1) y--;
            x += bx;
            y += by;

            graphics.setLineWidth(2);
            graphics.setLineStyle(SWT.LINE_SOLID);
            graphics.setLineCap(SWT.CAP_ROUND);
            graphics.setForegroundColor(ColorConstants.orange);
            graphics.drawLine(x-5, y-5, x+5, y+5);
            graphics.drawLine(x-5, y+5, x+5, y-5);
            // 7 * cos(45) == 5 so we use 8 for the circle radius (it looks slightly better than 7)
            graphics.setLineWidth(1);
            graphics.drawOval(x-8, y-8, 16, 16);
        }
    }
}
