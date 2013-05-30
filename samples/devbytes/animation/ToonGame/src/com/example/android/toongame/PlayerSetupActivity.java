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

package com.example.android.toongame;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

/**
 * This activity, launched from the ToonGame activity, takes the user between three
 * different setup screens where they choose a name, choose a difficulty rating, and
 * enter important financial information. All of the screens are meant to be
 * simple, engaging, and fun.
 */
public class PlayerSetupActivity extends Activity {

    private static final AccelerateInterpolator sAccelerator = new AccelerateInterpolator();
    private static final LinearInterpolator sLinearInterpolator = new LinearInterpolator();
    ViewGroup mContainer;
    EditText mEditText;
    
    private static final int NAME_STATE = 0;
    private static final int DIFFICULTY_STATE = 1;
    private static final int CREDIT_STATE = 2;
    private int mEntryState = NAME_STATE;

    SkewableTextView mNameTV, mDifficultyTV, mCreditTV;
    
    ViewGroup mNameButtons, mDifficultyButtons, mCreditButtons1, mCreditButtons2;
    
    Button mBobButton, mJaneButton, mPatButton;
    
    private static final TimeInterpolator sOvershooter = new OvershootInterpolator();
    private static final DecelerateInterpolator sDecelerator = new DecelerateInterpolator();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_setup_layout);
        overridePendingTransition(0, 0);
        
        mContainer = (ViewGroup) findViewById(R.id.container);
        mContainer.getViewTreeObserver().addOnPreDrawListener(mPreDrawListener);
        
        mNameTV = (SkewableTextView) findViewById(R.id.nameTV);
        mDifficultyTV = (SkewableTextView) findViewById(R.id.ageTV);
        mCreditTV = (SkewableTextView) findViewById(R.id.creditTV);
        
        mBobButton = setupButton(R.id.bobButton);
        setupButton(R.id.janeButton);
        setupButton(R.id.patButton);
        setupButton(R.id.easyButton);
        setupButton(R.id.hardButton);
        setupButton(R.id.megaHardButton);
        
        mNameButtons = (ViewGroup) findViewById(R.id.nameButtons);
        mDifficultyButtons = (ViewGroup) findViewById(R.id.difficultyButtons);
        mCreditButtons1 = (ViewGroup) findViewById(R.id.creditButtons1);
        mCreditButtons2 = (ViewGroup) findViewById(R.id.creditButtons2);
    }
    
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    private Button setupButton(int resourceId) {
        Button button = (Button) findViewById(resourceId);
        button.setOnTouchListener(mButtonPressListener);
        return button;
    }
    
    private View.OnTouchListener mButtonPressListener =
            new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().setDuration(ToonGame.SHORT_DURATION).
                                scaleX(.8f).scaleY(.8f).setInterpolator(sDecelerator);
                        break;
                    case MotionEvent.ACTION_UP:
                        v.animate().setDuration(ToonGame.SHORT_DURATION).
                                scaleX(1).scaleY(1).setInterpolator(sAccelerator);
                        break;
                    default:
                        break;
                    }
                    return false;
                }
            };

    public void buttonClick(View clickedView, int alignmentRule) {
        ViewGroup parent = (ViewGroup) clickedView.getParent();
        for (int i = 0; i < parent.getChildCount(); ++i) {
            Button child = (Button) parent.getChildAt(i);
            if (child != clickedView) {
                child.animate().alpha(0);
            } else {
                final Button buttonCopy = new Button(this);
                child.setVisibility(View.INVISIBLE);
                buttonCopy.setBackground(child.getBackground());
                buttonCopy.setText(((Button) child).getText());
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                params.addRule(alignmentRule);
                params.setMargins(25, 50, 25, 50);
                buttonCopy.setLayoutParams(params);
                buttonCopy.setPadding(child.getPaddingLeft(), child.getPaddingTop(),
                        child.getPaddingRight(), child.getPaddingBottom());
                buttonCopy.setTextSize(TypedValue.COMPLEX_UNIT_PX, child.getTextSize());
                buttonCopy.setTypeface(child.getTypeface(), Typeface.BOLD);
                ColorStateList colors = child.getTextColors();
                buttonCopy.setTextColor(colors.getDefaultColor());
                final int[] oldLocationInWindow = new int[2];
                child.getLocationInWindow(oldLocationInWindow);
                mContainer.addView(buttonCopy);
                buttonCopy.getViewTreeObserver().addOnPreDrawListener(
                        new ViewTreeObserver.OnPreDrawListener() {
                    
                    @Override
                    public boolean onPreDraw() {
                        buttonCopy.getViewTreeObserver().removeOnPreDrawListener(this);
                        int[] locationInWindow = new int[2];
                        buttonCopy.getLocationInWindow(locationInWindow);
                        float deltaX = oldLocationInWindow[0] - locationInWindow[0];
                        float deltaY = oldLocationInWindow[1] - locationInWindow[1];
    
                        buttonCopy.setTranslationX(deltaX);
                        buttonCopy.setTranslationY(deltaY);
                        
                        PropertyValuesHolder pvhSX =
                                PropertyValuesHolder.ofFloat(View.SCALE_X, 3);
                        PropertyValuesHolder pvhSY =
                                PropertyValuesHolder.ofFloat(View.SCALE_Y, 3);
                        ObjectAnimator bounceAnim = ObjectAnimator.ofPropertyValuesHolder(
                                buttonCopy, pvhSX, pvhSY);
                        bounceAnim.setRepeatCount(1);
                        bounceAnim.setRepeatMode(ValueAnimator.REVERSE);
                        bounceAnim.setInterpolator(sDecelerator);
                        bounceAnim.setDuration(300);
                        
                        PropertyValuesHolder pvhTX =
                                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0);
                        PropertyValuesHolder pvhTY =
                                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0);
                        ObjectAnimator moveAnim = ObjectAnimator.ofPropertyValuesHolder(
                                buttonCopy, pvhTX, pvhTY);
                        moveAnim.setDuration(600);
                        bounceAnim.start();
                        moveAnim.start();
                        moveAnim.addListener(new AnimatorListenerAdapter() {
                            public void onAnimationEnd(Animator animation) {
                                switch (mEntryState) {
                                case (NAME_STATE) :
                                {
                                    Runnable runnable = new Runnable() {
                                        public void run() {
                                            mDifficultyButtons.setVisibility(View.VISIBLE);
                                            mNameButtons.setVisibility(View.GONE);
                                            popChildrenIn(mDifficultyButtons, null);
                                        }
                                    };
                                    slideToNext(mNameTV, mDifficultyTV, runnable);
                                    mEntryState = DIFFICULTY_STATE;
                                    break;
                                }
                                case (DIFFICULTY_STATE) :
                                {
                                    mDifficultyButtons.setVisibility(View.GONE);
                                    for (int i = 0; i < 5; ++i) {
                                        mCreditButtons1.addView(setupNumberButton(i));
                                    }
                                    for (int i = 5; i < 10; ++i) {
                                        mCreditButtons2.addView(setupNumberButton(i));
                                    }
                                    Runnable runnable = new Runnable() {
                                        public void run() {
                                            mCreditButtons1.setVisibility(View.VISIBLE);
                                            Runnable runnable = new Runnable() {
                                                public void run() {
                                                    mCreditButtons2.setVisibility(View.VISIBLE);
                                                    popChildrenIn(mCreditButtons2, null);
                                                }
                                            };
                                            popChildrenIn(mCreditButtons1, runnable);
                                        }
                                    };
                                    slideToNext(mDifficultyTV, mCreditTV, runnable);
                                    mEntryState = CREDIT_STATE;
                                }
                                    break;
                                }
                            }
                        });
                        return true;
                    }
                });
            }
        }
    }
    
    public void selectDifficulty(View clickedView) {
        buttonClick(clickedView, RelativeLayout.ALIGN_PARENT_RIGHT);
    }
    
    public void selectName(View clickedView) {
        buttonClick(clickedView, RelativeLayout.ALIGN_PARENT_LEFT);
    }
    
    private Button setupNumberButton(int number) {
        Button button = new Button(PlayerSetupActivity.this);
        button.setTextSize(15);
        button.setTextColor(Color.WHITE);
        button.setTypeface(mBobButton.getTypeface(), Typeface.BOLD);
        button.setText(Integer.toString(number));
        button.setPadding(0, 0, 0, 0);
        
        OvalShape oval = new OvalShape();
        ShapeDrawable drawable = new ShapeDrawable(oval);
        drawable.getPaint().setColor(0xFF << 24 | (int) (50 + 150 * Math.random()) << 16 |
                (int) (50 + 150 * Math.random()) << 8 |  (int) (50 + 150 * Math.random()));
        button.setBackground(drawable);

        button.setOnTouchListener(mButtonPressListener);

        return button;
    }

    ViewTreeObserver.OnPreDrawListener mPreDrawListener =
            new ViewTreeObserver.OnPreDrawListener() {
        
        @Override
        public boolean onPreDraw() {
            mContainer.getViewTreeObserver().removeOnPreDrawListener(this);
            mContainer.setScaleX(0);
            mContainer.setScaleY(0);
            mContainer.animate().scaleX(1).scaleY(1).setInterpolator(new OvershootInterpolator());
            mContainer.animate().setDuration(ToonGame.LONG_DURATION).withEndAction(new Runnable() {
                
                @Override
                public void run() {
                    ViewGroup buttonsParent = (ViewGroup) findViewById(R.id.nameButtons);
                    buttonsParent.setVisibility(View.VISIBLE);
                    popChildrenIn(buttonsParent, null);
                }
            });
            return false;
        }
    };
    
    private void popChildrenIn(ViewGroup parent, final Runnable endAction) {
        // for all children, scale in one at a time
        TimeInterpolator overshooter = new OvershootInterpolator();
        int childCount = parent.getChildCount();
        ObjectAnimator[] childAnims = new ObjectAnimator[childCount];
        for (int i = 0; i < childCount; ++i) {
            View child = parent.getChildAt(i);
            child.setScaleX(0);
            child.setScaleY(0);
            PropertyValuesHolder pvhSX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1);
            PropertyValuesHolder pvhSY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1);
            ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(child, pvhSX, pvhSY);
            anim.setDuration(150);
            anim.setInterpolator(overshooter);
            childAnims[i] = anim;
        }
        AnimatorSet set = new AnimatorSet();
        set.playSequentially(childAnims);
        set.start();
        if (endAction != null) {
            set.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    endAction.run();
                }
            });
        }
    }
    
    private void slideToNext(final SkewableTextView currentView,
            final SkewableTextView nextView, final Runnable endAction) {
        // skew/anticipate current view, slide off, set GONE, restore translation
        ObjectAnimator currentSkewer = ObjectAnimator.ofFloat(currentView, "skewX", -.5f);
        currentSkewer.setInterpolator(sDecelerator);
        ObjectAnimator currentMover = ObjectAnimator.ofFloat(currentView, View.TRANSLATION_X,
                -mContainer.getWidth());
        currentMover.setInterpolator(sLinearInterpolator);
        currentMover.setDuration(ToonGame.MEDIUM_DURATION);
        
        // set next view visible, translate off to right, skew,
        // slide on in parallel, overshoot/wobble, unskew
        nextView.setVisibility(View.VISIBLE);
        nextView.setSkewX(-.5f);
        nextView.setTranslationX(mContainer.getWidth());
        
        ObjectAnimator nextMover = ObjectAnimator.ofFloat(nextView, View.TRANSLATION_X, 0);
        nextMover.setInterpolator(sAccelerator);
        nextMover.setDuration(ToonGame.MEDIUM_DURATION);
        ObjectAnimator nextSkewer = ObjectAnimator.ofFloat(nextView, "skewX", 0);
        nextSkewer.setInterpolator(sOvershooter);
        
        AnimatorSet moverSet = new AnimatorSet();
        moverSet.playTogether(currentMover, nextMover);
        AnimatorSet fullSet = new AnimatorSet();
        fullSet.playSequentially(currentSkewer, moverSet, nextSkewer);
        fullSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentView.setSkewX(0);
                currentView.setVisibility(View.GONE);
                currentView.setTranslationX(0);
                if (endAction != null) {
                    endAction.run();
                }
            }
        });
        
        fullSet.start();
    }

}
