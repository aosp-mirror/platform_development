/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.sdkuilib.internal.repository;

import com.android.sdklib.SdkConstants;
import com.android.sdklib.internal.repository.ITaskMonitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * A lightweight wrapper to start & stop ADB.
 */
public class AdbWrapper {

    /*
     * Note: we could bring ddmlib in SdkManager for that purpose, however this allows us to
     * specialize the start/stop methods to our needs (e.g. a task monitor, etc.)
     */

    private final String mAdbOsLocation;
    private final ITaskMonitor mMonitor;

    /**
     * Creates a new lightweight ADB wrapper.
     *
     * @param osSdkPath The root OS path of the SDK. Cannot be null.
     * @param monitor A logger object. Cannot be null.
     */
    public AdbWrapper(String osSdkPath, ITaskMonitor monitor) {
        mMonitor = monitor;

        if (!osSdkPath.endsWith(File.separator)) {
            osSdkPath += File.separator;
        }
        mAdbOsLocation = osSdkPath + SdkConstants.OS_SDK_TOOLS_FOLDER + SdkConstants.FN_ADB;
    }

    private void display(String format, Object...args) {
        mMonitor.setResult(format, args);
    }

    /**
     * Starts the adb host side server.
     * @return true if success
     */
    public synchronized boolean startAdb() {
        if (mAdbOsLocation == null) {
            display("Error: missing path to ADB."); //$NON-NLS-1$
            return false;
        }

        Process proc;
        int status = -1;

        try {
            String[] command = new String[2];
            command[0] = mAdbOsLocation;
            command[1] = "start-server"; //$NON-NLS-1$
            proc = Runtime.getRuntime().exec(command);

            ArrayList<String> errorOutput = new ArrayList<String>();
            ArrayList<String> stdOutput = new ArrayList<String>();
            status = grabProcessOutput(proc, errorOutput, stdOutput,
                    false /* waitForReaders */);

        } catch (IOException ioe) {
            display("Unable to run 'adb': %1$s.", ioe.getMessage()); //$NON-NLS-1$
            // we'll return false;
        } catch (InterruptedException ie) {
            display("Unable to run 'adb': %1$s.", ie.getMessage()); //$NON-NLS-1$
            // we'll return false;
        }

        if (status != 0) {
            display("'adb start-server' failed."); //$NON-NLS-1$
            return false;
        }

        display("'adb start-server' succeeded."); //$NON-NLS-1$

        return true;
    }

    /**
     * Stops the adb host side server.
     * @return true if success
     */
    public synchronized boolean stopAdb() {
        if (mAdbOsLocation == null) {
            display("Error: missing path to ADB."); //$NON-NLS-1$
            return false;
        }

        Process proc;
        int status = -1;

        try {
            String[] command = new String[2];
            command[0] = mAdbOsLocation;
            command[1] = "kill-server"; //$NON-NLS-1$
            proc = Runtime.getRuntime().exec(command);
            status = proc.waitFor();
        }
        catch (IOException ioe) {
            // we'll return false;
        }
        catch (InterruptedException ie) {
            // we'll return false;
        }

        if (status != 0) {
            display("'adb kill-server' failed -- run manually if necessary."); //$NON-NLS-1$
            return false;
        }

        display("'adb kill-server' succeeded."); //$NON-NLS-1$
        return true;
    }

    /**
     * Get the stderr/stdout outputs of a process and return when the process is done.
     * Both <b>must</b> be read or the process will block on windows.
     * @param process The process to get the ouput from
     * @param errorOutput The array to store the stderr output. cannot be null.
     * @param stdOutput The array to store the stdout output. cannot be null.
     * @param waitforReaders if true, this will wait for the reader threads.
     * @return the process return code.
     * @throws InterruptedException
     */
    private int grabProcessOutput(final Process process, final ArrayList<String> errorOutput,
            final ArrayList<String> stdOutput, boolean waitforReaders)
            throws InterruptedException {
        assert errorOutput != null;
        assert stdOutput != null;
        // read the lines as they come. if null is returned, it's
        // because the process finished
        Thread t1 = new Thread("") { //$NON-NLS-1$
            @Override
            public void run() {
                // create a buffer to read the stderr output
                InputStreamReader is = new InputStreamReader(process.getErrorStream());
                BufferedReader errReader = new BufferedReader(is);

                try {
                    while (true) {
                        String line = errReader.readLine();
                        if (line != null) {
                            display("ADB Error: %1$s", line);
                            errorOutput.add(line);
                        } else {
                            break;
                        }
                    }
                } catch (IOException e) {
                    // do nothing.
                }
            }
        };

        Thread t2 = new Thread("") { //$NON-NLS-1$
            @Override
            public void run() {
                InputStreamReader is = new InputStreamReader(process.getInputStream());
                BufferedReader outReader = new BufferedReader(is);

                try {
                    while (true) {
                        String line = outReader.readLine();
                        if (line != null) {
                            display("ADB: %1$s", line);
                            stdOutput.add(line);
                        } else {
                            break;
                        }
                    }
                } catch (IOException e) {
                    // do nothing.
                }
            }
        };

        t1.start();
        t2.start();

        // it looks like on windows process#waitFor() can return
        // before the thread have filled the arrays, so we wait for both threads and the
        // process itself.
        if (waitforReaders) {
            try {
                t1.join();
            } catch (InterruptedException e) {
            }
            try {
                t2.join();
            } catch (InterruptedException e) {
            }
        }

        // get the return code from the process
        return process.waitFor();
    }

}
