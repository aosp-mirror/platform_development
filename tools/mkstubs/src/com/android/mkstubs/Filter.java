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
 * A "filter" holds the various patterns that MkStubs should accept (include)
 * or reject (exclude). Patterns can be of two kind:
 * <ul>
 * <li>Full patterns are simple string matches, similar to a "^pattern$" regex.
 * <li>Prefix patterns are partial string matches, similar to a "^pattern.*" regex.
 * </ul>
 * <p/>
 * The {@link #accept(String)} method examines a given string against the known
 * pattern to decide if it should be included.
 */
class Filter {
    private TreeSet<String> mIncludePrefix = new TreeSet<String>();
    private TreeSet<String> mIncludeFull   = new TreeSet<String>();
    private TreeSet<String> mExcludePrefix = new TreeSet<String>();
    private TreeSet<String> mExcludeFull   = new TreeSet<String>();

    /**
     * Returns the set of all full patterns to be included.
     */
    public TreeSet<String> getIncludeFull() {
        return mIncludeFull;
    }

    /**
     * Returns the set of all prefix patterns to be included.
     */
    public TreeSet<String> getIncludePrefix() {
        return mIncludePrefix;
    }
    
    /**
     * Returns the set of all full patterns to be excluded.
     */
    public TreeSet<String> getExcludeFull() {
        return mExcludeFull;
    }
    
    /**
     * Returns the set of all prefix patterns to be excluded.
     */
    public TreeSet<String> getExcludePrefix() {
        return mExcludePrefix;
    }

    /**
     * Checks if the given string passes the various include/exclude rules.
     * The matching is done as follows:
     * <ul>
     * <li> The string must match either a full include or a prefix include.
     * <li> The string must not match any full exclude nor any prefix exclude.
     * </ul>
     * @param s The string to accept or reject.
     * @return True if the string can be accepted, false if it must be rejected.
     */
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
