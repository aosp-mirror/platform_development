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
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts.People;
import android.widget.ExpandableListAdapter;
import android.widget.SimpleCursorTreeAdapter;


/**
 * Demonstrates expandable lists backed by Cursors
 */
public class ExpandableList2 extends ExpandableListActivity {
    private int mGroupIdColumnIndex; 
    
    private String mPhoneNumberProjection[] = new String[] {
            People.Phones._ID, People.Phones.NUMBER
    };

    
    private ExpandableListAdapter mAdapter;
    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Query for people
        Cursor groupCursor = managedQuery(People.CONTENT_URI,
                new String[] {People._ID, People.NAME}, null, null, null);

        // Cache the ID column index
        mGroupIdColumnIndex = groupCursor.getColumnIndexOrThrow(People._ID);

        // Set up our adapter
        mAdapter = new MyExpandableListAdapter(groupCursor,
                this,
                android.R.layout.simple_expandable_list_item_1,
                android.R.layout.simple_expandable_list_item_1,
                new String[] {People.NAME}, // Name for group layouts
                new int[] {android.R.id.text1},
                new String[] {People.NUMBER}, // Number for child layouts
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
            // Given the group, we return a cursor for all the children within that group 

            // Return a cursor that points to this contact's phone numbers
            Uri.Builder builder = People.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, groupCursor.getLong(mGroupIdColumnIndex));
            builder.appendEncodedPath(People.Phones.CONTENT_DIRECTORY);
            Uri phoneNumbersUri = builder.build();

            // The returned Cursor MUST be managed by us, so we use Activity's helper
            // functionality to manage it for us.
            return managedQuery(phoneNumbersUri, mPhoneNumberProjection, null, null, null);
        }

    }
}
