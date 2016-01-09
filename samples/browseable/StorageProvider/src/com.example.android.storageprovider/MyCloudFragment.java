/*
* Copyright 2013 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/


package com.example.android.storageprovider;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;

import com.example.android.common.logger.Log;

/**
 * Toggles the user's login status via a login menu option, and enables/disables the cloud storage
 * content provider.
 */
public class MyCloudFragment extends Fragment {

    private static final String TAG = "MyCloudFragment";
    private static final String AUTHORITY = "com.example.android.storageprovider.documents";
    private boolean mLoggedIn = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLoggedIn = readLoginValue();

        setHasOptionsMenu(true);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem item = menu.findItem(R.id.sample_action);
        item.setTitle(mLoggedIn ? R.string.log_out : R.string.log_in);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.sample_action) {
            toggleLogin();
            item.setTitle(mLoggedIn ? R.string.log_out : R.string.log_in);

            // BEGIN_INCLUDE(notify_change)
            // Notify the system that the status of our roots has changed.  This will trigger
            // a call to MyCloudProvider.queryRoots() and force a refresh of the system
            // picker UI.  It's important to call this or stale results may persist.
            getActivity().getContentResolver().notifyChange(DocumentsContract.buildRootsUri
                    (AUTHORITY), null, false);
            // END_INCLUDE(notify_change)
        }
        return true;
    }

    /**
     * Dummy function to change the user's authorization status.
     */
    private void toggleLogin() {
        // Replace this with your standard method of authentication to determine if your app
        // should make the user's documents available.
        mLoggedIn = !mLoggedIn;
        writeLoginValue(mLoggedIn);
        Log.i(TAG, getString(mLoggedIn ? R.string.logged_in_info : R.string.logged_out_info));
    }

    /**
     * Dummy function to save whether the user is logged in.
     */
    private void writeLoginValue(boolean loggedIn) {
        final SharedPreferences sharedPreferences =
                getActivity().getSharedPreferences(getString(R.string.app_name),
                        getActivity().MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(getString(R.string.key_logged_in), loggedIn).commit();
    }

    /**
     * Dummy function to determine whether the user is logged in.
     */
    private boolean readLoginValue() {
        final SharedPreferences sharedPreferences =
                getActivity().getSharedPreferences(getString(R.string.app_name),
                        getActivity().MODE_PRIVATE);
        return sharedPreferences.getBoolean(getString(R.string.key_logged_in), false);
    }

}


