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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import com.google.common.annotations.VisibleForTesting;
import com.trustedlogic.trustednfc.android.NdefMessage;
import com.trustedlogic.trustednfc.android.NdefRecord;

import java.net.URI;
import java.util.Date;

/**
 * @author nnk@google.com (Nick Kralevich)
 */
public class TagDBHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;

    private static final String NDEF_MSG = "create table NdefMessage ("
            + "_id INTEGER NOT NULL, "
            + "bytes TEXT NOT NULL, "  // TODO: This should be a blob
            + "date TEXT NOT NULL, "
            + "PRIMARY KEY(_id)"
            + ")";

    private static final String INSERT =
            "INSERT INTO NdefMessage (bytes, date) values (?, ?)";


    public TagDBHelper(Context context) {
        this(context, "Tags.db");
    }

    @VisibleForTesting
    public TagDBHelper(Context context, String dbFile) {
        super(context, dbFile, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(NDEF_MSG);

        // A fake message containing 1 URL
        NdefMessage msg1 = new NdefMessage(new NdefRecord[] {
                NdefUtil.toUriRecord(URI.create("http://www.google.com"))
        });

        // A fake message containing 2 URLs
        NdefMessage msg2 = new NdefMessage(new NdefRecord[] {
                NdefUtil.toUriRecord(URI.create("http://www.youtube.com")),
                NdefUtil.toUriRecord(URI.create("http://www.android.com"))
        });

        insert(db, msg1);
        insert(db, msg2);
    }

    private void insert(SQLiteDatabase db, NdefMessage msg) {
        SQLiteStatement stmt = db.compileStatement(INSERT);
        stmt.bindString(1, new String(msg.toByteArray())); // TODO: This should be a blob
        stmt.bindString(2, new Date().toString());
        stmt.executeInsert();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
