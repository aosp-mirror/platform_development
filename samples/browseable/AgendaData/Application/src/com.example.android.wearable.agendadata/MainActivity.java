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

package com.example.android.wearable.agendadata;

import static com.example.android.wearable.agendadata.Constants.TAG;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

/**
 * Syncs or deletes calendar events (event time, description, and background image) to your
 * Wearable via the Wearable DataApi at the click of a button. Includes code to handle dynamic M+
 * permissions as well.
 */
public class MainActivity extends AppCompatActivity implements
        NodeApi.NodeListener,
        ConnectionCallbacks,
        OnConnectionFailedListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    /* Request code for launching the Intent to resolve Google Play services errors. */
    private static final int REQUEST_RESOLVE_ERROR = 1000;

    /* Id to identify calendar and contact permissions request. */
    private static final int REQUEST_CALENDAR_AND_CONTACTS = 0;


    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;

    private TextView mLogTextView;
    ScrollView mScroller;

    private View mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        mLayout = findViewById(R.id.main_layout);

        mLogTextView = (TextView) findViewById(R.id.log);
        mScroller = (ScrollView) findViewById(R.id.scroller);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient.isConnected()) {
            Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        }
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    public void onGetEventsClicked(View view) {

        Log.i(TAG, "onGetEventsClicked(): Checking permission.");

        // BEGIN_INCLUDE(calendar_and_contact_permissions)
        // Check if the Calendar permission is already available.
        boolean calendarApproved =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
                        == PackageManager.PERMISSION_GRANTED;

        boolean contactsApproved =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                        == PackageManager.PERMISSION_GRANTED;

        if (!calendarApproved || !contactsApproved) {
            // Calendar and/or Contact permissions have not been granted.
           requestCalendarAndContactPermissions();

        } else {
            // Calendar permissions is already available, start service
            Log.i(TAG, "Permissions already granted. Starting service.");
            pushCalendarToWear();
        }
        // END_INCLUDE(calendar_and_contact_permissions)

    }

    private void pushCalendarToWear() {
        startService(new Intent(this, CalendarQueryService.class));
    }

    /*
     * Requests Calendar and Contact permissions.
     * If the permission has been denied previously, a SnackBar will prompt the user to grant the
     * permission, otherwise it is requested directly.
     */
    private void requestCalendarAndContactPermissions() {
        Log.i(TAG, "CALENDAR permission has NOT been granted. Requesting permission.");

        // BEGIN_INCLUDE(calendar_and_contact_permissions_request)

        boolean showCalendarPermissionRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_CALENDAR);
        boolean showContactsPermissionRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_CONTACTS);

        if (showCalendarPermissionRationale || showContactsPermissionRationale) {
            /*
             * Provide an additional rationale to the user if the permission was not granted and
             * the user would benefit from additional context for the use of the permission. For
             * example, if the user has previously denied the permission.
             */
            Log.i(TAG, "Display calendar & contact permissions rationale for additional context.");

            Snackbar.make(mLayout, R.string.permissions_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[] {
                                            Manifest.permission.READ_CALENDAR,
                                            Manifest.permission.READ_CONTACTS},
                                    REQUEST_CALENDAR_AND_CONTACTS);
                        }
                    })
                    .show();


        } else {

            // Calendar/Contact permissions have not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.READ_CALENDAR,
                            Manifest.permission.READ_CONTACTS
                    },
                    REQUEST_CALENDAR_AND_CONTACTS);
        }
        // END_INCLUDE(calendar_and_contact_permissions_request)
    }



    public void onDeleteEventsClicked(View view) {
        if (mGoogleApiClient.isConnected()) {
            Wearable.DataApi.getDataItems(mGoogleApiClient)
                    .setResultCallback(new ResultCallback<DataItemBuffer>() {
                        @Override
                        public void onResult(DataItemBuffer result) {
                            try {
                                if (result.getStatus().isSuccess()) {
                                    deleteDataItems(result);
                                } else {
                                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                                        Log.d(TAG, "onDeleteEventsClicked(): failed to get Data "
                                                + "Items");
                                    }
                                }
                            } finally {
                                result.release();
                            }
                        }
                    });
        } else {
            Log.e(TAG, "Failed to delete data items"
                    + " - Client disconnected from Google Play Services");
        }
    }

    private void deleteDataItems(final DataItemBuffer dataItemList) {
        if (mGoogleApiClient.isConnected()) {
            for (final DataItem dataItem : dataItemList) {
                final Uri dataItemUri = dataItem.getUri();
                /*
                 * In a real calendar application, this might delete the corresponding calendar
                 * events from the calendar data provider. However, we simply delete the DataItem,
                 * but leave the phone's calendar data intact for this simple sample.
                 */
                Wearable.DataApi.deleteDataItems(mGoogleApiClient, dataItemUri)
                        .setResultCallback(new ResultCallback<DataApi.DeleteDataItemsResult>() {
                            @Override
                            public void onResult(DataApi.DeleteDataItemsResult deleteResult) {
                                if (deleteResult.getStatus().isSuccess()) {
                                    appendLog("Successfully deleted data item: " + dataItemUri);
                                } else {
                                    appendLog("Failed to delete data item:" + dataItemUri);
                                }
                            }
                        });
            }
        } else {
            Log.e(TAG, "Failed to delete data items"
                     + " - Client disconnected from Google Play Services");
        }
    }

    @Override
    public void onPeerConnected(Node peer) {
        appendLog("Device connected");
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        appendLog("Device disconnected");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Connected to Google Api Service.");
        }
        mResolvingError = false;
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // Ignore
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Disconnected from Google Api Service");
        }
        if (null != Wearable.NodeApi) {
            Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        }
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mResolvingError = false;
                mGoogleApiClient.connect();
            }
        } else {
            mResolvingError = false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onActivityResult request/result codes: " + requestCode + "/" + resultCode);
        }

        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            }
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onRequestPermissionsResult(): " + permissions);
        }

        if (requestCode == REQUEST_CALENDAR_AND_CONTACTS) {
            // BEGIN_INCLUDE(permissions_result)
            // Received permission result for calendar permission.
            Log.i(TAG, "Received response for Calendar permission request.");

            // Check if all required permissions have been granted.
            if ((grantResults.length == 2)
                    && (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    && (grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                // Calendar/Contact permissions have been granted, pull all calendar events
                Log.i(TAG, "All permission has now been granted. Showing preview.");
                Snackbar.make(mLayout, R.string.permisions_granted, Snackbar.LENGTH_SHORT).show();

                pushCalendarToWear();

            } else {
                Log.i(TAG, "CALENDAR and/or CONTACT permissions were NOT granted.");
                Snackbar.make(mLayout, R.string.permissions_denied, Snackbar.LENGTH_SHORT).show();
            }
            // END_INCLUDE(permissions_result)

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void appendLog(final String s) {
        mLogTextView.post(new Runnable() {
            @Override
            public void run() {
                mLogTextView.append(s);
                mLogTextView.append("\n");
                mScroller.fullScroll(View.FOCUS_DOWN);
            }
        });
    }
}
