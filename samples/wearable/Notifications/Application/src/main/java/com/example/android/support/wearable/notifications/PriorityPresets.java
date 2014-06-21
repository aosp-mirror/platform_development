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
