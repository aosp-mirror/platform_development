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
 * limitations under the License
 */

package com.android.apps.tag;

import com.android.apps.tag.TagDBHelper.NdefMessagesTable;

import android.app.IntentService;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.nfc.NdefMessage;
import android.os.Parcelable;

public class TagService extends IntentService {
    public static final String EXTRA_SAVE_MSGS = "msgs";
    public static final String EXTRA_DELETE_ID = "delete";

    public TagService() {
        super("SaveTagService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        TagDBHelper helper = TagDBHelper.getInstance(this);
        SQLiteDatabase db = helper.getWritableDatabase();
        if (intent.hasExtra(EXTRA_SAVE_MSGS)) {
            Parcelable[] parcels = intent.getParcelableArrayExtra(EXTRA_SAVE_MSGS);
            db.beginTransaction();
            try {
                for (Parcelable parcel : parcels) {
                    helper.insertNdefMessage(db, (NdefMessage) parcel, false);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            return;
        } else if (intent.hasExtra(EXTRA_DELETE_ID)) {
            long id = intent.getLongExtra(EXTRA_DELETE_ID, 0);
            db.delete(NdefMessagesTable.TABLE_NAME, NdefMessagesTable._ID + "=?",
                    new String[] { Long.toString(id) });
            return;
        }
    }
}
