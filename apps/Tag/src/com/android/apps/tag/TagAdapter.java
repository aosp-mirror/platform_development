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

import com.android.apps.tag.TagDBHelper.NdefMessagesTable;

import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.CursorAdapter;
import android.widget.TextView;

/**
 * A custom {@link Adapter} that renders tag entries for a list.
 */
public class TagAdapter extends CursorAdapter {

    private final LayoutInflater mInflater;

    public TagAdapter(Context context) {
        super(context, null, false);
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView mainLine = (TextView) view.findViewById(R.id.title);
        TextView dateLine = (TextView) view.findViewById(R.id.date);

        mainLine.setText(cursor.getString(cursor.getColumnIndex(NdefMessagesTable.TITLE)));
        dateLine.setText(DateUtils.getRelativeTimeSpanString(
                context, cursor.getLong(cursor.getColumnIndex(NdefMessagesTable.DATE))));
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.tag_list_item, null);
    }
}
