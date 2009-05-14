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

package com.android.ide.eclipse.adt.internal.ui;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.AndroidConstants;
import com.android.ide.eclipse.adt.internal.resources.manager.ProjectResourceItem;
import com.android.ide.eclipse.adt.internal.resources.manager.ProjectResources;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceFile;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceManager;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceMonitor.IResourceEventListener;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;

import java.util.Iterator;

/**
 * Resource Explorer View.
 * <p/>
 * This contains a basic Tree view, and uses a TreeViewer to handle the data.
 * <p/>
 * The view listener to change in selection in the workbench, and update to show the resource
 * of the project of the current selected item (either item in the package explorer, or of the
 * current editor).
 * 
 * @see ResourceContentProvider
 */
public class ResourceExplorerView extends ViewPart implements ISelectionListener,
        IResourceEventListener {
    
    // Note: keep using the obsolete AndroidConstants.EDITORS_NAMESPACE (which used
    // to be the Editors Plugin ID) to keep existing preferences functional.
    private final static String PREFS_COLUMN_RES =
        AndroidConstants.EDITORS_NAMESPACE + "ResourceExplorer.Col1"; //$NON-NLS-1$
    private final static String PREFS_COLUMN_2 =
        AndroidConstants.EDITORS_NAMESPACE + "ResourceExplorer.Col2"; //$NON-NLS-1$

    private Tree mTree;
    private TreeViewer mTreeViewer;
    
    private IProject mCurrentProject;

    public ResourceExplorerView() {
    }

    @Override
    public void createPartControl(Composite parent) {
        mTree = new Tree(parent, SWT.SINGLE | SWT.VIRTUAL);
        mTree.setLayoutData(new GridData(GridData.FILL_BOTH));
        mTree.setHeaderVisible(true);
        mTree.setLinesVisible(true);

        final IPreferenceStore store = AdtPlugin.getDefault().getPreferenceStore();

        // create 2 columns. The main one with the resources, and an "info" column.
        createTreeColumn(mTree, "Resources", SWT.LEFT,
                "abcdefghijklmnopqrstuvwxz", -1, PREFS_COLUMN_RES, store); //$NON-NLS-1$
        createTreeColumn(mTree, "Info", SWT.LEFT,
                "0123456789", -1, PREFS_COLUMN_2, store); //$NON-NLS-1$

        // create the jface wrapper
        mTreeViewer = new TreeViewer(mTree);
        
        mTreeViewer.setContentProvider(new ResourceContentProvider(true /* fullLevels */));
        mTreeViewer.setLabelProvider(new ResourceLabelProvider());
        
        // listen to selection change in the workbench.
        IWorkbenchPage page = getSite().getPage();
        
        page.addSelectionListener(this);
        
        // init with current selection
        selectionChanged(getSite().getPart(), page.getSelection());
        
        // add support for double click.
        mTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                ISelection sel = event.getSelection();

                if (sel instanceof IStructuredSelection) {
                    IStructuredSelection selection = (IStructuredSelection) sel;

                    if (selection.size() == 1) {
                        Object element = selection.getFirstElement();
                        
                        // if it's a resourceFile, we directly open it.
                        if (element instanceof ResourceFile) {
                            try {
                                IDE.openEditor(getSite().getWorkbenchWindow().getActivePage(),
                                        ((ResourceFile)element).getFile().getIFile());
                            } catch (PartInitException e) {
                            }
                        } else if (element instanceof ProjectResourceItem) {
                            // if it's a ResourceItem, we open the first file, but only if
                            // there's no alternate files.
                            ProjectResourceItem item = (ProjectResourceItem)element;
                            
                            if (item.isEditableDirectly()) {
                                ResourceFile[] files = item.getSourceFileArray();
                                if (files[0] != null) {
                                    try {
                                        IDE.openEditor(
                                                getSite().getWorkbenchWindow().getActivePage(),
                                                files[0].getFile().getIFile());
                                    } catch (PartInitException e) {
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
        
        // set up the resource manager to send us resource change notification
        AdtPlugin.getDefault().getResourceMonitor().addResourceEventListener(this);
    }
    
    @Override
    public void dispose() {
        AdtPlugin.getDefault().getResourceMonitor().removeResourceEventListener(this);

        super.dispose();
    }

    @Override
    public void setFocus() {
        mTree.setFocus();
    }

    /**
     * Processes a new selection.
     */
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        // first we test if the part is an editor.
        if (part instanceof IEditorPart) {
            // if it is, we check if it's a file editor.
            IEditorInput input = ((IEditorPart)part).getEditorInput();
            
            if (input instanceof IFileEditorInput) {
                // from the file editor we can get the IFile object, and from it, the IProject.
                IFile file = ((IFileEditorInput)input).getFile();
                
                // get the file project
                IProject project = file.getProject();
                
                handleProjectSelection(project);
            }
        } else if (selection instanceof IStructuredSelection) {
            // if it's not an editor, we look for structured selection.
            for (Iterator<?> it = ((IStructuredSelection) selection).iterator();
                    it.hasNext();) {
                Object element = it.next();
                IProject project = null;
                
                // if we are in the navigator or package explorer, the selection could contain a
                // IResource object.
                if (element instanceof IResource) {
                    project = ((IResource) element).getProject();
                } else if (element instanceof IJavaElement) {
                    // if we are in the package explorer on a java element, we handle that too.
                    IJavaElement javaElement = (IJavaElement)element;
                    IJavaProject javaProject = javaElement.getJavaProject();
                    if (javaProject != null) {
                        project = javaProject.getProject();
                    }
                } else if (element instanceof IAdaptable) {
                    // finally we try to get a project object from IAdaptable.
                    project = (IProject) ((IAdaptable) element)
                            .getAdapter(IProject.class);
                }

                // if we found a project, handle it, and return.
                if (project != null) {
                    if (handleProjectSelection(project)) {
                        return;
                    }
                }
            }
        }
    }

    /**
     * Handles a project selection.
     * @param project the new selected project
     * @return true if the project could be processed.
     */
    private boolean handleProjectSelection(IProject project) {
        try {
            // if it's an android project, then we get its resources, and feed them
            // to the tree viewer.
            if (project.hasNature(AndroidConstants.NATURE)) {
                if (mCurrentProject != project) {
                    ProjectResources projRes = ResourceManager.getInstance().getProjectResources(
                            project);
                    if (projRes != null) {
                        mTreeViewer.setInput(projRes);
                        mCurrentProject = project;
                        return true;
                    }
                }
            }
        } catch (CoreException e) {
        }
        
        return false;
    }
    
    /**
     * Create a TreeColumn with the specified parameters. If a
     * <code>PreferenceStore</code> object and a preference entry name String
     * object are provided then the column will listen to change in its width
     * and update the preference store accordingly.
     *
     * @param parent The Table parent object
     * @param header The header string
     * @param style The column style
     * @param sample_text A sample text to figure out column width if preference
     *            value is missing
     * @param fixedSize a fixed size. If != -1 the column is non resizable
     * @param pref_name The preference entry name for column width
     * @param prefs The preference store
     */
    public void createTreeColumn(Tree parent, String header, int style,
            String sample_text, int fixedSize, final String pref_name,
            final IPreferenceStore prefs) {

        // create the column
        TreeColumn col = new TreeColumn(parent, style);
        
        if (fixedSize != -1) {
            col.setWidth(fixedSize);
            col.setResizable(false);
        } else {
            // if there is no pref store or the entry is missing, we use the sample
            // text and pack the column.
            // Otherwise we just read the width from the prefs and apply it.
            if (prefs == null || prefs.contains(pref_name) == false) {
                col.setText(sample_text);
                col.pack();
    
                // init the prefs store with the current value
                if (prefs != null) {
                    prefs.setValue(pref_name, col.getWidth());
                }
            } else {
                col.setWidth(prefs.getInt(pref_name));
            }
    
            // if there is a pref store and a pref entry name, then we setup a
            // listener to catch column resize to put the new width value into the store.
            if (prefs != null && pref_name != null) {
                col.addControlListener(new ControlListener() {
                    public void controlMoved(ControlEvent e) {
                    }
    
                    public void controlResized(ControlEvent e) {
                        // get the new width
                        int w = ((TreeColumn)e.widget).getWidth();
    
                        // store in pref store
                        prefs.setValue(pref_name, w);
                    }
                });
            }
        }

        // set the header
        col.setText(header);
    }

    /**
     * Processes a start in a resource event change.
     */
    public void resourceChangeEventStart() {
        // pass
    }

    /**
     * Processes the end of a resource change event.
     */
    public void resourceChangeEventEnd() {
        try {
            mTree.getDisplay().asyncExec(new Runnable() {
                public void run() {
                    if (mTree.isDisposed() == false) {
                        mTreeViewer.refresh();
                    }
                }
            });
        } catch (SWTException e) {
            // display is disposed. nothing to do.
        }
    }
}
