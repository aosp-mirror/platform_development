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

/**
 * @author nnk@google.com (Nick Kralevich)
 */
public class TagDBHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_CREATE = "create table Tags ("
            + "_id INTEGER PRIMARY KEY ASC, "
            + "description TEXT, "
            + "date TEXT"
            + ")";

    private static final String FAKE_DATA =
            "INSERT INTO Tags (description) values ('hello world')";

    private static final String FAKE_DATA2 =
            "INSERT INTO Tags (description) values ('hi world')";


    public TagDBHelper(Context context) {
        super(context, "Tags.db", null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
        db.execSQL(FAKE_DATA);
        db.execSQL(FAKE_DATA2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
