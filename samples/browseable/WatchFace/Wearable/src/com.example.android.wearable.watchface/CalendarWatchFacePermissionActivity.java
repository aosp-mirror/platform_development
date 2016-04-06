package com.example.android.wearable.watchface;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;

/**
 * Simple Activity for displaying Calendar Permission Rationale to user.
 */
public class CalendarWatchFacePermissionActivity extends WearableActivity {

    private static final String TAG = "PermissionActivity";

    /* Id to identify permission request for calendar. */
    private static final int PERMISSION_REQUEST_READ_CALENDAR = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_watch_face_permission);
        setAmbientEnabled();
    }

    public void onClickEnablePermission(View view) {
        Log.d(TAG, "onClickEnablePermission()");

        // On 23+ (M+) devices, GPS permission not granted. Request permission.
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.READ_CALENDAR},
                PERMISSION_REQUEST_READ_CALENDAR);

    }

    /*
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        Log.d(TAG, "onRequestPermissionsResult()");

        if (requestCode == PERMISSION_REQUEST_READ_CALENDAR) {
            if ((grantResults.length == 1)
                    && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                finish();
            }
        }
    }
}