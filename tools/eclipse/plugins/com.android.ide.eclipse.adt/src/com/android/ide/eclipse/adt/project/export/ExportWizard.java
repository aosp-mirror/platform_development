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

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.project.ProjectHelper;
import com.android.jarutils.KeystoreHelper;
import com.android.jarutils.SignedJarBuilder;
import com.android.jarutils.DebugKeyProvider.IKeyGenOutput;
import com.android.jarutils.DebugKeyProvider.KeytoolException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Export wizard to export an apk signed with a release key/certificate. 
 */
public final class ExportWizard extends Wizard implements IExportWizard {

    private static final String PROJECT_LOGO_LARGE = "icons/android_large.png"; //$NON-NLS-1$
    
    private static final String PAGE_PROJECT_CHECK = "Page_ProjectCheck"; //$NON-NLS-1$
    private static final String PAGE_KEYSTORE_SELECTION = "Page_KeystoreSelection"; //$NON-NLS-1$
    private static final String PAGE_KEY_CREATION = "Page_KeyCreation"; //$NON-NLS-1$
    private static final String PAGE_KEY_SELECTION = "Page_KeySelection"; //$NON-NLS-1$
    private static final String PAGE_KEY_CHECK = "Page_KeyCheck"; //$NON-NLS-1$
    
    static final String PROPERTY_KEYSTORE = "keystore"; //$NON-NLS-1$
    static final String PROPERTY_ALIAS = "alias"; //$NON-NLS-1$
    static final String PROPERTY_DESTINATION = "destination"; //$NON-NLS-1$
    
    /**
     * Base page class for the ExportWizard page. This class add the {@link #onShow()} callback.
     */
    static abstract class ExportWizardPage extends WizardPage {
        
        /** bit mask constant for project data change event */
        protected static final int DATA_PROJECT = 0x001;
        /** bit mask constant for keystore data change event */
        protected static final int DATA_KEYSTORE = 0x002;
        /** bit mask constant for key data change event */
        protected static final int DATA_KEY = 0x004;

        protected static final VerifyListener sPasswordVerifier = new VerifyListener() {
            public void verifyText(VerifyEvent e) {
                // verify the characters are valid for password.
                int len = e.text.length();
                
                // first limit to 127 characters max
                if (len + ((Text)e.getSource()).getText().length() > 127) {
                    e.doit = false;
                    return;
                }
                
                // now only take non control characters
                for (int i = 0 ; i < len ; i++) {
                    if (e.text.charAt(i) < 32) {
                        e.doit = false;
                        return;
                    }
                }
            }
        };
        
        /**
         * Bit mask indicating what changed while the page was hidden.
         * @see #DATA_PROJECT
         * @see #DATA_KEYSTORE
         * @see #DATA_KEY
         */
        protected int mProjectDataChanged = 0;
        
        ExportWizardPage(String name) {
            super(name);
        }
        
        abstract void onShow();
        
        @Override
        public void setVisible(boolean visible) {
            super.setVisible(visible);
            if (visible) {
                onShow();
                mProjectDataChanged = 0;
            }
        }
        
        final void projectDataChanged(int changeMask) {
            mProjectDataChanged |= changeMask;
        }
        
        /**
         * Calls {@link #setErrorMessage(String)} and {@link #setPageComplete(boolean)} based on a
         * {@link Throwable} object.
         */
        protected final void onException(Throwable t) {
            String message = getExceptionMessage(t);
            
            setErrorMessage(message);
            setPageComplete(false);
        }
    }
    
    private ExportWizardPage mPages[] = new ExportWizardPage[5];

    private IProject mProject;

    private String mKeystore;
    private String mKeystorePassword;
    private boolean mKeystoreCreationMode;

    private String mKeyAlias;
    private String mKeyPassword;
    private int mValidity;
    private String mDName;

    private PrivateKey mPrivateKey;
    private X509Certificate mCertificate;

    private String mDestinationPath;
    private String mApkFilePath;
    private String mApkFileName;

    private ExportWizardPage mKeystoreSelectionPage;
    private ExportWizardPage mKeyCreationPage;
    private ExportWizardPage mKeySelectionPage;
    private ExportWizardPage mKeyCheckPage;

    private boolean mKeyCreationMode;

    private List<String> mExistingAliases;

    public ExportWizard() {
        setHelpAvailable(false); // TODO have help
        setWindowTitle("Export Android Application");
        setImageDescriptor();
    }
    
    @Override
    public void addPages() {
        addPage(mPages[0] = new ProjectCheckPage(this, PAGE_PROJECT_CHECK));
        addPage(mKeystoreSelectionPage = mPages[1] = new KeystoreSelectionPage(this,
                PAGE_KEYSTORE_SELECTION));
        addPage(mKeyCreationPage = mPages[2] = new KeyCreationPage(this, PAGE_KEY_CREATION));
        addPage(mKeySelectionPage = mPages[3] = new KeySelectionPage(this, PAGE_KEY_SELECTION));
        addPage(mKeyCheckPage = mPages[4] = new KeyCheckPage(this, PAGE_KEY_CHECK));
    }

    @Override
    public boolean performFinish() {
        // first we make sure export is fine if the destination file already exists
        File f = new File(mDestinationPath);
        if (f.isFile()) {
            if (AdtPlugin.displayPrompt("Export Wizard",
                    "File already exists. Do you want to overwrite it?") == false) {
                return false;
            }
        }
        
        // save the properties
        ProjectHelper.saveStringProperty(mProject, PROPERTY_KEYSTORE, mKeystore);
        ProjectHelper.saveStringProperty(mProject, PROPERTY_ALIAS, mKeyAlias);
        ProjectHelper.saveStringProperty(mProject, PROPERTY_DESTINATION, mDestinationPath);
        
        try {
            if (mKeystoreCreationMode || mKeyCreationMode) {
                final ArrayList<String> output = new ArrayList<String>();
                if (KeystoreHelper.createNewStore(
                        mKeystore,
                        null /*storeType*/,
                        mKeystorePassword,
                        mKeyAlias,
                        mKeyPassword,
                        mDName,
                        mValidity,
                        new IKeyGenOutput() {
                            public void err(String message) {
                                output.add(message);
                            }
                            public void out(String message) {
                                output.add(message);
                            }
                        }) == false) {
                    // keystore creation error!
                    displayError(output.toArray(new String[output.size()]));
                    return false;
                }
                
                // keystore is created, now load the private key and certificate.
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                FileInputStream fis = new FileInputStream(mKeystore);
                keyStore.load(fis, mKeystorePassword.toCharArray());
                fis.close();
                PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(
                        mKeyAlias, new KeyStore.PasswordProtection(mKeyPassword.toCharArray()));
                
                if (entry != null) {
                    mPrivateKey = entry.getPrivateKey();
                    mCertificate = (X509Certificate)entry.getCertificate();
                } else {
                    // this really shouldn't happen since we now let the user choose the key
                    // from a list read from the store.
                    displayError("Could not find key");
                    return false;
                }
            }
            
            // check the private key/certificate again since it may have been created just above.
            if (mPrivateKey != null && mCertificate != null) {
                FileOutputStream fos = new FileOutputStream(mDestinationPath);
                SignedJarBuilder builder = new SignedJarBuilder(fos, mPrivateKey, mCertificate);
                
                // get the input file.
                FileInputStream fis = new FileInputStream(mApkFilePath);
                try {
                    builder.writeZip(fis, null /* filter */);
                } finally {
                    fis.close();
                }
    
                builder.close();
                fos.close();
                
                return true;
            }
        } catch (FileNotFoundException e) {
            displayError(e);
        } catch (NoSuchAlgorithmException e) {
            displayError(e);
        } catch (IOException e) {
            displayError(e);
        } catch (GeneralSecurityException e) {
            displayError(e);
        } catch (KeytoolException e) {
            displayError(e);
        }

        return false;
    }
    
    @Override
    public boolean canFinish() {
        // check if we have the apk to resign, the destination location, and either
        // a private key/certificate or the creation mode. In creation mode, unless
        // all the key/keystore info is valid, the user cannot reach the last page, so there's
        // no need to check them again here.
        return mApkFilePath != null &&
                ((mPrivateKey != null && mCertificate != null)
                        || mKeystoreCreationMode || mKeyCreationMode) &&
                mDestinationPath != null;
    }
    
    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
     */
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        // get the project from the selection
        Object selected = selection.getFirstElement();
        
        if (selected instanceof IProject) {
            mProject = (IProject)selected;
        } else if (selected instanceof IAdaptable) {
            IResource r = (IResource)((IAdaptable)selected).getAdapter(IResource.class);
            if (r != null) {
                mProject = r.getProject();
            }
        }
    }
    
    ExportWizardPage getKeystoreSelectionPage() {
        return mKeystoreSelectionPage;
    }
    
    ExportWizardPage getKeyCreationPage() {
        return mKeyCreationPage;
    }
    
    ExportWizardPage getKeySelectionPage() {
        return mKeySelectionPage;
    }
    
    ExportWizardPage getKeyCheckPage() {
        return mKeyCheckPage;
    }

    /**
     * Returns an image descriptor for the wizard logo.
     */
    private void setImageDescriptor() {
        ImageDescriptor desc = AdtPlugin.getImageDescriptor(PROJECT_LOGO_LARGE);
        setDefaultPageImageDescriptor(desc);
    }
    
    IProject getProject() {
        return mProject;
    }
    
    void setProject(IProject project, String apkFilePath, String filename) {
        mProject = project;
        mApkFilePath = apkFilePath;
        mApkFileName = filename;
        
        updatePageOnChange(ExportWizardPage.DATA_PROJECT);
    }
    
    String getApkFilename() {
        return mApkFileName;
    }
    
    void setKeystore(String path) {
        mKeystore = path;
        mPrivateKey = null;
        mCertificate = null;
        
        updatePageOnChange(ExportWizardPage.DATA_KEYSTORE);
    }
    
    String getKeystore() {
        return mKeystore;
    }
    
    void setKeystoreCreationMode(boolean createStore) {
        mKeystoreCreationMode = createStore;
        updatePageOnChange(ExportWizardPage.DATA_KEYSTORE);
    }
    
    boolean getKeystoreCreationMode() {
        return mKeystoreCreationMode;
    }
    
    
    void setKeystorePassword(String password) {
        mKeystorePassword = password;
        mPrivateKey = null;
        mCertificate = null;

        updatePageOnChange(ExportWizardPage.DATA_KEYSTORE);
    }
    
    String getKeystorePassword() {
        return mKeystorePassword;
    }

    void setKeyCreationMode(boolean createKey) {
        mKeyCreationMode = createKey;
        updatePageOnChange(ExportWizardPage.DATA_KEY);
    }
    
    boolean getKeyCreationMode() {
        return mKeyCreationMode;
    }
    
    void setExistingAliases(List<String> aliases) {
        mExistingAliases = aliases;
    }
    
    List<String> getExistingAliases() {
        return mExistingAliases;
    }

    void setKeyAlias(String name) {
        mKeyAlias = name;
        mPrivateKey = null;
        mCertificate = null;

        updatePageOnChange(ExportWizardPage.DATA_KEY);
    }
    
    String getKeyAlias() {
        return mKeyAlias;
    }

    void setKeyPassword(String password) {
        mKeyPassword = password;
        mPrivateKey = null;
        mCertificate = null;

        updatePageOnChange(ExportWizardPage.DATA_KEY);
    }
    
    String getKeyPassword() {
        return mKeyPassword;
    }

    void setValidity(int validity) {
        mValidity = validity;
        updatePageOnChange(ExportWizardPage.DATA_KEY);
    }
    
    int getValidity() {
        return mValidity;
    }

    void setDName(String dName) {
        mDName = dName;
        updatePageOnChange(ExportWizardPage.DATA_KEY);
    }
    
    String getDName() {
        return mDName;
    }

    void setSigningInfo(PrivateKey privateKey, X509Certificate certificate) {
        mPrivateKey = privateKey;
        mCertificate = certificate;
    }

    void setDestination(String path) {
        mDestinationPath = path;
    }
    
    void updatePageOnChange(int changeMask) {
        for (ExportWizardPage page : mPages) {
            page.projectDataChanged(changeMask);
        }
    }
    
    private void displayError(String... messages) {
        String message = null;
        if (messages.length == 1) {
            message = messages[0];
        } else {
            StringBuilder sb = new StringBuilder(messages[0]);
            for (int i = 1;  i < messages.length; i++) {
                sb.append('\n');
                sb.append(messages[i]);
            }
            
            message = sb.toString();
        }

        AdtPlugin.displayError("Export Wizard", message);
    }
    
    private void displayError(Exception e) {
        String message = getExceptionMessage(e);
        displayError(message);
        
        AdtPlugin.log(e, "Export Wizard Error");
    }
    
    /**
     * Returns the {@link Throwable#getMessage()}. If the {@link Throwable#getMessage()} returns
     * <code>null</code>, the method is called again on the cause of the Throwable object.
     * <p/>If no Throwable in the chain has a valid message, the canonical name of the first
     * exception is returned.
     */
    private static String getExceptionMessage(Throwable t) {
        String message = t.getMessage();
        if (message == null) {
            Throwable cause = t.getCause();
            if (cause != null) {
                return getExceptionMessage(cause);
            }

            // no more cause and still no message. display the first exception.
            return cause.getClass().getCanonicalName();
        }
        
        return message;
    }
}
