/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.example.android.vdmdemo.demos;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Demo activity for testing latency in streaming with the VDM demo application. Increments a
 * counter every ~second.
 */
public final class LatencyDemoActivity extends AppCompatActivity {

    private static final int DELAY_MS = 1000;

    private final ScheduledExecutorService mExecutor = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> mScheduledFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.latency_demo_activity);
        View counter = requireViewById(R.id.counter);

        mScheduledFuture =
                mExecutor.scheduleAtFixedRate(
                        counter::invalidate, 0, DELAY_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onDestroy() {
        mScheduledFuture.cancel(true);
        mExecutor.shutdown();
        super.onDestroy();
    }
}
