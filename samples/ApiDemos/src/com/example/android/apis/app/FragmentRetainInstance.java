/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.example.android.apis.app;

import com.example.android.apis.R;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * This example shows how you can use a Fragment to easily propagate state
 * (such as threads) across activity instances when an activity needs to be
 * restarted due to, for example, a configuration change.  This is a lot
 * easier than using the raw Activity.onRetainNonConfiguratinInstance() API.
 */
public class FragmentRetainInstance extends Activity {
    RetainedFragment mRetainedFragment;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_retain_instance);
        
        // Watch for button clicks.
        Button button = (Button)findViewById(R.id.restart);
        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mRetainedFragment.restart();
            }
        });
        
        // Check to see if we retained the fragment.
        mRetainedFragment = (RetainedFragment)findFragmentByTag("retained");
        
        // If not retained (or first time running), we need to re-create it.
        if (mRetainedFragment == null) {
            mRetainedFragment = new RetainedFragment();
            openFragmentTransaction().add(mRetainedFragment, "retained").commit();
        }
    }
    
    /**
     * This is the Fragment implementation that will be retained across
     * activity instances.  It represents some ongoing work, here a thread
     * we have that sits around incrementing a progress indicator.
     */
    static class RetainedFragment extends Fragment {
        ProgressBar mProgressBar;
        int mPosition;
        boolean mReady = false;
        boolean mQuiting = false;
        
        /**
         * This is the thread that will do our work.  It sits in a loop running
         * the progress up until it has reached the top, then stops and waits.
         */
        final Thread mThread = new Thread() {
            @Override
            public void run() {
                // We'll figure the real value out later.
                int max = 10000;
                
                // This thread runs almost forever.
                while (true) {
                    
                    // Update our shared state with the UI.
                    synchronized (this) {
                        // Our thread is stopped if the UI is not ready
                        // or it has completed its work.
                        while (!mReady || mPosition >= max) {
                            if (mQuiting) {
                                return;
                            }
                            try {
                                wait();
                            } catch (InterruptedException e) {
                            }
                        }
                        
                        // Now update the progress.  Note it is important that
                        // we touch the progress bar with the lock held, so it
                        // doesn't disappear on us.
                        mPosition++;
                        max = mProgressBar.getMax();
                        mProgressBar.setProgress(mPosition);
                    }
                    
                    // Normally we would be doing some work, but put a kludge
                    // here to pretend like we are.
                    synchronized (this) {
                        try {
                            wait(50);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        };
        
        /**
         * Fragment initialization.  We way we want to be retained and
         * start our thread.
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            
            // Tell the framework to try to keep this fragment around
            // during a configuration change.
            setRetainInstance(true);
            
            // Start up the worker thread.
            mThread.start();
        }

        /**
         * This is called when the Fragment's Activity is ready to go, after
         * its content view has been installed; it is called both after
         * the initial fragment creation and after the fragment is re-attached
         * to a new activity.
         */
        @Override
        public void onReady(Bundle savedInstanceState) {
            super.onReady(savedInstanceState);
            
            // Retrieve the progress bar from the current activity.
            mProgressBar = (ProgressBar)getActivity().findViewById(
                    R.id.progress_horizontal);
            
            // We are ready for our thread to go.
            synchronized (mThread) {
                mReady = true;
                mThread.notify();
            }
        }

        /**
         * This is called when the fragment is going away.  It is NOT called
         * when the fragment is being propagated between activity instances.
         */
        @Override
        public void onDestroy() {
            // Make the thread go away.
            synchronized (mThread) {
                mReady = false;
                mQuiting = true;
                mThread.notify();
            }
            
            super.onDestroy();
        }

        /**
         * This is called right before the fragment is detached from its
         * current activity instance.
         */
        @Override
        public void onDetach() {
            // This fragment is being detached from its activity.  We need
            // to make sure its thread is not going to touch any activity
            // state after returning from this function.
            synchronized (mThread) {
                mProgressBar = null;
                mReady = false;
                mThread.notify();
            }
            
            super.onDetach();
        }
        
        /**
         * API for our UI to restart the progress thread.
         */
        public void restart() {
            synchronized (mThread) {
                mPosition = 0;
                mThread.notify();
            }
        }
    }
}
