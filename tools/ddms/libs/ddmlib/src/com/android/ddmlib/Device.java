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

import com.android.ddmlib.Client;
import com.android.ddmlib.log.LogReceiver;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A Device. It can be a physical device or an emulator.
 */
public final class Device {
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
    
    public final static String PROP_BUILD_VERSION = "ro.build.version.release";
    public final static String PROP_DEBUGGABLE = "ro.debuggable";

    /** Serial number of the first connected emulator. */
    public final static String FIRST_EMULATOR_SN = "emulator-5554"; //$NON-NLS-1$

    /** Emulator Serial Number regexp. */
    final static String RE_EMULATOR_SN = "emulator-(\\d+)"; //$NON-NLS-1$
    
    /** Device change bit mask: {@link DeviceState} change. */
    public static final int CHANGE_STATE = 0x0001;
    /** Device change bit mask: {@link Client} list change. */
    public static final int CHANGE_CLIENT_LIST = 0x0002;
    /** Device change bit mask: build info change. */
    public static final int CHANGE_BUILD_INFO = 0x0004;

    /** Serial number of the device */
    String serialNumber = null;

    /** State of the device. */
    DeviceState state = null;
    
    /** Device properties. */
    private final Map<String, String> mProperties = new HashMap<String, String>();

    private final ArrayList<Client> mClients = new ArrayList<Client>();
    private DeviceMonitor mMonitor;

    /**
     * Socket for the connection monitoring client connection/disconnection.
     */
    private SocketChannel mSocketChannel;
    
    /**
     * Returns the serial number of the device.
     */
    public String getSerialNumber() {
        return serialNumber;
    }
    
    /**
     * Returns the state of the device.
     */
    public DeviceState getState() {
        return state;
    }
    
    /**
     * Returns the device properties. It contains the whole output of 'getprop'
     */
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(mProperties);
    }

    /**
     * Returns the number of property for this device.
     */
    public int getPropertyCount() {
        return mProperties.size();
    }

    /**
     * Returns a property value.
     * @param name the name of the value to return.
     * @return the value or <code>null</code> if the property does not exist.
     */
    public String getProperty(String name) {
        return mProperties.get(name);
    }
    

    @Override
    public String toString() {
        return serialNumber;
    }

    /**
     * Returns if the device is ready.
     * @return <code>true</code> if {@link #getState()} returns {@link DeviceState#ONLINE}.
     */
    public boolean isOnline() {
        return state == DeviceState.ONLINE;
    }

    /**
     * Returns <code>true</code> if the device is an emulator.
     */
    public boolean isEmulator() {
        return serialNumber.matches(RE_EMULATOR_SN);
    }

    /**
     * Returns if the device is offline.
     * @return <code>true</code> if {@link #getState()} returns {@link DeviceState#OFFLINE}.
     */
    public boolean isOffline() {
        return state == DeviceState.OFFLINE;
    }

    /**
     * Returns if the device is in bootloader mode.
     * @return <code>true</code> if {@link #getState()} returns {@link DeviceState#BOOTLOADER}.
     */
    public boolean isBootLoader() {
        return state == DeviceState.BOOTLOADER;
    }

    /**
     * Returns whether the {@link Device} has {@link Client}s.
     */
    public boolean hasClients() {
        return mClients.size() > 0;
    }

    /**
     * Returns the array of clients.
     */
    public Client[] getClients() {
        synchronized (mClients) {
            return mClients.toArray(new Client[mClients.size()]);
        }
    }
    
    /**
     * Returns a {@link Client} by its application name.
     * @param applicationName the name of the application
     * @return the <code>Client</code> object or <code>null</code> if no match was found.
     */
    public Client getClient(String applicationName) {
        synchronized (mClients) {
            for (Client c : mClients) {
                if (applicationName.equals(c.getClientData().getClientDescription())) {
                    return c;
                }
            }

        }

        return null;
    }

    /**
     * Returns a {@link SyncService} object to push / pull files to and from the device.
     * @return <code>null</code> if the SyncService couldn't be created.
     */
    public SyncService getSyncService() {
        SyncService syncService = new SyncService(AndroidDebugBridge.sSocketAddr, this);
        if (syncService.openSync()) {
            return syncService;
         }

        return null;
    }

    /**
     * Returns a {@link FileListingService} for this device.
     */
    public FileListingService getFileListingService() {
        return new FileListingService(this);
    }

    /**
     * Takes a screen shot of the device and returns it as a {@link RawImage}.
     * @return the screenshot as a <code>RawImage</code> or <code>null</code> if
     * something went wrong.
     * @throws IOException
     */
    public RawImage getScreenshot() throws IOException {
        return AdbHelper.getFrameBuffer(AndroidDebugBridge.sSocketAddr, this);
    }

    /**
     * Executes a shell command on the device, and sends the result to a receiver.
     * @param command The command to execute
     * @param receiver The receiver object getting the result from the command.
     * @throws IOException
     */
    public void executeShellCommand(String command, IShellOutputReceiver receiver)
            throws IOException {
        AdbHelper.executeRemoteCommand(AndroidDebugBridge.sSocketAddr, command, this,
                receiver);
    }
    
    /**
     * Runs the event log service and outputs the event log to the {@link LogReceiver}.
     * @param receiver the receiver to receive the event log entries.
     * @throws IOException
     */
    public void runEventLogService(LogReceiver receiver) throws IOException {
        AdbHelper.runEventLogService(AndroidDebugBridge.sSocketAddr, this, receiver);
    }
    
    /**
     * Creates a port forwarding between a local and a remote port.
     * @param localPort the local port to forward
     * @param remotePort the remote port.
     * @return <code>true</code> if success.
     */
    public boolean createForward(int localPort, int remotePort) {
        try {
            return AdbHelper.createForward(AndroidDebugBridge.sSocketAddr, this,
                    localPort, remotePort);
        } catch (IOException e) {
            Log.e("adb-forward", e); //$NON-NLS-1$
            return false;
        }
    }

    /**
     * Removes a port forwarding between a local and a remote port.
     * @param localPort the local port to forward
     * @param remotePort the remote port.
     * @return <code>true</code> if success.
     */
    public boolean removeForward(int localPort, int remotePort) {
        try {
            return AdbHelper.removeForward(AndroidDebugBridge.sSocketAddr, this,
                    localPort, remotePort);
        } catch (IOException e) {
            Log.e("adb-remove-forward", e); //$NON-NLS-1$
            return false;
        }
    }

    /**
     * Returns the name of the client by pid or <code>null</code> if pid is unknown
     * @param pid the pid of the client.
     */
    public String getClientName(int pid) {
        synchronized (mClients) {
            for (Client c : mClients) {
                if (c.getClientData().getPid() == pid) {
                    return c.getClientData().getClientDescription();
                }
            }
        }

        return null;
    }


    Device(DeviceMonitor monitor) {
        mMonitor = monitor;
    }

    DeviceMonitor getMonitor() {
        return mMonitor;
    }

    void addClient(Client client) {
        synchronized (mClients) {
            mClients.add(client);
        }
    }

    List<Client> getClientList() {
        return mClients;
    }

    boolean hasClient(int pid) {
        synchronized (mClients) {
            for (Client client : mClients) {
                if (client.getClientData().getPid() == pid) {
                    return true;
                }
            }
        }

        return false;
    }
    
    void clearClientList() {
        synchronized (mClients) {
            mClients.clear();
        }
    }

    /**
     * Sets the client monitoring socket.
     * @param socketChannel the sockets
     */
    void setClientMonitoringSocket(SocketChannel socketChannel) {
        mSocketChannel = socketChannel;
    }

    /**
     * Returns the client monitoring socket.
     */
    SocketChannel getClientMonitoringSocket() {
        return mSocketChannel;
    }

    /**
     * Removes a {@link Client} from the list.
     * @param client the client to remove.
     * @param notify Whether or not to notify the listeners of a change.
     */
    void removeClient(Client client, boolean notify) {
        mMonitor.addPortToAvailableList(client.getDebuggerListenPort());
        synchronized (mClients) {
            mClients.remove(client);
        }
        if (notify) {
            mMonitor.getServer().deviceChanged(this, CHANGE_CLIENT_LIST);
        }
    }

    void update(int changeMask) {
        mMonitor.getServer().deviceChanged(this, changeMask);
    }

    void update(Client client, int changeMask) {
        mMonitor.getServer().clientChanged(client, changeMask);
    }

    void addProperty(String label, String value) {
        mProperties.put(label, value);
    }
}
