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
import org.eclipse.ui.forms.widgets.FormText;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;

/**
 * Final page of the wizard that checks the key and ask for the ouput location.
 */
final class KeyCheckPage extends ExportWizardPage {

    private final ExportWizard mWizard;
    private PrivateKey mPrivateKey;
    private X509Certificate mCertificate;
    private Text mDestination;
    private boolean mFatalSigningError;
    private FormText mDetailText;

    protected KeyCheckPage(ExportWizard wizard, String pageName) {
        super(pageName);
        mWizard = wizard;
        
        setTitle("Destination and key/certificate checks");
        setDescription(""); // TODO
    }

    public void createControl(Composite parent) {
        setErrorMessage(null);
        setMessage(null);

        // build the ui.
        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout gl = new GridLayout(3, false);
        gl.verticalSpacing *= 3;
        composite.setLayout(gl);
        
        GridData gd;

        new Label(composite, SWT.NONE).setText("Destination APK file:");
        mDestination = new Text(composite, SWT.BORDER);
        mDestination.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        mDestination.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                onDestinationChange();
            }
        });
        final Button browseButton = new Button(composite, SWT.PUSH);
        browseButton.setText("Browse...");
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog fileDialog = new FileDialog(browseButton.getShell(), SWT.SAVE);
                
                fileDialog.setText("Destination file name");
                fileDialog.setFileName(mWizard.getApkFilename());
        
                String saveLocation = fileDialog.open();
                if (saveLocation != null) {
                    mDestination.setText(saveLocation);
                }
            }
        });
        
        mDetailText = new FormText(composite, SWT.NONE);
        mDetailText.setLayoutData(gd = new GridData(GridData.FILL_BOTH));
        gd.horizontalSpan = 3;
        
        setControl(composite);
    }
    
    @Override
    void onShow() {
        // fill the texts with information loaded from the project.
        if ((mProjectDataChanged & DATA_PROJECT) != 0) {
            // reset the destination from the content of the project
            IProject project = mWizard.getProject();
            
            String destination = ProjectHelper.loadStringProperty(project,
                    ExportWizard.PROPERTY_DESTINATION);
            if (destination != null) {
                mDestination.setText(destination);
            }
        }
        
        // if anything change we basically reload the data.
        if (mProjectDataChanged != 0) {
            mFatalSigningError = false;

            // reset the wizard with no key/cert to make it not finishable, unless a valid
            // key/cert is found.
            mWizard.setSigningInfo(null, null);
    
            if (mWizard.getKeystoreCreationMode() || mWizard.getKeyCreationMode()) {
                int validity = mWizard.getValidity();
                StringBuilder sb = new StringBuilder(
                        String.format("<form><p>Certificate expires in %d years.</p>",
                        validity));

                if (validity < 25) {
                    sb.append("<p>Make sure the certificate is valid for the planned lifetime of the product.</p>");
                    sb.append("<p>If the certificate expires, you will be forced to sign your application with a different one.</p>");
                    sb.append("<p>Applications cannot be upgraded if their certificate changes from one version to another, ");
                    sb.append("forcing a full uninstall/install, which will make the user lose his/her data.</p>");
                    sb.append("<p>Android Market currently requires certificates to be valid until 2033.</p>");
                }

                sb.append("</form>");
                mDetailText.setText(sb.toString(), true /* parseTags */, true /* expandURLs */);
            } else {
                try {
                    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                    FileInputStream fis = new FileInputStream(mWizard.getKeystore());
                    keyStore.load(fis, mWizard.getKeystorePassword().toCharArray());
                    fis.close();
                    PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(
                            mWizard.getKeyAlias(),
                            new KeyStore.PasswordProtection(
                                    mWizard.getKeyPassword().toCharArray()));
                    
                    if (entry != null) {
                        mPrivateKey = entry.getPrivateKey();
                        mCertificate = (X509Certificate)entry.getCertificate();
                    } else {
                        setErrorMessage("Unable to find key.");
                        
                        setPageComplete(false);
                    }
                } catch (FileNotFoundException e) {
                    // this was checked at the first previous step and will not happen here, unless
                    // the file was removed during the export wizard execution.
                    onException(e);
                } catch (KeyStoreException e) {
                    onException(e);
                } catch (NoSuchAlgorithmException e) {
                    onException(e);
                } catch (UnrecoverableEntryException e) {
                    onException(e);
                } catch (CertificateException e) {
                    onException(e);
                } catch (IOException e) {
                    onException(e);
                }
                
                if (mPrivateKey != null && mCertificate != null) {
                    Calendar expirationCalendar = Calendar.getInstance();
                    expirationCalendar.setTime(mCertificate.getNotAfter());
                    Calendar today = Calendar.getInstance();
                    
                    if (expirationCalendar.before(today)) {
                        mDetailText.setText(String.format(
                                "<form><p>Certificate expired on %s</p></form>",
                                mCertificate.getNotAfter().toString()),
                                true /* parseTags */, true /* expandURLs */);
                        
                        // fatal error = nothing can make the page complete.
                        mFatalSigningError = true;
        
                        setErrorMessage("Certificate is expired.");
                        setPageComplete(false);
                    } else {
                        // valid, key/cert: put it in the wizard so that it can be finished
                        mWizard.setSigningInfo(mPrivateKey, mCertificate);
        
                        StringBuilder sb = new StringBuilder(String.format(
                                "<form><p>Certificate expires on %s.</p>",
                                mCertificate.getNotAfter().toString()));
                        
                        int expirationYear = expirationCalendar.get(Calendar.YEAR);
                        int thisYear = today.get(Calendar.YEAR);
                        
                        if (thisYear + 25 < expirationYear) {
                            // do nothing
                        } else {
                            if (expirationYear == thisYear) {
                                sb.append("<p>The certificate expires this year.</p>");
                            } else {
                                int count = expirationYear-thisYear;
                                sb.append(String.format(
                                        "<p>The Certificate expires in %1$s %2$s.</p>",
                                        count, count == 1 ? "year" : "years"));
                            }
                            
                            sb.append("<p>Make sure the certificate is valid for the planned lifetime of the product.</p>");
                            sb.append("<p>If the certificate expires, you will be forced to sign your application with a different one.</p>");
                            sb.append("<p>Applications cannot be upgraded if their certificate changes from one version to another, ");
                            sb.append("forcing a full uninstall/install, which will make the user lose his/her data.</p>");
                            sb.append("<p>Android Market currently requires certificates to be valid until 2033.</p>");
                        }
                        
                        sb.append("</form>");
        
                        mDetailText.setText(sb.toString(), true /* parseTags */, true /* expandURLs */);
                    }
                    mDetailText.getParent().layout();
                } else {
                    // fatal error = nothing can make the page complete.
                    mFatalSigningError = true;
                }
            }
        }

        onDestinationChange();
    }
    
    private void onDestinationChange() {
        if (mFatalSigningError == false) {
            // reset messages for now.
            setErrorMessage(null);
            setMessage(null);

            String path = mDestination.getText().trim();

            if (path.length() == 0) {
                setErrorMessage("Enter destination for the APK file.");
                mWizard.setDestination(null); // this is to reset canFinish in the wizard
                setPageComplete(false);
                return;
            }

            File file = new File(path);
            if (file.isDirectory()) {
                setErrorMessage("Destination is a directory.");
                mWizard.setDestination(null); // this is to reset canFinish in the wizard
                setPageComplete(false);
                return;
            }

            File parentFile = file.getParentFile();
            if (parentFile == null || parentFile.isDirectory() == false) {
                setErrorMessage("Not a valid directory.");
                mWizard.setDestination(null); // this is to reset canFinish in the wizard
                setPageComplete(false);
                return;
            }

            // no error, set the destination in the wizard.
            mWizard.setDestination(path);
            setPageComplete(true);
            
            // However, we should also test if the file already exists.
            if (file.isFile()) {
                setMessage("Destination file already exists.", WARNING);
            }
        }
    }
}
