/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.example.android.apis.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.example.android.apis.R;

/**
 * This is an example of implementing an application service that runs locally
 * in the same process as the application.  The {@link ServiceStartArgumentsController}
 * class shows how to interact with the service. 
 *
 * <p>Notice the use of the {@link NotificationManager} when interesting things
 * happen in the service.  This is generally how background services should
 * interact with the user, rather than doing something more disruptive such as
 * calling startActivity().
 */
public class ServiceStartArguments extends Service
{
    private NotificationManager mNM;
    private Intent mInvokeIntent;
    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        
        @Override
        public void handleMessage(Message msg)
        {
            Bundle arguments = (Bundle)msg.obj;
            String txt = getResources()
                    .getString(R.string.service_arguments_started);
            txt = txt + arguments.getString("name");
        
            Log.i("ServiceStartArguments", "Message: " + msg + ", " + txt);
        
            showNotification();
        
        // Normally we would do some work here...  for our sample, we will
        // just sleep for 10 seconds.
            long endTime = System.currentTimeMillis() + 5*1000;
            while (System.currentTimeMillis() < endTime) {
                synchronized (this) {
                    try {
                        wait(endTime - System.currentTimeMillis());
                    } catch (Exception e) {
                    }
                }
            }
        
            Log.i("ServiceStartArguments", "Done with #" + msg.arg1);
            stopSelf(msg.arg1);
        }

    };
    
    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // This is who should be launched if the user selects our persistent
        // notification.
        mInvokeIntent = new Intent(this, ServiceStartArgumentsController.class);

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.
        HandlerThread thread = new HandlerThread("ServiceStartArguments");
        thread.start();
        
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.i("ServiceStartArguments",
                "Starting #" + startId + ": " + intent.getExtras());
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent.getExtras();
        mServiceHandler.sendMessage(msg);
        Log.i("ServiceStartArguments", "Sending: " + msg);
    }

    @Override
    public void onDestroy() {
        mServiceLooper.quit();

        // Cancel the persistent notification.
        mNM.cancel(R.string.service_arguments_started);

        // Tell the user we stopped.
        Toast.makeText(ServiceStartArguments.this, R.string.service_arguments_stopped,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.service_arguments_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.stat_sample, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, AlarmService.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.service_start_arguments_label),
                       text, contentIntent);

        // Send the notification.
        // We use a string id because it is a unique number.  We use it later to cancel.
        mNM.notify(R.string.service_arguments_started, notification);
    }
}

