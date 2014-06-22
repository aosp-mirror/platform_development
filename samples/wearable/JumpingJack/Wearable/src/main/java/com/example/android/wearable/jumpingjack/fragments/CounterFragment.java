/*
 * Copyright (C) 2014 Google Inc. All Rights Reserved.
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

package com.example.android.wearable.jumpingjack.fragments;

import com.example.android.wearable.jumpingjack.R;
import com.example.android.wearable.jumpingjack.Utils;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * A simple fragment for showing the count
 */
public class CounterFragment extends Fragment {

    private static final long ANIMATION_INTERVAL_MS = 500; // in milliseconds
    private TextView mCounterText;
    private Timer mAnimationTimer;
    private Handler mHandler;
    private TimerTask mAnimationTask;
    private boolean up = false;
    private Drawable mDownDrawable;
    private Drawable mUpDrawable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.counter_layout, container, false);
        mDownDrawable = getResources().getDrawable(R.drawable.jump_down_50);
        mUpDrawable = getResources().getDrawable(R.drawable.jump_up_50);
        mCounterText = (TextView) view.findViewById(R.id.counter);
        mCounterText.setCompoundDrawablesWithIntrinsicBounds(mUpDrawable, null, null, null);
        setCounter(Utils.getCounterFromPreference(getActivity()));
        mHandler = new Handler();
        startAnimation();
        return view;
    }

    private void startAnimation() {
        mAnimationTask = new TimerTask() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCounterText.setCompoundDrawablesWithIntrinsicBounds(
                                up ? mUpDrawable : mDownDrawable, null, null, null);
                        up = !up;
                    }
                });
            }
        };
        mAnimationTimer = new Timer();
        mAnimationTimer.scheduleAtFixedRate(mAnimationTask, ANIMATION_INTERVAL_MS,
                ANIMATION_INTERVAL_MS);
    }

    public void setCounter(String text) {
        mCounterText.setText(text);
    }

    public void setCounter(int i) {
        setCounter(i < 0 ? "0" : String.valueOf(i));
    }

    @Override
    public void onDetach() {
        mAnimationTimer.cancel();
        super.onDetach();
    }
}
