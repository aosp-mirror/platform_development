/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps.PinItemRequest;
import android.os.UserHandle;
import android.util.Log;

import java.util.List;

public class ShortcutTemplateListFragment extends BaseActivityListFragment {
    private static final String TAG = "ShortcutTemplateListFragment";
    private static final int REQUEST_SHORTCUT = 1;

    @Override
    protected List<LauncherActivityInfo> getActivities(UserHandle user) {
        return mLauncherApps.getShortcutConfigActivityList(null, user);
    }

    @Override
    protected void onLaunch(LauncherActivityInfo ai) {
        final IntentSender is = mLauncherApps.getShortcutConfigActivityIntent(ai);
        try {
            startIntentSenderForResult(is, REQUEST_SHORTCUT, null, 0,0, 0, null);
        } catch (SendIntentException e) {
            Log.e(TAG, "Couldn't start activity", e);
            Global.showToast(getActivity(), "Couldn't start activity: " + e.getMessage());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return;
        }
        switch (requestCode) {
            case REQUEST_SHORTCUT:
                final PinItemRequest req =  mLauncherApps.getPinItemRequest(data);
                if (req == null) {
                    Global.showToast(getActivity(),
                            "App doesn't support app shortcut (only supports \"legacy\" ones)");
                } else {
                    req.accept();
                }
                break;
        }
    }
}
