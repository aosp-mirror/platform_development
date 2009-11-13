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
import com.android.ide.eclipse.adt.internal.wizards.newproject.NewTestProjectCreationPage;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Shell;

import java.lang.reflect.InvocationTargetException;

/**
 * Stub class for project creation wizard.
 * <p/>
 * Created so project creation logic can be run without UI creation/manipulation.
 */
public class StubProjectWizard extends NewProjectWizard {

    private final String mProjectName;
    private final String mProjectLocation;
    private final IAndroidTarget mTarget;

    /**
     * Constructor
     *
     * @param projectName
     * @param projectLocation
     * @parama target
     */
    public StubProjectWizard(String projectName, String projectLocation, IAndroidTarget target) {
        this.mProjectName = projectName;
        this.mProjectLocation = projectLocation;
        this.mTarget = target;
    }

    /**
     * Override parent to return stub page
     */
    @Override
    protected NewProjectCreationPage createMainPage() {
        return new StubProjectCreationPage(mProjectName, mProjectLocation, mTarget);
    }

    /**
     * Override parent to return null page
     */
    @Override
    protected NewTestProjectCreationPage createTestPage() {
        return null;
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
