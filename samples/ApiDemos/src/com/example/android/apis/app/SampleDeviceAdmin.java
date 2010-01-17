/*
 * Copyright (C) 2007 The Android Open Source Project
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
import android.app.DeviceAdmin;
import android.app.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

/**
 * Example of a do-nothing admin class.  When enabled, it lets you control
 * some of its policy and reports when there is interesting activity.
 */
public class SampleDeviceAdmin extends DeviceAdmin {

    void showToast(Context context, CharSequence msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onEnabled(Context context, Intent intent) {
        showToast(context, "Sample Device Admin: enabled");
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        showToast(context, "Sample Device Admin: disabled");
    }

    @Override
    public void onPasswordChanged(Context context, Intent intent) {
        showToast(context, "Sample Device Admin: pw changed");
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        showToast(context, "Sample Device Admin: pw failed");
    }

    /**
     * <p>Example of explicitly starting and stopping the local service.
     * This demonstrates the implementation of a service that runs in the same
     * process as the rest of the application, which is explicitly started and stopped
     * as desired.</p>
     * 
     * <p>Note that this is implemented as an inner class only keep the sample
     * all together; typically this code would appear in some separate class.
     */
    public static class Controller extends Activity {
        static final int RESULT_ENABLE = 1;
        
        DevicePolicyManager mDPM;
        ComponentName mSampleDeviceAdmin;
        
        Button mEnableButton;
        Button mDisableButton;
        
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
            mSampleDeviceAdmin = new ComponentName(Controller.this, SampleDeviceAdmin.class);
            
            setContentView(R.layout.sample_device_admin);

            // Watch for button clicks.
            mEnableButton = (Button)findViewById(R.id.enable);
            mEnableButton.setOnClickListener(mEnableListener);
            mDisableButton = (Button)findViewById(R.id.disable);
            mDisableButton.setOnClickListener(mDisableListener);
        }

        void updateButtonStates() {
            boolean active = mDPM.isAdminActive(mSampleDeviceAdmin);
            if (active) {
                mEnableButton.setEnabled(false);
                mDisableButton.setEnabled(true);
            } else {
                mEnableButton.setEnabled(true);
                mDisableButton.setEnabled(false);
            }
        }
        
        @Override
        protected void onResume() {
            super.onResume();
            updateButtonStates();
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case RESULT_ENABLE:
                    if (resultCode == Activity.RESULT_OK) {
                        Log.i("SampleDeviceAdmin", "Admin enabled!");
                    } else {
                        Log.i("SampleDeviceAdmin", "Admin enable FAILED!");
                    }
                    return;
            }
            
            super.onActivityResult(requestCode, resultCode, data);
        }

        private OnClickListener mEnableListener = new OnClickListener() {
            public void onClick(View v) {
                // Launch the activity to have the user enable our admin.
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                        mSampleDeviceAdmin);
                startActivityForResult(intent, RESULT_ENABLE);
            }
        };

        private OnClickListener mDisableListener = new OnClickListener() {
            public void onClick(View v) {
                mDPM.removeActiveAdmin(mSampleDeviceAdmin);
                updateButtonStates();
            }
        };
    }

    
}
