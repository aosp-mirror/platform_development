/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.sdkuilib.repository;

import org.eclipse.swt.widgets.ProgressBar;

/**
 * A monitor interface for a {@link ProgressTask}
 */
interface ITaskMonitor {

    /**
     * Sets the description in the current task dialog.
     * This method can be invoke from a non-UI thread.
     */
    public void setDescription(String description);

    /**
     * Sets the result text in the current task dialog.
     * This method can be invoked from a non-UI thread.
     */
    public void setResult(String result);

    /**
     * Sets the max value of the progress bar.
     * This method can be invoke from a non-UI thread.
     *
     * @see ProgressBar#setMaximum(int)
     */
    public void setProgressMax(int max);

    /**
     * Increments the current value of the progress bar.
     *
     * This method can be invoked from a non-UI thread.
     */
    public void incProgress(int delta);

    /**
     * Returns true if the "Cancel" button was selected.
     * It is up to the task thread to pool this and exit.
     */
    public boolean cancelRequested();


}
