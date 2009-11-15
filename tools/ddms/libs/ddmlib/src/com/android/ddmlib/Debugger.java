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

import com.android.ddmlib.ClientData.DebuggerStatus;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * This represents a pending or established connection with a JDWP debugger.
 */
class Debugger {

    /*
     * Messages from the debugger should be pretty small; may not even
     * need an expanding-buffer implementation for this.
     */
    private static final int INITIAL_BUF_SIZE = 1 * 1024;
    private static final int MAX_BUF_SIZE = 32 * 1024;
    private ByteBuffer mReadBuffer;

    private static final int PRE_DATA_BUF_SIZE = 256;
    private ByteBuffer mPreDataBuffer;

    /* connection state */
    private int mConnState;
    private static final int ST_NOT_CONNECTED = 1;
    private static final int ST_AWAIT_SHAKE   = 2;
    private static final int ST_READY         = 3;

    /* peer */
    private Client mClient;         // client we're forwarding to/from
    private int mListenPort;        // listen to me
    private ServerSocketChannel mListenChannel;

    /* this goes up and down; synchronize methods that access the field */
    private SocketChannel mChannel;

    /**
     * Create a new Debugger object, configured to listen for connections
     * on a specific port.
     */
    Debugger(Client client, int listenPort) throws IOException {

        mClient = client;
        mListenPort = listenPort;

        mListenChannel = ServerSocketChannel.open();
        mListenChannel.configureBlocking(false);        // required for Selector

        InetSocketAddress addr = new InetSocketAddress(
                InetAddress.getByName("localhost"), // $NON-NLS-1$
                listenPort);
        mListenChannel.socket().setReuseAddress(true);  // enable SO_REUSEADDR
        mListenChannel.socket().bind(addr);

        mReadBuffer = ByteBuffer.allocate(INITIAL_BUF_SIZE);
        mPreDataBuffer = ByteBuffer.allocate(PRE_DATA_BUF_SIZE);
        mConnState = ST_NOT_CONNECTED;

        Log.d("ddms", "Created: " + this.toString());
    }

    /**
     * Returns "true" if a debugger is currently attached to us.
     */
    boolean isDebuggerAttached() {
        return mChannel != null;
    }

    /**
     * Represent the Debugger as a string.
     */
    @Override
    public String toString() {
        // mChannel != null means we have connection, ST_READY means it's going
        return "[Debugger " + mListenPort + "-->" + mClient.getClientData().getPid()
                + ((mConnState != ST_READY) ? " inactive]" : " active]");
    }

    /**
     * Register the debugger's listen socket with the Selector.
     */
    void registerListener(Selector sel) throws IOException {
        mListenChannel.register(sel, SelectionKey.OP_ACCEPT, this);
    }

    /**
     * Return the Client being debugged.
     */
    Client getClient() {
        return mClient;
    }

    /**
     * Accept a new connection, but only if we don't already have one.
     *
     * Must be synchronized with other uses of mChannel and mPreBuffer.
     *
     * Returns "null" if we're already talking to somebody.
     */
    synchronized SocketChannel accept() throws IOException {
        return accept(mListenChannel);
    }

    /**
     * Accept a new connection from the specified listen channel.  This
     * is so we can listen on a dedicated port for the "current" client,
     * where "current" is constantly in flux.
     *
     * Must be synchronized with other uses of mChannel and mPreBuffer.
     *
     * Returns "null" if we're already talking to somebody.
     */
    synchronized SocketChannel accept(ServerSocketChannel listenChan)
        throws IOException {

        if (listenChan != null) {
            SocketChannel newChan;

            newChan = listenChan.accept();
            if (mChannel != null) {
                Log.w("ddms", "debugger already talking to " + mClient
                    + " on " + mListenPort);
                newChan.close();
                return null;
            }
            mChannel = newChan;
            mChannel.configureBlocking(false);         // required for Selector
            mConnState = ST_AWAIT_SHAKE;
            return mChannel;
        }

        return null;
    }

    /**
     * Close the data connection only.
     */
    synchronized void closeData() {
        try {
            if (mChannel != null) {
                mChannel.close();
                mChannel = null;
                mConnState = ST_NOT_CONNECTED;

                ClientData cd = mClient.getClientData();
                cd.setDebuggerConnectionStatus(DebuggerStatus.DEFAULT);
                mClient.update(Client.CHANGE_DEBUGGER_STATUS);
            }
        } catch (IOException ioe) {
            Log.w("ddms", "Failed to close data " + this);
        }
    }

    /**
     * Close the socket that's listening for new connections and (if
     * we're connected) the debugger data socket.
     */
    synchronized void close() {
        try {
            if (mListenChannel != null) {
                mListenChannel.close();
            }
            mListenChannel = null;
            closeData();
        } catch (IOException ioe) {
            Log.w("ddms", "Failed to close listener " + this);
        }
    }

    // TODO: ?? add a finalizer that verifies the channel was closed

    /**
     * Read data from our channel.
     *
     * This is called when data is known to be available, and we don't yet
     * have a full packet in the buffer.  If the buffer is at capacity,
     * expand it.
     */
    void read() throws IOException {
        int count;

        if (mReadBuffer.position() == mReadBuffer.capacity()) {
            if (mReadBuffer.capacity() * 2 > MAX_BUF_SIZE) {
                throw new BufferOverflowException();
            }
            Log.d("ddms", "Expanding read buffer to "
                + mReadBuffer.capacity() * 2);

            ByteBuffer newBuffer =
                    ByteBuffer.allocate(mReadBuffer.capacity() * 2);
            mReadBuffer.position(0);
            newBuffer.put(mReadBuffer);     // leaves "position" at end

            mReadBuffer = newBuffer;
        }

        count = mChannel.read(mReadBuffer);
        Log.v("ddms", "Read " + count + " bytes from " + this);
        if (count < 0) throw new IOException("read failed");
    }

    /**
     * Return information for the first full JDWP packet in the buffer.
     *
     * If we don't yet have a full packet, return null.
     *
     * If we haven't yet received the JDWP handshake, we watch for it here
     * and consume it without admitting to have done so.  We also send
     * the handshake response to the debugger, along with any pending
     * pre-connection data, which is why this can throw an IOException.
     */
    JdwpPacket getJdwpPacket() throws IOException {
        /*
         * On entry, the data starts at offset 0 and ends at "position".
         * "limit" is set to the buffer capacity.
         */
        if (mConnState == ST_AWAIT_SHAKE) {
            int result;

            result = JdwpPacket.findHandshake(mReadBuffer);
            //Log.v("ddms", "findHand: " + result);
            switch (result) {
                case JdwpPacket.HANDSHAKE_GOOD:
                    Log.d("ddms", "Good handshake from debugger");
                    JdwpPacket.consumeHandshake(mReadBuffer);
                    sendHandshake();
                    mConnState = ST_READY;

                    ClientData cd = mClient.getClientData();
                    cd.setDebuggerConnectionStatus(DebuggerStatus.ATTACHED);
                    mClient.update(Client.CHANGE_DEBUGGER_STATUS);

                    // see if we have another packet in the buffer
                    return getJdwpPacket();
                case JdwpPacket.HANDSHAKE_BAD:
                    // not a debugger, throw an exception so we drop the line
                    Log.d("ddms", "Bad handshake from debugger");
                    throw new IOException("bad handshake");
                case JdwpPacket.HANDSHAKE_NOTYET:
                    break;
                default:
                    Log.e("ddms", "Unknown packet while waiting for client handshake");
            }
            return null;
        } else if (mConnState == ST_READY) {
            if (mReadBuffer.position() != 0) {
                Log.v("ddms", "Checking " + mReadBuffer.position() + " bytes");
            }
            return JdwpPacket.findPacket(mReadBuffer);
        } else {
            Log.e("ddms", "Receiving data in state = " + mConnState);
        }

        return null;
    }

    /**
     * Forward a packet to the client.
     *
     * "mClient" will never be null, though it's possible that the channel
     * in the client has closed and our send attempt will fail.
     *
     * Consumes the packet.
     */
    void forwardPacketToClient(JdwpPacket packet) throws IOException {
        mClient.sendAndConsume(packet);
    }

    /**
     * Send the handshake to the debugger.  We also send along any packets
     * we already received from the client (usually just a VM_START event,
     * if anything at all).
     */
    private synchronized void sendHandshake() throws IOException {
        ByteBuffer tempBuffer = ByteBuffer.allocate(JdwpPacket.HANDSHAKE_LEN);
        JdwpPacket.putHandshake(tempBuffer);
        int expectedLength = tempBuffer.position();
        tempBuffer.flip();
        if (mChannel.write(tempBuffer) != expectedLength) {
            throw new IOException("partial handshake write");
        }

        expectedLength = mPreDataBuffer.position();
        if (expectedLength > 0) {
            Log.d("ddms", "Sending " + mPreDataBuffer.position()
                    + " bytes of saved data");
            mPreDataBuffer.flip();
            if (mChannel.write(mPreDataBuffer) != expectedLength) {
                throw new IOException("partial pre-data write");
            }
            mPreDataBuffer.clear();
        }
    }

    /**
     * Send a packet to the debugger.
     *
     * Ideally, we can do this with a single channel write.  If that doesn't
     * happen, we have to prevent anybody else from writing to the channel
     * until this packet completes, so we synchronize on the channel.
     *
     * Another goal is to avoid unnecessary buffer copies, so we write
     * directly out of the JdwpPacket's ByteBuffer.
     *
     * We must synchronize on "mChannel" before writing to it.  We want to
     * coordinate the buffered data with mChannel creation, so this whole
     * method is synchronized.
     */
    synchronized void sendAndConsume(JdwpPacket packet)
        throws IOException {

        if (mChannel == null) {
            /*
             * Buffer this up so we can send it to the debugger when it
             * finally does connect.  This is essential because the VM_START
             * message might be telling the debugger that the VM is
             * suspended.  The alternative approach would be for us to
             * capture and interpret VM_START and send it later if we
             * didn't choose to un-suspend the VM for our own purposes.
             */
            Log.d("ddms", "Saving packet 0x"
                    + Integer.toHexString(packet.getId()));
            packet.movePacket(mPreDataBuffer);
        } else {
            packet.writeAndConsume(mChannel);
        }
    }
}

