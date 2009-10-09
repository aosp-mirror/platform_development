/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License"); you
 * may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.ide.eclipse.tests;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.sdk.LoadStatus;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;

/**
 * A test case which uses the Sdk loaded by the Adt plugin.
 */
public abstract class AdtSdkTestCase extends SdkTestCase {

    protected AdtSdkTestCase() {
    }

    /**
     * Gets the current Sdk from Adt, waiting if necessary.
     */
    @Override
    protected Sdk loadSdk() {
        AdtPlugin adt = AdtPlugin.getDefault();
        Object sdkLock = adt.getSdkLockObject();
        LoadStatus loadStatus = LoadStatus.LOADING;
        // wait for Adt to load the Sdk on a separate thread
        // loop max of 600 times * 200 ms =  2 minutes
        final int maxWait = 600;
        for (int i=0; i < maxWait && loadStatus == LoadStatus.LOADING; i++) {
            try {
                Thread.sleep(200);
            }
            catch (InterruptedException e) {
                // ignore
            }
            synchronized(sdkLock) {
                loadStatus = adt.getSdkLoadStatus();
            }
        }
        Sdk sdk = null;
        synchronized(sdkLock) {
            assertEquals(LoadStatus.LOADED, loadStatus);
            sdk = Sdk.getCurrent();
        }
        assertNotNull(sdk);
        return sdk;
    }
}
