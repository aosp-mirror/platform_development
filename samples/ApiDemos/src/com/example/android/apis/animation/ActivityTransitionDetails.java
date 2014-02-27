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
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.Pair;
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

    private int mId = R.id.ducky;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setTriggerEarlyEnterTransition(false);
        getWindow().setBackgroundDrawable(new ColorDrawable(randomColor()));
        setContentView(R.layout.image_details);
        ImageView titleImage = (ImageView) findViewById(R.id.titleImage);
        titleImage.setImageDrawable(getHeroDrawable());

        TransitionManager transitionManager = getContentTransitionManager();
        TransitionSet transitions = new TransitionSet();

        Fall fall = new Fall();
        fall.setDuration(600);
        transitions.addTransition(fall);
        transitions.addTransition(new Up());
        transitions.addTransition(new ChangeBounds());
        transitions.addTransition(new ScaleTransition());
        transitionManager.setTransition(getContentScene(), transitions);
        transitionManager.setExitTransition(getContentScene(), transitions);
    }

    @Override
    public void onCaptureSharedElementStart(Transition transition) {
        ImageView imageView = (ImageView) findViewById(R.id.titleImage);
        imageView.setScaleX(1);
        imageView.setScaleY(1);
        imageView.offsetTopAndBottom(-imageView.getTop());
    }

    @Override
    public void onCaptureSharedElementEnd() {
        setScale();
    }

    private void setScale() {
        ImageView imageView = (ImageView) findViewById(R.id.titleImage);
        Drawable drawable = imageView.getDrawable();
        float intrinsicWidth = drawable.getIntrinsicWidth();
        View sharedElementTarget = findViewById(R.id.shared_element);
        float scale = sharedElementTarget.getWidth()/intrinsicWidth;
        imageView.setPivotY(imageView.getHeight());
        imageView.setScaleX(scale);
        imageView.setScaleY(scale);
    }

    private Drawable getHeroDrawable() {
        int id = getIntent().getIntExtra(KEY_ID, mId);
        mId = id;

        int resourceId;
        switch (id) {
            case R.id.ducky:
                resourceId = R.drawable.ducky;
                break;
            case R.id.jellies:
                resourceId = R.drawable.jellies;
                break;
            case R.id.mug:
                resourceId = R.drawable.mug;
                break;
            case R.id.pencil:
                resourceId = R.drawable.pencil;
                break;
            case R.id.scissors:
                resourceId = R.drawable.scissors;
                break;
            case R.id.woot:
                resourceId = R.drawable.woot;
                break;
            case R.id.ball:
                resourceId = R.drawable.ball;
                break;
            case R.id.block:
                resourceId = R.drawable.block;
                break;
            default:
                resourceId = mImageResourceId;
                break;
        }
        mImageResourceId = resourceId;
        return getResources().getDrawable(resourceId);
    }

    public void clicked(View v) {
        Intent intent = new Intent(this, ActivityTransition.class);
        intent.putExtra(KEY_ID, mId);
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
