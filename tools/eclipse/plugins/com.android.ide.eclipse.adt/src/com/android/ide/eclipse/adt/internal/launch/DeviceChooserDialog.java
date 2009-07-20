/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ide.eclipse.adt.internal.launch;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.Client;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;
import com.android.ddmlib.IDevice.DeviceState;
import com.android.ddmuilib.IImageLoader;
import com.android.ddmuilib.ImageHelper;
import com.android.ddmuilib.TableHelper;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.ide.eclipse.ddms.DdmsPlugin;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.internal.avd.AvdManager.AvdInfo;
import com.android.sdkuilib.internal.widgets.AvdSelector;
import com.android.sdkuilib.internal.widgets.AvdSelector.DisplayMode;
import com.android.sdkuilib.internal.widgets.AvdSelector.IAvdFilter;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

/**
 * A dialog that lets the user choose a device to deploy an application.
 * The user can either choose an exiting running device (including running emulators)
 * or start a new emulator using an Android Virtual Device configuration that matches
 * the current project.
 */
public class DeviceChooserDialog extends Dialog implements IDeviceChangeListener {

    private final static int ICON_WIDTH = 16;

    private final static String PREFS_COL_SERIAL = "deviceChooser.serial"; //$NON-NLS-1$
    private final static String PREFS_COL_STATE  = "deviceChooser.state"; //$NON-NLS-1$
    private final static String PREFS_COL_AVD     = "deviceChooser.avd"; //$NON-NLS-1$
    private final static String PREFS_COL_TARGET = "deviceChooser.target"; //$NON-NLS-1$
    private final static String PREFS_COL_DEBUG  = "deviceChooser.debug"; //$NON-NLS-1$

    private Table mDeviceTable;
    private TableViewer mViewer;
    private AvdSelector mPreferredAvdSelector;

    private Image mDeviceImage;
    private Image mEmulatorImage;
    private Image mMatchImage;
    private Image mNoMatchImage;
    private Image mWarningImage;

    private final DeviceChooserResponse mResponse;
    private final String mPackageName;
    private final IAndroidTarget mProjectTarget;
    private final Sdk mSdk;

    private Button mDeviceRadioButton;

    private boolean mDisableAvdSelectionChange = false;

    /**
     * Basic Content Provider for a table full of {@link IDevice} objects. The input is
     * a {@link AndroidDebugBridge}.
     */
    private class ContentProvider implements IStructuredContentProvider {
        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof AndroidDebugBridge) {
                return ((AndroidDebugBridge)inputElement).getDevices();
            }

            return new Object[0];
        }

        public void dispose() {
            // pass
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // pass
        }
    }


    /**
     * A Label Provider for the {@link TableViewer} in {@link DeviceChooserDialog}.
     * It provides labels and images for {@link IDevice} objects.
     */
    private class LabelProvider implements ITableLabelProvider {

        public Image getColumnImage(Object element, int columnIndex) {
            if (element instanceof IDevice) {
                IDevice device = (IDevice)element;
                switch (columnIndex) {
                    case 0:
                        return device.isEmulator() ? mEmulatorImage : mDeviceImage;

                    case 2:
                        // check for compatibility.
                        if (device.isEmulator() == false) { // physical device
                            // get the version of the device
                            AndroidVersion deviceVersion = Sdk.getDeviceVersion(device);
                            if (deviceVersion == null) {
                                return mWarningImage;
                            } else {
                                if (deviceVersion.canRun(mProjectTarget.getVersion()) == false) {
                                    return mNoMatchImage;
                                }

                                // if the project is compiling against an add-on,
                                // the optional API may be missing from the device.
                                return mProjectTarget.isPlatform() ?
                                        mMatchImage : mWarningImage;
                            }
                        } else {
                            // get the AvdInfo
                            AvdInfo info = mSdk.getAvdManager().getAvd(device.getAvdName(),
                                    true /*validAvdOnly*/);
                            if (info == null) {
                                return mWarningImage;
                            }
                            return mProjectTarget.isCompatibleBaseFor(info.getTarget()) ?
                                    mMatchImage : mNoMatchImage;
                        }
                }
            }

            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            if (element instanceof IDevice) {
                IDevice device = (IDevice)element;
                switch (columnIndex) {
                    case 0:
                        return device.getSerialNumber();
                    case 1:
                        if (device.isEmulator()) {
                            return device.getAvdName();
                        } else {
                            return "N/A"; // devices don't have AVD names.
                        }
                    case 2:
                        if (device.isEmulator()) {
                            AvdInfo info = mSdk.getAvdManager().getAvd(device.getAvdName(),
                                    true /*validAvdOnly*/);
                            if (info == null) {
                                return "?";
                            }
                            return info.getTarget().getFullName();
                        } else {
                            String deviceBuild = device.getProperty(IDevice.PROP_BUILD_VERSION);
                            if (deviceBuild == null) {
                                return "unknown";
                            }
                            return deviceBuild;
                        }
                    case 3:
                        String debuggable = device.getProperty(IDevice.PROP_DEBUGGABLE);
                        if (debuggable != null && debuggable.equals("1")) { //$NON-NLS-1$
                            return "Yes";
                        } else {
                            return "";
                        }
                    case 4:
                        return getStateString(device);
                }
            }

            return null;
        }

        public void addListener(ILabelProviderListener listener) {
            // pass
        }

        public void dispose() {
            // pass
        }

        public boolean isLabelProperty(Object element, String property) {
            // pass
            return false;
        }

        public void removeListener(ILabelProviderListener listener) {
            // pass
        }
    }

    public static class DeviceChooserResponse {
        private AvdInfo mAvdToLaunch;
        private IDevice mDeviceToUse;

        public void setDeviceToUse(IDevice d) {
            mDeviceToUse = d;
            mAvdToLaunch = null;
        }

        public void setAvdToLaunch(AvdInfo avd) {
            mAvdToLaunch = avd;
            mDeviceToUse = null;
        }

        public IDevice getDeviceToUse() {
            return mDeviceToUse;
        }

        public AvdInfo getAvdToLaunch() {
            return mAvdToLaunch;
        }
    }

    public DeviceChooserDialog(Shell parent, DeviceChooserResponse response, String packageName,
            IAndroidTarget projectTarget) {
        super(parent);
        mResponse = response;
        mPackageName = packageName;
        mProjectTarget = projectTarget;
        mSdk = Sdk.getCurrent();

        AndroidDebugBridge.addDeviceChangeListener(this);
        loadImages();
    }

    private void cleanup() {
        // done listening.
        AndroidDebugBridge.removeDeviceChangeListener(this);

        mEmulatorImage.dispose();
        mDeviceImage.dispose();
        mMatchImage.dispose();
        mNoMatchImage.dispose();
        mWarningImage.dispose();
    }

    @Override
    protected void okPressed() {
        cleanup();
        super.okPressed();
    }

    @Override
    protected void cancelPressed() {
        cleanup();
        super.cancelPressed();
    }

    @Override
    protected Control createContents(Composite parent) {
        Control content = super.createContents(parent);

        // this must be called after createContents() has happened so that the
        // ok button has been created (it's created after the call to createDialogArea)
        updateDefaultSelection();

        return content;
    }


    @Override
    protected Control createDialogArea(Composite parent) {
        // set dialog title
        getShell().setText("Android Device Chooser");

        Composite top = new Composite(parent, SWT.NONE);
        top.setLayout(new GridLayout(1, true));

        Label label = new Label(top, SWT.NONE);
        label.setText(String.format("Select a device compatible with target %s.",
                mProjectTarget.getFullName()));

        mDeviceRadioButton = new Button(top, SWT.RADIO);
        mDeviceRadioButton.setText("Choose a running Android device");
        mDeviceRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean deviceMode = mDeviceRadioButton.getSelection();

                mDeviceTable.setEnabled(deviceMode);
                mPreferredAvdSelector.setEnabled(!deviceMode);

                if (deviceMode) {
                    handleDeviceSelection();
                } else {
                    mResponse.setAvdToLaunch(mPreferredAvdSelector.getSelected());
                }

                enableOkButton();
            }
        });
        mDeviceRadioButton.setSelection(true);


        // offset the selector from the radio button
        Composite offsetComp = new Composite(top, SWT.NONE);
        offsetComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout layout = new GridLayout(1, false);
        layout.marginRight = layout.marginHeight = 0;
        layout.marginLeft = 30;
        offsetComp.setLayout(layout);

        IPreferenceStore store = AdtPlugin.getDefault().getPreferenceStore();
        mDeviceTable = new Table(offsetComp, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
        GridData gd;
        mDeviceTable.setLayoutData(gd = new GridData(GridData.FILL_BOTH));
        gd.heightHint = 100;

        mDeviceTable.setHeaderVisible(true);
        mDeviceTable.setLinesVisible(true);

        TableHelper.createTableColumn(mDeviceTable, "Serial Number",
                SWT.LEFT, "AAA+AAAAAAAAAAAAAAAAAAA", //$NON-NLS-1$
                PREFS_COL_SERIAL, store);

        TableHelper.createTableColumn(mDeviceTable, "AVD Name",
                SWT.LEFT, "engineering", //$NON-NLS-1$
                PREFS_COL_AVD, store);

        TableHelper.createTableColumn(mDeviceTable, "Target",
                SWT.LEFT, "AAA+Android 9.9.9", //$NON-NLS-1$
                PREFS_COL_TARGET, store);

        TableHelper.createTableColumn(mDeviceTable, "Debug",
                SWT.LEFT, "Debug", //$NON-NLS-1$
                PREFS_COL_DEBUG, store);

        TableHelper.createTableColumn(mDeviceTable, "State",
                SWT.LEFT, "bootloader", //$NON-NLS-1$
                PREFS_COL_STATE, store);

        // create the viewer for it
        mViewer = new TableViewer(mDeviceTable);
        mViewer.setContentProvider(new ContentProvider());
        mViewer.setLabelProvider(new LabelProvider());
        mViewer.setInput(AndroidDebugBridge.getBridge());

        mDeviceTable.addSelectionListener(new SelectionAdapter() {
            /**
             * Handles single-click selection on the device selector.
             * {@inheritDoc}
             */
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleDeviceSelection();
            }

            /**
             * Handles double-click selection on the device selector.
             * Note that the single-click handler will probably already have been called.
             * {@inheritDoc}
             */
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                handleDeviceSelection();
                if (isOkButtonEnabled()) {
                    okPressed();
                }
            }
        });

        Button radio2 = new Button(top, SWT.RADIO);
        radio2.setText("Launch a new Android Virtual Device");

        // offset the selector from the radio button
        offsetComp = new Composite(top, SWT.NONE);
        offsetComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        layout = new GridLayout(1, false);
        layout.marginRight = layout.marginHeight = 0;
        layout.marginLeft = 30;
        offsetComp.setLayout(layout);

        mPreferredAvdSelector = new AvdSelector(offsetComp,
                mSdk.getSdkLocation(),
                mSdk.getAvdManager(),
                new NonRunningAvdFilter(),
                DisplayMode.SIMPLE_SELECTION);
        mPreferredAvdSelector.setTableHeightHint(100);
        mPreferredAvdSelector.setEnabled(false);
        mPreferredAvdSelector.setSelectionListener(new SelectionAdapter() {
            /**
             * Handles single-click selection on the AVD selector.
             * {@inheritDoc}
             */
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (mDisableAvdSelectionChange == false) {
                    mResponse.setAvdToLaunch(mPreferredAvdSelector.getSelected());
                    enableOkButton();
                }
            }

            /**
             * Handles double-click selection on the AVD selector.
             *
             * Note that the single-click handler will probably already have been called
             * but the selected item can have changed in between.
             *
             * {@inheritDoc}
             */
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
                if (isOkButtonEnabled()) {
                    okPressed();
                }
            }
        });


        return top;
    }

    private void loadImages() {
        IImageLoader ddmsLoader = DdmsPlugin.getImageLoader();
        Display display = DdmsPlugin.getDisplay();
        IImageLoader adtLoader = AdtPlugin.getImageLoader();

        if (mDeviceImage == null) {
            mDeviceImage = ImageHelper.loadImage(ddmsLoader, display,
                    "device.png", //$NON-NLS-1$
                    ICON_WIDTH, ICON_WIDTH,
                    display.getSystemColor(SWT.COLOR_RED));
        }
        if (mEmulatorImage == null) {
            mEmulatorImage = ImageHelper.loadImage(ddmsLoader, display,
                    "emulator.png", ICON_WIDTH, ICON_WIDTH, //$NON-NLS-1$
                    display.getSystemColor(SWT.COLOR_BLUE));
        }

        if (mMatchImage == null) {
            mMatchImage = ImageHelper.loadImage(adtLoader, display,
                    "match.png", //$NON-NLS-1$
                    ICON_WIDTH, ICON_WIDTH,
                    display.getSystemColor(SWT.COLOR_GREEN));
        }

        if (mNoMatchImage == null) {
            mNoMatchImage = ImageHelper.loadImage(adtLoader, display,
                    "error.png", //$NON-NLS-1$
                    ICON_WIDTH, ICON_WIDTH,
                    display.getSystemColor(SWT.COLOR_RED));
        }

        if (mWarningImage == null) {
            mWarningImage = ImageHelper.loadImage(adtLoader, display,
                    "warning.png", //$NON-NLS-1$
                    ICON_WIDTH, ICON_WIDTH,
                    display.getSystemColor(SWT.COLOR_YELLOW));
        }

    }

    /**
     * Returns a display string representing the state of the device.
     * @param d the device
     */
    private static String getStateString(IDevice d) {
        DeviceState deviceState = d.getState();
        if (deviceState == DeviceState.ONLINE) {
            return "Online";
        } else if (deviceState == DeviceState.OFFLINE) {
            return "Offline";
        } else if (deviceState == DeviceState.BOOTLOADER) {
            return "Bootloader";
        }

        return "??";
    }

    /**
     * Sent when the a device is connected to the {@link AndroidDebugBridge}.
     * <p/>
     * This is sent from a non UI thread.
     * @param device the new device.
     *
     * @see IDeviceChangeListener#deviceConnected(IDevice)
     */
    public void deviceConnected(IDevice device) {
        final DeviceChooserDialog dialog = this;
        exec(new Runnable() {
            public void run() {
                if (mDeviceTable.isDisposed() == false) {
                    // refresh all
                    mViewer.refresh();

                    // update the selection
                    updateDefaultSelection();

                    // update the display of AvdInfo (since it's filtered to only display
                    // non running AVD.)
                    refillAvdList(false /*reloadAvds*/);
                } else {
                    // table is disposed, we need to do something.
                    // lets remove ourselves from the listener.
                    AndroidDebugBridge.removeDeviceChangeListener(dialog);
                }

            }
        });
    }

    /**
     * Sent when the a device is connected to the {@link AndroidDebugBridge}.
     * <p/>
     * This is sent from a non UI thread.
     * @param device the new device.
     *
     * @see IDeviceChangeListener#deviceDisconnected(IDevice)
     */
    public void deviceDisconnected(IDevice device) {
        deviceConnected(device);
    }

    /**
     * Sent when a device data changed, or when clients are started/terminated on the device.
     * <p/>
     * This is sent from a non UI thread.
     * @param device the device that was updated.
     * @param changeMask the mask indicating what changed.
     *
     * @see IDeviceChangeListener#deviceChanged(IDevice, int)
     */
    public void deviceChanged(final IDevice device, int changeMask) {
        if ((changeMask & (IDevice.CHANGE_STATE | IDevice.CHANGE_BUILD_INFO)) != 0) {
            final DeviceChooserDialog dialog = this;
            exec(new Runnable() {
                public void run() {
                    if (mDeviceTable.isDisposed() == false) {
                        // refresh the device
                        mViewer.refresh(device);

                        // update the defaultSelection.
                        updateDefaultSelection();

                        // update the display of AvdInfo (since it's filtered to only display
                        // non running AVD). This is done on deviceChanged because the avd name
                        // of a (emulator) device may be updated as the emulator boots.

                        refillAvdList(false /*reloadAvds*/);

                        // if the changed device is the current selection,
                        // we update the OK button based on its state.
                        if (device == mResponse.getDeviceToUse()) {
                            enableOkButton();
                        }

                    } else {
                        // table is disposed, we need to do something.
                        // lets remove ourselves from the listener.
                        AndroidDebugBridge.removeDeviceChangeListener(dialog);
                    }
                }
            });
        }
    }

    /**
     * Returns whether the dialog is in "device" mode (true), or in "avd" mode (false).
     */
    private boolean isDeviceMode() {
        return mDeviceRadioButton.getSelection();
    }

    /**
     * Enables or disables the OK button of the dialog based on various selections in the dialog.
     */
    private void enableOkButton() {
        Button okButton = getButton(IDialogConstants.OK_ID);

        if (isDeviceMode()) {
            okButton.setEnabled(mResponse.getDeviceToUse() != null &&
                    mResponse.getDeviceToUse().isOnline());
        } else {
            okButton.setEnabled(mResponse.getAvdToLaunch() != null);
        }
    }

    /**
     * Returns true if the ok button is enabled.
     */
    private boolean isOkButtonEnabled() {
        Button okButton = getButton(IDialogConstants.OK_ID);
        return okButton.isEnabled();
    }

    /**
     * Executes the {@link Runnable} in the UI thread.
     * @param runnable the runnable to execute.
     */
    private void exec(Runnable runnable) {
        try {
            Display display = mDeviceTable.getDisplay();
            display.asyncExec(runnable);
        } catch (SWTException e) {
            // tree is disposed, we need to do something. lets remove ourselves from the listener.
            AndroidDebugBridge.removeDeviceChangeListener(this);
        }
    }

    private void handleDeviceSelection() {
        int count = mDeviceTable.getSelectionCount();
        if (count != 1) {
            handleSelection(null);
        } else {
            int index = mDeviceTable.getSelectionIndex();
            Object data = mViewer.getElementAt(index);
            if (data instanceof IDevice) {
                handleSelection((IDevice)data);
            } else {
                handleSelection(null);
            }
        }
    }

    private void handleSelection(IDevice device) {
        mResponse.setDeviceToUse(device);
        enableOkButton();
    }

    /**
     * Look for a default device to select. This is done by looking for the running
     * clients on each device and finding one similar to the one being launched.
     * <p/>
     * This is done every time the device list changed unless there is a already selection.
     */
    private void updateDefaultSelection() {
        if (mDeviceTable.getSelectionCount() == 0) {
            AndroidDebugBridge bridge = AndroidDebugBridge.getBridge();

            IDevice[] devices = bridge.getDevices();

            for (IDevice device : devices) {
                Client[] clients = device.getClients();

                for (Client client : clients) {

                    if (mPackageName.equals(client.getClientData().getClientDescription())) {
                        // found a match! Select it.
                        mViewer.setSelection(new StructuredSelection(device));
                        handleSelection(device);

                        // and we're done.
                        return;
                    }
                }
            }
        }

        handleDeviceSelection();
    }

    private final class NonRunningAvdFilter implements IAvdFilter {

        private IDevice[] mDevices;

        public void prepare() {
            mDevices = AndroidDebugBridge.getBridge().getDevices();
        }

        public boolean accept(AvdInfo avd) {
            if (mDevices != null) {
                for (IDevice d : mDevices) {
                    if (mProjectTarget.isCompatibleBaseFor(avd.getTarget()) == false ||
                            avd.getName().equals(d.getAvdName())) {
                        return false;
                    }
                }
            }

            return true;
        }

        public void cleanup() {
            mDevices = null;
        }
    }

    /**
     * Refills the AVD list keeping the current selection.
     */
    private void refillAvdList(boolean reloadAvds) {
        // save the current selection
        AvdInfo selected = mPreferredAvdSelector.getSelected();

        // disable selection change.
        mDisableAvdSelectionChange = true;

        // refresh the list
        mPreferredAvdSelector.refresh(false);

        // attempt to reselect the proper avd if needed
        if (selected != null) {
            if (mPreferredAvdSelector.setSelection(selected) == false) {
                // looks like the selection is lost. this can happen if an emulator
                // running the AVD that was selected was launched from outside of Eclipse).
                mResponse.setAvdToLaunch(null);
                enableOkButton();
            }
        }

        // enable the selection change
        mDisableAvdSelectionChange = false;
    }
}

