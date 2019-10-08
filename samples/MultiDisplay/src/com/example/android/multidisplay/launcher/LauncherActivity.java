/**
 * Copyright (c) 2018 The Android Open Source Project
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

package com.example.android.multidisplay.launcher;

import static com.example.android.multidisplay.launcher.PinnedAppListViewModel.PINNED_APPS_KEY;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Spinner;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory;

import com.example.android.multidisplay.R;
import com.google.android.material.circularreveal.cardview.CircularRevealCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Main launcher activity. It's launch mode is configured as "singleTop" to allow showing on
 * multiple displays and to ensure a single instance per each display.
 */
public class LauncherActivity extends FragmentActivity implements AppPickedCallback,
        PopupMenu.OnMenuItemClickListener {

    private Spinner mDisplaySpinner;
    private ArrayAdapter<DisplayItem> mDisplayAdapter;
    private int mSelectedDisplayId = Display.INVALID_DISPLAY;
    private View mScrimView;
    private AppListAdapter mAppListAdapter;
    private AppListAdapter mPinnedAppListAdapter;
    private CircularRevealCardView mAppDrawerView;
    private FloatingActionButton mFab;
    private CheckBox mNewInstanceCheckBox;

    private boolean mAppDrawerShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mScrimView = findViewById(R.id.Scrim);
        mAppDrawerView = findViewById(R.id.FloatingSheet);
        mFab = findViewById(R.id.FloatingActionButton);

        mFab.setOnClickListener((View v) -> {
            showAppDrawer(true);
        });

        mScrimView.setOnClickListener((View v) -> {
            showAppDrawer(false);
        });

        mDisplaySpinner = findViewById(R.id.spinner);
        mDisplaySpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mSelectedDisplayId = mDisplayAdapter.getItem(i).mId;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                mSelectedDisplayId = Display.INVALID_DISPLAY;
            }
        });
        mDisplayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new ArrayList<DisplayItem>());
        mDisplayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDisplaySpinner.setAdapter(mDisplayAdapter);

        final ViewModelProvider viewModelProvider = new ViewModelProvider(getViewModelStore(),
                new AndroidViewModelFactory((Application) getApplicationContext()));

        mPinnedAppListAdapter = new AppListAdapter(this);
        final GridView pinnedAppGridView = findViewById(R.id.pinned_app_grid);
        pinnedAppGridView.setAdapter(mPinnedAppListAdapter);
        pinnedAppGridView.setOnItemClickListener((adapterView, view, position, id) -> {
            final AppEntry entry = mPinnedAppListAdapter.getItem(position);
            launch(entry.getLaunchIntent());
        });
        final PinnedAppListViewModel pinnedAppListViewModel =
                viewModelProvider.get(PinnedAppListViewModel.class);
        pinnedAppListViewModel.getPinnedAppList().observe(this, data -> {
            mPinnedAppListAdapter.setData(data);
        });

        mAppListAdapter = new AppListAdapter(this);
        final GridView appGridView = findViewById(R.id.app_grid);
        appGridView.setAdapter(mAppListAdapter);
        appGridView.setOnItemClickListener((adapterView, view, position, id) -> {
            final AppEntry entry = mAppListAdapter.getItem(position);
            launch(entry.getLaunchIntent());
        });
        final AppListViewModel appListViewModel = viewModelProvider.get(AppListViewModel.class);
        appListViewModel.getAppList().observe(this, data -> {
            mAppListAdapter.setData(data);
        });

        findViewById(R.id.RefreshButton).setOnClickListener(this::refreshDisplayPicker);
        mNewInstanceCheckBox = findViewById(R.id.NewInstanceCheckBox);

        ImageButton optionsButton = findViewById(R.id.OptionsButton);
        optionsButton.setOnClickListener((View v) -> {
            PopupMenu popup = new PopupMenu(this,v);
            popup.setOnMenuItemClickListener(this);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.context_menu, popup.getMenu());
            popup.show();
        });
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        // Respond to picking one of the popup menu items.
        switch (item.getItemId()) {
            case R.id.add_app_shortcut:
                FragmentManager fm = getSupportFragmentManager();
                PinnedAppPickerDialog pickerDialogFragment =
                        PinnedAppPickerDialog.newInstance(mAppListAdapter, this);
                pickerDialogFragment.show(fm, "fragment_app_picker");
                return true;
            case R.id.set_wallpaper:
                Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
                startActivity(Intent.createChooser(intent, getString(R.string.set_wallpaper)));
                return true;
            default:
                return true;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        showAppDrawer(false);
    }

    public void onBackPressed() {
        // If the app drawer was shown - hide it. Otherwise, not doing anything since we don't want
        // to close the launcher.
        showAppDrawer(false);
    }

    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            // Hide keyboard.
            final View v = getWindow().peekDecorView();
            if (v != null && v.getWindowToken() != null) {
                getSystemService(InputMethodManager.class).hideSoftInputFromWindow(
                        v.getWindowToken(), 0);
            }
        }

        // A new intent will bring the launcher to top. Hide the app drawer to reset the state.
        showAppDrawer(false);
    }

    void launch(Intent launchIntent) {
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (mNewInstanceCheckBox.isChecked()) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        }
        final ActivityOptions options = ActivityOptions.makeBasic();
        if (mSelectedDisplayId != Display.INVALID_DISPLAY) {
            options.setLaunchDisplayId(mSelectedDisplayId);
        }
        try {
            startActivity(launchIntent, options.toBundle());
        } catch (Exception e) {
            final AlertDialog.Builder builder =
                    new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
            builder.setTitle(R.string.couldnt_launch)
                    .setMessage(e.getLocalizedMessage())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    private void refreshDisplayPicker() {
        refreshDisplayPicker(mAppDrawerView);
    }

    private void refreshDisplayPicker(View view) {
        final int currentDisplayId = view.getDisplay().getDisplayId();
        final DisplayManager dm = getSystemService(DisplayManager.class);
        mDisplayAdapter.setNotifyOnChange(false);
        mDisplayAdapter.clear();
        mDisplayAdapter.add(new DisplayItem(Display.INVALID_DISPLAY, "Do not specify display"));

        for (Display display : dm.getDisplays()) {
            final int id = display.getDisplayId();
            final boolean isDisplayPrivate = (display.getFlags() & Display.FLAG_PRIVATE) != 0;
            final boolean isCurrentDisplay = id == currentDisplayId;
            final StringBuilder sb = new StringBuilder();
            sb.append(id).append(": ").append(display.getName());
            if (isDisplayPrivate) {
                sb.append(" (private)");
            }
            if (isCurrentDisplay) {
                sb.append(" [Current display]");
            }
            mDisplayAdapter.add(new DisplayItem(id, sb.toString()));
        }

        mDisplayAdapter.notifyDataSetChanged();
    }

    /**
     * Store the picked app to persistent pinned list and update the loader.
     */
    @Override
    public void onAppPicked(AppEntry appEntry) {
        final SharedPreferences sp = getSharedPreferences(PINNED_APPS_KEY, 0);
        Set<String> pinnedApps = sp.getStringSet(PINNED_APPS_KEY, null);
        if (pinnedApps == null) {
            pinnedApps = new HashSet<String>();
        } else {
            // Always need to create a new object to make sure that the changes are persisted.
            pinnedApps = new HashSet<String>(pinnedApps);
        }
        pinnedApps.add(appEntry.getComponentName().flattenToString());

        final SharedPreferences.Editor editor = sp.edit();
        editor.putStringSet(PINNED_APPS_KEY, pinnedApps);
        editor.apply();
    }

    /**
     * Show/hide app drawer card with animation.
     */
    private void showAppDrawer(boolean show) {
        if (show == mAppDrawerShown) {
            return;
        }

        final Animator animator = revealAnimator(mAppDrawerView, show);
        if (show) {
            mAppDrawerShown = true;
            mAppDrawerView.setVisibility(View.VISIBLE);
            mScrimView.setVisibility(View.VISIBLE);
            mFab.setVisibility(View.INVISIBLE);
            refreshDisplayPicker();
        } else {
            mAppDrawerShown = false;
            mScrimView.setVisibility(View.INVISIBLE);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mAppDrawerView.setVisibility(View.INVISIBLE);
                    mFab.setVisibility(View.VISIBLE);
                }
            });
        }
        animator.start();
    }

    /**
     * Create reveal/hide animator for app list card.
     */
    private Animator revealAnimator(View view, boolean open) {
        final int radius = (int) Math.hypot((double) view.getWidth(), (double) view.getHeight());
        return ViewAnimationUtils.createCircularReveal(view, view.getRight(), view.getBottom(),
                open ? 0 : radius, open ? radius : 0);
    }

    private static class DisplayItem {
        final int mId;
        final String mDescription;

        DisplayItem(int displayId, String description) {
            mId = displayId;
            mDescription = description;
        }

        @Override
        public String toString() {
            return mDescription;
        }
    }
}
