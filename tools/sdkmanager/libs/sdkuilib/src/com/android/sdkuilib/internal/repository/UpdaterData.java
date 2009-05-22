/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.sdkuilib.internal.repository;

/**
 * Data shared between {@link UpdaterWindowImpl} and its pages.
 */
class UpdaterData {
    private String mOsSdkRoot;
    private boolean mUserCanChangeSdkRoot;
    private RepoSources mSources = new RepoSources();

    public void setOsSdkRoot(String osSdkRoot) {
        mOsSdkRoot = osSdkRoot;
    }

    public String getOsSdkRoot() {
        return mOsSdkRoot;
    }

    public void setUserCanChangeSdkRoot(boolean userCanChangeSdkRoot) {
        mUserCanChangeSdkRoot = userCanChangeSdkRoot;
    }

    public boolean canUserChangeSdkRoot() {
        return mUserCanChangeSdkRoot;
    }

    public void setSources(RepoSources sources) {
        mSources = sources;
    }

    public RepoSources getSources() {
        return mSources;
    }

}
