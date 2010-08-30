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

import android.app.ExpandableListActivity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.widget.ExpandableListAdapter;
import android.widget.SimpleCursorTreeAdapter;

/**
 * Demonstrates expandable lists backed by Cursors
 */
public class ExpandableList2 extends ExpandableListActivity {
    private static final int COLUMN_CONTACT_ID = 0;

    private static final String[] CONTACT_PROJECTION = new String[] {
        Contacts._ID,
        Contacts.DISPLAY_NAME
    };

    private static final String[] PHONE_PROJECTION = new String[] {
        Phone._ID,
        Phone.CONTACT_ID,
        Phone.NUMBER
    };

    private ExpandableListAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Query for people
        Cursor groupCursor = managedQuery(Contacts.CONTENT_URI,
                CONTACT_PROJECTION, null, null, null);

        // Set up our adapter
        mAdapter = new MyExpandableListAdapter(groupCursor,
                this,
                android.R.layout.simple_expandable_list_item_1,
                android.R.layout.simple_expandable_list_item_1,
                new String[] {Contacts.DISPLAY_NAME}, // Name for group layouts
                new int[] {android.R.id.text1},
                new String[] {Phone.NUMBER}, // Number for child layouts
                new int[] {android.R.id.text1});
        setListAdapter(mAdapter);
    }

    public class MyExpandableListAdapter extends SimpleCursorTreeAdapter {

        public MyExpandableListAdapter(Cursor cursor, Context context, int groupLayout,
                int childLayout, String[] groupFrom, int[] groupTo, String[] childrenFrom,
                int[] childrenTo) {
            super(context, cursor, groupLayout, groupFrom, groupTo, childLayout, childrenFrom,
                    childrenTo);
        }

        @Override
        protected Cursor getChildrenCursor(Cursor groupCursor) {
            int contactId = groupCursor.getInt(COLUMN_CONTACT_ID);
            // The returned Cursor MUST be managed by us, so we use Activity's helper
            // functionality to manage it for us.
            return managedQuery(Phone.CONTENT_URI,
                    PHONE_PROJECTION,
                    Phone.CONTACT_ID + " = " + contactId,
                    null, null);
        }
    }
}