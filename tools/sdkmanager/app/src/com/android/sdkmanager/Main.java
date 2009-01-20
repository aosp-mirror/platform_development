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

    /** Java property that defines the location of the sdk/tools directory. */
    private final static String TOOLSDIR = "com.android.sdkmanager.toolsdir";
    /** Java property that defines the working directory. On Windows the current working directory
     *  is actually the tools dir, in which case this is used to get the original CWD. */
    private final static String WORKDIR = "com.android.sdkmanager.workdir";
    
    private final static String[] BOOLEAN_YES_REPLIES = new String[] { "yes", "y" };
    private final static String[] BOOLEAN_NO_REPLIES = new String[] { "no", "n" };

    /** Path to the SDK folder. This is the parent of {@link #TOOLSDIR}. */
    private String mSdkFolder;
    /** Logger object. Use this to print normal output, warnings or errors. */
    private ISdkLog mSdkLog;
    /** The SDK manager parses the SDK folder and gives access to the content. */
    private SdkManager mSdkManager;
    /** Virtual Machine manager to access the list of VMs or create new ones. */
    private VmManager mVmManager;
    /** Command-line processor with options specific to SdkManager. */
    private SdkCommandLine mSdkCommandLine;
    /** The working directory, either null or set to an existing absolute canonical directory. */
    private File mWorkDir;

    public static void main(String[] args) {
        new Main().run(args);
    }
    
    /**
     * Runs the sdk manager app
     */
    private void run(String[] args) {
        createLogger();
        init();
        mSdkCommandLine.parseArgs(args);
        parseSdk();
        doAction();
    }

    /**
     * Creates the {@link #mSdkLog} object.
     * <p/>
     * This must be done before {@link #init()} as it will be used to report errors.
     */
    private void createLogger() {
        mSdkLog = new ISdkLog() {
            public void error(Throwable t, String errorFormat, Object... args) {
                if (errorFormat != null) {
                    System.err.printf("Error: " + errorFormat, args);
                    if (!errorFormat.endsWith("\n")) {
                        System.err.printf("\n");
                    }
                }
                if (t != null) {
                    System.err.printf("Error: %s\n", t.getMessage());
                }
            }

            public void warning(String warningFormat, Object... args) {
                if (mSdkCommandLine.isVerbose()) {
                    System.out.printf("Warning: " + warningFormat, args);
                    if (!warningFormat.endsWith("\n")) {
                        System.out.printf("\n");
                    }
                }
            }

            public void printf(String msgFormat, Object... args) {
                System.out.printf(msgFormat, args);
            }
        };
    }

    /**
     * Init the application by making sure the SDK path is available and
     * doing basic parsing of the SDK.
     */
    private void init() {
        mSdkCommandLine = new SdkCommandLine(mSdkLog);

        // We get passed a property for the tools dir
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
            errorAndExit("The tools directory property is not set, please make sure you are executing %1$s",
                SdkConstants.androidCmdName());
        }
        
        // We might get passed a property for the working directory
        // Either it is a valid directory and mWorkDir is set to it's absolute canonical value
        // or mWorkDir remains null.
        String workDirProp = System.getProperty(WORKDIR);
        if (workDirProp == null) {
            workDirProp = System.getenv(WORKDIR);
        }
        if (workDirProp != null) {
            // This should be a valid directory
            mWorkDir = new File(workDirProp);
            try {
                mWorkDir = mWorkDir.getCanonicalFile().getAbsoluteFile();
            } catch (IOException e) {
                mWorkDir = null;
            }
            if (mWorkDir == null || !mWorkDir.isDirectory()) {
                errorAndExit("The working directory does not seem to be valid: '%1$s", workDirProp);
            }
        }
    }

    /**
     * Does the basic SDK parsing required for all actions
     */
    private void parseSdk() {
        mSdkManager = SdkManager.createManager(mSdkFolder, mSdkLog);
        
        if (mSdkManager == null) {
            errorAndExit("Unable to parse SDK content.");
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
                errorAndExit("Target id is not valid. Use '%s list -f target' to get the target Ids.",
                        SdkConstants.androidCmdName());
            }
            IAndroidTarget target = targets[targetId - 1];
            
            ProjectCreator creator = new ProjectCreator(mSdkFolder,
                    mSdkCommandLine.isVerbose() ? OutputLevel.VERBOSE :
                        mSdkCommandLine.isSilent() ? OutputLevel.SILENT :
                            OutputLevel.NORMAL,
                    mSdkLog);

            String projectDir = getProjectLocation(mSdkCommandLine.getNewProjectLocation());
            
            creator.createProject(projectDir,
                    mSdkCommandLine.getNewProjectName(),
                    mSdkCommandLine.getNewProjectPackage(),
                    mSdkCommandLine.getNewProjectActivity(),
                    target,
                    false /* isTestProject*/);
        } else if (SdkCommandLine.ACTION_UPDATE_PROJECT.equals(action)) {
            // get the target and try to resolve it.
            IAndroidTarget target = null;
            int targetId = mSdkCommandLine.getUpdateProjectTargetId();
            if (targetId >= 0) {
                IAndroidTarget[] targets = mSdkManager.getTargets();
                if (targetId < 1 || targetId > targets.length) {
                    errorAndExit("Target id is not valid. Use '%s list -f target' to get the target Ids.",
                            SdkConstants.androidCmdName());
                }
                target = targets[targetId - 1];
            }
            
            ProjectCreator creator = new ProjectCreator(mSdkFolder,
                    mSdkCommandLine.isVerbose() ? OutputLevel.VERBOSE :
                        mSdkCommandLine.isSilent() ? OutputLevel.SILENT :
                            OutputLevel.NORMAL,
                    mSdkLog);

            String projectDir = getProjectLocation(mSdkCommandLine.getUpdateProjectLocation());
            
            creator.updateProject(projectDir,
                    target,
                    mSdkCommandLine.getUpdateProjectName());
        } else {
            mSdkCommandLine.printHelpAndExit(null);
        }
    }

    /**
     * Adjusts the project location to make it absolute & canonical relative to the
     * working directory, if any.
     * 
     * @return The project absolute path relative to {@link #mWorkDir} or the original
     *         newProjectLocation otherwise.
     */
    private String getProjectLocation(String newProjectLocation) {
        
        // If the new project location is absolute, use it as-is
        File projectDir = new File(newProjectLocation);
        if (projectDir.isAbsolute()) {
            return newProjectLocation;
        }

        // if there's no working directory, just use the project location as-is.
        if (mWorkDir == null) {
            return newProjectLocation;
        }

        // Combine then and get an absolute canonical directory
        try {
            projectDir = new File(mWorkDir, newProjectLocation).getCanonicalFile();
            
            return projectDir.getPath();
        } catch (IOException e) {
            errorAndExit("Failed to combine working directory '%1$s' with project location '%2$s': %3$s",
                    mWorkDir.getPath(),
                    newProjectLocation,
                    e.getMessage());
            return null;
        }
    }

    /**
     * Displays the list of available Targets (Platforms and Add-ons)
     */
    private void displayTargetList() {
        mSdkLog.printf("Available Android targets:\n");

        int index = 1;
        for (IAndroidTarget target : mSdkManager.getTargets()) {
            if (target.isPlatform()) {
                mSdkLog.printf("[%d] %s\n", index, target.getName());
                mSdkLog.printf("     API level: %d\n", target.getApiVersionNumber());
            } else {
                mSdkLog.printf("[%d] Add-on: %s\n", index, target.getName());
                mSdkLog.printf("     Vendor: %s\n", target.getVendor());
                if (target.getDescription() != null) {
                    mSdkLog.printf("     Description: %s\n", target.getDescription());
                }
                mSdkLog.printf("     Based on Android %s (API level %d)\n",
                        target.getApiVersionName(), target.getApiVersionNumber());
                
                // display the optional libraries.
                IOptionalLibrary[] libraries = target.getOptionalLibraries();
                if (libraries != null) {
                    mSdkLog.printf("     Libraries:\n");
                    for (IOptionalLibrary library : libraries) {
                        mSdkLog.printf("     * %1$s (%2$s)\n",
                                library.getName(), library.getJarName());
                        mSdkLog.printf(String.format(
                                "         %1$s\n", library.getDescription()));
                    }
                }
            }

            // get the target skins
            String[] skins = target.getSkins();
            mSdkLog.printf("     Skins: ");
            if (skins != null) {
                boolean first = true;
                for (String skin : skins) {
                    if (first == false) {
                        mSdkLog.printf(", ");
                    } else {
                        first = false;
                    }
                    mSdkLog.printf(skin);
                }
                mSdkLog.printf("\n");
            } else {
                mSdkLog.printf("no skins.\n");
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

            mSdkLog.printf("Available Android VMs:\n");

            int index = 1;
            for (VmInfo info : mVmManager.getVms()) {
                mSdkLog.printf("[%d] %s\n", index, info.getName());
                mSdkLog.printf("    Path: %s\n", info.getPath());

                // get the target of the Vm
                IAndroidTarget target = info.getTarget();
                if (target.isPlatform()) {
                    mSdkLog.printf("    Target: %s (API level %d)\n", target.getName(),
                            target.getApiVersionNumber());
                } else {
                    mSdkLog.printf("    Target: %s (%s)\n", target.getName(), target
                            .getVendor());
                    mSdkLog.printf("    Based on Android %s (API level %d)\n", target
                            .getApiVersionName(), target.getApiVersionNumber());
                }

                index++;
            }
        } catch (AndroidLocationException e) {
            errorAndExit(e.getMessage());
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
            errorAndExit("Target id is not valid. Use '%s list -f target' to get the target Ids.",
                    SdkConstants.androidCmdName());
        }

        try {
            mVmManager = new VmManager(mSdkManager, mSdkLog);

            String vmName = mSdkCommandLine.getNewVmName();
            VmInfo info = mVmManager.getVm(vmName);
            if (info != null) {
                errorAndExit("VM %s already exists.", vmName);
            } else {
                String vmParentFolder = mSdkCommandLine.getNewVmLocation();
                if (vmParentFolder == null) {
                    vmParentFolder = AndroidLocation.getFolder() + AndroidLocation.FOLDER_VMS;
                }

                Map<String, String> hardwareConfig = null;
                if (target.isPlatform()) {
                    try {
                        hardwareConfig = promptForHardware(target);
                    } catch (IOException e) {
                        errorAndExit(e.getMessage());
                    }
                }

                mVmManager.createVm(vmParentFolder,
                        mSdkCommandLine.getNewVmName(),
                        target,
                        mSdkCommandLine.getNewVmSkin(),
                        mSdkCommandLine.getNewVmSdCard(),
                        hardwareConfig,
                        mSdkLog);
            }
        } catch (AndroidLocationException e) {
            errorAndExit(e.getMessage());
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
        
        mSdkLog.printf("%s is a basic Android platform.\n", createTarget.getName());
        mSdkLog.printf("Do you wish to create a custom hardware profile [%s]",
                defaultAnswer);
        
        result = readLine(readLineBuffer).trim();
        // handle default:
        if (result.length() == 0) {
            result = defaultAnswer;
        }

        if (getBooleanReply(result) == false) {
            // no custom config.
            return null;
        }
        
        mSdkLog.printf("\n"); // empty line
        
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
                mSdkLog.printf("%s: %s\n", property.getAbstract(), description);
            } else {
                mSdkLog.printf("%s\n", property.getAbstract());
            }

            String defaultValue = property.getDefault();
            
            if (defaultValue != null) {
                mSdkLog.printf("%s [%s]:", property.getName(), defaultValue);
            } else {
                mSdkLog.printf("%s (%s):", property.getName(), property.getType());
            }
            
            result = readLine(readLineBuffer);
            if (result.length() == 0) {
                if (defaultValue != null) {
                    mSdkLog.printf("\n"); // empty line
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
                        mSdkLog.printf("\n%s\n", e.getMessage());
                    }
                    break;
                case INTEGER:
                    try {
                        Integer.parseInt(result);
                        map.put(property.getName(), result);
                        i++; // valid reply, move to next property
                    } catch (NumberFormatException e) {
                        // display error, and do not increment i to redo this property
                        mSdkLog.printf("\n%s\n", e.getMessage());
                    }
                    break;
                case DISKSIZE:
                    // TODO check validity
                    map.put(property.getName(), result);
                    i++; // valid reply, move to next property
                    break;
            }
            
            mSdkLog.printf("\n"); // empty line
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
    
    private void errorAndExit(String format, Object...args) {
        mSdkLog.error(null, format, args);
        System.exit(1);
    }
}