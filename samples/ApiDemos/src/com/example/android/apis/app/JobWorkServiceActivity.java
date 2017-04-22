/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobWorkItem;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.view.View;
import android.widget.Button;

import com.example.android.apis.R;

/**
 * Example of interacting with {@link JobWorkService}.
 */
public class JobWorkServiceActivity extends Activity {
    JobScheduler mJobScheduler;
    JobInfo mJobInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mJobScheduler = (JobScheduler)getSystemService(JOB_SCHEDULER_SERVICE);
        mJobInfo = new JobInfo.Builder(R.string.job_service_created,
                new ComponentName(this, JobWorkService.class)).setOverrideDeadline(0).build();

        setContentView(R.layout.job_work_service_activity);

        // Watch for button clicks.
        Button button = findViewById(R.id.enqueue1);
        button.setOnClickListener(mEnqueue1Listener);
        button = findViewById(R.id.enqueue2);
        button.setOnClickListener(mEnqueue2Listener);
        button = findViewById(R.id.enqueue3);
        button.setOnClickListener(mEnqueue3Listener);
        button = findViewById(R.id.kill);
        button.setOnClickListener(mKillListener);
    }

    private View.OnClickListener mEnqueue1Listener = new View.OnClickListener() {
        public void onClick(View v) {
            mJobScheduler.enqueue(mJobInfo, new JobWorkItem(
                    new Intent("com.example.android.apis.ONE").putExtra("name", "One")));
        }
    };

    private View.OnClickListener mEnqueue2Listener = new View.OnClickListener() {
        public void onClick(View v) {
            mJobScheduler.enqueue(mJobInfo, new JobWorkItem(
                    new Intent("com.example.android.apis.TWO").putExtra("name", "Two")));
        }
    };

    private View.OnClickListener mEnqueue3Listener = new View.OnClickListener() {
        public void onClick(View v) {
            mJobScheduler.enqueue(mJobInfo, new JobWorkItem(
                    new Intent("com.example.android.apis.THREE").putExtra("name", "Three")));
        }
    };

    private View.OnClickListener mKillListener = new View.OnClickListener() {
        public void onClick(View v) {
            // This is to simulate the service being killed while it is
            // running in the background.
            Process.killProcess(Process.myPid());
        }
    };
}
