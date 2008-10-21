/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.common;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


/**
 * The activator class controls the plug-in life cycle
 */
public class CommonPlugin extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "com.android.ide.eclipse.common"; // $NON-NLS-1$

    // The shared instance
    private static CommonPlugin sPlugin;

    // The global android console
    private MessageConsole mAndroidConsole;

    /**
     * The constructor
     */
    public CommonPlugin() {
        // pass
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static CommonPlugin getDefault() {
        return sPlugin;
    }

    /** Returns the global android console */
    public MessageConsole getAndroidConsole() {
        return mAndroidConsole;
    }
    
    /**
     * The <code>AbstractUIPlugin</code> implementation of this <code>Plugin</code>
     * method refreshes the plug-in actions.  Subclasses may extend this method,
     * but must send super <b>first</b>.
     * 
     * {@inheritDoc}
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        sPlugin = this;
        
        /*
         * WARNING: think before adding any initialization here as plugins are dynamically
         * started and since no UI is being displayed by this plugin, it'll only start when
         * another plugin accesses some of its code.
         */
        
        // set the default android console.
        mAndroidConsole = new MessageConsole("Android", null); //$NON-NLS-1$
        ConsolePlugin.getDefault().getConsoleManager().addConsoles(
                new IConsole[] { mAndroidConsole });
    }

    /**
     * The <code>AbstractUIPlugin</code> implementation of this <code>Plugin</code>
     * method saves this plug-in's preference and dialog stores and shuts down 
     * its image registry (if they are in use). Subclasses may extend this
     * method, but must send super <b>last</b>. A try-finally statement should
     * be used where necessary to ensure that <code>super.shutdown()</code> is
     * always done.
     * 
     * {@inheritDoc}
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        sPlugin = null;
        super.stop(context);
    }

    /**
     * Logs a message to the default Eclipse log.
     * 
     * @param severity One of IStatus' severity codes: OK, ERROR, INFO, WARNING or CANCEL.
     * @param format The format string, like for String.format().
     * @param args The arguments for the format string, like for String.format().
     */
    public static void log(int severity, String format, Object ... args) {
        String message = String.format(format, args);
        Status status = new Status(severity, PLUGIN_ID, message);
        getDefault().getLog().log(status);
    }

    /**
     * Logs an exception to the default Eclipse log.
     * <p/>
     * The status severity is always set to ERROR.
     * 
     * @param exception The exception to log. Its call trace will be recorded.
     * @param format The format string, like for String.format().
     * @param args The arguments for the format string, like for String.format().
     */
    public static void log(Throwable exception, String format, Object ... args) {
        String message = String.format(format, args);
        Status status = new Status(IStatus.ERROR, PLUGIN_ID, message, exception);
        getDefault().getLog().log(status);
    }
    
    private static Display getDisplay() {
        IWorkbench bench = sPlugin.getWorkbench();
        if (bench!=null) {
            return bench.getDisplay();
        }
        return null;
    }

    /**
     * Display a yes/no question dialog box. This dialog is opened synchronously in the ui thread,
     * therefore this message can be called from any thread.
     * @param title The title of the dialog box
     * @param message The error message
     * @return true if OK was clicked.
     */
    public final static boolean displayPrompt(final String title, final String message) {
        // get the current Display and Shell
        final Display display = getDisplay();

        // we need to ask the user what he wants to do.
        final Boolean[] wrapper = new Boolean[] { new Boolean(false) };
        display.syncExec(new Runnable() {
            public void run() {
                Shell shell = display.getActiveShell();
                wrapper[0] = new Boolean(MessageDialog.openQuestion(shell, title, message));
            }
        });
        return wrapper[0].booleanValue();
    }

}
