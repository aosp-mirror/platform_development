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

import com.example.android.apis.R;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
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
    private static final String[] PROJECTION = new String[] {
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.HAS_PHONE_NUMBER,
            ContactsContract.Contacts.LOOKUP_KEY
    };

    private int mIdColumnIndex;
    private int mHasPhoneColumnIndex;

    private TextView mPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.list_7);

        mPhone = (TextView) findViewById(R.id.phone);
        getListView().setOnItemSelectedListener(this);

        // Get a cursor with all people
        Cursor c = managedQuery(ContactsContract.Contacts.CONTENT_URI,
                PROJECTION, null, null, null);
        mIdColumnIndex = c.getColumnIndex(ContactsContract.Contacts._ID);
        mHasPhoneColumnIndex = c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);

        ListAdapter adapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1, // Use a template
                                                     // that displays a
                                                     // text view
                c, // Give the cursor to the list adapter
                new String[] { ContactsContract.Contacts.DISPLAY_NAME }, // Map the NAME column in the
                                                                         // people database to...
                new int[] { android.R.id.text1 }); // The "text1" view defined in
                                                   // the XML template
        setListAdapter(adapter);
    }

    public void onItemSelected(AdapterView parent, View v, int position, long id) {
        if (position >= 0) {
            final Cursor c = (Cursor) parent.getItemAtPosition(position);
            if (c.getInt(mHasPhoneColumnIndex) > 0) {
                final long contactId = c.getLong(mIdColumnIndex);
                final Cursor phones = getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[] { ContactsContract.CommonDataKinds.Phone.NUMBER },
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactId, null,
                        ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY + " DESC");

                try {
                    phones.moveToFirst();
                    mPhone.setText(phones.getString(0));
                } finally {
                    phones.close();
                }
            } else {
                mPhone.setText(R.string.list_7_nothing);                
            }
        }
    }

    public void onNothingSelected(AdapterView parent) {
        mPhone.setText(R.string.list_7_nothing);
    }
}
