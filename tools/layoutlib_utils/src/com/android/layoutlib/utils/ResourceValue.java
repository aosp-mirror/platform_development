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

package com.android.layoutlib.utils;

import com.android.layoutlib.api.IResourceValue;

public class ResourceValue implements IResourceValue {
    private final String mType;
    private final String mName;
    private String mValue = null;
    private final boolean mIsFramwork;
    
    public ResourceValue(String type, String name, boolean isFramwork) {
        mType = type;
        mName = name;
        mIsFramwork = isFramwork;
    }

    public ResourceValue(String type, String name, String value, boolean isFramework) {
        mType = type;
        mName = name;
        mValue = value;
        mIsFramwork = isFramework;
    }

    public String getType() {
        return mType;
    }

    public final String getName() {
        return mName;
    }
    
    public final String getValue() {
        return mValue;
    }
    
    public final void setValue(String value) {
        mValue = value;
    }
    
    public void replaceWith(ResourceValue value) {
        mValue = value.mValue;
    }

    public boolean isFramework() {
        return mIsFramwork;
    }
}
