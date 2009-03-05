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

package com.android.ide.eclipse.editors.uimodel;

import com.android.ide.eclipse.adt.sdk.AndroidTargetData;
import com.android.ide.eclipse.editors.AndroidEditor;
import com.android.ide.eclipse.editors.descriptors.DescriptorsUtils;
import com.android.ide.eclipse.editors.descriptors.FlagAttributeDescriptor;
import com.android.ide.eclipse.editors.descriptors.TextAttributeDescriptor;
import com.android.ide.eclipse.editors.ui.SectionHelper;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SelectionStatusDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents an XML attribute that is defined by a set of flag values,
 * i.e. enum names separated by pipe (|) characters.
 * 
 * Note: in Android resources, a "flag" is a list of fixed values where one or
 * more values can be selected using an "or", e.g. "align='left|top'".
 * By contrast, an "enum" is a list of fixed values of which only one can be
 * selected at a given time, e.g. "gravity='right'".
 * <p/>
 * This class handles the "flag" case.
 * The "enum" case is done using {@link UiListAttributeNode}.
 */
public class UiFlagAttributeNode extends UiTextAttributeNode {

    public UiFlagAttributeNode(FlagAttributeDescriptor attributeDescriptor,
            UiElementNode uiParent) {
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
        FormToolkit toolkit = managedForm.getToolkit();
        TextAttributeDescriptor desc = (TextAttributeDescriptor) getDescriptor();

        Label label = toolkit.createLabel(parent, desc.getUiName());
        label.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));
        SectionHelper.addControlTooltip(label, DescriptorsUtils.formatTooltip(desc.getTooltip()));

        Composite composite = toolkit.createComposite(parent);
        composite.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.MIDDLE));
        GridLayout gl = new GridLayout(2, false);
        gl.marginHeight = gl.marginWidth = 0;
        composite.setLayout(gl);
        // Fixes missing text borders under GTK... also requires adding a 1-pixel margin
        // for the text field below
        toolkit.paintBordersFor(composite);
        
        final Text text = toolkit.createText(composite, getCurrentValue());
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalIndent = 1;  // Needed by the fixed composite borders under GTK
        text.setLayoutData(gd);
        final Button selectButton = toolkit.createButton(composite, "Select...", SWT.PUSH);
        
        setTextWidget(text);
        
        selectButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);

                String currentText = getTextWidgetValue();
                
                String result = showDialog(selectButton.getShell(), currentText);
                
                if (result != null) {
                    setTextWidgetValue(result);
                }
            }
        });
    }

    /**
     * Get the flag names, either from the initial names set in the attribute
     * or by querying the framework resource parser.
     * 
     * {@inheritDoc}
     */
    @Override
    public String[] getPossibleValues(String prefix) {
        String attr_name = getDescriptor().getXmlLocalName();
        String element_name = getUiParent().getDescriptor().getXmlName();
        
        String[] values = null;
        
        if (getDescriptor() instanceof FlagAttributeDescriptor &&
                ((FlagAttributeDescriptor) getDescriptor()).getNames() != null) {
            // Get enum values from the descriptor
            values = ((FlagAttributeDescriptor) getDescriptor()).getNames();
        }

        if (values == null) {
            // or from the AndroidTargetData
            UiElementNode uiNode = getUiParent();
            AndroidEditor editor = uiNode.getEditor();
            AndroidTargetData data = editor.getTargetData();
            if (data != null) {
                values = data.getAttributeValues(element_name, attr_name);
            }
        }
        
        return values;
    }
    
    /**
     * Shows a dialog letting the user choose a set of enum, and returns a string
     * containing the result.
     */
    public String showDialog(Shell shell, String currentValue) {
        FlagSelectionDialog dlg = new FlagSelectionDialog(
                shell, currentValue.trim().split("\\s*\\|\\s*")); //$NON-NLS-1$
        dlg.open();
        Object[] result = dlg.getResult();
        if (result != null) {
            StringBuilder buf = new StringBuilder();
            for (Object name : result) {
                if (name instanceof String) {
                    if (buf.length() > 0) {
                        buf.append("|"); //$NON-NLS-1$
                    }
                    buf.append(name);
                }
            }
            
            return buf.toString();
        }
        
        return null;

    }
    
    /**
     * Displays a list of flag names with checkboxes.
     */
    private class FlagSelectionDialog extends SelectionStatusDialog {

        private Set<String> mCurrentSet;
        private Table mTable;

        public FlagSelectionDialog(Shell parentShell, String[] currentNames) {
            super(parentShell);
            
            mCurrentSet = new HashSet<String>();
            for (String name : currentNames) {
                if (name.length() > 0) {
                    mCurrentSet.add(name);
                }
            }

            int shellStyle = getShellStyle();
            setShellStyle(shellStyle | SWT.MAX | SWT.RESIZE);
        }

        @Override
        protected void computeResult() {
            if (mTable != null) {
                ArrayList<String> results = new ArrayList<String>();
                
                for (TableItem item : mTable.getItems()) {
                    if (item.getChecked()) {
                        results.add((String)item.getData());
                    }
                }
                
                setResult(results);
            }
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            Composite composite= new Composite(parent, SWT.NONE);
            composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            composite.setLayout(new GridLayout(1, true));
            composite.setFont(parent.getFont());
            
            Label label = new Label(composite, SWT.NONE);
            label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            label.setText(String.format("Select the flag values for attribute %1$s:",
                    ((FlagAttributeDescriptor) getDescriptor()).getUiName()));
 
            mTable = new Table(composite, SWT.CHECK | SWT.BORDER);
            GridData data = new GridData();
            // The 60,18 hints are the ones used by AbstractElementListSelectionDialog
            data.widthHint = convertWidthInCharsToPixels(60);
            data.heightHint = convertHeightInCharsToPixels(18);
            data.grabExcessVerticalSpace = true;
            data.grabExcessHorizontalSpace = true;
            data.horizontalAlignment = GridData.FILL;
            data.verticalAlignment = GridData.FILL;
            mTable.setLayoutData(data);

            mTable.setHeaderVisible(false);
            final TableColumn column = new TableColumn(mTable, SWT.NONE);

            // List all the expected flag names and check those which are currently used
            String[] names = getPossibleValues(null);
            if (names != null) {
                for (String name : names) {
                    TableItem item = new TableItem(mTable, SWT.NONE);
                    item.setText(name);
                    item.setData(name);
                    
                    boolean hasName = mCurrentSet.contains(name);
                    item.setChecked(hasName);
                    if (hasName) {
                        mCurrentSet.remove(name);
                    }
                }
            }

            // If there are unknown flag names currently used, display them at the end if the
            // table already checked.
            if (!mCurrentSet.isEmpty()) {
                FontDescriptor fontDesc = JFaceResources.getDialogFontDescriptor();
                fontDesc = fontDesc.withStyle(SWT.ITALIC);
                Font font = fontDesc.createFont(JFaceResources.getDialogFont().getDevice());

                for (String name : mCurrentSet) {
                    TableItem item = new TableItem(mTable, SWT.NONE);
                    item.setText(String.format("%1$s (unknown flag)", name));
                    item.setData(name);
                    item.setChecked(true);
                    item.setFont(font);
                }
            }
            
            // Add a listener that will resize the column to the full width of the table
            // so that only one column appears in the table even if the dialog is resized.
            ControlAdapter listener = new ControlAdapter() {
                @Override
                public void controlResized(ControlEvent e) {
                    Rectangle r = mTable.getClientArea();
                    column.setWidth(r.width);
                }
            };
            
            mTable.addControlListener(listener);
            listener.controlResized(null /* event not used */);

            // Add a selection listener that will check/uncheck items when they are double-clicked
            mTable.addSelectionListener(new SelectionAdapter() {
                /** Default selection means double-click on "most" platforms */
                @Override
                public void widgetDefaultSelected(SelectionEvent e) {
                    if (e.item instanceof TableItem) {
                        TableItem i = (TableItem) e.item;
                        i.setChecked(!i.getChecked());
                    }
                    super.widgetDefaultSelected(e);
                } 
            });
            
            Dialog.applyDialogFont(composite);            
            setHelpAvailable(false);
            
            return composite;
        }
    }
}
