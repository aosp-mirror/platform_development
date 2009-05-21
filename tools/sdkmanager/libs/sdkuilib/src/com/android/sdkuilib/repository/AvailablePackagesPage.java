package com.android.sdkuilib.repository;

import com.android.sdkuilib.repository.UpdaterWindow.UpdaterData;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

class AvailablePackagesPage extends Composite {

    private final UpdaterData mUpdaterData;

    private CheckboxTreeViewer mTreeViewAvailPkg;
    private Tree mTreeAvailPkg;
    private TreeColumn mColumnAvailSummary;
    private TreeColumn mColumnAvailApiLevel;
    private TreeColumn mColumnAvailRevision;
    private TreeColumn mColumnAvailOs;
    private TreeColumn mColumnAvailInstalled;
    private Group mAvailDescription;
    private Button mAvailAddSite;
    private Button mAvailRemoveSite;
    private Label mPlaceholder3;
    private Button mAvailRefresh;
    private Button mAvailInstallSelected;


    /**
     * Create the composite.
     * @param parent The parent of the composite.
     * @param updaterData An instance of {@link UpdaterWindow.UpdaterData}. If null, a local
     *        one will be allocated just to help with the SWT Designer.
     */
    public AvailablePackagesPage(Composite parent, UpdaterData updaterData) {
        super(parent, SWT.BORDER);

        mUpdaterData = updaterData != null ? updaterData : new UpdaterWindow.UpdaterData();

        createContents(this);
    }

    private void createContents(Composite parent) {
        parent.setLayout(new GridLayout(5, false));

        mTreeViewAvailPkg = new CheckboxTreeViewer(parent, SWT.BORDER);
        mTreeViewAvailPkg.setContentProvider(mUpdaterData.getSources().getContentProvider());
        mTreeViewAvailPkg.setLabelProvider(mUpdaterData.getSources().getLabelProvider());
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

        mAvailDescription = new Group(parent, SWT.NONE);
        mAvailDescription.setText("Description");
        mAvailDescription.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 5, 1));

        mAvailAddSite = new Button(parent, SWT.NONE);
        mAvailAddSite.setText("Add Site...");

        mAvailRemoveSite = new Button(parent, SWT.NONE);
        mAvailRemoveSite.setText("Delete Site...");

        mPlaceholder3 = new Label(parent, SWT.NONE);
        mPlaceholder3.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1));

        mAvailRefresh = new Button(parent, SWT.NONE);
        mAvailRefresh.setText("Refresh");

        mAvailInstallSelected = new Button(parent, SWT.NONE);
        mAvailInstallSelected.setText("Install Selected");
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }

    // -- Start of internal part ----------
    // Hide everything down-below from SWT designer
    //$hide>>$

    public void setInput(RepoSources sources) {
        mTreeViewAvailPkg.setInput(sources);
    }


    // End of hiding from SWT Designer
    //$hide<<$

}
