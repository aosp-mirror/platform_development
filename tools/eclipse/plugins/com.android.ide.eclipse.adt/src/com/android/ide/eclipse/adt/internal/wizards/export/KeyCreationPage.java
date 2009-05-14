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
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.util.List;

/**
 * Key creation page. 
 */
final class KeyCreationPage extends ExportWizardPage {

    private final ExportWizard mWizard;
    private Text mAlias;
    private Text mKeyPassword;
    private Text mKeyPassword2;
    private Text mCnField;
    private boolean mDisableOnChange = false;
    private Text mOuField;
    private Text mOField;
    private Text mLField;
    private Text mStField;
    private Text mCField;
    private String mDName;
    private int mValidity = 0;
    private List<String> mExistingAliases;

    
    protected KeyCreationPage(ExportWizard wizard, String pageName) {
        super(pageName);
        mWizard = wizard;

        setTitle("Key Creation");
        setDescription(""); // TODO?
    }

    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout gl = new GridLayout(2, false);
        composite.setLayout(gl);
        
        GridData gd;

        new Label(composite, SWT.NONE).setText("Alias:");
        mAlias = new Text(composite, SWT.BORDER);
        mAlias.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));

        new Label(composite, SWT.NONE).setText("Password:");
        mKeyPassword = new Text(composite, SWT.BORDER | SWT.PASSWORD);
        mKeyPassword.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        mKeyPassword.addVerifyListener(sPasswordVerifier);

        new Label(composite, SWT.NONE).setText("Confirm:");
        mKeyPassword2 = new Text(composite, SWT.BORDER | SWT.PASSWORD);
        mKeyPassword2.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        mKeyPassword2.addVerifyListener(sPasswordVerifier);

        new Label(composite, SWT.NONE).setText("Validity (years):");
        final Text validityText = new Text(composite, SWT.BORDER);
        validityText.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        validityText.addVerifyListener(new VerifyListener() {
            public void verifyText(VerifyEvent e) {
                // check for digit only.
                for (int i = 0 ; i < e.text.length(); i++) {
                    char letter = e.text.charAt(i);
                    if (letter < '0' || letter > '9') {
                        e.doit = false;
                        return;
                    }
                }
            }
        });

        new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(
                gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.horizontalSpan = 2;
        
        new Label(composite, SWT.NONE).setText("First and Last Name:");
        mCnField = new Text(composite, SWT.BORDER);
        mCnField.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));

        new Label(composite, SWT.NONE).setText("Organizational Unit:");
        mOuField = new Text(composite, SWT.BORDER);
        mOuField.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));

        new Label(composite, SWT.NONE).setText("Organization:");
        mOField = new Text(composite, SWT.BORDER);
        mOField.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));

        new Label(composite, SWT.NONE).setText("City or Locality:");
        mLField = new Text(composite, SWT.BORDER);
        mLField.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));

        new Label(composite, SWT.NONE).setText("State or Province:");
        mStField = new Text(composite, SWT.BORDER);
        mStField.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        
        new Label(composite, SWT.NONE).setText("Country Code (XX):");
        mCField = new Text(composite, SWT.BORDER);
        mCField.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));

        // Show description the first time
        setErrorMessage(null);
        setMessage(null);
        setControl(composite);
        
        mAlias.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                mWizard.setKeyAlias(mAlias.getText().trim());
                onChange();
            }
        });
        mKeyPassword.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                mWizard.setKeyPassword(mKeyPassword.getText());
                onChange();
            }
        });
        mKeyPassword2.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                onChange();
            }
        });
        
        validityText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                try {
                    mValidity = Integer.parseInt(validityText.getText());
                } catch (NumberFormatException e2) {
                    // this should only happen if the text field is empty due to the verifyListener.
                    mValidity = 0;
                }
                mWizard.setValidity(mValidity);
                onChange();
            }
        });

        ModifyListener dNameListener = new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                onDNameChange();
            }
        };
        
        mCnField.addModifyListener(dNameListener);
        mOuField.addModifyListener(dNameListener);
        mOField.addModifyListener(dNameListener);
        mLField.addModifyListener(dNameListener);
        mStField.addModifyListener(dNameListener);
        mCField.addModifyListener(dNameListener);
    }
    
    @Override
    void onShow() {
        // fill the texts with information loaded from the project.
        if ((mProjectDataChanged & (DATA_PROJECT | DATA_KEYSTORE)) != 0) {
            // reset the keystore/alias from the content of the project
            IProject project = mWizard.getProject();
            
            // disable onChange for now. we'll call it once at the end.
            mDisableOnChange = true;
            
            String alias = ProjectHelper.loadStringProperty(project, ExportWizard.PROPERTY_ALIAS);
            if (alias != null) {
                mAlias.setText(alias);
            }
            
            // get the existing list of keys if applicable
            if (mWizard.getKeyCreationMode()) {
                mExistingAliases = mWizard.getExistingAliases();
            } else {
                mExistingAliases = null;
            }
            
            // reset the passwords
            mKeyPassword.setText(""); //$NON-NLS-1$
            mKeyPassword2.setText(""); //$NON-NLS-1$
            
            // enable onChange, and call it to display errors and enable/disable pageCompleted.
            mDisableOnChange = false;
            onChange();
        }
    }

    @Override
    public IWizardPage getPreviousPage() {
        if (mWizard.getKeyCreationMode()) { // this means we create a key from an existing store
            return mWizard.getKeySelectionPage();
        }
        
        return mWizard.getKeystoreSelectionPage();
    }

    @Override
    public IWizardPage getNextPage() {
        return mWizard.getKeyCheckPage();
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

        if (mAlias.getText().trim().length() == 0) {
            setErrorMessage("Enter key alias.");
            setPageComplete(false);
            return;
        } else if (mExistingAliases != null) {
            // we cannot use indexOf, because we need to do a case-insensitive check
            String keyAlias = mAlias.getText().trim();
            for (String alias : mExistingAliases) {
                if (alias.equalsIgnoreCase(keyAlias)) {
                    setErrorMessage("Key alias already exists in keystore.");
                    setPageComplete(false);
                    return;
                }
            }
        }

        String value = mKeyPassword.getText();
        if (value.length() == 0) {
            setErrorMessage("Enter key password.");
            setPageComplete(false);
            return;
        } else if (value.length() < 6) {
            setErrorMessage("Key password is too short - must be at least 6 characters.");
            setPageComplete(false);
            return;
        }

        if (value.equals(mKeyPassword2.getText()) == false) {
            setErrorMessage("Key passwords don't match.");
            setPageComplete(false);
            return;
        }

        if (mValidity == 0) {
            setErrorMessage("Key certificate validity is required.");
            setPageComplete(false);
            return;
        } else if (mValidity < 25) {
            setMessage("A 25 year certificate validity is recommended.", WARNING);
        } else if (mValidity > 1000) {
            setErrorMessage("Key certificate validity must be between 1 and 1000 years.");
            setPageComplete(false);
            return;
        }

        if (mDName == null || mDName.length() == 0) {
            setErrorMessage("At least one Certificate issuer field is required to be non-empty.");
            setPageComplete(false);
            return;
        }

        setPageComplete(true);
    }
    
    /**
     * Handles changes in the DName fields.
     */
    private void onDNameChange() {
        StringBuilder sb = new StringBuilder();
        
        buildDName("CN", mCnField, sb);
        buildDName("OU", mOuField, sb);
        buildDName("O", mOField, sb);
        buildDName("L", mLField, sb);
        buildDName("ST", mStField, sb);
        buildDName("C", mCField, sb);
        
        mDName = sb.toString();
        mWizard.setDName(mDName);

        onChange();
    }
    
    /**
     * Builds the distinguished name string with the provided {@link StringBuilder}.
     * @param prefix the prefix of the entry.
     * @param textField The {@link Text} field containing the entry value.
     * @param sb the string builder containing the dname.
     */
    private void buildDName(String prefix, Text textField, StringBuilder sb) {
        if (textField != null) {
            String value = textField.getText().trim();
            if (value.length() > 0) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                
                sb.append(prefix);
                sb.append('=');
                sb.append(value);
            }
        }
    }
}
