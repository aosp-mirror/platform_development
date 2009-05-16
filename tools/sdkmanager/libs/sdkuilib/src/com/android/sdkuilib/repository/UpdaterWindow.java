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

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableTree;
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

    protected Shell mshlAndroidSdkUpdater;
    private TabFolder tabFolder;
    private TabItem mtbtmInstalledPackages;
    private Composite compositeInst;
    private TabItem mtbtmAvailablePackages;
    private Composite compositeAvail;
    private Text text;
    private Button mbtnBrowse;
    private Label mlblSdkLocation;
    private Group mgrpDescription;
    private Composite composite_3;
    private Button mbtnUpdate;
    private Button mbtnDelete;
    private Button mbtnHomePage;
    private Label placeholder1;
    private Label placeholder2;
    private Label mlblInstalledPackages;
    private TableTree tableTree;
    private Tree tree;
    private Button mbtnRemoveSite;
    private Button mbtnAddSite;
    private Label placeholder3;
    private Button mbtnRefresh;
    private Button mbtnInstallSelected;
    private Group mgrpDescription_1;
    private Table table;
    private TableColumn mtblclmnSummary;
    private TableColumn mtblclmnApiLevel;
    private TableColumn mtblclmnRevision;
    private TreeColumn mtrclmnSummary;
    private TreeColumn mtrclmnApiLevel;
    private TreeColumn mtrclmnRevision;
    private TreeColumn mtrclmnOs;
    private TreeColumn mtrclmnInstalled;

    /**
     * Open the window.
     * @wbp.parser.entryPoint
     */
    public void open() {
        Display display = Display.getDefault();
        createContents();
        mshlAndroidSdkUpdater.open();
        mshlAndroidSdkUpdater.layout();
        while (!mshlAndroidSdkUpdater.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    /**
     * Create contents of the window.
     */
    protected void createContents() {
        mshlAndroidSdkUpdater = new Shell();
        mshlAndroidSdkUpdater.setMinimumSize(new Point(200, 50));
        mshlAndroidSdkUpdater.setLayout(new GridLayout(1, false));
        mshlAndroidSdkUpdater.setSize(633, 433);
        mshlAndroidSdkUpdater.setText("Android SDK Updater");

        tabFolder = new TabFolder(mshlAndroidSdkUpdater, SWT.NONE);
        tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        mtbtmInstalledPackages = new TabItem(tabFolder, SWT.NONE);
        mtbtmInstalledPackages.setText("Installed Packages");

        compositeInst = new Composite(tabFolder, SWT.NONE);
        compositeInst.setLayout(new GridLayout(3, false));
        mtbtmInstalledPackages.setControl(compositeInst);

        mlblSdkLocation = new Label(compositeInst, SWT.NONE);
        mlblSdkLocation.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mlblSdkLocation.setText("SDK Location:");

        text = new Text(compositeInst, SWT.BORDER);
        text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        mbtnBrowse = new Button(compositeInst, SWT.NONE);
        mbtnBrowse.setText("Browse...");

        mlblInstalledPackages = new Label(compositeInst, SWT.NONE);
        mlblInstalledPackages.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
        mlblInstalledPackages.setText("Installed Packages:");

        TableViewer tableInstalledPackage = new TableViewer(compositeInst, SWT.BORDER | SWT.FULL_SELECTION);
        table = tableInstalledPackage.getTable();
        table.setHeaderVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));

        mtblclmnSummary = new TableColumn(table, SWT.NONE);
        mtblclmnSummary.setWidth(377);
        mtblclmnSummary.setText("Summary");

        mtblclmnApiLevel = new TableColumn(table, SWT.NONE);
        mtblclmnApiLevel.setWidth(100);
        mtblclmnApiLevel.setText("API Level");

        mtblclmnRevision = new TableColumn(table, SWT.NONE);
        mtblclmnRevision.setWidth(100);
        mtblclmnRevision.setText("Revision");

        mgrpDescription = new Group(compositeInst, SWT.NONE);
        mgrpDescription.setText("Description");
        mgrpDescription.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));

        composite_3 = new Composite(compositeInst, SWT.NONE);
        composite_3.setLayout(new GridLayout(5, false));
        composite_3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

        mbtnUpdate = new Button(composite_3, SWT.NONE);
        mbtnUpdate.setText("Update...");

        placeholder1 = new Label(composite_3, SWT.NONE);
        placeholder1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));

        mbtnDelete = new Button(composite_3, SWT.NONE);
        mbtnDelete.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
        mbtnDelete.setText("Delete...");

        placeholder2 = new Label(composite_3, SWT.NONE);
        placeholder2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));

        mbtnHomePage = new Button(composite_3, SWT.NONE);
        mbtnHomePage.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mbtnHomePage.setText("Home Page...");

        mtbtmAvailablePackages = new TabItem(tabFolder, SWT.NONE);
        mtbtmAvailablePackages.setText("Available Packages");

        compositeAvail = new Composite(tabFolder, SWT.NONE);
        compositeAvail.setLayout(new GridLayout(5, false));
        mtbtmAvailablePackages.setControl(compositeAvail);

        CheckboxTreeViewer checkboxTreeAvailablePackages = new CheckboxTreeViewer(compositeAvail, SWT.BORDER);
        tree = checkboxTreeAvailablePackages.getTree();
        tree.setHeaderVisible(true);
        tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 5, 1));

        mtrclmnSummary = new TreeColumn(tree, SWT.NONE);
        mtrclmnSummary.setWidth(289);
        mtrclmnSummary.setText("Summary");

        mtrclmnApiLevel = new TreeColumn(tree, SWT.NONE);
        mtrclmnApiLevel.setWidth(66);
        mtrclmnApiLevel.setText("API Level");

        mtrclmnRevision = new TreeColumn(tree, SWT.NONE);
        mtrclmnRevision.setWidth(63);
        mtrclmnRevision.setText("Revision");

        mtrclmnOs = new TreeColumn(tree, SWT.NONE);
        mtrclmnOs.setWidth(100);
        mtrclmnOs.setText("OS/Arch");

        mtrclmnInstalled = new TreeColumn(tree, SWT.NONE);
        mtrclmnInstalled.setWidth(59);
        mtrclmnInstalled.setText("Installed");

        mgrpDescription_1 = new Group(compositeAvail, SWT.NONE);
        mgrpDescription_1.setText("Description");
        mgrpDescription_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 5, 1));

        mbtnAddSite = new Button(compositeAvail, SWT.NONE);
        mbtnAddSite.setText("Add Site...");

        mbtnRemoveSite = new Button(compositeAvail, SWT.NONE);
        mbtnRemoveSite.setText("Delete Site...");

        placeholder3 = new Label(compositeAvail, SWT.NONE);
        placeholder3.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1));

        mbtnRefresh = new Button(compositeAvail, SWT.NONE);
        mbtnRefresh.setText("Refresh");

        mbtnInstallSelected = new Button(compositeAvail, SWT.NONE);
        mbtnInstallSelected.setText("Install Selected");


    }
}
