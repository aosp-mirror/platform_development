package com.example.android.wearable.quiz;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

import static com.example.android.wearable.quiz.Constants.CONNECT_TIMEOUT_MS;
import static com.example.android.wearable.quiz.Constants.QUESTION_WAS_DELETED;

/**
 * Used to update quiz status on the phone when user dismisses a question on the watch.
 */
public class DeleteQuestionService extends IntentService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "DeleteQuestionReceiver";

    private GoogleApiClient mGoogleApiClient;

    public DeleteQuestionService() {
        super(DeleteQuestionService.class.getSimpleName());
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
    public void onHandleIntent(Intent intent) {
        mGoogleApiClient.blockingConnect(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        Uri dataItemUri = intent.getData();
        if (!mGoogleApiClient.isConnected()) {
            Log.e(TAG, "Failed to update data item " + dataItemUri
                    + " because client is disconnected from Google Play Services");
            return;
        }
        DataApi.DataItemResult dataItemResult = Wearable.DataApi.getDataItem(
                mGoogleApiClient, dataItemUri).await();
        PutDataMapRequest putDataMapRequest = PutDataMapRequest
                .createFromDataMapItem(DataMapItem.fromDataItem(dataItemResult.getDataItem()));
        DataMap dataMap = putDataMapRequest.getDataMap();
        dataMap.putBoolean(QUESTION_WAS_DELETED, true);
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request).await();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }
}
