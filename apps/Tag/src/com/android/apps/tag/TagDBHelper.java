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
import com.trustedlogic.trustednfc.android.NfcException;

import java.net.URI;
import java.util.Date;

/**
 * @author nnk@google.com (Nick Kralevich)
 */
public class TagDBHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;

    private static final String NDEF_MSG = "create table NdefMessage ("
            + "_id INTEGER NOT NULL, "
            + "bytes BLOB NOT NULL, "
            + "date TEXT NOT NULL, "
            + "PRIMARY KEY(_id)"
            + ")";

    private static final String INSERT =
            "INSERT INTO NdefMessage (bytes, date) values (?, ?)";

    private static final byte[] REAL_NFC_MSG = new byte[] {
            (byte) 0xd1,
            (byte) 0x02,
            (byte) 0x2b,
            (byte) 0x53,
            (byte) 0x70,
            (byte) 0x91,
            (byte) 0x01,
            (byte) 0x17,
            (byte) 0x54,
            (byte) 0x02,
            (byte) 0x65,
            (byte) 0x6e,
            (byte) 0x4e,
            (byte) 0x46,
            (byte) 0x43,
            (byte) 0x20,
            (byte) 0x46,
            (byte) 0x6f,
            (byte) 0x72,
            (byte) 0x75,
            (byte) 0x6d,
            (byte) 0x20,
            (byte) 0x54,
            (byte) 0x79,
            (byte) 0x70,
            (byte) 0x65,
            (byte) 0x20,
            (byte) 0x34,
            (byte) 0x20,
            (byte) 0x54,
            (byte) 0x61,
            (byte) 0x67,
            (byte) 0x51,
            (byte) 0x01,
            (byte) 0x0c,
            (byte) 0x55,
            (byte) 0x01,
            (byte) 0x6e,
            (byte) 0x78,
            (byte) 0x70,
            (byte) 0x2e,
            (byte) 0x63,
            (byte) 0x6f,
            (byte) 0x6d,
            (byte) 0x2f,
            (byte) 0x6e,
            (byte) 0x66,
            (byte) 0x63
    };

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

        try {
            // A real message obtained from an NFC Forum Type 4 tag.
            NdefMessage msg3 = new NdefMessage(REAL_NFC_MSG);
            insert(db, msg3);
        } catch (NfcException e) {
            throw new RuntimeException(e);
        }
    }

    private void insert(SQLiteDatabase db, NdefMessage msg) {
        SQLiteStatement stmt = db.compileStatement(INSERT);
        stmt.bindString(1, new String(msg.toByteArray())); // TODO: This should be a blob
        stmt.bindString(2, new Date().toString());
        stmt.executeInsert();
        stmt.close();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
