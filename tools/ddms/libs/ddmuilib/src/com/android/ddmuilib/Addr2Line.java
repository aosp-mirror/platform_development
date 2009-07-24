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

package com.android.ddmuilib;

import com.android.ddmlib.*;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;

/**
 * Represents an addr2line process to get filename/method information from a
 * memory address.<br>
 * Each process can only handle one library, which should be provided when
 * creating a new process.<br>
 * <br>
 * The processes take some time to load as they need to parse the library files.
 * For this reason, processes cannot be manually started. Instead the class
 * keeps an internal list of processes and one asks for a process for a specific
 * library, using <code>getProcess(String library)<code>.<br></br>
 * Internally, the processes are started in pipe mode to be able to query them
 * with multiple addresses.
 */
public class Addr2Line {

    /**
     * Loaded processes list. This is also used as a locking object for any
     * methods dealing with starting/stopping/creating processes/querying for
     * method.
     */
    private static final HashMap<String, Addr2Line> sProcessCache =
            new HashMap<String, Addr2Line>();

    /**
     * byte array representing a carriage return. Used to push addresses in the
     * process pipes.
     */
    private static final byte[] sCrLf = {
        '\n'
    };

    /** Path to the library */
    private String mLibrary;

    /** the command line process */
    private Process mProcess;

    /** buffer to read the result of the command line process from */
    private BufferedReader mResultReader;

    /**
     * output stream to provide new addresses to decode to the command line
     * process
     */
    private BufferedOutputStream mAddressWriter;

    /**
     * Returns the instance of a Addr2Line process for the specified library.
     * <br>The library should be in a format that makes<br>
     * <code>$ANDROID_PRODUCT_OUT + "/symbols" + library</code> a valid file.
     *
     * @param library the library in which to look for addresses.
     * @return a new Addr2Line object representing a started process, ready to
     *         be queried for addresses. If any error happened when launching a
     *         new process, <code>null</code> will be returned.
     */
    public static Addr2Line getProcess(final String library) {
        // synchronize around the hashmap object
        if (library != null) {
            synchronized (sProcessCache) {
                // look for an existing process
                Addr2Line process = sProcessCache.get(library);

                // if we don't find one, we create it
                if (process == null) {
                    process = new Addr2Line(library);

                    // then we start it
                    boolean status = process.start();

                    if (status) {
                        // if starting the process worked, then we add it to the
                        // list.
                        sProcessCache.put(library, process);
                    } else {
                        // otherwise we just drop the object, to return null
                        process = null;
                    }
                }
                // return the process
                return process;
            }
        }
        return null;
    }

    /**
     * Construct the object with a library name.
     * <br>The library should be in a format that makes<br>
     * <code>$ANDROID_PRODUCT_OUT + "/symbols" + library</code> a valid file.
     *
     * @param library the library in which to look for address.
     */
    private Addr2Line(final String library) {
        mLibrary = library;
    }

    /**
     * Starts the command line process.
     *
     * @return true if the process was started, false if it failed to start, or
     *         if there was any other errors.
     */
    private boolean start() {
        // because this is only called from getProcess() we know we don't need
        // to synchronize this code.

        // get the output directory.
        String symbols = System.getenv("ANDROID_SYMBOLS");
        if (symbols == null) {
            symbols = DdmUiPreferences.getSymbolDirectory();
        }

        // build the command line
        String[] command = new String[5];
        command[0] = DdmUiPreferences.getAddr2Line();
        command[1] = "-C";
        command[2] = "-f";
        command[3] = "-e";
        command[4] = symbols + mLibrary.replaceAll("libc\\.so", "libc_debug\\.so");

        try {
            // attempt to start the process
            mProcess = Runtime.getRuntime().exec(command);

            if (mProcess != null) {
                // get the result reader
                InputStreamReader is = new InputStreamReader(mProcess
                        .getInputStream());
                mResultReader = new BufferedReader(is);

                // get the outstream to write the addresses
                mAddressWriter = new BufferedOutputStream(mProcess
                        .getOutputStream());

                // check our streams are here
                if (mResultReader == null || mAddressWriter == null) {
                    // not here? stop the process and return false;
                    mProcess.destroy();
                    mProcess = null;
                    return false;
                }

                // return a success
                return true;
            }

        } catch (IOException e) {
            // log the error
            String msg = String.format(
                    "Error while trying to start %1$s process for library %2$s",
                    DdmUiPreferences.getAddr2Line(), mLibrary);
            Log.e("ddm-Addr2Line", msg);

            // drop the process just in case
            if (mProcess != null) {
                mProcess.destroy();
                mProcess = null;
            }
        }

        // we can be here either cause the allocation of mProcess failed, or we
        // caught an exception
        return false;
    }

    /**
     * Stops the command line process.
     */
    public void stop() {
        synchronized (sProcessCache) {
            if (mProcess != null) {
                // remove the process from the list
                sProcessCache.remove(mLibrary);

                // then stops the process
                mProcess.destroy();

                // set the reference to null.
                // this allows to make sure another thread calling getAddress()
                // will not query a stopped thread
                mProcess = null;
            }
        }
    }

    /**
     * Stops all current running processes.
     */
    public static void stopAll() {
        // because of concurrent access (and our use of HashMap.values()), we
        // can't rely on the synchronized inside stop(). We need to put one
        // around the whole loop.
        synchronized (sProcessCache) {
            // just a basic loop on all the values in the hashmap and call to
            // stop();
            Collection<Addr2Line> col = sProcessCache.values();
            for (Addr2Line a2l : col) {
                a2l.stop();
            }
        }
    }

    /**
     * Looks up an address and returns method name, source file name, and line
     * number.
     *
     * @param addr the address to look up
     * @return a BacktraceInfo object containing the method/filename/linenumber
     *         or null if the process we stopped before the query could be
     *         processed, or if an IO exception happened.
     */
    public NativeStackCallInfo getAddress(long addr) {
        // even though we don't access the hashmap object, we need to
        // synchronized on it to prevent
        // another thread from stopping the process we're going to query.
        synchronized (sProcessCache) {
            // check the process is still alive/allocated
            if (mProcess != null) {
                // prepare to the write the address to the output buffer.

                // first, conversion to a string containing the hex value.
                String tmp = Long.toString(addr, 16);

                try {
                    // write the address to the buffer
                    mAddressWriter.write(tmp.getBytes());

                    // add CR-LF
                    mAddressWriter.write(sCrLf);

                    // flush it all.
                    mAddressWriter.flush();

                    // read the result. We need to read 2 lines
                    String method = mResultReader.readLine();
                    String source = mResultReader.readLine();

                    // make the backtrace object and return it
                    if (method != null && source != null) {
                        return new NativeStackCallInfo(mLibrary, method, source);
                    }
                } catch (IOException e) {
                    // log the error
                    Log.e("ddms",
                            "Error while trying to get information for addr: "
                                    + tmp + " in library: " + mLibrary);
                    // we'll return null later
                }
            }
        }
        return null;
    }
}
