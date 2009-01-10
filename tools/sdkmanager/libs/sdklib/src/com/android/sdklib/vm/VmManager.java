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

package com.android.sdklib.vm;

import com.android.prefs.AndroidLocation;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.ISdkLog;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.SdkManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Virtual Machine manager to access the list of VMs or create new ones.
 */
public final class VmManager {
    
    private final static String VM_INFO_PATH = "path";
    private final static String VM_INFO_TARGET = "target";
    
    private final static String IMAGE_USERDATA = "userdata.img";
    private final static String CONFIG_INI = "config.ini";
    
    private final static Pattern INI_NAME_PATTERN = Pattern.compile("(.+)\\.ini$",
            Pattern.CASE_INSENSITIVE);

    public static final class VmInfo {
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
    
    private final ArrayList<VmInfo> mVmList = new ArrayList<VmInfo>();
    private ISdkLog mSdkLog;

    public VmManager(SdkManager sdk, ISdkLog sdkLog) throws AndroidLocationException {
        mSdkLog = sdkLog;
        buildVmList(sdk);
    }
    
    /**
     * Returns the existing VMs.
     * @return a newly allocated arrays containing all the VMs.
     */
    public VmInfo[] getVms() {
        return mVmList.toArray(new VmInfo[mVmList.size()]);
    }
    
    /**
     * Returns the {@link VmInfo} matching the given <var>name</var>.
     * @return the matching VmInfo or <code>null</code> if none were found.
     */
    public VmInfo getVm(String name) {
        for (VmInfo info : mVmList) {
            if (info.name.equals(name)) {
                return info;
            }
        }
        
        return null;
    }

    /**
     * Creates a new VM.
     * @param parentFolder the folder to contain the VM. A new folder will be created in this
     * folder with the name of the VM
     * @param name the name of the VM
     * @param target the target of the VM
     * @param skinName the name of the skin. Can be null.
     * @param sdcardPath the path to the sdCard. Can be null.
     * @param sdcardSize the size of a local sdcard to create. Can be 0 for no local sdcard.
     * @param hardwareConfig the hardware setup for the VM
     */
    public static void createVm(String parentFolder, String name, IAndroidTarget target,
            String skinName, String sdcardPath, int sdcardSize, Map<String,String> hardwareConfig,
            ISdkLog log) {

        // now write the ini file in the vmRoot folder.
        // get the Android prefs location.
        try {
            File rootDirectory = new File(parentFolder);
            if (rootDirectory.isDirectory() == false) {
                if (log != null) {
                    log.error(null, "%s does not exists.", parentFolder);
                }
                return;
            }
            
            File vmFolder = new File(parentFolder, name + ".avm");
            if (vmFolder.exists()) {
                if (log != null) {
                    log.error(null, "%s already exists.", vmFolder.getAbsolutePath());
                }
                return;
            }
            
            // create the vm folder.
            vmFolder.mkdir();

            HashMap<String, String> values = new HashMap<String, String>();

            // prepare the ini file.
            String vmRoot = AndroidLocation.getFolder() + AndroidLocation.FOLDER_VMS;
            File iniFile = new File(vmRoot, name + ".ini");
            values.put(VM_INFO_PATH, vmFolder.getAbsolutePath());
            values.put(VM_INFO_TARGET, target.hashString());
            createConfigIni(iniFile, values);

            // writes the userdata.img in it.
            String imagePath = target.getPath(IAndroidTarget.IMAGES);
            File userdataSrc = new File(imagePath, IMAGE_USERDATA);
            FileInputStream fis = new FileInputStream(userdataSrc);
            
            File userdataDest = new File(vmFolder, IMAGE_USERDATA);
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
                values.put("skin", skinName);
            } else {
                values.put("skin", SdkConstants.SKIN_DEFAULT);
            }
            
            if (sdcardPath != null) {
                values.put("sdcard", sdcardPath);
            } else if (sdcardSize != 0) {
                // TODO: create sdcard image.
            }

            if (hardwareConfig != null) {
                values.putAll(hardwareConfig);
            }

            File configIniFile = new File(vmFolder, CONFIG_INI);
            createConfigIni(configIniFile, values);
            
            if (target.isPlatform()) {
                System.out.println(String.format(
                        "Created VM '%s' based on %s", name, target.getName()));
            } else {
                System.out.println(String.format(
                        "Created VM '%s' based on %s (%s)", name, target.getName(),
                        target.getVendor()));
            }
        } catch (AndroidLocationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void buildVmList(SdkManager sdk) throws AndroidLocationException {
        // get the Android prefs location.
        String vmRoot = AndroidLocation.getFolder() + AndroidLocation.FOLDER_VMS;
        
        // ensure folder validity.
        File folder = new File(vmRoot);
        if (folder.isFile()) {
            throw new AndroidLocationException(String.format("%s is not a valid folder.", vmRoot));
        } else if (folder.exists() == false) {
            // folder is not there, we create it and return
            folder.mkdirs();
            return;
        }
        
        File[] vms = folder.listFiles(new FilenameFilter() {
            public boolean accept(File parent, String name) {
                if (INI_NAME_PATTERN.matcher(name).matches()) {
                    // check it's a file and not a folder
                    return new File(parent, name).isFile();
                }

                return false;
            }
        });
        
        for (File vm : vms) {
            VmInfo info = parseVmInfo(vm, sdk);
            if (info != null) {
                mVmList.add(info);
            }
        }
    }
    
    private VmInfo parseVmInfo(File path, SdkManager sdk) {
        Map<String, String> map = SdkManager.parsePropertyFile(path, mSdkLog);
        
        String vmPath = map.get(VM_INFO_PATH);
        if (vmPath == null) {
            return null;
        }
        
        String targetHash = map.get(VM_INFO_TARGET);
        if (targetHash == null) {
            return null;
        }

        IAndroidTarget target = sdk.getTargetFromHashString(targetHash);
        if (target == null) {
            return null;
        }

        VmInfo info = new VmInfo();
        Matcher matcher = INI_NAME_PATTERN.matcher(path.getName());
        if (matcher.matches()) {
            info.name = matcher.group(1);
        } else {
            info.name = path.getName(); // really this should not happen.
        }
        info.path = vmPath;
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
}
