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

package com.android.ide.eclipse.adt.internal.editors.uimodel;

import com.android.ide.eclipse.adt.internal.editors.descriptors.AttributeDescriptor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.w3c.dom.Node;

/**
 * Represents an XML attribute that can be modified by the XML editor's user interface.
 * <p/>
 * The characteristics of an {@link UiAttributeNode} are declared by a
 * corresponding {@link AttributeDescriptor}.
 * <p/>
 * This is an abstract class. Derived classes must implement the creation of the UI
 * and manage its synchronization with the XML.
 */
public abstract class UiAttributeNode {

    private AttributeDescriptor mDescriptor;
    private UiElementNode mUiParent;
    private boolean mIsDirty;
    private boolean mHasError;

    /** Creates a new {@link UiAttributeNode} linked to a specific {@link AttributeDescriptor} 
     * and the corresponding runtine {@link UiElementNode} parent. */
    public UiAttributeNode(AttributeDescriptor attributeDescriptor, UiElementNode uiParent) {
        mDescriptor = attributeDescriptor;
        mUiParent = uiParent;
    }

    /** Returns the {@link AttributeDescriptor} specific to this UI attribute node */
    public final AttributeDescriptor getDescriptor() {
        return mDescriptor;
    }

    /** Returns the {@link UiElementNode} that owns this {@link UiAttributeNode} */
    public final UiElementNode getUiParent() {
        return mUiParent;
    }
    
    /** Returns the current value of the node. */
    public abstract String getCurrentValue();

    /**
     * @return True if the attribute has been changed since it was last loaded
     *         from the XML model.
     */
    public final boolean isDirty() {
        return mIsDirty;
    }

    /**
     * Sets whether the attribute is dirty and also notifies the editor some part's dirty
     * flag as changed.
     * <p/>
     * Subclasses should set the to true as a result of user interaction with the widgets in
     * the section and then should set to false when the commit() method completed.
     */
    public void setDirty(boolean isDirty) {
        boolean old_value = mIsDirty;
        mIsDirty = isDirty;
        // TODO: for unknown attributes, getParent() != null && getParent().getEditor() != null
        if (old_value != isDirty) {
            getUiParent().getEditor().editorDirtyStateChanged();
        }
    }
    
    /**
     * Sets the error flag value.
     * @param errorFlag the error flag
     */
    public final void setHasError(boolean errorFlag) {
        mHasError = errorFlag;
    }
    
    /**
     * Returns whether this node has errors.
     */
    public final boolean hasError() {
        return mHasError;
    }
    
    /**
     * Called once by the parent user interface to creates the necessary
     * user interface to edit this attribute.
     * <p/>
     * This method can be called more than once in the life cycle of an UI node,
     * typically when the UI is part of a master-detail tree, as pages are swapped.
     * 
     * @param parent The composite where to create the user interface.
     * @param managedForm The managed form owning this part.
     */
    public abstract void createUiControl(Composite parent, IManagedForm managedForm);

    /**
     * Used to get a list of all possible values for this UI attribute.
     * <p/>
     * This is used, among other things, by the XML Content Assists to complete values
     * for an attribute.
     * <p/>
     * Implementations that do not have any known values should return null.
     * 
     * @param prefix An optional prefix string, which is whatever the user has already started
     *   typing. Can be null or an empty string. The implementation can use this to filter choices
     *   and only return strings that match this prefix. A lazy or default implementation can
     *   simply ignore this and return everything.
     * @return A list of possible completion values, and empty array or null.
     */
    public abstract String[] getPossibleValues(String prefix);

    /**
     * Called when the XML is being loaded or has changed to
     * update the value held by this user interface attribute node.
     * <p/>
     * The XML Node <em>may</em> be null, which denotes that the attribute is not
     * specified in the XML model. In general, this means the "default" value of the
     * attribute should be used.
     * <p/>
     * The caller doesn't really know if attributes have changed,
     * so it will call this to refresh the attribute anyway. It's up to the
     * UI implementation to minimize refreshes.
     * 
     * @param xml_attribute_node
     */
    public abstract void updateValue(Node xml_attribute_node);

    /**
     * Called by the user interface when the editor is saved or its state changed
     * and the modified attributes must be committed (i.e. written) to the XML model.
     * <p/>
     * Important behaviors:
     * <ul>
     * <li>The caller *must* have called IStructuredModel.aboutToChangeModel before.
     *     The implemented methods must assume it is safe to modify the XML model.
     * <li>On success, the implementation *must* call setDirty(false).
     * <li>On failure, the implementation can fail with an exception, which
     *     is trapped and logged by the caller, or do nothing, whichever is more
     *     appropriate.
     * </ul>
     */
    public abstract void commit();
}
