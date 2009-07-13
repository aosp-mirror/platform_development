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

import com.android.ide.eclipse.adt.AndroidConstants;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

/*
 * Quick Reference Link:
 * http://www.eclipse.org/articles/article.php?file=Article-Unleashing-the-Power-of-Refactoring/index.html
 * and
 * http://www.ibm.com/developerworks/opensource/library/os-ecjdt/
 */

/**
 * Action executed when the "Extract String" menu item is invoked.
 * <p/>
 * The intent of the action is to start a refactoring that extracts a source string and
 * replaces it by an Android string resource ID.
 * <p/>
 * Workflow:
 * <ul>
 * <li> The action is currently located in the Refactoring menu in the main menu.
 * <li> TODO: extend the popup refactoring menu in a Java or Android XML file.
 * <li> The action is only enabled if the selection is 1 character or more. That is at least part
 *     of the string must be selected, it's not enough to just move the insertion point. This is
 *     a limitation due to {@link #selectionChanged(IAction, ISelection)} not being called when
 *     the insertion point is merely moved. TODO: address this limitation.
 * <ul> The action gets the current {@link ISelection}. It also knows the current
 *     {@link IWorkbenchWindow}. However for the refactoring we are also interested in having the
 *     actual resource file. By looking at the Active Window > Active Page > Active Editor we
 *     can get the {@link IEditorInput} and find the {@link ICompilationUnit} (aka Java file)
 *     that is being edited.
 * <ul> TODO: change this to find the {@link IFile} being manipulated. The {@link ICompilationUnit}
 *     can be inferred using {@link JavaCore#createCompilationUnitFrom(IFile)}. This will allow
 *     us to be able to work with a selection from an Android XML file later.
 * <li> The action creates a new {@link ExtractStringRefactoring} and make it run on in a new
 *     {@link ExtractStringWizard}.
 * <ul>
 */
public class ExtractStringAction implements IWorkbenchWindowActionDelegate {

    /** Keep track of the current workbench window. */
    private IWorkbenchWindow mWindow;
    private ITextSelection mSelection;
    private IEditorPart mEditor;
    private IFile mFile;

    /**
     * Keep track of the current workbench window.
     */
    public void init(IWorkbenchWindow window) {
        mWindow = window;
    }

    public void dispose() {
        // Nothing to do
    }

    /**
     * Examine the selection to determine if the action should be enabled or not.
     * <p/>
     * Keep a link to the relevant selection structure (i.e. a part of the Java AST).
     */
    public void selectionChanged(IAction action, ISelection selection) {

        // Note, two kinds of selections are returned here:
        // ITextSelection on a Java source window
        // IStructuredSelection in the outline or navigator
        // This simply deals with the refactoring based on a non-empty selection.
        // At that point, just enable the action and later decide if it's valid when it actually
        // runs since we don't have access to the AST yet.

        mSelection = null;
        mFile = null;

        if (selection instanceof ITextSelection) {
            mSelection = (ITextSelection) selection;
            if (mSelection.getLength() > 0) {
                mEditor = getActiveEditor();
                mFile = getSelectedFile(mEditor);
            }
        }

        action.setEnabled(mSelection != null && mFile != null);
    }

    /**
     * Create a new instance of our refactoring and a wizard to configure it.
     */
    public void run(IAction action) {
        if (mSelection != null && mFile != null) {
            ExtractStringRefactoring ref = new ExtractStringRefactoring(mFile, mEditor, mSelection);
            RefactoringWizard wizard = new ExtractStringWizard(ref, mFile.getProject());
            RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
            try {
                op.run(mWindow.getShell(), wizard.getDefaultPageTitle());
            } catch (InterruptedException e) {
                // Interrupted. Pass.
            }
        }
    }

    /**
     * Returns the active editor (hopefully matching our selection) or null.
     */
    private IEditorPart getActiveEditor() {
        IWorkbenchWindow wwin = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (wwin != null) {
            IWorkbenchPage page = wwin.getActivePage();
            if (page != null) {
                return page.getActiveEditor();
            }
        }

        return null;
    }

    /**
     * Returns the active {@link IFile} (hopefully matching our selection) or null.
     * The file is only returned if it's a file from a project with an Android nature.
     * <p/>
     * At that point we do not try to analyze if the selection nor the file is suitable
     * for the refactoring. This check is performed when the refactoring is invoked since
     * it can then produce meaningful error messages as needed.
     */
    private IFile getSelectedFile(IEditorPart editor) {
        if (editor != null) {
            IEditorInput input = editor.getEditorInput();

            if (input instanceof FileEditorInput) {
                FileEditorInput fi = (FileEditorInput) input;
                IFile file = fi.getFile();
                if (file.exists()) {
                    IProject proj = file.getProject();
                    try {
                        if (proj != null && proj.hasNature(AndroidConstants.NATURE)) {
                            return file;
                        }
                    } catch (CoreException e) {
                        // ignore
                    }
                }
            }
        }

        return null;
    }
}
