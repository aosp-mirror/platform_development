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

package com.example.android.wearable.synchronizednotifications;

import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.android.wearable.synchronizednotifications.common.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * A simple fragment that presents three buttons that would trigger three different combinations of
 * notifications on the handset and the watch:
 * <ul>
 * <li>The first button builds a simple local-only notification on the handset.</li>
 * <li>The second one creates a wearable-only notification by putting a data item in the shared data
 * store and having a {@link com.google.android.gms.wearable.WearableListenerService} listen for
 * that on the wearable</li>
 * <li>The third one creates a local notification and a wearable notification by combining the above
 * two. It, however, demonstrates how one can set things up so that the dismissal of one
 * notification results in the dismissal of the other one.</li>
 * </ul>
 */
public class SynchronizedNotificationsFragment extends Fragment
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "SynchronizedNotificationsFragment";
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGoogleApiClient = new GoogleApiClient.Builder(this.getActivity())
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btn_phone_only:
                buildLocalOnlyNotification(getString(R.string.phone_only), now(),
                        Constants.PHONE_ONLY_ID, false);
                return true;
            case R.id.btn_wear_only:
                buildWearableOnlyNotification(getString(R.string.wear_only), now(),
                        Constants.WATCH_ONLY_PATH);
                return true;
            case R.id.btn_different:
                buildMirroredNotifications(getString(R.string.phone_both), getString(R.string.watch_both), now());
                return true;
        }
        return false;
    }

    /**
     * Builds a local-only notification for the handset. This is achieved by using
     * <code>setLocalOnly(true)</code>. If <code>withDismissal</code> is set to <code>true</code>, a
     * {@link android.app.PendingIntent} will be added to handle the dismissal of notification to
     * be able to remove the mirrored notification on the wearable.
     */
    private void buildLocalOnlyNotification(String title, String content, int notificationId,
                                            boolean withDismissal) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this.getActivity());
        builder.setContentTitle(title)
                .setContentText(content)
                .setLocalOnly(true)
                .setSmallIcon(R.drawable.ic_launcher);

        if (withDismissal) {
            Intent dismissIntent = new Intent(Constants.ACTION_DISMISS);
            dismissIntent.putExtra(Constants.KEY_NOTIFICATION_ID, Constants.BOTH_ID);
            PendingIntent pendingIntent = PendingIntent
                    .getService(this.getActivity(), 0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setDeleteIntent(pendingIntent);
        }
        NotificationManagerCompat.from(this.getActivity()).notify(notificationId, builder.build());
    }

    /**
     * Builds a DataItem that on the wearable will be interpreted as a request to show a
     * notification. The result will be a notification that only shows up on the wearable.
     */
    private void buildWearableOnlyNotification(String title, String content, String path) {
        if (mGoogleApiClient.isConnected()) {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(path);
            putDataMapRequest.getDataMap().putString(Constants.KEY_CONTENT, content);
            putDataMapRequest.getDataMap().putString(Constants.KEY_TITLE, title);
            PutDataRequest request = putDataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            if (!dataItemResult.getStatus().isSuccess()) {
                                Log.e(TAG, "buildWatchOnlyNotification(): Failed to set the data, "
                                        + "status: " + dataItemResult.getStatus().getStatusCode());
                            }
                        }
                    });
        } else {
            Log.e(TAG, "buildWearableOnlyNotification(): no Google API Client connection");
        }
    }

    /**
     * Builds a local notification and sets a DataItem that will be interpreted by the wearable as
     * a request to build a notification on the wearable as as well. The two notifications show
     * different messages.
     * Dismissing either of the notifications will result in dismissal of the other; this is
     * achieved by creating a {@link android.app.PendingIntent} that results in removal of
     * the DataItem that created the watch notification. The deletion of the DataItem is observed on
     * both sides, using WearableListenerService callbacks, and is interpreted on each side as a
     * request to dismiss the corresponding notification.
     */
    private void buildMirroredNotifications(String phoneTitle, String watchTitle, String content) {
        if (mGoogleApiClient.isConnected()) {
            // Wearable notification
            buildWearableOnlyNotification(watchTitle, content, Constants.BOTH_PATH);

            // Local notification, with a pending intent for dismissal
            buildLocalOnlyNotification(phoneTitle, content, Constants.BOTH_ID, true);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Failed to connect to Google API Client");
    }

    /**
     * Returns a string built from the current time
     */
    private String now() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

}
