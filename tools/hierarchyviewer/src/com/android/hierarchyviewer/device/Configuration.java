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

package com.android.hierarchyviewer.device;

public class Configuration {
    public static final int DEFAULT_SERVER_PORT = 4939;

    // These codes must match the auto-generated codes in IWindowManager.java
    // See IWindowManager.aidl as well
    public static final int SERVICE_CODE_START_SERVER = 1;
    public static final int SERVICE_CODE_STOP_SERVER = 2;
    public static final int SERVICE_CODE_IS_SERVER_RUNNING = 3;
}
