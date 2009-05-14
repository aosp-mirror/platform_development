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

package com.android.ide.eclipse.adt.internal.wizards.export;

import com.android.ide.eclipse.adt.internal.project.ProjectHelper;
import com.android.ide.eclipse.adt.internal.wizards.export.ExportWizard.ExportWizardPage;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.io.File;

/**
 * Keystore selection page. This page allows to choose to create a new keystore or use an
 * existing one. 
 */
final class KeystoreSelectionPage extends ExportWizardPage {

    private final ExportWizard mWizard;
    private Button mUseExistingKeystore;
    private Button mCreateKeystore;
    private Text mKeystore;
    private Text mKeystorePassword;
    private Label mConfirmLabel;
    private Text mKeystorePassword2;
    private boolean mDisableOnChange = false;

    protected KeystoreSelectionPage(ExportWizard wizard, String pageName) {
        super(pageName);
        mWizard = wizard;

        setTitle("Keystore selection");
        setDescription(""); //TODO
    }

    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout gl = new GridLayout(3, false);
        composite.setLayout(gl);
        
        GridData gd;
        
        mUseExistingKeystore = new Button(composite, SWT.RADIO);
        mUseExistingKeystore.setText("Use existing keystore");
        mUseExistingKeystore.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.horizontalSpan = 3;
        mUseExistingKeystore.setSelection(true);

        mCreateKeystore = new Button(composite, SWT.RADIO);
        mCreateKeystore.setText("Create new keystore");
        mCreateKeystore.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.horizontalSpan = 3;

        new Label(composite, SWT.NONE).setText("Location:");
        mKeystore = new Text(composite, SWT.BORDER);
        mKeystore.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        final Button browseButton = new Button(composite, SWT.PUSH);
        browseButton.setText("Browse...");
        browseButton.addSelectionListener(new SelectionAdapter() {
           @Override
           public void widgetSelected(SelectionEvent e) {
               FileDialog fileDialog;
               if (mUseExistingKeystore.getSelection()) {
                   fileDialog = new FileDialog(browseButton.getShell(),SWT.OPEN);
                   fileDialog.setText("Load Keystore");
               } else {
                   fileDialog = new FileDialog(browseButton.getShell(),SWT.SAVE);
                   fileDialog.setText("Select Keystore Name");
               }

               String fileName = fileDialog.open();
               if (fileName != null) {
                   mKeystore.setText(fileName);
               }
           }
        });

        new Label(composite, SWT.NONE).setText("Password:");
        mKeystorePassword = new Text(composite, SWT.BORDER | SWT.PASSWORD);
        mKeystorePassword.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        mKeystorePassword.addVerifyListener(sPasswordVerifier);
        new Composite(composite, SWT.NONE).setLayoutData(gd = new GridData());
        gd.heightHint = gd.widthHint = 0;

        mConfirmLabel = new Label(composite, SWT.NONE);
        mConfirmLabel.setText("Confirm:");
        mKeystorePassword2 = new Text(composite, SWT.BORDER | SWT.PASSWORD);
        mKeystorePassword2.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        mKeystorePassword2.addVerifyListener(sPasswordVerifier);
        new Composite(composite, SWT.NONE).setLayoutData(gd = new GridData());
        gd.heightHint = gd.widthHint = 0;
        mKeystorePassword2.setEnabled(false);

        // Show description the first time
        setErrorMessage(null);
        setMessage(null);
        setControl(composite);
        
        mUseExistingKeystore.addSelectionListener(new SelectionAdapter() {
           @Override
           public void widgetSelected(SelectionEvent e) {
               boolean createStore = !mUseExistingKeystore.getSelection();
               mKeystorePassword2.setEnabled(createStore);
               mConfirmLabel.setEnabled(createStore);
               mWizard.setKeystoreCreationMode(createStore);
               onChange();
            }
        });
        
        mKeystore.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                mWizard.setKeystore(mKeystore.getText().trim());
                onChange();
            }
        });

        mKeystorePassword.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                mWizard.setKeystorePassword(mKeystorePassword.getText());
                onChange();
            }
        });

        mKeystorePassword2.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                onChange();
            }
        });
    }
    
    @Override
    public IWizardPage getNextPage() {
        if (mUseExistingKeystore.getSelection()) {
            return mWizard.getKeySelectionPage();
        }
        
        return mWizard.getKeyCreationPage();
    }
    
    @Override
    void onShow() {
        // fill the texts with information loaded from the project.
        if ((mProjectDataChanged & DATA_PROJECT) != 0) {
            // reset the keystore/alias from the content of the project
            IProject project = mWizard.getProject();
            
            // disable onChange for now. we'll call it once at the end.
            mDisableOnChange = true;
            
            String keystore = ProjectHelper.loadStringProperty(project,
                    ExportWizard.PROPERTY_KEYSTORE);
            if (keystore != null) {
                mKeystore.setText(keystore);
            }
            
            // reset the passwords
            mKeystorePassword.setText(""); //$NON-NLS-1$
            mKeystorePassword2.setText(""); //$NON-NLS-1$
            
            // enable onChange, and call it to display errors and enable/disable pageCompleted.
            mDisableOnChange = false;
            onChange();
        }
    }

    /**
     * Handles changes and update the error message and calls {@link #setPageComplete(boolean)}.
     */
    private void onChange() {
        if (mDisableOnChange) {
            return;
        }

        setErrorMessage(null);
        setMessage(null);

        boolean createStore = !mUseExistingKeystore.getSelection();

        // checks the keystore path is non null.
        String keystore = mKeystore.getText().trim();
        if (keystore.length() == 0) {
            setErrorMessage("Enter path to keystore.");
            setPageComplete(false);
            return;
        } else {
            File f = new File(keystore);
            if (f.exists() == false) {
                if (createStore == false) {
                    setErrorMessage("Keystore does not exist.");
                    setPageComplete(false);
                    return;
                }
            } else if (f.isDirectory()) {
                setErrorMessage("Keystore path is a directory.");
                setPageComplete(false);
                return;
            } else if (f.isFile()) {
                if (createStore) {
                    setErrorMessage("File already exists.");
                    setPageComplete(false);
                    return;
                }
            }
        }
        
        String value = mKeystorePassword.getText();
        if (value.length() == 0) {
            setErrorMessage("Enter keystore password.");
            setPageComplete(false);
            return;
        } else if (createStore && value.length() < 6) {
            setErrorMessage("Keystore password is too short - must be at least 6 characters.");
            setPageComplete(false);
            return;
        }

        if (createStore) {
            if (mKeystorePassword2.getText().length() == 0) {
                setErrorMessage("Confirm keystore password.");
                setPageComplete(false);
                return;
            }
            
            if (mKeystorePassword.getText().equals(mKeystorePassword2.getText()) == false) {
                setErrorMessage("Keystore passwords do not match.");
                setPageComplete(false);
                return;
            }
        }

        setPageComplete(true);
    }
}
