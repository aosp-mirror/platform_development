package com.example.android.wearable.agendadata;

import static com.example.android.wearable.agendadata.Constants.TAG;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

public class MainActivity extends Activity implements NodeApi.NodeListener, ConnectionCallbacks,
        OnConnectionFailedListener {

    /** Request code for launching the Intent to resolve Google Play services errors. */
    private static final int REQUEST_RESOLVE_ERROR = 1000;

    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;

    private TextView mLogTextView;
    ScrollView mScroller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        mLogTextView = (TextView) findViewById(R.id.log);
        mScroller = (ScrollView) findViewById(R.id.scroller);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient.isConnected()) {
            Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        }
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    public void onGetEventsClicked(View v) {
        startService(new Intent(this, CalendarQueryService.class));
    }

    public void onDeleteEventsClicked(View v) {
        if (mGoogleApiClient.isConnected()) {
            Wearable.DataApi.getDataItems(mGoogleApiClient)
                    .setResultCallback(new ResultCallback<DataItemBuffer>() {
                        @Override
                        public void onResult(DataItemBuffer result) {
                            if (result.getStatus().isSuccess()) {
                                deleteDataItems(result);
                            } else {
                                if (Log.isLoggable(TAG, Log.DEBUG)) {
                                    Log.d(TAG, "onDeleteEventsClicked(): failed to get Data Items");
                                }
                            }
                            result.close();
                        }
                    });
        } else {
            Log.e(TAG, "Failed to delete data items"
                     + " - Client disconnected from Google Play Services");
        }
    }

    private void deleteDataItems(DataItemBuffer dataItems) {
        if (mGoogleApiClient.isConnected()) {
            // Store the DataItem URIs in a List and close the buffer. Then use these URIs
            // to delete the DataItems.
            final List<DataItem> dataItemList = FreezableUtils.freezeIterable(dataItems);
            dataItems.close();
            for (final DataItem dataItem : dataItemList) {
                final Uri dataItemUri = dataItem.getUri();
                // In a real calendar application, this might delete the corresponding calendar
                // event from the calendar data provider. In this sample, we simply delete the
                // DataItem, but leave the phone's calendar data intact.
                Wearable.DataApi.deleteDataItems(mGoogleApiClient, dataItemUri)
                        .setResultCallback(new ResultCallback<DataApi.DeleteDataItemsResult>() {
                            @Override
                            public void onResult(DataApi.DeleteDataItemsResult deleteResult) {
                                if (deleteResult.getStatus().isSuccess()) {
                                    appendLog("Successfully deleted data item: " + dataItemUri);
                                } else {
                                    appendLog("Failed to delete data item:" + dataItemUri);
                                }
                            }
                        });
            }
        } else {
            Log.e(TAG, "Failed to delete data items"
                     + " - Client disconnected from Google Play Services");
        }
    }

    private void appendLog(final String s) {
        mLogTextView.post(new Runnable() {
            @Override
            public void run() {
                mLogTextView.append(s);
                mLogTextView.append("\n");
                mScroller.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    @Override
    public void onPeerConnected(Node peer) {
        appendLog("Device connected");
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        appendLog("Device disconnected");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Connected to Google Api Service");
        }
        mResolvingError = false;
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // Ignore
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Disconnected from Google Api Service");
        }
        if (null != Wearable.NodeApi) {
            Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        }
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            mResolvingError = false;
        }
    }
}
