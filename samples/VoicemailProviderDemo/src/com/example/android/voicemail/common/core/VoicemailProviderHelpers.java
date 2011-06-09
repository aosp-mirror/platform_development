/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.voicemail.common.core;

import com.example.android.provider.VoicemailContract;
import com.example.android.provider.VoicemailContract.Voicemails;
import com.example.android.voicemail.common.logging.Logger;
import com.example.android.voicemail.common.utils.CloseUtils;
import com.example.android.voicemail.common.utils.DbQueryUtils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the {@link VoicemailProviderHelper} interface.
 */
public final class VoicemailProviderHelpers implements VoicemailProviderHelper {
    private static final Logger logger = Logger.getLogger(VoicemailProviderHelpers.class);

    /** Full projection on the voicemail table, giving us all the columns. */
    private static final String[] FULL_PROJECTION = new String[] {
            Voicemails._ID,
            Voicemails.HAS_CONTENT,
            Voicemails.NUMBER,
            Voicemails.DURATION,
            Voicemails.DATE,
            Voicemails.SOURCE_PACKAGE,
            Voicemails.SOURCE_DATA,
            Voicemails.NEW,
            Voicemails.STATE
    };

    private final ContentResolver mContentResolver;
    private final Uri mBaseUri;

    /**
     * Creates an instance of {@link VoicemailProviderHelpers} that wraps the supplied content
     * provider.
     *
     * @param contentResolver the ContentResolver used for opening the output stream to read and
     *            write to the file
     */
    private VoicemailProviderHelpers(Uri baseUri, ContentResolver contentResolver) {
        mContentResolver = contentResolver;
        mBaseUri = baseUri;
    }

    /**
     * Constructs a VoicemailProviderHelper with full access to all voicemails.
     * <p>
     * Requires the manifest permissions
     * <code>com.android.providers.voicemail.permission.READ_WRITE_ALL_VOICEMAIL</code> and
     * <code>com.android.providers.voicemail.permission.READ_WRITE_OWN_VOICEMAIL</code>.
     */
    public static VoicemailProviderHelper createFullVoicemailProvider(Context context) {
        return new VoicemailProviderHelpers(VoicemailContract.CONTENT_URI,
                context.getContentResolver());
    }

    /**
     * Constructs a VoicemailProviderHelper with limited access to voicemails created by this
     * source.
     * <p>
     * Requires the manifest permission
     * <code>com.android.providers.voicemail.permission.READ_WRITE_OWN_VOICEMAIL</code>.
     */
    public static VoicemailProviderHelper createPackageScopedVoicemailProvider(Context context) {
        Uri providerUri = Uri.withAppendedPath(VoicemailContract.CONTENT_URI_SOURCE,
                context.getPackageName());
        return new VoicemailProviderHelpers(providerUri, context.getContentResolver());
    }

    @Override
    public Uri insert(Voicemail voicemail) {
        check(!voicemail.hasId(), "Inserted voicemails must not have an id", voicemail);
        check(voicemail.hasTimestampMillis(), "Inserted voicemails must have a timestamp",
                voicemail);
        check(voicemail.hasNumber(), "Inserted voicemails must have a number", voicemail);
        logger.d(String.format("Inserting new voicemail: %s", voicemail));
        ContentValues contentValues = getContentValues(voicemail);
        return mContentResolver.insert(mBaseUri, contentValues);
    }

    @Override
    public int update(Uri uri, Voicemail voicemail) {
        check(!voicemail.hasUri(), "Can't update the Uri of a voicemail", voicemail);
        logger.d("Updating voicemail: " + voicemail + " for uri: " + uri);
        ContentValues values = getContentValues(voicemail);
        return mContentResolver.update(uri, values, null, null);
    }

    @Override
    public OutputStream setVoicemailContent(Uri voicemailUri, String mimeType) throws IOException {
        ContentValues values = new ContentValues();
        values.put(Voicemails.MIME_TYPE, mimeType);
        int updatedCount = mContentResolver.update(voicemailUri, values, null, null);
        if (updatedCount != 1) {
            throw new IOException("Updating voicemail should have updated 1 row, was: "
                    + updatedCount);
        }
        logger.d(String.format("Writing new voicemail content: %s", voicemailUri));
        return mContentResolver.openOutputStream(voicemailUri);
    }

    @Override
    public Voicemail findVoicemailBySourceData(String sourceData) {
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(mBaseUri, FULL_PROJECTION,
                    DbQueryUtils.getEqualityClause(Voicemails.SOURCE_DATA, sourceData),
                    null, null);
            if (cursor.getCount() != 1) {
                logger.w("Expected 1 voicemail matching sourceData " + sourceData + ", got " +
                        cursor.getCount());
                return null;
            }
            cursor.moveToFirst();
            return getVoicemailFromCursor(cursor);
        } finally {
            CloseUtils.closeQuietly(cursor);
        }
    }

    @Override
    public Voicemail findVoicemailByUri(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(uri, FULL_PROJECTION, null, null, null);
            if (cursor.getCount() != 1) {
                logger.w("Expected 1 voicemail matching uri " + uri + ", got " + cursor.getCount());
                return null;
            }
            cursor.moveToFirst();
            Voicemail voicemail = getVoicemailFromCursor(cursor);
            // Make sure this is an exact match.
            if (voicemail.getUri().equals(uri)) {
                return voicemail;
            } else {
                logger.w("Queried uri: " + uri + " do not represent a unique voicemail record.");
                return null;
            }
        } finally {
            CloseUtils.closeQuietly(cursor);
        }
    }

    @Override
    public Uri getUriForVoicemailWithId(long id) {
        return ContentUris.withAppendedId(mBaseUri, id);
    }

    /**
     * Checks that an assertion is true.
     *
     * @throws IllegalArgumentException if the assertion is false, along with a suitable message
     *             including a toString() representation of the voicemail
     */
    private void check(boolean assertion, String message, Voicemail voicemail) {
        if (!assertion) {
            throw new IllegalArgumentException(message + ": " + voicemail);
        }
    }

    @Override
    public int deleteAll() {
        logger.i(String.format("Deleting all voicemails"));
        return mContentResolver.delete(mBaseUri, "", new String[0]);
    }

    @Override
    public List<Voicemail> getAllVoicemails() {
        return getAllVoicemails(null, null, SortOrder.DEFAULT);
    }

    @Override
    public List<Voicemail> getAllVoicemails(VoicemailFilter filter,
            String sortColumn, SortOrder sortOrder) {
        logger.i(String.format("Fetching all voicemails"));
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(mBaseUri, FULL_PROJECTION,
                    filter != null ? filter.getWhereClause() : null,
                    null, getSortBy(sortColumn, sortOrder));
            List<Voicemail> results = new ArrayList<Voicemail>(cursor.getCount());
            while (cursor.moveToNext()) {
                // A performance optimisation is possible here.
                // The helper method extracts the column indices once every time it is called,
                // whilst
                // we could extract them all up front (without the benefit of the re-use of the
                // helper
                // method code).
                // At the moment I'm pretty sure the benefits outweigh the costs, so leaving as-is.
                results.add(getVoicemailFromCursor(cursor));
            }
            return results;
        } finally {
            CloseUtils.closeQuietly(cursor);
        }
    }

    private String getSortBy(String column, SortOrder sortOrder) {
        if (column == null) {
            return null;
        }
        switch (sortOrder) {
            case ASCENDING:
                return column + " ASC";
            case DESCENDING:
                return column + " DESC";
            case DEFAULT:
                return column;
        }
        // Should never reach here.
        return null;
    }

    private VoicemailImpl getVoicemailFromCursor(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(Voicemails._ID));
        String sourcePackage = cursor.getString(
                cursor.getColumnIndexOrThrow(Voicemails.SOURCE_PACKAGE));
        Uri voicemailUri = ContentUris.withAppendedId(
                Uri.withAppendedPath(VoicemailContract.CONTENT_URI_SOURCE, sourcePackage), id);
        VoicemailImpl voicemail = VoicemailImpl
                .createEmptyBuilder()
                .setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(Voicemails.DATE)))
                .setNumber(cursor.getString(cursor.getColumnIndexOrThrow(Voicemails.NUMBER)))
                .setId(id)
                .setDuration(cursor.getLong(cursor.getColumnIndexOrThrow(Voicemails.DURATION)))
                .setSourcePackage(sourcePackage)
                .setSourceData(cursor.getString(
                        cursor.getColumnIndexOrThrow(Voicemails.SOURCE_DATA)))
                .setUri(voicemailUri)
                .setHasContent(cursor.getInt(
                        cursor.getColumnIndexOrThrow(Voicemails.HAS_CONTENT)) == 1)
                .setIsRead(cursor.getInt(cursor.getColumnIndexOrThrow(Voicemails.NEW)) == 1)
                .setMailbox(mapValueToMailBoxEnum(cursor.getInt(
                        cursor.getColumnIndexOrThrow(Voicemails.STATE))))
                .build();
        return voicemail;
    }

    private Voicemail.Mailbox mapValueToMailBoxEnum(int value) {
        for (Voicemail.Mailbox mailbox : Voicemail.Mailbox.values()) {
            if (mailbox.getValue() == value) {
                return mailbox;
            }
        }
        throw new IllegalArgumentException("Value: " + value + " not valid for Voicemail.Mailbox.");
    }

    /**
     * Maps structured {@link Voicemail} to {@link ContentValues} understood by content provider.
     */
    private ContentValues getContentValues(Voicemail voicemail) {
        ContentValues contentValues = new ContentValues();
        if (voicemail.hasTimestampMillis()) {
            contentValues.put(Voicemails.DATE, String.valueOf(voicemail.getTimestampMillis()));
        }
        if (voicemail.hasNumber()) {
            contentValues.put(Voicemails.NUMBER, voicemail.getNumber());
        }
        if (voicemail.hasDuration()) {
            contentValues.put(Voicemails.DURATION, String.valueOf(voicemail.getDuration()));
        }
        if (voicemail.hasSourcePackage()) {
            contentValues.put(Voicemails.SOURCE_PACKAGE, voicemail.getSourcePackage());
        }
        if (voicemail.hasSourceData()) {
            contentValues.put(Voicemails.SOURCE_DATA, voicemail.getSourceData());
        }
        if (voicemail.hasRead()) {
            contentValues.put(Voicemails.NEW, voicemail.isRead() ? 1 : 0);
        }
        if (voicemail.hasMailbox()) {
            contentValues.put(Voicemails.STATE, voicemail.getMailbox().getValue());
        }
        return contentValues;
    }
}
