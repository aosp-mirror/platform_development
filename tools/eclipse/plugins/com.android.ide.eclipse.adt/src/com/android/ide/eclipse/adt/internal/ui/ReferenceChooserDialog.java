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

package com.android.ide.eclipse.adt.internal.ui;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.refactorings.extractstring.ExtractStringRefactoring;
import com.android.ide.eclipse.adt.internal.refactorings.extractstring.ExtractStringWizard;
import com.android.ide.eclipse.adt.internal.resources.IResourceRepository;
import com.android.ide.eclipse.adt.internal.resources.ResourceItem;
import com.android.ide.eclipse.adt.internal.resources.ResourceType;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.dialogs.SelectionStatusDialog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A dialog to let the user choose a reference to a resource.
 *
 */
public class ReferenceChooserDialog extends SelectionStatusDialog {

    private static Pattern sResourcePattern = Pattern.compile("@(.*)/(.+)"); //$NON-NLS-1$
    private static Pattern sInlineIdResourcePattern = Pattern.compile("@\\+id/(.+)"); //$NON-NLS-1$

    private static IDialogSettings sDialogSettings = new DialogSettings("");
    
    private IResourceRepository mResources;
    private String mCurrentResource;
    private FilteredTree mFilteredTree;
    private Button mNewResButton;
    private final IProject mProject;
    private TreeViewer mTreeViewer;

    /**
     * @param project 
     * @param parent
     */
    public ReferenceChooserDialog(IProject project, IResourceRepository resources, Shell parent) {
        super(parent);
        mProject = project;
        mResources = resources;

        int shellStyle = getShellStyle();
        setShellStyle(shellStyle | SWT.MAX | SWT.RESIZE);

        setTitle("Reference Chooser");
        setMessage(String.format("Choose a resource"));
        
        setDialogBoundsSettings(sDialogSettings, getDialogBoundsStrategy());
    }

    public void setCurrentResource(String resource) {
        mCurrentResource = resource;
    }
    
    public String getCurrentResource() {
        return mCurrentResource;
    }


    /* (non-Javadoc)
     * @see org.eclipse.ui.dialogs.SelectionStatusDialog#computeResult()
     */
    @Override
    protected void computeResult() {
        // get the selection
        TreePath treeSelection = getSelection();
        if (treeSelection != null) {
            if (treeSelection.getSegmentCount() == 2) {
                // get the resource type and the resource item
                ResourceType resourceType = (ResourceType)treeSelection.getFirstSegment();
                ResourceItem resourceItem = (ResourceItem)treeSelection.getLastSegment();
                
                mCurrentResource = resourceType.getXmlString(resourceItem, false /* system */); 
            }
        }
    }
    
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite top = (Composite)super.createDialogArea(parent);

        // create the standard message area
        createMessageArea(top);

        // create the filtered tree
        createFilteredTree(top);

        // setup the initial selection
        setupInitialSelection();
        
        // create the "New Resource" button
        createNewResButtons(top);
        
        return top;
    }

    /**
     * Creates the "New Resource" button.
     * @param top the parent composite
     */
    private void createNewResButtons(Composite top) {
        mNewResButton = new Button(top, SWT.NONE);
        mNewResButton.addSelectionListener(new OnNewResButtonSelected());
        updateNewResButton();
    }

    private void createFilteredTree(Composite parent) {
        mFilteredTree = new FilteredTree(parent, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION,
                new PatternFilter());
        
        GridData data = new GridData();
        data.widthHint = convertWidthInCharsToPixels(60);
        data.heightHint = convertHeightInCharsToPixels(18);
        data.grabExcessVerticalSpace = true;
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = GridData.FILL;
        data.verticalAlignment = GridData.FILL;
        mFilteredTree.setLayoutData(data);
        mFilteredTree.setFont(parent.getFont());
        
        mTreeViewer = mFilteredTree.getViewer();
        Tree tree = mTreeViewer.getTree();
        
        tree.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {
                handleDoubleClick();
            }

            public void widgetSelected(SelectionEvent e) {
                handleSelection();
            }
        });
        
        mTreeViewer.setLabelProvider(new ResourceLabelProvider());
        mTreeViewer.setContentProvider(new ResourceContentProvider(false /* fullLevels */));
        mTreeViewer.setInput(mResources);
    }

    protected void handleSelection() {
        validateCurrentSelection();
        updateNewResButton();
    }

    protected void handleDoubleClick() {
        if (validateCurrentSelection()) {
            buttonPressed(IDialogConstants.OK_ID);
        }
    }
    
    /**
     * Returns the selected item in the tree as a {@link TreePath} object.
     * @return the <code>TreePath</code> object or <code>null</code> if there was no selection.
     */
    private TreePath getSelection() {
        ISelection selection = mFilteredTree.getViewer().getSelection();
        if (selection instanceof TreeSelection) {
            TreeSelection treeSelection = (TreeSelection)selection;
            TreePath[] treePaths = treeSelection.getPaths();
            
            // the selection mode is SWT.SINGLE, so we just get the first one.
            if (treePaths.length > 0) {
                return treePaths[0];
            }
        }
        
        return null;
    }
    
    private boolean validateCurrentSelection() {
        TreePath treeSelection = getSelection();
        
        IStatus status;
        if (treeSelection != null) {
            if (treeSelection.getSegmentCount() == 2) {
                status = new Status(IStatus.OK, AdtPlugin.PLUGIN_ID,
                        IStatus.OK, "", //$NON-NLS-1$
                        null);
            } else {
                status = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                        IStatus.ERROR, "You must select a Resource Item",
                        null);
            }
        } else {
            status = new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                    IStatus.ERROR, "", //$NON-NLS-1$
                    null);
        }
        
        updateStatus(status);

        return status.isOK();
    }

    /**
     * Updates the new res button when the list selection changes.
     * The name of the button changes depending on the resource.
     */
    private void updateNewResButton() {
        ResourceType type = getSelectedResourceType();
        
        // We only support adding new strings right now
        mNewResButton.setEnabled(type == ResourceType.STRING);
        
        String title = String.format("New %1$s...",
                type == null ? "Resource" : type.getDisplayName());
        mNewResButton.setText(title);
        mNewResButton.pack();
    }

    /**
     * Callback invoked when the mNewResButton is selected by the user.
     */
    private class OnNewResButtonSelected extends SelectionAdapter {
        @Override
         public void widgetSelected(SelectionEvent e) {
             super.widgetSelected(e);
             
             ResourceType type = getSelectedResourceType();

             // We currently only support strings
             if (type == ResourceType.STRING) {

                 ExtractStringRefactoring ref = new ExtractStringRefactoring(
                         mProject, true /*enforceNew*/);
                 RefactoringWizard wizard = new ExtractStringWizard(ref, mProject);
                 RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
                 try {
                     IWorkbench w = PlatformUI.getWorkbench();
                     if (op.run(w.getDisplay().getActiveShell(), wizard.getDefaultPageTitle()) ==
                             IDialogConstants.OK_ID) {
                         mTreeViewer.refresh();
                         
                         // select it if possible
                         setupInitialSelection(type, ref.getXmlStringId());
                     }
                 } catch (InterruptedException ex) {
                     // Interrupted. Pass.
                 }
             }
         } 
     }

    /**
     * Returns the {@link ResourceType} of the selected element, if any.
     * Returns null if nothing suitable is selected.
     */
    private ResourceType getSelectedResourceType() {
        ResourceType type = null;

        TreePath selection = getSelection();
        if (selection != null && selection.getSegmentCount() > 0) {
            Object first = selection.getFirstSegment();
            if (first instanceof ResourceType) {
                type = (ResourceType) first;
            }
        }
        return type;
    }
    
    /**
     * Sets up the initial selection.
     * <p/>
     * This parses {@link #mCurrentResource} to find out the resource type and the resource name.
     */
    private void setupInitialSelection() {
        // checks the inline id pattern first as it's more restrictive than the other one.
        Matcher m = sInlineIdResourcePattern.matcher(mCurrentResource);
        if (m.matches()) {
            // get the matching name
            String resourceName = m.group(1);

            // setup initial selection
            setupInitialSelection(ResourceType.ID, resourceName);
        } else {
            // attempts the inline id pattern
            m = sResourcePattern.matcher(mCurrentResource);
            if (m.matches()) {
                // get the resource type.
                ResourceType resourceType = ResourceType.getEnum(m.group(1));
                if (resourceType != null) {
                    // get the matching name
                    String resourceName = m.group(2);
                    
                    // setup initial selection
                    setupInitialSelection(resourceType, resourceName);
                }
            }
        }
    }
    
    /**
     * Sets up the initial selection based on a {@link ResourceType} and a resource name.
     * @param resourceType the resource type.
     * @param resourceName the resource name.
     */
    private void setupInitialSelection(ResourceType resourceType, String resourceName) {
        // get all the resources of this type
        ResourceItem[] resourceItems = mResources.getResources(resourceType);
        
        for (ResourceItem resourceItem : resourceItems) {
            if (resourceName.equals(resourceItem.getName())) {
                // name of the resource match, we select it,
                TreePath treePath = new TreePath(new Object[] { resourceType, resourceItem });
                mFilteredTree.getViewer().setSelection(
                        new TreeSelection(treePath),
                        true /*reveal*/);
                
                // and we're done.
                return;
            }
        }
        
        // if we get here, the resource type is valid, but the resource is missing.
        // we select and expand the resource type element.
        TreePath treePath = new TreePath(new Object[] { resourceType });
        mFilteredTree.getViewer().setSelection(
                new TreeSelection(treePath),
                true /*reveal*/);
        mFilteredTree.getViewer().setExpandedState(resourceType, true /* expanded */);
    }
}
