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

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;

    
/**
 * The figure used to draw basic elements.
 * <p/>
 * The figure is totally empty and transparent except for the selection border.
 */
class ElementFigure extends Figure {

    private boolean mIsSelected;
    private Rectangle mInnerBounds;

    public ElementFigure() {
        setOpaque(false);
    }
    
    public void setSelected(boolean isSelected) {
        if (isSelected != mIsSelected) {
            mIsSelected = isSelected;
            repaint();
        }
    }
    
    @Override
    public void setBounds(Rectangle rect) {
        super.setBounds(rect);
        
        mInnerBounds = getBounds().getCopy();
        if (mInnerBounds.width > 0) {
            mInnerBounds.width--;
        }
        if (mInnerBounds.height > 0) {
            mInnerBounds.height--;
        }
    }
    
    public Rectangle getInnerBounds() {
        return mInnerBounds;
    }
    
    @Override
    protected void paintBorder(Graphics graphics) {
        super.paintBorder(graphics);

        if (mIsSelected) {
            graphics.setLineWidth(1);
            graphics.setLineStyle(SWT.LINE_SOLID);
            graphics.setForegroundColor(ColorConstants.red);
            graphics.drawRectangle(getInnerBounds());
        }
    }
}
