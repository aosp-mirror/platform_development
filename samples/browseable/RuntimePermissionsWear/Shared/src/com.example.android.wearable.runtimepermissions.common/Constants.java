/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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

package com.example.android.wearable.runtimepermissions.common;

import java.util.concurrent.TimeUnit;

/**
 * A collection of constants that is shared between the wearable and handset apps.
 */
public class Constants {

    // Shared
    public static final long CONNECTION_TIME_OUT_MS = TimeUnit.SECONDS.toMillis(5);

    public static final String KEY_COMM_TYPE = "communicationType";
    public static final String KEY_PAYLOAD = "payload";

    // Requests
    public static final int COMM_TYPE_REQUEST_PROMPT_PERMISSION = 1;
    public static final int COMM_TYPE_REQUEST_DATA = 2;

    // Responses
    public static final int COMM_TYPE_RESPONSE_PERMISSION_REQUIRED = 1001;
    public static final int COMM_TYPE_RESPONSE_USER_APPROVED_PERMISSION = 1002;
    public static final int COMM_TYPE_RESPONSE_USER_DENIED_PERMISSION = 1003;
    public static final int COMM_TYPE_RESPONSE_DATA = 1004;

    // Phone
    public static final String CAPABILITY_PHONE_APP = "phone_app_runtime_permissions";
    public static final String MESSAGE_PATH_PHONE = "/phone_message_path";

    // Wear
    public static final String CAPABILITY_WEAR_APP = "wear_app_runtime_permissions";
    public static final String MESSAGE_PATH_WEAR = "/wear_message_path";

    private Constants() {}
}