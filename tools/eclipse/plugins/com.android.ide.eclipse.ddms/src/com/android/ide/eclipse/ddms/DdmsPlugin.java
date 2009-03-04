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

package com.android.ide.eclipse.ddms;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.Client;
import com.android.ddmlib.DdmPreferences;
import com.android.ddmlib.Device;
import com.android.ddmlib.Log;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;
import com.android.ddmlib.Log.ILogOutput;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmuilib.DdmUiPreferences;
import com.android.ddmuilib.DevicePanel.IUiSelectionListener;
import com.android.ide.eclipse.ddms.preferences.PreferenceInitializer;
import com.android.ide.eclipse.ddms.views.DeviceView;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * The activator class controls the plug-in life cycle
 */
public final class DdmsPlugin extends AbstractUIPlugin implements IDeviceChangeListener,
        IUiSelectionListener {

    // The plug-in ID
    public static final String PLUGIN_ID = "com.android.ide.eclipse.ddms"; // $NON-NLS-1$

    private static final String ADB_LOCATION = PLUGIN_ID + ".adb"; // $NON-NLS-1$

    /** The singleton instance */
    private static DdmsPlugin sPlugin;

    /** Location of the adb command line executable */
    private static String sAdbLocation;

    /**
     * Debug Launcher for already running apps
     */
    private static IDebugLauncher sRunningAppDebugLauncher;

    /** Console for DDMS log message */
    private MessageConsole mDdmsConsole;

    /** Image loader object */
    private ImageLoader mLoader;
    
    private Device mCurrentDevice;
    private Client mCurrentClient;
    private boolean mListeningToUiSelection = false;
    
    private final ArrayList<ISelectionListener> mListeners = new ArrayList<ISelectionListener>();

    private Color mRed;

    private boolean mDdmlibInitialized;

    /**
     * Interface to provide debugger launcher for running apps.
     */
    public interface IDebugLauncher {
        public boolean debug(String packageName, int port);
    }
    
    /**
     * Classes which implement this interface provide methods that deals
     * with {@link Device} and {@link Client} selectionchanges.
     */
    public interface ISelectionListener {
        
        /**
         * Sent when a new {@link Client} is selected.
         * @param selectedClient The selected client. If null, no clients are selected.
         */
        public void selectionChanged(Client selectedClient);
        
        /**
         * Sent when a new {@link Device} is selected.
         * @param selectedDevice the selected device. If null, no devices are selected.
         */
        public void selectionChanged(Device selectedDevice);
    }

    /**
     * The constructor
     */
    public DdmsPlugin() {
        sPlugin = this;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        
        final Display display = getDisplay();

        // get the eclipse store
        final IPreferenceStore eclipseStore = getPreferenceStore();

        AndroidDebugBridge.addDeviceChangeListener(this);
        
        DdmUiPreferences.setStore(eclipseStore);

        //DdmUiPreferences.displayCharts();

        // set the consoles.
        mDdmsConsole = new MessageConsole("DDMS", null); // $NON-NLS-1$
        ConsolePlugin.getDefault().getConsoleManager().addConsoles(
                new IConsole[] {
                    mDdmsConsole
                });

        final MessageConsoleStream consoleStream = mDdmsConsole.newMessageStream();
        final MessageConsoleStream errorConsoleStream = mDdmsConsole.newMessageStream();
        mRed = new Color(display, 0xFF, 0x00, 0x00);

        // because this can be run, in some cases, by a non UI thread, and because
        // changing the console properties update the UI, we need to make this change
        // in the UI thread.
        display.asyncExec(new Runnable() {
            public void run() {
                errorConsoleStream.setColor(mRed);
            }
        });

        // set up the ddms log to use the ddms console.
        Log.setLogOutput(new ILogOutput() {
            public void printLog(LogLevel logLevel, String tag, String message) {
                if (logLevel.getPriority() >= LogLevel.ERROR.getPriority()) {
                    printToStream(errorConsoleStream, tag, message);
                    ConsolePlugin.getDefault().getConsoleManager().showConsoleView(mDdmsConsole);
                } else {
                    printToStream(consoleStream, tag, message);
                }
            }

            public void printAndPromptLog(final LogLevel logLevel, final String tag,
                    final String message) {
                printLog(logLevel, tag, message);
                // dialog box only run in UI thread..
                display.asyncExec(new Runnable() {
                    public void run() {
                        Shell shell = display.getActiveShell();
                        if (logLevel == LogLevel.ERROR) {
                            MessageDialog.openError(shell, tag, message);
                        } else {
                            MessageDialog.openWarning(shell, tag, message);
                        }
                    }
                });
            }
            
        });

        // create the loader that's able to load the images
        mLoader = new ImageLoader(this);
        
        // set the listener for the preference change
        Preferences prefs = getPluginPreferences();
        prefs.addPropertyChangeListener(new IPropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                // get the name of the property that changed.
                String property = event.getProperty();

                if (PreferenceInitializer.ATTR_DEBUG_PORT_BASE.equals(property)) {
                    DdmPreferences.setDebugPortBase(
                            eclipseStore.getInt(PreferenceInitializer.ATTR_DEBUG_PORT_BASE));
                } else if (PreferenceInitializer.ATTR_SELECTED_DEBUG_PORT.equals(property)) {
                    DdmPreferences.setSelectedDebugPort(
                            eclipseStore.getInt(PreferenceInitializer.ATTR_SELECTED_DEBUG_PORT));
                } else if (PreferenceInitializer.ATTR_THREAD_INTERVAL.equals(property)) {
                    DdmUiPreferences.setThreadRefreshInterval(
                            eclipseStore.getInt(PreferenceInitializer.ATTR_THREAD_INTERVAL));
                } else if (PreferenceInitializer.ATTR_LOG_LEVEL.equals(property)) {
                    DdmPreferences.setLogLevel(
                            eclipseStore.getString(PreferenceInitializer.ATTR_LOG_LEVEL));
                }
            }
        });
        
        // read the adb location from the prefs to attempt to start it properly without
        // having to wait for ADT to start
        sAdbLocation = eclipseStore.getString(ADB_LOCATION);

        // start it in a thread to return from start() asap.
        new Thread() {
            @Override
            public void run() {
                // init ddmlib if needed
                getDefault().initDdmlib();

                // create and start the first bridge
                AndroidDebugBridge.createBridge(sAdbLocation, true /* forceNewBridge */);
            }
        }.start();
    }

    public static Display getDisplay() {
        IWorkbench bench = sPlugin.getWorkbench();
        if (bench != null) {
            return bench.getDisplay();
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        AndroidDebugBridge.removeDeviceChangeListener(this);
        
        AndroidDebugBridge.terminate();
        
        mRed.dispose();

        sPlugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static DdmsPlugin getDefault() {
        return sPlugin;
    }

    /** Return the image loader for the plugin */
    public static ImageLoader getImageLoader() {
        if (sPlugin != null) {
            return sPlugin.mLoader;
        }
        return null;
    }

    public static String getAdb() {
        return sAdbLocation;
    }

    /**
     * Set the location of the adb executable and optionally starts adb
     * @param adb location of adb
     * @param startAdb flag to start adb
     */
    public static void setAdb(String adb, boolean startAdb) {
        sAdbLocation = adb;

        // store the location for future ddms only start.
        sPlugin.getPreferenceStore().setValue(ADB_LOCATION, sAdbLocation);

        // starts the server in a thread in case this is blocking.
        if (startAdb) {
            new Thread() {
                @Override
                public void run() {
                    // init ddmlib if needed
                    getDefault().initDdmlib();

                    // create and start the bridge
                    AndroidDebugBridge.createBridge(sAdbLocation, false /* forceNewBridge */);
                }
            }.start();
        }
    }
    
    private synchronized void initDdmlib() {
        if (mDdmlibInitialized == false) {
            // set the preferences.
            PreferenceInitializer.setupPreferences();
    
            // init the lib
            AndroidDebugBridge.init(true /* debugger support */);
            
            mDdmlibInitialized = true;
        }
    }

    /**
     * Sets the launcher responsible for connecting the debugger to running applications.
     * @param launcher The launcher.
     */
    public static void setRunningAppDebugLauncher(IDebugLauncher launcher) {
        sRunningAppDebugLauncher = launcher;

        // if the process view is already running, give it the launcher.
        // This method could be called from a non ui thread, so we make sure to do that
        // in the ui thread.
        Display display = getDisplay();
        if (display != null && display.isDisposed() == false) {
            display.asyncExec(new Runnable() {
                public void run() {
                    DeviceView dv = DeviceView.getInstance();
                    if (dv != null) {
                        dv.setDebugLauncher(sRunningAppDebugLauncher);
                    }
                }
            });
        }
    }

    public static IDebugLauncher getRunningAppDebugLauncher() {
        return sRunningAppDebugLauncher;
    }
    
    public synchronized void addSelectionListener(ISelectionListener listener) {
        mListeners.add(listener);
        
        // notify the new listener of the current selection
       listener.selectionChanged(mCurrentDevice);
       listener.selectionChanged(mCurrentClient);
    }

    public synchronized void removeSelectionListener(ISelectionListener listener) {
        mListeners.remove(listener);
    }

    public synchronized void setListeningState(boolean state) {
        mListeningToUiSelection = state;
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
        // if we are listening to selection coming from the ui, then we do nothing, as
        // any change in the devices/clients, will be handled by the UI, and we'll receive
        // selection notification through our implementation of IUiSelectionListener.
        if (mListeningToUiSelection == false) {
            if (mCurrentDevice == null) {
                handleDefaultSelection(device);
            }
        }
    }

    /**
     * Sent when the a device is disconnected to the {@link AndroidDebugBridge}.
     * <p/>
     * This is sent from a non UI thread.
     * @param device the new device.
     * 
     * @see IDeviceChangeListener#deviceDisconnected(Device)
     */
    public void deviceDisconnected(Device device) {
        // if we are listening to selection coming from the ui, then we do nothing, as
        // any change in the devices/clients, will be handled by the UI, and we'll receive
        // selection notification through our implementation of IUiSelectionListener.
        if (mListeningToUiSelection == false) {
            // test if the disconnected device was the default selection.
            if (mCurrentDevice == device) {
                // try to find a new device
                AndroidDebugBridge bridge = AndroidDebugBridge.getBridge();
                if (bridge != null) {
                    // get the device list
                    Device[] devices = bridge.getDevices();
                    
                    // check if we still have devices
                    if (devices.length == 0) {
                        handleDefaultSelection((Device)null);
                    } else {
                        handleDefaultSelection(devices[0]);
                    }
                } else {
                    handleDefaultSelection((Device)null);
                }
            }
        }
    }

    /**
     * Sent when a device data changed, or when clients are started/terminated on the device.
     * <p/>
     * This is sent from a non UI thread.
     * @param device the device that was updated.
     * @param changeMask the mask indicating what changed.
     * 
     * @see IDeviceChangeListener#deviceChanged(Device)
     */
    public void deviceChanged(Device device, int changeMask) {
        // if we are listening to selection coming from the ui, then we do nothing, as
        // any change in the devices/clients, will be handled by the UI, and we'll receive
        // selection notification through our implementation of IUiSelectionListener.
        if (mListeningToUiSelection == false) {
            
            // check if this is our device
            if (device == mCurrentDevice) {
                if (mCurrentClient == null) {
                    handleDefaultSelection(device);
                } else {
                    // get the clients and make sure ours is still in there.
                    Client[] clients = device.getClients();
                    boolean foundClient = false;
                    for (Client client : clients) {
                        if (client == mCurrentClient) {
                            foundClient = true;
                            break;
                        }
                    }
                    
                    // if we haven't found our client, lets look for a new one
                    if (foundClient == false) {
                        mCurrentClient = null;
                        handleDefaultSelection(device);
                    }
                }
            }
        }
    }

    /**
     * Sent when a new {@link Device} and {@link Client} are selected.
     * @param selectedDevice the selected device. If null, no devices are selected.
     * @param selectedClient The selected client. If null, no clients are selected.
     */
    public synchronized void selectionChanged(Device selectedDevice, Client selectedClient) {
        if (mCurrentDevice != selectedDevice) {
            mCurrentDevice = selectedDevice;

            // notify of the new default device
            for (ISelectionListener listener : mListeners) {
                listener.selectionChanged(mCurrentDevice);
            }
        }

        if (mCurrentClient != selectedClient) {
            mCurrentClient = selectedClient;
            
            // notify of the new default client
            for (ISelectionListener listener : mListeners) {
                listener.selectionChanged(mCurrentClient);
            }
        }
    }

    /**
     * Handles a default selection of a {@link Device} and {@link Client}.
     * @param device the selected device
     */
    private void handleDefaultSelection(final Device device) {
        // because the listener expect to receive this from the UI thread, and this is called
        // from the AndroidDebugBridge notifications, we need to run this in the UI thread.
        try {
            Display display = getDisplay();
            
            display.asyncExec(new Runnable() {
                public void run() {
                    // set the new device if different.
                    boolean newDevice = false;
                    if (mCurrentDevice != device) {
                        mCurrentDevice = device;
                        newDevice = true;
                
                        // notify of the new default device
                        for (ISelectionListener listener : mListeners) {
                            listener.selectionChanged(mCurrentDevice);
                        }
                    }
                    
                    if (device != null) {
                        // if this is a device switch or the same device but we didn't find a valid
                        // client the last time, we go look for a client to use again.
                        if (newDevice || mCurrentClient == null) {
                            // now get the new client
                            Client[] clients =  device.getClients();
                            if (clients.length > 0) {
                                handleDefaultSelection(clients[0]);
                            } else {
                                handleDefaultSelection((Client)null);
                            }
                        }
                    } else {
                        handleDefaultSelection((Client)null);
                    }
                }
            });
        } catch (SWTException e) {
            // display is disposed. Do nothing since we're quitting anyway.
        }
    }
    
    private void handleDefaultSelection(Client client) {
        mCurrentClient = client;
        
        // notify of the new default client
        for (ISelectionListener listener : mListeners) {
            listener.selectionChanged(mCurrentClient);
        }
    }
    
    /**
     * Prints a message, associated with a project to the specified stream
     * @param stream The stream to write to
     * @param tag The tag associated to the message. Can be null
     * @param message The message to print.
     */
    private static synchronized void printToStream(MessageConsoleStream stream, String tag,
            String message) {
        String dateTag = getMessageTag(tag);

        stream.print(dateTag);
        stream.println(message);
    }
    
    /**
     * Creates a string containing the current date/time, and the tag
     * @param tag The tag associated to the message. Can be null
     * @return The dateTag
     */
    private static String getMessageTag(String tag) {
        Calendar c = Calendar.getInstance();

        if (tag == null) {
            return String.format("[%1$tF %1$tT]", c);
        }

        return String.format("[%1$tF %1$tT - %2$s]", c, tag);
    }


}
