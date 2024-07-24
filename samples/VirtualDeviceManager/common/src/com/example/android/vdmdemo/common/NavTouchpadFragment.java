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

package com.example.android.vdmdemo.common;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.fragment.app.Fragment;

import dagger.hilt.android.AndroidEntryPoint;

import java.util.function.Consumer;

/** Fragment to show UI for a navigation touchpad. */
@AndroidEntryPoint(Fragment.class)
public final class NavTouchpadFragment extends Hilt_NavTouchpadFragment {

    private Consumer<MotionEvent> mInputEventListener;

    public NavTouchpadFragment() {
        super(R.layout.nav_touchpad_fragment);
    }

    public void setInputEventListener(Consumer<MotionEvent> listener) {
        mInputEventListener = listener;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        view.setOnTouchListener(
                (v, event) -> {
                    if (mInputEventListener != null) {
                        mInputEventListener.accept(event);
                    }
                    return true;
                });
    }
}
