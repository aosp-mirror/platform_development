/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.example.android.basiccontactables;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds;
import android.util.Log;
import android.widget.TextView;

/**
 * Helper class to handle all the callbacks that occur when interacting with loaders.  Most of the
 * interesting code in this sample app will be in this file.
 */
public class ContactablesLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {

    Context mContext;

    public static final String QUERY_KEY = "query";

    public static final String TAG = "ContactablesLoaderCallbacks";

    public ContactablesLoaderCallbacks(Context context) {
        mContext = context;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderIndex, Bundle args) {
        // Where the Contactables table excels is matching text queries,
        // not just data dumps from Contacts db.  One search term is used to query
        // display name, email address and phone number.  In this case, the query was extracted
        // from an incoming intent in the handleIntent() method, via the
        // intent.getStringExtra() method.

        // BEGIN_INCLUDE(uri_with_query)
        String query = args.getString(QUERY_KEY);
        Uri uri = Uri.withAppendedPath(
                CommonDataKinds.Contactables.CONTENT_FILTER_URI, query);
        // END_INCLUDE(uri_with_query)


        // BEGIN_INCLUDE(cursor_loader)
        // Easy way to limit the query to contacts with phone numbers.
        String selection =
                CommonDataKinds.Contactables.HAS_PHONE_NUMBER + " = " + 1;

        // Sort results such that rows for the same contact stay together.
        String sortBy = CommonDataKinds.Contactables.LOOKUP_KEY;

        return new CursorLoader(
                mContext,  // Context
                uri,       // URI representing the table/resource to be queried
                null,      // projection - the list of columns to return.  Null means "all"
                selection, // selection - Which rows to return (condition rows must match)
                null,      // selection args - can be provided separately and subbed into selection.
                sortBy);   // string specifying sort order
        // END_INCLUDE(cursor_loader)
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
        TextView tv  = (TextView) ((Activity)mContext).findViewById(R.id.sample_output);
        if(tv == null) {
            Log.e(TAG, "TextView is null?!");
        } else if (mContext == null) {
            Log.e(TAG, "Context is null?");
        } else {
            Log.e(TAG, "Nothing is null?!");
        }

        // Reset text in case of a previous query
        tv.setText(mContext.getText(R.string.intro_message) + "\n\n");

        if (cursor.getCount() == 0) {
            return;
        }

        // Pulling the relevant value from the cursor requires knowing the column index to pull
        // it from.
        // BEGIN_INCLUDE(get_columns)
        int phoneColumnIndex = cursor.getColumnIndex(CommonDataKinds.Phone.NUMBER);
        int emailColumnIndex = cursor.getColumnIndex(CommonDataKinds.Email.ADDRESS);
        int nameColumnIndex = cursor.getColumnIndex(CommonDataKinds.Contactables.DISPLAY_NAME);
        int lookupColumnIndex = cursor.getColumnIndex(CommonDataKinds.Contactables.LOOKUP_KEY);
        int typeColumnIndex = cursor.getColumnIndex(CommonDataKinds.Contactables.MIMETYPE);
        // END_INCLUDE(get_columns)

        cursor.moveToFirst();
        // Lookup key is the easiest way to verify a row of data is for the same
        // contact as the previous row.
        String lookupKey = "";
        do {
            // BEGIN_INCLUDE(lookup_key)
            String currentLookupKey = cursor.getString(lookupColumnIndex);
            if (!lookupKey.equals(currentLookupKey)) {
                String displayName = cursor.getString(nameColumnIndex);
                tv.append(displayName + "\n");
                lookupKey = currentLookupKey;
            }
            // END_INCLUDE(lookup_key)

            // BEGIN_INCLUDE(retrieve_data)
            // The data type can be determined using the mime type column.
            String mimeType = cursor.getString(typeColumnIndex);
            if (mimeType.equals(CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
                tv.append("\tPhone Number: " + cursor.getString(phoneColumnIndex) + "\n");
            } else if (mimeType.equals(CommonDataKinds.Email.CONTENT_ITEM_TYPE)) {
                tv.append("\tEmail Address: " + cursor.getString(emailColumnIndex) + "\n");
            }
            // END_INCLUDE(retrieve_data)

            // Look at DDMS to see all the columns returned by a query to Contactables.
            // Behold, the firehose!
            for(String column : cursor.getColumnNames()) {
                Log.d(TAG, column + column + ": " +
                        cursor.getString(cursor.getColumnIndex(column)) + "\n");
            }
        } while (cursor.moveToNext());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }
}
