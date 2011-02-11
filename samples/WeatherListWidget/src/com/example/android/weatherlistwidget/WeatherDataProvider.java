/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.example.android.weatherlistwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import java.util.ArrayList;

/**
 * A dummy class that we are going to use internally to store weather data.  Generally, this data
 * will be stored in an external and persistent location (ie. File, Database, SharedPreferences) so
 * that the data can persist if the process is ever killed.  For simplicity, in this sample the
 * data will only be stored in memory.
 */
class WeatherDataPoint {
    String city;
    int degrees;

    WeatherDataPoint(String c, int d) {
        city = c;
        degrees = d;
    }
}

/**
 * The AppWidgetProvider for our sample weather widget.
 */
public class WeatherDataProvider extends ContentProvider {
    public static final Uri CONTENT_URI =
        Uri.parse("content://com.example.android.weatherlistwidget.provider");
    public static class Columns {
        public static final String ID = "_id";
        public static final String CITY = "city";
        public static final String TEMPERATURE = "temperature";
    }

    /**
     * Generally, this data will be stored in an external and persistent location (ie. File,
     * Database, SharedPreferences) so that the data can persist if the process is ever killed.
     * For simplicity, in this sample the data will only be stored in memory.
     */
    private static final ArrayList<WeatherDataPoint> sData = new ArrayList<WeatherDataPoint>();

    @Override
    public boolean onCreate() {
        // We are going to initialize the data provider with some default values
        sData.add(new WeatherDataPoint("San Francisco", 13));
        sData.add(new WeatherDataPoint("New York", 1));
        sData.add(new WeatherDataPoint("Seattle", 7));
        sData.add(new WeatherDataPoint("Boston", 4));
        sData.add(new WeatherDataPoint("Miami", 22));
        sData.add(new WeatherDataPoint("Toronto", -10));
        sData.add(new WeatherDataPoint("Calgary", -13));
        sData.add(new WeatherDataPoint("Tokyo", 8));
        sData.add(new WeatherDataPoint("Kyoto", 11));
        sData.add(new WeatherDataPoint("London", -1));
        sData.add(new WeatherDataPoint("Nomanisan", 27));
        return true;
    }

    @Override
    public synchronized Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        assert(uri.getPathSegments().isEmpty());

        // In this sample, we only query without any parameters, so we can just return a cursor to
        // all the weather data.
        final MatrixCursor c = new MatrixCursor(
                new String[]{ Columns.ID, Columns.CITY, Columns.TEMPERATURE });
        for (int i = 0; i < sData.size(); ++i) {
            final WeatherDataPoint data = sData.get(i);
            c.addRow(new Object[]{ new Integer(i), data.city, new Integer(data.degrees) });
        }
        return c;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.dir/vnd.weatherlistwidget.citytemperature";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // This example code does not support inserting
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // This example code does not support deleting
        return 0;
    }

    @Override
    public synchronized int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        assert(uri.getPathSegments().size() == 1);

        // In this sample, we only update the content provider individually for each row with new
        // temperature values.
        final int index = Integer.parseInt(uri.getPathSegments().get(0));
        final MatrixCursor c = new MatrixCursor(
                new String[]{ Columns.ID, Columns.CITY, Columns.TEMPERATURE });
        assert(0 <= index && index < sData.size());
        final WeatherDataPoint data = sData.get(index);
        data.degrees = values.getAsInteger(Columns.TEMPERATURE);

        // Notify any listeners that the data backing the content provider has changed, and return
        // the number of rows affected.
        getContext().getContentResolver().notifyChange(uri, null);
        return 1;
    }

}