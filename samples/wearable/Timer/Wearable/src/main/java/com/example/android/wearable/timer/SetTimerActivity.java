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

package com.example.android.wearable.timer;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.wearable.timer.util.Constants;
import com.example.android.wearable.timer.util.TimerFormat;

/** This class sets a timer. */
public class SetTimerActivity extends Activity implements WearableListView.ClickListener {

    public static final int NUMBER_OF_TIMES = 10;
    public static final String TAG = "SetTimerActivity";

    private ListViewItem[] mTimeOptions = new ListViewItem[NUMBER_OF_TIMES];
    private WearableListView mWearableListView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int paramLength = getIntent().getIntExtra(AlarmClock.EXTRA_LENGTH, 0);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "SetTimerActivity:onCreate=" + paramLength);
        }
        if (paramLength > 0 && paramLength <= 86400) {
            long durationMillis = paramLength * 1000;
            setupTimer(durationMillis);
            finish();
            return;
        }

        Resources res = getResources();
        for (int i = 0; i < NUMBER_OF_TIMES; i++) {
            mTimeOptions[i] = new ListViewItem(
                    res.getQuantityString(R.plurals.timer_minutes, i + 1, i + 1),
                    (i + 1) * 60 * 1000);
        }

        setContentView(R.layout.timer_set_timer);

        // Initialize a simple list of countdown time options.
        mWearableListView = (WearableListView) findViewById(R.id.times_list_view);
        mWearableListView.setAdapter(new TimerWearableListViewAdapter(this));
        mWearableListView.setClickListener(this);
    }

    /**
     * Sets up an alarm (and an associated notification) to go off after <code>duration</code>
     * milliseconds.
     */
    private void setupTimer(long duration) {
        NotificationManager notifyMgr =
                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE));

        // Delete dataItem and cancel a potential old countdown.
        cancelCountdown(notifyMgr);

        // Build notification and set it.
        notifyMgr.notify(Constants.NOTIFICATION_TIMER_COUNTDOWN, buildNotification(duration));

        // Register with the alarm manager to display a notification when the timer is done.
        registerWithAlarmManager(duration);

        finish();
    }

    @Override
    public void onClick(WearableListView.ViewHolder holder) {
        long duration = mTimeOptions[holder.getPosition()].duration;
        setupTimer(duration);
    }

    @Override
    public void onTopEmptyRegionClick() {
    }

    private void registerWithAlarmManager(long duration) {
        // Get the alarm manager.
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Create intent that gets fired when timer expires.
        Intent intent = new Intent(Constants.ACTION_SHOW_ALARM, null, this,
                TimerNotificationService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Calculate the time when it expires.
        long wakeupTime = System.currentTimeMillis() + duration;

        // Schedule an alarm.
        alarm.setExact(AlarmManager.RTC_WAKEUP, wakeupTime, pendingIntent);
    }

    /**
     * Build a notification including different actions and other various setup and return it.
     *
     * @param duration the duration of the timer.
     * @return the notification to display.
     */

    private Notification buildNotification(long duration) {
        // Intent to restart a timer.
        Intent restartIntent = new Intent(Constants.ACTION_RESTART_ALARM, null, this,
                TimerNotificationService.class);
        PendingIntent pendingIntentRestart = PendingIntent
                .getService(this, 0, restartIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Intent to delete a timer.
        Intent deleteIntent = new Intent(Constants.ACTION_DELETE_ALARM, null, this,
                TimerNotificationService.class);
        PendingIntent pendingIntentDelete = PendingIntent
                .getService(this, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Create countdown notification using a chronometer style.
        return new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_cc_alarm)
                .setContentTitle(getString(R.string.timer_time_left))
                .setContentText(TimerFormat.getTimeString(duration))
                .setUsesChronometer(true)
                .setWhen(System.currentTimeMillis() + duration)
                .addAction(R.drawable.ic_cc_alarm, getString(R.string.timer_restart),
                        pendingIntentRestart)
                .addAction(R.drawable.ic_cc_alarm, getString(R.string.timer_delete),
                        pendingIntentDelete)
                .setDeleteIntent(pendingIntentDelete)
                .setLocalOnly(true)
                .build();
    }

    /**
     * Cancels an old countdown and deletes the dataItem.
     *
     * @param notifyMgr the notification manager.
     */
    private void cancelCountdown(NotificationManager notifyMgr) {
        notifyMgr.cancel(Constants.NOTIFICATION_TIMER_EXPIRED);
    }

    /** Model class for the listview. */
    private static class ListViewItem {

        // Duration in milliseconds.
        long duration;
        // Label to display.
        private String label;

        public ListViewItem(String label, long duration) {
            this.label = label;
            this.duration = duration;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final class TimerWearableListViewAdapter extends WearableListView.Adapter {
        private final Context mContext;
        private final LayoutInflater mInflater;

        private TimerWearableListViewAdapter(Context context) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new WearableListView.ViewHolder(
                    mInflater.inflate(R.layout.timer_list_item, null));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
            TextView view = (TextView) holder.itemView.findViewById(R.id.time_text);
            view.setText(mTimeOptions[position].label);
            holder.itemView.setTag(position);
        }

        @Override
        public int getItemCount() {
            return NUMBER_OF_TIMES;
        }
    }

}
