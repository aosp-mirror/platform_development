/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.sdkuilib;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Edit dialog to create/edit APK configuration. The dialog displays 2 text fields for the config
 * name and its filter.
 */
class ApkConfigEditDialog extends Dialog implements ModifyListener, VerifyListener {

    private String mName;
    private String mFilter;
    private Text mNameField;
    private Text mFilterField;
    private Button mOkButton;
    
    /**
     * Creates an edit dialog with optional initial values for the name and filter.
     * @param name optional value for the name. Can be null.
     * @param filter optional value for the filter. Can be null.
     * @param parentShell the parent shell.
     */
    protected ApkConfigEditDialog(String name, String filter, Shell parentShell) {
        super(parentShell);
        mName = name;
        mFilter = filter;
    }
    
    /**
     * Returns the name of the config. This is only valid if the user clicked OK and {@link #open()}
     * returned {@link Window#OK}
     */
    public String getName() {
        return mName;
    }
    
    /**
     * Returns the filter for the config. This is only valid if the user clicked OK and
     * {@link #open()} returned {@link Window#OK}
     */
    public String getFilter() {
        return mFilter;
    }
    
    @Override
    protected Control createContents(Composite parent) {
        Control control = super.createContents(parent);

        mOkButton = getButton(IDialogConstants.OK_ID);
        validateButtons();

        return control;
    }
    
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout;
        composite.setLayout(layout = new GridLayout(2, false));
        layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(
                IDialogConstants.HORIZONTAL_SPACING);

        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        Label l = new Label(composite, SWT.NONE);
        l.setText("Name");
        
        mNameField = new Text(composite, SWT.BORDER);
        mNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mNameField.addVerifyListener(this);
        if (mName != null) {
            mNameField.setText(mName);
        }
        mNameField.addModifyListener(this);

        l = new Label(composite, SWT.NONE);
        l.setText("Filter");
        
        mFilterField = new Text(composite, SWT.BORDER);
        mFilterField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (mFilter != null) {
            mFilterField.setText(mFilter);
        }
        mFilterField.addVerifyListener(this);
        mFilterField.addModifyListener(this);
        
        applyDialogFont(composite);
        return composite;
    }
    
    /**
     * Validates the OK button based on the content of the 2 text fields.
     */
    private void validateButtons() {
        mOkButton.setEnabled(mNameField.getText().trim().length() > 0 &&
                mFilterField.getText().trim().length() > 0);
    }

    @Override
    protected void okPressed() {
        mName = mNameField.getText();
        mFilter = mFilterField.getText().trim();
        super.okPressed();
    }

    /**
     * Callback for text modification in the 2 text fields.
     */
    public void modifyText(ModifyEvent e) {
        validateButtons();
    }

    /**
     * Callback to ensure the content of the text field are proper.
     */
    public void verifyText(VerifyEvent e) {
        Text source = ((Text)e.getSource());
        if (source == mNameField) {
            // check for a-zA-Z0-9.
            final String text = e.text;
            final int len = text.length();
            for (int i = 0 ; i < len; i++) {
                char letter = text.charAt(i);
                if (letter > 255 || Character.isLetterOrDigit(letter) == false) {
                    e.doit = false;
                    return;
                }
            }
        } else if (source == mFilterField) {
            // we can't validate the content as its typed, but we can at least ensure the characters
            // are valid. Same as mNameFiled + the comma.
            final String text = e.text;
            final int len = text.length();
            for (int i = 0 ; i < len; i++) {
                char letter = text.charAt(i);
                if (letter > 255 || (Character.isLetterOrDigit(letter) == false && letter != ',')) {
                    e.doit = false;
                    return;
                }
            }
        }
    }
}
