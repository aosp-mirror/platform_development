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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Provides UI for enabling the target app in this profile. The status of the app can be
 * uninstalled, hidden, or enabled depending on the situations. This fragment shows suitable
 * controls for each status.
 */
public class StatusFragment extends Fragment implements View.OnClickListener {

    private TextView mTextStatus;
    private Button mButtonUnhide;
    private StatusUpdatedListener mListener;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_status, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mTextStatus = (TextView) view.findViewById(R.id.status);
        mButtonUnhide = (Button) view.findViewById(R.id.unhide);
        mButtonUnhide.setOnClickListener(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mListener = (StatusUpdatedListener) activity;
    }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUi(getActivity());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.unhide: {
                unhideApp(getActivity());
                break;
            }
        }
    }

    private void updateUi(Activity activity) {
        PackageManager packageManager = activity.getPackageManager();
        try {
            ApplicationInfo info = packageManager.getApplicationInfo(
                    Constants.PACKAGE_NAME_APP_RESTRICTION_SCHEMA,
                    PackageManager.GET_UNINSTALLED_PACKAGES);
            DevicePolicyManager devicePolicyManager =
                    (DevicePolicyManager) activity.getSystemService(Activity.DEVICE_POLICY_SERVICE);
            if ((info.flags & ApplicationInfo.FLAG_INSTALLED) != 0) {
                if (!devicePolicyManager.isApplicationHidden(
                        EnforcerDeviceAdminReceiver.getComponentName(activity),
                        Constants.PACKAGE_NAME_APP_RESTRICTION_SCHEMA)) {
                    // The app is ready to enforce restrictions
                    // This is unlikely to happen in this sample as unhideApp() handles it.
                    mListener.onStatusUpdated();
                } else {
                    // The app is installed but hidden in this profile
                    mTextStatus.setText(R.string.status_not_activated);
                    mButtonUnhide.setVisibility(View.VISIBLE);
                }
            } else {
                // Need to reinstall the sample app
                mTextStatus.setText(R.string.status_need_reinstall);
                mButtonUnhide.setVisibility(View.GONE);
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Need to reinstall the sample app
            mTextStatus.setText(R.string.status_need_reinstall);
            mButtonUnhide.setVisibility(View.GONE);
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
                Constants.PACKAGE_NAME_APP_RESTRICTION_SCHEMA, false);
        Toast.makeText(activity, "Enabled the app", Toast.LENGTH_SHORT).show();
        mListener.onStatusUpdated();
    }

    public interface StatusUpdatedListener {
        public void onStatusUpdated();
    }

}
