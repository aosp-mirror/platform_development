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

import com.android.sdkuilib.ui.GridDialog;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

/**
 * Small dialog to let a user choose a screen size (from a fixed list) and a resolution
 * (as returned by {@link Display#getMonitors()}).

 * After the dialog as returned, one can query {@link #getDensity()} to get the chosen monitor
 * pixel density.
 */
class ResolutionChooserDialog extends GridDialog {
    public final static float[] MONITOR_SIZES = new float[] {
            13.3f, 14, 15.4f, 15.6f, 17, 19, 20, 21, 24, 30,
    };

    private Button mButton;
    private Combo mScreenSizeCombo;
    private Combo mMonitorCombo;

    private Monitor[] mMonitors;
    private int mScreenSizeIndex = -1;
    private int mMonitorIndex = 0;

    ResolutionChooserDialog(Shell parentShell) {
        super(parentShell, 2, false);
    }

    /**
     * Returns the pixel density of the user-chosen monitor.
     */
    int getDensity() {
        float size = MONITOR_SIZES[mScreenSizeIndex];
        Rectangle rect = mMonitors[mMonitorIndex].getBounds();

        // compute the density
        double d = Math.sqrt(rect.width * rect.width + rect.height * rect.height) / size;
        return (int)Math.round(d);
    }

    @Override
    protected void configureShell(Shell newShell) {
        newShell.setText("Monitor Density");
        super.configureShell(newShell);
    }

    @Override
    protected Control createContents(Composite parent) {
        Control control = super.createContents(parent);
        mButton = getButton(IDialogConstants.OK_ID);
        mButton.setEnabled(false);
        return control;
    }

    @Override
    public void createDialogContent(Composite parent) {
        Label l = new Label(parent, SWT.NONE);
        l.setText("Screen Size:");

        mScreenSizeCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (float size : MONITOR_SIZES) {
            if (Math.round(size) == size) {
                mScreenSizeCombo.add(String.format("%.0f\"", size));
            } else {
                mScreenSizeCombo.add(String.format("%.1f\"", size));
            }
        }
        mScreenSizeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                mScreenSizeIndex = mScreenSizeCombo.getSelectionIndex();
                mButton.setEnabled(mScreenSizeIndex != -1);
            }
        });

        l = new Label(parent, SWT.NONE);
        l.setText("Resolution:");

        mMonitorCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        mMonitors = parent.getDisplay().getMonitors();
        for (Monitor m : mMonitors) {
            Rectangle r = m.getBounds();
            mMonitorCombo.add(String.format("%d x %d", r.width, r.height));
        }
        mMonitorCombo.select(mMonitorIndex);
        mMonitorCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                mMonitorIndex = mMonitorCombo.getSelectionIndex();
            }
        });
    }
}
