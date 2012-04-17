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

package com.example.android.samplespellcheckerservice;

import android.os.Build;
import android.service.textservice.SpellCheckerService;
import android.util.Log;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;

import java.util.ArrayList;

public class SampleSpellCheckerService extends SpellCheckerService {
    private static final String TAG = SampleSpellCheckerService.class.getSimpleName();
    private static final boolean DBG = true;

    @Override
    public Session createSession() {
        return new AndroidSpellCheckerSession();
    }

    private static class AndroidSpellCheckerSession extends Session {

        private boolean isSentenceSpellCheckApiSupported() {
            // Note that the sentence level spell check APIs work on Jelly Bean or later.
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
        }

        private String mLocale;
        @Override
        public void onCreate() {
            mLocale = getLocale();
        }

        /**
         * This method should have a concrete implementation in all spell checker services.
         * Please note that the default implementation of
         * {@link SpellCheckerService.Session#onGetSuggestionsMultiple(TextInfo[], int, boolean)}
         * calls up this method. You may want to override
         * {@link SpellCheckerService.Session#onGetSuggestionsMultiple(TextInfo[], int, boolean)}
         * by your own implementation if you'd like to provide an optimized implementation for
         * {@link SpellCheckerService.Session#onGetSuggestionsMultiple(TextInfo[], int, boolean)}.
         */
        @Override
        public SuggestionsInfo onGetSuggestions(TextInfo textInfo, int suggestionsLimit) {
            if (DBG) {
                Log.d(TAG, "onGetSuggestions: " + textInfo.getText());
            }
            final String input = textInfo.getText();
            final int length = input.length();
            // Just a fake logic:
            // length <= 3 for short words that we assume are in the fake dictionary
            // length > 20 for too long words that we assume can't be recognized (such as CJK words)
            final int flags = length <= 3 ? SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY
                    : length <= 20 ? SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO : 0;
            return new SuggestionsInfo(flags,
                    new String[] {"aaa", "bbb", "Candidate for " + input, mLocale});
        }

        /**
         * Please consider providing your own implementation of sentence level spell checking.
         * Please note that this sample implementation is just a mock to demonstrate how a sentence
         * level spell checker returns the result.
         * If you don't override this method, the framework converts queries of
         * {@link SpellCheckerService.Session#onGetSentenceSuggestionsMultiple(TextInfo[], int)}
         * to queries of
         * {@link SpellCheckerService.Session#onGetSuggestionsMultiple(TextInfo[], int, boolean)}
         * by the default implementation.
         */
        @Override
        public SentenceSuggestionsInfo[] onGetSentenceSuggestionsMultiple(
                TextInfo[] textInfos, int suggestionsLimit) {
            if (!isSentenceSpellCheckApiSupported()) {
                Log.e(TAG, "Sentence spell check is not supported on this platform, "
                        + "but accidentially called.");
                return null;
            }
            final ArrayList<SentenceSuggestionsInfo> retval =
                    new ArrayList<SentenceSuggestionsInfo>();
            for (int i = 0; i < textInfos.length; ++i) {
                final TextInfo ti = textInfos[i];
                if (DBG) {
                    Log.d(TAG, "onGetSentenceSuggestionsMultiple: " + ti.getText());
                }
                final String input = ti.getText();
                final int length = input.length();
                final SuggestionsInfo[] sis;
                final int[] lengths;
                final int[] offsets;
                if (input.equalsIgnoreCase("I wold like to here form you")) {
                    // Return sentence level suggestion for this fixed input
                    final int flags0 = SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO;
                    final int flags1 = SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS
                            | SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO;
                    final int flags2 = flags1;
                    final SuggestionsInfo si0 = new SuggestionsInfo(
                            flags0, new String[] { "would" });
                    final SuggestionsInfo si1 = new SuggestionsInfo(
                            flags1, new String[] { "hear" });
                    final SuggestionsInfo si2 = new SuggestionsInfo(
                            flags2, new String[] { "from" });
                    sis = new SuggestionsInfo[] {si0, si1, si2};
                    offsets = new int[] { 2, 15, 20 };
                    lengths = new int[] { 4, 4, 4 };
                } else {
                    // Just a mock logic:
                    // length <= 3 for short words that we assume are in the fake dictionary
                    // length > 20 for too long words that we assume can't be recognized
                    // (such as CJK words)
                    final int flags = length <= 3 ? SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY
                            : length <= 20 ? SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO : 0;
                    final SuggestionsInfo si = new SuggestionsInfo(flags,
                            new String[] {"aaa", "bbb", "Candidate for " + input, mLocale});
                    sis = new SuggestionsInfo[] { si };
                    offsets = new int[] { 0 };
                    lengths = new int[] { ti.getText().length() };
                }
                final SentenceSuggestionsInfo ssi =
                        new SentenceSuggestionsInfo(sis, lengths, offsets);
                retval.add(ssi);
            }
            return retval.toArray(new SentenceSuggestionsInfo[0]);
        }
    }
}
