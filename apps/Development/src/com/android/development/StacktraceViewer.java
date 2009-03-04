/*
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License"); 
** you may not use this file except in compliance with the License. 
** You may obtain a copy of the License at 
**
**     http://www.apache.org/licenses/LICENSE-2.0 
**
** Unless required by applicable law or agreed to in writing, software 
** distributed under the License is distributed on an "AS IS" BASIS, 
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
** See the License for the specific language governing permissions and 
** limitations under the License.
*/

package com.android.development;


import android.app.Activity;
import android.widget.EditText;
import android.widget.TextView;
import android.text.method.ScrollingMovementMethod;
import android.os.Bundle;
import android.server.data.CrashData;
import android.server.data.ThrowableData;
import android.server.data.StackTraceElementData;
import android.graphics.Typeface;

/**
 * Views a single stack trace.
 */
public class StacktraceViewer extends Activity {

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.log_viewer);

        TextView text = (TextView) findViewById(R.id.text);
        text.setTextSize(10);
        text.setHorizontallyScrolling(true);
        text.setTypeface(Typeface.MONOSPACE);
        text.setMovementMethod(ScrollingMovementMethod.getInstance());
        
        String stacktrace = getIntent().getExtras().getString(
                CrashData.class.getName());

        text.setText(stacktrace);
    }
}
