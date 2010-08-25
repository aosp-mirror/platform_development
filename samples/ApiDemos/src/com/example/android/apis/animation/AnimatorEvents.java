/*
 * Copyright (C) 2010 The Android Open Source Project
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

// Need the following import to get access to the app resources, since this
// class is in a sub-package.
import android.widget.CheckBox;
import android.widget.TextView;
import com.example.android.apis.R;

import java.util.ArrayList;

import android.animation.Animatable;
import android.animation.Animator;
import android.animation.PropertyAnimator;
import android.animation.Sequencer;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * This demo shows how the AnimatableListener events work.
 */
public class AnimatorEvents extends Activity {

    TextView startText, repeatText, cancelText, endText;
    TextView startTextAnimator, repeatTextAnimator, cancelTextAnimator, endTextAnimator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.animator_events);
        LinearLayout container = (LinearLayout) findViewById(R.id.container);
        final MyAnimationView animView = new MyAnimationView(this);
        container.addView(animView);
        startText = (TextView) findViewById(R.id.startText);
        startText.setAlpha(.5f);
        repeatText = (TextView) findViewById(R.id.repeatText);
        repeatText.setAlpha(.5f);
        cancelText = (TextView) findViewById(R.id.cancelText);
        cancelText.setAlpha(.5f);
        endText = (TextView) findViewById(R.id.endText);
        endText.setAlpha(.5f);
        startTextAnimator = (TextView) findViewById(R.id.startTextAnimator);
        startTextAnimator.setAlpha(.5f);
        repeatTextAnimator = (TextView) findViewById(R.id.repeatTextAnimator);
        repeatTextAnimator.setAlpha(.5f);
        cancelTextAnimator = (TextView) findViewById(R.id.cancelTextAnimator);
        cancelTextAnimator.setAlpha(.5f);
        endTextAnimator = (TextView) findViewById(R.id.endTextAnimator);
        endTextAnimator.setAlpha(.5f);
        final CheckBox endCB = (CheckBox) findViewById(R.id.endCB);


        Button starter = (Button) findViewById(R.id.startButton);
        starter.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                animView.startAnimation(endCB.isChecked());
            }
        });

        Button canceler = (Button) findViewById(R.id.cancelButton);
        canceler.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                animView.cancelAnimation();
            }
        });

        Button ender = (Button) findViewById(R.id.endButton);
        ender.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                animView.endAnimation();
            }
        });

    }

    public class MyAnimationView extends View implements Animatable.AnimatableListener,
    Animator.AnimatorUpdateListener {

        public final ArrayList<ShapeHolder> balls = new ArrayList<ShapeHolder>();
        Animatable animation;
        ShapeHolder ball = null;
        boolean endImmediately = false;

        public MyAnimationView(Context context) {
            super(context);
            ball = createBall(25, 25);
        }

        private void createAnimation() {
            if (animation == null) {
                PropertyAnimator yAnim = new PropertyAnimator(1500, ball, "y",
                        ball.getY(), getHeight() - 50f);
                yAnim.setRepeatCount(1);
                yAnim.setRepeatMode(Animator.REVERSE);
                yAnim.setInterpolator(new AccelerateInterpolator(2f));
                yAnim.addUpdateListener(this);
                yAnim.addListener(this);

                PropertyAnimator xAnim = new PropertyAnimator(1500, ball, "x",
                        ball.getX(), ball.getX() + 300);
                xAnim.setRepeatCount(1);
                xAnim.setRepeatMode(Animator.REVERSE);
                xAnim.setInterpolator(new AccelerateInterpolator(2f));

                animation = new Sequencer();
                ((Sequencer) animation).playTogether(yAnim, xAnim);
                animation.addListener(this);
            }
        }

        public void startAnimation(boolean endImmediately) {
            this.endImmediately = endImmediately;
            startText.setAlpha(.5f);
            repeatText.setAlpha(.5f);
            cancelText.setAlpha(.5f);
            endText.setAlpha(.5f);
            startTextAnimator.setAlpha(.5f);
            repeatTextAnimator.setAlpha(.5f);
            cancelTextAnimator.setAlpha(.5f);
            endTextAnimator.setAlpha(.5f);
            createAnimation();
            animation.start();
        }

        public void cancelAnimation() {
            createAnimation();
            animation.cancel();
        }

        public void endAnimation() {
            createAnimation();
            animation.end();
        }

        private ShapeHolder createBall(float x, float y) {
            OvalShape circle = new OvalShape();
            circle.resize(50f, 50f);
            ShapeDrawable drawable = new ShapeDrawable(circle);
            ShapeHolder shapeHolder = new ShapeHolder(drawable);
            shapeHolder.setX(x - 25f);
            shapeHolder.setY(y - 25f);
            int red = (int)(Math.random() * 255);
            int green = (int)(Math.random() * 255);
            int blue = (int)(Math.random() * 255);
            int color = 0xff000000 | red << 16 | green << 8 | blue;
            Paint paint = drawable.getPaint(); //new Paint(Paint.ANTI_ALIAS_FLAG);
            int darkColor = 0xff000000 | red/4 << 16 | green/4 << 8 | blue/4;
            RadialGradient gradient = new RadialGradient(37.5f, 12.5f,
                    50f, color, darkColor, Shader.TileMode.CLAMP);
            paint.setShader(gradient);
            shapeHolder.setPaint(paint);
            return shapeHolder;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.save();
            canvas.translate(ball.getX(), ball.getY());
            ball.getShape().draw(canvas);
            canvas.restore();
        }

        public void onAnimationUpdate(Animator animation) {
            invalidate();
        }

        public void onAnimationStart(Animatable animation) {
            if (animation instanceof Sequencer) {
                startText.setAlpha(1f);
            } else {
                startTextAnimator.setAlpha(1f);
            }
            if (endImmediately) {
                animation.end();
            }
        }

        public void onAnimationEnd(Animatable animation) {
            if (animation instanceof Sequencer) {
                endText.setAlpha(1f);
            } else {
                endTextAnimator.setAlpha(1f);
            }
        }

        public void onAnimationCancel(Animatable animation) {
            if (animation instanceof Sequencer) {
                cancelText.setAlpha(1f);
            } else {
                cancelTextAnimator.setAlpha(1f);
            }
        }

        public void onAnimationRepeat(Animatable animation) {
            if (animation instanceof Sequencer) {
                repeatText.setAlpha(1f);
            } else {
                repeatTextAnimator.setAlpha(1f);
            }
        }
    }
}