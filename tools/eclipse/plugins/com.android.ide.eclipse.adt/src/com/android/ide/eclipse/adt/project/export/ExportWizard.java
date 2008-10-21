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
import com.android.jarutils.SignedJarBuilder;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Export wizard to export an apk signed with a release key/certificate. 
 */
public class ExportWizard extends Wizard implements IExportWizard {

    private static final String PROJECT_LOGO_LARGE = "icons/android_large.png"; //$NON-NLS-1$
    
    private static final String PAGE_PRE = "preExportPage"; //$NON-NLS-1$
    private static final String PAGE_SIGNING = "signingExportPage"; //$NON-NLS-1$
    private static final String PAGE_FINAL = "finalExportPage"; //$NON-NLS-1$
    
    static final String PROPERTY_KEYSTORE = "keystore"; //$NON-NLS-1$
    static final String PROPERTY_ALIAS = "alias"; //$NON-NLS-1$
    static final String PROPERTY_DESTINATION = "destination"; //$NON-NLS-1$
    
    /**
     * Base page class for the ExportWizard page. This class add the {@link #onShow()} callback.
     */
    static abstract class ExportWizardPage extends WizardPage {
        
        protected boolean mNewProjectReference = true;
        
        ExportWizardPage(String name) {
            super(name);
        }
        
        abstract void onShow();
        
        @Override
        public void setVisible(boolean visible) {
            super.setVisible(visible);
            if (visible) {
                onShow();
                mNewProjectReference = false;
            }
        }
        
        void newProjectReference() {
            mNewProjectReference = true;
        }
    }
    
    private ExportWizardPage mPages[] = new ExportWizardPage[3];

    private IProject mProject;

    private String mKeystore;
    private String mKeyAlias;
    private char[] mKeystorePassword;
    private char[] mKeyPassword;

    private PrivateKey mPrivateKey;
    private X509Certificate mCertificate;

    private String mDestinationPath;
    private String mApkFilePath;
    private String mApkFileName;

    public ExportWizard() {
        setHelpAvailable(false); // TODO have help
        setWindowTitle("Export Android Application");
        setImageDescriptor();
    }
    
    @Override
    public void addPages() {
        addPage(mPages[0] = new PreExportPage(this, PAGE_PRE));
        addPage(mPages[1] = new SigningExportPage(this, PAGE_SIGNING));
        addPage(mPages[2] = new FinalExportPage(this, PAGE_FINAL));
    }

    @Override
    public boolean performFinish() {
        // save the properties
        ProjectHelper.saveStringProperty(mProject, PROPERTY_KEYSTORE, mKeystore);
        ProjectHelper.saveStringProperty(mProject, PROPERTY_ALIAS, mKeyAlias);
        ProjectHelper.saveStringProperty(mProject, PROPERTY_DESTINATION, mDestinationPath);
        
        try {
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
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return false;
    }
    
    @Override
    public boolean canFinish() {
        return mApkFilePath != null &&
                mPrivateKey != null && mCertificate != null &&
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
        
        // indicate to the page that the project was changed.
        for (ExportWizardPage page : mPages) {
            page.newProjectReference();
        }
    }
    
    String getApkFilename() {
        return mApkFileName;
    }
    
    void setKeystore(String path) {
        mKeystore = path;
        mPrivateKey = null;
        mCertificate = null;
    }
    
    String getKeystore() {
        return mKeystore;
    }
    
    void setKeyAlias(String name) {
        mKeyAlias = name;
        mPrivateKey = null;
        mCertificate = null;
    }
    
    String getKeyAlias() {
        return mKeyAlias;
    }
    
    void setKeystorePassword(char[] password) {
        mKeystorePassword = password;
        mPrivateKey = null;
        mCertificate = null;
    }
    
    char[] getKeystorePassword() {
        return mKeystorePassword;
    }
    
    void setKeyPassword(char[] password) {
        mKeyPassword = password;
        mPrivateKey = null;
        mCertificate = null;
    }
    
    char[] getKeyPassword() {
        return mKeyPassword;
    }

    void setSigningInfo(PrivateKey privateKey, X509Certificate certificate) {
        mPrivateKey = privateKey;
        mCertificate = certificate;
    }

    void setDestination(String path) {
        mDestinationPath = path;
    }
    
}
