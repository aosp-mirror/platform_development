/*
 * Copyright (C) 2021 The Android Open Source Project
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
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.ResultReceiver;

import com.example.android.apis.R;

public class ContentPictureInPicture extends Activity {
    private ResultReceiver mOnStopReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_in_picture_content);
        mOnStopReceiver = getIntent().getParcelableExtra(PictureInPicture.KEY_ON_STOP_RECEIVER);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mOnStopReceiver != null) {
            mOnStopReceiver.send(0 /* resultCode */, Bundle.EMPTY);
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode,
            Configuration newConfig) {
        if (!isInPictureInPictureMode) {
            finish();
        }
    }
}
