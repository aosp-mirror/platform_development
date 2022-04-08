/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.example.android.inlinefillservice;

import static android.view.autofill.AutofillManager.EXTRA_AUTHENTICATION_RESULT;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.autofill.Dataset;
import android.service.autofill.FillResponse;
import android.util.ArrayMap;
import android.view.autofill.AutofillId;
import android.view.inputmethod.InlineSuggestionsRequest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Optional;

/**
 * Activity used for autofill authentication, it simply sets the dataste upon tapping OK.
 */
// TODO(b/114236837): should display a small dialog, not take the full screen
public class AuthActivity extends Activity {

    private static final String EXTRA_DATASET = "dataset";
    private static final String EXTRA_HINTS = "hints";
    private static final String EXTRA_IDS = "ids";
    private static final String EXTRA_AUTH_DATASETS = "auth_datasets";
    private static final String EXTRA_INLINE_REQUEST = "inline_request";

    private static int sPendingIntentId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_activity);
        findViewById(R.id.yes).setOnClickListener((view) -> onYes());
        findViewById(R.id.no).setOnClickListener((view) -> onNo());
    }

    private void onYes() {
        Intent myIntent = getIntent();
        Intent replyIntent = new Intent();
        Dataset dataset = myIntent.getParcelableExtra(EXTRA_DATASET);
        if (dataset != null) {
            replyIntent.putExtra(EXTRA_AUTHENTICATION_RESULT, dataset);
        } else {
            String[] hints = myIntent.getStringArrayExtra(EXTRA_HINTS);
            Parcelable[] ids = myIntent.getParcelableArrayExtra(EXTRA_IDS);
            boolean authenticateDatasets = myIntent.getBooleanExtra(EXTRA_AUTH_DATASETS, false);
            final InlineSuggestionsRequest inlineRequest =
                    myIntent.getParcelableExtra(EXTRA_INLINE_REQUEST);
            int size = hints.length;
            ArrayMap<String, AutofillId> fields = new ArrayMap<>(size);
            for (int i = 0; i < size; i++) {
                fields.put(hints[i], (AutofillId) ids[i]);
            }
            FillResponse response =
                    InlineFillService.createResponse(this, fields, 1, authenticateDatasets,
                            Optional.ofNullable(inlineRequest));
            replyIntent.putExtra(EXTRA_AUTHENTICATION_RESULT, response);
        }
        setResult(RESULT_OK, replyIntent);
        finish();
    }

    private void onNo() {
        setResult(RESULT_CANCELED);
        finish();
    }

    public static IntentSender newIntentSenderForDataset(@NonNull Context context,
            @NonNull Dataset dataset) {
        return newIntentSender(context, dataset, null, null, false, null);
    }

    public static IntentSender newIntentSenderForResponse(@NonNull Context context,
            @NonNull String[] hints, @NonNull AutofillId[] ids, boolean authenticateDatasets,
            @Nullable InlineSuggestionsRequest inlineRequest) {
        return newIntentSender(context, null, hints, ids, authenticateDatasets, inlineRequest);
    }

    private static IntentSender newIntentSender(@NonNull Context context,
            @Nullable Dataset dataset, @Nullable String[] hints, @Nullable AutofillId[] ids,
            boolean authenticateDatasets, @Nullable InlineSuggestionsRequest inlineRequest) {
        Intent intent = new Intent(context, AuthActivity.class);
        if (dataset != null) {
            intent.putExtra(EXTRA_DATASET, dataset);
        } else {
            intent.putExtra(EXTRA_HINTS, hints);
            intent.putExtra(EXTRA_IDS, ids);
            intent.putExtra(EXTRA_AUTH_DATASETS, authenticateDatasets);
            intent.putExtra(EXTRA_INLINE_REQUEST, inlineRequest);
        }

        return PendingIntent.getActivity(context, ++sPendingIntentId, intent,
                PendingIntent.FLAG_CANCEL_CURRENT).getIntentSender();
    }
}
