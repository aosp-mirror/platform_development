/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.example.android.hcgallery;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

public class MainActivity extends Activity implements ActionBar.TabListener {

    private View mActionBarView;
    private Animator mCurrentTitlesAnimator;
    private String[] mToggleLabels = {"Show Titles", "Hide Titles"};
    private int mLabelIndex = 1;
    private int mThemeId = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null && savedInstanceState.getInt("theme", -1) != -1) {
            mThemeId = savedInstanceState.getInt("theme");
            this.setTheme(mThemeId);
        }

        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.main);

        Directory.initializeDirectory();

        ActionBar bar = getActionBar();

        if (mThemeId == android.R.style.Theme_Holo_Light || mThemeId == -1) {
            bar.setBackgroundDrawable(
                    new ColorDrawable(getResources().getColor(R.color.actionbar_background_light)));
        } else {
            bar.setBackgroundDrawable(
                    new ColorDrawable(getResources().getColor(R.color.actionbar_background_dark)));
        }

        int i;
        for (i = 0; i < Directory.getCategoryCount(); i++) {
            bar.addTab(bar.newTab().setText(Directory.getCategory(i).getName())
                    .setTabListener(this));
        }

        mActionBarView = getLayoutInflater().inflate(
                R.layout.action_bar_custom, null);

        bar.setCustomView(mActionBarView);
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_USE_LOGO);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayShowHomeEnabled(true);

        // If category is not saved to the savedInstanceState,
        // 0 is returned by default.
        if(savedInstanceState != null) {
            int category = savedInstanceState.getInt("category");
            bar.selectTab(bar.getTabAt(category));
        }
    }

    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        TitlesFragment titleFrag = (TitlesFragment) getFragmentManager()
                .findFragmentById(R.id.frag_title);
        titleFrag.populateTitles(tab.getPosition());

        titleFrag.selectPosition(0);
    }

    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
    }

    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.camera:
            Intent intent = new Intent(this, CameraSample.class);
            intent.putExtra("theme", mThemeId);
            startActivity(intent);
            return true;
        case R.id.toggleTitles:
            toggleVisibleTitles();
            return true;

        case R.id.toggleTheme:
            if (mThemeId == android.R.style.Theme_Holo) {
                mThemeId = android.R.style.Theme_Holo_Light;
            } else {
                mThemeId = android.R.style.Theme_Holo;
            }
            this.recreate();
            return true;

        case R.id.showDialog:
            showDialog();
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public void toggleVisibleTitles() {
        // Use these for custom animations.
        final FragmentManager fm = getFragmentManager();
        final TitlesFragment f = (TitlesFragment) fm
                .findFragmentById(R.id.frag_title);
        final View titlesView = f.getView();
        mLabelIndex = 1 - mLabelIndex;

        // Determine if we're in portrait, and whether we're showing or hiding the titles
        // with this toggle.
        final boolean isPortrait = getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_PORTRAIT;

        final boolean shouldShow = f.isHidden() || mCurrentTitlesAnimator != null;

        // Cancel the current titles animation if there is one.
        if (mCurrentTitlesAnimator != null)
            mCurrentTitlesAnimator.cancel();

        // Begin setting up the object animator. We'll animate the bottom or right edge of the
        // titles view, as well as its alpha for a fade effect.
        ObjectAnimator objectAnimator = ObjectAnimator.ofPropertyValuesHolder(
                titlesView,
                PropertyValuesHolder.ofInt(
                        isPortrait ? "bottom" : "right",
                        shouldShow ? getResources().getDimensionPixelSize(R.dimen.titles_size)
                                   : 0),
                PropertyValuesHolder.ofFloat("alpha", shouldShow ? 1 : 0)
        );

        // At each step of the animation, we'll perform layout by calling setLayoutParams.
        final ViewGroup.LayoutParams lp = titlesView.getLayoutParams();
        objectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                // *** WARNING ***: triggering layout at each animation frame highly impacts
                // performance so you should only do this for simple layouts. More complicated
                // layouts can be better served with individual animations on child views to
                // avoid the performance penalty of layout.
                if (isPortrait) {
                    lp.height = (Integer) valueAnimator.getAnimatedValue();
                } else {
                    lp.width = (Integer) valueAnimator.getAnimatedValue();
                }
                titlesView.setLayoutParams(lp);
            }
        });

        if (shouldShow) {
            fm.beginTransaction().show(f).commit();
            objectAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    mCurrentTitlesAnimator = null;
                }
            });

        } else {
            objectAnimator.addListener(new AnimatorListenerAdapter() {
                boolean canceled;

                @Override
                public void onAnimationCancel(Animator animation) {
                    canceled = true;
                    super.onAnimationCancel(animation);
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    if (canceled)
                        return;
                    mCurrentTitlesAnimator = null;
                    fm.beginTransaction().hide(f).commit();
                }
            });
        }

        // Start the animation.
        objectAnimator.start();
        mCurrentTitlesAnimator = objectAnimator;

        invalidateOptionsMenu();
    }

    void showDialog() {

        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        DialogFragment newFragment = MyDialogFragment.newInstance("The Dialog Of Awesome");

        // Create and show the dialog.
        newFragment.show(ft, "dialog");
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.getItem(1).setTitle(mToggleLabels[mLabelIndex]);
        return true;
    }

    @Override
    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        ActionBar bar = getActionBar();
        int category = bar.getSelectedTab().getPosition();
        outState.putInt("category", category);
        outState.putInt("theme", mThemeId);
    }


    public static class MyDialogFragment extends DialogFragment {

        public static MyDialogFragment newInstance(String title) {
            MyDialogFragment frag = new MyDialogFragment();
            Bundle args = new Bundle();
            args.putString("title", title);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String title = getArguments().getString("title");

            return new AlertDialog.Builder(getActivity())
                    .setTitle(title)
                    .setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            }
                    )
                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            }
                    )
                    .create();
        }
    }
}
