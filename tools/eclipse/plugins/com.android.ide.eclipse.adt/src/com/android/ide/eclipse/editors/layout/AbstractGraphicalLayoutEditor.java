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

package com.android.ide.eclipse.editors.layout;

import com.android.ide.eclipse.editors.layout.LayoutReloadMonitor.ILayoutReloadListener;
import com.android.ide.eclipse.editors.layout.parts.ElementCreateCommand;
import com.android.ide.eclipse.editors.resources.configurations.FolderConfiguration;
import com.android.ide.eclipse.editors.uimodel.UiDocumentNode;
import com.android.ide.eclipse.editors.uimodel.UiElementNode;

import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.ui.parts.GraphicalEditorWithPalette;
import org.eclipse.gef.ui.parts.SelectionSynchronizer;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Abstract GraphicalLayoutEditor.
 */
/*package*/ abstract class AbstractGraphicalLayoutEditor extends GraphicalEditorWithPalette
    implements IWorkbenchPart, ILayoutReloadListener {

    /**
     * Sets the UI for the edition of a new file.
     * @param configuration the configuration of the new file.
     */
    abstract void editNewFile(FolderConfiguration configuration);

    /**
     * Reloads this editor, by getting the new model from the {@link LayoutEditor}.
     */
    abstract void reloadEditor();

    /**
     * Callback for XML model changed. Only update/recompute the layout if the editor is visible
     */
    abstract void onXmlModelChanged();

    /**
     * Responds to a page change that made the Graphical editor page the activated page.
     */
    abstract void activated();

    /**
     * Responds to a page change that made the Graphical editor page the deactivated page
     */
    abstract void deactivated();

    /**
     * Used by LayoutEditor.UiEditorActions.selectUiNode to select a new UI Node
     * created by  {@link ElementCreateCommand#execute()}.
     * 
     * @param uiNodeModel The {@link UiElementNode} to select.
     */
    abstract void selectModel(UiElementNode uiNodeModel);

    /**
     * Returns the selection synchronizer object.
     * The synchronizer can be used to sync the selection of 2 or more EditPartViewers.
     * <p/>
     * This is changed from protected to public so that the outline can use it.
     *
     * @return the synchronizer
     */
    @Override
    public SelectionSynchronizer getSelectionSynchronizer() {
        return super.getSelectionSynchronizer();
    }

    /**
     * Returns the edit domain.
     * <p/>
     * This is changed from protected to public so that the outline can use it.
     *
     * @return the edit domain
     */
    @Override
    public DefaultEditDomain getEditDomain() {
        return super.getEditDomain();
    }

    abstract void reloadPalette();

    abstract void recomputeLayout();

    abstract UiDocumentNode getModel();

    abstract LayoutEditor getLayoutEditor();

    abstract Clipboard getClipboard();

}
