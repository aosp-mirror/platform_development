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

package com.android.ide.eclipse.adt.internal.editors.manifest.pages;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.manifest.ManifestEditor;
import com.android.ide.eclipse.adt.internal.editors.manifest.descriptors.AndroidManifestDescriptors;
import com.android.ide.eclipse.adt.internal.editors.ui.tree.UiTreeBlock;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import java.util.ArrayList;
import java.util.HashSet;


/**
 * Page for overview settings, part of the AndroidManifest form editor.
 * <p/>
 * Useful reference:
 * <a href="http://www.eclipse.org/articles/Article-Forms/article.html">
 *   http://www.eclipse.org/articles/Article-Forms/article.html</a>
 */
public final class OverviewPage extends FormPage {

    /** Page id used for switching tabs programmatically */
    final static String PAGE_ID = "overview_page"; //$NON-NLS-1$
    
    /** Container editor */
    ManifestEditor mEditor;
    /** Overview part (attributes for manifest) */
    private OverviewInfoPart mOverviewPart;
    /** Overview link part */
    private OverviewLinksPart mOverviewLinkPart;

    private UiTreeBlock mTreeBlock;
    
    public OverviewPage(ManifestEditor editor) {
        super(editor, PAGE_ID, "Manifest");  // tab's label, user visible, keep it short
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
        form.setText("Android Manifest");
        form.setImage(AdtPlugin.getAndroidLogo());
        
        Composite body = form.getBody();
        FormToolkit toolkit = managedForm.getToolkit();
        
        // Usually we would set a ColumnLayout on body here. However the presence of the
        // UiTreeBlock forces a GridLayout with one column so we comply with it.

        mOverviewPart = new OverviewInfoPart(body, toolkit, mEditor);
        mOverviewPart.getSection().setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        managedForm.addPart(mOverviewPart);
        
        newManifestExtrasPart(managedForm);
        
        OverviewExportPart exportPart = new OverviewExportPart(this, body, toolkit, mEditor);
        exportPart.getSection().setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        managedForm.addPart(exportPart);
        
        mOverviewLinkPart = new OverviewLinksPart(body, toolkit, mEditor);
        mOverviewLinkPart.getSection().setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        managedForm.addPart(mOverviewLinkPart);
    }

    private void newManifestExtrasPart(IManagedForm managedForm) {
        UiElementNode manifest = mEditor.getUiRootNode();
        mTreeBlock = new UiTreeBlock(mEditor, manifest,
                true /* autoCreateRoot */,
                computeManifestExtraFilters(),
                "Manifest Extras",
                "Extra manifest elements");
        mTreeBlock.createContent(managedForm);
    }

    /**
     * Changes and refreshes the Application UI node handle by the sub parts.
     */
    public void refreshUiApplicationNode() {
        if (mOverviewPart != null) {
            mOverviewPart.onSdkChanged();
        }
        
        if (mOverviewLinkPart != null) {
            mOverviewLinkPart.onSdkChanged();
        }

        if (mTreeBlock != null) {
            UiElementNode manifest = mEditor.getUiRootNode();
            mTreeBlock.changeRootAndDescriptors(manifest,
                    computeManifestExtraFilters(),
                    true /* refresh */);
        }
    }
    
    private ElementDescriptor[] computeManifestExtraFilters() {
        UiElementNode manifest = mEditor.getUiRootNode();
        AndroidManifestDescriptors manifestDescriptor = mEditor.getManifestDescriptors();

        if (manifestDescriptor == null) {
            return null;
        }

        // get the elements we want to exclude
        HashSet<ElementDescriptor> excludes = new HashSet<ElementDescriptor>();
        excludes.add(manifestDescriptor.getApplicationElement());
        excludes.add(manifestDescriptor.getInstrumentationElement());
        excludes.add(manifestDescriptor.getPermissionElement());
        excludes.add(manifestDescriptor.getPermissionGroupElement());
        excludes.add(manifestDescriptor.getPermissionTreeElement());
        excludes.add(manifestDescriptor.getUsesPermissionElement());

        // walk through the known children of the manifest descriptor and keep what's not excluded
        ArrayList<ElementDescriptor> descriptorFilters = new ArrayList<ElementDescriptor>();
        for (ElementDescriptor child : manifest.getDescriptor().getChildren()) {
            if (!excludes.contains(child)) {
                descriptorFilters.add(child);
            }
        }

        if (descriptorFilters.size() == 0) {
            return null;
        }
        return descriptorFilters.toArray(new ElementDescriptor[descriptorFilters.size()]);
    }
}
