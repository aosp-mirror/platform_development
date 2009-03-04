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

package com.example.codelab.rssexample;

import android.app.Activity;
import android.content.Intent;
import android.view.View.OnClickListener;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.text.TextUtils;
import android.os.Bundle;

/*** Form to add a new RSS feed.
 It is a dialog form,**/ 
public class AddRssItem extends Activity {
    
    // Button handler for Submit/Cancel.
    // It is a dialog style form because it's declared as such
    // in the manifest.
    private OnClickListener mClickListener = new OnClickListener(){
        public void onClick(View v){
            if(v.getId() == R.id.submit){
                String title = ((TextView) findViewById(R.id.title_textbox)).getText().toString();
                String url = ((TextView) findViewById(R.id.url_textbox)).getText().toString();
                if(TextUtils.isEmpty(title) || TextUtils.isEmpty(url)){
                    showAlert("Missing Values", 
                              "You must specify both a title and a URL value", 
                              "OK", 
                              null, false, null);
                    return;
                }
                Intent res = new Intent("Accepted");
                res.putExtra(RssContentProvider.TITLE, title);
                res.putExtra(RssContentProvider.URL, url);
                res.putExtra(RssContentProvider.LAST_UPDATED, 0);
                res.putExtra(RssContentProvider.CONTENT, "<html><body><h2>Not updated yet.</h2></body></html>");
                setResult(RESULT_OK, res);
            }
            else
                setResult(RESULT_CANCELED, (new Intent()).setAction("Canceled" + v.getId()));
            
            finish();
            
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_item);
        setTitle(getString(R.string.add_item_label));
        
        Button btn = (Button) findViewById(R.id.cancel);
        btn.setOnClickListener(mClickListener);
        
        btn = (Button) findViewById(R.id.submit);
        btn.setOnClickListener(mClickListener);       
    }

}
