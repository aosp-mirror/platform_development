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

package com.example.android.apprestrictionenforcer;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.RestrictionEntry;
import android.content.RestrictionsManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This fragment provides UI and functionality to set restrictions on the AppRestrictionSchema
 * sample.
 */
public class AppRestrictionEnforcerFragment extends Fragment implements
        CompoundButton.OnCheckedChangeListener, AdapterView.OnItemSelectedListener,
        View.OnClickListener, ItemAddFragment.OnItemAddedListener {

    /**
     * Key for {@link SharedPreferences}
     */
    private static final String PREFS_KEY = "AppRestrictionEnforcerFragment";

    /**
     * Key for the boolean restriction in AppRestrictionSchema.
     */
    private static final String RESTRICTION_KEY_SAY_HELLO = "can_say_hello";

    /**
     * Key for the string restriction in AppRestrictionSchema.
     */
    private static final String RESTRICTION_KEY_MESSAGE = "message";

    /**
     * Key for the integer restriction in AppRestrictionSchema.
     */
    private static final String RESTRICTION_KEY_NUMBER = "number";

    /**
     * Key for the choice restriction in AppRestrictionSchema.
     */
    private static final String RESTRICTION_KEY_RANK = "rank";

    /**
     * Key for the multi-select restriction in AppRestrictionSchema.
     */
    private static final String RESTRICTION_KEY_APPROVALS = "approvals";

    /**
     * Key for the bundle array restriction in AppRestrictionSchema.
     */
    private static final String RESTRICTION_KEY_ITEMS = "items";
    private static final String RESTRICTION_KEY_ITEM_KEY = "key";
    private static final String RESTRICTION_KEY_ITEM_VALUE = "value";

    private static final String DELIMETER = ",";
    private static final String SEPARATOR = ":";

    private static final boolean BUNDLE_SUPPORTED = Build.VERSION.SDK_INT >= 23;

    /**
     * Current status of the restrictions.
     */
    private Bundle mCurrentRestrictions = new Bundle();

    // UI Components
    private Switch mSwitchSayHello;
    private EditText mEditMessage;
    private EditText mEditNumber;
    private Spinner mSpinnerRank;
    private LinearLayout mLayoutApprovals;
    private LinearLayout mLayoutItems;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_app_restriction_enforcer, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        // Retain references for the UI elements
        mSwitchSayHello = (Switch) view.findViewById(R.id.say_hello);
        mEditMessage = (EditText) view.findViewById(R.id.message);
        mEditNumber = (EditText) view.findViewById(R.id.number);
        mSpinnerRank = (Spinner) view.findViewById(R.id.rank);
        mLayoutApprovals = (LinearLayout) view.findViewById(R.id.approvals);
        mLayoutItems = (LinearLayout) view.findViewById(R.id.items);
        view.findViewById(R.id.item_add).setOnClickListener(this);
        View bundleArrayLayout = view.findViewById(R.id.bundle_array_layout);
        if (BUNDLE_SUPPORTED) {
            bundleArrayLayout.setVisibility(View.VISIBLE);
        } else {
            bundleArrayLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRestrictions(getActivity());
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        switch (compoundButton.getId()) {
            case R.id.say_hello: {
                saveCanSayHello(getActivity(), checked);
                break;
            }
            case R.id.approval: {
                if (checked) {
                    addApproval(getActivity(), (String) compoundButton.getTag());
                } else {
                    removeApproval(getActivity(), (String) compoundButton.getTag());
                }
                break;
            }
        }
    }

    private TextWatcher mWatcherMessage = new EasyTextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            saveMessage(getActivity(), s.toString());
        }
    };

    private TextWatcher mWatcherNumber = new EasyTextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            try {
                String string = s.toString();
                if (!TextUtils.isEmpty(string)) {
                    saveNumber(getActivity(), Integer.parseInt(string));
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), "Not an integer!", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.rank: {
                saveRank(getActivity(), (String) parent.getAdapter().getItem(position));
                break;
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Nothing to do
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.item_add:
                new ItemAddFragment().show(getChildFragmentManager(), "dialog");
                break;
            case R.id.item_remove:
                String key = (String) v.getTag();
                removeItem(key);
                mLayoutItems.removeView((View) v.getParent());
                break;
        }
    }

    @Override
    public void onItemAdded(String key, String value) {
        key = TextUtils.replace(key,
                new String[]{DELIMETER, SEPARATOR}, new String[]{"", ""}).toString();
        value = TextUtils.replace(value,
                new String[]{DELIMETER, SEPARATOR}, new String[]{"", ""}).toString();
        Parcelable[] parcelables = mCurrentRestrictions.getParcelableArray(RESTRICTION_KEY_ITEMS);
        Map<String, String> items = new HashMap<>();
        if (parcelables != null) {
            for (Parcelable parcelable : parcelables) {
                Bundle bundle = (Bundle) parcelable;
                items.put(bundle.getString(RESTRICTION_KEY_ITEM_KEY),
                        bundle.getString(RESTRICTION_KEY_ITEM_VALUE));
            }
        }
        items.put(key, value);
        insertItemRow(LayoutInflater.from(getActivity()), key, value);
        saveItems(getActivity(), items);
    }

    /**
     * Loads the restrictions for the AppRestrictionSchema sample.
     *
     * @param activity The activity
     */
    private void loadRestrictions(Activity activity) {
        RestrictionsManager manager =
                (RestrictionsManager) activity.getSystemService(Context.RESTRICTIONS_SERVICE);
        List<RestrictionEntry> restrictions =
                manager.getManifestRestrictions(Constants.PACKAGE_NAME_APP_RESTRICTION_SCHEMA);
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        for (RestrictionEntry restriction : restrictions) {
            String key = restriction.getKey();
            if (RESTRICTION_KEY_SAY_HELLO.equals(key)) {
                updateCanSayHello(prefs.getBoolean(RESTRICTION_KEY_SAY_HELLO,
                        restriction.getSelectedState()));
            } else if (RESTRICTION_KEY_MESSAGE.equals(key)) {
                updateMessage(prefs.getString(RESTRICTION_KEY_MESSAGE,
                        restriction.getSelectedString()));
            } else if (RESTRICTION_KEY_NUMBER.equals(key)) {
                updateNumber(prefs.getInt(RESTRICTION_KEY_NUMBER,
                        restriction.getIntValue()));
            } else if (RESTRICTION_KEY_RANK.equals(key)) {
                updateRank(activity, restriction.getChoiceValues(),
                        prefs.getString(RESTRICTION_KEY_RANK, restriction.getSelectedString()));
            } else if (RESTRICTION_KEY_APPROVALS.equals(key)) {
                updateApprovals(activity, restriction.getChoiceValues(),
                        TextUtils.split(prefs.getString(RESTRICTION_KEY_APPROVALS,
                                        TextUtils.join(DELIMETER,
                                                restriction.getAllSelectedStrings())),
                                DELIMETER));
            } else if (BUNDLE_SUPPORTED && RESTRICTION_KEY_ITEMS.equals(key)) {
                String itemsString = prefs.getString(RESTRICTION_KEY_ITEMS, "");
                HashMap<String, String> items = new HashMap<>();
                for (String itemString : TextUtils.split(itemsString, DELIMETER)) {
                    String[] strings = itemString.split(SEPARATOR, 2);
                    items.put(strings[0], strings[1]);
                }
                updateItems(activity, items);
            }
        }
    }

    private void updateCanSayHello(boolean canSayHello) {
        mCurrentRestrictions.putBoolean(RESTRICTION_KEY_SAY_HELLO, canSayHello);
        mSwitchSayHello.setOnCheckedChangeListener(null);
        mSwitchSayHello.setChecked(canSayHello);
        mSwitchSayHello.setOnCheckedChangeListener(this);
    }

    private void updateMessage(String message) {
        mCurrentRestrictions.putString(RESTRICTION_KEY_MESSAGE, message);
        mEditMessage.removeTextChangedListener(mWatcherMessage);
        mEditMessage.setText(message);
        mEditMessage.addTextChangedListener(mWatcherMessage);
    }

    private void updateNumber(int number) {
        mCurrentRestrictions.putInt(RESTRICTION_KEY_NUMBER, number);
        mEditNumber.removeTextChangedListener(mWatcherNumber);
        mEditNumber.setText(String.valueOf(number));
        mEditNumber.addTextChangedListener(mWatcherNumber);
    }

    private void updateRank(Context context, String[] ranks, String selectedRank) {
        mCurrentRestrictions.putString(RESTRICTION_KEY_RANK, selectedRank);
        mSpinnerRank.setAdapter(new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item, ranks));
        mSpinnerRank.setSelection(search(ranks, selectedRank));
        mSpinnerRank.setOnItemSelectedListener(this);
    }

    private void updateApprovals(Context context, String[] approvals,
                                 String[] selectedApprovals) {
        mCurrentRestrictions.putStringArray(RESTRICTION_KEY_APPROVALS, selectedApprovals);
        mLayoutApprovals.removeAllViews();
        for (String approval : approvals) {
            Switch sw = new Switch(context);
            sw.setText(approval);
            sw.setTag(approval);
            sw.setChecked(Arrays.asList(selectedApprovals).contains(approval));
            sw.setOnCheckedChangeListener(this);
            sw.setId(R.id.approval);
            mLayoutApprovals.addView(sw);
        }
    }

    private void updateItems(Context context, Map<String, String> items) {
        if (!BUNDLE_SUPPORTED) {
            return;
        }
        mCurrentRestrictions.putParcelableArray(RESTRICTION_KEY_ITEMS, convertToBundles(items));
        LayoutInflater inflater = LayoutInflater.from(context);
        mLayoutItems.removeAllViews();
        for (String key : items.keySet()) {
            insertItemRow(inflater, key, items.get(key));
        }
    }

    private void insertItemRow(LayoutInflater inflater, String key, String value) {
        View view = inflater.inflate(R.layout.item, mLayoutItems, false);
        TextView textView = (TextView) view.findViewById(R.id.item_text);
        textView.setText(getString(R.string.item, key, value));
        Button remove = (Button) view.findViewById(R.id.item_remove);
        remove.setTag(key);
        remove.setOnClickListener(this);
        mLayoutItems.addView(view);
    }

    @NonNull
    private Bundle[] convertToBundles(Map<String, String> items) {
        Bundle[] bundles = new Bundle[items.size()];
        int i = 0;
        for (String key : items.keySet()) {
            Bundle bundle = new Bundle();
            bundle.putString(RESTRICTION_KEY_ITEM_KEY, key);
            bundle.putString(RESTRICTION_KEY_ITEM_VALUE, items.get(key));
            bundles[i++] = bundle;
        }
        return bundles;
    }

    private void removeItem(String key) {
        Parcelable[] parcelables = mCurrentRestrictions.getParcelableArray(RESTRICTION_KEY_ITEMS);
        if (parcelables != null) {
            Map<String, String> items = new HashMap<>();
            for (Parcelable parcelable : parcelables) {
                Bundle bundle = (Bundle) parcelable;
                if (!key.equals(bundle.getString(RESTRICTION_KEY_ITEM_KEY))) {
                    items.put(bundle.getString(RESTRICTION_KEY_ITEM_KEY),
                            bundle.getString(RESTRICTION_KEY_ITEM_VALUE));
                }
            }
            saveItems(getActivity(), items);
        }
    }

    /**
     * Saves the value for the "cay_say_hello" restriction of AppRestrictionSchema.
     *
     * @param activity The activity
     * @param allow    The value to be set for the restriction.
     */
    private void saveCanSayHello(Activity activity, boolean allow) {
        mCurrentRestrictions.putBoolean(RESTRICTION_KEY_SAY_HELLO, allow);
        saveRestrictions(activity);
        // Note that the owner app needs to remember the restrictions on its own.
        editPreferences(activity).putBoolean(RESTRICTION_KEY_SAY_HELLO, allow).apply();
    }

    /**
     * Saves the value for the "message" restriction of AppRestrictionSchema.
     *
     * @param activity The activity
     * @param message  The value to be set for the restriction.
     */
    private void saveMessage(Activity activity, String message) {
        mCurrentRestrictions.putString(RESTRICTION_KEY_MESSAGE, message);
        saveRestrictions(activity);
        editPreferences(activity).putString(RESTRICTION_KEY_MESSAGE, message).apply();
    }

    /**
     * Saves the value for the "number" restriction of AppRestrictionSchema.
     *
     * @param activity The activity
     * @param number   The value to be set for the restriction.
     */
    private void saveNumber(Activity activity, int number) {
        mCurrentRestrictions.putInt(RESTRICTION_KEY_NUMBER, number);
        saveRestrictions(activity);
        editPreferences(activity).putInt(RESTRICTION_KEY_NUMBER, number).apply();
    }

    /**
     * Saves the value for the "rank" restriction of AppRestrictionSchema.
     *
     * @param activity The activity
     * @param rank     The value to be set for the restriction.
     */
    private void saveRank(Activity activity, String rank) {
        mCurrentRestrictions.putString(RESTRICTION_KEY_RANK, rank);
        saveRestrictions(activity);
        editPreferences(activity).putString(RESTRICTION_KEY_RANK, rank).apply();
    }

    private void addApproval(Activity activity, String approval) {
        List<String> approvals = new ArrayList<>(Arrays.asList(
                mCurrentRestrictions.getStringArray(RESTRICTION_KEY_APPROVALS)));
        if (approvals.contains(approval)) {
            return;
        }
        approvals.add(approval);
        saveApprovals(activity, approvals.toArray(new String[approvals.size()]));
    }

    private void removeApproval(Activity activity, String approval) {
        List<String> approvals = new ArrayList<>(Arrays.asList(
                mCurrentRestrictions.getStringArray(RESTRICTION_KEY_APPROVALS)));
        if (!approval.contains(approval)) {
            return;
        }
        approvals.remove(approval);
        saveApprovals(activity, approvals.toArray(new String[approvals.size()]));
    }

    /**
     * Saves the value for the "approvals" restriction of AppRestrictionSchema.
     *
     * @param activity  The activity
     * @param approvals The value to be set for the restriction.
     */
    private void saveApprovals(Activity activity, String[] approvals) {
        mCurrentRestrictions.putStringArray(RESTRICTION_KEY_APPROVALS, approvals);
        saveRestrictions(activity);
        editPreferences(activity).putString(RESTRICTION_KEY_APPROVALS,
                TextUtils.join(DELIMETER, approvals)).apply();
    }

    /**
     * Saves the value for the "items" restriction of AppRestrictionSchema.
     *
     * @param activity The activity.
     * @param items    The values.
     */
    private void saveItems(Activity activity, Map<String, String> items) {
        if (!BUNDLE_SUPPORTED) {
            return;
        }
        mCurrentRestrictions.putParcelableArray(RESTRICTION_KEY_ITEMS, convertToBundles(items));
        saveRestrictions(activity);
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String key : items.keySet()) {
            if (first) {
                first = false;
            } else {
                builder.append(DELIMETER);
            }
            builder.append(key);
            builder.append(SEPARATOR);
            builder.append(items.get(key));
        }
        editPreferences(activity).putString(RESTRICTION_KEY_ITEMS, builder.toString()).apply();
    }

    /**
     * Saves all the restrictions.
     *
     * @param activity The activity.
     */
    private void saveRestrictions(Activity activity) {
        DevicePolicyManager devicePolicyManager
                = (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        devicePolicyManager.setApplicationRestrictions(
                EnforcerDeviceAdminReceiver.getComponentName(activity),
                Constants.PACKAGE_NAME_APP_RESTRICTION_SCHEMA, mCurrentRestrictions);
    }

    private SharedPreferences.Editor editPreferences(Activity activity) {
        return activity.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE).edit();
    }

    /**
     * Sequential search
     *
     * @param array The string array
     * @param s     The string to search for
     * @return Index if found. -1 if not found.
     */
    private int search(String[] array, String s) {
        for (int i = 0; i < array.length; ++i) {
            if (s.equals(array[i])) {
                return i;
            }
        }
        return -1;
    }

}
