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

package com.android.ide.eclipse.adt.internal.wizards.actions;

import com.android.ide.eclipse.adt.internal.ui.IUpdateWizardDialog;
import com.android.ide.eclipse.adt.internal.ui.WizardDialogEx;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
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
/*package*/ abstract class OpenWizardAction
    implements IWorkbenchWindowActionDelegate, IObjectActionDelegate {

    /**
     * The wizard dialog width, extracted from {@link NewWizardShortcutAction}
     */
    private static final int SIZING_WIZARD_WIDTH = 500;

    /**
     * The wizard dialog height, extracted from {@link NewWizardShortcutAction}
     */
    private static final int SIZING_WIZARD_HEIGHT = 500;

    /** The wizard that was created by {@link #run(IAction)}. */
    private IWorkbenchWizard mWizard;
    /** The result from the dialog */
    private int mDialogResult;

    private ISelection mSelection;
    private IWorkbench mWorkbench;

    /** Returns the wizard that was created by {@link #run(IAction)}. */
    public IWorkbenchWizard getWizard() {
        return mWizard;
    }

    /** Returns the result from {@link Dialog#open()}, available after
     * the completion of {@link #run(IAction)}. */
    public int getDialogResult() {
        return mDialogResult;
    }

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
     * @param action The action that got us here. Can be null when used internally.
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {

        // get the workbench and the current window
        IWorkbench workbench = mWorkbench != null ? mWorkbench : PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

        // This code from NewWizardShortcutAction#run() gets the current window selection
        // and converts it to a workbench structured selection for the wizard, if possible.
        ISelection selection = mSelection;
        if (selection == null) {
            selection = window.getSelectionService().getSelection();
        }

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
        mWizard = instanciateWizard(action);
        mWizard.init(workbench, selectionToPass);

        // It's not visible yet until a dialog is created and opened
        Shell parent = window.getShell();
        WizardDialogEx dialog = new WizardDialogEx(parent, mWizard);
        dialog.create();

        if (mWizard instanceof IUpdateWizardDialog) {
            ((IUpdateWizardDialog) mWizard).updateWizardDialog(dialog);
        }

        // This code comes straight from NewWizardShortcutAction#run()
        Point defaultSize = dialog.getShell().getSize();
        dialog.getShell().setSize(
                Math.max(SIZING_WIZARD_WIDTH, defaultSize.x),
                Math.max(SIZING_WIZARD_HEIGHT, defaultSize.y));
        window.getWorkbench().getHelpSystem().setHelp(dialog.getShell(),
                IWorkbenchHelpContextIds.NEW_WIZARD_SHORTCUT);

        mDialogResult = dialog.open();
    }

    /**
     * Called by {@link #run(IAction)} to instantiate the actual wizard.
     *
     * @param action The action parameter from {@link #run(IAction)}.
     *               This can be null.
     * @return A new wizard instance. Must not be null.
     */
    protected abstract IWorkbenchWizard instanciateWizard(IAction action);

    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) {
        mSelection = selection;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction, org.eclipse.ui.IWorkbenchPart)
     */
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        mWorkbench = targetPart.getSite().getWorkbenchWindow().getWorkbench();
    }
}
