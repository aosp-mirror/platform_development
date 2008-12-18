/* 
 * Copyright (C) 2008 Google Inc.
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

package com.android.samples.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.example.android.apis.R;

import java.util.ArrayList;

public class VoiceRecognition extends Activity {
    
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
    
    private ListView mList;

    /**
     * Called with the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate our UI from its XML layout description.
        setContentView(R.layout.voice_recognition);

        // Get display items for later interaction
        Button speakButton = (Button) findViewById(R.id.btn_speak);
        
        mList = (ListView) findViewById(R.id.list);

        // Attach actions to buttons
        speakButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                startVoiceRecognitionActivity();
            }
        });
    }

    void startVoiceRecognitionActivity() {
        Intent intent = new Intent("android.intent.action.VOICE_RECOGNITION");
        this.startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        
        android.util.Log.d("mike", "**************  onActivityResult" + data );
        
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String>matches = data.getStringArrayListExtra("queries");
            
            
            android.util.Log.d("mike", "**************  onActivityResult" + matches);
            
            mList.setAdapter(new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1, matches));
        }
        
        super.onActivityResult(requestCode, resultCode, data);
    }

}
