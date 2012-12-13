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

import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ContentValues;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

/**
 * RSSPullParser reads an RSS feed from the Picasa featured pictures site. It uses
 * several packages from the widely-known XMLPull API.
 *
 */
public class RSSPullParser extends DefaultHandler {
    // Global constants

    // An attribute value indicating that the element contains media content
    private static final String CONTENT = "media:content";
    
    // An attribute value indicating that the element contains a thumbnail
    private static final String THUMBNAIL = "media:thumbnail";
    
    // An attribute value indicating that the element contains an item
    private static final String ITEM = "item";

    // Sets the initial size of the vector that stores data.
    private static final int VECTOR_INITIAL_SIZE = 500;

    // Storage for a single ContentValues for image data
    private static ContentValues mImage;
    
    // A vector that will contain all of the images
    private Vector<ContentValues> mImages;

    /**
     * A getter that returns the image data Vector
     * @return A Vector containing all of the image data retrieved by the parser
     */
    public Vector<ContentValues> getImages() {
        return mImages;
    }
    /**
     * This method parses XML in an input stream and stores parts of the data in memory
     *
     * @param inputStream a stream of data containing XML elements, usually a RSS feed
     * @param progressNotifier a helper class for sending status and logs
     * @throws XmlPullParserException defined by XMLPullParser; thrown if the thread is cancelled.
     * @throws IOException thrown if an IO error occurs during parsing
     */
    public void parseXml(InputStream inputStream,
            BroadcastNotifier progressNotifier)
            throws XmlPullParserException, IOException {

        // Instantiates a parser factory
        XmlPullParserFactory localXmlPullParserFactory = XmlPullParserFactory
                .newInstance();

        // Turns off namespace handling for the XML input
        localXmlPullParserFactory.setNamespaceAware(false);

        // Instantiates a new pull parser
        XmlPullParser localXmlPullParser = localXmlPullParserFactory
                .newPullParser();

        // Sets the parser's input stream
        localXmlPullParser.setInput(inputStream, null);

        // Gets the first event in the input sream
        int eventType = localXmlPullParser.getEventType();

        // Sets the number of images read to 1
        int imageCount = 1;

        // Returns if the current event (state) is not START_DOCUMENT
        if (eventType != XmlPullParser.START_DOCUMENT) {

            throw new XmlPullParserException("Invalid RSS");

        }

        // Creates a new store for image URL data
        mImages = new Vector<ContentValues>(VECTOR_INITIAL_SIZE);

        // Loops indefinitely. The exit occurs if there are no more URLs to process
        while (true) {

            // Gets the next event in the input stream
            int nextEvent = localXmlPullParser.next();

            // If the current thread is interrupted, throws an exception and returns
            if (Thread.currentThread().isInterrupted()) {

                throw new XmlPullParserException("Cancelled");

            // At the end of the feed, exits the loop
            } else if (nextEvent == XmlPullParser.END_DOCUMENT) {
                break;

            // At the beginning of the feed, skips the event and continues
            } else if (nextEvent == XmlPullParser.START_DOCUMENT) {
                continue;

            // At the start of a tag, gets the tag's name
            } else if (nextEvent == XmlPullParser.START_TAG) {
                String eventName = localXmlPullParser.getName();

                /*
                 * If this is the start of an individual item, logs it and creates a new
                 * ContentValues
                 */
                if (eventName.equalsIgnoreCase(ITEM)) {

                    mImage = new ContentValues();

                // If this isn't an item, then checks for other options
                } else {

                    // Defines keys to store the column names
                    String imageUrlKey;
                    String imageNameKey;
                    
                    // Defines a place to store the filename of a URL,
                    String fileName;

                    // If it's CONTENT
                    if (eventName.equalsIgnoreCase(CONTENT)) {

                        // Stores the image URL and image name column names as keys
                        imageUrlKey = DataProviderContract.IMAGE_URL_COLUMN;
                        imageNameKey = DataProviderContract.IMAGE_PICTURENAME_COLUMN;

                    // If it's a THUMBNAIL
                    } else if (eventName.equalsIgnoreCase(THUMBNAIL)) {

                        // Stores the thumbnail URL and thumbnail name column names as keys
                        imageUrlKey = DataProviderContract.IMAGE_THUMBURL_COLUMN;
                        imageNameKey = DataProviderContract.IMAGE_THUMBNAME_COLUMN;

                    // Otherwise it's some other event that isn't important
                    } else {
                        continue;
                    }

                    // It's not an ITEM. Gets the URL attribute from the event
                    String urlValue = localXmlPullParser.getAttributeValue(null, "url");

                    // If the value is null, exits
                    if (urlValue == null)
                        break;

                    // Puts the URL and the key into the ContentValues
                    mImage.put(imageUrlKey, urlValue);
                    
                    // Gets the filename of the URL and puts it into the ContentValues
                    fileName = Uri.parse(urlValue).getLastPathSegment();
                    mImage.put(imageNameKey, fileName);
                }
            }
            /*
             * If it's not an ITEM, and it is an END_TAG, and the current event is an ITEM, and
             * there is data in the current ContentValues
             */
            else if ((nextEvent == XmlPullParser.END_TAG)
                    && (localXmlPullParser.getName().equalsIgnoreCase(ITEM))
                    && (mImage != null)) {

                // Adds the current ContentValues to the ContentValues storage
                mImages.add(mImage);

                // Logs progress
                progressNotifier.notifyProgress("Parsed Image[" + imageCount + "]:"
                        + mImage.getAsString(DataProviderContract.IMAGE_URL_COLUMN));

                // Clears out the current ContentValues
                mImage = null;

                // Increments the count of the number of images stored.
                imageCount++;
            }
        }
    }
}
