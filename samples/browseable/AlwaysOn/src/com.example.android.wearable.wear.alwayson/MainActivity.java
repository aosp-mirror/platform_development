/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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
package com.example.android.wearable.wear.alwayson;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates support for Ambient screens by extending WearableActivity and overriding
 * onEnterAmbient, onUpdateAmbient, and onExitAmbient.
 *
 * There are two modes (Active and Ambient). To trigger future updates (data/screen), we use a
 * custom Handler for the "Active" mode and an Alarm for the "Ambient" mode.
 *
 * Why don't we use just one? Handlers are generally less battery intensive and can be triggered
 * every second. However, they can not wake up the processor (common in Ambient mode).
 *
 * Alarms can wake up the processor (what we need for Ambient), but they struggle with quick updates
 * (less than one second) and are much less efficient compared to Handlers.
 *
 * Therefore, we use Handlers for "Active" mode (can trigger every second and are better on the
 * battery), and we use Alarms for "Ambient" mode (only need to update once every 20 seconds and
 * they can wake up a sleeping processor).
 *
 * Again, the Activity waits 20 seconds between doing any processing (getting data, updating screen
 * etc.) while in ambient mode to conserving battery life (processor allowed to sleep). If you can
 * hold off on updates for a full minute, you can throw away all the Alarm code and just use
 * onUpdateAmbient() to save even more battery life.
 *
 * As always, you will still want to apply the performance guidelines outlined in the Watch Faces
 * documention to your app.
 *
 * Finally, in ambient mode, this Activity follows the same best practices outlined in the
 * Watch Faces API documentation, e.g., keep most pixels black, avoid large blocks of white pixels,
 * use only black and white, and disable anti-aliasing.
 *
 */
public class MainActivity extends WearableActivity {

    private static final String TAG = "MainActivity";

    /** Custom 'what' for Message sent to Handler. */
    private static final int MSG_UPDATE_SCREEN = 0;

    /** Milliseconds between updates based on state. */
    private static final long ACTIVE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(1);
    private static final long AMBIENT_INTERVAL_MS = TimeUnit.SECONDS.toMillis(20);

    /** Tracks latest ambient details, such as burnin offsets, etc. */
    private Bundle mAmbientDetails;

    private TextView mTimeTextView;
    private TextView mTimeStampTextView;
    private TextView mStateTextView;
    private TextView mUpdateRateTextView;
    private TextView mDrawCountTextView;

    private final SimpleDateFormat sDateFormat =
            new SimpleDateFormat("HH:mm:ss", Locale.US);

    private volatile int mDrawCount = 0;


    /**
     * Since the handler (used in active mode) can't wake up the processor when the device is in
     * ambient mode and undocked, we use an Alarm to cover ambient mode updates when we need them
     * more frequently than every minute. Remember, if getting updates once a minute in ambient
     * mode is enough, you can do away with the Alarm code and just rely on the onUpdateAmbient()
     * callback.
     */
    private AlarmManager mAmbientStateAlarmManager;
    private PendingIntent mAmbientStatePendingIntent;

    /**
     * This custom handler is used for updates in "Active" mode. We use a separate static class to
     * help us avoid memory leaks.
     */
    private final Handler mActiveModeUpdateHandler = new UpdateHandler(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        setAmbientEnabled();

        mAmbientStateAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent ambientStateIntent = new Intent(getApplicationContext(), MainActivity.class);

        mAmbientStatePendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0 /* requestCode */,
                ambientStateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);


        /** Determines whether watch is round or square and applies proper view. **/
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {

                mTimeTextView = (TextView) stub.findViewById(R.id.time);
                mTimeStampTextView = (TextView) stub.findViewById(R.id.time_stamp);
                mStateTextView = (TextView) stub.findViewById(R.id.state);
                mUpdateRateTextView = (TextView) stub.findViewById(R.id.update_rate);
                mDrawCountTextView = (TextView) stub.findViewById(R.id.draw_count);

                refreshDisplayAndSetNextUpdate();
            }
        });
    }

    /**
     * This is mostly triggered by the Alarms we set in Ambient mode and informs us we need to
     * update the screen (and process any data).
     */
    @Override
    public void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent(): " + intent);
        super.onNewIntent(intent);

        setIntent(intent);

        refreshDisplayAndSetNextUpdate();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");

        mActiveModeUpdateHandler.removeMessages(MSG_UPDATE_SCREEN);
        mAmbientStateAlarmManager.cancel(mAmbientStatePendingIntent);

        super.onDestroy();
    }

    /**
     * Prepares UI for Ambient view.
     */
    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        Log.d(TAG, "onEnterAmbient()");
        super.onEnterAmbient(ambientDetails);

        /**
         * In this sample, we aren't using the ambient details bundle (EXTRA_BURN_IN_PROTECTION or
         * EXTRA_LOWBIT_AMBIENT), but if you need them, you can pull them from the local variable
         * set here.
         */
        mAmbientDetails = ambientDetails;

        /** Clears Handler queue (only needed for updates in active mode). */
        mActiveModeUpdateHandler.removeMessages(MSG_UPDATE_SCREEN);

        /**
         * Following best practices outlined in WatchFaces API (keeping most pixels black,
         * avoiding large blocks of white pixels, using only black and white,
         * and disabling anti-aliasing anti-aliasing, etc.)
         */
        mStateTextView.setTextColor(Color.WHITE);
        mUpdateRateTextView.setTextColor(Color.WHITE);
        mDrawCountTextView.setTextColor(Color.WHITE);

        mTimeTextView.getPaint().setAntiAlias(false);
        mTimeStampTextView.getPaint().setAntiAlias(false);
        mStateTextView.getPaint().setAntiAlias(false);
        mUpdateRateTextView.getPaint().setAntiAlias(false);
        mDrawCountTextView.getPaint().setAntiAlias(false);

        refreshDisplayAndSetNextUpdate();
    }

    /**
     * Updates UI in Ambient view (once a minute). Because we need to update UI sooner than that
     * (every ~20 seconds), we also use an Alarm. However, since the processor is awake for this
     * callback, we might as well call refreshDisplayAndSetNextUpdate() to update screen and reset
     * the Alarm.
     *
     * If you are happy with just updating the screen once a minute in Ambient Mode (which will be
     * the case a majority of the time), then you can just use this method and remove all
     * references/code regarding Alarms.
     */
    @Override
    public void onUpdateAmbient() {
        Log.d(TAG, "onUpdateAmbient()");
        super.onUpdateAmbient();

        refreshDisplayAndSetNextUpdate();
    }

    /**
     * Prepares UI for Active view (non-Ambient).
     */
    @Override
    public void onExitAmbient() {
        Log.d(TAG, "onExitAmbient()");
        super.onExitAmbient();

        /** Clears out Alarms since they are only used in ambient mode. */
        mAmbientStateAlarmManager.cancel(mAmbientStatePendingIntent);

        mStateTextView.setTextColor(Color.GREEN);
        mUpdateRateTextView.setTextColor(Color.GREEN);
        mDrawCountTextView.setTextColor(Color.GREEN);

        mTimeTextView.getPaint().setAntiAlias(true);
        mTimeStampTextView.getPaint().setAntiAlias(true);
        mStateTextView.getPaint().setAntiAlias(true);
        mUpdateRateTextView.getPaint().setAntiAlias(true);
        mDrawCountTextView.getPaint().setAntiAlias(true);

        refreshDisplayAndSetNextUpdate();
    }

    /**
     * Loads data/updates screen (via method), but most importantly, sets up the next refresh
     * (active mode = Handler and ambient mode = Alarm).
     */
    private void refreshDisplayAndSetNextUpdate() {

        loadDataAndUpdateScreen();

        long timeMs = System.currentTimeMillis();

        if (isAmbient()) {
            /** Calculate next trigger time (based on state). */
            long delayMs = AMBIENT_INTERVAL_MS - (timeMs % AMBIENT_INTERVAL_MS);
            long triggerTimeMs = timeMs + delayMs;

            /**
             * Note: Make sure you have set activity launchMode to singleInstance in the manifest.
             * Otherwise, it is easy for the AlarmManager launch intent to open a new activity
             * every time the Alarm is triggered rather than reusing this Activity.
             */
            mAmbientStateAlarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    mAmbientStatePendingIntent);

        } else {
            /** Calculate next trigger time (based on state). */
            long delayMs = ACTIVE_INTERVAL_MS - (timeMs % ACTIVE_INTERVAL_MS);

            mActiveModeUpdateHandler.removeMessages(MSG_UPDATE_SCREEN);
            mActiveModeUpdateHandler.sendEmptyMessageDelayed(MSG_UPDATE_SCREEN, delayMs);
        }
    }

    /**
     * Updates display based on Ambient state. If you need to pull data, you should do it here.
     */
    private void loadDataAndUpdateScreen() {

        mDrawCount += 1;
        long currentTimeMs = System.currentTimeMillis();
        Log.d(TAG, "loadDataAndUpdateScreen(): " + currentTimeMs + "(" + isAmbient() + ")");

        if (isAmbient()) {

            mTimeTextView.setText(sDateFormat.format(new Date()));
            mTimeStampTextView.setText(getString(R.string.timestamp_label, currentTimeMs));

            mStateTextView.setText(getString(R.string.mode_ambient_label));
            mUpdateRateTextView.setText(
                    getString(R.string.update_rate_label, (AMBIENT_INTERVAL_MS / 1000)));

            mDrawCountTextView.setText(getString(R.string.draw_count_label, mDrawCount));

        } else {
            mTimeTextView.setText(sDateFormat.format(new Date()));
            mTimeStampTextView.setText(getString(R.string.timestamp_label, currentTimeMs));

            mStateTextView.setText(getString(R.string.mode_active_label));
            mUpdateRateTextView.setText(
                    getString(R.string.update_rate_label, (ACTIVE_INTERVAL_MS / 1000)));

            mDrawCountTextView.setText(getString(R.string.draw_count_label, mDrawCount));
        }
    }

    /**
     * Handler separated into static class to avoid memory leaks.
     */
    private static class UpdateHandler extends Handler {
        private final WeakReference<MainActivity> mMainActivityWeakReference;

        public UpdateHandler(MainActivity reference) {
            mMainActivityWeakReference = new WeakReference<MainActivity>(reference);
        }

        @Override
        public void handleMessage(Message message) {
            MainActivity mainActivity = mMainActivityWeakReference.get();

            if (mainActivity != null) {
                switch (message.what) {
                    case MSG_UPDATE_SCREEN:
                        mainActivity.refreshDisplayAndSetNextUpdate();
                        break;
                }
            }
        }
    }
}