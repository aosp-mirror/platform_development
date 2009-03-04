/* //device/apps/Notes/NotesList.java
**
** Copyright 2006, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License"); 
** you may not use this file except in compliance with the License. 
** You may obtain a copy of the License at 
**
**     http://www.apache.org/licenses/LICENSE-2.0 
**
** Unless required by applicable law or agreed to in writing, software 
** distributed under the License is distributed on an "AS IS" BASIS, 
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
** See the License for the specific language governing permissions and 
** limitations under the License.
*/
package com.android.development;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import java.util.ArrayList;

public class DataList extends ListActivity
{
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);

        Intent intent = getIntent();

        mCursor = getContentResolver().query(intent.getData(), null, null, null, null);
        mDisplay = intent.getStringExtra("display");
        if (mDisplay == null) {
            mDisplay = "_id";
        }
        
        if (mCursor != null) {
            setListAdapter(new SimpleCursorAdapter(
                    this,
                    R.layout.url_list,
                    mCursor,
                    new String[] {mDisplay},
                    new int[] {android.R.id.text1}));
        }
    }

    public void onStop()
    {
        super.onStop();

        if (mCursor != null) {
            mCursor.deactivate();
        }
    }

    public void onResume()
    {
        super.onResume();

        if (mCursor != null) {
            mCursor.requery();
        }
        
        setTitle("Showing " + mDisplay);
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 0, 0, "Requery").setOnMenuItemClickListener(mRequery);
        return true;
    }

    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        mCursor.moveToPosition(position);

        ArrayList<ColumnData> data = new ArrayList<ColumnData>();

        String[] columnNames = mCursor.getColumnNames();
        for (int i=0; i<columnNames.length; i++) {
            String str = mCursor.getString(i);
            ColumnData cd = new ColumnData(columnNames[i], str);
            data.add(cd);
        }


        Uri uri = null;
        int idCol = mCursor.getColumnIndex("_id");
        if (idCol >= 0) {
            uri = Uri.withAppendedPath(getIntent().getData(), mCursor.getString(idCol));
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setClass(this, Details.class);

        intent.putExtra("data", data);
        int displayColumn = mCursor.getColumnIndex(mDisplay);
        if (displayColumn >= 0) {
            intent.putExtra("title",
                                ((ColumnData)data.get(displayColumn)).value);
        }

        startActivity(intent);
    }

    MenuItem.OnMenuItemClickListener mRequery = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            // Should just do requery on cursor, but 
            // doesn't work right now.  So do this instead.
            mCursor.requery();
            if (mCursor != null) {
                setListAdapter(new SimpleCursorAdapter(
                        DataList.this,
                        R.layout.url_list,
                        mCursor,
                        new String[] {mDisplay},
                        new int[] {android.R.id.text1}));
            }
            return true;
        }
    };

    private String mDisplay;
    private Cursor mCursor;
}
