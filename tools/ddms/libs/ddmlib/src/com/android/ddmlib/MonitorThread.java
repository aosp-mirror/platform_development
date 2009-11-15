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


import com.android.ddmlib.DebugPortManager.IDebugPortProvider;
import com.android.ddmlib.Log.LogLevel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.NotYetBoundException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Monitor open connections.
 */
final class MonitorThread extends Thread {

    // For broadcasts to message handlers
    //private static final int CLIENT_CONNECTED = 1;

    private static final int CLIENT_READY = 2;

    private static final int CLIENT_DISCONNECTED = 3;

    private volatile boolean mQuit = false;

    // List of clients we're paying attention to
    private ArrayList<Client> mClientList;

    // The almighty mux
    private Selector mSelector;

    // Map chunk types to handlers
    private HashMap<Integer, ChunkHandler> mHandlerMap;

    // port for "debug selected"
    private ServerSocketChannel mDebugSelectedChan;

    private int mNewDebugSelectedPort;

    private int mDebugSelectedPort = -1;

    /**
     * "Selected" client setup to answer debugging connection to the mNewDebugSelectedPort port.
     */
    private Client mSelectedClient = null;

    // singleton
    private static MonitorThread mInstance;

    /**
     * Generic constructor.
     */
    private MonitorThread() {
        super("Monitor");
        mClientList = new ArrayList<Client>();
        mHandlerMap = new HashMap<Integer, ChunkHandler>();

        mNewDebugSelectedPort = DdmPreferences.getSelectedDebugPort();
    }

    /**
     * Creates and return the singleton instance of the client monitor thread.
     */
    static MonitorThread createInstance() {
        return mInstance = new MonitorThread();
    }

    /**
     * Get singleton instance of the client monitor thread.
     */
    static MonitorThread getInstance() {
        return mInstance;
    }


    /**
     * Sets or changes the port number for "debug selected".
     */
    synchronized void setDebugSelectedPort(int port) throws IllegalStateException {
        if (mInstance == null) {
            return;
        }

        if (AndroidDebugBridge.getClientSupport() == false) {
            return;
        }

        if (mDebugSelectedChan != null) {
            Log.d("ddms", "Changing debug-selected port to " + port);
            mNewDebugSelectedPort = port;
            wakeup();
        } else {
            // we set mNewDebugSelectedPort instead of mDebugSelectedPort so that it's automatically
            // opened on the first run loop.
            mNewDebugSelectedPort = port;
        }
    }

    /**
     * Sets the client to accept debugger connection on the custom "Selected debug port".
     * @param selectedClient the client. Can be null.
     */
    synchronized void setSelectedClient(Client selectedClient) {
        if (mInstance == null) {
            return;
        }

        if (mSelectedClient != selectedClient) {
            Client oldClient = mSelectedClient;
            mSelectedClient = selectedClient;

            if (oldClient != null) {
                oldClient.update(Client.CHANGE_PORT);
            }

            if (mSelectedClient != null) {
                mSelectedClient.update(Client.CHANGE_PORT);
            }
        }
    }

    /**
     * Returns the client accepting debugger connection on the custom "Selected debug port".
     */
    Client getSelectedClient() {
        return mSelectedClient;
    }


    /**
     * Returns "true" if we want to retry connections to clients if we get a bad
     * JDWP handshake back, "false" if we want to just mark them as bad and
     * leave them alone.
     */
    boolean getRetryOnBadHandshake() {
        return true; // TODO? make configurable
    }

    /**
     * Get an array of known clients.
     */
    Client[] getClients() {
        synchronized (mClientList) {
            return mClientList.toArray(new Client[0]);
        }
    }

    /**
     * Register "handler" as the handler for type "type".
     */
    synchronized void registerChunkHandler(int type, ChunkHandler handler) {
        if (mInstance == null) {
            return;
        }

        synchronized (mHandlerMap) {
            if (mHandlerMap.get(type) == null) {
                mHandlerMap.put(type, handler);
            }
        }
    }

    /**
     * Watch for activity from clients and debuggers.
     */
    @Override
    public void run() {
        Log.d("ddms", "Monitor is up");

        // create a selector
        try {
            mSelector = Selector.open();
        } catch (IOException ioe) {
            Log.logAndDisplay(LogLevel.ERROR, "ddms",
                    "Failed to initialize Monitor Thread: " + ioe.getMessage());
            return;
        }

        while (!mQuit) {

            try {
                /*
                 * sync with new registrations: we wait until addClient is done before going through
                 * and doing mSelector.select() again.
                 * @see {@link #addClient(Client)}
                 */
                synchronized (mClientList) {
                }

                // (re-)open the "debug selected" port, if it's not opened yet or
                // if the port changed.
                try {
                    if (AndroidDebugBridge.getClientSupport()) {
                        if ((mDebugSelectedChan == null ||
                                mNewDebugSelectedPort != mDebugSelectedPort) &&
                                mNewDebugSelectedPort != -1) {
                            if (reopenDebugSelectedPort()) {
                                mDebugSelectedPort = mNewDebugSelectedPort;
                            }
                        }
                    }
                } catch (IOException ioe) {
                    Log.e("ddms",
                            "Failed to reopen debug port for Selected Client to: " + mNewDebugSelectedPort);
                    Log.e("ddms", ioe);
                    mNewDebugSelectedPort = mDebugSelectedPort; // no retry
                }

                int count;
                try {
                    count = mSelector.select();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    continue;
                } catch (CancelledKeyException cke) {
                    continue;
                }

                if (count == 0) {
                    // somebody called wakeup() ?
                    // Log.i("ddms", "selector looping");
                    continue;
                }

                Set<SelectionKey> keys = mSelector.selectedKeys();
                Iterator<SelectionKey> iter = keys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    try {
                        if (key.attachment() instanceof Client) {
                            processClientActivity(key);
                        }
                        else if (key.attachment() instanceof Debugger) {
                            processDebuggerActivity(key);
                        }
                        else if (key.attachment() instanceof MonitorThread) {
                            processDebugSelectedActivity(key);
                        }
                        else {
                            Log.e("ddms", "unknown activity key");
                        }
                    } catch (Exception e) {
                        // we don't want to have our thread be killed because of any uncaught
                        // exception, so we intercept all here.
                        Log.e("ddms", "Exception during activity from Selector.");
                        Log.e("ddms", e);
                    }
                }
            } catch (Exception e) {
                // we don't want to have our thread be killed because of any uncaught
                // exception, so we intercept all here.
                Log.e("ddms", "Exception MonitorThread.run()");
                Log.e("ddms", e);
            }
        }
    }


    /**
     * Returns the port on which the selected client listen for debugger
     */
    int getDebugSelectedPort() {
        return mDebugSelectedPort;
    }

    /*
     * Something happened. Figure out what.
     */
    private void processClientActivity(SelectionKey key) {
        Client client = (Client)key.attachment();

        try {
            if (key.isReadable() == false || key.isValid() == false) {
                Log.d("ddms", "Invalid key from " + client + ". Dropping client.");
                dropClient(client, true /* notify */);
                return;
            }

            client.read();

            /*
             * See if we have a full packet in the buffer. It's possible we have
             * more than one packet, so we have to loop.
             */
            JdwpPacket packet = client.getJdwpPacket();
            while (packet != null) {
                if (packet.isDdmPacket()) {
                    // unsolicited DDM request - hand it off
                    assert !packet.isReply();
                    callHandler(client, packet, null);
                    packet.consume();
                } else if (packet.isReply()
                        && client.isResponseToUs(packet.getId()) != null) {
                    // reply to earlier DDM request
                    ChunkHandler handler = client
                            .isResponseToUs(packet.getId());
                    if (packet.isError())
                        client.packetFailed(packet);
                    else if (packet.isEmpty())
                        Log.d("ddms", "Got empty reply for 0x"
                                + Integer.toHexString(packet.getId())
                                + " from " + client);
                    else
                        callHandler(client, packet, handler);
                    packet.consume();
                    client.removeRequestId(packet.getId());
                } else {
                    Log.v("ddms", "Forwarding client "
                            + (packet.isReply() ? "reply" : "event") + " 0x"
                            + Integer.toHexString(packet.getId()) + " to "
                            + client.getDebugger());
                    client.forwardPacketToDebugger(packet);
                }

                // find next
                packet = client.getJdwpPacket();
            }
        } catch (CancelledKeyException e) {
            // key was canceled probably due to a disconnected client before we could
            // read stuff coming from the client, so we drop it.
            dropClient(client, true /* notify */);
        } catch (IOException ex) {
            // something closed down, no need to print anything. The client is simply dropped.
            dropClient(client, true /* notify */);
        } catch (Exception ex) {
            Log.e("ddms", ex);

            /* close the client; automatically un-registers from selector */
            dropClient(client, true /* notify */);

            if (ex instanceof BufferOverflowException) {
                Log.w("ddms",
                        "Client data packet exceeded maximum buffer size "
                                + client);
            } else {
                // don't know what this is, display it
                Log.e("ddms", ex);
            }
        }
    }

    /*
     * Process an incoming DDM packet. If this is a reply to an earlier request,
     * "handler" will be set to the handler responsible for the original
     * request. The spec allows a JDWP message to include multiple DDM chunks.
     */
    private void callHandler(Client client, JdwpPacket packet,
            ChunkHandler handler) {

        // on first DDM packet received, broadcast a "ready" message
        if (!client.ddmSeen())
            broadcast(CLIENT_READY, client);

        ByteBuffer buf = packet.getPayload();
        int type, length;
        boolean reply = true;

        type = buf.getInt();
        length = buf.getInt();

        if (handler == null) {
            // not a reply, figure out who wants it
            synchronized (mHandlerMap) {
                handler = mHandlerMap.get(type);
                reply = false;
            }
        }

        if (handler == null) {
            Log.w("ddms", "Received unsupported chunk type "
                    + ChunkHandler.name(type) + " (len=" + length + ")");
        } else {
            Log.d("ddms", "Calling handler for " + ChunkHandler.name(type)
                    + " [" + handler + "] (len=" + length + ")");
            ByteBuffer ibuf = buf.slice();
            ByteBuffer roBuf = ibuf.asReadOnlyBuffer(); // enforce R/O
            roBuf.order(ChunkHandler.CHUNK_ORDER);
            // do the handling of the chunk synchronized on the client list
            // to be sure there's no concurrency issue when we look for HOME
            // in hasApp()
            synchronized (mClientList) {
                handler.handleChunk(client, type, roBuf, reply, packet.getId());
            }
        }
    }

    /**
     * Drops a client from the monitor.
     * <p/>This will lock the {@link Client} list of the {@link Device} running <var>client</var>.
     * @param client
     * @param notify
     */
    synchronized void dropClient(Client client, boolean notify) {
        if (mInstance == null) {
            return;
        }

        synchronized (mClientList) {
            if (mClientList.remove(client) == false) {
                return;
            }
        }
        client.close(notify);
        broadcast(CLIENT_DISCONNECTED, client);

        /*
         * http://forum.java.sun.com/thread.jspa?threadID=726715&start=0
         * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5073504
         */
        wakeup();
    }

    /*
     * Process activity from one of the debugger sockets. This could be a new
     * connection or a data packet.
     */
    private void processDebuggerActivity(SelectionKey key) {
        Debugger dbg = (Debugger)key.attachment();

        try {
            if (key.isAcceptable()) {
                try {
                    acceptNewDebugger(dbg, null);
                } catch (IOException ioe) {
                    Log.w("ddms", "debugger accept() failed");
                    ioe.printStackTrace();
                }
            } else if (key.isReadable()) {
                processDebuggerData(key);
            } else {
                Log.d("ddm-debugger", "key in unknown state");
            }
        } catch (CancelledKeyException cke) {
            // key has been cancelled we can ignore that.
        }
    }

    /*
     * Accept a new connection from a debugger. If successful, register it with
     * the Selector.
     */
    private void acceptNewDebugger(Debugger dbg, ServerSocketChannel acceptChan)
            throws IOException {

        synchronized (mClientList) {
            SocketChannel chan;

            if (acceptChan == null)
                chan = dbg.accept();
            else
                chan = dbg.accept(acceptChan);

            if (chan != null) {
                chan.socket().setTcpNoDelay(true);

                wakeup();

                try {
                    chan.register(mSelector, SelectionKey.OP_READ, dbg);
                } catch (IOException ioe) {
                    // failed, drop the connection
                    dbg.closeData();
                    throw ioe;
                } catch (RuntimeException re) {
                    // failed, drop the connection
                    dbg.closeData();
                    throw re;
                }
            } else {
                Log.w("ddms", "ignoring duplicate debugger");
                // new connection already closed
            }
        }
    }

    /*
     * We have incoming data from the debugger. Forward it to the client.
     */
    private void processDebuggerData(SelectionKey key) {
        Debugger dbg = (Debugger)key.attachment();

        try {
            /*
             * Read pending data.
             */
            dbg.read();

            /*
             * See if we have a full packet in the buffer. It's possible we have
             * more than one packet, so we have to loop.
             */
            JdwpPacket packet = dbg.getJdwpPacket();
            while (packet != null) {
                Log.v("ddms", "Forwarding dbg req 0x"
                        + Integer.toHexString(packet.getId()) + " to "
                        + dbg.getClient());

                dbg.forwardPacketToClient(packet);

                packet = dbg.getJdwpPacket();
            }
        } catch (IOException ioe) {
            /*
             * Close data connection; automatically un-registers dbg from
             * selector. The failure could be caused by the debugger going away,
             * or by the client going away and failing to accept our data.
             * Either way, the debugger connection does not need to exist any
             * longer. We also need to recycle the connection to the client, so
             * that the VM sees the debugger disconnect. For a DDM-aware client
             * this won't be necessary, and we can just send a "debugger
             * disconnected" message.
             */
            Log.d("ddms", "Closing connection to debugger " + dbg);
            dbg.closeData();
            Client client = dbg.getClient();
            if (client.isDdmAware()) {
                // TODO: soft-disconnect DDM-aware clients
                Log.d("ddms", " (recycling client connection as well)");

                // we should drop the client, but also attempt to reopen it.
                // This is done by the DeviceMonitor.
                client.getDeviceImpl().getMonitor().addClientToDropAndReopen(client,
                        IDebugPortProvider.NO_STATIC_PORT);
            } else {
                Log.d("ddms", " (recycling client connection as well)");
                // we should drop the client, but also attempt to reopen it.
                // This is done by the DeviceMonitor.
                client.getDeviceImpl().getMonitor().addClientToDropAndReopen(client,
                        IDebugPortProvider.NO_STATIC_PORT);
            }
        }
    }

    /*
     * Tell the thread that something has changed.
     */
    private void wakeup() {
        mSelector.wakeup();
    }

    /**
     * Tell the thread to stop. Called from UI thread.
     */
    synchronized void quit() {
        mQuit = true;
        wakeup();
        Log.d("ddms", "Waiting for Monitor thread");
        try {
            this.join();
            // since we're quitting, lets drop all the client and disconnect
            // the DebugSelectedPort
            synchronized (mClientList) {
                for (Client c : mClientList) {
                    c.close(false /* notify */);
                    broadcast(CLIENT_DISCONNECTED, c);
                }
                mClientList.clear();
            }

            if (mDebugSelectedChan != null) {
                mDebugSelectedChan.close();
                mDebugSelectedChan.socket().close();
                mDebugSelectedChan = null;
            }
            mSelector.close();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        mInstance = null;
    }

    /**
     * Add a new Client to the list of things we monitor. Also adds the client's
     * channel and the client's debugger listener to the selection list. This
     * should only be called from one thread (the VMWatcherThread) to avoid a
     * race between "alreadyOpen" and Client creation.
     */
    synchronized void addClient(Client client) {
        if (mInstance == null) {
            return;
        }

        Log.d("ddms", "Adding new client " + client);

        synchronized (mClientList) {
            mClientList.add(client);

            /*
             * Register the Client's socket channel with the selector. We attach
             * the Client to the SelectionKey. If you try to register a new
             * channel with the Selector while it is waiting for I/O, you will
             * block. The solution is to call wakeup() and then hold a lock to
             * ensure that the registration happens before the Selector goes
             * back to sleep.
             */
            try {
                wakeup();

                client.register(mSelector);

                Debugger dbg = client.getDebugger();
                if (dbg != null) {
                    dbg.registerListener(mSelector);
                }
            } catch (IOException ioe) {
                // not really expecting this to happen
                ioe.printStackTrace();
            }
        }
    }

    /*
     * Broadcast an event to all message handlers.
     */
    private void broadcast(int event, Client client) {
        Log.d("ddms", "broadcast " + event + ": " + client);

        /*
         * The handler objects appear once in mHandlerMap for each message they
         * handle. We want to notify them once each, so we convert the HashMap
         * to a HashSet before we iterate.
         */
        HashSet<ChunkHandler> set;
        synchronized (mHandlerMap) {
            Collection<ChunkHandler> values = mHandlerMap.values();
            set = new HashSet<ChunkHandler>(values);
        }

        Iterator<ChunkHandler> iter = set.iterator();
        while (iter.hasNext()) {
            ChunkHandler handler = iter.next();
            switch (event) {
                case CLIENT_READY:
                    try {
                        handler.clientReady(client);
                    } catch (IOException ioe) {
                        // Something failed with the client. It should
                        // fall out of the list the next time we try to
                        // do something with it, so we discard the
                        // exception here and assume cleanup will happen
                        // later. May need to propagate farther. The
                        // trouble is that not all values for "event" may
                        // actually throw an exception.
                        Log.w("ddms",
                                "Got exception while broadcasting 'ready'");
                        return;
                    }
                    break;
                case CLIENT_DISCONNECTED:
                    handler.clientDisconnected(client);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

    }

    /**
     * Opens (or reopens) the "debug selected" port and listen for connections.
     * @return true if the port was opened successfully.
     * @throws IOException
     */
    private boolean reopenDebugSelectedPort() throws IOException {

        Log.d("ddms", "reopen debug-selected port: " + mNewDebugSelectedPort);
        if (mDebugSelectedChan != null) {
            mDebugSelectedChan.close();
        }

        mDebugSelectedChan = ServerSocketChannel.open();
        mDebugSelectedChan.configureBlocking(false); // required for Selector

        InetSocketAddress addr = new InetSocketAddress(
                InetAddress.getByName("localhost"), //$NON-NLS-1$
                mNewDebugSelectedPort);
        mDebugSelectedChan.socket().setReuseAddress(true); // enable SO_REUSEADDR

        try {
            mDebugSelectedChan.socket().bind(addr);
            if (mSelectedClient != null) {
                mSelectedClient.update(Client.CHANGE_PORT);
            }

            mDebugSelectedChan.register(mSelector, SelectionKey.OP_ACCEPT, this);

            return true;
        } catch (java.net.BindException e) {
            displayDebugSelectedBindError(mNewDebugSelectedPort);

            // do not attempt to reopen it.
            mDebugSelectedChan = null;
            mNewDebugSelectedPort = -1;

            return false;
        }
    }

    /*
     * We have some activity on the "debug selected" port. Handle it.
     */
    private void processDebugSelectedActivity(SelectionKey key) {
        assert key.isAcceptable();

        ServerSocketChannel acceptChan = (ServerSocketChannel)key.channel();

        /*
         * Find the debugger associated with the currently-selected client.
         */
        if (mSelectedClient != null) {
            Debugger dbg = mSelectedClient.getDebugger();

            if (dbg != null) {
                Log.d("ddms", "Accepting connection on 'debug selected' port");
                try {
                    acceptNewDebugger(dbg, acceptChan);
                } catch (IOException ioe) {
                    // client should be gone, keep going
                }

                return;
            }
        }

        Log.w("ddms",
                "Connection on 'debug selected' port, but none selected");
        try {
            SocketChannel chan = acceptChan.accept();
            chan.close();
        } catch (IOException ioe) {
            // not expected; client should be gone, keep going
        } catch (NotYetBoundException e) {
            displayDebugSelectedBindError(mDebugSelectedPort);
        }
    }

    private void displayDebugSelectedBindError(int port) {
        String message = String.format(
                "Could not open Selected VM debug port (%1$d). Make sure you do not have another instance of DDMS or of the eclipse plugin running. If it's being used by something else, choose a new port number in the preferences.",
                port);

        Log.logAndDisplay(LogLevel.ERROR, "ddms", message);
    }
}
