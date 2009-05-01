/*
 * Copyright (C) 2009 The Android Open Source Project
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


package com.android.ide.eclipse.adt.wizards.avdmanager;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.sdk.Sdk;
import com.android.ide.eclipse.adt.sdk.Sdk.ITargetChangeListener;
import com.android.prefs.AndroidLocation;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.ISdkLog;
import com.android.sdklib.avd.AvdManager;
import com.android.sdklib.avd.AvdManager.AvdInfo;
import com.android.sdkuilib.AvdSelector;
import com.android.sdkuilib.AvdSelector.SelectionMode;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;

/**
 * This is the single page of the {@link AvdManagerWizard} which provides the ability to display
 * the AVDs and edit them quickly.
 */
class AvdManagerListPage extends WizardPage {

    private AvdSelector mAvdSelector;
    private Button mRefreshButton;
    private Text mCreateName;
    private Combo mCreateTargetCombo;
    private Text mCreateSdCard;
    private Combo mCreateSkinCombo;
    private Button mCreateForce;
    private Button mCreateButton;
    private HashSet<String> mKnownAvdNames = new HashSet<String>();
    private TreeMap<String, IAndroidTarget> mCurrentTargets = new TreeMap<String, IAndroidTarget>();
    private ITargetChangeListener mSdkTargetChangeListener;
    
    /**
     * Constructs a new {@link AvdManagerListPage}.
     * <p/>
     * Called by {@link AvdManagerWizard#createMainPage()}.
     */
    protected AvdManagerListPage(String pageName) {
        super(pageName);
        setPageComplete(false);
    }

    /**
     * Called by the parent Wizard to create the UI for this Wizard Page.
     * 
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        composite.setFont(parent.getFont());

        initializeDialogUnits(parent);

        composite.setLayout(new GridLayout(1, false /*makeColumnsEqualWidth*/));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        createAvdGroup(composite);
        createCreateGroup(composite);
        registerSdkChangeListener();

        // Show description the first time
        setErrorMessage(null);
        setMessage(null);
        setControl(composite);

        // Update state the first time
        reloadAvdList();
        reloadTargetCombo();
        validatePage();
    }
    
    private void registerSdkChangeListener() {

        mSdkTargetChangeListener = new ITargetChangeListener() {
            public void onProjectTargetChange(IProject changedProject) {
                // Ignore
            }

            public void onTargetsLoaded() {
                // Update the AVD list, since the SDK change will influence the "good" avd list
                reloadAvdList();
                // Update the sdk target combo with the new targets
                reloadTargetCombo();
                validatePage();
            }
        };        
        AdtPlugin.getDefault().addTargetListener(mSdkTargetChangeListener);
        
    }

    @Override
    public void dispose() {
        if (mSdkTargetChangeListener != null) {
            AdtPlugin.getDefault().removeTargetListener(mSdkTargetChangeListener);
            mSdkTargetChangeListener = null;
        }
        
        super.dispose();
    }

    // --- UI creation ---

    /**
     * Creates the AVD selector and refresh & delete buttons.
     */
    private void createAvdGroup(Composite parent) {
        final Composite grid2 = new Composite(parent, SWT.NONE);
        grid2.setLayout(new GridLayout(2,  false /*makeColumnsEqualWidth*/));
        grid2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        Label label = new Label(grid2, SWT.NONE);
        label.setText("List of existing Android Virtual Devices:");
        label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));

        mRefreshButton = new Button(grid2, SWT.PUSH);
        mRefreshButton.setText("Refresh");
        mRefreshButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        mRefreshButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                reloadAvdList();
           }
        });

        mAvdSelector = new AvdSelector(parent,
                SelectionMode.SELECT,
                new AvdSelector.IExtraAction() {
                    public String label() {
                        return "Delete AVD...";
                    }

                    public boolean isEnabled() {
                        return mAvdSelector != null && mAvdSelector.getSelected() != null;
                    }

                    public void run() {
                        onDelete();
                    }
            });
    }
    
    /**
     * Creates the "Create" group
     */
    private void createCreateGroup(Composite parent) {
        
        Group grid = new Group(parent, SWT.SHADOW_ETCHED_IN);
        grid.setLayout(new GridLayout(4,  false /*makeColumnsEqualWidth*/));
        grid.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        grid.setFont(parent.getFont());
        grid.setText("Create AVD");

        // first line

        Label label = new Label(grid, SWT.NONE);
        label.setText("Name");
        
        mCreateName = new Text(grid, SWT.BORDER);
        mCreateName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mCreateName.addModifyListener(new CreateNameModifyListener());

        label = new Label(grid, SWT.NONE);
        label.setText("Target");
        
        mCreateTargetCombo = new Combo(grid, SWT.READ_ONLY | SWT.DROP_DOWN);
        mCreateTargetCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mCreateTargetCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                reloadSkinCombo();
                validatePage();
            } 
        });

        // second line
        
        label = new Label(grid, SWT.NONE);
        label.setText("SDCard");
        
        ValidateListener validateListener = new ValidateListener();
        
        mCreateSdCard = new Text(grid, SWT.BORDER);
        mCreateSdCard.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mCreateSdCard.addModifyListener(validateListener);

        label = new Label(grid, SWT.NONE);
        label.setText("Skin");
        
        mCreateSkinCombo = new Combo(grid, SWT.READ_ONLY | SWT.DROP_DOWN);
        mCreateSkinCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        // dummies for alignment
        label = new Label(grid, SWT.NONE);
        label = new Label(grid, SWT.NONE);        
        
        mCreateForce = new Button(grid, SWT.CHECK);
        mCreateForce.setText("Force");
        mCreateForce.setEnabled(false);
        mCreateForce.addSelectionListener(validateListener);

        mCreateButton = new Button(grid, SWT.PUSH);
        mCreateButton.setText("Create AVD");
        mCreateButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        mCreateButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                onCreate();
            }
        });
    }
    
    /**
     * Callback when the AVD name is changed.
     * Enables the force checkbox if the name is a duplicate.
     */
    private class CreateNameModifyListener implements ModifyListener {

        public void modifyText(ModifyEvent e) {
            String name = mCreateName.getText().trim();
            if (mKnownAvdNames.contains(name)) {
                mCreateForce.setEnabled(true);
            } else {
                mCreateForce.setEnabled(false);
                mCreateForce.setSelection(false);
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
    
    private void reloadTargetCombo() {
        
        String selected = null;
        int index = mCreateTargetCombo.getSelectionIndex();
        if (index >= 0) {
            selected = mCreateTargetCombo.getItem(index);
        }
        
        mCurrentTargets.clear();
        mCreateTargetCombo.removeAll();

        boolean found = false;
        index = -1;
        
        Sdk sdk = Sdk.getCurrent();
        if (sdk != null) {
            for (IAndroidTarget target : sdk.getTargets()) {
                String name = String.format("%s - %s",
                        target.getName(),
                        target.getApiVersionName());
                mCurrentTargets.put(name, target);
                mCreateTargetCombo.add(name);
                if (!found) {
                    index++;
                    found = name.equals(selected);
                }
            }
        }

        mCreateTargetCombo.setEnabled(mCurrentTargets.size() > 0);
        
        if (found) {
            mCreateTargetCombo.select(index);
        }
        
        reloadSkinCombo();
    }

    private void reloadSkinCombo() {
        String selected = null;
        int index = mCreateSkinCombo.getSelectionIndex();
        if (index >= 0) {
            selected = mCreateSkinCombo.getItem(index);
        }
        
        mCreateSkinCombo.removeAll();
        mCreateSkinCombo.setEnabled(false);
        
        index = mCreateTargetCombo.getSelectionIndex();
        if (index >= 0) {
            
            String targetName = mCreateTargetCombo.getItem(index);

            boolean found = false;
            IAndroidTarget target = mCurrentTargets.get(targetName);
            if (target != null) {
                mCreateSkinCombo.add(String.format("Default (%s)", target.getDefaultSkin()));

                index = -1;
                for (String skin : target.getSkins()) {
                    mCreateSkinCombo.add(skin);
                    if (!found) {
                        index++;
                        found = skin.equals(selected);
                    }
                }

                mCreateSkinCombo.setEnabled(true);

                if (found) {
                    mCreateSkinCombo.select(index);
                } else {
                    mCreateSkinCombo.select(0);  // default
                }
            }
        }
    }

    /**
     * Validates the fields, displays errors and warnings.
     * Enables the finish button if there are no errors.
     * <p/>
     * Not really used here yet. Keep as a placeholder.
     */
    private void validatePage() {
        String error = null;
        String warning = null;


        // Validate AVD name 
        String avdName = mCreateName.getText().trim();
        boolean hasAvdName = avdName.length() > 0;
        if (hasAvdName && !AvdManager.RE_AVD_NAME.matcher(avdName).matches()) {
            error = String.format(
                "AVD name '%1$s' contains invalid characters. Allowed characters are: %2$s",
                avdName, AvdManager.CHARS_AVD_NAME);
        }
        
        // Validate target
        if (hasAvdName && error == null && mCreateTargetCombo.getSelectionIndex() < 0) {
            error = "A target must be selected in order to create an AVD.";
        }

        // Validate SDCard path or value
        if (error == null) {
            String sdName = mCreateSdCard.getText().trim();
            
            if (sdName.length() > 0 &&
                    !new File(sdName).isFile() &&
                    !AvdManager.SDCARD_SIZE_PATTERN.matcher(sdName).matches()) {
                error = "SD Card must be either a file path or a size such as 128K or 64M.";                
            }
        }
        
        // Check for duplicate AVD name
        if (hasAvdName && error == null) {
            if (mKnownAvdNames.contains(avdName) && !mCreateForce.getSelection()) {
                error = String.format(
                        "The AVD name '%s' is already used. " +
                        "Check \"Force\" if you really want to create a new AVD with that name and delete the previous one.",
                        avdName);
            }
        }
        
        // Validate the create button
        boolean can_create = hasAvdName && error == null;
        if (can_create) {
            can_create &= mCreateTargetCombo.getSelectionIndex() >= 0;
        }
        mCreateButton.setEnabled(can_create);

        // -- update UI
        setPageComplete(true);
        if (error != null) {
            setMessage(error, WizardPage.ERROR);
        } else if (warning != null) {
            setMessage(warning, WizardPage.WARNING);
        } else {
            setErrorMessage(null);
            setMessage(null);
        }
    }

    /**
     * Reloads the AVD list in the AVD selector.
     * Tries to preserve the selection.
     */
    private void reloadAvdList() {
        AvdInfo selected = mAvdSelector.getSelected();

        AvdManager avdm = getAvdManager();
        AvdInfo[] avds = null;
        
        // For the AVD manager to reload the list, in case AVDs where created using the
        // command line tool.
        // The AVD manager may not exist yet, typically when loading the SDK.
        if (avdm != null) {
            try {
                avdm.reloadAvds();
            } catch (AndroidLocationException e) {
                AdtPlugin.log(e, "AVD Manager reload failed");  //$NON-NLS-1$
            }

            avds = avdm.getValidAvds();
        }
        
        mAvdSelector.setAvds(avds, null /*filter*/);

        // Keep the list of known AVD names to check if they exist quickly. however
        // use the list of all AVDs, including broken ones (unless we don't know their
        // name).
        mKnownAvdNames.clear();
        if (avdm != null) {
            for (AvdInfo avd : avdm.getAllAvds()) {
                String name = avd.getName();
                if (name != null) {
                    mKnownAvdNames.add(name);
                }
            }
        }
        
        mAvdSelector.setSelection(selected);
    }

    /**
     * Triggered when the user selects the "delete" button (the extra action in the selector)
     * Deletes the currently selected AVD, if any.
     */
    private void onDelete() {
        AvdInfo avdInfo = mAvdSelector.getSelected();
        AvdManager avdm = getAvdManager();
        if (avdInfo == null || avdm == null) {
            return;
        }
        
        // Confirm you want to delete this AVD
        if (!AdtPlugin.displayPrompt("Delete Android Virtual Device",
                String.format("Please confirm that you want to delete the Android Virtual Device named '%s'. This operation cannot be reverted.",
                        avdInfo.getName()))) {
            return;
        }

        SdkLog log = new SdkLog(String.format("Result of deleting AVD '%s':", avdInfo.getName()));
        
        boolean success = avdm.deleteAvd(avdInfo, log);
        
        log.display(success);
        reloadAvdList();
    }

    /**
     * Triggered when the user selects the "create" button.
     */
    private void onCreate() {
        String avdName = mCreateName.getText().trim();
        String sdName = mCreateSdCard.getText().trim();
        int targetIndex = mCreateTargetCombo.getSelectionIndex();
        int skinIndex = mCreateSkinCombo.getSelectionIndex();
        boolean force = mCreateForce.getSelection();
        AvdManager avdm = getAvdManager();

        if (avdm == null ||
                avdName.length() == 0 ||
                targetIndex < 0) {
            return;
        }

        String targetName = mCreateTargetCombo.getItem(targetIndex);
        IAndroidTarget target = mCurrentTargets.get(targetName);
        if (target == null) {
            return;
        }
        
        String skinName = null;
        if (skinIndex > 0) {
            // index 0 is the default, we don't use it
            skinName = mCreateSkinCombo.getItem(skinIndex);
        }
        
        SdkLog log = new SdkLog(String.format("Result of creating AVD '%s':", avdName));

        File avdFolder;
        try {
            avdFolder = new File(
                    AndroidLocation.getFolder() + AndroidLocation.FOLDER_AVD,
                    avdName + AvdManager.AVD_FOLDER_EXTENSION);
        } catch (AndroidLocationException e) {
            AdtPlugin.logAndPrintError(e, null /*tag*/,
                    "AndroidLocation.getFolder failed");  //$NON-NLS-1$
            return;
        }

        ISdkLog oldLog = null;
        boolean success = false;
        try {
            // Temporarily change the AvdManager's logger for ours, since the API no longer
            // takes a logger argument.
            // TODO revisit this later. See comments in AvdManager#mSdkLog.
            oldLog = avdm.setSdkLog(log);
            
            AvdInfo avdInfo = avdm.createAvd(
                    avdFolder, 
                    avdName,
                    target,
                    skinName,
                    sdName,
                    null, // hardwareConfig,
                    force);

            success = avdInfo != null;
            
        } finally {
            avdm.setSdkLog(oldLog);
        }
                
        log.display(success);
        
        if (success) {
            // clear the name field on success
            mCreateName.setText("");  //$NON-NLS-1$
        }

        reloadAvdList();
    }

    /**
     * Collects all log from the AVD action and displays it in a dialog.
     */
    private class SdkLog implements ISdkLog {
        
        final ArrayList<String> logMessages = new ArrayList<String>();
        private final String mMessage;

        public SdkLog(String message) {
            mMessage = message;
        }
        
        public void error(Throwable throwable, String errorFormat, Object... arg) {
            if (errorFormat != null) {
                logMessages.add(String.format("Error: " + errorFormat, arg));
            }
            
            if (throwable != null) {
                logMessages.add(throwable.getMessage());
            }
        }

        public void warning(String warningFormat, Object... arg) {
            logMessages.add(String.format("Warning: " + warningFormat, arg));
        }
        
        public void printf(String msgFormat, Object... arg) {
            logMessages.add(String.format(msgFormat, arg));
        }

        /**
         * Displays the log if anything was captured.
         */
        public void display(boolean success) {
            if (logMessages.size() > 0) {
                StringBuilder sb = new StringBuilder(mMessage + "\n");
                for (String msg : logMessages) {
                    sb.append('\n');
                    sb.append(msg);
                }
                if (success) {
                    AdtPlugin.displayWarning("Android Virtual Devices Manager", sb.toString());
                } else {
                    AdtPlugin.displayError("Android Virtual Devices Manager", sb.toString());
                }
            }
        }        
    }

    /**
     * Returns the current AVD Manager or null if none has been created yet.
     * This can happen when the SDK hasn't finished loading or the manager failed to
     * parse the AVD directory.
     */
    private AvdManager getAvdManager() {
        Sdk sdk = Sdk.getCurrent();
        if (sdk != null) {
            return sdk.getAvdManager();
        }
        return null;
    }
}
