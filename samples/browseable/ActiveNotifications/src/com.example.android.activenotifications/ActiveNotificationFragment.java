package com.example.android.activenotifications;
/*
* Copyright 2015 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.common.logger.Log;

/**
 * A fragment that enables display of notifications.
 */
public class ActiveNotificationFragment extends Fragment {

    private static final String TAG = "ActiveNotificationFragment";

    private NotificationManager mNotificationManager;
    private TextView mNumberOfNotifications;

    // Every notification needs a unique ID otherwise the previous one would be overwritten.
    private int mNotificationId = 0;
    private PendingIntent mDeletePendingIntent;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification_builder, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateNumberOfNotifications();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mNotificationManager = (NotificationManager) getActivity().getSystemService(
                Context.NOTIFICATION_SERVICE);
        mNumberOfNotifications = (TextView) view.findViewById(R.id.number_of_notifications);

        // Supply actions to the button that is displayed on screen.
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.add_notification: {
                        addNotificationAndReadNumber();
                        break;
                    }
                }
            }
        };
        view.findViewById(R.id.add_notification).setOnClickListener(onClickListener);

        // [BEGIN create_pending_intent_for_deletion]
        // Create a PendingIntent to be fired upon deletion of a Notification.
        Intent deleteIntent = new Intent(ActiveNotificationActivity.ACTION_NOTIFICATION_DELETE);
        mDeletePendingIntent = PendingIntent.getBroadcast(getActivity(),
                2323 /* requestCode */, deleteIntent, 0);
        // [END create_pending_intent_for_deletion]
    }

    /**
     * Add a new {@link Notification} with sample data and send it to the system.
     * Then read the current number of displayed notifications for this application.
     */
    private void addNotificationAndReadNumber() {
        // [BEGIN create_notification]
        // Create a Notification and notify the system.
        final Notification.Builder builder = new Notification.Builder(getActivity())
                .setSmallIcon(R.mipmap.ic_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.sample_notification_content))
                .setAutoCancel(true)
                .setDeleteIntent(mDeletePendingIntent);

        final Notification notification = builder.build();
        mNotificationManager.notify(++mNotificationId, notification);
        // [END create_notification]
        Log.i(TAG, "Add a notification");
        updateNumberOfNotifications();
    }

    /**
     * Request the current number of notifications from the {@link NotificationManager} and
     * display them to the user.
     */
    protected void updateNumberOfNotifications() {
        // [BEGIN get_active_notifications]
        // Query the currently displayed notifications.
        final StatusBarNotification[] activeNotifications = mNotificationManager
                .getActiveNotifications();
        // [END get_active_notifications]
        final int numberOfNotifications = activeNotifications.length;
        mNumberOfNotifications.setText(getString(R.string.active_notifications,
                numberOfNotifications));
        Log.i(TAG, getString(R.string.active_notifications, numberOfNotifications));
    }
}
