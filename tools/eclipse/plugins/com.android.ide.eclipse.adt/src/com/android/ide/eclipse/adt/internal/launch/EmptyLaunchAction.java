/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.ide.eclipse.adt.internal.launch;

import com.android.ddmlib.IDevice;
import com.android.ide.eclipse.adt.AdtPlugin;

/**
 * A launch action that does nothing after the application has been installed
 */
public class EmptyLaunchAction implements IAndroidLaunchAction {

    public boolean doLaunchAction(DelayedLaunchInfo info, IDevice device) {
        // we're not supposed to do anything, just return;
        String msg = String.format("%1$s installed on device",
                info.getPackageFile().getFullPath().toOSString());
        AdtPlugin.printToConsole(info.getProject(), msg, "Done!");
        // return false so launch controller will not wait for debugger to attach
        return false;
    }

    public String getLaunchDescription() {
        return "sync";
    }
}
