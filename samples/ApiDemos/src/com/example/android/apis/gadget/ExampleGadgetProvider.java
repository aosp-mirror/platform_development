/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.example.android.apis.gadget;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.gadget.GadgetManager;
import android.gadget.GadgetProvider;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.ArrayList;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.
import com.example.android.apis.R;

/**
 * A gadget provider.  We have a string that we pull from a preference in order to show
 * the configuration settings and the current time when the gadget was updated.  We also
 * register a BroadcastReceiver for time-changed and timezone-changed broadcasts, and
 * update then too.
 *
 * <p>See also the following files:
 * <ul>
 *   <li>ExampleGadgetConfigure.java</li>
 *   <li>ExampleBroadcastReceiver.java</li>
 *   <li>res/layout/gadget_configure.xml</li>
 *   <li>res/layout/gadget_provider.xml</li>
 *   <li>res/xml/gadget_provider.xml</li>
 * </ul>
 */
public class ExampleGadgetProvider extends GadgetProvider {
    // log tag
    private static final String TAG = "ExampleGadgetProvider";

    public void onUpdate(Context context, GadgetManager gadgetManager, int[] gadgetIds) {
        Log.d(TAG, "onUpdate");
        // For each gadget that needs an update, get the text that we should display:
        //   - Create a RemoteViews object for it
        //   - Set the text in the RemoteViews object
        //   - Tell the GadgetManager to show that views object for the gadget.
        final int N = gadgetIds.length;
        for (int i=0; i<N; i++) {
            int gadgetId = gadgetIds[i];
            String titlePrefix = ExampleGadgetConfigure.loadTitlePref(context, gadgetId);
            updateGadget(context, gadgetManager, gadgetId, titlePrefix);
        }
    }
    
    public void onDeleted(Context context, int[] gadgetIds) {
        Log.d(TAG, "onDeleted");
        // When the user deletes the gadget, delete the preference associated with it.
        final int N = gadgetIds.length;
        for (int i=0; i<N; i++) {
            ExampleGadgetConfigure.deleteTitlePref(context, gadgetIds[i]);
        }
    }

    public void onEnabled(Context context) {
        Log.d(TAG, "onEnabled");
        // When the first gadget is created, register for the TIMEZONE_CHANGED and TIME_CHANGED
        // broadcasts.  We don't want to be listening for these if nobody has our gadget active.
        // This setting is sticky across reboots, but that doesn't matter, because this will
        // be called after boot if there is a gadget instance for this provider.
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(
                new ComponentName("com.example.android.apis", ".gadget.ExampleBroadcastReceiver"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    public void onDisabled(Context context) {
        // When the first gadget is created, stop listening for the TIMEZONE_CHANGED and
        // TIME_CHANGED broadcasts.
        Log.d(TAG, "onDisabled");
        Class clazz = ExampleBroadcastReceiver.class;
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(
                new ComponentName("com.example.android.apis", ".gadget.ExampleBroadcastReceiver"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    static void updateGadget(Context context, GadgetManager gadgetManager,
            int gadgetId, String titlePrefix) {
        Log.d(TAG, "updateGadget gadgetId=" + gadgetId + " titlePrefix=" + titlePrefix);
        // Getting the string this way allows the string to be localized.  The format
        // string is filled in using java.util.Formatter-style format strings.
        CharSequence text = context.getString(R.string.gadget_text_format,
                ExampleGadgetConfigure.loadTitlePref(context, gadgetId),
                "0x" + Long.toHexString(SystemClock.elapsedRealtime()));

        // Construct the RemoteViews object.  It takes the package name (in our case, it's our
        // package, but it needs this because on the other side it's the gadget host inflating
        // the layout from our package).
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.gadget_provider);
        views.setTextViewText(R.id.gadget_text, text);

        // Tell the gadget manager
        gadgetManager.updateGadget(gadgetId, views);
    }
}


