/*
 * Copyright 2015 Google Inc. All rights reserved.
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

package com.example.android.xyztouristattractions.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.wearable.view.DismissOverlayView;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.example.android.xyztouristattractions.R;
import com.example.android.xyztouristattractions.common.Attraction;
import com.example.android.xyztouristattractions.common.Constants;
import com.example.android.xyztouristattractions.common.Utils;
import com.example.android.xyztouristattractions.service.UtilityService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The main Wear activity that displays nearby attractions in a
 * {@link android.support.wearable.view.GridViewPager}. Each row shows
 * one attraction and each column shows information or actions for that
 * particular attraction.
 */
public class AttractionsActivity extends Activity
        implements AttractionsGridPagerAdapter.OnChromeFadeListener {
    private static final String TAG = AttractionsActivity.class.getSimpleName();

    private GestureDetectorCompat mGestureDetector;
    private DismissOverlayView mDismissOverlayView;
    private GridViewPager mGridViewPager;
    private AttractionsGridPagerAdapter mAdapter;
    private DotsPageIndicator mDotsPageIndicator;
    private ProgressBar mProgressBar;
    private Rect mInsets = new Rect(0, 0, 0, 0);

    private ArrayList<Attraction> mAttractions = new ArrayList<Attraction>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        final FrameLayout topFrameLayout = (FrameLayout) findViewById(R.id.topFrameLayout);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mGridViewPager = (GridViewPager) findViewById(R.id.gridViewPager);
        mDotsPageIndicator = (DotsPageIndicator) findViewById(R.id.dotsPageIndicator);
        mAdapter = new AttractionsGridPagerAdapter(this, mAttractions);
        mAdapter.setOnChromeFadeListener(this);
        mGridViewPager.setAdapter(mAdapter);

        topFrameLayout.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                // Call through to super implementation
                insets = topFrameLayout.onApplyWindowInsets(insets);

                boolean round = insets.isRound();

                // Store system window insets regardless of screen shape
                mInsets.set(insets.getSystemWindowInsetLeft(),
                        insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(),
                        insets.getSystemWindowInsetBottom());

                if (round) {
                    // On a round screen calculate the square inset to use.
                    // Alternatively could use BoxInsetLayout, although calculating
                    // the inset ourselves lets us position views outside the center
                    // box. For example, slightly lower on the round screen (by giving
                    // up some horizontal space).
                    mInsets = Utils.calculateBottomInsetsOnRoundDevice(
                            getWindowManager().getDefaultDisplay(), mInsets);

                    // Boost the dots indicator up by the bottom inset
                    FrameLayout.LayoutParams params =
                            (FrameLayout.LayoutParams) mDotsPageIndicator.getLayoutParams();
                    params.bottomMargin = mInsets.bottom;
                    mDotsPageIndicator.setLayoutParams(params);
                }

                mAdapter.setInsets(mInsets);
                return insets;
            }
        });

        // Set up the DismissOverlayView
        mDismissOverlayView = (DismissOverlayView) findViewById(R.id.dismiss_overlay);
        mDismissOverlayView.setIntroText(getString(R.string.exit_intro_text));
        mDismissOverlayView.showIntroIfNecessary();
        mGestureDetector = new GestureDetectorCompat(this, new LongPressListener());

        Uri attractionsUri = getIntent().getParcelableExtra(Constants.EXTRA_ATTRACTIONS_URI);
        if (attractionsUri != null) {
            new FetchDataAsyncTask(this).execute(attractionsUri);
            UtilityService.clearNotification(this);
            UtilityService.clearRemoteNotifications(this);
        } else {
            finish();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event) || super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    public void onChromeFadeIn() {
        // As the custom UI chrome fades in, also fade the DotsPageIndicator in
        mDotsPageIndicator.animate().alpha(1).setDuration(
                AttractionsGridPagerAdapter.FADE_IN_TIME_MS).start();
    }

    @Override
    public void onChromeFadeOut() {
        // As the custom UI chrome fades out, also fade the DotsPageIndicator out
        mDotsPageIndicator.animate().alpha(0).setDuration(
                AttractionsGridPagerAdapter.FADE_OUT_TIME_MS).start();
    }

    private class LongPressListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public void onLongPress(MotionEvent event) {
            mDismissOverlayView.show();
        }
    }

    /**
     * A background task to load the attraction data via the Wear DataApi.
     * This can take a second or two sometimes as several images need to
     * be loaded.
     */
    private class FetchDataAsyncTask extends
            AsyncTask<Uri, Void, ArrayList<Attraction>> {

        private Context mContext;

        public FetchDataAsyncTask(Context context) {
            mContext = context;
        }

        @Override
        protected ArrayList<Attraction> doInBackground(Uri... params) {
            mAttractions.clear();

            // Connect to Play Services and the Wearable API
            GoogleApiClient googleApiClient = new GoogleApiClient.Builder(mContext)
                    .addApi(Wearable.API)
                    .build();

            ConnectionResult connectionResult = googleApiClient.blockingConnect(
                    Constants.GOOGLE_API_CLIENT_TIMEOUT_S, TimeUnit.SECONDS);

            if (!connectionResult.isSuccess() || !googleApiClient.isConnected()) {
                Log.e(TAG, String.format(Constants.GOOGLE_API_CLIENT_ERROR_MSG,
                        connectionResult.getErrorCode()));
                return null;
            }

            Uri attractionsUri = params[0];
            DataApi.DataItemResult dataItemResult =
                    Wearable.DataApi.getDataItem(googleApiClient, attractionsUri).await();

            if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItemResult.getDataItem());
                List<DataMap> attractionsData =
                        dataMapItem.getDataMap().getDataMapArrayList(Constants.EXTRA_ATTRACTIONS);

                // Loop through each attraction, adding them to the list
                Iterator<DataMap> itr = attractionsData.iterator();
                while (itr.hasNext()) {
                    DataMap attractionData = itr.next();

                    Attraction attraction = new Attraction();
                    attraction.name = attractionData.getString(Constants.EXTRA_TITLE);
                    attraction.description =
                            attractionData.getString(Constants.EXTRA_DESCRIPTION);
                    attraction.city = attractionData.get(Constants.EXTRA_CITY);
                    attraction.distance =
                            attractionData.getString(Constants.EXTRA_DISTANCE);
                    attraction.location = new LatLng(
                            attractionData.getDouble(Constants.EXTRA_LOCATION_LAT),
                            attractionData.getDouble(Constants.EXTRA_LOCATION_LNG));
                    attraction.image = Utils.loadBitmapFromAsset(googleApiClient,
                            attractionData.getAsset(Constants.EXTRA_IMAGE));
                    attraction.secondaryImage = Utils.loadBitmapFromAsset(googleApiClient,
                            attractionData.getAsset(Constants.EXTRA_IMAGE_SECONDARY));

                    mAttractions.add(attraction);
                }
            }

            googleApiClient.disconnect();

            return mAttractions;
        }

        @Override
        protected void onPostExecute(ArrayList<Attraction> result) {
            if (result != null && result.size() > 0) {
                // Update UI based on the result of the background processing
                mAdapter.setData(result);
                mAdapter.notifyDataSetChanged();
                mDotsPageIndicator.setPager(mGridViewPager);
                mDotsPageIndicator.setOnPageChangeListener(mAdapter);
                mProgressBar.setVisibility(View.GONE);
                mDotsPageIndicator.setVisibility(View.VISIBLE);
                mGridViewPager.setVisibility(View.VISIBLE);
            } else {
                finish();
            }
        }
    }
}
