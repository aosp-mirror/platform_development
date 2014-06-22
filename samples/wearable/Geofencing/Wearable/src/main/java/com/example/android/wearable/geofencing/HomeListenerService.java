package com.example.android.wearable.geofencing;

import static com.example.android.wearable.geofencing.Constants.ACTION_CHECK_IN;
import static com.example.android.wearable.geofencing.Constants.ACTION_DELETE_DATA_ITEM;
import static com.example.android.wearable.geofencing.Constants.ANDROID_BUILDING_ID;
import static com.example.android.wearable.geofencing.Constants.KEY_GEOFENCE_ID;
import static com.example.android.wearable.geofencing.Constants.NOTIFICATION_ID;
import static com.example.android.wearable.geofencing.Constants.TAG;
import static com.example.android.wearable.geofencing.Constants.YERBA_BUENA_ID;

import android.app.Notification;
import android.app.Notification.Action;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Listens to DataItem events on the wearable device.
 */
public class HomeListenerService extends WearableListenerService {

    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this.getApplicationContext())
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    /**
     * Listen for DataItems added/deleted from the geofence service running on the companion.
     */
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onDataChanged: " + dataEvents + " for " + getPackageName());
        }
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_DELETED) {
                cancelNotificationForDataItem(event.getDataItem());
            } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                // The user has entered a geofence - post a notification!
                String geofenceId = DataMap.fromByteArray(event.getDataItem().getData())
                        .getString(KEY_GEOFENCE_ID);
                postNotificationForGeofenceId(geofenceId, event.getDataItem().getUri());
            }
        }
        dataEvents.close();
    }

    /**
     * Deletes the check-in notification when the DataItem is deleted.
     * @param dataItem Used only for logging in this sample, but could be used to identify which
     *                 notification to cancel (in this case, there is at most 1 notification).
     */
    private void cancelNotificationForDataItem(DataItem dataItem) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "onDataItemDeleted:DataItem=" + dataItem.getUri());
        }
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
    }

    /**
     * Posts a local notification for the given geofence id, with an option to check in.
     * @param geofenceId The geofence id that the user has triggered.
     * @param dataItemUri The Uri for the DataItem that triggered this notification. Used to delete
     *                    this DataItem when the notification is dismissed.
     */
    private void postNotificationForGeofenceId(String geofenceId, Uri dataItemUri) {
        // Use the geofenceId to determine the title and background of the check-in notification.
        // A SpannableString is used for the notification title for resizing capabilities.
        SpannableString checkInTitle;
        Bitmap notificationBackground;
        if (ANDROID_BUILDING_ID.equals(geofenceId)) {
            checkInTitle = new SpannableString(getText(R.string.android_building_title));
            notificationBackground =
                    BitmapFactory.decodeResource(getResources(), R.drawable.android_building);
        } else if (YERBA_BUENA_ID.equals(geofenceId)) {
            checkInTitle = new SpannableString(getText(R.string.yerba_buena_title));
            notificationBackground =
                    BitmapFactory.decodeResource(getResources(), R.drawable.yerba_buena);
        } else {
            Log.e(TAG, "Unrecognized geofence id: " + geofenceId);
            return;
        }
        // Resize the title to avoid truncation.
        checkInTitle.setSpan(new RelativeSizeSpan(0.8f), 0, checkInTitle.length(),
                Spannable.SPAN_POINT_MARK);

        Intent checkInOperation =
                new Intent(this, CheckInAndDeleteDataItemsService.class).setData(dataItemUri);
        PendingIntent checkInIntent = PendingIntent.getService(this, 0,
                checkInOperation.setAction(ACTION_CHECK_IN), PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent deleteDataItemIntent = PendingIntent.getService(this, 1,
                checkInOperation.setAction(ACTION_DELETE_DATA_ITEM),
                PendingIntent.FLAG_CANCEL_CURRENT);
        // This action will be embedded into the notification.
        Action checkInAction = new Action(R.drawable.ic_action_check_in,
                getText(R.string.check_in_prompt), checkInIntent);

        Notification notification = new Notification.Builder(this)
                .setContentTitle(checkInTitle)
                .setContentText(getText(R.string.check_in_prompt))
                .setSmallIcon(R.drawable.ic_launcher)
                .setDeleteIntent(deleteDataItemIntent)
                .extend(new Notification.WearableExtender()
                        .setBackground(notificationBackground)
                        .addAction(checkInAction)
                        .setContentAction(0)
                        .setHintHideIcon(true))
                .setLocalOnly(true)
                .build();

        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .notify(NOTIFICATION_ID, notification);
    }

}
