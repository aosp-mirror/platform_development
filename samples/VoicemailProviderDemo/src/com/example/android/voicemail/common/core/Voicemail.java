/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.example.android.voicemail.common.core;

import android.net.Uri;

/**
 * Represents a single voicemail stored in the voicemail content provider.
 * <p>
 * The presence of a field is indicated by a corresponding 'has' method.
 */
public interface Voicemail {
    /**
     * The identifier of the voicemail in the content provider.
     * <p>
     * This may be missing in the case of a new {@link Voicemail} that we plan to insert into the
     * content provider, since until it has been inserted we don't know what id it should have. If
     * none is specified, we return -1.
     */
    public long getId();

    public boolean hasId();

    /** The number of the person leaving the voicemail, empty string if unknown, null if not set. */
    public String getNumber();

    public boolean hasNumber();

    /** The timestamp the voicemail was received, in millis since the epoch, zero if not set. */
    public long getTimestampMillis();

    public boolean hasTimestampMillis();

    /** Gets the duration of the voicemail in millis, or zero if the field is not set. */
    public long getDuration();

    public boolean hasDuration();

    /**
     * Returns the package name of the source that added this voicemail, or null if this field is
     * not set.
     */
    public String getSourcePackage();

    public boolean hasSourcePackage();

    /**
     * Returns the application-specific data type stored with the voicemail, or null if this field
     * is not set.
     * <p>
     * Source data is typically used as an identifier to uniquely identify the voicemail against
     * the voicemail server. This is likely to be something like the IMAP UID, or some other
     * server-generated identifying string.
     */
    public String getSourceData();

    public boolean hasSourceData();

    /**
     * Gets the Uri that can be used to refer to this voicemail, and to make it play.
     * <p>
     * Returns null if we don't know the Uri.
     */
    public Uri getUri();

    public boolean hasUri();

    /**
     * Tells us if the voicemail message has been marked as read.
     * <p>
     * Always returns false if this field has not been set, i.e. if hasRead() returns false.
     */
    public boolean isRead();

    public boolean hasRead();

    /**
     * Tells us if there is content stored at the Uri.
     */
    public boolean hasContent();
}
