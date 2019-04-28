/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.example.android.themednavbarkeyboard;

import static android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A sample {@link InputMethodService} to demonstrates how to integrate the software keyboard with
 * custom themed navigation bar.
 */
public class ThemedNavBarKeyboard extends InputMethodService {

    private final int MINT_COLOR = 0xff98fb98;
    private final int LIGHT_RED = 0xff98fb98;

    private static final class BuildCompat {
        private static final boolean IS_RELEASE_BUILD = Build.VERSION.CODENAME.equals("REL");

        /**
         * The "effective" API version.
         * {@link android.os.Build.VERSION#SDK_INT} if the platform is a release build.
         * {@link android.os.Build.VERSION#SDK_INT} plus 1 if the platform is a development build.
         */
        private static final int EFFECTIVE_SDK_INT = IS_RELEASE_BUILD
                ? Build.VERSION.SDK_INT
                : Build.VERSION.SDK_INT + 1;
    }

    private KeyboardLayoutView mLayout;

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildCompat.EFFECTIVE_SDK_INT > Build.VERSION_CODES.P) {
            // Disable contrast for extended navbar gradient.
            getWindow().getWindow().setNavigationBarContrastEnforced(false);
        }
    }

    @Override
    public View onCreateInputView() {
        mLayout = new KeyboardLayoutView(this, getWindow().getWindow());
        return mLayout;
    }

    @Override
    public void onComputeInsets(Insets outInsets) {
        super.onComputeInsets(outInsets);

        // For floating mode, tweak Insets to avoid relayout in the target app.
        if (mLayout != null && mLayout.isFloatingMode()) {
            // Lying that the visible keyboard height is 0.
            outInsets.visibleTopInsets = getWindow().getWindow().getDecorView().getHeight();
            outInsets.contentTopInsets = getWindow().getWindow().getDecorView().getHeight();

            // But make sure that touch events are still sent to the IME.
            final int[] location = new int[2];
            mLayout.getLocationInWindow(location);
            final int x = location[0];
            final int y = location[1];
            outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_REGION;
            outInsets.touchableRegion.set(x, y, x + mLayout.getWidth(), y + mLayout.getHeight());
        }
    }

    private enum InputViewMode {
        /**
         * The input view is adjacent to the bottom Navigation Bar (if present). In this mode the
         * IME is expected to control Navigation Bar appearance, including button color.
         *
         * <p>Call {@link Window#setNavigationBarColor(int)} to change the navigation bar color.</p>
         *
         * <p>Call {@link View#setSystemUiVisibility(int)} with
         * {@link View#SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR} to optimize the navigation bar for
         * light color.</p>
         */
        SYSTEM_OWNED_NAV_BAR_LAYOUT,
        /**
         * The input view is extended to the bottom Navigation Bar (if present). In this mode the
         * IME is expected to control Navigation Bar appearance, including button color.
         *
         * <p>In this state, the system does not automatically place the input view above the
         * navigation bar.  You need to take care of the inset manually.</p>
         *
         * <p>Call {@link View#setSystemUiVisibility(int)} with
         * {@link View#SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR} to optimize the navigation bar for
         * light color.</p>

         * @see View#SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
         * @see View#SYSTEM_UI_FLAG_LAYOUT_STABLE
         */
        IME_OWNED_NAV_BAR_LAYOUT,
        /**
         * The input view is floating off of the bottom Navigation Bar region (if present). In this
         * mode the target application is expected to control Navigation Bar appearance, including
         * button color.
         */
        FLOATING_LAYOUT,
    }

    private final class KeyboardLayoutView extends LinearLayout {

        private final Window mWindow;
        private InputViewMode mMode = InputViewMode.SYSTEM_OWNED_NAV_BAR_LAYOUT;

        private void updateBottomPaddingIfNecessary(int newPaddingBottom) {
            if (getPaddingBottom() != newPaddingBottom) {
                setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), newPaddingBottom);
            }
        }

        @Override
        public WindowInsets onApplyWindowInsets(WindowInsets insets) {
            if (insets.isConsumed()
                    || (getSystemUiVisibility() & SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION) == 0) {
                // In this case we are not interested in consuming NavBar region.
                // Make sure that the bottom padding is empty.
                updateBottomPaddingIfNecessary(0);
                return insets;
            }

            // In some cases the bottom system window inset is not a navigation bar. Wear devices
            // that have bottom chin are examples.  For now, assume that it's a navigation bar if it
            // has the same height as the root window's stable bottom inset.
            final WindowInsets rootWindowInsets = getRootWindowInsets();
            if (rootWindowInsets != null && (rootWindowInsets.getStableInsetBottom() !=
                    insets.getSystemWindowInsetBottom())) {
                // This is probably not a NavBar.
                updateBottomPaddingIfNecessary(0);
                return insets;
            }

            final int possibleNavBarHeight = insets.getSystemWindowInsetBottom();
            updateBottomPaddingIfNecessary(possibleNavBarHeight);
            return possibleNavBarHeight <= 0
                    ? insets
                    : insets.replaceSystemWindowInsets(
                            insets.getSystemWindowInsetLeft(),
                            insets.getSystemWindowInsetTop(),
                            insets.getSystemWindowInsetRight(),
                            0 /* bottom */);
        }

        public KeyboardLayoutView(Context context, final Window window) {
            super(context);
            mWindow = window;
            setOrientation(VERTICAL);

            if (BuildCompat.EFFECTIVE_SDK_INT <= Build.VERSION_CODES.O_MR1) {
                final TextView textView = new TextView(context);
                textView.setText("ThemedNavBarKeyboard works only on API 28 and higher devices");
                textView.setGravity(Gravity.CENTER);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                textView.setPadding(20, 10, 20, 20);
                addView(textView);
                setBackgroundColor(LIGHT_RED);
                return;
            }

            // By default use "SeparateNavBarMode" mode.
            switchToSeparateNavBarMode(Color.DKGRAY, false /* lightNavBar */);
            setBackgroundColor(MINT_COLOR);

            addView(createButton("Floating Mode", () -> {
                switchToFloatingMode();
                setBackgroundColor(Color.TRANSPARENT);
            }));
            addView(createButton("Extended Dark Navigation Bar", () -> {
                switchToExtendedNavBarMode(false /* lightNavBar */);
                final GradientDrawable drawable = new GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[] {MINT_COLOR, Color.DKGRAY});
                setBackground(drawable);
            }));
            addView(createButton("Extended Light Navigation Bar", () -> {
                switchToExtendedNavBarMode(true /* lightNavBar */);
                final GradientDrawable drawable = new GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[] {MINT_COLOR, Color.WHITE});
                setBackground(drawable);
            }));
            addView(createButton("Separate Dark Navigation Bar", () -> {
                switchToSeparateNavBarMode(Color.DKGRAY, false /* lightNavBar */);
                setBackgroundColor(MINT_COLOR);
            }));
            addView(createButton("Separate Light Navigation Bar", () -> {
                switchToSeparateNavBarMode(Color.GRAY, true /* lightNavBar */);
                setBackgroundColor(MINT_COLOR);
            }));

            // Spacer
            addView(new View(getContext()), 0, 40);
        }

        public boolean isFloatingMode() {
            return mMode == InputViewMode.FLOATING_LAYOUT;
        }

        private View createButton(String text, final Runnable onClickCallback) {
            final Button button = new Button(getContext());
            button.setText(text);
            button.setOnClickListener(view -> onClickCallback.run());
            return button;
        }

        private void updateSystemUiFlag(int flags) {
            final int maskFlags = SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            final int visFlags = getSystemUiVisibility();
            setSystemUiVisibility((visFlags & ~maskFlags) | (flags & maskFlags));
        }

        /**
         * Updates the current input view mode to {@link InputViewMode#FLOATING_LAYOUT}.
         */
        private void switchToFloatingMode() {
            mMode = InputViewMode.FLOATING_LAYOUT;

            final int prevFlags = mWindow.getAttributes().flags;

            // This allows us to keep the navigation bar appearance based on the target application,
            // rather than the IME itself.
            mWindow.setFlags(0, FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            updateSystemUiFlag(0);

            // View#onApplyWindowInsets() will not be called if direct or indirect parent View
            // consumes all the insets.  Hence we need to make sure that the bottom padding is
            // cleared here.
            updateBottomPaddingIfNecessary(0);

            // For some reasons, seems that we need to post another requestLayout() when
            // FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS is changed.
            // TODO: Investigate the reason.
            if ((prevFlags & FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS) != 0) {
                post(() -> requestLayout());
            }
        }

        /**
         * Updates the current input view mode to {@link InputViewMode#SYSTEM_OWNED_NAV_BAR_LAYOUT}.
         *
         * @param navBarColor color to be passed to {@link Window#setNavigationBarColor(int)}.
         *                    {@link Color#TRANSPARENT} cannot be used here because it hides the
         *                    color view itself. Consider floating mode for that use case.
         * @param isLightNavBar {@code true} when the navigation bar should be optimized for light
         *                      color
         */
        private void switchToSeparateNavBarMode(int navBarColor, boolean isLightNavBar) {
            mMode = InputViewMode.SYSTEM_OWNED_NAV_BAR_LAYOUT;
            mWindow.setNavigationBarColor(navBarColor);

            // This allows us to use setNavigationBarColor() + SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.
            mWindow.setFlags(FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS, FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            updateSystemUiFlag(isLightNavBar ? SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0);

            // View#onApplyWindowInsets() will not be called if direct or indirect parent View
            // consumes all the insets.  Hence we need to make sure that the bottom padding is
            // cleared here.
            updateBottomPaddingIfNecessary(0);
        }

        /**
         * Updates the current input view mode to {@link InputViewMode#IME_OWNED_NAV_BAR_LAYOUT}.
         *
         * @param isLightNavBar {@code true} when the navigation bar should be optimized for light
         *                      color
         */
        private void switchToExtendedNavBarMode(boolean isLightNavBar) {
            mMode = InputViewMode.IME_OWNED_NAV_BAR_LAYOUT;

            // This hides the ColorView.
            mWindow.setNavigationBarColor(Color.TRANSPARENT);

            // This allows us to use setNavigationBarColor() + SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.
            mWindow.setFlags(FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS, FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            updateSystemUiFlag(SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | (isLightNavBar ? SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0));
        }
    }
}
