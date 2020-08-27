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

import static com.example.android.intentplayground.Node.newTaskNode;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.android.intentplayground.Tracking.Tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Implements the shared functionality for all of the other activities.
 */
public abstract class BaseActivity extends AppCompatActivity implements
        IntentBuilderView.OnLaunchCallback {
    public final static String EXTRA_LAUNCH_FORWARD = "com.example.android.launchForward";
    public final static String BUILDER_VIEW = "com.example.android.builderFragment";
    public static final String TREE_FRAGMENT = "com.example.android.treeFragment";
    public static final String EXPECTED_TREE_FRAGMENT = "com.example.android.expectedTreeFragment";
    public static final int LAUNCH_REQUEST_CODE = 0xEF;
    private static final int LAUNCH_FOR_RESULT_ID = 1;

    public enum Mode {LAUNCH, VERIFY, RESULT}

    public boolean userLeaveHintWasCalled = false;
    protected Mode mStatus = Mode.LAUNCH;

    /**
     * To display the task / activity overview in {@link TreeFragment} we track onResume and
     * onDestroy calls in this global location. {@link BaseActivity} should delegate to
     * {@link Tracker#onResume(Activity)} and {@link Tracker#onDestroy(Activity)} in it's respective
     * lifecycle callbacks.
     */
    private static Tracker mTracker = new Tracker();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (BuildConfig.DEBUG) Log.d(getLocalClassName(), "onCreate()");
        // Setup action bar
        Toolbar appBar = findViewById(R.id.app_bar);
        appBar.setTitle(this.getClass().getSimpleName());
        setSupportActionBar(appBar);

        FloatingActionButton launchButton = findViewById(R.id.launch_fab);
        launchButton.setOnClickListener(l -> {
            LaunchFragment fragment = new LaunchFragment();

            getSupportFragmentManager().beginTransaction()
                    .addToBackStack(null)
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        });

        BaseActivityViewModel viewModel = (new ViewModelProvider(this,
                new ViewModelProvider.NewInstanceFactory())).get(BaseActivityViewModel.class);

        viewModel.getFabActions().observe(this, action -> {
            switch (action) {
                case Show:
                    launchButton.show();
                    break;
                case Hide:
                    launchButton.hide();
                    break;
            }
        });


        loadMode(Mode.LAUNCH);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTracker.onResume(this);
        Intent launchForward = prepareLaunchForward();
        if (launchForward != null) {
            startActivity(launchForward);
        }
    }

    @Override
    protected  void onDestroy() {
        super.onDestroy();
        mTracker.onDestroy(this);
    }

    static void addTrackerListener(Consumer<List<Tracking.Task>> listener) {
        mTracker.addListener(listener);
    }

    static void removeTrackerListener(Consumer<List<Tracking.Task>> listener) {
        mTracker.removeListener(listener);
    }

    /**
     * Initializes the UI for the specified {@link Mode}.
     *
     * @param mode The mode to display.
     */
    protected void loadMode(Mode mode) {
        FragmentManager fragmentManager = getSupportFragmentManager();

        if (fragmentManager.findFragmentById(R.id.fragment_container) == null) {
            FragmentTransaction transaction = fragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
            if (mode == Mode.LAUNCH) {
                TreeFragment currentTaskFragment = new TreeFragment();
                Bundle args = new Bundle();
                args.putString(TreeFragment.FRAGMENT_TITLE,
                        getString(R.string.current_task_hierarchy_title));
                currentTaskFragment.setArguments(args);
                transaction.add(R.id.fragment_container, currentTaskFragment, TREE_FRAGMENT);
                transaction.add(R.id.fragment_container, new IntentFragment());
                transaction.commit();

                mStatus = Mode.LAUNCH;
            }
        }
    }

    /**
     * Launches activity with the selected options.
     */
    @Override
    public void launchActivity(Intent intent, boolean forResult) {
        if (forResult) {
            startActivityForResult(intent, LAUNCH_FOR_RESULT_ID);
        } else {
            startActivity(intent);
        }
        
        // If people press back we want them to see the overview rather than the launch fragment.
        // To achieve this we pop the launchFragment from the stack when we go to the next activity.
        getSupportFragmentManager().popBackStack();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.app_bar_test:
                runIntentTests();
                break;
            case R.id.app_bar_launch_default:
                askToLaunchTasks();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void askToLaunchTasks() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(R.string.launch_explanation)
                .setTitle(R.string.ask_to_launch)
                .setPositiveButton(R.string.ask_to_launch_affirm, (dialogInterface, i) -> {
                    setupTaskPreset().startActivities(TestBase.LaunchStyle.TASK_STACK_BUILDER);
                    dialogInterface.dismiss();
                })
                .setNegativeButton(R.string.ask_to_launch_cancel, (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                })
                .create();
        dialog.show();
    }

    protected TestBase setupTaskPreset() {
        Node mRoot = Node.newRootNode();
        // Describe initial setup of tasks
        // create singleTask, singleInstance, and two documents in separate tasks
        Node singleTask = newTaskNode()
                .addChild(new Node(new ComponentName(this, SingleTaskActivity.class)));
        Node docLaunchAlways = newTaskNode()
                .addChild(new Node(new ComponentName(this, DocumentLaunchAlwaysActivity.class)));
        Node docLaunchInto = newTaskNode()
                .addChild(new Node(new ComponentName(this, DocumentLaunchIntoActivity.class)));
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
        return new TestBase(this, mRoot);
    }

    protected void runIntentTests() {
        final Intent intent = getPackageManager()
            .getLaunchIntentForPackage("com.example.android.intentplayground.test");
        if (intent != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this,
                R.string.launch_testing_activities_failed, Toast.LENGTH_LONG).show();
        }
    }

    protected Intent prepareLaunchForward() {
        Intent intent = getIntent();
        Intent nextIntent = null;
        if (intent.hasExtra(EXTRA_LAUNCH_FORWARD)) {
            Log.e(getLocalClassName(), "It's happening! LAUNCH_FORWARD");
            ArrayList<Intent> intents = intent.getParcelableArrayListExtra(EXTRA_LAUNCH_FORWARD);
            if (!intents.isEmpty()) {
                nextIntent = intents.remove(0);
                nextIntent.putParcelableArrayListExtra(EXTRA_LAUNCH_FORWARD, intents);
                if (BuildConfig.DEBUG) {
                    Log.d(getLocalClassName(), EXTRA_LAUNCH_FORWARD + " "
                            + nextIntent.getComponent().toString());
                }
            }
        }
        return nextIntent;
    }

    /**
     * Sets a public field for the purpose of testing.
     */
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        userLeaveHintWasCalled = true;
    }
}
