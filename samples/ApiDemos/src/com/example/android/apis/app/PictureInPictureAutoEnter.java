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
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Rational;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.example.android.apis.R;

public class PictureInPictureAutoEnter extends Activity {

    private final View.OnLayoutChangeListener mOnLayoutChangeListener =
            (v, oldLeft, oldTop, oldRight, oldBottom, newLeft, newTop, newRight, newBottom) -> {
                updatePictureInPictureParams();
            };

    private final CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener =
            (v, isChecked) -> updatePictureInPictureParams();

    private View mImageView;
    private View mButtonView;
    private Switch mSwitchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().hide();
        setContentView(R.layout.picture_in_picture_auto_enter);

        mImageView = findViewById(R.id.image);
        mImageView.addOnLayoutChangeListener(mOnLayoutChangeListener);
        mButtonView = findViewById(R.id.change_orientation);
        mButtonView.setOnClickListener((v) -> {
            final int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        });
        mSwitchView = findViewById(R.id.source_rect_hint_toggle);
        mSwitchView.setOnCheckedChangeListener(mOnCheckedChangeListener);

        // there is a bug that setSourceRectHint(null) does not clear the source rect hint
        // once there is a non-null source rect hint ever been set. set this to false by default
        // therefore this demo activity can be used for testing autoEnterPip behavior without
        // source rect hint when launched for the first time.
        mSwitchView.setChecked(false);
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode,
            Configuration newConfig) {
        mButtonView.setVisibility(isInPictureInPictureMode ? View.GONE : View.VISIBLE);
        mSwitchView.setVisibility(isInPictureInPictureMode ? View.GONE: View.VISIBLE);
    }

    private void updatePictureInPictureParams() {
        final Rect imageViewRect = new Rect();
        mImageView.getGlobalVisibleRect(imageViewRect);
        // bail early if mImageView has not been measured yet
        if (imageViewRect.isEmpty()) return;
        final Rect sourceRectHint = mSwitchView.isChecked() ? new Rect(imageViewRect) : null;
        final Rational aspectRatio = new Rational(mImageView.getWidth(), mImageView.getHeight());
        final PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder()
                .setAutoEnterEnabled(true)
                .setAspectRatio(aspectRatio)
                .setSourceRectHint(sourceRectHint);
        setPictureInPictureParams(builder.build());
    }
}
