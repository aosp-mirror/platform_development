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
import org.python.core.PyException;
import org.python.core.PyObject;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

/**
 * This is the main interface class into the jython bindings.
 */
public class MonkeyRunner {
    private static final Logger LOG = Logger.getLogger(MonkeyRunner.class.getCanonicalName());
    private static MonkeyRunnerBackend backend;

    /**
     * Set the backend MonkeyRunner is using.
     *
     * @param backend the backend to use.
     */
    /* package */ static void setBackend(MonkeyRunnerBackend backend) {
        MonkeyRunner.backend = backend;
    }

    @MonkeyRunnerExported(doc = "Wait for the specified device to connect.",
            args = {"timeout", "deviceId"},
            argDocs = {"The timeout in seconds to wait for the device to connect. (default " +
                "is to wait forever)",
            "A regular expression that specifies the device of for valid devices" +
                " to wait for."},
    returns = "A MonkeyDevice representing the connected device.")
    public static MonkeyDevice waitForConnection(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        long timeoutMs;
        try {
            double timeoutInSecs = JythonUtils.getFloat(ap, 0);
            timeoutMs = (long) (timeoutInSecs * 1000.0);
        } catch (PyException e) {
            timeoutMs = Long.MAX_VALUE;
        }

        return backend.waitForConnection(timeoutMs,
                ap.getString(1, ".*"));
    }

    @MonkeyRunnerExported(doc = "Pause script processing for the specified number of seconds",
            args = {"seconds"},
            argDocs = {"The number of seconds to pause processing"})
            public static void sleep(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        double seconds = JythonUtils.getFloat(ap, 0);

        long ms = (long) (seconds * 1000.0);

        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            LOG.log(Level.SEVERE, "Error sleeping", e);
        }
    }

    @MonkeyRunnerExported(doc = "Simple help command to dump the MonkeyRunner supported " +
            "commands",
            returns = "The help text")
    public static String help(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        return MonkeyRunnerHelp.helpString();
    }

    @MonkeyRunnerExported(doc = "Put up an alert dialog to inform the user of something that " +
            "happened.  This is modal dialog and will stop processing of " +
            "the script until the user acknowledges the alert message",
            args = { "message", "title", "okTitle" },
            argDocs = {
            "The contents of the message of the dialog box",
            "The title to display for the dialog box.  (default value is \"Alert\")",
            "The title to use for the acknowledgement button (default value is \"OK\")"
    })
    public static void alert(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        String message = ap.getString(0);
        String title = ap.getString(1, "Alert");
        String buttonTitle = ap.getString(2, "OK");

        alert(message, title, buttonTitle);
    }

    @MonkeyRunnerExported(doc = "Put up an input dialog that allows the user to input a string." +
            "  This is a modal dialog that will stop processing of the script until the user " +
            "inputs the requested information.",
            args = {"message", "initialValue", "title", "okTitle", "cancelTitle"},
            argDocs = {
            "The message to display for the input.",
            "The initial value to supply the user (default is empty string)",
            "The title of the dialog box to display. (default is \"Input\")"
    },
    returns = "The test entered by the user, or None if the user canceled the input;"
    )
    public static String input(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        String message = ap.getString(0);
        String initialValue = ap.getString(1, "");
        String title = ap.getString(2, "Input");

        return input(message, initialValue, title);
    }

    @MonkeyRunnerExported(doc = "Put up a choice dialog that allows the user to select a single " +
            "item from a list of items that were presented.",
            args = {"message", "choices", "title"},
            argDocs = {
            "The message to display for the input.",
            "The list of choices to display.",
            "The title of the dialog box to display. (default is \"Input\")" },
            returns = "The numeric offset of the choice selected.")
    public static int choice(PyObject[] args, String kws[]) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        String message = ap.getString(0);
        Collection<String> choices = Collections2.transform(JythonUtils.getList(ap, 1),
                Functions.toStringFunction());
        String title = ap.getString(2, "Input");

        return choice(message, title, choices);
    }

    /**
     * Display an alert dialog.
     *
     * @param message the message to show.
     * @param title the title of the dialog box.
     * @param okTitle the title of the button.
     */
    private static void alert(String message, String title, String okTitle) {
        Object[] options = { okTitle };
        JOptionPane.showOptionDialog(null, message, title, JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
    }

    /**
     * Display a dialog allow the user to pick a choice from a list of choices.
     *
     * @param message the message to show.
     * @param title the title of the dialog box.
     * @param choices the list of the choices to display.
     * @return the index of the selected choice, or -1 if nothing was chosen.
     */
    private static int choice(String message, String title, Collection<String> choices) {
        Object[] possibleValues = choices.toArray();
        Object selectedValue = JOptionPane.showInputDialog(null, message, title,
                JOptionPane.QUESTION_MESSAGE, null, possibleValues, possibleValues[0]);

        for (int x = 0; x < possibleValues.length; x++) {
            if (possibleValues[x].equals(selectedValue)) {
                return x;
            }
        }
        // Error
        return -1;
    }

    /**
     * Display a dialog that allows the user to input a text string.
     *
     * @param message the message to show.
     * @param initialValue the initial value to display in the dialog
     * @param title the title of the dialog box.
     * @return the entered string, or null if cancelled
     */
    private static String input(String message, String initialValue, String title) {
        return (String) JOptionPane.showInputDialog(null, message, title,
                JOptionPane.QUESTION_MESSAGE, null, null, initialValue);
    }
}
