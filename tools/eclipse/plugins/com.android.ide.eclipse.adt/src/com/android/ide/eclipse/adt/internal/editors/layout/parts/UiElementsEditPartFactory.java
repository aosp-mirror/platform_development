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

import com.android.ide.eclipse.adt.internal.editors.uimodel.UiDocumentNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.swt.widgets.Display;

/**
 * A factory that returns the appropriate {@link EditPart} for a given model object.
 * <p/>
 * The only model objects we use are {@link UiElementNode} objects and they are
 * edited using {@link UiElementEditPart}.
 *
 * @since GLE1
 */
public class UiElementsEditPartFactory implements EditPartFactory {

    private Display mDisplay;

    public UiElementsEditPartFactory(Display display) {
        mDisplay = display;
    }

    public EditPart createEditPart(EditPart context, Object model) {
        if (model instanceof UiDocumentNode) {
            return new UiDocumentEditPart((UiDocumentNode) model, mDisplay);
        } else if (model instanceof UiElementNode) {
            UiElementNode node = (UiElementNode) model;

            if (node.getDescriptor().hasChildren()) {
                return new UiLayoutEditPart(node);
            }

            return new UiViewEditPart(node);
        }
        return null;
    }

}
