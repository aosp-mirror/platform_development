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

package com.android.sdkuilib.repository;

import com.android.sdkuilib.repository.ProgressTask.ThreadTask;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableTree;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

public class UpdaterWindow {

    private final String mOsSdkRoot;
    private final boolean mUserCanChangeSdkRoot;

    private RepoSources mSources = new RepoSources();

    // --- UI members ---

    protected Shell mAndroidSdkUpdater;
    private TabFolder mTabFolder;
    private TabItem mTabInstalledPkg;
    private Composite mRootInst;
    private TabItem mTabAvailPkg;
    private Composite mRootAvail;
    private Text mSdkLocText;
    private Button mSdkLocBrowse;
    private Label mSdkLocLabel;
    private Group mInstDescription;
    private Composite mInstButtons;
    private Button mInstUpdate;
    private Button mInstDelete;
    private Button mInstHomePage;
    private Label mPlaceholder1;
    private Label mPlaceholder2;
    private Label mInstalledPkgLabel;
    private TableTree tableTree;
    private Tree mTreeAvailPkg;
    private Button mAvailRemoveSite;
    private Button mAvailAddSite;
    private Label mPlaceholder3;
    private Button mAvailRefresh;
    private Button mAvailInstallSelected;
    private Group mAvailDescription;
    private Table mTableInstPkg;
    private TableColumn mColumnInstSummary;
    private TableColumn mColumnInstApiLevel;
    private TableColumn mColumnInstRevision;
    private TreeColumn mColumnAvailSummary;
    private TreeColumn mColumnAvailApiLevel;
    private TreeColumn mColumnAvailRevision;
    private TreeColumn mColumnAvailOs;
    private TreeColumn mColumnAvailInstalled;
    private CheckboxTreeViewer mTreeViewAvailPkg;
    private TableViewer mTableViewerInstPkg;

    public UpdaterWindow(String osSdkRoot, boolean userCanChangeSdkRoot) {
        mOsSdkRoot = osSdkRoot;
        mUserCanChangeSdkRoot = userCanChangeSdkRoot;
    }

    /**
     * Open the window.
     * @wbp.parser.entryPoint
     */
    public void open() {
        Display display = Display.getDefault();
        createContents();
        mAndroidSdkUpdater.open();
        mAndroidSdkUpdater.layout();
        firstInit();
        while (!mAndroidSdkUpdater.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    /**
     * Create contents of the window.
     */
    protected void createContents() {
        mAndroidSdkUpdater = new Shell();
        mAndroidSdkUpdater.setMinimumSize(new Point(200, 50));
        mAndroidSdkUpdater.setLayout(new GridLayout(1, false));
        mAndroidSdkUpdater.setSize(633, 433);
        mAndroidSdkUpdater.setText("Android SDK Updater");

        mTabFolder = new TabFolder(mAndroidSdkUpdater, SWT.NONE);
        mTabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        createInstalledPackagesTab();
        createAvailablePackagesTab();
    }

    private void createInstalledPackagesTab() {
        mTabInstalledPkg = new TabItem(mTabFolder, SWT.NONE);
        mTabInstalledPkg.setText("Installed Packages");

        mRootInst = new Composite(mTabFolder, SWT.NONE);
        mRootInst.setLayout(new GridLayout(3, false));
        mTabInstalledPkg.setControl(mRootInst);

        createSdkLocation();

        mInstalledPkgLabel = new Label(mRootInst, SWT.NONE);
        mInstalledPkgLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
        mInstalledPkgLabel.setText("Installed Packages:");

        mTableViewerInstPkg = new TableViewer(mRootInst, SWT.BORDER | SWT.FULL_SELECTION);
        mTableInstPkg = mTableViewerInstPkg.getTable();
        mTableInstPkg.setHeaderVisible(true);
        mTableInstPkg.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));

        mColumnInstSummary = new TableColumn(mTableInstPkg, SWT.NONE);
        mColumnInstSummary.setWidth(377);
        mColumnInstSummary.setText("Summary");

        mColumnInstApiLevel = new TableColumn(mTableInstPkg, SWT.NONE);
        mColumnInstApiLevel.setWidth(100);
        mColumnInstApiLevel.setText("API Level");

        mColumnInstRevision = new TableColumn(mTableInstPkg, SWT.NONE);
        mColumnInstRevision.setWidth(100);
        mColumnInstRevision.setText("Revision");

        mInstDescription = new Group(mRootInst, SWT.NONE);
        mInstDescription.setText("Description");
        mInstDescription.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));

        mInstButtons = new Composite(mRootInst, SWT.NONE);
        mInstButtons.setLayout(new GridLayout(5, false));
        mInstButtons.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

        mInstUpdate = new Button(mInstButtons, SWT.NONE);
        mInstUpdate.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onUpdateInstalledPackage();
            }
        });
        mInstUpdate.setText("Update...");

        mPlaceholder1 = new Label(mInstButtons, SWT.NONE);
        mPlaceholder1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));

        mInstDelete = new Button(mInstButtons, SWT.NONE);
        mInstDelete.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
        mInstDelete.setText("Delete...");

        mPlaceholder2 = new Label(mInstButtons, SWT.NONE);
        mPlaceholder2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));

        mInstHomePage = new Button(mInstButtons, SWT.NONE);
        mInstHomePage.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mInstHomePage.setText("Home Page...");
    }

    private void createSdkLocation() {
        mSdkLocLabel = new Label(mRootInst, SWT.NONE);
        mSdkLocLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mSdkLocLabel.setText("SDK Location:");

        // If the sdk path is not user-customizable, do not create
        // the browse button and use horizSpan=2 on the text field.

        mSdkLocText = new Text(mRootInst, SWT.BORDER);
        mSdkLocText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        if (mUserCanChangeSdkRoot) {
            mSdkLocBrowse = new Button(mRootInst, SWT.NONE);
            mSdkLocBrowse.setText("Browse...");
        } else {
            mSdkLocText.setEditable(false);
            ((GridData)mSdkLocText.getLayoutData()).horizontalSpan++;
        }

        mSdkLocText.setText(mOsSdkRoot);
    }

    private void createAvailablePackagesTab() {
        mTabAvailPkg = new TabItem(mTabFolder, SWT.NONE);
        mTabAvailPkg.setText("Available Packages");

        mRootAvail = new Composite(mTabFolder, SWT.NONE);
        mRootAvail.setLayout(new GridLayout(5, false));
        mTabAvailPkg.setControl(mRootAvail);

        mTreeViewAvailPkg = new CheckboxTreeViewer(mRootAvail, SWT.BORDER);
        mTreeViewAvailPkg.setContentProvider(mSources.getContentProvider());
        mTreeViewAvailPkg.setLabelProvider(mSources.getLabelProvider());
        mTreeAvailPkg = mTreeViewAvailPkg.getTree();
        mTreeAvailPkg.setHeaderVisible(true);
        mTreeAvailPkg.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 5, 1));

        mColumnAvailSummary = new TreeColumn(mTreeAvailPkg, SWT.NONE);
        mColumnAvailSummary.setWidth(289);
        mColumnAvailSummary.setText("Summary");

        mColumnAvailApiLevel = new TreeColumn(mTreeAvailPkg, SWT.NONE);
        mColumnAvailApiLevel.setWidth(66);
        mColumnAvailApiLevel.setText("API Level");

        mColumnAvailRevision = new TreeColumn(mTreeAvailPkg, SWT.NONE);
        mColumnAvailRevision.setWidth(63);
        mColumnAvailRevision.setText("Revision");

        mColumnAvailOs = new TreeColumn(mTreeAvailPkg, SWT.NONE);
        mColumnAvailOs.setWidth(100);
        mColumnAvailOs.setText("OS/Arch");

        mColumnAvailInstalled = new TreeColumn(mTreeAvailPkg, SWT.NONE);
        mColumnAvailInstalled.setWidth(59);
        mColumnAvailInstalled.setText("Installed");

        mAvailDescription = new Group(mRootAvail, SWT.NONE);
        mAvailDescription.setText("Description");
        mAvailDescription.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 5, 1));

        mAvailAddSite = new Button(mRootAvail, SWT.NONE);
        mAvailAddSite.setText("Add Site...");

        mAvailRemoveSite = new Button(mRootAvail, SWT.NONE);
        mAvailRemoveSite.setText("Delete Site...");

        mPlaceholder3 = new Label(mRootAvail, SWT.NONE);
        mPlaceholder3.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1));

        mAvailRefresh = new Button(mRootAvail, SWT.NONE);
        mAvailRefresh.setText("Refresh");

        mAvailInstallSelected = new Button(mRootAvail, SWT.NONE);
        mAvailInstallSelected.setText("Install Selected");
    }

    // --- UI Callbacks -----------

    protected void onUpdateInstalledPackage() {
        ProgressTask.start(getShell(), "Test", new ThreadTask() {
            public void PerformTask(ITaskMonitor monitor) {
                monitor.setDescription("Test");
                monitor.setProgressMax(100);
                int n = 0;
                int d = 1;
                while(!monitor.cancelRequested()) {
                    monitor.incProgress(d);
                    n += d;
                    if (n == 0 || n == 100) d = -d;
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                    }
                }
            }
        });
    }


    // --------------

    private Shell getShell() {
        return mAndroidSdkUpdater;
    }

    /**
     * Once the UI has been created, initialize the content
     */
    private void firstInit() {

        setupSources();
        scanLocalSdkFolders();
    }

    private void setupSources() {
        mSources.setShell(getShell());

        mSources.add(new RepoSource(
                "https://dl.google.com/android/eclipse/repository/index.xml",          //$NON-NLS-1$
                false /* addonOnly */));
        mSources.add(new RepoSource(
                "http://www.corp.google.com/~raphael/android/sdk-repo/repository.xml", //$NON-NLS-1$
                false /* addonOnly */));

        mTreeViewAvailPkg.setInput(mSources);
    }

    private void scanLocalSdkFolders() {
        // TODO Auto-generated method stub

    }
}
