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

import com.android.sdklib.AndroidVersion;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.internal.repository.Archive;
import com.android.sdklib.internal.repository.IPackageVersion;
import com.android.sdklib.internal.repository.Package;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;
import com.android.sdkuilib.ui.GridDialog;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import java.util.ArrayList;


/**
 * Implements an {@link UpdateChooserDialog}.
 */
final class UpdateChooserDialog extends GridDialog {

    /** Last dialog size for this session. */
    private static Point sLastSize;
    private boolean mLicenseAcceptAll;
    private boolean mInternalLicenseRadioUpdate;

    // UI fields
    private SashForm mSashForm;
    private Composite mPackageRootComposite;
    private TableViewer mTableViewPackage;
    private Table mTablePackage;
    private TableColumn mTableColum;
    private StyledText mPackageText;
    private Button mLicenseRadioAccept;
    private Button mLicenseRadioReject;
    private Button mLicenseRadioAcceptAll;
    private Group mPackageTextGroup;
    private final UpdaterData mUpdaterData;
    private Group mTableGroup;
    private Label mErrorLabel;

    /**
     * List of all archives to be installed with dependency information.
     *
     * Note: in a lot of cases, we need to find the archive info for a given archive. This
     * is currently done using a simple linear search, which is fine since we only have a very
     * limited number of archives to deal with (e.g. < 10 now). We might want to revisit
     * this later if it becomes an issue. Right now just do the simple thing.
     *
     * Typically we could add a map Archive=>ArchiveInfo later.
     */
    private final ArrayList<ArchiveInfo> mArchives;



    /**
     * Create the dialog.
     * @param parentShell The shell to use, typically updaterData.getWindowShell()
     * @param updaterData The updater data
     * @param archives The archives to be installed
     */
    public UpdateChooserDialog(Shell parentShell,
            UpdaterData updaterData,
            ArrayList<ArchiveInfo> archives) {
        super(parentShell, 3, false/*makeColumnsEqual*/);
        mUpdaterData = updaterData;
        mArchives = archives;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    /**
     * Returns the results, i.e. the list of selected new archives to install.
     * This is similar to the {@link ArchiveInfo} list instance given to the constructor
     * except only accepted archives are present.
     *
     * An empty list is returned if cancel was choosen.
     */
    public ArrayList<ArchiveInfo> getResult() {
        ArrayList<ArchiveInfo> ais = new ArrayList<ArchiveInfo>();

        if (getReturnCode() == Window.OK) {
            for (ArchiveInfo ai : mArchives) {
                if (ai.isAccepted()) {
                    ais.add(ai);
                }
            }
        }

        return ais;
    }

    /**
     * Create the main content of the dialog.
     * See also {@link #createButtonBar(Composite)} below.
     */
    @Override
    public void createDialogContent(Composite parent) {
        // Sash form
        mSashForm = new SashForm(parent, SWT.NONE);
        mSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));


        // Left part of Sash Form

        mTableGroup = new Group(mSashForm, SWT.NONE);
        mTableGroup.setText("Packages");
        mTableGroup.setLayout(new GridLayout(1, false/*makeColumnsEqual*/));

        mTableViewPackage = new TableViewer(mTableGroup, SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE);
        mTablePackage = mTableViewPackage.getTable();
        mTablePackage.setHeaderVisible(false);
        mTablePackage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        mTablePackage.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onPackageSelected();  //$hide$
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                onPackageDoubleClick();
            }
        });

        mTableColum = new TableColumn(mTablePackage, SWT.NONE);
        mTableColum.setWidth(100);
        mTableColum.setText("Packages");


        // Right part of Sash form
        mPackageRootComposite = new Composite(mSashForm, SWT.NONE);
        mPackageRootComposite.setLayout(new GridLayout(4, false/*makeColumnsEqual*/));
        mPackageRootComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        mPackageTextGroup = new Group(mPackageRootComposite, SWT.NONE);
        mPackageTextGroup.setText("Package Description && License");
        mPackageTextGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
        mPackageTextGroup.setLayout(new GridLayout(1, false/*makeColumnsEqual*/));

        mPackageText = new StyledText(mPackageTextGroup,                        SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
        mPackageText.setBackground(
                getParentShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        mPackageText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        mLicenseRadioAccept = new Button(mPackageRootComposite, SWT.RADIO);
        mLicenseRadioAccept.setText("Accept");
        mLicenseRadioAccept.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onLicenseRadioSelected();
            }
        });

        mLicenseRadioReject = new Button(mPackageRootComposite, SWT.RADIO);
        mLicenseRadioReject.setText("Reject");
        mLicenseRadioReject.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onLicenseRadioSelected();
            }
        });

        Label placeholder = new Label(mPackageRootComposite, SWT.NONE);
        placeholder.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));

        mLicenseRadioAcceptAll = new Button(mPackageRootComposite, SWT.RADIO);
        mLicenseRadioAcceptAll.setText("Accept All");
        mLicenseRadioAcceptAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onLicenseRadioSelected();
            }
        });

        mSashForm.setWeights(new int[] {200, 300});
    }

    /**
     * Creates and returns the contents of this dialog's button bar.
     * <p/>
     * This reimplements most of the code from the base class with a few exceptions:
     * <ul>
     * <li>Enforces 3 columns.
     * <li>Inserts a full-width error label.
     * <li>Inserts a help label on the left of the first button.
     * <li>Renames the OK button into "Install"
     * </ul>
     */
    @Override
    protected Control createButtonBar(Composite parent) {

        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 0; // this is incremented by createButton
        layout.makeColumnsEqualWidth = false;
        layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        composite.setLayout(layout);
        GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
        composite.setLayoutData(data);
        composite.setFont(parent.getFont());

        // Error message area
        mErrorLabel = new Label(composite, SWT.NONE);
        mErrorLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

        // Label at the left of the install/cancel buttons
        Label label = new Label(composite, SWT.NONE);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        label.setText("[*] Something depends on this package");
        label.setEnabled(false);
        layout.numColumns++;

        // Add the ok/cancel to the button bar.
        createButtonsForButtonBar(composite);

        // the ok button should be an "install" button
        Button button = getButton(IDialogConstants.OK_ID);
        button.setText("Install");

        return composite;
    }

    // -- End of UI, Start of internal logic ----------
    // Hide everything down-below from SWT designer
    //$hide>>$

    @Override
    public void create() {
        super.create();

        // set window title
        getShell().setText("Choose Packages to Install");

        setWindowImage();

        // Automatically accept those with an empty license or no license
        for (ArchiveInfo ai : mArchives) {
            Archive a = ai.getNewArchive();
            assert a != null;

            String license = a.getParentPackage().getLicense();
            ai.setAccepted(license == null || license.trim().length() == 0);
        }

        // Fill the list with the replacement packages
        mTableViewPackage.setLabelProvider(new NewArchivesLabelProvider());
        mTableViewPackage.setContentProvider(new NewArchivesContentProvider());
        mTableViewPackage.setInput(mArchives);

        adjustColumnsWidth();

        // select first item
        mTablePackage.select(0);
        onPackageSelected();
    }

    /**
     * Creates the icon of the window shell.
     */
    private void setWindowImage() {
        String imageName = "android_icon_16.png"; //$NON-NLS-1$
        if (SdkConstants.currentPlatform() == SdkConstants.PLATFORM_DARWIN) {
            imageName = "android_icon_128.png"; //$NON-NLS-1$
        }

        if (mUpdaterData != null) {
            ImageFactory imgFactory = mUpdaterData.getImageFactory();
            if (imgFactory != null) {
                getShell().setImage(imgFactory.getImageByName(imageName));
            }
        }
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
                Rectangle r = mTablePackage.getClientArea();
                mTableColum.setWidth(r.width);
            }
        };
        mTablePackage.addControlListener(resizer);
        resizer.controlResized(null);
    }

    /**
     * Captures the window size before closing this.
     * @see #getInitialSize()
     */
    @Override
    public boolean close() {
        sLastSize = getShell().getSize();
        return super.close();
    }

    /**
     * Tries to reuse the last window size during this session.
     * <p/>
     * Note: the alternative would be to implement {@link #getDialogBoundsSettings()}
     * since the default {@link #getDialogBoundsStrategy()} is to persist both location
     * and size.
     */
    @Override
    protected Point getInitialSize() {
        if (sLastSize != null) {
            return sLastSize;
        } else {
            // Arbitrary values that look good on my screen and fit on 800x600
            return new Point(740, 370);
        }
    }

    /**
     * Callback invoked when a package item is selected in the list.
     */
    private void onPackageSelected() {
        ArchiveInfo ai = getSelectedArchive();
        displayInformation(ai);
        displayMissingDependency(ai);
        updateLicenceRadios(ai);
    }

    /** Returns the currently selected {@link ArchiveInfo} or null. */
    private ArchiveInfo getSelectedArchive() {
        ISelection sel = mTableViewPackage.getSelection();
        if (sel instanceof IStructuredSelection) {
            Object elem = ((IStructuredSelection) sel).getFirstElement();
            if (elem instanceof ArchiveInfo) {
                return (ArchiveInfo) elem;
            }
        }
        return null;
    }

    /**
     * Updates the package description and license text depending on the selected package.
     */
    private void displayInformation(ArchiveInfo ai) {
        if (ai == null) {
            mPackageText.setText("Please select a package.");
            return;
        }

        Archive aNew = ai.getNewArchive();
        Package pNew = aNew.getParentPackage();

        mPackageText.setText("");                                                //$NON-NLS-1$

        addSectionTitle("Package Description\n");
        addText(pNew.getLongDescription(), "\n\n");          //$NON-NLS-1$

        Archive aOld = ai.getReplaced();
        if (aOld != null) {
            Package pOld = aOld.getParentPackage();

            int rOld = pOld.getRevision();
            int rNew = pNew.getRevision();

            boolean showRev = true;

            if (pNew instanceof IPackageVersion && pOld instanceof IPackageVersion) {
                AndroidVersion vOld = ((IPackageVersion) pOld).getVersion();
                AndroidVersion vNew = ((IPackageVersion) pNew).getVersion();

                if (!vOld.equals(vNew)) {
                    // Versions are different, so indicate more than just the revision.
                    addText(String.format("This update will replace API %1$s revision %2$d with API %3$s revision %4$d.\n\n",
                            vOld.getApiString(), rOld,
                            vNew.getApiString(), rNew));
                    showRev = false;
                }
            }

            if (showRev) {
                addText(String.format("This update will replace revision %1$d with revision %2$d.\n\n",
                        rOld,
                        rNew));
            }
        }

        ArchiveInfo aDep = ai.getDependsOn();
        if (aDep != null || ai.isDependencyFor()) {
            addSectionTitle("Dependencies\n");

            if (aDep != null) {
                addText(String.format("This package depends on %1$s.\n\n",
                        aDep.getNewArchive().getParentPackage().getShortDescription()));
            }

            if (ai.isDependencyFor()) {
                addText("This package is a dependency for:");
                for (ArchiveInfo ai2 : ai.getDependenciesFor()) {
                    addText("\n- " +
                            ai2.getNewArchive().getParentPackage().getShortDescription());
                }
                addText("\n\n");
            }
        }

        addSectionTitle("Archive Description\n");
        addText(aNew.getLongDescription(), "\n\n");                             //$NON-NLS-1$

        String license = pNew.getLicense();
        if (license != null) {
            addSectionTitle("License\n");
            addText(license.trim(), "\n\n");                                       //$NON-NLS-1$
        }

        addSectionTitle("Site\n");
        addText(pNew.getParentSource().getShortDescription());
    }

    /**
     * Computes and display missing dependency.
     * If there's a selected package, check the dependency for that one.
     * Otherwise display the first missing dependency.
     */
    private void displayMissingDependency(ArchiveInfo ai) {
        String error = null;

        try {
            if (ai != null) {

                if (!ai.isAccepted()) {
                    // Case where this package blocks another one when not accepted
                    for (ArchiveInfo ai2 : ai.getDependenciesFor()) {
                        // It only matters if the blocked one is accepted
                        if (ai2.isAccepted()) {
                            error = String.format("Package '%1$s' depends on this one.",
                                    ai2.getNewArchive().getParentPackage().getShortDescription());
                            return;
                        }
                    }
                } else {
                    // Case where this package is accepted but blocked by another non-accepted one
                    ArchiveInfo adep = ai.getDependsOn();
                    if (adep != null && !adep.isAccepted()) {
                        error = String.format("This package depends on '%1$s'.",
                                adep.getNewArchive().getParentPackage().getShortDescription());
                        return;
                    }
                }
            }

            // If there's no selection, just find the first missing dependency of any accepted
            // package.
            for (ArchiveInfo ai2 : mArchives) {
                if (ai2.isAccepted()) {
                    ArchiveInfo adep = ai2.getDependsOn();
                    if (adep != null && !adep.isAccepted()) {
                        error = String.format("Package '%1$s' depends on '%2$s'",
                                ai2.getNewArchive().getParentPackage().getShortDescription(),
                                adep.getNewArchive().getParentPackage().getShortDescription());
                        return;
                    }
                }
            }
        } finally {
            mErrorLabel.setText(error == null ? "" : error);        //$NON-NLS-1$
        }
    }

    private void addText(String...string) {
        for (String s : string) {
            mPackageText.append(s);
        }
    }

    private void addSectionTitle(String string) {
        String s = mPackageText.getText();
        int start = (s == null ? 0 : s.length());
        mPackageText.append(string);

        StyleRange sr = new StyleRange();
        sr.start = start;
        sr.length = string.length();
        sr.fontStyle = SWT.BOLD;
        sr.underline = true;
        mPackageText.setStyleRange(sr);
    }

    private void updateLicenceRadios(ArchiveInfo ai) {
        if (mInternalLicenseRadioUpdate) {
            return;
        }
        mInternalLicenseRadioUpdate = true;

        boolean oneAccepted = false;

        if (mLicenseAcceptAll) {
            mLicenseRadioAcceptAll.setSelection(true);
            mLicenseRadioAccept.setEnabled(true);
            mLicenseRadioReject.setEnabled(true);
            mLicenseRadioAccept.setSelection(false);
            mLicenseRadioReject.setSelection(false);
        } else {
            mLicenseRadioAcceptAll.setSelection(false);
            oneAccepted = ai != null && ai.isAccepted();
            mLicenseRadioAccept.setEnabled(ai != null);
            mLicenseRadioReject.setEnabled(ai != null);
            mLicenseRadioAccept.setSelection(oneAccepted);
            mLicenseRadioReject.setSelection(ai != null && ai.isRejected());
        }

        // The install button is enabled if there's at least one package accepted.
        // If the current one isn't, look for another one.
        boolean missing = mErrorLabel.getText() != null && mErrorLabel.getText().length() > 0;
        if (!missing && !oneAccepted) {
            for(ArchiveInfo ai2 : mArchives) {
                if (ai2.isAccepted()) {
                    oneAccepted = true;
                    break;
                }
            }
        }

        getButton(IDialogConstants.OK_ID).setEnabled(!missing && oneAccepted);

        mInternalLicenseRadioUpdate = false;
    }

    /**
     * Callback invoked when one of the radio license buttons is selected.
     *
     * - accept/refuse: toggle, update item checkbox
     * - accept all: set accept-all, check all items
     */
    private void onLicenseRadioSelected() {
        if (mInternalLicenseRadioUpdate) {
            return;
        }
        mInternalLicenseRadioUpdate = true;

        ArchiveInfo ai = getSelectedArchive();
        boolean needUpdate = true;

        if (!mLicenseAcceptAll && mLicenseRadioAcceptAll.getSelection()) {
            // Accept all has been switched on. Mark all packages as accepted
            mLicenseAcceptAll = true;
            for(ArchiveInfo ai2 : mArchives) {
                ai2.setAccepted(true);
                ai2.setRejected(false);
            }

        } else if (mLicenseRadioAccept.getSelection()) {
            // Accept only this one
            mLicenseAcceptAll = false;
            ai.setAccepted(true);
            ai.setRejected(false);

        } else if (mLicenseRadioReject.getSelection()) {
            // Reject only this one
            mLicenseAcceptAll = false;
            ai.setAccepted(false);
            ai.setRejected(true);

        } else {
            needUpdate = false;
        }

        mInternalLicenseRadioUpdate = false;

        if (needUpdate) {
            if (mLicenseAcceptAll) {
                mTableViewPackage.refresh();
            } else {
               mTableViewPackage.refresh(ai);
            }
            displayMissingDependency(ai);
            updateLicenceRadios(ai);
        }
    }

    /**
     * Callback invoked when a package item is double-clicked in the list.
     */
    private void onPackageDoubleClick() {
        ArchiveInfo ai = getSelectedArchive();

        boolean wasAccepted = ai.isAccepted();
        ai.setAccepted(!wasAccepted);
        ai.setRejected(wasAccepted);

        // update state
        mLicenseAcceptAll = false;
        mTableViewPackage.refresh(ai);
        updateLicenceRadios(ai);
    }

    private class NewArchivesLabelProvider extends LabelProvider {
        @Override
        public Image getImage(Object element) {
            assert element instanceof ArchiveInfo;
            ArchiveInfo ai = (ArchiveInfo) element;

            ImageFactory imgFactory = mUpdaterData.getImageFactory();
            if (imgFactory != null) {
                if (ai.isAccepted()) {
                    return imgFactory.getImageByName("accept_icon16.png");
                } else if (ai.isRejected()) {
                    return imgFactory.getImageByName("reject_icon16.png");
                }
                return imgFactory.getImageByName("unknown_icon16.png");
            }
            return super.getImage(element);
        }

        @Override
        public String getText(Object element) {
            assert element instanceof ArchiveInfo;
            ArchiveInfo ai = (ArchiveInfo) element;

            String desc = ai.getNewArchive().getParentPackage().getShortDescription();

            if (ai.isDependencyFor()) {
                desc += " [*]";
            }

            return desc;
        }
    }

    private class NewArchivesContentProvider implements IStructuredContentProvider {

        public void dispose() {
            // pass
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // Ignore. The input is always mArchives
        }

        public Object[] getElements(Object inputElement) {
            return mArchives.toArray();
        }
    }

    // End of hiding from SWT Designer
    //$hide<<$
}
