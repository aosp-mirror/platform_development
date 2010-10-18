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

import android.app.Activity;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

/**
 * A browsing {@link Activity} that displays the saved tags in categories under tabs.
 */
public class TagBrowserActivity extends TabActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        Resources res = getResources();
        TabHost tabHost = getTabHost();

        tabHost.addTab(tabHost.newTabSpec("tags")
                .setIndicator(getText(R.string.tab_tags),
                        res.getDrawable(R.drawable.ic_menu_tag))
                .setContent(new Intent().setClass(this, TagList.class)));

        tabHost.addTab(tabHost.newTabSpec("starred")
                .setIndicator(getText(R.string.tab_starred),
                        res.getDrawable(R.drawable.ic_tab_starred))
                .setContent(new Intent().setClass(this, TagList.class)
                        .putExtra(TagList.EXTRA_SHOW_STARRED_ONLY, true)));
    }

    @Override
    public void onStart() {
        super.onStart();

        // Restore the last active tab
        SharedPreferences prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        getTabHost().setCurrentTabByTag(prefs.getString("tab", "tags"));
    }

    @Override
    public void onStop() {
        super.onStop();

        // Save the active tab
        SharedPreferences.Editor edit = getSharedPreferences("prefs", Context.MODE_PRIVATE).edit();
        edit.putString("tab", getTabHost().getCurrentTabTag());
        edit.apply();
    }
}
