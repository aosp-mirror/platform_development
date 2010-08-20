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
import com.example.android.apis.R;

import android.animation.*;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;

import java.util.ArrayList;


public class BouncingBalls extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bouncing_balls);
        LinearLayout container = (LinearLayout) findViewById(R.id.container);
        container.addView(new MyAnimationView(this));
    }

    public class MyAnimationView extends View implements Animator.AnimatorUpdateListener, Animatable.AnimatableListener {

        private static final int RED = 0xffFF8080;
        private static final int BLUE = 0xff8080FF;
        private static final int CYAN = 0xff80ffff;
        private static final int GREEN = 0xff80ff80;

        public final ArrayList<ShapeHolder> balls = new ArrayList<ShapeHolder>();
        Sequencer animation = null;

        public MyAnimationView(Context context) {
            super(context);

            // Create a colored background
            ShapeDrawable background = new ShapeDrawable(new RectShape());
            setBackgroundDrawable(background);
            Paint paint = background.getPaint();
            paint.setColor(RED);

            // Animate background color
            Animator colorAnim = new PropertyAnimator(3000, paint, "color", BLUE);
            colorAnim.setEvaluator(new RGBEvaluator());
            colorAnim.setRepeatCount(Animator.INFINITE);
            colorAnim.setRepeatMode(Animator.REVERSE);
            colorAnim.addUpdateListener(this); // forces invalidation to get the redraw
            colorAnim.start();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_DOWN &&
                    event.getAction() != MotionEvent.ACTION_MOVE) {
                return false;
            }
            ShapeHolder newBall = addBall(event.getX(), event.getY());

            // Bouncing animation with squash and stretch
            float startY = newBall.getY();
            float endY = getHeight() - 50f;
            float h = (float)getHeight();
            float eventY = event.getY();
            int duration = (int)(500 * ((h - eventY)/h));
            Animator bounceAnim = new PropertyAnimator(duration, newBall, "y", startY, endY);
            bounceAnim.setInterpolator(new AccelerateInterpolator());
            Animator squashAnim1 = new PropertyAnimator(duration/4, newBall, "x", newBall.getX(),
                    newBall.getX() - 25f);
            squashAnim1.setRepeatCount(1);
            squashAnim1.setRepeatMode(Animator.REVERSE);
            squashAnim1.setInterpolator(new DecelerateInterpolator());
            Animator squashAnim2 = new PropertyAnimator(duration/4, newBall, "width", newBall.getWidth(),
                    newBall.getWidth() + 50);
            squashAnim2.setRepeatCount(1);
            squashAnim2.setRepeatMode(Animator.REVERSE);
            squashAnim2.setInterpolator(new DecelerateInterpolator());
            Animator stretchAnim1 = new PropertyAnimator(duration/4, newBall, "y", endY,
                    endY + 25f);
            stretchAnim1.setRepeatCount(1);
            stretchAnim1.setInterpolator(new DecelerateInterpolator());
            stretchAnim1.setRepeatMode(Animator.REVERSE);
            Animator stretchAnim2 = new PropertyAnimator(duration/4, newBall, "height",
                    newBall.getHeight(), newBall.getHeight() - 25);
            stretchAnim2.setRepeatCount(1);
            stretchAnim2.setInterpolator(new DecelerateInterpolator());
            stretchAnim2.setRepeatMode(Animator.REVERSE);
            Animator bounceBackAnim = new PropertyAnimator(duration, newBall, "y", endY,
                    startY);
            bounceBackAnim.setInterpolator(new DecelerateInterpolator());
            // Sequence the down/squash&stretch/up animations
            Sequencer bouncer = new Sequencer();
            bouncer.play(bounceAnim).before(squashAnim1);
            bouncer.play(squashAnim1).with(squashAnim2);
            bouncer.play(squashAnim1).with(stretchAnim1);
            bouncer.play(squashAnim1).with(stretchAnim2);
            bouncer.play(bounceBackAnim).after(stretchAnim2);

            // Fading animation - remove the ball when the animation is done
            Animator fadeAnim = new PropertyAnimator(250, newBall, "alpha", 1f, 0f);
            fadeAnim.addListener(this);

            // Sequence the two animations to play one after the other
            Sequencer sequencer = new Sequencer();
            sequencer.play(bouncer).before(fadeAnim);

            // Start the animation
            sequencer.start();

            return true;
        }

        private ShapeHolder addBall(float x, float y) {
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
            balls.add(shapeHolder);
            return shapeHolder;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            for (int i = 0; i < balls.size(); ++i) {
                ShapeHolder shapeHolder = balls.get(i);
                canvas.save();
                canvas.translate(shapeHolder.getX(), shapeHolder.getY());
                shapeHolder.getShape().draw(canvas);
                canvas.restore();
            }
        }

        public void onAnimationUpdate(Animator animation) {
            invalidate();
        }

        @Override
        public void onAnimationCancel(Animatable animation) {
        }

        @Override
        public void onAnimationEnd(Animatable animation) {
            balls.remove(((PropertyAnimator)animation).getTarget());

        }

        @Override
        public void onAnimationRepeat(Animatable animation) {
        }

        @Override
        public void onAnimationStart(Animatable animation) {
        }
    }
}