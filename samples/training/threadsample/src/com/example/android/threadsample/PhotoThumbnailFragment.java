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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.CursorAdapter;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.RejectedExecutionException;

/**
 * PhotoThumbnailFragment displays a GridView of picture thumbnails downloaded from Picasa
 */
public class PhotoThumbnailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {
    
    private static final String STATE_IS_HIDDEN =
            "com.example.android.threadsample.STATE_IS_HIDDEN";
    
    // The width of each column in the grid
    private int mColumnWidth;
    
    // A Drawable for a grid cell that's empty
    private Drawable mEmptyDrawable;
    
    // The GridView for displaying thumbnails
    private GridView mGridView;
    
    // Denotes if the GridView has been loaded
    private boolean mIsLoaded;
    
    // Intent for starting the IntentService that downloads the Picasa featured picture RSS feed
    private Intent mServiceIntent;
    
    // An adapter between a Cursor and the Fragment's GridView
    private GridViewAdapter mAdapter;

    // The URL of the Picasa featured picture RSS feed, in String format
    private static final String PICASA_RSS_URL =
            "http://picasaweb.google.com/data/feed/base/featured?" +
            "alt=rss&kind=photo&access=public&slabel=featured&hl=en_US&imgmax=1600";

    private static final String[] PROJECTION =
    {
        DataProviderContract._ID,
        DataProviderContract.IMAGE_THUMBURL_COLUMN,
        DataProviderContract.IMAGE_URL_COLUMN
    };
    
    // Constants that define the order of columns in the returned cursor
    private static final int IMAGE_THUMBURL_CURSOR_INDEX = 1;
    private static final int IMAGE_URL_CURSOR_INDEX = 2;

    // Identifies a particular Loader being used in this component
    private static final int URL_LOADER = 0;
    
    /*
     * This callback is invoked when the framework is starting or re-starting the Loader. It
     * returns a CursorLoader object containing the desired query
     */
    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle)
    {
        /*
         * Takes action based on the ID of the Loader that's being created
         */
        switch (loaderID) {
            case URL_LOADER:
            // Returns a new CursorLoader
            return new CursorLoader(
                        getActivity(),                                     // Context
                        DataProviderContract.PICTUREURL_TABLE_CONTENTURI,  // Table to query
                        PROJECTION,                                        // Projection to return
                        null,                                              // No selection clause
                        null,                                              // No selection arguments
                        null                                               // Default sort order
            );
            default:
                // An invalid id was passed in
                return null;

        }
        
    }

    /*
     * This callback is invoked when the the Fragment's View is being loaded. It sets up the View.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle bundle) {
        
        // Always call the super method first
        super.onCreateView(inflater, viewGroup, bundle);
        
        /*
         * Inflates the View from the gridlist layout file, using the layout parameters in
         * "viewGroup"
         */
        View localView = inflater.inflate(R.layout.gridlist, viewGroup, false);
        
        // Sets the View's data adapter to be a new GridViewAdapter
        mAdapter = new GridViewAdapter(getActivity());
        
        // Gets a handle to the GridView in the layout
        mGridView = ((GridView) localView.findViewById(android.R.id.list));
        
        // Instantiates a DisplayMetrics object
        DisplayMetrics localDisplayMetrics = new DisplayMetrics();
        
        // Gets the current display metrics from the current Window
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(localDisplayMetrics);
        
        /*
         * Gets the dp value from the thumbSize resource as an integer in dps. The value can
         * be adjusted for specific display sizes, etc. in the dimens.xml file for a particular
         * values-<qualifier> directory
         */
        int pixelSize = getResources().getDimensionPixelSize(R.dimen.thumbSize);
        
        /*
         * Calculates a width scale factor from the pixel width of the current display and the
         * desired pixel size
         */
        int widthScale = localDisplayMetrics.widthPixels / pixelSize;
        
        // Calculates the grid column width
        mColumnWidth = (localDisplayMetrics.widthPixels / widthScale);
        
        // Sets the GridView's column width
        mGridView.setColumnWidth(mColumnWidth);
        
        // Starts by setting the GridView to have no columns
        mGridView.setNumColumns(-1);
        
        // Sets the GridView's data adapter
        mGridView.setAdapter(mAdapter);
        
        /*
         * Sets the GridView's click listener to be this class. As a result, when users click the
         * GridView, PhotoThumbnailFragment.onClick() is invoked.
         */
        mGridView.setOnItemClickListener(this);
        
        /*
         * Sets the "empty" View for the layout. If there's nothing to show, a ProgressBar
         * is displayed.
         */
        mGridView.setEmptyView(localView.findViewById(R.id.progressRoot));
        
        // Sets a dark background to show when no image is queued to be downloaded
        mEmptyDrawable = getResources().getDrawable(R.drawable.imagenotqueued);
        
        // Initializes the CursorLoader
        getLoaderManager().initLoader(URL_LOADER, null, this);
        
        /*
         * Creates a new Intent to send to the download IntentService. The Intent contains the
         * URL of the Picasa feature picture RSS feed
         */
        mServiceIntent =
                new Intent(getActivity(), RSSPullService.class)
                        .setData(Uri.parse(PICASA_RSS_URL));
        
        // If there's no pre-existing state for this Fragment
        if (bundle == null) {
            // If the data wasn't previously loaded
            if (!this.mIsLoaded) {
                // Starts the IntentService to download the RSS feed data
                getActivity().startService(mServiceIntent);
            }

        // If this Fragment existed previously, gets its state
        } else if (bundle.getBoolean(STATE_IS_HIDDEN, false)) {
            
            // Begins a transaction
            FragmentTransaction localFragmentTransaction =
                    getFragmentManager().beginTransaction();
            
            // Hides the Fragment
            localFragmentTransaction.hide(this);
            
            // Commits the transaction
            localFragmentTransaction.commit();
        }
        
        // Returns the View inflated from the layout
        return localView;
    }

    /*
     * This callback is invoked when the Fragment is being destroyed.
     */
    @Override
    public void onDestroyView() {
        
        // Sets variables to null, to avoid memory leaks
        mGridView = null;
        
        // If the EmptyDrawable contains something, sets those members to null
        if (mEmptyDrawable != null) {
            this.mEmptyDrawable.setCallback(null);
            this.mEmptyDrawable = null;
        }
        
        // Always call the super method last
        super.onDestroyView();
    }

    /*
     * This callback is invoked after onDestroyView(). It clears out variables, shuts down the
     * CursorLoader, and so forth
     */
    @Override
    public void onDetach() {
        
        // Destroys variables and references, and catches Exceptions
        try {
            getLoaderManager().destroyLoader(0);
            if (mAdapter != null) {
                mAdapter.changeCursor(null);
                mAdapter = null;
            }
        } catch (Throwable localThrowable) {
        }
        
        // Always call the super method last
        super.onDetach();
        return;
    }

    /*
     * This is invoked whenever the visibility state of the Fragment changes
     */
    @Override
    public void onHiddenChanged(boolean viewState) {
        super.onHiddenChanged(viewState);
    }

    /*
     * Implements OnItemClickListener.onItemClick() for this View's listener.
     * This implementation detects the View that was clicked and retrieves its picture URL.
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int viewId, long rowId) {
        
        // Returns a one-row cursor for the data that backs the View that was clicked.
        Cursor cursor = (Cursor) mAdapter.getItem(viewId);

        // Retrieves the urlString from the cursor
        String urlString = cursor.getString(IMAGE_URL_CURSOR_INDEX);

        /*
         * Creates a new Intent to get the full picture for the thumbnail that the user clicked.
         * The full photo is loaded into a separate Fragment
         */
        Intent localIntent =
                new Intent(Constants.ACTION_VIEW_IMAGE)
                .setData(Uri.parse(urlString));
        
        // Broadcasts the Intent to receivers in this app. See DisplayActivity.FragmentDisplayer.
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(localIntent);
    }

    /*
     * Invoked when the CursorLoader finishes the query. A reference to the Loader and the
     * returned Cursor are passed in as arguments
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor returnCursor) {
        
        /*
         *  Changes the adapter's Cursor to be the results of the load. This forces the View to
         *  redraw.
         */
        
        mAdapter.changeCursor(returnCursor);
    }

    /*
     * Invoked when the CursorLoader is being reset. For example, this is called if the
     * data in the provider changes and the Cursor becomes stale.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        
        // Sets the Adapter's backing data to null. This prevents memory leaks.
        mAdapter.changeCursor(null);
    }

    /*
     * This callback is invoked when the system has to destroy the Fragment for some reason. It
     * allows the Fragment to save its state, so the state can be restored later on.
     */
    @Override
    public void onSaveInstanceState(Bundle bundle) {
        
        // Saves the show-hide status of the display
        bundle.putBoolean(STATE_IS_HIDDEN, isHidden());
        
        // Always call the super method last
        super.onSaveInstanceState(bundle);
    }

    // Sets the state of the loaded flag
    public void setLoaded(boolean loadState) {
        mIsLoaded = loadState;
    }

    /**
     * Defines a custom View adapter that extends CursorAdapter. The main reason to do this is to
     * display images based on the backing Cursor, rather than just displaying the URLs that the
     * Cursor contains.
     */
    private class GridViewAdapter extends CursorAdapter {
        
        /**
         * Simplified constructor that calls the super constructor with the input Context,
         * a null value for Cursor, and no flags
         * @param context A Context for this object
         */
        public GridViewAdapter(Context context) {
            super(context, null, false);
        }
        
        /**
         *
         * Binds a View and a Cursor
         *
         * @param view An existing View object
         * @param context A Context for the View and Cursor
         * @param cursor The Cursor to bind to the View, representing one row of the returned query.
         */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            
            // Gets a handle to the View
            PhotoView localImageDownloaderView = (PhotoView) view.getTag();
            
            // Converts the URL string to a URL and tries to retrieve the picture
            try {
                // Gets the URL
                URL localURL =
                        new URL(
                            cursor.getString(IMAGE_THUMBURL_CURSOR_INDEX)
                        )
                ;
                /*
                 * Invokes setImageURL for the View. If the image isn't already available, this
                 * will download and decode it.
                 */
                localImageDownloaderView.setImageURL(
                            localURL, true, PhotoThumbnailFragment.this.mEmptyDrawable);
            
            // Catches an invalid URL
            } catch (MalformedURLException localMalformedURLException) {
                localMalformedURLException.printStackTrace();
            
            // Catches errors trying to download and decode the picture in a ThreadPool
            } catch (RejectedExecutionException localRejectedExecutionException) {
            }
        }

        /**
         * Creates a new View that shows the contents of the Cursor
         *
         *
         * @param context A Context for the View and Cursor
         * @param cursor The Cursor to display. This is a single row of the returned query
         * @param viewGroup The viewGroup that's the parent of the new View
         * @return the newly-created View
         */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            // Gets a new layout inflater instance
            LayoutInflater inflater = LayoutInflater.from(context);
            
            /*
             * Creates a new View by inflating the specified layout file. The root ViewGroup is
             * the root of the layout file. This View is a FrameLayout
             */
            View layoutView = inflater.inflate(R.layout.galleryitem, null);
            
            /*
             * Creates a second View to hold the thumbnail image.
             */
            View thumbView = layoutView.findViewById(R.id.thumbImage);
            
            /*
             * Sets layout parameters for the layout based on the layout parameters of a virtual
             * list. In addition, this sets the layoutView's width to be MATCH_PARENT, and its
             * height to be the column width?
             */
            layoutView.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT,
                    PhotoThumbnailFragment.this.mColumnWidth));
            
            // Sets the layoutView's tag to be the same as the thumbnail image tag.
            layoutView.setTag(thumbView);
            return layoutView;
        }

    }
}
