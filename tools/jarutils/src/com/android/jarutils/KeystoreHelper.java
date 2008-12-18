/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.jarutils;

import com.android.jarutils.DebugKeyProvider.IKeyGenOutput;
import com.android.jarutils.DebugKeyProvider.KeytoolException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.ArrayList;

/**
 * A Helper to create new keystore/key.
 */
public final class KeystoreHelper {

    /**
     * Creates a new store
     * @param osKeyStorePath the location of the store
     * @param storeType an optional keystore type, or <code>null</code> if the default is to
     * be used.
     * @param output an optional {@link IKeyGenOutput} object to get the stdout and stderr
     * of the keytool process call.
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws UnrecoverableEntryException
     * @throws IOException
     * @throws KeytoolException
     */
    public static boolean createNewStore(
            String osKeyStorePath,
            String storeType,
            String storePassword,
            String alias,
            String keyPassword,
            String description,
            int validityYears,
            IKeyGenOutput output)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException,
            UnrecoverableEntryException, IOException, KeytoolException {
        
        // get the executable name of keytool depending on the platform.
        String os = System.getProperty("os.name");

        String keytoolCommand;
        if (os.startsWith("Windows")) {
            keytoolCommand = "keytool.exe";
        } else {
            keytoolCommand = "keytool";
        }

        String javaHome = System.getProperty("java.home");

        if (javaHome != null && javaHome.length() > 0) {
            keytoolCommand = javaHome + File.separator + "bin" + File.separator + keytoolCommand; 
        }
        
        // create the command line to call key tool to build the key with no user input.
        ArrayList<String> commandList = new ArrayList<String>();
        commandList.add(keytoolCommand);
        commandList.add("-genkey");
        commandList.add("-alias");
        commandList.add(alias);
        commandList.add("-keyalg");
        commandList.add("RSA");
        commandList.add("-dname");
        commandList.add(description);
        commandList.add("-validity");
        commandList.add(Integer.toString(validityYears * 365));
        commandList.add("-keypass");
        commandList.add(keyPassword);
        commandList.add("-keystore");
        commandList.add(osKeyStorePath);
        commandList.add("-storepass");
        commandList.add(storePassword);
        if (storeType != null) {
            commandList.add("-storetype");
            commandList.add(storeType);
        }

        String[] commandArray = commandList.toArray(new String[commandList.size()]);

        // launch the command line process
        int result = 0;
        try {
            result = grabProcessOutput(Runtime.getRuntime().exec(commandArray), output);
        } catch (Exception e) {
            // create the command line as one string
            StringBuilder builder = new StringBuilder();
            boolean firstArg = true;
            for (String arg : commandArray) {
                boolean hasSpace = arg.indexOf(' ') != -1;
                
                if (firstArg == true) {
                    firstArg = false;
                } else {
                    builder.append(' ');
                }
                
                if (hasSpace) {
                    builder.append('"');
                }
                
                builder.append(arg);

                if (hasSpace) {
                    builder.append('"');
                }
            }
            
            throw new KeytoolException("Failed to create key: " + e.getMessage(),
                    javaHome, builder.toString());
        }
        
        if (result != 0) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Get the stderr/stdout outputs of a process and return when the process is done.
     * Both <b>must</b> be read or the process will block on windows.
     * @param process The process to get the ouput from
     * @return the process return code.
     * @throws InterruptedException
     */
    private static int grabProcessOutput(final Process process, final IKeyGenOutput output) {
        // read the lines as they come. if null is returned, it's
        // because the process finished
        Thread t1 = new Thread("") {
            @Override
            public void run() {
                // create a buffer to read the stderr output
                InputStreamReader is = new InputStreamReader(process.getErrorStream());
                BufferedReader errReader = new BufferedReader(is);

                try {
                    while (true) {
                        String line = errReader.readLine();
                        if (line != null) {
                            if (output != null) {
                                output.err(line);
                            } else {
                                System.err.println(line);
                            }
                        } else {
                            break;
                        }
                    }
                } catch (IOException e) {
                    // do nothing.
                }
            }
        };

        Thread t2 = new Thread("") {
            @Override
            public void run() {
                InputStreamReader is = new InputStreamReader(process.getInputStream());
                BufferedReader outReader = new BufferedReader(is);

                try {
                    while (true) {
                        String line = outReader.readLine();
                        if (line != null) {
                            if (output != null) {
                                output.out(line);
                            } else {
                                System.out.println(line);
                            }
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
        try {
            t1.join();
        } catch (InterruptedException e) {
        }
        try {
            t2.join();
        } catch (InterruptedException e) {
        }

        // get the return code from the process
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            // since we're waiting for the output thread above, we should never actually wait
            // on the process to end, since it'll be done by the time we call waitFor()
            return 0;
        }
    }
}
