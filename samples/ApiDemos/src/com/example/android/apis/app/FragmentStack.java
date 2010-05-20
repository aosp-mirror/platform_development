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
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class FragmentStack extends Activity {
    int mStackLevel = 1;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_stack);
        
        // Watch for button clicks.
        Button button = (Button)findViewById(R.id.next);
        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                addFragmentToStack();
            }
        });
        
        if (savedInstanceState == null) {
            // Do first time initialization -- add initial fragment. 
            Fragment newFragment = new CountingFragment(mStackLevel);
            FragmentTransaction ft = openFragmentTransaction();
            ft.add(R.id.simple_fragment, newFragment).commit();
        } else {
            mStackLevel = savedInstanceState.getInt("level");
        }
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("level", mStackLevel);
    }

    void addFragmentToStack() {
        mStackLevel++;
        Fragment newFragment = new CountingFragment(mStackLevel);
        FragmentTransaction ft = openFragmentTransaction();
        ft.replace(R.id.simple_fragment, newFragment);
        ft.setTransition(FragmentTransaction.TRANSIT_WALLPAPER_INTRA_OPEN);
        ft.addToBackStack(null);
        ft.commit();
    }
    
    public static class CountingFragment extends Fragment {
        int mNum;
        
        public CountingFragment() {
            mNum = -1;
        }
        
        public CountingFragment(int num) {
            mNum = num;
        }
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null) {
                mNum = savedInstanceState.getInt("num");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt("num", mNum);
        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.hello_world, container, false);
            View tv = v.findViewById(R.id.text);
            ((TextView)tv).setText("Fragment #" + mNum);
            return v;
        }
    }
}
