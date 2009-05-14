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

package com.android.ide.eclipse.adt.internal.editors.ui;

import com.android.ide.eclipse.adt.internal.editors.uimodel.UiListAttributeNode;

import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * ComboBoxCellEditor able to receive a {@link UiListAttributeNode} in the {@link #setValue(Object)}
 * method, and returning a {@link String} in {@link #getValue()} instead of an {@link Integer}.
 */
public class ListValueCellEditor extends ComboBoxCellEditor {
    private String[] mItems;
    private CCombo mCombo;
    
    public ListValueCellEditor(Composite parent) {
        super(parent, new String[0], SWT.DROP_DOWN);
    }
    
    @Override
    protected Control createControl(Composite parent) {
        mCombo = (CCombo) super.createControl(parent);
        return mCombo;
    }
    
    @Override
    protected void doSetValue(Object value) {
        if (value instanceof UiListAttributeNode) {
            UiListAttributeNode uiListAttribute = (UiListAttributeNode)value;
            
            // set the possible values in the combo
            String[] items = uiListAttribute.getPossibleValues(null);
            mItems = new String[items.length];
            System.arraycopy(items, 0, mItems, 0, items.length);
            setItems(mItems);
            
            // now edit the current value of the attribute
            String attrValue = uiListAttribute.getCurrentValue();
            mCombo.setText(attrValue);
            
            return;
        }
        
        // default behavior
        super.doSetValue(value);
    }
    
    @Override
    protected Object doGetValue() {
        String comboText = mCombo.getText();
        if (comboText == null) {
            return ""; //$NON-NLS-1$
        }
        return comboText;
    }

}
