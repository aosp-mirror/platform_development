/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.basicmediarouter;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * <p>
 * A {@link android.app.Presentation} used to demonstrate interaction between primary and
 * secondary screens.
 * </p>
 * <p>
 * It displays the name of the display in which it has been embedded (see
 * {@link android.app.Presentation#getDisplay()}) and exposes a facility to change its
 * background color and display its text.
 * </p>
 */
public class SamplePresentation extends Presentation {

    private LinearLayout mLayout;
    private TextView mText;

    public SamplePresentation(Context outerContext, Display display) {
        super(outerContext, display);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the content view to the custom layout
        setContentView(R.layout.display);

        // Get the Views
        mLayout = (LinearLayout) findViewById(R.id.display_layout);
        mText = (TextView) findViewById(R.id.display_text);

        /*
         * Show the name of the display this presentation was embedded in.
         */
        TextView smallText = (TextView) findViewById(R.id.display_smalltext);
        final String name = getDisplay().getName();
        smallText.setText(getResources().getString(R.string.display_name, name));
    }

    /**
     * Set the background color of the layout and display the color as a String.
     *
     * @param color The background color
     */
    public void setColor(int color) {
        mLayout.setBackgroundColor(color);

        // Display the color as a string on screen
        String s = getResources().getString(R.string.display_color, color);
        mText.setText(s);
    }

}
