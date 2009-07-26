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

package com.android.ide.eclipse.adt.internal.editors.menu;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.editors.ui.tree.UiTreeBlock;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;

import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * Page for the menu form editor.
 */
public final class MenuTreePage extends FormPage {
    /** Page id used for switching tabs programmatically */
    public final static String PAGE_ID = "layout_tree_page"; //$NON-NLS-1$

    /** Container editor */
    MenuEditor mEditor;

    public MenuTreePage(MenuEditor editor) {
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
        form.setText("Android Menu");
        form.setImage(AdtPlugin.getAndroidLogo());

        UiElementNode rootNode = mEditor.getUiRootNode();
        UiTreeBlock block = new UiTreeBlock(mEditor, rootNode,
                true /* autoCreateRoot */,
                null /* no element filters */,
                "Menu Elements",
                "List of all menu elements in this XML file.");
        block.createContent(managedForm);
    }
}
