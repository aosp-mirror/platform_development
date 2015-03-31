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
import android.os.Bundle;
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

    // Message to show when the button is clicked (String restriction)
    private String mMessage;

    // UI Components
    private TextView mTextSayHello;
    private Button mButtonSayHello;
    private TextView mTextNumber;
    private TextView mTextRank;
    private TextView mTextApprovals;

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
        mButtonSayHello.setOnClickListener(this);
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
        List<RestrictionEntry> entries = manager.getManifestRestrictions(getActivity().getApplicationContext().getPackageName());
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
