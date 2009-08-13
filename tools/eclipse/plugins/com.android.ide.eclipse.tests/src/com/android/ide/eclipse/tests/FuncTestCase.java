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

import junit.framework.TestCase;

/**
 * Generic superclass for Eclipse Android functional test cases, that provides
 * common facilities
 */
public class FuncTestCase extends TestCase {

    private String mOsSdkLocation;

    /**
     * Constructor
     *
     * @throws IllegalArgumentException if environment variable "sdk_home" is
     *         not set
     */
    protected FuncTestCase() {
        mOsSdkLocation = System.getProperty("sdk_home");
        if (mOsSdkLocation == null) {
            mOsSdkLocation = System.getenv("sdk_home");
        }
        if (mOsSdkLocation == null || mOsSdkLocation.length() < 1) {
            throw new IllegalArgumentException(
                    "Environment variable sdk_home is not set");
        }
    }

    /**
     * Returns the absolute file system path of the Android SDK location to use
     * for this test
     */
    protected String getOsSdkLocation() {
        return mOsSdkLocation;
    }


}
