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
import android.content.Context;
import android.content.Intent;
import android.content.RestrictionEntry;
import android.os.Bundle;
import android.os.UserManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This fragment is included in {@code CustomRestrictionsActivity}.  It demonstrates how an app
 * can integrate its own custom app restriction settings with the restricted profile feature.
 *
 * This sample app maintains custom app restriction settings in shared preferences.  Your app
 * can use other methods to maintain the settings.  When this activity is invoked
 * (from Settings > Users > Restricted Profile), the shared preferences are used to initialize
 * the custom configuration on the user interface.
 *
 * Three sample input types are shown: checkbox, single-choice, and multi-choice.  When the
 * settings are modified by the user, the corresponding restriction entries are saved in the
 * platform.  The saved restriction entries are retrievable when the app is launched under a
 * restricted profile.
 */
public class CustomRestrictionsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    // Shared preference key for the boolean restriction.
    private static final String KEY_BOOLEAN_PREF = "pref_boolean";
    // Shared preference key for the single-select restriction.
    private static final String KEY_CHOICE_PREF = "pref_choice";
    // Shared preference key for the multi-select restriction.
    private static final String KEY_MULTI_PREF = "pref_multi";


    private List<RestrictionEntry> mRestrictions;
    private Bundle mRestrictionsBundle;

    // Shared preferences for each of the sample input types.
    private CheckBoxPreference mBooleanPref;
    private ListPreference mChoicePref;
    private MultiSelectListPreference mMultiPref;

    // Restriction entries for each of the sample input types.
    private RestrictionEntry mBooleanEntry;
    private RestrictionEntry mChoiceEntry;
    private RestrictionEntry mMultiEntry;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.custom_prefs);

        // This sample app uses shared preferences to maintain app restriction settings.  Your app
        // can use other methods to maintain the settings.
        mBooleanPref = (CheckBoxPreference) findPreference(KEY_BOOLEAN_PREF);
        mChoicePref = (ListPreference) findPreference(KEY_CHOICE_PREF);
        mMultiPref = (MultiSelectListPreference) findPreference(KEY_MULTI_PREF);

        mBooleanPref.setOnPreferenceChangeListener(this);
        mChoicePref.setOnPreferenceChangeListener(this);
        mMultiPref.setOnPreferenceChangeListener(this);

        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Activity activity = getActivity();

        // BEGIN_INCLUDE (GET_CURRENT_RESTRICTIONS)
        // Existing app restriction settings, if exist, can be retrieved from the Bundle.
        mRestrictionsBundle =
                activity.getIntent().getBundleExtra(Intent.EXTRA_RESTRICTIONS_BUNDLE);

        if (mRestrictionsBundle == null) {
            mRestrictionsBundle =
                    ((UserManager) activity.getSystemService(Context.USER_SERVICE))
                            .getApplicationRestrictions(activity.getPackageName());
        }

        if (mRestrictionsBundle == null) {
            mRestrictionsBundle = new Bundle();
        }

        mRestrictions = activity.getIntent().getParcelableArrayListExtra(
                Intent.EXTRA_RESTRICTIONS_LIST);
        // END_INCLUDE (GET_CURRENT_RESTRICTIONS)

        // Transfers the saved values into the preference hierarchy.
        if (mRestrictions != null) {
            for (RestrictionEntry entry : mRestrictions) {
                if (entry.getKey().equals(GetRestrictionsReceiver.KEY_BOOLEAN)) {
                    mBooleanPref.setChecked(entry.getSelectedState());
                    mBooleanEntry = entry;
                } else if (entry.getKey().equals(GetRestrictionsReceiver.KEY_CHOICE)) {
                    mChoicePref.setValue(entry.getSelectedString());
                    mChoiceEntry = entry;
                } else if (entry.getKey().equals(GetRestrictionsReceiver.KEY_MULTI_SELECT)) {
                    HashSet<String> set = new HashSet<String>();
                    for (String value : entry.getAllSelectedStrings()) {
                        set.add(value);
                    }
                    mMultiPref.setValues(set);
                    mMultiEntry = entry;
                }
            }
        } else {
            mRestrictions = new ArrayList<RestrictionEntry>();

            // Initializes the boolean restriction entry and updates its corresponding shared
            // preference value.
            mBooleanEntry = new RestrictionEntry(GetRestrictionsReceiver.KEY_BOOLEAN,
                    mRestrictionsBundle.getBoolean(GetRestrictionsReceiver.KEY_BOOLEAN, false));
            mBooleanEntry.setType(RestrictionEntry.TYPE_BOOLEAN);
            mBooleanPref.setChecked(mBooleanEntry.getSelectedState());

            // Initializes the single choice restriction entry and updates its corresponding
            // shared preference value.
            mChoiceEntry = new RestrictionEntry(GetRestrictionsReceiver.KEY_CHOICE,
                    mRestrictionsBundle.getString(GetRestrictionsReceiver.KEY_CHOICE));
            mChoiceEntry.setType(RestrictionEntry.TYPE_CHOICE);
            mChoicePref.setValue(mChoiceEntry.getSelectedString());

            // Initializes the multi-select restriction entry and updates its corresponding
            // shared preference value.
            mMultiEntry = new RestrictionEntry(GetRestrictionsReceiver.KEY_MULTI_SELECT,
                    mRestrictionsBundle.getStringArray(
                            GetRestrictionsReceiver.KEY_MULTI_SELECT));
            mMultiEntry.setType(RestrictionEntry.TYPE_MULTI_SELECT);
            if (mMultiEntry.getAllSelectedStrings() != null) {
                HashSet<String> set = new HashSet<String>();
                final String[] values = mRestrictionsBundle.getStringArray(
                        GetRestrictionsReceiver.KEY_MULTI_SELECT);
                if (values != null) {
                    for (String value : values) {
                        set.add(value);
                    }
                }
                mMultiPref.setValues(set);
            }
            mRestrictions.add(mBooleanEntry);
            mRestrictions.add(mChoiceEntry);
            mRestrictions.add(mMultiEntry);
        }
        // Prepares result to be passed back to the Settings app when the custom restrictions
        // activity finishes.
        Intent intent = new Intent(getActivity().getIntent());
        intent.putParcelableArrayListExtra(Intent.EXTRA_RESTRICTIONS_LIST,
                new ArrayList<RestrictionEntry>(mRestrictions));
        getActivity().setResult(Activity.RESULT_OK, intent);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mBooleanPref) {
            mBooleanEntry.setSelectedState((Boolean) newValue);
        } else if (preference == mChoicePref) {
            mChoiceEntry.setSelectedString((String) newValue);
        } else if (preference == mMultiPref) {
            String[] selectedStrings = new String[((Set<String>)newValue).size()];
            int i = 0;
            for (String value : (Set<String>) newValue) {
                selectedStrings[i++] = value;
            }
            mMultiEntry.setAllSelectedStrings(selectedStrings);
        }

        // Saves all the app restriction configuration changes from the custom activity.
        Intent intent = new Intent(getActivity().getIntent());
        intent.putParcelableArrayListExtra(Intent.EXTRA_RESTRICTIONS_LIST,
                new ArrayList<RestrictionEntry>(mRestrictions));
        getActivity().setResult(Activity.RESULT_OK, intent);
        return true;
    }
}
