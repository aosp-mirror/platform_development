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

import com.example.android.wearable.speedtracker.common.LocationEntry;
import com.example.android.wearable.speedtracker.common.Utils;
import com.example.android.wearable.speedtracker.db.LocationDbHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class that wraps database access and provides a cache for various GPS data.
 */
public class LocationDataManager {

    private final Map<String, List<LocationEntry>> mPointsMap =
            new HashMap<String, List<LocationEntry>>();

    private LocationDbHelper mDbHelper;

    public LocationDataManager(LocationDbHelper dbHelper) {
        mDbHelper = dbHelper;
    }

    /**
     * Returns a list of {@link com.example.android.wearable.speedtracker.common.LocationEntry}
     * objects for the day that the {@link java.util.Calendar} object points at. Internally it uses
     * a cache to speed up subsequent calls. If there is no cached value, it gets the result from
     * the database.
     */
    public final List<LocationEntry> getPoints(Calendar calendar) {
        String day = Utils.getHashedDay(calendar);
        synchronized (mPointsMap) {
            if (mPointsMap.get(day) == null) {
                // there is no cache for this day, so lets get it from DB
                List<LocationEntry> points = mDbHelper.read(calendar);
                mPointsMap.put(day, points);
            }
        }
        return mPointsMap.get(day);
    }

    /**
     * Clears the data for the day that the {@link java.util.Calendar} object falls on. This method
     * removes the entries from the database and updates the cache accordingly.
     */
    public final int clearPoints(Calendar calendar) {
        synchronized (mPointsMap) {
            String day = Utils.getHashedDay(calendar);
            mPointsMap.remove(day);
            return mDbHelper.delete(day);
        }
    }

    /**
     * Adds a {@link com.example.android.wearable.speedtracker.common.LocationEntry} point to the
     * database and cache if it is a new point.
     */
    public final void addPoint(LocationEntry entry) {
        synchronized (mPointsMap) {
            List<LocationEntry> points = getPoints(entry.calendar);
            if (points == null || points.isEmpty()) {
                mDbHelper.insert(entry);
                if (points == null) {
                    points = new ArrayList<LocationEntry>();
                }
                points.add(entry);
                mPointsMap.put(entry.day, points);
            } else {
                if (!points.contains(entry)) {
                    mDbHelper.insert(entry);
                    points.add(entry);
                }
            }
        }
    }
}
