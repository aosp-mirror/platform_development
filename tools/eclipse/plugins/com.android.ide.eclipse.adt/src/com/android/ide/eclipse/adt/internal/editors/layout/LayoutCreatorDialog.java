/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.internal.editors.layout;

import com.android.ide.eclipse.adt.internal.editors.IconFactory;
import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;
import com.android.ide.eclipse.adt.internal.resources.configurations.ResourceQualifier;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceFolderType;
import com.android.ide.eclipse.adt.internal.ui.ConfigurationSelector;
import com.android.ide.eclipse.adt.internal.ui.ConfigurationSelector.ConfigurationState;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog to choose a non existing {@link FolderConfiguration}.
 */
class LayoutCreatorDialog extends TrayDialog {

    private ConfigurationSelector mSelector;
    private Composite mStatusComposite;
    private Label mStatusLabel;
    private Label mStatusImage;

    private final FolderConfiguration mConfig = new FolderConfiguration();
    private final String mFileName;
    private final IAndroidTarget mTarget;

    /**
     * Creates a dialog, and init the UI from a {@link FolderConfiguration}.
     * @param parentShell the parent {@link Shell}.
     * @param config The starting configuration.
     */
    LayoutCreatorDialog(Shell parentShell, String fileName, IAndroidTarget target,
            FolderConfiguration config) {
        super(parentShell);

        mFileName = fileName;
        mTarget = target;

        // FIXME: add some data to know what configurations already exist.
        mConfig.set(config);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite top = new Composite(parent, SWT.NONE);
        top.setLayoutData(new GridData());
        top.setLayout(new GridLayout(1, false));

        new Label(top, SWT.NONE).setText(
                String.format("Configuration for the alternate version of %1$s", mFileName));

        mSelector = new ConfigurationSelector(top);
        mSelector.setConfiguration(mConfig);

        // parent's layout is a GridLayout as specified in the javadoc.
        GridData gd = new GridData();
        gd.widthHint = ConfigurationSelector.WIDTH_HINT;
        gd.heightHint = ConfigurationSelector.HEIGHT_HINT;
        mSelector.setLayoutData(gd);

        // add a listener to check on the validity of the FolderConfiguration as
        // they are built.
        mSelector.setOnChangeListener(new Runnable() {
            public void run() {
                ConfigurationState state = mSelector.getState();

                switch (state) {
                    case OK:
                        mSelector.getConfiguration(mConfig);

                        resetStatus();
                        mStatusImage.setImage(null);
                        getButton(IDialogConstants.OK_ID).setEnabled(true);
                        break;
                    case INVALID_CONFIG:
                        ResourceQualifier invalidQualifier = mSelector.getInvalidQualifier();
                        mStatusLabel.setText(String.format(
                                "Invalid Configuration: %1$s has no filter set.",
                                invalidQualifier.getName()));
                        mStatusImage.setImage(IconFactory.getInstance().getIcon("warning")); //$NON-NLS-1$
                        getButton(IDialogConstants.OK_ID).setEnabled(false);
                        break;
                    case REGION_WITHOUT_LANGUAGE:
                        mStatusLabel.setText(
                                "The Region qualifier requires the Language qualifier.");
                        mStatusImage.setImage(IconFactory.getInstance().getIcon("warning")); //$NON-NLS-1$
                        getButton(IDialogConstants.OK_ID).setEnabled(false);
                        break;
                }

                // need to relayout, because of the change in size in mErrorImage.
                mStatusComposite.layout();
            }
        });

        mStatusComposite = new Composite(top, SWT.NONE);
        mStatusComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout gl = new GridLayout(2, false);
        mStatusComposite.setLayout(gl);
        gl.marginHeight = gl.marginWidth = 0;

        mStatusImage = new Label(mStatusComposite, SWT.NONE);
        mStatusLabel = new Label(mStatusComposite, SWT.NONE);
        mStatusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        resetStatus();

        return top;
    }

    public void getConfiguration(FolderConfiguration config) {
        config.set(mConfig);
    }

    /**
     * resets the status label to show the file that will be created.
     */
    private void resetStatus() {
        mStatusLabel.setText(String.format("New File: res/%1$s/%2$s",
                mConfig.getFolderName(ResourceFolderType.LAYOUT, mTarget), mFileName));
    }
}
