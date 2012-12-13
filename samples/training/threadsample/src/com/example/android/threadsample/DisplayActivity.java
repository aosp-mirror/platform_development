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
package com.example.android.threadsample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

/**
 * This activity displays Picasa's current featured images. It uses a service running
 * a background thread to download Picasa's "featured image" RSS feed.
 * <p>
 * An IntentHandler is used to communicate between the active Fragment and this
 * activity. This pattern simulates some of the communication used between
 * activities, and allows this activity to make choices of how to manage the
 * fragments.
 */
public class DisplayActivity extends FragmentActivity implements OnBackStackChangedListener {
    
    // A handle to the main screen view
    View mMainView;
    
    // An instance of the status broadcast receiver
    DownloadStateReceiver mDownloadStateReceiver;
    
    // Tracks whether Fragments are displaying side-by-side
    boolean mSideBySide;
    
    // Tracks whether navigation should be hidden
    boolean mHideNavigation;
    
    // Tracks whether the app is in full-screen mode
    boolean mFullScreen;
    
    // Tracks the number of Fragments on the back stack
    int mPreviousStackCount;
    
    // Instantiates a new broadcast receiver for handling Fragment state
    private FragmentDisplayer mFragmentDisplayer = new FragmentDisplayer();
    
    // Sets a tag to use in logging
    private static final String CLASS_TAG = "DisplayActivity";

    /**
     * Sets full screen mode on the device, by setting parameters in the current
     * window and View
     * @param fullscreen
     */
    public void setFullScreen(boolean fullscreen) {
        // If full screen is set, sets the fullscreen flag in the Window manager
        getWindow().setFlags(
                fullscreen ? WindowManager.LayoutParams.FLAG_FULLSCREEN : 0,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // Sets the global fullscreen flag to the current setting
        mFullScreen = fullscreen;

        // If the platform version is Android 3.0 (Honeycomb) or above
        if (Build.VERSION.SDK_INT >= 11) {
            
            // Sets the View to be "low profile". Status and navigation bar icons will be dimmed
            int flag = fullscreen ? View.SYSTEM_UI_FLAG_LOW_PROFILE : 0;
            
            // If the platform version is Android 4.0 (ICS) or above
            if (Build.VERSION.SDK_INT >= 14 && fullscreen) {
                
                // Hides all of the navigation icons
                flag |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }
            
            // Applies the settings to the screen View
            mMainView.setSystemUiVisibility(flag);

            // If the user requests a full-screen view, hides the Action Bar.
            if ( fullscreen ) {
                this.getActionBar().hide();
            } else {
                this.getActionBar().show();
            }
        }
    }

    /*
     * A callback invoked when the task's back stack changes. This allows the app to
     * move to the previous state of the Fragment being displayed.
     *
     */
    @Override
    public void onBackStackChanged() {
        
        // Gets the previous global stack count
        int previousStackCount = mPreviousStackCount;
        
        // Gets a FragmentManager instance
        FragmentManager localFragmentManager = getSupportFragmentManager();
        
        // Sets the current back stack count
        int currentStackCount = localFragmentManager.getBackStackEntryCount();
        
        // Re-sets the global stack count to be the current count
        mPreviousStackCount = currentStackCount;
        
        /*
         * If the current stack count is less than the previous, something was popped off the stack
         * probably because the user clicked Back.
         */
        boolean popping = currentStackCount < previousStackCount;
        Log.d(CLASS_TAG, "backstackchanged: popping = " + popping);
        
        // When going backwards in the back stack, turns off full screen mode.
        if (popping) {
            setFullScreen(false);
        }
    }

    /*
     * This callback is invoked by the system when the Activity is being killed
     * It saves the full screen status, so it can be restored when the Activity is restored
     *
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(Constants.EXTENDED_FULLSCREEN, mFullScreen);
        super.onSaveInstanceState(outState);
    }

    /*
     * This callback is invoked when the Activity is first created. It sets up the Activity's
     * window and initializes the Fragments associated with the Activity
     */
    @Override
    public void onCreate(Bundle stateBundle) {
        // Sets fullscreen-related flags for the display
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
        
        // Calls the super method (required)
        super.onCreate(stateBundle);
        
        // Inflates the main View, which will be the host View for the fragments
        mMainView = getLayoutInflater().inflate(R.layout.fragmenthost, null);
        
        // Sets the content view for the Activity
        setContentView(mMainView);
                
        /*
         * Creates an intent filter for DownloadStateReceiver that intercepts broadcast Intents
         */
        
        // The filter's action is BROADCAST_ACTION
        IntentFilter statusIntentFilter = new IntentFilter(
                Constants.BROADCAST_ACTION);
        
        // Sets the filter's category to DEFAULT
        statusIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        
        // Instantiates a new DownloadStateReceiver
        mDownloadStateReceiver = new DownloadStateReceiver();
        
        // Registers the DownloadStateReceiver and its intent filters
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mDownloadStateReceiver,
                statusIntentFilter);
        
        /*
         * Creates intent filters for the FragmentDisplayer
         */
        
        // One filter is for the action ACTION_VIEW_IMAGE
        IntentFilter displayerIntentFilter = new IntentFilter(
                Constants.ACTION_VIEW_IMAGE);
        
        // Adds a data filter for the HTTP scheme
        displayerIntentFilter.addDataScheme("http");
        
        // Registers the receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mFragmentDisplayer,
                displayerIntentFilter);
       
        // Creates a second filter for ACTION_ZOOM_IMAGE
        displayerIntentFilter = new IntentFilter(Constants.ACTION_ZOOM_IMAGE);
        
        // Registers the receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mFragmentDisplayer,
                displayerIntentFilter);
        
        // Gets an instance of the support library FragmentManager
        FragmentManager localFragmentManager = getSupportFragmentManager();
        
        /*
         * Detects if side-by-side display should be enabled. It's only available on xlarge and
         * sw600dp devices (for example, tablets). The setting in res/values/ is "false", but this
         * is overridden in values-xlarge and values-sw600dp.
         */
        mSideBySide = getResources().getBoolean(R.bool.sideBySide);
        
        /*
         * Detects if hiding navigation controls should be enabled. On xlarge andsw600dp, it should
         * be false, to avoid having the user enter an additional tap.
         */
        mHideNavigation = getResources().getBoolean(R.bool.hideNavigation);
        
        /*
         * Adds the back stack change listener defined in this Activity as the listener for the
         * FragmentManager. See the method onBackStackChanged().
         */
        localFragmentManager.addOnBackStackChangedListener(this);

        // If the incoming state of the Activity is null, sets the initial view to be thumbnails
        if (null == stateBundle) {
            
            // Starts a Fragment transaction to track the stack
            FragmentTransaction localFragmentTransaction = localFragmentManager
                    .beginTransaction();
            
            // Adds the PhotoThumbnailFragment to the host View
            localFragmentTransaction.add(R.id.fragmentHost,
                    new PhotoThumbnailFragment(), Constants.THUMBNAIL_FRAGMENT_TAG);
            
            // Commits this transaction to display the Fragment
            localFragmentTransaction.commit();
            
        // The incoming state of the Activity isn't null.
        } else {
            
            // Gets the previous state of the fullscreen indicator
            mFullScreen = stateBundle.getBoolean(Constants.EXTENDED_FULLSCREEN);
            
            // Sets the fullscreen flag to its previous state
            setFullScreen(mFullScreen);
            
            // Gets the previous backstack entry count.
            mPreviousStackCount = localFragmentManager.getBackStackEntryCount();
        }
    }

    /*
     * This callback is invoked when the system is about to destroy the Activity.
     */
    @Override
    public void onDestroy() {
        
        // If the DownloadStateReceiver still exists, unregister it and set it to null
        if (mDownloadStateReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mDownloadStateReceiver);
            mDownloadStateReceiver = null;
        }
        
        // Unregisters the FragmentDisplayer instance
        LocalBroadcastManager.getInstance(this).unregisterReceiver(this.mFragmentDisplayer);
        
        // Sets the main View to null
        mMainView = null;
        
        // Must always call the super method at the end.
        super.onDestroy();
    }

    /*
     * This callback is invoked when the system is stopping the Activity. It stops
     * background threads.
     */
    @Override
    protected void onStop() {
        
        // Cancel all the running threads managed by the PhotoManager
        PhotoManager.cancelAll();
        super.onStop();
    }

    /**
     * This class uses the BroadcastReceiver framework to detect and handle status messages from
     * the service that downloads URLs.
     */
    private class DownloadStateReceiver extends BroadcastReceiver {
        
        private DownloadStateReceiver() {
            
            // prevents instantiation by other packages.
        }
        /**
         *
         * This method is called by the system when a broadcast Intent is matched by this class'
         * intent filters
         *
         * @param context An Android context
         * @param intent The incoming broadcast Intent
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            
            /*
             * Gets the status from the Intent's extended data, and chooses the appropriate action
             */
            switch (intent.getIntExtra(Constants.EXTENDED_DATA_STATUS,
                    Constants.STATE_ACTION_COMPLETE)) {
                
                // Logs "started" state
                case Constants.STATE_ACTION_STARTED:
                    if (Constants.LOGD) {
                        
                        Log.d(CLASS_TAG, "State: STARTED");
                    }
                    break;
                // Logs "connecting to network" state
                case Constants.STATE_ACTION_CONNECTING:
                    if (Constants.LOGD) {
                        
                        Log.d(CLASS_TAG, "State: CONNECTING");
                    }
                    break;
                 // Logs "parsing the RSS feed" state
                 case Constants.STATE_ACTION_PARSING:
                    if (Constants.LOGD) {
                        
                        Log.d(CLASS_TAG, "State: PARSING");
                    }
                    break;
                // Logs "Writing the parsed data to the content provider" state
                case Constants.STATE_ACTION_WRITING:
                    if (Constants.LOGD) {
                        
                        Log.d(CLASS_TAG, "State: WRITING");
                    }
                    break;
                // Starts displaying data when the RSS download is complete
                case Constants.STATE_ACTION_COMPLETE:
                    // Logs the status
                    if (Constants.LOGD) {
                        
                        Log.d(CLASS_TAG, "State: COMPLETE");
                    }

                    // Finds the fragment that displays thumbnails
                    PhotoThumbnailFragment localThumbnailFragment =
                            (PhotoThumbnailFragment) getSupportFragmentManager().findFragmentByTag(
                                    Constants.THUMBNAIL_FRAGMENT_TAG);
                    
                    // If the thumbnail Fragment is hidden, don't change its display status
                    if ((localThumbnailFragment == null)
                            || (!localThumbnailFragment.isVisible()))
                        return;
                    
                    // Indicates that the thumbnail Fragment is visible
                    localThumbnailFragment.setLoaded(true);
                    break;
                default:
                    break;
            }
        }
    }
    
    /**
     * This class uses the broadcast receiver framework to detect incoming broadcast Intents
     * and change the currently-visible fragment based on the Intent action.
     * It adds or replaces Fragments as necessary, depending on how much screen real-estate is
     * available.
     */
    private class FragmentDisplayer extends BroadcastReceiver {
        
        // Default null constructor
        public FragmentDisplayer() {
            
            // Calls the constructor for BroadcastReceiver
            super();
        }
        /**
         * Receives broadcast Intents for viewing or zooming pictures, and displays the
         * appropriate Fragment.
         *
         * @param context The current Context of the callback
         * @param intent The broadcast Intent that triggered the callback
         */
        @Override
        public void onReceive(Context context, Intent intent) {

            // Declares a local FragmentManager instance
            FragmentManager fragmentManager1;
            
            // Declares a local instance of the Fragment that displays photos
            PhotoFragment photoFragment;
            
            // Stores a string representation of the URL in the incoming Intent
            String urlString;
            
            // If the incoming Intent is a request is to view an image
            if (intent.getAction().equals(Constants.ACTION_VIEW_IMAGE)) {
                
                // Gets an instance of the support library fragment manager
                fragmentManager1 = getSupportFragmentManager();
                
                // Gets a handle to the Fragment that displays photos
                photoFragment =
                        (PhotoFragment) fragmentManager1.findFragmentByTag(
                            Constants.PHOTO_FRAGMENT_TAG
                );
                
                // Gets the URL of the picture to display
                urlString = intent.getDataString();
                
                // If the photo Fragment exists from a previous display
                if (null != photoFragment) {
                    
                    // If the incoming URL is not already being displayed
                    if (!urlString.equals(photoFragment.getURLString())) {
                        
                        // Sets the Fragment to use the URL from the Intent for the photo
                        photoFragment.setPhoto(urlString);
                        
                        // Loads the photo into the Fragment
                        photoFragment.loadPhoto();
                    }
                
                // If the Fragment doesn't already exist
                } else {
                    // Instantiates a new Fragment
                    photoFragment = new PhotoFragment();
                    
                    // Sets the Fragment to use the URL from the Intent for the photo
                    photoFragment.setPhoto(urlString);
                    
                    // Starts a new Fragment transaction
                    FragmentTransaction localFragmentTransaction2 =
                            fragmentManager1.beginTransaction();
                    
                    // If the fragments are side-by-side, adds the photo Fragment to the display
                    if (mSideBySide) {
                        localFragmentTransaction2.add(
                                R.id.fragmentHost,
                                photoFragment,
                                Constants.PHOTO_FRAGMENT_TAG
                        );
                    /*
                     * If the Fragments are not side-by-side, replaces the current Fragment with
                     * the photo Fragment
                     */
                    } else {
                        localFragmentTransaction2.replace(
                                R.id.fragmentHost,
                                photoFragment,
                                Constants.PHOTO_FRAGMENT_TAG);
                    }
                    
                    // Don't remember the transaction (sets the Fragment backstack to null)
                    localFragmentTransaction2.addToBackStack(null);
                    
                    // Commits the transaction
                    localFragmentTransaction2.commit();
                }
                
                // If not in side-by-side mode, sets "full screen", so that no controls are visible
                if (!mSideBySide) setFullScreen(true);
                
            /*
             * If the incoming Intent is a request to zoom in on an existing image
             * (Notice that zooming is only supported on large-screen devices)
             */
            } else if (intent.getAction().equals(Constants.ACTION_ZOOM_IMAGE)) {
                
                // If the Fragments are being displayed side-by-side
                if (mSideBySide) {
                    
                    // Gets another instance of the FragmentManager
                    FragmentManager localFragmentManager2 = getSupportFragmentManager();
                    
                    // Gets a thumbnail Fragment instance
                    PhotoThumbnailFragment localThumbnailFragment =
                            (PhotoThumbnailFragment) localFragmentManager2.findFragmentByTag(
                                Constants.THUMBNAIL_FRAGMENT_TAG);
                    
                    // If the instance exists from a previous display
                    if (null != localThumbnailFragment) {
                        
                        // if the existing instance is visible
                        if (localThumbnailFragment.isVisible()) {
                            
                            // Starts a fragment transaction
                            FragmentTransaction localFragmentTransaction2 =
                                    localFragmentManager2.beginTransaction();
                            
                            /*
                             * Hides the current thumbnail, clears the backstack, and commits the
                             * transaction
                             */
                            localFragmentTransaction2.hide(localThumbnailFragment);
                            localFragmentTransaction2.addToBackStack(null);
                            localFragmentTransaction2.commit();
                        
                        // If the existing instance is not visible, display it by going "Back"
                        } else {
                            
                            // Pops the back stack to show the previous Fragment state
                            localFragmentManager2.popBackStack();
                        }
                    }
                    
                    // Removes controls from the screen
                    setFullScreen(true);
                }
            }
        }
    }
    
}
