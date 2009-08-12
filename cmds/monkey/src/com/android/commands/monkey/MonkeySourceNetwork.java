/*
 * Copyright 2009, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.commands.monkey;

import android.content.Context;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.Integer;
import java.lang.NumberFormatException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.StringTokenizer;

/**
 * An Event source for getting Monkey Network Script commands from
 * over the network.
 */
public class MonkeySourceNetwork implements MonkeyEventSource {
    private static final String TAG = "MonkeyStub";

    private interface MonkeyCommand {
        MonkeyEvent translateCommand(List<String> command, CommandQueue queue);
    }

    /**
     * Command to simulate closing and opening the keyboard.
     */
    private static class FlipCommand implements MonkeyCommand {
        // flip open
        // flip closed
        public MonkeyEvent translateCommand(List<String> command,
                                            CommandQueue queue) {
            if (command.size() > 1) {
                String direction = command.get(1);
                if ("open".equals(direction)) {
                    return new MonkeyFlipEvent(true);
                } else if ("close".equals(direction)) {
                    return new MonkeyFlipEvent(false);
                }
            }
            return null;
        }
    }

    /**
     * Command to send touch events to the input system.
     */
    private static class TouchCommand implements MonkeyCommand {
        // touch [down|up|move] [x] [y]
        // touch down 120 120
        // touch move 140 140
        // touch up 140 140
        public MonkeyEvent translateCommand(List<String> command,
                                            CommandQueue queue) {
            if (command.size() == 4) {
                String actionName = command.get(1);
                int x = 0;
                int y = 0;
                try {
                    x = Integer.parseInt(command.get(2));
                    y = Integer.parseInt(command.get(3));
                } catch (NumberFormatException e) {
                    // Ok, it wasn't a number
                    Log.e(TAG, "Got something that wasn't a number", e);
                    return null;
                }

                // figure out the action
                int action = -1;
                if ("down".equals(actionName)) {
                    action = MotionEvent.ACTION_DOWN;
                } else if ("up".equals(actionName)) {
                    action = MotionEvent.ACTION_UP;
                } else if ("move".equals(actionName)) {
                    action = MotionEvent.ACTION_MOVE;
                }
                if (action == -1) {
                    Log.e(TAG, "Got a bad action: " + actionName);
                    return null;
                }

                return new MonkeyMotionEvent(MonkeyEvent.EVENT_TYPE_POINTER,
                                             -1, action, x, y, 0);
            }
            return null;

        }
    }

    /**
     * Command to send Trackball events to the input system.
     */
    private static class TrackballCommand implements MonkeyCommand {
        // trackball [dx] [dy]
        // trackball 1 0 -- move right
        // trackball -1 0 -- move left
        public MonkeyEvent translateCommand(List<String> command,
                                            CommandQueue queue) {
            if (command.size() == 3) {
                int dx = 0;
                int dy = 0;
                try {
                    dx = Integer.parseInt(command.get(1));
                    dy = Integer.parseInt(command.get(2));
                } catch (NumberFormatException e) {
                    // Ok, it wasn't a number
                    Log.e(TAG, "Got something that wasn't a number", e);
                    return null;
                }
                return new MonkeyMotionEvent(MonkeyEvent.EVENT_TYPE_TRACKBALL, -1,
                    MotionEvent.ACTION_MOVE, dx, dy, 0);

            }
            return null;
        }
    }

    /**
     * Command to send Key events to the input system.
     */
    private static class KeyCommand implements MonkeyCommand {
        // key [down|up] [keycode]
        // key down 82
        // key up 82
        public MonkeyEvent translateCommand(List<String> command,
                                            CommandQueue queue) {
            if (command.size() == 3) {
                int keyCode = getKeyCode(command.get(2));
                if (keyCode < 0) {
                    // Ok, you gave us something bad.
                    Log.e(TAG, "Can't find keyname: " + command.get(2));
                    return null;
                }
                Log.d(TAG, "keycode: " + keyCode);
                int action = -1;
                if ("down".equals(command.get(1))) {
                    action = KeyEvent.ACTION_DOWN;
                } else if ("up".equals(command.get(1))) {
                    action = KeyEvent.ACTION_UP;
                }
                if (action == -1) {
                    Log.e(TAG, "got unknown action.");
                    return null;
                }
                return new MonkeyKeyEvent(action, keyCode);
            }
            return null;
        }
    }

    /**
     * Get an integer keycode value from a given keyname.
     *
     * @param keyName the key name to get the code for
     * @returns the integer keycode value, or -1 on error.
     */
    private static int getKeyCode(String keyName) {
        int keyCode = -1;
        try {
            keyCode = Integer.parseInt(keyName);
        } catch (NumberFormatException e) {
            // Ok, it wasn't a number, see if we have a
            // keycode name for it
            keyCode = MonkeySourceRandom.getKeyCode(keyName);
            if (keyCode == -1) {
                // OK, one last ditch effort to find a match.
                // Build the KEYCODE_STRING from the string
                // we've been given and see if that key
                // exists.  This would allow you to do "key
                // down menu", for example.
                keyCode = MonkeySourceRandom.getKeyCode("KEYCODE_" + keyName.toUpperCase());
            }
        }
        return keyCode;
    }

    /**
     * Command to put the Monkey to sleep.
     */
    private static class SleepCommand implements MonkeyCommand {
        // sleep 2000
        public MonkeyEvent translateCommand(List<String> command,
                                            CommandQueue queue) {
            if (command.size() == 2) {
                int sleep = -1;
                String sleepStr = command.get(1);
                try {
                    sleep = Integer.parseInt(sleepStr);
                } catch (NumberFormatException e) {
                  Log.e(TAG, "Not a number: " + sleepStr, e);
                }
                return new MonkeyThrottleEvent(sleep);
            }
            return null;
        }
    }

    /**
     * Command to type a string
     */
    private static class TypeCommand implements MonkeyCommand {
        // wake
        public MonkeyEvent translateCommand(List<String> command,
                                            CommandQueue queue) {
            if (command.size() == 2) {
                String str = command.get(1);

                char[] chars = str.toString().toCharArray();

                // Convert the string to an array of KeyEvent's for
                // the built in keymap.
                KeyCharacterMap keyCharacterMap = KeyCharacterMap.
                        load(KeyCharacterMap.BUILT_IN_KEYBOARD);
                KeyEvent[] events = keyCharacterMap.getEvents(chars);

                // enqueue all the events we just got.
                for (KeyEvent event : events) {
                    queue.enqueueEvent(new MonkeyKeyEvent(event));
                }
                return new MonkeyNoopEvent();
            }
            return null;
        }
    }

    /**
     * Command to wake the device up
     */
    private static class WakeCommand implements MonkeyCommand {
        // wake
        public MonkeyEvent translateCommand(List<String> command,
                                            CommandQueue queue) {
            if (!wake()) {
                return null;
            }
            return new MonkeyNoopEvent();
        }
    }

    /**
     * Command to "tap" at a location (Sends a down and up touch
     * event).
     */
    private static class TapCommand implements MonkeyCommand {
        // tap x y
        public MonkeyEvent translateCommand(List<String> command,
                                            CommandQueue queue) {
            if (command.size() == 3) {
                int x = 0;
                int y = 0;
                try {
                    x = Integer.parseInt(command.get(1));
                    y = Integer.parseInt(command.get(2));
                } catch (NumberFormatException e) {
                    // Ok, it wasn't a number
                    Log.e(TAG, "Got something that wasn't a number", e);
                    return null;
                }

                queue.enqueueEvent(new MonkeyMotionEvent(MonkeyEvent.EVENT_TYPE_POINTER,
                                                         -1, MotionEvent.ACTION_DOWN,
                                                         x, y, 0));
                queue.enqueueEvent(new MonkeyMotionEvent(MonkeyEvent.EVENT_TYPE_POINTER,
                                                         -1, MotionEvent.ACTION_UP,
                                                         x, y, 0));
                return new MonkeyNoopEvent();
            }
            return null;
        }
    }

    /**
     * Command to "press" a buttons (Sends an up and down key event.)
     */
    private static class PressCommand implements MonkeyCommand {
        // press keycode
        public MonkeyEvent translateCommand(List<String> command,
                                            CommandQueue queue) {
            if (command.size() == 2) {
                int keyCode = getKeyCode(command.get(1));
                if (keyCode < 0) {
                    // Ok, you gave us something bad.
                    Log.e(TAG, "Can't find keyname: " + command.get(1));
                    return null;
                }

                queue.enqueueEvent(new MonkeyKeyEvent(KeyEvent.ACTION_DOWN, keyCode));
                queue.enqueueEvent(new MonkeyKeyEvent(KeyEvent.ACTION_UP, keyCode));
                return new MonkeyNoopEvent();

            }
            return null;
        }
    }

    /**
     * Force the device to wake up.
     *
     * @return true if woken up OK.
     */
    private static final boolean wake() {
        IPowerManager pm =
                IPowerManager.Stub.asInterface(ServiceManager.getService(Context.POWER_SERVICE));
        try {
            pm.userActivityWithForce(SystemClock.uptimeMillis(), true, true);
        } catch (RemoteException e) {
            Log.e(TAG, "Got remote exception", e);
            return false;
        }
        return true;
    }

    // This maps from command names to command implementations.
    private static final Map<String, MonkeyCommand> COMMAND_MAP = new HashMap<String, MonkeyCommand>();

    static {
        // Add in all the commands we support
        COMMAND_MAP.put("flip", new FlipCommand());
        COMMAND_MAP.put("touch", new TouchCommand());
        COMMAND_MAP.put("trackball", new TrackballCommand());
        COMMAND_MAP.put("key", new KeyCommand());
        COMMAND_MAP.put("sleep", new SleepCommand());
        COMMAND_MAP.put("wake", new WakeCommand());
        COMMAND_MAP.put("tap", new TapCommand());
        COMMAND_MAP.put("press", new PressCommand());
        COMMAND_MAP.put("type", new TypeCommand());
    }

    // QUIT command
    private static final String QUIT = "quit";

    // command response strings
    private static final String OK = "OK";
    private static final String ERROR = "ERROR";

    private static interface CommandQueue {
        /**
         * Enqueue an event to be returned later.  This allows a
         * command to return multiple events.  Commands using the
         * command queue still have to return a valid event from their
         * translateCommand method.  The returned command will be
         * executed before anything put into the queue.
         *
         * @param e the event to be enqueued.
         */
        public void enqueueEvent(MonkeyEvent e);
    };

    // Queue of Events to be processed.  This allows commands to push
    // multiple events into the queue to be processed.
    private static class CommandQueueImpl implements CommandQueue{
        private final Queue<MonkeyEvent> queuedEvents = new LinkedList<MonkeyEvent>();

        public void enqueueEvent(MonkeyEvent e) {
            queuedEvents.offer(e);
        }

        /**
         * Get the next queued event to excecute.
         *
         * @returns the next event, or null if there aren't any more.
         */
        public MonkeyEvent getNextQueuedEvent() {
            return queuedEvents.poll();
        }
    };

    private final CommandQueueImpl commandQueue = new CommandQueueImpl();

    private final int port;
    private BufferedReader input;
    private PrintWriter output;
    private boolean started = false;

    public MonkeySourceNetwork(int port) {
        this.port = port;
    }

    /**
     * Start a network server listening on the specified port.  The
     * network protocol is a line oriented protocol, where each line
     * is a different command that can be run.
     *
     * @param port the port to listen on
     */
    private void startServer() throws IOException {
        // Only bind this to local host.  This means that you can only
        // talk to the monkey locally, or though adb port forwarding.
        ServerSocket server = new ServerSocket(port,
                                               0, // default backlog
                                               InetAddress.getLocalHost());
        Socket s = server.accept();
        // At this point, we have a client connected.  Wake the device
        // up in preparation for doing some commands.
        wake();

        input = new BufferedReader(new InputStreamReader(s.getInputStream()));
        // auto-flush
        output = new PrintWriter(s.getOutputStream(), true);
    }

    /**
     * Helper function for commandLineSplit that replaces quoted
     * charaters with their real values.
     *
     * @param input the string to do replacement on.
     * @returns the results with the characters replaced.
     */
    private static String replaceQuotedChars(String input) {
        return input.replace("\\\"", "\"");
    }

    /**
     * This function splits the given line into String parts.  It obey's quoted
     * strings and returns them as a single part.
     *
     * "This is a test" -> returns only one element
     * This is a test -> returns four elements
     *
     * @param line the line to parse
     * @return the List of elements
     */
    private static List<String> commandLineSplit(String line) {
        ArrayList<String> result = new ArrayList<String>();
        StringTokenizer tok = new StringTokenizer(line);

        boolean insideQuote = false;
        StringBuffer quotedWord = new StringBuffer();
        while (tok.hasMoreTokens()) {
            String cur = tok.nextToken();
            if (!insideQuote && cur.startsWith("\"")) {
                // begin quote
                quotedWord.append(replaceQuotedChars(cur));
                insideQuote = true;
            } else if (insideQuote) {
                // end quote
                if (cur.endsWith("\"")) {
                    insideQuote = false;
                    quotedWord.append(" ").append(replaceQuotedChars(cur));
                    String word = quotedWord.toString();

                    // trim off the quotes
                    result.add(word.substring(1, word.length() - 1));
                } else {
                    quotedWord.append(" ").append(replaceQuotedChars(cur));
                }
            } else {
                result.add(replaceQuotedChars(cur));
            }
        }
        return result;
    }

    /**
     * Translate the given command line into a MonkeyEvent.
     *
     * @param commandLine the full command line given.
     * @returns the MonkeyEvent corresponding to the command, or null
     * if there was an issue.
     */
    private MonkeyEvent translateCommand(String commandLine) {
        Log.d(TAG, "translateCommand: " + commandLine);
        List<String> parts = commandLineSplit(commandLine);
        if (parts.size() > 0) {
            MonkeyCommand command = COMMAND_MAP.get(parts.get(0));
            if (command != null) {
                return command.translateCommand(parts,
                                                commandQueue);
            }
            return null;
        }
        return null;
    }

    public MonkeyEvent getNextEvent() {
        if (!started) {
            try {
                startServer();
            } catch (IOException e) {
                Log.e(TAG, "Got IOException from server", e);
                return null;
            }
            started = true;
        }

        // Now, get the next command.  This call may block, but that's OK
        try {
            while (true) {
                // Check to see if we have any events queued up.  If
                // we do, use those until we have no more.  Then get
                // more input from the user.
                MonkeyEvent queuedEvent = commandQueue.getNextQueuedEvent();
                if (queuedEvent != null) {
                    // dispatch the event
                    return queuedEvent;
                }

                String command = input.readLine();
                if (command == null) {
                    Log.d(TAG, "Connection dropped.");
                    return null;
                }
                // Do quit checking here
                if (QUIT.equals(command)) {
                    // then we're done
                    Log.d(TAG, "Quit requested");
                    // let the host know the command ran OK
                    output.println(OK);
                    return null;
                }

                // Do comment checking here.  Comments aren't a
                // command, so we don't echo anything back to the
                // user.
                if (command.startsWith("#")) {
                  // keep going
                  continue;
                }

                // Translate the command line
                MonkeyEvent event = translateCommand(command);
                if (event != null) {
                    // let the host know the command ran OK
                    output.println(OK);
                    return event;
                }
                // keep going.  maybe the next command will make more sense
                Log.e(TAG, "Got unknown command! \"" + command + "\"");
                output.println(ERROR);
            }
        } catch (IOException e) {
            Log.e(TAG, "Exception: ", e);
            return null;
        }
    }

    public void setVerbose(int verbose) {
        // We're not particualy verbose
    }

    public boolean validate() {
        // we have no pre-conditions to validate
        return true;
    }
}
