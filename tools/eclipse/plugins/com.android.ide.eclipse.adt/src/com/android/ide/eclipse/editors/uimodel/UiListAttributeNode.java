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

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.sdk.AndroidTargetData;
import com.android.ide.eclipse.editors.AndroidEditor;
import com.android.ide.eclipse.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.editors.descriptors.DescriptorsUtils;
import com.android.ide.eclipse.editors.descriptors.ListAttributeDescriptor;
import com.android.ide.eclipse.editors.descriptors.TextAttributeDescriptor;
import com.android.ide.eclipse.editors.descriptors.XmlnsAttributeDescriptor;
import com.android.ide.eclipse.editors.ui.SectionHelper;
import com.android.sdklib.SdkConstants;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;

/**
 * Represents an XML attribute which has possible built-in values, and can be modified by
 * an editable Combo box.
 * <p/>
 * See {@link UiTextAttributeNode} for more information.
 */
public class UiListAttributeNode extends UiAbstractTextAttributeNode {

    protected Combo mCombo;

    public UiListAttributeNode(ListAttributeDescriptor attributeDescriptor,
            UiElementNode uiParent) {
        super(attributeDescriptor, uiParent);
    }
    
    /* (non-java doc)
     * Creates a label widget and an associated text field.
     * <p/>
     * As most other parts of the android manifest editor, this assumes the
     * parent uses a table layout with 2 columns.
     */
    @Override
    public final void createUiControl(final Composite parent, IManagedForm managedForm) {
        FormToolkit toolkit = managedForm.getToolkit();
        TextAttributeDescriptor desc = (TextAttributeDescriptor) getDescriptor();

        Label label = toolkit.createLabel(parent, desc.getUiName());
        label.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));
        SectionHelper.addControlTooltip(label, DescriptorsUtils.formatTooltip(desc.getTooltip()));

        int style = SWT.DROP_DOWN;
        mCombo = new Combo(parent, style); 
        TableWrapData twd = new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.MIDDLE);
        twd.maxWidth = 100;
        mCombo.setLayoutData(twd);
        
        fillCombo();
        
        setTextWidgetValue(getCurrentValue());
        
        mCombo.addModifyListener(new ModifyListener() {
            /**
             * Sent when the text is modified, whether by the user via manual
             * input or programmatic input via setText().
             * <p/>
             * Simply mark the attribute as dirty if it really changed.
             * The container SectionPart will collect these flag and manage them.
             */
            public void modifyText(ModifyEvent e) {
                if (!isInInternalTextModification() &&
                        !isDirty() &&
                        mCombo != null &&
                        getCurrentValue() != null &&
                        !mCombo.getText().equals(getCurrentValue())) {
                    setDirty(true);
                }
            }            
        });

        // Remove self-reference when the widget is disposed
        mCombo.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                mCombo = null;
            }
        });
    }
    
    protected void fillCombo() {
        String[] values = getPossibleValues(null);

        if (values == null) {
            AdtPlugin.log(IStatus.ERROR,
                    "FrameworkResourceManager did not provide values yet for %1$s",
                    getDescriptor().getXmlLocalName());
        } else {
            for (String value : values) {
                mCombo.add(value);
            }
        }
    }
    
    /**
     * Get the list values, either from the initial values set in the attribute
     * or by querying the framework resource parser.
     * 
     * {@inheritDoc}
     */
    @Override
    public String[] getPossibleValues(String prefix) {
        AttributeDescriptor descriptor = getDescriptor();
        UiElementNode uiParent = getUiParent();

        String attr_name = descriptor.getXmlLocalName();
        String element_name = uiParent.getDescriptor().getXmlName();
        
        // FrameworkResourceManager expects a specific prefix for the attribute.
        String nsPrefix = "";
        if (SdkConstants.NS_RESOURCES.equals(descriptor.getNamespaceUri())) {
            nsPrefix = "android:"; //$NON-NLS-1$
        } else if (XmlnsAttributeDescriptor.XMLNS_URI.equals(descriptor.getNamespaceUri())) {
            nsPrefix = "xmlns:"; //$NON-NLS-1$
        }
        attr_name = nsPrefix + attr_name;
        
        String[] values = null;
        
        if (descriptor instanceof ListAttributeDescriptor &&
                ((ListAttributeDescriptor) descriptor).getValues() != null) {
            // Get enum values from the descriptor
            values = ((ListAttributeDescriptor) descriptor).getValues();
        }

        if (values == null) {
            // or from the AndroidTargetData
            UiElementNode uiNode = getUiParent();
            AndroidEditor editor = uiNode.getEditor();
            AndroidTargetData data = editor.getTargetData();
            if (data != null) {
                // get the great-grand-parent descriptor.
                
                // the parent should always exist.
                UiElementNode grandParentNode = uiParent.getUiParent();
    
                String greatGrandParentNodeName = null;
                if (grandParentNode != null) {
                    UiElementNode greatGrandParentNode = grandParentNode.getUiParent();
                    if (greatGrandParentNode != null) {
                        greatGrandParentNodeName =
                            greatGrandParentNode.getDescriptor().getXmlName();
                    }
                }
            
                values = data.getAttributeValues(element_name, attr_name, greatGrandParentNodeName);
            }
        }
        
        return values;
    }

    @Override
    public String getTextWidgetValue() {
        if (mCombo != null) {
            return mCombo.getText();
        }
        
        return null;
    }

    @Override
    public final boolean isValid() {
        return mCombo != null;
    }

    @Override
    public void setTextWidgetValue(String value) {
        if (mCombo != null) {
            mCombo.setText(value);
        }
    }
}
