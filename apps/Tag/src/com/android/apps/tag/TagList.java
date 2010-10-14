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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

/**
 * @author nnk@google.com (Nick Kralevich)
 */
public class TagList extends ListActivity implements DialogInterface.OnClickListener {

    private SQLiteDatabase db;
    private Cursor cursor;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(getBaseContext(), "entered method", Toast.LENGTH_SHORT).show();

        db = new TagDBHelper(getBaseContext()).getReadableDatabase();
        cursor = db.query("NdefMessage", new String[] { "_id", "bytes", "date" }, null, null, null, null, null);
        SimpleCursorAdapter sca =
                new SimpleCursorAdapter(this,
                        android.R.layout.two_line_list_item,
                        cursor,
                        new String[] { "bytes", "date" },
                        new int[] { android.R.id.text1, android.R.id.text2 });

        setListAdapter(sca);
        registerForContextMenu(getListView());
        Toast.makeText(getBaseContext(), "exit method", Toast.LENGTH_SHORT).show();
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
        if (cursor != null) {
            cursor.close();
        }
        if (db != null) {
            db.close();
        }
        super.onDestroy();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        showDialog(1);
        super.onListItemClick(l, v, position, id);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) { }

}
