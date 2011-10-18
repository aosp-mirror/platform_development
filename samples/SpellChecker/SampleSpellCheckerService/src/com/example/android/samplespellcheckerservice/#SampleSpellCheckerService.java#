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

import android.service.textservice.SpellCheckerService;
import android.util.Log;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;

public class SampleSpellCheckerService extends SpellCheckerService {
    private static final String TAG = SampleSpellCheckerService.class.getSimpleName();
    private static final boolean DBG = true;
    @Override
    public Session createSession() {
        return new AndroidSpellCheckerSession();
    }

    private static class AndroidSpellCheckerSession extends Session {
        private String mLocale;
        @Override
        public void onCreate() {
            mLocale = getLocale();
        }

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
    }
}
