/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.wearable.geofencing;

/** Constants used in wearable app. */
public final class Constants {

    private Constants() {
    }

    public static final String TAG = "ExampleGeofencingApp";

    // Timeout for making a connection to GoogleApiClient (in milliseconds).
    public static final long CONNECTION_TIME_OUT_MS = 100;

    public static final int NOTIFICATION_ID = 1;
    public static final String ANDROID_BUILDING_ID = "1";
    public static final String YERBA_BUENA_ID = "2";

    public static final String ACTION_CHECK_IN = "check_in";
    public static final String ACTION_DELETE_DATA_ITEM = "delete_data_item";
    public static final String KEY_GEOFENCE_ID = "geofence_id";

}
