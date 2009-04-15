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
import com.android.ide.eclipse.common.AndroidConstants;
import com.android.ide.eclipse.common.project.AndroidManifestParser;
import com.android.ide.eclipse.common.project.BaseProjectHelper;
import com.android.ide.eclipse.editors.AndroidEditor;
import com.android.ide.eclipse.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.editors.descriptors.TextAttributeDescriptor;
import com.android.ide.eclipse.editors.manifest.descriptors.AndroidManifestDescriptors;
import com.android.ide.eclipse.editors.ui.SectionHelper;
import com.android.ide.eclipse.editors.uimodel.UiElementNode;
import com.android.ide.eclipse.editors.uimodel.UiTextAttributeNode;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.OpenNewClassWizardAction;
import org.eclipse.jdt.ui.dialogs.ITypeInfoFilterExtension;
import org.eclipse.jdt.ui.dialogs.ITypeInfoRequestor;
import org.eclipse.jdt.ui.dialogs.ITypeSelectionComponent;
import org.eclipse.jdt.ui.dialogs.TypeSelectionExtension;
import org.eclipse.jdt.ui.wizards.NewClassWizardPage;
import org.eclipse.jface.dialogs.IMessageProvider;
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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.w3c.dom.Element;

import java.util.ArrayList;

/**
 * Represents an XML attribute for a class, that can be modified using a simple text field or
 * a dialog to choose an existing class. Also, there's a link to create a new class.
 * <p/>
 * See {@link UiTextAttributeNode} for more information.
 */
public class UiClassAttributeNode extends UiTextAttributeNode {

    private String mReferenceClass;
    private IPostTypeCreationAction mPostCreationAction;
    private boolean mMandatory;
    private final boolean mDefaultToProjectOnly;
    
    private class HierarchyTypeSelection extends TypeSelectionExtension {
        
        private IJavaProject mJavaProject;
        private IType mReferenceType;
        private Button mProjectOnly;
        private boolean mUseProjectOnly;

        public HierarchyTypeSelection(IProject project, String referenceClass)
                throws JavaModelException {
            mJavaProject = JavaCore.create(project);
            mReferenceType = mJavaProject.findType(referenceClass);
        }

        @Override
        public ITypeInfoFilterExtension getFilterExtension() {
            return new ITypeInfoFilterExtension() {
                public boolean select(ITypeInfoRequestor typeInfoRequestor) {
                    
                    boolean projectOnly = mUseProjectOnly;
                    
                    String packageName = typeInfoRequestor.getPackageName();
                    String typeName = typeInfoRequestor.getTypeName();
                    String enclosingType = typeInfoRequestor.getEnclosingName();
                    
                    // build the full class name.
                    StringBuilder sb = new StringBuilder(packageName);
                    sb.append('.');
                    if (enclosingType.length() > 0) {
                        sb.append(enclosingType);
                        sb.append('.');
                    }
                    sb.append(typeName);
                    
                    String className = sb.toString();
                    
                    try {
                        IType type = mJavaProject.findType(className);

                        if (type == null) {
                            return false;
                        }

                        // don't display abstract classes
                        if ((type.getFlags() & Flags.AccAbstract) != 0) {
                            return false;
                        }

                        // if project-only is selected, make sure the package fragment is
                        // an actual source (thus "from this project").
                        if (projectOnly) {
                            IPackageFragment frag = type.getPackageFragment();
                            if (frag == null || frag.getKind() != IPackageFragmentRoot.K_SOURCE) {
                                return false;
                            }
                        }
                        
                        // get the type hierarchy and reference type is one of the super classes.
                        ITypeHierarchy hierarchy = type.newSupertypeHierarchy(
                                new NullProgressMonitor());
                        
                        IType[] supertypes = hierarchy.getAllSupertypes(type);
                        int n = supertypes.length;
                        for (int i = 0; i < n; i++) {
                            IType st = supertypes[i];
                            if (mReferenceType.equals(st)) {
                                return true;
                            }
                        }
                    } catch (JavaModelException e) {
                    }
                    
                    return false;
                }
            };
        }
        
        @Override
        public Control createContentArea(Composite parent) {

            mProjectOnly = new Button(parent, SWT.CHECK);
            mProjectOnly.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mProjectOnly.setText(String.format("Display classes from sources of project '%s' only",
                    mJavaProject.getProject().getName()));
            
            mUseProjectOnly = mDefaultToProjectOnly;
            mProjectOnly.setSelection(mUseProjectOnly);
            
            mProjectOnly.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    super.widgetSelected(e);
                    mUseProjectOnly = mProjectOnly.getSelection();
                    getTypeSelectionComponent().triggerSearch();
                }
            });
            
            return super.createContentArea(parent);
        }
    }

    /**
     * Classes which implement this interface provide a method processing newly created classes.
     */
    public static interface IPostTypeCreationAction {
        /**
         * Sent to process a newly created class.
         * @param newType the IType representing the newly created class.
         */
        public void processNewType(IType newType);
    }

    /**
     * Creates a {@link UiClassAttributeNode} object that will display ui to select or create
     * classes.
     * @param referenceClass The allowed supertype of the classes that are to be selected
     * or created. Can be null.
     * @param postCreationAction a {@link IPostTypeCreationAction} object handling post creation
     * modification of the class.
     * @param mandatory indicates if the class value is mandatory
     * @param attributeDescriptor the {@link AttributeDescriptor} object linked to the Ui Node.
     * @param defaultToProjectOnly When true display classes of this project only by default.
     *         When false any class path will be considered. The user can always toggle this. 
     */
    public UiClassAttributeNode(String referenceClass, IPostTypeCreationAction postCreationAction,
            boolean mandatory, AttributeDescriptor attributeDescriptor, UiElementNode uiParent,
            boolean defaultToProjectOnly) {
        super(attributeDescriptor, uiParent);
        
        mReferenceClass = referenceClass;
        mPostCreationAction = postCreationAction;
        mMandatory = mandatory;
        mDefaultToProjectOnly = defaultToProjectOnly;
    }

    /* (non-java doc)
     * Creates a label widget and an associated text field.
     * <p/>
     * As most other parts of the android manifest editor, this assumes the
     * parent uses a table layout with 2 columns.
     */
    @Override
    public void createUiControl(final Composite parent, IManagedForm managedForm) {
        setManagedForm(managedForm);
        FormToolkit toolkit = managedForm.getToolkit();
        TextAttributeDescriptor desc = (TextAttributeDescriptor) getDescriptor();

        StringBuilder label = new StringBuilder();
        label.append("<form><p><a href='unused'>");
        label.append(desc.getUiName());
        label.append("</a></p></form>");
        FormText formText = SectionHelper.createFormText(parent, toolkit, true /* isHtml */,
                label.toString(), true /* setupLayoutData */);
        formText.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(HyperlinkEvent e) {
                super.linkActivated(e);
                handleLabelClick();
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
        Button browseButton = toolkit.createButton(composite, "Browse...", SWT.PUSH);
        
        setTextWidget(text);

        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                handleBrowseClick();
            }
        });
    }
    
    /* (non-java doc)
     * 
     * Add a modify listener that will check the validity of the class
     */
    @Override
    protected void onAddValidators(final Text text) {
        ModifyListener listener = new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                try {
                    String textValue = text.getText().trim();
                    if (textValue.length() == 0) {
                        if (mMandatory) {
                            setErrorMessage("Value is mandatory", text);
                        } else {
                            setErrorMessage(null, text);
                        }
                        return;
                    }
                    // first we need the current java package.
                    String javaPackage = getManifestPackage();

                    // build the fully qualified name of the class
                    String className = AndroidManifestParser.combinePackageAndClassName(
                            javaPackage, textValue);
                    
                    // only test the vilibility for activities.
                    boolean testVisibility = AndroidConstants.CLASS_ACTIVITY.equals(
                            mReferenceClass); 

                    // test the class
                    setErrorMessage(BaseProjectHelper.testClassForManifest(
                            BaseProjectHelper.getJavaProject(getProject()), className,
                            mReferenceClass, testVisibility), text);
                } catch (CoreException ce) {
                    setErrorMessage(ce.getMessage(), text);
                }
            }
        };

        text.addModifyListener(listener);

        // Make sure the validator removes its message(s) when the widget is disposed
        text.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                // we don't want to use setErrorMessage, because we don't want to reset
                // the error flag in the UiAttributeNode
                getManagedForm().getMessageManager().removeMessage(text, text);
            }
        });

        // Finally call the validator once to make sure the initial value is processed
        listener.modifyText(null);
    }

    private void handleBrowseClick() {
        Text text = getTextWidget();
        
        // we need to get the project of the manifest.
        IProject project = getProject();
        if (project != null) {
            
            // Create a search scope including only the source folder of the current
            // project.
            IPackageFragmentRoot[] packageFragmentRoots = getPackageFragmentRoots(project,
                    true /*include_containers*/);
            IJavaSearchScope scope = SearchEngine.createJavaSearchScope(
                    packageFragmentRoots,
                    false);

            try {
                SelectionDialog dlg = JavaUI.createTypeDialog(text.getShell(),
                    PlatformUI.getWorkbench().getProgressService(),
                    scope,
                    IJavaElementSearchConstants.CONSIDER_CLASSES,  // style
                    false, // no multiple selection
                    "**",  //$NON-NLS-1$ //filter
                    new HierarchyTypeSelection(project, mReferenceClass));
                dlg.setMessage(String.format("Select class name for element %1$s:",
                        getUiParent().getBreadcrumbTrailDescription(false /* include_root */)));
                if (dlg instanceof ITypeSelectionComponent) {
                    ((ITypeSelectionComponent)dlg).triggerSearch();
                }
                
                if (dlg.open() == Window.OK) {
                    Object[] results = dlg.getResult();
                    if (results.length == 1) {
                        handleNewType((IType)results[0]);
                    }
                }
            } catch (JavaModelException e1) {
                AdtPlugin.log(e1, "UiClassAttributeNode HandleBrowser failed");
            }
        }
    }

    private void handleLabelClick() {
        // get the current value
        String className = getTextWidget().getText().trim();

        // get the package name from the manifest.
        String packageName = getManifestPackage();
        
        if (className.length() == 0) {
            createNewClass(packageName, null /* className */);
        } else {
            // build back the fully qualified class name.
            String fullClassName = className;
            if (className.startsWith(".")) { //$NON-NLS-1$
                fullClassName = packageName + className;
            } else {
                String[] segments = className.split(AndroidConstants.RE_DOT);
                if (segments.length == 1) {
                    fullClassName = packageName + "." + className; //$NON-NLS-1$
                }
            }
            
            // in case the type is enclosed, we need to replace the $ with .
            fullClassName = fullClassName.replaceAll("\\$", "\\."); //$NON-NLS-1$ //$NON-NLS2$
            
            // now we try to find the file that contains this class and we open it in the editor.
            IProject project = getProject();
            IJavaProject javaProject = JavaCore.create(project);

            try {
                IType result = javaProject.findType(fullClassName);
                if (result != null) {
                    JavaUI.openInEditor(result);
                } else {
                    // split the last segment from the fullClassname
                    int index = fullClassName.lastIndexOf('.');
                    if (index != -1) {
                        createNewClass(fullClassName.substring(0, index),
                                fullClassName.substring(index+1));
                    } else {
                        createNewClass(packageName, className);
                    }
                }
            } catch (JavaModelException e) {
                AdtPlugin.log(e, "UiClassAttributeNode HandleLabel failed");
            } catch (PartInitException e) {
                AdtPlugin.log(e, "UiClassAttributeNode HandleLabel failed");
            }
        }
    }
    
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
     * Returns the current value of the /manifest/package attribute.
     * @return the package or an empty string if not found
     */
    private String getManifestPackage() {
        // get the root uiNode to get the 'package' attribute value.
        UiElementNode rootNode = getUiParent().getUiRoot();
                  
        Element xmlElement = (Element) rootNode.getXmlNode();

        if (xmlElement != null) {
            return xmlElement.getAttribute(AndroidManifestDescriptors.PACKAGE_ATTR);
        }
        return ""; //$NON-NLS-1$
    }


    /**
     * Computes and return the {@link IPackageFragmentRoot}s corresponding to the source folders of
     * the specified project.
     * @param project the project
     * @param b 
     * @return an array of IPackageFragmentRoot.
     */
    private IPackageFragmentRoot[] getPackageFragmentRoots(IProject project,
            boolean include_containers) {
        ArrayList<IPackageFragmentRoot> result = new ArrayList<IPackageFragmentRoot>();
        try {
            IJavaProject javaProject = JavaCore.create(project);
            IPackageFragmentRoot[] roots = javaProject.getPackageFragmentRoots();
            for (int i = 0; i < roots.length; i++) {
                IClasspathEntry entry = roots[i].getRawClasspathEntry();
                if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE ||
                        (include_containers &&
                                entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER)) {
                    result.add(roots[i]);
                }
            }
        } catch (JavaModelException e) {
        }

        return result.toArray(new IPackageFragmentRoot[result.size()]);
    }
    
    private void handleNewType(IType type) {
        Text text = getTextWidget();

        // get the fully qualified name with $ to properly detect the enclosing types.
        String name = type.getFullyQualifiedName('$');
        
        String packageValue = getManifestPackage();
        
        // check if the class doesn't start with the package.
        if (packageValue.length() > 0 && name.startsWith(packageValue)) {
            // if it does, we remove the package and the first dot.
            name = name.substring(packageValue.length() + 1);
            
            // look for how many segments we have left.
            // if one, just write it that way.
            // if more than one, write it with a leading dot.
            String[] packages = name.split(AndroidConstants.RE_DOT);
            if (packages.length == 1) {
                text.setText(name);
            } else {
                text.setText("." + name); //$NON-NLS-1$
            }
        } else {
            text.setText(name);
        }
    }
    
    private void createNewClass(String packageName, String className) {
        // create the wizard page for the class creation, and configure it
        NewClassWizardPage page = new NewClassWizardPage();
        
        // set the parent class
        page.setSuperClass(mReferenceClass, true /* canBeModified */);
        
        // get the source folders as java elements.
        IPackageFragmentRoot[] roots = getPackageFragmentRoots(getProject(),
                true /*include_containers*/);

        IPackageFragmentRoot currentRoot = null;
        IPackageFragment currentFragment = null;
        int packageMatchCount = -1;
        
        for (IPackageFragmentRoot root : roots) {
            // Get the java element for the package.
            // This method is said to always return a IPackageFragment even if the
            // underlying folder doesn't exist...
            IPackageFragment fragment = root.getPackageFragment(packageName);
            if (fragment != null && fragment.exists()) {
                // we have a perfect match! we use it.
                currentRoot = root;
                currentFragment = fragment;
                packageMatchCount = -1;
                break;
            } else {
                // we don't have a match. we look for the fragment with the best match
                // (ie the closest parent package we can find)
                try {
                    IJavaElement[] children;
                    children = root.getChildren();
                    for (IJavaElement child : children) {
                        if (child instanceof IPackageFragment) {
                            fragment = (IPackageFragment)child;
                            if (packageName.startsWith(fragment.getElementName())) {
                                // its a match. get the number of segments
                                String[] segments = fragment.getElementName().split("\\."); //$NON-NLS-1$
                                if (segments.length > packageMatchCount) {
                                    packageMatchCount = segments.length;
                                    currentFragment = fragment;
                                    currentRoot = root;
                                }
                            }
                        }
                    }
                } catch (JavaModelException e) {
                    // Couldn't get the children: we just ignore this package root.
                }
            }
        }
        
        ArrayList<IPackageFragment> createdFragments = null;

        if (currentRoot != null) {
            // if we have a perfect match, we set it and we're done.
            if (packageMatchCount == -1) {
                page.setPackageFragmentRoot(currentRoot, true /* canBeModified*/);
                page.setPackageFragment(currentFragment, true /* canBeModified */);
            } else {
                // we have a partial match.
                // create the package. We have to start with the first segment so that we
                // know what to delete in case of a cancel.
                try {
                    createdFragments = new ArrayList<IPackageFragment>();
                    
                    int totalCount = packageName.split("\\.").length; //$NON-NLS-1$
                    int count = 0;
                    int index = -1;
                    // skip the matching packages
                    while (count < packageMatchCount) {
                        index = packageName.indexOf('.', index+1);
                        count++;
                    }
                    
                    // create the rest of the segments, except for the last one as indexOf will
                    // return -1;
                    while (count < totalCount - 1) {
                        index = packageName.indexOf('.', index+1);
                        count++;
                        createdFragments.add(currentRoot.createPackageFragment(
                                packageName.substring(0, index),
                                true /* force*/, new NullProgressMonitor()));
                    }
                    
                    // create the last package
                    createdFragments.add(currentRoot.createPackageFragment(
                            packageName, true /* force*/, new NullProgressMonitor()));
                    
                    // set the root and fragment in the Wizard page
                    page.setPackageFragmentRoot(currentRoot, true /* canBeModified*/);
                    page.setPackageFragment(createdFragments.get(createdFragments.size()-1),
                            true /* canBeModified */);
                } catch (JavaModelException e) {
                    // if we can't create the packages, there's a problem. we revert to the default
                    // package
                    for (IPackageFragmentRoot root : roots) {
                        // Get the java element for the package.
                        // This method is said to always return a IPackageFragment even if the
                        // underlying folder doesn't exist...
                        IPackageFragment fragment = root.getPackageFragment(packageName);
                        if (fragment != null && fragment.exists()) {
                            page.setPackageFragmentRoot(root, true /* canBeModified*/);
                            page.setPackageFragment(fragment, true /* canBeModified */);
                            break;
                        }
                    }
                }
            }
        } else if (roots.length > 0) {
            // if we haven't found a valid fragment, we set the root to the first source folder.
            page.setPackageFragmentRoot(roots[0], true /* canBeModified*/);
        }
        
        // if we have a starting class name we use it
        if (className != null) {
            page.setTypeName(className, true /* canBeModified*/);
        }
        
        // create the action that will open it the wizard.
        OpenNewClassWizardAction action = new OpenNewClassWizardAction();
        action.setConfiguredWizardPage(page);
        action.run();
        IJavaElement element = action.getCreatedElement();
        
        if (element != null) {
            if (element.getElementType() == IJavaElement.TYPE) {
                    
                IType type = (IType)element;
                
                if (mPostCreationAction != null) {
                    mPostCreationAction.processNewType(type);
                }
                
                handleNewType(type);
            }
        } else {
            // lets delete the packages we created just for this.
            // we need to start with the leaf and go up
            if (createdFragments != null) {
                try {
                    for (int i = createdFragments.size() - 1 ; i >= 0 ; i--) {
                        createdFragments.get(i).delete(true /* force*/, new NullProgressMonitor());
                    }
                } catch (JavaModelException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Sets the error messages. If message is <code>null</code>, the message is removed.
     * @param message the message to set, or <code>null</code> to remove the current message
     * @param textWidget the {@link Text} widget associated to the message.
     */
    private final void setErrorMessage(String message, Text textWidget) {
        if (message != null) {
            setHasError(true);
            getManagedForm().getMessageManager().addMessage(textWidget, message, null /* data */,
                    IMessageProvider.ERROR, textWidget);
        } else {
            setHasError(false);
            getManagedForm().getMessageManager().removeMessage(textWidget, textWidget);
        }
    }
    
    @Override
    public String[] getPossibleValues(String prefix) {
        // TODO: compute a list of existing classes for content assist completion
        return null;
    }
}

