/*
* Copyright (C) 2012 The Android Open Source Project
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
package com.example.android.advancedimmersivemode;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.example.android.common.logger.Log;

/**
 * Demonstrates how to update the app's UI by toggling immersive mode.
 * Checkboxes are also made available for toggling other UI flags which can
 * alter the behavior of immersive mode.
 */
public class AdvancedImmersiveModeFragment extends Fragment {

    public static final String TAG = "AdvancedImmersiveModeFragment";
    public CheckBox mHideNavCheckbox;
    public CheckBox mHideStatusBarCheckBox;
    public CheckBox mImmersiveModeCheckBox;
    public CheckBox mImmersiveModeStickyCheckBox;
    public CheckBox mLowProfileCheckBox;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final View decorView = getActivity().getWindow().getDecorView();
        ViewGroup parentView = (ViewGroup) getActivity().getWindow().getDecorView()
                .findViewById(R.id.sample_main_layout);

        mLowProfileCheckBox = new CheckBox(getActivity());
        mLowProfileCheckBox.setText("Enable Low Profile mode.");
        parentView.addView(mLowProfileCheckBox);

        mHideNavCheckbox = new CheckBox(getActivity());
        mHideNavCheckbox.setChecked(true);
        mHideNavCheckbox.setText("Hide Navigation bar");
        parentView.addView(mHideNavCheckbox);

        mHideStatusBarCheckBox = new CheckBox(getActivity());
        mHideStatusBarCheckBox.setChecked(true);
        mHideStatusBarCheckBox.setText("Hide Status Bar");
        parentView.addView(mHideStatusBarCheckBox);

        mImmersiveModeCheckBox = new CheckBox(getActivity());
        mImmersiveModeCheckBox.setText("Enable Immersive Mode.");
        parentView.addView(mImmersiveModeCheckBox);

        mImmersiveModeStickyCheckBox = new CheckBox(getActivity());
        mImmersiveModeStickyCheckBox.setText("Enable Immersive Mode (Sticky)");
        parentView.addView(mImmersiveModeStickyCheckBox);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.sample_action) {
            toggleImmersiveMode();
        }
        return true;
    }

    /**
     * Detects and toggles immersive mode (also known as "hidey bar" mode).
     */
    public void toggleImmersiveMode() {

        // BEGIN_INCLUDE (get_current_ui_flags)
        // The "Decor View" is the parent view of the Activity.  It's also conveniently the easiest
        // one to find from within a fragment, since there's a handy helper method to pull it, and
        // we don't have to bother with picking a view somewhere deeper in the hierarchy and calling
        // "findViewById" on it.
        View decorView = getActivity().getWindow().getDecorView();
        int uiOptions = decorView.getSystemUiVisibility();
        int newUiOptions = uiOptions;
        // END_INCLUDE (get_current_ui_flags)

        // BEGIN_INCLUDE (toggle_lowprofile_mode)
        // Low profile mode doesn't resize the screen at all, but it covers the nav & status bar
        // icons with black so they're less distracting.  Unlike "full screen" and "hide nav bar,"
        // this mode doesn't interact with immersive mode at all, but it's instructive when running
        // this sample to observe the differences in behavior.
        if (mLowProfileCheckBox.isChecked()) {
            newUiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
        } else {
            newUiOptions &= ~View.SYSTEM_UI_FLAG_LOW_PROFILE;
        }
        // END_INCLUDE (toggle_lowprofile_mode)

        // BEGIN_INCLUDE (toggle_fullscreen_mode)
        // When enabled, this flag hides non-critical UI, such as the status bar,
        // which usually shows notification icons, battery life, etc
        // on phone-sized devices.  The bar reappears when the user swipes it down.  When immersive
        // mode is also enabled, the app-drawable area expands, and when the status bar is swiped
        // down, it appears semi-transparently and slides in over the app, instead of pushing it
        // down.
        if (mHideStatusBarCheckBox.isChecked()) {
            newUiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        } else {
            newUiOptions &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
        }
        // END_INCLUDE (toggle_fullscreen_mode)

        // BEGIN_INCLUDE (toggle_hidenav_mode)
        // When enabled, this flag hides the black nav bar along the bottom,
        // where the home/back buttons are.  The nav bar normally instantly reappears
        // when the user touches the screen.  When immersive mode is also enabled, the nav bar
        // stays hidden until the user swipes it back.
        if (mHideNavCheckbox.isChecked()) {
            newUiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        } else {
            newUiOptions &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }
        // END_INCLUDE (toggle_hidenav_mode)

        // BEGIN_INCLUDE (toggle_immersive_mode)
        // Immersive mode doesn't do anything without at least one of the previous flags
        // enabled.  When enabled, it allows the user to swipe the status and/or nav bars
        // off-screen.  When the user swipes the bars back onto the screen, the flags are cleared
        // and immersive mode is automatically disabled.
        if (mImmersiveModeCheckBox.isChecked()) {
            newUiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE;
        } else {
            newUiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE;
        }
        // END_INCLUDE (toggle_immersive_mode)

        // BEGIN_INCLUDE (toggle_immersive_mode_sticky)
        // There's actually two forms of immersive mode, normal and "sticky".  Sticky immersive mode
        // is different in 2 key ways:
        //
        // * Uses semi-transparent bars for the nav and status bars
        // * This UI flag will *not* be cleared when the user interacts with the UI.
        //   When the user swipes, the bars will temporarily appear for a few seconds and then
        //   disappear again.
        if (mImmersiveModeStickyCheckBox.isChecked()) {
            newUiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        } else {
            newUiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        // END_INCLUDE (toggle_immersive_mode_sticky)

        // BEGIN_INCLUDE (set_ui_flags)
        //Set the new UI flags.
        decorView.setSystemUiVisibility(newUiOptions);
        Log.i(TAG, "Current height: " + decorView.getHeight() + ", width: " + decorView.getWidth());
        // END_INCLUDE (set_ui_flags)
    }
}
