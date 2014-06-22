package com.example.android.wearable.agendadata;

import static com.example.android.wearable.agendadata.Constants.TAG;
import static com.example.android.wearable.agendadata.Constants.EXTRA_SILENT;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.wearable.activity.ConfirmationActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

/**
 * Handles "Delete" button on calendar event card.
 */
public class DeleteService extends IntentService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    /* Timeout for making a connection to GoogleApiClient (in milliseconds) */
    private static final long TIME_OUT = 100;

    private GoogleApiClient mGoogleApiClient;

    public DeleteService() {
        super(DeleteService.class.getSimpleName());
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
        mGoogleApiClient.blockingConnect(TIME_OUT, TimeUnit.MILLISECONDS);
        Uri dataItemUri = intent.getData();
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "DeleteService.onHandleIntent=" + dataItemUri);
        }
        if (mGoogleApiClient.isConnected()) {
            DataApi.DeleteDataItemsResult result = Wearable.DataApi
                    .deleteDataItems(mGoogleApiClient, dataItemUri).await();
            if (result.getStatus().isSuccess() && !intent.getBooleanExtra(EXTRA_SILENT, false)) {
                // Show the success animation on the watch unless Silent extra is true.
                startConfirmationActivity(ConfirmationActivity.SUCCESS_ANIMATION,
                                          getString(R.string.delete_successful));
            } else {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "DeleteService.onHandleIntent: Failed to delete dataItem:"
                            + dataItemUri);
                }
                // Show the failure animation on the watch unless Silent extra is true.
                if (!intent.getBooleanExtra(EXTRA_SILENT, false)) {
                    startConfirmationActivity(ConfirmationActivity.FAILURE_ANIMATION,
                                              getString(R.string.delete_unsuccessful));
                }
            }
        } else {
            Log.e(TAG, "Failed to delete data item: " + dataItemUri
                    + " - Client disconnected from Google Play Services");
            // Show the failure animation on the watch unless Silent extra is true.
            if (!intent.getBooleanExtra(EXTRA_SILENT, false)) {
                startConfirmationActivity(ConfirmationActivity.FAILURE_ANIMATION,
                        getString(R.string.delete_unsuccessful));
            }
        }
        mGoogleApiClient.disconnect();
    }

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