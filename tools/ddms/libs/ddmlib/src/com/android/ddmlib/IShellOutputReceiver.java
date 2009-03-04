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

package com.android.ddmlib;

/**
 * Classes which implement this interface provide methods that deal with out from a remote shell
 * command on a device/emulator.
 */
public interface IShellOutputReceiver {
    /**
     * Called every time some new data is available.
     * @param data The new data.
     * @param offset The offset at which the new data starts.
     * @param length The length of the new data.
     */
    public void addOutput(byte[] data, int offset, int length);

    /**
     * Called at the end of the process execution (unless the process was
     * canceled). This allows the receiver to terminate and flush whatever
     * data was not yet processed.
     */
    public void flush();

    /**
     * Cancel method to stop the execution of the remote shell command.
     * @return true to cancel the execution of the command.
     */
    public boolean isCancelled();
};
