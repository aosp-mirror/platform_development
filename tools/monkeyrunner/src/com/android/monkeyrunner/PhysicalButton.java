/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.monkeyrunner;

public enum PhysicalButton {
    HOME("home"),
    SEARCH("search"),
    MENU("menu"),
    BACK("back"),
    DPAD_UP("DPAD_UP"),
    DPAD_DOWN("DPAD_DOWN"),
    DPAD_LEFT("DPAD_LEFT"),
    DPAD_RIGHT("DPAD_RIGHT"),
    DPAD_CENTER("DPAD_CENTER"),
    ENTER("enter");

    private String keyName;

    private PhysicalButton(String keyName) {
        this.keyName = keyName;
    }

    public String getKeyName() {
        return keyName;
    }
}
