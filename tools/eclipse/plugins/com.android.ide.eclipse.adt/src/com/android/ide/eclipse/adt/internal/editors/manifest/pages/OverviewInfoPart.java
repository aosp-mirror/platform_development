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

import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.manifest.ManifestEditor;
import com.android.ide.eclipse.adt.internal.editors.manifest.descriptors.AndroidManifestDescriptors;
import com.android.ide.eclipse.adt.internal.editors.ui.UiElementPart;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Generic info section part for overview page: it displays all the attributes from
 * the manifest element.
 */
final class OverviewInfoPart extends UiElementPart {

    private IManagedForm mManagedForm;

    public OverviewInfoPart(Composite body, FormToolkit toolkit, ManifestEditor editor) {
        super(body, toolkit, editor,
                getManifestUiNode(editor),  // uiElementNode
                "Manifest General Attributes", // section title
                "Defines general information about the AndroidManifest.xml", // section description
                Section.TWISTIE | Section.EXPANDED);
    }

    /**
     * Retrieves the UiElementNode that this part will edit. The node must exist
     * and can't be null, by design, because it's a mandatory node.
     */
    private static UiElementNode getManifestUiNode(ManifestEditor editor) {
        AndroidManifestDescriptors manifestDescriptors = editor.getManifestDescriptors();
        if (manifestDescriptors != null) {
            ElementDescriptor desc = manifestDescriptors.getManifestElement();
            if (editor.getUiRootNode().getDescriptor() == desc) {
                return editor.getUiRootNode();
            } else {
                return editor.getUiRootNode().findUiChildNode(desc.getXmlName());
            }
        }
        
        // No manifest descriptor: we have a dummy UiRootNode, so we return that.
        // The editor will be reloaded once we have the proper descriptors anyway.
        return editor.getUiRootNode();
    }

    /**
     * Overridden in order to capture the current managed form.
     * 
     * {@inheritDoc}
     */
    @Override
    protected void createFormControls(final IManagedForm managedForm) {
        mManagedForm = managedForm; 
        super.createFormControls(managedForm);
    }

    /**
     * Removes any existing Attribute UI widgets and recreate them if the SDK has changed.
     * <p/>
     * This is called by {@link OverviewPage#refreshUiApplicationNode()} when the
     * SDK has changed.
     */
    public void onSdkChanged() {
        setUiElementNode(getManifestUiNode(getEditor()));
        createUiAttributes(mManagedForm);
    }
}
