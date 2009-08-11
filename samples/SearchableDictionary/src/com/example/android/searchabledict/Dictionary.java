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

package com.example.android.searchabledict;

import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contains logic to load the word of words and definitions and find a list of matching words
 * given a query.  Everything is held in memory; this is not a robust way to serve lots of
 * words and is only for demo purposes.
 *
 * You may want to consider using an SQLite database. In practice, you'll want to make sure your
 * suggestion provider is as efficient as possible, as the system will be taxed while performing
 * searches across many sources for each keystroke the user enters into Quick Search Box.
 */
public class Dictionary {

    public static class Word {
        public final String word;
        public final String definition;

        public Word(String word, String definition) {
            this.word = word;
            this.definition = definition;
        }
    }

    private static final Dictionary sInstance = new Dictionary();

    public static Dictionary getInstance() {
        return sInstance;
    }

    private final Map<String, List<Word>> mDict = new ConcurrentHashMap<String, List<Word>>();

    private Dictionary() {
    }

    private boolean mLoaded = false;

    /**
     * Loads the words and definitions if they haven't been loaded already.
     *
     * @param resources Used to load the file containing the words and definitions.
     */
    public synchronized void ensureLoaded(final Resources resources) {
        if (mLoaded) return;

        new Thread(new Runnable() {
            public void run() {
                try {
                    loadWords(resources);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private synchronized void loadWords(Resources resources) throws IOException {
        if (mLoaded) return;

        Log.d("dict", "loading words");
        InputStream inputStream = resources.openRawResource(R.raw.definitions);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        try {
            String line;
            while((line = reader.readLine()) != null) {
                String[] strings = TextUtils.split(line, "-");
                if (strings.length < 2) continue;
                addWord(strings[0].trim(), strings[1].trim());
            }
        } finally {
            reader.close();
        }
        mLoaded = true;
    }


    public List<Word> getMatches(String query) {
        List<Word> list = mDict.get(query);
        return list == null ? Collections.EMPTY_LIST : list;
    }

    private void addWord(String word, String definition) {
        final Word theWord = new Word(word, definition);

        final int len = word.length();
        for (int i = 0; i < len; i++) {
            final String prefix = word.substring(0, len - i);
            addMatch(prefix, theWord);
        }
    }

    private void addMatch(String query, Word word) {
        List<Word> matches = mDict.get(query);
        if (matches == null) {
            matches = new ArrayList<Word>();
            mDict.put(query, matches);
        }
        matches.add(word);
    }
}
