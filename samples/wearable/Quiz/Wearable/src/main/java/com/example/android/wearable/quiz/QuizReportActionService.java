package com.example.android.wearable.quiz;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

import static com.example.android.wearable.quiz.Constants.CONNECT_TIMEOUT_MS;
import static com.example.android.wearable.quiz.Constants.RESET_QUIZ_PATH;

/**
 * Service to reset the quiz (by sending a message to the phone) when the Reset Quiz
 * action on the Quiz Report is selected.
 */
public class QuizReportActionService extends IntentService {
    public static final String ACTION_RESET_QUIZ = "com.example.android.wearable.quiz.RESET_QUIZ";

    private static final String TAG = "QuizReportActionReceiver";

    public QuizReportActionService() {
        super(QuizReportActionService.class.getSimpleName());
    }

    @Override
    public void onHandleIntent(Intent intent) {
        if (intent.getAction().equals(ACTION_RESET_QUIZ)) {
            GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .build();
            ConnectionResult result = googleApiClient.blockingConnect(CONNECT_TIMEOUT_MS,
                    TimeUnit.MILLISECONDS);
            if (!result.isSuccess()) {
                Log.e(TAG, "QuizListenerService failed to connect to GoogleApiClient.");
                return;
            }
            NodeApi.GetConnectedNodesResult nodes =
                    Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
            for (Node node : nodes.getNodes()) {
                Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), RESET_QUIZ_PATH,
                        new byte[0]);
            }
        }
    }
}
