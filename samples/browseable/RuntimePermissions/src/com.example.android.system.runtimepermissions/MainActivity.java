/*
* Copyright 2015 The Android Open Source Project
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

package com.example.android.system.runtimepermissions;

import com.example.android.common.activities.SampleActivityBase;
import com.example.android.common.logger.Log;
import com.example.android.common.logger.LogFragment;
import com.example.android.common.logger.LogWrapper;
import com.example.android.common.logger.MessageOnlyLogFilter;
import com.example.android.system.runtimepermissions.camera.CameraPreviewFragment;
import com.example.android.system.runtimepermissions.contacts.ContactsFragment;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.ViewAnimator;

/**
 * Launcher Activity that demonstrates the use of runtime permissions for Android M.
 * It contains a summary sample description, sample log and a Fragment that calls callbacks on this
 * Activity to illustrate parts of the runtime permissions API.
 * <p>
 * This Activity requests permissions to access the camera ({@link android.Manifest.permission#CAMERA})
 * when the 'Show Camera' button is clicked to display the camera preview.
 * Contacts permissions (({@link android.Manifest.permission#READ_CONTACTS} and ({@link
 * android.Manifest.permission#WRITE_CONTACTS})) are requested when the 'Show and Add Contacts'
 * button is
 * clicked to display the first contact in the contacts database and to add a dummy contact
 * directly
 * to it. First, permissions are checked if they have already been granted through {@link
 * android.app.Activity#checkSelfPermission(String)} (wrapped in {@link
 * PermissionUtil#hasSelfPermission(Activity, String)} and {@link PermissionUtil#hasSelfPermission(Activity,
 * String[])} for compatibility). If permissions have not been granted, they are requested through
 * {@link Activity#requestPermissions(String[], int)} and the return value checked in {@link
 * Activity#onRequestPermissionsResult(int, String[], int[])}.
 * <p>
 * Before requesting permissions, {@link Activity#shouldShowRequestPermissionRationale(String)}
 * should be called to provide the user with additional context for the use of permissions if they
 * have been denied previously.
 * <p>
 * If this sample is executed on a device running a platform version below M, all permissions
 * declared
 * in the Android manifest file are always granted at install time and cannot be requested at run
 * time.
 * <p>
 * This sample targets the M platform and must therefore request permissions at runtime. Change the
 * targetSdk in the file 'Application/build.gradle' to 22 to run the application in compatibility
 * mode.
 * Now, if a permission has been disable by the system through the application settings, disabled
 * APIs provide compatibility data.
 * For example the camera cannot be opened or an empty list of contacts is returned. No special
 * action is required in this case.
 * <p>
 * (This class is based on the MainActivity used in the SimpleFragment sample template.)
 */
public class MainActivity extends SampleActivityBase {

    public static final String TAG = "MainActivity";

    /**
     * Id to identify a camera permission request.
     */
    private static final int REQUEST_CAMERA = 0;

    /**
     * Id to identify a contacts permission request.
     */
    private static final int REQUEST_CONTACTS = 1;

    /**
     * Permissions required to read and write contacts. Used by the {@link ContactsFragment}.
     */
    private static String[] PERMISSIONS_CONTACT = {Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS};

    // Whether the Log Fragment is currently shown.
    private boolean mLogShown;


    /**
     * Called when the 'show camera' button is clicked.
     * Callback is defined in resource layout definition.
     */
    public void showCamera(View view) {
        Log.i(TAG, "Show camera button pressed. Checking permission.");
        // BEGIN_INCLUDE(camera_permission)
        // Check if the Camera permission is already available.
        if (PermissionUtil.hasSelfPermission(this, Manifest.permission.CAMERA)) {
            // Camera permissions is already available, show the camera preview.
            Log.i(TAG,
                    "CAMERA permission has already been granted. Displaying camera preview.");
            showCameraPreview();
        } else {
            // Camera permission has not been granted.
            Log.i(TAG, "CAMERA permission has NOT been granted. Requesting permission.");

            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                Log.i(TAG,
                        "Displaying camera permission rationale to provide additional context.");
                Toast.makeText(this, R.string.permission_camera_rationale, Toast.LENGTH_SHORT)
                        .show();
            }

            // Request Camera permission
            requestPermissions(new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA);
        }
        // END_INCLUDE(camera_permission)

    }

    /**
     * Called when the 'show camera' button is clicked.
     * Callback is defined in resource layout definition.
     */
    public void showContacts(View v) {
        Log.i(TAG, "Show contacts button pressed. Checking permissions.");
        // Verify that all required contact permissions have been granted.
        if (PermissionUtil.hasSelfPermission(this, PERMISSIONS_CONTACT)) {
            Log.i(TAG,
                    "Contact permissions have already been granted. Displaying contact details.");
            // Contact permissions have been granted. Show the contacts fragment.
            showContactDetails();
        } else {
            // Contacts permissions have not been granted.
            Log.i(TAG, "Contact permissions has NOT been granted. Requesting permission.");

            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                Log.i(TAG,
                        "Displaying contacts permission rationale to provide additional context.");
                Toast.makeText(this, R.string.permission_contacts_rationale, Toast.LENGTH_SHORT)
                        .show();
            }

            // contact permissions has not been granted (read and write contacts). Request them.
            requestPermissions(PERMISSIONS_CONTACT, REQUEST_CONTACTS);
        }
    }

    /**
     * Display the {@link CameraPreviewFragment} in the content area if the required Camera
     * permission has been granted.
     */
    private void showCameraPreview() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.sample_content_fragment, CameraPreviewFragment.newInstance())
                .addToBackStack("contacts")
                .commit();
    }

    /**
     * Display the {@link ContactsFragment} in the content area if the required contacts
     * permissions
     * have been granted.
     */
    private void showContactDetails() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.sample_content_fragment, ContactsFragment.newInstance())
                .addToBackStack("contacts")
                .commit();
    }


    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {

        if (requestCode == REQUEST_CAMERA) {
            // BEGIN_INCLUDE(permission_result)
            // Received permission result for camera permission.
            Log.i(TAG, "Received response for Camera permission request.");

            // Check if the only required permission has been granted
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission has been granted, preview can be displayed
                Log.i(TAG, "CAMERA permission has now been granted. Showing preview.");
                Toast.makeText(this, R.string.permision_available_camera, Toast.LENGTH_SHORT)
                        .show();
            } else {
                Log.i(TAG, "CAMERA permission was NOT granted.");
                Toast.makeText(this, R.string.permissions_not_granted, Toast.LENGTH_SHORT).show();

            }
            // END_INCLUDE(permission_result)

        } else if (requestCode == REQUEST_CONTACTS) {
            Log.i(TAG, "Received response for contact permissions request.");

            // We have requested multiple permissions for contacts, so all of them need to be
            // checked.
            if (PermissionUtil.verifyPermissions(grantResults)) {
                // All required permissions have been granted, display contacts fragment.
                Toast.makeText(this, R.string.permision_available_contacts, Toast.LENGTH_SHORT)
                        .show();
            } else {
                Log.i(TAG, "Contacts permissions were NOT granted.");
                Toast.makeText(this, R.string.permissions_not_granted, Toast.LENGTH_SHORT).show();
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /* Note: Methods and definitions below are only used to provide the UI for this sample and are
    not relevant for the execution of the runtime permissions API. */


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem logToggle = menu.findItem(R.id.menu_toggle_log);
        logToggle.setVisible(findViewById(R.id.sample_output) instanceof ViewAnimator);
        logToggle.setTitle(mLogShown ? R.string.sample_hide_log : R.string.sample_show_log);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_toggle_log:
                mLogShown = !mLogShown;
                ViewAnimator output = (ViewAnimator) findViewById(R.id.sample_output);
                if (mLogShown) {
                    output.setDisplayedChild(1);
                } else {
                    output.setDisplayedChild(0);
                }
                supportInvalidateOptionsMenu();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Create a chain of targets that will receive log data */
    @Override
    public void initializeLogging() {
        // Wraps Android's native log framework.
        LogWrapper logWrapper = new LogWrapper();
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        Log.setLogNode(logWrapper);

        // Filter strips out everything except the message text.
        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
        logWrapper.setNext(msgFilter);

        // On screen logging via a fragment with a TextView.
        LogFragment logFragment = (LogFragment) getSupportFragmentManager()
                .findFragmentById(R.id.log_fragment);
        msgFilter.setNext(logFragment.getLogView());
    }

    public void onBackClick(View view) {
        getSupportFragmentManager().popBackStack();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            RuntimePermissionsFragment fragment = new RuntimePermissionsFragment();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
        }

        // This method sets up our custom logger, which will print all log messages to the device
        // screen, as well as to adb logcat.
        initializeLogging();
    }
}
