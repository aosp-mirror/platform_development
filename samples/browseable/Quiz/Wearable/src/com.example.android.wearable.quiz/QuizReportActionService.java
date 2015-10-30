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

package com.example.android.wearable.quiz;

import static com.example.android.wearable.quiz.Constants.CONNECT_TIMEOUT_MS;
import static com.example.android.wearable.quiz.Constants.GET_CAPABILITIES_TIMEOUT_MS;
import static com.example.android.wearable.quiz.Constants.RESET_QUIZ_PATH;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service to reset the quiz (by sending a message to the phone) when the Reset Quiz
 * action on the Quiz Report is selected.
 */
public class QuizReportActionService extends IntentService {

    public static final String ACTION_RESET_QUIZ = "com.example.android.wearable.quiz.RESET_QUIZ";

    private static final String TAG = "QuizReportActionService";
    private static final String RESET_QUIZ_CAPABILITY_NAME = "reset_quiz";

    public QuizReportActionService() {
        super(QuizReportActionService.class.getSimpleName());
    }

    @Override
    public void onHandleIntent(Intent intent) {
        if (intent.getAction().equals(ACTION_RESET_QUIZ)) {
            final GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .build();
            ConnectionResult result = googleApiClient.blockingConnect(CONNECT_TIMEOUT_MS,
                    TimeUnit.MILLISECONDS);
            if (!result.isSuccess()) {
                Log.e(TAG, "QuizReportActionService failed to connect to GoogleApiClient.");
                return;
            }

            CapabilityApi.GetCapabilityResult capabilityResult = Wearable.CapabilityApi
                    .getCapability(googleApiClient, RESET_QUIZ_CAPABILITY_NAME,
                            CapabilityApi.FILTER_REACHABLE)
                    .await(GET_CAPABILITIES_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (capabilityResult.getStatus().isSuccess()) {
                sendResetMessage(googleApiClient, capabilityResult.getCapability());
            } else {
                Log.e(TAG, "Failed to get capabilities, status: "
                        + capabilityResult.getStatus().getStatusMessage());
            }
        }
    }

    private void sendResetMessage(GoogleApiClient googleApiClient, CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        if (connectedNodes.isEmpty()) {
            Log.w(TAG, "No node capable of resetting quiz was found");
        } else {
            for (Node node : connectedNodes) {
                Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), RESET_QUIZ_PATH,
                        new byte[0]);
            }
        }
    }
}
