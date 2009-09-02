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

package com.android.ddmuilib;

import com.android.ddmlib.SyncService.ISyncProgressMonitor;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Implementation of the {@link ISyncProgressMonitor} wrapping an Eclipse {@link IProgressMonitor}.
 */
public class SyncProgressMonitor implements ISyncProgressMonitor {

    private IProgressMonitor mMonitor;
    private String mName;

    public SyncProgressMonitor(IProgressMonitor monitor, String name) {
        mMonitor = monitor;
        mName = name;
    }

    public void start(int totalWork) {
        mMonitor.beginTask(mName, totalWork);
    }

    public void stop() {
        mMonitor.done();
    }

    public void advance(int work) {
        mMonitor.worked(work);
    }

    public boolean isCanceled() {
        return mMonitor.isCanceled();
    }

    public void startSubTask(String name) {
        mMonitor.subTask(name);
    }
}
