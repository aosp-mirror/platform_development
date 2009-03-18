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

import com.android.ide.eclipse.editors.AndroidEditor;
import com.android.ide.eclipse.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.editors.descriptors.DescriptorsUtils;
import com.android.ide.eclipse.editors.descriptors.TextAttributeDescriptor;
import com.android.ide.eclipse.editors.ui.SectionHelper;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.TableWrapData;

/**
 * Represents an XML attribute in that can be modified using a simple text field
 * in the XML editor's user interface.
 * <p/>
 * The XML attribute has no default value. When unset, the text field is blank.
 * When updating the XML, if the field is empty, the attribute will be removed
 * from the XML element.  
 * <p/>
 * See {@link UiAttributeNode} for more information.
 */
public class UiTextAttributeNode extends UiAbstractTextAttributeNode {

    /** Text field */
    private Text mText;
    /** The managed form, set only once createUiControl has been called. */
    private IManagedForm mManagedForm;

    public UiTextAttributeNode(AttributeDescriptor attributeDescriptor, UiElementNode uiParent) {
        super(attributeDescriptor, uiParent);
    }
    
    /* (non-java doc)
     * Creates a label widget and an associated text field.
     * <p/>
     * As most other parts of the android manifest editor, this assumes the
     * parent uses a table layout with 2 columns.
     */
    @Override
    public void createUiControl(Composite parent, IManagedForm managedForm) {
        setManagedForm(managedForm);
        TextAttributeDescriptor desc = (TextAttributeDescriptor) getDescriptor();
        Text text = SectionHelper.createLabelAndText(parent, managedForm.getToolkit(),
                desc.getUiName(), getCurrentValue(),
                DescriptorsUtils.formatTooltip(desc.getTooltip()));

        setTextWidget(text);
    }

    /**
     * No completion values for this UI attribute.
     * 
     * {@inheritDoc}
     */
    @Override
    public String[] getPossibleValues(String prefix) {
        return null;
    }
    
    /**
     * Sets the internal managed form.
     * This is usually set by createUiControl.
     */
    protected void setManagedForm(IManagedForm managedForm) {
         mManagedForm = managedForm;
    }
    
    /**
     * @return The managed form, set only once createUiControl has been called.
     */
    protected IManagedForm getManagedForm() {
        return mManagedForm;
    }
    
    /* (non-java doc)
     * Returns if the attribute node is valid, and its UI has been created.
     */
    @Override
    public boolean isValid() {
        return mText != null;
    }

    @Override
    public String getTextWidgetValue() {
        if (mText != null) {
            return mText.getText();
        }
        
        return null;
    }

    @Override
    public void setTextWidgetValue(String value) {
        if (mText != null) {
            mText.setText(value);
        }
    }

    /**
     * Sets the Text widget object, and prepares it to handle modification and synchronization
     * with the XML node.
     * @param textWidget
     */
    protected final void setTextWidget(Text textWidget) {
        mText = textWidget;
 
        if (textWidget != null) {
            // Sets the with hint for the text field. Derived classes can always override it.
            // This helps the grid layout to resize correctly on smaller screen sizes.
            Object data = textWidget.getLayoutData();
            if (data == null) {
            } else if (data instanceof GridData) {
                ((GridData)data).widthHint = AndroidEditor.TEXT_WIDTH_HINT;
            } else if (data instanceof TableWrapData) {
                ((TableWrapData)data).maxWidth = 100;
            }
            
            mText.addModifyListener(new ModifyListener() {
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
                            mText != null &&
                            getCurrentValue() != null &&
                            !mText.getText().equals(getCurrentValue())) {
                        setDirty(true);
                    }
                }            
            });
            
            // Remove self-reference when the widget is disposed
            mText.addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent e) {
                    mText = null;
                }
            });
        }
        
        onAddValidators(mText);
    }

    /**
     * Called after the text widget as been created.
     * <p/>
     * Derived classes typically want to:
     * <li> Create a new {@link ModifyListener} and attach it to the given {@link Text} widget.
     * <li> In the modify listener, call getManagedForm().getMessageManager().addMessage()
     *      and getManagedForm().getMessageManager().removeMessage() as necessary.
     * <li> Call removeMessage in a new text.addDisposeListener.
     * <li> Call the validator once to setup the initial messages as needed.
     * <p/>
     * The base implementation does nothing.
     * 
     * @param text The {@link Text} widget to validate.
     */
    protected void onAddValidators(Text text) {
    }

    /**
     * Returns the text widget.
     */
    protected final Text getTextWidget() {
        return mText;
    }
}
