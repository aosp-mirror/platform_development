/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.example.android.apis.animation;

import com.example.android.apis.R;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.ArrayMap;
import android.util.Pair;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import java.util.Random;

/**
 *
 */
public class ActivityTransition extends Activity {

    private static final String TAG = "ActivityTransition";

    private static final String KEY_ID = "ViewTransitionValues:id";

    private Random mRandom = new Random();

    private ImageView mHero;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setTriggerEarlyEnterTransition(true);
        getWindow().setBackgroundDrawable(new ColorDrawable(randomColor()));
        setContentView(R.layout.image_block);
        setupHero();
        TransitionManager transitionManager = getContentTransitionManager();
        TransitionSet transitions = new TransitionSet();
        Fall fall = new Fall();
        fall.setDuration(600);
        transitions.addTransition(fall);
        transitions.addTransition(new ScaleTransition());
        transitions.addTransition(new ChangeBounds());
        transitions.addTransition(new Up());
        transitionManager.setTransition(getContentScene(), transitions);
        transitionManager.setExitTransition(getContentScene(), transitions);
    }

    private void setupHero() {
        int id = getIntent().getIntExtra(KEY_ID, 0);
        mHero = (ImageView) findViewById(id);
        if (mHero != null) {
            ArrayMap<String, String> sharedElementsMap = new ArrayMap<String, String>();
            sharedElementsMap.put("hero", mHero.getSharedElementName());
            getWindow().mapTransitionTargets(sharedElementsMap);
        }
    }

    public void clicked(View v) {
        mHero = (ImageView) v;
        Intent intent = new Intent(this, ActivityTransitionDetails.class);
        intent.putExtra(KEY_ID, v.getId());
        ActivityOptions activityOptions
                = ActivityOptions.makeSceneTransitionAnimation(mHero, "hero");
        startActivity(intent, activityOptions.toBundle());
    }

    private int randomColor() {
        int red = mRandom.nextInt(128);
        int green = mRandom.nextInt(128);
        int blue = mRandom.nextInt(128);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    @Override
    public void onCaptureSharedElementStart(Transition transition) {
        int width = mHero.getWidth();
        int newTop = mHero.getBottom() - width;
        mHero.setTop(newTop);

        int imageWidth = mHero.getDrawable().getIntrinsicWidth();
        mHero.setPivotX(0);
        mHero.setPivotY(0);
        float scale = ((float)width)/imageWidth;
        mHero.setScaleX(scale);
        mHero.setScaleY(scale);
    }

    @Override
    public void onCaptureSharedElementEnd() {
        mHero.setPivotX(0);
        mHero.setPivotY(0);
        mHero.setScaleX(1);
        mHero.setScaleY(1);
    }
}
