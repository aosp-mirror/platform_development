/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.example.android.apis.view;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import com.example.android.apis.R;


/**
 * Sample creating 10 webviews.
 */
public class WebView1 extends Activity {
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        setContentView(R.layout.webview_1);
        
        final String mimeType = "text/html";
        
        WebView wv;
        
        wv = (WebView) findViewById(R.id.wv1);
        wv.loadData("<a href='x'>Hello World! - 1</a>", mimeType, null);
        
        wv = (WebView) findViewById(R.id.wv2);
        wv.loadData("<a href='x'>Hello World! - 2</a>", mimeType, null);
        
        wv = (WebView) findViewById(R.id.wv3);
        wv.loadData("<a href='x'>Hello World! - 3</a>", mimeType, null);
        
        wv = (WebView) findViewById(R.id.wv4);
        wv.loadData("<a href='x'>Hello World! - 4</a>", mimeType, null);
        
        wv = (WebView) findViewById(R.id.wv5);
        wv.loadData("<a href='x'>Hello World! - 5</a>", mimeType, null);
        
        wv = (WebView) findViewById(R.id.wv6);
        wv.loadData("<a href='x'>Hello World! - 6</a>", mimeType, null);
        
        wv = (WebView) findViewById(R.id.wv7);
        wv.loadData("<a href='x'>Hello World! - 7</a>", mimeType, null);
        
        wv = (WebView) findViewById(R.id.wv8);
        wv.loadData("<a href='x'>Hello World! - 8</a>", mimeType, null);
        
        wv = (WebView) findViewById(R.id.wv9);
        wv.loadData("<a href='x'>Hello World! - 9</a>", mimeType, null);
        
        wv = (WebView) findViewById(R.id.wv10);
        wv.loadData("<a href='x'>Hello World! - 10</a>", mimeType, null);
    }
}
