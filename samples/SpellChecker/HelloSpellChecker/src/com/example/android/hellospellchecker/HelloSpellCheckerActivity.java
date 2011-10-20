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

package com.example.android.hellospellchecker;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;
import android.view.textservice.SpellCheckerSession.SpellCheckerSessionListener;
import android.widget.TextView;
import java.lang.StringBuilder;

public class HelloSpellCheckerActivity extends Activity implements SpellCheckerSessionListener {
    private static final String TAG = HelloSpellCheckerActivity.class.getSimpleName();
    private TextView mMainView;
    private SpellCheckerSession mScs;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mMainView = (TextView)findViewById(R.id.main);
    }

    @Override
    public void onResume() {
        super.onResume();
        final TextServicesManager tsm = (TextServicesManager) getSystemService(
                Context.TEXT_SERVICES_MANAGER_SERVICE);
        mScs = tsm.newSpellCheckerSession(null, null, this, true);
        if (mScs != null) {
            // Instantiate TextInfo for each query
            // TextInfo can be passed a sequence number and a cookie number to identify the result
            mScs.getSuggestions(new TextInfo("tgis"), 3);
            mScs.getSuggestions(new TextInfo("hllo"), 3);
            mScs.getSuggestions(new TextInfo("helloworld"), 3);
        } else {
            Log.e(TAG, "Couldn't obtain the spell checker service.");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mScs != null) {
            mScs.close();
        }
    }

    @Override
    public void onGetSuggestions(final SuggestionsInfo[] arg0) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arg0.length; ++i) {
            // Returned suggestions are contained in SuggestionsInfo
            final int len = arg0[i].getSuggestionsCount();
            sb.append('\n');
            for (int j = 0; j < len; ++j) {
                sb.append("," + arg0[i].getSuggestionAt(j));
            }
            sb.append(" (" + len + ")");
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMainView.append(sb.toString());
            }
        });
    }
}
