/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.example.android.aconfig.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/** Main landing page of Aconfig Demo app. */
public class AconfigDemoActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Button button1 = findViewById(R.id.button1);
        button1.setText("Launch Java Codelab");
        button1.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent =
                                new Intent(
                                        AconfigDemoActivity.this, AconfigJavaCodelabActivity.class);
                        startActivity(intent);
                    }
                });

        Button button2 = findViewById(R.id.button2);
        button2.setText("Launch Native Codelab");
        button2.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent =
                                new Intent(
                                        AconfigDemoActivity.this,
                                        AconfigNativeCodelabActivity.class);
                        startActivity(intent);
                    }
                });

        Button button3 = findViewById(R.id.button3);
        button3.setText("Launch Simple Demo");
        button3.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent =
                                new Intent(
                                        AconfigDemoActivity.this, AconfigSimpleDemoActivity.class);
                        startActivity(intent);
                    }
                });
    }
}
