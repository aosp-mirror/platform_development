/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.example.android.vdmdemo.host;

import android.Manifest;
import android.companion.AssociationRequest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.function.Consumer;

class PermissionHelper {
    public static final int STREAMING_PERMISSION_REQUEST_ID = 1;
    private static final String TAG = "VdmPermissionHelper";
    private final Context mContext;
    private final PreferenceController mPreferenceController;

    PermissionHelper(Context context) {
        mContext = context;
        mPreferenceController = new PreferenceController(context);
    }

    boolean hasStreamingPermission() {
        return hasStreamingPermissionOrRequest(null);
    }

    boolean hasStreamingPermissionOrRequest(@Nullable Consumer<String[]> permissionRequester) {
        String permission = getStreamingPermission();

        if (mContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Already holding permission " + permission);
            return true;
        }

        // In real life, this can't happen since the application is preinstalled and pre-granted
        // the permission
        Log.d(TAG, "Not holding REQUEST_COMPANION_PROFILE_APP_STREAMING permission.");

        if (permissionRequester != null) {
            Log.d(TAG, " Requesting permission " + permission);
            permissionRequester.accept(new String[]{permission});
        }
        return false;
    }

    @NonNull
    String getStreamingPermission() {
        String deviceProfile = mPreferenceController.getString(
                R.string.pref_device_profile);
        return switch (deviceProfile) {
            case AssociationRequest.DEVICE_PROFILE_APP_STREAMING ->
                    Manifest.permission.REQUEST_COMPANION_PROFILE_APP_STREAMING;
            case AssociationRequest.DEVICE_PROFILE_NEARBY_DEVICE_STREAMING ->
                    Manifest.permission.REQUEST_COMPANION_PROFILE_NEARBY_DEVICE_STREAMING;
            default -> throw new IllegalArgumentException(
                    deviceProfile + " is not a supported device profile");
        };
    }
}
