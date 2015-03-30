/*
 * Copyright 2015 Google Inc. All rights reserved.
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

package com.example.android.xyztouristattractions.service;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * A simply utility receiver used to ensure the device stays awake for the
 * duration of the work being done by
 * {@link com.example.android.xyztouristattractions.service.UtilityService}.
 */
public class UtilityReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Pass right over to UtilityService class, the wakeful receiver is
        // just needed in case the geofence is triggered while the device
        // is asleep otherwise the service may not have time to trigger the
        // notification.
        intent.setClass(context, UtilityService.class);
        intent.setAction(UtilityService.ACTION_GEOFENCE_TRIGGERED);
        startWakefulService(context, intent);
    }

}
