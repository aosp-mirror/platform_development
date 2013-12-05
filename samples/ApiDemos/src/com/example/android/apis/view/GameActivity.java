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

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SearchView.OnQueryTextListener;

import com.example.android.apis.R;
import com.example.android.apis.graphics.TouchPaint;

/**
 * This activity demonstrates how to use the system UI flags to
 * implement an immersive game.
 */
public class GameActivity extends Activity {

    /**
     * Implementation of a view for the game, filling the entire screen.
     */
//BEGIN_INCLUDE(content)
    public static class Content extends TouchPaint.PaintView implements
            View.OnSystemUiVisibilityChangeListener, View.OnClickListener {
        Activity mActivity;
        Button mPlayButton;
        boolean mPaused;
        int mLastSystemUiVis;
        boolean mUpdateSystemUi;

        Runnable mFader = new Runnable() {
            @Override public void run() {
                fade();
                if (mUpdateSystemUi) {
                    updateNavVisibility();
                }
                if (!mPaused) {
                    getHandler().postDelayed(mFader, 1000/30);
                }
            }
        };

        public Content(Context context, AttributeSet attrs) {
            super(context, attrs);
            setOnSystemUiVisibilityChangeListener(this);
        }

        public void init(Activity activity, Button playButton) {
            // This called by the containing activity to supply the surrounding
            // state of the game that it will interact with.
            mActivity = activity;
            mPlayButton = playButton;
            mPlayButton.setOnClickListener(this);
            setGamePaused(true);
        }

        @Override public void onSystemUiVisibilityChange(int visibility) {
            // Detect when we go out of nav-hidden mode, to reset back to having
            // it hidden; our game wants those elements to stay hidden as long
            // as it is being played and stay shown when paused.
            int diff = mLastSystemUiVis ^ visibility;
            mLastSystemUiVis = visibility;
            if (!mPaused && (diff&SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0
                    && (visibility&SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                // We are running and the system UI navigation has become
                // shown...  we want it to remain hidden, so update our system
                // UI state at the next game loop.
                mUpdateSystemUi = true;
            }
        }

        @Override protected void onWindowVisibilityChanged(int visibility) {
            super.onWindowVisibilityChanged(visibility);

            // When we become visible or invisible, play is paused.
            setGamePaused(true);
        }

        @Override
        public void onWindowFocusChanged(boolean hasWindowFocus) {
            super.onWindowFocusChanged(hasWindowFocus);

            // When we become visible or invisible, play is paused.
            // Optional: pause game when window loses focus.  This will cause it to
            // pause, for example, when the notification shade is pulled down.
            if (!hasWindowFocus) {
                //setGamePaused(true);
            }
        }

        @Override public void onClick(View v) {
            if (v == mPlayButton) {
                // Clicking on the play/pause button toggles its state.
                setGamePaused(!mPaused);
            }
        }

        void setGamePaused(boolean paused) {
            mPaused = paused;
            mPlayButton.setText(paused ? R.string.play : R.string.pause);
            setKeepScreenOn(!paused);
            updateNavVisibility();
            Handler h = getHandler();
            if (h != null) {
                getHandler().removeCallbacks(mFader);
                if (!paused) {
                    mFader.run();
                    text("Draw!");
                }
            }
        }

        void updateNavVisibility() {
            int newVis = SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | SYSTEM_UI_FLAG_LAYOUT_STABLE;
            if (!mPaused) {
                newVis |= SYSTEM_UI_FLAG_LOW_PROFILE | SYSTEM_UI_FLAG_FULLSCREEN
                        | SYSTEM_UI_FLAG_HIDE_NAVIGATION  | SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }

            // Set the new desired visibility.
            setSystemUiVisibility(newVis);
            mUpdateSystemUi = false;
        }
    }
//END_INCLUDE(content)

    Content mContent;

    public GameActivity() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.game);
        mContent = (Content)findViewById(R.id.content);
        mContent.init(this, (Button)findViewById(R.id.play));
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Pause game when its activity is paused.
        mContent.setGamePaused(true);
    }
}
