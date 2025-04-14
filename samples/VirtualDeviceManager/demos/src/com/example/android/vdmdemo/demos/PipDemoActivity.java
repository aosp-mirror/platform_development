/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.app.PictureInPictureParams;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Activity that can enter picture in picture mode. */
public final class PipDemoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isInPictureInPictureMode()) {
            setContentView(R.layout.text_demo_activity);
            ((TextView) requireViewById(R.id.text)).setText(R.string.pip_demo);
        } else {
            setContentView(R.layout.pip_demo_activity);
        }
    }

    /** Handles PiP request. */
    public void onPipRequested(View view) {
        enterPictureInPictureMode(new PictureInPictureParams.Builder().build());
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        enterPictureInPictureMode(new PictureInPictureParams.Builder().build());
    }
}
