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

import com.android.ide.eclipse.editors.uimodel.UiElementNode;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.XYLayout;
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
    }

    @Override
    protected IFigure createFigure() {
        Label f = new Label();
        f.setLayoutManager(new XYLayout());
        return f;
    }

    @Override
    protected void hideSelection() {
        IFigure f = getFigure();
        if (f instanceof Label) {
            f.setBorder(null);
        }
    }

    @Override
    protected void showSelection() {
        IFigure f = getFigure();
        if (f instanceof Label) {
            f.setBorder(new LineBorder(ColorConstants.red, 1));
        }
    }
    
    public void showDropTarget() {
        IFigure f = getFigure();
        if (f instanceof Label) {
            f.setBorder(new LineBorder(ColorConstants.blue, 1));
        }
    }

    public void hideDropTarget() {
        hideSelection();
    }
}
