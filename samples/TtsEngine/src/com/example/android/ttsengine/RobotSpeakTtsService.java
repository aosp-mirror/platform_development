/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.example.android.ttsengine;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.speech.tts.SynthesisCallback;
import android.speech.tts.SynthesisRequest;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeechService;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * A text to speech engine that generates "speech" that a robot might understand.
 * The engine supports two different "languages", each with their own frequency
 * mappings.
 *
 * It exercises all aspects of the Text to speech engine API
 * {@link android.speech.tts.TextToSpeechService}.
 */
public class RobotSpeakTtsService extends TextToSpeechService {
    private static final String TAG = "ExampleTtsService";

    /*
     * This is the sampling rate of our output audio. This engine outputs
     * audio at 16khz 16bits per sample PCM audio.
     */
    private static final int SAMPLING_RATE_HZ = 16000;

    /*
     * We multiply by a factor of two since each sample contains 16 bits (2 bytes).
     */
    private final byte[] mAudioBuffer = new byte[SAMPLING_RATE_HZ * 2];

    private Map<Character, Integer> mFrequenciesMap;
    private volatile String[] mCurrentLanguage = null;
    private volatile boolean mStopRequested = false;
    private SharedPreferences mSharedPrefs = null;

    @Override
    public void onCreate() {
        super.onCreate();
        mSharedPrefs = getSharedPreferences(GeneralSettingsFragment.SHARED_PREFS_NAME,
                Context.MODE_PRIVATE);
        // We load the default language when we start up. This isn't strictly
        // required though, it can always be loaded lazily on the first call to
        // onLoadLanguage or onSynthesizeText. This a tradeoff between memory usage
        // and the latency of the first call.
        onLoadLanguage("eng", "usa", "");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected String[] onGetLanguage() {
        // Note that mCurrentLanguage is volatile because this can be called from
        // multiple threads.
        return mCurrentLanguage;
    }

    @Override
    protected int onIsLanguageAvailable(String lang, String country, String variant) {
        // The robot speak synthesizer supports only english.
        if ("eng".equals(lang)) {
            // We support two specific robot languages, the british robot language
            // and the american robot language.
            if ("USA".equals(country) || "GBR".equals(country)) {
                // If the engine supported a specific variant, we would have
                // something like.
                //
                // if ("android".equals(variant)) {
                //     return TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE;
                // }
                return TextToSpeech.LANG_COUNTRY_AVAILABLE;
            }

            // We support the language, but not the country.
            return TextToSpeech.LANG_AVAILABLE;
        }

        return TextToSpeech.LANG_NOT_SUPPORTED;
    }

    /*
     * Note that this method is synchronized, as is onSynthesizeText because
     * onLoadLanguage can be called from multiple threads (while onSynthesizeText
     * is always called from a single thread only).
     */
    @Override
    protected synchronized int onLoadLanguage(String lang, String country, String variant) {
        final int isLanguageAvailable = onIsLanguageAvailable(lang, country, variant);

        if (isLanguageAvailable == TextToSpeech.LANG_NOT_SUPPORTED) {
            return isLanguageAvailable;
        }

        String loadCountry = country;
        if (isLanguageAvailable == TextToSpeech.LANG_AVAILABLE) {
            loadCountry = "USA";
        }

        // If we've already loaded the requested language, we can return early.
        if (mCurrentLanguage != null) {
            if (mCurrentLanguage[0].equals(lang) && mCurrentLanguage[1].equals(country)) {
                return isLanguageAvailable;
            }
        }

        Map<Character, Integer> newFrequenciesMap = null;
        try {
            InputStream file = getAssets().open(lang + "-" + loadCountry + ".freq");
            newFrequenciesMap = buildFrequencyMap(file);
            file.close();
        } catch (IOException e) {
            Log.e(TAG, "Error loading data for : " + lang + "-" + country);
        }

        mFrequenciesMap = newFrequenciesMap;
        mCurrentLanguage = new String[] { lang, loadCountry, ""};

        return isLanguageAvailable;
    }

    @Override
    protected void onStop() {
        mStopRequested = true;
    }

    @Override
    protected synchronized void onSynthesizeText(SynthesisRequest request,
            SynthesisCallback callback) {
        // Note that we call onLoadLanguage here since there is no guarantee
        // that there would have been a prior call to this function.
        int load = onLoadLanguage(request.getLanguage(), request.getCountry(),
                request.getVariant());

        // We might get requests for a language we don't support - in which case
        // we error out early before wasting too much time.
        if (load == TextToSpeech.LANG_NOT_SUPPORTED) {
            callback.error();
            return;
        }

        // At this point, we have loaded the language we need for synthesis and
        // it is guaranteed that we support it so we proceed with synthesis.

        // We denote that we are ready to start sending audio across to the
        // framework. We use a fixed sampling rate (16khz), and send data across
        // in 16bit PCM mono.
        callback.start(SAMPLING_RATE_HZ,
                AudioFormat.ENCODING_PCM_16BIT, 1 /* Number of channels. */);

        // We then scan through each character of the request string and
        // generate audio for it.
        final String text = request.getText().toLowerCase();
        for (int i = 0; i < text.length(); ++i) {
            char value = normalize(text.charAt(i));
            // It is crucial to call either of callback.error() or callback.done() to ensure
            // that audio / other resources are released as soon as possible.
            if (!generateOneSecondOfAudio(value, callback)) {
                callback.error();
                return;
            }
        }

        // Alright, we're done with our synthesis - yay!
        callback.done();
    }

    /*
     * Normalizes a given character to the range 'a' - 'z' (inclusive). Our
     * frequency mappings contain frequencies for each of these characters.
     */
    private static char normalize(char input) {
        if (input == ' ') {
            return input;
        }

        if (input < 'a') {
            return 'a';
        }
        if (input > 'z') {
            return 'z';
        }

        return input;
    }

    private Map<Character, Integer> buildFrequencyMap(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = null;
        Map<Character, Integer> map = new HashMap<Character, Integer>();
        try {
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length != 2) {
                    throw new IOException("Invalid line encountered: " + line);
                }
                map.put(parts[0].charAt(0), Integer.parseInt(parts[1]));
            }
            map.put(' ', 0);
            return map;
        } finally {
            is.close();
        }
    }

    private boolean generateOneSecondOfAudio(char alphabet, SynthesisCallback cb) {
        ByteBuffer buffer = ByteBuffer.wrap(mAudioBuffer).order(ByteOrder.LITTLE_ENDIAN);

        // Someone called onStop, end the current synthesis and return.
        // The mStopRequested variable will be reset at the beginning of the
        // next synthesis.
        //
        // In general, a call to onStop( ) should make a best effort attempt
        // to stop all processing for the *current* onSynthesizeText request (if
        // one is active).
        if (mStopRequested) {
            return false;
        }


        if (mFrequenciesMap == null || !mFrequenciesMap.containsKey(alphabet)) {
            return false;
        }

        final int frequency = mFrequenciesMap.get(alphabet);

        if (frequency > 0) {
            // This is the wavelength in samples. The frequency is chosen so that the
            // waveLength is always a multiple of two and frequency divides the
            // SAMPLING_RATE exactly.
            final int waveLength = SAMPLING_RATE_HZ / frequency;
            final int times = SAMPLING_RATE_HZ / waveLength;

            for (int j = 0; j < times; ++j) {
                // For a square curve, half of the values will be at Short.MIN_VALUE
                // and the other half will be Short.MAX_VALUE.
                for (int i = 0; i < waveLength / 2; ++i) {
                    buffer.putShort((short)(getAmplitude() * -1));
                }
                for (int i = 0; i < waveLength / 2; ++i) {
                    buffer.putShort(getAmplitude());
                }
            }
        } else {
            // Play a second of silence.
            for (int i = 0; i < mAudioBuffer.length / 2; ++i) {
                buffer.putShort((short) 0);
            }
        }

        // Get the maximum allowed size of data we can send across in audioAvailable.
        final int maxBufferSize = cb.getMaxBufferSize();
        int offset = 0;
        while (offset < mAudioBuffer.length) {
            int bytesToWrite = Math.min(maxBufferSize, mAudioBuffer.length - offset);
            cb.audioAvailable(mAudioBuffer, offset, bytesToWrite);
            offset += bytesToWrite;
        }
        return true;
    }

    private short getAmplitude() {
        boolean whisper = mSharedPrefs.getBoolean(GeneralSettingsFragment.WHISPER_KEY, false);
        return (short) (whisper ? 2048 : 8192);
    }
}
