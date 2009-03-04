/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.traceview;

public class Selection {

    private Action mAction;
    private String mName;
    private Object mValue;

    public Selection(Action action, String name, Object value) {
        mAction = action;
        mName = name;
        mValue = value;
    }

    public static Selection highlight(String name, Object value) {
        return new Selection(Action.Highlight, name, value);
    }

    public static Selection include(String name, Object value) {
        return new Selection(Action.Include, name, value);
    }

    public static Selection exclude(String name, Object value) {
        return new Selection(Action.Exclude, name, value);
    }

    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public void setValue(Object value) {
        mValue = value;
    }

    public Object getValue() {
        return mValue;
    }

    public void setAction(Action action) {
        mAction = action;
    }

    public Action getAction() {
        return mAction;
    }

    public static enum Action {
        Highlight, Include, Exclude, Aggregate
    };
}
