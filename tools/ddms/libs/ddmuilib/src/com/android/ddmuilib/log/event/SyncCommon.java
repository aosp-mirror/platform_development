/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.ddmuilib.log.event;

import com.android.ddmlib.log.EventContainer;
import com.android.ddmlib.log.EventLogParser;
import com.android.ddmlib.log.InvalidTypeException;

import java.awt.Color;

abstract public class SyncCommon extends EventDisplay {

    // State information while processing the event stream
    private int mLastState; // 0 if event started, 1 if event stopped
    private long mLastStartTime; // ms
    private long mLastStopTime; //ms
    private String mLastDetails;
    private int mLastSyncSource; // poll, server, user, etc.

    // Some common variables for sync display.  These define the sync backends
    //and how they should be displayed.
    protected static final int CALENDAR = 0;
    protected static final int GMAIL = 1;
    protected static final int FEEDS = 2;
    protected static final int CONTACTS = 3;
    protected static final int ERRORS = 4;
    protected static final int NUM_AUTHS = (CONTACTS + 1);
    protected static final String AUTH_NAMES[] = {"Calendar", "Gmail", "Feeds", "Contacts",
            "Errors"};
    protected static final Color AUTH_COLORS[] = {Color.MAGENTA, Color.GREEN, Color.BLUE,
            Color.ORANGE, Color.RED};

    // Values from data/etc/event-log-tags
    final int EVENT_SYNC = 2720;
    final int EVENT_TICKLE = 2742;
    final int EVENT_SYNC_DETAILS = 2743;
    final int EVENT_CONTACTS_AGGREGATION = 2747;

    protected SyncCommon(String name) {
        super(name);
    }

    /**
     * Resets the display.
     */
    @Override
    void resetUI() {
        mLastStartTime = 0;
        mLastStopTime = 0;
        mLastState = -1;
        mLastSyncSource = -1;
        mLastDetails = "";
    }

    /**
     * Updates the display with a new event.  This is the main entry point for
     * each event.  This method has the logic to tie together the start event,
     * stop event, and details event into one graph item.  The combined sync event
     * is handed to the subclass via processSycnEvent.  Note that the details
     * can happen before or after the stop event.
     *
     * @param event     The event
     * @param logParser The parser providing the event.
     */
    @Override
    void newEvent(EventContainer event, EventLogParser logParser) {
        try {
            if (event.mTag == EVENT_SYNC) {
                int state = Integer.parseInt(event.getValueAsString(1));
                if (state == 0) { // start
                    mLastStartTime = (long) event.sec * 1000L + (event.nsec / 1000000L);
                    mLastState = 0;
                    mLastSyncSource = Integer.parseInt(event.getValueAsString(2));                    
                    mLastDetails = "";
                } else if (state == 1) { // stop
                    if (mLastState == 0) {
                        mLastStopTime = (long) event.sec * 1000L + (event.nsec / 1000000L);
                        if (mLastStartTime == 0) {
                            // Log starts with a stop event
                            mLastStartTime = mLastStopTime;
                        }
                        int auth = getAuth(event.getValueAsString(0));
                        processSyncEvent(event, auth, mLastStartTime, mLastStopTime, mLastDetails,
                                true, mLastSyncSource);
                        mLastState = 1;
                    }
                }
            } else if (event.mTag == EVENT_SYNC_DETAILS) {
                mLastDetails = event.getValueAsString(3);
                if (mLastState != 0) { // Not inside event
                    long updateTime = (long) event.sec * 1000L + (event.nsec / 1000000L);
                    if (updateTime - mLastStopTime <= 250) {
                        // Got details within 250ms after event, so delete and re-insert
                        // Details later than 250ms (arbitrary) are discarded as probably
                        // unrelated.
                        int auth = getAuth(event.getValueAsString(0));
                        processSyncEvent(event, auth, mLastStartTime, mLastStopTime, mLastDetails,
                                false, mLastSyncSource);
                    }
                }
            } else if (event.mTag == EVENT_CONTACTS_AGGREGATION) {
                long stopTime = (long) event.sec * 1000L + (event.nsec / 1000000L);
                long startTime = stopTime - Long.parseLong(event.getValueAsString(0));
                String details;
                int count = Integer.parseInt(event.getValueAsString(1));
                if (count < 0) {
                    details = "g" + (-count);
                } else {
                    details = "G" + count;
                }
                processSyncEvent(event, CONTACTS, startTime, stopTime, details,
                        true /* newEvent */, mLastSyncSource);
            }
        } catch (InvalidTypeException e) {
        }
    }

    /**
     * Callback hook for subclass to process a sync event.  newEvent has the logic
     * to combine start and stop events and passes a processed event to the
     * subclass.
     *
     * @param event     The sync event
     * @param auth      The sync authority
     * @param startTime Start time (ms) of events
     * @param stopTime  Stop time (ms) of events
     * @param details   Details associated with the event.
     * @param newEvent  True if this event is a new sync event.  False if this event
     * @param syncSource Poll, user, server, etc.
     */
    abstract void processSyncEvent(EventContainer event, int auth, long startTime, long stopTime,
            String details, boolean newEvent, int syncSource);
     
    /**
     * Converts authority name to auth number.
     *
     * @param authname "calendar", etc.
     * @return number series number associated with the authority
     */
    protected int getAuth(String authname) throws InvalidTypeException {
        if ("calendar".equals(authname) || "cl".equals(authname)) {
            return CALENDAR;
        } else if ("contacts".equals(authname) || "cp".equals(authname) ||
                "com.android.contacts".equals(authname)) {
            return CONTACTS;
        } else if ("subscribedfeeds".equals(authname)) {
            return FEEDS;
        } else if ("gmail-ls".equals(authname) || "mail".equals(authname)) {
            return GMAIL;
        } else if ("gmail-live".equals(authname)) {
            return GMAIL;
        } else if ("unknown".equals(authname)) {
            return -1; // Unknown tickles; discard
        } else {
            throw new InvalidTypeException("Unknown authname " + authname);
        }
    }
}
