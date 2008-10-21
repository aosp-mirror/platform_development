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

// Need the following import to get access to the app resources, since this
// class is in a sub-package.
import com.example.android.apis.R;


import android.app.ListActivity;
import android.database.Cursor;
import android.provider.Contacts.People;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * A list view example where the data comes from a cursor.
 */
public class List7 extends ListActivity implements OnItemSelectedListener {
    private static String[] PROJECTION = new String[] {
        People._ID, People.NAME, People.NUMBER
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_7);
        mPhone = (TextView) findViewById(R.id.phone);
        getListView().setOnItemSelectedListener(this);

        // Get a cursor with all people
        Cursor c = getContentResolver().query(People.CONTENT_URI, PROJECTION, null, null, null);
        startManagingCursor(c);
        mPhoneColumnIndex = c.getColumnIndex(People.NUMBER);

        ListAdapter adapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1, // Use a template
                                                        // that displays a
                                                        // text view
                c, // Give the cursor to the list adatper
                new String[] {People.NAME}, // Map the NAME column in the
                                            // people database to...
                new int[] {android.R.id.text1}); // The "text1" view defined in
                                            // the XML template
        setListAdapter(adapter);
    }

    public void onItemSelected(AdapterView parent, View v, int position, long id) {
        if (position >= 0) {
            Cursor c = (Cursor) parent.getItemAtPosition(position);
            mPhone.setText(c.getString(mPhoneColumnIndex));
        }
    }

    public void onNothingSelected(AdapterView parent) {
        mPhone.setText(R.string.list_7_nothing);

    }

    private int mPhoneColumnIndex;
    private TextView mPhone;
}
