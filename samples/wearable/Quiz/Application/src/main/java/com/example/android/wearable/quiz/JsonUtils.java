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

package com.example.android.wearable.quiz;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

final class JsonUtils {
    public static final String JSON_FIELD_QUESTIONS = "questions";
    public static final String JSON_FIELD_QUESTION = "question";
    public static final String JSON_FIELD_ANSWERS = "answers";
    public static final String JSON_FIELD_CORRECT_INDEX = "correctIndex";
    public static final int NUM_ANSWER_CHOICES = 4;

    private JsonUtils() {
    }

    public static JSONObject loadJsonFile(Context context, String fileName) throws IOException,
            JSONException {
        InputStream is = context.getAssets().open(fileName);
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        String jsonString = new String(buffer);
        return new JSONObject(jsonString);
    }
}
