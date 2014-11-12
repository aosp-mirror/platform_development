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

package com.example.android.support.wearable.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class NotificationUtil {
    public static final String EXTRA_MESSAGE =
            "com.example.android.support.wearable.notifications.MESSAGE";
    public static final String EXTRA_REPLY =
            "com.example.android.support.wearable.notifications.REPLY";

    public static PendingIntent getExamplePendingIntent(Context context, int messageResId) {
        Intent intent = new Intent(NotificationIntentReceiver.ACTION_EXAMPLE)
                .setClass(context, NotificationIntentReceiver.class);
        intent.putExtra(EXTRA_MESSAGE, context.getString(messageResId));
        return PendingIntent.getBroadcast(context, messageResId /* requestCode */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
