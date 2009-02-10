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

package com.android.sdklib.avd;

import com.android.prefs.AndroidLocation;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.ISdkLog;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.SdkManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Virtual Device Manager to access the list of AVDs or create new ones.
 */
public final class AvdManager {
    
    private static final String AVD_FOLDER_EXTENSION = ".avd";
    private final static String AVD_INFO_PATH = "path";
    private final static String AVD_INFO_TARGET = "target";

    private final static String IMAGE_USERDATA = "userdata.img";
    private final static String CONFIG_INI = "config.ini";

    private final static Pattern INI_NAME_PATTERN = Pattern.compile("(.+)\\.ini$",
            Pattern.CASE_INSENSITIVE);

    private final static Pattern SDCARD_SIZE_PATTERN = Pattern.compile("\\d+[MK]?");

    public static final class AvdInfo {
        String name;
        String path;
        IAndroidTarget target;

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        public IAndroidTarget getTarget() {
            return target;
        }
    }

    private final ArrayList<AvdInfo> mAvdList = new ArrayList<AvdInfo>();
    private ISdkLog mSdkLog;
    private final SdkManager mSdk;

    public AvdManager(SdkManager sdk, ISdkLog sdkLog) throws AndroidLocationException {
        mSdk = sdk;
        mSdkLog = sdkLog;
        buildAvdList();
    }

    /**
     * Returns the existing AVDs.
     * @return a newly allocated array containing all the AVDs.
     */
    public AvdInfo[] getAvds() {
        return mAvdList.toArray(new AvdInfo[mAvdList.size()]);
    }

    /**
     * Returns the {@link AvdInfo} matching the given <var>name</var>.
     * @return the matching AvdInfo or <code>null</code> if none were found.
     */
    public AvdInfo getAvd(String name) {
        for (AvdInfo info : mAvdList) {
            if (info.name.equals(name)) {
                return info;
            }
        }
        
        return null;
    }

    /**
     * Creates a new AVD. It is expected that there is no existing AVD with this name already.
     * @param parentFolder the folder to contain the AVD. A new folder will be created in this
     * folder with the name of the AVD
     * @param name the name of the AVD
     * @param target the target of the AVD
     * @param skinName the name of the skin. Can be null.
     * @param sdcard the parameter value for the sdCard. Can be null. This is either a path to
     * an existing sdcard image or a sdcard size (\d+, \d+K, \dM).
     * @param hardwareConfig the hardware setup for the AVD
     * @param removePrevious If true remove any previous files.
     */
    public AvdInfo createAvd(String parentFolder, String name, IAndroidTarget target,
            String skinName, String sdcard, Map<String,String> hardwareConfig,
            boolean removePrevious, ISdkLog log) {
        
        try {
            File rootDirectory = new File(parentFolder);
            if (rootDirectory.isDirectory() == false) {
                if (log != null) {
                    log.error(null, "Folder %s does not exist.", parentFolder);
                }
                return null;
            }
            
            File avdFolder = new File(parentFolder, name + AVD_FOLDER_EXTENSION);
            if (avdFolder.exists()) {
                if (removePrevious) {
                    // AVD already exists and removePrevious is set, try to remove the
                    // directory's content first (but not the directory itself).
                    recursiveDelete(avdFolder);
                } else {
                    // AVD shouldn't already exist if removePrevious is false.
                    if (log != null) {
                        log.error(null, "Folder %s is in the way.", avdFolder.getAbsolutePath());
                    }
                    return null;
                }
            }

            // create the AVD folder.
            avdFolder.mkdir();

            HashMap<String, String> values = new HashMap<String, String>();

            // prepare the ini file.
            String avdRoot = AndroidLocation.getFolder() + AndroidLocation.FOLDER_AVD;
            File iniFile = new File(avdRoot, name + ".ini");
            values.put(AVD_INFO_PATH, avdFolder.getAbsolutePath());
            values.put(AVD_INFO_TARGET, target.hashString());
            createConfigIni(iniFile, values);

            // writes the userdata.img in it.
            String imagePath = target.getPath(IAndroidTarget.IMAGES);
            File userdataSrc = new File(imagePath, IMAGE_USERDATA);
            FileInputStream fis = new FileInputStream(userdataSrc);
            
            File userdataDest = new File(avdFolder, IMAGE_USERDATA);
            FileOutputStream fos = new FileOutputStream(userdataDest);
            
            byte[] buffer = new byte[4096];
            int count;
            while ((count = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, count);
            }
            
            fos.close();
            fis.close();
            
            // Config file
            values.clear();
            if (skinName != null) {
                // check that the skin name is valid
                String[] skinNames = target.getSkins();
                boolean found = false;
                for (String n : skinNames) {
                    if (n.equals(skinName)) {
                        values.put("skin", skinName);
                        found = true;
                        break;
                    }
                }

                if (found == false && log != null) {
                    log.warning("Skin '%1$s' does not exists, using default skin.", skinName);
                }
            }

            if (sdcard != null) {
                File sdcardFile = new File(sdcard);
                if (sdcardFile.isFile()) {
                    values.put("sdcard", sdcard);
                } else {
                    // check that it matches the pattern for sdcard size
                    Matcher m = SDCARD_SIZE_PATTERN.matcher(sdcard);
                    if (m.matches()) {
                        // create the sdcard.
                        sdcardFile = new File(avdFolder, "sdcard.img");
                        String path = sdcardFile.getAbsolutePath();
                        
                        // execute mksdcard with the proper parameters.
                        File toolsFolder = new File(mSdk.getLocation(), SdkConstants.FD_TOOLS);
                        File mkSdCard = new File(toolsFolder, SdkConstants.mkSdCardCmdName());
                        
                        if (mkSdCard.isFile() == false) {
                            log.error(null, "'%1$s' is missing from the SDK tools folder.",
                                    mkSdCard.getName());
                            return null;
                        }
                        
                        if (createSdCard(mkSdCard.getAbsolutePath(), sdcard, path, log) == false) {
                            return null; // mksdcard output has already been displayed, no need to
                                         // output anything else.
                        }
                        
                        // add its path to the values.
                        values.put("sdcard", path);
                    } else {
                        log.error(null, "'%1$s' is not recognized as a valid sdcard value", sdcard);
                        return null;
                    }
                }
            }

            if (hardwareConfig != null) {
                values.putAll(hardwareConfig);
            }

            File configIniFile = new File(avdFolder, CONFIG_INI);
            createConfigIni(configIniFile, values);
            
            if (log != null) {
                if (target.isPlatform()) {
                    log.printf("Created AVD '%s' based on %s\n", name, target.getName());
                } else {
                    log.printf("Created AVD '%s' based on %s (%s)\n", name, target.getName(),
                               target.getVendor());
                }
            }
            
            // create the AvdInfo object, and add it to the list
            AvdInfo avdInfo = new AvdInfo();
            avdInfo.name = name;
            avdInfo.path = avdFolder.getAbsolutePath();
            avdInfo.target = target;
            
            mAvdList.add(avdInfo);
            
            return avdInfo;
        } catch (AndroidLocationException e) {
            if (log != null) {
                log.error(e, null);
            }
        } catch (IOException e) {
            if (log != null) {
                log.error(e, null);
            }
        }
        
        return null;
    }

    /**
     * Helper method to recursively delete a folder's content (but not the folder itself).
     * 
     * @throws SecurityException like {@link File#delete()} does if file/folder is not writable.
     */
    public void recursiveDelete(File folder) {
        for (File f : folder.listFiles()) {
            if (f.isDirectory()) {
                recursiveDelete(folder);
            }
            f.delete();
        }
    }

    private void buildAvdList() throws AndroidLocationException {
        // get the Android prefs location.
        String avdRoot = AndroidLocation.getFolder() + AndroidLocation.FOLDER_AVD;
        
        // ensure folder validity.
        File folder = new File(avdRoot);
        if (folder.isFile()) {
            throw new AndroidLocationException(String.format("%s is not a valid folder.", avdRoot));
        } else if (folder.exists() == false) {
            // folder is not there, we create it and return
            folder.mkdirs();
            return;
        }
        
        File[] avds = folder.listFiles(new FilenameFilter() {
            public boolean accept(File parent, String name) {
                if (INI_NAME_PATTERN.matcher(name).matches()) {
                    // check it's a file and not a folder
                    return new File(parent, name).isFile();
                }

                return false;
            }
        });
        
        for (File avd : avds) {
            AvdInfo info = parseAvdInfo(avd);
            if (info != null) {
                mAvdList.add(info);
            }
        }
    }
    
    private AvdInfo parseAvdInfo(File path) {
        Map<String, String> map = SdkManager.parsePropertyFile(path, mSdkLog);
        
        String avdPath = map.get(AVD_INFO_PATH);
        if (avdPath == null) {
            return null;
        }
        
        String targetHash = map.get(AVD_INFO_TARGET);
        if (targetHash == null) {
            return null;
        }

        IAndroidTarget target = mSdk.getTargetFromHashString(targetHash);
        if (target == null) {
            return null;
        }

        AvdInfo info = new AvdInfo();
        Matcher matcher = INI_NAME_PATTERN.matcher(path.getName());
        if (matcher.matches()) {
            info.name = matcher.group(1);
        } else {
            info.name = path.getName(); // really this should not happen.
        }
        info.path = avdPath;
        info.target = target;
        
        return info;
    }
    
    private static void createConfigIni(File iniFile, Map<String, String> values)
            throws IOException {
        FileWriter writer = new FileWriter(iniFile);
        
        for (Entry<String, String> entry : values.entrySet()) {
            writer.write(String.format("%s=%s\n", entry.getKey(), entry.getValue()));
        }
        writer.close();

    }
    
    private boolean createSdCard(String toolLocation, String size, String location, ISdkLog log) {
        try {
            String[] command = new String[3];
            command[0] = toolLocation;
            command[1] = size;
            command[2] = location;
            Process process = Runtime.getRuntime().exec(command);
    
            ArrayList<String> errorOutput = new ArrayList<String>();
            ArrayList<String> stdOutput = new ArrayList<String>();
            int status = grabProcessOutput(process, errorOutput, stdOutput,
                    true /* waitForReaders */);
    
            if (status != 0) {
                log.error(null, "Failed to create the SD card.");
                for (String error : errorOutput) {
                    log.error(null, error);
                }
                
                return false;
            }

            return true;
        } catch (InterruptedException e) {
            log.error(null, "Failed to create the SD card.");
        } catch (IOException e) {
            log.error(null, "Failed to create the SD card.");
        }
        
        return false;
    }
    
    /**
     * Gets the stderr/stdout outputs of a process and returns when the process is done.
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

    /**
     * Removes an {@link AvdInfo} from the internal list.
     * 
     * @param avdInfo The {@link AvdInfo} to remove.
     * @return true if this {@link AvdInfo} was present and has been removed.
     */
    public boolean removeAvd(AvdInfo avdInfo) {
        return mAvdList.remove(avdInfo);
    }

}
