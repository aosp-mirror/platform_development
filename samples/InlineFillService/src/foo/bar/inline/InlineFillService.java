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
package foo.bar.inline;

import android.app.PendingIntent;
import android.app.assist.AssistStructure;
import android.app.assist.AssistStructure.ViewNode;
import android.app.slice.Slice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.drawable.Icon;
import android.os.CancellationSignal;
import android.service.autofill.AutofillService;
import android.service.autofill.Dataset;
import android.service.autofill.FillCallback;
import android.service.autofill.FillContext;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.InlinePresentation;
import android.service.autofill.SaveCallback;
import android.service.autofill.SaveInfo;
import android.service.autofill.SaveRequest;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.inline.InlinePresentationSpec;
import android.view.inputmethod.InlineSuggestionsRequest;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.autofill.InlinePresentationBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A basic {@link AutofillService} implementation that only shows dynamic-generated datasets
 * and supports inline suggestions.
 */
public class InlineFillService extends AutofillService {

    private static final String TAG = "InlineFillService";

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

        // Find autofillable fields
        AssistStructure structure = getLatestAssistStructure(request);

        ArrayMap<String, AutofillId> fields = getAutofillableFields(structure);
        Log.d(TAG, "autofillable fields:" + fields);

        if (fields.isEmpty()) {
            showMessage("Service could not figure out how to autofill this screen");
            callback.onSuccess(null);
            return;
        }

        final InlineSuggestionsRequest inlineRequest = request.getInlineSuggestionsRequest();
        final int maxSuggestionsCount = inlineRequest == null
                ? NUMBER_DATASETS
                : Math.min(inlineRequest.getMaxSuggestionCount(), NUMBER_DATASETS);

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
                    ids, mAuthenticateDatasets, inlineRequest);
            RemoteViews presentation = newDatasetPresentation(getPackageName(),
                    "Tap to auth response");

            final InlinePresentation inlinePresentation;
            if (inlineRequest != null) {
                final Slice authSlice = new InlinePresentationBuilder("Tap to auth respones")
                        .build();
                final List<InlinePresentationSpec> specs = inlineRequest.getInlinePresentationSpecs();
                final int specsSize = specs.size();
                final InlinePresentationSpec currentSpec = specsSize > 0 ? specs.get(0) : null;
                inlinePresentation = new InlinePresentation(authSlice, currentSpec,
                        /* pined= */ false);
            } else {
                inlinePresentation = null;
            }

            response = new FillResponse.Builder()
                    .setAuthentication(ids, authentication, presentation, inlinePresentation)
                    .build();
        } else {
            response = createResponse(this, fields, maxSuggestionsCount, mAuthenticateDatasets,
                    request.getInlineSuggestionsRequest());
        }

        callback.onSuccess(response);
    }

    static FillResponse createResponse(@NonNull Context context,
            @NonNull ArrayMap<String, AutofillId> fields, int numDatasets,
            boolean authenticateDatasets, @Nullable InlineSuggestionsRequest inlineRequest) {
        String packageName = context.getPackageName();
        FillResponse.Builder response = new FillResponse.Builder();
        // 1.Add the dynamic datasets
        for (int i = 1; i <= numDatasets; i++) {
            Dataset unlockedDataset = newUnlockedDataset(context, fields, packageName, i,
                    inlineRequest);
            if (authenticateDatasets) {
                Dataset.Builder lockedDataset = new Dataset.Builder();
                for (Entry<String, AutofillId> field : fields.entrySet()) {
                    String hint = field.getKey();
                    AutofillId id = field.getValue();
                    String value = i + "-" + hint;
                    IntentSender authentication =
                            AuthActivity.newIntentSenderForDataset(context, unlockedDataset);
                    RemoteViews presentation = newDatasetPresentation(packageName,
                            "Tap to auth " + value);

                    final InlinePresentation inlinePresentation;
                    if (inlineRequest != null) {
                        final Slice authSlice = new InlinePresentationBuilder(
                                "Tap to auth " + value).build();
                        final List<InlinePresentationSpec> specs
                                = inlineRequest.getInlinePresentationSpecs();
                        final int specsSize = specs.size();
                        final InlinePresentationSpec currentSpec =
                                specsSize > 0 ? specs.get(0) : null;
                        inlinePresentation = new InlinePresentation(authSlice, currentSpec,
                                /* pined= */ false);
                        lockedDataset.setValue(id, null, presentation, inlinePresentation)
                                .setAuthentication(authentication);
                    } else {
                        lockedDataset.setValue(id, null, presentation)
                                .setAuthentication(authentication);
                    }
                }
                response.addDataset(lockedDataset.build());
            } else {
                response.addDataset(unlockedDataset);
            }
        }

        if (inlineRequest != null) {
            // Reuse the first spec's height for the inline action size, as there isn't dedicated
            // value from the request for this.
            final int height = inlineRequest.getInlinePresentationSpecs().get(0)
                    .getMinSize().getHeight();
            final Size actionIconSize = new Size(height, height);
            response.addDataset(
                    newInlineActionDataset(context, actionIconSize, R.drawable.ic_settings,
                            fields));
            response.addDataset(
                    newInlineActionDataset(context, actionIconSize, R.drawable.ic_settings,
                            fields));
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

    static Dataset newInlineActionDataset(@NonNull Context context,
            @NonNull Size size, int drawable, ArrayMap<String, AutofillId> fields) {
        Intent intent = new Intent().setComponent(
                new ComponentName(context.getPackageName(), SettingsActivity.class.getName()));
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        final Slice suggestionSlice = new InlinePresentationBuilder()
                .setStartIcon(Icon.createWithResource(context, drawable))
                .setAttribution(pendingIntent)
                .build();
        final InlinePresentationSpec currentSpec = new InlinePresentationSpec.Builder(size,
                size).build();
        Dataset.Builder builder = new Dataset.Builder()
                .setInlinePresentation(
                        new InlinePresentation(suggestionSlice, currentSpec, /** pined= */true))
                .setAuthentication(pendingIntent.getIntentSender());
        for (AutofillId fieldId : fields.values()) {
            builder.setValue(fieldId, null);
        }
        return builder.build();
    }

    static Dataset newUnlockedDataset(@NonNull Context context,
            @NonNull Map<String, AutofillId> fields, @NonNull String packageName, int i,
            @Nullable InlineSuggestionsRequest inlineRequest) {

        Dataset.Builder dataset = new Dataset.Builder();
        for (Entry<String, AutofillId> field : fields.entrySet()) {
            final String hint = field.getKey();
            final AutofillId id = field.getValue();
            final String value = hint + i;

            // We're simple - our dataset values are hardcoded as "hintN" (for example,
            // "username1", "username2") and they're displayed as such, except if they're a
            // password
            final String displayValue = hint.contains("password") ? "password for #" + i : value;
            final RemoteViews presentation = newDatasetPresentation(packageName, displayValue);

            // Add Inline Suggestion required info.
            if (inlineRequest != null) {
                Log.d(TAG, "Found InlineSuggestionsRequest in FillRequest: " + inlineRequest);

                Intent intent = new Intent().setComponent(
                        new ComponentName(context.getPackageName(), SettingsActivity.class.getName()));
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                final Slice suggestionSlice = new InlinePresentationBuilder(value)
                        .setAttribution(pendingIntent).build();

                final List<InlinePresentationSpec> specs = inlineRequest.getInlinePresentationSpecs();
                final int specsSize = specs.size();
                final InlinePresentationSpec currentSpec = i - 1 < specsSize
                        ? specs.get(i - 1)
                        : specs.get(specsSize - 1);
                final InlinePresentation inlinePresentation =
                        new InlinePresentation(suggestionSlice, currentSpec, /** pined= */false);
                dataset.setValue(id, AutofillValue.forText(value), presentation,
                        inlinePresentation);
            } else {
                dataset.setValue(id, AutofillValue.forText(value), presentation);
            }
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
    private ArrayMap<String, AutofillId> getAutofillableFields(@NonNull AssistStructure structure) {
        ArrayMap<String, AutofillId> fields = new ArrayMap<>();
        int nodes = structure.getWindowNodeCount();
        for (int i = 0; i < nodes; i++) {
            ViewNode node = structure.getWindowNodeAt(i).getRootViewNode();
            addAutofillableFields(fields, node);
        }
        ArrayMap<String, AutofillId> result = new ArrayMap<>();
        int filedCount = fields.size();
        for (int i = 0; i < filedCount; i++) {
            String key = fields.keyAt(i);
            AutofillId value = fields.valueAt(i);
            // For fields with no hint we just use Field
            if (key.equals(value.toString())) {
                result.put("Field:" + i + "-", fields.valueAt(i));
            } else {
                result.put(key, fields.valueAt(i));
            }
        }
        return result;
    }

    /**
     * Adds any autofillable view from the {@link ViewNode} and its descendants to the map.
     */
    private void addAutofillableFields(@NonNull Map<String, AutofillId> fields,
            @NonNull ViewNode node) {
        if (node.getAutofillType() == View.AUTOFILL_TYPE_TEXT) {
            if (!fields.containsValue(node.getAutofillId())) {
                final String key;
                if (node.getHint() != null) {
                    key = node.getHint().toLowerCase();
                } else {
                    key = node.getAutofillId().toString();
                }
                fields.put(key, node.getAutofillId());
            }
        }
        int childrenSize = node.getChildCount();
        for (int i = 0; i < childrenSize; i++) {
            addAutofillableFields(fields, node.getChildAt(i));
        }
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
