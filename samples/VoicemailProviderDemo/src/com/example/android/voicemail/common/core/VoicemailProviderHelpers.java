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

import com.example.android.voicemail.common.logging.Logger;
import com.example.android.voicemail.common.utils.CloseUtils;
import com.example.android.voicemail.common.utils.DbQueryUtils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.VoicemailContract;
import android.provider.VoicemailContract.Voicemails;

import java.io.IOException;
import java.io.InputStream;
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
            Voicemails.IS_READ
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
        return new VoicemailProviderHelpers(Voicemails.CONTENT_URI, context.getContentResolver());
    }

    /**
     * Constructs a VoicemailProviderHelper with limited access to voicemails created by this
     * source.
     * <p>
     * Requires the manifest permission
     * <code>com.android.providers.voicemail.permission.READ_WRITE_OWN_VOICEMAIL</code>.
     */
    public static VoicemailProviderHelper createPackageScopedVoicemailProvider(Context context) {
        return new VoicemailProviderHelpers(Voicemails.buildSourceUri(context.getPackageName()),
                context.getContentResolver());
    }

    @Override
    public Uri insert(Voicemail voicemail) {
        check(!voicemail.hasId(), "Inserted voicemails must not have an id", voicemail);
        check(voicemail.hasTimestampMillis(), "Inserted voicemails must have a timestamp",
                voicemail);
        check(voicemail.hasNumber(), "Inserted voicemails must have a number", voicemail);
        logger.d(String.format("Inserting new voicemail: %s", voicemail));
        ContentValues contentValues = getContentValues(voicemail);
        if (!voicemail.hasRead()) {
            // If is_read is not set then set it to false as default value.
            contentValues.put(Voicemails.IS_READ, 0);
        }
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
    public void setVoicemailContent(Uri voicemailUri, InputStream inputStream, String mimeType)
            throws IOException {
        setVoicemailContent(voicemailUri, null, inputStream, mimeType);
    }

    @Override
    public void setVoicemailContent(Uri voicemailUri, byte[] inputBytes, String mimeType)
            throws IOException {
        setVoicemailContent(voicemailUri, inputBytes, null, mimeType);
    }

    private void setVoicemailContent(Uri voicemailUri, byte[] inputBytes, InputStream inputStream,
            String mimeType) throws IOException {
        if (inputBytes != null && inputStream != null) {
            throw new IllegalArgumentException("Both inputBytes & inputStream non-null. Don't" +
                    " know which one to use.");
        }

        logger.d(String.format("Writing new voicemail content: %s", voicemailUri));
        OutputStream outputStream = null;
        try {
            outputStream = mContentResolver.openOutputStream(voicemailUri);
            if (inputBytes != null) {
                outputStream.write(inputBytes);
            } else if (inputStream != null) {
                copyStreamData(inputStream, outputStream);
            }
        } finally {
            CloseUtils.closeQuietly(outputStream);
        }
        // Update mime_type & has_content after we are done with file update.
        ContentValues values = new ContentValues();
        values.put(Voicemails.MIME_TYPE, mimeType);
        values.put(Voicemails.HAS_CONTENT, true);
        int updatedCount = mContentResolver.update(voicemailUri, values, null, null);
        if (updatedCount != 1) {
            throw new IOException("Updating voicemail should have updated 1 row, was: "
                    + updatedCount);
        }
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
        VoicemailImpl voicemail = VoicemailImpl
                .createEmptyBuilder()
                .setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(Voicemails.DATE)))
                .setNumber(cursor.getString(cursor.getColumnIndexOrThrow(Voicemails.NUMBER)))
                .setId(id)
                .setDuration(cursor.getLong(cursor.getColumnIndexOrThrow(Voicemails.DURATION)))
                .setSourcePackage(sourcePackage)
                .setSourceData(cursor.getString(
                        cursor.getColumnIndexOrThrow(Voicemails.SOURCE_DATA)))
                .setUri(buildUriWithSourcePackage(id, sourcePackage))
                .setHasContent(cursor.getInt(
                        cursor.getColumnIndexOrThrow(Voicemails.HAS_CONTENT)) == 1)
                .setIsRead(cursor.getInt(cursor.getColumnIndexOrThrow(Voicemails.IS_READ)) == 1)
                .build();
        return voicemail;
    }

    private Uri buildUriWithSourcePackage(long id, String sourcePackage) {
        return ContentUris.withAppendedId(Voicemails.buildSourceUri(sourcePackage), id);
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
            contentValues.put(Voicemails.IS_READ, voicemail.isRead() ? 1 : 0);
        }
        return contentValues;
    }

    private void copyStreamData(InputStream in, OutputStream out) throws IOException {
        byte[] data = new byte[8 * 1024];
        int numBytes;
        while ((numBytes = in.read(data)) > 0) {
            out.write(data, 0, numBytes);
        }

    }
}
