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

package com.android.fontlab;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;


public abstract class FontPicker extends ListActivity 
{
    
    public void onCreate(Bundle icicle) 
    {
        super.onCreate(icicle);

        setListAdapter(new SimpleAdapter(this,
                getData(),
                android.R.layout.simple_list_item_1,
                new String[] {"title"},
                new int[] {android.R.id.text1}));
    }
    
    protected List getData()
    {
        List myData = new ArrayList<Bundle>(7);
        addItem(myData, "Sans",                 "sans-serif",   Typeface.NORMAL);
        addItem(myData, "Sans Bold",            "sans-serif",   Typeface.BOLD);
        addItem(myData, "Serif",                "serif",        Typeface.NORMAL);
        addItem(myData, "Serif Bold",           "serif",        Typeface.BOLD);
        addItem(myData, "Serif Italic",         "serif",        Typeface.ITALIC);
        addItem(myData, "Serif Bold Italic",    "serif",        Typeface.BOLD_ITALIC);
        addItem(myData, "Mono",                 "monospace",    Typeface.NORMAL);
        return myData;
    }
    
    protected void addItem(List<Bundle> data, String name, String fontName, int style)
    {
        Bundle temp = new Bundle();
        temp.putString("title", name);
        temp.putString("font", fontName);
        temp.putInt("style", style);
        data.add(temp);
    }

    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        Bundle map = (Bundle) l.getItemAtPosition(position);
        setResult(RESULT_OK, (new Intent()).putExtras(map));
        finish();
    }
  
}
