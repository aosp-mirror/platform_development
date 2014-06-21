package com.example.android.support.wearable.notifications;

/**
 * Base class for presets that have a simple name to display.
 */
public abstract class NamedPreset {
    public final int nameResId;

    public NamedPreset(int nameResId) {
        this.nameResId = nameResId;
    }
}
