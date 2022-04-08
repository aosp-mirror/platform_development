/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.content.Context;
import android.content.IntentSender;
import android.os.CancellationSignal;
import android.service.autofill.AutofillService;
import android.service.autofill.FillCallback;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.InlinePresentation;
import android.service.autofill.SaveCallback;
import android.service.autofill.SaveInfo;
import android.service.autofill.SaveRequest;
import android.util.ArrayMap;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.view.inputmethod.InlineSuggestionsRequest;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.Optional;

/**
 * A basic {@link AutofillService} implementation that only shows dynamic-generated datasets
 * and supports inline suggestions.
 */
public class InlineFillService extends AutofillService {

    static final String TAG = "InlineFillService";

    /**
     * Number of datasets sent on each request - we're simple, that value is hardcoded in our DNA!
     */
    static final int NUMBER_DATASETS = 6;

    private final boolean mAuthenticateResponses = false;
    private final boolean mAuthenticateDatasets = false;

    @Override
    public void onFillRequest(FillRequest request, CancellationSignal cancellationSignal,
            FillCallback callback) {
        Log.d(TAG, "onFillRequest()");

        final Context context = getApplicationContext();

        // Find autofillable fields
        ArrayMap<String, AutofillId> fields = Helper.getAutofillableFields(request);
        Log.d(TAG, "autofillable fields:" + fields);
        if (fields.isEmpty()) {
            Helper.showMessage(context,
                    "InlineFillService could not figure out how to autofill this screen");
            callback.onSuccess(null);
            return;
        }
        final Optional<InlineSuggestionsRequest> inlineRequest =
                InlineRequestHelper.getInlineSuggestionsRequest(request);
        final int maxSuggestionsCount = InlineRequestHelper.getMaxSuggestionCount(inlineRequest,
                NUMBER_DATASETS);

        // Create the base response
        final FillResponse response;
        if (mAuthenticateResponses) {
            int size = fields.size();
            String[] hints = new String[size];
            AutofillId[] ids = new AutofillId[size];
            for (int i = 0; i < size; i++) {
                hints[i] = fields.keyAt(i);
                ids[i] = fields.valueAt(i);
            }
            IntentSender authentication = AuthActivity.newIntentSenderForResponse(this, hints,
                    ids, mAuthenticateDatasets, inlineRequest.orElse(null));
            RemoteViews presentation = ResponseHelper.newDatasetPresentation(getPackageName(),
                    "Tap to auth response");

            InlinePresentation inlinePresentation =
                    InlineRequestHelper.maybeCreateInlineAuthenticationResponse(context,
                            inlineRequest);
            response = new FillResponse.Builder()
                    .setAuthentication(ids, authentication, presentation, inlinePresentation)
                    .build();
        } else {
            response = createResponse(this, fields, maxSuggestionsCount, mAuthenticateDatasets,
                    inlineRequest);
        }

        callback.onSuccess(response);
    }

    static FillResponse createResponse(@NonNull Context context,
            @NonNull ArrayMap<String, AutofillId> fields, int numDatasets,
            boolean authenticateDatasets,
            @NonNull Optional<InlineSuggestionsRequest> inlineRequest) {
        String packageName = context.getPackageName();
        FillResponse.Builder response = new FillResponse.Builder();
        // 1.Add the dynamic datasets
        for (int i = 0; i < numDatasets; i++) {
            if (authenticateDatasets) {
                response.addDataset(ResponseHelper.newLockedDataset(context, fields, packageName, i,
                        inlineRequest));
            } else {
                response.addDataset(ResponseHelper.newUnlockedDataset(context, fields,
                        packageName, i, inlineRequest));
            }
        }

        // 2. Add some inline actions
        if (inlineRequest.isPresent()) {
            response.addDataset(InlineRequestHelper.createInlineActionDataset(context, fields,
                    inlineRequest.get(), R.drawable.ic_settings));
            response.addDataset(InlineRequestHelper.createInlineActionDataset(context, fields,
                    inlineRequest.get(), R.drawable.ic_settings));
        }

        // 3.Add save info
        Collection<AutofillId> ids = fields.values();
        AutofillId[] requiredIds = new AutofillId[ids.size()];
        ids.toArray(requiredIds);
        response.setSaveInfo(
                // We're simple, so we're generic
                new SaveInfo.Builder(SaveInfo.SAVE_DATA_TYPE_GENERIC, requiredIds).build());

        // 4.Profit!
        return response.build();
    }

    @Override
    public void onSaveRequest(SaveRequest request, SaveCallback callback) {
        Log.d(TAG, "onSaveRequest()");
        Helper.showMessage(getApplicationContext(), "InlineFillService doesn't support Save");
        callback.onSuccess();
    }
}
