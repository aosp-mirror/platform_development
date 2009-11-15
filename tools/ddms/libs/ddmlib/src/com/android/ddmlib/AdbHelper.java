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
import com.android.ddmlib.log.LogReceiver;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;

/**
 * Helper class to handle requests and connections to adb.
 * <p/>{@link DebugBridgeServer} is the public API to connection to adb, while {@link AdbHelper}
 * does the low level stuff.
 * <p/>This currently uses spin-wait non-blocking I/O. A Selector would be more efficient,
 * but seems like overkill for what we're doing here.
 */
final class AdbHelper {

    // public static final long kOkay = 0x59414b4fL;
    // public static final long kFail = 0x4c494146L;

    static final int WAIT_TIME = 5; // spin-wait sleep, in ms

    static final String DEFAULT_ENCODING = "ISO-8859-1"; //$NON-NLS-1$

    /** do not instantiate */
    private AdbHelper() {
    }

    /**
     * Response from ADB.
     */
    static class AdbResponse {
        public AdbResponse() {
            // ioSuccess = okay = timeout = false;
            message = "";
        }

        public boolean ioSuccess; // read all expected data, no timeoutes

        public boolean okay; // first 4 bytes in response were "OKAY"?

        public boolean timeout; // TODO: implement

        public String message; // diagnostic string
    }

    /**
     * Create and connect a new pass-through socket, from the host to a port on
     * the device.
     *
     * @param adbSockAddr
     * @param device the device to connect to. Can be null in which case the connection will be
     * to the first available device.
     * @param devicePort the port we're opening
     */
    public static SocketChannel open(InetSocketAddress adbSockAddr,
            Device device, int devicePort) throws IOException {

        SocketChannel adbChan = SocketChannel.open(adbSockAddr);
        try {
            adbChan.socket().setTcpNoDelay(true);
            adbChan.configureBlocking(false);

            // if the device is not -1, then we first tell adb we're looking to
            // talk to a specific device
            setDevice(adbChan, device);

            byte[] req = createAdbForwardRequest(null, devicePort);
            // Log.hexDump(req);

            if (write(adbChan, req) == false)
                throw new IOException("failed submitting request to ADB"); //$NON-NLS-1$

            AdbResponse resp = readAdbResponse(adbChan, false);
            if (!resp.okay)
                throw new IOException("connection request rejected"); //$NON-NLS-1$

            adbChan.configureBlocking(true);
        } catch (IOException ioe) {
            adbChan.close();
            throw ioe;
        }

        return adbChan;
    }

    /**
     * Creates and connects a new pass-through socket, from the host to a port on
     * the device.
     *
     * @param adbSockAddr
     * @param device the device to connect to. Can be null in which case the connection will be
     * to the first available device.
     * @param pid the process pid to connect to.
     */
    public static SocketChannel createPassThroughConnection(InetSocketAddress adbSockAddr,
            Device device, int pid) throws IOException {

        SocketChannel adbChan = SocketChannel.open(adbSockAddr);
        try {
            adbChan.socket().setTcpNoDelay(true);
            adbChan.configureBlocking(false);

            // if the device is not -1, then we first tell adb we're looking to
            // talk to a specific device
            setDevice(adbChan, device);

            byte[] req = createJdwpForwardRequest(pid);
            // Log.hexDump(req);

            if (write(adbChan, req) == false)
                throw new IOException("failed submitting request to ADB"); //$NON-NLS-1$

            AdbResponse resp = readAdbResponse(adbChan, false /* readDiagString */);
            if (!resp.okay)
                throw new IOException("connection request rejected: " + resp.message); //$NON-NLS-1$

            adbChan.configureBlocking(true);
        } catch (IOException ioe) {
            adbChan.close();
            throw ioe;
        }

        return adbChan;
    }

    /**
     * Creates a port forwarding request for adb. This returns an array
     * containing "####tcp:{port}:{addStr}".
     * @param addrStr the host. Can be null.
     * @param port the port on the device. This does not need to be numeric.
     */
    private static byte[] createAdbForwardRequest(String addrStr, int port) {
        String reqStr;

        if (addrStr == null)
            reqStr = "tcp:" + port;
        else
            reqStr = "tcp:" + port + ":" + addrStr;
        return formAdbRequest(reqStr);
    }

    /**
     * Creates a port forwarding request to a jdwp process. This returns an array
     * containing "####jwdp:{pid}".
     * @param pid the jdwp process pid on the device.
     */
    private static byte[] createJdwpForwardRequest(int pid) {
        String reqStr = String.format("jdwp:%1$d", pid); //$NON-NLS-1$
        return formAdbRequest(reqStr);
    }

    /**
     * Create an ASCII string preceeded by four hex digits. The opening "####"
     * is the length of the rest of the string, encoded as ASCII hex (case
     * doesn't matter). "port" and "host" are what we want to forward to. If
     * we're on the host side connecting into the device, "addrStr" should be
     * null.
     */
    static byte[] formAdbRequest(String req) {
        String resultStr = String.format("%04X%s", req.length(), req); //$NON-NLS-1$
        byte[] result;
        try {
            result = resultStr.getBytes(DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace(); // not expected
            return null;
        }
        assert result.length == req.length() + 4;
        return result;
    }

    /**
     * Reads the response from ADB after a command.
     * @param chan The socket channel that is connected to adb.
     * @param readDiagString If true, we're expecting an OKAY response to be
     *      followed by a diagnostic string. Otherwise, we only expect the
     *      diagnostic string to follow a FAIL.
     */
    static AdbResponse readAdbResponse(SocketChannel chan, boolean readDiagString)
            throws IOException {

        AdbResponse resp = new AdbResponse();

        byte[] reply = new byte[4];
        if (read(chan, reply) == false) {
            return resp;
        }
        resp.ioSuccess = true;

        if (isOkay(reply)) {
            resp.okay = true;
        } else {
            readDiagString = true; // look for a reason after the FAIL
            resp.okay = false;
        }

        // not a loop -- use "while" so we can use "break"
        while (readDiagString) {
            // length string is in next 4 bytes
            byte[] lenBuf = new byte[4];
            if (read(chan, lenBuf) == false) {
                Log.w("ddms", "Expected diagnostic string not found");
                break;
            }

            String lenStr = replyToString(lenBuf);

            int len;
            try {
                len = Integer.parseInt(lenStr, 16);
            } catch (NumberFormatException nfe) {
                Log.w("ddms", "Expected digits, got '" + lenStr + "': "
                        + lenBuf[0] + " " + lenBuf[1] + " " + lenBuf[2] + " "
                        + lenBuf[3]);
                Log.w("ddms", "reply was " + replyToString(reply));
                break;
            }

            byte[] msg = new byte[len];
            if (read(chan, msg) == false) {
                Log.w("ddms", "Failed reading diagnostic string, len=" + len);
                break;
            }

            resp.message = replyToString(msg);
            Log.v("ddms", "Got reply '" + replyToString(reply) + "', diag='"
                    + resp.message + "'");

            break;
        }

        return resp;
    }

    /**
     * Retrieve the frame buffer from the device.
     */
    public static RawImage getFrameBuffer(InetSocketAddress adbSockAddr, Device device)
            throws IOException {

        RawImage imageParams = new RawImage();
        byte[] request = formAdbRequest("framebuffer:"); //$NON-NLS-1$
        byte[] nudge = {
            0
        };
        byte[] reply;

        SocketChannel adbChan = null;
        try {
            adbChan = SocketChannel.open(adbSockAddr);
            adbChan.configureBlocking(false);

            // if the device is not -1, then we first tell adb we're looking to talk
            // to a specific device
            setDevice(adbChan, device);

            if (write(adbChan, request) == false)
                throw new IOException("failed asking for frame buffer");

            AdbResponse resp = readAdbResponse(adbChan, false /* readDiagString */);
            if (!resp.ioSuccess || !resp.okay) {
                Log.w("ddms", "Got timeout or unhappy response from ADB fb req: "
                        + resp.message);
                adbChan.close();
                return null;
            }

            // first the protocol version.
            reply = new byte[4];
            if (read(adbChan, reply) == false) {
                Log.w("ddms", "got partial reply from ADB fb:");
                Log.hexDump("ddms", LogLevel.WARN, reply, 0, reply.length);
                adbChan.close();
                return null;
            }
            ByteBuffer buf = ByteBuffer.wrap(reply);
            buf.order(ByteOrder.LITTLE_ENDIAN);

            int version = buf.getInt();

            // get the header size (this is a count of int)
            int headerSize = RawImage.getHeaderSize(version);

            // read the header
            reply = new byte[headerSize * 4];
            if (read(adbChan, reply) == false) {
                Log.w("ddms", "got partial reply from ADB fb:");
                Log.hexDump("ddms", LogLevel.WARN, reply, 0, reply.length);
                adbChan.close();
                return null;
            }
            buf = ByteBuffer.wrap(reply);
            buf.order(ByteOrder.LITTLE_ENDIAN);

            // fill the RawImage with the header
            if (imageParams.readHeader(version, buf) == false) {
                Log.e("Screenshot", "Unsupported protocol: " + version);
                return null;
            }

            Log.d("ddms", "image params: bpp=" + imageParams.bpp + ", size="
                    + imageParams.size + ", width=" + imageParams.width
                    + ", height=" + imageParams.height);

            if (write(adbChan, nudge) == false)
                throw new IOException("failed nudging");

            reply = new byte[imageParams.size];
            if (read(adbChan, reply) == false) {
                Log.w("ddms", "got truncated reply from ADB fb data");
                adbChan.close();
                return null;
            }

            imageParams.data = reply;
        } finally {
            if (adbChan != null) {
                adbChan.close();
            }
        }

        return imageParams;
    }

    /**
     * Execute a command on the device and retrieve the output. The output is
     * handed to "rcvr" as it arrives.
     */
    public static void executeRemoteCommand(InetSocketAddress adbSockAddr,
            String command, Device device, IShellOutputReceiver rcvr)
            throws IOException {
        Log.v("ddms", "execute: running " + command);

        SocketChannel adbChan = null;
        try {
            adbChan = SocketChannel.open(adbSockAddr);
            adbChan.configureBlocking(false);

            // if the device is not -1, then we first tell adb we're looking to
            // talk
            // to a specific device
            setDevice(adbChan, device);

            byte[] request = formAdbRequest("shell:" + command); //$NON-NLS-1$
            if (write(adbChan, request) == false)
                throw new IOException("failed submitting shell command");

            AdbResponse resp = readAdbResponse(adbChan, false /* readDiagString */);
            if (!resp.ioSuccess || !resp.okay) {
                Log.e("ddms", "ADB rejected shell command (" + command + "): " + resp.message);
                throw new IOException("sad result from adb: " + resp.message);
            }

            byte[] data = new byte[16384];
            ByteBuffer buf = ByteBuffer.wrap(data);
            while (true) {
                int count;

                if (rcvr != null && rcvr.isCancelled()) {
                    Log.v("ddms", "execute: cancelled");
                    break;
                }

                count = adbChan.read(buf);
                if (count < 0) {
                    // we're at the end, we flush the output
                    rcvr.flush();
                    Log.v("ddms", "execute '" + command + "' on '" + device + "' : EOF hit. Read: "
                            + count);
                    break;
                } else if (count == 0) {
                    try {
                        Thread.sleep(WAIT_TIME * 5);
                    } catch (InterruptedException ie) {
                    }
                } else {
                    if (rcvr != null) {
                        rcvr.addOutput(buf.array(), buf.arrayOffset(), buf.position());
                    }
                    buf.rewind();
                }
            }
        } finally {
            if (adbChan != null) {
                adbChan.close();
            }
            Log.v("ddms", "execute: returning");
        }
    }

    /**
     * Runs the Event log service on the {@link Device}, and provides its output to the
     * {@link LogReceiver}.
     * @param adbSockAddr the socket address to connect to adb
     * @param device the Device on which to run the service
     * @param rcvr the {@link LogReceiver} to receive the log output
     * @throws IOException
     */
    public static void runEventLogService(InetSocketAddress adbSockAddr, Device device,
            LogReceiver rcvr) throws IOException {
        runLogService(adbSockAddr, device, "events", rcvr); //$NON-NLS-1$
    }

    /**
     * Runs a log service on the {@link Device}, and provides its output to the {@link LogReceiver}.
     * @param adbSockAddr the socket address to connect to adb
     * @param device the Device on which to run the service
     * @param logName the name of the log file to output
     * @param rcvr the {@link LogReceiver} to receive the log output
     * @throws IOException
     */
    public static void runLogService(InetSocketAddress adbSockAddr, Device device, String logName,
            LogReceiver rcvr) throws IOException {
        SocketChannel adbChan = null;

        try {
            adbChan = SocketChannel.open(adbSockAddr);
            adbChan.configureBlocking(false);

            // if the device is not -1, then we first tell adb we're looking to talk
            // to a specific device
            setDevice(adbChan, device);

            byte[] request = formAdbRequest("log:" + logName);
            if (write(adbChan, request) == false) {
                throw new IOException("failed to submit the log command");
            }

            AdbResponse resp = readAdbResponse(adbChan, false /* readDiagString */);
            if (!resp.ioSuccess || !resp.okay) {
                throw new IOException("Device rejected log command: " + resp.message);
            }

            byte[] data = new byte[16384];
            ByteBuffer buf = ByteBuffer.wrap(data);
            while (true) {
                int count;

                if (rcvr != null && rcvr.isCancelled()) {
                    break;
                }

                count = adbChan.read(buf);
                if (count < 0) {
                    break;
                } else if (count == 0) {
                    try {
                        Thread.sleep(WAIT_TIME * 5);
                    } catch (InterruptedException ie) {
                    }
                } else {
                    if (rcvr != null) {
                        rcvr.parseNewData(buf.array(), buf.arrayOffset(), buf.position());
                    }
                    buf.rewind();
                }
            }
        } finally {
            if (adbChan != null) {
                adbChan.close();
            }
        }
    }

    /**
     * Creates a port forwarding between a local and a remote port.
     * @param adbSockAddr the socket address to connect to adb
     * @param device the device on which to do the port fowarding
     * @param localPort the local port to forward
     * @param remotePort the remote port.
     * @return <code>true</code> if success.
     * @throws IOException
     */
    public static boolean createForward(InetSocketAddress adbSockAddr, Device device, int localPort,
            int remotePort) throws IOException {

        SocketChannel adbChan = null;
        try {
            adbChan = SocketChannel.open(adbSockAddr);
            adbChan.configureBlocking(false);

            byte[] request = formAdbRequest(String.format(
                    "host-serial:%1$s:forward:tcp:%2$d;tcp:%3$d", //$NON-NLS-1$
                    device.getSerialNumber(), localPort, remotePort));

            if (write(adbChan, request) == false) {
                throw new IOException("failed to submit the forward command.");
            }

            AdbResponse resp = readAdbResponse(adbChan, false /* readDiagString */);
            if (!resp.ioSuccess || !resp.okay) {
                throw new IOException("Device rejected command: " + resp.message);
            }
        } finally {
            if (adbChan != null) {
                adbChan.close();
            }
        }

        return true;
    }

    /**
     * Remove a port forwarding between a local and a remote port.
     * @param adbSockAddr the socket address to connect to adb
     * @param device the device on which to remove the port fowarding
     * @param localPort the local port of the forward
     * @param remotePort the remote port.
     * @return <code>true</code> if success.
     * @throws IOException
     */
    public static boolean removeForward(InetSocketAddress adbSockAddr, Device device, int localPort,
            int remotePort) throws IOException {

        SocketChannel adbChan = null;
        try {
            adbChan = SocketChannel.open(adbSockAddr);
            adbChan.configureBlocking(false);

            byte[] request = formAdbRequest(String.format(
                    "host-serial:%1$s:killforward:tcp:%2$d;tcp:%3$d", //$NON-NLS-1$
                    device.getSerialNumber(), localPort, remotePort));

            if (!write(adbChan, request)) {
                throw new IOException("failed to submit the remove forward command.");
            }

            AdbResponse resp = readAdbResponse(adbChan, false /* readDiagString */);
            if (!resp.ioSuccess || !resp.okay) {
                throw new IOException("Device rejected command: " + resp.message);
            }
        } finally {
            if (adbChan != null) {
                adbChan.close();
            }
        }

        return true;
    }

    /**
     * Checks to see if the first four bytes in "reply" are OKAY.
     */
    static boolean isOkay(byte[] reply) {
        return reply[0] == (byte)'O' && reply[1] == (byte)'K'
                && reply[2] == (byte)'A' && reply[3] == (byte)'Y';
    }

    /**
     * Converts an ADB reply to a string.
     */
    static String replyToString(byte[] reply) {
        String result;
        try {
            result = new String(reply, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace(); // not expected
            result = "";
        }
        return result;
    }

    /**
     * Reads from the socket until the array is filled, or no more data is coming (because
     * the socket closed or the timeout expired).
     *
     * @param chan the opened socket to read from. It must be in non-blocking
     *      mode for timeouts to work
     * @param data the buffer to store the read data into.
     * @return "true" if all data was read.
     * @throws IOException
     */
    static boolean read(SocketChannel chan, byte[] data) {
       try {
           read(chan, data, -1, DdmPreferences.getTimeOut());
       } catch (IOException e) {
           Log.d("ddms", "readAll: IOException: " + e.getMessage());
           return false;
       }

       return true;
    }

    /**
     * Reads from the socket until the array is filled, the optional length
     * is reached, or no more data is coming (because the socket closed or the
     * timeout expired). After "timeout" milliseconds since the
     * previous successful read, this will return whether or not new data has
     * been found.
     *
     * @param chan the opened socket to read from. It must be in non-blocking
     *      mode for timeouts to work
     * @param data the buffer to store the read data into.
     * @param length the length to read or -1 to fill the data buffer completely
     * @param timeout The timeout value. A timeout of zero means "wait forever".
     * @throws IOException
     */
    static void read(SocketChannel chan, byte[] data, int length, int timeout) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(data, 0, length != -1 ? length : data.length);
        int numWaits = 0;

        while (buf.position() != buf.limit()) {
            int count;

            count = chan.read(buf);
            if (count < 0) {
                Log.d("ddms", "read: channel EOF");
                throw new IOException("EOF");
            } else if (count == 0) {
                // TODO: need more accurate timeout?
                if (timeout != 0 && numWaits * WAIT_TIME > timeout) {
                    Log.d("ddms", "read: timeout");
                    throw new IOException("timeout");
                }
                // non-blocking spin
                try {
                    Thread.sleep(WAIT_TIME);
                } catch (InterruptedException ie) {
                }
                numWaits++;
            } else {
                numWaits = 0;
            }
        }
    }

    /**
     * Write until all data in "data" is written or the connection fails.
     * @param chan the opened socket to write to.
     * @param data the buffer to send.
     * @return "true" if all data was written.
     */
    static boolean write(SocketChannel chan, byte[] data) {
        try {
            write(chan, data, -1, DdmPreferences.getTimeOut());
        } catch (IOException e) {
            Log.e("ddms", e);
            return false;
        }

        return true;
    }

    /**
     * Write until all data in "data" is written, the optional length is reached,
     * the timeout expires, or the connection fails. Returns "true" if all
     * data was written.
     * @param chan the opened socket to write to.
     * @param data the buffer to send.
     * @param length the length to write or -1 to send the whole buffer.
     * @param timeout The timeout value. A timeout of zero means "wait forever".
     * @throws IOException
     */
    static void write(SocketChannel chan, byte[] data, int length, int timeout)
            throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(data, 0, length != -1 ? length : data.length);
        int numWaits = 0;

        while (buf.position() != buf.limit()) {
            int count;

            count = chan.write(buf);
            if (count < 0) {
                Log.d("ddms", "write: channel EOF");
                throw new IOException("channel EOF");
            } else if (count == 0) {
                // TODO: need more accurate timeout?
                if (timeout != 0 && numWaits * WAIT_TIME > timeout) {
                    Log.d("ddms", "write: timeout");
                    throw new IOException("timeout");
                }
                // non-blocking spin
                try {
                    Thread.sleep(WAIT_TIME);
                } catch (InterruptedException ie) {
                }
                numWaits++;
            } else {
                numWaits = 0;
            }
        }
    }

    /**
     * tells adb to talk to a specific device
     *
     * @param adbChan the socket connection to adb
     * @param device The device to talk to.
     * @throws IOException
     */
    static void setDevice(SocketChannel adbChan, Device device)
            throws IOException {
        // if the device is not -1, then we first tell adb we're looking to talk
        // to a specific device
        if (device != null) {
            String msg = "host:transport:" + device.getSerialNumber(); //$NON-NLS-1$
            byte[] device_query = formAdbRequest(msg);

            if (write(adbChan, device_query) == false)
                throw new IOException("failed submitting device (" + device +
                        ") request to ADB");

            AdbResponse resp = readAdbResponse(adbChan, false /* readDiagString */);
            if (!resp.okay)
                throw new IOException("device (" + device +
                        ") request rejected: " + resp.message);
        }

    }
}
