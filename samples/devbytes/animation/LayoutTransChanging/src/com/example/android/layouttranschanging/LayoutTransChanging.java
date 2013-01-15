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

package com.example.android.layouttranschanging;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

/**
 * This example shows how to use LayoutTransition to animate simple changes in a layout
 * container.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at https://www.youtube.com/watch?v=55wLsaWpQ4g.
 */
public class LayoutTransChanging extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final Button addButton =
                (Button) findViewById(R.id.addButton);
        final Button removeButton =
                (Button) findViewById(R.id.removeButton);
        final LinearLayout container =
                (LinearLayout) findViewById(R.id.container);
        final Context context = this;

        // Start with two views
        for (int i = 0; i < 2; ++i) {
            container.addView(new ColoredView(this));
        }

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Adding a view will cause a LayoutTransition animation
                container.addView(new ColoredView(context), 1);
            }
        });

        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (container.getChildCount() > 0) {
                    // Removing a view will cause a LayoutTransition animation
                    container.removeViewAt(Math.min(1, container.getChildCount() - 1));
                }
            }
        });

        // Note that this assumes a LayoutTransition is set on the container, which is the
        // case here because the container has the attribute "animateLayoutChanges" set to true
        // in the layout file. You can also call setLayoutTransition(new LayoutTransition()) in
        // code to set a LayoutTransition on any container.
        LayoutTransition transition = container.getLayoutTransition();

        // New capability as of Jellybean; monitor the container for *all* layout changes
        // (not just add/remove/visibility changes) and animate these changes as well.
        transition.enableTransitionType(LayoutTransition.CHANGING);
    }

    /**
     * Custom view painted with a random background color and two different sizes which are
     * toggled between due to user interaction.
     */
    private static class ColoredView extends View {

        private boolean mExpanded = false;

        private LayoutParams mCompressedParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 50);

        private LayoutParams mExpandedParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 200);

        private ColoredView(Context context) {
            super(context);
            int red = (int)(Math.random() * 128 + 127);
            int green = (int)(Math.random() * 128 + 127);
            int blue = (int)(Math.random() * 128 + 127);
            int color = 0xff << 24 | (red << 16) |
                    (green << 8) | blue;
            setBackgroundColor(color);
            setLayoutParams(mCompressedParams);
            setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Size changes will cause a LayoutTransition animation if the CHANGING
                    // transition is enabled
                    setLayoutParams(mExpanded ? mCompressedParams : mExpandedParams);
                    mExpanded = !mExpanded;
                    requestLayout();
                }
            });
        }
    }
}
