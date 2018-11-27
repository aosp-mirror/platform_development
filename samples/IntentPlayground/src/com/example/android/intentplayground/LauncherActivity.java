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

package com.example.android.intentplayground;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.TextView;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
import static com.example.android.intentplayground.Node.newTaskNode;

/**
 * A singleInstance activity that is responsible for a launching a bootstrap stack of activities.
 */
public class LauncherActivity extends BaseActivity {
    public static final String TAG = "LauncherActivity";
    private static final long SNACKBAR_DELAY = 75;
    private TestBase mTester;
    private boolean mFirstLaunch;
    private View mSnackBarRootView;
    private boolean mSnackBarIsVisible = false;
    private boolean mDontLaunch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupTaskPreset();
        mFirstLaunch = true;
    }

    /**
     * Sets up a hierarchy of{@link Node}s that represents the desired initial task stack.
     */
    protected void setupTaskPreset() {
        Node mRoot = Node.newRootNode();
        // Describe initial setup of tasks
        // create singleTask, singleInstance, and two documents in separate tasks
        Node singleTask = newTaskNode()
                .addChild( new Node(new ComponentName(this, SingleTaskActivity.class)));
        Node docLaunchAlways = newTaskNode()
                .addChild( new Node(new ComponentName(this, DocumentLaunchAlwaysActivity.class)));
        Node docLaunchInto = newTaskNode()
                .addChild( new Node(new ComponentName(this, DocumentLaunchIntoActivity.class)));
        // Create three t0asks with three activities each, with affinity set
        Node taskAffinity1 = newTaskNode()
                .addChild(new Node(new ComponentName(this, TaskAffinity1Activity.class)))
                .addChild(new Node(new ComponentName(this, TaskAffinity1Activity.class)))
                .addChild(new Node(new ComponentName(this, TaskAffinity1Activity.class)));
        Node taskAffinity2 = newTaskNode()
                .addChild(new Node(new ComponentName(this, TaskAffinity2Activity.class)))
                .addChild(new Node(new ComponentName(this, TaskAffinity2Activity.class)))
                .addChild(new Node(new ComponentName(this, TaskAffinity2Activity.class)));
        Node taskAffinity3 = newTaskNode()
                .addChild(new Node(new ComponentName(this, TaskAffinity3Activity.class)))
                .addChild(new Node(new ComponentName(this, TaskAffinity3Activity.class)))
                .addChild(new Node(new ComponentName(this, TaskAffinity3Activity.class)));
        mRoot.addChild(singleTask).addChild(docLaunchAlways).addChild(docLaunchInto)
                .addChild(taskAffinity1).addChild(taskAffinity2).addChild(taskAffinity3);
        mTester = new TestBase(this, mRoot);
    }

    @Override
    protected void loadMode(Mode mode) {
        if (mode == Mode.LAUNCH) {
            super.loadMode(mode);
            long nonEmptyTasks = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nonEmptyTasks = getSystemService(ActivityManager.class)
                        .getAppTasks().stream().filter(t -> TaskInfo.getActivities(t.getTaskInfo()).size() != 0)
                        .count();
            }
            if (nonEmptyTasks <= 1 && !mDontLaunch) askToLaunchTasks();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mFirstLaunch && mSnackBarIsVisible) {
            hideSnackBar();
        } else {
            mFirstLaunch = false;
        }
    }

    private void askToLaunchTasks() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(R.string.launch_explanation)
                .setTitle(R.string.ask_to_launch)
                .setPositiveButton(R.string.ask_to_launch_affirm, (dialogInterface, i) -> {
                    showSnackBar(() -> {
                        mTester.startActivities(TestBase.LaunchStyle.TASK_STACK_BUILDER);
                    });
                    dialogInterface.dismiss();
                })
                .setNegativeButton(R.string.ask_to_launch_cancel, (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    mDontLaunch = true;
                    hideSnackBar();
                })
                .create();
        startSnackBar();
        dialog.show();
    }


    /**
     * Prepares the custom snackbar window. We must use an application overlay because normal
     * toasts and snackbars disappear once TestBase.startActivities() starts.
     */
    private void startSnackBar() {
        WindowManager wm = getSystemService(WindowManager.class);
        LayoutInflater inflater = getLayoutInflater();
        FrameLayout frame = new FrameLayout(this);
        inflater.inflate(R.layout.snack_loading, frame, true /* attachToRoot */);
        mSnackBarRootView = frame;
        if (!requestOverlayPermission()) return;
        wm.addView(frame, makeLayoutParams(TYPE_APPLICATION_OVERLAY));
        mSnackBarIsVisible = true;
    }

    private boolean requestOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            return true;
        } else {
            // Start manage overlays activity
            Intent intent = new Intent().setAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    .setData(Uri.fromParts(getString(R.string.package_uri_scheme),
                            getPackageName(),  null /* fragment */));
            startActivity(intent);
            return false;
        }
    }

    private void showSnackBar(Runnable onShow) {
        TextView tv = mSnackBarRootView.findViewById(R.id.snackbar_text);
        tv.setText(getString(R.string.launch_wait_toast));
        mSnackBarRootView.postDelayed(onShow, SNACKBAR_DELAY);
    }

    private void hideSnackBar() {
        if (mSnackBarIsVisible) {
            WindowManager wm = getSystemService(WindowManager.class);
            mSnackBarIsVisible = false;
            wm.removeView(mSnackBarRootView);
        }
    }

    private LayoutParams makeLayoutParams(int type) {
        LayoutParams params = new LayoutParams(type, FLAG_NOT_TOUCH_MODAL);
        params.format = PixelFormat.TRANSLUCENT;
        params.width = MATCH_PARENT;
        params.height = WRAP_CONTENT;
        params.gravity = Gravity.getAbsoluteGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM,
                View.LAYOUT_DIRECTION_RTL);
        return params;
    }

    /**
     * Launches activity with the selected options.
     */
    @Override
    public void launchActivity(Intent intent) {
        startActivityForResult(intent, LAUNCH_REQUEST_CODE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }
}