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

import com.android.ide.eclipse.adt.internal.editors.uimodel.UiFlagAttributeNode;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * DialogCellEditor able to receive a {@link UiFlagAttributeNode} in the {@link #setValue(Object)}
 * method.
 * <p/>The dialog box opened is the same as the one in the ui created by
 * {@link UiFlagAttributeNode#createUiControl(Composite, org.eclipse.ui.forms.IManagedForm)}
 */
public class FlagValueCellEditor extends EditableDialogCellEditor {
    
    private UiFlagAttributeNode mUiFlagAttribute;

    public FlagValueCellEditor(Composite parent) {
        super(parent);
    }
    
    @Override
    protected Object openDialogBox(Control cellEditorWindow) {
        if (mUiFlagAttribute != null) {
            String currentValue = (String)getValue();
            return mUiFlagAttribute.showDialog(cellEditorWindow.getShell(), currentValue);
        }
        
        return null;
    }
    
    @Override
    protected void doSetValue(Object value) {
        if (value instanceof UiFlagAttributeNode) {
            mUiFlagAttribute = (UiFlagAttributeNode)value;
            super.doSetValue(mUiFlagAttribute.getCurrentValue());
            return;
        }
        
        super.doSetValue(value);
    }
}
