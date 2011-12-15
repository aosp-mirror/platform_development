/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.example.training.deviceadmin;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Main Activity for the sample application.
 *
 * The Activity maintains and presents 2 different
 * screens to the user -- a screen for configuring device administration policy and another screen
 * for viewing configured policy.  When it is detected that the screen-lock password satisfies
 * the password strength required by the policy, the user is sent to an Activity containing
 * protected content.
 */
public class PolicySetupActivity extends Activity {
    private static final int REQ_ACTIVATE_DEVICE_ADMIN = 10;
    private static final String SCREEN_ID_KEY = "LAYOUT_ID";

    private static final String APP_PREF = "APP_PREF";
    private static final int UNKNOWN_SCREEN_ID = -1;

    // UI controls in policy setup screen.
    private Spinner mPasswordQualityInputField;
    private EditText mPasswordLengthInputField;
    private EditText mPasswordMinUppercaseInputField;

    private Policy mPolicy;
    private int mCurrentScreenId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPolicy = new Policy(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences(APP_PREF, MODE_PRIVATE);
        final int savedScreenId = prefs.getInt(SCREEN_ID_KEY, UNKNOWN_SCREEN_ID);
        if (savedScreenId == UNKNOWN_SCREEN_ID || !mPolicy.isAdminActive()) {
            setScreenContent(R.layout.activity_policy_setup);
        } else {
            setScreenContent(savedScreenId);
        }
    }

    private void setScreenContent(final int screenId) {
        mCurrentScreenId = screenId;
        setContentView(mCurrentScreenId);
        getSharedPreferences(APP_PREF, MODE_PRIVATE).edit().putInt(
                SCREEN_ID_KEY, mCurrentScreenId).commit();
        switch (mCurrentScreenId) {
        case R.layout.activity_policy_setup:
            initPolicySetupScreen();
            initNavigation();
            break;
        case R.layout.activity_view_policy:
            initViewPolicyScreen();
            initNavigation();
            break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCurrentScreenId == R.layout.activity_policy_setup) writePolicy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_ACTIVATE_DEVICE_ADMIN && resultCode == RESULT_OK) {
            // User just activated the application as a device administrator.
            setScreenContent(mCurrentScreenId);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        if (mCurrentScreenId == R.layout.activity_view_policy) {
            setScreenContent(R.layout.activity_policy_setup);
            return;
        }
        super.onBackPressed();
    }

    // Initialize policy set up screen.
    private void initPolicySetupScreen() {
        mPasswordQualityInputField = (Spinner) findViewById(R.id.policy_password_quality);
        mPasswordLengthInputField = (EditText) findViewById(R.id.policy_password_length);
        mPasswordMinUppercaseInputField = (EditText) findViewById(R.id.policy_password_uppercase);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.password_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPasswordQualityInputField.setAdapter(adapter);
        mPasswordQualityInputField.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                LinearLayout passwordMinUppercaseView =
                        (LinearLayout) findViewById(R.id.password_uppercase_view);
                // The minimum number of upper case field is only applicable for password
                // qualities: alpha, alphanumeric, or complex.
                if (pos > 2)
                    passwordMinUppercaseView.setVisibility(View.VISIBLE);
                else
                    passwordMinUppercaseView.setVisibility(View.GONE);
            }

            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Read previously saved policy and populate on the UI.
        mPolicy.readFromLocal();
        mPasswordQualityInputField.setSelection(mPolicy.getPasswordQuality());
        if (mPolicy.getPasswordLength() > 0) {
            mPasswordLengthInputField.setText(String.valueOf(mPolicy.getPasswordLength()));
        } else {
            mPasswordLengthInputField.setText("");
        }

        if (mPolicy.getPasswordMinUpperCase() > 0) {
            mPasswordMinUppercaseInputField.setText(
                    String.valueOf(mPolicy.getPasswordMinUpperCase()));
        } else {
            mPasswordMinUppercaseInputField.setText("");
        }
    }

    // Initialize policy viewing screen.
    private void initViewPolicyScreen() {
        TextView passwordQualityView = (TextView) findViewById(R.id.policy_password_quality);
        TextView passwordLengthView = (TextView) findViewById(R.id.policy_password_length);

        // Read previously saved policy and populate on the UI.
        mPolicy.readFromLocal();
        int passwordQualitySelection = mPolicy.getPasswordQuality();
        passwordQualityView.setText(
                getResources().getStringArray(R.array.password_types)[passwordQualitySelection]);
        passwordLengthView.setText(String.valueOf(mPolicy.getPasswordLength()));
        if (passwordQualitySelection > 2) {
            LinearLayout passwordMinUppercaseView =
                    (LinearLayout) findViewById(R.id.password_uppercase_view);
            passwordMinUppercaseView.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.policy_password_uppercase)).setText(
                    String.valueOf(mPolicy.getPasswordMinUpperCase()));
        }
    }

    // Set up navigation message and action button.
    private void initNavigation() {
        if (!mPolicy.isAdminActive()) {
            // Activates device administrator.
            setupNavigation(R.string.setup_message_activate,
                    R.string.setup_action_activate,
                    mActivateButtonListener);
        } else if (mCurrentScreenId == R.layout.activity_policy_setup) {
            setupNavigation(R.string.setup_message_set_policy,
                    R.string.setup_action_set_policy,
                    new View.OnClickListener() {
                        public void onClick(View view) {
                            writePolicy();
                            mPolicy.configurePolicy();
                            setScreenContent(R.layout.activity_view_policy);
                        }
                    });
        }
        else if (!mPolicy.isActivePasswordSufficient()) {
            // Launches password set-up screen in Settings.
            setupNavigation(R.string.setup_message_enforce_policy,
                    R.string.setup_action_enforce_policy,
                    mEnforcePolicyListener);
        } else {
            // Grants access to secure content.
            setupNavigation(R.string.setup_message_go_secured,
                    R.string.setup_action_go_secured,
                    new View.OnClickListener() {
                        public void onClick(View view) {
                            startActivity(new Intent(view.getContext(), SecureActivity.class));
                        }
                    });
        }
    }

    private View.OnClickListener mActivateButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // First, persist the policy.  Then, launch intent to trigger the system screen
            // requesting user to confirm the activation of the device administrator.
            writePolicy();
            Intent activateDeviceAdminIntent =
                new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            activateDeviceAdminIntent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    mPolicy.getPolicyAdmin());
            // It is good practice to include the optional explanation text to explain to
            // user why the application is requesting to be a device administrator.  The system
            // will display this message on the activation screen.
            activateDeviceAdminIntent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getResources().getString(R.string.device_admin_activation_message));
            startActivityForResult(activateDeviceAdminIntent, REQ_ACTIVATE_DEVICE_ADMIN);
        }
    };

    private View.OnClickListener mEnforcePolicyListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            writePolicy();
            // The device administration API does not "fix" the password if it is
            // determined that the current password does not conform to what is requested
            // by the policy.  The caller is responsible for triggering the password set up
            // screen via the below intent.
            Intent intent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
            startActivity(intent);
        }
    };

    // Setup action button text and listener.
    private void setupNavigation(int messageResId, int buttonTextResId,
            View.OnClickListener listener) {
        TextView setupMessage = (TextView) findViewById(R.id.setup_message);
        setupMessage.setText(messageResId);
        Button actionBtn = (Button) findViewById(R.id.setup_action_btn);
        actionBtn.setText(buttonTextResId);
        actionBtn.setOnClickListener(listener);
    }

    // Save policy to shared preferences.
    private void writePolicy() {
        int passwordQuality = (int) mPasswordQualityInputField.getSelectedItemId();

        int passwordLength = 0;
        try {
            passwordLength = Integer.valueOf(mPasswordLengthInputField.getText().toString());
        } catch (NumberFormatException nfe) {}  // Defaults to 0.

        int passwordMinUppercase = 0;
        try {
            passwordMinUppercase =
                    Integer.valueOf(mPasswordMinUppercaseInputField.getText().toString());
        } catch (NumberFormatException nfe) {}  // Defaults to 0.

        mPolicy.saveToLocal(passwordQuality, passwordLength, passwordMinUppercase);
    }
}

