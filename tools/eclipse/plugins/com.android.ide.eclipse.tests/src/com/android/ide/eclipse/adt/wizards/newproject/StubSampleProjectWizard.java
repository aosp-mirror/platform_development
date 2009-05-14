/*
 * Copyright (C) 2008 The Android Open Source Project
 * 
 * Licensed under the Eclipse Public License, Version 1.0 (the "License"); you
 * may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 *      http://www.eclipse.org/org/documents/epl-v10.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.ide.eclipse.adt.wizards.newproject;

import com.android.ide.eclipse.adt.internal.wizards.newproject.NewProjectCreationPage;
import com.android.ide.eclipse.adt.internal.wizards.newproject.NewProjectWizard;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Shell;

import java.lang.reflect.InvocationTargetException;

/**
 * Stub class for project creation wizard Created so project creation logic can
 * be run without UI creation/manipulation Returns canned responses for creating
 * a sample project
 */
public class StubSampleProjectWizard extends NewProjectWizard {

    private final String mSampleProjectName;
    private final String mOsSdkLocation;

    /**
     * Constructor
     * 
     * @param sampleProjectName
     * @param osSdkLocation
     */
    public StubSampleProjectWizard(String sampleProjectName, String osSdkLocation) {
        this.mSampleProjectName = sampleProjectName;
        this.mOsSdkLocation = osSdkLocation;
    }

    /**
     * Override parent to return stub page
     */
    @Override
    protected NewProjectCreationPage createMainPage() {
        return new StubSampleProjectCreationPage(mSampleProjectName, mOsSdkLocation);
    }

    /**
     * Overrides parent to return dummy wizard container
     */
    @Override
    public IWizardContainer getContainer() {
        return new IWizardContainer() {

            public IWizardPage getCurrentPage() {
                return null;
            }

            public Shell getShell() {
                return null;
            }

            public void showPage(IWizardPage page) {
                // pass
            }

            public void updateButtons() {
                // pass
            }

            public void updateMessage() {
                // pass
            }

            public void updateTitleBar() {
                // pass
            }

            public void updateWindowTitle() {
                // pass
            }

            /**
             * Executes runnable on current thread
             */
            public void run(boolean fork, boolean cancelable,
                    IRunnableWithProgress runnable)
                    throws InvocationTargetException, InterruptedException {
                runnable.run(new NullProgressMonitor());
            }

        };
    }
}
