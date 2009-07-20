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

package com.android.ddmlib;

import com.android.ddmlib.log.LogReceiver;

import java.io.IOException;
import java.util.Map;


/**
 *  A Device. It can be a physical device or an emulator.
 */
public interface IDevice {

    public final static String PROP_BUILD_VERSION = "ro.build.version.release";
    public final static String PROP_BUILD_API_LEVEL = "ro.build.version.sdk";
    public final static String PROP_BUILD_CODENAME = "ro.build.version.codename";

    public final static String PROP_DEBUGGABLE = "ro.debuggable";

    /** Serial number of the first connected emulator. */
    public final static String FIRST_EMULATOR_SN = "emulator-5554"; //$NON-NLS-1$
    /** Device change bit mask: {@link DeviceState} change. */
    public static final int CHANGE_STATE = 0x0001;
    /** Device change bit mask: {@link Client} list change. */
    public static final int CHANGE_CLIENT_LIST = 0x0002;
    /** Device change bit mask: build info change. */
    public static final int CHANGE_BUILD_INFO = 0x0004;

    /** @deprecated Use {@link #PROP_BUILD_API_LEVEL}. */
    public final static String PROP_BUILD_VERSION_NUMBER = PROP_BUILD_API_LEVEL;

    /**
     * The state of a device.
     */
    public static enum DeviceState {
        BOOTLOADER("bootloader"), //$NON-NLS-1$
        OFFLINE("offline"), //$NON-NLS-1$
        ONLINE("device"); //$NON-NLS-1$

        private String mState;

        DeviceState(String state) {
            mState = state;
        }

        /**
         * Returns a {@link DeviceState} from the string returned by <code>adb devices</code>.
         * @param state the device state.
         * @return a {@link DeviceState} object or <code>null</code> if the state is unknown.
         */
        public static DeviceState getState(String state) {
            for (DeviceState deviceState : values()) {
                if (deviceState.mState.equals(state)) {
                    return deviceState;
                }
            }
            return null;
        }
    }

    /**
     * Returns the serial number of the device.
     */
    public String getSerialNumber();

    /**
     * Returns the name of the AVD the emulator is running.
     * <p/>This is only valid if {@link #isEmulator()} returns true.
     * <p/>If the emulator is not running any AVD (for instance it's running from an Android source
     * tree build), this method will return "<code>&lt;build&gt;</code>".
     * @return the name of the AVD or <code>null</code> if there isn't any.
     */
    public String getAvdName();

    /**
     * Returns the state of the device.
     */
    public DeviceState getState();

    /**
     * Returns the device properties. It contains the whole output of 'getprop'
     */
    public Map<String, String> getProperties();

    /**
     * Returns the number of property for this device.
     */
    public int getPropertyCount();

    /**
     * Returns a property value.
     * @param name the name of the value to return.
     * @return the value or <code>null</code> if the property does not exist.
     */
    public String getProperty(String name);

    /**
     * Returns if the device is ready.
     * @return <code>true</code> if {@link #getState()} returns {@link DeviceState#ONLINE}.
     */
    public boolean isOnline();

    /**
     * Returns <code>true</code> if the device is an emulator.
     */
    public boolean isEmulator();

    /**
     * Returns if the device is offline.
     * @return <code>true</code> if {@link #getState()} returns {@link DeviceState#OFFLINE}.
     */
    public boolean isOffline();

    /**
     * Returns if the device is in bootloader mode.
     * @return <code>true</code> if {@link #getState()} returns {@link DeviceState#BOOTLOADER}.
     */
    public boolean isBootLoader();

    /**
     * Returns whether the {@link Device} has {@link Client}s.
     */
    public boolean hasClients();

    /**
     * Returns the array of clients.
     */
    public Client[] getClients();

    /**
     * Returns a {@link Client} by its application name.
     * @param applicationName the name of the application
     * @return the <code>Client</code> object or <code>null</code> if no match was found.
     */
    public Client getClient(String applicationName);

    /**
     * Returns a {@link SyncService} object to push / pull files to and from the device.
     * @return <code>null</code> if the SyncService couldn't be created. This can happen if adb
     * refuse to open the connection because the {@link IDevice} is invalid (or got disconnected).
     * @throws IOException if the connection with adb failed.
     */
    public SyncService getSyncService() throws IOException;

    /**
     * Returns a {@link FileListingService} for this device.
     */
    public FileListingService getFileListingService();

    /**
     * Takes a screen shot of the device and returns it as a {@link RawImage}.
     * @return the screenshot as a <code>RawImage</code> or <code>null</code> if
     * something went wrong.
     * @throws IOException
     */
    public RawImage getScreenshot() throws IOException;

    /**
     * Executes a shell command on the device, and sends the result to a receiver.
     * @param command The command to execute
     * @param receiver The receiver object getting the result from the command.
     * @throws IOException
     */
    public void executeShellCommand(String command,
            IShellOutputReceiver receiver) throws IOException;

    /**
     * Runs the event log service and outputs the event log to the {@link LogReceiver}.
     * @param receiver the receiver to receive the event log entries.
     * @throws IOException
     */
    public void runEventLogService(LogReceiver receiver) throws IOException;

    /**
     * Runs the log service for the given log and outputs the log to the {@link LogReceiver}.
     * @param logname the logname of the log to read from.
     * @param receiver the receiver to receive the event log entries.
     * @throws IOException
     */
    public void runLogService(String logname, LogReceiver receiver) throws IOException;

    /**
     * Creates a port forwarding between a local and a remote port.
     * @param localPort the local port to forward
     * @param remotePort the remote port.
     * @return <code>true</code> if success.
     */
    public boolean createForward(int localPort, int remotePort);

    /**
     * Removes a port forwarding between a local and a remote port.
     * @param localPort the local port to forward
     * @param remotePort the remote port.
     * @return <code>true</code> if success.
     */
    public boolean removeForward(int localPort, int remotePort);

    /**
     * Returns the name of the client by pid or <code>null</code> if pid is unknown
     * @param pid the pid of the client.
     */
    public String getClientName(int pid);

    /**
     * Installs an Android application on device.
     * This is a helper method that combines the syncPackageToDevice, installRemotePackage,
     * and removePackage steps
     * @param packageFilePath the absolute file system path to file on local host to install
     * @param reinstall set to <code>true</code> if re-install of app should be performed
     * @return a {@link String} with an error code, or <code>null</code> if success.
     * @throws IOException
     */
    public String installPackage(String packageFilePath, boolean reinstall)  throws IOException;

    /**
     * Pushes a file to device
     * @param localFilePath the absolute path to file on local host
     * @return {@link String} destination path on device for file
     * @throws IOException if fatal error occurred when pushing file
     */
    public String syncPackageToDevice(String localFilePath)
            throws IOException;

    /**
     * Installs the application package that was pushed to a temporary location on the device.
     * @param remoteFilePath absolute file path to package file on device
     * @param reinstall set to <code>true</code> if re-install of app should be performed
     * @throws InstallException if installation failed
     */
    public String installRemotePackage(String remoteFilePath, boolean reinstall)
            throws IOException;

    /**
     * Remove a file from device
     * @param remoteFilePath path on device of file to remove
     * @throws IOException if file removal failed
     */
    public void removeRemotePackage(String remoteFilePath) throws IOException;

    /**
     * Uninstall an package from the device.
     * @param packageName the Android application package name to uninstall
     * @return a {@link String} with an error code, or <code>null</code> if success.
     * @throws IOException
     */
    public String uninstallPackage(String packageName) throws IOException;

}
