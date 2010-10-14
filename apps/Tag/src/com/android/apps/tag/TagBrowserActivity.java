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
import android.content.Intent;
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

        TabHost.TabSpec spec1 = tabHost.newTabSpec("saved")
                .setIndicator(getText(R.string.tab_saved), res.getDrawable(R.drawable.ic_menu_tag))
                .setContent(new Intent().setClass(this, TagList.class)
                        .putExtra(TagList.EXTRA_SHOW_SAVED_ONLY, true));
        tabHost.addTab(spec1);

        TabHost.TabSpec spec2 = tabHost.newTabSpec("recent")
                .setIndicator(getText(R.string.tab_recent), res.getDrawable(R.drawable.ic_menu_desk_clock))
                .setContent(new Intent().setClass(this, TagList.class));
        tabHost.addTab(spec2);
    }
}

