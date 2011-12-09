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
 * A news article.
 *
 * An article consists of a headline and a body. In this example app, article text is dynamically
 * generated nonsense.
 */
public class NewsArticle {
    // How many sentences in each paragraph?
    final int SENTENCES_PER_PARAGRAPH = 20;

    // How many paragraphs in each article?
    final int PARAGRAPHS_PER_ARTICLE = 5;

    // Headline and body
    String mHeadline, mBody;

    /**
     * Create a news article with randomly generated text.
     * @param ngen the nonsense generator to use.
     */
    public NewsArticle(NonsenseGenerator ngen) {
        mHeadline = ngen.makeHeadline();

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h1>" + mHeadline + "</h1>");
        int i;
        for (i = 0; i < PARAGRAPHS_PER_ARTICLE; i++) {
            sb.append("<p>").append(ngen.makeText(SENTENCES_PER_PARAGRAPH)).append("</p>");
        }

        sb.append("</body></html>");
        mBody = sb.toString();
    }

    /** Returns the headline. */
    public String getHeadline() {
        return mHeadline;
    }

    /** Returns the article body (HTML)*/
    public String getBody() {
        return mBody;
    }
}
