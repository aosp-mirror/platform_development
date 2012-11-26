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

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;

import org.apache.http.HttpStatus;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Vector;

/**
 * This service pulls RSS content from a web site URL contained in the incoming Intent (see
 * onHandleIntent()). As it runs, it broadcasts its status using LocalBroadcastManager; any
 * component that wants to see the status should implement a subclass of BroadcastReceiver and
 * register to receive broadcast Intents with category = CATEGORY_DEFAULT and action
 * Constants.BROADCAST_ACTION.
 *
 */
public class RSSPullService extends IntentService {
    // Used to write to the system log from this class.
    public static final String LOG_TAG = "RSSPullService";

    // Defines and instantiates an object for handling status updates.
    private BroadcastNotifier mBroadcaster = new BroadcastNotifier(this);

    /**
     * An IntentService must always have a constructor that calls the super constructor. The
     * string supplied to the super constructor is used to give a name to the IntentService's
     * background thread.
     */
    public RSSPullService() {

        super("RSSPullService");
    }

    /**
     * In an IntentService, onHandleIntent is run on a background thread.  As it
     * runs, it broadcasts its current status using the LocalBroadcastManager.
     * @param workIntent The Intent that starts the IntentService. This Intent contains the
     * URL of the web site from which the RSS parser gets data.
     */
    @Override
    protected void onHandleIntent(Intent workIntent) {
        // Gets a URL to read from the incoming Intent's "data" value
        String localUrlString = workIntent.getDataString();

        // Creates a projection to use in querying the modification date table in the provider.
        final String[] dateProjection = new String[]
        {
            DataProviderContract.ROW_ID,
            DataProviderContract.DATA_DATE_COLUMN
        };

        // A URL that's local to this method
        URL localURL;

        // A cursor that's local to this method.
        Cursor cursor = null;

        /*
         * A block that tries to connect to the Picasa featured picture URL passed as the "data"
         * value in the incoming Intent. The block throws exceptions (see the end of the block).
         */
        try {

            // Convert the incoming data string to a URL.
            localURL = new URL(localUrlString);

            /*
             * Tries to open a connection to the URL. If an IO error occurs, this throws an
             * IOException
             */
            URLConnection localURLConnection = localURL.openConnection();

            // If the connection is an HTTP connection, continue
            if ((localURLConnection instanceof HttpURLConnection)) {

                // Broadcasts an Intent indicating that processing has started.
                mBroadcaster.broadcastIntentWithState(Constants.STATE_ACTION_STARTED);

                // Casts the connection to a HTTP connection
                HttpURLConnection localHttpURLConnection = (HttpURLConnection) localURLConnection;

                // Sets the user agent for this request.
                localHttpURLConnection.setRequestProperty("User-Agent", Constants.USER_AGENT);

                /*
                 * Queries the content provider to see if this URL was read previously, and when.
                 * The content provider throws an exception if the URI is invalid.
                 */
                cursor = getContentResolver().query(
                        DataProviderContract.DATE_TABLE_CONTENTURI,
                        dateProjection,
                        null,
                        null,
                        null);

                // Flag to indicate that new metadata was retrieved
                boolean newMetadataRetrieved;

                /*
                 * Tests to see if the table contains a modification date for the URL
                 */
                if (null != cursor && cursor.moveToFirst()) {

                    // Find the URL's last modified date in the content provider
                    long storedModifiedDate =
                            cursor.getLong(cursor.getColumnIndex(
                                    DataProviderContract.DATA_DATE_COLUMN)
                            )
                    ;

                    /*
                     * If the modified date isn't 0, sets another request property to ensure that
                     * data is only downloaded if it has changed since the last recorded
                     * modification date. Formats the date according to the RFC1123 format.
                     */
                    if (0 != storedModifiedDate) {
                        localHttpURLConnection.setRequestProperty(
                                "If-Modified-Since",
                                org.apache.http.impl.cookie.DateUtils.formatDate(
                                        new Date(storedModifiedDate),
                                        org.apache.http.impl.cookie.DateUtils.PATTERN_RFC1123));
                    }

                    // Marks that new metadata does not need to be retrieved
                    newMetadataRetrieved = false;

                } else {

                    /*
                     * No modification date was found for the URL, so newmetadata has to be
                     * retrieved.
                     */
                    newMetadataRetrieved = true;

                }

                // Reports that the service is about to connect to the RSS feed
                mBroadcaster.broadcastIntentWithState(Constants.STATE_ACTION_CONNECTING);

                // Gets a response code from the RSS server
                int responseCode = localHttpURLConnection.getResponseCode();

                switch (responseCode) {

                    // If the response is OK
                    case HttpStatus.SC_OK:

                        // Gets the last modified data for the URL
                        long lastModifiedDate = localHttpURLConnection.getLastModified();

                        // Reports that the service is parsing
                        mBroadcaster.broadcastIntentWithState(Constants.STATE_ACTION_PARSING);

                        /*
                         * Instantiates a pull parser and uses it to parse XML from the RSS feed.
                         * The mBroadcaster argument send a broadcaster utility object to the
                         * parser.
                         */
                        RSSPullParser localPicasaPullParser = new RSSPullParser();

                        localPicasaPullParser.parseXml(
                            localURLConnection.getInputStream(),
                            mBroadcaster);

                        // Reports that the service is now writing data to the content provider.
                        mBroadcaster.broadcastIntentWithState(Constants.STATE_ACTION_WRITING);

                        // Gets image data from the parser
                        Vector<ContentValues> imageValues = localPicasaPullParser.getImages();

                        // Stores the number of images
                        int imageVectorSize = imageValues.size();

                        // Creates one ContentValues for each image
                        ContentValues[] imageValuesArray = new ContentValues[imageVectorSize];

                        imageValuesArray = imageValues.toArray(imageValuesArray);

                        /*
                         * Stores the image data in the content provider. The content provider
                         * throws an exception if the URI is invalid.
                         */
                        getContentResolver().bulkInsert(
                                DataProviderContract.PICTUREURL_TABLE_CONTENTURI, imageValuesArray);

                        // Creates another ContentValues for storing date information
                        ContentValues dateValues = new ContentValues();

                        // Adds the URL's last modified date to the ContentValues
                        dateValues.put(DataProviderContract.DATA_DATE_COLUMN, lastModifiedDate);

                        if (newMetadataRetrieved) {

                            // No previous metadata existed, so insert the data
                            getContentResolver().insert(
                                DataProviderContract.DATE_TABLE_CONTENTURI,
                                dateValues
                            );

                        } else {

                            // Previous metadata existed, so update it.
                            getContentResolver().update(
                                    DataProviderContract.DATE_TABLE_CONTENTURI,
                                    dateValues,
                                    DataProviderContract.ROW_ID + "=" +
                                            cursor.getString(cursor.getColumnIndex(
                                                            DataProviderContract.ROW_ID)), null);
                        }
                        break;

                }

                // Reports that the feed retrieval is complete.
                mBroadcaster.broadcastIntentWithState(Constants.STATE_ACTION_COMPLETE);
            }

        // Handles possible exceptions
        } catch (MalformedURLException localMalformedURLException) {

            localMalformedURLException.printStackTrace();

        } catch (IOException localIOException) {

            localIOException.printStackTrace();

        } catch (XmlPullParserException localXmlPullParserException) {

            localXmlPullParserException.printStackTrace();

        } finally {

            // If an exception occurred, close the cursor to prevent memory leaks.
            if (null != cursor) {
                cursor.close();
            }
        }
    }

}
