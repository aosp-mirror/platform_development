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

package com.android.ide.eclipse.editors.layout;

import com.android.ide.eclipse.editors.EditorsPlugin;
import com.android.ide.eclipse.editors.resources.manager.ResourceFolder;
import com.android.ide.eclipse.editors.resources.manager.ResourceManager;
import com.android.ide.eclipse.editors.ui.tree.UiTreeBlock;
import com.android.ide.eclipse.editors.uimodel.UiElementNode;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Page for the layout tree-based form editor.
 * 
 * @deprecated This is being phased out. We use the GraphicalLayoutEditor instead.
 */
public final class LayoutTreePage extends FormPage {
    /** Page id used for switching tabs programmatically */
    public final static String PAGE_ID = "layout_tree_page"; //$NON-NLS-1$

    /** Container editor */
    LayoutEditor mEditor;

    public LayoutTreePage(LayoutEditor editor) {
        super(editor, PAGE_ID, "Layout");  // tab's label, keep it short
        mEditor = editor;
    }

    /**
     * Creates the content in the form hosted in this page.
     * 
     * @param managedForm the form hosted in this page.
     */
    @Override
    protected void createFormContent(IManagedForm managedForm) {
        super.createFormContent(managedForm);
        ScrolledForm form = managedForm.getForm();

        String configText = null;
        IEditorInput input = mEditor.getEditorInput();
        if (input instanceof FileEditorInput) {
            FileEditorInput fileInput = (FileEditorInput)input;
            IFile iFile = fileInput.getFile();
            
            ResourceFolder resFolder = ResourceManager.getInstance().getResourceFolder(iFile);
            if (resFolder != null) {
                configText = resFolder.getConfiguration().toDisplayString();
            }
        }
        
        if (configText != null) {
            form.setText(String.format("Android Layout (%1$s)", configText));
        } else {
            form.setText("Android Layout");
        }

        form.setImage(EditorsPlugin.getAndroidLogo());

        UiElementNode rootNode = mEditor.getUiRootNode();
        UiTreeBlock block = new UiTreeBlock(mEditor, rootNode,
                true /* autoCreateRoot */,
                null /* no element filters */,
                "Layout Elements",
                "List of all layout elements in this XML file.");
        block.createContent(managedForm);
    }
}
