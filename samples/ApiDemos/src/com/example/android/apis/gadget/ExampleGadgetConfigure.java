/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.example.android.apis.gadget;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.gadget.GadgetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.util.ArrayList;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.
import com.example.android.apis.R;

/**
 * The configuration screen for the ExampleGadgetProvider gadget sample.
 */
public class ExampleGadgetConfigure extends Activity {
    static final String TAG = "ExampleGadgetConfigure";

    private static final String PREFS_NAME
            = "com.example.android.apis.gadget.ExampleGadgetProvider";
    private static final String PREF_PREFIX_KEY = "prefix_";

    int mGadgetId = GadgetManager.INVALID_GADGET_ID;
    EditText mGadgetPrefix;

    public ExampleGadgetConfigure() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the gadget host to cancel
        // out of the gadget placement if they press the back button.
        setResult(RESULT_CANCELED);

        // Set the view layout resource to use.
        setContentView(R.layout.gadget_configure);

        // Find the EditText
        mGadgetPrefix = (EditText)findViewById(R.id.gadget_prefix);

        // Bind the action for the save button.
        findViewById(R.id.save_button).setOnClickListener(mOnClickListener);

        // Find the gadget id from the intent. 
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mGadgetId = extras.getInt(
                    GadgetManager.EXTRA_GADGET_ID, GadgetManager.INVALID_GADGET_ID);
        }

        // If they gave us an intent without the gadget id, just bail.
        if (mGadgetId == GadgetManager.INVALID_GADGET_ID) {
            finish();
        }

        mGadgetPrefix.setText(loadTitlePref(ExampleGadgetConfigure.this, mGadgetId));
    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            // When the button is clicked, save the string in our prefs and return that they
            // clicked OK.
            saveTitlePref(ExampleGadgetConfigure.this, mGadgetId,
                    mGadgetPrefix.getText().toString());

            setResult(RESULT_OK);
            finish();
        }
    };

    // Write the prefix to the SharedPreferences object for this gadget
    static void saveTitlePref(Context context, int gadgetId, String text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + gadgetId, text);
        prefs.commit();
    }

    // Read the prefix from the SharedPreferences object for this gadget.
    // If there is no preference saved, get the default from a resource
    static String loadTitlePref(Context context, int gadgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String prefix = prefs.getString(PREF_PREFIX_KEY, null);
        if (prefix != null) {
            return prefix;
        } else {
            return context.getString(R.string.gadget_prefix_default);
        }
    }

    static void deleteTitlePref(Context context, int gadgetId) {
    }

    static void loadAllTitlePrefs(Context context, ArrayList<Integer> gadgetIds,
            ArrayList<String> texts) {
    }
}



