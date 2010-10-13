// Copyright 2010 Google Inc. All Rights Reserved.

package com.android.apps.tag;

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
public class TagCursorAdapter extends CursorAdapter {

    private final LayoutInflater mInflater;

    public TagCursorAdapter(Context context, Cursor c) {
        super(context, c);

        mInflater = LayoutInflater.from(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView mainLine = (TextView) view.findViewById(R.id.title);
        TextView dateLine = (TextView) view.findViewById(R.id.date);

        // TODO(benkomalo): either write a cursor abstraction, or use constants for column indices.
        mainLine.setText(cursor.getString(cursor.getColumnIndex("bytes")));
        dateLine.setText(DateUtils.getRelativeTimeSpanString(
                context, cursor.getLong(cursor.getColumnIndex("date"))));
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.tag_list_item, null);
    }
}
