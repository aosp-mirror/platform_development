/*
 * Copyright 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.tabcompat.lib;

import android.support.v4.app.FragmentTransaction;

/**
 * @see android.app.ActionBar.TabListener
 */
public interface CompatTabListener {
    /**
     * @see android.app.ActionBar.TabListener#onTabSelected(
     *android.app.ActionBar.Tab, android.app.FragmentTransaction)
     */
    public void onTabSelected(CompatTab tab, FragmentTransaction ft);

    /**
     * @see android.app.ActionBar.TabListener#onTabUnselected(
     *android.app.ActionBar.Tab, android.app.FragmentTransaction)
     */
    public void onTabUnselected(CompatTab tab, FragmentTransaction ft);

    /**
     * @see android.app.ActionBar.TabListener#onTabReselected(
     *android.app.ActionBar.Tab, android.app.FragmentTransaction)
     */
    public void onTabReselected(CompatTab tab, FragmentTransaction ft);
}
