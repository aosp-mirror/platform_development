/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ddmuilib;

import com.android.ddmlib.Log;

/**
 * base background thread class. The class provides a synchronous quit method
 * which sets a quitting flag to true. Inheriting classes should regularly test
 * this flag with <code>isQuitting()</code> and should finish if the flag is
 * true.
 */
public abstract class BackgroundThread extends Thread {
    private boolean mQuit = false;

    /**
     * Tell the thread to exit. This is usually called from the UI thread. The
     * call is synchronous and will only return once the thread has terminated
     * itself.
     */
    public final void quit() {
        mQuit = true;
        Log.d("ddms", "Waiting for BackgroundThread to quit");
        try {
            this.join();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    /** returns if the thread was asked to quit. */
    protected final boolean isQuitting() {
        return mQuit;
    }

}
