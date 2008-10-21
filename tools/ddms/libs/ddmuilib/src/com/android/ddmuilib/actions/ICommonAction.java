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

package com.android.ddmuilib.actions;

/**
 * Common interface for basic action handling. This allows the common ui
 * components to access ToolItem or Action the same way.
 */
public interface ICommonAction {
    /**
     * Sets the enabled state of this action.
     * @param enabled <code>true</code> to enable, and
     *   <code>false</code> to disable
     */
    public void setEnabled(boolean enabled);

    /**
     * Sets the checked status of this action.
     * @param checked the new checked status
     */
    public void setChecked(boolean checked);
    
    /**
     * Sets the {@link Runnable} that will be executed when the action is triggered.
     */
    public void setRunnable(Runnable runnable);
}

