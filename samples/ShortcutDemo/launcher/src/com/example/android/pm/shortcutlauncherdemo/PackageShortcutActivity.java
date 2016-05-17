/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.example.android.pm.shortcutlauncherdemo;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;

public class PackageShortcutActivity extends Activity {
    private static final String KEY_TARGET_PACKAGE = "PackageShortcutActivity.target_package";
    private static final String KEY_TARGET_ACTIVITY = "PackageShortcutActivity.target_activity";
    private static final String KEY_TARGET_USER = "PackageShortcutActivity.user";
    private static final String KEY_TITLE = "PackageShortcutActivity.title";

    public static Intent getLaunchIntent(Context context, String targetPackage,
            ComponentName targetActivity, UserHandle user, CharSequence title) {
        final Intent i = new Intent(Intent.ACTION_VIEW);
        i.setComponent(new ComponentName(context, PackageShortcutActivity.class));

        i.putExtra(KEY_TARGET_PACKAGE, targetPackage);
        i.putExtra(KEY_TARGET_ACTIVITY, targetActivity);
        i.putExtra(KEY_TARGET_USER, user);
        i.putExtra(KEY_TITLE, title);

        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.framelayout);

        final Intent i = getIntent();

        final Fragment f = new ShortcutListFragment().setArguments(
                i.getStringExtra(KEY_TARGET_PACKAGE),
                /* targetActivity=*/ i.getParcelableExtra(KEY_TARGET_ACTIVITY),
                /*includeDynamic=*/ true,
                /*includeManifest=*/ true,
                /*includePinned=*/ true,
                i.getParcelableExtra(KEY_TARGET_USER),
                /* showDetails =*/ false
                );

        setTitle(i.getStringExtra(KEY_TITLE));

        getFragmentManager().beginTransaction().replace(R.id.main, f).commit();
    }
}
