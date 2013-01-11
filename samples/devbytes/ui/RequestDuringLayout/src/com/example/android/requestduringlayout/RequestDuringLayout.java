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

package com.example.android.requestduringlayout;

import com.android.requestduringlayout.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * This example shows what horrible things can result from calling requestLayout() during
 * a layout pass. DON'T DO THIS.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at https://www.youtube.com/watch?v=HbAeTGoKG6k.
 */
public class RequestDuringLayout extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_during_layout);

        final MyLayout myLayout = (MyLayout) findViewById(R.id.container);
        Button addViewButton = (Button) findViewById(R.id.addView);
        Button removeViewButton = (Button) findViewById(R.id.removeView);
        Button forceLayoutButton = (Button) findViewById(R.id.forceLayout);

        addViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myLayout.mAddRequestPending = true;
                myLayout.requestLayout();
            }
        });

        removeViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myLayout.mRemoveRequestPending = true;
                myLayout.requestLayout();
            }
        });

        forceLayoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myLayout.requestLayout();
            }
        });

    }

    /**
     * Custom layout to enable the convoluted way of requesting-during-layout that we're
     * trying to show here. Yes, it's a hack. But it's a case that many apps hit (in much more
     * complicated and less demoable ways), so it's interesting to at least understand the
     * artifacts that come from this sequence of events.
     */
    static class MyLayout extends LinearLayout {

        int numButtons = 0;
        boolean mAddRequestPending = false;
        boolean mRemoveRequestPending = false;

        public MyLayout(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        public MyLayout(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public MyLayout(Context context) {
            super(context);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
            // Here is the root of the problem: we are adding/removing views during layout. This
            // means that this view and its container will be put into an uncertain state that
            // can be difficult to discover and recover from.
            // Better approach: just add/remove at a time when layout is not running, certainly not
            // in the middle of onLayout(), or other layout-associated logic.
            if (mRemoveRequestPending) {
                removeButton();
                mRemoveRequestPending = false;
            }
            if (mAddRequestPending) {
                addButton();
                mAddRequestPending = false;
            }
        }

        private void removeButton() {
            if (getChildCount() > 1) {
                removeViewAt(1);
            }
        }

        private void addButton() {
            Button button = new Button(getContext());
            button.setLayoutParams(new LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            button.setText("Button " + (numButtons++));
            addView(button);
        }

    }

}
