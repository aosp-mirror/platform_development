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


public class ProfileData {

    protected MethodData mElement;
    
    /** mContext is either the parent or child of mElement */
    protected MethodData mContext;
    protected boolean mElementIsParent;
    protected long mElapsedInclusive;
    protected int mNumCalls;

    public ProfileData() {
    }

    public ProfileData(MethodData context, MethodData element,
            boolean elementIsParent) {
        mContext = context;
        mElement = element;
        mElementIsParent = elementIsParent;
    }

    public String getProfileName() {
        return mElement.getProfileName();
    }

    public MethodData getMethodData() {
        return mElement;
    }

    public void addElapsedInclusive(long elapsedInclusive) {
        mElapsedInclusive += elapsedInclusive;
        mNumCalls += 1;
    }

    public void setElapsedInclusive(long elapsedInclusive) {
        mElapsedInclusive = elapsedInclusive;
    }

    public long getElapsedInclusive() {
        return mElapsedInclusive;
    }

    public void setNumCalls(int numCalls) {
        mNumCalls = numCalls;
    }

    public String getNumCalls() {
        int totalCalls;
        if (mElementIsParent)
            totalCalls = mContext.getTotalCalls();
        else
            totalCalls = mElement.getTotalCalls();
        return String.format("%d/%d", mNumCalls, totalCalls);
    }

    public boolean isParent() {
        return mElementIsParent;
    }

    public MethodData getContext() {
        return mContext;
    }
}
