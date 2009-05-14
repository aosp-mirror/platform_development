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

package com.android.ide.eclipse.adt.internal.editors.descriptors;

import com.android.ide.eclipse.adt.internal.editors.uimodel.UiAttributeNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiTextValueNode;


/**
 * Describes the value of an XML element.
 * <p/>
 * The value is a simple text string, displayed by an {@link UiTextValueNode}.
 */
public class TextValueDescriptor extends TextAttributeDescriptor {
    
    /**
     * Creates a new {@link TextValueDescriptor}
     * 
     * @param uiName The UI name of the attribute. Cannot be an empty string and cannot be null.
     * @param tooltip A non-empty tooltip string or null
     */
    public TextValueDescriptor(String uiName, String tooltip) {
        super("#text" /* xmlLocalName */, uiName, null /* nsUri */, tooltip);
    }

    /**
     * @return A new {@link UiTextValueNode} linked to this descriptor.
     */
    @Override
    public UiAttributeNode createUiNode(UiElementNode uiParent) {
        return new UiTextValueNode(this, uiParent);
    }
}
