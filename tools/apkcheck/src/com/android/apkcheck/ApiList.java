/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.apkcheck;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Holds a list of API members, including classes, fields, and methods.
 */
public class ApiList {
    private HashMap<String,PackageInfo> mPackageList;
    private String mDebugString;
    private int mWarnings, mErrors;

    /**
     * Constructs an ApiList.
     *
     * @param debugString Identification string useful for debugging.
     */
    public ApiList(String debugString) {
        mPackageList = new HashMap<String,PackageInfo>();
        mDebugString = debugString;
    }

    /**
     * Returns the source filename.  Useful for debug messages only.
     */
    public String getDebugString() {
        return mDebugString;
    }

    /**
     * Increment the number of warnings associated with this API list.
     */
    public void incrWarnings() {
        mWarnings++;
    }

    /**
     * Increment the errors of warnings associated with this API list.
     */
    public void incrErrors() {
        mErrors++;
    }

    /**
     * Returns the number of warnings associated with this API list.
     */
    public int getWarningCount() {
        return mWarnings;
    }

    /**
     * Returns the number of errors associated with this API list.
     */
    public int getErrorCount() {
        return mErrors;
    }

    /**
     * Retrieves the named package.
     *
     * @return the package, or null if no match was found
     */
    public PackageInfo getPackage(String name) {
        return mPackageList.get(name);
    }

    /**
     * Retrieves the named package, creating it if it doesn't already
     * exist.
     */
    public PackageInfo getOrCreatePackage(String name) {
        PackageInfo pkgInfo = mPackageList.get(name);
        if (pkgInfo == null) {
            pkgInfo = new PackageInfo(name);
            mPackageList.put(name, pkgInfo);
        }
        return pkgInfo;
    }

    /**
     * Returns an iterator for the set of known packages.
     */
    public Iterator<PackageInfo> getPackageIterator() {
        return mPackageList.values().iterator();
    }
}

