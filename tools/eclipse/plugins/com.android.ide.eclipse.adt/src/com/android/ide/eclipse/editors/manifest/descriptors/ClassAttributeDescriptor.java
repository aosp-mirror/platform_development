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

package com.android.ide.eclipse.editors.manifest.descriptors;

import com.android.ide.eclipse.editors.descriptors.TextAttributeDescriptor;
import com.android.ide.eclipse.editors.manifest.model.UiClassAttributeNode;
import com.android.ide.eclipse.editors.manifest.model.UiClassAttributeNode.IPostTypeCreationAction;
import com.android.ide.eclipse.editors.uimodel.UiAttributeNode;
import com.android.ide.eclipse.editors.uimodel.UiElementNode;
import com.android.sdklib.SdkConstants;

/**
 * Describes an XML attribute representing a class name.
 * It is displayed by a {@link UiClassAttributeNode}.
 */
public class ClassAttributeDescriptor extends TextAttributeDescriptor {

    /** Superclass of the class value. */
    private String mSuperClassName;
    
    private IPostTypeCreationAction mPostCreationAction;
    
    /** indicates if the class parameter is mandatory */
    boolean mMandatory;

    private final boolean mDefaultToProjectOnly;
    
    /**
     * Creates a new {@link ClassAttributeDescriptor}
     * @param superClassName the fully qualified name of the superclass of the class represented
     * by the attribute.
     * @param xmlLocalName The XML name of the attribute (case sensitive, with android: prefix).
     * @param uiName The UI name of the attribute. Cannot be an empty string and cannot be null.
     * @param nsUri The URI of the attribute. Can be null if attribute has no namespace.
     *              See {@link SdkConstants#NS_RESOURCES} for a common value.
     * @param tooltip A non-empty tooltip string or null.
     * @param mandatory indicates if the class attribute is mandatory.
     */
    public ClassAttributeDescriptor(String superClassName,
            String xmlLocalName,
            String uiName,
            String nsUri,
            String tooltip,
            boolean mandatory) {
        super(xmlLocalName, uiName, nsUri, tooltip);
        mSuperClassName = superClassName;
        mDefaultToProjectOnly = true;
    }

    /**
     * Creates a new {@link ClassAttributeDescriptor}
     * @param superClassName the fully qualified name of the superclass of the class represented
     * by the attribute.
     * @param postCreationAction the {@link IPostTypeCreationAction} to be executed on the
     *        newly created class.
     * @param xmlLocalName The XML local name of the attribute (case sensitive).
     * @param uiName The UI name of the attribute. Cannot be an empty string and cannot be null.
     * @param nsUri The URI of the attribute. Can be null if attribute has no namespace.
     *              See {@link SdkConstants#NS_RESOURCES} for a common value.
     * @param tooltip A non-empty tooltip string or null.
     * @param mandatory indicates if the class attribute is mandatory.
     * @param defaultToProjectOnly True if only classes from the sources of this project should
     *         be shown by default in the class browser.
     */
    public ClassAttributeDescriptor(String superClassName,
            IPostTypeCreationAction postCreationAction,
            String xmlLocalName,
            String uiName,
            String nsUri,
            String tooltip,
            boolean mandatory,
            boolean defaultToProjectOnly) {
        super(xmlLocalName, uiName, nsUri, tooltip);
        mSuperClassName = superClassName;
        mPostCreationAction = postCreationAction;
        mDefaultToProjectOnly = defaultToProjectOnly;
    }

    /**
     * @return A new {@link UiClassAttributeNode} linked to this descriptor.
     */
    @Override
    public UiAttributeNode createUiNode(UiElementNode uiParent) {
        return new UiClassAttributeNode(mSuperClassName, mPostCreationAction,
                mMandatory, this, uiParent, mDefaultToProjectOnly);
    }
}
