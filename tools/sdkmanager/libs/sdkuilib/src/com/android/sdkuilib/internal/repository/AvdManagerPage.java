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

import com.android.sdkuilib.internal.widgets.AvdSelector;
import com.android.sdkuilib.internal.widgets.AvdSelector.DisplayMode;
import com.android.sdkuilib.repository.UpdaterWindow.ISdkListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class AvdManagerPage extends Composite implements ISdkListener {

    private AvdSelector mAvdSelector;

    private final UpdaterData mUpdaterData;

    /**
     * Create the composite.
     * @param parent The parent of the composite.
     * @param updaterData An instance of {@link UpdaterData}.
     */
    public AvdManagerPage(Composite parent, UpdaterData updaterData) {
        super(parent, SWT.BORDER);

        mUpdaterData = updaterData;
        mUpdaterData.addListeners(this);

        createContents(this);
        postCreate();  //$hide$
    }

    private void createContents(Composite parent) {
        parent.setLayout(new GridLayout(1, false));

        Label label = new Label(parent, SWT.NONE);
        label.setText("List of existing Android Virtual Devices:");

        mAvdSelector = new AvdSelector(parent,
                mUpdaterData.getOsSdkRoot(),
                mUpdaterData.getAvdManager(),
                DisplayMode.MANAGER);
    }

    @Override
    public void dispose() {
        mUpdaterData.removeListener(this);
        super.dispose();
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }

    // -- Start of internal part ----------
    // Hide everything down-below from SWT designer
    //$hide>>$

    /**
     * Called by the constructor right after {@link #createContents(Composite)}.
     */
    private void postCreate() {
        // nothing to be done for now.
    }

    public void onSdkChange() {
        mAvdSelector.refresh(false /*reload*/);
    }

    // End of hiding from SWT Designer
    //$hide<<$
}
