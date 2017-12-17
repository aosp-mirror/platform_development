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
package com.example.android.apis.view;

import com.example.android.apis.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.TextView;

/**
 * Demonstrates the display of overlay windows, i.e. windows that are drawn on top of other apps.
 */
public class OverlayWindowActivity extends Activity {

    private static int MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 1;
    private View mOverlayView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.overlay_window);

        Button button = (Button)findViewById(R.id.show_overlay);
        button.setOnClickListener(mShowOverlayListener);
        button = (Button)findViewById(R.id.hide_overlay);
        button.setOnClickListener(mHideOverlayListener);
    }

    private OnClickListener mShowOverlayListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (Settings.canDrawOverlays(OverlayWindowActivity.this)) {
                drawOverlay();
            } else {
                // Need to ask the user's permission first. We'll redirect them to Settings.
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
            }
        }
    };

    private OnClickListener mHideOverlayListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mOverlayView != null) {
                WindowManager wm = getWindowManager();
                wm.removeView(mOverlayView);
                mOverlayView = null;
            }
        }
    };

    /**
     * This is called after the user chooses whether they grant permission to the app to display
     * overlays or not.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            // Check if the user granted permission to draw overlays.
            if (Settings.canDrawOverlays(this)) {
                drawOverlay();
            }
        }
    }

    private void drawOverlay() {
        if (mOverlayView != null) {
            // Already shown.
            return;
        }

        TextView textView = new TextView(this);
        textView.setText("I'm an overlay");
        textView.setBackgroundColor(Color.WHITE);
        textView.setTextColor(Color.BLACK);
        textView.setPadding(10, 10, 10, 10);

        WindowManager wm = getWindowManager();
        LayoutParams params = new LayoutParams();

        params.type = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? LayoutParams.TYPE_APPLICATION_OVERLAY
                : LayoutParams.TYPE_PHONE;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        params.format = PixelFormat.TRANSPARENT;

        params.width = LayoutParams.WRAP_CONTENT;
        params.height = LayoutParams.WRAP_CONTENT;
        // Snap to the upper right corner of the screen.
        params.gravity = Gravity.TOP | Gravity.RIGHT;
        // Set position relative to upper right corner.
        params.x = 10;
        params.y = 10;

        wm.addView(textView, params);
        mOverlayView = textView;
    }
}
