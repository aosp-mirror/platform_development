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

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import java.util.ArrayList;

/**
 * Implements the shared functionality for all of the other activities.
 */
public abstract class BaseActivity extends AppCompatActivity implements
        IntentBuilderView.OnLaunchCallback {
    public final static String EXTRA_LAUNCH_FORWARD = "com.example.android.launchForward";
    public final static String BUILDER_VIEW = "com.example.android.builderFragment";
    public static final String TREE_FRAGMENT =  "com.example.android.treeFragment";
    public static final String EXPECTED_TREE_FRAGMENT = "com.example.android.expectedTreeFragment";
    public static final int LAUNCH_REQUEST_CODE = 0xEF;
    public enum Mode {LAUNCH, VERIFY, RESULT}
    public boolean userLeaveHintWasCalled = false;
    protected Mode mStatus = Mode.LAUNCH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (BuildConfig.DEBUG) Log.d(getLocalClassName(), "onCreate()");
        // Setup action bar
        Toolbar appBar = findViewById(R.id.app_bar);
        setSupportActionBar(appBar);
        loadMode(Mode.LAUNCH);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent launchForward = prepareLaunchForward();
        if (launchForward != null) {
            startActivity(launchForward);
        }
    }

    /**
     * Initializes the UI for the specified {@link Mode}.
     * @param mode The mode to display.
     */
    protected void loadMode(Mode mode) {
        Intent intent = getIntent();
        ViewGroup container = findViewById(R.id.fragment_container);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction()
                .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
        if (mode == Mode.LAUNCH) {
            transaction.replace(R.id.fragment_container, new CurrentTaskFragment());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                TreeFragment currentTaskFragment = new TreeFragment();
                Bundle args = new Bundle();
                args.putString(TreeFragment.FRAGMENT_TITLE,
                        getString(R.string.current_task_hierarchy_title));
                currentTaskFragment.setArguments(args);
                transaction.add(R.id.fragment_container, currentTaskFragment, TREE_FRAGMENT);
            }
            transaction.add(R.id.fragment_container, new IntentFragment());
            transaction.commit();
            // Ensure IntentBuilderView is last by adding it to the container after commit()
            transaction.runOnCommit(() -> {
                IntentBuilderView builderView = new IntentBuilderView(this, mode);
                builderView.setOnLaunchCallback(this::launchActivity);
                View bottomAnchorView = new View(this);
                bottomAnchorView.setId(R.id.fragment_container_bottom);
                container.addView(builderView);
                container.addView(bottomAnchorView);
            });
            mStatus = Mode.LAUNCH;
        }
    }

    /**
     * Launches activity with the selected options.
     */
    public void launchActivity(Intent intent) {
        startActivity(intent);
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
            case R.id.app_bar_help:
                showHelpDialog();
                break;
            case R.id.app_bar_test:
                runIntentTests();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void runIntentTests() {
        startActivity(getPackageManager()
                .getLaunchIntentForPackage("com.example.android.intentplayground.test"));
    }

    /**
     * Creates and displays a help overlay on this activity.
     */
    protected void showHelpDialog() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        LinearLayout container = findViewById(R.id.fragment_container);
        container.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);
        ShowcaseFragment demo = new ShowcaseFragment();
        demo.addStep(R.string.help_step_one, R.id.task_tree_container, () -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                TreeFragment frag = (TreeFragment) fragmentManager.findFragmentByTag(TREE_FRAGMENT);
                if (frag != null) {
                    frag.openTask(0);
                    frag.openTask(1);
                }
            }
        });
        demo.addStep(R.string.help_step_two, R.id.intent_container);
        demo.addStep(R.string.help_step_three, R.id.build_intent_container,
                R.id.build_intent_view);
        demo.addStep(R.string.help_step_four, R.id.fragment_container_bottom,
                R.id.launch_button);
        demo.setScroller((ScrollView) findViewById(R.id.scroll_container));
        demo.setOnFinish(() -> container.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE));
        fragmentManager.beginTransaction()
                .add(R.id.root_container, demo)
                .addToBackStack(null)
                .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                .commit();
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
