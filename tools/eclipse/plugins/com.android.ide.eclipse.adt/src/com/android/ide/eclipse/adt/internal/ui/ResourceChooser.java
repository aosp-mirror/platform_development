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

import com.android.ide.eclipse.adt.internal.refactorings.extractstring.ExtractStringRefactoring;
import com.android.ide.eclipse.adt.internal.refactorings.extractstring.ExtractStringWizard;
import com.android.ide.eclipse.adt.internal.resources.IResourceRepository;
import com.android.ide.eclipse.adt.internal.resources.ResourceItem;
import com.android.ide.eclipse.adt.internal.resources.ResourceType;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.AbstractElementListSelectionDialog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A dialog to let the user select a resource based on a resource type. 
 */
public class ResourceChooser extends AbstractElementListSelectionDialog {

    private Pattern mProjectResourcePattern;

    private ResourceType mResourceType;

    private IResourceRepository mProjectResources;

    private final static boolean SHOW_SYSTEM_RESOURCE = false;  // TODO re-enable at some point 
    private Pattern mSystemResourcePattern;
    private IResourceRepository mSystemResources;
    private Button mProjectButton;
    private Button mSystemButton;
    
    private String mCurrentResource;

    private final IProject mProject;
    
    /**
     * Creates a Resource Chooser dialog.
     * @param project Project being worked on
     * @param type The type of the resource to choose
     * @param projectResources The repository for the project
     * @param systemResources The System resource repository
     * @param parent the parent shell
     */
    public ResourceChooser(IProject project, ResourceType type,
            IResourceRepository projectResources,
            IResourceRepository systemResources,
            Shell parent) {
        super(parent, new ResourceLabelProvider());
        mProject = project;

        mResourceType = type;
        mProjectResources = projectResources;
        
        mProjectResourcePattern = Pattern.compile(
                "@" + mResourceType.getName() + "/(.+)"); //$NON-NLS-1$ //$NON-NLS-2$

        if (SHOW_SYSTEM_RESOURCE) {
            mSystemResources = systemResources;
            mSystemResourcePattern = Pattern.compile(
                    "@android:" + mResourceType.getName() + "/(.+)"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        setTitle("Resource Chooser");
        setMessage(String.format("Choose a %1$s resource",
                mResourceType.getDisplayName().toLowerCase()));
    }
    
    public void setCurrentResource(String resource) {
        mCurrentResource = resource;
    }
    
    public String getCurrentResource() {
        return mCurrentResource;
    }

    @Override
    protected void computeResult() {
        Object[] elements = getSelectedElements();
        if (elements.length == 1 && elements[0] instanceof ResourceItem) {
            ResourceItem item = (ResourceItem)elements[0];
            
            mCurrentResource = mResourceType.getXmlString(item,
                    SHOW_SYSTEM_RESOURCE && mSystemButton.getSelection()); 
        }
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite top = (Composite)super.createDialogArea(parent);

        createMessageArea(top);

        createButtons(top);
        createFilterText(top);
        createFilteredList(top);
        
        // create the "New Resource" button
        createNewResButtons(top);

        setupResourceList();
        selectResourceString(mCurrentResource);
        
        return top;
    }

    /**
     * Creates the radio button to switch between project and system resources.
     * @param top the parent composite
     */
    private void createButtons(Composite top) {
        if (!SHOW_SYSTEM_RESOURCE) {
            return;
        }
        mProjectButton = new Button(top, SWT.RADIO);
        mProjectButton.setText("Project Resources");
        mProjectButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                if (mProjectButton.getSelection()) {
                    setListElements(mProjectResources.getResources(mResourceType));
                }
            }
        });
        mSystemButton = new Button(top, SWT.RADIO);
        mSystemButton.setText("System Resources");
        mSystemButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                if (mProjectButton.getSelection()) {
                    setListElements(mSystemResources.getResources(mResourceType));
                }
            }
        });
    }

    /**
     * Creates the "New Resource" button.
     * @param top the parent composite
     */
    private void createNewResButtons(Composite top) {
        
        Button newResButton = new Button(top, SWT.NONE);

        String title = String.format("New %1$s...", mResourceType.getDisplayName());
        newResButton.setText(title);

        // We only support adding new strings right now
        newResButton.setEnabled(mResourceType == ResourceType.STRING);

        newResButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                
                if (mResourceType == ResourceType.STRING) {
                    createNewString();
                }
            }
        });
    }
    
    private void createNewString() {
        ExtractStringRefactoring ref = new ExtractStringRefactoring(
                mProject, true /*enforceNew*/);
        RefactoringWizard wizard = new ExtractStringWizard(ref, mProject);
        RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
        try {
            IWorkbench w = PlatformUI.getWorkbench();
            if (op.run(w.getDisplay().getActiveShell(), wizard.getDefaultPageTitle()) ==
                    IDialogConstants.OK_ID) {

                // Recompute the "current resource" to select the new id
                setupResourceList();
                
                // select it if possible
                selectItemName(ref.getXmlStringId());
            }
        } catch (InterruptedException ex) {
            // Interrupted. Pass.
        }
    }

    /**
     * @return The repository currently selected.
     */
    private IResourceRepository getCurrentRepository() {
        IResourceRepository repo = mProjectResources;
        
        if (SHOW_SYSTEM_RESOURCE && mSystemButton.getSelection()) {
            repo = mSystemResources;
        }
        return repo;
    }

    /**
     * Setups the current list.
     */
    private void setupResourceList() {
        IResourceRepository repo = getCurrentRepository();
        setListElements(repo.getResources(mResourceType));
    }
    
    /**
     * Select an item by its name, if possible.
     */
    private void selectItemName(String itemName) {
        if (itemName == null) {
            return;
        }

        IResourceRepository repo = getCurrentRepository();

        ResourceItem[] items = repo.getResources(mResourceType); 
        
        for (ResourceItem item : items) {
            if (itemName.equals(item.getName())) {
                setSelection(new Object[] { item });
                break;
            }
        }
    }

    /**
     * Select an item by its full resource string.
     * This also selects between project and system repository based on the resource string.
     */
    private void selectResourceString(String resourceString) {
        boolean isSystem = false;
        String itemName = null;
        
        // Is this a system resource?
        // If not a system resource or if they are not available, this will be a project res.
        if (SHOW_SYSTEM_RESOURCE) {
            Matcher m = mSystemResourcePattern.matcher(resourceString);
            if (m.matches()) {
                itemName = m.group(1);
                isSystem = true;
            }
        }

        if (!isSystem && itemName == null) {
            // Try to match project resource name
            Matcher m = mProjectResourcePattern.matcher(resourceString);
            if (m.matches()) {
                itemName = m.group(1);
            }
        }
        
        // Update the repository selection
        if (SHOW_SYSTEM_RESOURCE) {
            mProjectButton.setSelection(!isSystem);
            mSystemButton.setSelection(isSystem);
        }

        // Update the list
        setupResourceList();
        
        // If we have a selection name, select it
        if (itemName != null) {
            selectItemName(itemName);
        }
    }
}
