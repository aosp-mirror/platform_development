/*
 * Copyright (C) 2009 The Android Open Source Project
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

import java.util.List;

import android.app.IActivityManager;
import android.view.IWindowManager;


/**
 * monkey throttle event
 */
public class MonkeyThrottleEvent extends MonkeyEvent {
    private long mThrottle; 
        
    public MonkeyThrottleEvent(long throttle) {
        super(MonkeyEvent.EVENT_TYPE_THROTTLE);
        mThrottle = throttle;
    }  

    @Override
    public int injectEvent(IWindowManager iwm, IActivityManager iam, int verbose) {

        if (verbose > 1) {
            System.out.println("Sleeping for " + mThrottle + " milliseconds");
        }
        try {
            Thread.sleep(mThrottle);
        } catch (InterruptedException e1) {
            System.out.println("** Monkey interrupted in sleep.");
            return MonkeyEvent.INJECT_FAIL;
        }
        
        return MonkeyEvent.INJECT_SUCCESS;
    }
}
