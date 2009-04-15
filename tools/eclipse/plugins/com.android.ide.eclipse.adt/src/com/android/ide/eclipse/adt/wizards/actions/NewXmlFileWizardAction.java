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

package com.android.ide.eclipse.adt.wizards.actions;

import com.android.ide.eclipse.adt.wizards.newxmlfile.NewXmlFileWizard;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;

public class NewXmlFileWizardAction implements IObjectActionDelegate {

    private ISelection mSelection;
    private IWorkbench mWorkbench;

    /**
     * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
     */
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        mWorkbench = targetPart.getSite().getWorkbenchWindow().getWorkbench();
    }

    public void run(IAction action) {
        if (mSelection instanceof IStructuredSelection) {
            IStructuredSelection selection = (IStructuredSelection)mSelection;

            // call the new xml file wizard on the current selection.
            NewXmlFileWizard wizard = new NewXmlFileWizard();
            wizard.init(mWorkbench, selection);
            WizardDialog dialog = new WizardDialog(mWorkbench.getDisplay().getActiveShell(),
                    wizard);
            dialog.open();
        }
    }

    public void selectionChanged(IAction action, ISelection selection) {
        this.mSelection = selection;
    }
}
