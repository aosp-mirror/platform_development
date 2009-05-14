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

package com.android.ide.eclipse.adt.internal.editors.ui.tree;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.editors.AndroidEditor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.DescriptorsUtils;
import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.SeparatorAttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.XmlnsAttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.ui.SectionHelper;
import com.android.ide.eclipse.adt.internal.editors.ui.SectionHelper.ManifestSectionPart;
import com.android.ide.eclipse.adt.internal.editors.uimodel.IUiUpdateListener;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiAttributeNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;

import java.util.Collection;
import java.util.HashSet;

/**
 * Details page for the {@link UiElementNode} nodes in the tree view.
 * <p/>
 * See IDetailsBase for more details.
 */
class UiElementDetail implements IDetailsPage {

    /** The master-detail part, composed of a main tree and an auxiliary detail part */
    private ManifestSectionPart mMasterPart;

    private Section mMasterSection;
    private UiElementNode mCurrentUiElementNode;
    private Composite mCurrentTable;
    private boolean mIsDirty;

    private IManagedForm mManagedForm;

    private final UiTreeBlock mTree;
    
    public UiElementDetail(UiTreeBlock tree) {
        mTree = tree;
        mMasterPart = mTree.getMasterPart();
        mManagedForm = mMasterPart.getManagedForm();
    }
    
    /* (non-java doc)
     * Initializes the part.
     */
    public void initialize(IManagedForm form) {
        mManagedForm = form;
    }

    /* (non-java doc)
     * Creates the contents of the page in the provided parent.
     */
    public void createContents(Composite parent) {
        mMasterSection = createMasterSection(parent);
    }

    /* (non-java doc)
     * Called when the provided part has changed selection state.
     * <p/>
     * Only reply when our master part originates the selection.
     */
    public void selectionChanged(IFormPart part, ISelection selection) {
        if (part == mMasterPart &&
                !selection.isEmpty() &&
                selection instanceof ITreeSelection) {
            ITreeSelection tree_selection = (ITreeSelection) selection;

            Object first = tree_selection.getFirstElement();
            if (first instanceof UiElementNode) {
                UiElementNode ui_node = (UiElementNode) first;
                createUiAttributeControls(mManagedForm, ui_node);
            }
        }
    }

    /* (non-java doc)
     * Instructs it to commit the new (modified) data back into the model.
     */
    public void commit(boolean onSave) {
        
        IStructuredModel model = mTree.getEditor().getModelForEdit();
        try {
            // Notify the model we're about to change data...
            model.aboutToChangeModel();

            if (mCurrentUiElementNode != null) {
                mCurrentUiElementNode.commit();
            }
            
            // Finally reset the dirty flag if everything was saved properly
            mIsDirty = false;
        } catch (Exception e) {
            AdtPlugin.log(e, "Detail node failed to commit XML attribute!"); //$NON-NLS-1$
        } finally {
            // Notify the model we're done modifying it. This must *always* be executed.
            model.changedModel();
            model.releaseFromEdit();
        }
    }

    public void dispose() {
        // pass
    }


    /* (non-java doc)
     * Returns true if the part has been modified with respect to the data
     * loaded from the model.
     */
    public boolean isDirty() {
        if (mCurrentUiElementNode != null && mCurrentUiElementNode.isDirty()) {
            markDirty();
        }
        return mIsDirty;
    }

    public boolean isStale() {
        // pass
        return false;
    }

    /**
     * Called by the master part when the tree is refreshed after the framework resources
     * have been reloaded.
     */
    public void refresh() {
        if (mCurrentTable != null) {
            mCurrentTable.dispose();
            mCurrentTable = null;
        }
        mCurrentUiElementNode = null;
        mMasterSection.getParent().pack(true /* changed */);
    }

    public void setFocus() {
        // pass
    }

    public boolean setFormInput(Object input) {
        // pass
        return false;
    }

    /**
     * Creates a TableWrapLayout in the DetailsPage, which in turns contains a Section.
     * 
     * All the UI should be created in a layout which parent is the mSection itself.
     * The hierarchy is:
     * <pre>
     * DetailPage
     * + TableWrapLayout
     *   + Section (with title/description && fill_grab horizontal)
     *     + TableWrapLayout [*]
     *       + Labels/Forms/etc... [*]
     * </pre>
     * Both items marked with [*] are created by the derived classes to fit their needs.
     * 
     * @param parent Parent of the mSection (from createContents)
     * @return The new Section
     */
    private Section createMasterSection(Composite parent) {
        TableWrapLayout layout = new TableWrapLayout();
        layout.topMargin = 0;
        parent.setLayout(layout);

        FormToolkit toolkit = mManagedForm.getToolkit();
        Section section = toolkit.createSection(parent, Section.TITLE_BAR);
        section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP));
        return section;
    }

    /**
     * Create the ui attribute controls to edit the attributes for the given
     * ElementDescriptor.
     * <p/>
     * This is called by the constructor.
     * Derived classes can override this if necessary.
     * 
     * @param managedForm The managed form
     */
    private void createUiAttributeControls(
            final IManagedForm managedForm,
            final UiElementNode ui_node) {

        final ElementDescriptor elem_desc = ui_node.getDescriptor();
        mMasterSection.setText(String.format("Attributes for %1$s", ui_node.getShortDescription()));

        if (mCurrentUiElementNode != ui_node) {
            // Before changing the table, commit all dirty state.
            if (mIsDirty) {
                commit(false);
            }
            if (mCurrentTable != null) {
                mCurrentTable.dispose();
                mCurrentTable = null;
            }

            // To iterate over all attributes, we use the {@link ElementDescriptor} instead
            // of the {@link UiElementNode} because the attributes order is guaranteed in the
            // descriptor but not in the node itself.
            AttributeDescriptor[] attr_desc_list = ui_node.getAttributeDescriptors();

            // If the attribute list contains at least one SeparatorAttributeDescriptor,
            // sub-sections will be used. This needs to be known early as it influences the
            // creation of the master table.
            boolean useSubsections = false;
            for (AttributeDescriptor attr_desc : attr_desc_list) {
                if (attr_desc instanceof SeparatorAttributeDescriptor) {
                    // Sub-sections will be used. The default sections should no longer be
                    useSubsections = true;
                    break;
                }
            }

            FormToolkit toolkit = managedForm.getToolkit();
            Composite masterTable = SectionHelper.createTableLayout(mMasterSection,
                    toolkit, useSubsections ? 1 : 2 /* numColumns */);
            mCurrentTable = masterTable;

            mCurrentUiElementNode = ui_node;
                
            if (elem_desc.getTooltip() != null) {
                String tooltip;
                if (Sdk.getCurrent() != null &&
                        Sdk.getCurrent().getDocumentationBaseUrl() != null) {
                    tooltip = DescriptorsUtils.formatFormText(elem_desc.getTooltip(),
                            elem_desc,
                            Sdk.getCurrent().getDocumentationBaseUrl());
                } else {
                    tooltip = elem_desc.getTooltip();
                }

                try {
                    FormText text = SectionHelper.createFormText(masterTable, toolkit,
                            true /* isHtml */, tooltip, true /* setupLayoutData */);
                    text.addHyperlinkListener(mTree.getEditor().createHyperlinkListener());
                    Image icon = elem_desc.getIcon();
                    if (icon != null) {
                        text.setImage(DescriptorsUtils.IMAGE_KEY, icon);
                    }
                } catch(Exception e) {
                    // The FormText parser is really really basic and will fail as soon as the
                    // HTML javadoc is ever so slightly malformatted.
                    AdtPlugin.log(e,
                            "Malformed javadoc, rejected by FormText for node %1$s: '%2$s'", //$NON-NLS-1$
                            ui_node.getDescriptor().getXmlName(),
                            tooltip);
                    
                    // Fallback to a pure text tooltip, no fancy HTML
                    tooltip = DescriptorsUtils.formatTooltip(elem_desc.getTooltip());
                    Label label = SectionHelper.createLabel(masterTable, toolkit,
                            tooltip, tooltip);
                }
            }

            Composite table = useSubsections ? null : masterTable;
            
            for (AttributeDescriptor attr_desc : attr_desc_list) {
                if (attr_desc instanceof XmlnsAttributeDescriptor) {
                    // Do not show hidden attributes
                    continue;
                } else if (table == null || attr_desc instanceof SeparatorAttributeDescriptor) {
                    String title = null;
                    if (attr_desc instanceof SeparatorAttributeDescriptor) {
                        // xmlName is actually the label of the separator
                        title = attr_desc.getXmlLocalName();
                    } else {
                        title = String.format("Attributes from %1$s", elem_desc.getUiName());
                    }

                    table = createSubSectionTable(toolkit, masterTable, title);
                    if (attr_desc instanceof SeparatorAttributeDescriptor) {
                        continue;
                    }
                }

                UiAttributeNode ui_attr = ui_node.findUiAttribute(attr_desc);

                if (ui_attr != null) {
                    ui_attr.createUiControl(table, managedForm);
                    
                    if (ui_attr.getCurrentValue() != null &&
                            ui_attr.getCurrentValue().length() > 0) {
                        ((Section) table.getParent()).setExpanded(true);
                    }
                } else {
                    // The XML has an extra unknown attribute.
                    // This is not expected to happen so it is ignored.
                    AdtPlugin.log(IStatus.INFO,
                            "Attribute %1$s not declared in node %2$s, ignored.", //$NON-NLS-1$
                            attr_desc.getXmlLocalName(),
                            ui_node.getDescriptor().getXmlName());
                }
            }

            // Create a sub-section for the unknown attributes.
            // It is initially hidden till there are some attributes to show here.
            final Composite unknownTable = createSubSectionTable(toolkit, masterTable,
                    "Unknown XML Attributes");
            unknownTable.getParent().setVisible(false); // set section to not visible
            final HashSet<UiAttributeNode> reference = new HashSet<UiAttributeNode>();
            
            final IUiUpdateListener updateListener = new IUiUpdateListener() {
                public void uiElementNodeUpdated(UiElementNode ui_node, UiUpdateState state) {
                    if (state == UiUpdateState.ATTR_UPDATED) {
                        updateUnknownAttributesSection(ui_node, unknownTable, managedForm,
                                reference);
                    }
                }
            };
            ui_node.addUpdateListener(updateListener);
            
            // remove the listener when the UI is disposed
            unknownTable.addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent e) {
                    ui_node.removeUpdateListener(updateListener);
                }
            });
            
            updateUnknownAttributesSection(ui_node, unknownTable, managedForm, reference);
            
            mMasterSection.getParent().pack(true /* changed */);
        }
    }

    /**
     * Create a sub Section and its embedding wrapper table with 2 columns.
     * @return The table, child of a new section.
     */
    private Composite createSubSectionTable(FormToolkit toolkit,
            Composite masterTable, String title) {
        
        // The Section composite seems to ignore colspan when assigned a TableWrapData so
        // if the parent is a table with more than one column an extra table with one column
        // is inserted to respect colspan.
        int parentNumCol = ((TableWrapLayout) masterTable.getLayout()).numColumns;
        if (parentNumCol > 1) {
            masterTable = SectionHelper.createTableLayout(masterTable, toolkit, 1);
            TableWrapData twd = new TableWrapData(TableWrapData.FILL_GRAB);
            twd.maxWidth = AndroidEditor.TEXT_WIDTH_HINT;
            twd.colspan = parentNumCol;
            masterTable.setLayoutData(twd);
        }
        
        Composite table;
        Section section = toolkit.createSection(masterTable,
                Section.TITLE_BAR | Section.TWISTIE);

        // Add an expansion listener that will trigger a reflow on the parent
        // ScrolledPageBook (which is actually a SharedScrolledComposite). This will
        // recompute the correct size and adjust the scrollbar as needed.
        section.addExpansionListener(new IExpansionListener() {
            public void expansionStateChanged(ExpansionEvent e) {
                reflowMasterSection();
            }

            public void expansionStateChanging(ExpansionEvent e) {
                // pass
            }
        });

        section.setText(title);
        section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB,
                                                TableWrapData.TOP));
        table = SectionHelper.createTableLayout(section, toolkit, 2 /* numColumns */);
        return table;
    }

    /**
     * Reflow the parent ScrolledPageBook (which is actually a SharedScrolledComposite).
     * This will recompute the correct size and adjust the scrollbar as needed.
     */
    private void reflowMasterSection() {
        for(Composite c = mMasterSection; c != null; c = c.getParent()) {
            if (c instanceof SharedScrolledComposite) {
                ((SharedScrolledComposite) c).reflow(true /* flushCache */);
                break;
            }
        }
    }

    /**
     * Updates the unknown attributes section for the UI Node.
     */
    private void updateUnknownAttributesSection(UiElementNode ui_node,
            final Composite unknownTable, final IManagedForm managedForm,
            HashSet<UiAttributeNode> reference) {
        Collection<UiAttributeNode> ui_attrs = ui_node.getUnknownUiAttributes();
        Section section = ((Section) unknownTable.getParent());
        boolean needs_reflow = false;

        // The table was created hidden, show it if there are unknown attributes now
        if (ui_attrs.size() > 0 && !section.isVisible()) {
            section.setVisible(true);
            needs_reflow = true;
        }

        // Compare the new attribute set with the old "reference" one
        boolean has_differences = ui_attrs.size() != reference.size();
        if (!has_differences) {
            for (UiAttributeNode ui_attr : ui_attrs) {
                if (!reference.contains(ui_attr)) {
                    has_differences = true;
                    break;
                }
            }
        }

        if (has_differences) {
            needs_reflow = true;
            reference.clear();
            
            // Remove all children of the table
            for (Control c : unknownTable.getChildren()) {
                c.dispose();
            }
    
            // Recreate all attributes UI
            for (UiAttributeNode ui_attr : ui_attrs) {
                reference.add(ui_attr);
                ui_attr.createUiControl(unknownTable, managedForm);
    
                if (ui_attr.getCurrentValue() != null && ui_attr.getCurrentValue().length() > 0) {
                    section.setExpanded(true);
                }
            }
        }
        
        if (needs_reflow) {
            reflowMasterSection();
        }
    }
    
    /**
     * Marks the part dirty. Called as a result of user interaction with the widgets in the
     * section.
     */
    private void markDirty() {
        if (!mIsDirty) {
            mIsDirty = true;
            mManagedForm.dirtyStateChanged();
        }
    }
}


