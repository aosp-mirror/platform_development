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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;

import com.example.android.wearable.speedtracker.common.LocationEntry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * The main activity for the handset application. When a watch device reconnects to the handset
 * app, the collected GPS data on the watch, if any, is synced up and user can see his/her track on
 * a map. This data is then saved into an internal database and the corresponding data items are
 * deleted.
 */
public class PhoneMainActivity extends Activity implements DatePickerDialog.OnDateSetListener {

    private static final String TAG = "PhoneMainActivity";
    private static final int BOUNDING_BOX_PADDING_PX = 50;
    private TextView mSelectedDateText;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        mSelectedDateText = (TextView) findViewById(R.id.selected_date);
        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
    }

    public void onClick(View view) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        new DatePickerDialog(PhoneMainActivity.this, PhoneMainActivity.this,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        // the following if-clause is to get around a bug that causes this callback to be called
        // twice
        if (view.isShown()) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, monthOfYear);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            String date = DateUtils.formatDateTime(this, calendar.getTimeInMillis(),
                    DateUtils.FORMAT_SHOW_DATE);
            mSelectedDateText.setText(getString(R.string.showing_for_date, date));
            showTrack(calendar);
        }
    }

    /**
     * An {@link android.os.AsyncTask} that is responsible for getting a list of {@link
     * com.example.android.wearable.speedtracker.common.LocationEntry} objects for a given day and
     * building a track from those points. In addition, it sets the smallest bounding box for the
     * map that covers all the points on the track.
     */
    private void showTrack(Calendar calendar) {
        new AsyncTask<Calendar, Void, Void>() {

            private List<LatLng> coordinates;
            private LatLngBounds bounds;

            @Override
            protected Void doInBackground(Calendar... params) {
                LocationDataManager dataManager = ((PhoneApplication) getApplicationContext())
                        .getDataManager();
                List<LocationEntry> entries = dataManager.getPoints(params[0]);
                if (entries != null && !entries.isEmpty()) {
                    coordinates = new ArrayList<LatLng>();
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (LocationEntry entry : entries) {
                        LatLng latLng = new LatLng(entry.latitude, entry.longitude);
                        builder.include(latLng);
                        coordinates.add(latLng);
                    }
                    bounds = builder.build();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                mMap.clear();
                if (coordinates == null || coordinates.isEmpty()) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "No Entries found for that date");
                    }
                } else {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds,
                            BOUNDING_BOX_PADDING_PX));
                    mMap.addPolyline(new PolylineOptions().geodesic(true).addAll(coordinates));
                }
            }

        }.execute(calendar);
    }
}
