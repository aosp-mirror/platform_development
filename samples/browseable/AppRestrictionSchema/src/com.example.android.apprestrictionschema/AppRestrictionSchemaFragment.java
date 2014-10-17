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
import android.content.RestrictionsManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.common.logger.Log;

/**
 * Pressing the button on this fragment pops up a simple Toast message. The button is enabled or
 * disabled according to the restrictions set by device/profile owner. You can use the
 * AppRestrictionEnforcer sample as a profile owner for this.
 */
public class AppRestrictionSchemaFragment extends Fragment implements View.OnClickListener {

    // Tag for the logger
    private static final String TAG = "AppRestrictionSchemaFragment";

    // UI Components
    private TextView mTextSayHello;
    private Button mButtonSayHello;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_app_restriction_schema, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mTextSayHello = (TextView) view.findViewById(R.id.say_hello_explanation);
        mButtonSayHello = (Button) view.findViewById(R.id.say_hello);
        mButtonSayHello.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update the UI according to the configured restrictions
        RestrictionsManager restrictionsManager =
                (RestrictionsManager) getActivity().getSystemService(Context.RESTRICTIONS_SERVICE);
        Bundle restrictions = restrictionsManager.getApplicationRestrictions();
        updateUI(restrictions);
    }

    private void updateUI(Bundle restrictions) {
        if (canSayHello(restrictions)) {
            mTextSayHello.setText(R.string.explanation_can_say_hello_true);
            mButtonSayHello.setEnabled(true);
        } else {
            mTextSayHello.setText(R.string.explanation_can_say_hello_false);
            mButtonSayHello.setEnabled(false);
        }
    }

    /**
     * Returns the current status of the restriction.
     *
     * @param restrictions The application restrictions
     * @return True if the app is allowed to say hello
     */
    private boolean canSayHello(Bundle restrictions) {
        final boolean defaultValue = false;
        boolean canSayHello = restrictions == null ? defaultValue :
                restrictions.getBoolean("can_say_hello", defaultValue);
        Log.d(TAG, "canSayHello: " + canSayHello);
        return canSayHello;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.say_hello: {
                Toast.makeText(getActivity(), R.string.message_hello, Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }

}
