/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.example.android.intentplayground;

import android.content.ComponentName;
import androidx.annotation.ColorRes;
import java.util.HashMap;
import java.util.Map;

/**
 * Assigns colors to given tasks and activities.
 */
public class ColorManager {
    private static Map<String, Integer> mActivityColorMap = new HashMap<>();
    private static Map<Integer, Integer> mTaskColorMap = new HashMap<>();
    private static int[] mColors = new int[] {
            R.color.md_red_500,
            R.color.md_pink_500,
            R.color.md_purple_500,
            R.color.md_deep_purple_500,
            R.color.md_indigo_500,
            R.color.md_blue_500,
            R.color.md_light_blue_500,
            R.color.md_cyan_500,
            R.color.md_green_500,
            R.color.md_light_green_500,
            R.color.md_lime_500,
            R.color.md_yellow_500,
            R.color.md_amber_500,
            R.color.md_orange_500,
            R.color.md_deep_orange_500,
            R.color.md_brown_500,
            R.color.md_blue_grey_500
    };
    private static int mLastTaskColor = -1;
    private static int mLastActivityColor = -1;

    /**
     * Retrieves the assigned color for the given activity.
     * @param activity The activity to retrieve a color for.
     * @return The corresponding color for this activity.
     */
    public static @ColorRes int getColorForActivity(ComponentName activity) {
        String className = activity.getClassName();
        if (mActivityColorMap.containsKey(className)) {
            return mActivityColorMap.get(className);
        } else {
            int newColor = nextActivityColor();
            mActivityColorMap.put(className, newColor);
            return newColor;
        }
    }

    /**
     * Retrieves the assigned color for the given task.
     * @param taskPersistentId The ID of the task to retrieve a color for.
     * @return The corresponding color for this task.
     */
    public static @ColorRes int getColorForTask(int taskPersistentId) {
        if (mTaskColorMap.containsKey(taskPersistentId)) {
            return mTaskColorMap.get(taskPersistentId);
        } else {
            int newColor = nextTaskColor();
            mTaskColorMap.put(taskPersistentId, newColor);
            return newColor;
        }
    }

    private static @ColorRes int nextTaskColor() {
        return mColors[++mLastTaskColor % mColors.length];
    }

    private static @ColorRes int nextActivityColor() {
        return mColors[++mLastActivityColor % mColors.length];
    }
}
