/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.example.android.newsreader;

/**
 * Source of strange and wonderful news.
 *
 * This singleton functions as the repository for the news we display.
 */
public class NewsSource {
    // the instance
    static NewsSource instance = null;

    // the category names
    final String[] CATEGORIES = { "Top Stories", "US", "Politics", "Economy" };

    // category objects, representing each category
    NewsCategory[] mCategory;

    /** Returns the singleton instance of this class. */
    public static NewsSource getInstance() {
        if (instance == null) {
            instance = new NewsSource();
        }
        return instance;
    }

    public NewsSource() {
        int i;
        mCategory = new NewsCategory[CATEGORIES.length];
        for (i = 0; i < CATEGORIES.length; i++) {
            mCategory[i] = new NewsCategory();
        }
    }

    /** Returns the list of news categories. */
    public String[] getCategories() {
        return CATEGORIES;
    }

    /** Returns a category by index. */
    public NewsCategory getCategory(int categoryIndex) {
        return mCategory[categoryIndex];
    }
}
