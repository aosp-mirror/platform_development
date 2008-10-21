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

package com.android.ide.eclipse.adt.project.export;

import com.android.ide.eclipse.adt.project.ProjectHelper;
import com.android.ide.eclipse.adt.project.export.ExportWizard.ExportWizardPage;

import org.eclipse.core.resources.IProject;
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
 * Second export wizard page. 
 */
public class SigningExportPage extends ExportWizardPage {

    private final ExportWizard mWizard;
    private Text mKeystore;
    private Text mAlias;
    private Text mKeystorePassword;
    private Text mKeyPassword;
    private boolean mDisableOnChange = false;

    protected SigningExportPage(ExportWizard wizard, String pageName) {
        super(pageName);
        mWizard = wizard;

        setTitle("Application Signing");
        setDescription("Defines which store, key and certificate to use to sign the Android Application.");
    }

    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout gl = new GridLayout(3, false);
        composite.setLayout(gl);
        
        GridData gd;

        new Label(composite, SWT.NONE).setText("Keystore:");
        mKeystore = new Text(composite, SWT.BORDER);
        mKeystore.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        final Button browseButton = new Button(composite, SWT.PUSH);
        browseButton.setText("Browse...");
        browseButton.addSelectionListener(new SelectionAdapter() {
           @Override
           public void widgetSelected(SelectionEvent e) {
               FileDialog fileDialog = new FileDialog(browseButton.getShell(), SWT.OPEN);
               fileDialog.setText("Load Keystore");

               String fileName = fileDialog.open();
               if (fileName != null) {
                   mKeystore.setText(fileName);
               }
           }
        });
        
        new Composite(composite, SWT.NONE).setLayoutData(gd = new GridData());
        gd.horizontalSpan = 2;
        gd.heightHint = 0;
        new Button(composite, SWT.PUSH).setText("New...");

        new Label(composite, SWT.NONE).setText("Key Alias:");
        mAlias = new Text(composite, SWT.BORDER);
        mAlias.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));

        new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.horizontalSpan = 3;

        new Label(composite, SWT.NONE).setText("Store password:");
        mKeystorePassword = new Text(composite, SWT.BORDER | SWT.PASSWORD);
        mKeystorePassword.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        new Composite(composite, SWT.NONE).setLayoutData(gd = new GridData());
        gd.heightHint = gd.widthHint = 0;

        new Label(composite, SWT.NONE).setText("Key password:");
        mKeyPassword = new Text(composite, SWT.BORDER | SWT.PASSWORD);
        mKeyPassword.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));

        // Show description the first time
        setErrorMessage(null);
        setMessage(null);
        setControl(composite);
        
        mKeystore.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                mWizard.setKeystore(mKeystore.getText().trim());
                onChange();
            }
        });
        mAlias.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                mWizard.setKeyAlias(mAlias.getText().trim());
                onChange();
            }
        });
        mKeystorePassword.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                mWizard.setKeystorePassword(mKeystorePassword.getText().trim().toCharArray());
                onChange();
            }
        });
        mKeyPassword.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                mWizard.setKeyPassword(mKeyPassword.getText().trim().toCharArray());
                onChange();
            }
        });
    }
    
    @Override
    void onShow() {
        // fill the texts with information loaded from the project.
        if (mNewProjectReference) {
            // reset the keystore/alias from the content of the project
            IProject project = mWizard.getProject();
            
            // disable onChange for now. we'll call it once at the end.
            mDisableOnChange = true;
            
            String keystore = ProjectHelper.loadStringProperty(project,
                    ExportWizard.PROPERTY_KEYSTORE);
            if (keystore != null) {
                mKeystore.setText(keystore);
            }
            
            String alias = ProjectHelper.loadStringProperty(project, ExportWizard.PROPERTY_ALIAS);
            if (alias != null) {
                mAlias.setText(alias);
            }
            
            // reset the passwords
            mKeystorePassword.setText(""); //$NON-NLS-1$
            mKeyPassword.setText(""); //$NON-NLS-1$
            
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

        // checks the keystore path is non null.
        String keystore = mKeystore.getText().trim();
        if (keystore.length() == 0) {
            setErrorMessage("Enter path to keystore.");
            setPageComplete(false);
            return;
        } else {
            File f = new File(keystore);
            if (f.exists() == false) {
                setErrorMessage("Keystore does not exists!");
                setPageComplete(false);
                return;
            } else if (f.isDirectory()) {
                setErrorMessage("Keystore is a directory!");
                setPageComplete(false);
                return;
            }
        }
        
        if (mAlias.getText().trim().length() == 0) {
            setErrorMessage("Enter key alias.");
            setPageComplete(false);
            return;
        }

        if (mKeystorePassword.getText().trim().length() == 0) {
            setErrorMessage("Enter keystore password.");
            setPageComplete(false);
            return;
        }

        if (mKeyPassword.getText().trim().length() == 0) {
            setErrorMessage("Enter key password.");
            setPageComplete(false);
            return;
        }

        setPageComplete(true);
    }
}
