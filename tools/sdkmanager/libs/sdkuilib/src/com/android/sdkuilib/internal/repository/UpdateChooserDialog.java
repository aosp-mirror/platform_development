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

import com.android.sdklib.SdkConstants;
import com.android.sdklib.internal.repository.Archive;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import java.util.ArrayList;


/**
 * Implements an {@link UpdateChooserDialog}.
 */
final class UpdateChooserDialog extends Dialog {

    /** Min Y location for dialog. Need to deal with the menu bar on mac os. */
    private final static int MIN_Y =
        SdkConstants.CURRENT_PLATFORM == SdkConstants.PLATFORM_DARWIN ? 20 : 0;

    /** Last dialog size for this session. */
    private static Point sLastSize;
    private boolean mCancelled = true;
    private boolean mCompleted;
    private boolean mLicenseAcceptAll;
    private boolean mInternalLicenseRadioUpdate;

    // UI fields
    private Shell mDialogShell;
    private SashForm mSashForm;
    private Composite mPackageRootComposite;
    private Button mCancelButton;
    private Button mInstallButton;
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
        super(parentShell,
              SWT.APPLICATION_MODAL);
        mUpdaterData = updaterData;
        mArchives = archives;
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

        if (!mCancelled) {
            for (ArchiveInfo ai : mArchives) {
                if (ai.isAccepted()) {
                    ais.add(ai);
                }
            }
        }

        return ais;
    }

    /**
     * Open the dialog and blocks till it gets closed
     */
    public void open() {
        createContents();
        positionShell();            //$hide$ (hide from SWT designer)
        mDialogShell.open();
        mDialogShell.layout();

        postCreate();               //$hide$ (hide from SWT designer)

        Display display = getParent().getDisplay();
        while (!mDialogShell.isDisposed() && !mCompleted) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        if (!mDialogShell.isDisposed()) {
            mDialogShell.close();
        }
    }

    /**
     * Create contents of the dialog.
     */
    private void createContents() {
        mDialogShell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
        mDialogShell.addShellListener(new ShellAdapter() {
            @Override
            public void shellClosed(ShellEvent e) {
                onShellClosed(e);
            }
        });
        mDialogShell.setLayout(new GridLayout(3, false/*makeColumnsEqual*/));
        mDialogShell.setSize(600, 400);
        mDialogShell.setText("Choose Packages to Install");

        // Sash form
        mSashForm = new SashForm(mDialogShell, SWT.NONE);
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
                getParent().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
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

        // Error message area

        mErrorLabel = new Label(mDialogShell, SWT.NONE);
        mErrorLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));


        // Bottom buttons area

        placeholder = new Label(mDialogShell, SWT.NONE);
        placeholder.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));

        // for MacOS, the Cancel button should be left.
        if (SdkConstants.currentPlatform() == SdkConstants.PLATFORM_DARWIN) {
            mCancelButton = new Button(mDialogShell, SWT.PUSH);
        }

        mInstallButton = new Button(mDialogShell, SWT.PUSH);
        mInstallButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mInstallButton.setText("Install Accepted");
        mInstallButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onInstallSelected();
            }
        });

        // if we haven't created the cancel button yet (macos?), create it now.
        if (mCancelButton == null) {
            mCancelButton = new Button(mDialogShell, SWT.PUSH);
        }
        mCancelButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mCancelButton.setText("Cancel");
        mCancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onCancelSelected();
            }
        });
    }

    // -- End of UI, Start of internal logic ----------
    // Hide everything down-below from SWT designer
    //$hide>>$

    /**
     * Starts the thread that runs the task.
     * This is deferred till the UI is created.
     */
    private void postCreate() {
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
                mDialogShell.setImage(imgFactory.getImageByName(imageName));
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
     * Callback invoked when the shell is closed either by clicking the close button
     * on by calling shell.close().
     * Captures the window size before closing this.
     */
    private void onShellClosed(ShellEvent e) {
        sLastSize = mDialogShell.getSize();
    }

    /**
     * Centers the dialog in its parent shell.
     */
    private void positionShell() {
        // Centers the dialog in its parent shell
        Shell child = mDialogShell;
        Shell parent = getParent();
        if (child != null && parent != null) {

            // get the parent client area with a location relative to the display
            Rectangle parentArea = parent.getClientArea();
            Point parentLoc = parent.getLocation();
            int px = parentLoc.x;
            int py = parentLoc.y;
            int pw = parentArea.width;
            int ph = parentArea.height;

            // Reuse the last size if there's one, otherwise use the default
            Point childSize = sLastSize != null ? sLastSize : child.getSize();
            int cw = childSize.x;
            int ch = childSize.y;

            int x = px + (pw - cw) / 2;
            int y = py + (ph - ch) / 2;

            if (x < 0) {
                x = 0;
            }

            if (y < MIN_Y) {
                y = MIN_Y;
            }

            child.setLocation(x, y);
            child.setSize(cw, ch);
        }
    }

    /**
     * Callback invoked when the Install button is selected. Completes the dialog.
     */
    private void onInstallSelected() {
        mCancelled = false;
        mCompleted = true;
    }

    /**
     * Callback invoked when the Cancel button is selected.
     */
    private void onCancelSelected() {
        mCancelled = true;
        mCompleted = true;
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

        Archive anew = ai.getNewArchive();

        mPackageText.setText("");                                                //$NON-NLS-1$

        addSectionTitle("Package Description\n");
        addText(anew.getParentPackage().getLongDescription(), "\n\n");          //$NON-NLS-1$

        Archive aold = ai.getReplaced();
        if (aold != null) {
            addText(String.format("This update will replace revision %1$s with revision %2$s.\n\n",
                    aold.getParentPackage().getRevision(),
                    anew.getParentPackage().getRevision()));
        }

        ArchiveInfo adep = ai.getDependsOn();
        if (adep != null || ai.isDependencyFor()) {
            addSectionTitle("Dependencies\n");

            if (adep != null) {
                addText(String.format("This package depends on %1$s.\n\n",
                        adep.getNewArchive().getParentPackage().getShortDescription()));
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
        addText(anew.getLongDescription(), "\n\n");                             //$NON-NLS-1$

        String license = anew.getParentPackage().getLicense();
        if (license != null) {
            addSectionTitle("License\n");
            addText(license.trim(), "\n");                                       //$NON-NLS-1$
        }
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
        mInstallButton.setEnabled(!missing && oneAccepted);

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
