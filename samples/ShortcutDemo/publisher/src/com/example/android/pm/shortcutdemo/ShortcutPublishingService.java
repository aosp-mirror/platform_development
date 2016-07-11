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
package com.example.android.pm.shortcutdemo;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;

import java.util.Arrays;

/**
 * This allows to create a shortcut in background.
 *
 * Usage:
   adb shell am startservice -a com.example.android.pm.shortcutdemo.ADD \
     com.example.android.pm.shortcutdemo/com.example.android.pm.shortcutdemo.ShortcutPublishingService
 * Or for package 2,
   adb shell am startservice -a com.example.android.pm.shortcutdemo.ADD \
     com.example.android.pm.shortcutdemo2/com.example.android.pm.shortcutdemo.ShortcutPublishingService

 */
public class ShortcutPublishingService extends IntentService {
    public ShortcutPublishingService() {
        super("ShortcutPublishingService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction().endsWith(".ADD")) {
            addShortcut();
            return;
        }
    }

    private void addShortcut() {
        final ShortcutInfo si1 = ShortcutPublisher.addRandomIntents(
                this, new ShortcutInfo.Builder(this, ("shortcut-" + System.currentTimeMillis())))
                .build();
        ShortcutPublisher.callApi(this, () ->
            getSystemService(ShortcutManager.class).addDynamicShortcuts(Arrays.asList(si1)));
    }
}
