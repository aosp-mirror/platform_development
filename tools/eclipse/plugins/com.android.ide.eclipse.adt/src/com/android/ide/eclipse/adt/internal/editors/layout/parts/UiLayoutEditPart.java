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

package com.android.ide.eclipse.adt.internal.editors.layout.parts;

import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ContainerEditPolicy;
import org.eclipse.gef.requests.CreateRequest;

/**
 * Graphical edit part for an {@link UiElementNode} that represents a ViewLayout.
 * <p/>
 * It acts as a simple container. 
 */
public final class UiLayoutEditPart extends UiElementEditPart {
    
    static class HighlightInfo {
        public boolean drawDropBorder;
        public UiElementEditPart[] childParts;
        public Point anchorPoint;
        public Point linePoints[];

        public final Point tempPoints[] = new Point[] { new Point(), new Point() };

        public void clear() {
            drawDropBorder = false;
            childParts = null;
            anchorPoint = null;
            linePoints = null;
        }
    }
    
    private final HighlightInfo mHighlightInfo = new HighlightInfo();
    
    public UiLayoutEditPart(UiElementNode uiElementNode) {
        super(uiElementNode);
    }
    
    @Override
    protected void createEditPolicies() {
        super.createEditPolicies();
        
        installEditPolicy(EditPolicy.CONTAINER_ROLE, new ContainerEditPolicy() {
            @Override
            protected Command getCreateCommand(CreateRequest request) {
                return null;
            }
        });
        
        installLayoutEditPolicy(this);
    }

    @Override
    protected IFigure createFigure() {
        IFigure f = new LayoutFigure();
        f.setLayoutManager(new XYLayout());
        return f;
    }

    @Override
    protected void showSelection() {
        IFigure f = getFigure();
        if (f instanceof ElementFigure) {
            ((ElementFigure) f).setSelected(true);
        }
    }

    @Override
    protected void hideSelection() {
        IFigure f = getFigure();
        if (f instanceof ElementFigure) {
            ((ElementFigure) f).setSelected(false);
        }
    }

    public void showDropTarget(Point where) {
        if (where != null) {
            mHighlightInfo.clear();
            mHighlightInfo.drawDropBorder = true;
            DropFeedback.computeDropFeedback(this, mHighlightInfo, where);

            IFigure f = getFigure();
            if (f instanceof LayoutFigure) {
                ((LayoutFigure) f).setHighlighInfo(mHighlightInfo);
            }
        }
    }

    public void hideDropTarget() {
        mHighlightInfo.clear();
        IFigure f = getFigure();
        if (f instanceof LayoutFigure) {
            ((LayoutFigure) f).setHighlighInfo(mHighlightInfo);
        }
    }
}
