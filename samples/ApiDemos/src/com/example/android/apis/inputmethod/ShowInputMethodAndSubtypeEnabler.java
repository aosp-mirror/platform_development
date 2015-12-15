/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.apis.inputmethod;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import static android.widget.LinearLayout.VERTICAL;

/**
 * Demonstrates how to show the input method subtype enabler without relying on
 * {@link InputMethodManager#showInputMethodAndSubtypeEnabler(String)}, which is highly likely to be
 * broken.
 */
public class ShowInputMethodAndSubtypeEnabler extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(VERTICAL);

        {
            final Button button = new Button(this);
            button.setText("Show (All IMEs)");
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showInputMethodAndSubtypeEnabler(ShowInputMethodAndSubtypeEnabler.this, null);
                }
            });
            layout.addView(button);
        }

        for (InputMethodInfo imi : getEnabledInputMethodsThatHaveMultipleSubtypes()) {
            final Button button = new Button(this);
            final String id = imi.getId();
            button.setText("Show (" + id + ")");
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showInputMethodAndSubtypeEnabler(ShowInputMethodAndSubtypeEnabler.this, id);
                }
            });
            layout.addView(button);
        }
        setContentView(layout);
    }

//BEGIN_INCLUDE(showInputMethodAndSubtypeEnabler)
    static void showInputMethodAndSubtypeEnabler(Context context, String inputMethodId) {
        final Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (!TextUtils.isEmpty(inputMethodId)) {
            intent.putExtra(Settings.EXTRA_INPUT_METHOD_ID, inputMethodId);
        }
        context.startActivity(intent, null);
    }
//END_INCLUDE(showInputMethodAndSubtypeEnabler)

    private List<InputMethodInfo> getEnabledInputMethodsThatHaveMultipleSubtypes() {
        final InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        final List<InputMethodInfo> result = new ArrayList<>();
        if (imm == null) {
            return result;
        }
        for (InputMethodInfo imi : imm.getEnabledInputMethodList()) {
            if (imi.getSubtypeCount() > 1) {
                result.add(imi);
            }
        }
        return result;
    }
}
