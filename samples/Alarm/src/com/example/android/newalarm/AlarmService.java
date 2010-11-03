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

package com.example.android.newalarm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.widget.Toast;

/**
 * <p>
 * This class implements a service. The service is started by AlarmActivity, which contains a
 * repeating countdown timer that sends a PendingIntent. The user starts and stops the timer with
 * buttons in the UI.
 * </p>
 * <p>
 * When this service is started, it creates a Runnable and starts it in a new Thread. The
 * Runnable does a synchronized lock on the service's Binder object for 15 seconds, then issues
 * a stopSelf(). The net effect is a new worker thread that takes 15 seconds to run and then
 * shuts down the entire service. The activity restarts the service after 15 more seconds, when the
 * countdown timer triggers again.
 * </p>
 * <p>
 * This service is provided as the service under test for the sample test application
 * AlarmServiceTest.
 * </p>
 * <p>
 * Note: Since this sample is based on the Android 1.5 platform, it does not implement
 * onStartCommand. See the Javadoc for android.app.Service for more details.
 * </p>
 */
public class AlarmService extends Service {
    // Defines a label for the thread that this service starts
    private static final String ALARM_SERVICE_THREAD = "AlarmService";

    // Defines 15 seconds
    public static final long WAIT_TIME_SECONDS = 15;

    // Define the number of milliseconds in one second
    public static final long MILLISECS_PER_SEC = 1000;

    /*
     * For testing purposes, the following variables are defined as fields and set to
     * package visibility.
     */

    // The NotificationManager used to send notifications to the status bar.
    NotificationManager mNotificationManager;

    // An Intent that displays the client if the user clicks the notification.
    PendingIntent mContentIntent;

    // A Notification to send to the Notification Manager when the service is started.
    Notification mNotification;

    // A Binder, used as the lock object for the worker thread.
    IBinder mBinder = new AlarmBinder();

    // A Thread object that will run the background task
    Thread mWorkThread;

    // The Runnable that is the service's "task". This illustrates how a service is used to
    // offload work from a client.
    Runnable mWorkTask = new Runnable() {
        public void run() {
            // Sets the wait time to 15 seconds, simulating a 15-second background task.
            long waitTime = System.currentTimeMillis() + WAIT_TIME_SECONDS * MILLISECS_PER_SEC;

            // Puts the wait in a while loop to ensure that it actually waited 15 seconds.
            // This covers the situation where an interrupt might have overridden the wait.
            while (System.currentTimeMillis() < waitTime) {
                // Waits for 15 seconds or interruption
                synchronized (mBinder) {
                    try {
                        // Waits for 15 seconds or until an interrupt triggers an exception.
                        // If an interrupt occurs, the wait is recalculated to ensure a net
                        // wait of 15 seconds.
                        mBinder.wait(waitTime - System.currentTimeMillis());
                    } catch (InterruptedException e) {
                    }
                }
            }
            // Stops the current service. In response, Android calls onDestroy().
            stopSelf();
        }
    };

    /**
     *  Makes a full concrete subclass of Binder, rather than doing it in line, for readability.
     */
    public class AlarmBinder extends Binder {
        // Constructor. Calls the super constructor to set up the instance.
        public AlarmBinder() {
            super();
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
            throws RemoteException {

            // Call the parent method with the arguments passed in
            return super.onTransact(code, data, reply, flags);
        }
    }

    /**
     * Initializes the service when it is first started by a call to startService() or
     * bindService().
     */
    @Override
    public void onCreate() {
        // Gets a handle to the system mNotification service.
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Updates the status bar to indicate that this service is running.
        showNotification();

        // Creates a new thread. A new thread is used so that the service's work doesn't block
        // anything on the calling client's thread. By default, a service runs in the same
        // process and thread as the client that starts it.
        mWorkThread = new Thread(
            null,  // threadgroup (in this case, null)
            mWorkTask, // the Runnable that will run in this thread
            ALARM_SERVICE_THREAD
        );
        // Starts the thread
        mWorkThread.start();
    }

    /**
     * Stops the service in response to the stopSelf() issued when the wait is over. Other
     * clients that use this service could stop it by issuing a stopService() or a stopSelf() on
     * the service object.
     */
    @Override
    public void onDestroy() {
        // Cancels the status bar mNotification based on its ID, which is set in showNotification().
        mNotificationManager.cancel(R.string.alarm_service_started);

        // Sends a notification to the screen.
        Toast.makeText(
            this,  // the current context
            R.string.alarm_service_finished,  // the message to show
            Toast.LENGTH_LONG   // how long to keep the message on the screen
        ).show();  // show the text
    }

    // Returns the service's binder object to clients that issue onBind().
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Displays a notification in the status bar that this service is running. This method
     * also creates an Intent for the AlarmActivity client and attaches it to the notification
     * line. If the user clicks the line in the expanded status window, the Intent triggers
     * AlarmActivity.
     */
    private void showNotification() {
        // Sets the text to use for the status bar and status list views.
        CharSequence notificationText = getText(R.string.alarm_service_started);

        // Sets the icon, status bar text, and display time for the mNotification.
        mNotification = new Notification(
            R.drawable.stat_sample,  // the status icon
            notificationText, // the status text
            System.currentTimeMillis()  // the time stamp
        );

        // Sets up the Intent that starts AlarmActivity
        mContentIntent = PendingIntent.getActivity(
            this,  // Start the Activity in the current context
            0,   // not used
            new Intent(this, AlarmActivity.class),  // A new Intent for AlarmActivity
            0  // Use an existing activity instance if available
        );

        // Creates a new content view for the mNotification. The view appears when the user
        // shows the expanded status window.
        mNotification.setLatestEventInfo(
            this,  //  Put the content view in the current context
            getText(R.string.alarm_service_label),  // The text to use as the label of the entry
            notificationText,  // The text to use as the contents of the entry
            mContentIntent  // The intent to send when the entry is clicked
        );

        // Sets a unique ID for the notification and sends it to NotificationManager to be
        // displayed. The ID is the integer marker for the notification string, which is
        // guaranteed to be unique within the entire application.
        mNotificationManager.notify(
            R.string.alarm_service_started,  // unique id for the mNotification
            mNotification   // the mNotification object
        );
    }
}
