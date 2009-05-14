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

package com.android.ide.eclipse.adt.internal.refactorings.extractstring;

import org.eclipse.core.resources.IProject;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

/**
 * A wizard for ExtractString based on a simple dialog with one page.
 * 
 * @see ExtractStringInputPage
 * @see ExtractStringRefactoring
 */
public class ExtractStringWizard extends RefactoringWizard {

    private final IProject mProject;

    /**
     * Create a wizard for ExtractString based on a simple dialog with one page.
     * 
     * @param ref The instance of {@link ExtractStringRefactoring} to associate to the wizard.
     * @param project The project where the wizard was invoked from (e.g. where the user selection
     *                happened, so that we can retrieve project resources.)
     */
    public ExtractStringWizard(ExtractStringRefactoring ref, IProject project) {
        super(ref, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
        mProject = project;
        setDefaultPageTitle(ref.getName());
    }

    @Override
    protected void addUserInputPages() {
        addPage(new ExtractStringInputPage(mProject));
    }

}
