/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.directshare;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * The dialog for selecting a contact to share the text with. This dialog is shown when the user
 * taps on this sample's icon rather than any of the Direct Share contacts.
 */
public class SelectContactActivity extends Activity {

    /**
     * The action string for Intents.
     */
    public static final String ACTION_SELECT_CONTACT
            = "com.example.android.directshare.intent.action.SELECT_CONTACT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_contact);
        Intent intent = getIntent();
        if (!ACTION_SELECT_CONTACT.equals(intent.getAction())) {
            finish();
            return;
        }
        // Set up the list of contacts
        ListView list = (ListView) findViewById(R.id.list);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(mOnItemClickListener);
    }

    private final ListAdapter mAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return Contact.CONTACTS.length;
        }

        @Override
        public Object getItem(int i) {
            return Contact.byId(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup parent) {
            if (view == null) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact, parent,
                        false);
            }
            TextView textView = (TextView) view;
            Contact contact = (Contact) getItem(i);
            ContactViewBinder.bind(contact, textView);
            return textView;
        }
    };

    private final AdapterView.OnItemClickListener mOnItemClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            Intent data = new Intent();
            data.putExtra(Contact.ID, i);
            setResult(RESULT_OK, data);
            finish();
        }
    };

}
