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

import com.example.android.apis.R;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class IncomingMessage extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.incoming_message);

        Button button = (Button) findViewById(R.id.notify);
        button.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    showToast();
                    showNotification();
                }
            });
    }

    /**
     * The toast pops up a quick message to the user showing what could be
     * the text of an incoming message.  It uses a custom view to do so.
     */
    protected void showToast() {
        // create the view
        View view = inflateView(R.layout.incoming_message_panel);

        // set the text in the view
        TextView tv = (TextView)view.findViewById(R.id.message);
        tv.setText("khtx. meet u for dinner. cul8r");

        // show the toast
        Toast toast = new Toast(this);
        toast.setView(view);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.show();
    }

    private View inflateView(int resource) {
        LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return vi.inflate(resource, null);
    }

    /**
     * The notification is the icon and associated expanded entry in the
     * status bar.
     */
    protected void showNotification() {
        // look up the notification manager service
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // The details of our fake message
        CharSequence from = "Joe";
        CharSequence message = "kthx. meet u for dinner. cul8r";

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, IncomingMessageView.class), 0);

        // The ticker text, this uses a formatted string so our message could be localized
        String tickerText = getString(R.string.imcoming_message_ticker_text, message);

        // construct the Notification object.
        Notification notif = new Notification(R.drawable.stat_sample, tickerText,
                System.currentTimeMillis());

        // Set the info for the views that show in the notification panel.
        notif.setLatestEventInfo(this, from, message, contentIntent);

        // after a 100ms delay, vibrate for 250ms, pause for 100 ms and
        // then vibrate for 500ms.
        notif.vibrate = new long[] { 100, 250, 100, 500};

        // Note that we use R.layout.incoming_message_panel as the ID for
        // the notification.  It could be any integer you want, but we use
        // the convention of using a resource id for a string related to
        // the notification.  It will always be a unique number within your
        // application.
        nm.notify(R.string.imcoming_message_ticker_text, notif);
    }
}
