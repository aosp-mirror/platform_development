/*
 * Copyright (C) 2008 The Android Open Source Project
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

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.Client;
import com.android.ddmlib.ClientData;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.SyncService;
import com.android.ddmlib.AndroidDebugBridge.IClientChangeListener;
import com.android.ddmlib.ClientData.IHprofDumpHandler;
import com.android.ddmlib.ClientData.MethodProfilingStatus;
import com.android.ddmlib.SyncService.SyncResult;
import com.android.ddmuilib.handler.BaseFileHandler;
import com.android.ddmuilib.handler.MethodProfilingHandler;
import com.android.ddmuilib.DevicePanel;
import com.android.ddmuilib.ScreenShotDialog;
import com.android.ddmuilib.DevicePanel.IUiSelectionListener;
import com.android.ide.eclipse.ddms.DdmsPlugin;
import com.android.ide.eclipse.ddms.DdmsPlugin.IDebugLauncher;
import com.android.ide.eclipse.ddms.preferences.PreferenceInitializer;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;

import java.io.File;
import java.io.IOException;

public class DeviceView extends ViewPart implements IUiSelectionListener, IClientChangeListener {

    private final static boolean USE_SELECTED_DEBUG_PORT = true;

    public static final String ID =
        "com.android.ide.eclipse.ddms.views.DeviceView"; //$NON-NLS-1$

    private static DeviceView sThis;

    private Shell mParentShell;
    private DevicePanel mDeviceList;

    private Action mResetAdbAction;
    private Action mCaptureAction;
    private Action mUpdateThreadAction;
    private Action mUpdateHeapAction;
    private Action mGcAction;
    private Action mKillAppAction;
    private Action mDebugAction;
    private Action mHprofAction;
    private Action mTracingAction;
    private IDebugLauncher mDebugLauncher;

    private ImageDescriptor mTracingStartImage;
    private ImageDescriptor mTracingStopImage;

    public class HProfHandler extends BaseFileHandler implements IHprofDumpHandler {
        public final static String ACTION_SAVE ="hprof.save"; //$NON-NLS-1$
        public final static String ACTION_OPEN = "hprof.open"; //$NON-NLS-1$

        public final static String DOT_HPROF = ".hprof"; //$NON-NLS-1$

        HProfHandler(Shell parentShell) {
            super(parentShell);
        }

        public void onFailure(final Client client) {
            mParentShell.getDisplay().asyncExec(new Runnable() {
                public void run() {
                    try {
                        displayError("Unable to create HPROF file for application '%1$s'.\n" +
                                "Check logcat for more information.",
                                client.getClientData().getClientDescription());
                    } finally {
                        // this will make sure the dump hprof button is re-enabled for the
                        // current selection. as the client is finished dumping an hprof file
                        doSelectionChanged(mDeviceList.getSelectedClient());
                    }
                }
            });
        }

        public void onSuccess(final String remoteFilePath, final Client client) {
            mParentShell.getDisplay().asyncExec(new Runnable() {
                public void run() {
                    final IDevice device = client.getDevice();
                    try {
                        // get the sync service to pull the HPROF file
                        final SyncService sync = client.getDevice().getSyncService();
                        if (sync != null) {
                            // get from the preference what action to take
                            IPreferenceStore store = DdmsPlugin.getDefault().getPreferenceStore();
                            String value = store.getString(PreferenceInitializer.ATTR_HPROF_ACTION);

                            SyncResult result = null;
                            if (ACTION_OPEN.equals(value)) {
                                File temp = File.createTempFile("android", DOT_HPROF); //$NON-NLS-1$
                                String tempPath = temp.getAbsolutePath();
                                result = pull(sync, tempPath, remoteFilePath);
                                if (result != null && result.getCode() == SyncService.RESULT_OK) {
                                    open(tempPath);
                                }
                            } else {
                                // default action is ACTION_SAVE
                                result = promptAndPull(sync,
                                        client.getClientData().getClientDescription() + DOT_HPROF,
                                        remoteFilePath, "Save HPROF file");

                            }

                            if (result != null && result.getCode() != SyncService.RESULT_OK) {
                                displayError(
                                        "Unable to download HPROF file from device '%1$s'.\n\n%2$s",
                                        device.getSerialNumber(), result.getMessage());
                            }
                        } else {
                            displayError("Unable to download HPROF file from device '%1$s'.",
                                    device.getSerialNumber());
                        }
                    } catch (Exception e) {
                        displayError("Unable to download HPROF file from device '%1$s'.",
                                device.getSerialNumber());

                    } finally {
                        // this will make sure the dump hprof button is re-enabled for the
                        // current selection. as the client is finished dumping an hprof file
                        doSelectionChanged(mDeviceList.getSelectedClient());
                    }
                }
            });
        }

        private void open(String path) throws IOException, InterruptedException, PartInitException {
            // make a temp file to convert the hprof into something
            // readable by normal tools
            File temp = File.createTempFile("android", DOT_HPROF);
            String tempPath = temp.getAbsolutePath();

            String[] command = new String[3];
            command[0] = DdmsPlugin.getHprofConverter();
            command[1] = path;
            command[2] = tempPath;

            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();

            IFileStore fileStore =  EFS.getLocalFileSystem().getStore(new Path(tempPath));
            if (!fileStore.fetchInfo().isDirectory() && fileStore.fetchInfo().exists()) {
                IDE.openEditorOnFileStore(
                        getSite().getWorkbenchWindow().getActivePage(),
                        fileStore);
            }
        }

        private void displayError(String format, Object... args) {
            MessageDialog.openError(mParentShell, "HPROF Error",
                    String.format(format, args));
        }
    }


    public DeviceView() {
        // the view is declared with allowMultiple="false" so we
        // can safely do this.
        sThis = this;
    }

    public static DeviceView getInstance() {
        return sThis;
    }

    /**
     * Sets the {@link IDebugLauncher}.
     * @param debugLauncher
     */
    public void setDebugLauncher(DdmsPlugin.IDebugLauncher debugLauncher) {
        mDebugLauncher = debugLauncher;
        if (mDebugAction != null && mDeviceList != null) {
            Client currentClient = mDeviceList.getSelectedClient();
            if (currentClient != null) {
                mDebugAction.setEnabled(true);
            }
        }
    }

    @Override
    public void createPartControl(Composite parent) {
        mParentShell = parent.getShell();
        ClientData.setHprofDumpHandler(new HProfHandler(mParentShell));
        AndroidDebugBridge.addClientChangeListener(this);
        ClientData.setMethodProfilingHandler(new MethodProfilingHandler(mParentShell));

        mDeviceList = new DevicePanel(DdmsPlugin.getImageLoader(), USE_SELECTED_DEBUG_PORT);
        mDeviceList.createPanel(parent);
        mDeviceList.addSelectionListener(this);

        DdmsPlugin plugin = DdmsPlugin.getDefault();
        mDeviceList.addSelectionListener(plugin);
        plugin.setListeningState(true);

        mCaptureAction = new Action("Screen Capture") {
            @Override
            public void run() {
                ScreenShotDialog dlg = new ScreenShotDialog(
                        DdmsPlugin.getDisplay().getActiveShell());
                dlg.open(mDeviceList.getSelectedDevice());
            }
        };
        mCaptureAction.setToolTipText("Screen Capture");
        mCaptureAction.setImageDescriptor(
                DdmsPlugin.getImageLoader().loadDescriptor("capture.png")); //$NON-NLS-1$

        mResetAdbAction = new Action("Reset adb") {
            @Override
            public void run() {
                AndroidDebugBridge bridge = AndroidDebugBridge.getBridge();
                if (bridge != null) {
                    if (bridge.restart() == false) {
                        // get the current Display
                        final Display display = DdmsPlugin.getDisplay();

                        // dialog box only run in ui thread..
                        display.asyncExec(new Runnable() {
                            public void run() {
                                Shell shell = display.getActiveShell();
                                MessageDialog.openError(shell, "Adb Error",
                                        "Adb failed to restart!\n\nMake sure the plugin is properly configured.");
                            }
                        });
                    }
                }
            }
        };
        mResetAdbAction.setToolTipText("Reset the adb host daemon");
        mResetAdbAction.setImageDescriptor(PlatformUI.getWorkbench()
                .getSharedImages().getImageDescriptor(
                        ISharedImages.IMG_OBJS_WARN_TSK));

        mKillAppAction = new Action() {
            @Override
            public void run() {
                mDeviceList.killSelectedClient();
            }
        };

        mKillAppAction.setText("Stop Process");
        mKillAppAction.setToolTipText("Stop Process");
        mKillAppAction.setImageDescriptor(DdmsPlugin.getImageLoader()
                .loadDescriptor(DevicePanel.ICON_HALT));

        mGcAction = new Action() {
            @Override
            public void run() {
                mDeviceList.forceGcOnSelectedClient();
            }
        };

        mGcAction.setText("Cause GC");
        mGcAction.setToolTipText("Cause GC");
        mGcAction.setImageDescriptor(DdmsPlugin.getImageLoader()
                .loadDescriptor(DevicePanel.ICON_GC));

        mHprofAction = new Action() {
            @Override
            public void run() {
                mDeviceList.dumpHprof();
                doSelectionChanged(mDeviceList.getSelectedClient());
            }
        };
        mHprofAction.setText("Dump HPROF file");
        mHprofAction.setToolTipText("Dump HPROF file");
        mHprofAction.setImageDescriptor(DdmsPlugin.getImageLoader()
                .loadDescriptor(DevicePanel.ICON_HPROF));

        mUpdateHeapAction = new Action("Update Heap", IAction.AS_CHECK_BOX) {
            @Override
            public void run() {
                boolean enable = mUpdateHeapAction.isChecked();
                mDeviceList.setEnabledHeapOnSelectedClient(enable);
            }
        };
        mUpdateHeapAction.setToolTipText("Update Heap");
        mUpdateHeapAction.setImageDescriptor(DdmsPlugin.getImageLoader()
                .loadDescriptor(DevicePanel.ICON_HEAP));

        mUpdateThreadAction = new Action("Update Threads", IAction.AS_CHECK_BOX) {
            @Override
            public void run() {
                boolean enable = mUpdateThreadAction.isChecked();
                mDeviceList.setEnabledThreadOnSelectedClient(enable);
            }
        };
        mUpdateThreadAction.setToolTipText("Update Threads");
        mUpdateThreadAction.setImageDescriptor(DdmsPlugin.getImageLoader()
                .loadDescriptor(DevicePanel.ICON_THREAD));

        mTracingAction = new Action() {
            @Override
            public void run() {
                mDeviceList.toggleMethodProfiling();
            }
        };
        mTracingAction.setText("Start Method Profiling");
        mTracingAction.setToolTipText("Start Method Profiling");
        mTracingStartImage = DdmsPlugin.getImageLoader().loadDescriptor(
                DevicePanel.ICON_TRACING_START);
        mTracingStopImage = DdmsPlugin.getImageLoader().loadDescriptor(
                DevicePanel.ICON_TRACING_STOP);
        mTracingAction.setImageDescriptor(mTracingStartImage);

        // check if there's already a debug launcher set up in the plugin class
        mDebugLauncher = DdmsPlugin.getRunningAppDebugLauncher();

        mDebugAction = new Action("Debug Process") {
            @Override
            public void run() {
                if (mDebugLauncher != null) {
                    Client currentClient = mDeviceList.getSelectedClient();
                    if (currentClient != null) {
                        ClientData clientData = currentClient.getClientData();

                        // make sure the client can be debugged
                        switch (clientData.getDebuggerConnectionStatus()) {
                            case ERROR: {
                                Display display = DdmsPlugin.getDisplay();
                                Shell shell = display.getActiveShell();
                                MessageDialog.openError(shell, "Process Debug",
                                        "The process debug port is already in use!");
                                return;
                            }
                            case ATTACHED: {
                                Display display = DdmsPlugin.getDisplay();
                                Shell shell = display.getActiveShell();
                                MessageDialog.openError(shell, "Process Debug",
                                        "The process is already being debugged!");
                                return;
                            }
                        }

                        // get the name of the client
                        String packageName = clientData.getClientDescription();
                        if (packageName != null) {
                            if (mDebugLauncher.debug(packageName,
                                    currentClient.getDebuggerListenPort()) == false) {

                                // if we get to this point, then we failed to find a project
                                // that matched the application to debug
                                Display display = DdmsPlugin.getDisplay();
                                Shell shell = display.getActiveShell();
                                MessageDialog.openError(shell, "Process Debug",
                                        String.format(
                                                "No opened project found for %1$s. Debug session failed!",
                                                packageName));
                            }
                        }
                    }
                }
            }
        };
        mDebugAction.setToolTipText("Debug the selected process, provided its source project is present and opened in the workspace.");
        mDebugAction.setImageDescriptor(DdmsPlugin.getImageLoader()
                .loadDescriptor("debug-attach.png")); //$NON-NLS-1$
        if (mDebugLauncher == null) {
            mDebugAction.setEnabled(false);
        }

        placeActions();
    }

    @Override
    public void setFocus() {
        mDeviceList.setFocus();
    }

    /**
     * Sent when a new {@link IDevice} and {@link Client} are selected.
     * @param selectedDevice the selected device. If null, no devices are selected.
     * @param selectedClient The selected client. If null, no clients are selected.
     */
    public void selectionChanged(IDevice selectedDevice, Client selectedClient) {
        // update the buttons
        doSelectionChanged(selectedClient);
        doSelectionChanged(selectedDevice);
    }

    private void doSelectionChanged(Client selectedClient) {
        // update the buttons
        if (selectedClient != null) {
            if (USE_SELECTED_DEBUG_PORT) {
                // set the client as the debug client
                selectedClient.setAsSelectedClient();
            }

            mDebugAction.setEnabled(mDebugLauncher != null);
            mKillAppAction.setEnabled(true);
            mGcAction.setEnabled(true);

            mUpdateHeapAction.setEnabled(true);
            mUpdateHeapAction.setChecked(selectedClient.isHeapUpdateEnabled());

            mUpdateThreadAction.setEnabled(true);
            mUpdateThreadAction.setChecked(selectedClient.isThreadUpdateEnabled());

            ClientData data = selectedClient.getClientData();

            if (data.hasFeature(ClientData.FEATURE_HPROF)) {
                mHprofAction.setEnabled(data.hasPendingHprofDump() == false);
                mHprofAction.setToolTipText("Dump HPROF file");
            } else {
                mHprofAction.setEnabled(false);
                mHprofAction.setToolTipText("Dump HPROF file (not supported by this VM)");
            }

            if (data.hasFeature(ClientData.FEATURE_PROFILING)) {
                mTracingAction.setEnabled(true);
                if (data.getMethodProfilingStatus() == MethodProfilingStatus.ON) {
                    mTracingAction.setToolTipText("Stop Method Profiling");
                    mTracingAction.setText("Stop Method Profiling");
                    mTracingAction.setImageDescriptor(mTracingStopImage);
                } else {
                    mTracingAction.setToolTipText("Start Method Profiling");
                    mTracingAction.setImageDescriptor(mTracingStartImage);
                    mTracingAction.setText("Start Method Profiling");
                }
            } else {
                mTracingAction.setEnabled(false);
                mTracingAction.setImageDescriptor(mTracingStartImage);
                mTracingAction.setToolTipText("Start Method Profiling (not supported by this VM)");
                mTracingAction.setText("Start Method Profiling");
            }
        } else {
            if (USE_SELECTED_DEBUG_PORT) {
                // set the client as the debug client
                AndroidDebugBridge bridge = AndroidDebugBridge.getBridge();
                if (bridge != null) {
                    bridge.setSelectedClient(null);
                }
            }

            mDebugAction.setEnabled(false);
            mKillAppAction.setEnabled(false);
            mGcAction.setEnabled(false);
            mUpdateHeapAction.setChecked(false);
            mUpdateHeapAction.setEnabled(false);
            mUpdateThreadAction.setEnabled(false);
            mUpdateThreadAction.setChecked(false);
            mHprofAction.setEnabled(false);

            mHprofAction.setEnabled(false);
            mHprofAction.setToolTipText("Dump HPROF file");

            mTracingAction.setEnabled(false);
            mTracingAction.setImageDescriptor(mTracingStartImage);
            mTracingAction.setToolTipText("Start Method Profiling");
            mTracingAction.setText("Start Method Profiling");
        }
    }

    private void doSelectionChanged(IDevice selectedDevice) {
        mCaptureAction.setEnabled(selectedDevice != null);
    }

    /**
     * Place the actions in the ui.
     */
    private final void placeActions() {
        IActionBars actionBars = getViewSite().getActionBars();

        // first in the menu
        IMenuManager menuManager = actionBars.getMenuManager();
        menuManager.removeAll();
        menuManager.add(mDebugAction);
        menuManager.add(new Separator());
        menuManager.add(mUpdateHeapAction);
        menuManager.add(mHprofAction);
        menuManager.add(mGcAction);
        menuManager.add(new Separator());
        menuManager.add(mUpdateThreadAction);
        menuManager.add(mTracingAction);
        menuManager.add(new Separator());
        menuManager.add(mKillAppAction);
        menuManager.add(new Separator());
        menuManager.add(mCaptureAction);
        menuManager.add(new Separator());
        menuManager.add(mResetAdbAction);

        // and then in the toolbar
        IToolBarManager toolBarManager = actionBars.getToolBarManager();
        toolBarManager.removeAll();
        toolBarManager.add(mDebugAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(mUpdateHeapAction);
        toolBarManager.add(mHprofAction);
        toolBarManager.add(mGcAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(mUpdateThreadAction);
        toolBarManager.add(mTracingAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(mKillAppAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(mCaptureAction);
    }

    public void clientChanged(final Client client, int changeMask) {
        if ((changeMask & Client.CHANGE_METHOD_PROFILING_STATUS) ==
                Client.CHANGE_METHOD_PROFILING_STATUS) {
            if (mDeviceList.getSelectedClient() == client) {
                mParentShell.getDisplay().asyncExec(new Runnable() {
                    public void run() {
                        // force refresh of the button enabled state.
                        doSelectionChanged(client);
                    }
                });
            }
        }
    }
}
