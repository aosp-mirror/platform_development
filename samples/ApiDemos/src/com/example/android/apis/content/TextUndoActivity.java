/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.apis.content;

import android.app.Activity;
//import android.content.UndoManager;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import com.example.android.apis.R;

import android.os.Bundle;
import android.widget.TextView;

/**
 * Simple example of using an UndoManager for editing text in a TextView.
 */
public class TextUndoActivity extends Activity {
    //UndoManager mUndoManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        mUndoManager = new UndoManager();
        if (savedInstanceState != null) {
            Parcelable p = savedInstanceState.getParcelable("undo");
            if (p != null) {
                mUndoManager.restoreInstanceState(p);
            }
        }
        */

        setContentView(R.layout.text_undo);

        /*
        ((TextView)findViewById(R.id.text)).setUndoManager(mUndoManager, "text");
        ((Button)findViewById(R.id.undo)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUndoManager.undo(null, 1);
            }
        });
        ((Button)findViewById(R.id.redo)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUndoManager.redo(null, 1);
            }
        });
        */
     }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //outState.putParcelable("undo", mUndoManager.saveInstanceState());
    }
}
