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
import com.example.android.apis.Shakespeare;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Demonstration of using fragments to implement different activity layouts.
 * This sample provides a different layout (and activity flow) when run in
 * landscape.
 */
public class FragmentLayout extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_layout);
    }
    
    public static class DialogActivity extends Activity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            
            if (getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_LANDSCAPE) {
                // If the screen is now in landscape mode, we can show the
                // dialog in-line with the list so we don't need this activity.
                finish();
                return;
            }
            
            DialogFragment dialog = new DialogFragment();
            this.openFragmentTransaction().add(android.R.id.content, dialog).commit();
            dialog.setText(getIntent().getIntExtra("text", -1));
        }
    }
    
    public static class TitlesFragment extends Fragment
            implements AdapterView.OnItemClickListener {
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            ListView list = new ListView(getActivity());
            list.setDrawSelectorOnTop(false);
            list.setAdapter(new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_list_item_1, Shakespeare.TITLES));
            list.setOnItemClickListener(this);
            list.setId(android.R.id.list);  // set id to allow state save/restore.
            return list;
        }

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            DialogFragment frag = (DialogFragment)getActivity().findFragmentById(R.id.dialog);
            if (frag != null && frag.isVisible()) {
                frag.setText((int)id);
            } else {
                Intent intent = new Intent();
                intent.setClass(getActivity(), DialogActivity.class);
                intent.putExtra("text", (int)id);
                startActivity(intent);
            }
        }
    }
    
    public static class DialogFragment extends Fragment {
        int mDisplayedText = -1;
        TextView mText;
        
        public void setText(int id) {
            mDisplayedText = id;
            if (mText != null && id >= 0) {
                mText.setText(Shakespeare.DIALOGUE[id]);
            }
        }
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null) {
                mDisplayedText = savedInstanceState.getInt("text", -1);
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt("text", mDisplayedText);
        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            ScrollView scroller = new ScrollView(getActivity());
            mText = new TextView(getActivity());
            scroller.addView(mText);
            setText(mDisplayedText);
            return scroller;
        }
    }
}
