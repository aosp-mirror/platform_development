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

package com.android.ddms;

import com.android.ddmlib.IDevice;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.util.ArrayList;

/**
 * Small dialog box to edit a static port number.
 */
public class StaticPortEditDialog extends Dialog {

    private static final int DLG_WIDTH = 400;
    private static final int DLG_HEIGHT = 200;

    private Shell mParent;

    private Shell mShell;

    private boolean mOk = false;

    private String mAppName;

    private String mPortNumber;

    private Button mOkButton;

    private Label mWarning;

    /** List of ports already in use */
    private ArrayList<Integer> mPorts;

    /** This is the port being edited. */
    private int mEditPort = -1;
    private String mDeviceSn;

    /**
     * Creates a dialog with empty fields.
     * @param parent The parent Shell
     * @param ports The list of already used port numbers.
     */
    public StaticPortEditDialog(Shell parent, ArrayList<Integer> ports) {
        super(parent, SWT.DIALOG_TRIM | SWT.BORDER | SWT.APPLICATION_MODAL);
        mPorts = ports;
        mDeviceSn = IDevice.FIRST_EMULATOR_SN;
    }

    /**
     * Creates a dialog with predefined values.
     * @param shell The parent shell
     * @param ports The list of already used port numbers.
     * @param oldDeviceSN the device serial number to display
     * @param oldAppName The application name to display
     * @param oldPortNumber The port number to display
     */
    public StaticPortEditDialog(Shell shell, ArrayList<Integer> ports,
            String oldDeviceSN, String oldAppName, String oldPortNumber) {
        this(shell, ports);

        mDeviceSn = oldDeviceSN;
        mAppName = oldAppName;
        mPortNumber = oldPortNumber;
        mEditPort = Integer.valueOf(mPortNumber);
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

        return mOk;
    }

    public String getDeviceSN() {
        return mDeviceSn;
    }

    public String getAppName() {
        return mAppName;
    }

    public int getPortNumber() {
        return Integer.valueOf(mPortNumber);
    }

    private void createUI() {
        mParent = getParent();
        mShell = new Shell(mParent, getStyle());
        mShell.setText("Static Port");

        mShell.setLayout(new GridLayout(1, false));

        mShell.addListener(SWT.Close, new Listener() {
            public void handleEvent(Event event) {
            }
        });

        // center part with the edit field
        Composite main = new Composite(mShell, SWT.NONE);
        main.setLayoutData(new GridData(GridData.FILL_BOTH));
        main.setLayout(new GridLayout(2, false));

        Label l0 = new Label(main, SWT.NONE);
        l0.setText("Device Name:");

        final Text deviceSNText = new Text(main, SWT.SINGLE | SWT.BORDER);
        deviceSNText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (mDeviceSn != null) {
            deviceSNText.setText(mDeviceSn);
        }
        deviceSNText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                mDeviceSn = deviceSNText.getText().trim();
                validate();
            }
        });

        Label l = new Label(main, SWT.NONE);
        l.setText("Application Name:");

        final Text appNameText = new Text(main, SWT.SINGLE | SWT.BORDER);
        if (mAppName != null) {
            appNameText.setText(mAppName);
        }
        appNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        appNameText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                mAppName = appNameText.getText().trim();
                validate();
            }
        });

        Label l2 = new Label(main, SWT.NONE);
        l2.setText("Debug Port:");

        final Text debugPortText = new Text(main, SWT.SINGLE | SWT.BORDER);
        if (mPortNumber != null) {
            debugPortText.setText(mPortNumber);
        }
        debugPortText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        debugPortText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                mPortNumber = debugPortText.getText().trim();
                validate();
            }
        });

        // warning label
        Composite warningComp = new Composite(mShell, SWT.NONE);
        warningComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        warningComp.setLayout(new GridLayout(1, true));

        mWarning = new Label(warningComp, SWT.NONE);
        mWarning.setText("");
        mWarning.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

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
     * Validates the content of the 2 text fields and enable/disable "ok", while
     * setting up the warning/error message.
     */
    private void validate() {
        // first we reset the warning dialog. This allows us to latter
        // display warnings.
        mWarning.setText(""); // $NON-NLS-1$

        // check the device name field is not empty
        if (mDeviceSn == null || mDeviceSn.length() == 0) {
            mWarning.setText("Device name missing.");
            mOkButton.setEnabled(false);
            return;
        }

        // check the application name field is not empty
        if (mAppName == null || mAppName.length() == 0) {
            mWarning.setText("Application name missing.");
            mOkButton.setEnabled(false);
            return;
        }

        String packageError = "Application name must be a valid Java package name.";

        // validate the package name as well. It must be a fully qualified
        // java package.
        String[] packageSegments = mAppName.split("\\."); // $NON-NLS-1$
        for (String p : packageSegments) {
            if (p.matches("^[a-zA-Z][a-zA-Z0-9]*") == false) { // $NON-NLS-1$
                mWarning.setText(packageError);
                mOkButton.setEnabled(false);
                return;
            }

            // lets also display a warning if the package contains upper case
            // letters.
            if (p.matches("^[a-z][a-z0-9]*") == false) { // $NON-NLS-1$
                mWarning.setText("Lower case is recommended for Java packages.");
            }
        }

        // the split will not detect the last char being a '.'
        // so we test it manually
        if (mAppName.charAt(mAppName.length()-1) == '.') {
            mWarning.setText(packageError);
            mOkButton.setEnabled(false);
            return;
        }

        // now we test the package name field is not empty.
        if (mPortNumber == null || mPortNumber.length() == 0) {
            mWarning.setText("Port Number missing.");
            mOkButton.setEnabled(false);
            return;
        }

        // then we check it only contains digits.
        if (mPortNumber.matches("[0-9]*") == false) { // $NON-NLS-1$
            mWarning.setText("Port Number invalid.");
            mOkButton.setEnabled(false);
            return;
        }

        // get the int from the port number to validate
        long port = Long.valueOf(mPortNumber);
        if (port >= 32767) {
            mOkButton.setEnabled(false);
            return;
        }

        // check if its in the list of already used ports
        if (port != mEditPort) {
            for (Integer i : mPorts) {
                if (port == i.intValue()) {
                    mWarning.setText("Port already in use.");
                    mOkButton.setEnabled(false);
                    return;
                }
            }
        }

        // at this point there's not error, so we enable the ok button.
        mOkButton.setEnabled(true);
    }
}
