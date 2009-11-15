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

import com.android.ide.eclipse.adt.internal.editors.AndroidEditor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.layout.LayoutEditor;
import com.android.ide.eclipse.adt.internal.editors.layout.LayoutEditor.UiEditorActions;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.commands.Command;

/**
 * A command that knows how to instantiate a new element based on a given {@link ElementDescriptor},
 * the parent {@link UiElementEditPart} and an optional target location.
 *
 * @since GLE1
 */
public class ElementCreateCommand extends Command {

    /** Descriptor of the new element to create */
    private final ElementDescriptor mDescriptor;
    /** The edit part that hosts the new edit part */
    private final UiElementEditPart mParentPart;
    /** The drop location in parent coordinates */
    private final Point mTargetPoint;

    /**
     * Creates a new {@link ElementCreateCommand}.
     *
     * @param descriptor Descriptor of the new element to create
     * @param targetPart The edit part that hosts the new edit part
     * @param targetPoint The drop location in parent coordinates
     */
    public ElementCreateCommand(ElementDescriptor descriptor,
            UiElementEditPart targetPart, Point targetPoint) {
                mDescriptor = descriptor;
                mParentPart = targetPart;
                mTargetPoint = targetPoint;
    }

    // --- Methods inherited from Command ---

    @Override
    public boolean canExecute() {
        return mDescriptor != null &&
            mParentPart != null &&
            mParentPart.getUiNode() != null &&
            mParentPart.getUiNode().getEditor() instanceof LayoutEditor;
    }

    @Override
    public void execute() {
        super.execute();
        UiElementNode uiParent = mParentPart.getUiNode();
        if (uiParent != null) {
            final AndroidEditor editor = uiParent.getEditor();
            if (editor instanceof LayoutEditor) {
                ((LayoutEditor) editor).wrapUndoRecording(
                        String.format("Create %1$s", mDescriptor.getXmlLocalName()),
                        new Runnable() {
                    public void run() {
                        UiEditorActions actions = ((LayoutEditor) editor).getUiEditorActions();
                        if (actions != null) {
                            DropFeedback.addElementToXml(mParentPart, mDescriptor, mTargetPoint,
                                    actions);
                        }
                    }
                });
            }
        }
    }

    @Override
    public void redo() {
        throw new UnsupportedOperationException("redo not supported by this command"); //$NON-NLS-1$
    }

    @Override
    public void undo() {
        throw new UnsupportedOperationException("undo not supported by this command"); //$NON-NLS-1$
    }

}
