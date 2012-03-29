/*
 * Copyright (C) 2012 Google Inc.
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

import android.app.IActivityManager;
import android.os.RemoteException;
import android.view.IWindowManager;
/**
 * monkey screen rotation event
 */
public class MonkeyRotationEvent extends MonkeyEvent {

    private final int mRotationDegree;
    private final boolean mPersist;

    /**
     * Construct a rotation Event.
     *
     * @param degree Possible rotation degrees, see constants in
     * anroid.view.Suface.
     * @param persist Should we keep the rotation lock after the orientation
     * change.
     */
    public MonkeyRotationEvent(int degree, boolean persist) {
        super(EVENT_TYPE_ROTATION);
        mRotationDegree = degree;
        mPersist = persist;
    }

    @Override
    public int injectEvent(IWindowManager iwm, IActivityManager iam, int verbose) {
        if (verbose > 0) {
            System.out.println(":Sending rotation degree=" + mRotationDegree +
                               ", persist=" + mPersist);
        }

        // inject rotation event
        try {
            iwm.freezeRotation(mRotationDegree);
            if (!mPersist) {
                iwm.thawRotation();
            }
            return MonkeyEvent.INJECT_SUCCESS;
        } catch (RemoteException ex) {
            return MonkeyEvent.INJECT_ERROR_REMOTE_EXCEPTION;
        }
    }
}
