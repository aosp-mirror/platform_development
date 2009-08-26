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

package com.android.sdklib.internal.project;

import java.util.HashMap;
import java.util.Map;

/**
 * Settings for multiple APK generation.
 */
public class ApkSettings {
    private boolean mSplitByDpi = false;

    public ApkSettings() {
    }

    /**
     * Returns a map of configuration filters to be used by the -c option of aapt.
     * <p/>The map stores (key, value) pairs where the keys is a filename modifier and the value
     * is the parameter to pass to aapt through the -c option.
     * @return a map, always. It can however be empty.
     */
    public Map<String, String> getResourceFilters() {
        Map<String, String> map = new HashMap<String, String>();
        if (mSplitByDpi) {
            map.put("hdpi", "hdpi,nodpi");
            map.put("mdpi", "mdpi,nodpi");
            map.put("ldpi", "ldpi,nodpi");
        }

        return map;
    }

    /**
     * Indicates whether APKs should be generate for each dpi level.
     */
    public boolean isSplitByDpi() {
        return mSplitByDpi;
    }

    public void setSplitByDensity(boolean split) {
        mSplitByDpi = split;
    }
}
