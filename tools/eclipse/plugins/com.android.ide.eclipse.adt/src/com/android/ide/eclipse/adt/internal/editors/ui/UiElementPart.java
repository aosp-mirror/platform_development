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

package com.android.ide.eclipse.adt.internal.editors.ui;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.XmlnsAttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.manifest.ManifestEditor;
import com.android.ide.eclipse.adt.internal.editors.ui.SectionHelper.ManifestSectionPart;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiAttributeNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Generic page's section part that displays all attributes of a given {@link UiElementNode}.
 * <p/>
 * This part is designed to be displayed in a page that has a table column layout.
 * It is linked to a specific {@link UiElementNode} and automatically displays all of its
 * attributes, manages its dirty state and commits the attributes when necessary.
 * <p/>
 * No derivation is needed unless the UI or workflow needs to be extended.
 */
public class UiElementPart extends ManifestSectionPart {

    /** A reference to the container editor */
    private ManifestEditor mEditor;
    /** The {@link UiElementNode} manipulated by this SectionPart. It can be null. */
    private UiElementNode mUiElementNode;
    /** Table that contains all the attributes */
    private Composite mTable;

    public UiElementPart(Composite body, FormToolkit toolkit, ManifestEditor editor,
            UiElementNode uiElementNode, String sectionTitle, String sectionDescription,
            int extra_style) {
        super(body, toolkit, extra_style, sectionDescription != null);
        mEditor = editor;
        mUiElementNode = uiElementNode;
        setupSection(sectionTitle, sectionDescription);

        if (uiElementNode == null) {
            // This is serious and should never happen. Instead of crashing, simply abort.
            // There will be no UI, which will prevent further damage.
            AdtPlugin.log(IStatus.ERROR, "Missing node to edit!"); //$NON-NLS-1$
            return;
        }
    }

    /**
     * Returns the Editor associated with this part.
     */
    public ManifestEditor getEditor() {
        return mEditor;
    }
    
    /**
     * Returns the {@link UiElementNode} associated with this part.
     */
    public UiElementNode getUiElementNode() {
        return mUiElementNode;
    }

    /**
     * Changes the element node handled by this part.
     * 
     * @param uiElementNode The new element node for the part. 
     */
    public void setUiElementNode(UiElementNode uiElementNode) {
        mUiElementNode = uiElementNode;
    }
    
    /**
     * Initializes the form part.
     * <p/>
     * This is called by the owning managed form as soon as the part is added to the form,
     * which happens right after the part is actually created.
     */
    @Override
    public void initialize(IManagedForm form) {
        super.initialize(form);
        createFormControls(form);
    }

    /**
     * Setup the section that contains this part.
     * <p/>
     * This is called by the constructor to set the section's title and description
     * with parameters given in the constructor.
     * <br/>
     * Derived class override this if needed, however in most cases the default
     * implementation should be enough.
     * 
     * @param sectionTitle The section part's title
     * @param sectionDescription The section part's description
     */
    protected void setupSection(String sectionTitle, String sectionDescription) {
        Section section = getSection();
        section.setText(sectionTitle);
        section.setDescription(sectionDescription);
    }

    /**
     * Create the controls to edit the attributes for the given ElementDescriptor.
     * <p/>
     * This MUST not be called by the constructor. Instead it must be called from
     * <code>initialize</code> (i.e. right after the form part is added to the managed form.)
     * <p/>
     * Derived classes can override this if necessary.
     * 
     * @param managedForm The owner managed form
     */
    protected void createFormControls(IManagedForm managedForm) {
        setTable(createTableLayout(managedForm.getToolkit(), 2 /* numColumns */));

        createUiAttributes(managedForm);
    }

    /**
     * Sets the table where the attribute UI needs to be created.
     */
    protected void setTable(Composite table) {
        mTable = table;
    }

    /**
     * Returns the table where the attribute UI needs to be created.
     */
    protected Composite getTable() {
        return mTable;
    }

    /**
     * Add all the attribute UI widgets into the underlying table layout.
     * 
     * @param managedForm The owner managed form
     */
    protected void createUiAttributes(IManagedForm managedForm) {
        Composite table = getTable();
        if (table == null || managedForm == null) {
            return;
        }

        // Remove any old UI controls 
        for (Control c : table.getChildren()) {
            c.dispose();
        }

        fillTable(table, managedForm);

        // Tell the section that the layout has changed.
        layoutChanged();
    }

    /**
     * Actually fills the table. 
     * This is called by {@link #createUiAttributes(IManagedForm)} to populate the new
     * table. The default implementation is to use
     * {@link #insertUiAttributes(UiElementNode, Composite, IManagedForm)} to actually
     * place the attributes of the default {@link UiElementNode} in the table.
     * <p/>
     * Derived classes can override this to add controls in the table before or after.
     * 
     * @param table The table to fill. It must have 2 columns.
     * @param managedForm The managed form for new controls.
     */
    protected void fillTable(Composite table, IManagedForm managedForm) {
        int inserted = insertUiAttributes(mUiElementNode, table, managedForm);
        
        if (inserted == 0) {
            createLabel(table, managedForm.getToolkit(),
                    "No attributes to display, waiting for SDK to finish loading...",
                    null /* tooltip */ );
        }
    }

    /**
     * Insert the UI attributes of the given {@link UiElementNode} in the given table.
     * 
     * @param uiNode The {@link UiElementNode} that contains the attributes to display.
     *               Must not be null.
     * @param table The table to fill. It must have 2 columns.
     * @param managedForm The managed form for new controls.
     * @return The number of UI attributes inserted. It is >= 0.
     */
    protected int insertUiAttributes(UiElementNode uiNode, Composite table, IManagedForm managedForm) {
        if (uiNode == null || table == null || managedForm == null) {
            return 0;
        }

        // To iterate over all attributes, we use the {@link ElementDescriptor} instead
        // of the {@link UiElementNode} because the attributes' order is guaranteed in the
        // descriptor but not in the node itself.
        AttributeDescriptor[] attr_desc_list = uiNode.getAttributeDescriptors();
        for (AttributeDescriptor attr_desc : attr_desc_list) {
            if (attr_desc instanceof XmlnsAttributeDescriptor) {
                // Do not show hidden attributes
                continue;
            }

            UiAttributeNode ui_attr = uiNode.findUiAttribute(attr_desc);
            if (ui_attr != null) {
                ui_attr.createUiControl(table, managedForm);
            } else {
                // The XML has an extra attribute which wasn't declared in
                // AndroidManifestDescriptors. This is not a problem, we just ignore it.
                AdtPlugin.log(IStatus.WARNING,
                        "Attribute %1$s not declared in node %2$s, ignored.", //$NON-NLS-1$
                        attr_desc.getXmlLocalName(),
                        uiNode.getDescriptor().getXmlName());
            }
        }
        return attr_desc_list.length;
    }

    /**
     * Tests whether the part is dirty i.e. its widgets have state that is
     * newer than the data in the model.
     * <p/>
     * This is done by iterating over all attributes and updating the super's
     * internal dirty flag. Stop once at least one attribute is dirty.
     * 
     * @return <code>true</code> if the part is dirty, <code>false</code>
     *         otherwise.
     */
    @Override
    public boolean isDirty() {
        if (mUiElementNode != null && !super.isDirty()) {
            for (UiAttributeNode ui_attr : mUiElementNode.getUiAttributes()) {
                if (ui_attr.isDirty()) {
                    markDirty();
                    break;
                }
            }
        }
        return super.isDirty();
    }
    
    /**
     * If part is displaying information loaded from a model, this method
     * instructs it to commit the new (modified) data back into the model.
     * 
     * @param onSave
     *            indicates if commit is called during 'save' operation or for
     *            some other reason (for example, if form is contained in a
     *            wizard or a multi-page editor and the user is about to leave
     *            the page).
     */
    @Override
    public void commit(boolean onSave) {
        if (mUiElementNode != null) {
            mEditor.editXmlModel(new Runnable() {
                public void run() {
                    for (UiAttributeNode ui_attr : mUiElementNode.getUiAttributes()) {
                        ui_attr.commit();
                    }
                }
            });
        }

        // We need to call super's commit after we synchronized the nodes to make sure we
        // reset the dirty flag after all the side effects from committing have occurred.
        super.commit(onSave);
    }
}
