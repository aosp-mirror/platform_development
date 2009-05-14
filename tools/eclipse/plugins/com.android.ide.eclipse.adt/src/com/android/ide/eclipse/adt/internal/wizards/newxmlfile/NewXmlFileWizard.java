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



package com.android.ide.eclipse.adt.internal.wizards.newxmlfile;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.editors.IconFactory;
import com.android.ide.eclipse.adt.internal.wizards.newxmlfile.NewXmlFileCreationPage.TypeInfo;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * The "New Android XML File Wizard" provides the ability to create skeleton XML
 * resources files for Android projects.
 * <p/>
 * The wizard has one page, {@link NewXmlFileCreationPage}, used to select the project,
 * the resource folder, resource type and file name. It then creates the XML file.
 */
public class NewXmlFileWizard extends Wizard implements INewWizard {

    private static final String PROJECT_LOGO_LARGE = "android_large"; //$NON-NLS-1$
    
    protected static final String MAIN_PAGE_NAME = "newAndroidXmlFilePage"; //$NON-NLS-1$

    private NewXmlFileCreationPage mMainPage;

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setHelpAvailable(false); // TODO have help
        setWindowTitle("New Android XML File");
        setImageDescriptor();

        mMainPage = createMainPage();
        mMainPage.setTitle("New Android XML File");
        mMainPage.setDescription("Creates a new Android XML file.");
        mMainPage.setInitialSelection(selection);
    }
    
    /**
     * Creates the wizard page.
     * <p/>
     * Please do NOT override this method.
     * <p/>
     * This is protected so that it can be overridden by unit tests.
     * However the contract of this class is private and NO ATTEMPT will be made
     * to maintain compatibility between different versions of the plugin.
     */
    protected NewXmlFileCreationPage createMainPage() {
        return new NewXmlFileCreationPage(MAIN_PAGE_NAME);
    }

    // -- Methods inherited from org.eclipse.jface.wizard.Wizard --
    //
    // The Wizard class implements most defaults and boilerplate code needed by
    // IWizard

    /**
     * Adds pages to this wizard.
     */
    @Override
    public void addPages() {
        addPage(mMainPage);
    }

    /**
     * Performs any actions appropriate in response to the user having pressed
     * the Finish button, or refuse if finishing now is not permitted: here, it
     * actually creates the workspace project and then switch to the Java
     * perspective.
     *
     * @return True
     */
    @Override
    public boolean performFinish() {
        IFile file = createXmlFile();
        if (file == null) {
            return false;
        } else {
            // Open the file in an editor
            IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (win != null) {
                IWorkbenchPage page = win.getActivePage();
                if (page != null) {
                    try {
                        IDE.openEditor(page, file);
                    } catch (PartInitException e) {
                        AdtPlugin.log(e, "Failed to create %1$s: missing type",  //$NON-NLS-1$
                                file.getFullPath().toString());
                    }
                }
            }
            return true;
        }
    }

    // -- Custom Methods --
    
    private IFile createXmlFile() {
        IFile file = mMainPage.getDestinationFile();
        String name = file.getFullPath().toString();
        boolean need_delete = false;

        if (file.exists()) {
            if (!AdtPlugin.displayPrompt("New Android XML File",
                String.format("Do you want to overwrite the file %1$s ?", name))) {
                // abort if user selects cancel.
                return null;
            }
            need_delete = true;
        } else {
            createWsParentDirectory(file.getParent());
        }
        
        TypeInfo type = mMainPage.getSelectedType();
        if (type == null) {
            // this is not expected to happen
            AdtPlugin.log(IStatus.ERROR, "Failed to create %1$s: missing type", name);  //$NON-NLS-1$
            return null;
        }
        String xmlns = type.getXmlns();
        String root = mMainPage.getRootElement();
        if (root == null) {
            // this is not expected to happen
            AdtPlugin.log(IStatus.ERROR, "Failed to create %1$s: missing root element", //$NON-NLS-1$
                    file.toString());
            return null;
        }
        
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");   //$NON-NLS-1$

        sb.append('<').append(root);
        if (xmlns != null) {
            sb.append('\n').append("  xmlns:android=\"").append(xmlns).append("\"");  //$NON-NLS-1$ //$NON-NLS-2$
        }
        
        String attrs = type.getDefaultAttrs();
        if (attrs != null) {
            sb.append("\n  ");                       //$NON-NLS-1$
            sb.append(attrs.replace("\n", "\n  "));  //$NON-NLS-1$ //$NON-NLS-2$
        }
        
        sb.append(">\n");                            //$NON-NLS-1$
        sb.append("</").append(root).append(">\n");  //$NON-NLS-1$ //$NON-NLS-2$

        String result = sb.toString();
        String error = null;
        try {
            byte[] buf = result.getBytes("UTF8");
            InputStream stream = new ByteArrayInputStream(buf);
            if (need_delete) {
                file.delete(IFile.KEEP_HISTORY | IFile.FORCE, null /*monitor*/);
            }
            file.create(stream, true /*force*/, null /*progres*/);
            return file;
        } catch (UnsupportedEncodingException e) {
            error = e.getMessage();
        } catch (CoreException e) {
            error = e.getMessage();
        }

        error = String.format("Failed to generate %1$s: %2$s", name, error);
        AdtPlugin.displayError("New Android XML File", error);
        return null;
    }

    private boolean createWsParentDirectory(IContainer wsPath) {
        if (wsPath.getType() == IContainer.FOLDER) {
            if (wsPath == null || wsPath.exists()) {
                return true;
            }

            IFolder folder = (IFolder) wsPath;
            try {
                if (createWsParentDirectory(wsPath.getParent())) {
                    folder.create(true /* force */, true /* local */, null /* monitor */);
                    return true;
                }
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
        
        return false;
    }

    /**
     * Returns an image descriptor for the wizard logo.
     */
    private void setImageDescriptor() {
        ImageDescriptor desc = IconFactory.getInstance().getImageDescriptor(PROJECT_LOGO_LARGE);
        setDefaultPageImageDescriptor(desc);
    }

}
