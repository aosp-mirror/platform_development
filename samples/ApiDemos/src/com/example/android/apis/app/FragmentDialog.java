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
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class FragmentDialog extends Activity {
    int mStackLevel = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_dialog);

        View tv = findViewById(R.id.text);
        ((TextView)tv).setText("Example of displaying dialogs with a DialogFragment.  "
                + "Press the show button below to see the first dialog; pressing "
                + "successive show buttons will display other dialog styles as a "
                + "stack, with dismissing or back going to the previous dialog.");

        // Watch for button clicks.
        Button button = (Button)findViewById(R.id.show);
        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showDialog();
            }
        });

        if (savedInstanceState != null) {
            mStackLevel = savedInstanceState.getInt("level");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("level", mStackLevel);
    }

    void showDialog() {
        mStackLevel++;
        DialogFragment newFragment = new MyDialogFragment(mStackLevel);
        FragmentTransaction ft = openFragmentTransaction();
        Fragment prev = findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        newFragment.show(this, ft, "dialog");
    }

    static int getStyleForNum(int num) {
        switch ((num-1)%6) {
            case 1: return DialogFragment.STYLE_NO_TITLE;
            case 2: return DialogFragment.STYLE_NO_FRAME;
            case 3: return DialogFragment.STYLE_NO_INPUT;
            case 4: return DialogFragment.STYLE_NORMAL;
            case 5: return DialogFragment.STYLE_NORMAL;
        }
        return DialogFragment.STYLE_NORMAL;
    }

    static int getThemeForNum(int num) {
        switch ((num-1)%6) {
            case 4: return android.R.style.Theme_Light;
            case 5: return android.R.style.Theme;
        }
        return 0;
    }

    static String getNameForNum(int num) {
        switch ((num-1)%6) {
            case 1: return "STYLE_NO_TITLE";
            case 2: return "STYLE_NO_FRAME";
            case 3: return "STYLE_NO_INPUT (this window can't receive input, so "
                    + "you will need to press the bottom show button)";
            case 4: return "STYLE_NORMAL with light fullscreen theme";
            case 5: return "STYLE_NORMAL with dark fullscreen theme";
        }
        return "STYLE_NORMAL";
    }

    public static class MyDialogFragment extends DialogFragment {
        int mNum;

        public MyDialogFragment() {
            mNum = -1;
        }

        public MyDialogFragment(int num) {
            super(getStyleForNum(num), getThemeForNum(num));
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
            View v = inflater.inflate(R.layout.fragment_dialog, container, false);
            View tv = v.findViewById(R.id.text);
            ((TextView)tv).setText("Dialog #" + mNum + ": using style "
                    + getNameForNum(mNum));

            // Watch for button clicks.
            Button button = (Button)v.findViewById(R.id.show);
            button.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    ((FragmentDialog)getActivity()).showDialog();
                }
            });

            return v;
        }
    }
}
