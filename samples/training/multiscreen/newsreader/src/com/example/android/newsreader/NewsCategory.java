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
 * A news category (collection of articles).
 */
public class NewsCategory {
    // how many articles?
    final int ARTICLES_PER_CATEGORY = 20;

    // array of our articles
    NewsArticle[] mArticles;

    /**
     * Create a news category.
     *
     * The articles are dynamically generated with fun and random nonsense.
     */
    public NewsCategory() {
        NonsenseGenerator ngen = new NonsenseGenerator();
        mArticles = new NewsArticle[ARTICLES_PER_CATEGORY];
        int i;
        for (i = 0; i < mArticles.length; i++) {
            mArticles[i] = new NewsArticle(ngen);
        }
    }

    /** Returns how many articles exist in this category. */
    public int getArticleCount() {
        return mArticles.length;
    }

    /** Gets a particular article by index. */
    public NewsArticle getArticle(int index) {
        return mArticles[index];
    }
}
