package com.example.android.wearable.agendadata;


import static com.example.android.wearable.agendadata.Constants.TAG;
import static com.example.android.wearable.agendadata.Constants.CONNECTION_TIME_OUT_MS;
import static com.example.android.wearable.agendadata.Constants.CAL_DATA_ITEM_PATH_PREFIX;
import static com.example.android.wearable.agendadata.Constants.ALL_DAY;
import static com.example.android.wearable.agendadata.Constants.BEGIN;
import static com.example.android.wearable.agendadata.Constants.DATA_ITEM_URI;
import static com.example.android.wearable.agendadata.Constants.DESCRIPTION;
import static com.example.android.wearable.agendadata.Constants.END;
import static com.example.android.wearable.agendadata.Constants.EVENT_ID;
import static com.example.android.wearable.agendadata.Constants.ID;
import static com.example.android.wearable.agendadata.Constants.PROFILE_PIC;
import static com.example.android.wearable.agendadata.Constants.TITLE;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.text.format.Time;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Queries calendar events using Android Calendar Provider API and creates a data item for each
 * event.
 */
public class CalendarQueryService extends IntentService
        implements ConnectionCallbacks, OnConnectionFailedListener {

    private static final String[] INSTANCE_PROJECTION = {
            CalendarContract.Instances._ID,
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.ORGANIZER
    };

    private static final String[] CONTACT_PROJECTION = new String[] { Data._ID, Data.CONTACT_ID };
    private static final String CONTACT_SELECTION = Email.ADDRESS + " = ?";

    private GoogleApiClient mGoogleApiClient;

    public CalendarQueryService() {
        super(CalendarQueryService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mGoogleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
        // Query calendar events in the next 24 hours.
        Time time = new Time();
        time.setToNow();
        long beginTime = time.toMillis(true);
        time.monthDay++;
        time.normalize(true);
        long endTime = time.normalize(true);

        List<Event> events = queryEvents(this, beginTime, endTime);
        for (Event event : events) {
            final PutDataMapRequest putDataMapRequest = event.toPutDataMapRequest();
            if (mGoogleApiClient.isConnected()) {
                Wearable.DataApi.putDataItem(
                    mGoogleApiClient, putDataMapRequest.asPutDataRequest()).await();
            } else {
                Log.e(TAG, "Failed to send data item: " + putDataMapRequest
                         + " - Client disconnected from Google Play Services");
            }
        }
        mGoogleApiClient.disconnect();
    }

    private static String makeDataItemPath(long eventId, long beginTime) {
        return CAL_DATA_ITEM_PATH_PREFIX + eventId + "/" + beginTime;
    }

    private static List<Event> queryEvents(Context context, long beginTime, long endTime) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, beginTime);
        ContentUris.appendId(builder, endTime);

        Cursor cursor = contentResolver.query(builder.build(), INSTANCE_PROJECTION,
                null /* selection */, null /* selectionArgs */, null /* sortOrder */);
        try {
            int idIdx = cursor.getColumnIndex(CalendarContract.Instances._ID);
            int eventIdIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID);
            int titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE);
            int beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN);
            int endIdx = cursor.getColumnIndex(CalendarContract.Instances.END);
            int allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY);
            int descIdx = cursor.getColumnIndex(CalendarContract.Instances.DESCRIPTION);
            int ownerEmailIdx = cursor.getColumnIndex(CalendarContract.Instances.ORGANIZER);

            List<Event> events = new ArrayList<Event>(cursor.getCount());
            while (cursor.moveToNext()) {
                Event event = new Event();
                event.id = cursor.getLong(idIdx);
                event.eventId = cursor.getLong(eventIdIdx);
                event.title = cursor.getString(titleIdx);
                event.begin = cursor.getLong(beginIdx);
                event.end = cursor.getLong(endIdx);
                event.allDay = cursor.getInt(allDayIdx) != 0;
                event.description = cursor.getString(descIdx);
                String ownerEmail = cursor.getString(ownerEmailIdx);
                Cursor contactCursor = contentResolver.query(Data.CONTENT_URI,
                        CONTACT_PROJECTION, CONTACT_SELECTION, new String[] {ownerEmail}, null);
                int ownerIdIdx = contactCursor.getColumnIndex(Data.CONTACT_ID);
                long ownerId = -1;
                if (contactCursor.moveToFirst()) {
                    ownerId = contactCursor.getLong(ownerIdIdx);
                }
                contactCursor.close();
                // Use event organizer's profile picture as the notification background.
                event.ownerProfilePic = getProfilePicture(contentResolver, context, ownerId);
                events.add(event);
            }
            return events;
        } finally {
            cursor.close();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
    }

    @Override
    public void onConnectionSuspended(int cause) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
    }

    private static Asset getDefaultProfile(Resources res) {
        Bitmap bitmap = BitmapFactory.decodeResource(res, R.drawable.nobody);
        return Asset.createFromBytes(toByteArray(bitmap));
    }

    private static Asset getProfilePicture(ContentResolver contentResolver, Context context,
                                           long contactId) {
        if (contactId != -1) {
            // Try to retrieve the profile picture for the given contact.
            Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
            InputStream inputStream = Contacts.openContactPhotoInputStream(contentResolver,
                    contactUri, true /*preferHighres*/);

            if (null != inputStream) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    if (bitmap != null) {
                        return Asset.createFromBytes(toByteArray(bitmap));
                    } else {
                        Log.e(TAG, "Cannot decode profile picture for contact " + contactId);
                    }
                } finally {
                    closeQuietly(inputStream);
                }
            }
        }
        // Use a default background image if the user has no profile picture or there was an error.
        return getDefaultProfile(context.getResources());
    }

    private static byte[] toByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        closeQuietly(stream);
        return byteArray;
    }

    private static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            Log.e(TAG, "IOException while closing closeable.", e);
        }
    }

    private static class Event {

        public long id;
        public long eventId;
        public String title;
        public long begin;
        public long end;
        public boolean allDay;
        public String description;
        public Asset ownerProfilePic;

        public PutDataMapRequest toPutDataMapRequest(){
            final PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(
                    makeDataItemPath(eventId, begin));
            DataMap data = putDataMapRequest.getDataMap();
            data.putString(DATA_ITEM_URI, putDataMapRequest.getUri().toString());
            data.putLong(ID, id);
            data.putLong(EVENT_ID, eventId);
            data.putString(TITLE, title);
            data.putLong(BEGIN, begin);
            data.putLong(END, end);
            data.putBoolean(ALL_DAY, allDay);
            data.putString(DESCRIPTION, description);
            data.putAsset(PROFILE_PIC, ownerProfilePic);

            return putDataMapRequest;
        }
    }
}
