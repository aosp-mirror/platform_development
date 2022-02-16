/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.os.Bundle;
import android.widget.Button;

import com.example.android.apis.R;

public class PictureInPicture extends Activity {

    private Button mEnterPip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_in_picture);

        mEnterPip = (Button)findViewById(R.id.enter_pip);
        mEnterPip.setOnClickListener((v) -> enterPictureInPictureMode());
    }

    @Override
    protected void onUserLeaveHint() {
        enterPictureInPictureMode();
    }
}
