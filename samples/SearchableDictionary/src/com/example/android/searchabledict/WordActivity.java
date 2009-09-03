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

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.content.Intent;

/**
 * Displays a word and its definition.
 */
public class WordActivity extends Activity {

    private TextView mWord;
    private TextView mDefinition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.word);

        mWord = (TextView) findViewById(R.id.word);
        mDefinition = (TextView) findViewById(R.id.definition);

        Intent intent = getIntent();

        String word = intent.getStringExtra("word");
        String definition = intent.getStringExtra("definition");

        mWord.setText(word);
        mDefinition.setText(definition);
    }
}
