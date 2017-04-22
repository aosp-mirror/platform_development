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

package com.example.android.apis.app;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.app.job.JobWorkItem;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.example.android.apis.R;

/**
 * This is an example of implementing a {@link JobService} that dispatches work enqueued in
 * to it.  The {@link JobWorkServiceActivity} class shows how to interact with the service.
 */
//BEGIN_INCLUDE(service)
public class JobWorkService extends JobService {
    private NotificationManager mNM;
    private CommandProcessor mCurProcessor;

    /**
     * This is a task to dequeue and process work in the background.
     */
    final class CommandProcessor extends AsyncTask<Void, Void, Void> {
        private final JobParameters mParams;

        CommandProcessor(JobParameters params) {
            mParams = params;
        }

        @Override
        protected Void doInBackground(Void... params) {
            boolean cancelled;
            JobWorkItem work;

            /**
             * Iterate over available work.  Once dequeueWork() returns null, the
             * job's work queue is empty and the job has stopped, so we can let this
             * async task complete.
             */
            while (!(cancelled=isCancelled()) && (work=mParams.dequeueWork()) != null) {
                String txt = work.getIntent().getStringExtra("name");
                Log.i("JobWorkService", "Processing work: " + work + ", msg: " + txt);
                showNotification(txt);

                // Process work here...  we'll pretend by sleeping.
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }

                hideNotification();

                // Tell system we have finished processing the work.
                Log.i("JobWorkService", "Done with: " + work);
                mParams.completeWork(work);
            }

            if (cancelled) {
                Log.i("JobWorkService", "CANCELLED!");
            }

            return null;
        }
    }

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        Toast.makeText(this, R.string.service_created, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        hideNotification();
        Toast.makeText(this, R.string.service_destroyed, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        // Start task to pull work out of the queue and process it.
        mCurProcessor = new CommandProcessor(params);
        mCurProcessor.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        // Allow the job to continue running while we process work.
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        // Have the processor cancel its current work.
        mCurProcessor.cancel(true);

        // Tell the system to reschedule the job -- the only reason we would be here is
        // because the job needs to stop for some reason before it has completed all of
        // its work, so we would like it to remain to finish that work in the future.
        return true;
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification(String text) {
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, JobWorkServiceActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        Notification.Builder noteBuilder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.stat_sample)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.service_start_arguments_label))  // the label
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent);  // The intent to send when the entry is clicked

        // We show this for as long as our service is processing a command.
        noteBuilder.setOngoing(true);

        // Send the notification.
        // We use a string id because it is a unique number.  We use it later to cancel.
        mNM.notify(R.string.job_service_created, noteBuilder.build());
    }

    private void hideNotification() {
        mNM.cancel(R.string.service_created);
    }
}
//END_INCLUDE(service)
