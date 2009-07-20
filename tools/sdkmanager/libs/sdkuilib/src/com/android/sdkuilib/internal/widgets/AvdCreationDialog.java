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

import com.android.prefs.AndroidLocation;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkManager;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdklib.internal.avd.AvdManager.AvdInfo;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;
import com.android.sdkuilib.internal.widgets.AvdSelector.SdkLog;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.io.File;
import java.util.TreeMap;

/**
 * AVD creator dialog.
 *
 * TODO:
 * - support custom hardware properties
 * - use SdkTargetSelector instead of Combo
 * - Better UI for the sdcard (radio button for K or M, info about what is valid value)
 * - Support for ###x### skins
 * - tooltips on widgets.
 *
 */
final class AvdCreationDialog extends Dialog {

    private final AvdManager mAvdManager;
    private final TreeMap<String, IAndroidTarget> mCurrentTargets =
        new TreeMap<String, IAndroidTarget>();

    private Text mAvdName;
    private Combo mTargetCombo;
    private Text mSdCard;
    private Button mBrowseSdCard;
    private Combo mSkinCombo;
    private Button mForceCreation;
    private Button mOkButton;
    private Label mStatusIcon;
    private Label mStatusLabel;
    private Composite mStatusComposite;
    private final ImageFactory mImageFactory;

    /**
     * Callback when the AVD name is changed.
     * Enables the force checkbox if the name is a duplicate.
     */
    private class CreateNameModifyListener implements ModifyListener {

        public void modifyText(ModifyEvent e) {
            String name = mAvdName.getText().trim();
            AvdInfo avdMatch = mAvdManager.getAvd(name, false /*validAvdOnly*/);
            if (avdMatch != null) {
                mForceCreation.setEnabled(true);
            } else {
                mForceCreation.setEnabled(false);
                mForceCreation.setSelection(false);
            }

            validatePage();
        }
    }

    private class ValidateListener extends SelectionAdapter implements ModifyListener {

        public void modifyText(ModifyEvent e) {
            validatePage();
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            super.widgetSelected(e);
            validatePage();
        }
    }

    protected AvdCreationDialog(Shell parentShell, AvdManager avdManager,
            ImageFactory imageFactory) {
        super(parentShell);
        mAvdManager = avdManager;
        mImageFactory = imageFactory;
    }

    @Override
    public void create() {
        super.create();

        Point p = getShell().getSize();
        if (p.x < 400) {
            p.x = 400;
        }
        getShell().setSize(p);
    }

    @Override
    protected Control createContents(Composite parent) {
        Control control = super.createContents(parent);
        getShell().setText("Create new AVD");

        mOkButton = getButton(IDialogConstants.OK_ID);
        validatePage();

        return control;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite top = new Composite(parent, SWT.NONE);
        top.setLayoutData(new GridData(GridData.FILL_BOTH));
        top.setLayout(new GridLayout(3, false));

        Label label = new Label(top, SWT.NONE);
        label.setText("Name:");

        mAvdName = new Text(top, SWT.BORDER);
        mAvdName.setLayoutData(new GridData(GridData.FILL, GridData.CENTER,
                true, false, 2, 1));
        mAvdName.addModifyListener(new CreateNameModifyListener());

        label = new Label(top, SWT.NONE);
        label.setText("Target:");

        mTargetCombo = new Combo(top, SWT.READ_ONLY | SWT.DROP_DOWN);
        mTargetCombo.setLayoutData(new GridData(GridData.FILL, GridData.CENTER,
                true, false, 2, 1));
        mTargetCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                reloadSkinCombo();
                validatePage();
            }
        });

        label = new Label(top, SWT.NONE);
        label.setText("SD Card:");
        label.setToolTipText("Either a path to an existing SD card image\n" +
                "or an image size in K or M (e.g. 512K, 10M).");

        ValidateListener validateListener = new ValidateListener();

        mSdCard = new Text(top, SWT.BORDER);
        mSdCard.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mSdCard.addModifyListener(validateListener);

        mBrowseSdCard = new Button(top, SWT.PUSH);
        mBrowseSdCard.setText("Browse...");
        mBrowseSdCard.addSelectionListener(new SelectionAdapter() {
           @Override
            public void widgetSelected(SelectionEvent arg0) {
               onBrowseSdCard();
               validatePage();
            }
        });

        label = new Label(top, SWT.NONE);
        label.setText("Skin");

        mSkinCombo = new Combo(top, SWT.READ_ONLY | SWT.DROP_DOWN);
        mSkinCombo.setLayoutData(new GridData(GridData.FILL, GridData.CENTER,
                true, false, 2, 1));

        // dummies for alignment
        label = new Label(top, SWT.NONE);

        mForceCreation = new Button(top, SWT.CHECK);
        mForceCreation.setText("Force");
        mForceCreation.setLayoutData(new GridData(GridData.FILL, GridData.CENTER,
                true, false, 2, 1));
        mForceCreation.setEnabled(false);
        mForceCreation.addSelectionListener(validateListener);

        // add a separator to separate from the ok/cancel button
        label = new Label(top, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 3, 1));

        // add stuff for the error display
        mStatusComposite = new Composite(top, SWT.NONE);
        mStatusComposite.setLayoutData(new GridData(GridData.FILL, GridData.CENTER,
                true, false, 3, 1));
        GridLayout gl;
        mStatusComposite.setLayout(gl = new GridLayout(2, false));
        gl.marginHeight = gl.marginWidth = 0;

        mStatusIcon = new Label(mStatusComposite, SWT.NONE);
        mStatusIcon.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING,
                false, false));
        mStatusLabel = new Label(mStatusComposite, SWT.NONE);
        mStatusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mStatusLabel.setText(" \n "); //$NON-NLS-1$

        reloadTargetCombo();

        return top;
    }

    @Override
    protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
        if (id == IDialogConstants.OK_ID) {
            label = "Create AVD";
        }

        return super.createButton(parent, id, label, defaultButton);
    }

    @Override
    protected void okPressed() {
        if (onCreate()) {
            super.okPressed();
        }
    }

    private void onBrowseSdCard() {
        FileDialog dlg = new FileDialog(getContents().getShell(), SWT.OPEN);
        dlg.setText("Choose SD Card image file.");

        String fileName = dlg.open();
        if (fileName != null) {
            mSdCard.setText(fileName);
        }
    }

    private void reloadTargetCombo() {
        String selected = null;
        int index = mTargetCombo.getSelectionIndex();
        if (index >= 0) {
            selected = mTargetCombo.getItem(index);
        }

        mCurrentTargets.clear();
        mTargetCombo.removeAll();

        boolean found = false;
        index = -1;

        SdkManager sdkManager = mAvdManager.getSdkManager();
        if (sdkManager != null) {
            for (IAndroidTarget target : sdkManager.getTargets()) {
                String name;
                if (target.isPlatform()) {
                    name = String.format("%s - API Level %s",
                            target.getName(),
                            target.getVersion().getApiString());
                } else {
                    name = String.format("%s (%s) - API Level %s",
                            target.getName(),
                            target.getVendor(),
                            target.getVersion().getApiString());
                }
                mCurrentTargets.put(name, target);
                mTargetCombo.add(name);
                if (!found) {
                    index++;
                    found = name.equals(selected);
                }
            }
        }

        mTargetCombo.setEnabled(mCurrentTargets.size() > 0);

        if (found) {
            mTargetCombo.select(index);
        }

        reloadSkinCombo();
    }

    private void reloadSkinCombo() {
        String selected = null;
        int index = mSkinCombo.getSelectionIndex();
        if (index >= 0) {
            selected = mSkinCombo.getItem(index);
        }

        mSkinCombo.removeAll();
        mSkinCombo.setEnabled(false);

        index = mTargetCombo.getSelectionIndex();
        if (index >= 0) {

            String targetName = mTargetCombo.getItem(index);

            boolean found = false;
            IAndroidTarget target = mCurrentTargets.get(targetName);
            if (target != null) {
                mSkinCombo.add(String.format("Default (%s)", target.getDefaultSkin()));

                index = -1;
                for (String skin : target.getSkins()) {
                    mSkinCombo.add(skin);
                    if (!found) {
                        index++;
                        found = skin.equals(selected);
                    }
                }

                mSkinCombo.setEnabled(true);

                if (found) {
                    mSkinCombo.select(index);
                } else {
                    mSkinCombo.select(0);  // default
                }
            }
        }
    }

    /**
     * Validates the fields, displays errors and warnings.
     * Enables the finish button if there are no errors.
     */
    private void validatePage() {
        String error = null;

        // Validate AVD name
        String avdName = mAvdName.getText().trim();
        boolean hasAvdName = avdName.length() > 0;
        if (hasAvdName && !AvdManager.RE_AVD_NAME.matcher(avdName).matches()) {
            error = String.format(
                "AVD name '%1$s' contains invalid characters.\nAllowed characters are: %2$s",
                avdName, AvdManager.CHARS_AVD_NAME);
        }

        // Validate target
        if (hasAvdName && error == null && mTargetCombo.getSelectionIndex() < 0) {
            error = "A target must be selected in order to create an AVD.";
        }

        // Validate SDCard path or value
        if (error == null) {
            String sdName = mSdCard.getText().trim();

            if (sdName.length() > 0 &&
                    !new File(sdName).isFile() &&
                    !AvdManager.SDCARD_SIZE_PATTERN.matcher(sdName).matches()) {
                error = "SD Card must be either a file path or a size\nsuch as 128K or 64M.";
            }
        }

        // Check for duplicate AVD name
        if (hasAvdName && error == null) {
            AvdInfo avdMatch = mAvdManager.getAvd(avdName, false /*validAvdOnly*/);
            if (avdMatch != null && !mForceCreation.getSelection()) {
                error = String.format(
                        "The AVD name '%s' is already used.\n" +
                        "Check \"Force\" to override existing AVD.",
                        avdName);
            }
        }

        // Validate the create button
        boolean can_create = hasAvdName && error == null;
        if (can_create) {
            can_create &= mTargetCombo.getSelectionIndex() >= 0;
        }
        mOkButton.setEnabled(can_create);

        // -- update UI
        if (error != null) {
            mStatusIcon.setImage(mImageFactory.getImageByName("reject_icon16.png"));
            mStatusLabel.setText(error);
        } else {
            mStatusIcon.setImage(null);
            mStatusLabel.setText(" \n "); //$NON-NLS-1$
        }

        mStatusComposite.pack(true);
    }

    /**
     * Creates an AVD from the values in the UI. Called when the user presses the OK button.
     */
    private boolean onCreate() {
        String avdName = mAvdName.getText().trim();
        String sdName = mSdCard.getText().trim();
        int targetIndex = mTargetCombo.getSelectionIndex();
        int skinIndex = mSkinCombo.getSelectionIndex();
        boolean force = mForceCreation.getSelection();

        if (avdName.length() == 0 || targetIndex < 0) {
            return false;
        }

        String targetName = mTargetCombo.getItem(targetIndex);
        IAndroidTarget target = mCurrentTargets.get(targetName);
        if (target == null) {
            return false;
        }

        String skinName = null;
        if (skinIndex > 0) {
            // index 0 is the default, we don't use it
            skinName = mSkinCombo.getItem(skinIndex);
        }

        SdkLog log = new SdkLog(String.format("Result of creating AVD '%s':", avdName),
                getContents().getDisplay());

        File avdFolder;
        try {
            avdFolder = new File(
                    AndroidLocation.getFolder() + AndroidLocation.FOLDER_AVD,
                    avdName + AvdManager.AVD_FOLDER_EXTENSION);
        } catch (AndroidLocationException e) {
            return false;
        }

        boolean success = false;
        AvdInfo avdInfo = mAvdManager.createAvd(
                avdFolder,
                avdName,
                target,
                skinName,
                sdName,
                null, // hardwareConfig,
                force,
                log);

        success = avdInfo != null;

        log.displayResult(success);

        return success;
    }
}
