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

import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdklib.internal.avd.AvdManager.AvdInfo;
import com.android.sdkuilib.internal.repository.UpdaterData.ISdkListener;
import com.android.sdkuilib.internal.widgets.AvdSelector;
import com.android.sdkuilib.internal.widgets.AvdSelector.SelectionMode;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import java.util.HashSet;

public class AvdManagerPage extends Composite implements ISdkListener {

    private Button mRefreshButton;
    private AvdSelector mAvdSelector;

    private final HashSet<String> mKnownAvdNames = new HashSet<String>();
    private final UpdaterData mUpdaterData;

    /**
     * Create the composite.
     * @param parent The parent of the composite.
     * @param updaterData An instance of {@link UpdaterData}. If null, a local
     *        one will be allocated just to help with the SWT Designer.
     */
    public AvdManagerPage(Composite parent, UpdaterData updaterData) {
        super(parent, SWT.BORDER);

        mUpdaterData = updaterData != null ? updaterData : new UpdaterData();
        mUpdaterData.addListeners(this);

        createContents(this);
        postCreate();  //$hide$
    }

    private void createContents(Composite parent) {
        parent.setLayout(new GridLayout(3, false));

        Label label = new Label(parent, SWT.NONE);
        label.setText("List of existing Android Virtual Devices:");
        label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));

        mRefreshButton = new Button(parent, SWT.PUSH);
        mRefreshButton.setText("Refresh");
        mRefreshButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        mRefreshButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onRefreshSelected(); //$hide$
           }
        });

        Composite group = new Composite(parent, SWT.NONE);
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
        GridLayout gl;
        group.setLayout(gl = new GridLayout(1, false /*makeColumnsEqualWidth*/));
        gl.marginHeight = gl.marginWidth = 0;

        mAvdSelector = new AvdSelector(group,
                SelectionMode.SELECT,
                new AvdSelector.IExtraAction() {
                    public String label() {
                        return "Delete AVD...";
                    }

                    public boolean isEnabled() {
                        return mAvdSelector != null && mAvdSelector.getSelected() != null;
                    }

                    public void run() {
                        //onDelete();
                    }
            });
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
        reloadAvdList();
    }

    /**
     * Reloads the AVD list in the AVD selector.
     * Tries to preserve the selection.
     */
    private void reloadAvdList() {
        AvdInfo selected = mAvdSelector.getSelected();

        AvdInfo[] avds = null;

        AvdManager manager = mUpdaterData.getAvdManager();
        if (manager != null) {
            avds = manager.getValidAvds();
        }

        mAvdSelector.setAvds(avds, null /*filter*/);

        // Keep the list of known AVD names to check if they exist quickly. however
        // use the list of all AVDs, including broken ones (unless we don't know their
        // name).
        mKnownAvdNames.clear();
        if (manager != null) {
            for (AvdInfo avd : manager.getAllAvds()) {
                String name = avd.getName();
                if (name != null) {
                    mKnownAvdNames.add(name);
                }
            }
        }

        mAvdSelector.setSelection(selected);
    }

    public void onSdkChange() {
        reloadAvdList();
    }

    private void onRefreshSelected() {
        mUpdaterData.reloadAvds();
        reloadAvdList();
    }

    // End of hiding from SWT Designer
    //$hide<<$
}
