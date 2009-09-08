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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.InvalidParameterException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides control over emulated hardware of the Android emulator.
 * <p/>This is basically a wrapper around the command line console normally used with telnet.
 *<p/>
 * Regarding line termination handling:<br>
 * One of the issues is that the telnet protocol <b>requires</b> usage of <code>\r\n</code>. Most
 * implementations don't enforce it (the dos one does). In this particular case, this is mostly
 * irrelevant since we don't use telnet in Java, but that means we want to make
 * sure we use the same line termination than what the console expects. The console
 * code removes <code>\r</code> and waits for <code>\n</code>.
 * <p/>However this means you <i>may</i> receive <code>\r\n</code> when reading from the console.
 * <p/>
 * <b>This API will change in the near future.</b>
 */
public final class EmulatorConsole {

    private final static String DEFAULT_ENCODING = "ISO-8859-1"; //$NON-NLS-1$

    private final static int WAIT_TIME = 5; // spin-wait sleep, in ms

    private final static int STD_TIMEOUT = 5000; // standard delay, in ms

    private final static String HOST = "127.0.0.1";  //$NON-NLS-1$

    private final static String COMMAND_PING = "help\r\n"; //$NON-NLS-1$
    private final static String COMMAND_AVD_NAME = "avd name\r\n"; //$NON-NLS-1$
    private final static String COMMAND_KILL = "kill\r\n"; //$NON-NLS-1$
    private final static String COMMAND_GSM_STATUS = "gsm status\r\n"; //$NON-NLS-1$
    private final static String COMMAND_GSM_CALL = "gsm call %1$s\r\n"; //$NON-NLS-1$
    private final static String COMMAND_GSM_CANCEL_CALL = "gsm cancel %1$s\r\n"; //$NON-NLS-1$
    private final static String COMMAND_GSM_DATA = "gsm data %1$s\r\n"; //$NON-NLS-1$
    private final static String COMMAND_GSM_VOICE = "gsm voice %1$s\r\n"; //$NON-NLS-1$
    private final static String COMMAND_SMS_SEND = "sms send %1$s %2$s\r\n"; //$NON-NLS-1$
    private final static String COMMAND_NETWORK_STATUS = "network status\r\n"; //$NON-NLS-1$
    private final static String COMMAND_NETWORK_SPEED = "network speed %1$s\r\n"; //$NON-NLS-1$
    private final static String COMMAND_NETWORK_LATENCY = "network delay %1$s\r\n"; //$NON-NLS-1$
    private final static String COMMAND_GPS =
        "geo nmea $GPGGA,%1$02d%2$02d%3$02d.%4$03d," + //$NON-NLS-1$
        "%5$03d%6$09.6f,%7$c,%8$03d%9$09.6f,%10$c," + //$NON-NLS-1$
        "1,10,0.0,0.0,0,0.0,0,0.0,0000\r\n"; //$NON-NLS-1$

    private final static Pattern RE_KO = Pattern.compile("KO:\\s+(.*)"); //$NON-NLS-1$

    /**
     * Array of delay values: no delay, gprs, edge/egprs, umts/3d
     */
    public final static int[] MIN_LATENCIES = new int[] {
        0,      // No delay
        150,    // gprs
        80,     // edge/egprs
        35      // umts/3g
    };

    /**
     * Array of download speeds: full speed, gsm, hscsd, gprs, edge/egprs, umts/3g, hsdpa.
     */
    public final int[] DOWNLOAD_SPEEDS = new int[] {
        0,          // full speed
        14400,      // gsm
        43200,      // hscsd
        80000,      // gprs
        236800,     // edge/egprs
        1920000,    // umts/3g
        14400000    // hsdpa
    };

    /** Arrays of valid network speeds */
    public final static String[] NETWORK_SPEEDS = new String[] {
        "full", //$NON-NLS-1$
        "gsm", //$NON-NLS-1$
        "hscsd", //$NON-NLS-1$
        "gprs", //$NON-NLS-1$
        "edge", //$NON-NLS-1$
        "umts", //$NON-NLS-1$
        "hsdpa", //$NON-NLS-1$
    };

    /** Arrays of valid network latencies */
    public final static String[] NETWORK_LATENCIES = new String[] {
        "none", //$NON-NLS-1$
        "gprs", //$NON-NLS-1$
        "edge", //$NON-NLS-1$
        "umts", //$NON-NLS-1$
    };

    /** Gsm Mode enum. */
    public static enum GsmMode {
        UNKNOWN((String)null),
        UNREGISTERED(new String[] { "unregistered", "off" }),
        HOME(new String[] { "home", "on" }),
        ROAMING("roaming"),
        SEARCHING("searching"),
        DENIED("denied");

        private final String[] tags;

        GsmMode(String tag) {
            if (tag != null) {
                this.tags = new String[] { tag };
            } else {
                this.tags = new String[0];
            }
        }

        GsmMode(String[] tags) {
            this.tags = tags;
        }

        public static GsmMode getEnum(String tag) {
            for (GsmMode mode : values()) {
                for (String t : mode.tags) {
                    if (t.equals(tag)) {
                        return mode;
                    }
                }
            }
            return UNKNOWN;
        }

        /**
         * Returns the first tag of the enum.
         */
        public String getTag() {
            if (tags.length > 0) {
                return tags[0];
            }
            return null;
        }
    }

    public final static String RESULT_OK = null;

    private final static Pattern sEmulatorRegexp = Pattern.compile(Device.RE_EMULATOR_SN);
    private final static Pattern sVoiceStatusRegexp = Pattern.compile(
            "gsm\\s+voice\\s+state:\\s*([a-z]+)", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
    private final static Pattern sDataStatusRegexp = Pattern.compile(
            "gsm\\s+data\\s+state:\\s*([a-z]+)", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
    private final static Pattern sDownloadSpeedRegexp = Pattern.compile(
            "\\s+download\\s+speed:\\s+(\\d+)\\s+bits.*", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
    private final static Pattern sMinLatencyRegexp = Pattern.compile(
            "\\s+minimum\\s+latency:\\s+(\\d+)\\s+ms", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

    private final static HashMap<Integer, EmulatorConsole> sEmulators =
        new HashMap<Integer, EmulatorConsole>();

    /** Gsm Status class */
    public static class GsmStatus {
        /** Voice status. */
        public GsmMode voice = GsmMode.UNKNOWN;
        /** Data status. */
        public GsmMode data = GsmMode.UNKNOWN;
    }

    /** Network Status class */
    public static class NetworkStatus {
        /** network speed status. This is an index in the {@link #DOWNLOAD_SPEEDS} array. */
        public int speed = -1;
        /** network latency status.  This is an index in the {@link #MIN_LATENCIES} array. */
        public int latency = -1;
    }

    private int mPort;

    private SocketChannel mSocketChannel;

    private byte[] mBuffer = new byte[1024];

    /**
     * Returns an {@link EmulatorConsole} object for the given {@link Device}. This can
     * be an already existing console, or a new one if it hadn't been created yet.
     * @param d The device that the console links to.
     * @return an <code>EmulatorConsole</code> object or <code>null</code> if the connection failed.
     */
    public static synchronized EmulatorConsole getConsole(IDevice d) {
        // we need to make sure that the device is an emulator
        Matcher m = sEmulatorRegexp.matcher(d.getSerialNumber());
        if (m.matches()) {
            // get the port number. This is the console port.
            int port;
            try {
                port = Integer.parseInt(m.group(1));
                if (port <= 0) {
                    return null;
                }
            } catch (NumberFormatException e) {
                // looks like we failed to get the port number. This is a bit strange since
                // it's coming from a regexp that only accept digit, but we handle the case
                // and return null.
                return null;
            }

            EmulatorConsole console = sEmulators.get(port);

            if (console != null) {
                // if the console exist, we ping the emulator to check the connection.
                if (console.ping() == false) {
                    RemoveConsole(console.mPort);
                    console = null;
                }
            }

            if (console == null) {
                // no console object exists for this port so we create one, and start
                // the connection.
                console = new EmulatorConsole(port);
                if (console.start()) {
                    sEmulators.put(port, console);
                } else {
                    console = null;
                }
            }

            return console;
        }

        return null;
    }

    /**
     * Removes the console object associated with a port from the map.
     * @param port The port of the console to remove.
     */
    private static synchronized void RemoveConsole(int port) {
        sEmulators.remove(port);
    }

    private EmulatorConsole(int port) {
        super();
        mPort = port;
    }

    /**
     * Starts the connection of the console.
     * @return true if success.
     */
    private boolean start() {

        InetSocketAddress socketAddr;
        try {
            InetAddress hostAddr = InetAddress.getByName(HOST);
            socketAddr = new InetSocketAddress(hostAddr, mPort);
        } catch (UnknownHostException e) {
            return false;
        }

        try {
            mSocketChannel = SocketChannel.open(socketAddr);
        } catch (IOException e1) {
            return false;
        }

        // read some stuff from it
        readLines();

        return true;
    }

    /**
     * Ping the emulator to check if the connection is still alive.
     * @return true if the connection is alive.
     */
    private synchronized boolean ping() {
        // it looks like we can send stuff, even when the emulator quit, but we can't read
        // from the socket. So we check the return of readLines()
        if (sendCommand(COMMAND_PING)) {
            return readLines() != null;
        }

        return false;
    }

    /**
     * Sends a KILL command to the emulator.
     */
    public synchronized void kill() {
        if (sendCommand(COMMAND_KILL)) {
            RemoveConsole(mPort);
        }
    }

    public synchronized String getAvdName() {
        if (sendCommand(COMMAND_AVD_NAME)) {
            String[] result = readLines();
            if (result != null && result.length == 2) { // this should be the name on first line,
                                                        // and ok on 2nd line
                return result[0];
            } else {
                // try to see if there's a message after KO
                Matcher m = RE_KO.matcher(result[result.length-1]);
                if (m.matches()) {
                    return m.group(1);
                }
            }
        }

        return null;
    }

    /**
     * Get the network status of the emulator.
     * @return a {@link NetworkStatus} object containing the {@link GsmStatus}, or
     * <code>null</code> if the query failed.
     */
    public synchronized NetworkStatus getNetworkStatus() {
        if (sendCommand(COMMAND_NETWORK_STATUS)) {
            /* Result is in the format
                Current network status:
                download speed:      14400 bits/s (1.8 KB/s)
                upload speed:        14400 bits/s (1.8 KB/s)
                minimum latency:  0 ms
                maximum latency:  0 ms
             */
            String[] result = readLines();

            if (isValid(result)) {
                // we only compare agains the min latency and the download speed
                // let's not rely on the order of the output, and simply loop through
                // the line testing the regexp.
                NetworkStatus status = new NetworkStatus();
                for (String line : result) {
                    Matcher m = sDownloadSpeedRegexp.matcher(line);
                    if (m.matches()) {
                        // get the string value
                        String value = m.group(1);

                        // get the index from the list
                        status.speed = getSpeedIndex(value);

                        // move on to next line.
                        continue;
                    }

                    m = sMinLatencyRegexp.matcher(line);
                    if (m.matches()) {
                        // get the string value
                        String value = m.group(1);

                        // get the index from the list
                        status.latency = getLatencyIndex(value);

                        // move on to next line.
                        continue;
                    }
                }

                return status;
            }
        }

        return null;
    }

    /**
     * Returns the current gsm status of the emulator
     * @return a {@link GsmStatus} object containing the gms status, or <code>null</code>
     * if the query failed.
     */
    public synchronized GsmStatus getGsmStatus() {
        if (sendCommand(COMMAND_GSM_STATUS)) {
            /*
             * result is in the format:
             * gsm status
             * gsm voice state: home
             * gsm data state:  home
             */

            String[] result = readLines();
            if (isValid(result)) {

                GsmStatus status = new GsmStatus();

                // let's not rely on the order of the output, and simply loop through
                // the line testing the regexp.
                for (String line : result) {
                    Matcher m = sVoiceStatusRegexp.matcher(line);
                    if (m.matches()) {
                        // get the string value
                        String value = m.group(1);

                        // get the index from the list
                        status.voice = GsmMode.getEnum(value.toLowerCase());

                        // move on to next line.
                        continue;
                    }

                    m = sDataStatusRegexp.matcher(line);
                    if (m.matches()) {
                        // get the string value
                        String value = m.group(1);

                        // get the index from the list
                        status.data = GsmMode.getEnum(value.toLowerCase());

                        // move on to next line.
                        continue;
                    }
                }

                return status;
            }
        }

        return null;
    }

    /**
     * Sets the GSM voice mode.
     * @param mode the {@link GsmMode} value.
     * @return RESULT_OK if success, an error String otherwise.
     * @throws InvalidParameterException if mode is an invalid value.
     */
    public synchronized String setGsmVoiceMode(GsmMode mode) throws InvalidParameterException {
        if (mode == GsmMode.UNKNOWN) {
            throw new InvalidParameterException();
        }

        String command = String.format(COMMAND_GSM_VOICE, mode.getTag());
        return processCommand(command);
    }

    /**
     * Sets the GSM data mode.
     * @param mode the {@link GsmMode} value
     * @return {@link #RESULT_OK} if success, an error String otherwise.
     * @throws InvalidParameterException if mode is an invalid value.
     */
    public synchronized String setGsmDataMode(GsmMode mode) throws InvalidParameterException {
        if (mode == GsmMode.UNKNOWN) {
            throw new InvalidParameterException();
        }

        String command = String.format(COMMAND_GSM_DATA, mode.getTag());
        return processCommand(command);
    }

    /**
     * Initiate an incoming call on the emulator.
     * @param number a string representing the calling number.
     * @return {@link #RESULT_OK} if success, an error String otherwise.
     */
    public synchronized String call(String number) {
        String command = String.format(COMMAND_GSM_CALL, number);
        return processCommand(command);
    }

    /**
     * Cancels a current call.
     * @param number the number of the call to cancel
     * @return {@link #RESULT_OK} if success, an error String otherwise.
     */
    public synchronized String cancelCall(String number) {
        String command = String.format(COMMAND_GSM_CANCEL_CALL, number);
        return processCommand(command);
    }

    /**
     * Sends an SMS to the emulator
     * @param number The sender phone number
     * @param message The SMS message. \ characters must be escaped. The carriage return is
     * the 2 character sequence  {'\', 'n' }
     *
     * @return {@link #RESULT_OK} if success, an error String otherwise.
     */
    public synchronized String sendSms(String number, String message) {
        String command = String.format(COMMAND_SMS_SEND, number, message);
        return processCommand(command);
    }

    /**
     * Sets the network speed.
     * @param selectionIndex The index in the {@link #NETWORK_SPEEDS} table.
     * @return {@link #RESULT_OK} if success, an error String otherwise.
     */
    public synchronized String setNetworkSpeed(int selectionIndex) {
        String command = String.format(COMMAND_NETWORK_SPEED, NETWORK_SPEEDS[selectionIndex]);
        return processCommand(command);
    }

    /**
     * Sets the network latency.
     * @param selectionIndex The index in the {@link #NETWORK_LATENCIES} table.
     * @return {@link #RESULT_OK} if success, an error String otherwise.
     */
    public synchronized String setNetworkLatency(int selectionIndex) {
        String command = String.format(COMMAND_NETWORK_LATENCY, NETWORK_LATENCIES[selectionIndex]);
        return processCommand(command);
    }

    public synchronized String sendLocation(double longitude, double latitude, double elevation) {

        Calendar c = Calendar.getInstance();

        double absLong = Math.abs(longitude);
        int longDegree = (int)Math.floor(absLong);
        char longDirection = 'E';
        if (longitude < 0) {
            longDirection = 'W';
        }

        double longMinute = (absLong - Math.floor(absLong)) * 60;

        double absLat = Math.abs(latitude);
        int latDegree = (int)Math.floor(absLat);
        char latDirection = 'N';
        if (latitude < 0) {
            latDirection = 'S';
        }

        double latMinute = (absLat - Math.floor(absLat)) * 60;

        String command = String.format(COMMAND_GPS,
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
                c.get(Calendar.SECOND), c.get(Calendar.MILLISECOND),
                latDegree, latMinute, latDirection,
                longDegree, longMinute, longDirection);

        return processCommand(command);
    }

    /**
     * Sends a command to the emulator console.
     * @param command The command string. <b>MUST BE TERMINATED BY \n</b>.
     * @return true if success
     */
    private boolean sendCommand(String command) {
        boolean result = false;
        try {
            byte[] bCommand;
            try {
                bCommand = command.getBytes(DEFAULT_ENCODING);
            } catch (UnsupportedEncodingException e) {
                // wrong encoding...
                return result;
            }

            // write the command
            AdbHelper.write(mSocketChannel, bCommand, bCommand.length, DdmPreferences.getTimeOut());

            result = true;
        } catch (IOException e) {
            return false;
        } finally {
            if (result == false) {
                // FIXME connection failed somehow, we need to disconnect the console.
                RemoveConsole(mPort);
            }
        }

        return result;
    }

    /**
     * Sends a command to the emulator and parses its answer.
     * @param command the command to send.
     * @return {@link #RESULT_OK} if success, an error message otherwise.
     */
    private String processCommand(String command) {
        if (sendCommand(command)) {
            String[] result = readLines();

            if (result != null && result.length > 0) {
                Matcher m = RE_KO.matcher(result[result.length-1]);
                if (m.matches()) {
                    return m.group(1);
                }
                return RESULT_OK;
            }

            return "Unable to communicate with the emulator";
        }

        return "Unable to send command to the emulator";
    }

    /**
     * Reads line from the console socket. This call is blocking until we read the lines:
     * <ul>
     * <li>OK\r\n</li>
     * <li>KO<msg>\r\n</li>
     * </ul>
     * @return the array of strings read from the emulator.
     */
    private String[] readLines() {
        try {
            ByteBuffer buf = ByteBuffer.wrap(mBuffer, 0, mBuffer.length);
            int numWaits = 0;
            boolean stop = false;

            while (buf.position() != buf.limit() && stop == false) {
                int count;

                count = mSocketChannel.read(buf);
                if (count < 0) {
                    return null;
                } else if (count == 0) {
                    if (numWaits * WAIT_TIME > STD_TIMEOUT) {
                        return null;
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

                // check the last few char aren't OK. For a valid message to test
                // we need at least 4 bytes (OK/KO + \r\n)
                if (buf.position() >= 4) {
                    int pos = buf.position();
                    if (endsWithOK(pos) || lastLineIsKO(pos)) {
                        stop = true;
                    }
                }
            }

            String msg = new String(mBuffer, 0, buf.position(), DEFAULT_ENCODING);
            return msg.split("\r\n"); //$NON-NLS-1$
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Returns true if the 4 characters *before* the current position are "OK\r\n"
     * @param currentPosition The current position
     */
    private boolean endsWithOK(int currentPosition) {
        if (mBuffer[currentPosition-1] == '\n' &&
                mBuffer[currentPosition-2] == '\r' &&
                mBuffer[currentPosition-3] == 'K' &&
                mBuffer[currentPosition-4] == 'O') {
            return true;
        }

        return false;
    }

    /**
     * Returns true if the last line starts with KO and is also terminated by \r\n
     * @param currentPosition the current position
     */
    private boolean lastLineIsKO(int currentPosition) {
        // first check that the last 2 characters are CRLF
        if (mBuffer[currentPosition-1] != '\n' ||
                mBuffer[currentPosition-2] != '\r') {
            return false;
        }

        // now loop backward looking for the previous CRLF, or the beginning of the buffer
        int i = 0;
        for (i = currentPosition-3 ; i >= 0; i--) {
            if (mBuffer[i] == '\n') {
                // found \n!
                if (i > 0 && mBuffer[i-1] == '\r') {
                    // found \r!
                    break;
                }
            }
        }

        // here it is either -1 if we reached the start of the buffer without finding
        // a CRLF, or the position of \n. So in both case we look at the characters at i+1 and i+2
        if (mBuffer[i+1] == 'K' && mBuffer[i+2] == 'O') {
            // found error!
            return true;
        }

        return false;
    }

    /**
     * Returns true if the last line of the result does not start with KO
     */
    private boolean isValid(String[] result) {
        if (result != null && result.length > 0) {
            return !(RE_KO.matcher(result[result.length-1]).matches());
        }
        return false;
    }

    private int getLatencyIndex(String value) {
        try {
            // get the int value
            int latency = Integer.parseInt(value);

            // check for the speed from the index
            for (int i = 0 ; i < MIN_LATENCIES.length; i++) {
                if (MIN_LATENCIES[i] == latency) {
                    return i;
                }
            }
        } catch (NumberFormatException e) {
            // Do nothing, we'll just return -1.
        }

        return -1;
    }

    private int getSpeedIndex(String value) {
        try {
            // get the int value
            int speed = Integer.parseInt(value);

            // check for the speed from the index
            for (int i = 0 ; i < DOWNLOAD_SPEEDS.length; i++) {
                if (DOWNLOAD_SPEEDS[i] == speed) {
                    return i;
                }
            }
        } catch (NumberFormatException e) {
            // Do nothing, we'll just return -1.
        }

        return -1;
    }
}
