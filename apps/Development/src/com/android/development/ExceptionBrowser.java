/*
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License"); 
** you may not use this file except in compliance with the License. 
** You may obtain a copy of the License at 
**
**     http://www.apache.org/licenses/LICENSE-2.0 
**
** Unless required by applicable law or agreed to in writing, software 
** distributed under the License is distributed on an "AS IS" BASIS, 
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
** See the License for the specific language governing permissions and 
** limitations under the License.
*/

package com.android.development;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.ContentObserver;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Checkin;
import android.server.data.CrashData;
import android.server.data.ThrowableData;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.widget.*;

import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 *
 *
 */
public class ExceptionBrowser extends ListActivity {
    /** Logging identifier. */
    private static final String TAG = "ExceptionBrowser";

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Cursor cursor = getContentResolver().query(
                Checkin.Crashes.CONTENT_URI,
                new String[] { Checkin.Crashes._ID, Checkin.Crashes.DATA },
                null, null, null);

        if (cursor != null) {
            startManagingCursor(cursor);

            setListAdapter(new CursorAdapter(this, cursor, true) {
                public View newView(Context context, Cursor c, ViewGroup v) {
                    return new CrashListItem(context);
                }

                public void bindView(View view, Context c, Cursor cursor) {
                    CrashListItem item = (CrashListItem) view;
                    try {
                        String data = cursor.getString(1);
                        CrashData crash = new CrashData(
                            new DataInputStream(
                                new ByteArrayInputStream(
                                    Base64.decodeBase64(data.getBytes()))));

                        ThrowableData exc = crash.getThrowableData();
                        item.setText(exc.getType() + ": " + exc.getMessage());
                        item.setCrashData(crash);
                    } catch (IOException e) {
                        item.setText("Invalid crash: " + e);
                        Log.e(TAG, "Invalid crash", e);
                    }
                }
            });
        } else {
            // No database, no exceptions, empty list.
            setListAdapter(new BaseAdapter() {
                public int getCount() {
                    return 0;
                }

                public Object getItem(int position) {
                    throw new AssertionError();
                }

                public long getItemId(int position) {
                    throw new AssertionError();
                }

                public View getView(int position, View convertView,
                        ViewGroup parent) {
                    throw new AssertionError();
                }
            });
        }
    }

    private static final int UPLOAD_ID = Menu.FIRST;
    private static final int CLEAR_ID = Menu.FIRST + 1;

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, UPLOAD_ID, 0, R.string.menu_upload_exceptions);
        menu.add(0, CLEAR_ID, 0, R.string.menu_clear_exceptions);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        switch (item.getItemId()) {
            case UPLOAD_ID:
                sendBroadcast(new Intent(Checkin.TriggerIntent.ACTION));
                break;
            case CLEAR_ID:
                getContentResolver().delete(
                        Checkin.Crashes.CONTENT_URI, null, null);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    static class CrashListItem extends TextView {
        CrashData crashData = null;

        public CrashListItem(Context context) {
            super(context);
            setTextSize(10);
            setTypeface(Typeface.MONOSPACE);
        }

        public CrashData getCrashData() {
            return crashData;
        }

        public void setCrashData(CrashData crashData) {
            this.crashData = crashData;
        }
    }

    @Override
    protected void onListItemClick(ListView l, View view, int pos, long id) {
        // TODO: Use a generic VIEW action on the crash's content URI.
        CrashData crash = ((CrashListItem) view).getCrashData();
        if (crash != null) {
            Intent intent = new Intent();
            intent.setClass(this, StacktraceViewer.class);
            intent.putExtra(
                    CrashData.class.getName(),
                    crash.getThrowableData().toString());
            startActivity(intent);
        }
    }
}
