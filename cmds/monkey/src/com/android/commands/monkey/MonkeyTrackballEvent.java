/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.commands.monkey;

import android.os.RemoteException;
import android.view.IWindowManager;
import android.view.InputDevice;
import android.view.MotionEvent;


/**
 * monkey trackball event
 */
public class MonkeyTrackballEvent extends MonkeyMotionEvent {
    public MonkeyTrackballEvent(int action) {
        super(MonkeyEvent.EVENT_TYPE_TRACKBALL, InputDevice.SOURCE_TRACKBALL, action);
    }

    @Override
    protected String getTypeLabel() {
        return "Trackball";
    }

    @Override
    protected boolean injectMotionEvent(IWindowManager iwm, MotionEvent me)
            throws RemoteException {
        return iwm.injectTrackballEvent(me, false);
    }
}
