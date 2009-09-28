/*
 * Copyright (C) 2009 The Android Open Source Project
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

// Android JET demonstration code:
// See the JetBoyView.java file for examples on the use of the JetPlayer class.

package com.example.android.jetboy;

import com.example.android.jetboy.JetBoyView.JetBoyThread;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class JetBoy extends Activity implements View.OnClickListener {

    /** A handle to the thread that's actually running the animation. */
    private JetBoyThread mJetBoyThread;

    /** A handle to the View in which the game is running. */
    private JetBoyView mJetBoyView;

    // the play start button
    private Button mButton;

    // used to hit retry
    private Button mButtonRetry;

    // the window for instructions and such
    private TextView mTextView;

    // game window timer
    private TextView mTimerView;

    /**
     * Required method from parent class
     * 
     * @param savedInstanceState - The previous instance of this app
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        // get handles to the JetView from XML and the JET thread.
        mJetBoyView = (JetBoyView)findViewById(R.id.JetBoyView);
        mJetBoyThread = mJetBoyView.getThread();

        // look up the happy shiny button
        mButton = (Button)findViewById(R.id.Button01);
        mButton.setOnClickListener(this);

        mButtonRetry = (Button)findViewById(R.id.Button02);
        mButtonRetry.setOnClickListener(this);

        // set up handles for instruction text and game timer text
        mTextView = (TextView)findViewById(R.id.text);
        mTimerView = (TextView)findViewById(R.id.timer);

        mJetBoyView.setTimerView(mTimerView);

        mJetBoyView.SetButtonView(mButtonRetry);

        mJetBoyView.SetTextView(mTextView);
    }
    

    /**
     * Handles component interaction
     * 
     * @param v The object which has been clicked
     */
    public void onClick(View v) {
        // this is the first screen
        if (mJetBoyThread.getGameState() == JetBoyThread.STATE_START) {
            mButton.setText("PLAY!");
            mTextView.setVisibility(View.VISIBLE);

            mTextView.setText(R.string.helpText);
            mJetBoyThread.setGameState(JetBoyThread.STATE_PLAY);

        }
        // we have entered game play, now we about to start running
        else if (mJetBoyThread.getGameState() == JetBoyThread.STATE_PLAY) {
            mButton.setVisibility(View.INVISIBLE);
            mTextView.setVisibility(View.INVISIBLE);
            mTimerView.setVisibility(View.VISIBLE);
            mJetBoyThread.setGameState(JetBoyThread.STATE_RUNNING);

        }
        // this is a retry button
        else if (mButtonRetry.equals(v)) {

            mTextView.setText(R.string.helpText);

            mButton.setText("PLAY!");
            mButtonRetry.setVisibility(View.INVISIBLE);
            // mButtonRestart.setVisibility(View.INVISIBLE);

            mTextView.setVisibility(View.VISIBLE);
            mButton.setText("PLAY!");
            mButton.setVisibility(View.VISIBLE);

            mJetBoyThread.setGameState(JetBoyThread.STATE_PLAY);

        } else {
            Log.d("JB VIEW", "unknown click " + v.getId());

            Log.d("JB VIEW", "state is  " + mJetBoyThread.mState);

        }
    }

    /**
     * Standard override to get key-press events.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent msg) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return super.onKeyDown(keyCode, msg);
        } else {
            return mJetBoyThread.doKeyDown(keyCode, msg);
        }
    }

    /**
     * Standard override for key-up.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent msg) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return super.onKeyUp(keyCode, msg);
        } else {
            return mJetBoyThread.doKeyUp(keyCode, msg);
        }
    }
}
