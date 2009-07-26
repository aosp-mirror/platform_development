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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Key Selection Page. This is used when an existing keystore is used. 
 */
final class KeySelectionPage extends ExportWizardPage {

    private final ExportWizard mWizard;
    private Label mKeyAliasesLabel;
    private Combo mKeyAliases;
    private Label mKeyPasswordLabel;
    private Text mKeyPassword;
    private boolean mDisableOnChange = false;
    private Button mUseExistingKey;
    private Button mCreateKey;

    protected KeySelectionPage(ExportWizard wizard, String pageName) {
        super(pageName);
        mWizard = wizard;

        setTitle("Key alias selection");
        setDescription(""); // TODO
    }

    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout gl = new GridLayout(3, false);
        composite.setLayout(gl);

        GridData gd;

        mUseExistingKey = new Button(composite, SWT.RADIO);
        mUseExistingKey.setText("Use existing key");
        mUseExistingKey.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.horizontalSpan = 3;
        mUseExistingKey.setSelection(true);

        new Composite(composite, SWT.NONE).setLayoutData(gd = new GridData());
        gd.heightHint = 0;
        gd.widthHint = 50;
        mKeyAliasesLabel = new Label(composite, SWT.NONE);
        mKeyAliasesLabel.setText("Alias:");
        mKeyAliases = new Combo(composite, SWT.READ_ONLY);
        mKeyAliases.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        new Composite(composite, SWT.NONE).setLayoutData(gd = new GridData());
        gd.heightHint = 0;
        gd.widthHint = 50;
        mKeyPasswordLabel = new Label(composite, SWT.NONE);
        mKeyPasswordLabel.setText("Password:");
        mKeyPassword = new Text(composite, SWT.BORDER | SWT.PASSWORD);
        mKeyPassword.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        mCreateKey = new Button(composite, SWT.RADIO);
        mCreateKey.setText("Create new key");
        mCreateKey.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.horizontalSpan = 3;

        // Show description the first time
        setErrorMessage(null);
        setMessage(null);
        setControl(composite);
        
        mUseExistingKey.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mWizard.setKeyCreationMode(!mUseExistingKey.getSelection());
                enableWidgets();
                onChange();
            }
        });
        
        mKeyAliases.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mWizard.setKeyAlias(mKeyAliases.getItem(mKeyAliases.getSelectionIndex()));
                onChange();
            }
        });
        
        mKeyPassword.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                mWizard.setKeyPassword(mKeyPassword.getText());
                onChange();
            }
        });
    }
    
    @Override
    void onShow() {
        // fill the texts with information loaded from the project.
        if ((mProjectDataChanged & (DATA_PROJECT | DATA_KEYSTORE)) != 0) {
            // disable onChange for now. we'll call it once at the end.
            mDisableOnChange = true;

            // reset the alias from the content of the project
            try {
                // reset to using a key
                mWizard.setKeyCreationMode(false);
                mUseExistingKey.setSelection(true);
                mCreateKey.setSelection(false);
                enableWidgets();

                // remove the content of the alias combo always and first, in case the
                // keystore password is wrong
                mKeyAliases.removeAll();

                // get the alias list (also used as a keystore password test)
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                FileInputStream fis = new FileInputStream(mWizard.getKeystore());
                keyStore.load(fis, mWizard.getKeystorePassword().toCharArray());
                fis.close();
                
                Enumeration<String> aliases = keyStore.aliases();

                // get the alias from the project previous export, and look for a match as
                // we add the aliases to the combo.
                IProject project = mWizard.getProject();

                String keyAlias = ProjectHelper.loadStringProperty(project,
                        ExportWizard.PROPERTY_ALIAS);
                
                ArrayList<String> aliasList = new ArrayList<String>();

                int selection = -1;
                int count = 0;
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    mKeyAliases.add(alias);
                    aliasList.add(alias);
                    if (selection == -1 && alias.equalsIgnoreCase(keyAlias)) {
                        selection = count;
                    }
                    count++;
                }
                
                mWizard.setExistingAliases(aliasList);

                if (selection != -1) {
                    mKeyAliases.select(selection);

                    // since a match was found and is selected, we need to give it to
                    // the wizard as well
                    mWizard.setKeyAlias(keyAlias);
                } else {
                    mKeyAliases.clearSelection();
                }

                // reset the password
                mKeyPassword.setText(""); //$NON-NLS-1$

                // enable onChange, and call it to display errors and enable/disable pageCompleted.
                mDisableOnChange = false;
                onChange();
            } catch (KeyStoreException e) {
                onException(e);
            } catch (FileNotFoundException e) {
                onException(e);
            } catch (NoSuchAlgorithmException e) {
                onException(e);
            } catch (CertificateException e) {
                onException(e);
            } catch (IOException e) {
                onException(e);
            } finally {
                // in case we exit with an exception, we need to reset this
                mDisableOnChange = false;
            }
        }
    }
    
    @Override
    public IWizardPage getPreviousPage() {
        return mWizard.getKeystoreSelectionPage();
    }

    @Override
    public IWizardPage getNextPage() {
        if (mWizard.getKeyCreationMode()) {
            return mWizard.getKeyCreationPage();
        }
        
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

        if (mWizard.getKeyCreationMode() == false) {
            if (mKeyAliases.getSelectionIndex() == -1) {
                setErrorMessage("Select a key alias.");
                setPageComplete(false);
                return;
            }
    
            if (mKeyPassword.getText().trim().length() == 0) {
                setErrorMessage("Enter key password.");
                setPageComplete(false);
                return;
            }
        }

        setPageComplete(true);
    }
    
    private void enableWidgets() {
        boolean useKey = !mWizard.getKeyCreationMode();
        mKeyAliasesLabel.setEnabled(useKey);
        mKeyAliases.setEnabled(useKey);
        mKeyPassword.setEnabled(useKey);
        mKeyPasswordLabel.setEnabled(useKey);
    }
}
