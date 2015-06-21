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

import android.app.IActivityManager;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.view.IWindowManager;

public class MonkeyPermissionEvent extends MonkeyEvent {
    private String mPkg;
    private PermissionInfo mPermissionInfo;

    public MonkeyPermissionEvent(String pkg, PermissionInfo permissionInfo) {
        super(EVENT_TYPE_PERMISSION);
        mPkg = pkg;
        mPermissionInfo = permissionInfo;
    }

    @Override
    public int injectEvent(IWindowManager iwm, IActivityManager iam, int verbose) {
        IPackageManager pm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        try {
            // determine if we should grant or revoke permission
            int perm = pm.checkPermission(mPermissionInfo.name, mPkg, UserHandle.myUserId());
            boolean grant = perm == PackageManager.PERMISSION_DENIED;
            // log before calling pm in case we hit an error
            System.out.println(String.format(":Permission %s %s to package %s",
                    grant ? "grant" : "revoke", mPermissionInfo.name, mPkg));
            if (grant) {
                pm.grantRuntimePermission(mPkg, mPermissionInfo.name, UserHandle.myUserId());
            } else {
                pm.revokeRuntimePermission(mPkg, mPermissionInfo.name, UserHandle.myUserId());
            }
            return MonkeyEvent.INJECT_SUCCESS;
        } catch (RemoteException re) {
            return MonkeyEvent.INJECT_ERROR_REMOTE_EXCEPTION;
        }
    }
}
