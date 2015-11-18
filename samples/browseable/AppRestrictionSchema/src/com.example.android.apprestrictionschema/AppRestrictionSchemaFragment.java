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

package com.example.android.apprestrictionschema;

import android.content.Context;
import android.content.RestrictionEntry;
import android.content.RestrictionsManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.common.logger.Log;

import java.util.List;

/**
 * Pressing the button on this fragment pops up a simple Toast message. The button is enabled or
 * disabled according to the restrictions set by device/profile owner. You can use the
 * AppRestrictionEnforcer sample as a profile owner for this.
 */
public class AppRestrictionSchemaFragment extends Fragment implements View.OnClickListener {

    // Tag for the logger
    private static final String TAG = "AppRestrictionSchemaFragment";

    private static final String KEY_CAN_SAY_HELLO = "can_say_hello";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_NUMBER = "number";
    private static final String KEY_RANK = "rank";
    private static final String KEY_APPROVALS = "approvals";
    private static final String KEY_PROFILE = "profile";
    private static final String KEY_PROFILE_NAME = "name";
    private static final String KEY_PROFILE_AGE = "age";
    private static final String KEY_ITEMS = "items";
    private static final String KEY_ITEM_KEY = "key";
    private static final String KEY_ITEM_VALUE = "value";

    private static final boolean BUNDLE_SUPPORTED = Build.VERSION.SDK_INT >= 23;

    // Message to show when the button is clicked (String restriction)
    private String mMessage;

    // UI Components
    private TextView mTextSayHello;
    private Button mButtonSayHello;
    private TextView mTextNumber;
    private TextView mTextRank;
    private TextView mTextApprovals;
    private TextView mTextProfile;
    private TextView mTextItems;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_app_restriction_schema, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mTextSayHello = (TextView) view.findViewById(R.id.say_hello_explanation);
        mButtonSayHello = (Button) view.findViewById(R.id.say_hello);
        mTextNumber = (TextView) view.findViewById(R.id.your_number);
        mTextRank = (TextView) view.findViewById(R.id.your_rank);
        mTextApprovals = (TextView) view.findViewById(R.id.approvals_you_have);
        View bundleSeparator = view.findViewById(R.id.bundle_separator);
        mTextProfile = (TextView) view.findViewById(R.id.your_profile);
        View bundleArraySeparator = view.findViewById(R.id.bundle_array_separator);
        mTextItems = (TextView) view.findViewById(R.id.your_items);
        mButtonSayHello.setOnClickListener(this);
        if (BUNDLE_SUPPORTED) {
            bundleSeparator.setVisibility(View.VISIBLE);
            mTextProfile.setVisibility(View.VISIBLE);
            bundleArraySeparator.setVisibility(View.VISIBLE);
            mTextItems.setVisibility(View.VISIBLE);
        } else {
            bundleSeparator.setVisibility(View.GONE);
            mTextProfile.setVisibility(View.GONE);
            bundleArraySeparator.setVisibility(View.GONE);
            mTextItems.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        resolveRestrictions();
    }

    private void resolveRestrictions() {
        RestrictionsManager manager =
                (RestrictionsManager) getActivity().getSystemService(Context.RESTRICTIONS_SERVICE);
        Bundle restrictions = manager.getApplicationRestrictions();
        List<RestrictionEntry> entries = manager.getManifestRestrictions(
                getActivity().getApplicationContext().getPackageName());
        for (RestrictionEntry entry : entries) {
            String key = entry.getKey();
            Log.d(TAG, "key: " + key);
            if (key.equals(KEY_CAN_SAY_HELLO)) {
                updateCanSayHello(entry, restrictions);
            } else if (key.equals(KEY_MESSAGE)) {
                updateMessage(entry, restrictions);
            } else if (key.equals(KEY_NUMBER)) {
                updateNumber(entry, restrictions);
            } else if (key.equals(KEY_RANK)) {
                updateRank(entry, restrictions);
            } else if (key.equals(KEY_APPROVALS)) {
                updateApprovals(entry, restrictions);
            } else if (key.equals(KEY_PROFILE)) {
                updateProfile(entry, restrictions);
            } else if (key.equals(KEY_ITEMS)) {
                updateItems(entry, restrictions);
            }
        }
    }

    private void updateCanSayHello(RestrictionEntry entry, Bundle restrictions) {
        boolean canSayHello;
        if (restrictions == null || !restrictions.containsKey(KEY_CAN_SAY_HELLO)) {
            canSayHello = entry.getSelectedState();
        } else {
            canSayHello = restrictions.getBoolean(KEY_CAN_SAY_HELLO);
        }
        mTextSayHello.setText(canSayHello ?
                R.string.explanation_can_say_hello_true :
                R.string.explanation_can_say_hello_false);
        mButtonSayHello.setEnabled(canSayHello);
    }

    private void updateMessage(RestrictionEntry entry, Bundle restrictions) {
        if (restrictions == null || !restrictions.containsKey(KEY_MESSAGE)) {
            mMessage = entry.getSelectedString();
        } else {
            mMessage = restrictions.getString(KEY_MESSAGE);
        }
    }

    private void updateNumber(RestrictionEntry entry, Bundle restrictions) {
        int number;
        if (restrictions == null || !restrictions.containsKey(KEY_NUMBER)) {
            number = entry.getIntValue();
        } else {
            number = restrictions.getInt(KEY_NUMBER);
        }
        mTextNumber.setText(getString(R.string.your_number, number));
    }

    private void updateRank(RestrictionEntry entry, Bundle restrictions) {
        String rank;
        if (restrictions == null || !restrictions.containsKey(KEY_RANK)) {
            rank = entry.getSelectedString();
        } else {
            rank = restrictions.getString(KEY_RANK);
        }
        mTextRank.setText(getString(R.string.your_rank, rank));
    }

    private void updateApprovals(RestrictionEntry entry, Bundle restrictions) {
        String[] approvals;
        if (restrictions == null || !restrictions.containsKey(KEY_APPROVALS)) {
            approvals = entry.getAllSelectedStrings();
        } else {
            approvals = restrictions.getStringArray(KEY_APPROVALS);
        }
        String text;
        if (approvals == null || approvals.length == 0) {
            text = getString(R.string.none);
        } else {
            text = TextUtils.join(", ", approvals);
        }
        mTextApprovals.setText(getString(R.string.approvals_you_have, text));
    }

    private void updateProfile(RestrictionEntry entry, Bundle restrictions) {
        if (!BUNDLE_SUPPORTED) {
            return;
        }
        String name = null;
        int age = 0;
        if (restrictions == null || !restrictions.containsKey(KEY_PROFILE)) {
            RestrictionEntry[] entries = entry.getRestrictions();
            for (RestrictionEntry profileEntry : entries) {
                String key = profileEntry.getKey();
                if (key.equals(KEY_PROFILE_NAME)) {
                    name = profileEntry.getSelectedString();
                } else if (key.equals(KEY_PROFILE_AGE)) {
                    age = profileEntry.getIntValue();
                }
            }
        } else {
            Bundle profile = restrictions.getBundle(KEY_PROFILE);
            if (profile != null) {
                name = profile.getString(KEY_PROFILE_NAME);
                age = profile.getInt(KEY_PROFILE_AGE);
            }
        }
        mTextProfile.setText(getString(R.string.your_profile, name, age));
    }

    private void updateItems(RestrictionEntry entry, Bundle restrictions) {
        if (!BUNDLE_SUPPORTED) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        if (restrictions != null) {
            Parcelable[] parcelables = restrictions.getParcelableArray(KEY_ITEMS);
            if (parcelables != null && parcelables.length > 0) {
                Bundle[] items = new Bundle[parcelables.length];
                for (int i = 0; i < parcelables.length; i++) {
                    items[i] = (Bundle) parcelables[i];
                }
                boolean first = true;
                for (Bundle item : items) {
                    if (!item.containsKey(KEY_ITEM_KEY) || !item.containsKey(KEY_ITEM_VALUE)) {
                        continue;
                    }
                    if (first) {
                        first = false;
                    } else {
                        builder.append(", ");
                    }
                    builder.append(item.getString(KEY_ITEM_KEY));
                    builder.append(":");
                    builder.append(item.getString(KEY_ITEM_VALUE));
                }
            } else {
                builder.append(getString(R.string.none));
            }
        } else {
            builder.append(getString(R.string.none));
        }
        mTextItems.setText(getString(R.string.your_items, builder));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.say_hello: {
                Toast.makeText(getActivity(), getString(R.string.message, mMessage),
                        Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }

}
