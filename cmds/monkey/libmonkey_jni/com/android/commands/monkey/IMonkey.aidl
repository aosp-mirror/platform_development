/*
 * Copyright 2024 The Android Open Source Project
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

/**
 * A local AIDL interface used as a foreign function interface (ffi) to
 * communicate with the libinput library.
 *
 * NOTE: Since we use this as a local interface, all processing happens on the
 * calling thread.
 */
interface IMonkey {
    boolean writeTouchEvent(int pointerId, int toolType, int action, float locationX,
                             float locationY, float pressure, float majorAxisSize,
                             long eventTime);
}
