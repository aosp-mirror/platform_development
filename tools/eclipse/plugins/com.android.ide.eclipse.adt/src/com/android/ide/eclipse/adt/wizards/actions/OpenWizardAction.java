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

package com.android.ide.eclipse.adt.wizards.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.IWorkbenchHelpContextIds;
import org.eclipse.ui.internal.LegacyResourceSupport;
import org.eclipse.ui.internal.actions.NewWizardShortcutAction;
import org.eclipse.ui.internal.util.Util;

/**
 * An abstract action that displays one of our wizards.
 * Derived classes must provide the actual wizard to display.
 */
/*package*/ abstract class OpenWizardAction implements IWorkbenchWindowActionDelegate {

    /**
     * The wizard dialog width, extracted from {@link NewWizardShortcutAction}
     */
    private static final int SIZING_WIZARD_WIDTH = 500;

    /**
     * The wizard dialog height, extracted from {@link NewWizardShortcutAction}
     */
    private static final int SIZING_WIZARD_HEIGHT = 500;

    
    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
     */
    public void dispose() {
        // pass
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
     */
    public void init(IWorkbenchWindow window) {
        // pass
    }

    /**
     * Opens and display the Android New Project Wizard.
     * <p/>
     * Most of this implementation is extracted from {@link NewWizardShortcutAction#run()}.
     * 
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {

        // get the workbench and the current window
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        
        // This code from NewWizardShortcutAction#run() gets the current window selection
        // and converts it to a workbench structured selection for the wizard, if possible.
        ISelection selection = window.getSelectionService().getSelection();
        IStructuredSelection selectionToPass = StructuredSelection.EMPTY;
        if (selection instanceof IStructuredSelection) {
            selectionToPass = (IStructuredSelection) selection;
        } else {
            // Build the selection from the IFile of the editor
            IWorkbenchPart part = window.getPartService().getActivePart();
            if (part instanceof IEditorPart) {
                IEditorInput input = ((IEditorPart) part).getEditorInput();
                Class<?> fileClass = LegacyResourceSupport.getFileClass();
                if (input != null && fileClass != null) {
                    Object file = Util.getAdapter(input, fileClass);
                    if (file != null) {
                        selectionToPass = new StructuredSelection(file);
                    }
                }
            }
        }

        // Create the wizard and initialize it with the selection
        IWorkbenchWizard wizard = instanciateWizard(action);
        wizard.init(workbench, selectionToPass);
        
        // It's not visible yet until a dialog is created and opened
        Shell parent = window.getShell();
        WizardDialog dialog = new WizardDialog(parent, wizard);
        dialog.create();
        
        // This code comes straight from NewWizardShortcutAction#run()
        Point defaultSize = dialog.getShell().getSize();
        dialog.getShell().setSize(
                Math.max(SIZING_WIZARD_WIDTH, defaultSize.x),
                Math.max(SIZING_WIZARD_HEIGHT, defaultSize.y));
        window.getWorkbench().getHelpSystem().setHelp(dialog.getShell(),
                IWorkbenchHelpContextIds.NEW_WIZARD_SHORTCUT);
        
        dialog.open();
    }

    /**
     * Called by {@link #run(IAction)} to instantiate the actual wizard.
     * 
     * @param action The action parameter from {@link #run(IAction)}.
     * @return A new wizard instance. Must not be null.
     */
    protected abstract IWorkbenchWizard instanciateWizard(IAction action);

    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) {
        // pass
    }

}
