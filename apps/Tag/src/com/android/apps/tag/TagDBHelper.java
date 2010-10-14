/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.apps.tag;

import com.google.common.annotations.VisibleForTesting;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;

/**
 * Database utilities for the saved tags.
 */
public class TagDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "tags.db";
    private static final int DATABASE_VERSION = 3;

    public interface NdefMessagesTable {
        public static final String TABLE_NAME = "nedf_msg";

        public static final String _ID = "_id";
        public static final String TITLE = "title";
        public static final String BYTES = "bytes";
        public static final String DATE = "date";
        public static final String SAVED = "saved";
    }

    private static TagDBHelper sInstance;

    public static synchronized TagDBHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new TagDBHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private TagDBHelper(Context context) {
        this(context, DATABASE_NAME);
    }

    @VisibleForTesting
    TagDBHelper(Context context, String dbFile) {
        super(context, dbFile, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + NdefMessagesTable.TABLE_NAME + " (" +
                NdefMessagesTable._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                NdefMessagesTable.TITLE + " TEXT NOT NULL DEFAULT ''," +
                NdefMessagesTable.BYTES + " BLOB NOT NULL, " +
                NdefMessagesTable.DATE + " INTEGER NOT NULL, " +
                NdefMessagesTable.SAVED + " INTEGER NOT NULL DEFAULT 0" +  // boolean
                ");");

        db.execSQL("CREATE INDEX msgIndex ON " + NdefMessagesTable.TABLE_NAME + " (" +
                NdefMessagesTable.DATE + " DESC, " +
                NdefMessagesTable.SAVED + " ASC" +
                ")");

        addTestData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop everything and recreate it for now
        db.execSQL("DROP TABLE IF EXISTS " + NdefMessagesTable.TABLE_NAME);
        onCreate(db);
    }

    private void addTestData(SQLiteDatabase db) {
        // A fake message containing 1 URL
        NdefMessage msg1 = new NdefMessage(new NdefRecord[] {
                NdefUtil.toUriRecord(Uri.parse("http://www.google.com"))
        });

        // A fake message containing 2 URLs
        NdefMessage msg2 = new NdefMessage(new NdefRecord[] {
                NdefUtil.toUriRecord(Uri.parse("http://www.youtube.com")),
                NdefUtil.toUriRecord(Uri.parse("http://www.android.com"))
        });

        insertNdefMessage(db, msg1, false);
        insertNdefMessage(db, msg2, true);

        try {
            // insert some real messages we found in the field.
            for (byte[] msg : MockNdefMessages.ALL_MOCK_MESSAGES) {
                NdefMessage msg3 = new NdefMessage(msg);
                insertNdefMessage(db, msg3, false);
            }
        } catch (FormatException e) {
            throw new RuntimeException(e);
        }
    }

    public void insertNdefMessage(SQLiteDatabase db, NdefMessage msg, boolean isSaved) {
        SQLiteStatement stmt = null;
        try {
            stmt = db.compileStatement("INSERT INTO " + NdefMessagesTable.TABLE_NAME +
                    "(" + NdefMessagesTable.BYTES + ", " + NdefMessagesTable.DATE + ", " +
                    NdefMessagesTable.SAVED + ") values (?, ?, ?)");
            stmt.bindBlob(1, msg.toByteArray());
            stmt.bindLong(2, System.currentTimeMillis());
            stmt.bindLong(3, isSaved ? 1 : 0);
            stmt.executeInsert();
        } finally {
            if (stmt != null) stmt.close();
        }
    }
}
