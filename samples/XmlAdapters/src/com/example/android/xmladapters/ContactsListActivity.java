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

package com.example.android.xmladapters;

import android.app.ListActivity;
import android.os.Bundle;

/**
 * This activity demonstrates how to create a complex UI using a ListView
 * and an adapter defined in XML.
 * 
 * The following activity shows a list of contacts, their starred status
 * and their photos, using the adapter defined in res/xml.
 */
public class ContactsListActivity extends ListActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.contacts_list);
        setListAdapter(Adapters.loadAdapter(this, R.xml.contacts));
    }
}
