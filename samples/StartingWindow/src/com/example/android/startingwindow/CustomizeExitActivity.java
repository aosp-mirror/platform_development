/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.example.android.startingwindow;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Resources;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.provider.Settings.Global;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.window.SplashScreen;
import android.window.SplashScreenView;

import androidx.annotation.RequiresApi;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

@RequiresApi(api = VERSION_CODES.S)
public class CustomizeExitActivity extends Activity {

  public static final Interpolator EASE_IN_OUT = new PathInterpolator(.48f, .11f, .53f, .87f);
  public static final Interpolator ACCELERATE = new AccelerateInterpolator();
  public static final int MOCK_DELAY = 200;
  public static final int MARGIN_ANIMATION_DURATION = 800;
  public static final int SPLASHSCREEN_ALPHA_ANIMATION_DURATION = 500;
  public static final int SPLASHSCREEN_TY_ANIMATION_DURATION = 1000;
  public static final boolean WAIT_FOR_AVD_TO_FINISH = true;
  public static final boolean DEBUG = false;

  boolean appReady = false;
  private float animationScale = 1.0f;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_activity);
    getWindow().setDecorFitsSystemWindows(false);

    // On Android S, this new method has been added to Activity
    SplashScreen splashScreen = getSplashScreen();

    // Setting an OnExitAnimationListener on the SplashScreen indicates
    // to the system that the application will handle the exit animation.
    // This means that the SplashScreen will be inflated in the application
    // process once the process has started.
    // Otherwise, the splashscreen stays in the SystemUI process and will be
    // dismissed once the first frame of the app is drawn
    splashScreen.setOnExitAnimationListener(this::onSplashScreenExit);

    animationScale = Global.getFloat(getContentResolver(),
        Global.ANIMATOR_DURATION_SCALE, 1.0f);

    // Create some artificial delay to simulate some local database fetch for example
    new Handler(Looper.getMainLooper())
        .postDelayed(() -> appReady = true, (long) (MOCK_DELAY * animationScale));

    // We use a pre draw listener to delay the removal of the splashscreen
    // until our app is ready
    final View content = findViewById(android.R.id.content);
    content.getViewTreeObserver().addOnPreDrawListener(
        new OnPreDrawListener() {
          @Override
          public boolean onPreDraw() {
            if (appReady) {
              content.getViewTreeObserver().removeOnPreDrawListener(this);
              return true;
            }
            return false;
          }
        }
    );
  }

  private void onSplashScreenExit(SplashScreenView view) {
    // At this point the first frame of the application is drawn and
    // the SplashScreen is ready to be removed.

    // It is now up to the application to animate the provided view
    // since the listener is registered
    AnimatorSet animatorSet = new AnimatorSet();
    animatorSet.setDuration(500);

    ObjectAnimator translationY = ObjectAnimator.ofFloat(view, "translationY", 0, view.getHeight());
    translationY.setInterpolator(ACCELERATE);

    ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 1, 0);
    alpha.setInterpolator(ACCELERATE);

    // To get fancy, we'll also animate our content
    ValueAnimator marginAnimator = createContentAnimation();

    animatorSet.playTogether(translationY, alpha, marginAnimator);
    animatorSet.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        view.remove();
      }
    });

    // If we want to wait for our Animated Vector Drawable to finish animating, we can compute
    // the remaining time to delay the start of the exit animation
    long delayMillis = Instant.now()
          .until(view.getIconAnimationStart().plus(view.getIconAnimationDuration()),
              ChronoUnit.MILLIS);
    view.postDelayed(animatorSet::start, (long) (delayMillis * animationScale));
  }

  private ValueAnimator createContentAnimation() {
    Resources r = getResources();
    float marginStart = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 30, r.getDisplayMetrics()
    );

    float marginEnd = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 10, r.getDisplayMetrics()
    );

    ValueAnimator marginAnimator = ValueAnimator.ofFloat(marginStart, marginEnd);
    marginAnimator.addUpdateListener(valueAnimator -> {
      LinearLayout container = findViewById(R.id.container);
      int marginTop = Math.round((Float) valueAnimator.getAnimatedValue());
      for (int i = 0; i < container.getChildCount(); i++) {
        View child = container.getChildAt(i);
        ((LayoutParams) child.getLayoutParams()).setMargins(0, marginTop, 0, 0);
      }
      container.requestLayout();
    });
    marginAnimator.setInterpolator(EASE_IN_OUT);
    return marginAnimator;
  }

  @SuppressLint("SetTextI18n")
  private void createShortcutButton() {
    final Button Button = new Button(this);
    Button.setText("Create shortcut");
    Button.setOnClickListener((v) -> createShortcut());
    addContentView(Button, new ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
  }

  private void createShortcut() {
    ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
    String shortcutId1 = "shortcutId1";
    shortcutManager.removeDynamicShortcuts(Collections.singletonList(shortcutId1));
    final ShortcutInfo.Builder b = new ShortcutInfo.Builder(this, shortcutId1);
    final ComponentName name = new ComponentName("com.example.android.startingwindow",
        "com.example.android.startingwindow.SecondActivity");
    final Intent i = new Intent(Intent.ACTION_MAIN)
        .setComponent(name);
    ShortcutInfo shortcut = b.setShortLabel("label")
        .setLongLabel("Long label")
        .setIntent(i)
        .setStartingTheme(android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        .build();
    shortcutManager.addDynamicShortcuts(Collections.singletonList(shortcut));
  }

  @Override
  protected void onPause() {
    // For the sake of this demo app, we kill the app on pause so
    // we see a cold start animation for each launch
    super.onPause();
    finishAndRemoveTask();
    Process.killProcess(Process.myPid());
  }
}