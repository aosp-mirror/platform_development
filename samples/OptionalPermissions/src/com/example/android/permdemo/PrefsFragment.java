/*
** Copyright 2013, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package com.example.android.permdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

public class PrefsFragment extends PreferenceFragment {

    private static final int REQUEST_CODE_PROMPT_PERMISSIONS = 1;

    private static final String[] ALL_PERMISSIONS = {
            android.Manifest.permission.VIBRATE,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.READ_PHONE_STATE,
    };

    private Preference mVibrateStatusPreference;
    private Preference mInternetStatusPreference;
    private Preference mPhoneStateStatusPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        findPreference("vibrate_do").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                onVibrateActionClicked();
                return false;
            }
        });
        findPreference("vibrate_prompt").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                onPromptPermissionsClicked(android.Manifest.permission.VIBRATE);
                return false;
            }
        });

        findPreference("internet_do").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                onInternetActionClicked();
                return false;
            }
        });
        findPreference("internet_prompt").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                onPromptPermissionsClicked(android.Manifest.permission.INTERNET);
                return false;
            }
        });

        findPreference("phone_state_do").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                onReadPhoneStateActionClicked();
                return false;
            }
        });
        findPreference("phone_state_prompt").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                onPromptPermissionsClicked(android.Manifest.permission.READ_PHONE_STATE);
                return false;
            }
        });

        findPreference("all_prompt").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                onPromptPermissionsClicked(ALL_PERMISSIONS);
                return false;
            }
        });

        mVibrateStatusPreference = findPreference("vibrate_status");
        mInternetStatusPreference = findPreference("internet_status");
        mPhoneStateStatusPreference = findPreference("phone_state_status");
        refreshPermissionsStatus();
    }

    private void onVibrateActionClicked() {
        Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null) {
            Toast.makeText(
                    getActivity(),
                    R.string.vibrate_feature_not_available,
                    Toast.LENGTH_LONG).show();
            return;
        }
        try {
            vibrator.vibrate(300);
        } catch (Exception e) {
            displayException(e);
        }
    }

    private void onInternetActionClicked() {
        new AsyncTask<Void, Void, Object>() {
            private ProgressDialog mProgressDialog;

            @Override
            protected void onPreExecute() {
                mProgressDialog = ProgressDialog.show(
                        getActivity(),
                        null,
                        getString(R.string.get_time_progress_dialog_message),
                        true, // indeterminate progress
                        true, // cancelable
                        new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                // Cancel this AsyncTask
                                cancel(true);
                            }
                        });
                super.onPreExecute();
            }

            @Override
            protected Object doInBackground(Void... params) {
                HttpURLConnection connection = null;
                Date date;
                try {
                    connection =
                            (HttpURLConnection) new URL("https://www.google.com").openConnection();
                    connection.setDefaultUseCaches(false);
                    connection.setInstanceFollowRedirects(false);
                    connection.setRequestMethod("HEAD");

                    // Force the request to fail if there's no Internet connectivity
                    connection.getResponseCode();

                    long timeMillis = connection.getDate();
                    if (timeMillis == 0) {
                        throw new IOException("No time returned by the server");
                    }
                    date = new Date(timeMillis);
                } catch (Throwable t) {
                    t.printStackTrace();
                    return t;
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }

                return date;
            }

            @Override
            protected void onPostExecute(Object result) {
                if (getActivity().isFinishing()) {
                    // The Activity is already finishing/finished -- no need to display the date
                    return;
                }

                dismissProgressDialog();

                if (result instanceof Throwable) {
                    displayException((Throwable) result);
                    return;
                }

                Date date = (Date) result;
                String formattedDate = DateFormat.getDateFormat(getActivity()).format(date)
                        + " " + DateFormat.getTimeFormat(getActivity()).format(date);
                new AlertDialog.Builder(getActivity())
                        .setMessage(formattedDate)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }

            @Override
            protected void onCancelled() {
                dismissProgressDialog();
                super.onCancelled();
            }

            private void dismissProgressDialog() {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
            }

        }.execute((Void[]) null);
    }

    private void onReadPhoneStateActionClicked() {
        TelephonyManager telephonyManager =
                (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) {
            Toast.makeText(
                    getActivity(),
                    R.string.telephony_feature_not_available,
                    Toast.LENGTH_LONG).show();
            return;
        }

        String phoneNumber;
        try {
            phoneNumber = telephonyManager.getLine1Number();
        } catch (Exception e) {
            displayException(e);
            return;
        }

        new AlertDialog.Builder(getActivity())
                .setMessage("Phone number: " + phoneNumber)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void onPromptPermissionsClicked(String... permissions) {
        Intent i = getActivity().getPackageManager()
                .buildPermissionRequestIntent(permissions);
        startActivityForResult(i, REQUEST_CODE_PROMPT_PERMISSIONS);
    }

    private void onGrantPermissionsResult(boolean granted) {
        if (granted) {
            displayPermissionsGrantApprovedToast();
        } else {
            displayPermissionsGrantDeniedToast();
        }
        refreshPermissionsStatus();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_PROMPT_PERMISSIONS:
                onGrantPermissionsResult(resultCode == Activity.RESULT_OK);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void displayException(Throwable exception) {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.exception_dialog_title)
                .setMessage(exception.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void displayPermissionsGrantApprovedToast() {
        Toast.makeText(
                getActivity(),
                R.string.toast_permission_grant_approved,
                Toast.LENGTH_SHORT).show();
    }

    private void displayPermissionsGrantDeniedToast() {
        Toast.makeText(
                getActivity(),
                R.string.toast_permission_grant_denied,
                Toast.LENGTH_SHORT).show();
    }

    private void refreshPermissionsStatus() {
        mVibrateStatusPreference.setSummary(
                hasPermission(android.Manifest.permission.VIBRATE)
                ? R.string.permission_status_granted : R.string.permission_status_not_granted);
        mInternetStatusPreference.setSummary(
                hasPermission(android.Manifest.permission.INTERNET)
                ? R.string.permission_status_granted : R.string.permission_status_not_granted);
        mPhoneStateStatusPreference.setSummary(
                hasPermission(
                        android.Manifest.permission.READ_PHONE_STATE)
                ? R.string.permission_status_granted : R.string.permission_status_not_granted);
    }

    private boolean hasPermission(String permission) {
        long identity = Binder.clearCallingIdentity();
        try {
            return getActivity().getApplicationContext()
                .checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }
}
