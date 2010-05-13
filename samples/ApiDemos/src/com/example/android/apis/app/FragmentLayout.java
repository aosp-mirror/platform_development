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

package com.example.android.apis.app;

import com.example.android.apis.R;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FragmentLayout extends Activity {
    View mFirstFragmentView;
    View mSecondFragmentView;
    
    Fragment mFirstFragment;
    Fragment mSecondFragment;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_layout);
        mFirstFragmentView = findViewById(R.id.fragment1);
        mSecondFragmentView = findViewById(R.id.fragment2);
        mFirstFragment = findFragmentById(R.id.fragment1);
        mSecondFragment = findFragmentById(R.id.fragment2);
    }
    
    static class FirstFragment extends Fragment {
        TextView mTextView;
        
        // Explicit constructor needed for inflation.
        public FirstFragment() {
        }
        
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.labeled_text_edit, container, false);
            View tv = v.findViewById(R.id.msg);
            ((TextView)tv).setText("The fragment saves and restores this text.");
            
            // Retrieve the text editor, and restore the last saved state if needed.
            mTextView = (TextView)v.findViewById(R.id.saved);
            if (savedInstanceState != null) {
                mTextView.setText(savedInstanceState.getCharSequence("text"));
            }
            return v;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            
            // Remember the current text, to restore if we later restart.
            outState.putCharSequence("text", mTextView.getText());
        }
    }
    
    static class SecondFragment extends Fragment {
        // Explicit constructor needed for inflation.
        public SecondFragment() {
        }
        
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.labeled_text_edit, container, false);
            View tv = v.findViewById(R.id.msg);
            ((TextView)tv).setText("The TextView saves and restores this text.");
            
            // Retrieve the text editor and tell it to save and restore its state.
            // Note that you will often set this in the layout XML, but since
            // we are sharing our layout with the other fragment we will customize
            // it here.
            ((TextView)v.findViewById(R.id.saved)).setSaveEnabled(true);
            return v;
        }
    }
}
