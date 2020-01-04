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
package foo.bar.fill;

import android.app.assist.AssistStructure;
import android.app.assist.AssistStructure.ViewNode;
import android.app.slice.Slice;
import android.app.slice.SliceSpec;
import android.content.Context;
import android.net.Uri;
import android.os.CancellationSignal;
import android.service.autofill.AutofillService;
import android.service.autofill.Dataset;
import android.service.autofill.FillCallback;
import android.service.autofill.FillContext;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveCallback;
import android.service.autofill.SaveInfo;
import android.service.autofill.SaveRequest;
import android.util.ArrayMap;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.view.inline.InlinePresentationSpec;
import android.view.inputmethod.InlineSuggestionsRequest;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import foo.bar.fill.R;

/**
 * A basic {@link AutofillService} implementation that only shows dynamic-generated datasets
 * and supports inline suggestions.
 */
public class InlineFillService extends AutofillService {

    private static final String TAG = "InlineFillService";

    /**
     * Number of datasets sent on each request - we're simple, that value is hardcoded in our DNA!
     */
    static final int NUMBER_DATASETS = 4;

    @Override
    public void onFillRequest(FillRequest request, CancellationSignal cancellationSignal,
            FillCallback callback) {
        Log.d(TAG, "onFillRequest()");

        // Find autofillable fields
        AssistStructure structure = getLatestAssistStructure(request);

        ArrayMap<String, AutofillId> fields = getAutofillableFields(structure, request.getFlags());
        Log.d(TAG, "autofillable fields:" + fields);

        if (fields.isEmpty()) {
            showMessage("Service could not figure out how to autofill this screen");
            callback.onSuccess(null);
            return;
        }

        // Create the base response
        FillResponse response = createResponse(this, fields, NUMBER_DATASETS,
                request.getInlineSuggestionsRequest());
        callback.onSuccess(response);
    }

    static FillResponse createResponse(@NonNull Context context,
            @NonNull ArrayMap<String, AutofillId> fields, int numDatasets,
            @Nullable InlineSuggestionsRequest inlineRequest) {
        String packageName = context.getPackageName();
        FillResponse.Builder response = new FillResponse.Builder();
        // 1.Add the dynamic datasets
        for (int i = 1; i <= numDatasets; i++) {
            Dataset unlockedDataset = newUnlockedDataset(fields, packageName, i);
            response.addDataset(unlockedDataset);
        }

        if (inlineRequest != null) {
            Log.d(TAG, "Found InlineSuggestionsRequest in FillRequest: " + inlineRequest);
            final int maxSuggestionsCount = Math.min(inlineRequest.getMaxSuggestionCount(),
                    NUMBER_DATASETS);
            final List<InlinePresentationSpec> presentationSpecs =
                    inlineRequest.getPresentationSpecs();
            final int specsSize = presentationSpecs.size();

            InlinePresentationSpec currentSpecs = presentationSpecs.get(0);
            for (int i = 1; i <= maxSuggestionsCount; i++) {
                if (currentSpecs == null) {
                    break;
                }

                if (i < specsSize) {
                    currentSpecs = presentationSpecs.get(i);
                }

                final Uri uri = new Uri.Builder().appendPath("BasicService-" + i).build();
                final ArrayList<String> autofillHints = new ArrayList<>();
                autofillHints.add(fields.keyAt(0));
                final Slice suggestionSlice = new Slice.Builder(uri,
                        new SliceSpec("InlineSuggestion", 1))
                        .addInt(currentSpecs.getMinSize().getWidth(), "SUBTYPE_MIN_WIDTH",
                                Collections.EMPTY_LIST)
                        .addInt(currentSpecs.getMaxSize().getWidth(), "SUBTYPE_MAX_WIDTH",
                                Collections.EMPTY_LIST)
                        .addInt(currentSpecs.getMinSize().getHeight(), "SUBTYPE_MIN_HEIGHT",
                                Collections.EMPTY_LIST)
                        .addInt(currentSpecs.getMaxSize().getHeight(), "SUBTYPE_MAX_HEIGHT",
                                Collections.EMPTY_LIST)
                        .addHints(autofillHints)
                        .build();
                response.addInlineSuggestionSlice(suggestionSlice);
            }
        }

        // 2.Add save info
        Collection<AutofillId> ids = fields.values();
        AutofillId[] requiredIds = new AutofillId[ids.size()];
        ids.toArray(requiredIds);
        response.setSaveInfo(
                // We're simple, so we're generic
                new SaveInfo.Builder(SaveInfo.SAVE_DATA_TYPE_GENERIC, requiredIds).build());

        // 3.Profit!
        return response.build();
    }

    static Dataset newUnlockedDataset(@NonNull Map<String, AutofillId> fields,
            @NonNull String packageName, int i) {
        Dataset.Builder dataset = new Dataset.Builder();
        for (Entry<String, AutofillId> field : fields.entrySet()) {
            String hint = field.getKey();
            AutofillId id = field.getValue();
            String value = hint + i;

            // We're simple - our dataset values are hardcoded as "hintN" (for example,
            // "username1", "username2") and they're displayed as such, except if they're a
            // password
            String displayValue = hint.contains("password") ? "password for #" + i : value;
            RemoteViews presentation = newDatasetPresentation(packageName, displayValue);
            dataset.setValue(id, AutofillValue.forText(value), presentation);
        }

        return dataset.build();
    }

    @Override
    public void onSaveRequest(SaveRequest request, SaveCallback callback) {
        Log.d(TAG, "onSaveRequest()");
        showMessage("Save not supported");
        callback.onSuccess();
    }

    /**
     * Parses the {@link AssistStructure} representing the activity being autofilled, and returns a
     * map of autofillable fields (represented by their autofill ids) mapped by the hint associate
     * with them.
     *
     * <p>An autofillable field is a {@link ViewNode} whose getHint(ViewNode) method.
     */
    @NonNull
    private ArrayMap<String, AutofillId> getAutofillableFields(@NonNull AssistStructure structure,
            int flags) {
        ArrayMap<String, AutofillId> fields = new ArrayMap<>();
        int nodes = structure.getWindowNodeCount();
        for (int i = 0; i < nodes; i++) {
            ViewNode node = structure.getWindowNodeAt(i).getRootViewNode();
            addAutofillableFields(fields, node, flags);
        }
        return fields;
    }

    /**
     * Adds any autofillable view from the {@link ViewNode} and its descendants to the map.
     */
    private void addAutofillableFields(@NonNull Map<String, AutofillId> fields,
            @NonNull ViewNode node, int flags) {
        int type = node.getAutofillType();
        String hint = getHint(node, flags);
        if (hint != null) {
            AutofillId id = node.getAutofillId();
            if (!fields.containsKey(hint)) {
                Log.v(TAG, "Setting hint " + hint + " on " + id);
                fields.put(hint, id);
            } else {
                Log.v(TAG, "Ignoring hint " + hint + " on " + id
                        + " because it was already set");
            }
        }
        int childrenSize = node.getChildCount();
        for (int i = 0; i < childrenSize; i++) {
            addAutofillableFields(fields, node.getChildAt(i), flags);
        }
    }

    /**
     * Gets the autofill hint associated with the given node.
     *
     * <p>By default it just return the first entry on the node's
     * {@link ViewNode#getAutofillHints() autofillHints} (when available), but subclasses could
     * extend it to use heuristics when the app developer didn't explicitly provide these hints.
     *
     */
    @Nullable
    protected String getHint(@NonNull ViewNode node, int flags) {
        String[] hints = node.getAutofillHints();
        if (hints == null) return null;

        // We're simple, we only care about the first hint
        String hint = hints[0].toLowerCase();
        return hint;
    }

    /**
     * Helper method to get the {@link AssistStructure} associated with the latest request
     * in an autofill context.
     */
    @NonNull
    private static AssistStructure getLatestAssistStructure(@NonNull FillRequest request) {
        List<FillContext> fillContexts = request.getFillContexts();
        return fillContexts.get(fillContexts.size() - 1).getStructure();
    }

    /**
     * Helper method to create a dataset presentation with the given text.
     */
    @NonNull
    private static RemoteViews newDatasetPresentation(@NonNull String packageName,
            @NonNull CharSequence text) {
        RemoteViews presentation =
                new RemoteViews(packageName, R.layout.list_item);
        presentation.setTextViewText(R.id.text, text);
        return presentation;
    }

    /**
     * Displays a toast with the given message.
     */
    private void showMessage(@NonNull CharSequence message) {
        Log.i(TAG, message.toString());
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}
