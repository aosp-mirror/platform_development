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

import com.android.ddmlib.ClientData.MethodProfilingStatus;
import com.android.ddmlib.DebugPortManager.IDebugPortProvider;
import com.android.ddmlib.AndroidDebugBridge.IClientChangeListener;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;

/**
 * This represents a single client, usually a DAlvik VM process.
 * <p/>This class gives access to basic client information, as well as methods to perform actions
 * on the client.
 * <p/>More detailed information, usually updated in real time, can be access through the
 * {@link ClientData} class. Each <code>Client</code> object has its own <code>ClientData</code>
 * accessed through {@link #getClientData()}.
 */
public class Client {

    private static final int SERVER_PROTOCOL_VERSION = 1;

    /** Client change bit mask: application name change */
    public static final int CHANGE_NAME                       = 0x0001;
    /** Client change bit mask: debugger status change */
    public static final int CHANGE_DEBUGGER_STATUS            = 0x0002;
    /** Client change bit mask: debugger port change */
    public static final int CHANGE_PORT                       = 0x0004;
    /** Client change bit mask: thread update flag change */
    public static final int CHANGE_THREAD_MODE                = 0x0008;
    /** Client change bit mask: thread data updated */
    public static final int CHANGE_THREAD_DATA                = 0x0010;
    /** Client change bit mask: heap update flag change */
    public static final int CHANGE_HEAP_MODE                  = 0x0020;
    /** Client change bit mask: head data updated */
    public static final int CHANGE_HEAP_DATA                  = 0x0040;
    /** Client change bit mask: native heap data updated */
    public static final int CHANGE_NATIVE_HEAP_DATA           = 0x0080;
    /** Client change bit mask: thread stack trace updated */
    public static final int CHANGE_THREAD_STACKTRACE          = 0x0100;
    /** Client change bit mask: allocation information updated */
    public static final int CHANGE_HEAP_ALLOCATIONS           = 0x0200;
    /** Client change bit mask: allocation information updated */
    public static final int CHANGE_HEAP_ALLOCATION_STATUS     = 0x0400;
    /** Client change bit mask: allocation information updated */
    public static final int CHANGE_METHOD_PROFILING_STATUS    = 0x0800;

    /** Client change bit mask: combination of {@link Client#CHANGE_NAME},
     * {@link Client#CHANGE_DEBUGGER_STATUS}, and {@link Client#CHANGE_PORT}.
     */
    public static final int CHANGE_INFO = CHANGE_NAME | CHANGE_DEBUGGER_STATUS | CHANGE_PORT;

    private SocketChannel mChan;

    // debugger we're associated with, if any
    private Debugger mDebugger;
    private int mDebuggerListenPort;

    // list of IDs for requests we have sent to the client
    private HashMap<Integer,ChunkHandler> mOutstandingReqs;

    // chunk handlers stash state data in here
    private ClientData mClientData;

    // User interface state.  Changing the value causes a message to be
    // sent to the client.
    private boolean mThreadUpdateEnabled;
    private boolean mHeapUpdateEnabled;

    /*
     * Read/write buffers.  We can get large quantities of data from the
     * client, e.g. the response to a "give me the list of all known classes"
     * request from the debugger.  Requests from the debugger, and from us,
     * are much smaller.
     *
     * Pass-through debugger traffic is sent without copying.  "mWriteBuffer"
     * is only used for data generated within Client.
     */
    private static final int INITIAL_BUF_SIZE = 2*1024;
    private static final int MAX_BUF_SIZE = 200*1024*1024;
    private ByteBuffer mReadBuffer;

    private static final int WRITE_BUF_SIZE = 256;
    private ByteBuffer mWriteBuffer;

    private Device mDevice;

    private int mConnState;

    private static final int ST_INIT         = 1;
    private static final int ST_NOT_JDWP     = 2;
    private static final int ST_AWAIT_SHAKE  = 10;
    private static final int ST_NEED_DDM_PKT = 11;
    private static final int ST_NOT_DDM      = 12;
    private static final int ST_READY        = 13;
    private static final int ST_ERROR        = 20;
    private static final int ST_DISCONNECTED = 21;


    /**
     * Create an object for a new client connection.
     *
     * @param device the device this client belongs to
     * @param chan the connected {@link SocketChannel}.
     * @param pid the client pid.
     */
    Client(Device device, SocketChannel chan, int pid) {
        mDevice = device;
        mChan = chan;

        mReadBuffer = ByteBuffer.allocate(INITIAL_BUF_SIZE);
        mWriteBuffer = ByteBuffer.allocate(WRITE_BUF_SIZE);

        mOutstandingReqs = new HashMap<Integer,ChunkHandler>();

        mConnState = ST_INIT;

        mClientData = new ClientData(pid);

        mThreadUpdateEnabled = DdmPreferences.getInitialThreadUpdate();
        mHeapUpdateEnabled = DdmPreferences.getInitialHeapUpdate();
    }

    /**
     * Returns a string representation of the {@link Client} object.
     */
    @Override
    public String toString() {
        return "[Client pid: " + mClientData.getPid() + "]";
    }

    /**
     * Returns the {@link IDevice} on which this Client is running.
     */
    public IDevice getDevice() {
        return mDevice;
    }

    /** Returns the {@link Device} on which this Client is running.
     */
    Device getDeviceImpl() {
        return mDevice;
    }

    /**
     * Returns the debugger port for this client.
     */
    public int getDebuggerListenPort() {
        return mDebuggerListenPort;
    }

    /**
     * Returns <code>true</code> if the client VM is DDM-aware.
     *
     * Calling here is only allowed after the connection has been
     * established.
     */
    public boolean isDdmAware() {
        switch (mConnState) {
            case ST_INIT:
            case ST_NOT_JDWP:
            case ST_AWAIT_SHAKE:
            case ST_NEED_DDM_PKT:
            case ST_NOT_DDM:
            case ST_ERROR:
            case ST_DISCONNECTED:
                return false;
            case ST_READY:
                return true;
            default:
                assert false;
                return false;
        }
    }

    /**
     * Returns <code>true</code> if a debugger is currently attached to the client.
     */
    public boolean isDebuggerAttached() {
        return mDebugger.isDebuggerAttached();
    }

    /**
     * Return the Debugger object associated with this client.
     */
    Debugger getDebugger() {
        return mDebugger;
    }

    /**
     * Returns the {@link ClientData} object containing this client information.
     */
    public ClientData getClientData() {
        return mClientData;
    }

    /**
     * Forces the client to execute its garbage collector.
     */
    public void executeGarbageCollector() {
        try {
            HandleHeap.sendHPGC(this);
        } catch (IOException ioe) {
            Log.w("ddms", "Send of HPGC message failed");
            // ignore
        }
    }

    /**
     * Makes the VM dump an HPROF file
     */
    public void dumpHprof() {
        try {
            String file = "/sdcard/" + mClientData.getClientDescription().replaceAll("\\:.*", "") +
                ".hprof";
            HandleHeap.sendHPDU(this, file);
        } catch (IOException e) {
            Log.w("ddms", "Send of HPDU message failed");
            // ignore
        }
    }

    public void toggleMethodProfiling() {
        try {
            if (mClientData.getMethodProfilingStatus() == MethodProfilingStatus.ON) {
                HandleProfiling.sendMPRE(this);
            } else {
                String file = "/sdcard/" + mClientData.getClientDescription().replaceAll("\\:.*", "") +
                ".trace";
                HandleProfiling.sendMPRS(this, file, 8*1024*1024, 0 /*flags*/);
            }
        } catch (IOException e) {
            Log.w("ddms", "Toggle method profiling failed");
            // ignore
        }
    }

    /**
     * Sends a request to the VM to send the enable status of the method profiling.
     * This is asynchronous.
     * <p/>The allocation status can be accessed by {@link ClientData#getAllocationStatus()}.
     * The notification that the new status is available will be received through
     * {@link IClientChangeListener#clientChanged(Client, int)} with a <code>changeMask</code>
     * containing the mask {@link #CHANGE_HEAP_ALLOCATION_STATUS}.
     */
    public void requestMethodProfilingStatus() {
        try {
            HandleHeap.sendREAQ(this);
        } catch (IOException e) {
            Log.e("ddmlib", e);
        }
    }


    /**
     * Enables or disables the thread update.
     * <p/>If <code>true</code> the VM will be able to send thread information. Thread information
     * must be requested with {@link #requestThreadUpdate()}.
     * @param enabled the enable flag.
     */
    public void setThreadUpdateEnabled(boolean enabled) {
        mThreadUpdateEnabled = enabled;
        if (enabled == false) {
            mClientData.clearThreads();
        }

        try {
            HandleThread.sendTHEN(this, enabled);
        } catch (IOException ioe) {
            // ignore it here; client will clean up shortly
            ioe.printStackTrace();
        }

        update(CHANGE_THREAD_MODE);
    }

    /**
     * Returns whether the thread update is enabled.
     */
    public boolean isThreadUpdateEnabled() {
        return mThreadUpdateEnabled;
    }

    /**
     * Sends a thread update request. This is asynchronous.
     * <p/>The thread info can be accessed by {@link ClientData#getThreads()}. The notification
     * that the new data is available will be received through
     * {@link IClientChangeListener#clientChanged(Client, int)} with a <code>changeMask</code>
     * containing the mask {@link #CHANGE_THREAD_DATA}.
     */
    public void requestThreadUpdate() {
        HandleThread.requestThreadUpdate(this);
    }

    /**
     * Sends a thread stack trace update request. This is asynchronous.
     * <p/>The thread info can be accessed by {@link ClientData#getThreads()} and
     * {@link ThreadInfo#getStackTrace()}.
     * <p/>The notification that the new data is available
     * will be received through {@link IClientChangeListener#clientChanged(Client, int)}
     * with a <code>changeMask</code> containing the mask {@link #CHANGE_THREAD_STACKTRACE}.
     */
    public void requestThreadStackTrace(int threadId) {
        HandleThread.requestThreadStackCallRefresh(this, threadId);
    }

    /**
     * Enables or disables the heap update.
     * <p/>If <code>true</code>, any GC will cause the client to send its heap information.
     * <p/>The heap information can be accessed by {@link ClientData#getVmHeapData()}.
     * <p/>The notification that the new data is available
     * will be received through {@link IClientChangeListener#clientChanged(Client, int)}
     * with a <code>changeMask</code> containing the value {@link #CHANGE_HEAP_DATA}.
     * @param enabled the enable flag
     */
    public void setHeapUpdateEnabled(boolean enabled) {
        mHeapUpdateEnabled = enabled;

        try {
            HandleHeap.sendHPIF(this,
                    enabled ? HandleHeap.HPIF_WHEN_EVERY_GC : HandleHeap.HPIF_WHEN_NEVER);

            HandleHeap.sendHPSG(this,
                    enabled ? HandleHeap.WHEN_GC : HandleHeap.WHEN_DISABLE,
                    HandleHeap.WHAT_MERGE);
        } catch (IOException ioe) {
            // ignore it here; client will clean up shortly
        }

        update(CHANGE_HEAP_MODE);
    }

    /**
     * Returns whether the heap update is enabled.
     * @see #setHeapUpdateEnabled(boolean)
     */
    public boolean isHeapUpdateEnabled() {
        return mHeapUpdateEnabled;
    }

    /**
     * Sends a native heap update request. this is asynchronous.
     * <p/>The native heap info can be accessed by {@link ClientData#getNativeAllocationList()}.
     * The notification that the new data is available will be received through
     * {@link IClientChangeListener#clientChanged(Client, int)} with a <code>changeMask</code>
     * containing the mask {@link #CHANGE_NATIVE_HEAP_DATA}.
     */
    public boolean requestNativeHeapInformation() {
        try {
            HandleNativeHeap.sendNHGT(this);
            return true;
        } catch (IOException e) {
            Log.e("ddmlib", e);
        }

        return false;
    }

    /**
     * Enables or disables the Allocation tracker for this client.
     * <p/>If enabled, the VM will start tracking allocation informations. A call to
     * {@link #requestAllocationDetails()} will make the VM sends the information about all the
     * allocations that happened between the enabling and the request.
     * @param enable
     * @see #requestAllocationDetails()
     */
    public void enableAllocationTracker(boolean enable) {
        try {
            HandleHeap.sendREAE(this, enable);
        } catch (IOException e) {
            Log.e("ddmlib", e);
        }
    }

    /**
     * Sends a request to the VM to send the enable status of the allocation tracking.
     * This is asynchronous.
     * <p/>The allocation status can be accessed by {@link ClientData#getAllocationStatus()}.
     * The notification that the new status is available will be received through
     * {@link IClientChangeListener#clientChanged(Client, int)} with a <code>changeMask</code>
     * containing the mask {@link #CHANGE_HEAP_ALLOCATION_STATUS}.
     */
    public void requestAllocationStatus() {
        try {
            HandleHeap.sendREAQ(this);
        } catch (IOException e) {
            Log.e("ddmlib", e);
        }
    }

    /**
     * Sends a request to the VM to send the information about all the allocations that have
     * happened since the call to {@link #enableAllocationTracker(boolean)} with <var>enable</var>
     * set to <code>null</code>. This is asynchronous.
     * <p/>The allocation information can be accessed by {@link ClientData#getAllocations()}.
     * The notification that the new data is available will be received through
     * {@link IClientChangeListener#clientChanged(Client, int)} with a <code>changeMask</code>
     * containing the mask {@link #CHANGE_HEAP_ALLOCATIONS}.
     */
    public void requestAllocationDetails() {
        try {
            HandleHeap.sendREAL(this);
        } catch (IOException e) {
            Log.e("ddmlib", e);
        }
    }

    /**
     * Sends a kill message to the VM.
     */
    public void kill() {
        try {
            HandleExit.sendEXIT(this, 1);
        } catch (IOException ioe) {
            Log.w("ddms", "Send of EXIT message failed");
            // ignore
        }
    }

    /**
     * Registers the client with a Selector.
     */
    void register(Selector sel) throws IOException {
        if (mChan != null) {
            mChan.register(sel, SelectionKey.OP_READ, this);
        }
    }

    /**
     * Sets the client to accept debugger connection on the "selected debugger port".
     *
     * @see AndroidDebugBridge#setSelectedClient(Client)
     * @see DdmPreferences#setSelectedDebugPort(int)
     */
    public void setAsSelectedClient() {
        MonitorThread monitorThread = MonitorThread.getInstance();
        if (monitorThread != null) {
            monitorThread.setSelectedClient(this);
        }
    }

    /**
     * Returns whether this client is the current selected client, accepting debugger connection
     * on the "selected debugger port".
     *
     * @see #setAsSelectedClient()
     * @see AndroidDebugBridge#setSelectedClient(Client)
     * @see DdmPreferences#setSelectedDebugPort(int)
     */
    public boolean isSelectedClient() {
        MonitorThread monitorThread = MonitorThread.getInstance();
        if (monitorThread != null) {
            return monitorThread.getSelectedClient() == this;
        }

        return false;
    }

    /**
     * Tell the client to open a server socket channel and listen for
     * connections on the specified port.
     */
    void listenForDebugger(int listenPort) throws IOException {
        mDebuggerListenPort = listenPort;
        mDebugger = new Debugger(this, listenPort);
    }

    /**
     * Initiate the JDWP handshake.
     *
     * On failure, closes the socket and returns false.
     */
    boolean sendHandshake() {
        assert mWriteBuffer.position() == 0;

        try {
            // assume write buffer can hold 14 bytes
            JdwpPacket.putHandshake(mWriteBuffer);
            int expectedLen = mWriteBuffer.position();
            mWriteBuffer.flip();
            if (mChan.write(mWriteBuffer) != expectedLen)
                throw new IOException("partial handshake write");
        }
        catch (IOException ioe) {
            Log.e("ddms-client", "IO error during handshake: " + ioe.getMessage());
            mConnState = ST_ERROR;
            close(true /* notify */);
            return false;
        }
        finally {
            mWriteBuffer.clear();
        }

        mConnState = ST_AWAIT_SHAKE;

        return true;
    }


    /**
     * Send a non-DDM packet to the client.
     *
     * Equivalent to sendAndConsume(packet, null).
     */
    void sendAndConsume(JdwpPacket packet) throws IOException {
        sendAndConsume(packet, null);
    }

    /**
     * Send a DDM packet to the client.
     *
     * Ideally, we can do this with a single channel write.  If that doesn't
     * happen, we have to prevent anybody else from writing to the channel
     * until this packet completes, so we synchronize on the channel.
     *
     * Another goal is to avoid unnecessary buffer copies, so we write
     * directly out of the JdwpPacket's ByteBuffer.
     */
    void sendAndConsume(JdwpPacket packet, ChunkHandler replyHandler)
        throws IOException {

        if (mChan == null) {
            // can happen for e.g. THST packets
            Log.v("ddms", "Not sending packet -- client is closed");
            return;
        }

        if (replyHandler != null) {
            /*
             * Add the ID to the list of outstanding requests.  We have to do
             * this before sending the packet, in case the response comes back
             * before our thread returns from the packet-send function.
             */
            addRequestId(packet.getId(), replyHandler);
        }

        synchronized (mChan) {
            try {
                packet.writeAndConsume(mChan);
            }
            catch (IOException ioe) {
                removeRequestId(packet.getId());
                throw ioe;
            }
        }
    }

    /**
     * Forward the packet to the debugger (if still connected to one).
     *
     * Consumes the packet.
     */
    void forwardPacketToDebugger(JdwpPacket packet)
        throws IOException {

        Debugger dbg = mDebugger;

        if (dbg == null) {
            Log.d("ddms", "Discarding packet");
            packet.consume();
        } else {
            dbg.sendAndConsume(packet);
        }
    }

    /**
     * Read data from our channel.
     *
     * This is called when data is known to be available, and we don't yet
     * have a full packet in the buffer.  If the buffer is at capacity,
     * expand it.
     */
    void read()
        throws IOException, BufferOverflowException {

        int count;

        if (mReadBuffer.position() == mReadBuffer.capacity()) {
            if (mReadBuffer.capacity() * 2 > MAX_BUF_SIZE) {
                Log.e("ddms", "Exceeded MAX_BUF_SIZE!");
                throw new BufferOverflowException();
            }
            Log.d("ddms", "Expanding read buffer to "
                + mReadBuffer.capacity() * 2);

            ByteBuffer newBuffer = ByteBuffer.allocate(mReadBuffer.capacity() * 2);

            // copy entire buffer to new buffer
            mReadBuffer.position(0);
            newBuffer.put(mReadBuffer);  // leaves "position" at end of copied

            mReadBuffer = newBuffer;
        }

        count = mChan.read(mReadBuffer);
        if (count < 0)
            throw new IOException("read failed");

        if (Log.Config.LOGV) Log.v("ddms", "Read " + count + " bytes from " + this);
        //Log.hexDump("ddms", Log.DEBUG, mReadBuffer.array(),
        //    mReadBuffer.arrayOffset(), mReadBuffer.position());
    }

    /**
     * Return information for the first full JDWP packet in the buffer.
     *
     * If we don't yet have a full packet, return null.
     *
     * If we haven't yet received the JDWP handshake, we watch for it here
     * and consume it without admitting to have done so.  Upon receipt
     * we send out the "HELO" message, which is why this can throw an
     * IOException.
     */
    JdwpPacket getJdwpPacket() throws IOException {

        /*
         * On entry, the data starts at offset 0 and ends at "position".
         * "limit" is set to the buffer capacity.
         */
        if (mConnState == ST_AWAIT_SHAKE) {
            /*
             * The first thing we get from the client is a response to our
             * handshake.  It doesn't look like a packet, so we have to
             * handle it specially.
             */
            int result;

            result = JdwpPacket.findHandshake(mReadBuffer);
            //Log.v("ddms", "findHand: " + result);
            switch (result) {
                case JdwpPacket.HANDSHAKE_GOOD:
                    Log.d("ddms",
                        "Good handshake from client, sending HELO to " + mClientData.getPid());
                    JdwpPacket.consumeHandshake(mReadBuffer);
                    mConnState = ST_NEED_DDM_PKT;
                    HandleHello.sendHelloCommands(this, SERVER_PROTOCOL_VERSION);
                    // see if we have another packet in the buffer
                    return getJdwpPacket();
                case JdwpPacket.HANDSHAKE_BAD:
                    Log.d("ddms", "Bad handshake from client");
                    if (MonitorThread.getInstance().getRetryOnBadHandshake()) {
                        // we should drop the client, but also attempt to reopen it.
                        // This is done by the DeviceMonitor.
                        mDevice.getMonitor().addClientToDropAndReopen(this,
                                IDebugPortProvider.NO_STATIC_PORT);
                    } else {
                        // mark it as bad, close the socket, and don't retry
                        mConnState = ST_NOT_JDWP;
                        close(true /* notify */);
                    }
                    break;
                case JdwpPacket.HANDSHAKE_NOTYET:
                    Log.d("ddms", "No handshake from client yet.");
                    break;
                default:
                    Log.e("ddms", "Unknown packet while waiting for client handshake");
            }
            return null;
        } else if (mConnState == ST_NEED_DDM_PKT ||
            mConnState == ST_NOT_DDM ||
            mConnState == ST_READY) {
            /*
             * Normal packet traffic.
             */
            if (mReadBuffer.position() != 0) {
                if (Log.Config.LOGV) Log.v("ddms",
                    "Checking " + mReadBuffer.position() + " bytes");
            }
            return JdwpPacket.findPacket(mReadBuffer);
        } else {
            /*
             * Not expecting data when in this state.
             */
            Log.e("ddms", "Receiving data in state = " + mConnState);
        }

        return null;
    }

    /*
     * Add the specified ID to the list of request IDs for which we await
     * a response.
     */
    private void addRequestId(int id, ChunkHandler handler) {
        synchronized (mOutstandingReqs) {
            if (Log.Config.LOGV) Log.v("ddms",
                "Adding req 0x" + Integer.toHexString(id) +" to set");
            mOutstandingReqs.put(id, handler);
        }
    }

    /*
     * Remove the specified ID from the list, if present.
     */
    void removeRequestId(int id) {
        synchronized (mOutstandingReqs) {
            if (Log.Config.LOGV) Log.v("ddms",
                "Removing req 0x" + Integer.toHexString(id) + " from set");
            mOutstandingReqs.remove(id);
        }

        //Log.w("ddms", "Request " + Integer.toHexString(id)
        //    + " could not be removed from " + this);
    }

    /**
     * Determine whether this is a response to a request we sent earlier.
     * If so, return the ChunkHandler responsible.
     */
    ChunkHandler isResponseToUs(int id) {

        synchronized (mOutstandingReqs) {
            ChunkHandler handler = mOutstandingReqs.get(id);
            if (handler != null) {
                if (Log.Config.LOGV) Log.v("ddms",
                    "Found 0x" + Integer.toHexString(id)
                    + " in request set - " + handler);
                return handler;
            }
        }

        return null;
    }

    /**
     * An earlier request resulted in a failure.  This is the expected
     * response to a HELO message when talking to a non-DDM client.
     */
    void packetFailed(JdwpPacket reply) {
        if (mConnState == ST_NEED_DDM_PKT) {
            Log.d("ddms", "Marking " + this + " as non-DDM client");
            mConnState = ST_NOT_DDM;
        } else if (mConnState != ST_NOT_DDM) {
            Log.w("ddms", "WEIRD: got JDWP failure packet on DDM req");
        }
    }

    /**
     * The MonitorThread calls this when it sees a DDM request or reply.
     * If we haven't seen a DDM packet before, we advance the state to
     * ST_READY and return "false".  Otherwise, just return true.
     *
     * The idea is to let the MonitorThread know when we first see a DDM
     * packet, so we can send a broadcast to the handlers when a client
     * connection is made.  This method is synchronized so that we only
     * send the broadcast once.
     */
    synchronized boolean ddmSeen() {
        if (mConnState == ST_NEED_DDM_PKT) {
            mConnState = ST_READY;
            return false;
        } else if (mConnState != ST_READY) {
            Log.w("ddms", "WEIRD: in ddmSeen with state=" + mConnState);
        }
        return true;
    }

    /**
     * Close the client socket channel.  If there is a debugger associated
     * with us, close that too.
     *
     * Closing a channel automatically unregisters it from the selector.
     * However, we have to iterate through the selector loop before it
     * actually lets them go and allows the file descriptors to close.
     * The caller is expected to manage that.
     * @param notify Whether or not to notify the listeners of a change.
     */
    void close(boolean notify) {
        Log.d("ddms", "Closing " + this.toString());

        mOutstandingReqs.clear();

        try {
            if (mChan != null) {
                mChan.close();
                mChan = null;
            }

            if (mDebugger != null) {
                mDebugger.close();
                mDebugger = null;
            }
        }
        catch (IOException ioe) {
            Log.w("ddms", "failed to close " + this);
            // swallow it -- not much else to do
        }

        mDevice.removeClient(this, notify);
    }

    /**
     * Returns whether this {@link Client} has a valid connection to the application VM.
     */
    public boolean isValid() {
        return mChan != null;
    }

    void update(int changeMask) {
        mDevice.update(this, changeMask);
    }
}

