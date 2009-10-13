/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License"); you
 * may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.ide.eclipse.tests;

import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetParser;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;

import junit.framework.TestCase;

/**
 * A test case that needs a reference to a SDK.
 */
public abstract class SdkTestCase extends TestCase {

    private Sdk mSdk;

    protected SdkTestCase() {
    }

    /**
     * Retrieve the {@link Sdk} under test.
     */
    protected Sdk getSdk() {
        if (mSdk == null) {
            mSdk = loadSdk();
            assertNotNull(mSdk);
            validateSdk(mSdk);
        }
        return mSdk;
    }

    /**
     * Loads the {@link Sdk} to use for test
     */
    protected abstract Sdk loadSdk();

    /**
     * Checks that the provided sdk contains one or more valid targets.
     * @param sdk the {@link Sdk} to validate.
     */
    private void validateSdk(Sdk sdk) {
        assertTrue("sdk has no targets", sdk.getTargets().length > 0);
        for (IAndroidTarget target : sdk.getTargets()) {
            IStatus status = new AndroidTargetParser(target).run(new NullProgressMonitor());
            if (status.getCode() != IStatus.OK) {
                fail("Failed to parse targets data");
            }
        }
    }
}
