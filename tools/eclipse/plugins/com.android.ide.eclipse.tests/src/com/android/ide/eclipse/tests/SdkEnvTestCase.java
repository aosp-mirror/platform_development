/*
 * Copyright (C) 2009 The Android Open Source Project
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

import com.android.ide.eclipse.adt.internal.sdk.Sdk;


/**
 * A test case that receives a specific Sdk to test via the "sdk_home" environment variable.
 */
public abstract class SdkEnvTestCase extends SdkTestCase {

    protected SdkEnvTestCase() {
    }

    /**
     * Loads the {@link Sdk}.
     * <p/>
     * Fails test if environment variable "sdk_home" is not set.
     */
    @Override
    protected Sdk loadSdk() {
        String osSdkLocation = System.getProperty("sdk_home");
        if (osSdkLocation == null) {
            osSdkLocation = System.getenv("sdk_home");
        }
        if (osSdkLocation == null || osSdkLocation.length() < 1) {
            fail("Environment variable sdk_home is not set");
        }
        return Sdk.loadSdk(osSdkLocation);
    }
}
