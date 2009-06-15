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

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
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
import org.eclipse.swt.widgets.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;


/**
 * Implements an {@link UpdateChooserDialog}.
 */
final class UpdateChooserDialog extends Dialog {

    /** Last dialog size for this session. */
    private static Point sLastSize;
    private boolean mCompleted;
    private final Map<Archive, Archive> mNewToOldArchiveMap;
    private boolean mLicenseAcceptAll;
    private boolean mInternalLicenseRadioUpdate;
    private ArrayList<Archive> mResult = new ArrayList<Archive>();

    // UI fields
    private Shell mDialogShell;
    private SashForm mSashForm;
    private Composite mPackageRootComposite;
    private Button mCancelButton;
    private Button mInstallButton;
    private CheckboxTableViewer mTableViewPackage;
    private Table mTablePackage;
    private TableColumn mTableColum;
    private Text mPackageText;
    private Button mLicenseRadioAccept;
    private Button mLicenseRadioRefuse;
    private Button mLicenseRadioAcceptAll;
    private Group mPackageTextGroup;


    /**
     * Create the dialog.
     * @param parent Parent container
     * @param newToOldUpdates The map [new archive => old archive] of potential updates
     */
    public UpdateChooserDialog(Shell parent, Map<Archive, Archive> newToOldUpdates) {
        super(parent, SWT.APPLICATION_MODAL);

        mNewToOldArchiveMap = new TreeMap<Archive, Archive>(new Comparator<Archive>() {
            public int compare(Archive a1, Archive a2) {
                // The items are archive but what we show are packages so we'll
                // sort of packages short descriptions
                String desc1 = a1.getParentPackage().getShortDescription();
                String desc2 = a2.getParentPackage().getShortDescription();
                return desc1.compareTo(desc2);
            }
        });
        mNewToOldArchiveMap.putAll(newToOldUpdates);
    }

    /**
     * Returns the results, i.e. the list of selected new archives to install.
     * The list is always non null. It is empty when cancel is selected or when
     * all potential updates have been refused.
     */
    public Collection<Archive> getResult() {
        return mResult;
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
        mDialogShell.setText(getText());

        // Sash form
        mSashForm = new SashForm(mDialogShell, SWT.NONE);
        mSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));


        // Left part of Sash Form

        mTableViewPackage = CheckboxTableViewer.newCheckList(mSashForm,
                SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE);
        mTablePackage = mTableViewPackage.getTable();
        mTablePackage.setHeaderVisible(false);

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

        mTableViewPackage.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                onPackageChecked(event);
            }
        });

        mTableColum = new TableColumn(mTablePackage, SWT.NONE);
        mTableColum.setWidth(100);
        mTableColum.setText("Packages");


        // Right part of Sash form
        mPackageRootComposite = new Composite(mSashForm, SWT.NONE);
        mPackageRootComposite.setLayout(new GridLayout(4, false/*makeColumnsEqual*/));

        mPackageTextGroup = new Group(mPackageRootComposite, SWT.NONE);
        mPackageTextGroup.setText("Package Description && License");
        mPackageTextGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
        mPackageTextGroup.setLayout(new GridLayout(1, false/*makeColumnsEqual*/));

        mPackageText = new Text(mPackageTextGroup,
                SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
        mPackageText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        mLicenseRadioAccept = new Button(mPackageRootComposite, SWT.RADIO);
        mLicenseRadioAccept.setText("Accept");
        mLicenseRadioAccept.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
        mLicenseRadioAccept.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onLicenseRadioSelected();
            }
        });

        mLicenseRadioRefuse = new Button(mPackageRootComposite, SWT.RADIO);
        mLicenseRadioRefuse.setText("Refuse");
        mLicenseRadioRefuse.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
        mLicenseRadioRefuse.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onLicenseRadioSelected();
            }
        });

        Label placeholder = new Label(mPackageRootComposite, SWT.NONE);
        placeholder.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));

        mLicenseRadioAcceptAll = new Button(mPackageRootComposite, SWT.RADIO);
        mLicenseRadioAcceptAll.setText("Accept All");
        mLicenseRadioAcceptAll.setLayoutData(
                new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
        mLicenseRadioAcceptAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onLicenseRadioSelected();
            }
        });

        mSashForm.setWeights(new int[] {200, 300});

        // Bottom buttons
        placeholder = new Label(mDialogShell, SWT.NONE);
        placeholder.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));

        mInstallButton = new Button(mDialogShell, SWT.PUSH);
        mInstallButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mInstallButton.setText("Install Selected");
        mInstallButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onInstallSelected();
            }
        });

        mCancelButton = new Button(mDialogShell, SWT.PUSH);
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
        // Fill the list with the replacement packages
        mTableViewPackage.setLabelProvider(new NewArchivesLabelProvider());
        mTableViewPackage.setContentProvider(new NewArchivesContentProvider());
        mTableViewPackage.setInput(mNewToOldArchiveMap);

        adjustColumnsWidth();

        // select first item
        mTablePackage.select(0);
        onPackageSelected();
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

            child.setLocation(px + (pw - cw) / 2, py + (ph - ch) / 2);
            child.setSize(cw, ch);
        }
    }

    /**
     * Callback invoked when the Install button is selected. Fills {@link #mResult} and
     * completes the dialog.
     */
    private void onInstallSelected() {

        // get list of checked items
        Object[] checked = mTableViewPackage.getCheckedElements();

        if (checked != null) {
            for (Object obj : checked) {
                mResult.add((Archive) obj);
            }
        }

        mCompleted = true;
    }

    /**
     * Callback invoked when the Cancel button is selected.
     */
    private void onCancelSelected() {
        mCompleted = true;
    }

    /**
     * Callback invoked when a package item is selected in the list.
     */
    private void onPackageSelected() {
        Archive a = getSelectedArchive();
        displayInformation(a);
        updateLicenceRadios(a);
    }

    /** Returns the currently selected Archive or null. */
    private Archive getSelectedArchive() {
        ISelection sel = mTableViewPackage.getSelection();
        if (sel instanceof IStructuredSelection) {
            Object elem = ((IStructuredSelection) sel).getFirstElement();
            if (elem instanceof Archive) {
                return (Archive) elem;
            }
        }
        return null;
    }

    private void setSelectedAchive(Archive a) {
        if (a == null) {
            mTablePackage.deselectAll();
        } else {
            IStructuredSelection sel = new StructuredSelection(a);
            mTableViewPackage.setSelection(sel, true /*reveal*/);
        }
    }

    private void displayInformation(Archive a) {
        if (a == null) {
            mPackageText.setText("Please select a package.");
            return;
        }

        StringBuilder sb = new StringBuilder();

        Archive aold = mNewToOldArchiveMap.get(a);
        if (aold != null) {
            sb.append("*** Existing Package Description:\n");
            sb.append(aold.getParentPackage().getLongDescription()).append("\n\n");
        }

        sb.append("*** New Package Description:\n");
        sb.append(a.getParentPackage().getLongDescription()).append("\n\n");

        sb.append("\n*** Archive Description:\n");
        sb.append(a.getLongDescription()).append("\n");

        sb.append("\n*** Package License:\n");
        sb.append(a.getParentPackage().getLicense()).append("\n");

        mPackageText.setText(sb.toString());
    }

    private void updateLicenceRadios(Archive a) {
        if (mInternalLicenseRadioUpdate) {
            return;
        }
        mInternalLicenseRadioUpdate = true;

        if (mLicenseAcceptAll) {
            mLicenseRadioAcceptAll.setSelection(true);
            mLicenseRadioAccept.setSelection(false);
            mLicenseRadioRefuse.setSelection(false);
        } else {
            boolean accept = mTableViewPackage.getChecked(a);

            mLicenseRadioAcceptAll.setSelection(false);
            mLicenseRadioAccept.setSelection(accept);
            mLicenseRadioRefuse.setSelection(!accept);
        }

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

        Archive a = getSelectedArchive();

        if (!mLicenseAcceptAll && mLicenseRadioAcceptAll.getSelection()) {
            // Accept all has been switched on. Mark all packages as accepted
            mLicenseAcceptAll = true;
            mTableViewPackage.setAllChecked(true);

        } else if (mLicenseRadioAccept.getSelection()) {
            // Accept only this one
            mLicenseAcceptAll = false;
            mTableViewPackage.setChecked(a, true);

        } else if (mLicenseRadioRefuse.getSelection()) {
            // Refuse only this one
            mLicenseAcceptAll = false;
            mTableViewPackage.setChecked(a, false);
        }

        mInternalLicenseRadioUpdate = false;
    }

    /**
     * Callback invoked when a package item is double-clicked in the list.
     */
    private void onPackageDoubleClick() {
        Archive a = getSelectedArchive();

        mTableViewPackage.setChecked(a, !mTableViewPackage.getChecked(a));
    }

    private void onPackageChecked(CheckStateChangedEvent event) {
        Object e = event.getElement();
        if (e instanceof Archive) {
            Archive a = (Archive) e;

            // select it
            mLicenseAcceptAll = false;
            setSelectedAchive(a);
            updateLicenceRadios(a);
        }
    }

    private class NewArchivesLabelProvider extends LabelProvider {
        @Override
        public Image getImage(Object element) {
            return super.getImage(element);
        }

        @Override
        public String getText(Object element) {
            if (element instanceof Archive) {
                return ((Archive) element).getParentPackage().getShortDescription();
            }
            return super.getText(element);
        }
    }

    private class NewArchivesContentProvider implements IStructuredContentProvider {

        public void dispose() {
            // pass
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // Ignore. The input is always mNewArchives
        }

        public Object[] getElements(Object inputElement) {
            return mNewToOldArchiveMap.keySet().toArray();
        }
    }

    // End of hiding from SWT Designer
    //$hide<<$
}
