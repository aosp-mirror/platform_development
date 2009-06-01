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

package com.android.sdklib.internal.repository;


/**
 * A monitor interface for a {@link ITask}.
 * <p/>
 * Depending on the task factory that created the task, there might not be any UI
 * or it might not implement all the methods, in which case calling them would be
 * a no-op but is guaranteed not to crash.
 * <p/>
 * If the task runs in a non-UI worker thread, the task factory implementation
 * will take care of the update the UI in the correct thread. The task itself
 * must not have to deal with it.
 */
public interface ITaskMonitor {

    /**
     * Sets the description in the current task dialog.
     * This method can be invoked from a non-UI thread.
     */
    public void setDescription(String descriptionFormat, Object...args);

    /**
     * Sets the result text in the current task dialog.
     * This method can be invoked from a non-UI thread.
     */
    public void setResult(String resultFormat, Object...args);

    /**
     * Sets the max value of the progress bar.
     * This method can be invoked from a non-UI thread.
     */
    public void setProgressMax(int max);

    /**
     * Increments the current value of the progress bar.
     * This method can be invoked from a non-UI thread.
     */
    public void incProgress(int delta);

    /**
     * Returns true if the user requested to cancel the operation.
     * It is up to the task thread to pool this and exit as soon
     * as possible.
     */
    public boolean cancelRequested();
}
