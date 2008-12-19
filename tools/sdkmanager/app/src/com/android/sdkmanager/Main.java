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

package com.android.sdkmanager;

import com.android.prefs.AndroidLocation;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.ISdkLog;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.SdkManager;
import com.android.sdklib.IAndroidTarget.IOptionalLibrary;
import com.android.sdklib.vm.HardwareProperties;
import com.android.sdklib.vm.VmManager;
import com.android.sdklib.vm.HardwareProperties.HardwareProperty;
import com.android.sdklib.vm.VmManager.VmInfo;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main class for the 'android' application
 *
 */
class Main {
    
    private final static String TOOLSDIR = "com.android.sdkmanager.toolsdir";
    
    private final static String ARG_LIST_TARGET = "target";
    private final static String ARG_LIST_VM = "vm";
    
    private final static String[] BOOLEAN_YES_REPLIES = new String[] { "yes", "y" };
    private final static String[] BOOLEAN_NO_REPLIES = new String[] { "no", "n" };

    private String mSdkFolder;
    private SdkManager mSdkManager;
    private VmManager mVmManager;

    /* --list parameters */
    private String mListObject;

    /* --create parameters */
    private boolean mCreateVm;
    private int mCreateTargetId;
    private IAndroidTarget mCreateTarget;
    private String mCreateName;

    public static void main(String[] args) {
        new Main().run(args);
    }
    
    /**
     * Runs the sdk manager app
     * @param args
     */
    private void run(String[] args) {
        init();
        parseArgs(args);
        parseSdk();
        doAction();
    }

    /**
     * Init the application by making sure the SDK path is available and
     * doing basic parsing of the SDK.
     */
    private void init() {
        /* We get passed a property for the tools dir */
        String toolsDirProp = System.getProperty(TOOLSDIR);
        if (toolsDirProp == null) {
            // for debugging, it's easier to override using the process environment
            toolsDirProp = System.getenv(TOOLSDIR);
        }
        if (toolsDirProp == null) {
            printHelpAndExit("ERROR: The tools directory property is not set, please make sure you are executing android or android.bat");
        }
        
        // got back a level for the SDK folder
        File tools = new File(toolsDirProp);
        mSdkFolder = tools.getParent();
        
    }
    
    /**
     * Parses command-line arguments, or prints help/usage and exits if error.
     * @param args arguments passed to the program
     */
    private void parseArgs(String[] args) {
        final int numArgs = args.length;

        try {
            int argPos = 0;
            for (; argPos < numArgs; argPos++) {
                final String arg = args[argPos];
                if (arg.equals("-l") || arg.equals("--list")) {
                    mListObject = args[++argPos];
                } else if (arg.equals("-c") || arg.equals("--create")) {
                    mCreateVm = true;
                    parseCreateArgs(args, ++argPos);
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            /* Any OOB triggers help */
            printHelpAndExit("ERROR: Not enough arguments.");
        }
    }

    private void parseCreateArgs(String[] args, int argPos) {
        final int numArgs = args.length;

        try {
            for (; argPos < numArgs; argPos++) {
                final String arg = args[argPos];
                if (arg.equals("-t") || arg.equals("--target")) {
                    String targetId = args[++argPos];
                    try {
                        // get the target id
                        mCreateTargetId = Integer.parseInt(targetId);
                    } catch (NumberFormatException e) {
                        printHelpAndExit("ERROR: Target Id is not a number");
                    }
                } else if (arg.equals("-n") || arg.equals("--name")) {
                    mCreateName = args[++argPos];
                } else {
                    printHelpAndExit("ERROR: '%s' unknown argument for --create mode",
                            args[argPos]);
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            /* Any OOB triggers help */
            printHelpAndExit("ERROR: Not enough arguments for --create");
        }
    }

    /**
     * Does the basic SDK parsing required for all actions
     */
    private void parseSdk() {
        mSdkManager = SdkManager.createManager(mSdkFolder, new ISdkLog() {
            public void error(String errorFormat, Object... args) {
                System.err.printf("Error: " + errorFormat, args);
                System.err.println("");
            }

            public void warning(String warningFormat, Object... args) {
                if (false) {
                    // TODO: on display warnings in verbose mode.
                    System.out.printf("Warning: " + warningFormat, args);
                    System.out.println("");
                }
            }
        });
        
        if (mSdkManager == null) {
            printHelpAndExit("ERROR: Unable to parse SDK content.");
        }
    }
    
    /**
     * Actually do an action...
     */
    private void doAction() {
        if (mListObject != null) {
            // list action.
            if (ARG_LIST_TARGET.equals(mListObject)) {
                displayTargetList();
            } else if (ARG_LIST_VM.equals(mListObject)) {
                displayVmList();
            } else {
                printHelpAndExit("'%s' is not a valid --list option", mListObject);
            }
        } else if (mCreateVm) {
            createVm();
        } else {
            printHelpAndExit(null);
        }
    }

    /**
     * Displays the list of available Targets (Platforms and Add-ons)
     */
    private void displayTargetList() {
        System.out.println("Available Android targets:");

        int index = 1;
        for (IAndroidTarget target : mSdkManager.getTargets()) {
            if (target.isPlatform()) {
                System.out.printf("[%d] %s\n", index, target.getName());
                System.out.printf("     API level: %d\n", target.getApiVersionNumber());
            } else {
                System.out.printf("[%d] Add-on: %s\n", index, target.getName());
                System.out.printf("     Vendor: %s\n", target.getVendor());
                if (target.getDescription() != null) {
                    System.out.printf("     Description: %s\n", target.getDescription());
                }
                System.out.printf("     Based on Android %s (API level %d)\n",
                        target.getApiVersionName(), target.getApiVersionNumber());
                
                // display the optional libraries.
                IOptionalLibrary[] libraries = target.getOptionalLibraries();
                if (libraries != null) {
                    for (IOptionalLibrary library : libraries) {
                        System.out.printf("     Library: %s (%s)\n", library.getName(),
                                library.getJarName());
                    }
                }
            }

            // get the target skins
            String[] skins = target.getSkins();
            System.out.print("     Skins: ");
            if (skins != null) {
                boolean first = true;
                for (String skin : skins) {
                    if (first == false) {
                        System.out.print(", ");
                    } else {
                        first = false;
                    }
                    System.out.print(skin);
                }
                System.out.println("");
            } else {
                System.out.println("no skins.");
            }
            
            index++;
        }
    }
    
    /**
     * Displays the list of available VMs.
     */
    private void displayVmList() {
        try {
            mVmManager = new VmManager(mSdkManager, null /* sdklog */);

            System.out.println("Available Android VMs:");

            int index = 1;
            for (VmInfo info : mVmManager.getVms()) {
                System.out.printf("[%d] %s\n", index, info.getName());
                System.out.printf("    Path: %s\n", info.getPath());

                // get the target of the Vm
                IAndroidTarget target = info.getTarget();
                if (target.isPlatform()) {
                    System.out.printf("    Target: %s (API level %d)\n", target.getName(),
                            target.getApiVersionNumber());
                } else {
                    System.out.printf("    Target: %s (%s)\n", target.getName(), target
                            .getVendor());
                    System.out.printf("    Based on Android %s (API level %d)\n", target
                            .getApiVersionName(), target.getApiVersionNumber());

                }

                index++;
            }
        } catch (AndroidLocationException e) {
            printHelpAndExit(e.getMessage());
        }
    }
    
    /**
     * Creates a new VM. This is a text based creation with command line prompt.
     */
    private void createVm() {
        // find a matching target
        if (mCreateTargetId >= 1 && mCreateTargetId <= mSdkManager.getTargets().length) {
            mCreateTarget = mSdkManager.getTargets()[mCreateTargetId-1]; // target it is 1-based
        } else {
            printHelpAndExit(
                    "ERROR: Target Id is not a valid Id. Check android --list target for the list of targets.");
        }
        
        try {
            // default to standard path now
            String vmRoot = AndroidLocation.getFolder() + AndroidLocation.FOLDER_VMS;
            
            Map<String, String> hardwareConfig = null;
            if (mCreateTarget.isPlatform()) {
                try {
                    hardwareConfig = promptForHardware(mCreateTarget);
                } catch (IOException e) {
                    printHelpAndExit(e.getMessage());
                }
            }
            
            VmManager.createVm(vmRoot, mCreateName, mCreateTarget, null /*skinName*/,
                    null /*sdcardPath*/, 0 /*sdcardSize*/, hardwareConfig,
                    null /* sdklog */);
        } catch (AndroidLocationException e) {
            printHelpAndExit(e.getMessage());
        }
    }

    /**
     * Prompts the user to setup a hardware config for a Platform-based VM.
     * @throws IOException 
     */
    private Map<String, String> promptForHardware(IAndroidTarget createTarget) throws IOException {
        byte[] readLineBuffer = new byte[256];
        String result;
        String defaultAnswer = "no";
        
        System.out.print(String.format("%s is a basic Android platform.\n",
                createTarget.getName()));
        System.out.print(String.format("Do you which to create a custom hardware profile [%s]",
                defaultAnswer));
        
        result = readLine(readLineBuffer);
        // handle default:
        if (result.length() == 0) {
            result = defaultAnswer;
        }

        if (getBooleanReply(result) == false) {
            // no custom config.
            return null;
        }
        
        System.out.println(""); // empty line
        
        // get the list of possible hardware properties
        File hardwareDefs = new File (mSdkFolder + File.separator +
                SdkConstants.OS_SDK_TOOLS_LIB_FOLDER, SdkConstants.FN_HARDWARE_INI);
        List<HardwareProperty> list = HardwareProperties.parseHardwareDefinitions(hardwareDefs,
                null /*sdkLog*/);
        
        HashMap<String, String> map = new HashMap<String, String>();
        
        for (int i = 0 ; i < list.size() ;) {
            HardwareProperty property = list.get(i);

            String description = property.getDescription();
            if (description != null) {
                System.out.printf("%s: %s\n", property.getAbstract(), description);
            } else {
                System.out.println(property.getAbstract());
            }

            String defaultValue = property.getDefault();
            
            if (defaultValue != null) {
                System.out.printf("%s [%s]:", property.getName(), defaultValue);
            } else {
                System.out.printf("%s (%s):", property.getName(), property.getType());
            }
            
            result = readLine(readLineBuffer);
            if (result.length() == 0) {
                if (defaultValue != null) {
                    System.out.println(""); // empty line
                    i++; // go to the next property if we have a valid default value.
                         // if there's no default, we'll redo this property
                }
                continue;
            }
            
            switch (property.getType()) {
                case BOOLEAN:
                    try {
                        if (getBooleanReply(result)) {
                            map.put(property.getName(), "yes");
                            i++; // valid reply, move to next property
                        } else {
                            map.put(property.getName(), "no");
                            i++; // valid reply, move to next property
                        }
                    } catch (IOException e) {
                        // display error, and do not increment i to redo this property
                        System.out.println("\n" + e.getMessage());
                    }
                    break;
                case INTEGER:
                    try {
                        @SuppressWarnings("unused")
                        int value = Integer.parseInt(result);
                        map.put(property.getName(), result);
                        i++; // valid reply, move to next property
                    } catch (NumberFormatException e) {
                        // display error, and do not increment i to redo this property
                        System.out.println("\n" + e.getMessage());
                    }
                    break;
                case DISKSIZE:
                    // TODO check validity
                    map.put(property.getName(), result);
                    i++; // valid reply, move to next property
                    break;
            }
            
            System.out.println(""); // empty line
        }

        return map;
    }
    
    /**
     * Read the line from the input stream.
     * @param buffer
     * @return
     * @throws IOException
     */
    private String readLine(byte[] buffer) throws IOException {
        int count = System.in.read(buffer);
        
        // is the input longer than the buffer?
        if (count == buffer.length && buffer[count-1] != 10) {
            // create a new temp buffer
            byte[] tempBuffer = new byte[256];

            // and read the rest
            String secondHalf = readLine(tempBuffer);
            
            // return a concat of both
            return new String(buffer, 0, count) + secondHalf;
        }

        return new String(buffer, 0, count - 1); // -1 to not include the carriage return
    }
    
    /**
     * Returns the boolean value represented by the string.
     * @throws IOException If the value is not a boolean string.
     */
    private boolean getBooleanReply(String reply) throws IOException {
        for (String valid : BOOLEAN_YES_REPLIES) {
            if (valid.equalsIgnoreCase(reply)) {
                return true;
            }
        }

        for (String valid : BOOLEAN_NO_REPLIES) {
            if (valid.equalsIgnoreCase(reply)) {
                return false;
            }
        }

        throw new IOException(String.format("%s is not a valid reply", reply));
    }

    /**
     * Prints the help/usage and exits.
     * @param errorFormat Optional error message to print prior to usage using String.format 
     * @param args Arguments for String.format
     */
    private void printHelpAndExit(String errorFormat, Object... args) {
        if (errorFormat != null) {
            System.err.println(String.format(errorFormat, args));
        }
        
        /*
         * usage should fit in 80 columns
         *   12345678901234567890123456789012345678901234567890123456789012345678901234567890
         */
        final String usage = "\n" +
            "Usage:\n" +
            "  android --list [target|vm]\n" +
            "  android --create --target <target id> --name <name>\n" +
            "\n" +
            "Options:\n" +
            " -l [target|vm], --list [target|vm]\n" +
            "         Outputs the available targets or Virtual Machines and their Ids.\n" +
            "\n";
        
        System.out.println(usage);
        System.exit(1);
    }
}