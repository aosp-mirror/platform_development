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

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;

import com.android.monkeyrunner.doc.MonkeyRunnerExported;

import org.python.core.ArgParser;
import org.python.core.PyDictionary;
import org.python.core.PyException;
import org.python.core.PyObject;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;

/*
 * Abstract base class that represents a single connected Android
 * Device and provides MonkeyRunner API methods for interacting with
 * that device.  Each backend will need to create a concrete
 * implementation of this class.
 */
public abstract class MonkeyDevice {
    /**
     * Create a MonkeyMananger for talking to this device.
     *
     * NOTE: This is not part of the jython API.
     *
     * @return the MonkeyManager
     */
    public abstract MonkeyManager getManager();

    /**
     * Dispose of any native resoureces this device may have taken hold of.
     *
     *  NOTE: This is not part of the jython API.
     */
    public abstract void dispose();

    @MonkeyRunnerExported(doc = "Fetch the screenbuffer from the device and return it.",
            returns = "The captured snapshot.")
    public abstract MonkeyImage takeSnapshot();

    @MonkeyRunnerExported(doc = "Get a MonkeyRunner property (like build.fingerprint)",
            args = {"key"},
            argDocs = {"The key of the property to return"},
            returns = "The value of the property")
    public String getProperty(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        return getProperty(ap.getString(0));
    }

    @MonkeyRunnerExported(doc = "Get a system property (returns the same value as getprop).",
            args = {"key"},
            argDocs = {"The key of the property to return"},
            returns = "The value of the property")
    public String getSystemProperty(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);
        return getSystemProperty(ap.getString(0));
    }

    @MonkeyRunnerExported(doc = "Enumeration of possible touch and press event types.  This gets " +
            "passed into a press or touch call to specify the event type.",
            argDocs = {"Indicates the down part of a touch/press event",
            "Indicates the up part of a touch/press event.",
            "Indicates that the monkey should send a down event immediately " +
                "followed by an up event"})
    public enum TouchPressType {
        DOWN, UP, DOWN_AND_UP
    }

    @MonkeyRunnerExported(doc = "Send a touch event at the specified location",
            args = { "x", "y", "type" },
            argDocs = { "x coordinate", "y coordinate", "the type of touch event to send"})
    public void touch(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        int x = ap.getInt(0);
        int y = ap.getInt(1);

        // Default
        MonkeyDevice.TouchPressType type = MonkeyDevice.TouchPressType.DOWN_AND_UP;
        try {
            PyObject pyObject = ap.getPyObject(2);
            type = (TouchPressType) pyObject.__tojava__(MonkeyDevice.TouchPressType.class);
        } catch (PyException e) {
            // bad stuff was passed in, just use the already specified default value
            type = MonkeyDevice.TouchPressType.DOWN_AND_UP;
        }
        touch(x, y, type);
    }

    @MonkeyRunnerExported(doc = "Send a key press event to the specified button",
            args = { "name", "type" },
            argDocs = { "the name of the key to press", "the type of touch event to send"})
    public void press(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        String name = ap.getString(0);

        // Default
        MonkeyDevice.TouchPressType type = MonkeyDevice.TouchPressType.DOWN_AND_UP;
        try {
            PyObject pyObject = ap.getPyObject(1);
            type = (TouchPressType) pyObject.__tojava__(MonkeyDevice.TouchPressType.class);
        } catch (PyException e) {
            // bad stuff was passed in, just use the already specified default value
            type = MonkeyDevice.TouchPressType.DOWN_AND_UP;
        }
        press(name, type);
    }

    @MonkeyRunnerExported(doc = "Type the specified string on the keyboard.",
            args = { "message" },
            argDocs = { "the message to type." })
    public void type(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        String message = ap.getString(0);
        type(message);
    }

    @MonkeyRunnerExported(doc = "Execute the given command on the shell.",
            args = { "cmd"},
            argDocs = { "The command to execute" },
            returns = "The output of the command")
    public String shell(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        String cmd = ap.getString(0);
        return shell(cmd);
    }

    @MonkeyRunnerExported(doc = "Reboot the specified device",
            args = { "into" },
            argDocs = { "the bootloader to reboot into (bootloader, recovery, or None)"})
    public void reboot(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        String into = ap.getString(0, null);

        reboot(into);
    }

    @MonkeyRunnerExported(doc = "Install the specified apk onto the device.",
            args = { "path" },
            argDocs = { "The path on the host filesystem to the APK to install." },
            returns = "True if install succeeded")
    public boolean installPackage(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        String path = ap.getString(0);
        return installPackage(path);
    }

    @MonkeyRunnerExported(doc = "Remove the specified package from the device.",
            args = { "package"},
            argDocs = { "The name of the package to uninstall"},
            returns = "'True if remove succeeded")
    public boolean removePackage(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        String packageName = ap.getString(0);
        return removePackage(packageName);
    }

    @MonkeyRunnerExported(doc = "Start the Activity specified by the intent.",
            args = { "uri", "action", "data", "mimetype", "categories", "extras",
                     "component", "flags" },
            argDocs = { "The URI for the intent",
                        "The action for the intent",
                        "The data URI for the intent",
                        "The mime type for the intent",
                        "The list of category names for the intent",
                        "A dictionary of extras to add to the intent.  Types of these extras " +
                            "are inferred from the python types of the values",
                        "The component of the intent",
                        "A list of flags for the intent" })
    public void startActivity(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        String uri = ap.getString(0, null);
        String action = ap.getString(1, null);
        String data = ap.getString(2, null);
        String mimetype = ap.getString(3, null);
        Collection<String> categories = Collections2.transform(JythonUtils.getList(ap, 4),
                Functions.toStringFunction());
        Map<String, Object> extras = JythonUtils.getMap(ap, 5);
        String component = ap.getString(6, null);
        int flags = ap.getInt(7, 0);

        startActivity(uri, action, data, mimetype, categories, extras, component, flags);
    }

    @MonkeyRunnerExported(doc = "Start the specified broadcast intent on the device.",
            args = { "uri", "action", "data", "mimetype", "categories", "extras",
                     "component", "flags" },
            argDocs = { "The URI for the intent",
                        "The action for the intent",
                        "The data URI for the intent",
                        "The mime type for the intent",
                        "The list of category names for the intent",
                        "A dictionary of extras to add to the intent.  Types of these extras " +
                            "are inferred from the python types of the values",
                        "The component of the intent",
                        "A list of flags for the intent" })
    public void broadcastIntent(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        String uri = ap.getString(0, null);
        String action = ap.getString(1, null);
        String data = ap.getString(2, null);
        String mimetype = ap.getString(3, null);
        Collection<String> categories = Collections2.transform(JythonUtils.getList(ap, 4),
                Functions.toStringFunction());
        Map<String, Object> extras = JythonUtils.getMap(ap, 5);
        String component = ap.getString(6, null);
        int flags = ap.getInt(7, 0);

        broadcastIntent(uri, action, data, mimetype, categories, extras, component, flags);
    }

    @MonkeyRunnerExported(doc = "Instrument the specified package and return the results from it.",
            args = { "className", "args" },
            argDocs = { "The class name to instrument (like com.android.test/.TestInstrument)",
                        "A Map of String to Objects for the aruments to pass to this " +
                        "instrumentation (default value is None)" },
            returns = "A map of string to objects for the results this instrumentation returned")
    public PyDictionary instrument(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        String packageName = ap.getString(0);
        Map<String, Object> instrumentArgs = JythonUtils.getMap(ap, 1);
        if (instrumentArgs == null) {
            instrumentArgs = Collections.emptyMap();
        }

        Map<String, Object> result = instrument(packageName, instrumentArgs);
        return JythonUtils.convertMapToDict(result);
    }

    public void wake(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        wake();
    }

    /**
     * Reboot the device.
     *
     * @param into which bootloader to boot into.  Null means default reboot.
     */
    protected abstract void reboot(@Nullable String into);

    protected abstract String getProperty(String key);
    protected abstract String getSystemProperty(String key);
    protected abstract void touch(int x, int y, TouchPressType type);
    protected abstract void press(String keyName, TouchPressType type);
    protected abstract void type(String string);
    protected abstract String shell(String cmd);
    protected abstract boolean installPackage(String path);
    protected abstract boolean removePackage(String packageName);
    protected abstract void startActivity(@Nullable String uri, @Nullable String action,
            @Nullable String data, @Nullable String mimetype,
            Collection<String> categories, Map<String, Object> extras, @Nullable String component,
            int flags);
    protected abstract void broadcastIntent(@Nullable String uri, @Nullable String action,
            @Nullable String data, @Nullable String mimetype,
            Collection<String> categories, Map<String, Object> extras, @Nullable String component,
            int flags);
    protected abstract Map<String, Object> instrument(String packageName,
            Map<String, Object> args);
    protected abstract void wake();
}
