/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ide.eclipse.editors.manifest.pages;

import com.android.ide.eclipse.editors.EditorsPlugin;
import com.android.ide.eclipse.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.editors.manifest.ManifestEditor;
import com.android.ide.eclipse.editors.manifest.descriptors.AndroidManifestDescriptors;
import com.android.ide.eclipse.editors.ui.tree.UiTreeBlock;
import com.android.ide.eclipse.editors.uimodel.UiElementNode;

import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * Page for instrumentation settings, part of the AndroidManifest form editor.
 */
public final class InstrumentationPage extends FormPage {
    /** Page id used for switching tabs programmatically */
    public final static String PAGE_ID = "instrumentation_page"; //$NON-NLS-1$

    /** Container editor */
    ManifestEditor mEditor;

    public InstrumentationPage(ManifestEditor editor) {
        super(editor, PAGE_ID, "Instrumentation");  // tab's label, keep it short
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
        form.setText("Android Manifest Instrumentation");
        form.setImage(EditorsPlugin.getAndroidLogo());

        UiElementNode manifest = mEditor.getUiRootNode();
        UiTreeBlock block = new UiTreeBlock(mEditor, manifest,
                true /* autoCreateRoot */,
                new ElementDescriptor[] { AndroidManifestDescriptors.INTRUMENTATION_ELEMENT },
                "Instrumentation",
                "List of instrumentations defined in the manifest");
        block.createContent(managedForm);
    }
}
