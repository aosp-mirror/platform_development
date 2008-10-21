/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.hierarchyviewer.device;

public class Window {
    public static final Window FOCUSED_WINDOW = new Window("<Focused Window>", -1);

    private String title;
    private int hashCode;

    public Window(String title, int hashCode) {
        this.title = title;
        this.hashCode = hashCode;
    }

    public String getTitle() {
        return title;
    }

    public int getHashCode() {
        return hashCode;
    }

    public String encode() {
        return Integer.toHexString(hashCode);
    }

    public String toString() {
        return title;
    }
}
