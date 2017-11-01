/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.commands.monkey;

import android.Manifest;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Build;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Utility class that encapsulates runtime permission related methods for monkey
 *
 */
public class MonkeyPermissionUtil {

    private static final String PERMISSION_PREFIX = "android.permission.";
    private static final String PERMISSION_GROUP_PREFIX = "android.permission-group.";

    // from com.android.packageinstaller.permission.utils
    private static final String[] MODERN_PERMISSION_GROUPS = {
            Manifest.permission_group.CALENDAR, Manifest.permission_group.CAMERA,
            Manifest.permission_group.CONTACTS, Manifest.permission_group.LOCATION,
            Manifest.permission_group.SENSORS, Manifest.permission_group.SMS,
            Manifest.permission_group.PHONE, Manifest.permission_group.MICROPHONE,
            Manifest.permission_group.STORAGE
    };

    // from com.android.packageinstaller.permission.utils
    private static boolean isModernPermissionGroup(String name) {
        for (String modernGroup : MODERN_PERMISSION_GROUPS) {
            if (modernGroup.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * actual list of packages to target, with invalid packages excluded, and may optionally include
     * system packages
     */
    private List<String> mTargetedPackages;
    /** if we should target system packages regardless if they are listed */
    private boolean mTargetSystemPackages;
    private IPackageManager mPm;

    /** keep track of runtime permissions requested for each package targeted */
    private Map<String, List<PermissionInfo>> mPermissionMap;

    public MonkeyPermissionUtil() {
        mPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
    }

    public void setTargetSystemPackages(boolean targetSystemPackages) {
        mTargetSystemPackages = targetSystemPackages;
    }

    /**
     * Decide if a package should be targeted by permission monkey
     * @param info
     * @return
     */
    private boolean shouldTargetPackage(PackageInfo info) {
        // target if permitted by white listing / black listing rules
        if (MonkeyUtils.getPackageFilter().checkEnteringPackage(info.packageName)) {
            return true;
        }
        if (mTargetSystemPackages
                // not explicitly black listed
                && !MonkeyUtils.getPackageFilter().isPackageInvalid(info.packageName)
                // is a system app
                && (info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
            return true;
        }
        return false;
    }

    private boolean shouldTargetPermission(String pkg, PermissionInfo pi) throws RemoteException {
        int flags = mPm.getPermissionFlags(pi.name, pkg, UserHandle.myUserId());
        int fixedPermFlags = PackageManager.FLAG_PERMISSION_SYSTEM_FIXED
                | PackageManager.FLAG_PERMISSION_POLICY_FIXED;
        return pi.group != null && pi.protectionLevel == PermissionInfo.PROTECTION_DANGEROUS
                && ((flags & fixedPermFlags) == 0)
                && isModernPermissionGroup(pi.group);
    }

    public boolean populatePermissionsMapping() {
        mPermissionMap = new HashMap<>();
        try {
            List<?> pkgInfos = mPm.getInstalledPackages(
                    PackageManager.GET_PERMISSIONS, UserHandle.myUserId()).getList();
            for (Object o : pkgInfos) {
                PackageInfo info = (PackageInfo)o;
                if (!shouldTargetPackage(info)) {
                    continue;
                }
                List<PermissionInfo> permissions = new ArrayList<>();
                if (info.applicationInfo.targetSdkVersion <= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    // skip apps targetting lower API level
                    continue;
                }
                if (info.requestedPermissions == null) {
                    continue;
                }
                for (String perm : info.requestedPermissions) {
                    PermissionInfo pi = mPm.getPermissionInfo(perm, "shell", 0);
                    if (pi != null && shouldTargetPermission(info.packageName, pi)) {
                        permissions.add(pi);
                    }
                }
                if (!permissions.isEmpty()) {
                    mPermissionMap.put(info.packageName, permissions);
                }
            }
        } catch (RemoteException re) {
            Logger.err.println("** Failed talking with package manager!");
            return false;
        }
        if (!mPermissionMap.isEmpty()) {
            mTargetedPackages = new ArrayList<>(mPermissionMap.keySet());
        }
        return true;
    }

    public void dump() {
        Logger.out.println("// Targeted packages and permissions:");
        for (Map.Entry<String, List<PermissionInfo>> e : mPermissionMap.entrySet()) {
            Logger.out.println(String.format("//  + Using %s", e.getKey()));
            for (PermissionInfo pi : e.getValue()) {
                String name = pi.name;
                if (name != null) {
                    if (name.startsWith(PERMISSION_PREFIX)) {
                        name = name.substring(PERMISSION_PREFIX.length());
                    }
                }
                String group = pi.group;
                if (group != null) {
                    if (group.startsWith(PERMISSION_GROUP_PREFIX)) {
                        group = group.substring(PERMISSION_GROUP_PREFIX.length());
                    }
                }
                Logger.out.println(String.format("//    Permission: %s [%s]", name, group));
            }
        }
    }

    public MonkeyPermissionEvent generateRandomPermissionEvent(Random random) {
        String pkg = mTargetedPackages.get(random.nextInt(mTargetedPackages.size()));
        List<PermissionInfo> infos = mPermissionMap.get(pkg);
        return new MonkeyPermissionEvent(pkg, infos.get(random.nextInt(infos.size())));
    }
}
