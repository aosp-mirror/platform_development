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

import android.app.Notification;
import android.content.Context;

/**
 * Base class for notification preset generators.
 */
public abstract class NotificationPreset extends NamedPreset {
    public final int titleResId;
    public final int textResId;

    public NotificationPreset(int nameResId, int titleResId, int textResId) {
        super(nameResId);
        this.titleResId = titleResId;
        this.textResId = textResId;
    }

    public static class BuildOptions {
        public final CharSequence titlePreset;
        public final CharSequence textPreset;
        public final PriorityPreset priorityPreset;
        public final ActionsPreset actionsPreset;
        public final boolean includeLargeIcon;
        public final boolean isLocalOnly;
        public final boolean hasContentIntent;
        public final boolean vibrate;
        public final Integer[] backgroundIds;

        public BuildOptions(CharSequence titlePreset, CharSequence textPreset,
                PriorityPreset priorityPreset, ActionsPreset actionsPreset,
                boolean includeLargeIcon, boolean isLocalOnly, boolean hasContentIntent,
                boolean vibrate, Integer[] backgroundIds) {
            this.titlePreset = titlePreset;
            this.textPreset = textPreset;
            this.priorityPreset = priorityPreset;
            this.actionsPreset = actionsPreset;
            this.includeLargeIcon = includeLargeIcon;
            this.isLocalOnly = isLocalOnly;
            this.hasContentIntent = hasContentIntent;
            this.vibrate = vibrate;
            this.backgroundIds = backgroundIds;
        }
    }

    /** Build a notification with this preset and the provided options */
    public abstract Notification[] buildNotifications(Context context, BuildOptions options);

    /** Whether actions are required to use this preset. */
    public boolean actionsRequired() {
        return false;
    }

    /** Number of background pickers required */
    public int countBackgroundPickersRequired() {
        return 0;
    }
}
