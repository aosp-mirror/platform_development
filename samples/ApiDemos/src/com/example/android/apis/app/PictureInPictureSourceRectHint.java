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
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Rational;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.VideoView;

import com.example.android.apis.R;

public class PictureInPictureSourceRectHint extends Activity {

    private FrameLayout mFrameLayout;
    private VideoView mVideoView;
    private Button mEntryButton;
    private Button mExitButton;

    private int mPositionOnEntry;
    private int mPositionOnExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().hide();
        setContentView(R.layout.picture_in_picture_source_rect_hint);

        mFrameLayout = findViewById(R.id.frame_layout);
        mVideoView = findViewById(R.id.video_view);
        mEntryButton = findViewById(R.id.entry_source_btn);
        mExitButton = findViewById(R.id.exit_source_btn);

        initPlayer(Uri.parse("android.resource://" + getPackageName() +
                "/" + R.raw.videoviewdemo));


        setEntryState(Gravity.TOP);
        setExitState(Gravity.TOP);

        mEntryButton.setOnClickListener(v -> {
            // Toggle the position and update the views.
            setEntryState(mPositionOnEntry == Gravity.TOP ? Gravity.BOTTOM : Gravity.TOP);
        });
        mExitButton.setOnClickListener(v -> {
            // Toggle the position and update the views.
            setExitState(mPositionOnExit == Gravity.TOP ? Gravity.BOTTOM : Gravity.TOP);
        });

        mVideoView.addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                    if (left == oldLeft && right == oldRight && top == oldTop
                            && bottom == oldBottom) return;

                    setPictureInPictureParams(new PictureInPictureParams.Builder()
                            .setSourceRectHint(getVideoRectHint())
                            .build());
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideoView.stopPlayback();
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode,
            Configuration newConfig) {
        mEntryButton.setVisibility(isInPictureInPictureMode ? View.GONE : View.VISIBLE);
        mExitButton.setVisibility(isInPictureInPictureMode ? View.GONE : View.VISIBLE);

        final FrameLayout.LayoutParams params;
        if (isInPictureInPictureMode) {
            // In PIP mode the video should take up the full width and height.
            params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        } else {
            // Exiting PIP, return the video to its original size and place it to the preferred
            // gravity selected before entering PIP mode.
            params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            params.gravity = mPositionOnExit;
            // The "exit" gravity becomes the current "entry" gravity, update it and its views.
            setEntryState(mPositionOnExit);
        }
        mVideoView.setLayoutParams(params);
    }

    @Override
    public boolean onPictureInPictureRequested() {
        final Rect hint = getVideoRectHint();
        enterPictureInPictureMode(new PictureInPictureParams.Builder()
                .setSourceRectHint(hint)
                .setAspectRatio(new Rational(hint.width(), hint.height()))
                .build());
        return true;
    }

    private void initPlayer(Uri uri) {
        mVideoView.setVideoURI(uri);
        mVideoView.requestFocus();
        mVideoView.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            mVideoView.start();
        });
    }

    private Rect getVideoRectHint() {
        final Rect hint = new Rect();
        mVideoView.getGlobalVisibleRect(hint);
        return hint;
    }

    private void setEntryState(int gravity) {
        mPositionOnEntry = gravity;
        mEntryButton.setText(getString(
                R.string.activity_picture_in_picture_source_rect_hint_current_position,
                getGravityName(mPositionOnEntry)));
        final FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) mVideoView.getLayoutParams();
        p.gravity = mPositionOnEntry;
        mVideoView.setLayoutParams(p);
    }

    private void setExitState(int gravity) {
        mPositionOnExit = gravity;
        mExitButton.setText(getString(
                R.string.activity_picture_in_picture_source_rect_hint_position_on_exit,
                getGravityName(mPositionOnExit)));
    }

    private String getGravityName(int gravity) {
        return gravity == Gravity.TOP ? "TOP" : "BOTTOM";
    }
}
