/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.example.android.apis.app;

import com.example.android.apis.R;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

/**
 * Example of a do-nothing admin class.  When enabled, it lets you control
 * some of its policy and reports when there is interesting activity.
 */
public class DeviceAdminSample extends DeviceAdminReceiver {

    private static final String TAG = "DeviceAdminSample";
    private static final long MS_PER_DAY = 86400 * 1000;
    private static final long MS_PER_HOUR = 3600 * 1000;
    private static final long MS_PER_MINUTE = 60 * 1000;

    static SharedPreferences getSamplePreferences(Context context) {
        return context.getSharedPreferences(DeviceAdminReceiver.class.getName(), 0);
    }

    static String PREF_PASSWORD_QUALITY = "password_quality";
    static String PREF_PASSWORD_LENGTH = "password_length";
    static String PREF_PASSWORD_MINIMUM_LETTERS = "password_minimum_letters";
    static String PREF_PASSWORD_MINIMUM_UPPERCASE = "password_minimum_uppercase";
    static String PREF_PASSWORD_MINIMUM_LOWERCASE = "password_minimum_lowercase";
    static String PREF_PASSWORD_MINIMUM_NUMERIC = "password_minimum_numeric";
    static String PREF_PASSWORD_MINIMUM_SYMBOLS = "password_minimum_symbols";
    static String PREF_PASSWORD_MINIMUM_NONLETTER = "password_minimum_nonletter";
    static String PREF_PASSWORD_HISTORY_LENGTH = "password_history_length";
    static String PREF_PASSWORD_EXPIRATION_TIMEOUT = "password_expiration_timeout";
    static String PREF_MAX_FAILED_PW = "max_failed_pw";

    void showToast(Context context, CharSequence msg) {
        Toast.makeText(context, "Sample Device Admin: " + msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        showToast(context, "enabled");
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "This is an optional message to warn the user about disabling.";
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        showToast(context, "disabled");
    }

    @Override
    public void onPasswordChanged(Context context, Intent intent) {
        showToast(context, "pw changed");
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        showToast(context, "pw failed");
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
        showToast(context, "pw succeeded");
    }

    static String countdownString(long time) {
        long days = time / MS_PER_DAY;
        long hours = (time / MS_PER_HOUR) % 24;
        long minutes = (time / MS_PER_MINUTE) % 60;
        return days + "d" + hours + "h" + minutes + "m";
    }

    @Override
    public void onPasswordExpiring(Context context, Intent intent) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        long expr = dpm.getPasswordExpiration(new ComponentName(context, DeviceAdminSample.class));
        long delta = expr - System.currentTimeMillis();
        boolean expired = delta < 0L;
        String msg = expired ? "Password expired " : "Password will expire "
                + countdownString(Math.abs(delta))
                + (expired ? " ago" : " from now");
        showToast(context, msg);
        Log.v(TAG, msg);
    }

    /**
     * <p>UI control for the sample device admin.  This provides an interface
     * to enable, disable, and perform other operations with it to see
     * their effect.</p>
     *
     * <p>Note that this is implemented as an inner class only keep the sample
     * all together; typically this code would appear in some separate class.
     */
    public static class Controller extends Activity {

        static final int REQUEST_CODE_ENABLE_ADMIN = 1;
        static final int REQUEST_CODE_START_ENCRYPTION = 2;

        private static final long MS_PER_MINUTE = 60*1000;

        DevicePolicyManager mDPM;
        ActivityManager mAM;
        ComponentName mDeviceAdminSample;

        Button mEnableButton;
        Button mDisableButton;

        // Password quality spinner choices
        // This list must match the list found in samples/ApiDemos/res/values/arrays.xml
        final static int mPasswordQualityValues[] = new int[] {
            DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED,
            DevicePolicyManager.PASSWORD_QUALITY_SOMETHING,
            DevicePolicyManager.PASSWORD_QUALITY_NUMERIC,
            DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC,
            DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC,
            DevicePolicyManager.PASSWORD_QUALITY_COMPLEX
        };

        Spinner mPasswordQuality;
        EditText mPasswordLength;
        EditText mPasswordMinimumLetters;
        EditText mPasswordMinimumUppercase;
        EditText mPasswordMinimumLowercase;
        EditText mPasswordMinimumNumeric;
        EditText mPasswordMinimumSymbols;
        EditText mPasswordMinimumNonLetter;
        EditText mPasswordHistoryLength;
        Button mSetPasswordButton;

        EditText mPassword;
        Button mResetPasswordButton;

        EditText mMaxFailedPw;

        Button mForceLockButton;
        Button mWipeDataButton;
        Button mWipeAllDataButton;

        private Button mTimeoutButton;

        private EditText mTimeout;

        private EditText mPasswordExpirationTimeout;
        private Button mPasswordExpirationButton;
        private TextView mPasswordExpirationStatus;
        private Button mPasswordExpirationStatusButton;

        private Button mEnableEncryptionButton;
        private Button mDisableEncryptionButton;
        private Button mActivateEncryptionButton;
        private Button mEncryptionStatusButton;
        private TextView mEncryptionStatus;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
            mAM = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
            mDeviceAdminSample = new ComponentName(Controller.this, DeviceAdminSample.class);

            setContentView(R.layout.device_admin_sample);

            // Watch for button clicks.
            mEnableButton = (Button)findViewById(R.id.enable);
            mEnableButton.setOnClickListener(mEnableListener);
            mDisableButton = (Button)findViewById(R.id.disable);
            mDisableButton.setOnClickListener(mDisableListener);

            mPasswordQuality = (Spinner)findViewById(R.id.password_quality);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    this, R.array.password_qualities, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mPasswordQuality.setAdapter(adapter);
            mPasswordQuality.setOnItemSelectedListener(
                    new OnItemSelectedListener() {
                        public void onItemSelected(
                                AdapterView<?> parent, View view, int position, long id) {
                            setPasswordQuality(mPasswordQualityValues[position]);
                        }

                        public void onNothingSelected(AdapterView<?> parent) {
                            setPasswordQuality(DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
                        }
                    });
            mPasswordLength = (EditText)findViewById(R.id.password_length);
            mPasswordLength.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                }
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        setPasswordLength(Integer.parseInt(s.toString()));
                    } catch (NumberFormatException e) {
                    }
                }
            });
            mPasswordMinimumLetters = (EditText)findViewById(R.id.password_minimum_letters);
            mPasswordMinimumLetters.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                }
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        setPasswordMinimumLetters(Integer.parseInt(s.toString()));
                    } catch (NumberFormatException e) {
                    }
                }
            });
            mPasswordMinimumUppercase = (EditText)findViewById(R.id.password_minimum_uppercase);
            mPasswordMinimumUppercase.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                }
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        setPasswordMinimumUppercase(Integer.parseInt(s.toString()));
                    } catch (NumberFormatException e) {
                    }
                }
            });
            mPasswordMinimumLowercase = (EditText)findViewById(R.id.password_minimum_lowercase);
            mPasswordMinimumLowercase.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                }
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        setPasswordMinimumLowercase(Integer.parseInt(s.toString()));
                    } catch (NumberFormatException e) {
                    }
                }
            });
            mPasswordMinimumNumeric = (EditText)findViewById(R.id.password_minimum_numeric);
            mPasswordMinimumNumeric.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                }
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        setPasswordMinimumNumeric(Integer.parseInt(s.toString()));
                    } catch (NumberFormatException e) {
                    }
                }
            });
            mPasswordMinimumSymbols = (EditText)findViewById(R.id.password_minimum_symbols);
            mPasswordMinimumSymbols.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                }
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        setPasswordMinimumSymbols(Integer.parseInt(s.toString()));
                    } catch (NumberFormatException e) {
                    }
                }
            });
            mPasswordMinimumNonLetter = (EditText)findViewById(R.id.password_minimum_nonletter);
            mPasswordMinimumNonLetter.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                }
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        setPasswordMinimumNonLetter(Integer.parseInt(s.toString()));
                    } catch (NumberFormatException e) {
                    }
                }
            });
            mPasswordHistoryLength = (EditText)findViewById(R.id.password_history_length);
            mPasswordHistoryLength.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                }
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        setPasswordHistoryLength(Integer.parseInt(s.toString()));
                    } catch (NumberFormatException e) {
                    }
                }
            });

            mPasswordExpirationTimeout = (EditText)findViewById(R.id.password_expiration);
            mPasswordExpirationButton = (Button) findViewById(R.id.update_expiration_button);
            mPasswordExpirationButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    try {
                        setPasswordExpiration(
                                Long.parseLong(mPasswordExpirationTimeout.getText().toString()));
                    } catch (NumberFormatException nfe) {
                    }
                    updatePasswordExpirationStatus();
                }
            });

            mPasswordExpirationStatus = (TextView) findViewById(R.id.password_expiration_status);
            mPasswordExpirationStatusButton =
                    (Button) findViewById(R.id.update_expiration_status_button);
            mPasswordExpirationStatusButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    updatePasswordExpirationStatus();
                }
            });

            mSetPasswordButton = (Button)findViewById(R.id.set_password);
            mSetPasswordButton.setOnClickListener(mSetPasswordListener);

            mPassword = (EditText)findViewById(R.id.password);
            mResetPasswordButton = (Button)findViewById(R.id.reset_password);
            mResetPasswordButton.setOnClickListener(mResetPasswordListener);

            mMaxFailedPw = (EditText)findViewById(R.id.max_failed_pw);
            mMaxFailedPw.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                }
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        int maxFailCount = Integer.parseInt(s.toString());
                        if (maxFailCount > 0) {
                            Toast.makeText(Controller.this, "WARNING: Phone will wipe after " +
                                    s + " incorrect passwords", Toast.LENGTH_SHORT).show();
                        }
                        setMaxFailedPw(maxFailCount);
                    } catch (NumberFormatException e) {
                    }
                }
            });

            mForceLockButton = (Button)findViewById(R.id.force_lock);
            mForceLockButton.setOnClickListener(mForceLockListener);
            mWipeDataButton = (Button)findViewById(R.id.wipe_data);
            mWipeDataButton.setOnClickListener(mWipeDataListener);
            mWipeAllDataButton = (Button)findViewById(R.id.wipe_all_data);
            mWipeAllDataButton.setOnClickListener(mWipeDataListener);

            mTimeout = (EditText) findViewById(R.id.timeout);
            mTimeoutButton = (Button) findViewById(R.id.set_timeout);
            mTimeoutButton.setOnClickListener(mSetTimeoutListener);

            mEnableEncryptionButton = (Button) findViewById(R.id.encryption_enable_button);
            mEnableEncryptionButton.setOnClickListener(mEncryptionButtonListener);
            mDisableEncryptionButton = (Button) findViewById(R.id.encryption_disable_button);
            mDisableEncryptionButton.setOnClickListener(mEncryptionButtonListener);
            mActivateEncryptionButton = (Button) findViewById(R.id.encryption_activate_button);
            mActivateEncryptionButton.setOnClickListener(mEncryptionButtonListener);
            mEncryptionStatusButton = (Button) findViewById(R.id.encryption_update_status_button);
            mEncryptionStatusButton.setOnClickListener(mEncryptionButtonListener);
            mEncryptionStatus = (TextView) findViewById(R.id.encryption_status);
        }

        void updateButtonStates() {
            boolean active = mDPM.isAdminActive(mDeviceAdminSample);
            if (active) {
                mEnableButton.setEnabled(false);
                mDisableButton.setEnabled(true);
                mPasswordQuality.setEnabled(true);
                mPasswordLength.setEnabled(true);
                mPasswordMinimumLetters.setEnabled(true);
                mPasswordMinimumUppercase.setEnabled(true);
                mPasswordMinimumLowercase.setEnabled(true);
                mPasswordMinimumSymbols.setEnabled(true);
                mPasswordMinimumNumeric.setEnabled(true);
                mPasswordMinimumNonLetter.setEnabled(true);
                mPasswordHistoryLength.setEnabled(true);
                mSetPasswordButton.setEnabled(true);
                mPassword.setEnabled(true);
                mResetPasswordButton.setEnabled(true);
                mForceLockButton.setEnabled(true);
                mWipeDataButton.setEnabled(true);
                mWipeAllDataButton.setEnabled(true);
                mEnableEncryptionButton.setEnabled(true);
                mDisableEncryptionButton.setEnabled(true);
                mActivateEncryptionButton.setEnabled(true);
                mEncryptionStatusButton.setEnabled(true);
            } else {
                mEnableButton.setEnabled(true);
                mDisableButton.setEnabled(false);
                mPasswordQuality.setEnabled(false);
                mPasswordLength.setEnabled(false);
                mPasswordMinimumLetters.setEnabled(false);
                mPasswordMinimumUppercase.setEnabled(false);
                mPasswordMinimumLowercase.setEnabled(false);
                mPasswordMinimumSymbols.setEnabled(false);
                mPasswordMinimumNumeric.setEnabled(false);
                mPasswordMinimumNonLetter.setEnabled(false);
                mPasswordHistoryLength.setEnabled(false);
                mSetPasswordButton.setEnabled(false);
                mPassword.setEnabled(false);
                mResetPasswordButton.setEnabled(false);
                mForceLockButton.setEnabled(false);
                mWipeDataButton.setEnabled(false);
                mWipeAllDataButton.setEnabled(false);
                mEnableEncryptionButton.setEnabled(false);
                mDisableEncryptionButton.setEnabled(false);
                mActivateEncryptionButton.setEnabled(false);
                mEncryptionStatusButton.setEnabled(false);
            }
        }

        void updateControls() {
            SharedPreferences prefs = getSamplePreferences(this);
            final int pwQuality = prefs.getInt(PREF_PASSWORD_QUALITY,
                    DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
            final int pwLength = prefs.getInt(PREF_PASSWORD_LENGTH, 0);
            final int pwMinLetters = prefs.getInt(PREF_PASSWORD_MINIMUM_LETTERS, 0);
            final int pwMinUppercase = prefs.getInt(PREF_PASSWORD_MINIMUM_UPPERCASE, 0);
            final int pwMinLowercase = prefs.getInt(PREF_PASSWORD_MINIMUM_LOWERCASE, 0);
            final int pwMinNumeric = prefs.getInt(PREF_PASSWORD_MINIMUM_NUMERIC, 0);
            final int pwMinSymbols = prefs.getInt(PREF_PASSWORD_MINIMUM_SYMBOLS, 0);
            final int pwMinNonLetter = prefs.getInt(PREF_PASSWORD_MINIMUM_NONLETTER, 0);
            final int pwHistoryLength = prefs.getInt(PREF_PASSWORD_HISTORY_LENGTH, 0);
            final long pwExpirationTimeout = prefs.getLong(PREF_PASSWORD_EXPIRATION_TIMEOUT, 0L);
            final int maxFailedPw = prefs.getInt(PREF_MAX_FAILED_PW, 0);

            for (int i=0; i<mPasswordQualityValues.length; i++) {
                if (mPasswordQualityValues[i] == pwQuality) {
                    mPasswordQuality.setSelection(i);
                }
            }
            mPasswordLength.setText(Integer.toString(pwLength));
            mPasswordMinimumLetters.setText(Integer.toString(pwMinLetters));
            mPasswordMinimumUppercase.setText(Integer.toString(pwMinUppercase));
            mPasswordMinimumLowercase.setText(Integer.toString(pwMinLowercase));
            mPasswordMinimumSymbols.setText(Integer.toString(pwMinSymbols));
            mPasswordMinimumNumeric.setText(Integer.toString(pwMinNumeric));
            mPasswordMinimumNonLetter.setText(Integer.toString(pwMinNonLetter));
            mPasswordHistoryLength.setText(Integer.toString(pwHistoryLength));
            mPasswordExpirationTimeout.setText(Long.toString(pwExpirationTimeout/MS_PER_MINUTE));
            mMaxFailedPw.setText(Integer.toString(maxFailedPw));
        }

        void updatePasswordExpirationStatus() {
            boolean active = mDPM.isAdminActive(mDeviceAdminSample);
            String statusText;
            if (active) {
                long now = System.currentTimeMillis();
                // We'll query the DevicePolicyManager twice - first for the expiration values
                // set by the sample app, and later, for the system values (which may be different
                // if there is another administrator active.)
                long expirationDate = mDPM.getPasswordExpiration(mDeviceAdminSample);
                long mSecUntilExpiration = expirationDate - now;
                if (mSecUntilExpiration >= 0) {
                    statusText = "Expiration in " + countdownString(mSecUntilExpiration);
                } else {
                    statusText = "Expired " + countdownString(-mSecUntilExpiration) + " ago";
                }

                // expirationTimeout is the cycle time between required password refresh
                long expirationTimeout = mDPM.getPasswordExpirationTimeout(mDeviceAdminSample);
                statusText += " / timeout period " + countdownString(expirationTimeout);

                // Now report the aggregate (global) expiration time
                statusText += " / Aggregate ";
                expirationDate = mDPM.getPasswordExpiration(null);
                mSecUntilExpiration = expirationDate - now;
                if (mSecUntilExpiration >= 0) {
                    statusText += "expiration in " + countdownString(mSecUntilExpiration);
                } else {
                    statusText += "expired " + countdownString(-mSecUntilExpiration) + " ago";
                }
            } else {
                statusText = "<inactive>";
            }
            mPasswordExpirationStatus.setText(statusText);
        }

        void updatePolicies() {
            SharedPreferences prefs = getSamplePreferences(this);
            final int pwQuality = prefs.getInt(PREF_PASSWORD_QUALITY,
                    DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
            final int pwLength = prefs.getInt(PREF_PASSWORD_LENGTH, 0);
            final int pwMinLetters = prefs.getInt(PREF_PASSWORD_MINIMUM_LETTERS, 0);
            final int pwMinUppercase = prefs.getInt(PREF_PASSWORD_MINIMUM_UPPERCASE, 0);
            final int pwMinLowercase = prefs.getInt(PREF_PASSWORD_MINIMUM_LOWERCASE, 0);
            final int pwMinNumeric = prefs.getInt(PREF_PASSWORD_MINIMUM_NUMERIC, 0);
            final int pwMinSymbols = prefs.getInt(PREF_PASSWORD_MINIMUM_SYMBOLS, 0);
            final int pwMinNonLetter = prefs.getInt(PREF_PASSWORD_MINIMUM_NONLETTER, 0);
            final int pwHistoryLength = prefs.getInt(PREF_PASSWORD_HISTORY_LENGTH, 0);
            final long pwExpiration = prefs.getLong(PREF_PASSWORD_EXPIRATION_TIMEOUT, 0L);
            final int maxFailedPw = prefs.getInt(PREF_MAX_FAILED_PW, 0);

            boolean active = mDPM.isAdminActive(mDeviceAdminSample);
            if (active) {
                mDPM.setPasswordQuality(mDeviceAdminSample, pwQuality);
                mDPM.setPasswordMinimumLength(mDeviceAdminSample, pwLength);
                mDPM.setPasswordMinimumLetters(mDeviceAdminSample, pwMinLetters);
                mDPM.setPasswordMinimumUpperCase(mDeviceAdminSample, pwMinUppercase);
                mDPM.setPasswordMinimumLowerCase(mDeviceAdminSample, pwMinLowercase);
                mDPM.setPasswordMinimumNumeric(mDeviceAdminSample, pwMinNumeric);
                mDPM.setPasswordMinimumSymbols(mDeviceAdminSample, pwMinSymbols);
                mDPM.setPasswordMinimumNonLetter(mDeviceAdminSample, pwMinNonLetter);
                mDPM.setPasswordHistoryLength(mDeviceAdminSample, pwHistoryLength);
                mDPM.setMaximumFailedPasswordsForWipe(mDeviceAdminSample, maxFailedPw);
                mDPM.setPasswordExpirationTimeout(mDeviceAdminSample, pwExpiration);
            }
        }

        void setPasswordQuality(int quality) {
            SharedPreferences prefs = getSamplePreferences(this);
            prefs.edit().putInt(PREF_PASSWORD_QUALITY, quality).commit();
            updatePolicies();
        }

        void setPasswordLength(int length) {
            SharedPreferences prefs = getSamplePreferences(this);
            prefs.edit().putInt(PREF_PASSWORD_LENGTH, length).commit();
            updatePolicies();
        }

        void setPasswordMinimumLetters(int length) {
            SharedPreferences prefs = getSamplePreferences(this);
            prefs.edit().putInt(PREF_PASSWORD_MINIMUM_LETTERS, length).commit();
            updatePolicies();
        }

        void setPasswordMinimumUppercase(int length) {
            SharedPreferences prefs = getSamplePreferences(this);
            prefs.edit().putInt(PREF_PASSWORD_MINIMUM_UPPERCASE, length).commit();
            updatePolicies();
        }

        void setPasswordMinimumLowercase(int length) {
            SharedPreferences prefs = getSamplePreferences(this);
            prefs.edit().putInt(PREF_PASSWORD_MINIMUM_LOWERCASE, length).commit();
            updatePolicies();
        }

        void setPasswordMinimumNumeric(int length) {
            SharedPreferences prefs = getSamplePreferences(this);
            prefs.edit().putInt(PREF_PASSWORD_MINIMUM_NUMERIC, length).commit();
            updatePolicies();
        }

        void setPasswordMinimumSymbols(int length) {
            SharedPreferences prefs = getSamplePreferences(this);
            prefs.edit().putInt(PREF_PASSWORD_MINIMUM_SYMBOLS, length).commit();
            updatePolicies();
        }

        void setPasswordMinimumNonLetter(int length) {
            SharedPreferences prefs = getSamplePreferences(this);
            prefs.edit().putInt(PREF_PASSWORD_MINIMUM_NONLETTER, length).commit();
            updatePolicies();
        }

        void setPasswordHistoryLength(int length) {
            SharedPreferences prefs = getSamplePreferences(this);
            prefs.edit().putInt(PREF_PASSWORD_HISTORY_LENGTH, length).commit();
            updatePolicies();
        }

        void setPasswordExpiration(long expiration) {
            SharedPreferences prefs = getSamplePreferences(this);
            long exp = expiration * MS_PER_MINUTE; // convert from UI units to ms
            prefs.edit().putLong(PREF_PASSWORD_EXPIRATION_TIMEOUT, exp).commit();
            updatePolicies();
            // Show confirmation dialog
            long confirm = mDPM.getPasswordExpiration(mDeviceAdminSample);
            String date = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT)
                    .format(new Date(confirm));
            new AlertDialog.Builder(this)
                    .setMessage("Password will expire on " + date)
                    .setPositiveButton("OK", null)
                    .create()
                    .show();
        }

        void setMaxFailedPw(int length) {
            SharedPreferences prefs = getSamplePreferences(this);
            prefs.edit().putInt(PREF_MAX_FAILED_PW, length).commit();
            updatePolicies();
        }

        @Override
        protected void onResume() {
            super.onResume();
            updateButtonStates();
            updatePasswordExpirationStatus();
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case REQUEST_CODE_ENABLE_ADMIN:
                    if (resultCode == Activity.RESULT_OK) {
                        Log.i(TAG, "Admin enabled!");
                    } else {
                        Log.i(TAG, "Admin enable FAILED!");
                    }
                    return;
                case REQUEST_CODE_START_ENCRYPTION:
                    updateEncryptionStatus();
                    return;
            }

            super.onActivityResult(requestCode, resultCode, data);
        }

        private OnClickListener mEnableListener = new OnClickListener() {
            public void onClick(View v) {
                // Launch the activity to have the user enable our admin.
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                        mDeviceAdminSample);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Additional text explaining why this needs to be added.");
                startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
            }
        };

        private OnClickListener mDisableListener = new OnClickListener() {
            public void onClick(View v) {
                mDPM.removeActiveAdmin(mDeviceAdminSample);
                updateButtonStates();
            }
        };

        private OnClickListener mSetPasswordListener = new OnClickListener() {
            public void onClick(View v) {
                // Launch the activity to have the user set a new password.
                Intent intent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
                startActivity(intent);
            }
        };

        private OnClickListener mResetPasswordListener = new OnClickListener() {
            public void onClick(View v) {
                if (ActivityManager.isUserAMonkey()) {
                    // Don't trust monkeys to do the right thing!
                    AlertDialog.Builder builder = new AlertDialog.Builder(Controller.this);
                    builder.setMessage("You can't reset my password because you are a monkey!");
                    builder.setPositiveButton("I admit defeat", null);
                    builder.show();
                    return;
                }
                boolean active = mDPM.isAdminActive(mDeviceAdminSample);
                if (active) {
                    mDPM.resetPassword(mPassword.getText().toString(),
                            DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
                }
            }
        };

        private OnClickListener mForceLockListener = new OnClickListener() {
            public void onClick(View v) {
                if (ActivityManager.isUserAMonkey()) {
                    // Don't trust monkeys to do the right thing!
                    AlertDialog.Builder builder = new AlertDialog.Builder(Controller.this);
                    builder.setMessage("You can't lock my screen because you are a monkey!");
                    builder.setPositiveButton("I admit defeat", null);
                    builder.show();
                    return;
                }
                boolean active = mDPM.isAdminActive(mDeviceAdminSample);
                if (active) {
                    mDPM.lockNow();
                }
            }
        };

        private OnClickListener mWipeDataListener = new OnClickListener() {
            public void onClick(final View v) {
                if (ActivityManager.isUserAMonkey()) {
                    // Don't trust monkeys to do the right thing!
                    AlertDialog.Builder builder = new AlertDialog.Builder(Controller.this);
                    builder.setMessage("You can't wipe my data because you are a monkey!");
                    builder.setPositiveButton("I admit defeat", null);
                    builder.show();
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(Controller.this);
                builder.setMessage("This will erase all of your data.  Are you sure?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(Controller.this);
                        if (v == mWipeAllDataButton) {
                            builder.setMessage("This is not a test.  "
                                    + "This WILL erase all of your data, "
                                    + "including external storage!  "
                                    + "Are you really absolutely sure?");
                        } else {
                            builder.setMessage("This is not a test.  "
                                    + "This WILL erase all of your data!  "
                                    + "Are you really absolutely sure?");
                        }
                        builder.setPositiveButton("BOOM!", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                boolean active = mDPM.isAdminActive(mDeviceAdminSample);
                                if (active) {
                                    mDPM.wipeData(v == mWipeAllDataButton
                                            ? DevicePolicyManager.WIPE_EXTERNAL_STORAGE : 0);
                                }
                            }
                        });
                        builder.setNegativeButton("Oops, run away!", null);
                        builder.show();
                    }
                });
                builder.setNegativeButton("No way!", null);
                builder.show();
            }
        };

        private OnClickListener mSetTimeoutListener = new OnClickListener() {

            public void onClick(View v) {
                if (ActivityManager.isUserAMonkey()) {
                    // Don't trust monkeys to do the right thing!
                    AlertDialog.Builder builder = new AlertDialog.Builder(Controller.this);
                    builder.setMessage("You can't lock my screen because you are a monkey!");
                    builder.setPositiveButton("I admit defeat", null);
                    builder.show();
                    return;
                }
                boolean active = mDPM.isAdminActive(mDeviceAdminSample);
                if (active) {
                    long timeMs = 1000L*Long.parseLong(mTimeout.getText().toString());
                    mDPM.setMaximumTimeToLock(mDeviceAdminSample, timeMs);
                }
            }
        };

        private OnClickListener mEncryptionButtonListener = new OnClickListener() {
            public void onClick(View v) {
                int buttonId = v.getId();
                if (buttonId == R.id.encryption_enable_button) {
                    mDPM.setStorageEncryption(mDeviceAdminSample, true);
                } else if (buttonId == R.id.encryption_disable_button) {
                    mDPM.setStorageEncryption(mDeviceAdminSample, false);
                } else if (buttonId == R.id.encryption_activate_button) {
                    if (ActivityManager.isUserAMonkey()) {
                        // Don't trust monkeys to do the right thing!
                        AlertDialog.Builder builder = new AlertDialog.Builder(Controller.this);
                        builder.setMessage("You can't activate encryption, you're a monkey!");
                        builder.setPositiveButton("I admit defeat", null);
                        builder.show();
                        return;
                    }
                    if (mDPM.getStorageEncryptionStatus() ==
                            DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(Controller.this);
                        builder.setMessage("Encryption is unsupported on this device.");
                        builder.setPositiveButton("OK", null);
                        builder.show();
                        return;
                    }
                    // Launch the activity to activate encryption.  May or may not return!
                    Intent intent = new Intent(DevicePolicyManager.ACTION_START_ENCRYPTION);
                    startActivityForResult(intent, REQUEST_CODE_START_ENCRYPTION);
                }

                // In all cases, fall through to update status
                updateEncryptionStatus();
            }
        };

        private void updateEncryptionStatus() {
            boolean sampleAdminStatusValue = mDPM.getStorageEncryption(mDeviceAdminSample);
            String sampleAdminStatus = Boolean.toString(sampleAdminStatusValue);
            boolean adminStatusValue = mDPM.getStorageEncryption(null);
            String adminStatus = Boolean.toString(adminStatusValue);
            int deviceStatusCode = mDPM.getStorageEncryptionStatus();
            String deviceStatus = statusCodeToString(deviceStatusCode);
            mEncryptionStatus.setText("sample:" + sampleAdminStatus + " admins:" + adminStatus
                    + " device:" + deviceStatus);
        }

        private String statusCodeToString(int newStatusCode) {
            String newStatus = "unknown";
            switch (newStatusCode) {
                case DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED:
                    newStatus = "unsupported";
                    break;
                case DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE:
                    newStatus = "inactive";
                    break;
                case DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING:
                    newStatus = "activating";
                    break;
                case DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE:
                    newStatus = "active";
                    break;
            }
            return newStatus;
        }
    }
}

