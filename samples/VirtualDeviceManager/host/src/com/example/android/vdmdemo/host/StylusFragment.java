/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.graphics.PointF;
import android.hardware.input.VirtualStylusButtonEvent;
import android.hardware.input.VirtualStylusMotionEvent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import dagger.hilt.android.AndroidEntryPoint;

import java.util.Optional;

import javax.inject.Inject;


/** VDM Host Input fragment for stylus event injection. */
@AndroidEntryPoint(Fragment.class)
@SuppressLint("NewApi")
public final class StylusFragment extends Hilt_StylusFragment {

    @Inject
    InputController mInputController;

    private int mTiltX = 0;
    private int mTiltY = 0;
    private boolean mHover = true;
    private int mToolType = VirtualStylusMotionEvent.TOOL_TYPE_STYLUS;

    public StylusFragment() {
        super(R.layout.fragment_input_stylus);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        SwitchMaterial hoverSwitch = view.requireViewById(R.id.hover);
        hoverSwitch.setOnCheckedChangeListener((v, isChecked) -> mHover = isChecked);

        SwitchMaterial eraserSwitch = view.requireViewById(R.id.eraser);
        eraserSwitch.setOnCheckedChangeListener((v, isChecked) ->
                mToolType = isChecked
                        ? VirtualStylusMotionEvent.TOOL_TYPE_ERASER
                        : VirtualStylusMotionEvent.TOOL_TYPE_STYLUS);

        SwitchMaterial buttonPrimarySwitch = view.requireViewById(R.id.button_primary);
        buttonPrimarySwitch.setOnCheckedChangeListener((v, isChecked) ->
                sendButtonEvent(
                        isChecked
                                ? VirtualStylusButtonEvent.ACTION_BUTTON_PRESS
                                : VirtualStylusButtonEvent.ACTION_BUTTON_RELEASE,
                        VirtualStylusButtonEvent.BUTTON_PRIMARY));

        SwitchMaterial buttonSecondarySwitch = view.requireViewById(R.id.button_secondary);
        buttonSecondarySwitch.setOnCheckedChangeListener((v, isChecked) ->
                sendButtonEvent(
                        isChecked
                                ? VirtualStylusButtonEvent.ACTION_BUTTON_PRESS
                                : VirtualStylusButtonEvent.ACTION_BUTTON_RELEASE,
                        VirtualStylusButtonEvent.BUTTON_SECONDARY));

        Slider sliderX = view.requireViewById(R.id.tiltx);
        sliderX.addOnChangeListener((slider, value, user) -> mTiltX = (int) value);

        Slider sliderY = view.requireViewById(R.id.tilty);
        sliderY.addOnChangeListener((slider, value, user) -> mTiltY = (int) value);

        TextView touchpad = view.requireViewById(R.id.stylus);
        touchpad.setOnTouchListener(this::sendMotionEvent);
    }

    private boolean sendMotionEvent(View view, MotionEvent e) {
        int action = getVirtualStylusEventAction(e.getAction());
        Optional<PointF> displaySize = mInputController.getFocusedDisplaySize();
        if (action < 0 || !displaySize.isPresent()) {
            return false;
        }
        // Scale the coordinates w.r.t. the display
        int x = (int) (e.getX() / view.getWidth() * displaySize.get().x);
        int y = (int) (e.getY() / view.getHeight() * displaySize.get().y);
        VirtualStylusMotionEvent event = new VirtualStylusMotionEvent.Builder()
                .setPressure(mHover ? 0 : ((int) (e.getPressure() * 255)))
                .setX(x)
                .setY(y)
                .setTiltX(mTiltX)
                .setTiltY(mTiltY)
                .setToolType(mToolType)
                .setAction(action)
                .build();
        mInputController.sendStylusEventToFocusedDisplay(event);
        return true;
    }

    private void sendButtonEvent(int action, int button) {
        VirtualStylusButtonEvent event = new VirtualStylusButtonEvent.Builder()
                .setAction(action)
                .setButtonCode(button)
                .build();
        mInputController.sendStylusEventToFocusedDisplay(event);
    }

    private static int getVirtualStylusEventAction(int action) {
        return switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_DOWN ->
                    VirtualStylusMotionEvent.ACTION_DOWN;
            case MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP ->
                    VirtualStylusMotionEvent.ACTION_UP;
            case MotionEvent.ACTION_MOVE ->  VirtualStylusMotionEvent.ACTION_MOVE;
            default -> -1;
        };
    }
}
