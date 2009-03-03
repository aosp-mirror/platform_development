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

import com.android.ddmlib.Device.DeviceState;
import com.android.ddmlib.log.LogReceiver;

import java.io.IOException;
import java.util.Map;


/**
 *  A Device. It can be a physical device or an emulator.
 */
public interface IDevice {

    public final static String PROP_BUILD_VERSION = "ro.build.version.release";
    public final static String PROP_BUILD_VERSION_NUMBER = "ro.build.version.sdk";
    public final static String PROP_DEBUGGABLE = "ro.debuggable";
    /** Serial number of the first connected emulator. */
    public final static String FIRST_EMULATOR_SN = "emulator-5554"; //$NON-NLS-1$
    /** Device change bit mask: {@link DeviceState} change. */
    public static final int CHANGE_STATE = 0x0001;
    /** Device change bit mask: {@link Client} list change. */
    public static final int CHANGE_CLIENT_LIST = 0x0002;
    /** Device change bit mask: build info change. */
    public static final int CHANGE_BUILD_INFO = 0x0004;

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
     * @return <code>null</code> if the SyncService couldn't be created.
     */
    public SyncService getSyncService();

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

}
