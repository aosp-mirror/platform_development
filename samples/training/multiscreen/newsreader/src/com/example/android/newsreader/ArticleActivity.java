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

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 * Activity that displays a particular news article onscreen.
 *
 * This activity is started only when the screen is not large enough for a two-pane layout, in
 * which case this separate activity is shown in order to display the news article. This activity
 * kills itself if the display is reconfigured into a shape that allows a two-pane layout, since
 * in that case the news article will be displayed by the {@link NewsReaderActivity} and this
 * Activity therefore becomes unnecessary.
 */
public class ArticleActivity extends FragmentActivity {
    // The news category index and the article index for the article we are to display
    int mCatIndex, mArtIndex;

    /**
     * Sets up the activity.
     *
     * Setting up the activity means reading the category/article index from the Intent that
     * fired this Activity and loading it onto the UI. We also detect if there has been a
     * screen configuration change (in particular, a rotation) that makes this activity
     * unnecessary, in which case we do the honorable thing and get out of the way.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCatIndex = getIntent().getExtras().getInt("catIndex", 0);
        mArtIndex = getIntent().getExtras().getInt("artIndex", 0);

        // If we are in two-pane layout mode, this activity is no longer necessary
        if (getResources().getBoolean(R.bool.has_two_panes)) {
            finish();
            return;
        }

        // Place an ArticleFragment as our content pane
        ArticleFragment f = new ArticleFragment();
        getSupportFragmentManager().beginTransaction().add(android.R.id.content, f).commit();

        // Display the correct news article on the fragment
        NewsArticle article = NewsSource.getInstance().getCategory(mCatIndex).getArticle(mArtIndex);
        f.displayArticle(article);
    }
}
