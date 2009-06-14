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

package com.android.sdklib.internal.repository;

import java.util.ArrayList;

/**
 * A list of sdk-repository sources.
 */
public class RepoSources {

    private ArrayList<RepoSource> mSources = new ArrayList<RepoSource>();

    public RepoSources() {
    }

    /**
     * Adds a new source to the Sources list.
     */
    public void add(RepoSource source) {
        mSources.add(source);
    }

    /**
     * Returns the sources list array. This is never null.
     */
    public ArrayList<RepoSource> getSources() {
        return mSources;
    }
}
