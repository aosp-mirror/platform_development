/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.example.android.apis.view;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.apis.R;

/**
 * This activity demonstrates some of the available ways to reduce the size or visual contrast of
 * the system decor, in order to better focus the user's attention or use available screen real
 * estate on the task at hand.
 */
public class OverscanActivity extends Activity {
    public static class IV extends ImageView {
        private OverscanActivity mActivity;
        public IV(Context context) {
            super(context);
        }
        public IV(Context context, AttributeSet attrs) {
            super(context, attrs);
        }
        public void setActivity(OverscanActivity act) {
            mActivity = act;
        }
        public void onSizeChanged(int w, int h, int oldw, int oldh) {
            mActivity.refreshSizes();
        }
        public void onSystemUiVisibilityChanged(int visibility) {
            mActivity.getState().onSystemUiVisibilityChanged(visibility);
        }
    }

    private interface State {
        void apply();
        State next();
        void onSystemUiVisibilityChanged(int visibility);
    }
    private class NormalState implements State {
        public void apply() {
            display("Normal");
            setFullscreen(false);
            mImage.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
        public State next() {
            return new FullscreenState();
        }
        public void onSystemUiVisibilityChanged(int visibility) {
        }
    }
    private class FullscreenState implements State {
        public void apply() {
            display("FULLSCREEN");
            setFullscreen(true);
        }
        public State next() {
            return new FullscreenLightsOutState();
        }
        public void onSystemUiVisibilityChanged(int visibility) {
        }
    }
    private class FullscreenLightsOutState implements State {
        public void apply() {
            display("FULLSCREEN + LOW_PROFILE");
            mImage.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
        public State next() {
            return new OverscanState();
        }
        public void onSystemUiVisibilityChanged(int visibility) {
        }
    }
    private class OverscanState implements State {
        public void apply() {
            display("FULLSCREEN + HIDE_NAVIGATION");
            mImage.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
        public State next() {
            return new NormalState();
        }
        public void onSystemUiVisibilityChanged(int visibility) {
        }
    }

    private void setFullscreen(boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        if (on) {
            winParams.flags |=  bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    private String getDisplaySize() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        return String.format("DisplayMetrics = (%d x %d)", dm.widthPixels, dm.heightPixels);
    }
    private String getViewSize() {
        return String.format("View = (%d,%d - %d,%d)",
                mImage.getLeft(), mImage.getTop(),
                mImage.getRight(), mImage.getBottom());
    }
    void refreshSizes() {
        mText2.setText(getDisplaySize() + " " + getViewSize());
    }
    private void display(String text) {
        mText1.setText(text);
        refreshSizes();
    }
    State getState() {
        return mState;
    }

    static int TOAST_LENGTH = 500;
    IV mImage;
    TextView mText1, mText2;
    State mState;

    public OverscanActivity() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // we need to ask for LAYOUT_IN_SCREEN before the window decor appears
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        win.setAttributes(winParams);

        setContentView(R.layout.overscan);
        mImage = (IV) findViewById(R.id.image);
        mImage.setActivity(this);
        mText1 = (TextView) findViewById(R.id.text1);
        mText2 = (TextView) findViewById(R.id.text2);
    }

    @Override
    public void onAttachedToWindow() {
        mState = new NormalState();
        mState.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void clicked(View v) {
        mState = mState.next();
        mState.apply();
    }
}
