package com.example.android.wearable.watchface;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

/**
 * Allows users of the Fit WatchFace to tie their Google Fit account to the WatchFace.
 */
public class FitDistanceWatchFaceConfigActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "FitDistanceConfig";

    // Request code for launching the Intent to resolve authorization.
    private static final int REQUEST_OAUTH = 1;

    // Shared Preference used to record if the user has enabled Google Fit previously.
    private static final String PREFS_FIT_ENABLED_BY_USER =
            "com.example.android.wearable.watchface.preferences.FIT_ENABLED_BY_USER";

    /* Tracks whether an authorization activity is stacking over the current activity, i.e., when
     *  a known auth error is being resolved, such as showing the account chooser or presenting a
     *  consent dialog. This avoids common duplications as might happen on screen rotations, etc.
     */
    private static final String EXTRA_AUTH_STATE_PENDING =
            "com.example.android.wearable.watchface.extra.AUTH_STATE_PENDING";

    private static final long FIT_DISABLE_TIMEOUT_SECS = TimeUnit.SECONDS.toMillis(5);;

    private boolean mResolvingAuthorization;

    private boolean mFitEnabled;

    private GoogleApiClient mGoogleApiClient;

    private Switch mFitAuthSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fit_watch_face_config);

        mFitAuthSwitch = (Switch) findViewById(R.id.fit_auth_switch);

        if (savedInstanceState != null) {
            mResolvingAuthorization =
                    savedInstanceState.getBoolean(EXTRA_AUTH_STATE_PENDING, false);
        } else {
            mResolvingAuthorization = false;
        }

        // Checks if user previously enabled/approved Google Fit.
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        mFitEnabled =
                sharedPreferences.getBoolean(PREFS_FIT_ENABLED_BY_USER, false);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.HISTORY_API)
                .addApi(Fitness.RECORDING_API)
                .addApi(Fitness.CONFIG_API)
                .addScope(new Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if ((mFitEnabled) && (mGoogleApiClient != null)) {

            mFitAuthSwitch.setChecked(true);
            mFitAuthSwitch.setEnabled(true);

            mGoogleApiClient.connect();

        } else {

            mFitAuthSwitch.setChecked(false);
            mFitAuthSwitch.setEnabled(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if ((mGoogleApiClient != null) && (mGoogleApiClient.isConnected())) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean(EXTRA_AUTH_STATE_PENDING, mResolvingAuthorization);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState != null) {
            mResolvingAuthorization =
                    savedInstanceState.getBoolean(EXTRA_AUTH_STATE_PENDING, false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult()");

        if (requestCode == REQUEST_OAUTH) {
            mResolvingAuthorization = false;

            if (resultCode == RESULT_OK) {
                setUserFitPreferences(true);

                if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            } else {
                // User cancelled authorization, reset the switch.
                setUserFitPreferences(false);
            }
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected: " + connectionHint);
    }

    @Override
    public void onConnectionSuspended(int cause) {

        if (cause == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
            Log.i(TAG, "Connection lost.  Cause: Network Lost.");
        } else if (cause == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
            Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
        } else {
            Log.i(TAG, "onConnectionSuspended: " + cause);
        }

        mFitAuthSwitch.setChecked(false);
        mFitAuthSwitch.setEnabled(true);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "Connection to Google Fit failed. Cause: " + result.toString());

        if (!result.hasResolution()) {
            // User cancelled authorization, reset the switch.
            mFitAuthSwitch.setChecked(false);
            mFitAuthSwitch.setEnabled(true);
            // Show the localized error dialog
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 0).show();
            return;
        }

        // Resolve failure if not already trying/authorizing.
        if (!mResolvingAuthorization) {
            try {
                Log.i(TAG, "Attempting to resolve failed GoogleApiClient connection");
                mResolvingAuthorization = true;
                result.startResolutionForResult(this, REQUEST_OAUTH);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Exception while starting resolution activity", e);
            }
        }
    }

    public void onSwitchClicked(View view) {

        boolean userWantsToEnableFit = mFitAuthSwitch.isChecked();

        if (userWantsToEnableFit) {

            Log.d(TAG, "User wants to enable Fit.");
            if ((mGoogleApiClient != null) && (!mGoogleApiClient.isConnected())) {
                mGoogleApiClient.connect();
            }

        } else {
            Log.d(TAG, "User wants to disable Fit.");

            // Disable switch until disconnect request is finished.
            mFitAuthSwitch.setEnabled(false);

            PendingResult<Status> pendingResult = Fitness.ConfigApi.disableFit(mGoogleApiClient);

            pendingResult.setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {

                    if (status.isSuccess()) {
                        Toast.makeText(
                                FitDistanceWatchFaceConfigActivity.this,
                                "Disconnected from Google Fit.",
                                Toast.LENGTH_LONG).show();

                        setUserFitPreferences(false);

                        mGoogleApiClient.disconnect();


                    } else {
                        Toast.makeText(
                                FitDistanceWatchFaceConfigActivity.this,
                                "Unable to disconnect from Google Fit. See logcat for details.",
                                Toast.LENGTH_LONG).show();

                        // Re-set the switch since auth failed.
                        setUserFitPreferences(true);
                    }
                }
            }, FIT_DISABLE_TIMEOUT_SECS, TimeUnit.SECONDS);
        }
    }

    private void setUserFitPreferences(boolean userFitPreferences) {

        mFitEnabled = userFitPreferences;
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREFS_FIT_ENABLED_BY_USER, userFitPreferences);
        editor.commit();

        mFitAuthSwitch.setChecked(userFitPreferences);
        mFitAuthSwitch.setEnabled(true);
    }
}
