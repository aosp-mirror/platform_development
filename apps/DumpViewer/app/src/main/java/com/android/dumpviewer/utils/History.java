/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.dumpviewer.utils;

import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class History {
    private final SharedPreferences mPrefs;
    private final String mSharedPrefKey;
    private final int mMaxSize;

    private static final String SEPARATOR = "\u0001sep\u0001";

    private ArrayList<String> mItems = new ArrayList<>();

    public History(SharedPreferences prefs, String sharedPrefKey, int maxSize) {
        mPrefs = prefs;
        mSharedPrefKey = sharedPrefKey;
        mMaxSize = maxSize;
    }

    private void ensureMaxSize() {
        while (mItems.size() > mMaxSize) {
            mItems.remove(0);
        }
    }

    public void load() {
        mItems.clear();

        mItems.addAll(Arrays.asList(
                TextUtils.split(mPrefs.getString(mSharedPrefKey, ""), SEPARATOR)));
        ensureMaxSize();
    }

    private void save() {
        String[] items = mItems.toArray(new String[mItems.size()]);
        mPrefs.edit().putString(mSharedPrefKey, TextUtils.join(SEPARATOR, items)).apply();
    }

    public void add(String item) {
        item = item.trim();
        if (item.length() == 0) {
            return;
        }
        String fitem = item;
        mItems.removeIf(v -> v.equals(fitem));
        mItems.add(item);
        ensureMaxSize();

        save();
    }

    public void addAllTo(Collection<String> col) {
        for (int i = mItems.size() - 1; i >= 0; i--) {
            col.add(mItems.get(i));
        }
    }
}
