/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.example.android.vdmdemo.host;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.companion.virtual.VirtualDeviceManager;
import android.companion.virtual.VirtualDeviceManager.VirtualDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.PointF;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.hardware.display.VirtualDisplayConfig;
import android.hardware.input.VirtualDpad;
import android.hardware.input.VirtualDpadConfig;
import android.hardware.input.VirtualKeyEvent;
import android.hardware.input.VirtualKeyboard;
import android.hardware.input.VirtualKeyboardConfig;
import android.hardware.input.VirtualMouse;
import android.hardware.input.VirtualMouseButtonEvent;
import android.hardware.input.VirtualMouseConfig;
import android.hardware.input.VirtualMouseRelativeEvent;
import android.hardware.input.VirtualMouseScrollEvent;
import android.hardware.input.VirtualNavigationTouchpad;
import android.hardware.input.VirtualNavigationTouchpadConfig;
import android.hardware.input.VirtualTouchEvent;
import android.hardware.input.VirtualTouchscreen;
import android.hardware.input.VirtualTouchscreenConfig;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;

import com.example.android.vdmdemo.common.RemoteEventProto.DisplayCapabilities;
import com.example.android.vdmdemo.common.RemoteEventProto.DisplayRotation;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteEvent;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteInputEvent;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteKeyEvent;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteMotionEvent;
import com.example.android.vdmdemo.common.RemoteEventProto.StopStreaming;
import com.example.android.vdmdemo.common.RemoteIo;
import com.example.android.vdmdemo.common.VideoManager;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

class RemoteDisplay implements AutoCloseable {

    private static final String TAG = "VdmHost";

    private static final int DISPLAY_FPS = 60;

    private static final int DEFAULT_VIRTUAL_DISPLAY_FLAGS =
            DisplayManager.VIRTUAL_DISPLAY_FLAG_TRUSTED
                    | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
                    | DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY;

    private final Context context;
    private final RemoteIo remoteIo;
    private final Settings settings;
    private final Consumer<RemoteEvent> remoteEventConsumer = this::processRemoteEvent;
    private final VirtualDisplay virtualDisplay;
    private final VirtualDpad dpad;
    private final int remoteDisplayId;
    private final Executor pendingIntentExecutor;
    private final VirtualDevice virtualDevice;
    private final boolean supportsHome;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private int rotation;
    private int width;
    private int height;
    private int dpi;

    private VideoManager videoManager;
    private VirtualTouchscreen touchscreen;
    private VirtualMouse mouse;
    private VirtualNavigationTouchpad navigationTouchpad;
    private VirtualKeyboard keyboard;

    @SuppressLint("WrongConstant")
    public RemoteDisplay(
            Context context,
            RemoteEvent event,
            VirtualDevice virtualDevice,
            RemoteIo remoteIo,
            boolean supportsHome,
            boolean supportsMirroring,
            Settings settings) {
        this.context = context;
        this.remoteIo = remoteIo;
        this.remoteDisplayId = event.getDisplayId();
        this.virtualDevice = virtualDevice;
        this.pendingIntentExecutor = context.getMainExecutor();
        this.supportsHome = supportsHome;
        this.settings = settings;

        setCapabilities(event.getDisplayCapabilities());

        int flags = DEFAULT_VIRTUAL_DISPLAY_FLAGS;
        if (settings.displayRotationEnabled) {
            flags |= DisplayManager.VIRTUAL_DISPLAY_FLAG_ROTATES_WITH_CONTENT;
        }
        if (supportsMirroring) {
            flags &= ~DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY;
        }
        virtualDisplay =
                virtualDevice.createVirtualDisplay(
                        new VirtualDisplayConfig.Builder(
                                        "VirtualDisplay" + remoteDisplayId, width, height, dpi)
                                .setFlags(flags)
                                .setHomeSupported(supportsHome)
                                .build(),
                        /* executor= */ Runnable::run,
                        /* callback= */ null);

        dpad =
                virtualDevice.createVirtualDpad(
                        new VirtualDpadConfig.Builder()
                                .setAssociatedDisplayId(virtualDisplay.getDisplay().getDisplayId())
                                .setInputDeviceName("vdmdemo-dpad" + remoteDisplayId)
                                .build());

        remoteIo.addMessageConsumer(remoteEventConsumer);

        reset();
    }

    void reset(DisplayCapabilities capabilities) {
        setCapabilities(capabilities);
        virtualDisplay.resize(width, height, dpi);
        reset();
    }

    private void reset() {
        if (videoManager != null) {
            videoManager.stop();
        }
        videoManager =
                VideoManager.createEncoder(remoteDisplayId, remoteIo, settings.recordEncoderOutput);
        Surface surface = videoManager.createInputSurface(width, height, DISPLAY_FPS);
        virtualDisplay.setSurface(surface);

        rotation = virtualDisplay.getDisplay().getRotation();

        if (touchscreen != null) {
            touchscreen.close();
        }
        touchscreen =
                virtualDevice.createVirtualTouchscreen(
                        new VirtualTouchscreenConfig.Builder(width, height)
                                .setAssociatedDisplayId(virtualDisplay.getDisplay().getDisplayId())
                                .setInputDeviceName("vdmdemo-touchscreen" + remoteDisplayId)
                                .build());

        videoManager.startEncoding();
    }

    private void setCapabilities(DisplayCapabilities capabilities) {
        width = capabilities.getViewportWidth();
        height = capabilities.getViewportHeight();
        dpi = capabilities.getDensityDpi();

        // Video encoder needs round dimensions...
        height -= height % 10;
        width -= width % 10;
    }

    public void launchIntent(PendingIntent intent) {
        virtualDevice.launchPendingIntent(
                virtualDisplay.getDisplay().getDisplayId(),
                intent,
                pendingIntentExecutor,
                (result) -> {
                    switch (result) {
                        case VirtualDeviceManager.LAUNCH_SUCCESS:
                            Log.i(
                                    TAG,
                                    "launchAppOnDisplay: Launched app successfully on display "
                                            + virtualDisplay.getDisplay().getDisplayId());
                            break;
                        case VirtualDeviceManager.LAUNCH_FAILURE_NO_ACTIVITY:
                        case VirtualDeviceManager.LAUNCH_FAILURE_PENDING_INTENT_CANCELED:
                            Log.w(
                                    TAG,
                                    "launchAppOnDisplay: Launching app failed with reason: "
                                            + result);
                            break;
                        default:
                            Log.w(
                                    TAG,
                                    "launchAppOnDisplay: Unexpected result when launching app: "
                                            + result);
                    }
                });
    }

    public int getRemoteDisplayId() {
        return remoteDisplayId;
    }

    public int getDisplayId() {
        return virtualDisplay.getDisplay().getDisplayId();
    }

    public void onDisplayChanged() {
        if (rotation != virtualDisplay.getDisplay().getRotation()) {
            rotation = virtualDisplay.getDisplay().getRotation();
            int rotationDegrees = displayRotationToDegrees(rotation);
            Log.v(TAG, "Notify client for rotation event: " + rotationDegrees);
            remoteIo.sendMessage(
                    RemoteEvent.newBuilder()
                            .setDisplayId(getRemoteDisplayId())
                            .setDisplayRotation(
                                    DisplayRotation.newBuilder()
                                            .setRotationDegrees(rotationDegrees))
                            .build());
        }
    }

    @SuppressWarnings("PendingIntentMutability")
    private void processRemoteEvent(RemoteEvent event) {
        if (event.getDisplayId() != remoteDisplayId) {
            return;
        }
        if (event.hasHomeEvent() && supportsHome) {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(
                    homeIntent,
                    ActivityOptions.makeBasic().setLaunchDisplayId(getDisplayId()).toBundle());
        } else if (event.hasInputEvent()) {
            processInputEvent(event.getInputEvent());
        } else if (event.hasStopStreaming() && event.getStopStreaming().getPause()) {
            if (videoManager != null) {
                videoManager.stop();
                videoManager = null;
            }
        }
    }

    private void processInputEvent(RemoteInputEvent inputEvent) {
        switch (inputEvent.getDeviceType()) {
            case DEVICE_TYPE_NONE:
                Log.e(TAG, "Received no input device type");
                break;
            case DEVICE_TYPE_DPAD:
                dpad.sendKeyEvent(remoteEventToVirtualKeyEvent(inputEvent));
                break;
            case DEVICE_TYPE_NAVIGATION_TOUCHPAD:
                if (navigationTouchpad == null) {
                    // Any arbitrarily big enough nav touchpad would work.
                    Point displaySize = new Point(5000, 5000);
                    navigationTouchpad =
                            virtualDevice.createVirtualNavigationTouchpad(
                                    new VirtualNavigationTouchpadConfig.Builder(
                                                    displaySize.x, displaySize.y)
                                            .setAssociatedDisplayId(getDisplayId())
                                            .setInputDeviceName(
                                                    "vdmdemo-navtouchpad" + remoteDisplayId)
                                            .build());
                }
                navigationTouchpad.sendTouchEvent(remoteEventToVirtualTouchEvent(inputEvent));
                break;
            case DEVICE_TYPE_MOUSE:
                processMouseEvent(inputEvent);
                break;
            case DEVICE_TYPE_TOUCHSCREEN:
                touchscreen.sendTouchEvent(remoteEventToVirtualTouchEvent(inputEvent));
                break;
            case DEVICE_TYPE_KEYBOARD:
                if (keyboard == null) {
                    keyboard =
                            virtualDevice.createVirtualKeyboard(
                                    new VirtualKeyboardConfig.Builder()
                                            .setInputDeviceName(
                                                    "vdmdemo-keyboard" + remoteDisplayId)
                                            .setAssociatedDisplayId(getDisplayId())
                                            .build());
                }
                keyboard.sendKeyEvent(remoteEventToVirtualKeyEvent(inputEvent));
                break;
            default:
                Log.e(
                        TAG,
                        "processInputEvent got an invalid input device type: "
                                + inputEvent.getDeviceType().getNumber());
                break;
        }
    }

    private void processMouseEvent(RemoteInputEvent inputEvent) {
        if (mouse == null) {
            mouse =
                    virtualDevice.createVirtualMouse(
                            new VirtualMouseConfig.Builder()
                                    .setAssociatedDisplayId(getDisplayId())
                                    .setInputDeviceName("vdmdemo-mouse" + remoteDisplayId)
                                    .build());
        }
        if (inputEvent.hasMouseButtonEvent()) {
            mouse.sendButtonEvent(
                    new VirtualMouseButtonEvent.Builder()
                            .setButtonCode(inputEvent.getMouseButtonEvent().getKeyCode())
                            .setAction(inputEvent.getMouseButtonEvent().getAction())
                            .build());
        } else if (inputEvent.hasMouseScrollEvent()) {
            mouse.sendScrollEvent(
                    new VirtualMouseScrollEvent.Builder()
                            .setXAxisMovement(inputEvent.getMouseScrollEvent().getX())
                            .setYAxisMovement(inputEvent.getMouseScrollEvent().getY())
                            .build());
        } else if (inputEvent.hasMouseRelativeEvent()) {
            PointF cursorPosition = mouse.getCursorPosition();
            mouse.sendRelativeEvent(
                    new VirtualMouseRelativeEvent.Builder()
                            .setRelativeX(
                                    inputEvent.getMouseRelativeEvent().getX() - cursorPosition.x)
                            .setRelativeY(
                                    inputEvent.getMouseRelativeEvent().getY() - cursorPosition.y)
                            .build());
        } else {
            Log.e(TAG, "Received an invalid mouse event");
        }
    }

    private static int getVirtualTouchEventAction(int action) {
        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
                return VirtualTouchEvent.ACTION_DOWN;
            case MotionEvent.ACTION_POINTER_UP:
                return VirtualTouchEvent.ACTION_UP;
            default:
                return action;
        }
    }

    private static int getVirtualTouchEventToolType(int action) {
        switch (action) {
            case MotionEvent.ACTION_CANCEL:
                return VirtualTouchEvent.TOOL_TYPE_PALM;
            default:
                return VirtualTouchEvent.TOOL_TYPE_FINGER;
        }
    }

    // Surface rotation is in opposite direction to display rotation.
    // See https://developer.android.com/reference/android/view/Display?hl=en#getRotation()
    private static int displayRotationToDegrees(int displayRotation) {
        switch (displayRotation) {
            case Surface.ROTATION_90:
                return -90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 90;
            case Surface.ROTATION_0:
            default:
                return 0;
        }
    }

    private static VirtualKeyEvent remoteEventToVirtualKeyEvent(RemoteInputEvent event) {
        RemoteKeyEvent keyEvent = event.getKeyEvent();
        return new VirtualKeyEvent.Builder()
                .setEventTimeNanos((long) (event.getTimestampMs() * 1e6))
                .setKeyCode(keyEvent.getKeyCode())
                .setAction(keyEvent.getAction())
                .build();
    }

    private static VirtualTouchEvent remoteEventToVirtualTouchEvent(RemoteInputEvent event) {
        RemoteMotionEvent motionEvent = event.getTouchEvent();
        return new VirtualTouchEvent.Builder()
                .setEventTimeNanos((long) (event.getTimestampMs() * 1e6))
                .setPointerId(motionEvent.getPointerId())
                .setAction(getVirtualTouchEventAction(motionEvent.getAction()))
                .setPressure(motionEvent.getPressure() * 255f)
                .setToolType(getVirtualTouchEventToolType(motionEvent.getAction()))
                .setX(motionEvent.getX())
                .setY(motionEvent.getY())
                .build();
    }

    @Override
    public void close() {
        close(false);
    }

    void close(boolean pause) {
        if (closed.getAndSet(true)) { // Prevent double closure.
            return;
        }
        remoteIo.sendMessage(
                RemoteEvent.newBuilder()
                        .setDisplayId(getRemoteDisplayId())
                        .setStopStreaming(StopStreaming.newBuilder().setPause(pause))
                        .build());
        remoteIo.removeMessageConsumer(remoteEventConsumer);
        dpad.close();
        touchscreen.close();
        if (mouse != null) {
            mouse.close();
        }
        if (navigationTouchpad != null) {
            navigationTouchpad.close();
        }
        if (keyboard != null) {
            keyboard.close();
        }
        virtualDisplay.release();
        if (videoManager != null) {
            videoManager.stop();
            videoManager = null;
        }
    }
}
