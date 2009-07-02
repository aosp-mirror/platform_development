/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.sdklib;

/**
 * Dummy implementation of an {@link ISdkLog}.
 * <p/>
 * Use {@link #getLogger()} to get a default instance of this {@link NullSdkLog}.
 *
 */
public class NullSdkLog implements ISdkLog {

    private static final ISdkLog sThis = new NullSdkLog();

    public static ISdkLog getLogger() {
        return sThis;
    }

    public void error(Throwable t, String errorFormat, Object... args) {
        // ignore
    }

    public void printf(String msgFormat, Object... args) {
        // ignore
    }

    public void warning(String warningFormat, Object... args) {
        // ignore
    }
}
