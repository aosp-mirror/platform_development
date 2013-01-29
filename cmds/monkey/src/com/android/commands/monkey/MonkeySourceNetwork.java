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
    /* The version of the monkey network protocol */
    public static final int MONKEY_NETWORK_VERSION = 2;
    private static DeferredReturn deferredReturn;

    /**
     * ReturnValue from the MonkeyCommand that indicates whether the
     * command was sucessful or not.
     */
    public static class MonkeyCommandReturn {
        private final boolean success;
        private final String message;

        public MonkeyCommandReturn(boolean success) {
            this.success = success;
            this.message = null;
        }

        public MonkeyCommandReturn(boolean success,
                                   String message) {
            this.success = success;
            this.message = message;
        }

        boolean hasMessage() {
            return message != null;
        }

        String getMessage() {
            return message;
        }

        boolean wasSuccessful() {
            return success;
        }
    }

    public final static MonkeyCommandReturn OK = new MonkeyCommandReturn(true);
    public final static MonkeyCommandReturn ERROR = new MonkeyCommandReturn(false);
    public final static MonkeyCommandReturn EARG = new MonkeyCommandReturn(false,
                                                                            "Invalid Argument");

    /**
     * Interface that MonkeyCommands must implement.
     */
    public interface MonkeyCommand {
        /**
         * Translate the command line into a sequence of MonkeyEvents.
         *
         * @param command the command line.
         * @param queue the command queue.
         * @return MonkeyCommandReturn indicating what happened.
         */
        MonkeyCommandReturn translateCommand(List<String> command, CommandQueue queue);
    }

    /**
     * Command to simulate closing and opening the keyboard.
     */
    private static class FlipCommand implements MonkeyCommand {
        // flip open
        // flip closed
        public MonkeyCommandReturn translateCommand(List<String> command,
                                                    CommandQueue queue) {
            if (command.size() > 1) {
                String direction = command.get(1);
                if ("open".equals(direction)) {
                    queue.enqueueEvent(new MonkeyFlipEvent(true));
                    return OK;
                } else if ("close".equals(direction)) {
                    queue.enqueueEvent(new MonkeyFlipEvent(false));
                    return OK;
                }
            }
            return EARG;
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
        public MonkeyCommandReturn translateCommand(List<String> command,
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
                    return EARG;
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
                    return EARG;
                }

                queue.enqueueEvent(new MonkeyTouchEvent(action)
                        .addPointer(0, x, y));
                return OK;
            }
            return EARG;
        }
    }

    /**
     * Command to send Trackball events to the input system.
     */
    private static class TrackballCommand implements MonkeyCommand {
        // trackball [dx] [dy]
        // trackball 1 0 -- move right
        // trackball -1 0 -- move left
        public MonkeyCommandReturn translateCommand(List<String> command,
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
                    return EARG;
                }
                queue.enqueueEvent(new MonkeyTrackballEvent(MotionEvent.ACTION_MOVE)
                        .addPointer(0, dx, dy));
                return OK;

            }
            return EARG;
        }
    }

    /**
     * Command to send Key events to the input system.
     */
    private static class KeyCommand implements MonkeyCommand {
        // key [down|up] [keycode]
        // key down 82
        // key up 82
        public MonkeyCommandReturn translateCommand(List<String> command,
                                                    CommandQueue queue) {
            if (command.size() == 3) {
                int keyCode = getKeyCode(command.get(2));
                if (keyCode < 0) {
                    // Ok, you gave us something bad.
                    Log.e(TAG, "Can't find keyname: " + command.get(2));
                    return EARG;
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
                    return EARG;
                }
                queue.enqueueEvent(new MonkeyKeyEvent(action, keyCode));
                return OK;
            }
            return EARG;
        }
    }

    /**
     * Get an integer keycode value from a given keyname.
     *
     * @param keyName the key name to get the code for
     * @return the integer keycode value, or -1 on error.
     */
    private static int getKeyCode(String keyName) {
        int keyCode = -1;
        try {
            keyCode = Integer.parseInt(keyName);
        } catch (NumberFormatException e) {
            // Ok, it wasn't a number, see if we have a
            // keycode name for it
            keyCode = MonkeySourceRandom.getKeyCode(keyName);
            if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
                // OK, one last ditch effort to find a match.
                // Build the KEYCODE_STRING from the string
                // we've been given and see if that key
                // exists.  This would allow you to do "key
                // down menu", for example.
                keyCode = MonkeySourceRandom.getKeyCode("KEYCODE_" + keyName.toUpperCase());
                if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
                    // Still unknown
                    return -1;
                }
            }
        }
        return keyCode;
    }

    /**
     * Command to put the Monkey to sleep.
     */
    private static class SleepCommand implements MonkeyCommand {
        // sleep 2000
        public MonkeyCommandReturn translateCommand(List<String> command,
                                                    CommandQueue queue) {
            if (command.size() == 2) {
                int sleep = -1;
                String sleepStr = command.get(1);
                try {
                    sleep = Integer.parseInt(sleepStr);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Not a number: " + sleepStr, e);
                    return EARG;
                }
                queue.enqueueEvent(new MonkeyThrottleEvent(sleep));
                return OK;
            }
            return EARG;
        }
    }

    /**
     * Command to type a string
     */
    private static class TypeCommand implements MonkeyCommand {
        // wake
        public MonkeyCommandReturn translateCommand(List<String> command,
                                                    CommandQueue queue) {
            if (command.size() == 2) {
                String str = command.get(1);

                char[] chars = str.toString().toCharArray();

                // Convert the string to an array of KeyEvent's for
                // the built in keymap.
                KeyCharacterMap keyCharacterMap = KeyCharacterMap.
                        load(KeyCharacterMap.VIRTUAL_KEYBOARD);
                KeyEvent[] events = keyCharacterMap.getEvents(chars);

                // enqueue all the events we just got.
                for (KeyEvent event : events) {
                    queue.enqueueEvent(new MonkeyKeyEvent(event));
                }
                return OK;
            }
            return EARG;
        }
    }

    /**
     * Command to wake the device up
     */
    private static class WakeCommand implements MonkeyCommand {
        // wake
        public MonkeyCommandReturn translateCommand(List<String> command,
                                                    CommandQueue queue) {
            if (!wake()) {
                return ERROR;
            }
            return OK;
        }
    }

    /**
     * Command to "tap" at a location (Sends a down and up touch
     * event).
     */
    private static class TapCommand implements MonkeyCommand {
        // tap x y
        public MonkeyCommandReturn translateCommand(List<String> command,
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
                    return EARG;
                }

                queue.enqueueEvent(new MonkeyTouchEvent(MotionEvent.ACTION_DOWN)
                        .addPointer(0, x, y));
                queue.enqueueEvent(new MonkeyTouchEvent(MotionEvent.ACTION_UP)
                        .addPointer(0, x, y));
                return OK;
            }
            return EARG;
        }
    }

    /**
     * Command to "press" a buttons (Sends an up and down key event.)
     */
    private static class PressCommand implements MonkeyCommand {
        // press keycode
        public MonkeyCommandReturn translateCommand(List<String> command,
                                                    CommandQueue queue) {
            if (command.size() == 2) {
                int keyCode = getKeyCode(command.get(1));
                if (keyCode < 0) {
                    // Ok, you gave us something bad.
                    Log.e(TAG, "Can't find keyname: " + command.get(1));
                    return EARG;
                }

                queue.enqueueEvent(new MonkeyKeyEvent(KeyEvent.ACTION_DOWN, keyCode));
                queue.enqueueEvent(new MonkeyKeyEvent(KeyEvent.ACTION_UP, keyCode));
                return OK;

            }
            return EARG;
        }
    }

    /**
     * Command to defer the return of another command until the given event occurs.
     * deferreturn takes three arguments. It takes an event to wait for (e.g. waiting for the
     * device to display a different activity would the "screenchange" event), a
     * timeout, which is the number of microseconds to wait for the event to occur, and it takes
     * a command. The command can be any other Monkey command that can be issued over the network
     * (e.g. press KEYCODE_HOME). deferreturn will then run this command, return an OK, wait for
     * the event to occur and return the deferred return value when either the event occurs or
     * when the timeout is reached (whichever occurs first). Note that there is no difference
     * between an event occurring and the timeout being reached; the client will have to verify
     * that the change actually occured.
     *
     * Example:
     *     deferreturn screenchange 1000 press KEYCODE_HOME
     * This command will press the home key on the device and then wait for the screen to change
     * for up to one second. Either the screen will change, and the results fo the key press will
     * be returned to the client, or the timeout will be reached, and the results for the key
     * press will be returned to the client.
     */
    private static class DeferReturnCommand implements MonkeyCommand {
        // deferreturn [event] [timeout (ms)] [command]
        // deferreturn screenchange 100 tap 10 10
        public MonkeyCommandReturn translateCommand(List<String> command,
                                                    CommandQueue queue) {
            if (command.size() > 3) {
                String event = command.get(1);
                int eventId;
                if (event.equals("screenchange")) {
                    eventId = DeferredReturn.ON_WINDOW_STATE_CHANGE;
                } else {
                    return EARG;
                }
                long timeout = Long.parseLong(command.get(2));
                MonkeyCommand deferredCommand = COMMAND_MAP.get(command.get(3));
                if (deferredCommand != null) {
                    List<String> parts = command.subList(3, command.size());
                    MonkeyCommandReturn ret = deferredCommand.translateCommand(parts, queue);
                    deferredReturn = new DeferredReturn(eventId, ret, timeout);
                    return OK;
                }
            }
            return EARG;
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
            pm.wakeUp(SystemClock.uptimeMillis());
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
        COMMAND_MAP.put("listvar", new MonkeySourceNetworkVars.ListVarCommand());
        COMMAND_MAP.put("getvar", new MonkeySourceNetworkVars.GetVarCommand());
        COMMAND_MAP.put("listviews", new MonkeySourceNetworkViews.ListViewsCommand());
        COMMAND_MAP.put("queryview", new MonkeySourceNetworkViews.QueryViewCommand());
        COMMAND_MAP.put("getrootview", new MonkeySourceNetworkViews.GetRootViewCommand());
        COMMAND_MAP.put("getviewswithtext",
                        new MonkeySourceNetworkViews.GetViewsWithTextCommand());
        COMMAND_MAP.put("deferreturn", new DeferReturnCommand());
    }

    // QUIT command
    private static final String QUIT = "quit";
    // DONE command
    private static final String DONE = "done";

    // command response strings
    private static final String OK_STR = "OK";
    private static final String ERROR_STR = "ERROR";

    public static interface CommandQueue {
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
         * @return the next event, or null if there aren't any more.
         */
        public MonkeyEvent getNextQueuedEvent() {
            return queuedEvents.poll();
        }
    };

    // A holder class for a deferred return value. This allows us to defer returning the success of
    // a call until a given event has occurred.
    private static class DeferredReturn {
        public static final int ON_WINDOW_STATE_CHANGE = 1;

        private int event;
        private MonkeyCommandReturn deferredReturn;
        private long timeout;

        public DeferredReturn(int event, MonkeyCommandReturn deferredReturn, long timeout) {
            this.event = event;
            this.deferredReturn = deferredReturn;
            this.timeout = timeout;
        }

        /**
         * Wait until the given event has occurred before returning the value.
         * @return The MonkeyCommandReturn from the command that was deferred.
         */
        public MonkeyCommandReturn waitForEvent() {
            switch(event) {
                case ON_WINDOW_STATE_CHANGE:
                    try {
                        synchronized(MonkeySourceNetworkViews.class) {
                            MonkeySourceNetworkViews.class.wait(timeout);
                        }
                    } catch(InterruptedException e) {
                        Log.d(TAG, "Deferral interrupted: " + e.getMessage());
                    }
            }
            return deferredReturn;
        }
    };

    private final CommandQueueImpl commandQueue = new CommandQueueImpl();

    private BufferedReader input;
    private PrintWriter output;
    private boolean started = false;

    private ServerSocket serverSocket;
    private Socket clientSocket;

    public MonkeySourceNetwork(int port) throws IOException {
        // Only bind this to local host.  This means that you can only
        // talk to the monkey locally, or though adb port forwarding.
        serverSocket = new ServerSocket(port,
                                        0, // default backlog
                                        InetAddress.getLocalHost());
    }

    /**
     * Start a network server listening on the specified port.  The
     * network protocol is a line oriented protocol, where each line
     * is a different command that can be run.
     *
     * @param port the port to listen on
     */
    private void startServer() throws IOException {
        clientSocket = serverSocket.accept();
        // At this point, we have a client connected.
        // Attach the accessibility listeners so that we can start receiving
        // view events. Do this before wake so we can catch the wake event
        // if possible.
        MonkeySourceNetworkViews.setup();
        // Wake the device up in preparation for doing some commands.
        wake();

        input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        // auto-flush
        output = new PrintWriter(clientSocket.getOutputStream(), true);
    }

    /**
     * Stop the server from running so it can reconnect a new client.
     */
    private void stopServer() throws IOException {
        clientSocket.close();
        MonkeySourceNetworkViews.teardown();
        input.close();
        output.close();
        started = false;
    }

    /**
     * Helper function for commandLineSplit that replaces quoted
     * charaters with their real values.
     *
     * @param input the string to do replacement on.
     * @return the results with the characters replaced.
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
     */
    private void translateCommand(String commandLine) {
        Log.d(TAG, "translateCommand: " + commandLine);
        List<String> parts = commandLineSplit(commandLine);
        if (parts.size() > 0) {
            MonkeyCommand command = COMMAND_MAP.get(parts.get(0));
            if (command != null) {
                MonkeyCommandReturn ret = command.translateCommand(parts, commandQueue);
                handleReturn(ret);
            }
        }
    }

    private void handleReturn(MonkeyCommandReturn ret) {
        if (ret.wasSuccessful()) {
            if (ret.hasMessage()) {
                returnOk(ret.getMessage());
            } else {
                returnOk();
            }
        } else {
            if (ret.hasMessage()) {
                returnError(ret.getMessage());
            } else {
                returnError();
            }
        }
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

                // Check to see if we have any returns that have been deferred. If so, now that
                // we've run the queued commands, wait for the given event to happen (or the timeout
                // to be reached), and handle the deferred MonkeyCommandReturn.
                if (deferredReturn != null) {
                    Log.d(TAG, "Waiting for event");
                    MonkeyCommandReturn ret = deferredReturn.waitForEvent();
                    deferredReturn = null;
                    handleReturn(ret);
                }

                String command = input.readLine();
                if (command == null) {
                    Log.d(TAG, "Connection dropped.");
                    // Treat this exactly the same as if the user had
                    // ended the session cleanly with a done commant.
                    command = DONE;
                }

                if (DONE.equals(command)) {
                    // stop the server so it can accept new connections
                    try {
                        stopServer();
                    } catch (IOException e) {
                        Log.e(TAG, "Got IOException shutting down!", e);
                        return null;
                    }
                    // return a noop event so we keep executing the main
                    // loop
                    return new MonkeyNoopEvent();
                }

                // Do quit checking here
                if (QUIT.equals(command)) {
                    // then we're done
                    Log.d(TAG, "Quit requested");
                    // let the host know the command ran OK
                    returnOk();
                    return null;
                }

                // Do comment checking here.  Comments aren't a
                // command, so we don't echo anything back to the
                // user.
                if (command.startsWith("#")) {
                    // keep going
                    continue;
                }

                // Translate the command line.  This will handle returning error/ok to the user
                translateCommand(command);
            }
        } catch (IOException e) {
            Log.e(TAG, "Exception: ", e);
            return null;
        }
    }

    /**
     * Returns ERROR to the user.
     */
    private void returnError() {
        output.println(ERROR_STR);
    }

    /**
     * Returns ERROR to the user.
     *
     * @param msg the error message to include
     */
    private void returnError(String msg) {
        output.print(ERROR_STR);
        output.print(":");
        output.println(msg);
    }

    /**
     * Returns OK to the user.
     */
    private void returnOk() {
        output.println(OK_STR);
    }

    /**
     * Returns OK to the user.
     *
     * @param returnValue the value to return from this command.
     */
    private void returnOk(String returnValue) {
        output.print(OK_STR);
        output.print(":");
        output.println(returnValue);
    }

    public void setVerbose(int verbose) {
        // We're not particualy verbose
    }

    public boolean validate() {
        // we have no pre-conditions to validate
        return true;
    }
}
