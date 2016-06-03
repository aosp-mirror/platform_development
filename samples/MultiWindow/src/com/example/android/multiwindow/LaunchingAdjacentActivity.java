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

package com.example.android.multiwindow;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class LaunchingAdjacentActivity extends Activity implements View.OnClickListener {

    private static final String INSTANCE_NUMBER_KEY = "instance_number";

    private static int mInstanceCount;
    private int mInstanceNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launching_adjacent_layout);
        findViewById(R.id.launch_settings_adjacent).setOnClickListener(this);
        findViewById(R.id.launch_new_task_single).setOnClickListener(this);
        findViewById(R.id.launch_new_task_multiple).setOnClickListener(this);
        findViewById(R.id.launch_new_task_adjacent).setOnClickListener(this);
        if (savedInstanceState != null) {
            mInstanceNumber = savedInstanceState.getInt(INSTANCE_NUMBER_KEY);
        } else {
            mInstanceNumber = mInstanceCount++;
        }
        ((TextView) findViewById(R.id.instance_number))
                .setText(getString(R.string.instance_number) + mInstanceNumber);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.launch_settings_adjacent: {
                Intent intent = new Intent("android.settings.SETTINGS");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
                startActivity(intent);
            }
            break;
            case R.id.launch_new_task_single: {
                Intent intent = newAdjacentActivityIntent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            break;
            case R.id.launch_new_task_multiple: {
                Intent intent = newAdjacentActivityIntent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                startActivity(intent);
            }
            break;
            case R.id.launch_new_task_adjacent: {
                Intent intent = newAdjacentActivityIntent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                        | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
                startActivity(intent);
            }
            break;
        }
    }

    private Intent newAdjacentActivityIntent() {
        Intent intent = new Intent(this, LaunchingAdjacentActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        return intent;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(INSTANCE_NUMBER_KEY, mInstanceNumber);
        super.onSaveInstanceState(savedInstanceState);
    }
}
