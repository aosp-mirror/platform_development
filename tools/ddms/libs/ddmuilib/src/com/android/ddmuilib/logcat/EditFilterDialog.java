/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ddmuilib.logcat;

import com.android.ddmuilib.IImageLoader;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Small dialog box to edit a static port number.
 */
public class EditFilterDialog extends Dialog {

    private static final int DLG_WIDTH = 400;
    private static final int DLG_HEIGHT = 250;

    private Shell mParent;

    private Shell mShell;

    private boolean mOk = false;

    private IImageLoader mImageLoader;

    /**
     * Filter being edited or created
     */
    private LogFilter mFilter;

    private String mName;
    private String mTag;
    private String mPid;

    /** Log level as an index of the drop-down combo
     * @see getLogLevel
     * @see getComboIndex
     */
    private int mLogLevel;

    private Button mOkButton;

    private Label mPidWarning;

    public EditFilterDialog(IImageLoader imageLoader, Shell parent) {
        super(parent, SWT.DIALOG_TRIM | SWT.BORDER | SWT.APPLICATION_MODAL);
        mImageLoader = imageLoader;
    }

    public EditFilterDialog(IImageLoader imageLoader, Shell shell,
            LogFilter filter) {
        this(imageLoader, shell);
        mFilter = filter;
    }

    /**
     * Opens the dialog. The method will return when the user closes the dialog
     * somehow.
     *
     * @return true if ok was pressed, false if cancelled.
     */
    public boolean open() {
        createUI();

        if (mParent == null || mShell == null) {
            return false;
        }

        mShell.setMinimumSize(DLG_WIDTH, DLG_HEIGHT);
        Rectangle r = mParent.getBounds();
        // get the center new top left.
        int cx = r.x + r.width/2;
        int x = cx - DLG_WIDTH / 2;
        int cy = r.y + r.height/2;
        int y = cy - DLG_HEIGHT / 2;
        mShell.setBounds(x, y, DLG_WIDTH, DLG_HEIGHT);

        mShell.open();

        Display display = mParent.getDisplay();
        while (!mShell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }

        // we're quitting with OK.
        // Lets update the filter if needed
        if (mOk) {
            // if it was a "Create filter" action we need to create it first.
            if (mFilter == null) {
                mFilter = new LogFilter(mName);
            }

            // setup the filter
            mFilter.setTagMode(mTag);

            if (mPid != null && mPid.length() > 0) {
                mFilter.setPidMode(Integer.parseInt(mPid));
            } else {
                mFilter.setPidMode(-1);
            }

            mFilter.setLogLevel(getLogLevel(mLogLevel));
        }

        return mOk;
    }

    public LogFilter getFilter() {
        return mFilter;
    }

    private void createUI() {
        mParent = getParent();
        mShell = new Shell(mParent, getStyle());
        mShell.setText("Log Filter");

        mShell.setLayout(new GridLayout(1, false));

        mShell.addListener(SWT.Close, new Listener() {
            public void handleEvent(Event event) {
            }
        });

        // top part with the filter name
        Composite nameComposite = new Composite(mShell, SWT.NONE);
        nameComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        nameComposite.setLayout(new GridLayout(2, false));

        Label l = new Label(nameComposite, SWT.NONE);
        l.setText("Filter Name:");

        final Text filterNameText = new Text(nameComposite,
                SWT.SINGLE | SWT.BORDER);
        if (mFilter != null) {
            mName = mFilter.getName();
            if (mName != null) {
                filterNameText.setText(mName);
            }
        }
        filterNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        filterNameText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                mName = filterNameText.getText().trim();
                validate();
            }
        });

        // separator
        l = new Label(mShell, SWT.SEPARATOR | SWT.HORIZONTAL);
        l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));


        // center part with the filter parameters
        Composite main = new Composite(mShell, SWT.NONE);
        main.setLayoutData(new GridData(GridData.FILL_BOTH));
        main.setLayout(new GridLayout(3, false));

        l = new Label(main, SWT.NONE);
        l.setText("by Log Tag:");

        final Text tagText = new Text(main, SWT.SINGLE | SWT.BORDER);
        if (mFilter != null) {
            mTag = mFilter.getTagFilter();
            if (mTag != null) {
                tagText.setText(mTag);
            }
        }
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        tagText.setLayoutData(gd);
        tagText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                mTag = tagText.getText().trim();
                validate();
            }
        });

        l = new Label(main, SWT.NONE);
        l.setText("by pid:");

        final Text pidText = new Text(main, SWT.SINGLE | SWT.BORDER);
        if (mFilter != null) {
            if (mFilter.getPidFilter() != -1) {
                mPid = Integer.toString(mFilter.getPidFilter());
            } else {
                mPid = "";
            }
            pidText.setText(mPid);
        }
        pidText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        pidText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                mPid = pidText.getText().trim();
                validate();
            }
        });

        mPidWarning = new Label(main, SWT.NONE);
        mPidWarning.setImage(mImageLoader.loadImage("empty.png", // $NON-NLS-1$
                mShell.getDisplay()));

        l = new Label(main, SWT.NONE);
        l.setText("by Log level:");

        final Combo logCombo = new Combo(main, SWT.DROP_DOWN | SWT.READ_ONLY);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        logCombo.setLayoutData(gd);

        // add the labels
        logCombo.add("<none>");
        logCombo.add("Error");
        logCombo.add("Warning");
        logCombo.add("Info");
        logCombo.add("Debug");
        logCombo.add("Verbose");

        if (mFilter != null) {
            mLogLevel = getComboIndex(mFilter.getLogLevel());
            logCombo.select(mLogLevel);
        } else {
            logCombo.select(0);
        }

        logCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // get the selection
                mLogLevel = logCombo.getSelectionIndex();
                validate();
            }
        });

        // separator
        l = new Label(mShell, SWT.SEPARATOR | SWT.HORIZONTAL);
        l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // bottom part with the ok/cancel
        Composite bottomComp = new Composite(mShell, SWT.NONE);
        bottomComp
                .setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
        bottomComp.setLayout(new GridLayout(2, true));

        mOkButton = new Button(bottomComp, SWT.NONE);
        mOkButton.setText("OK");
        mOkButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mOk = true;
                mShell.close();
            }
        });
        mOkButton.setEnabled(false);
        mShell.setDefaultButton(mOkButton);

        Button cancelButton = new Button(bottomComp, SWT.NONE);
        cancelButton.setText("Cancel");
        cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mShell.close();
            }
        });

        validate();
    }

    /**
     * Returns the log level from a combo index.
     * @param index the Combo index
     * @return a log level valid for the Log class.
     */
    protected int getLogLevel(int index) {
        if (index == 0) {
            return -1;
        }

        return 7 - index;
    }

    /**
     * Returns the index in the combo that matches the log level
     * @param logLevel The Log level.
     * @return the combo index
     */
    private int getComboIndex(int logLevel) {
        if (logLevel == -1) {
            return 0;
        }

        return 7 - logLevel;
    }

    /**
     * Validates the content of the 2 text fields and enable/disable "ok", while
     * setting up the warning/error message.
     */
    private void validate() {

        // then we check it only contains digits.
        if (mPid != null) {
            if (mPid.matches("[0-9]*") == false) { // $NON-NLS-1$
                mOkButton.setEnabled(false);
                mPidWarning.setImage(mImageLoader.loadImage(
                        "warning.png", // $NON-NLS-1$
                        mShell.getDisplay()));
                return;
            } else {
                mPidWarning.setImage(mImageLoader.loadImage(
                        "empty.png", // $NON-NLS-1$
                        mShell.getDisplay()));
            }
        }

        if (mName == null || mName.length() == 0) {
            mOkButton.setEnabled(false);
            return;
        }

        mOkButton.setEnabled(true);
    }
}
