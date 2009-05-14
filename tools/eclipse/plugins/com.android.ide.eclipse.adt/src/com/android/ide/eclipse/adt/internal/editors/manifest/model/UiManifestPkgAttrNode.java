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

package com.android.ide.eclipse.adt.internal.editors.manifest.model;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.TextAttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.manifest.ManifestEditor;
import com.android.ide.eclipse.adt.internal.editors.ui.SectionHelper;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiTextAttributeNode;
import com.android.ide.eclipse.adt.internal.project.AndroidManifestParser;
import com.android.ide.eclipse.adt.internal.project.BaseProjectHelper;
import com.android.ide.eclipse.adt.internal.wizards.actions.NewProjectAction;
import com.android.ide.eclipse.adt.internal.wizards.newproject.NewProjectWizard;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.part.FileEditorInput;

import java.util.TreeSet;

/**
 * Represents an XML attribute to select an exisintg manifest package, that can be modified using
 * a simple text field or a dialog to choose an existing package.
 * <p/>
 * See {@link UiTextAttributeNode} for more information.
 */
public class UiManifestPkgAttrNode extends UiTextAttributeNode {

    /**
     * Creates a {@link UiManifestPkgAttrNode} object that will display ui to select or create
     * a manifest package.
     * @param attributeDescriptor the {@link AttributeDescriptor} object linked to the Ui Node.
     */
    public UiManifestPkgAttrNode(AttributeDescriptor attributeDescriptor, UiElementNode uiParent) {
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
        
        // Display the list of AndroidManifest packages in a selection dialog
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(
                getTextWidget().getShell(),
                new ILabelProvider() {
                    public Image getImage(Object element) {
                        return null;
                    }

                    public String getText(Object element) {
                        return element.toString();
                    }

                    public void addListener(ILabelProviderListener listener) {
                    }

                    public void dispose() {
                    }

                    public boolean isLabelProperty(Object element, String property) {
                        return false;
                    }

                    public void removeListener(ILabelProviderListener listener) {
                    }
                });

        dialog.setTitle("Android Manifest Package Selection");
        dialog.setMessage("Select the Android Manifest package to target.");

        dialog.setElements(getPossibleValues(null));

        // open the dialog and use the object selected if OK was clicked, or null otherwise
        if (dialog.open() == Window.OK) {
            String result = (String) dialog.getFirstResult();
            if (result != null && result.length() > 0) {
                getTextWidget().setText(result);
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
            createNewProject();
        } else {
            displayExistingManifest(package_name);
        }
    }

    /**
     * When the label is clicked and there's already a package name, this method
     * attempts to find the project matching the android package name and it attempts
     * to open the manifest editor.
     * 
     * @param package_name The android package name to find. Must not be null.
     */
    private void displayExistingManifest(String package_name) {

        // Look for the first project that uses this package name
        for (IJavaProject project : BaseProjectHelper.getAndroidProjects()) {
            // check that there is indeed a manifest file.
            IFile manifestFile = AndroidManifestParser.getManifest(project.getProject());
            if (manifestFile == null) {
                // no file? skip this project.
                continue;
            }

            AndroidManifestParser parser = null;
            try {
                parser = AndroidManifestParser.parseForData(manifestFile);
            } catch (CoreException e) {
                // ignore, handled below.
            }
            if (parser == null) {
                // skip this project.
                continue;
            }

            if (package_name.equals(parser.getPackage())) {
                // Found the project. 

                IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                if (win != null) {
                    IWorkbenchPage page = win.getActivePage();
                    if (page != null) {
                        try {
                            page.openEditor(
                                    new FileEditorInput(manifestFile),
                                    ManifestEditor.ID,
                                    true, /* activate */
                                    IWorkbenchPage.MATCH_INPUT);
                        } catch (PartInitException e) {
                            AdtPlugin.log(e,
                                    "Opening editor failed for %s",  //$NON-NLS-1$
                                    manifestFile.getFullPath());
                        }
                    }
                }

                // We found the project; even if we failed there's no need to keep looking.
                return;
            }
        }
    }

    /**
     * Displays the New Project Wizard to create a new project.
     * If one is successfully created, use the Android Package name.
     */
    private void createNewProject() {

        NewProjectAction npwAction = new NewProjectAction();
        npwAction.run(null /*action*/);
        if (npwAction.getDialogResult() == Dialog.OK) {
            NewProjectWizard npw = (NewProjectWizard) npwAction.getWizard();
            String name = npw.getPackageName();
            if (name != null && name.length() > 0) {
                getTextWidget().setText(name);
            }
        }
    }

    /**
     * Returns all the possible android package names that could be used.
     * The prefix is not used.
     * 
     * {@inheritDoc}
     */
    @Override
    public String[] getPossibleValues(String prefix) {
        TreeSet<String> packages = new TreeSet<String>();

        for (IJavaProject project : BaseProjectHelper.getAndroidProjects()) {
            // check that there is indeed a manifest file.
            IFile manifestFile = AndroidManifestParser.getManifest(project.getProject());
            if (manifestFile == null) {
                // no file? skip this project.
                continue;
            }

            AndroidManifestParser parser = null;
            try {
                parser = AndroidManifestParser.parseForData(manifestFile);
            } catch (CoreException e) {
                // ignore, handled below.
            }
            if (parser == null) {
                // skip this project.
                continue;
            }

            packages.add(parser.getPackage());
        }

        return packages.toArray(new String[packages.size()]);
    }
}

