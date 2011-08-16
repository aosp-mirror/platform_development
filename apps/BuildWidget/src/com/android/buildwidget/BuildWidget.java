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

package com.android.buildwidget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.IBinder;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Define a simple widget that shows the Wiktionary "Word of the day." To build
 * an update we spawn a background {@link Service} to perform the API queries.
 */
public class BuildWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        // To prevent any ANR timeouts, we perform the update in a service
        context.startService(new Intent(context, UpdateService.class));
    }

    public static class UpdateService extends Service {
        @Override
        public void onStart(Intent intent, int startId) {
            // Build the widget update
            RemoteViews updateViews = buildUpdate(this);

            // Push update for this widget to the home screen
            ComponentName thisWidget = new ComponentName(this, BuildWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(thisWidget, updateViews);
        }

        public RemoteViews buildUpdate(Context context) {
            // Pick out month names from resources
            Resources res = context.getResources();
            RemoteViews updateViews = new RemoteViews(
                context.getPackageName(), R.layout.widget);

            PendingIntent pendingIntent = PendingIntent.getActivity(context,
                    0 /* no requestCode */, 
                    new Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS),
                    0 /* no flags */);
            updateViews.setOnClickPendingIntent(R.id.widget, pendingIntent);

            updateViews.setTextViewText(R.id.build_info, android.os.Build.ID);
            updateViews.setTextViewText(R.id.build_date, 
                    DateUtils.formatDateTime(context, android.os.Build.TIME, 
                        DateUtils.FORMAT_NUMERIC_DATE));
            updateViews.setTextViewText(R.id.build_extra, android.os.Build.FINGERPRINT);
            return updateViews;
        }

        @Override
        public IBinder onBind(Intent intent) {
            // We don't need to bind to this service
            return null;
        }
    }
}
