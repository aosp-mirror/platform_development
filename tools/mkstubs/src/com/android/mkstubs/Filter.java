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

package com.android.mkstubs;

import java.util.TreeSet;

/**
 * 
 */
class Filter {
    private TreeSet<String> mIncludePrefix = new TreeSet<String>();
    private TreeSet<String> mIncludeFull   = new TreeSet<String>();
    private TreeSet<String> mExcludePrefix = new TreeSet<String>();
    private TreeSet<String> mExcludeFull   = new TreeSet<String>();

    public TreeSet<String> getIncludeFull() {
        return mIncludeFull;
    }
    
    public TreeSet<String> getIncludePrefix() {
        return mIncludePrefix;
    }
    
    public TreeSet<String> getExcludeFull() {
        return mExcludeFull;
    }
    
    public TreeSet<String> getExcludePrefix() {
        return mExcludePrefix;
    }
    
    public boolean accept(String s) {
        
        // Check if it can be included.
        boolean accept = mIncludeFull.contains(s);
        if (!accept) {
            // Check for a prefix inclusion
            for (String prefix : mIncludePrefix) {
                if (s.startsWith(prefix)) {
                    accept = true;
                    break;
                }
            }
        }
        
        if (accept) {
            // check for a full exclusion
            accept = !mExcludeFull.contains(s);
        }
        if (accept) {
            // or check for prefix exclusion
            for (String prefix : mExcludePrefix) {
                if (s.startsWith(prefix)) {
                    accept = false;
                    break;
                }
            }
        }

        return accept;
    }    
}
