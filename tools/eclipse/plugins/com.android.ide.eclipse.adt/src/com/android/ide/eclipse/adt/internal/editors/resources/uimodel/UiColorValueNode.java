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

package com.android.ide.eclipse.adt.internal.editors.resources.uimodel;

import com.android.ide.eclipse.adt.internal.editors.descriptors.TextValueDescriptor;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiAttributeNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiTextValueNode;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Text;

import java.util.regex.Pattern;

/**
 * Displays and edits a color XML element value with a custom validator.
 * <p/>
 * See {@link UiAttributeNode} for more information.
 */
public class UiColorValueNode extends UiTextValueNode {

    /** Accepted RGBA formats are one of #RGB, #ARGB, #RRGGBB or #AARRGGBB. */
    private static final Pattern RGBA_REGEXP = Pattern.compile(
            "#(?:[0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})"); //$NON-NLS-1$
    
    public UiColorValueNode(TextValueDescriptor attributeDescriptor, UiElementNode uiParent) {
        super(attributeDescriptor, uiParent);
    }

    /* (non-java doc)
     * 
     * Add a modify listener that will check colors have the proper format,
     * that is one of #RGB, #ARGB, #RRGGBB or #AARRGGBB.
     */
    @Override
    protected void onAddValidators(final Text text) {
        ModifyListener listener = new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String color = text.getText();
                if (RGBA_REGEXP.matcher(color).matches()) {
                    getManagedForm().getMessageManager().removeMessage(text, text);
                } else {
                    getManagedForm().getMessageManager().addMessage(text,
                            "Accepted color formats are one of #RGB, #ARGB, #RRGGBB or #AARRGGBB.",
                            null /* data */, IMessageProvider.ERROR, text);
                }
            }
        };

        text.addModifyListener(listener);

        // Make sure the validator removes its message(s) when the widget is disposed
        text.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                getManagedForm().getMessageManager().removeMessage(text, text);
            }
        });

        // Finally call the validator once to make sure the initial value is processed
        listener.modifyText(null);
    }
}
