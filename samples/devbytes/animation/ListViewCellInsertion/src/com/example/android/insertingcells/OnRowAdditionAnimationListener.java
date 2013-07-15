/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.insertingcells;

/**
 * This listener is used to determine when the animation of a new row addition
 * begins and ends. The primary use of this interface is to create a callback
 * under which certain elements, such as the listview itself, can be disabled
 * to prevent unpredictable behaviour during the actual cell animation.
 */
public interface OnRowAdditionAnimationListener {
    public void onRowAdditionAnimationStart();
    public void onRowAdditionAnimationEnd();
}
