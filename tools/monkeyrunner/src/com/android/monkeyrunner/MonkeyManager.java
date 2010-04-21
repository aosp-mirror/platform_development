/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.monkeyrunner;

import com.google.common.collect.Lists;

import com.android.ddmlib.IDevice;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a nicer interface to interacting with the low-level network access protocol for talking
 * to the monkey.
 *
 * This class is thread-safe and can handle being called from multiple threads.
 */
public class MonkeyManager {
    private static String DEFAULT_MONKEY_SERVER_ADDRESS = "127.0.0.1";
    private static int DEFAULT_MONKEY_PORT = 12345;

    private static Logger LOG = Logger.getLogger(MonkeyManager.class.getName());

    private Socket monkeySocket;
    private BufferedWriter monkeyWriter;
    private BufferedReader monkeyReader;
    private final IDevice device;

    /**
     * Create a new MonkeyMananger to talk to the specified device.
     *
     * @param device the device to talk to
     * @param address the address on which to talk to the device
     * @param port the port on which to talk to the device
     */
    public MonkeyManager(IDevice device, String address, int port) {
        this.device = device;
        device.createForward(port, port);
        String command = "monkey --port " + port + "&";
        try {
            device.executeShellCommand(command, new LoggingOutputReceiver(LOG));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            InetAddress addr = InetAddress.getByName(address);
            monkeySocket = new Socket(addr, port);
            monkeyWriter = new BufferedWriter(new OutputStreamWriter(monkeySocket.getOutputStream()));
            monkeyReader = new BufferedReader(new InputStreamReader(monkeySocket.getInputStream()));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a new MonkeyMananger to talk to the specified device.
     *
     * @param device the device to talk to
     */
    public MonkeyManager(IDevice device) {
        this(device, DEFAULT_MONKEY_SERVER_ADDRESS, DEFAULT_MONKEY_PORT);
    }

    /**
     * Send a touch down event at the specified location.
     *
     * @param x the x coordinate of where to click
     * @param y the y coordinate of where to click
     * @return success or not
     * @throws IOException on error communicating with the device
     */
    public boolean touchDown(int x, int y) throws IOException {
        return sendMonkeyEvent("touch down " + x + " " + y);
    }

    /**
     * Send a touch down event at the specified location.
     *
     * @param x the x coordinate of where to click
     * @param y the y coordinate of where to click
     * @return success or not
     * @throws IOException on error communicating with the device
     */
    public boolean touchUp(int x, int y) throws IOException {
        return sendMonkeyEvent("touch up " + x + " " + y);
    }

    /**
     * Send a touch (down and then up) event at the specified location.
     *
     * @param x the x coordinate of where to click
     * @param y the y coordinate of where to click
     * @return success or not
     * @throws IOException on error communicating with the device
     */
    public boolean touch(int x, int y) throws IOException {
        return sendMonkeyEvent("tap " + x + " " + y);
    }

    /**
     * Press a physical button on the device.
     *
     * @param name the name of the button (As specified in the protocol)
     * @return success or not
     * @throws IOException on error communicating with the device
     */
    public boolean press(String name) throws IOException {
        return sendMonkeyEvent("press " + name);
    }

    /**
     * Press a physical button on the device.
     *
     * @param button the button to press
     * @return success or not
     * @throws IOException on error communicating with the device
     */
    public boolean press(PhysicalButton button) throws IOException {
        return press(button.getKeyName());
    }

    /**
     * This function allows the communication bridge between the host and the device
     * to be invisible to the script for internal needs.
     * It splits a command into monkey events and waits for responses for each over an adb tcp socket.
     * Returns on an error, else continues and sets up last response.
     *
     * @param command the monkey command to send to the device
     * @return the (unparsed) response returned from the monkey.
     */
    private String sendMonkeyEventAndGetResponse(String command) throws IOException {
        command = command.trim();
        LOG.info("Monkey Command: " + command + ".");

        // send a single command and get the response
        monkeyWriter.write(command + "\n");
        monkeyWriter.flush();
        return monkeyReader.readLine();
    }

    /**
     * Parse a monkey response string to see if the command succeeded or not.
     *
     * @param monkeyResponse the response
     * @return true if response code indicated success.
     */
    private boolean parseResponseForSuccess(String monkeyResponse) {
        if (monkeyResponse == null) {
            return false;
        }
        // return on ok
        if(monkeyResponse.startsWith("OK")) {
            return true;
        }

        return false;
    }

    /**
     * Parse a monkey response string to get the extra data returned.
     *
     * @param monkeyResponse the response
     * @return any extra data that was returned, or empty string if there was nothing.
     */
    private String parseResponseForExtra(String monkeyResponse) {
        int offset = monkeyResponse.indexOf(':');
        if (offset < 0) {
            return "";
        }
        return monkeyResponse.substring(offset + 1);
    }

    /**
     * This function allows the communication bridge between the host and the device
     * to be invisible to the script for internal needs.
     * It splits a command into monkey events and waits for responses for each over an
     * adb tcp socket.
     *
     * @param command the monkey command to send to the device
     * @return true on success.
     */
    private boolean sendMonkeyEvent(String command) throws IOException {
        synchronized (this) {
            String monkeyResponse = sendMonkeyEventAndGetResponse(command);
            return parseResponseForSuccess(monkeyResponse);
        }
    }

    /**
     * Close all open resources related to this device.
     */
    public void close() {
        try {
            monkeySocket.close();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Unable to close monkeySocket", e);
        }
        try {
            monkeyReader.close();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Unable to close monkeyReader", e);
        }
        try {
            monkeyWriter.close();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Unable to close monkeyWriter", e);
        }
    }

    /**
     * Function to get a static variable from the device
     *
     * @param name name of static variable to get
     * @return the value of the variable, or empty string if there was an error
     */
    public String getVariable(String name) throws IOException {
        synchronized (this) {
            String response = sendMonkeyEventAndGetResponse("getvar " + name);
            if (!parseResponseForSuccess(response)) {
                return "";
            }
            return parseResponseForExtra(response);
        }
    }

    /**
     * Function to get the list of static variables from the device
     */
    public Collection<String> listVariable() throws IOException {
        synchronized (this) {
            String response = sendMonkeyEventAndGetResponse("listvar");
            if (!parseResponseForSuccess(response)) {
                Collections.emptyList();
            }
            String extras = parseResponseForExtra(response);
            return Lists.newArrayList(extras.split(" "));
        }
    }

    /**
     * Tells the monkey that we are done for this session.
     * @throws IOException
     */
    public void done() throws IOException {
        // this command just drops the connection, so handle it here
        synchronized (this) {
            sendMonkeyEventAndGetResponse("done");
        }
    }

    /**
     * Send a tap event at the specified location.
     *
     * @param x the x coordinate of where to click
     * @param y the y coordinate of where to click
     * @return success or not
     * @throws IOException
     * @throws IOException on error communicating with the device
     */
    public boolean tap(int x, int y) throws IOException {
        return sendMonkeyEvent("tap " + x + " " + y);
    }

    /**
     * Type the following string to the monkey.
     *
     * @param text the string to type
     * @return success
     * @throws IOException
     */
    public boolean type(String text) throws IOException {
        // The network protocol can't handle embedded line breaks, so we have to handle it
        // here instead
        StringTokenizer tok = new StringTokenizer(text, "\n", true);
        while (tok.hasMoreTokens()) {
            String line = tok.nextToken();
            if ("\n".equals(line)) {
                boolean success = press(PhysicalButton.ENTER);
                if (!success) {
                    return false;
                }
            } else {
                boolean success = sendMonkeyEvent("type " + line);
                if (!success) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Type the character to the monkey.
     *
     * @param keyChar the character to type.
     * @return success
     * @throws IOException
     */
    public boolean type(char keyChar) throws IOException {
        return type(Character.toString(keyChar));
    }

    /**
     * Gets the underlying device so low-level commands can be executed.
     *
     * NOTE: using this method doesn't provide any thread safety.  If needed, the MonkeyMananger
     * itself should be used as the lock for synchronization.  For Example:
     *
     * <code>
     * MonkeyMananger mgr;
     * IDevice device = mgr.getDevice();
     * synchronized (mgr) {
     *   /// Do stuff with the device
     * }
     * </code>
     *
     * @return the device.
     */
    public IDevice getDevice() {
        return device;
    }
}
