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

import org.eclipse.gef.editparts.AbstractTreeEditPart;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * Base {@link AbstractTreeEditPart} to represent {@link UiElementNode} objects in the
 * {@link IContentOutlinePage} linked to the layout editor.
 *
 * @since GLE1
 */
public class UiElementTreeEditPart extends AbstractTreeEditPart {

    public UiElementTreeEditPart(UiElementNode uiElementNode) {
        setModel(uiElementNode);
    }

    @Override
    protected void createEditPolicies() {
        // TODO Auto-generated method stub
        super.createEditPolicies();
    }

    @Override
    protected Image getImage() {
        return getUiNode().getDescriptor().getIcon();
    }

    @Override
    protected String getText() {
        return getUiNode().getShortDescription();
    }

    @Override
    public void activate() {
        if (!isActive()) {
            super.activate();
            // TODO
        }
    }

    @Override
    public void deactivate() {
        if (isActive()) {
            super.deactivate();
            // TODO
        }
    }

    /**
     * Returns the casted model object represented by this {@link AbstractTreeEditPart}.
     */
    protected UiElementNode getUiNode() {
        return (UiElementNode)getModel();
    }

}
