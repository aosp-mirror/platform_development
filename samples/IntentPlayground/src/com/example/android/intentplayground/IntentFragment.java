/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.example.android.intentplayground;

import android.app.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.List;
import java.util.Set;

/**
 * Displays details about the intent that launched the current activity.
 */
public class IntentFragment extends Fragment {
    private TextView mActionTextView, mUriTextView, mTypeTextView, mPackageTextView;
    private LinearLayout mCategoryListLayout, mFlagListLayout;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_intent, container,
                false /* attachToRoot */);
        // Get handles to views
        mActionTextView = layout.findViewById(R.id.intentAction);
        mUriTextView = layout.findViewById(R.id.intentUri);
        mTypeTextView = layout.findViewById(R.id.intentType);
        mPackageTextView = layout.findViewById(R.id.intentPackage);
        mCategoryListLayout = layout.findViewById(R.id.intentCategories);
        mFlagListLayout = layout.findViewById(R.id.intentFlags);
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity activity = getActivity();
        Intent intent = activity.getIntent();
        // Get intent info
        String action = intent.getAction();
        String dataUri = intent.getDataString();
        String intentType = intent.getType();
        String intentPackage = intent.getPackage();
        Set<String> categories = intent.getCategories();
        List<String> flags = FlagUtils.discoverFlags(intent);
        // set data
        mActionTextView.setText(action);
        mUriTextView.setText(dataUri);
        mTypeTextView.setText(intentType);
        mPackageTextView.setText(intentPackage);
        if (categories != null) {
            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(activity,
                    R.layout.simple_list_item,
                    categories.toArray(new String[0]));
            fillLayout(mCategoryListLayout, categoryAdapter);
        }
        ArrayAdapter<String> flagAdapter = new ArrayAdapter<>(activity,
                R.layout.simple_list_item, flags);
        fillLayout(mFlagListLayout, flagAdapter);
    }

    /**
     * Takes a @{link ViewGroup} and uses the given adapter to fill it; used in the cases where we
     * need a non-scrollable list that is a child of {@link android.widget.ScrollView}.
     * @param layout The layout to be filled.
     * @param adapter The adapter that provides the views for the layout.
     */
    private void fillLayout(ViewGroup layout, ArrayAdapter<?> adapter) {
        layout.removeAllViews();
        for (int i = 0; i < adapter.getCount(); i++) {
            layout.addView(adapter.getView(i, null /* convertView */, null /* parent */));
        }
    }
}
