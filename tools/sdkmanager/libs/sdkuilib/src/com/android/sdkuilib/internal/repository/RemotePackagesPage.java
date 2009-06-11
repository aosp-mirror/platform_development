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


import com.android.sdklib.internal.repository.Archive;
import com.android.sdklib.internal.repository.IDescription;
import com.android.sdklib.internal.repository.Package;
import com.android.sdklib.internal.repository.RepoSource;
import com.android.sdkuilib.internal.repository.UpdaterData.ISdkListener;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import java.util.ArrayList;

/*
 * TODO list
 * - check source => toggle packages: all, none
 * - check package => set source check to tri-state
 * - check callback => install enable if has selection
 * - select tree item: delete site enable if add-on source
 * - select tree item: refresh enable if source
 * - load add-on sites from pref
 * - delete site callback, update pref
 * - refresh callback
 * - install selected callback
 */

public class RemotePackagesPage extends Composite implements ISdkListener {

    private final UpdaterData mUpdaterData;

    private CheckboxTreeViewer mTreeViewerSources;
    private Tree mTreeSources;
    private TreeColumn mColumnSource;
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
    RemotePackagesPage(Composite parent,
            UpdaterData updaterData) {
        super(parent, SWT.BORDER);

        mUpdaterData = updaterData != null ? updaterData : new UpdaterData();
        mUpdaterData.addListeners(this);

        createContents(this);
        postCreate();  //$hide$
    }

    private void createContents(Composite parent) {
        parent.setLayout(new GridLayout(5, false));

        mTreeViewerSources = new CheckboxTreeViewer(parent, SWT.BORDER);
        mTreeViewerSources.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                onTreeDoubleClick(event); //$hide$
            }
        });
        mTreeViewerSources.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                onTreeCheckStateChanged(event); //$hide$
            }
        });
        mTreeSources = mTreeViewerSources.getTree();
        mTreeSources.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onTreeSelected(); //$hide$
            }
        });
        mTreeSources.setHeaderVisible(true);
        mTreeSources.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 5, 1));

        mColumnSource = new TreeColumn(mTreeSources, SWT.NONE);
        mColumnSource.setWidth(289);
        mColumnSource.setText("Sources, Packages and Archives");

        mDescriptionContainer = new Group(parent, SWT.NONE);
        mDescriptionContainer.setLayout(new GridLayout(1, false));
        mDescriptionContainer.setText("Description");
        mDescriptionContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 5, 1));

        mDescriptionLabel = new Label(mDescriptionContainer, SWT.NONE);
        mDescriptionLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        mDescriptionLabel.setText("Line1\nLine2\nLine3");

        mAddSiteButton = new Button(parent, SWT.NONE);
        mAddSiteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onAddSiteSelected(); //$hide$
            }
        });
        mAddSiteButton.setText("Add Site...");

        mRemoveSiteButton = new Button(parent, SWT.NONE);
        mRemoveSiteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onRemoveSiteSelected(); //$hide$
            }
        });
        mRemoveSiteButton.setText("Delete Site...");

        mPlaceholder3 = new Label(parent, SWT.NONE);
        mPlaceholder3.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1));

        mRefreshButton = new Button(parent, SWT.NONE);
        mRefreshButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onRefreshSelected(); //$hide$
            }
        });
        mRefreshButton.setText("Refresh");

        mInstallSelectedButton = new Button(parent, SWT.NONE);
        mInstallSelectedButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onInstallSelectedArchives();  //$hide$
            }
        });
        mInstallSelectedButton.setText("Install Selected");
    }

    @Override
    public void dispose() {
        mUpdaterData.removeListener(this);
        super.dispose();
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }

    // -- Start of internal part ----------
    // Hide everything down-below from SWT designer
    //$hide>>$

    /**
     * Called by the constructor right after {@link #createContents(Composite)}.
     */
    private void postCreate() {
        adjustColumnsWidth();
    }

    /**
     * Adds a listener to adjust the columns width when the parent is resized.
     * <p/>
     * If we need something more fancy, we might want to use this:
     * http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet77.java?view=co
     */
    private void adjustColumnsWidth() {
        // Add a listener to resize the column to the full width of the table
        mTreeSources.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Rectangle r = mTreeSources.getClientArea();
                mColumnSource.setWidth(r.width);
            }
        });
    }

    /**
     * Called when an item in the package table viewer is selected.
     * If the items is an {@link IDescription} (as it should), this will display its long
     * description in the description area. Otherwise when the item is not of the expected
     * type or there is no selection, it empties the description area.
     */
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

    /**
     * Handle checking and unchecking of the tree items.
     *
     * When unchecking, all sub-tree items checkboxes are cleared too.
     * When checking a source, all of its packages are checked too.
     * When checking a package, only its compatible archives are checked.
     */
    private void onTreeCheckStateChanged(CheckStateChangedEvent event) {
        boolean b = event.getChecked();
        Object elem = event.getElement(); // Will be Archive or Package or RepoSource

        assert event.getSource() == mTreeViewerSources;

        // when deselecting, we just deselect all children too
        if (b == false) {
            mTreeViewerSources.setSubtreeChecked(elem, b);
            return;
        }

        ITreeContentProvider provider =
            (ITreeContentProvider) mTreeViewerSources.getContentProvider();

        // When selecting, we want to only select compatible archives.
        if (elem instanceof RepoSource) {
            mTreeViewerSources.setExpandedState(elem, true);
            for (Object pkg : provider.getChildren(elem)) {
                mTreeViewerSources.setChecked(pkg, true);
                selectCompatibleArchives(pkg, provider);
            }
        } else if (elem instanceof Package) {
            selectCompatibleArchives(elem, provider);
        }
    }

    private void selectCompatibleArchives(Object pkg, ITreeContentProvider provider) {
        mTreeViewerSources.setExpandedState(pkg, true);
        for (Object archive : provider.getChildren(pkg)) {
            if (archive instanceof Archive) {
                if (((Archive) archive).isCompatible()) {
                    mTreeViewerSources.setChecked(archive, true);
                } else {
                    mTreeViewerSources.setChecked(archive, false);
                    // TODO change the item image to mark it incompatible
                }
            }
        }
    }

    private void onTreeDoubleClick(DoubleClickEvent event) {
        // TODO use or remove
    }

    private void onInstallSelectedArchives() {

        ArrayList<Archive> archives = new ArrayList<Archive>();
        for (Object element : mTreeViewerSources.getCheckedElements()) {
            if (element instanceof Archive) {
                archives.add((Archive) element);
            }
        }

        if (mUpdaterData != null) {
            mUpdaterData.installArchives(archives);
        }
    }

    private void onAddSiteSelected() {
        // TODO prompt for new addon site URL, store, refresh
    }

    private void onRemoveSiteSelected() {
        // TODO prompt for removing addon site URL, store, refresh
    }

    private void onRefreshSelected() {
        if (mUpdaterData != null) {
            mUpdaterData.refreshSources(false /*forceFetching*/, null /*monitor*/);
        }
    }

    public void onSdkChange() {
        RepoSourcesAdapter sources = mUpdaterData.getSourcesAdapter();
        mTreeViewerSources.setContentProvider(sources.getContentProvider());
        mTreeViewerSources.setLabelProvider(  sources.getLabelProvider());
        mTreeViewerSources.setInput(sources);
        onTreeSelected();
    }

    // End of hiding from SWT Designer
    //$hide<<$
}
