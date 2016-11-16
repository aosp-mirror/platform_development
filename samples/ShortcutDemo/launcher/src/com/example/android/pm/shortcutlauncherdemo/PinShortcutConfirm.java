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
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class PinShortcutConfirm extends Activity implements OnClickListener {
    private static final String TAG = "ShortcutConfirm";

    protected LauncherApps mLauncherApps;

    private LauncherApps.PinItemRequest mRequest;
    private ShortcutInfo mShortcutInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.pin_shortcut_confirm);

        mLauncherApps = getSystemService(LauncherApps.class);

        findViewById(R.id.ok).setOnClickListener(this);
        findViewById(R.id.cancel).setOnClickListener(this);

        mRequest = getSystemService(LauncherApps.class)
                .getPinItemRequest(getIntent());

        mShortcutInfo = mRequest.getShortcutInfo();

        final TextView description = (TextView) findViewById(R.id.shortcut_description);
        description.setText("ID: " + mShortcutInfo.getId() + "\n"
                + "Label: " + mShortcutInfo.getShortLabel());

        final Drawable icon = mLauncherApps.getShortcutIconDrawable(mShortcutInfo, 0);
        if (icon != null) {
            ((ImageView) findViewById(R.id.image)).setImageDrawable(icon);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ok:
                acceptShortcut();
                finish();
                break;

            case R.id.cancel:
                finish();
                break;
        }
    }

    private void acceptShortcut() {
        final boolean result = mRequest.accept();
        Log.i(TAG, "Accept returned: " + result);
        mRequest = null;
    }
}
