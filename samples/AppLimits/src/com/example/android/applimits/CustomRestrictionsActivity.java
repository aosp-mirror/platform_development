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

import android.content.Context;
import android.content.Intent;
import android.content.RestrictionEntry;
import android.os.Bundle;
import android.os.UserManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;

import com.example.android.applimits.GetRestrictionsReceiver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CustomRestrictionsActivity extends PreferenceActivity
        implements OnPreferenceChangeListener {

    private static final String KEY_CUSTOM_PREF = "custom";
    private static final String KEY_CHOICE_PREF = "choice";
    private static final String KEY_MULTI_PREF = "multi";

    List<RestrictionEntry> mRestrictions;
    private Bundle mRestrictionsBundle;

    CheckBoxPreference mCustomPref;
    ListPreference mChoicePref;
    MultiSelectListPreference mMultiPref;

    RestrictionEntry mCustomEntry;
    RestrictionEntry mChoiceEntry;
    RestrictionEntry mMultiEntry;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRestrictionsBundle = getIntent().getBundleExtra(Intent.EXTRA_RESTRICTIONS_BUNDLE);
        if (mRestrictionsBundle == null) {
            mRestrictionsBundle =
                ((UserManager) getSystemService(Context.USER_SERVICE))
                .getApplicationRestrictions(getPackageName());
        }
        if (mRestrictionsBundle == null) {
            mRestrictionsBundle = new Bundle();
        }

        if (savedInstanceState != null
                && savedInstanceState.containsKey(Intent.EXTRA_RESTRICTIONS_LIST)) {
            mRestrictions = savedInstanceState.getParcelableArrayList(
                    Intent.EXTRA_RESTRICTIONS_LIST);
        }

        this.addPreferencesFromResource(R.xml.custom_prefs);
        mCustomPref = (CheckBoxPreference) findPreference(KEY_CUSTOM_PREF);
        mChoicePref = (ListPreference) findPreference(KEY_CHOICE_PREF);
        mMultiPref = (MultiSelectListPreference) findPreference(KEY_MULTI_PREF);

        // Transfer the saved values into the preference hierarchy
        if (mRestrictions != null) {
            for (RestrictionEntry entry : mRestrictions) {
                if (entry.getKey().equals(GetRestrictionsReceiver.KEY_CUSTOM)) {
                    mCustomPref.setChecked(entry.getSelectedState());
                    mCustomEntry = entry;
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
            mCustomEntry = new RestrictionEntry(GetRestrictionsReceiver.KEY_CUSTOM,
                    mRestrictionsBundle.getBoolean(GetRestrictionsReceiver.KEY_CUSTOM, false));
            mCustomEntry.setType(RestrictionEntry.TYPE_BOOLEAN);
            mCustomPref.setChecked(mCustomEntry.getSelectedState());
            mChoiceEntry = new RestrictionEntry(GetRestrictionsReceiver.KEY_CHOICE,
                    mRestrictionsBundle.getString(GetRestrictionsReceiver.KEY_CHOICE));
            mChoiceEntry.setType(RestrictionEntry.TYPE_CHOICE);
            mChoicePref.setValue(mChoiceEntry.getSelectedString());
            mMultiEntry = new RestrictionEntry(GetRestrictionsReceiver.KEY_MULTI_SELECT,
                    mRestrictionsBundle.getStringArray(GetRestrictionsReceiver.KEY_MULTI_SELECT));
            mMultiEntry.setType(RestrictionEntry.TYPE_MULTI_SELECT);
            if (mMultiEntry.getAllSelectedStrings() != null) {
                HashSet<String> set = new HashSet<String>();
                for (String value : mRestrictionsBundle.getStringArray(
                        GetRestrictionsReceiver.KEY_MULTI_SELECT)) {
                    set.add(value);
                }
                mMultiPref.setValues(set);
            }
            mRestrictions.add(mCustomEntry);
            mRestrictions.add(mChoiceEntry);
            mRestrictions.add(mMultiEntry);
        }
        mCustomPref.setOnPreferenceChangeListener(this);
        mChoicePref.setOnPreferenceChangeListener(this);
        mMultiPref.setOnPreferenceChangeListener(this);
        Intent intent = new Intent(getIntent());
        intent.putParcelableArrayListExtra(Intent.EXTRA_RESTRICTIONS_LIST,
                new ArrayList<RestrictionEntry>(mRestrictions));
        setResult(RESULT_OK, intent);
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(Intent.EXTRA_RESTRICTIONS_LIST,
                new ArrayList<RestrictionEntry>(mRestrictions));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mCustomPref) {
            mCustomEntry.setSelectedState((Boolean) newValue);
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
        Intent intent = new Intent(getIntent());
        intent.putParcelableArrayListExtra(Intent.EXTRA_RESTRICTIONS_LIST,
                new ArrayList<RestrictionEntry>(mRestrictions));
        setResult(RESULT_OK, intent);
        return true;
    }
}
