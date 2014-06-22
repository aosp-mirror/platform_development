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
