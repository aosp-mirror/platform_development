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
 *
 * TODO: make this class package-protected, and shift all callers to use IDevice
 */
public final class Device implements IDevice {
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

    /** Emulator Serial Number regexp. */
    final static String RE_EMULATOR_SN = "emulator-(\\d+)"; //$NON-NLS-1$

    /** Serial number of the device */
    String serialNumber = null;

    /** Name of the AVD */
    String mAvdName = null;

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

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.IDevice#getSerialNumber()
     */
    public String getSerialNumber() {
        return serialNumber;
    }

    public String getAvdName() {
        return mAvdName;
    }


    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.IDevice#getState()
     */
    public DeviceState getState() {
        return state;
    }

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.IDevice#getProperties()
     */
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(mProperties);
    }

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.IDevice#getPropertyCount()
     */
    public int getPropertyCount() {
        return mProperties.size();
    }

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.IDevice#getProperty(java.lang.String)
     */
    public String getProperty(String name) {
        return mProperties.get(name);
    }


    @Override
    public String toString() {
        return serialNumber;
    }

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.IDevice#isOnline()
     */
    public boolean isOnline() {
        return state == DeviceState.ONLINE;
    }

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.IDevice#isEmulator()
     */
    public boolean isEmulator() {
        return serialNumber.matches(RE_EMULATOR_SN);
    }

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.IDevice#isOffline()
     */
    public boolean isOffline() {
        return state == DeviceState.OFFLINE;
    }

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.IDevice#isBootLoader()
     */
    public boolean isBootLoader() {
        return state == DeviceState.BOOTLOADER;
    }

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.IDevice#hasClients()
     */
    public boolean hasClients() {
        return mClients.size() > 0;
    }

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.IDevice#getClients()
     */
    public Client[] getClients() {
        synchronized (mClients) {
            return mClients.toArray(new Client[mClients.size()]);
        }
    }

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.IDevice#getClient(java.lang.String)
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

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.IDevice#getSyncService()
     */
    public SyncService getSyncService() {
        SyncService syncService = new SyncService(AndroidDebugBridge.sSocketAddr, this);
        if (syncService.openSync()) {
            return syncService;
         }

        return null;
    }

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.IDevice#getFileListingService()
     */
    public FileListingService getFileListingService() {
        return new FileListingService(this);
    }

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.IDevice#getScreenshot()
     */
    public RawImage getScreenshot() throws IOException {
        return AdbHelper.getFrameBuffer(AndroidDebugBridge.sSocketAddr, this);
    }

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.IDevice#executeShellCommand(java.lang.String, com.android.ddmlib.IShellOutputReceiver)
     */
    public void executeShellCommand(String command, IShellOutputReceiver receiver)
            throws IOException {
        AdbHelper.executeRemoteCommand(AndroidDebugBridge.sSocketAddr, command, this,
                receiver);
    }

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.IDevice#runEventLogService(com.android.ddmlib.log.LogReceiver)
     */
    public void runEventLogService(LogReceiver receiver) throws IOException {
        AdbHelper.runEventLogService(AndroidDebugBridge.sSocketAddr, this, receiver);
    }

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.IDevice#runLogService(com.android.ddmlib.log.LogReceiver)
     */
    public void runLogService(String logname,
            LogReceiver receiver) throws IOException {
        AdbHelper.runLogService(AndroidDebugBridge.sSocketAddr, this, logname, receiver);
    }

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.IDevice#createForward(int, int)
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

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.IDevice#removeForward(int, int)
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

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.IDevice#getClientName(int)
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
