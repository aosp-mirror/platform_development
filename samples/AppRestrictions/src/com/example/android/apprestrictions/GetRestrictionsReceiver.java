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

package com.example.android.apprestrictions;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.RestrictionEntry;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;

public class GetRestrictionsReceiver extends BroadcastReceiver {
    private static final String TAG = GetRestrictionsReceiver.class.getSimpleName();

    // Keys for referencing app restriction settings from the platform.
    public static final String KEY_BOOLEAN = "boolean_key";
    public static final String KEY_CHOICE = "choice_key";
    public static final String KEY_MULTI_SELECT = "multi_key";

    @Override
    public void onReceive(final Context context, Intent intent) {
        final PendingResult result = goAsync();

        // If app restriction settings are already created, they will be included in the Bundle
        // as key/value pairs.
        final Bundle existingRestrictions =
                intent.getBundleExtra(Intent.EXTRA_RESTRICTIONS_BUNDLE);
        Log.i(TAG, "existingRestrictions = " + existingRestrictions);

        new Thread() {
            public void run() {
                createRestrictions(context, result, existingRestrictions);
            }
        }.start();
    }

    // Initializes a boolean type restriction entry.
    public static void populateBooleanEntry(Resources res, RestrictionEntry entry) {
        entry.setType(RestrictionEntry.TYPE_BOOLEAN);
        entry.setTitle(res.getString(R.string.boolean_entry_title));
    }

    // Initializes a single choice type restriction entry.
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

    // Initializes a multi-select type restriction entry.
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

    // Demonstrates the creation of standard app restriction types: boolean, single choice, and
    // multi-select.
    private ArrayList<RestrictionEntry> initRestrictions(Context context) {
        ArrayList<RestrictionEntry> newRestrictions = new ArrayList<RestrictionEntry>();
        Resources res = context.getResources();

        RestrictionEntry reBoolean = new RestrictionEntry(KEY_BOOLEAN, false);
        populateBooleanEntry(res, reBoolean);
        newRestrictions.add(reBoolean);

        RestrictionEntry reSingleChoice = new RestrictionEntry(KEY_CHOICE, (String) null);
        populateChoiceEntry(res, reSingleChoice);
        newRestrictions.add(reSingleChoice);

        RestrictionEntry reMultiSelect = new RestrictionEntry(KEY_MULTI_SELECT, (String[]) null);
        populateMultiEntry(res, reMultiSelect);
        newRestrictions.add(reMultiSelect);

        return newRestrictions;
    }

    private void createRestrictions(Context context, PendingResult result,
                                    Bundle existingRestrictions) {
        // The incoming restrictions bundle contains key/value pairs representing existing app
        // restrictions for this package. In order to retain existing app restrictions, you need to
        // construct new restriction entries and then copy in any existing values for the new keys.
        ArrayList<RestrictionEntry> newEntries = initRestrictions(context);

        // If app restrictions were not previously configured for the package, create the default
        // restrictions entries and return them.
        if (existingRestrictions == null) {
            Bundle extras = new Bundle();
            extras.putParcelableArrayList(Intent.EXTRA_RESTRICTIONS_LIST, newEntries);
            result.setResult(Activity.RESULT_OK, null, extras);
            result.finish();
            return;
        }

        // Retains current restriction settings by transferring existing restriction entries to
        // new ones.
        for (RestrictionEntry entry : newEntries) {
            final String key = entry.getKey();
            if (KEY_BOOLEAN.equals(key)) {
                entry.setSelectedState(existingRestrictions.getBoolean(KEY_BOOLEAN));
            } else if (KEY_CHOICE.equals(key)) {
                if (existingRestrictions.containsKey(KEY_CHOICE)) {
                    entry.setSelectedString(existingRestrictions.getString(KEY_CHOICE));
                }
            } else if (KEY_MULTI_SELECT.equals(key)) {
                if (existingRestrictions.containsKey(KEY_MULTI_SELECT)) {
                    entry.setAllSelectedStrings(existingRestrictions.getStringArray(key));
                }
            }
        }

        final Bundle extras = new Bundle();

        // This path demonstrates the use of a custom app restriction activity instead of standard
        // types.  When a custom activity is set, the standard types will not be available under
        // app restriction settings.
        //
        // If your app has an existing activity for app restriction configuration, you can set it
        // up with the intent here.
        if (PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(MainActivity.CUSTOM_CONFIG_KEY, false)) {
            final Intent customIntent = new Intent();
            customIntent.setClass(context, CustomRestrictionsActivity.class);
            extras.putParcelable(Intent.EXTRA_RESTRICTIONS_INTENT, customIntent);
        }

        extras.putParcelableArrayList(Intent.EXTRA_RESTRICTIONS_LIST, newEntries);
        result.setResult(Activity.RESULT_OK, null, extras);
        result.finish();
    }
}
