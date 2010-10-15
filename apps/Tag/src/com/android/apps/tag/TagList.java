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

package com.android.apps.tag;

import com.android.apps.tag.TagDBHelper.NdefMessagesTable;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;

/**
 * An {@link Activity} that displays a flat list of tags that can be "opened".
 */
public class TagList extends ListActivity implements DialogInterface.OnClickListener {
    static final String TAG = "TagList";

    static final String EXTRA_SHOW_SAVED_ONLY = "show_saved_only";

    SQLiteDatabase mDatabase;
    TagAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean showSavedOnly = getIntent().getBooleanExtra(EXTRA_SHOW_SAVED_ONLY, false);
        mDatabase = TagDBHelper.getInstance(this).getReadableDatabase();
        String selection = showSavedOnly ? NdefMessagesTable.SAVED + "=1" : null;

        new TagLoaderTask().execute(selection);
        mAdapter = new TagAdapter(this);
        setListAdapter(mAdapter);
        registerForContextMenu(getListView());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add("hello world");
        return true;
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        String[] stuff = new String[] { "a", "b" };
        return new AlertDialog.Builder(this)
                .setTitle("blah")
                .setItems(stuff, this)
                .setPositiveButton("Delete", null)
                .setNegativeButton("Cancel", null)
                .create();
    }

    @Override
    protected void onDestroy() {
        if (mAdapter != null) {
            mAdapter.changeCursor(null);
        }
        super.onDestroy();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Cursor cursor = mAdapter.getCursor();
        cursor.moveToPosition(position);
        byte[] tagBytes = cursor.getBlob(cursor.getColumnIndexOrThrow(NdefMessagesTable.BYTES));
        try {
            NdefMessage msg = new NdefMessage(tagBytes);
            Intent intent = new Intent(this, TagViewer.class);
            intent.putExtra(TagViewer.EXTRA_MESSAGE, msg);
            intent.putExtra(TagViewer.EXTRA_TAG_DB_ID, id);
            startActivity(intent);
        } catch (FormatException e) {
            Log.e(TAG, "bad format for tag " + id + ": " + tagBytes, e);
            return;
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
    }

    final class TagLoaderTask extends AsyncTask<String, Void, Cursor> {
        @Override
        public Cursor doInBackground(String... args) {
            String selection = args[0];
            Cursor cursor = mDatabase.query(
                    NdefMessagesTable.TABLE_NAME,
                    new String[] { 
                            NdefMessagesTable._ID,
                            NdefMessagesTable.BYTES,
                            NdefMessagesTable.DATE,
                            NdefMessagesTable.TITLE },
                    selection,
                    null, null, null, null);
            cursor.getCount();
            return cursor;
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            mAdapter.changeCursor(cursor);
        }
    }
}
