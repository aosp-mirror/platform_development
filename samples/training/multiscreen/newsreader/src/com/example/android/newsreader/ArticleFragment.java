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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

/**
 * Fragment that displays a news article.
 */
public class ArticleFragment extends Fragment {
    // The webview where we display the article (our only view)
    WebView mWebView;

    // The article we are to display
    NewsArticle mNewsArticle = null;

    // Parameterless constructor is needed by framework
    public ArticleFragment() {
        super();
    }

    /**
     * Sets up the UI. It consists if a single WebView.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mWebView = new WebView(getActivity());
        loadWebView();
        return mWebView;
    }

    /**
     * Displays a particular article.
     *
     * @param article the article to display
     */
    public void displayArticle(NewsArticle article) {
        mNewsArticle = article;
        loadWebView();
    }

    /**
     * Loads article data into the webview.
     *
     * This method is called internally to update the webview's contents to the appropriate
     * article's text.
     */
    void loadWebView() {
        if (mWebView != null) {
            mWebView.loadData(mNewsArticle == null ? "" : mNewsArticle.getBody(), "text/html",
                        "utf-8");
        }
    }
}
