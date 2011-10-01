package com.example.android.musicplayer;

import android.content.ComponentName;
import android.media.AudioManager;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Class that assists with handling new media button APIs available in API level 8.
 */
public class MediaButtonHelper {
    // Backwards compatibility code (methods available as of API Level 8)
    private static final String TAG = "MediaButtonHelper";

    static {
        initializeStaticCompatMethods();
    }

    static Method sMethodRegisterMediaButtonEventReceiver;
    static Method sMethodUnregisterMediaButtonEventReceiver;

    static void initializeStaticCompatMethods() {
        try {
            sMethodRegisterMediaButtonEventReceiver = AudioManager.class.getMethod(
                    "registerMediaButtonEventReceiver",
                    new Class[] { ComponentName.class });
            sMethodUnregisterMediaButtonEventReceiver = AudioManager.class.getMethod(
                    "unregisterMediaButtonEventReceiver",
                    new Class[] { ComponentName.class });
        } catch (NoSuchMethodException e) {
            // Silently fail when running on an OS before API level 8.
        }
    }

    public static void registerMediaButtonEventReceiverCompat(AudioManager audioManager,
            ComponentName receiver) {
        if (sMethodRegisterMediaButtonEventReceiver == null)
            return;

        try {
            sMethodRegisterMediaButtonEventReceiver.invoke(audioManager, receiver);
        } catch (InvocationTargetException e) {
            // Unpack original exception when possible
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                // Unexpected checked exception; wrap and re-throw
                throw new RuntimeException(e);
            }
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException invoking registerMediaButtonEventReceiver.");
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    public static void unregisterMediaButtonEventReceiverCompat(AudioManager audioManager,
            ComponentName receiver) {
        if (sMethodUnregisterMediaButtonEventReceiver == null)
            return;

        try {
            sMethodUnregisterMediaButtonEventReceiver.invoke(audioManager, receiver);
        } catch (InvocationTargetException e) {
            // Unpack original exception when possible
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                // Unexpected checked exception; wrap and re-throw
                throw new RuntimeException(e);
            }
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException invoking unregisterMediaButtonEventReceiver.");
            e.printStackTrace();
        }
    }
}
