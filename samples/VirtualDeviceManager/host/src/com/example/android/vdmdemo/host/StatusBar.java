/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.example.android.vdmdemo.host;

import static android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;

import android.content.Context;
import android.content.Intent;
import android.graphics.Insets;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Custom status bar shown on remote displays. */
public class StatusBar extends LinearLayout {

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mClockUpdater;

    public StatusBar(Context context) {
        super(context);
    }

    public StatusBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public StatusBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public StatusBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        TextView clock = requireViewById(R.id.clock);
        SimpleDateFormat df = new SimpleDateFormat("EEE, MMM d, HH:mm:ss", Locale.US);

        mClockUpdater = () -> {
            try {
                clock.post(() -> clock.setText(df.format(Calendar.getInstance().getTime())));
            } finally {
                mHandler.postDelayed(mClockUpdater, /* delayMillis= */1000);
            }
        };
        mClockUpdater.run();

        clock.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_CALENDAR);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
        });
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeCallbacks(mClockUpdater);
    }

    void destroy(Context displayContext) {
        WindowManager windowManager = displayContext.getSystemService(WindowManager.class);
        windowManager.removeViewImmediate(this);
    }

    static StatusBar create(Context displayContext) {
        final int statusBarHeight =
                displayContext.getResources().getDimensionPixelSize(R.dimen.status_bar_height);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                statusBarHeight,
                WindowManager.LayoutParams.TYPE_STATUS_BAR,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                        | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
                PixelFormat.TRANSLUCENT);
        lp.token = new Binder();
        lp.gravity = Gravity.TOP;
        lp.setFitInsetsTypes(0);
        lp.setTitle("StatusBar");
        lp.packageName = displayContext.getPackageName();
        lp.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        lp.setInsetsParams(List.of(new WindowManager.InsetsParams(WindowInsets.Type.statusBars())
                .setInsetsSize(Insets.of(0, statusBarHeight, 0, 0))));

        LayoutInflater inflater = LayoutInflater.from(displayContext);
        StatusBar statusBar = (StatusBar) inflater.inflate(R.layout.status_bar, null);

        WindowManager windowManager = displayContext.getSystemService(WindowManager.class);
        windowManager.addView(statusBar, lp);
        return statusBar;
    }
}
