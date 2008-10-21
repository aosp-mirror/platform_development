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

package com.android.ddmlib;

import com.android.ddmlib.Log.LogLevel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Thread.State;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A connection to the host-side android debug bridge (adb)
 * <p/>This is the central point to communicate with any devices, emulators, or the applications
 * running on them.
 * <p/><b>{@link #init(boolean)} must be called before anything is done.</b>
 */
public final class AndroidDebugBridge {
    
    /*
     * Minimum and maximum version of adb supported. This correspond to
     * ADB_SERVER_VERSION found in //device/tools/adb/adb.h
     */

    private final static int ADB_VERSION_MICRO_MIN = 20;
    private final static int ADB_VERSION_MICRO_MAX = -1;

    private final static Pattern sAdbVersion = Pattern.compile(
            "^.*(\\d+)\\.(\\d+)\\.(\\d+)$"); //$NON-NLS-1$
    
    private final static String ADB = "adb"; //$NON-NLS-1$
    private final static String DDMS = "ddms"; //$NON-NLS-1$

    // Where to find the ADB bridge.
    final static String ADB_HOST = "127.0.0.1"; //$NON-NLS-1$
    final static int ADB_PORT = 5037;

    static InetAddress sHostAddr;
    static InetSocketAddress sSocketAddr;

    static {
        // built-in local address/port for ADB.
        try {
            sHostAddr = InetAddress.getByName(ADB_HOST);
            sSocketAddr = new InetSocketAddress(sHostAddr, ADB_PORT);
        } catch (UnknownHostException e) {

        }
    }

    private static AndroidDebugBridge sThis;
    private static boolean sClientSupport;

    /** Full path to adb. */
    private String mAdbOsLocation = null;

    private boolean mVersionCheck;

    private boolean mStarted = false;

    private DeviceMonitor mDeviceMonitor;

    private final static ArrayList<IDebugBridgeChangeListener> sBridgeListeners =
        new ArrayList<IDebugBridgeChangeListener>();
    private final static ArrayList<IDeviceChangeListener> sDeviceListeners =
        new ArrayList<IDeviceChangeListener>();
    private final static ArrayList<IClientChangeListener> sClientListeners =
        new ArrayList<IClientChangeListener>();

    // lock object for synchronization
    private static final Object sLock = sBridgeListeners;

    /**
     * Classes which implement this interface provide a method that deals
     * with {@link AndroidDebugBridge} changes.
     */
    public interface IDebugBridgeChangeListener {
        /**
         * Sent when a new {@link AndroidDebugBridge} is connected.
         * <p/>
         * This is sent from a non UI thread.
         * @param bridge the new {@link AndroidDebugBridge} object.
         */
        public void bridgeChanged(AndroidDebugBridge bridge);
    }

    /**
     * Classes which implement this interface provide methods that deal
     * with {@link Device} addition, deletion, and changes.
     */
    public interface IDeviceChangeListener {
        /**
         * Sent when the a device is connected to the {@link AndroidDebugBridge}.
         * <p/>
         * This is sent from a non UI thread.
         * @param device the new device.
         */
        public void deviceConnected(Device device);

        /**
         * Sent when the a device is connected to the {@link AndroidDebugBridge}.
         * <p/>
         * This is sent from a non UI thread.
         * @param device the new device.
         */
        public void deviceDisconnected(Device device);

        /**
         * Sent when a device data changed, or when clients are started/terminated on the device.
         * <p/>
         * This is sent from a non UI thread.
         * @param device the device that was updated.
         * @param changeMask the mask describing what changed. It can contain any of the following
         * values: {@link Device#CHANGE_BUILD_INFO}, {@link Device#CHANGE_STATE},
         * {@link Device#CHANGE_CLIENT_LIST}
         */
        public void deviceChanged(Device device, int changeMask);
    }

    /**
     * Classes which implement this interface provide methods that deal
     * with {@link Client}  changes.
     */
    public interface IClientChangeListener {
        /**
         * Sent when an existing client information changed.
         * <p/>
         * This is sent from a non UI thread.
         * @param client the updated client.
         * @param changeMask the bit mask describing the changed properties. It can contain
         * any of the following values: {@link Client#CHANGE_INFO},
         * {@link Client#CHANGE_DEBUGGER_INTEREST}, {@link Client#CHANGE_THREAD_MODE},
         * {@link Client#CHANGE_THREAD_DATA}, {@link Client#CHANGE_HEAP_MODE},
         * {@link Client#CHANGE_HEAP_DATA}, {@link Client#CHANGE_NATIVE_HEAP_DATA}
         */
        public void clientChanged(Client client, int changeMask);
    }

    /**
     * Initializes the <code>ddm</code> library.
     * <p/>This must be called once <b>before</b> any call to
     * {@link #createBridge(String, boolean)}.
     * <p>The library can be initialized in 2 ways:
     * <ul>
     * <li>Mode 1: <var>clientSupport</var> == <code>true</code>.<br>The library monitors the
     * devices and the applications running on them. It will connect to each application, as a
     * debugger of sort, to be able to interact with them through JDWP packets.</li>
     * <li>Mode 2: <var>clientSupport</var> == <code>false</code>.<br>The library only monitors
     * devices. The applications are left untouched, letting other tools built on
     * <code>ddmlib</code> to connect a debugger to them.</li>
     * </ul>
     * <p/><b>Only one tool can run in mode 1 at the same time.</b>
     * <p/>Note that mode 1 does not prevent debugging of applications running on devices. Mode 1
     * lets debuggers connect to <code>ddmlib</code> which acts as a proxy between the debuggers and
     * the applications to debug. See {@link Client#getDebuggerListenPort()}.
     * <p/>The preferences of <code>ddmlib</code> should also be initialized with whatever default
     * values were changed from the default values.
     * <p/>When the application quits, {@link #terminate()} should be called.
     * @param clientSupport Indicates whether the library should enable the monitoring and
     * interaction with applications running on the devices.
     * @see AndroidDebugBridge#createBridge(String, boolean)
     * @see DdmPreferences
     */
    public static void init(boolean clientSupport) {
        sClientSupport = clientSupport;

        MonitorThread monitorThread = MonitorThread.createInstance();
        monitorThread.start();

        HandleHello.register(monitorThread);
        HandleAppName.register(monitorThread);
        HandleTest.register(monitorThread);
        HandleThread.register(monitorThread);
        HandleHeap.register(monitorThread);
        HandleWait.register(monitorThread);
    }

    /**
     * Terminates the ddm library. This must be called upon application termination.
     */
    public static void terminate() {
        // kill the monitoring services
        if (sThis != null && sThis.mDeviceMonitor != null) {
            sThis.mDeviceMonitor.stop();
            sThis.mDeviceMonitor = null;
        }

        MonitorThread monitorThread = MonitorThread.getInstance();
        if (monitorThread != null) {
            monitorThread.quit();
        }
    }
    
    /**
     * Returns whether the ddmlib is setup to support monitoring and interacting with
     * {@link Client}s running on the {@link Device}s.
     */
    static boolean getClientSupport() {
        return sClientSupport;
    }
    
    /**
     * Creates a {@link AndroidDebugBridge} that is not linked to any particular executable.
     * <p/>This bridge will expect adb to be running. It will not be able to start/stop/restart
     * adb.
     * <p/>If a bridge has already been started, it is directly returned with no changes (similar
     * to calling {@link #getBridge()}).
     * @return a connected bridge.
     */
    public static AndroidDebugBridge createBridge() {
        synchronized (sLock) {
            if (sThis != null) {
                return sThis;
            }

            try {
                sThis = new AndroidDebugBridge();
                sThis.start();
            } catch (InvalidParameterException e) {
                sThis = null;
            }

            // because the listeners could remove themselves from the list while processing
            // their event callback, we make a copy of the list and iterate on it instead of
            // the main list.
            // This mostly happens when the application quits.
            IDebugBridgeChangeListener[] listenersCopy = sBridgeListeners.toArray(
                    new IDebugBridgeChangeListener[sBridgeListeners.size()]);

            // notify the listeners of the change
            for (IDebugBridgeChangeListener listener : listenersCopy) {
                // we attempt to catch any exception so that a bad listener doesn't kill our
                // thread
                try {
                    listener.bridgeChanged(sThis);
                } catch (Exception e) {
                    Log.e(DDMS, e);
                }
            }

            return sThis;
        }
    }


    /**
     * Creates a new debug bridge from the location of the command line tool.
     * <p/>
     * Any existing server will be disconnected, unless the location is the same and
     * <code>forceNewBridge</code> is set to false.
     * @param osLocation the location of the command line tool 'adb'
     * @param forceNewBridge force creation of a new bridge even if one with the same location
     * already exists.
     * @return a connected bridge.
     */
    public static AndroidDebugBridge createBridge(String osLocation, boolean forceNewBridge) {
        synchronized (sLock) {
            if (sThis != null) {
                if (sThis.mAdbOsLocation != null && sThis.mAdbOsLocation.equals(osLocation) &&
                        forceNewBridge == false) {
                    return sThis;
                } else {
                    // stop the current server
                    sThis.stop();
                }
            }

            try {
                sThis = new AndroidDebugBridge(osLocation);
                sThis.start();
            } catch (InvalidParameterException e) {
                sThis = null;
            }

            // because the listeners could remove themselves from the list while processing
            // their event callback, we make a copy of the list and iterate on it instead of
            // the main list.
            // This mostly happens when the application quits.
            IDebugBridgeChangeListener[] listenersCopy = sBridgeListeners.toArray(
                    new IDebugBridgeChangeListener[sBridgeListeners.size()]);

            // notify the listeners of the change
            for (IDebugBridgeChangeListener listener : listenersCopy) {
                // we attempt to catch any exception so that a bad listener doesn't kill our
                // thread
                try {
                    listener.bridgeChanged(sThis);
                } catch (Exception e) {
                    Log.e(DDMS, e);
                }
            }

            return sThis;
        }
    }

    /**
     * Returns the current debug bridge. Can be <code>null</code> if none were created.
     */
    public static AndroidDebugBridge getBridge() {
        return sThis;
    }

    /**
     * Disconnects the current debug bridge, and destroy the object.
     * <p/>
     * A new object will have to be created with {@link #createBridge(String, boolean)}.
     */
    public static void disconnectBridge() {
        synchronized (sLock) {
            if (sThis != null) {
                sThis.stop();
                sThis = null;

                // because the listeners could remove themselves from the list while processing
                // their event callback, we make a copy of the list and iterate on it instead of
                // the main list.
                // This mostly happens when the application quits.
                IDebugBridgeChangeListener[] listenersCopy = sBridgeListeners.toArray(
                        new IDebugBridgeChangeListener[sBridgeListeners.size()]);

                // notify the listeners.
                for (IDebugBridgeChangeListener listener : listenersCopy) {
                    // we attempt to catch any exception so that a bad listener doesn't kill our
                    // thread
                    try {
                        listener.bridgeChanged(sThis);
                    } catch (Exception e) {
                        Log.e(DDMS, e);
                    }
                }
            }
        }
    }

    /**
     * Adds the listener to the collection of listeners who will be notified when a new
     * {@link AndroidDebugBridge} is connected, by sending it one of the messages defined
     * in the {@link IDebugBridgeChangeListener} interface.
     * @param listener The listener which should be notified.
     */
    public static void addDebugBridgeChangeListener(IDebugBridgeChangeListener listener) {
        synchronized (sLock) {
            if (sBridgeListeners.contains(listener) == false) {
                sBridgeListeners.add(listener);
                if (sThis != null) {
                    // we attempt to catch any exception so that a bad listener doesn't kill our
                    // thread
                    try {
                        listener.bridgeChanged(sThis);
                    } catch (Exception e) {
                        Log.e(DDMS, e);
                    }
                }
            }
        }
    }

    /**
     * Removes the listener from the collection of listeners who will be notified when a new
     * {@link AndroidDebugBridge} is started.
     * @param listener The listener which should no longer be notified.
     */
    public static void removeDebugBridgeChangeListener(IDebugBridgeChangeListener listener) {
        synchronized (sLock) {
            sBridgeListeners.remove(listener);
        }
    }

    /**
     * Adds the listener to the collection of listeners who will be notified when a {@link Device}
     * is connected, disconnected, or when its properties or its {@link Client} list changed,
     * by sending it one of the messages defined in the {@link IDeviceChangeListener} interface.
     * @param listener The listener which should be notified.
     */
    public static void addDeviceChangeListener(IDeviceChangeListener listener) {
        synchronized (sLock) {
            if (sDeviceListeners.contains(listener) == false) {
                sDeviceListeners.add(listener);
            }
        }
    }

    /**
     * Removes the listener from the collection of listeners who will be notified when a
     * {@link Device} is connected, disconnected, or when its properties or its {@link Client}
     * list changed.
     * @param listener The listener which should no longer be notified.
     */
    public static void removeDeviceChangeListener(IDeviceChangeListener listener) {
        synchronized (sLock) {
            sDeviceListeners.remove(listener);
        }
    }

    /**
     * Adds the listener to the collection of listeners who will be notified when a {@link Client}
     * property changed, by sending it one of the messages defined in the
     * {@link IClientChangeListener} interface.
     * @param listener The listener which should be notified.
     */
    public static void addClientChangeListener(IClientChangeListener listener) {
        synchronized (sLock) {
            if (sClientListeners.contains(listener) == false) {
                sClientListeners.add(listener);
            }
        }
    }

    /**
     * Removes the listener from the collection of listeners who will be notified when a
     * {@link Client} property changed.
     * @param listener The listener which should no longer be notified.
     */
    public static void removeClientChangeListener(IClientChangeListener listener) {
        synchronized (sLock) {
            sClientListeners.remove(listener);
        }
    }


    /**
     * Returns the devices.
     * @see #hasInitialDeviceList()
     */
    public Device[] getDevices() {
        synchronized (sLock) {
            if (mDeviceMonitor != null) {
                return mDeviceMonitor.getDevices();
            }
        }

        return new Device[0];
    }
    
    /**
     * Returns whether the bridge has acquired the initial list from adb after being created.
     * <p/>Calling {@link #getDevices()} right after {@link #createBridge(String, boolean)} will
     * generally result in an empty list. This is due to the internal asynchronous communication
     * mechanism with <code>adb</code> that does not guarantee that the {@link Device} list has been
     * built before the call to {@link #getDevices()}.
     * <p/>The recommended way to get the list of {@link Device} objects is to create a 
     * {@link IDeviceChangeListener} object.
     */
    public boolean hasInitialDeviceList() {
        if (mDeviceMonitor != null) {
            return mDeviceMonitor.hasInitialDeviceList();
        }
        
        return false;
    }

    /**
     * Sets the client to accept debugger connection on the custom "Selected debug port".
     * @param selectedClient the client. Can be null.
     */
    public void setSelectedClient(Client selectedClient) {
        MonitorThread monitorThread = MonitorThread.getInstance();
        if (monitorThread != null) {
            monitorThread.setSelectedClient(selectedClient);
        }
    }

    /**
     * Returns whether the {@link AndroidDebugBridge} object is still connected to the adb daemon.
     */
    public boolean isConnected() {
        MonitorThread monitorThread = MonitorThread.getInstance();
        if (mDeviceMonitor != null && monitorThread != null) {
            return mDeviceMonitor.isMonitoring() && monitorThread.getState() != State.TERMINATED;
        }
        return false;
    }

    /**
     * Returns the number of times the {@link AndroidDebugBridge} object attempted to connect
     * to the adb daemon.
     */
    public int getConnectionAttemptCount() {
        if (mDeviceMonitor != null) {
            return mDeviceMonitor.getConnectionAttemptCount();
        }
        return -1;
    }

    /**
     * Returns the number of times the {@link AndroidDebugBridge} object attempted to restart
     * the adb daemon.
     */
    public int getRestartAttemptCount() {
        if (mDeviceMonitor != null) {
            return mDeviceMonitor.getRestartAttemptCount();
        }
        return -1;
    }
    
    /**
     * Creates a new bridge.
     * @param osLocation the location of the command line tool
     * @throws InvalidParameterException
     */
    private AndroidDebugBridge(String osLocation) throws InvalidParameterException {
        if (osLocation == null || osLocation.length() == 0) {
            throw new InvalidParameterException();
        }
        mAdbOsLocation = osLocation;

        checkAdbVersion();
    }
    
    /**
     * Creates a new bridge not linked to any particular adb executable.
     */
    private AndroidDebugBridge() {
    }

    /**
     * Queries adb for its version number and checks it against {@link #MIN_VERSION_NUMBER} and
     * {@link #MAX_VERSION_NUMBER}
     */
    private void checkAdbVersion() {
        // default is bad check
        mVersionCheck = false;

        if (mAdbOsLocation == null) {
            return;
        }

        try {
            String[] command = new String[2];
            command[0] = mAdbOsLocation;
            command[1] = "version"; //$NON-NLS-1$
            Log.d(DDMS, String.format("Checking '%1$s version'", mAdbOsLocation)); //$NON-NLS-1$
            Process process = Runtime.getRuntime().exec(command);

            ArrayList<String> errorOutput = new ArrayList<String>();
            ArrayList<String> stdOutput = new ArrayList<String>();
            int status = grabProcessOutput(process, errorOutput, stdOutput,
                    true /* waitForReaders */);

            if (status != 0) {
                StringBuilder builder = new StringBuilder("'adb version' failed!"); //$NON-NLS-1$
                for (String error : errorOutput) {
                    builder.append('\n');
                    builder.append(error);
                }
                Log.logAndDisplay(LogLevel.ERROR, "adb", builder.toString());
            }

            // check both stdout and stderr
            boolean versionFound = false;
            for (String line : stdOutput) {
                versionFound = scanVersionLine(line);
                if (versionFound) {
                    break;
                }
            }
            if (!versionFound) {
                for (String line : errorOutput) {
                    versionFound = scanVersionLine(line);
                    if (versionFound) {
                        break;
                    }
                }
            }

            if (!versionFound) {
                // if we get here, we failed to parse the output.
                Log.logAndDisplay(LogLevel.ERROR, ADB,
                        "Failed to parse the output of 'adb version'"); //$NON-NLS-1$
            }
            
        } catch (IOException e) {
            Log.logAndDisplay(LogLevel.ERROR, ADB,
                    "Failed to get the adb version: " + e.getMessage()); //$NON-NLS-1$
        } catch (InterruptedException e) {
        } finally {

        }
    }

    /**
     * Scans a line resulting from 'adb version' for a potential version number.
     * <p/>
     * If a version number is found, it checks the version number against what is expected
     * by this version of ddms.
     * <p/>
     * Returns true when a version number has been found so that we can stop scanning,
     * whether the version number is in the acceptable range or not.
     * 
     * @param line The line to scan.
     * @return True if a version number was found (whether it is acceptable or not).
     */
    private boolean scanVersionLine(String line) {
        if (line != null) {
            Matcher matcher = sAdbVersion.matcher(line);
            if (matcher.matches()) {
                int majorVersion = Integer.parseInt(matcher.group(1));
                int minorVersion = Integer.parseInt(matcher.group(2));
                int microVersion = Integer.parseInt(matcher.group(3));

                // check only the micro version for now.
                if (microVersion < ADB_VERSION_MICRO_MIN) {
                    String message = String.format(
                            "Required minimum version of adb: %1$d.%2$d.%3$d." //$NON-NLS-1$
                            + "Current version is %1$d.%2$d.%4$d", //$NON-NLS-1$
                            majorVersion, minorVersion, ADB_VERSION_MICRO_MIN,
                            microVersion);
                    Log.logAndDisplay(LogLevel.ERROR, ADB, message);
                } else if (ADB_VERSION_MICRO_MAX != -1 &&
                        microVersion > ADB_VERSION_MICRO_MAX) {
                    String message = String.format(
                            "Required maximum version of adb: %1$d.%2$d.%3$d." //$NON-NLS-1$
                            + "Current version is %1$d.%2$d.%4$d", //$NON-NLS-1$
                            majorVersion, minorVersion, ADB_VERSION_MICRO_MAX,
                            microVersion);
                    Log.logAndDisplay(LogLevel.ERROR, ADB, message);
                } else {
                    mVersionCheck = true;
                }

                return true;
            }
        }
        return false;
    }

    /**
     * Starts the debug bridge.
     * @return true if success.
     */
    boolean start() {
        if (mAdbOsLocation != null && (mVersionCheck == false || startAdb() == false)) {
            return false;
        }

        mStarted = true;

        // now that the bridge is connected, we start the underlying services.
        mDeviceMonitor = new DeviceMonitor(this);
        mDeviceMonitor.start();

        return true;
    }

   /**
     * Kills the debug bridge.
     * @return true if success
     */
    boolean stop() {
        // if we haven't started we return false;
        if (mStarted == false) {
            return false;
        }

        // kill the monitoring services
        mDeviceMonitor.stop();
        mDeviceMonitor = null;

        if (stopAdb() == false) {
            return false;
        }

        mStarted = false;
        return true;
    }

    /**
     * Restarts adb, but not the services around it.
     * @return true if success.
     */
    public boolean restart() {
        if (mAdbOsLocation == null) {
            Log.e(ADB,
                    "Cannot restart adb when AndroidDebugBridge is created without the location of adb."); //$NON-NLS-1$
            return false;
        }

        if (mVersionCheck == false) {
            Log.logAndDisplay(LogLevel.ERROR, ADB,
                    "Attempting to restart adb, but version check failed!"); //$NON-NLS-1$
            return false;
        }
        synchronized (this) {
            stopAdb();

            boolean restart = startAdb();

            if (restart && mDeviceMonitor == null) {
                mDeviceMonitor = new DeviceMonitor(this);
                mDeviceMonitor.start();
            }

            return restart;
        }
    }

    /**
     * Notify the listener of a new {@link Device}.
     * <p/>
     * The notification of the listeners is done in a synchronized block. It is important to
     * expect the listeners to potentially access various methods of {@link Device} as well as
     * {@link #getDevices()} which use internal locks.
     * <p/>
     * For this reason, any call to this method from a method of {@link DeviceMonitor},
     * {@link Device} which is also inside a synchronized block, should first synchronize on
     * the {@link AndroidDebugBridge} lock. Access to this lock is done through {@link #getLock()}.
     * @param device the new <code>Device</code>.
     * @see #getLock()
     */
    void deviceConnected(Device device) {
        // because the listeners could remove themselves from the list while processing
        // their event callback, we make a copy of the list and iterate on it instead of
        // the main list.
        // This mostly happens when the application quits.
        IDeviceChangeListener[] listenersCopy = null;
        synchronized (sLock) {
            listenersCopy = sDeviceListeners.toArray(
                    new IDeviceChangeListener[sDeviceListeners.size()]);
        }
        
        // Notify the listeners
        for (IDeviceChangeListener listener : listenersCopy) {
            // we attempt to catch any exception so that a bad listener doesn't kill our
            // thread
            try {
                listener.deviceConnected(device);
            } catch (Exception e) {
                Log.e(DDMS, e);
            }
        }
    }

    /**
     * Notify the listener of a disconnected {@link Device}.
     * <p/>
     * The notification of the listeners is done in a synchronized block. It is important to
     * expect the listeners to potentially access various methods of {@link Device} as well as
     * {@link #getDevices()} which use internal locks.
     * <p/>
     * For this reason, any call to this method from a method of {@link DeviceMonitor},
     * {@link Device} which is also inside a synchronized block, should first synchronize on
     * the {@link AndroidDebugBridge} lock. Access to this lock is done through {@link #getLock()}.
     * @param device the disconnected <code>Device</code>.
     * @see #getLock()
     */
    void deviceDisconnected(Device device) {
        // because the listeners could remove themselves from the list while processing
        // their event callback, we make a copy of the list and iterate on it instead of
        // the main list.
        // This mostly happens when the application quits.
        IDeviceChangeListener[] listenersCopy = null;
        synchronized (sLock) {
            listenersCopy = sDeviceListeners.toArray(
                    new IDeviceChangeListener[sDeviceListeners.size()]);
        }
        
        // Notify the listeners
        for (IDeviceChangeListener listener : listenersCopy) {
            // we attempt to catch any exception so that a bad listener doesn't kill our
            // thread
            try {
                listener.deviceDisconnected(device);
            } catch (Exception e) {
                Log.e(DDMS, e);
            }
        }
    }

    /**
     * Notify the listener of a modified {@link Device}.
     * <p/>
     * The notification of the listeners is done in a synchronized block. It is important to
     * expect the listeners to potentially access various methods of {@link Device} as well as
     * {@link #getDevices()} which use internal locks.
     * <p/>
     * For this reason, any call to this method from a method of {@link DeviceMonitor},
     * {@link Device} which is also inside a synchronized block, should first synchronize on
     * the {@link AndroidDebugBridge} lock. Access to this lock is done through {@link #getLock()}.
     * @param device the modified <code>Device</code>.
     * @see #getLock()
     */
    void deviceChanged(Device device, int changeMask) {
        // because the listeners could remove themselves from the list while processing
        // their event callback, we make a copy of the list and iterate on it instead of
        // the main list.
        // This mostly happens when the application quits.
        IDeviceChangeListener[] listenersCopy = null;
        synchronized (sLock) {
            listenersCopy = sDeviceListeners.toArray(
                    new IDeviceChangeListener[sDeviceListeners.size()]);
        }
        
        // Notify the listeners
        for (IDeviceChangeListener listener : listenersCopy) {
            // we attempt to catch any exception so that a bad listener doesn't kill our
            // thread
            try {
                listener.deviceChanged(device, changeMask);
            } catch (Exception e) {
                Log.e(DDMS, e);
            }
        }
    }

    /**
     * Notify the listener of a modified {@link Client}.
     * <p/>
     * The notification of the listeners is done in a synchronized block. It is important to
     * expect the listeners to potentially access various methods of {@link Device} as well as
     * {@link #getDevices()} which use internal locks.
     * <p/>
     * For this reason, any call to this method from a method of {@link DeviceMonitor},
     * {@link Device} which is also inside a synchronized block, should first synchronize on
     * the {@link AndroidDebugBridge} lock. Access to this lock is done through {@link #getLock()}.
     * @param device the modified <code>Client</code>.
     * @param changeMask the mask indicating what changed in the <code>Client</code>
     * @see #getLock()
     */
    void clientChanged(Client client, int changeMask) {
        // because the listeners could remove themselves from the list while processing
        // their event callback, we make a copy of the list and iterate on it instead of
        // the main list.
        // This mostly happens when the application quits.
        IClientChangeListener[] listenersCopy = null;
        synchronized (sLock) {
            listenersCopy = sClientListeners.toArray(
                    new IClientChangeListener[sClientListeners.size()]);
            
        }

        // Notify the listeners
        for (IClientChangeListener listener : listenersCopy) {
            // we attempt to catch any exception so that a bad listener doesn't kill our
            // thread
            try {
                listener.clientChanged(client, changeMask);
            } catch (Exception e) {
                Log.e(DDMS, e);
            }
        }
    }

    /**
     * Returns the {@link DeviceMonitor} object.
     */
    DeviceMonitor getDeviceMonitor() {
        return mDeviceMonitor;
    }

    /**
     * Starts the adb host side server.
     * @return true if success
     */
    synchronized boolean startAdb() {
        if (mAdbOsLocation == null) {
            Log.e(ADB,
                "Cannot start adb when AndroidDebugBridge is created without the location of adb."); //$NON-NLS-1$
            return false;
        }

        Process proc;
        int status = -1;

        try {
            String[] command = new String[2];
            command[0] = mAdbOsLocation;
            command[1] = "start-server"; //$NON-NLS-1$
            Log.d(DDMS,
                    String.format("Launching '%1$s %2$s' to ensure ADB is running.", //$NON-NLS-1$
                    mAdbOsLocation, command[1]));
            proc = Runtime.getRuntime().exec(command);

            ArrayList<String> errorOutput = new ArrayList<String>();
            ArrayList<String> stdOutput = new ArrayList<String>();
            status = grabProcessOutput(proc, errorOutput, stdOutput,
                    false /* waitForReaders */);

        } catch (IOException ioe) {
            Log.d(DDMS, "Unable to run 'adb': " + ioe.getMessage()); //$NON-NLS-1$
            // we'll return false;
        } catch (InterruptedException ie) {
            Log.d(DDMS, "Unable to run 'adb': " + ie.getMessage()); //$NON-NLS-1$
            // we'll return false;
        }

        if (status != 0) {
            Log.w(DDMS,
                    "'adb start-server' failed -- run manually if necessary"); //$NON-NLS-1$
            return false;
        }

        Log.d(DDMS, "'adb start-server' succeeded"); //$NON-NLS-1$

        return true;
    }

    /**
     * Stops the adb host side server.
     * @return true if success
     */
    private synchronized boolean stopAdb() {
        if (mAdbOsLocation == null) {
            Log.e(ADB,
                "Cannot stop adb when AndroidDebugBridge is created without the location of adb."); //$NON-NLS-1$
            return false;
        }

        Process proc;
        int status = -1;

        try {
            String[] command = new String[2];
            command[0] = mAdbOsLocation;
            command[1] = "kill-server"; //$NON-NLS-1$
            proc = Runtime.getRuntime().exec(command);
            status = proc.waitFor();
        }
        catch (IOException ioe) {
            // we'll return false;
        }
        catch (InterruptedException ie) {
            // we'll return false;
        }

        if (status != 0) {
            Log.w(DDMS,
                    "'adb kill-server' failed -- run manually if necessary"); //$NON-NLS-1$
            return false;
        }

        Log.d(DDMS, "'adb kill-server' succeeded"); //$NON-NLS-1$
        return true;
    }

    /**
     * Get the stderr/stdout outputs of a process and return when the process is done.
     * Both <b>must</b> be read or the process will block on windows.
     * @param process The process to get the ouput from
     * @param errorOutput The array to store the stderr output. cannot be null.
     * @param stdOutput The array to store the stdout output. cannot be null.
     * @param displayStdOut If true this will display stdout as well
     * @param waitforReaders if true, this will wait for the reader threads. 
     * @return the process return code.
     * @throws InterruptedException
     */
    private int grabProcessOutput(final Process process, final ArrayList<String> errorOutput,
            final ArrayList<String> stdOutput, boolean waitforReaders)
            throws InterruptedException {
        assert errorOutput != null;
        assert stdOutput != null;
        // read the lines as they come. if null is returned, it's
        // because the process finished
        Thread t1 = new Thread("") { //$NON-NLS-1$
            @Override
            public void run() {
                // create a buffer to read the stderr output
                InputStreamReader is = new InputStreamReader(process.getErrorStream());
                BufferedReader errReader = new BufferedReader(is);

                try {
                    while (true) {
                        String line = errReader.readLine();
                        if (line != null) {
                            Log.e(ADB, line);
                            errorOutput.add(line);
                        } else {
                            break;
                        }
                    }
                } catch (IOException e) {
                    // do nothing.
                }
            }
        };

        Thread t2 = new Thread("") { //$NON-NLS-1$
            @Override
            public void run() {
                InputStreamReader is = new InputStreamReader(process.getInputStream());
                BufferedReader outReader = new BufferedReader(is);

                try {
                    while (true) {
                        String line = outReader.readLine();
                        if (line != null) {
                            Log.d(ADB, line);
                            stdOutput.add(line);
                        } else {
                            break;
                        }
                    }
                } catch (IOException e) {
                    // do nothing.
                }
            }
        };

        t1.start();
        t2.start();

        // it looks like on windows process#waitFor() can return
        // before the thread have filled the arrays, so we wait for both threads and the
        // process itself.
        if (waitforReaders) {
            try {
                t1.join();
            } catch (InterruptedException e) {
            }
            try {
                t2.join();
            } catch (InterruptedException e) {
            }
        }

        // get the return code from the process
        return process.waitFor();
    }

    /**
     * Returns the singleton lock used by this class to protect any access to the listener.
     * <p/>
     * This includes adding/removing listeners, but also notifying listeners of new bridges,
     * devices, and clients.
     */
    static Object getLock() {
        return sLock;
    }
}
