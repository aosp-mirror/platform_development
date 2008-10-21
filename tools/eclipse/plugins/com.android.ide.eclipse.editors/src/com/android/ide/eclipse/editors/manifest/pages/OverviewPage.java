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
import com.android.ide.eclipse.editors.manifest.ManifestEditor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ColumnLayout;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;


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
    
    public OverviewPage(ManifestEditor editor) {
        super(editor, PAGE_ID, "Overview");  // tab's label, user visible, keep it short
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
        form.setText("Android Manifest Overview");
        form.setImage(EditorsPlugin.getAndroidLogo());
        
        Composite body = form.getBody();
        FormToolkit toolkit = managedForm.getToolkit();
        ColumnLayout cl = new ColumnLayout();
        cl.minNumColumns = cl.maxNumColumns = 1;
        body.setLayout(cl);
        mOverviewPart = new OverviewInfoPart(body, toolkit, mEditor);
        managedForm.addPart(mOverviewPart);
        managedForm.addPart(new OverviewExportPart(this, body, toolkit, mEditor));
        managedForm.addPart(new OverviewLinksPart(body, toolkit, mEditor));
    }

    /**
     * Changes and refreshes the Application UI node handle by the sub parts.
     */
    public void refreshUiApplicationNode() {
        if (mOverviewPart != null) {
            mOverviewPart.onSdkChanged();
        }
    }
}
