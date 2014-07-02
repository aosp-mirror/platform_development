/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.wearable.recipeassistant;

public final class Constants {
    private Constants() {
    }
    public static final String RECIPE_LIST_FILE = "recipelist.json";
    public static final String RECIPE_NAME_TO_LOAD = "recipe_name";

    public static final String RECIPE_FIELD_LIST = "recipe_list";
    public static final String RECIPE_FIELD_IMAGE = "img";
    public static final String RECIPE_FIELD_INGREDIENTS = "ingredients";
    public static final String RECIPE_FIELD_NAME = "name";
    public static final String RECIPE_FIELD_SUMMARY = "summary";
    public static final String RECIPE_FIELD_STEPS = "steps";
    public static final String RECIPE_FIELD_TEXT = "text";
    public static final String RECIPE_FIELD_TITLE = "title";
    public static final String RECIPE_FIELD_STEP_TEXT = "step_text";
    public static final String RECIPE_FIELD_STEP_IMAGE = "step_image";

    static final String ACTION_START_COOKING =
            "com.example.android.wearable.recipeassistant.START_COOKING";
    public static final String EXTRA_RECIPE = "recipe";

    public static final int NOTIFICATION_ID = 0;
    public static final int NOTIFICATION_IMAGE_WIDTH = 280;
    public static final int NOTIFICATION_IMAGE_HEIGHT = 280;
}
