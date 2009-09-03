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
import com.android.sdkuilib.repository.UpdaterWindow.ISdkListener;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.window.Window;
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


public class RemotePackagesPage extends Composite implements ISdkListener {

    private final UpdaterData mUpdaterData;

    private CheckboxTreeViewer mTreeViewerSources;
    private Tree mTreeSources;
    private TreeColumn mColumnSource;
    private Button mUpdateOnlyCheckBox;
    private Group mDescriptionContainer;
    private Button mAddSiteButton;
    private Button mDeleteSiteButton;
    private Button mRefreshButton;
    private Button mInstallSelectedButton;
    private Label mDescriptionLabel;



    /**
     * Create the composite.
     * @param parent The parent of the composite.
     * @param updaterData An instance of {@link UpdaterData}.
     */
    RemotePackagesPage(Composite parent, UpdaterData updaterData) {
        super(parent, SWT.BORDER);

        mUpdaterData = updaterData;

        createContents(this);
        postCreate();  //$hide$
    }

    private void createContents(Composite parent) {
        parent.setLayout(new GridLayout(5, false));

        mTreeViewerSources = new CheckboxTreeViewer(parent, SWT.BORDER);
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
        mAddSiteButton.setText("Add Site...");
        mAddSiteButton.setToolTipText("Allows you to enter a new user external site. " +
                "Such site can only contribute add-ons and extra packages.");
        mAddSiteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onAddSiteSelected(); //$hide$
            }
        });

        mDeleteSiteButton = new Button(parent, SWT.NONE);
        mDeleteSiteButton.setText("Delete Site...");
        mDeleteSiteButton.setToolTipText("Allows you to remove an external site. " +
                "Built-in sites cannot be removed.");
        mDeleteSiteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onRemoveSiteSelected(); //$hide$
            }
        });

        mUpdateOnlyCheckBox = new Button(parent, SWT.CHECK);
        mUpdateOnlyCheckBox.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1));
        mUpdateOnlyCheckBox.setText("Display updates only");
        mUpdateOnlyCheckBox.setToolTipText("When selected, only compatible update packages are shown in the list above.");
        mUpdateOnlyCheckBox.setSelection(mUpdaterData.getSettingsController().getShowUpdateOnly());
        mUpdateOnlyCheckBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                onShowUpdateOnly(); //$hide$
            }
        });

        mRefreshButton = new Button(parent, SWT.NONE);
        mRefreshButton.setText("Refresh");
        mRefreshButton.setToolTipText("Refreshes the list of packages from open sites.");
        mRefreshButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onRefreshSelected(); //$hide$
            }
        });

        mInstallSelectedButton = new Button(parent, SWT.NONE);
        mInstallSelectedButton.setText("Install Selected");
        mInstallSelectedButton.setToolTipText("Allows you to review all selected packages " +
                "and install them.");
        mInstallSelectedButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onInstallSelectedArchives();  //$hide$
            }
        });
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
        mUpdaterData.addListeners(this);
        adjustColumnsWidth();
        updateButtonsState();
    }

    /**
     * Adds a listener to adjust the columns width when the parent is resized.
     * <p/>
     * If we need something more fancy, we might want to use this:
     * http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet77.java?view=co
     */
    private void adjustColumnsWidth() {
        // Add a listener to resize the column to the full width of the table
        ControlAdapter resizer = new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Rectangle r = mTreeSources.getClientArea();
                mColumnSource.setWidth(r.width);
            }
        };

        mTreeSources.addControlListener(resizer);
        resizer.controlResized(null);
    }

    /**
     * Called when an item in the package table viewer is selected.
     * If the items is an {@link IDescription} (as it should), this will display its long
     * description in the description area. Otherwise when the item is not of the expected
     * type or there is no selection, it empties the description area.
     */
    private void onTreeSelected() {
        updateButtonsState();

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
        updateButtonsState();

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
                mTreeViewerSources.setChecked(archive, ((Archive) archive).isCompatible());
            }
        }
    }

    private void onShowUpdateOnly() {
        SettingsController controller = mUpdaterData.getSettingsController();
        controller.setShowUpdateOnly(mUpdateOnlyCheckBox.getSelection());
        controller.saveSettings();
        mTreeViewerSources.refresh();
    }

    private void onInstallSelectedArchives() {
        ArrayList<Archive> archives = new ArrayList<Archive>();
        for (Object element : mTreeViewerSources.getCheckedElements()) {
            if (element instanceof Archive) {
                archives.add((Archive) element);
            }
        }

        if (mUpdaterData != null) {
            mUpdaterData.updateOrInstallAll(archives);
        }
    }

    private void onAddSiteSelected() {

        final RepoSource[] knowSources = mUpdaterData.getSources().getSources();
        String title = "Add Site URL";
        String msg = "Please enter the URL of the repository.xml for the new site:";

        InputDialog dlg = new InputDialog(getShell(), title, msg, null, new IInputValidator() {
            public String isValid(String newText) {

                if (newText == null || newText.length() == 0) {
                    return "Please enter an URL.";
                }

                // A URL should have one of the following prefixes
                if (!newText.startsWith("file://") &&
                        !newText.startsWith("ftp://") &&
                        !newText.startsWith("http://") &&
                        !newText.startsWith("https://")) {
                    return "The URL must start by one of file://, ftp://, http:// or https://";
                }

                // Reject URLs that are already in the source list
                for (RepoSource s : knowSources) {
                    if (newText.equals(s.getUrl())) {
                        return "This site is already listed.";
                    }
                }

                return null;
            }
        });

        if (dlg.open() == Window.OK) {
            String url = dlg.getValue();
            mUpdaterData.getSources().add(new RepoSource(url, true /*userSource*/));
            onRefreshSelected();
        }
    }

    private void onRemoveSiteSelected() {
        boolean changed = false;

        ISelection sel = mTreeViewerSources.getSelection();
        if (mUpdaterData != null && sel instanceof ITreeSelection) {
            for (Object c : ((ITreeSelection) sel).toList()) {
                if (c instanceof RepoSource && ((RepoSource) c).isUserSource()) {
                    RepoSource source = (RepoSource) c;

                    String title = "Delete Site?";

                    String msg = String.format("Are you sure you want to delete the site '%1$s'?",
                            source.getUrl());

                    if (MessageDialog.openQuestion(getShell(), title, msg)) {
                        mUpdaterData.getSources().remove(source);
                        changed = true;
                    }
                }
            }
        }

        if (changed) {
            onRefreshSelected();
        }
    }

    private void onRefreshSelected() {
        if (mUpdaterData != null) {
            mUpdaterData.refreshSources(false /*forceFetching*/);
        }
        mTreeViewerSources.refresh();
        updateButtonsState();
    }

    public void onSdkChange() {
        RepoSourcesAdapter sources = mUpdaterData.getSourcesAdapter();
        mTreeViewerSources.setContentProvider(sources.getContentProvider());
        mTreeViewerSources.setLabelProvider(  sources.getLabelProvider());
        mTreeViewerSources.setInput(sources);
        onTreeSelected();
    }

    private void updateButtonsState() {
        // We install archives, so there should be at least one checked archive.
        // Having sites or packages checked does not count.
        boolean hasCheckedArchive = false;
        Object[] checked = mTreeViewerSources.getCheckedElements();
        if (checked != null) {
            for (Object c : checked) {
                if (c instanceof Archive) {
                    hasCheckedArchive = true;
                    break;
                }
            }
        }

        // Is there a selected site Source?
        boolean hasSelectedUserSource = false;
        ISelection sel = mTreeViewerSources.getSelection();
        if (sel instanceof ITreeSelection) {
            for (Object c : ((ITreeSelection) sel).toList()) {
                if (c instanceof RepoSource &&
                        ((RepoSource) c).isUserSource()) {
                    hasSelectedUserSource = true;
                    break;
                }
            }
        }

        mAddSiteButton.setEnabled(true);
        mDeleteSiteButton.setEnabled(hasSelectedUserSource);
        mRefreshButton.setEnabled(true);
        mInstallSelectedButton.setEnabled(hasCheckedArchive);

        // set value on the show only update checkbox
        mUpdateOnlyCheckBox.setSelection(mUpdaterData.getSettingsController().getShowUpdateOnly());
    }

    // End of hiding from SWT Designer
    //$hide<<$
}
