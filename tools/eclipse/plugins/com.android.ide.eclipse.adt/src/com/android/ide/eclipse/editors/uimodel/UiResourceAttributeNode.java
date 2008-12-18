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

package com.android.ide.eclipse.editors.uimodel;

import com.android.ide.eclipse.adt.sdk.AndroidTargetData;
import com.android.ide.eclipse.common.resources.IResourceRepository;
import com.android.ide.eclipse.common.resources.ResourceType;
import com.android.ide.eclipse.editors.AndroidEditor;
import com.android.ide.eclipse.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.editors.descriptors.DescriptorsUtils;
import com.android.ide.eclipse.editors.descriptors.TextAttributeDescriptor;
import com.android.ide.eclipse.editors.resources.manager.ResourceManager;
import com.android.ide.eclipse.editors.ui.SectionHelper;
import com.android.ide.eclipse.editors.wizards.ReferenceChooserDialog;
import com.android.ide.eclipse.editors.wizards.ResourceChooser;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;

/**
 * Represents an XML attribute for a resource that can be modified using a simple text field or
 * a dialog to choose an existing resource.
 * <p/>
 * It can be configured to represent any kind of resource, by providing the desired
 * {@link ResourceType} in the constructor.
 * <p/>
 * See {@link UiTextAttributeNode} for more information.
 */
public class UiResourceAttributeNode extends UiTextAttributeNode {
    
    private ResourceType mType;
    
    public UiResourceAttributeNode(ResourceType type,
            AttributeDescriptor attributeDescriptor, UiElementNode uiParent) {
        super(attributeDescriptor, uiParent);
        
        mType = type;
    }

    /* (non-java doc)
     * Creates a label widget and an associated text field.
     * <p/>
     * As most other parts of the android manifest editor, this assumes the
     * parent uses a table layout with 2 columns.
     */
    @Override
    public void createUiControl(final Composite parent, IManagedForm managedForm) {
        setManagedForm(managedForm);
        FormToolkit toolkit = managedForm.getToolkit();
        TextAttributeDescriptor desc = (TextAttributeDescriptor) getDescriptor();

        Label label = toolkit.createLabel(parent, desc.getUiName());
        label.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));
        SectionHelper.addControlTooltip(label, DescriptorsUtils.formatTooltip(desc.getTooltip()));

        Composite composite = toolkit.createComposite(parent);
        composite.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.MIDDLE));
        GridLayout gl = new GridLayout(2, false);
        gl.marginHeight = gl.marginWidth = 0;
        composite.setLayout(gl);
        // Fixes missing text borders under GTK... also requires adding a 1-pixel margin
        // for the text field below
        toolkit.paintBordersFor(composite);
        
        final Text text = toolkit.createText(composite, getCurrentValue());
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalIndent = 1;  // Needed by the fixed composite borders under GTK
        text.setLayoutData(gd);
        Button browseButton = toolkit.createButton(composite, "Browse...", SWT.PUSH);
        
        setTextWidget(text);

        // TODO Add a validator using onAddModifyListener
        
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String result = showDialog(parent.getShell(), text.getText().trim());
                if (result != null) {
                    text.setText(result);
                }
            }
        });
    }
    
    /**
     * Shows a dialog letting the user choose a set of enum, and returns a string
     * containing the result.
     */
    public String showDialog(Shell shell, String currentValue) {
        // we need to get the project of the file being edited.
        UiElementNode uiNode = getUiParent();
        AndroidEditor editor = uiNode.getEditor();
        IProject project = editor.getProject();
        if (project != null) {
            // get the resource repository for this project and the system resources.
            IResourceRepository projectRepository =
                ResourceManager.getInstance().getProjectResources(project);
            
            if (mType != null) {
                // get the Target Data to get the system resources
                AndroidTargetData data = editor.getTargetData();
                IResourceRepository systemRepository = data.getSystemResources();

                // open a resource chooser dialog for specified resource type.
                ResourceChooser dlg = new ResourceChooser(mType,
                        projectRepository, systemRepository, shell);

                dlg.setCurrentResource(currentValue);

                if (dlg.open() == Window.OK) {
                    return dlg.getCurrentResource();
                }
            } else {
                ReferenceChooserDialog dlg = new ReferenceChooserDialog(projectRepository,
                        shell);

                dlg.setCurrentResource(currentValue);

                if (dlg.open() == Window.OK) {
                    return dlg.getCurrentResource();
                }
            }
        }

        return null;
    }
    
    @Override
    public String[] getPossibleValues() {
        // TODO: compute a list of existing resources for content assist completion
        return null;
    }
}
