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

import com.android.ide.eclipse.adt.internal.editors.uimodel.UiDocumentNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;
import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.ui.parts.SelectionSynchronizer;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

/**
 * Graphical layout editor part, version 2.
 *
 * @since GLE2
 */
public class GLE2 extends EditorPart implements IGraphicalLayoutEditor {

    /*
     * Useful notes:
     * To understand Drag'n'drop:
     *   http://www.eclipse.org/articles/Article-Workbench-DND/drag_drop.html
     */

    /** Reference to the layout editor */
    private final LayoutEditor mLayoutEditor;

    public GLE2(LayoutEditor layoutEditor) {
        mLayoutEditor = layoutEditor;
        setPartName("Graphical Layout");
    }

    // ------------------------------------
    // Methods overridden from base classes
    //------------------------------------

    /**
     * Initializes the editor part with a site and input.
     * {@inheritDoc}
     */
    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        // TODO Auto-generated method stub

    }

    @Override
    public void doSaveAs() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isDirty() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void createPartControl(Composite parent) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setFocus() {
        // TODO Auto-generated method stub

    }

    public void activated() {
        // TODO Auto-generated method stub

    }

    public void deactivated() {
        // TODO Auto-generated method stub

    }

    public void editNewFile(FolderConfiguration configuration) {
        // TODO Auto-generated method stub

    }

    public Clipboard getClipboard() {
        // TODO Auto-generated method stub
        return null;
    }

    public LayoutEditor getLayoutEditor() {
        // TODO Auto-generated method stub
        return null;
    }

    public UiDocumentNode getModel() {
        // TODO Auto-generated method stub
        return null;
    }

    public SelectionSynchronizer getSelectionSynchronizer() {
        // TODO Auto-generated method stub
        return null;
    }

    public void onXmlModelChanged() {
        // TODO Auto-generated method stub

    }

    public void recomputeLayout() {
        // TODO Auto-generated method stub

    }

    public void reloadEditor() {
        // TODO Auto-generated method stub

    }

    public void reloadPalette() {
        // TODO Auto-generated method stub

    }

    public void selectModel(UiElementNode uiNodeModel) {
        // TODO Auto-generated method stub

    }

    public void reloadLayout(boolean codeChange, boolean rChange,
            boolean resChange) {
        // TODO Auto-generated method stub

    }

}
