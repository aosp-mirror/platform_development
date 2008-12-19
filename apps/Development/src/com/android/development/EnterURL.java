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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.graphics.Rect;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;

public class EnterURL extends ListActivity
{
    private static final int DATABASE_VERSION = 1;
    public static class UrlEditText extends EditText
    {
        public UrlEditText(Context context, AttributeSet attrs)
        {
            super(context, attrs);
        }
        
        @Override
        protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect)
        {
            super.onFocusChanged(focused, direction, previouslyFocusedRect);
            if (focused) {
                Editable text = getText();
                String str = text.toString();
                int highlightStart = 0;
                if (str.startsWith("content://")) {
                    highlightStart = "content://".length();
                }
                Selection.setSelection(text, highlightStart, text.length());
            }
        }
    }

    public static class DisplayEditText extends EditText
    {
        public DisplayEditText(Context context, AttributeSet attrs)
        {
            super(context, attrs);
        }
        
        @Override
        protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect)
        {
            super.onFocusChanged(focused, direction, previouslyFocusedRect);
            if (focused) {
                Editable text = getText();
                Selection.setSelection(text, 0, text.length());
            }
        }
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs)
    {
        if (name.equals("com.android.development.UrlEditText")) {
            return new UrlEditText(this, attrs);
        }
        if (name.equals("com.android.development.DisplayEditText")) {
            return new DisplayEditText(this, attrs);
        }
        return null;
    }

    View.OnClickListener mViewItemAction = new View.OnClickListener () {
        public void onClick(View v)
        {
            String url = mUrlField.getText().toString();
            String display = mDisplayField.getText().toString();
            viewItem(url, display);
        }
    };

    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        setContentView(R.layout.enter_url);

        // display
        mDisplayField = (DisplayEditText)findViewById(R.id.display_edit_text);
        mDisplayField.setOnClickListener(mViewItemAction);
        // url
        mUrlField = (UrlEditText)findViewById(R.id.url_edit_text);
        mUrlField.setOnClickListener(mViewItemAction);
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

        // show the history
        loadPrefs();
        fillListView();
        if (mHistory.size() > 0) {
            ListView lv = this.getListView();
            lv.setSelection(0);
            lv.requestFocus();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 0, 0, "Clear Bookmarks").setOnMenuItemClickListener(mClearBookmarks);
        return true;
    }

    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        HistoryEntry he = mHistory.get(position);
        viewItem(he.url, he.display);
    }

    private final void viewItem(String url, String display)
    {
        // -------------- save this in the history ----------------
        // look in the history
        int count = mHistory.size();
        int i;
        for (i=0; i<count; i++) {
            HistoryEntry he = mHistory.get(i);
            if (he.url.equals(url) && he.display.equals(display)) {
                he.updateAccessTime();
                mHistory.remove(i);
                mHistory.add(0, he);
                break;
            }
        }
        if (i >= count) {
            // didn't find it, add it first
            HistoryEntry he = new HistoryEntry();
            he.url = url;
            he.display = display;
            he.updateAccessTime();
            mHistory.add(0, he);
        }

        savePrefs();

        // -------------- view it ---------------------------------
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setClass(this, DataList.class);
        intent.putExtra("display", display);
        startActivity(intent);
    }

    MenuItem.OnMenuItemClickListener mClearBookmarks = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            mHistory.clear();
            savePrefs();
            fillListView();
            return true;
        }
    };

    private void fillListView()
    {
        loadPrefs();
        ArrayList<HashMap<String, String>> d = new ArrayList<HashMap<String, String>>();
        int count = mHistory.size();
        for (int i=0; i<count; i++) {
            HashMap<String, String> m = new HashMap<String, String>();
            HistoryEntry he = mHistory.get(i);
            m.put("title", he.url + " (" + he.display + ")");
            d.add(m);
        }
        setListAdapter(new SimpleAdapter(this, d, R.layout.url_list,
                                         new String[] {"title"},
                                         new int[] {android.R.id.text1}));
    }

    SQLiteDatabase openDB()
    {
        SQLiteDatabase db = null;
        db = openOrCreateDatabase("inspector.db", 0, null);
        int version = db.getVersion();
        if (version != DATABASE_VERSION) {
            db.execSQL("CREATE TABLE History ("
                        + " url TEXT,"
                        + " display TEXT,"
                        + " lastAccessTime TEXT"
                        + ");");
            db.execSQL("CREATE TABLE FieldState ("
                        + " url TEXT,"
                        + " display TEXT"
                        + ");");
            db.setVersion(DATABASE_VERSION);
        }
        return db;
    }

    private void loadPrefs()
    {
        SQLiteDatabase db = openDB();
        Cursor c = db.query("History",
                            new String[] { "url", "display", "lastAccessTime" },
                            null, null, null, null, "lastAccessTime DESC");
        int urlCol = c.getColumnIndex("url");
        int accessCol = c.getColumnIndex("lastAccessTime");
        int displayCol = c.getColumnIndex("display");
        mHistory.clear();
        while (c.moveToNext()) {
            HistoryEntry he = new HistoryEntry();
            he.url = c.getString(urlCol);
            he.display = c.getString(displayCol);
            he.lastAccessTime = c.getString(accessCol);
            mHistory.add(he);
        }

        c = db.query("FieldState", null, null, null, null, null, null);
        if (c.moveToNext()) {
            urlCol = c.getColumnIndex("url");
            displayCol = c.getColumnIndex("display");
            mUrlField.setText(c.getString(urlCol));
            mDisplayField.setText(c.getString(displayCol));
        } else {
            mDisplayField.setText("_id");
            mUrlField.setText("content://");
        }

        db.close();
    }

    private void savePrefs()
    {
        ContentValues m;
        HistoryEntry he;

        SQLiteDatabase db = openDB();
        db.execSQL("DELETE FROM History;");
        int count = mHistory.size();
        for (int i=0; i<count; i++) {
            m = new ContentValues();
            he = mHistory.get(i);
            m.put("url", he.url);
            m.put("display", he.display);
            m.put("lastAccessTime", he.lastAccessTime);
            db.insert("History", null, m);
        }

        db.execSQL("DELETE FROM FieldState");
        m = new ContentValues();
        m.put("url", mUrlField.getText().toString());
        m.put("display", mDisplayField.getText().toString());
        db.insert("FieldState", null, m);

        db.close();
    }

    private class HistoryEntry
    {
        public String url;
        public String display;
        public String lastAccessTime;
        public void updateAccessTime()
        {
            this.lastAccessTime = DateUtils.writeDateTime(
                                                    new GregorianCalendar());
        }
    }

    private ArrayList<HistoryEntry> mHistory = new ArrayList<HistoryEntry>();
    private UrlEditText mUrlField;
    private DisplayEditText mDisplayField;
    private Cursor mCursor;
}
