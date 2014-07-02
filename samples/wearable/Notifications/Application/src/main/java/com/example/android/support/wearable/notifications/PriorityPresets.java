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
import android.support.v4.app.NotificationCompat;

/**
 * Collection of notification priority presets.
 */
public class PriorityPresets {
    public static final PriorityPreset DEFAULT = new SimplePriorityPreset(
            R.string.default_priority, Notification.PRIORITY_DEFAULT);

    public static final PriorityPreset[] PRESETS = new PriorityPreset[] {
            new SimplePriorityPreset(R.string.min_priority, Notification.PRIORITY_MIN),
            new SimplePriorityPreset(R.string.low_priority, Notification.PRIORITY_LOW),
            DEFAULT,
            new SimplePriorityPreset(R.string.high_priority, Notification.PRIORITY_HIGH),
            new SimplePriorityPreset(R.string.max_priority, Notification.PRIORITY_MAX)
    };

    /**
     * Simple notification priority preset that sets a priority using
     * {@link android.support.v4.app.NotificationCompat.Builder#setPriority}
     */
    private static class SimplePriorityPreset extends PriorityPreset {
        private final int mPriority;

        public SimplePriorityPreset(int nameResId, int priority) {
            super(nameResId);
            mPriority = priority;
        }

        @Override
        public void apply(NotificationCompat.Builder builder,
                NotificationCompat.WearableExtender wearableOptions) {
            builder.setPriority(mPriority);
        }
    }
}
