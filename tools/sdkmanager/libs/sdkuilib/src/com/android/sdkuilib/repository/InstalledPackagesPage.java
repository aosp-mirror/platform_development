package com.android.sdkuilib.repository;

import com.android.sdkuilib.repository.ProgressTask.ThreadTask;
import com.android.sdkuilib.repository.UpdaterWindow.UpdaterData;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

public class InstalledPackagesPage extends Composite {
    private UpdaterData mUpdaterData;

    private Label mSdkLocLabel;
    private Text mSdkLocText;
    private Button mSdkLocBrowse;
    private Label mInstalledPkgLabel;
    private TableViewer mTableViewerInstPkg;
    private Table mTableInstPkg;
    private TableColumn mColumnInstSummary;
    private TableColumn mColumnInstApiLevel;
    private TableColumn mColumnInstRevision;
    private Group mInstDescription;
    private Composite mInstButtons;
    private Button mInstUpdate;
    private Label mPlaceholder1;
    private Button mInstDelete;
    private Label mPlaceholder2;
    private Button mInstHomePage;

    /**
     * Create the composite.
     * @param parent The parent of the composite.
     * @param updaterData An instance of {@link UpdaterWindow.UpdaterData}. If null, a local
     *        one will be allocated just to help with the SWT Designer.
     */
    public InstalledPackagesPage(Composite parent, UpdaterData updaterData) {
        super(parent, SWT.BORDER);

        mUpdaterData = updaterData != null ? updaterData : new UpdaterWindow.UpdaterData();

        createContents(this);
    }

    private void createContents(Composite parent) {
        parent.setLayout(new GridLayout(3, false));

        createSdkLocation(parent);

        mInstalledPkgLabel = new Label(parent, SWT.NONE);
        mInstalledPkgLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
        mInstalledPkgLabel.setText("Installed Packages:");

        mTableViewerInstPkg = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);
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

        mInstDescription = new Group(parent, SWT.NONE);
        mInstDescription.setText("Description");
        mInstDescription.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));

        mInstButtons = new Composite(parent, SWT.NONE);
        mInstButtons.setLayout(new GridLayout(5, false));
        mInstButtons.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

        mInstUpdate = new Button(mInstButtons, SWT.NONE);
        mInstUpdate.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onUpdateInstalledPackage();  //$hide$ (hide from SWT designer)
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

    private void createSdkLocation(Composite parent) {
        mSdkLocLabel = new Label(parent, SWT.NONE);
        mSdkLocLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mSdkLocLabel.setText("SDK Location:");

        // If the sdk path is not user-customizable, do not create
        // the browse button and use horizSpan=2 on the text field.

        mSdkLocText = new Text(parent, SWT.BORDER);
        mSdkLocText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        if (mUpdaterData.canUserChangeSdkRoot()) {
            mSdkLocBrowse = new Button(parent, SWT.NONE);
            mSdkLocBrowse.setText("Browse...");
        } else {
            mSdkLocText.setEditable(false);
            ((GridData)mSdkLocText.getLayoutData()).horizontalSpan++;
        }

        if (mUpdaterData.getOsSdkRoot() != null) {
            mSdkLocText.setText(mUpdaterData.getOsSdkRoot());
        }
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }

    // -- Start of internal part ----------
    // Hide everything down-below from SWT designer
    //$hide>>$

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


    // End of hiding from SWT Designer
    //$hide<<$
}
