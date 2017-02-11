/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.wearable.wear.wearnotifications;

import android.support.wearable.view.DefaultOffsettingHelper;
import android.support.wearable.view.WearableRecyclerView;
import android.view.View;

/**
 * Customizes all items (children) in a {@link WearableRecyclerView} to align to left side of
 * surface/watch and shrinks each item (child) as you scroll away from it.
 */
public class ScalingOffsettingHelper extends DefaultOffsettingHelper {

    // Max we scale the child View
    private static final float MAX_CHILD_SCALE = 0.65f;

    private float mProgressToCenter;

    public ScalingOffsettingHelper() {}

    // Shrinks icons/text and you scroll away
    @Override
    public void updateChild(View child, WearableRecyclerView parent) {
        super.updateChild(child, parent);

        // Figure out % progress from top to bottom
        float centerOffset = ((float) child.getHeight() / 2.0f) /  (float) parent.getHeight();
        float yRelativeToCenterOffset = (child.getY() / parent.getHeight()) + centerOffset;

        // Normalize for center
        mProgressToCenter = Math.abs(0.5f - yRelativeToCenterOffset);

        // Adjust to the maximum scale
        mProgressToCenter = Math.min(mProgressToCenter, MAX_CHILD_SCALE);

        child.setScaleX(1 - mProgressToCenter);
        child.setScaleY(1 - mProgressToCenter);
    }
}