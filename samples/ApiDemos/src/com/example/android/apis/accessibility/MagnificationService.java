/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.apis.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityService.MagnificationController.OnMagnificationChangedListener;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.graphics.Region;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * This class is an {@link AccessibilityService} that controls the state of
 * display magnification in response to key events. It demonstrates the
 * following key features of the Android accessibility APIs:
 * <ol>
 *   <li>Basic implementation of an AccessibilityService
 *   <li>Observing and respond to user-generated key events
 *   <li>Querying and modifying the state of display magnification
 * </ol>
 */
public class MagnificationService extends AccessibilityService {
    private static final String LOG_TAG = "MagnificationService";

    /**
     * Callback for {@link android.view.accessibility.AccessibilityEvent}s.
     *
     * @param event An event.
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // No events required for this service.
    }

    /**
     * Callback for interrupting the accessibility feedback.
     */
    @Override
    public void onInterrupt() {
        // No interruptible actions taken by this service.
    }

    /**
     * Callback that allows an accessibility service to observe the key events
     * before they are passed to the rest of the system. This means that the events
     * are first delivered here before they are passed to the device policy, the
     * input method, or applications.
     * <p>
     * <strong>Note:</strong> It is important that key events are handled in such
     * a way that the event stream that would be passed to the rest of the system
     * is well-formed. For example, handling the down event but not the up event
     * and vice versa would generate an inconsistent event stream.
     * </p>
     * <p>
     * <strong>Note:</strong> The key events delivered in this method are copies
     * and modifying them will have no effect on the events that will be passed
     * to the system. This method is intended to perform purely filtering
     * functionality.
     * <p>
     *
     * @param event The event to be processed.
     * @return If true then the event will be consumed and not delivered to
     *         applications, otherwise it will be delivered as usual.
     */
    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        // Only consume volume key events.
        final int keyCode = event.getKeyCode();
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP
                && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false;
        }

        // Handle the event when the user releases the volume key. To prevent
        // the keys from actually adjusting the device volume, we'll ignore
        // the result of handleVolumeKey() and always return true to consume
        // the events.
        final int action = event.getAction();
        if (action == KeyEvent.ACTION_UP) {
            handleVolumeKey(keyCode == KeyEvent.KEYCODE_VOLUME_UP);
        }

        // Consume all volume key events.
        return true;
    }

    /**
     * Adjusts the magnification scale in response to volume key actions.
     *
     * @param isVolumeUp {@code true} if the volume up key was pressed or
     *                   {@code false} if the volume down key was pressed
     * @return {@code true} if the magnification scale changed as a result of
     *         the key
     */
    private boolean handleVolumeKey(boolean isVolumeUp) {
        // Obtain the controller on-demand, which allows us to avoid
        // dependencies on the accessibility service's lifecycle.
        final MagnificationController controller = getMagnificationController();

        // Adjust the current scale based on which volume key was pressed,
        // constraining the scale between 1x and 5x.
        final float currScale = controller.getScale();
        final float increment = isVolumeUp ? 0.1f : -0.1f;
        final float nextScale = Math.max(1f, Math.min(5f, currScale + increment));
        if (nextScale == currScale) {
            return false;
        }

        // Set the pivot, then scale around it.
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        controller.setScale(nextScale, true /* animate */);
        controller.setCenter(metrics.widthPixels / 2f, metrics.heightPixels / 2f, true);
        return true;
    }

    /**
     * This method is a part of the {@link AccessibilityService} lifecycle and is
     * called after the system has successfully bound to the service. If is
     * convenient to use this method for setting the {@link AccessibilityServiceInfo}.
     *
     * @see AccessibilityServiceInfo
     * @see #setServiceInfo(AccessibilityServiceInfo)
     */
    @Override
    public void onServiceConnected() {
        final AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) {
            // If we fail to obtain the service info, the service is not really
            // connected and we should avoid setting anything up.
            return;
        }

        // We declared our intent to request key filtering in the meta-data
        // attached to our service in the manifest. Now, we can explicitly
        // turn on key filtering when needed.
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        setServiceInfo(info);

        // Set up a listener for changes in the state of magnification.
        getMagnificationController().addListener(new OnMagnificationChangedListener() {
            @Override
            public void onMagnificationChanged(MagnificationController controller,
                    Region region, float scale, float centerX, float centerY) {
                Log.e(LOG_TAG, "Magnification scale is now " + scale);
            }
        });
    }
}
