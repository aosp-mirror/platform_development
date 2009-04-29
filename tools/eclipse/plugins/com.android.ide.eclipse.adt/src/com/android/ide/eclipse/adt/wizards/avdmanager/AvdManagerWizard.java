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



package com.android.ide.eclipse.adt.wizards.avdmanager;

import com.android.ide.eclipse.editors.IconFactory;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * The "AVD Manager Wizard" provides a quick way to edit AVDs.
 * <p/>
 * The wizard has one page, {@link AvdManagerListPage}, used to display and edit the AVDs. 
 * In fact the whole UI is not really a wizard. It has just been implemented that way
 * to get something quick out of the door. We'll need to revisit this when we implement
 * the final standalone AVD Manager UI and this Wizard will go away.
 */
public class AvdManagerWizard extends Wizard implements INewWizard {

    private static final String PROJECT_LOGO_LARGE = "android_large"; //$NON-NLS-1$
    
    protected static final String MAIN_PAGE_NAME = "avdManagerListPage"; //$NON-NLS-1$

    private AvdManagerListPage mMainPage;

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setHelpAvailable(false); // TODO have help
        setWindowTitle("Android Virtual Devices Manager");
        setImageDescriptor();

        mMainPage = createMainPage();
        mMainPage.setTitle("Android Virtual Devices Manager");
        mMainPage.setDescription("Displays existing Android Virtual Devices. Lets you create new ones or delete existing ones.");
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
    protected AvdManagerListPage createMainPage() {
        return new AvdManagerListPage(MAIN_PAGE_NAME);
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
     * the Finish button, or refuse if finishing now is not permitted: here, it does nothing.
     *
     * @return True
     */
    @Override
    public boolean performFinish() {
        return true;
    }

    /**
     * Returns an image descriptor for the wizard logo.
     */
    private void setImageDescriptor() {
        ImageDescriptor desc = IconFactory.getInstance().getImageDescriptor(PROJECT_LOGO_LARGE);
        setDefaultPageImageDescriptor(desc);
    }

}
