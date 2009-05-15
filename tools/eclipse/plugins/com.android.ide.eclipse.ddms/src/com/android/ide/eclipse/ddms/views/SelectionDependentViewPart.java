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

package com.android.ide.eclipse.ddms.views;

import com.android.ddmlib.Client;
import com.android.ddmlib.IDevice;
import com.android.ddmuilib.SelectionDependentPanel;
import com.android.ide.eclipse.ddms.DdmsPlugin;
import com.android.ide.eclipse.ddms.DdmsPlugin.ISelectionListener;

import org.eclipse.swt.graphics.Device;
import org.eclipse.ui.part.ViewPart;

/**
 * A Workbench {@link ViewPart} that requires {@link Device}/{@link Client} selection notifications
 * from {@link DdmsPlugin} through the {@link ISelectionListener} interface.
 */
public abstract class SelectionDependentViewPart extends ViewPart implements ISelectionListener {

    private SelectionDependentPanel mPanel;

    protected final void setSelectionDependentPanel(SelectionDependentPanel panel) {
        // remember the panel
        mPanel = panel;

        // and add ourself as listener of selection events.
        DdmsPlugin.getDefault().addSelectionListener(this);
    }

    @Override
    public void dispose() {
        DdmsPlugin.getDefault().removeSelectionListener(this);
        super.dispose();
    }

    /**
     * Sent when a new {@link Client} is selected.
     * @param selectedClient The selected client.
     *
     * @see ISelectionListener
     */
    public final void selectionChanged(Client selectedClient) {
        mPanel.clientSelected(selectedClient);
    }

    /**
     * Sent when a new {@link Device} is selected.
     * @param selectedDevice the selected device.
     *
     * @see ISelectionListener
     */
    public final void selectionChanged(IDevice selectedDevice) {
        mPanel.deviceSelected(selectedDevice);
    }
}
