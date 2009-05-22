/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.sdkuilib.internal.repository;


import com.android.sdklib.internal.repository.IDescription;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

/*
 * TODO list
 * - check source => toggle packages: all, none
 * - check package => set source check to tri-state
 * - check callback => install enable if has selection
 * - fill columns (or remove them?)
 * - select tree item: delete site enable if add-on source
 * - select tree item: refresh enable if source
 * - load add-on sites from pref
 * - delete site callback, update pref
 * - refresh callback
 * - install selected callback
 */

public class AvailablePackagesPage extends Composite {

    private final UpdaterData mUpdaterData;

    private CheckboxTreeViewer mTreeViewerSources;
    private Tree mTreeSources;
    private TreeColumn mColumnAvailSummary;
    private TreeColumn mColumnAvailApiLevel;
    private TreeColumn mColumnAvailRevision;
    private TreeColumn mColumnAvailOs;
    private TreeColumn mColumnAvailInstalled;
    private Group mDescriptionContainer;
    private Button mAddSiteButton;
    private Button mRemoveSiteButton;
    private Label mPlaceholder3;
    private Button mRefreshButton;
    private Button mInstallSelectedButton;
    private Label mDescriptionLabel;


    /**
     * Create the composite.
     * @param parent The parent of the composite.
     * @param updaterData An instance of {@link UpdaterData}. If null, a local
     *        one will be allocated just to help with the SWT Designer.
     */
    public AvailablePackagesPage(Composite parent, UpdaterData updaterData) {
        super(parent, SWT.BORDER);

        mUpdaterData = updaterData != null ? updaterData : new UpdaterData();

        createContents(this);
    }

    private void createContents(Composite parent) {
        parent.setLayout(new GridLayout(5, false));

        mTreeViewerSources = new CheckboxTreeViewer(parent, SWT.BORDER);
        mTreeViewerSources.setContentProvider(mUpdaterData.getSources().getContentProvider());
        mTreeViewerSources.setLabelProvider(mUpdaterData.getSources().getLabelProvider());
        mTreeSources = mTreeViewerSources.getTree();
        mTreeSources.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onTreeSelected(); //$hide$
            }
        });
        mTreeSources.setHeaderVisible(true);
        mTreeSources.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 5, 1));

        mColumnAvailSummary = new TreeColumn(mTreeSources, SWT.NONE);
        mColumnAvailSummary.setWidth(289);
        mColumnAvailSummary.setText("Summary");

        mColumnAvailApiLevel = new TreeColumn(mTreeSources, SWT.NONE);
        mColumnAvailApiLevel.setWidth(66);
        mColumnAvailApiLevel.setText("API Level");

        mColumnAvailRevision = new TreeColumn(mTreeSources, SWT.NONE);
        mColumnAvailRevision.setWidth(63);
        mColumnAvailRevision.setText("Revision");

        mColumnAvailOs = new TreeColumn(mTreeSources, SWT.NONE);
        mColumnAvailOs.setWidth(100);
        mColumnAvailOs.setText("OS/Arch");

        mColumnAvailInstalled = new TreeColumn(mTreeSources, SWT.NONE);
        mColumnAvailInstalled.setWidth(59);
        mColumnAvailInstalled.setText("Installed");

        mDescriptionContainer = new Group(parent, SWT.NONE);
        mDescriptionContainer.setLayout(new GridLayout(1, false));
        mDescriptionContainer.setText("Description");
        mDescriptionContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 5, 1));

        mDescriptionLabel = new Label(mDescriptionContainer, SWT.NONE);
        mDescriptionLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        mDescriptionLabel.setText("Line1\nLine2\nLine3");

        mAddSiteButton = new Button(parent, SWT.NONE);
        mAddSiteButton.setText("Add Site...");

        mRemoveSiteButton = new Button(parent, SWT.NONE);
        mRemoveSiteButton.setText("Delete Site...");

        mPlaceholder3 = new Label(parent, SWT.NONE);
        mPlaceholder3.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1));

        mRefreshButton = new Button(parent, SWT.NONE);
        mRefreshButton.setText("Refresh");

        mInstallSelectedButton = new Button(parent, SWT.NONE);
        mInstallSelectedButton.setText("Install Selected");
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }

    // -- Start of internal part ----------
    // Hide everything down-below from SWT designer
    //$hide>>$

    public void setInput(RepoSources sources) {
        mTreeViewerSources.setInput(sources);
        onTreeSelected();
    }

    private void onTreeSelected() {
        ISelection sel = mTreeViewerSources.getSelection();
        if (sel instanceof ITreeSelection) {
            Object elem = ((ITreeSelection) sel).getFirstElement();
            if (elem instanceof IDescription) {
                mDescriptionLabel.setText(((IDescription) elem).getLongDescription());
                mDescriptionContainer.layout(true);
                return;
            }
        }
        mDescriptionLabel.setText("");  //$NON-NLS1-$
    }


    // End of hiding from SWT Designer
    //$hide<<$

}
