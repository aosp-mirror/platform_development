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

import android.app.ActivityOptions;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import dagger.hilt.android.AndroidEntryPoint;

/** A dialog prompting the user to unlock the default display for a fallback activity launch. */
@AndroidEntryPoint(AppCompatActivity.class)
public class UnlockKeyguardDialog extends Hilt_UnlockKeyguardDialog
        implements KeyguardManager.KeyguardLockedStateListener {
    private static final String TAG = "UnlockKeyguardDialog";

    private IntentSender mTarget;
    private KeyguardManager mKeyguardManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        mTarget = intent.getParcelableExtra(Intent.EXTRA_INTENT,
                android.content.IntentSender.class);
        if (mTarget == null) {
            Log.wtf(TAG, "Invalid intent: " + intent);
            finish();
        }

        mKeyguardManager = getSystemService(KeyguardManager.class);
        if (mKeyguardManager == null) {
            Log.wtf(TAG, "KeyguardManager not available");
            finish();
        }

        setContentView(R.layout.unlock_keyguard_dialog);
        ((TextView) requireViewById(R.id.message_view))
                .setText(getString(R.string.unlock_dialog_message, Build.MODEL));

        if (!mKeyguardManager.isKeyguardLocked()) {
            onKeyguardLockedStateChanged(false);
        } else {
            mKeyguardManager.addKeyguardLockedStateListener(getMainExecutor(), this);
        }

        setFinishOnTouchOutside(true);
    }

    @Override
    public void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onDestroy() {
        mKeyguardManager.removeKeyguardLockedStateListener(this);
        super.onDestroy();
    }

    @Override
    public void onKeyguardLockedStateChanged(boolean isKeyguardLocked) {
        if (isKeyguardLocked) return;

        Bundle activityOptions =
                ActivityOptions.makeBasic()
                        .setPendingIntentBackgroundActivityStartMode(
                                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                        .setLaunchDisplayId(Display.DEFAULT_DISPLAY)
                        .toBundle();
        try {
            startIntentSender(mTarget, /* fillInIntent= */ null, /* flagsMask= */ 0,
                    /* flagsValues= */ 0, /* extraFlags= */ 0, activityOptions);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Error while starting intent sender", e);
        }
        finish();
    }

    /** Called when the dialog has been canceled by the user. */
    public void onCanceled(View view) {
        finish();
    }

    /**
     * Creates an intent that launches UnlockKeyguardDialog when an activity launch is blocked.
     */
    public static Intent createIntent(Context context, IntentSender target) {
        return new Intent()
                .setClass(context, UnlockKeyguardDialog.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(Intent.EXTRA_INTENT, target);
    }
}
