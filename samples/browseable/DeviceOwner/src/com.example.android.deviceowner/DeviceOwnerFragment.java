/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.deviceowner;

import android.app.Activity;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Demonstrates the usage of the most common device management APIs for the device owner case.
 * In addition to various features available for profile owners, device owners can perform extra
 * actions, such as configuring global settings and enforcing a preferred Activity for a specific
 * IntentFilter.
 */
public class DeviceOwnerFragment extends Fragment {

    // Keys for SharedPreferences
    private static final String PREFS_DEVICE_OWNER = "DeviceOwnerFragment";
    private static final String PREF_LAUNCHER = "launcher";

    private DevicePolicyManager mDevicePolicyManager;

    // View references
    private Switch mSwitchAutoTime;
    private Switch mSwitchAutoTimeZone;
    private Spinner mAvailableLaunchers;
    private Button mButtonLauncher;

    // Adapter for the spinner to show list of available launchers
    private LauncherAdapter mAdapter;

    /**
     * Handles events on the Switches.
     */
    private Switch.OnCheckedChangeListener mOnCheckedChangeListener
            = new Switch.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            switch (buttonView.getId()) {
                case R.id.switch_auto_time:
                    setBooleanGlobalSetting(Settings.Global.AUTO_TIME, isChecked);
                    retrieveCurrentSettings(getActivity());
                    break;
                case R.id.switch_auto_time_zone:
                    setBooleanGlobalSetting(Settings.Global.AUTO_TIME_ZONE, isChecked);
                    retrieveCurrentSettings(getActivity());
                    break;
            }
        }

    };

    /**
     * Handles click events on the Button.
     */
    private View.OnClickListener mOnClickListener
            = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.set_preferred_launcher:
                    if (loadPersistentPreferredLauncher(getActivity()) == null) {
                        setPreferredLauncher();
                    } else {
                        clearPreferredLauncher();
                    }
                    retrieveCurrentSettings(getActivity());
                    break;
            }
        }

    };

    /**
     * @return A newly instantiated {@link DeviceOwnerFragment}.
     */
    public static DeviceOwnerFragment newInstance() {
        return new DeviceOwnerFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_owner, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Retain references
        mSwitchAutoTime = (Switch) view.findViewById(R.id.switch_auto_time);
        mSwitchAutoTimeZone = (Switch) view.findViewById(R.id.switch_auto_time_zone);
        mAvailableLaunchers = (Spinner) view.findViewById(R.id.available_launchers);
        mButtonLauncher = (Button) view.findViewById(R.id.set_preferred_launcher);
        // Bind event handlers
        mSwitchAutoTime.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mSwitchAutoTimeZone.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mButtonLauncher.setOnClickListener(mOnClickListener);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mDevicePolicyManager =
                (DevicePolicyManager) activity.getSystemService(Activity.DEVICE_POLICY_SERVICE);
    }

    @Override
    public void onDetach() {
        mDevicePolicyManager = null;
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity activity = getActivity();
        if (activity != null) {
            retrieveCurrentSettings(activity);
        }
    }

    /**
     * Retrieves the current global settings and changes the UI accordingly.
     *
     * @param activity The activity
     */
    private void retrieveCurrentSettings(Activity activity) {
        // Global settings
        setCheckedSafely(mSwitchAutoTime,
                getBooleanGlobalSetting(activity.getContentResolver(), Settings.Global.AUTO_TIME));
        setCheckedSafely(mSwitchAutoTimeZone,
                getBooleanGlobalSetting(activity.getContentResolver(),
                        Settings.Global.AUTO_TIME_ZONE));

        // Launcher
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> list = activity.getPackageManager()
                .queryIntentActivities(intent, /* default flags */ 0);
        mAdapter = new LauncherAdapter(activity, list);
        mAvailableLaunchers.setAdapter(mAdapter);
        String packageName = loadPersistentPreferredLauncher(activity);
        if (packageName == null) { // No preferred launcher is set
            mAvailableLaunchers.setEnabled(true);
            mButtonLauncher.setText(R.string.set_as_preferred);
        } else {
            int position = -1;
            for (int i = 0; i < list.size(); ++i) {
                if (list.get(i).activityInfo.packageName.equals(packageName)) {
                    position = i;
                    break;
                }
            }
            if (position != -1) {
                mAvailableLaunchers.setSelection(position);
                mAvailableLaunchers.setEnabled(false);
                mButtonLauncher.setText(R.string.clear_preferred);
            }
        }
    }

    /**
     * Retrieves the current boolean value of the specified global setting.
     *
     * @param resolver The ContentResolver
     * @param setting  The setting to be retrieved
     * @return The current boolean value
     */
    private static boolean getBooleanGlobalSetting(ContentResolver resolver, String setting) {
        return 0 != Settings.Global.getInt(resolver, setting, 0);
    }

    /**
     * Sets the boolean value of the specified global setting.
     *
     * @param setting The setting to be set
     * @param value   The value to be set
     */
    private void setBooleanGlobalSetting(String setting, boolean value) {
        mDevicePolicyManager.setGlobalSetting(
                // The ComponentName of the device owner
                DeviceOwnerReceiver.getComponentName(getActivity()),
                // The settings to be set
                setting,
                // The value we write here is a string representation for SQLite
                value ? "1" : "0");
    }

    /**
     * A utility method to set the checked state of the button without invoking its listener.
     *
     * @param button  The button
     * @param checked The value to be set
     */
    private void setCheckedSafely(CompoundButton button, boolean checked) {
        button.setOnCheckedChangeListener(null);
        button.setChecked(checked);
        button.setOnCheckedChangeListener(mOnCheckedChangeListener);
    }

    /**
     * Loads the package name from SharedPreferences.
     *
     * @param activity The activity
     * @return The package name of the launcher currently set as preferred, or null if there is no
     * preferred launcher.
     */
    private static String loadPersistentPreferredLauncher(Activity activity) {
        return activity.getSharedPreferences(PREFS_DEVICE_OWNER, Context.MODE_PRIVATE)
                .getString(PREF_LAUNCHER, null);
    }

    /**
     * Saves the package name into SharedPreferences.
     *
     * @param activity    The activity
     * @param packageName The package name to be saved. Pass null to remove the preferred launcher.
     */
    private static void savePersistentPreferredLauncher(Activity activity, String packageName) {
        SharedPreferences.Editor editor = activity.getSharedPreferences(PREFS_DEVICE_OWNER,
                Context.MODE_PRIVATE).edit();
        if (packageName == null) {
            editor.remove(PREF_LAUNCHER);
        } else {
            editor.putString(PREF_LAUNCHER, packageName);
        }
        editor.apply();
    }

    /**
     * Sets the selected launcher as preferred.
     */
    private void setPreferredLauncher() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_HOME);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        ComponentName componentName = mAdapter.getComponentName(
                mAvailableLaunchers.getSelectedItemPosition());
        mDevicePolicyManager.addPersistentPreferredActivity(
                DeviceOwnerReceiver.getComponentName(activity), filter, componentName);
        savePersistentPreferredLauncher(activity, componentName.getPackageName());
    }

    /**
     * Clears the launcher currently set as preferred.
     */
    private void clearPreferredLauncher() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        mDevicePolicyManager.clearPackagePersistentPreferredActivities(
                DeviceOwnerReceiver.getComponentName(activity),
                loadPersistentPreferredLauncher(activity));
        savePersistentPreferredLauncher(activity, null);
    }

    /**
     * Shows list of {@link ResolveInfo} in a {@link Spinner}.
     */
    private static class LauncherAdapter extends SimpleAdapter {

        private static final String KEY_PACKAGE_NAME = "package_name";
        private static final String KEY_ACTIVITY_NAME = "activity_name";

        public LauncherAdapter(Context context, List<ResolveInfo> list) {
            super(context, createData(list), android.R.layout.simple_list_item_1,
                    new String[]{KEY_PACKAGE_NAME},
                    new int[]{android.R.id.text1});
        }

        private static List<HashMap<String, String>> createData(List<ResolveInfo> list) {
            List<HashMap<String, String>> data = new ArrayList<>();
            for (ResolveInfo info : list) {
                HashMap<String, String> map = new HashMap<>();
                map.put(KEY_PACKAGE_NAME, info.activityInfo.packageName);
                map.put(KEY_ACTIVITY_NAME, info.activityInfo.name);
                data.add(map);
            }
            return data;
        }

        public ComponentName getComponentName(int position) {
            @SuppressWarnings("unchecked")
            HashMap<String, String> map = (HashMap<String, String>) getItem(position);
            return new ComponentName(map.get(KEY_PACKAGE_NAME), map.get(KEY_ACTIVITY_NAME));
        }

    }

}
