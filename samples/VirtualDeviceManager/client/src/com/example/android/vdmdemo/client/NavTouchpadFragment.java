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

package com.example.android.vdmdemo.client;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.android.vdmdemo.common.RemoteEventProto.InputDeviceType;

import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;

/** Fragment to show UI for a navigation touchpad. */
@AndroidEntryPoint(Fragment.class)
public final class NavTouchpadFragment extends Hilt_NavTouchpadFragment {

    @Inject InputManager mInputManager;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nav_touchpad, container, false);

        TextView navTouchpad = view.requireViewById(R.id.nav_touchpad);
        navTouchpad.setOnTouchListener(
                (v, event) -> {
                    mInputManager.sendInputEventToFocusedDisplay(
                            InputDeviceType.DEVICE_TYPE_NAVIGATION_TOUCHPAD, event);
                    return true;
                });
        return view;
    }
}
