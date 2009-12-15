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

package com.example.android.wiktionary;

import com.example.android.wiktionary.SimpleWikiHelper.ApiException;
import com.example.android.wiktionary.SimpleWikiHelper.ParseException;

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
import android.text.format.Time;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Define a simple widget that shows the Wiktionary "Word of the day." To build
 * an update we spawn a background {@link Service} to perform the API queries.
 */
public class WordWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // To prevent any ANR timeouts, we perform the update in a service
        context.startService(new Intent(context, UpdateService.class));
    }

    public static class UpdateService extends Service {
        @Override
        public void onStart(Intent intent, int startId) {
            // Build the widget update for today
            RemoteViews updateViews = buildUpdate(this);

            // Push update for this widget to the home screen
            ComponentName thisWidget = new ComponentName(this, WordWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(thisWidget, updateViews);
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        /**
         * Regular expression that splits "Word of the day" entry into word
         * name, word type, and the first description bullet point.
         */
        private static final String WOTD_PATTERN =
            "(?s)\\{\\{wotd\\|(.+?)\\|(.+?)\\|([^#\\|]+).*?\\}\\}";

        /**
         * Build a widget update to show the current Wiktionary
         * "Word of the day." Will block until the online API returns.
         */
        public RemoteViews buildUpdate(Context context) {
            // Pick out month names from resources
            Resources res = context.getResources();
            String[] monthNames = res.getStringArray(R.array.month_names);

            // Find current month and day
            Time today = new Time();
            today.setToNow();

            // Build the page title for today, such as "March 21"
            String pageName = res.getString(R.string.template_wotd_title,
                    monthNames[today.month], today.monthDay);
            String pageContent = null;

            try {
                // Try querying the Wiktionary API for today's word
                SimpleWikiHelper.prepareUserAgent(context);
                pageContent = SimpleWikiHelper.getPageContent(pageName, false);
            } catch (ApiException e) {
                Log.e("WordWidget", "Couldn't contact API", e);
            } catch (ParseException e) {
                Log.e("WordWidget", "Couldn't parse API response", e);
            }

            RemoteViews views = null;
            Matcher matcher = Pattern.compile(WOTD_PATTERN).matcher(pageContent);
            if (matcher.find()) {
                // Build an update that holds the updated widget contents
                views = new RemoteViews(context.getPackageName(), R.layout.widget_word);

                String wordTitle = matcher.group(1);
                views.setTextViewText(R.id.word_title, wordTitle);
                views.setTextViewText(R.id.word_type, matcher.group(2));
                views.setTextViewText(R.id.definition, matcher.group(3).trim());

                // When user clicks on widget, launch to Wiktionary definition page
                String definePage = String.format("%s://%s/%s", ExtendedWikiHelper.WIKI_AUTHORITY,
                        ExtendedWikiHelper.WIKI_LOOKUP_HOST, wordTitle);
                Intent defineIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(definePage));
                PendingIntent pendingIntent = PendingIntent.getActivity(context,
                        0 /* no requestCode */, defineIntent, 0 /* no flags */);
                views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            } else {
                // Didn't find word of day, so show error message
                views = new RemoteViews(context.getPackageName(), R.layout.widget_message);
                views.setTextViewText(R.id.message, context.getString(R.string.widget_error));
            }
            return views;
        }
    }
}
