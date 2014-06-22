package com.example.android.wearable.geofencing;

import static com.example.android.wearable.geofencing.Constants.CONNECTION_TIME_OUT_MS;
import static com.example.android.wearable.geofencing.Constants.GEOFENCE_DATA_ITEM_PATH;
import static com.example.android.wearable.geofencing.Constants.GEOFENCE_DATA_ITEM_URI;
import static com.example.android.wearable.geofencing.Constants.KEY_GEOFENCE_ID;
import static com.example.android.wearable.geofencing.Constants.TAG;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

/**
 * Listens for geofence transition changes.
 */
public class GeofenceTransitionsIntentService extends IntentService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;

    public GeofenceTransitionsIntentService() {
        super(GeofenceTransitionsIntentService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    /**
     * Handles incoming intents.
     * @param intent The Intent sent by Location Services. This Intent is provided to Location
     *               Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        // First check for errors.
        if (LocationClient.hasError(intent)) {
            int errorCode = LocationClient.getErrorCode(intent);
            Log.e(TAG, "Location Services error: " + errorCode);
        } else {
            // Get the type of geofence transition (i.e. enter or exit in this sample).
            int transitionType = LocationClient.getGeofenceTransition(intent);
            // Create a DataItem when a user enters one of the geofences. The wearable app will
            // receive this and create a notification to prompt him/her to check in.
            if (Geofence.GEOFENCE_TRANSITION_ENTER == transitionType) {
                // Connect to the Google Api service in preparation for sending a DataItem.
                mGoogleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                // Get the geofence id triggered. Note that only one geofence can be triggered at a
                // time in this example, but in some cases you might want to consider the full list
                // of geofences triggered.
                String triggeredGeofenceId = LocationClient.getTriggeringGeofences(intent).get(0)
                        .getRequestId();
                // Create a DataItem with this geofence's id. The wearable can use this to create
                // a notification.
                final PutDataMapRequest putDataMapRequest =
                        PutDataMapRequest.create(GEOFENCE_DATA_ITEM_PATH);
                putDataMapRequest.getDataMap().putString(KEY_GEOFENCE_ID, triggeredGeofenceId);
                if (mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.putDataItem(
                        mGoogleApiClient, putDataMapRequest.asPutDataRequest()).await();
                } else {
                    Log.e(TAG, "Failed to send data item: " + putDataMapRequest
                             + " - Client disconnected from Google Play Services");
                }
                mGoogleApiClient.disconnect();
            } else if (Geofence.GEOFENCE_TRANSITION_EXIT == transitionType) {
                // Delete the data item when leaving a geofence region.
                mGoogleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                Wearable.DataApi.deleteDataItems(mGoogleApiClient, GEOFENCE_DATA_ITEM_URI).await();
                mGoogleApiClient.disconnect();
            }
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
    }

    @Override
    public void onConnectionSuspended(int cause) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
    }

}
