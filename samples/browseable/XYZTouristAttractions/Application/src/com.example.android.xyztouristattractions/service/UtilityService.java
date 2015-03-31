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

package com.example.android.xyztouristattractions.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.location.Location;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.android.xyztouristattractions.R;
import com.example.android.xyztouristattractions.common.Attraction;
import com.example.android.xyztouristattractions.common.Constants;
import com.example.android.xyztouristattractions.common.Utils;
import com.example.android.xyztouristattractions.provider.TouristAttractions;
import com.example.android.xyztouristattractions.ui.DetailActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.example.android.xyztouristattractions.provider.TouristAttractions.ATTRACTIONS;
import static com.google.android.gms.location.LocationServices.FusedLocationApi;
import static com.google.android.gms.location.LocationServices.GeofencingApi;

/**
 * A utility IntentService, used for a variety of asynchronous background
 * operations that do not necessarily need to be tied to a UI.
 */
public class UtilityService extends IntentService {
    private static final String TAG = UtilityService.class.getSimpleName();

    public static final String ACTION_GEOFENCE_TRIGGERED = "geofence_triggered";
    private static final String ACTION_LOCATION_UPDATED = "location_updated";
    private static final String ACTION_REQUEST_LOCATION = "request_location";
    private static final String ACTION_ADD_GEOFENCES = "add_geofences";
    private static final String ACTION_CLEAR_NOTIFICATION = "clear_notification";
    private static final String ACTION_CLEAR_REMOTE_NOTIFICATIONS = "clear_remote_notifications";
    private static final String ACTION_FAKE_UPDATE = "fake_update";
    private static final String EXTRA_TEST_MICROAPP = "test_microapp";

    public static IntentFilter getLocationUpdatedIntentFilter() {
        return new IntentFilter(UtilityService.ACTION_LOCATION_UPDATED);
    }

    public static void triggerWearTest(Context context, boolean microApp) {
        Intent intent = new Intent(context, UtilityService.class);
        intent.setAction(UtilityService.ACTION_FAKE_UPDATE);
        intent.putExtra(EXTRA_TEST_MICROAPP, microApp);
        context.startService(intent);
    }

    public static void addGeofences(Context context) {
        Intent intent = new Intent(context, UtilityService.class);
        intent.setAction(UtilityService.ACTION_ADD_GEOFENCES);
        context.startService(intent);
    }

    public static void requestLocation(Context context) {
        Intent intent = new Intent(context, UtilityService.class);
        intent.setAction(UtilityService.ACTION_REQUEST_LOCATION);
        context.startService(intent);
    }

    public static void clearNotification(Context context) {
        Intent intent = new Intent(context, UtilityService.class);
        intent.setAction(UtilityService.ACTION_CLEAR_NOTIFICATION);
        context.startService(intent);
    }

    public static Intent getClearRemoteNotificationsIntent(Context context) {
        Intent intent = new Intent(context, UtilityService.class);
        intent.setAction(UtilityService.ACTION_CLEAR_REMOTE_NOTIFICATIONS);
        return intent;
    }

    public UtilityService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_ADD_GEOFENCES.equals(action)) {
            addGeofencesInternal();
        } else if (ACTION_GEOFENCE_TRIGGERED.equals(action)) {
            geofenceTriggered(intent);
        } else if (ACTION_REQUEST_LOCATION.equals(action)) {
            requestLocationInternal();
        } else if (ACTION_LOCATION_UPDATED.equals(action)) {
            locationUpdated(intent);
        } else if (ACTION_CLEAR_NOTIFICATION.equals(action)) {
            clearNotificationInternal();
        } else if (ACTION_CLEAR_REMOTE_NOTIFICATIONS.equals(action)) {
            clearRemoteNotifications();
        } else if (ACTION_FAKE_UPDATE.equals(action)) {
            LatLng currentLocation = Utils.getLocation(this);

            // If location unknown use test city, otherwise use closest city
            String city = currentLocation == null ? TouristAttractions.TEST_CITY :
                    TouristAttractions.getClosestCity(currentLocation);

            showNotification(city,
                    intent.getBooleanExtra(EXTRA_TEST_MICROAPP, Constants.USE_MICRO_APP));
        }
    }

    /**
     * Add geofences using Play Services
     */
    private void addGeofencesInternal() {
        Log.v(TAG, ACTION_ADD_GEOFENCES);
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .build();

        // It's OK to use blockingConnect() here as we are running in an
        // IntentService that executes work on a separate (background) thread.
        ConnectionResult connectionResult = googleApiClient.blockingConnect(
                Constants.GOOGLE_API_CLIENT_TIMEOUT_S, TimeUnit.SECONDS);

        if (connectionResult.isSuccess() && googleApiClient.isConnected()) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this, 0, new Intent(this, UtilityReceiver.class), 0);
            GeofencingApi.addGeofences(googleApiClient,
                    TouristAttractions.getGeofenceList(), pendingIntent);
            googleApiClient.disconnect();
        } else {
            Log.e(TAG, String.format(Constants.GOOGLE_API_CLIENT_ERROR_MSG,
                    connectionResult.getErrorCode()));
        }
    }

    /**
     * Called when a geofence is triggered
     */
    private void geofenceTriggered(Intent intent) {
        Log.v(TAG, ACTION_GEOFENCE_TRIGGERED);

        // Check if geofences are enabled
        boolean geofenceEnabled = Utils.getGeofenceEnabled(this);

        // Extract the geofences from the intent
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        List<Geofence> geofences = event.getTriggeringGeofences();

        if (geofenceEnabled && geofences != null && geofences.size() > 0) {
            if (event.getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_ENTER) {
                // Trigger the notification based on the first geofence
                showNotification(geofences.get(0).getRequestId(), Constants.USE_MICRO_APP);
            } else if (event.getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_EXIT) {
                // Clear notifications
                clearNotificationInternal();
                clearRemoteNotifications();
            }
        }
        UtilityReceiver.completeWakefulIntent(intent);
    }

    /**
     * Called when a location update is requested
     */
    private void requestLocationInternal() {
        Log.v(TAG, ACTION_REQUEST_LOCATION);
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .build();

        // It's OK to use blockingConnect() here as we are running in an
        // IntentService that executes work on a separate (background) thread.
        ConnectionResult connectionResult = googleApiClient.blockingConnect(
                Constants.GOOGLE_API_CLIENT_TIMEOUT_S, TimeUnit.SECONDS);

        if (connectionResult.isSuccess() && googleApiClient.isConnected()) {

            Intent locationUpdatedIntent = new Intent(this, UtilityService.class);
            locationUpdatedIntent.setAction(ACTION_LOCATION_UPDATED);

            // Send last known location out first if available
            Location location = FusedLocationApi.getLastLocation(googleApiClient);
            if (location != null) {
                Intent lastLocationIntent = new Intent(locationUpdatedIntent);
                lastLocationIntent.putExtra(
                        FusedLocationProviderApi.KEY_LOCATION_CHANGED, location);
                startService(lastLocationIntent);
            }

            // Request new location
            LocationRequest mLocationRequest = new LocationRequest()
                    .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            FusedLocationApi.requestLocationUpdates(
                    googleApiClient, mLocationRequest,
                    PendingIntent.getService(this, 0, locationUpdatedIntent, 0));

            googleApiClient.disconnect();
        } else {
            Log.e(TAG, String.format(Constants.GOOGLE_API_CLIENT_ERROR_MSG,
                    connectionResult.getErrorCode()));
        }
    }

    /**
     * Called when the location has been updated
     */
    private void locationUpdated(Intent intent) {
        Log.v(TAG, ACTION_LOCATION_UPDATED);

        // Extra new location
        Location location =
                intent.getParcelableExtra(FusedLocationProviderApi.KEY_LOCATION_CHANGED);

        if (location != null) {
            LatLng latLngLocation = new LatLng(location.getLatitude(), location.getLongitude());

            // Store in a local preference as well
            Utils.storeLocation(this, latLngLocation);

            // Send a local broadcast so if an Activity is open it can respond
            // to the updated location
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    /**
     * Clears the local device notification
     */
    private void clearNotificationInternal() {
        Log.v(TAG, ACTION_CLEAR_NOTIFICATION);
        NotificationManagerCompat.from(this).cancel(Constants.MOBILE_NOTIFICATION_ID);
    }

    /**
     * Clears remote device notifications using the Wearable message API
     */
    private void clearRemoteNotifications() {
        Log.v(TAG, ACTION_CLEAR_REMOTE_NOTIFICATIONS);
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        // It's OK to use blockingConnect() here as we are running in an
        // IntentService that executes work on a separate (background) thread.
        ConnectionResult connectionResult = googleApiClient.blockingConnect(
                Constants.GOOGLE_API_CLIENT_TIMEOUT_S, TimeUnit.SECONDS);

        if (connectionResult.isSuccess() && googleApiClient.isConnected()) {

            // Loop through all nodes and send a clear notification message
            Iterator<String> itr = Utils.getNodes(googleApiClient).iterator();
            while (itr.hasNext()) {
                Wearable.MessageApi.sendMessage(
                        googleApiClient, itr.next(), Constants.CLEAR_NOTIFICATIONS_PATH, null);
            }
            googleApiClient.disconnect();
        }
    }


    /**
     * Show the notification. Either the regular notification with wearable features
     * added to enhance, or trigger the full micro app on the wearable.
     *
     * @param cityId The city to trigger the notification for
     * @param microApp If the micro app should be triggered or just enhanced notifications
     */
    private void showNotification(String cityId, boolean microApp) {

        List<Attraction> attractions = ATTRACTIONS.get(cityId);

        if (microApp) {
            // If micro app we first need to transfer some data over
            sendDataToWearable(attractions);
        }

        // The first (closest) tourist attraction
        Attraction attraction = attractions.get(0);

        // Limit attractions to send
        int count = attractions.size() > Constants.MAX_ATTRACTIONS ?
                Constants.MAX_ATTRACTIONS : attractions.size();

        // Pull down the tourist attraction images from the network and store
        HashMap<String, Bitmap> bitmaps = new HashMap<>();
        try {
            for (int i = 0; i < count; i++) {
                bitmaps.put(attractions.get(i).name,
                        Glide.with(this)
                                .load(attractions.get(i).imageUrl)
                                .asBitmap()
                                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                .into(Constants.WEAR_IMAGE_SIZE, Constants.WEAR_IMAGE_SIZE)
                                .get());
            }
        } catch (InterruptedException | ExecutionException e) {
            Log.e(TAG, "Error fetching image from network: " + e);
        }

        // The intent to trigger when the notification is tapped
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                DetailActivity.getLaunchIntent(this, attraction.name),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // The intent to trigger when the notification is dismissed, in this case
        // we want to clear remote notifications as well
        PendingIntent deletePendingIntent =
                PendingIntent.getService(this, 0, getClearRemoteNotificationsIntent(this), 0);

        // Construct the main notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setStyle(new NotificationCompat.BigPictureStyle()
                                .bigPicture(bitmaps.get(attraction.name))
                                .setBigContentTitle(attraction.name)
                                .setSummaryText(getString(R.string.nearby_attraction))
                )
                .setLocalOnly(microApp)
                .setContentTitle(attraction.name)
                .setContentText(getString(R.string.nearby_attraction))
                .setSmallIcon(R.drawable.ic_stat_maps_pin_drop)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setCategory(Notification.CATEGORY_RECOMMENDATION)
                .setAutoCancel(true);

        if (!microApp) {
            // If not a micro app, create some wearable pages for
            // the other nearby tourist attractions.
            ArrayList<Notification> pages = new ArrayList<Notification>();
            for (int i = 1; i < count; i++) {

                // Calculate the distance from current location to tourist attraction
                String distance = Utils.formatDistanceBetween(
                        Utils.getLocation(this), attractions.get(i).location);

                // Construct the notification and add it as a page
                pages.add(new NotificationCompat.Builder(this)
                        .setContentTitle(attractions.get(i).name)
                        .setContentText(distance)
                        .setSmallIcon(R.drawable.ic_stat_maps_pin_drop)
                        .extend(new NotificationCompat.WearableExtender()
                                .setBackground(bitmaps.get(attractions.get(i).name))
                        )
                        .build());
            }
            builder.extend(new NotificationCompat.WearableExtender().addPages(pages));
        }

        // Trigger the notification
        NotificationManagerCompat.from(this).notify(
                Constants.MOBILE_NOTIFICATION_ID, builder.build());
    }

    /**
     * Transfer the required data over to the wearable
     * @param attractions list of attraction data to transfer over
     */
    private void sendDataToWearable(List<Attraction> attractions) {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        // It's OK to use blockingConnect() here as we are running in an
        // IntentService that executes work on a separate (background) thread.
        ConnectionResult connectionResult = googleApiClient.blockingConnect(
                Constants.GOOGLE_API_CLIENT_TIMEOUT_S, TimeUnit.SECONDS);

        // Limit attractions to send
        int count = attractions.size() > Constants.MAX_ATTRACTIONS ?
                Constants.MAX_ATTRACTIONS : attractions.size();

        ArrayList<DataMap> attractionsData = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            Attraction attraction = attractions.get(i);

            Bitmap image = null;
            Bitmap secondaryImage = null;

            try {
                // Fetch and resize attraction image bitmap
                image = Glide.with(this)
                        .load(attraction.imageUrl)
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .into(Constants.WEAR_IMAGE_SIZE_PARALLAX_WIDTH, Constants.WEAR_IMAGE_SIZE)
                        .get();

                secondaryImage = Glide.with(this)
                        .load(attraction.secondaryImageUrl)
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .into(Constants.WEAR_IMAGE_SIZE_PARALLAX_WIDTH, Constants.WEAR_IMAGE_SIZE)
                        .get();
            } catch (InterruptedException | ExecutionException e) {
                Log.e(TAG, "Exception loading bitmap from network");
            }

            if (image != null && secondaryImage != null) {

                DataMap attractionData = new DataMap();

                String distance = Utils.formatDistanceBetween(
                        Utils.getLocation(this), attraction.location);

                attractionData.putString(Constants.EXTRA_TITLE, attraction.name);
                attractionData.putString(Constants.EXTRA_DESCRIPTION, attraction.description);
                attractionData.putDouble(
                        Constants.EXTRA_LOCATION_LAT, attraction.location.latitude);
                attractionData.putDouble(
                        Constants.EXTRA_LOCATION_LNG, attraction.location.longitude);
                attractionData.putString(Constants.EXTRA_DISTANCE, distance);
                attractionData.putString(Constants.EXTRA_CITY, attraction.city);
                attractionData.putAsset(Constants.EXTRA_IMAGE,
                        Utils.createAssetFromBitmap(image));
                attractionData.putAsset(Constants.EXTRA_IMAGE_SECONDARY,
                        Utils.createAssetFromBitmap(secondaryImage));

                attractionsData.add(attractionData);
            }
        }

        if (connectionResult.isSuccess() && googleApiClient.isConnected()
                && attractionsData.size() > 0) {

            PutDataMapRequest dataMap = PutDataMapRequest.create(Constants.ATTRACTION_PATH);
            dataMap.getDataMap().putDataMapArrayList(Constants.EXTRA_ATTRACTIONS, attractionsData);
            dataMap.getDataMap().putLong(Constants.EXTRA_TIMESTAMP, new Date().getTime());
            PutDataRequest request = dataMap.asPutDataRequest();

            // Send the data over
            DataApi.DataItemResult result =
                    Wearable.DataApi.putDataItem(googleApiClient, request).await();

            if (!result.getStatus().isSuccess()) {
                Log.e(TAG, String.format("Error sending data using DataApi (error code = %d)",
                        result.getStatus().getStatusCode()));
            }

        } else {
            Log.e(TAG, String.format(Constants.GOOGLE_API_CLIENT_ERROR_MSG,
                    connectionResult.getErrorCode()));
        }
        googleApiClient.disconnect();
    }
}
