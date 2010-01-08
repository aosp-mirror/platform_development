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

import java.util.ArrayList;

import android.content.Intent;
import android.app.Activity;
import android.database.Cursor;
import android.graphics.Typeface;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;

public class Details extends Activity
{
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);

        Intent intent = getIntent();

        String title = intent.getStringExtra("title");
        if (title == null) {
            title = "Details";
        }
        setTitle(title);

        mScrollView = new ScrollView(this);
        setContentView(mScrollView);
        mScrollView.setFocusable(true);

        mData = (ArrayList<ColumnData>)getIntent().getExtra("data");
        addDataViews();
    }

    public void onResume()
    {
        super.onResume();
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 0, 0, "Requery").setOnMenuItemClickListener(mRequery);
        menu.add(0, 0, 0, "Print to stdout").setOnMenuItemClickListener(mPrintToStdout);
        return true;
    }

    void addDataViews()
    {
        int oldScroll = 0;

        if (mLinearLayout != null) {
            mScrollView.removeView(mLinearLayout);
        }
        mLinearLayout = new LinearLayout(this);
        mScrollView.addView(mLinearLayout, new ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT));
        mLinearLayout.setOrientation(LinearLayout.VERTICAL);

        // Here in onStart, we're given data.  We use that because some
        // data that we show is transient and can't be retrieved from a url.
        // We'll try to use that in requery
        int count = mData.size();
        for (int i=0; i<count; i++) {
            ColumnData cd = mData.get(i);
            TextView label = makeView(cd.key, true, 12);
            TextView contents = makeView(cd.value, false, 12);
            contents.setPadding(3, 0, 0, i==count-1?0:3);
            mLinearLayout.addView(label, lazy());
            mLinearLayout.addView(contents, lazy());
        }
    }

    TextView makeView(String str, boolean bold, int fontSize)
    {
        if (str == null) {
            str = "(null)";
        }
        TextView v = new TextView(this);
        v.setText(str);
        v.setTextSize(fontSize);
        if (bold) {
            v.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return v;
    }
    
    LinearLayout.LayoutParams lazy()
    {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                 ViewGroup.LayoutParams.WRAP_CONTENT, 0);
    }

    MenuItem.OnMenuItemClickListener mRequery = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            Intent intent = getIntent();
            Cursor c = getContentResolver().query(intent.getData(), null, null, null, null);
            if (c != null && c.moveToNext()) {
                mData.clear();
                String[] columnNames = c.getColumnNames();
                for (int i=0; i<columnNames.length; i++) {
                    String str = c.getString(i);
                    ColumnData cd = new ColumnData(columnNames[i], str);
                    mData.add(cd);
                }
                addDataViews();
            } else {
                TextView error = new TextView(Details.this);
                error.setText("Showing old data.\nURL couldn't be requeried:\n"
                        + intent.getData());
                error.setTextColor(0xffff0000);
                error.setTextSize(11);
                mLinearLayout.addView(error, 0, lazy());
            }
            return true;
        }
    };

    MenuItem.OnMenuItemClickListener mPrintToStdout = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            System.out.println("=== begin data ===");
            int count = mData.size();
            for (int i=0; i<count; i++) {
                ColumnData cd = mData.get(i);
                System.out.println("  " + cd.key + ": " + cd.value);
            }
            System.out.println("=== end data ===");
            return true;
        }
    };

    LinearLayout mLinearLayout;
    ScrollView mScrollView;
    ArrayList<ColumnData> mData;
}
