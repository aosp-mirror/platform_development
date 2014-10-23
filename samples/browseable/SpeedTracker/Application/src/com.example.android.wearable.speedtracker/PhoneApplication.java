/*
 * Copyright (C) 2014 Google Inc. All Rights Reserved.
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

package com.example.android.wearable.speedtracker;

import android.app.Application;

import com.example.android.wearable.speedtracker.db.LocationDbHelper;

/**
 * The {@link android.app.Application} class for the handset app.
 */
public class PhoneApplication extends Application {

    private LocationDataManager mDataManager;

    @Override
    public void onCreate() {
        super.onCreate();
        LocationDbHelper dbHelper = new LocationDbHelper(getApplicationContext());
        mDataManager = new LocationDataManager(dbHelper);
    }

    /**
     * Returns an instance of {@link com.example.android.wearable.speedtracker.LocationDataManager}.
     */
    public final LocationDataManager getDataManager() {
        return mDataManager;
    }
}
