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

package com.android.sdklib.internal.repository;

import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.ISdkLog;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.SdkManager;
import com.android.sdklib.internal.repository.Archive.Arch;
import com.android.sdklib.internal.repository.Archive.Os;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Scans a local SDK to find which packages are currently installed.
 */
public class LocalSdkParser {

    static final String SOURCE_PROPERTIES = "source.properties";  //$NON-NLS-1$
    private Package[] mPackages;

    public LocalSdkParser() {
        // pass
    }

    /**
     * Returns the packages found by the last call to {@link #parseSdk(String, SdkManager, ISdkLog)}.
     */
    public Package[] getPackages() {
        return mPackages;
    }

    /**
     * Clear the internal packages list. After this call, {@link #getPackages()} will return
     * null till {@link #parseSdk(String, SdkManager, ISdkLog)} is called.
     */
    public void clearPackages() {
        mPackages = null;
    }

    /**
     * Scan the give SDK to find all the packages already installed at this location.
     * <p/>
     * Store the packages internally. You can use {@link #getPackages()} to retrieve them
     * at any time later.
     *
     * @param osSdkRoot The path to the SDK folder.
     * @param sdkManager An existing SDK manager to list current platforms and addons.
     * @param log An SDK logger object.
     * @return The packages found. Can be retrieved later using {@link #getPackages()}.
     */
    public Package[] parseSdk(String osSdkRoot, SdkManager sdkManager, ISdkLog log) {
        ArrayList<Package> packages = new ArrayList<Package>();
        HashSet<File> visited = new HashSet<File>();

        File dir = new File(osSdkRoot, SdkConstants.FD_DOCS);
        Package pkg = scanDoc(dir, log);
        if (pkg != null) {
            packages.add(pkg);
            visited.add(dir);
        }

        dir = new File(osSdkRoot, SdkConstants.FD_TOOLS);
        pkg = scanTools(dir, log);
        if (pkg != null) {
            packages.add(pkg);
            visited.add(dir);
        }

        // for platforms and add-ons, rely on the SdkManager parser
        for(IAndroidTarget target : sdkManager.getTargets()) {

            Properties props = parseProperties(new File(target.getLocation(), SOURCE_PROPERTIES));

            try {
                if (target.isPlatform()) {
                    pkg = new PlatformPackage(target, props);
                } else {
                    pkg = new AddonPackage(target, props);
                }
            } catch (Exception e) {
                log.error(e, null);
            }

            if (pkg != null) {
                packages.add(pkg);
                visited.add(new File(target.getLocation()));
            }
        }

        scanExtra(osSdkRoot, visited, packages, log);

        mPackages = packages.toArray(new Package[packages.size()]);
        return mPackages;
    }

    /**
     * Find any other directory what we haven't successfully visited and
     * assume they contain extra packages.
     * @param log
     */
    private void scanExtra(String osSdkRoot,
            HashSet<File> visited,
            ArrayList<Package> packages,
            ISdkLog log) {
        File root = new File(osSdkRoot);
        for (File dir : root.listFiles()) {
            if (dir.isDirectory() && !visited.contains(dir)) {

                Properties props = parseProperties(new File(dir, SOURCE_PROPERTIES));
                if (props != null) {
                    try {
                        ExtraPackage pkg = new ExtraPackage(
                                null,                       //source
                                props,                      //properties
                                dir.getName(),              //path
                                0,                          //revision
                                null,                       //license
                                "Tools",                    //description
                                null,                       //descUrl
                                Os.getCurrentOs(),          //archiveOs
                                Arch.getCurrentArch(),      //archiveArch
                                dir.getPath()               //archiveOsPath
                                );

                        // We only accept this as an extra package if it has a valid local path.
                        if (pkg.isPathValid()) {
                            packages.add(pkg);
                        }
                    } catch (Exception e) {
                        log.error(e, null);
                    }
                }
            }
        }
    }

    /**
     * Try to find a tools package at the given location.
     * Returns null if not found.
     */
    private Package scanTools(File toolFolder, ISdkLog log) {
        // Can we find some properties?
        Properties props = parseProperties(new File(toolFolder, SOURCE_PROPERTIES));

        // We're not going to check that all tools are present. At the very least
        // we should expect to find adb, android and an emulator adapted to the current OS.
        Set<String> names = new HashSet<String>();
        for (File file : toolFolder.listFiles()) {
            names.add(file.getName());
        }
        if (!names.contains(SdkConstants.FN_ADB) ||
                !names.contains(SdkConstants.androidCmdName()) ||
                !names.contains(SdkConstants.FN_EMULATOR)) {
            return null;
        }

        // Create are package. use the properties if we found any.
        try {
            ToolPackage pkg = new ToolPackage(
                    null,                       //source
                    props,                      //properties
                    0,                          //revision
                    null,                       //license
                    "Tools",                    //description
                    null,                       //descUrl
                    Os.getCurrentOs(),          //archiveOs
                    Arch.getCurrentArch(),      //archiveArch
                    toolFolder.getPath()        //archiveOsPath
                    );

            return pkg;
        } catch (Exception e) {
            log.error(e, null);
        }
        return null;
    }

    /**
     * Try to find a docs package at the given location.
     * Returns null if not found.
     */
    private Package scanDoc(File docFolder, ISdkLog log) {
        // Can we find some properties?
        Properties props = parseProperties(new File(docFolder, SOURCE_PROPERTIES));

        // To start with, a doc folder should have an "index.html" to be acceptable.
        // We don't actually check the content of the file.
        if (new File(docFolder, "index.html").isFile()) {
            try {
                DocPackage pkg = new DocPackage(
                        null,                       //source
                        props,                      //properties
                        0,                          //apiLevel
                        null,                       //codename
                        0,                          //revision
                        null,                       //license
                        null,                       //description
                        null,                       //descUrl
                        Os.getCurrentOs(),          //archiveOs
                        Arch.getCurrentArch(),      //archiveArch
                        docFolder.getPath()         //archiveOsPath
                        );

                return pkg;
            } catch (Exception e) {
                log.error(e, null);
            }
        }

        return null;
    }

    /**
     * Parses the given file as properties file if it exists.
     * Returns null if the file does not exist, cannot be parsed or has no properties.
     */
    private Properties parseProperties(File propsFile) {
        FileInputStream fis = null;
        try {
            if (propsFile.exists()) {
                fis = new FileInputStream(propsFile);

                Properties props = new Properties();
                props.load(fis);

                // To be valid, there must be at least one property in it.
                if (props.size() > 0) {
                    return props;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }
}
