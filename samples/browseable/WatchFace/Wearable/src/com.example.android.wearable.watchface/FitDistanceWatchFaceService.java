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

package com.example.android.wearable.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.result.DailyTotalResult;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Displays the user's daily distance total via Google Fit. Distance is polled initially when the
 * Google API Client successfully connects and once a minute after that via the onTimeTick callback.
 * If you want more frequent updates, you will want to add your own  Handler.
 *
 * Authentication IS a requirement to request distance from Google Fit on Wear. Otherwise, distance
 * will always come back as zero (or stay at whatever the distance was prior to you
 * de-authorizing watchface). To authenticate and communicate with Google Fit, you must create a
 * project in the Google Developers Console, activate the Fitness API, create an OAuth 2.0
 * client ID, and register the public certificate from your app's signed APK. More details can be
 * found here: https://developers.google.com/fit/android/get-started#step_3_enable_the_fitness_api
 *
 * In ambient mode, the seconds are replaced with an AM/PM indicator.
 *
 * On devices with low-bit ambient mode, the text is drawn without anti-aliasing. On devices which
 * require burn-in protection, the hours are drawn in normal rather than bold.
 *
 */
public class FitDistanceWatchFaceService extends CanvasWatchFaceService {

    private static final String TAG = "DistanceWatchFace";

    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for active mode (non-ambient).
     */
    private static final long ACTIVE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(1);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener,
            ResultCallback<DailyTotalResult> {

        private static final int BACKGROUND_COLOR = Color.BLACK;
        private static final int TEXT_HOURS_MINS_COLOR = Color.WHITE;
        private static final int TEXT_SECONDS_COLOR = Color.GRAY;
        private static final int TEXT_AM_PM_COLOR = Color.GRAY;
        private static final int TEXT_COLON_COLOR = Color.GRAY;
        private static final int TEXT_DISTANCE_COUNT_COLOR = Color.GRAY;

        private static final String COLON_STRING = ":";

        private static final int MSG_UPDATE_TIME = 0;

        /* Handler to update the time periodically in interactive mode. */
        private final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        Log.v(TAG, "updating time");
                        invalidate();
                        if (shouldUpdateTimeHandlerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs =
                                    ACTIVE_INTERVAL_MS - (timeMs % ACTIVE_INTERVAL_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        /**
         * Handles time zone and locale changes.
         */
        private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        /**
         * Unregistering an unregistered receiver throws an exception. Keep track of the
         * registration state to prevent that.
         */
        private boolean mRegisteredReceiver = false;

        private Paint mHourPaint;
        private Paint mMinutePaint;
        private Paint mSecondPaint;
        private Paint mAmPmPaint;
        private Paint mColonPaint;
        private Paint mDistanceCountPaint;

        private float mColonWidth;

        private Calendar mCalendar;

        private float mXOffset;
        private float mXDistanceOffset;
        private float mYOffset;
        private float mLineHeight;

        private String mAmString;
        private String mPmString;


        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        private boolean mLowBitAmbient;

        /*
         * Google API Client used to make Google Fit requests for step data.
         */
        private GoogleApiClient mGoogleApiClient;

        private boolean mDistanceRequested;

        private float mDistanceTotal = 0;

        @Override
        public void onCreate(SurfaceHolder holder) {
            Log.d(TAG, "onCreate");

            super.onCreate(holder);

            mDistanceRequested = false;
            mGoogleApiClient = new GoogleApiClient.Builder(FitDistanceWatchFaceService.this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Fitness.HISTORY_API)
                    .addApi(Fitness.RECORDING_API)
                    .addScope(new Scope(Scopes.FITNESS_LOCATION_READ))
                    // When user has multiple accounts, useDefaultAccount() allows Google Fit to
                    // associated with the main account for steps. It also replaces the need for
                    // a scope request.
                    .useDefaultAccount()
                    .build();

            setWatchFaceStyle(new WatchFaceStyle.Builder(FitDistanceWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            Resources resources = getResources();

            mYOffset = resources.getDimension(R.dimen.fit_y_offset);
            mLineHeight = resources.getDimension(R.dimen.fit_line_height);
            mAmString = resources.getString(R.string.fit_am);
            mPmString = resources.getString(R.string.fit_pm);

            mHourPaint = createTextPaint(TEXT_HOURS_MINS_COLOR, BOLD_TYPEFACE);
            mMinutePaint = createTextPaint(TEXT_HOURS_MINS_COLOR);
            mSecondPaint = createTextPaint(TEXT_SECONDS_COLOR);
            mAmPmPaint = createTextPaint(TEXT_AM_PM_COLOR);
            mColonPaint = createTextPaint(TEXT_COLON_COLOR);
            mDistanceCountPaint = createTextPaint(TEXT_DISTANCE_COUNT_COLOR);

            mCalendar = Calendar.getInstance();

        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int color) {
            return createTextPaint(color, NORMAL_TYPEFACE);
        }

        private Paint createTextPaint(int color, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(color);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            Log.d(TAG, "onVisibilityChanged: " + visible);

            super.onVisibilityChanged(visible);

            if (visible) {
                mGoogleApiClient.connect();

                registerReceiver();

                // Update time zone and date formats, in case they changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
            } else {
                unregisterReceiver();

                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.disconnect();
                }
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }


        private void registerReceiver() {
            if (mRegisteredReceiver) {
                return;
            }
            mRegisteredReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            FitDistanceWatchFaceService.this.registerReceiver(mReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredReceiver) {
                return;
            }
            mRegisteredReceiver = false;
            FitDistanceWatchFaceService.this.unregisterReceiver(mReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            Log.d(TAG, "onApplyWindowInsets: " + (insets.isRound() ? "round" : "square"));

            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = FitDistanceWatchFaceService.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.fit_x_offset_round : R.dimen.fit_x_offset);
            mXDistanceOffset =
                    resources.getDimension(
                            isRound ?
                            R.dimen.fit_steps_or_distance_x_offset_round :
                            R.dimen.fit_steps_or_distance_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.fit_text_size_round : R.dimen.fit_text_size);
            float amPmSize = resources.getDimension(isRound
                    ? R.dimen.fit_am_pm_size_round : R.dimen.fit_am_pm_size);

            mHourPaint.setTextSize(textSize);
            mMinutePaint.setTextSize(textSize);
            mSecondPaint.setTextSize(textSize);
            mAmPmPaint.setTextSize(amPmSize);
            mColonPaint.setTextSize(textSize);
            mDistanceCountPaint.setTextSize(
                    resources.getDimension(R.dimen.fit_steps_or_distance_text_size));

            mColonWidth = mColonPaint.measureText(COLON_STRING);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);

            boolean burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
            mHourPaint.setTypeface(burnInProtection ? NORMAL_TYPEFACE : BOLD_TYPEFACE);

            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);

            Log.d(TAG, "onPropertiesChanged: burn-in protection = " + burnInProtection
                    + ", low-bit ambient = " + mLowBitAmbient);

        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            getTotalDistance();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);

            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;;
                mHourPaint.setAntiAlias(antiAlias);
                mMinutePaint.setAntiAlias(antiAlias);
                mSecondPaint.setAntiAlias(antiAlias);
                mAmPmPaint.setAntiAlias(antiAlias);
                mColonPaint.setAntiAlias(antiAlias);
                mDistanceCountPaint.setAntiAlias(antiAlias);
            }
            invalidate();

            // Whether the timer should be running depends on whether we're in ambient mode (as well
            // as whether we're visible), so we may need to start or stop the timer.
            updateTimer();
        }

        private String formatTwoDigitNumber(int hour) {
            return String.format("%02d", hour);
        }

        private String getAmPmString(int amPm) {
            return amPm == Calendar.AM ? mAmString : mPmString;
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);
            boolean is24Hour = DateFormat.is24HourFormat(FitDistanceWatchFaceService.this);

            // Draw the background.
            canvas.drawColor(BACKGROUND_COLOR);

            // Draw the hours.
            float x = mXOffset;
            String hourString;
            if (is24Hour) {
                hourString = formatTwoDigitNumber(mCalendar.get(Calendar.HOUR_OF_DAY));
            } else {
                int hour = mCalendar.get(Calendar.HOUR);
                if (hour == 0) {
                    hour = 12;
                }
                hourString = String.valueOf(hour);
            }
            canvas.drawText(hourString, x, mYOffset, mHourPaint);
            x += mHourPaint.measureText(hourString);

            // Draw first colon (between hour and minute).
            canvas.drawText(COLON_STRING, x, mYOffset, mColonPaint);

            x += mColonWidth;

            // Draw the minutes.
            String minuteString = formatTwoDigitNumber(mCalendar.get(Calendar.MINUTE));
            canvas.drawText(minuteString, x, mYOffset, mMinutePaint);
            x += mMinutePaint.measureText(minuteString);

            // In interactive mode, draw a second colon followed by the seconds.
            // Otherwise, if we're in 12-hour mode, draw AM/PM
            if (!isInAmbientMode()) {
                canvas.drawText(COLON_STRING, x, mYOffset, mColonPaint);

                x += mColonWidth;
                canvas.drawText(formatTwoDigitNumber(
                        mCalendar.get(Calendar.SECOND)), x, mYOffset, mSecondPaint);
            } else if (!is24Hour) {
                x += mColonWidth;
                canvas.drawText(getAmPmString(
                        mCalendar.get(Calendar.AM_PM)), x, mYOffset, mAmPmPaint);
            }

            // Only render distance if there is no peek card, so they do not bleed into each other
            // in ambient mode.
            if (getPeekCardPosition().isEmpty()) {
                canvas.drawText(
                        getString(R.string.fit_distance, mDistanceTotal),
                        mXDistanceOffset,
                        mYOffset + mLineHeight,
                        mDistanceCountPaint);
            }
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            Log.d(TAG, "updateTimer");

            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldUpdateTimeHandlerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldUpdateTimeHandlerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        private void getTotalDistance() {

            Log.d(TAG, "getTotalDistance()");

            if ((mGoogleApiClient != null)
                    && (mGoogleApiClient.isConnected())
                    && (!mDistanceRequested)) {

                mDistanceRequested = true;

                PendingResult<DailyTotalResult> distanceResult =
                        Fitness.HistoryApi.readDailyTotal(
                                mGoogleApiClient,
                                DataType.TYPE_DISTANCE_DELTA);

                distanceResult.setResultCallback(this);
            }
        }

        @Override
        public void onConnected(Bundle connectionHint) {
            Log.d(TAG, "mGoogleApiAndFitCallbacks.onConnected: " + connectionHint);

            mDistanceRequested = false;

            // Subscribe covers devices that do not have Google Fit installed.
            subscribeToDistance();

            getTotalDistance();
        }

        /*
         * Subscribes to distance.
         */
        private void subscribeToDistance() {

            if ((mGoogleApiClient != null) && (mGoogleApiClient.isConnecting())) {

                Fitness.RecordingApi.subscribe(mGoogleApiClient, DataType.TYPE_DISTANCE_DELTA)
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status status) {
                                if (status.isSuccess()) {
                                    if (status.getStatusCode()
                                            == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                        Log.i(TAG, "Existing subscription for activity detected.");
                                    } else {
                                        Log.i(TAG, "Successfully subscribed!");
                                    }
                                } else {
                                    Log.i(TAG, "There was a problem subscribing.");
                                }
                            }
                        });
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(TAG, "mGoogleApiAndFitCallbacks.onConnectionSuspended: " + cause);
        }

        @Override
        public void onConnectionFailed(ConnectionResult result) {
            Log.d(TAG, "mGoogleApiAndFitCallbacks.onConnectionFailed: " + result);
        }

        @Override
        public void onResult(DailyTotalResult dailyTotalResult) {
            Log.d(TAG, "mGoogleApiAndFitCallbacks.onResult(): " + dailyTotalResult);

            mDistanceRequested = false;

            if (dailyTotalResult.getStatus().isSuccess()) {

                List<DataPoint> points = dailyTotalResult.getTotal().getDataPoints();

                if (!points.isEmpty()) {
                    mDistanceTotal = points.get(0).getValue(Field.FIELD_DISTANCE).asFloat();
                    Log.d(TAG, "distance updated: " + mDistanceTotal);
                }
            } else {
                Log.e(TAG, "onResult() failed! " + dailyTotalResult.getStatus().getStatusMessage());
            }
        }
    }
}