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

import com.android.prefs.AndroidLocation;
import com.android.prefs.AndroidLocation.AndroidLocationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;

/**
 * A provider of a dummy key to sign Android application for debugging purpose.
 * <p/>This provider uses a custom keystore to create and store a key with a known password.
 */
public class DebugKeyProvider {
    
    public interface IKeyGenOutput {
        public void out(String message);
        public void err(String message);
    }
    
    private static final String PASSWORD_STRING = "android";
    private static final char[] PASSWORD_CHAR = PASSWORD_STRING.toCharArray();
    private static final String DEBUG_ALIAS = "AndroidDebugKey";
    
    // Certificate CN value. This is a hard-coded value for the debug key.
    // Android Market checks against this value in order to refuse applications signed with
    // debug keys.
    private static final String CERTIFICATE_DESC = "CN=Android Debug,O=Android,C=US";
    
    private KeyStore.PrivateKeyEntry mEntry;
    
    public static class KeytoolException extends Exception {
        /** default serial uid */
        private static final long serialVersionUID = 1L;
        private String mJavaHome = null;
        private String mCommandLine = null;
        
        KeytoolException(String message) {
            super(message);
        }

        KeytoolException(String message, String javaHome, String commandLine) {
            super(message);
            
            mJavaHome = javaHome;
            mCommandLine = commandLine;
        }
        
        public String getJavaHome() {
            return mJavaHome;
        }
        
        public String getCommandLine() {
            return mCommandLine;
        }
    }
    
    /**
     * Creates a provider using a keystore at the given location.
     * <p/>The keystore, and a new random android debug key are created if they do not yet exist.
     * <p/>Password for the store/key is <code>android</code>, and the key alias is
     * <code>AndroidDebugKey</code>.
     * @param osKeyStorePath the OS path to the keystore.
     * @param storeType an optional keystore type, or <code>null</code> if the default is to
     * be used.
     * @param output an optional {@link IKeyGenOutput} object to get the stdout and stderr
     * of the keytool process call.
     * @throws KeytoolException If the creation of the debug key failed.
     */
    public DebugKeyProvider(String osKeyStorePath, String storeType, IKeyGenOutput output)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException,
            UnrecoverableEntryException, IOException, KeytoolException {
        
        if (loadKeyEntry(osKeyStorePath, storeType) == false) {
            // create the store with they key
            createNewStore(osKeyStorePath, storeType, output);
        }
    }

    /**
     * Creates a provider using the default keystore location.
     * <p/>The keystore, and a new random android debug key are created if they do not yet exist.
     * <p/>Password for the store/key is <code>android</code>, and the key alias is
     * <code>AndroidDebugKey</code>.
     * @param storeType an optional keystore type, or <code>null</code> if the default is to
     * be used.
     * @param output an optional {@link IKeyGenOutput} object to get the stdout and stderr
     * of the keytool process call.
     * @throws KeytoolException If the creation of the debug key failed.
     * @throws AndroidLocationException If getting the location to store android files failed.
     */
    public DebugKeyProvider(String storeType, IKeyGenOutput output) throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, UnrecoverableEntryException,
            IOException, KeytoolException, AndroidLocationException {

        String osKeyStorePath = getDefaultKeyStoreOsPath();
        if (loadKeyEntry(osKeyStorePath, storeType) == false) {
            // create the store with the key
            createNewStore(osKeyStorePath, storeType, output);
        }
    }
    
    /**
     * Returns the OS path to the default debug keystore.
     * 
     * @return The OS path to the default debug keystore.
     * @throws KeytoolException
     * @throws AndroidLocationException
     */
    public static String getDefaultKeyStoreOsPath()
            throws KeytoolException, AndroidLocationException {
        String folder = AndroidLocation.getFolder();
        if (folder == null) {
            throw new KeytoolException("Failed to get HOME directory!\n");
        }
        String osKeyStorePath = folder + "debug.keystore";

        return osKeyStorePath;
    }

    /**
     * Returns the debug {@link PrivateKey} to use to sign applications for debug purpose.
     * @return the private key or <code>null</code> if its creation failed.
     */
    public PrivateKey getDebugKey() throws KeyStoreException, NoSuchAlgorithmException,
            UnrecoverableKeyException, UnrecoverableEntryException {
        if (mEntry != null) {
            return mEntry.getPrivateKey();
        }
        
        return null;
    }

    /**
     * Returns the debug {@link Certificate} to use to sign applications for debug purpose.
     * @return the certificate or <code>null</code> if its creation failed.
     */
    public Certificate getCertificate() throws KeyStoreException, NoSuchAlgorithmException,
            UnrecoverableKeyException, UnrecoverableEntryException {
        if (mEntry != null) {
            return mEntry.getCertificate();
        }

        return null;
    }
    
    /**
     * Loads the debug key from the keystore.
     * @param osKeyStorePath the OS path to the keystore.
     * @param storeType an optional keystore type, or <code>null</code> if the default is to
     * be used.
     * @return <code>true</code> if success, <code>false</code> if the keystore does not exist.
     */
    private boolean loadKeyEntry(String osKeyStorePath, String storeType) throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException,
            UnrecoverableEntryException {
        try {
            KeyStore keyStore = KeyStore.getInstance(
                    storeType != null ? storeType : KeyStore.getDefaultType());
            FileInputStream fis = new FileInputStream(osKeyStorePath);
            keyStore.load(fis, PASSWORD_CHAR);
            fis.close();
            mEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(
                    DEBUG_ALIAS, new KeyStore.PasswordProtection(PASSWORD_CHAR));
        } catch (FileNotFoundException e) {
            return false;
        }
        
        return true;
    }

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
    private void createNewStore(String osKeyStorePath, String storeType, IKeyGenOutput output)
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
        commandList.add(DEBUG_ALIAS);
        commandList.add("-keyalg");
        commandList.add("RSA");
        commandList.add("-dname");
        commandList.add(CERTIFICATE_DESC);
        commandList.add("-validity");
        commandList.add("365");
        commandList.add("-keypass");
        commandList.add(PASSWORD_STRING);
        commandList.add("-keystore");
        commandList.add(osKeyStorePath);
        commandList.add("-storepass");
        commandList.add(PASSWORD_STRING);
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
            
            throw new KeytoolException("Failed to create debug key: " + e.getMessage(),
                    javaHome, builder.toString());
        }
        
        if (result != 0) {
            return;
        }
        loadKeyEntry(osKeyStorePath, storeType);
    }
    
    /**
     * Get the stderr/stdout outputs of a process and return when the process is done.
     * Both <b>must</b> be read or the process will block on windows.
     * @param process The process to get the ouput from
     * @return the process return code.
     * @throws InterruptedException
     */
    private int grabProcessOutput(final Process process, final IKeyGenOutput output) {
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
