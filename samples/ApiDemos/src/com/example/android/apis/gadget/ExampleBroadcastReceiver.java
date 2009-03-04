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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
 * A BroadcastReceiver that listens for updates for the ExampleGadgetProvider.  This
 * BroadcastReceiver starts off disabled, and we only enable it when there is a gadget
 * instance created, in order to only receive notifications when we need them.
 */
public class ExampleBroadcastReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        Log.d("ExmampleBroadcastReceiver", "intent=" + intent);

        // For our example, we'll also update all of the gadgets when the timezone
        // changes, or the user or network sets the time.
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_TIMEZONE_CHANGED)
                || action.equals(Intent.ACTION_TIME_CHANGED)) {
            GadgetManager gm = GadgetManager.getInstance(context);
            ArrayList<Integer> gadgetIds = new ArrayList();
            ArrayList<String> texts = new ArrayList();

            ExampleGadgetConfigure.loadAllTitlePrefs(context, gadgetIds, texts);

            final int N = gadgetIds.size();
            for (int i=0; i<N; i++) {
                ExampleGadgetProvider.updateGadget(context, gm, gadgetIds.get(i), texts.get(i));
            }
        }
    }

}
