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

package com.example.android.wearable.speedtracker.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.example.android.wearable.speedtracker.common.LocationEntry;
import com.example.android.wearable.speedtracker.common.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * A helper class to set up the database that holds the GPS location information
 */
public class LocationDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "LocationDbHelper";

    public static final String TABLE_NAME = "location";
    public static final String COLUMN_NAME_DAY = "day";
    public static final String COLUMN_NAME_LATITUDE = "lat";
    public static final String COLUMN_NAME_LONGITUDE = "lon";
    public static final String COLUMN_NAME_TIME = "time";

    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String REAL_TYPE = " REAL";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " ("
                    + BaseColumns._ID + " INTEGER PRIMARY KEY,"
                    + COLUMN_NAME_DAY + TEXT_TYPE + COMMA_SEP
                    + COLUMN_NAME_LATITUDE + REAL_TYPE + COMMA_SEP
                    + COLUMN_NAME_LONGITUDE + REAL_TYPE + COMMA_SEP
                    + COLUMN_NAME_TIME + INTEGER_TYPE
                    + " )";
    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + TABLE_NAME;

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Location.db";

    public LocationDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    /**
     * Inserts a {@link com.example.android.wearable.speedtracker.common.LocationEntry} item to the
     * database.
     */
    public final long insert(LocationEntry entry) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Inserting a LocationEntry");
        }
        // Gets the data repository in write mode
        SQLiteDatabase db = getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_DAY, entry.day);
        values.put(COLUMN_NAME_LONGITUDE, entry.longitude);
        values.put(COLUMN_NAME_LATITUDE, entry.latitude);
        values.put(COLUMN_NAME_TIME, entry.calendar.getTimeInMillis());

        // Insert the new row, returning the primary key value of the new row
        return db.insert(TABLE_NAME, "null", values);
    }

    /**
     * Returns a list of {@link com.example.android.wearable.speedtracker.common.LocationEntry}
     * objects from the database for a given day. The list can be empty (but not {@code null}) if
     * there are no such items. This method looks at the day that the calendar argument points at.
     */
    public final List<LocationEntry> read(Calendar calendar) {
        SQLiteDatabase db = getReadableDatabase();
        String[] projection = {
                COLUMN_NAME_LONGITUDE,
                COLUMN_NAME_LATITUDE,
                COLUMN_NAME_TIME
        };
        String day = Utils.getHashedDay(calendar);

        // sort ASC based on the time of the entry
        String sortOrder = COLUMN_NAME_TIME + " ASC";
        String selection = COLUMN_NAME_DAY + " LIKE ?";

        Cursor cursor = db.query(
                TABLE_NAME,                 // The table to query
                projection,                 // The columns to return
                selection,                  // The columns for the WHERE clause
                new String[]{day},          // The values for the WHERE clause
                null,                       // don't group the rows
                null,                       // don't filter by row groups
                sortOrder                   // The sort order
        );

        List<LocationEntry> result = new ArrayList<LocationEntry>();
        int count = cursor.getCount();
        if (count > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(cursor.getLong(2));
                LocationEntry entry = new LocationEntry(cal, cursor.getDouble(1),
                        cursor.getDouble(0));
                result.add(entry);
                cursor.moveToNext();
            }
        }
        cursor.close();
        return result;
    }

    /**
     * Deletes all the entries in the database for the given day. The argument {@code day} should
     * match the format provided by {@link getHashedDay()}
     */
    public final int delete(String day) {
        SQLiteDatabase db = getWritableDatabase();
        // Define 'where' part of the query.
        String selection = COLUMN_NAME_DAY + " LIKE ?";
        String[] selectionArgs = {day};
        return db.delete(TABLE_NAME, selection, selectionArgs);
    }

    /**
     * Deletes all the entries in the database for the day that the {@link java.util.Calendar}
     * argument points at.
     */
    public final int delete(Calendar calendar) {
        return delete(Utils.getHashedDay(calendar));
    }
}
