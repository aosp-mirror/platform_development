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

package com.android.ide.eclipse.adt.debug.launching;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.Client;
import com.android.ddmlib.Device;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;
import com.android.ddmlib.Device.DeviceState;
import com.android.ddmuilib.IImageLoader;
import com.android.ddmuilib.ImageHelper;
import com.android.ddmuilib.TableHelper;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.debug.launching.AndroidLaunchController.AndroidLaunchConfiguration;
import com.android.ide.eclipse.adt.debug.launching.AndroidLaunchController.DelayedLaunchInfo;
import com.android.ide.eclipse.adt.sdk.Sdk;
import com.android.ide.eclipse.ddms.DdmsPlugin;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.vm.VmManager.VmInfo;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
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
import org.eclipse.swt.widgets.Table;

public class DeviceChooserDialog extends Dialog implements IDeviceChangeListener {

    private final static int DLG_WIDTH = 500;
    private final static int DLG_HEIGHT = 300;
    private final static int ICON_WIDTH = 16;

    private final static String PREFS_COL_SERIAL = "deviceChooser.serial"; //$NON-NLS-1$
    private final static String PREFS_COL_STATE = "deviceChooser.state"; //$NON-NLS-1$
    private final static String PREFS_COL_VM = "deviceChooser.vm"; //$NON-NLS-1$
    private final static String PREFS_COL_TARGET = "deviceChooser.target"; //$NON-NLS-1$
    private final static String PREFS_COL_DEBUG = "deviceChooser.debug"; //$NON-NLS-1$

    private Table mDeviceTable;
    private TableViewer mViewer;
    
    private Image mDeviceImage;
    private Image mEmulatorImage;
    private Image mMatchImage;
    private Image mNoMatchImage;
    private Image mWarningImage;

    private Button mOkButton;
    private Button mCreateButton;
    
    private DeviceChooserResponse mResponse;
    private DelayedLaunchInfo mLaunchInfo;
    private IAndroidTarget mProjectTarget;
    private Sdk mSdk;
    
    /**
     * Basic Content Provider for a table full of {@link Device} objects. The input is
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
     * It provides labels and images for {@link Device} objects.
     */
    private class LabelProvider implements ITableLabelProvider {

        public Image getColumnImage(Object element, int columnIndex) {
            if (element instanceof Device) {
                Device device = (Device)element;
                switch (columnIndex) {
                    case 0:
                        return device.isEmulator() ? mEmulatorImage : mDeviceImage;
                        
                    case 2:
                        // check for compatibility.
                        if (device.isEmulator() == false) { // physical device
                            // get the api level of the device
                            try {
                                String apiValue = device.getProperty(
                                        IDevice.PROP_BUILD_VERSION_NUMBER);
                                int api = Integer.parseInt(apiValue);
                                if (api >= mProjectTarget.getApiVersionNumber()) {
                                    // if the project is compiling against an add-on, the optional
                                    // API may be missing from the device.
                                    return mProjectTarget.isPlatform() ?
                                            mMatchImage : mWarningImage;
                                } else {
                                    return mNoMatchImage;
                                }
                            } catch (NumberFormatException e) {
                                // lets consider the device non compatible
                                return mNoMatchImage;
                            }
                        } else {
                            // get the VmInfo
                            VmInfo info = mSdk.getVmManager().getVm(device.getVmName());
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
            if (element instanceof Device) {
                Device device = (Device)element;
                switch (columnIndex) {
                    case 0:
                        return device.getSerialNumber();
                    case 1:
                        if (device.isEmulator()) {
                            return device.getVmName();
                        } else {
                            return "N/A"; // devices don't have VM names.
                        }
                    case 2:
                        if (device.isEmulator()) {
                            VmInfo info = mSdk.getVmManager().getVm(device.getVmName());
                            if (info == null) {
                                return "?";
                            }
                            return info.getTarget().getFullName();
                        } else {
                            return device.getProperty(IDevice.PROP_BUILD_VERSION);
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
        public boolean mustContinue;
        public boolean mustLaunchEmulator;
        public VmInfo vmToLaunch;
        public Device deviceToUse;
    }
    
    public DeviceChooserDialog(Shell parent) {
        super(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    }

    /**
     * Prepare and display the dialog.
     * @param response
     * @param project 
     * @param projectTarget 
     * @param launch 
     * @param launchInfo 
     * @param config 
     */
    public void open(DeviceChooserResponse response, IProject project,
            IAndroidTarget projectTarget, AndroidLaunch launch, DelayedLaunchInfo launchInfo,
            AndroidLaunchConfiguration config) {
        mResponse = response;
        mProjectTarget = projectTarget;
        mLaunchInfo = launchInfo;
        mSdk = Sdk.getCurrent();

        Shell parent = getParent();
        Shell shell = new Shell(parent, getStyle());
        shell.setText("Device Chooser");

        loadImages();
        createContents(shell);
        
        // Set the dialog size.
        shell.setMinimumSize(DLG_WIDTH, DLG_HEIGHT);
        Rectangle r = parent.getBounds();
        // get the center new top left.
        int cx = r.x + r.width/2;
        int x = cx - DLG_WIDTH / 2;
        int cy = r.y + r.height/2;
        int y = cy - DLG_HEIGHT / 2;
        shell.setBounds(x, y, DLG_WIDTH, DLG_HEIGHT);
        
        shell.pack();
        shell.open();
        
        // start the listening.
        AndroidDebugBridge.addDeviceChangeListener(this);

        Display display = parent.getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
        
        // done listening.
        AndroidDebugBridge.removeDeviceChangeListener(this);

        mEmulatorImage.dispose();
        mDeviceImage.dispose();
        mMatchImage.dispose();
        mNoMatchImage.dispose();
        mWarningImage.dispose();

        AndroidLaunchController.getInstance().continueLaunch(response, project, launch,
                launchInfo, config);
    }
    
    /**
     * Create the device chooser dialog contents.
     * @param shell the parent shell.
     */
    private void createContents(final Shell shell) {
        shell.setLayout(new GridLayout(1, true));

        shell.addListener(SWT.Close, new Listener() {
            public void handleEvent(Event event) {
                event.doit = true;
            }
        });
        
        Label l = new Label(shell, SWT.NONE);
        l.setText("Select the target device.");

        IPreferenceStore store = AdtPlugin.getDefault().getPreferenceStore(); 
        mDeviceTable = new Table(shell, SWT.SINGLE | SWT.FULL_SELECTION);
        mDeviceTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        mDeviceTable.setHeaderVisible(true);
        mDeviceTable.setLinesVisible(true);

        TableHelper.createTableColumn(mDeviceTable, "Serial Number",
                SWT.LEFT, "AAA+AAAAAAAAAAAAAAAAAAA", //$NON-NLS-1$
                PREFS_COL_SERIAL, store);

        TableHelper.createTableColumn(mDeviceTable, "VM Name",
                SWT.LEFT, "engineering", //$NON-NLS-1$
                PREFS_COL_VM, store);

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
        mViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                ISelection selection = event.getSelection();
                if (selection instanceof IStructuredSelection) {
                    IStructuredSelection structuredSelection = (IStructuredSelection)selection;
                    Object object = structuredSelection.getFirstElement();
                    if (object instanceof Device) {
                        Device selectedDevice = (Device)object;
                        
                        mResponse.deviceToUse = selectedDevice;
                        mResponse.mustContinue = true;
                        shell.close();
                    }
                }
            }
        });

        // bottom part with the ok/cancel
        Composite bottomComp = new Composite(shell, SWT.NONE);
        bottomComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        // 3 items in the layout: createButton, spacer, composite with ok/cancel
        // (to force same width).
        bottomComp.setLayout(new GridLayout(3 /* numColums */, false /* makeColumnsEqualWidth */));
        
        mCreateButton = new Button(bottomComp, SWT.NONE);
        mCreateButton.setText("Launch Emulator");
        mCreateButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mResponse.mustContinue = true;
                mResponse.mustLaunchEmulator = true;
                shell.close();
            }
        });
        
        // the spacer
        Composite spacer = new Composite(bottomComp, SWT.NONE);
        GridData gd;
        spacer.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.heightHint = 0;
        
        // the composite to contain ok/cancel
        Composite buttonContainer = new Composite(bottomComp, SWT.NONE);
        GridLayout gl = new GridLayout(2 /* numColums */, true /* makeColumnsEqualWidth */);
        gl.marginHeight = gl.marginWidth = 0;
        buttonContainer.setLayout(gl);

        mOkButton = new Button(buttonContainer, SWT.NONE);
        mOkButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mOkButton.setEnabled(false);
        mOkButton.setText("OK");
        mOkButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mResponse.mustContinue = true;
                shell.close();
            }
        });

        Button cancelButton = new Button(buttonContainer, SWT.NONE);
        cancelButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        cancelButton.setText("Cancel");
        cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mResponse.mustContinue = false;
                shell.close();
            }
        });

        mDeviceTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int count = mDeviceTable.getSelectionCount();
                if (count != 1) {
                    handleSelection(null);
                } else {
                    int index = mDeviceTable.getSelectionIndex();
                    Object data = mViewer.getElementAt(index);
                    if (data instanceof Device) {
                        handleSelection((Device)data);
                    } else {
                        handleSelection(null);
                    }
                }
            }
        });
        
        mDeviceTable.setFocus();
        shell.setDefaultButton(mOkButton);
        
        updateDefaultSelection();
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
    private static String getStateString(Device d) {
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
     * @see IDeviceChangeListener#deviceConnected(Device)
     */
    public void deviceConnected(Device device) {
        final DeviceChooserDialog dialog = this;
        exec(new Runnable() {
            public void run() {
                if (mDeviceTable.isDisposed() == false) {
                    // refresh all
                    mViewer.refresh();
                    
                    // update the selection
                    updateDefaultSelection();
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
     * @see IDeviceChangeListener#deviceDisconnected(Device)
     */
    public void deviceDisconnected(Device device) {
        deviceConnected(device);
    }

    /**
     * Sent when a device data changed, or when clients are started/terminated on the device.
     * <p/>
     * This is sent from a non UI thread.
     * @param device the device that was updated.
     * @param changeMask the mask indicating what changed.
     * 
     * @see IDeviceChangeListener#deviceChanged(Device, int)
     */
    public void deviceChanged(final Device device, int changeMask) {
        if ((changeMask & (Device.CHANGE_STATE | Device.CHANGE_BUILD_INFO)) != 0) {
            final DeviceChooserDialog dialog = this;
            exec(new Runnable() {
                public void run() {
                    if (mDeviceTable.isDisposed() == false) {
                        // refresh the device
                        mViewer.refresh(device);
                        
                        // update the defaultSelection.
                        updateDefaultSelection();
                        
                        // if the changed device is the current selection,
                        // we update the OK button based on its state.
                        if (device == mResponse.deviceToUse) {
                            mOkButton.setEnabled(mResponse.deviceToUse.isOnline());
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
    
    private void handleSelection(Device device) {
        mResponse.deviceToUse = device;
        mOkButton.setEnabled(device != null && mResponse.deviceToUse.isOnline());
    }
    
    /**
     * Look for a default device to select. This is done by looking for the running
     * clients on each device and finding one similar to the one being launched.
     * <p/>
     * This is done every time the device list changed, until there is a selection..
     */
    private void updateDefaultSelection() {
        if (mDeviceTable.getSelectionCount() == 0) {
            AndroidDebugBridge bridge = AndroidDebugBridge.getBridge();
            
            Device[] devices = bridge.getDevices();
            
            for (Device device : devices) {
                Client[] clients = device.getClients();
                
                for (Client client : clients) {
                    
                    if (mLaunchInfo.mPackageName.equals(
                            client.getClientData().getClientDescription())) {
                        // found a match! Select it.
                        mViewer.setSelection(new StructuredSelection(device));
                        handleSelection(device);
                        
                        // and we're done.
                        return;
                    }
                }
            }
        }
    }

}
