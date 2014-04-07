/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.transition.MoveImage;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import java.util.Random;

/**
 *
 */
public class ActivityTransitionDetails extends Activity {

    private static final String TAG = "ActivityTransitionDetails";

    private static final String KEY_ID = "ViewTransitionValues:id";

    private Random mRandom = new Random();

    private int mImageResourceId = R.drawable.ducky;

    private String mName = "ducky";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setAllowOverlappingEnterTransition(false);
        getWindow().setAllowOverlappingExitTransition(true);
        getWindow().setBackgroundDrawable(new ColorDrawable(randomColor()));
        setContentView(R.layout.image_details);
        ImageView titleImage = (ImageView) findViewById(R.id.titleImage);
        titleImage.setImageDrawable(getHeroDrawable());

        TransitionManager transitionManager = getContentTransitionManager();
        TransitionSet transitions = new TransitionSet();

        Slide slide = new Slide();
        slide.setDuration(600);
        transitions.addTransition(slide);
        transitions.addTransition(new MoveImage());
        transitionManager.setTransition(getContentScene(), transitions);
        transitionManager.setExitTransition(getContentScene(), transitions);
    }

    private Drawable getHeroDrawable() {
        String name = getIntent().getStringExtra(KEY_ID);
        if (name != null) {
            mName = name;
            mImageResourceId = ActivityTransition.getDrawableIdForKey(name);
        }

        return getResources().getDrawable(mImageResourceId);
    }

    public void clicked(View v) {
        Intent intent = new Intent(this, ActivityTransition.class);
        intent.putExtra(KEY_ID, mName);
        ActivityOptions activityOptions = ActivityOptions.makeSceneTransitionAnimation(v, "hero");
        startActivity(intent, activityOptions.toBundle());
    }

    private int randomColor() {
        int red = mRandom.nextInt(128);
        int green = mRandom.nextInt(128);
        int blue = mRandom.nextInt(128);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

}
