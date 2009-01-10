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
import com.android.sdklib.project.ProjectCreator;
import com.android.sdklib.project.ProjectCreator.OutputLevel;
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
 * Main class for the 'android' application.
 */
class Main {
    
    private final static String TOOLSDIR = "com.android.sdkmanager.toolsdir";
    
    private final static String[] BOOLEAN_YES_REPLIES = new String[] { "yes", "y" };
    private final static String[] BOOLEAN_NO_REPLIES = new String[] { "no", "n" };

    private String mSdkFolder;
    private ISdkLog mSdkLog;
    private SdkManager mSdkManager;
    private VmManager mVmManager;
    private SdkCommandLine mSdkCommandLine;

    public static void main(String[] args) {
        new Main().run(args);
    }
    
    /**
     * Runs the sdk manager app
     * @param args
     */
    private void run(String[] args) {
        init();
        mSdkCommandLine.parseArgs(args);
        parseSdk();
        doAction();
    }

    /**
     * Init the application by making sure the SDK path is available and
     * doing basic parsing of the SDK.
     */
    private void init() {
        mSdkCommandLine = new SdkCommandLine();

        /* We get passed a property for the tools dir */
        String toolsDirProp = System.getProperty(TOOLSDIR);
        if (toolsDirProp == null) {
            // for debugging, it's easier to override using the process environment
            toolsDirProp = System.getenv(TOOLSDIR);
        }
    
        if (toolsDirProp != null) {
            // got back a level for the SDK folder
            File tools;
            if (toolsDirProp.length() > 0) {
                tools = new File(toolsDirProp);
                mSdkFolder = tools.getParent();
            } else {
                try {
                    tools = new File(".").getCanonicalFile();
                    mSdkFolder = tools.getParent();
                } catch (IOException e) {
                    // Will print an error below since mSdkFolder is not defined
                }
            }
        }

        if (mSdkFolder == null) {
            String os = System.getProperty("os.name");
            String cmd = "android";
            if (os.startsWith("Windows")) {
                cmd += ".bat";
            }

            mSdkCommandLine.printHelpAndExit(
                "ERROR: The tools directory property is not set, please make sure you are executing %1$s",
                cmd);
        }
    }

    /**
     * Does the basic SDK parsing required for all actions
     */
    private void parseSdk() {
        mSdkLog = new ISdkLog() {
            public void error(Throwable t, String errorFormat, Object... args) {
                if (errorFormat != null) {
                    System.err.printf("Error: " + errorFormat, args);
                    System.err.println("");
                }
                if (t != null) {
                    System.err.print("Error: " + t.getMessage());
                }
            }

            public void warning(String warningFormat, Object... args) {
                if (false) {
                    // TODO: on display warnings in verbose mode.
                    System.out.printf("Warning: " + warningFormat, args);
                    System.out.println("");
                }
            }

            public void printf(String msgFormat, Object... args) {
                System.out.printf(msgFormat, args);
            }
        };
        mSdkManager = SdkManager.createManager(mSdkFolder, mSdkLog);
        
        if (mSdkManager == null) {
            mSdkCommandLine.printHelpAndExit("ERROR: Unable to parse SDK content.");
        }
    }
    
    /**
     * Actually do an action...
     */
    private void doAction() {
        String action = mSdkCommandLine.getActionRequested();
        
        if (SdkCommandLine.ACTION_LIST.equals(action)) {
            // list action.
            if (SdkCommandLine.ARG_TARGET.equals(mSdkCommandLine.getListFilter())) {
                displayTargetList();
            } else if (SdkCommandLine.ARG_VM.equals(mSdkCommandLine.getListFilter())) {
                displayVmList();
            } else {
                displayTargetList();
                displayVmList();
            }
        } else if (SdkCommandLine.ACTION_NEW_VM.equals(action)) {
            createVm();
        } else if (SdkCommandLine.ACTION_NEW_PROJECT.equals(action)) {
            // get the target and try to resolve it.
            int targetId = mSdkCommandLine.getNewProjectTargetId();
            IAndroidTarget[] targets = mSdkManager.getTargets();
            if (targetId < 1 || targetId > targets.length) {
                mSdkCommandLine.printHelpAndExit("ERROR: Wrong target id.");
            }
            IAndroidTarget target = targets[targetId - 1];
            
            ProjectCreator creator = new ProjectCreator(mSdkFolder,
                    OutputLevel.NORMAL, mSdkLog);
            
            creator.createProject(mSdkCommandLine.getNewProjectLocation(),
                    mSdkCommandLine.getNewProjectName(), mSdkCommandLine.getNewProjectPackage(),
                    mSdkCommandLine.getNewProjectActivity(), target, true);
        } else {
            mSdkCommandLine.printHelpAndExit(null);
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
            mSdkCommandLine.printHelpAndExit(e.getMessage());
        }
    }
    
    /**
     * Creates a new VM. This is a text based creation with command line prompt.
     */
    private void createVm() {
        // find a matching target
        int targetId = mSdkCommandLine.getNewVmTargetId();
        IAndroidTarget target = null;
        
        if (targetId >= 1 && targetId <= mSdkManager.getTargets().length) {
            target = mSdkManager.getTargets()[targetId-1]; // target it is 1-based
        } else {
            mSdkCommandLine.printHelpAndExit(
                    "ERROR: Target Id is not a valid Id. Check 'android list target' for the list of targets.");
        }
        
        try {
            // default to standard path now
            String vmRoot = AndroidLocation.getFolder() + AndroidLocation.FOLDER_VMS;
            
            Map<String, String> hardwareConfig = null;
            if (target.isPlatform()) {
                try {
                    hardwareConfig = promptForHardware(target);
                } catch (IOException e) {
                    mSdkCommandLine.printHelpAndExit(e.getMessage());
                }
            }
            
            VmManager.createVm(vmRoot,
                    mSdkCommandLine.getNewVmName(),
                    target,
                    null /*skinName*/,
                    null /*sdcardPath*/,
                    0 /*sdcardSize*/,
                    hardwareConfig,
                    null /* sdklog */);
        } catch (AndroidLocationException e) {
            mSdkCommandLine.printHelpAndExit(e.getMessage());
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
        
        result = readLine(readLineBuffer).trim();
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
                        Integer.parseInt(result);
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
     * Reads the line from the input stream.
     * @param buffer
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

        // ignore end whitespace
        while (count > 0 && (buffer[count-1] == '\r' || buffer[count-1] == '\n')) {
            count--;
        }
        
        return new String(buffer, 0, count);
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
}