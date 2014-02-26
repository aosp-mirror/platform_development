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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.RectEvaluator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.view.Window;
import android.widget.ImageView;

import java.util.Random;

/**
 *
 */
public class ActivityTransition extends Activity {
    private static final String TAG = "ActivityTransition";

    private static final String KEY_LEFT_ON_SCREEN = "ViewTransitionValues:left:";
    private static final String KEY_TOP_ON_SCREEN = "ViewTransitionValues:top:";
    private static final String KEY_WIDTH = "ViewTransitionValues:width:";
    private static final String KEY_HEIGHT = "ViewTransitionValues:height:";
    private static final String KEY_ID = "ViewTransitionValues:id";

    private Random mRandom = new Random();
    private boolean mComeBack;
    private Fall mFall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        setEarlyBackgroundTransition(false);
        getWindow().setBackgroundDrawable(new ColorDrawable(randomColor()));
        setContentView(R.layout.image_block);
        View hero = getHero();
        if (hero != null) {
            hero.setSharedElementName("hero");
        }
        TransitionManager transitionManager = getContentTransitionManager();
        TransitionSet transitions = new TransitionSet();
        Fall fall = new Fall();
        fall.setDuration(600);
        fall.setStartDelay(600);
        fall.setHero(hero);
        transitions.addTransition(fall);
        transitions.addTransition(new Up());
        transitionManager.setTransition("null", getContentScene(), transitions);

        transitions = new TransitionSet();
        mFall = new Fall();
        mFall.setDuration(600);
        transitions.addTransition(mFall);
        transitions.addTransition(new Up());
        transitionManager.setTransition(getContentScene(), "null", transitions);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mComeBack) {
            mComeBack = false;
            setContentView(R.layout.image_block);
        }
    }

    private View getHero() {
        Bundle transitionArgs = getTransitionArgs();
        if (transitionArgs == null) {
            return null;
        }
        int id = transitionArgs.getInt(KEY_ID);
        return findViewById(id);
    }

    private static Property<Drawable, Rect> DRAWABLE_BOUNDS
            = new Property<Drawable, Rect>(Rect.class, "bounds") {
        @Override
        public Rect get(Drawable object) {
            return null;
        }

        @Override
        public void set(Drawable object, Rect value) {
            object.setBounds(value);
            object.invalidateSelf();
        }
    };

    @Override
    public void startSharedElementTransition(Bundle transitionArgs) {
        final ImageView hero = (ImageView)getHero();
        hero.setSharedElementName(null);
        hero.setVisibility(View.INVISIBLE);

        int[] loc = new int[2];
        hero.getLocationOnScreen(loc);
        int endScreenLeft = loc[0];
        int endScreenTop = loc[1];
        int originalWidth = hero.getWidth();
        int originalHeight = hero.getHeight();

        hero.setVisibility(View.INVISIBLE);
        ViewGroup sceneRoot = getContentScene().getSceneRoot();
        sceneRoot.getLocationOnScreen(loc);
        int overlayLeft = loc[0];
        int overlayTop = loc[1];
        final ViewGroupOverlay overlay = sceneRoot.getOverlay();

        int endX = endScreenLeft - overlayLeft;
        int endY = endScreenTop - overlayTop;

        int startX = transitionArgs.getInt(KEY_LEFT_ON_SCREEN) - overlayLeft;
        int startY = transitionArgs.getInt(KEY_TOP_ON_SCREEN) - overlayTop;
        int startWidth = transitionArgs.getInt(KEY_WIDTH);
        int startHeight = transitionArgs.getInt(KEY_HEIGHT);

        int endHeight = originalWidth * startHeight / startWidth;
        final Drawable image = hero.getDrawable();
        Rect startBounds = new Rect(startX, startY, startX + startWidth, startY + startHeight);
        endY += originalHeight - endHeight;
        Rect endBounds = new Rect(endX, endY, endX + originalWidth, endY + endHeight);
        ObjectAnimator boundsAnimator = ObjectAnimator.ofObject(image, DRAWABLE_BOUNDS,
                new RectEvaluator(new Rect()), startBounds, endBounds);
        hero.setImageDrawable(null);
        image.setBounds(startBounds);
        overlay.add(image);

        boundsAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                overlay.remove(image);
                hero.setImageDrawable(image);
                hero.setVisibility(View.VISIBLE);
            }
        });
        boundsAnimator.start();
    }

    public void clicked(View v) {
        v.setSharedElementName("hero");
        mFall.setHero(v);
        Intent intent = new Intent(this, ActivityTransitionDetails.class);
        Bundle args = getHeroInfo(v);
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(args);
        startActivity(intent, options.toBundle());
        //v.setTranslationZ(300);
        mComeBack = true;
    }

    private int randomColor() {
        int red = mRandom.nextInt(128);
        int green = mRandom.nextInt(128);
        int blue = mRandom.nextInt(128);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    static Bundle getHeroInfo(View view) {
        int[] loc = new int[2];
        view.getLocationOnScreen(loc);
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_LEFT_ON_SCREEN, loc[0]);
        bundle.putInt(KEY_TOP_ON_SCREEN, loc[1]);
        bundle.putInt(KEY_WIDTH, view.getWidth());
        bundle.putInt(KEY_HEIGHT, view.getHeight());
        bundle.putInt(KEY_ID, view.getId());
        return bundle;
    }
}
