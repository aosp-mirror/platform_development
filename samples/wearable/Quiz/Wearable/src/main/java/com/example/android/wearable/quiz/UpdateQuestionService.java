package com.example.android.wearable.quiz;

import android.app.IntentService;
import android.app.NotificationManager;
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

import static com.example.android.wearable.quiz.Constants.CHOSEN_ANSWER_CORRECT;
import static com.example.android.wearable.quiz.Constants.QUESTION_INDEX;
import static com.example.android.wearable.quiz.Constants.QUESTION_WAS_ANSWERED;

/**
 * Updates quiz status on the phone when user selects an answer to a question on the watch.
 */
public class UpdateQuestionService extends IntentService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static final String EXTRA_QUESTION_CORRECT = "extra_question_correct";
    public static final String EXTRA_QUESTION_INDEX = "extra_question_index";

    private static final long TIME_OUT_MS = 100;
    private static final String TAG = "UpdateQuestionService";

    private GoogleApiClient mGoogleApiClient;

    public UpdateQuestionService() {
        super(UpdateQuestionService.class.getSimpleName());
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
        mGoogleApiClient.blockingConnect(TIME_OUT_MS, TimeUnit.MILLISECONDS);
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

        // Update quiz status variables, which will be reflected on the phone.
        int questionIndex = intent.getIntExtra(EXTRA_QUESTION_INDEX, -1);
        boolean chosenAnswerCorrect = intent.getBooleanExtra(EXTRA_QUESTION_CORRECT, false);
        dataMap.putInt(QUESTION_INDEX, questionIndex);
        dataMap.putBoolean(CHOSEN_ANSWER_CORRECT, chosenAnswerCorrect);
        dataMap.putBoolean(QUESTION_WAS_ANSWERED, true);
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request).await();

        // Remove this question notification.
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(questionIndex);
        mGoogleApiClient.disconnect();
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
