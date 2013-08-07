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

package com.example.android.slidingfragments;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * In order to animate the fragment containing text on/off the screen,
 * it is required that we know the height of the device being used. However,
 * this can only be determined at runtime, so we cannot specify the required
 * translation in an xml file. Since FragmentTransaction's setCustomAnimations
 * method requires an ID of an animation defined via an xml file, this linear
 * layout was built as a workaround. This custom linear layout is created to specify
 * the location of the fragment's layout as a fraction of the device's height. By
 * animating yFraction from 0 to 1, we can animate the fragment from the top of
 * the screen to the bottom of the screen, regardless of the device's specific size.
 */
public class FractionalLinearLayout extends LinearLayout {

    private float mYFraction;
    private int mScreenHeight;

    public FractionalLinearLayout(Context context) {
        super(context);
    }

    public FractionalLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mScreenHeight = h;
        setY(mScreenHeight);
    }

    public float getYFraction() {
        return mYFraction;
    }

    public void setYFraction(float yFraction) {
        mYFraction = yFraction;
        setY((mScreenHeight > 0) ? (mScreenHeight - yFraction * mScreenHeight) : 0);
    }
}
