/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.basicmediarouter;

import android.app.Activity;
import android.app.MediaRouteActionProvider;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaRouter;
import android.media.MediaRouter.RouteInfo;
import android.os.Bundle;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

/**
 * <p>
 * This sample demonstrates the use of the MediaRouter API to show content on a
 * secondary display using a {@link android.app.Presentation}.
 * </p>
 * <p>
 * The activity uses the {@link android.media.MediaRouter} API to automatically detect when a
 * presentation display is available and to allow the user to control the media
 * routes using a menu item provided by the {@link android.app.MediaRouteActionProvider}.
 * When a presentation display is available a {@link android.app.Presentation} (implemented
 * as a {@link SamplePresentation}) is shown on the preferred display. A button
 * toggles the background color of the secondary screen to show the interaction
 * between the primary and secondary screens.
 * </p>
 * <p>
 * This sample requires an HDMI or Wifi display. Alternatively, the
 * "Simulate secondary displays" feature in Development Settings can be enabled
 * to simulate secondary displays.
 * </p>
 *
 * @see android.app.Presentation
 * @see android.media.MediaRouter
 */
public class MainActivity extends Activity {

    private MediaRouter mMediaRouter;

    // Active Presentation, set to null if no secondary screen is enabled
    private SamplePresentation mPresentation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sample_main);
        mTextStatus = (TextView) findViewById(R.id.textStatus);

        // get the list of background colors
        mColors = getResources().getIntArray(R.array.androidcolors);

        // Enable clicks on the 'change color' button
        mButton = (Button) findViewById(R.id.button1);
        mButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showNextColor();
            }
        });

        // BEGIN_INCLUDE(getMediaRouter)
        // Get the MediaRouter service
        mMediaRouter = (MediaRouter) getSystemService(Context.MEDIA_ROUTER_SERVICE);
        // END_INCLUDE(getMediaRouter)
    }

    /**
     * Implementing a {@link android.media.MediaRouter.Callback} to update the displayed
     * {@link android.app.Presentation} when a route is selected, unselected or the
     * presentation display has changed. The provided stub implementation
     * {@link android.media.MediaRouter.SimpleCallback} is extended and only
     * {@link android.media.MediaRouter.SimpleCallback#onRouteSelected(android.media.MediaRouter, int, android.media.MediaRouter.RouteInfo)}
     * ,
     * {@link android.media.MediaRouter.SimpleCallback#onRouteUnselected(android.media.MediaRouter, int, android.media.MediaRouter.RouteInfo)}
     * and
     * {@link android.media.MediaRouter.SimpleCallback#onRoutePresentationDisplayChanged(android.media.MediaRouter, android.media.MediaRouter.RouteInfo)}
     * are overridden to update the displayed {@link android.app.Presentation} in
     * {@link #updatePresentation()}. These callbacks enable or disable the
     * second screen presentation based on the routing provided by the
     * {@link android.media.MediaRouter} for {@link android.media.MediaRouter#ROUTE_TYPE_LIVE_VIDEO}
     * streams. @
     */
    private final MediaRouter.SimpleCallback mMediaRouterCallback =
            new MediaRouter.SimpleCallback() {

                // BEGIN_INCLUDE(SimpleCallback)
                /**
                 * A new route has been selected as active. Disable the current
                 * route and enable the new one.
                 */
                @Override
                public void onRouteSelected(MediaRouter router, int type, RouteInfo info) {
                    updatePresentation();
                }

                /**
                 * The route has been unselected.
                 */
                @Override
                public void onRouteUnselected(MediaRouter router, int type, RouteInfo info) {
                    updatePresentation();

                }

                /**
                 * The route's presentation display has changed. This callback
                 * is called when the presentation has been activated, removed
                 * or its properties have changed.
                 */
                @Override
                public void onRoutePresentationDisplayChanged(MediaRouter router, RouteInfo info) {
                    updatePresentation();
                }
                // END_INCLUDE(SimpleCallback)
            };

    /**
     * Updates the displayed presentation to enable a secondary screen if it has
     * been selected in the {@link android.media.MediaRouter} for the
     * {@link android.media.MediaRouter#ROUTE_TYPE_LIVE_VIDEO} type. If no screen has been
     * selected by the {@link android.media.MediaRouter}, the current screen is disabled.
     * Otherwise a new {@link SamplePresentation} is initialized and shown on
     * the secondary screen.
     */
    private void updatePresentation() {

        // BEGIN_INCLUDE(updatePresentationInit)
        // Get the selected route for live video
        RouteInfo selectedRoute = mMediaRouter.getSelectedRoute(
                MediaRouter.ROUTE_TYPE_LIVE_VIDEO);

        // Get its Display if a valid route has been selected
        Display selectedDisplay = null;
        if (selectedRoute != null) {
            selectedDisplay = selectedRoute.getPresentationDisplay();
        }
        // END_INCLUDE(updatePresentationInit)

        // BEGIN_INCLUDE(updatePresentationDismiss)
        /*
         * Dismiss the current presentation if the display has changed or no new
         * route has been selected
         */
        if (mPresentation != null && mPresentation.getDisplay() != selectedDisplay) {
            mPresentation.dismiss();
            mPresentation = null;
            mButton.setEnabled(false);
            mTextStatus.setText(R.string.secondary_notconnected);
        }
        // END_INCLUDE(updatePresentationDismiss)

        // BEGIN_INCLUDE(updatePresentationNew)
        /*
         * Show a new presentation if the previous one has been dismissed and a
         * route has been selected.
         */
        if (mPresentation == null && selectedDisplay != null) {

            // Initialise a new Presentation for the Display
            mPresentation = new SamplePresentation(this, selectedDisplay);
            mPresentation.setOnDismissListener(mOnDismissListener);

            // Try to show the presentation, this might fail if the display has
            // gone away in the mean time
            try {
                mPresentation.show();
                mTextStatus.setText(getResources().getString(R.string.secondary_connected,
                        selectedRoute.getName(MainActivity.this)));
                mButton.setEnabled(true);
                showNextColor();
            } catch (WindowManager.InvalidDisplayException ex) {
                // Couldn't show presentation - display was already removed
                mPresentation = null;
            }
        }
        // END_INCLUDE(updatePresentationNew)

    }

    @Override
    protected void onResume() {
        super.onResume();

        // BEGIN_INCLUDE(addCallback)
        // Register a callback for all events related to live video devices
        mMediaRouter.addCallback(MediaRouter.ROUTE_TYPE_LIVE_VIDEO, mMediaRouterCallback);
        // END_INCLUDE(addCallback)

        // Show the 'Not connected' status message
        mButton.setEnabled(false);
        mTextStatus.setText(R.string.secondary_notconnected);

        // Update the displays based on the currently active routes
        updatePresentation();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // BEGIN_INCLUDE(onPause)
        // Stop listening for changes to media routes.
        mMediaRouter.removeCallback(mMediaRouterCallback);
        // END_INCLUDE(onPause)
    }

    @Override
    protected void onStop() {
        super.onStop();

        // BEGIN_INCLUDE(onStop)
        // Dismiss the presentation when the activity is not visible.
        if (mPresentation != null) {
            mPresentation.dismiss();
            mPresentation = null;
        }
        // BEGIN_INCLUDE(onStop)
    }

    /**
     * Inflates the ActionBar or options menu. The menu file defines an item for
     * the {@link android.app.MediaRouteActionProvider}, which is registered here for all
     * live video devices using {@link android.media.MediaRouter#ROUTE_TYPE_LIVE_VIDEO}.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.main, menu);

        // BEGIN_INCLUDE(MediaRouteActionProvider)
        // Configure the media router action provider
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.menu_media_route);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) mediaRouteMenuItem.getActionProvider();
        mediaRouteActionProvider.setRouteTypes(MediaRouter.ROUTE_TYPE_LIVE_VIDEO);
        // BEGIN_INCLUDE(MediaRouteActionProvider)

        return true;
    }

    /**
     * Listens for dismissal of the {@link SamplePresentation} and removes its
     * reference.
     */
    private final DialogInterface.OnDismissListener mOnDismissListener =
            new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (dialog == mPresentation) {
                        mPresentation = null;
                    }
                }
            };

    // Views used to display status information on the primary screen
    private TextView mTextStatus;
    private Button mButton;

    // selected color index
    private int mColor = 0;

    // background colors
    public int[] mColors;

    /**
     * Displays the next color on the secondary screen if it is activate.
     */
    private void showNextColor() {
        if (mPresentation != null) {
            // a second screen is active and initialized, show the next color
            mPresentation.setColor(mColors[mColor]);
            mColor = (mColor + 1) % mColors.length;
        }
    }

}
