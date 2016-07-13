/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.bugreport.util;

/**
 * Helper class for parsing command line arguments.
 */
public class ArgParser {
    private final String[] mArgs;
    private int mPos;

    /**
     * Construct with the arguments from main(String[]).
     */
    public ArgParser(String[] args) {
        mArgs = args;
    }

    public String nextFlag() {
        if (mPos < mArgs.length) {
            if (mArgs[mPos].startsWith("--")) {
                return mArgs[mPos++];
            }
        }
        return null;
    }

    public boolean hasData(int count) {
        for (int i=mPos; i<mPos+count; i++) {
            if (i >= mArgs.length) {
                return false;
            }
            if (mArgs[i].startsWith("--")) {
                return false;
            }
        }
        return true;
    }

    public String nextData() {
        if (mPos < mArgs.length) {
            return mArgs[mPos++];
        }
        return null;
    }

    public int remaining() {
        return mArgs.length - mPos;
    }

    public int pos() {
        return mPos;
    }
}
