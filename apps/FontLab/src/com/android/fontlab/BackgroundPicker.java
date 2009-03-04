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
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;


public abstract class BackgroundPicker extends ListActivity 
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
        List myData = new ArrayList<Bundle>();
        addItem(myData, "Solid White", 0, 0xFFFFFFFF, 0xFF000000);
        addItem(myData, "Solid Light Gray", 0, 0xFFBFBFBF, 0xFF000000);
        addItem(myData, "Solid Dark Gray", 0, 0xFF404040, 0xFFFFFFFF);
        addItem(myData, "Solid Black", 0, 0xFF000000, 0xFFFFFFFF);
        addItem(myData, "Solid Blue", 0, 0xFF1a387a, 0xFFFFFFFF);
        addItem(myData, "Textured White", 0, 0, 0xFF000000);
        // addItem(myData, "Textured Blue", android.R.drawable.screen_background_blue, 0, 0xFFFFFFFF);

        return myData;
    }
    
    protected void addItem(List<Bundle> data, String name, int textureRes, int bgColor, int textColor)
    {
        Bundle temp = new Bundle();
        temp.putString("title", name);
        if (textureRes != 0) {
            temp.putInt("texture", textureRes);
        }
        temp.putInt("bgcolor", bgColor);
        temp.putInt("text", textColor);
        data.add(temp);
    }

    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        Bundle map = (Bundle) l.getItemAtPosition(position);
        setResult(RESULT_OK, (new Intent()).putExtras(map));
        finish();
    }
  
}
