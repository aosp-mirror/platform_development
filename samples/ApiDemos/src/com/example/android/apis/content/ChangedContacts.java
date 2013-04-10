/*
 * Copyright (C) 2013 The Android Open Source Project
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
 * limitations under the License
 */

package com.example.android.apis.content;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Demonstrates selecting contacts that have changed since a certain time.
 */
public class ChangedContacts extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String CLASS = ChangedContacts.class.getSimpleName();

    private static final String PREF_KEY_CHANGE = "timestamp_change";
    private static final String PREF_KEY_DELETE = "timestamp_delete";

    private static final int ID_CHANGE_LOADER = 1;
    private static final int ID_DELETE_LOADER = 2;

    /**
     * To see this in action, "clear data" for the contacts storage app in the system settings.
     * Then come into this app and hit any of the delta buttons.  This will cause the contacts
     * database to be re-created.
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast toast = Toast.makeText(context, "Contacts database created.", Toast.LENGTH_SHORT);
            toast.show();
        }
    };

    private DeleteAdapter mDeleteAdapter;
    private ChangeAdapter mChangeAdapter;
    private long mSearchTime;
    private TextView mDisplayView;
    private ListView mList;
    private Button mDeleteButton;
    private Button mChangeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDeleteAdapter = new DeleteAdapter(this, null, 0);
        mChangeAdapter = new ChangeAdapter(this, null, 0);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);

        mChangeButton = new Button(this);
        mChangeButton.setText("Changed since " + getLastTimestamp(0, PREF_KEY_CHANGE));
        mChangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeClick();
            }
        });

        mDeleteButton = new Button(this);
        mDeleteButton.setText("Deleted since " + getLastTimestamp(0, PREF_KEY_DELETE));
        mDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteClick();
            }
        });

        main.addView(mChangeButton);
        main.addView(mDeleteButton);

        mDisplayView = new TextView(this);
        mDisplayView.setPadding(5, 5, 5, 5);
        main.addView(mDisplayView);

        mList = new ListView(this);
        mList.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        main.addView(mList);

        setContentView(main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ContactsContract.Intents.CONTACTS_DATABASE_CREATED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    private void changeClick() {
        mChangeAdapter.swapCursor(null);
        LoaderManager manager = getLoaderManager();
        manager.destroyLoader(ID_DELETE_LOADER);
        manager.restartLoader(ID_CHANGE_LOADER, null, this);
    }

    private void deleteClick() {
        mChangeAdapter.swapCursor(null);
        LoaderManager manager = getLoaderManager();
        manager.destroyLoader(ID_CHANGE_LOADER);
        manager.restartLoader(ID_DELETE_LOADER, null, this);
    }

    private void saveLastTimestamp(long time, String key) {
        SharedPreferences pref = getSharedPreferences(CLASS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putLong(key, time);
        editor.commit();
    }

    private long getLastTimestamp(long time, String key) {
        SharedPreferences pref = getSharedPreferences(CLASS, Context.MODE_PRIVATE);
        return pref.getLong(key, time);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch(id) {
            case ID_CHANGE_LOADER:
                return getChangeLoader();
            case ID_DELETE_LOADER:
                return getDeleteLoader();
        }
        return null;
    }

    private CursorLoader getChangeLoader() {
        String[] projection = new String[]{
                ContactsContract.Data._ID,
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.Data.DISPLAY_NAME,
                ContactsContract.Data.CONTACT_LAST_UPDATED_TIMESTAMP
        };

        mSearchTime = getLastTimestamp(0, PREF_KEY_CHANGE);

        String selection = ContactsContract.Data.CONTACT_LAST_UPDATED_TIMESTAMP + " > ?";
        String[] bindArgs = new String[]{mSearchTime + ""};
        return new CursorLoader(this, ContactsContract.Data.CONTENT_URI, projection,
                selection, bindArgs, ContactsContract.Data.CONTACT_LAST_UPDATED_TIMESTAMP
                + " desc, " + ContactsContract.Data.CONTACT_ID + " desc");
    }

    private CursorLoader getDeleteLoader() {
        String[] projection = new String[]{
                ContactsContract.DeletedContacts.CONTACT_ID,
                ContactsContract.DeletedContacts.CONTACT_DELETED_TIMESTAMP
        };

        mSearchTime = getLastTimestamp(0, PREF_KEY_DELETE);

        String selection = ContactsContract.DeletedContacts.CONTACT_DELETED_TIMESTAMP + " > ?";
        String[] bindArgs = new String[]{mSearchTime + ""};
        return new CursorLoader(this, ContactsContract.DeletedContacts.CONTENT_URI, projection,
                selection, bindArgs, ContactsContract.DeletedContacts.CONTACT_DELETED_TIMESTAMP +
                " desc");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor data) {
        long timestamp = 0;


        switch (cursorLoader.getId()) {
            case ID_CHANGE_LOADER:
                mDisplayView.setText(data.getCount() + " change(s) since " + mSearchTime);
                mList.setAdapter(mChangeAdapter);
                mChangeAdapter.swapCursor(data);

                // Save the largest timestamp returned.  Only need the first one due to the sort
                // order.
                if (data.moveToNext()) {
                    timestamp = data.getLong(data.getColumnIndex(
                            ContactsContract.Data.CONTACT_LAST_UPDATED_TIMESTAMP));
                    data.moveToPrevious();
                }
                if (timestamp > 0) {
                    saveLastTimestamp(timestamp, PREF_KEY_CHANGE);
                    mChangeButton.setText("Changed since " + timestamp);
                }
                break;
            case ID_DELETE_LOADER:
                mDisplayView.setText(data.getCount() + " delete(s) since " + mSearchTime);
                mList.setAdapter(mDeleteAdapter);
                mDeleteAdapter.swapCursor(new DeleteCursorWrapper(data));
                if (data.moveToNext()) {
                    timestamp = data.getLong(data.getColumnIndex(
                            ContactsContract.DeletedContacts.CONTACT_DELETED_TIMESTAMP));
                    data.moveToPrevious();
                }
                if (timestamp > 0) {
                    saveLastTimestamp(timestamp, PREF_KEY_DELETE);
                    mDeleteButton.setText("Deleted since " + timestamp);
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mDisplayView.setText("");
        switch (cursorLoader.getId()) {
            case ID_CHANGE_LOADER:
                mChangeAdapter.swapCursor(null);
                break;
            case ID_DELETE_LOADER:
                mDeleteAdapter.swapCursor(null);
                break;
        }
    }

    private class DeleteCursorWrapper extends CursorWrapper {

        /**
         * Creates a cursor wrapper.
         *
         * @param cursor The underlying cursor to wrap.
         */
        public DeleteCursorWrapper(Cursor cursor) {
            super(cursor);
        }

        @Override
        public int getColumnIndexOrThrow(String columnName) {
            if (columnName.equals("_id")) {
                return super.getColumnIndex(ContactsContract.DeletedContacts.CONTACT_ID);
            }
            return super.getColumnIndex(columnName);
        }
    }

    private static class DeleteAdapter extends CursorAdapter {

        private Context mContext;

        public DeleteAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
            this.mContext = context;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LinearLayout item = new LinearLayout(mContext);
            item.addView(buildText(context));
            item.addView(buildText(context));
            return item;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            LinearLayout item = (LinearLayout) view;
            String id = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.DeletedContacts.CONTACT_ID));
            String timestamp = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.DeletedContacts.CONTACT_DELETED_TIMESTAMP));

            setText(item.getChildAt(0), id);
            setText(item.getChildAt(1), timestamp);
        }
    }

    private static class ChangeAdapter extends CursorAdapter {

        private Context mContext;

        public ChangeAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
            mContext = context;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LinearLayout item = new LinearLayout(mContext);
            item.addView(buildText(context));
            item.addView(buildText(context));
            item.addView(buildText(context));
            return item;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            LinearLayout item = (LinearLayout) view;

            String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID));
            String name = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.Data.DISPLAY_NAME));
            String timestamp = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.Data.CONTACT_LAST_UPDATED_TIMESTAMP));

            setText(item.getChildAt(0), id);
            setText(item.getChildAt(1), name);
            setText(item.getChildAt(2), timestamp);
        }
    }

    private static void setText(View view, String value) {
        TextView text = (TextView) view;
        text.setText(value);
    }

    private static TextView buildText(Context context) {
        TextView view = new TextView(context);
        view.setPadding(3, 3, 3, 3);
        return view;
    }
}
