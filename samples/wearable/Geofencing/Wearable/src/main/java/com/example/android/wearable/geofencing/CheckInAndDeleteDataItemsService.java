package com.example.android.wearable.geofencing;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.Wearable;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.wearable.activity.ConfirmationActivity;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import static com.example.android.wearable.geofencing.Constants.ACTION_CHECK_IN;
import static com.example.android.wearable.geofencing.Constants.ACTION_DELETE_DATA_ITEM;
import static com.example.android.wearable.geofencing.Constants.CONNECTION_TIME_OUT_MS;
import static com.example.android.wearable.geofencing.Constants.NOTIFICATION_ID;
import static com.example.android.wearable.geofencing.Constants.TAG;

/**
 * Handles "Check In" action on the location-based notification. Also deletes orphan DataItems
 * when a notification is dismissed from the wearable.
 */
public class CheckInAndDeleteDataItemsService extends IntentService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;

    public CheckInAndDeleteDataItemsService() {
        super(CheckInAndDeleteDataItemsService.class.getSimpleName());
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

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ACTION_CHECK_IN.equals(intent.getAction())) {
            // In a real app, code for checking in would go here. For this sample, we will simply
            // display a success animation.
            startConfirmationActivity(ConfirmationActivity.SUCCESS_ANIMATION,
                    getString(R.string.check_in_success));
            // Dismiss the check-in notification.
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
        } else if (!ACTION_DELETE_DATA_ITEM.equals(intent.getAction())) {
            // The only possible actions should be checking in or dismissing the notification
            // (which causes an intent with ACTION_DELETE_DATA_ITEM).
            Log.e(TAG, "Unrecognized action: " + intent.getAction());
            return;
        }
        // Regardless of the action, delete the DataItem (we are only be handling intents
        // if the notification is dismissed or if the user has chosen to check in, either of which
        // would be completed at this point).
        mGoogleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
        Uri dataItemUri = intent.getData();
        if (mGoogleApiClient.isConnected()) {
            DataApi.DeleteDataItemsResult result = Wearable.DataApi
                    .deleteDataItems(mGoogleApiClient, dataItemUri).await();
            if (!result.getStatus().isSuccess()) {
                Log.e(TAG, "CheckInAndDeleteDataItemsService.onHandleIntent: "
                         + "Failed to delete dataItem: " + dataItemUri);
            } else if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Successfully deleted data item: " + dataItemUri);
            }
        } else {
            Log.e(TAG, "Failed to delete data item: " + dataItemUri
                     + " - Client disconnected from Google Play Services");
        }
        mGoogleApiClient.disconnect();
    }

    /**
     * Helper method to create confirmation animations on the wearable.
     * @param animationType Defined by constants in ConfirmationActivity.
     * @param message The message to display with the animation.
     */
    private void startConfirmationActivity(int animationType, String message) {
        Intent confirmationActivity = new Intent(this, ConfirmationActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, animationType)
                .putExtra(ConfirmationActivity.EXTRA_MESSAGE, message);
        startActivity(confirmationActivity);
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