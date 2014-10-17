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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * This fragment provides UI and functionality to set restrictions on the AppRestrictionSchema
 * sample.
 */
public class AppRestrictionEnforcerFragment extends Fragment implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener {

    /**
     * Package name of the AppRestrictionSchema sample.
     */
    private static final String PACKAGE_NAME_APP_RESTRICTION_SCHEMA
            = "com.example.android.apprestrictionschema";

    /**
     * Key for {@link SharedPreferences}
     */
    private static final String PREFS_KEY = "AppRestrictionEnforcerFragment";

    /**
     * Key for the boolean restriction in AppRestrictionSchema.
     */
    private static final String RESTRICTION_KEY_SAY_HELLO = "can_say_hello";

    /**
     * Default boolean value for "can_say_hello" restriction. The actual value is loaded in
     * {@link #loadRestrictions(android.app.Activity)}.
     */
    private boolean mDefaultValueRestrictionSayHello;

    // UI Components
    private TextView mTextStatus;
    private Button mButtonUnhide;
    private Switch mSwitchSayHello;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_app_restriction_enforcer, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mTextStatus = (TextView) view.findViewById(R.id.status);
        mButtonUnhide = (Button) view.findViewById(R.id.unhide);
        mSwitchSayHello = (Switch) view.findViewById(R.id.say_hello);
        mButtonUnhide.setOnClickListener(this);
        mSwitchSayHello.setOnCheckedChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUi(getActivity());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.unhide: {
                unhideApp(getActivity());
                break;
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        switch (compoundButton.getId()) {
            case R.id.say_hello: {
                allowSayHello(getActivity(), checked);
                break;
            }
        }
    }

    /**
     * Updates the UI components according to the current status of AppRestrictionSchema and its
     * restriction.
     *
     * @param activity The activity
     */
    private void updateUi(Activity activity) {
        PackageManager packageManager = activity.getPackageManager();
        try {
            ApplicationInfo info = packageManager.getApplicationInfo(
                    PACKAGE_NAME_APP_RESTRICTION_SCHEMA, PackageManager.GET_UNINSTALLED_PACKAGES);
            DevicePolicyManager devicePolicyManager =
                    (DevicePolicyManager) activity.getSystemService(Activity.DEVICE_POLICY_SERVICE);
            if (0 < (info.flags & ApplicationInfo.FLAG_INSTALLED)) {
                if (!devicePolicyManager.isApplicationHidden(
                        EnforcerDeviceAdminReceiver.getComponentName(activity),
                        PACKAGE_NAME_APP_RESTRICTION_SCHEMA)) {
                    // The app is ready
                    loadRestrictions(activity);
                    mTextStatus.setVisibility(View.GONE);
                    mButtonUnhide.setVisibility(View.GONE);
                    mSwitchSayHello.setVisibility(View.VISIBLE);
                    mSwitchSayHello.setOnCheckedChangeListener(null);
                    mSwitchSayHello.setChecked(canSayHello(activity));
                    mSwitchSayHello.setOnCheckedChangeListener(this);
                } else {
                    // The app is installed but hidden in this profile
                    mTextStatus.setText(R.string.status_not_activated);
                    mTextStatus.setVisibility(View.VISIBLE);
                    mButtonUnhide.setVisibility(View.VISIBLE);
                    mSwitchSayHello.setVisibility(View.GONE);
                }
            } else {
                // Need to reinstall the sample app
                mTextStatus.setText(R.string.status_need_reinstall);
                mTextStatus.setVisibility(View.VISIBLE);
                mButtonUnhide.setVisibility(View.GONE);
                mSwitchSayHello.setVisibility(View.GONE);
            }
        } catch (PackageManager.NameNotFoundException e) {
            mTextStatus.setText(R.string.status_not_installed);
            mTextStatus.setVisibility(View.VISIBLE);
            mButtonUnhide.setVisibility(View.GONE);
            mSwitchSayHello.setVisibility(View.GONE);
        }
    }

    /**
     * Unhides the AppRestrictionSchema sample in case it is hidden in this profile.
     *
     * @param activity The activity
     */
    private void unhideApp(Activity activity) {
        DevicePolicyManager devicePolicyManager =
                (DevicePolicyManager) activity.getSystemService(Activity.DEVICE_POLICY_SERVICE);
        devicePolicyManager.setApplicationHidden(
                EnforcerDeviceAdminReceiver.getComponentName(activity),
                PACKAGE_NAME_APP_RESTRICTION_SCHEMA, false);
        Toast.makeText(activity, "Enabled the app", Toast.LENGTH_SHORT).show();
        updateUi(activity);
    }

    /**
     * Loads the restrictions for the AppRestrictionSchema sample. In this implementation, we just
     * read the default value for the "can_say_hello" restriction.
     *
     * @param activity The activity
     */
    private void loadRestrictions(Activity activity) {
        RestrictionsManager restrictionsManager =
                (RestrictionsManager) activity.getSystemService(Context.RESTRICTIONS_SERVICE);
        List<RestrictionEntry> restrictions =
                restrictionsManager.getManifestRestrictions(PACKAGE_NAME_APP_RESTRICTION_SCHEMA);
        for (RestrictionEntry restriction : restrictions) {
            if (RESTRICTION_KEY_SAY_HELLO.equals(restriction.getKey())) {
                mDefaultValueRestrictionSayHello = restriction.getSelectedState();
            }
        }
    }

    /**
     * Returns whether the AppRestrictionSchema is currently allowed to say hello to its user. Note
     * that a profile/device owner needs to remember each restriction value on its own.
     *
     * @param activity The activity
     * @return True if the AppRestrictionSchema is allowed to say hello
     */
    private boolean canSayHello(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        return prefs.getBoolean(RESTRICTION_KEY_SAY_HELLO, mDefaultValueRestrictionSayHello);
    }

    /**
     * Sets the value for the "cay_say_hello" restriction of AppRestrictionSchema.
     *
     * @param activity The activity
     * @param allow    The value to be set for the restriction.
     */
    private void allowSayHello(Activity activity, boolean allow) {
        DevicePolicyManager devicePolicyManager
                = (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        Bundle restrictions = new Bundle();
        restrictions.putBoolean(RESTRICTION_KEY_SAY_HELLO, allow);
        devicePolicyManager.setApplicationRestrictions(
                EnforcerDeviceAdminReceiver.getComponentName(activity),
                PACKAGE_NAME_APP_RESTRICTION_SCHEMA, restrictions);
        // The profile/device owner needs to remember the current state of restrictions on its own
        activity.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(RESTRICTION_KEY_SAY_HELLO, allow)
                .apply();
        Toast.makeText(activity, allow ? R.string.allowed : R.string.disallowed,
                Toast.LENGTH_SHORT).show();
    }

}
