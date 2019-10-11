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
package com.android.dumpviewer.pickers;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;

import com.android.dumpviewer.R;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class PickerActivity extends AppCompatActivity {
    protected abstract String[] getList();

    private static final String KEY_SELECTION = "KEY_SELECTION";

    private AutoCompleteTextView mSearch; // TODO Save history? Would that be really useful?

    private ArrayAdapter<String> mAdapter;
    private ListView mList;

    private String[] mAllItems;
    private String mFilter = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.picker);

        mSearch = findViewById(R.id.search);

        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line);

        mList = findViewById(R.id.list);
        mList.setAdapter(mAdapter);

        mSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mFilter = s.toString();
                refreshList();
            }
        });
        mList.setOnItemClickListener(this::onListItemClicked);

        mLoader.execute();
    }

    @Override
    protected void onDestroy() {
        mLoader.cancel(false);
        super.onDestroy();
    }

    public static String getSelectedString(Intent i) {
        return i.getStringExtra(KEY_SELECTION);
    }

    private String getSelectedItem(int position) {
        final String val = mAdapter.getItem(position).toString();
        final int sepIndex = val.indexOf(' ');
        if (sepIndex >= 0) {
            return val.substring(0, sepIndex);
        }
        return val;
    }

    private void onListItemClicked(AdapterView<?> parent, View view, int position, long id) {
        final Intent result = new Intent().putExtra(KEY_SELECTION, getSelectedItem(position));
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    private final AsyncTask<Void, Void, String[]> mLoader = new AsyncTask<Void, Void, String[]>() {
        @Override
        protected String[] doInBackground(Void... voids) {
            return getList();
        }

        @Override
        protected void onPostExecute(String[] list) {
            if (list != null) {
                mAllItems = list;
                refreshList();
            }
        }
    };

    private void refreshList() {
        if (isDestroyed() || mAllItems == null) {
            return;
        }
        final ArrayList<String> list = new ArrayList<>(mAllItems.length);

        final String filter = mFilter.toLowerCase();
        for (String s : mAllItems) {
            if (filter.length() == 0 || s.toLowerCase().contains(filter)) {
                list.add(s);
            }
        }
        mAdapter.clear();
        mAdapter.addAll(list);
    }
}
