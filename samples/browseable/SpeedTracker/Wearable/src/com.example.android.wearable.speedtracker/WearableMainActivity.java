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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.wearable.speedtracker.common.Constants;
import com.example.android.wearable.speedtracker.common.LocationEntry;
import com.example.android.wearable.speedtracker.ui.LocationSettingActivity;

import java.util.Calendar;

/**
 * The main activity for the wearable app. User can pick a speed limit, and after this activity
 * obtains a fix on the GPS, it starts reporting the speed. In addition to showing the current
 * speed, if user's speed gets close to the selected speed limit, the color of speed turns yellow
 * and if the user exceeds the speed limit, it will turn red. In order to show the user that GPS
 * location data is coming in, a small green dot keeps on blinking while GPS data is available.
 */
public class WearableMainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "WearableActivity";

    private static final long UPDATE_INTERVAL_MS = 5 * 1000;
    private static final long FASTEST_INTERVAL_MS = 5 * 1000;

    public static final float MPH_IN_METERS_PER_SECOND = 2.23694f;

    public static final String PREFS_SPEED_LIMIT_KEY = "speed_limit";
    public static final int SPEED_LIMIT_DEFAULT_MPH = 45;
    private static final long INDICATOR_DOT_FADE_AWAY_MS = 500L;

    private GoogleApiClient mGoogleApiClient;
    private TextView mSpeedLimitText;
    private TextView mCurrentSpeedText;
    private ImageView mSaveImageView;
    private TextView mAcquiringGps;
    private TextView mCurrentSpeedMphText;

    private int mCurrentSpeedLimit;
    private float mCurrentSpeed;
    private View mDot;
    private Handler mHandler = new Handler();
    private Calendar mCalendar;
    private boolean mSaveGpsLocation;

    private enum SpeedState {
        BELOW(R.color.speed_below), CLOSE(R.color.speed_close), ABOVE(R.color.speed_above);

        private int mColor;

        SpeedState(int color) {
            mColor = color;
        }

        int getColor() {
            return mColor;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);
        if (!hasGps()) {
            // If this hardware doesn't support GPS, we prefer to exit.
            // Note that when such device is connected to a phone with GPS capabilities, the
            // framework automatically routes the location requests to the phone. For this
            // application, this would not be desirable so we exit the app but for some other
            // applications, that might be a valid scenario.
            Log.w(TAG, "This hardware doesn't have GPS, so we exit");
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.gps_not_available))
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                            dialog.cancel();
                        }
                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            dialog.cancel();
                            finish();
                        }
                    })
                    .setCancelable(false)
                    .create()
                    .show();
        }

        setupViews();
        updateSpeedVisibility(false);
        setSpeedLimit();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    private void setupViews() {
        mSpeedLimitText = (TextView) findViewById(R.id.max_speed_text);
        mCurrentSpeedText = (TextView) findViewById(R.id.current_speed_text);
        mSaveImageView = (ImageView) findViewById(R.id.saving);
        ImageButton settingButton = (ImageButton) findViewById(R.id.settings);
        mAcquiringGps = (TextView) findViewById(R.id.acquiring_gps);
        mCurrentSpeedMphText = (TextView) findViewById(R.id.current_speed_mph);
        mDot = findViewById(R.id.dot);

        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent speedIntent = new Intent(WearableMainActivity.this,
                        SpeedPickerActivity.class);
                startActivity(speedIntent);
            }
        });

        mSaveImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent savingIntent = new Intent(WearableMainActivity.this,
                        LocationSettingActivity.class);
                startActivity(savingIntent);
            }
        });
    }

    private void setSpeedLimit(int speedLimit) {
        mSpeedLimitText.setText(getString(R.string.speed_limit, speedLimit));
    }

    private void setSpeedLimit() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        mCurrentSpeedLimit = pref.getInt(PREFS_SPEED_LIMIT_KEY, SPEED_LIMIT_DEFAULT_MPH);
        setSpeedLimit(mCurrentSpeedLimit);
    }

    private void setCurrentSpeed(float speed) {
        mCurrentSpeed = speed;
        mCurrentSpeedText.setText(String.format(getString(R.string.speed_format), speed));
        adjustColor();
    }

    /**
     * Adjusts the color of the speed based on its value relative to the speed limit.
     */
    private void adjustColor() {
        SpeedState state = SpeedState.ABOVE;
        if (mCurrentSpeed <= mCurrentSpeedLimit - 5) {
            state = SpeedState.BELOW;
        } else if (mCurrentSpeed <= mCurrentSpeedLimit) {
            state = SpeedState.CLOSE;
        }

        mCurrentSpeedText.setTextColor(getResources().getColor(state.getColor()));
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL_MS)
                .setFastestInterval(FASTEST_INTERVAL_MS);

        LocationServices.FusedLocationApi
                .requestLocationUpdates(mGoogleApiClient, locationRequest, this)
                .setResultCallback(new ResultCallback<Status>() {

                    @Override
                    public void onResult(Status status) {
                        if (status.getStatus().isSuccess()) {
                            if (Log.isLoggable(TAG, Log.DEBUG)) {
                                Log.d(TAG, "Successfully requested location updates");
                            }
                        } else {
                            Log.e(TAG,
                                    "Failed in requesting location updates, "
                                            + "status code: "
                                            + status.getStatusCode() + ", message: " + status
                                            .getStatusMessage());
                        }
                    }
                });
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnectionSuspended(): connection to location client suspended");
        }
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed(): connection to location client failed");
    }

    @Override
    public void onLocationChanged(Location location) {
        updateSpeedVisibility(true);
        setCurrentSpeed(location.getSpeed() * MPH_IN_METERS_PER_SECOND);
        flashDot();
        addLocationEntry(location.getLatitude(), location.getLongitude());
    }

    /**
     * Causes the (green) dot blinks when new GPS location data is acquired.
     */
    private void flashDot() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mDot.setVisibility(View.VISIBLE);
            }
        });
        mDot.setVisibility(View.VISIBLE);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mDot.setVisibility(View.INVISIBLE);
            }
        }, INDICATOR_DOT_FADE_AWAY_MS);
    }

    /**
     * Adjusts the visibility of speed indicator based on the arrival of GPS data.
     */
    private void updateSpeedVisibility(boolean speedVisible) {
        if (speedVisible) {
            mAcquiringGps.setVisibility(View.GONE);
            mCurrentSpeedText.setVisibility(View.VISIBLE);
            mCurrentSpeedMphText.setVisibility(View.VISIBLE);
        } else {
            mAcquiringGps.setVisibility(View.VISIBLE);
            mCurrentSpeedText.setVisibility(View.GONE);
            mCurrentSpeedMphText.setVisibility(View.GONE);
        }
    }

    /**
     * Adds a data item to the data Layer storage
     */
    private void addLocationEntry(double latitude, double longitude) {
        if (!mSaveGpsLocation || !mGoogleApiClient.isConnected()) {
            return;
        }
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        LocationEntry entry = new LocationEntry(mCalendar, latitude, longitude);
        String path = Constants.PATH + "/" + mCalendar.getTimeInMillis();
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(path);
        putDataMapRequest.getDataMap().putDouble(Constants.KEY_LATITUDE, entry.latitude);
        putDataMapRequest.getDataMap().putDouble(Constants.KEY_LONGITUDE, entry.longitude);
        putDataMapRequest.getDataMap()
                .putLong(Constants.KEY_TIME, entry.calendar.getTimeInMillis());
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        if (!dataItemResult.getStatus().isSuccess()) {
                            Log.e(TAG, "AddPoint:onClick(): Failed to set the data, "
                                    + "status: " + dataItemResult.getStatus()
                                    .getStatusCode());
                        }
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mCalendar = Calendar.getInstance();
        setSpeedLimit();
        adjustColor();
        updateRecordingIcon();
    }

    private void updateRecordingIcon() {
        mSaveGpsLocation = LocationSettingActivity.getGpsRecordingStatusFromPreferences(this);
        mSaveImageView.setImageResource(mSaveGpsLocation ? R.drawable.ic_gps_saving_grey600_96dp
                : R.drawable.ic_gps_not_saving_grey600_96dp);
    }

    /**
     * Returns {@code true} if this device has the GPS capabilities.
     */
    private boolean hasGps() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }
}
