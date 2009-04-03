/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.ide.eclipse.adt.wizards.newstring;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.Wizard;

/**
 * 
 */
public class NewStringWizard extends Wizard {

    protected static final String MAIN_PAGE_NAME = "newXmlStringPage"; //$NON-NLS-1$

    private NewStringWizardPage mMainPage;

    public NewStringWizard(IProject project) {
        super();
        
        mMainPage = createMainPage(project);
    }
    
    /**
     * Creates the wizard page.
     * <p/>
     * Please do NOT override this method.
     * <p/>
     * This is protected so that it can be overridden by unit tests.
     * However the contract of this class is private and NO ATTEMPT will be made
     * to maintain compatibility between different versions of the plugin.
     * @param project 
     */
    protected NewStringWizardPage createMainPage(IProject project) {
        return new NewStringWizardPage(project, MAIN_PAGE_NAME);
    }
    
    @Override
    public void addPages() {
        addPage(mMainPage);
        super.addPages();
    }

    /**
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        // pass
        return false;
    }

}
