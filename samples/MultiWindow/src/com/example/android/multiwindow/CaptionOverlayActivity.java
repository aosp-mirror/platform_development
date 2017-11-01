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

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;

/**
 * A minimal application that overlays caption on the content.
 */
public class CaptionOverlayActivity extends Activity
        implements Window.OnRestrictedCaptionAreaChangedListener {
    private static final String TAG = "CaptionOverlayActivity";

    /**
     * Called with the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Overlay the caption on the content.
        //setOverlayWithDecorCaptionEnabled(true);
        setContentView(R.layout.caption_overlay_layout);
        getWindow().setRestrictedCaptionAreaListener(this);
        getWindow().setResizingCaptionDrawable(new ColorDrawable(Color.BLACK));
        getWindow().setDecorCaptionShade(Window.DECOR_CAPTION_SHADE_AUTO);

        View decorView = getWindow().getDecorView();
        //Hide the status bar, because it likes to consume touch events.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        //Remember that you should never show the action bar if the
        // status bar is hidden, so hide that too if necessary.
        ActionBar actionBar = getActionBar();
        actionBar.hide();

    }

    @Override
    public void onRestrictedCaptionAreaChanged(Rect rect) {
        Log.d(TAG, "rect: " + rect);
    }
}

