package com.example.android.wearable.geofencing;

import static com.example.android.wearable.geofencing.Constants.ANDROID_BUILDING_ID;
import static com.example.android.wearable.geofencing.Constants.ANDROID_BUILDING_LATITUDE;
import static com.example.android.wearable.geofencing.Constants.ANDROID_BUILDING_LONGITUDE;
import static com.example.android.wearable.geofencing.Constants.ANDROID_BUILDING_RADIUS_METERS;
import static com.example.android.wearable.geofencing.Constants.CONNECTION_FAILURE_RESOLUTION_REQUEST;
import static com.example.android.wearable.geofencing.Constants.GEOFENCE_EXPIRATION_TIME;
import static com.example.android.wearable.geofencing.Constants.TAG;
import static com.example.android.wearable.geofencing.Constants.YERBA_BUENA_ID;
import static com.example.android.wearable.geofencing.Constants.YERBA_BUENA_LATITUDE;
import static com.example.android.wearable.geofencing.Constants.YERBA_BUENA_LONGITUDE;
import static com.example.android.wearable.geofencing.Constants.YERBA_BUENA_RADIUS_METERS;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationClient.OnAddGeofencesResultListener;
import com.google.android.gms.location.LocationStatusCodes;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements ConnectionCallbacks,
        OnConnectionFailedListener, OnAddGeofencesResultListener {

    // Internal List of Geofence objects. In a real app, these might be provided by an API based on
    // locations within the user's proximity.
    List<Geofence> mGeofenceList;

    // These will store hard-coded geofences in this sample app.
    private SimpleGeofence mAndroidBuildingGeofence;
    private SimpleGeofence mYerbaBuenaGeofence;

    // Persistent storage for geofences.
    private SimpleGeofenceStore mGeofenceStorage;

    private LocationClient mLocationClient;
    // Stores the PendingIntent used to request geofence monitoring.
    private PendingIntent mGeofenceRequestIntent;

    // Defines the allowable request types (in this example, we only add geofences).
    private enum REQUEST_TYPE {ADD}
    private REQUEST_TYPE mRequestType;
    // Flag that indicates if a request is underway.
    private boolean mInProgress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Rather than displayng this activity, simply display a toast indicating that the geofence
        // service is being created. This should happen in less than a second.
        Toast.makeText(this, getString(R.string.start_geofence_service), Toast.LENGTH_SHORT).show();

        // Instantiate a new geofence storage area.
        mGeofenceStorage = new SimpleGeofenceStore(this);
        // Instantiate the current List of geofences.
        mGeofenceList = new ArrayList<Geofence>();
        // Start with the request flag set to false.
        mInProgress = false;

        createGeofences();
        addGeofences();

        finish();
    }

    /**
     * In this sample, the geofences are predetermined and are hard-coded here. A real app might
     * dynamically create geofences based on the user's location.
     */
    public void createGeofences() {
        // Create internal "flattened" objects containing the geofence data.
        mAndroidBuildingGeofence = new SimpleGeofence(
                ANDROID_BUILDING_ID,                // geofenceId.
                ANDROID_BUILDING_LATITUDE,
                ANDROID_BUILDING_LONGITUDE,
                ANDROID_BUILDING_RADIUS_METERS,
                GEOFENCE_EXPIRATION_TIME,
                Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT
        );
        mYerbaBuenaGeofence = new SimpleGeofence(
                YERBA_BUENA_ID,                // geofenceId.
                YERBA_BUENA_LATITUDE,
                YERBA_BUENA_LONGITUDE,
                YERBA_BUENA_RADIUS_METERS,
                GEOFENCE_EXPIRATION_TIME,
                Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT
        );

        // Store these flat versions in SharedPreferences and add them to the geofence list.
        mGeofenceStorage.setGeofence(ANDROID_BUILDING_ID, mAndroidBuildingGeofence);
        mGeofenceStorage.setGeofence(YERBA_BUENA_ID, mYerbaBuenaGeofence);
        mGeofenceList.add(mAndroidBuildingGeofence.toGeofence());
        mGeofenceList.add(mYerbaBuenaGeofence.toGeofence());
    }

    /**
     * Start a request for geofence monitoring by calling LocationClient.connect().
     */
    public void addGeofences() {
        // Start a request to add geofences.
        mRequestType = REQUEST_TYPE.ADD;
        // Test for Google Play services after setting the request type.
        if (!isGooglePlayServicesAvailable()) {
            Log.e(TAG, "Unable to add geofences - Google Play services unavailable.");
            return;
        }
        // Create a new location client object. Since this activity class implements
        // ConnectionCallbacks and OnConnectionFailedListener, it can be used as the listener for
        // both parameters.
        mLocationClient = new LocationClient(this, this, this);
        // If a request is not already underway.
        if (!mInProgress) {
            // Indicate that a request is underway.
            mInProgress = true;
            // Request a connection from the client to Location Services.
            mLocationClient.connect();
        // A request is already underway, so disconnect the client and retry the request.
        } else {
            mLocationClient.disconnect();
            mLocationClient.connect();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mInProgress = false;
        // If the error has a resolution, start a Google Play services activity to resolve it.
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Exception while resolving connection error.", e);
            }
        } else {
            int errorCode = connectionResult.getErrorCode();
            Log.e(TAG, "Connection to Google Play services failed with error code " + errorCode);
        }
    }

    /**
     * Called by Location Services if the location client disconnects.
     */
    @Override
    public void onDisconnected() {
        // Turn off the request flag.
        mInProgress = false;
        // Destroy the current location client.
        mLocationClient = null;
    }

    /**
     * Once the connection is available, send a request to add the Geofences.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Use mRequestType to determine what action to take. Only ADD is used in this sample.
        if (REQUEST_TYPE.ADD == mRequestType) {
            // Get the PendingIntent for the geofence monitoring request.
            mGeofenceRequestIntent = getGeofenceTransitionPendingIntent();
            // Send a request to add the current geofences.
            mLocationClient.addGeofences(mGeofenceList, mGeofenceRequestIntent, this);
        }
    }

    /**
     * Called when request to add geofences is complete, with a result status code.
     */
    @Override
    public void onAddGeofencesResult(int statusCode, String[] geofenceRequestIds) {
        // Log if adding the geofences was successful.
        if (LocationStatusCodes.SUCCESS == statusCode) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Added geofences successfully.");
            }
        } else {
            Log.e(TAG, "Failed to add geofences. Status code: " + statusCode);
        }
        // Turn off the in progress flag and disconnect the client.
        mInProgress = false;
        mLocationClient.disconnect();
    }

    /**
     * Checks if Google Play services is available.
     * @return true if it is.
     */
    private boolean isGooglePlayServicesAvailable() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == resultCode) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Google Play services is available.");
            }
            return true;
        } else {
            Log.e(TAG, "Google Play services is unavailable.");
            return false;
        }
    }

    /**
     * Create a PendingIntent that triggers GeofenceTransitionIntentService when a geofence
     * transition occurs.
     */
    private PendingIntent getGeofenceTransitionPendingIntent() {
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
