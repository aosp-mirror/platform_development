/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.example.android.ttsengine;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * Checks if the voice data is present.
 */
public class CheckVoiceData extends Activity {
    private static final String TAG = "CheckVoiceData";

    private static final String[] SUPPORTED_LANGUAGES = { "eng-GBR", "eng-USA" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        List<String> checkLanguages = getCheckVoiceDataFor(intent);

        // If the call didn't specify which languages to check, check
        // for all the supported ones.
        if (checkLanguages.isEmpty()) {
            checkLanguages = Arrays.asList(SUPPORTED_LANGUAGES);
        }

        ArrayList<String> available = new ArrayList<String>();
        ArrayList<String> unavailable = new ArrayList<String>();

        for (String lang : checkLanguages) {
            // This check is required because checkLanguages might contain
            // an arbitrary list of languages if the intent specified them
            // {@link #getCheckVoiceDataFor}.
            if (isLanguageSupported(lang)) {
                if (isDataInstalled(lang)) {
                    available.add(lang);
                } else {
                    unavailable.add(lang);
                }
            }
        }

        int result;
        if (!checkLanguages.isEmpty() && available.isEmpty()) {
            // No voices available at all.
            result = TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL;
        } else if (!unavailable.isEmpty()) {
            // Some voices are available, but some have missing
            // data.
            result = TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_DATA;
        } else {
            // All voices are available.
            result = TextToSpeech.Engine.CHECK_VOICE_DATA_PASS;
        }

        // We now return the list of available and unavailable voices
        // as well as the return code.
        Intent returnData = new Intent();
        returnData.putStringArrayListExtra(
                TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES, available);
        returnData.putStringArrayListExtra(
                TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES, unavailable);
        setResult(result, returnData);
        finish();
    }

    /**
     * The intent that launches this activity can contain an intent extra
     * {@link TextToSpeech.Engine.EXTRA_CHECK_VOICE_DATA_FOR} that might specify
     * a given language to check voice data for. If the intent does not contain
     * this extra, we assume that a voice check for all supported languages
     * was requested.
     */
    private List<String> getCheckVoiceDataFor(Intent intent) {
        ArrayList<String> list = intent.getStringArrayListExtra(
                TextToSpeech.Engine.EXTRA_CHECK_VOICE_DATA_FOR);
        ArrayList<String> ret = new ArrayList<String>();
        if (list != null) {
            for (String lang : list) {
                if (!TextUtils.isEmpty(lang)) {
                    ret.add(lang);
                }
            }
        }
        return ret;
    }

    /**
     * Checks whether a given language is in the list of supported languages.
     */
    private boolean isLanguageSupported(String input) {
        for (String lang : SUPPORTED_LANGUAGES) {
            if (lang.equals(input)) {
                return true;
            }
        }

        return false;
    }

    /*
     * Note that in our example, all data is packaged in our APK as
     * assets (it could be a raw resource as well). This check is unnecessary
     * because it will always succeed.
     *
     * If for example, engine data was downloaded or installed on external storage,
     * this check would make much more sense.
     */
    private boolean isDataInstalled(String lang) {
        try {
            InputStream is = getAssets().open(lang + ".freq");

            if (is != null) {
                is.close();
            } else {
                return false;
            }
        } catch (IOException e) {
            Log.w(TAG, "Unable to find data for: " + lang + ", exception: " + e);
            return false;
        }

        // The asset InputStream was non null, and therefore this
        // data file is available.
        return true;
    }
}
