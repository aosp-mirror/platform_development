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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.RectEvaluator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.Log;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ImageView;

import java.util.Random;

/**
 *
 */
public class ActivityTransitionDetails extends Activity {
    private static final String TAG = "ActivityTransitionDetails";

    private static final String KEY_LEFT_ON_SCREEN = "ViewTransitionValues:left:";
    private static final String KEY_TOP_ON_SCREEN = "ViewTransitionValues:top:";
    private static final String KEY_WIDTH = "ViewTransitionValues:width:";
    private static final String KEY_HEIGHT = "ViewTransitionValues:height:";
    private static final String KEY_ID = "ViewTransitionValues:id";

    private Random mRandom = new Random();
    private boolean mComeBack;
    private int mImageResourceId = R.drawable.ducky;
    private int mId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        setEarlyBackgroundTransition(false);
        getWindow().setBackgroundDrawable(new ColorDrawable(randomColor()));
        setContentView(R.layout.image_details);
        setImageMatrix();
        ImageView hero = (ImageView)findViewById(R.id.titleImage);
        hero.setImageDrawable(getHeroDrawable());
        //hero.setTranslationZ(300);
        TransitionManager transitionManager = getContentTransitionManager();
        TransitionSet transitions = new TransitionSet();
        Fall fall = new Fall();
        fall.setDuration(600);
        fall.setStartDelay(600);
        transitions.addTransition(fall);
        transitions.addTransition(new Up());
        transitionManager.setTransition("null", getContentScene(), transitions);

        transitions = new TransitionSet();
        fall = new Fall();
        fall.setDuration(600);
        transitions.addTransition(fall);
        transitions.addTransition(new Up());
        transitionManager.setTransition(getContentScene(), "null", transitions);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mComeBack) {
            mComeBack = false;
            setContentView(R.layout.image_details);
            ImageView hero = (ImageView)findViewById(R.id.titleImage);
            hero.setImageDrawable(getHeroDrawable());
            setImageMatrix();
        }
    }

    private Drawable getHeroDrawable() {
        Bundle args = getTransitionArgs();
        int id = args == null ? 0 : args.getInt(KEY_ID);
        int resourceId;
        switch (id) {
            case R.id.ducky: resourceId = R.drawable.ducky; break;
            case R.id.jellies: resourceId = R.drawable.jellies; break;
            case R.id.mug: resourceId = R.drawable.mug; break;
            case R.id.pencil: resourceId = R.drawable.pencil; break;
            case R.id.scissors: resourceId = R.drawable.scissors; break;
            case R.id.woot: resourceId = R.drawable.woot; break;
            case R.id.ball: resourceId = R.drawable.ball; break;
            case R.id.block: resourceId = R.drawable.block; break;
            default:
                resourceId = mImageResourceId;
                break;
        }
        mImageResourceId = resourceId;
        return getResources().getDrawable(resourceId);
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

    private void setImageMatrix() {
        getWindow().getDecorView().getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                getWindow().getDecorView().getViewTreeObserver().removeOnPreDrawListener(this);
                // Use an image matrix such that it aligns with the bottom.
                final ImageView hero = (ImageView)findViewById(R.id.titleImage);
                hero.setScaleType(ImageView.ScaleType.MATRIX);
                Matrix matrix = hero.getImageMatrix();
                int height = hero.getHeight();
                int width = hero.getWidth();
                Drawable image = hero.getDrawable();
                int intrinsicHeight = image.getIntrinsicHeight();
                int intrinsicWidth = image.getIntrinsicWidth();
                int scaledHeight = intrinsicHeight * width / intrinsicWidth;
                matrix.postTranslate(0, (height - scaledHeight) / 2);
                hero.setImageMatrix(matrix);
                return true;
            }
        });
    }

    @Override
    public void startSharedElementTransition(Bundle transitionArgs) {
        mId = transitionArgs.getInt(KEY_ID);
        final ImageView hero = (ImageView)findViewById(R.id.titleImage);
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
        Intent intent = new Intent(this, ActivityTransition.class);
        Bundle args = getHeroInfo((ImageView)v);
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(args);
        startActivity(intent, options.toBundle());
        mComeBack = true;
    }

    private int randomColor() {
        int red = mRandom.nextInt(128);
        int green = mRandom.nextInt(128);
        int blue = mRandom.nextInt(128);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private Bundle getHeroInfo(ImageView view) {
        int[] loc = new int[2];
        view.getLocationOnScreen(loc);
        Bundle bundle = new Bundle();
        Drawable image = view.getDrawable();
        int intrinsicWidth = image.getIntrinsicWidth();
        int intrinsicHeight = image.getIntrinsicHeight();
        int width = view.getWidth();
        int height = intrinsicHeight * width / intrinsicWidth;
        int top = loc[1] + view.getHeight() - height;
        bundle.putInt(KEY_LEFT_ON_SCREEN, loc[0]);
        bundle.putInt(KEY_TOP_ON_SCREEN, top);
        bundle.putInt(KEY_WIDTH, width);
        bundle.putInt(KEY_HEIGHT, height);
        bundle.putInt(KEY_ID, mId);
        return bundle;
    }

}
