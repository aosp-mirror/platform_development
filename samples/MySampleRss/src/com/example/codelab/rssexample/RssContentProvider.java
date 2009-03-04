/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.example.codelab.rssexample;

import android.content.ContentProvider;
import android.content.ContentProviderDatabaseHelper;
import android.content.UriMatcher;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.content.ContentValues;
import android.text.TextUtils;

import java.util.logging.Logger;

// Content Provider for RSS feed information. Each row describes a single
// RSS feed. See the public static constants at the end of this class
// to learn what each record contains.
public class RssContentProvider extends ContentProvider {
    private Logger mLogger = Logger.getLogger("com.example.codelab.rssexample");
    private SQLiteDatabase mDb;
    private DatabaseHelper mDbHelper = new DatabaseHelper();
    private static final String DATABASE_NAME = "rssitems.db";
    private static final String DATABASE_TABLE_NAME = "rssItems";
    private static final int DB_VERSION = 1;
    private static final int ALL_MESSAGES = 1;
    private static final int SPECIFIC_MESSAGE = 2;

    // Set up our URL matchers to help us determine what an
    // incoming URI parameter is.
    private static final UriMatcher URI_MATCHER;
    static{
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI("my_rss_item", "rssitem", ALL_MESSAGES);
        URI_MATCHER.addURI("my_rss_item", "rssitem/#", SPECIFIC_MESSAGE);
    }

    // Here's the public URI used to query for RSS items.
    public static final Uri CONTENT_URI = Uri.parse( "content://my_rss_item/rssitem");

    // Here are our column name constants, used to query for field values.
    public static final String ID = "_id";
    public static final String URL = "url";
    public static final String TITLE = "title";
    public static final String HAS_BEEN_READ = "hasbeenread";
    public static final String CONTENT = "rawcontent";
    public static final String LAST_UPDATED = "lastupdated";
    public static final String DEFAULT_SORT_ORDER = TITLE + " DESC";

    // Database creation/version management helper.
    // Create it statically because we don't need to have customized instances.
    private static class DatabaseHelper extends ContentProviderDatabaseHelper{

        @Override
        public void onCreate(SQLiteDatabase db){
            try{
                String sql = "CREATE TABLE " + DATABASE_TABLE_NAME + "(" +
                ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                URL + " TEXT," +
                TITLE + " TEXT," +
                HAS_BEEN_READ + " BOOLEAN DEFAULT 0," +
                CONTENT + " TEXT," +
                LAST_UPDATED + " INTEGER DEFAULT 0);";
                Logger.getLogger("com.example.codelab.rssexample").info("DatabaseHelper.onCreate(): SQL statement: " + sql);
                db.execSQL(sql);
                Logger.getLogger("com.example.codelab.rssexample").info("DatabaseHelper.onCreate(): Created a database");
            } catch (SQLException e) {
                Logger.getLogger("com.example.codelab.rssexample").warning("DatabaseHelper.onCreate(): Couldn't create a database!");
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
            // Don't have any upgrades yet, so if this gets called for some reason we'll
            // just drop the existing table, and recreate the database with the
            // standard method.
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_NAME + ";");

        }
    }

    @Override
    public boolean onCreate() {
        // First we need to open the database. If this is our first time,
        // the attempt to retrieve a database will throw
        // FileNotFoundException, and we will then create the database.
        final Context con = getContext();
        try{
            mDb = mDbHelper.openDatabase(getContext(), DATABASE_NAME, null, DB_VERSION);
            mLogger.info("RssContentProvider.onCreate(): Opened a database");
        } catch (Exception ex) {
              return false;
        }
        if(mDb == null){
            return false;
        } else {
            return true;
        }
    }

    // Convert the URI into a custom MIME type.
    // Our UriMatcher will parse the URI to decide whether the
    // URI is for a single item or a list.
    @Override
    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)){
            case ALL_MESSAGES:
                return "vnd.android.cursor.dir/rssitem"; // List of items.
            case SPECIFIC_MESSAGE:
                return "vnd.android.cursor.item/rssitem";     // Specific item.
            default:
                return null;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String groupBy, String having, String sortOrder) {
        // We won't bother checking the validity of params here, but you should!

        // SQLiteQueryBuilder is the helper class that creates the
        // proper SQL syntax for us.
        SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();

        // Set the table we're querying.
        qBuilder.setTables(DATABASE_TABLE_NAME);

        // If the query ends in a specific record number, we're
        // being asked for a specific record, so set the
        // WHERE clause in our query.
        if((URI_MATCHER.match(uri)) == SPECIFIC_MESSAGE){
            qBuilder.appendWhere("_id=" + uri.getPathLeafId());
        }

        // Set sort order. If none specified, use default.
        if(TextUtils.isEmpty(sortOrder)){
            sortOrder = DEFAULT_SORT_ORDER;
        }

        // Make the query.
        Cursor c = qBuilder.query(mDb,
                projection,
                selection,
                selectionArgs,
                groupBy,
                having,
                sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String whereClause) {
        // NOTE Argument checking code omitted. Check your parameters!
        int updateCount = mDb.update(DATABASE_TABLE_NAME, values, whereClause);

        // Notify any listeners and return the updated row count.
        getContext().getContentResolver().notifyUpdate(uri, null);
        return updateCount;
    }

    @Override
    public Uri insert(Uri requestUri, ContentValues initialValues) {
        // NOTE Argument checking code omitted. Check your parameters! Check that
        // your row addition request succeeded!

       long rowId = -1;
       rowId = mDb.insert(DATABASE_TABLE_NAME, "rawcontent", initialValues);
       Uri newUri = CONTENT_URI.addId(rowId);

       // Notify any listeners and return the URI of the new row.
       getContext().getContentResolver().notifyInsert(CONTENT_URI, null);
       return newUri;
    }

    @Override
    public int delete(Uri uri, String where) {
        // NOTE Argument checking code omitted. Check your parameters!
        int rowCount = mDb.delete(DATABASE_TABLE_NAME, ID + " = " + uri.getPathLeafId());

        // Notify any listeners and return the deleted row count.
        getContext().getContentResolver().notifyDelete(uri, null);
        return rowCount;
    }
}
