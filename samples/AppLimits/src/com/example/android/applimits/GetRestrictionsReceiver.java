/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.applimits;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.RestrictionEntry;
import android.content.BroadcastReceiver.PendingResult;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class GetRestrictionsReceiver extends BroadcastReceiver {
    private static final String TAG = "AppLimits$GetRestrictionsReceiver";

    static final String KEY_CUSTOM = "custom_or_not";
    static final String KEY_CHOICE = "choice";
    static final String KEY_MULTI_SELECT = "multi";

    @Override
    public void onReceive(final Context context, Intent intent) {
        final PendingResult result = goAsync();
        final ArrayList<RestrictionEntry> oldRestrictions =
                intent.getParcelableArrayListExtra(Intent.EXTRA_RESTRICTIONS);
        Log.i(TAG, "oldRestrictions = " + oldRestrictions);
        new Thread() {
            public void run() {
                createRestrictions(context, result, oldRestrictions);
            }
        }.start();
    }

    public static void populateCustomEntry(Resources res, RestrictionEntry entry) {
        entry.setType(RestrictionEntry.TYPE_BOOLEAN);
        entry.setTitle(res.getString(R.string.custom_or_not_title));
    }

    public static void populateChoiceEntry(Resources res, RestrictionEntry reSingleChoice) {
        String[] choiceEntries = res.getStringArray(R.array.choice_entry_entries);
        String[] choiceValues = res.getStringArray(R.array.choice_entry_values);
        if (reSingleChoice.getSelectedString() == null) {
            reSingleChoice.setSelectedString(choiceValues[0]);
        }
        reSingleChoice.setTitle(res.getString(R.string.choice_entry_title));
        reSingleChoice.setChoiceEntries(choiceEntries);
        reSingleChoice.setChoiceValues(choiceValues);
        reSingleChoice.setType(RestrictionEntry.TYPE_CHOICE);
    }

    public static void populateMultiEntry(Resources res, RestrictionEntry reMultiSelect) {
        String[] multiEntries = res.getStringArray(R.array.multi_entry_entries);
        String[] multiValues = res.getStringArray(R.array.multi_entry_values);
        if (reMultiSelect.getAllSelectedStrings() == null) {
            reMultiSelect.setAllSelectedStrings(new String[0]);
        }
        reMultiSelect.setTitle(res.getString(R.string.multi_entry_title));
        reMultiSelect.setChoiceEntries(multiEntries);
        reMultiSelect.setChoiceValues(multiValues);
        reMultiSelect.setType(RestrictionEntry.TYPE_MULTI_SELECT);
    }

    private ArrayList<RestrictionEntry> initRestrictions(Context context) {
        ArrayList<RestrictionEntry> newRestrictions = new ArrayList<RestrictionEntry>();
        Resources res = context.getResources();

        RestrictionEntry reCustomOrNot = new RestrictionEntry(KEY_CUSTOM, false);
        populateCustomEntry(res, reCustomOrNot);
        newRestrictions.add(reCustomOrNot);

        RestrictionEntry reSingleChoice = new RestrictionEntry(KEY_CHOICE, (String) null);
        populateChoiceEntry(res, reSingleChoice);
        newRestrictions.add(reSingleChoice);

        RestrictionEntry reMultiSelect = new RestrictionEntry(KEY_MULTI_SELECT, (String[]) null);
        populateMultiEntry(res, reMultiSelect);
        newRestrictions.add(reMultiSelect);

        return newRestrictions;
    }

    private void createRestrictions(Context context,
            PendingResult result, ArrayList<RestrictionEntry> old) {
        Resources res = context.getResources();

        ArrayList<RestrictionEntry> newEntries = initRestrictions(context);
        // If this is the first time, create the default restrictions entries and return them.
        if (old == null) {
            Bundle extras = new Bundle();
            extras.putParcelableArrayList(Intent.EXTRA_RESTRICTIONS, newEntries);
            result.setResult(Activity.RESULT_OK, null, extras);
            result.finish();
            return;
        }

        boolean custom = false;
        for (RestrictionEntry entry : old) {
            if (entry.getKey().equals(KEY_CUSTOM)) {
                if (entry.getSelectedState()) {
                    custom = true;
                }
                RestrictionEntry newEntry = find(newEntries, KEY_CUSTOM);
                newEntry.setSelectedState(entry.getSelectedState());
            } else if (entry.getKey().equals(KEY_CHOICE)) {
                RestrictionEntry newEntry = find(newEntries, KEY_CHOICE);
                newEntry.setSelectedString(entry.getSelectedString());
            } else if (entry.getKey().equals(KEY_MULTI_SELECT)) {
                RestrictionEntry newEntry = find(newEntries, KEY_MULTI_SELECT);
                newEntry.setAllSelectedStrings(entry.getAllSelectedStrings());
            }
        }

        Bundle extras = new Bundle();
        if (custom) {
            Intent customIntent = new Intent();
            customIntent.setClass(context, CustomRestrictionsActivity.class);
            extras.putParcelable(Intent.EXTRA_RESTRICTIONS_INTENT, customIntent);
        }
        extras.putParcelableArrayList(Intent.EXTRA_RESTRICTIONS, newEntries);
        result.setResult(Activity.RESULT_OK, null, extras);
        result.finish();
    }

    private RestrictionEntry find(ArrayList<RestrictionEntry> entries, String key) {
        for (RestrictionEntry entry : entries) {
            if (entry.getKey().equals(key)) {
                return entry;
            }
        }
        return null;
    }
}
