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
import android.app.PictureInPictureParams;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;

import com.example.android.apis.R;

public class PictureInPictureSeamlessResize extends Activity {

    private Switch mSwitchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().hide();
        setContentView(R.layout.picture_in_picture_seamless_resize);

        mSwitchView = findViewById(R.id.seamless_resize_switch);
        mSwitchView.setOnCheckedChangeListener((v, isChecked) -> {
            onSeamlessResizeCheckedChanged(isChecked);
        });
        onSeamlessResizeCheckedChanged(mSwitchView.isChecked());
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode,
            Configuration newConfig) {
        mSwitchView.setVisibility(isInPictureInPictureMode ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onPictureInPictureRequested() {
        enterPictureInPictureMode(new PictureInPictureParams.Builder().build());
        return true;
    }

    private void onSeamlessResizeCheckedChanged(boolean checked) {
        final PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder()
                .setSeamlessResizeEnabled(checked);
        setPictureInPictureParams(builder.build());
    }
}
