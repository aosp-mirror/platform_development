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

public class ProfileNode {

    private String mLabel;
    private MethodData mMethodData;
    private ProfileData[] mChildren;
    private boolean mIsParent;
    private boolean mIsRecursive;

    public ProfileNode(String label, MethodData methodData,
            ProfileData[] children, boolean isParent, boolean isRecursive) {
        mLabel = label;
        mMethodData = methodData;
        mChildren = children;
        mIsParent = isParent;
        mIsRecursive = isRecursive;
    }

    public String getLabel() {
        return mLabel;
    }
    
    public ProfileData[] getChildren() {
        return mChildren;
    }

    public boolean isParent() {
        return mIsParent;
    }

    public boolean isRecursive() {
        return mIsRecursive;
    }
}
