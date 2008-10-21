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

package com.android.ide.eclipse.editors.descriptors;

import com.android.ide.eclipse.editors.uimodel.UiAttributeNode;
import com.android.ide.eclipse.editors.uimodel.UiElementNode;
import com.android.ide.eclipse.editors.uimodel.UiSeparatorAttributeNode;

/**
 * {@link SeparatorAttributeDescriptor} does not represent any real attribute.
 * <p/>
 * It is used to separate groups of attributes visually.
 */
public class SeparatorAttributeDescriptor extends AttributeDescriptor {
    
    /**
     * Creates a new {@link SeparatorAttributeDescriptor}
     */
    public SeparatorAttributeDescriptor(String label) {
        super(label /* xmlLocalName */, null /* nsUri */);
    }

    /**
     * @return A new {@link UiAttributeNode} linked to this descriptor or null if this
     *         attribute has no user interface.
     */
    @Override
    public UiAttributeNode createUiNode(UiElementNode uiParent) {
        return new UiSeparatorAttributeNode(this, uiParent);
    }
}
