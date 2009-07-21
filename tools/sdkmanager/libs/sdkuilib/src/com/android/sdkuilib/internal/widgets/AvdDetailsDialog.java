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

package com.android.sdkuilib.internal.widgets;

import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdklib.internal.avd.AvdManager.AvdInfo;
import com.android.sdklib.internal.avd.AvdManager.AvdInfo.AvdStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Dialog displaying the details of an AVD.
 */
final class AvdDetailsDialog extends Dialog {

    /** Last dialog size for this session. */
    private static Point sLastSize;

    private Shell mDialogShell;
    private final AvdInfo mAvdInfo;

    private Composite mRootComposite;

    public AvdDetailsDialog(Shell shell, AvdInfo avdInfo) {
        super(shell, SWT.APPLICATION_MODAL);
        mAvdInfo = avdInfo;

        setText("AVD details");
    }

    /**
     * Open the dialog and blocks till it gets closed
     */
    public void open() {
        createContents();
        positionShell();            //$hide$ (hide from SWT designer)
        mDialogShell.open();
        mDialogShell.layout();

        Display display = getParent().getDisplay();
        while (!mDialogShell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        if (!mDialogShell.isDisposed()) {
            sLastSize = mDialogShell.getSize();
            mDialogShell.close();
        }
    }

    /**
     * Create contents of the dialog.
     */
    private void createContents() {
        mDialogShell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.RESIZE);
        mDialogShell.setLayout(new GridLayout(1, false));
        mDialogShell.setSize(450, 300);
        mDialogShell.setText(getText());

        mRootComposite = new Composite(mDialogShell, SWT.NONE);
        mRootComposite.setLayout(new GridLayout(2, false));
        mRootComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        GridLayout gl;

        Composite c = new Composite(mRootComposite, SWT.NONE);
        c.setLayout(gl = new GridLayout(2, false));
        gl.marginHeight = gl.marginWidth = 0;
        c.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        if (mAvdInfo != null) {
            displayValue(c, "Name:", mAvdInfo.getName());
            displayValue(c, "Path:", mAvdInfo.getPath());

            if (mAvdInfo.getStatus() != AvdStatus.OK) {
                displayValue(c, "Error:", mAvdInfo.getErrorMessage());
            } else {
                IAndroidTarget target = mAvdInfo.getTarget();
                AndroidVersion version = target.getVersion();
                displayValue(c, "Target:", String.format("%s (API level %s)",
                        target.getName(), version.getApiString()));

                // display some extra values.
                Map<String, String> properties = mAvdInfo.getProperties();
                if (properties != null) {
                    String skin = properties.get(AvdManager.AVD_INI_SKIN_NAME);
                    if (skin != null) {
                        displayValue(c, "Skin:", skin);
                    }

                    String sdcard = properties.get(AvdManager.AVD_INI_SDCARD_SIZE);
                    if (sdcard == null) {
                        sdcard = properties.get(AvdManager.AVD_INI_SDCARD_PATH);
                    }
                    if (sdcard != null) {
                        displayValue(c, "SD Card:", sdcard);
                    }

                    // display other hardware
                    HashMap<String, String> copy = new HashMap<String, String>(properties);
                    // remove stuff we already displayed (or that we don't want to display)
                    copy.remove(AvdManager.AVD_INI_SKIN_NAME);
                    copy.remove(AvdManager.AVD_INI_SKIN_PATH);
                    copy.remove(AvdManager.AVD_INI_SDCARD_SIZE);
                    copy.remove(AvdManager.AVD_INI_SDCARD_PATH);
                    copy.remove(AvdManager.AVD_INI_IMAGES_1);
                    copy.remove(AvdManager.AVD_INI_IMAGES_2);

                    if (copy.size() > 0) {
                        Label l = new Label(mRootComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
                        l.setLayoutData(new GridData(
                                GridData.FILL, GridData.CENTER, false, false, 2, 1));

                        c = new Composite(mRootComposite, SWT.NONE);
                        c.setLayout(gl = new GridLayout(2, false));
                        gl.marginHeight = gl.marginWidth = 0;
                        c.setLayoutData(new GridData(GridData.FILL_BOTH));

                        Set<String> keys = copy.keySet();
                        for (String key : keys) {
                            displayValue(c, key + ":", copy.get(key));
                        }
                    }
                }
            }
        }
    }

    // -- Start of internal part ----------
    // Hide everything down-below from SWT designer
    //$hide>>$

    /**
     * Displays a value with a label.
     *
     * @param parent the parent Composite in which to display the value. This Composite must use a
     * {@link GridLayout} with 2 columns.
     * @param label the label of the value to display.
     * @param value the string value to display.
     */
    private void displayValue(Composite parent, String label, String value) {
        Label l = new Label(parent, SWT.NONE);
        l.setText(label);
        l.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));

        l = new Label(parent, SWT.NONE);
        l.setText(value);
        l.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
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

    // End of hiding from SWT Designer
    //$hide<<$
}
