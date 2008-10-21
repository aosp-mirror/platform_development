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

package com.android.ide.eclipse.editors.wizards;

import com.android.ide.eclipse.common.resources.IResourceRepository;
import com.android.ide.eclipse.common.resources.ResourceItem;
import com.android.ide.eclipse.common.resources.ResourceType;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
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

    // TODO: enable when we can display the system resources.
    // private Pattern mSystemResourcePattern;
    // private IResourceRepository mSystemResources;
    // private Button mProjectButton;
    // private Button mSystemButton;
    
    private String mCurrentResource;
    
    /**
     * Creates a Resource Chooser dialog.
     * @param type The type of the resource to choose
     * @param project The repository for the project
     * @param system The System resource repository
     * @param parent the parent shell
     */
    public ResourceChooser(ResourceType type, IResourceRepository project,
            IResourceRepository system, Shell parent) {
        super(parent, new ResourceLabelProvider());

        mResourceType = type;
        mProjectResources = project;
        // TODO: enable when we can display the system resources.
        // mSystemResources = system;
        
        mProjectResourcePattern = Pattern.compile(
                "@" + mResourceType.getName() + "/(.+)"); //$NON-NLS-1$ //$NON-NLS-2$
        // TODO: enable when we can display the system resources.
        // mSystemResourcePattern = Pattern.compile(
        //        "@android:" + mResourceType.getName() + "/(.+)"); //$NON-NLS-1$ //$NON-NLS-2$

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
                    // TODO: enable when we can display the system resources.
                    false /*mSystemButton.getSelection()*/); 
        }
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite top = (Composite)super.createDialogArea(parent);

        createMessageArea(top);

        // TODO: enable when we can display the system resources.
        // createButtons(top);
        
        createFilterText(top);
        createFilteredList(top);
        
        setupResourceListAndCurrent();
        
        return top;
    }

    /**
     * Creates the radio button to switch between project and system resources.
     * @param top the parent composite
     */
    /* TODO: enable when we can display the system resources.
    private void createButtons(Composite top) {
        mProjectButton = new Button(top, SWT.RADIO);
        mProjectButton.setText("Project Resources");
        mProjectButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                setListElements(mProjectResources.getResources(mResourceType));
            }
        });
        mSystemButton = new Button(top, SWT.RADIO);
        mSystemButton.setText("System Resources");
        mSystemButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                setListElements(mSystemResources.getResources(mResourceType));
            }
        });
    }
    */
    
    /**
     * Setups the current list based on the current resource.
     */
    private void setupResourceListAndCurrent() {
        if (setupInitialSelection(mProjectResourcePattern, mProjectResources) == false) {
            // if we couldn't understand the current value, we default to the project resources
            ResourceItem[] items = mProjectResources.getResources(mResourceType); 
            setListElements(items);
        }
        /*
         * TODO: enable when we can display the system resources.
        if (setupInitialSelection(mProjectResourcePattern, mProjectResources) == false) {
            if (setupInitialSelection(mSystemResourcePattern, mSystemResources) == false) {
                // if we couldn't understand the current value, we default to the project resources
                IResourceItem[] items = mProjectResources.getResources(mResourceType); 
                setListElements(items);
                mProjectButton.setSelection(true);
            } else {
                mSystemButton.setSelection(true);
            }
        } else {
            mProjectButton.setSelection(true);
        }*/
    }
    
    /**
     * Attempts to setup the list of element from a repository if the current resource
     * matches the provided pattern. 
     * @param pattern the pattern to test the current value
     * @param repository the repository to use if the pattern matches.
     * @return true if success.
     */
    private boolean setupInitialSelection(Pattern pattern, IResourceRepository repository) {
        Matcher m = pattern.matcher(mCurrentResource);
        if (m.matches()) {
            // we have a project resource, let's setup the list
            ResourceItem[] items = repository.getResources(mResourceType); 
            setListElements(items);
            
            // and let's look for the item we found
            String name = m.group(1);

            for (ResourceItem item : items) {
                if (name.equals(item.getName())) {
                    setSelection(new Object[] { item });
                    break;
                }
            }
            return true;
        }
        return false;
    }
}
