/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ide.eclipse.editors.manifest.model;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.editors.AndroidEditor;
import com.android.ide.eclipse.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.editors.descriptors.TextAttributeDescriptor;
import com.android.ide.eclipse.editors.ui.SectionHelper;
import com.android.ide.eclipse.editors.uimodel.UiElementNode;
import com.android.ide.eclipse.editors.uimodel.UiTextAttributeNode;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.OpenNewPackageWizardAction;
import org.eclipse.jdt.ui.actions.ShowInPackageViewAction;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;

import java.util.ArrayList;

/**
 * Represents an XML attribute for a package, that can be modified using a simple text field or
 * a dialog to choose an existing package. Also, there's a link to create a new package.
 * <p/>
 * See {@link UiTextAttributeNode} for more information.
 */
public class UiPackageAttributeNode extends UiTextAttributeNode {

    /**
     * Creates a {@link UiPackageAttributeNode} object that will display ui to select or create
     * a package.
     * @param attributeDescriptor the {@link AttributeDescriptor} object linked to the Ui Node.
     */
    public UiPackageAttributeNode(AttributeDescriptor attributeDescriptor, UiElementNode uiParent) {
        super(attributeDescriptor, uiParent);
    }

    /* (non-java doc)
     * Creates a label widget and an associated text field.
     * <p/>
     * As most other parts of the android manifest editor, this assumes the
     * parent uses a table layout with 2 columns.
     */
    @Override
    public void createUiControl(final Composite parent, final IManagedForm managedForm) {
        setManagedForm(managedForm);
        FormToolkit toolkit = managedForm.getToolkit();
        TextAttributeDescriptor desc = (TextAttributeDescriptor) getDescriptor();

        StringBuilder label = new StringBuilder();
        label.append("<form><p><a href='unused'>");  //$NON-NLS-1$
        label.append(desc.getUiName());
        label.append("</a></p></form>");  //$NON-NLS-1$
        FormText formText = SectionHelper.createFormText(parent, toolkit, true /* isHtml */,
                label.toString(), true /* setupLayoutData */);
        formText.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(HyperlinkEvent e) {
                super.linkActivated(e);
                doLabelClick();
            }
        });
        formText.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));
        SectionHelper.addControlTooltip(formText, desc.getTooltip());
        
        Composite composite = toolkit.createComposite(parent);
        composite.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.MIDDLE));
        GridLayout gl = new GridLayout(2, false);
        gl.marginHeight = gl.marginWidth = 0;
        composite.setLayout(gl);
        // Fixes missing text borders under GTK... also requires adding a 1-pixel margin
        // for the text field below
        toolkit.paintBordersFor(composite);
        
        final Text text = toolkit.createText(composite, getCurrentValue());
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalIndent = 1;  // Needed by the fixed composite borders under GTK
        text.setLayoutData(gd);

        setTextWidget(text);

        Button browseButton = toolkit.createButton(composite, "Browse...", SWT.PUSH);
        
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                doBrowseClick();
            }
        });
        
    }
    
    /* (non-java doc)
     * Adds a validator to the text field that calls managedForm.getMessageManager().
     */
    @Override
    protected void onAddValidators(final Text text) {
        ModifyListener listener = new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String package_name = text.getText();
                if (package_name.indexOf('.') < 1) {
                    getManagedForm().getMessageManager().addMessage(text,
                            "Package name should contain at least two identifiers.",
                            null /* data */, IMessageProvider.ERROR, text);
                } else {
                    getManagedForm().getMessageManager().removeMessage(text, text);
                }
            }
        };

        text.addModifyListener(listener);

        // Make sure the validator removes its message(s) when the widget is disposed
        text.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                getManagedForm().getMessageManager().removeMessage(text, text);
            }
        });

        // Finally call the validator once to make sure the initial value is processed
        listener.modifyText(null);
    }

    /**
     * Handles response to the Browse button by creating a Package dialog.
     * */
    private void doBrowseClick() {
        Text text = getTextWidget();
        
        // we need to get the project of the manifest.
        IProject project = getProject();
        if (project != null) {
            
            try {
                SelectionDialog dlg = JavaUI.createPackageDialog(text.getShell(),
                        JavaCore.create(project), 0);
                dlg.setTitle("Select Android Package");
                dlg.setMessage("Select the package for the Android project.");
                SelectionDialog.setDefaultImage(AdtPlugin.getAndroidLogo());

                if (dlg.open() == Window.OK) {
                    Object[] results = dlg.getResult();
                    if (results.length == 1) {
                        setPackageTextField((IPackageFragment)results[0]);
                    }
                }
            } catch (JavaModelException e1) {
            }
        }
    }

    /**
     * Handles response to the Label hyper link being activated.
     */
    private void doLabelClick() {
        // get the current package name
        String package_name = getTextWidget().getText().trim();
        
        if (package_name.length() == 0) {
            createNewPackage();
        } else {
            // Try to select the package in the Package Explorer for the current
            // project and the current editor's site.

            IProject project = getProject();
            if (project == null) {
                AdtPlugin.log(IStatus.ERROR, "Failed to get project for UiPackageAttribute"); //$NON-NLS-1$
                return;
            }

            IWorkbenchPartSite site = getUiParent().getEditor().getSite();
            if (site == null) {
                AdtPlugin.log(IStatus.ERROR, "Failed to get editor site for UiPackageAttribute"); //$NON-NLS-1$
                return;
            }

            for (IPackageFragmentRoot root : getPackageFragmentRoots(project)) {
                IPackageFragment fragment = root.getPackageFragment(package_name);
                if (fragment != null && fragment.exists()) {
                    ShowInPackageViewAction action = new ShowInPackageViewAction(site);
                    action.run(fragment);
                    // This action's run() doesn't provide the status (although internally it could)
                    // so we just assume it worked.
                    return;
                }
            }
        }
    }

    /**
     * Utility method that returns the project for the current file being edited.
     * 
     * @return The IProject for the current file being edited or null.
     */
    private IProject getProject() {
        UiElementNode uiNode = getUiParent();
        AndroidEditor editor = uiNode.getEditor();
        IEditorInput input = editor.getEditorInput();
        if (input instanceof IFileEditorInput) {
            // from the file editor we can get the IFile object, and from it, the IProject.
            IFile file = ((IFileEditorInput)input).getFile();
            return file.getProject();
        }
        
        return null;
    }

    /**
     * Utility method that computes and returns the list of {@link IPackageFragmentRoot}
     * corresponding to the source folder of the specified project.
     * 
     * @param project the project
     * @return an array of IPackageFragmentRoot. Can be empty but not null.
     */
    private IPackageFragmentRoot[] getPackageFragmentRoots(IProject project) {
        ArrayList<IPackageFragmentRoot> result = new ArrayList<IPackageFragmentRoot>();
        try {
            IJavaProject javaProject = JavaCore.create(project);
            IPackageFragmentRoot[] roots = javaProject.getPackageFragmentRoots();
            for (int i = 0; i < roots.length; i++) {
                IClasspathEntry entry = roots[i].getRawClasspathEntry();
                if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    result.add(roots[i]);
                }
            }
        } catch (JavaModelException e) {
        }

        return result.toArray(new IPackageFragmentRoot[result.size()]);
    }
    
    /**
     * Utility method that sets the package's text field to the package fragment's name.
     * */
    private void setPackageTextField(IPackageFragment type) {
        Text text = getTextWidget();

        String name = type.getElementName();
        
        text.setText(name);
    }
    

    /**
     * Displays and handles a "Create Package Wizard".
     * 
     * This is invoked by doLabelClick() when clicking on the hyperlink label with an
     * empty package text field.  
     */
    private void createNewPackage() {
        OpenNewPackageWizardAction action = new OpenNewPackageWizardAction();

        IProject project = getProject();
        action.setSelection(new StructuredSelection(project));
        action.run();

        IJavaElement element = action.getCreatedElement();
        if (element != null &&
                element.exists() &&
                element.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
            setPackageTextField((IPackageFragment) element);
        }
    }
    
    @Override
    public String[] getPossibleValues(String prefix) {
        // TODO: compute a list of existing packages for content assist completion
        return null;
    }
}

