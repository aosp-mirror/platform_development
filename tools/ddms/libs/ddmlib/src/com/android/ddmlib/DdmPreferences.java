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

/**
 * Preferences for the ddm library.
 * <p/>This class does not handle storing the preferences. It is merely a central point for
 * applications using the ddmlib to override the default values.
 * <p/>Various components of the ddmlib query this class to get their values.
 * <p/>Calls to some <code>set##()</code> methods will update the components using the values
 * right away, while other methods will have no effect once {@link AndroidDebugBridge#init(boolean)}
 * has been called.
 * <p/>Check the documentation of each method.
 */
public final class DdmPreferences {

    /** Default value for thread update flag upon client connection. */
    public final static boolean DEFAULT_INITIAL_THREAD_UPDATE = false;
    /** Default value for heap update flag upon client connection. */
    public final static boolean DEFAULT_INITIAL_HEAP_UPDATE = false;
    /** Default value for the selected client debug port */
    public final static int DEFAULT_SELECTED_DEBUG_PORT = 8700;
    /** Default value for the debug port base */
    public final static int DEFAULT_DEBUG_PORT_BASE = 8600;
    /** Default value for the logcat {@link LogLevel} */
    public final static LogLevel DEFAULT_LOG_LEVEL = LogLevel.ERROR;
    /** Default timeout values for adb connection (milliseconds) */
    public static final int DEFAULT_TIMEOUT = 5000; // standard delay, in ms

    private static boolean sThreadUpdate = DEFAULT_INITIAL_THREAD_UPDATE;
    private static boolean sInitialHeapUpdate = DEFAULT_INITIAL_HEAP_UPDATE;

    private static int sSelectedDebugPort = DEFAULT_SELECTED_DEBUG_PORT;
    private static int sDebugPortBase = DEFAULT_DEBUG_PORT_BASE;
    private static LogLevel sLogLevel = DEFAULT_LOG_LEVEL;
    private static int sTimeOut = DEFAULT_TIMEOUT;

    /**
     * Returns the initial {@link Client} flag for thread updates.
     * @see #setInitialThreadUpdate(boolean)
     */
    public static boolean getInitialThreadUpdate() {
        return sThreadUpdate;
    }

    /**
     * Sets the initial {@link Client} flag for thread updates.
     * <p/>This change takes effect right away, for newly created {@link Client} objects.
     */
    public static void setInitialThreadUpdate(boolean state) {
        sThreadUpdate = state;
    }

    /**
     * Returns the initial {@link Client} flag for heap updates.
     * @see #setInitialHeapUpdate(boolean)
     */
    public static boolean getInitialHeapUpdate() {
        return sInitialHeapUpdate;
    }

    /**
     * Sets the initial {@link Client} flag for heap updates.
     * <p/>If <code>true</code>, the {@link ClientData} will automatically be updated with
     * the VM heap information whenever a GC happens.
     * <p/>This change takes effect right away, for newly created {@link Client} objects.
     */
    public static void setInitialHeapUpdate(boolean state) {
        sInitialHeapUpdate = state;
    }

    /**
     * Returns the debug port used by the selected {@link Client}.
     */
    public static int getSelectedDebugPort() {
        return sSelectedDebugPort;
    }

    /**
     * Sets the debug port used by the selected {@link Client}.
     * <p/>This change takes effect right away.
     * @param port the new port to use.
     */
    public static void setSelectedDebugPort(int port) {
        sSelectedDebugPort = port;

        MonitorThread monitorThread = MonitorThread.getInstance();
        if (monitorThread != null) {
            monitorThread.setDebugSelectedPort(port);
        }
    }

    /**
     * Returns the debug port used by the first {@link Client}. Following clients, will use the
     * next port.
     */
    public static int getDebugPortBase() {
        return sDebugPortBase;
    }

    /**
     * Sets the debug port used by the first {@link Client}.
     * <p/>Once a port is used, the next Client will use port + 1. Quitting applications will
     * release their debug port, and new clients will be able to reuse them.
     * <p/>This must be called before {@link AndroidDebugBridge#init(boolean)}.
     */
    public static void setDebugPortBase(int port) {
        sDebugPortBase = port;
    }

    /**
     * Returns the minimum {@link LogLevel} being displayed.
     */
    public static LogLevel getLogLevel() {
        return sLogLevel;
    }

    /**
     * Sets the minimum {@link LogLevel} to display.
     * <p/>This change takes effect right away.
     */
    public static void setLogLevel(String value) {
        sLogLevel = LogLevel.getByString(value);

        Log.setLevel(sLogLevel);
    }

    /**
     * Returns the timeout to be used in adb connections (milliseconds).
     */
    public static int getTimeOut() {
        return sTimeOut;
    }

    /**
     * Sets the timeout value for adb connection.
     * <p/>This change takes effect for newly created connections only.
     * @param timeOut the timeout value (milliseconds).
     */
    public static void setTimeOut(int timeOut) {
        sTimeOut = timeOut;
    }

    /**
     * Non accessible constructor.
     */
    private DdmPreferences() {
        // pass, only static methods in the class.
    }
}
