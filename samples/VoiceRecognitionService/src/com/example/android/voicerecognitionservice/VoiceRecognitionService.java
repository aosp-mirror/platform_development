/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.example.android.voicerecognitionservice;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionService;

/**
 * A sample implementation of a {@link RecognitionService}. This very simple implementation does
 * no actual voice recognition. It just immediately returns fake recognition results.
 * Depending on the setting chosen in {@link VoiceRecognitionSettings}, it either returns a
 * list of letters ("a", "b", "c"), or a list of numbers ("1", "2", "3").
 */
public class VoiceRecognitionService extends RecognitionService {

    @Override
    protected void onCancel(Callback listener) {
        // A real recognizer would do something to shut down recognition here.
    }

    @Override
    protected void onStartListening(Intent recognizerIntent, Callback listener) {
        // A real recognizer would probably utilize a lot of the other listener callback
        // methods. But we'll just skip all that and pretend we've got a result.
        ArrayList<String> results = new ArrayList<String>();
        
        SharedPreferences prefs = getSharedPreferences(
                VoiceRecognitionSettings.SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        
        String resultType = prefs.getString(
                VoiceRecognitionSettings.PREF_KEY_RESULTS_TYPE,
                String.valueOf(VoiceRecognitionSettings.RESULT_TYPE_LETTERS));
        int resultTypeInt = Integer.parseInt(resultType);
        
        if (resultTypeInt == VoiceRecognitionSettings.RESULT_TYPE_LETTERS) {
            results.add("a");
            results.add("b");
            results.add("c");            
        } else if (resultTypeInt == VoiceRecognitionSettings.RESULT_TYPE_NUMBERS) {
            results.add("1");
            results.add("2");
            results.add("3");
        }
        
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, results);
        
        try {
            listener.results(bundle);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onStopListening(Callback listener) {
        // Not implemented - in this sample we assume recognition would be endpointed
        // automatically, though certain applications may wish to expose an affordance
        // for stopping recording manually.
    }
    
}
