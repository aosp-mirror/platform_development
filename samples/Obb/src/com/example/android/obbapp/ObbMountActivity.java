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

package com.example.android.obbapp;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.OnObbStateChangeListener;
import android.os.storage.StorageManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;

/**
 * This class provides a basic demonstration of how to manage an OBB file. It
 * provides two buttons: one to mount an OBB and another to unmount an OBB. The
 * main feature is that it implements an OnObbStateChangeListener which updates
 * some text fields with relevant information.
 */
public class ObbMountActivity extends Activity {
    private static final String TAG = "ObbMount";

    private static String mObbPath;

    private TextView mStatus;
    private TextView mPath;

    private StorageManager mSM;

    /** Called with the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate our UI from its XML layout description.
        setContentView(R.layout.obb_mount_activity);

        // Hook up button presses to the appropriate event handler.
        ((Button) findViewById(R.id.mount)).setOnClickListener(mMountListener);
        ((Button) findViewById(R.id.unmount)).setOnClickListener(mUnmountListener);

        // Text indications of current status
        mStatus = (TextView) findViewById(R.id.status);
        mPath = (TextView) findViewById(R.id.path);

        ObbState state = (ObbState) getLastNonConfigurationInstance();

        if (state != null) {
            mSM = state.storageManager;
            mStatus.setText(state.status);
            mPath.setText(state.path);
        } else {
            // Get an instance of the StorageManager
            mSM = (StorageManager) getApplicationContext().getSystemService(STORAGE_SERVICE);
        }

        mObbPath = new File(Environment.getExternalStorageDirectory(), "test1.obb").getPath();
    }

    OnObbStateChangeListener mEventListener = new OnObbStateChangeListener() {
        @Override
        public void onObbStateChange(String path, int state) {
            Log.d(TAG, "path=" + path + "; state=" + state);
            mStatus.setText(String.valueOf(state));
            if (state == OnObbStateChangeListener.MOUNTED) {
                mPath.setText(mSM.getMountedObbPath(mObbPath));
            } else {
                mPath.setText("");
            }
        }
    };

    /**
     * A call-back for when the user presses the back button.
     */
    OnClickListener mMountListener = new OnClickListener() {
        public void onClick(View v) {
            try {
                // We don't need to synchronize here to avoid clobbering the
                // content of mStatus because the callback comes to our main
                // looper.
                if (mSM.mountObb(mObbPath, null, mEventListener)) {
                    mStatus.setText(R.string.attempting_mount);
                } else {
                    mStatus.setText(R.string.failed_to_start_mount);
                }
            } catch (IllegalArgumentException e) {
                mStatus.setText(R.string.obb_already_mounted);
                Log.d(TAG, "OBB already mounted");
            }
        }
    };

    /**
     * A call-back for when the user presses the clear button.
     */
    OnClickListener mUnmountListener = new OnClickListener() {
        public void onClick(View v) {
            try {
                if (mSM.unmountObb(mObbPath, false, mEventListener)) {
                    mStatus.setText(R.string.attempting_unmount);
                } else {
                    mStatus.setText(R.string.failed_to_start_unmount);
                }
            } catch (IllegalArgumentException e) {
                mStatus.setText(R.string.obb_not_mounted);
                Log.d(TAG, "OBB not mounted");
            }
        }
    };

    @Override
    public Object onRetainNonConfigurationInstance() {
        // Since our OBB mount is tied to the StorageManager, retain it
        ObbState state = new ObbState(mSM, mStatus.getText(), mPath.getText());
        return state;
    }

    private static class ObbState {
        public StorageManager storageManager;
        public CharSequence status;
        public CharSequence path;

        ObbState(StorageManager storageManager, CharSequence status, CharSequence path) {
            this.storageManager = storageManager;
            this.status = status;
            this.path = path;
        }
    }
}
