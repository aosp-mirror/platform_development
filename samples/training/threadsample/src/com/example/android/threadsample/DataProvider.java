/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.example.android.threadsample;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;

/**
 *
 * Defines a ContentProvider that stores URLs of Picasa featured pictures
 * The provider also has a table that tracks the last time a picture URL was updated.
 */
public class DataProvider extends ContentProvider {
    // Indicates that the incoming query is for a picture URL
    public static final int IMAGE_URL_QUERY = 1;

    // Indicates that the incoming query is for a URL modification date
    public static final int URL_DATE_QUERY = 2;

    // Indicates an invalid content URI
    public static final int INVALID_URI = -1;

    // Constants for building SQLite tables during initialization
    private static final String TEXT_TYPE = "TEXT";
    private static final String PRIMARY_KEY_TYPE = "INTEGER PRIMARY KEY";
    private static final String INTEGER_TYPE = "INTEGER";

    // Defines an SQLite statement that builds the Picasa picture URL table
    private static final String CREATE_PICTUREURL_TABLE_SQL = "CREATE TABLE" + " " +
            DataProviderContract.PICTUREURL_TABLE_NAME + " " +
            "(" + " " +
            DataProviderContract.ROW_ID + " " + PRIMARY_KEY_TYPE + " ," +
            DataProviderContract.IMAGE_THUMBURL_COLUMN + " " + TEXT_TYPE + " ," +
            DataProviderContract.IMAGE_URL_COLUMN + " " + TEXT_TYPE + " ," +
            DataProviderContract.IMAGE_THUMBNAME_COLUMN + " " + TEXT_TYPE + " ," +
            DataProviderContract.IMAGE_PICTURENAME_COLUMN + " " + TEXT_TYPE +
            ")";

    // Defines an SQLite statement that builds the URL modification date table
    private static final String CREATE_DATE_TABLE_SQL = "CREATE TABLE" + " " +
            DataProviderContract.DATE_TABLE_NAME + " " +
            "(" + " " +
            DataProviderContract.ROW_ID + " " + PRIMARY_KEY_TYPE + " ," +
            DataProviderContract.DATA_DATE_COLUMN + " " + INTEGER_TYPE +
            ")";

    // Identifies log statements issued by this component
    public static final String LOG_TAG = "DataProvider";

    // Defines an helper object for the backing database
    private SQLiteOpenHelper mHelper;

    // Defines a helper object that matches content URIs to table-specific parameters
    private static final UriMatcher sUriMatcher;

    // Stores the MIME types served by this provider
    private static final SparseArray<String> sMimeTypes;

    /*
     * Initializes meta-data used by the content provider:
     * - UriMatcher that maps content URIs to codes
     * - MimeType array that returns the custom MIME type of a table
     */
    static {
        
        // Creates an object that associates content URIs with numeric codes
        sUriMatcher = new UriMatcher(0);

        /*
         * Sets up an array that maps content URIs to MIME types, via a mapping between the
         * URIs and an integer code. These are custom MIME types that apply to tables and rows
         * in this particular provider.
         */
        sMimeTypes = new SparseArray<String>();

        // Adds a URI "match" entry that maps picture URL content URIs to a numeric code
        sUriMatcher.addURI(
                DataProviderContract.AUTHORITY,
                DataProviderContract.PICTUREURL_TABLE_NAME,
                IMAGE_URL_QUERY);

        // Adds a URI "match" entry that maps modification date content URIs to a numeric code
        sUriMatcher.addURI(
            DataProviderContract.AUTHORITY,
            DataProviderContract.DATE_TABLE_NAME,
            URL_DATE_QUERY);
        
        // Specifies a custom MIME type for the picture URL table
        sMimeTypes.put(
                IMAGE_URL_QUERY,
                "vnd.android.cursor.dir/vnd." +
                DataProviderContract.AUTHORITY + "." +
                DataProviderContract.PICTUREURL_TABLE_NAME);

        // Specifies the custom MIME type for a single modification date row
        sMimeTypes.put(
                URL_DATE_QUERY,
                "vnd.android.cursor.item/vnd."+
                DataProviderContract.AUTHORITY + "." +
                DataProviderContract.DATE_TABLE_NAME);
    }

    // Closes the SQLite database helper class, to avoid memory leaks
    public void close() {
        mHelper.close();
    }
    
    /**
     * Defines a helper class that opens the SQLite database for this provider when a request is
     * received. If the database doesn't yet exist, the helper creates it.
     */
    private class DataProviderHelper extends SQLiteOpenHelper {
        /**
         * Instantiates a new SQLite database using the supplied database name and version
         *
         * @param context The current context
         */
        DataProviderHelper(Context context) {
            super(context,
                    DataProviderContract.DATABASE_NAME,
                    null,
                    DataProviderContract.DATABASE_VERSION);
        }


        /**
         * Executes the queries to drop all of the tables from the database.
         *
         * @param db A handle to the provider's backing database.
         */
        private void dropTables(SQLiteDatabase db) {

            // If the table doesn't exist, don't throw an error
            db.execSQL("DROP TABLE IF EXISTS " + DataProviderContract.PICTUREURL_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + DataProviderContract.DATE_TABLE_NAME);
        }

        /**
         * Does setup of the database. The system automatically invokes this method when
         * SQLiteDatabase.getWriteableDatabase() or SQLiteDatabase.getReadableDatabase() are
         * invoked and no db instance is available.
         *
         * @param db the database instance in which to create the tables.
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            // Creates the tables in the backing database for this provider
            db.execSQL(CREATE_PICTUREURL_TABLE_SQL);
            db.execSQL(CREATE_DATE_TABLE_SQL);

        }

        /**
         * Handles upgrading the database from a previous version. Drops the old tables and creates
         * new ones.
         *
         * @param db The database to upgrade
         * @param version1 The old database version
         * @param version2 The new database version
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int version1, int version2) {
            Log.w(DataProviderHelper.class.getName(),
                    "Upgrading database from version " + version1 + " to "
                            + version2 + ", which will destroy all the existing data");

            // Drops all the existing tables in the database
            dropTables(db);

            // Invokes the onCreate callback to build new tables
            onCreate(db);
        }
        /**
         * Handles downgrading the database from a new to a previous version. Drops the old tables
         * and creates new ones.
         * @param db The database object to downgrade
         * @param version1 The old database version
         * @param version2 The new database version
         */
        @Override
        public void onDowngrade(SQLiteDatabase db, int version1, int version2) {
            Log.w(DataProviderHelper.class.getName(),
                "Downgrading database from version " + version1 + " to "
                        + version2 + ", which will destroy all the existing data");
    
            // Drops all the existing tables in the database
            dropTables(db);
    
            // Invokes the onCreate callback to build new tables
            onCreate(db);
            
        }
    }
    /**
     * Initializes the content provider. Notice that this method simply creates a
     * the SQLiteOpenHelper instance and returns. You should do most of the initialization of a
     * content provider in its static initialization block or in SQLiteDatabase.onCreate().
     */
    @Override
    public boolean onCreate() {

        // Creates a new database helper object
        mHelper = new DataProviderHelper(getContext());

        return true;
    }
    /**
     * Returns the result of querying the chosen table.
     * @see android.content.ContentProvider#query(Uri, String[], String, String[], String)
     * @param uri The content URI of the table
     * @param projection The names of the columns to return in the cursor
     * @param selection The selection clause for the query
     * @param selectionArgs An array of Strings containing search criteria
     * @param sortOrder A clause defining the order in which the retrieved rows should be sorted
     * @return The query results, as a {@link android.database.Cursor} of rows and columns
     */
    @Override
    public Cursor query(
        Uri uri,
        String[] projection,
        String selection,
        String[] selectionArgs,
        String sortOrder) {

        SQLiteDatabase db = mHelper.getReadableDatabase();
        // Decodes the content URI and maps it to a code
        switch (sUriMatcher.match(uri)) {

            // If the query is for a picture URL
            case IMAGE_URL_QUERY:
                // Does the query against a read-only version of the database
                Cursor returnCursor = db.query(
                    DataProviderContract.PICTUREURL_TABLE_NAME,
                    projection,
                    null, null, null, null, null);

                // Sets the ContentResolver to watch this content URI for data changes
                returnCursor.setNotificationUri(getContext().getContentResolver(), uri);
                return returnCursor;

            // If the query is for a modification date URL
            case URL_DATE_QUERY:
                returnCursor = db.query(
                    DataProviderContract.DATE_TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder);

                // No notification Uri is set, because the data doesn't have to be watched.
                return returnCursor;

            case INVALID_URI:

                throw new IllegalArgumentException("Query -- Invalid URI:" + uri);
        }

        return null;
    }

    /**
     * Returns the mimeType associated with the Uri (query).
     * @see android.content.ContentProvider#getType(Uri)
     * @param uri the content URI to be checked
     * @return the corresponding MIMEtype
     */
    @Override
    public String getType(Uri uri) {

        return sMimeTypes.get(sUriMatcher.match(uri));
    }
    /**
     *
     * Insert a single row into a table
     * @see android.content.ContentProvider#insert(Uri, ContentValues)
     * @param uri the content URI of the table
     * @param values a {@link android.content.ContentValues} object containing the row to insert
     * @return the content URI of the new row
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {

        // Decode the URI to choose which action to take
        switch (sUriMatcher.match(uri)) {

            // For the modification date table
            case URL_DATE_QUERY:

                // Creates a writeable database or gets one from cache
                SQLiteDatabase localSQLiteDatabase = mHelper.getWritableDatabase();

                // Inserts the row into the table and returns the new row's _id value
                long id = localSQLiteDatabase.insert(
                        DataProviderContract.DATE_TABLE_NAME,
                        DataProviderContract.DATA_DATE_COLUMN,
                        values
                );

                // If the insert succeeded, notify a change and return the new row's content URI.
                if (-1 != id) {
                    getContext().getContentResolver().notifyChange(uri, null);
                    return Uri.withAppendedPath(uri, Long.toString(id));
                } else {

                    throw new SQLiteException("Insert error:" + uri);
                }
            case IMAGE_URL_QUERY:

                throw new IllegalArgumentException("Insert: Invalid URI" + uri);
        }

        return null;
    }
    /**
     * Implements bulk row insertion using
     * {@link SQLiteDatabase#insert(String, String, ContentValues) SQLiteDatabase.insert()}
     * and SQLite transactions. The method also notifies the current
     * {@link android.content.ContentResolver} that the {@link android.content.ContentProvider} has
     * been changed.
     * @see android.content.ContentProvider#bulkInsert(Uri, ContentValues[])
     * @param uri The content URI for the insertion
     * @param insertValuesArray A {@link android.content.ContentValues} array containing the row to
     * insert
     * @return The number of rows inserted.
     */
    @Override
    public int bulkInsert(Uri uri, ContentValues[] insertValuesArray) {

        // Decodes the content URI and choose which insert to use
        switch (sUriMatcher.match(uri)) {

            // picture URLs table
            case IMAGE_URL_QUERY:

                // Gets a writeable database instance if one is not already cached
                SQLiteDatabase localSQLiteDatabase = mHelper.getWritableDatabase();

                /*
                 * Begins a transaction in "exclusive" mode. No other mutations can occur on the
                 * db until this transaction finishes.
                 */
                localSQLiteDatabase.beginTransaction();

                // Deletes all the existing rows in the table
                localSQLiteDatabase.delete(DataProviderContract.PICTUREURL_TABLE_NAME, null, null);

                // Gets the size of the bulk insert
                int numImages = insertValuesArray.length;

                // Inserts each ContentValues entry in the array as a row in the database
                for (int i = 0; i < numImages; i++) {

                    localSQLiteDatabase.insert(DataProviderContract.PICTUREURL_TABLE_NAME,
                            DataProviderContract.IMAGE_URL_COLUMN, insertValuesArray[i]);
                }

                // Reports that the transaction was successful and should not be backed out.
                localSQLiteDatabase.setTransactionSuccessful();

                // Ends the transaction and closes the current db instances
                localSQLiteDatabase.endTransaction();
                localSQLiteDatabase.close();

                /*
                 * Notifies the current ContentResolver that the data associated with "uri" has
                 * changed.
                 */

                getContext().getContentResolver().notifyChange(uri, null);

                // The semantics of bulkInsert is to return the number of rows inserted.
                return numImages;

            // modification date table
            case URL_DATE_QUERY:

                // Do inserts by calling SQLiteDatabase.insert on each row in insertValuesArray
                return super.bulkInsert(uri, insertValuesArray);

            case INVALID_URI:

                // An invalid URI was passed. Throw an exception
                throw new IllegalArgumentException("Bulk insert -- Invalid URI:" + uri);

        }

        return -1;

    }
    /**
     * Returns an UnsupportedOperationException if delete is called
     * @see android.content.ContentProvider#delete(Uri, String, String[])
     * @param uri The content URI
     * @param selection The SQL WHERE string. Use "?" to mark places that should be substituted by
     * values in selectionArgs.
     * @param selectionArgs An array of values that are mapped to each "?" in selection. If no "?"
     * are used, set this to NULL.
     *
     * @return the number of rows deleted
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        throw new UnsupportedOperationException("Delete -- unsupported operation " + uri);
    }

    /**
     * Updates one or more rows in a table.
     * @see android.content.ContentProvider#update(Uri, ContentValues, String, String[])
     * @param uri The content URI for the table
     * @param values The values to use to update the row or rows. You only need to specify column
     * names for the columns you want to change. To clear the contents of a column, specify the
     * column name and NULL for its value.
     * @param selection An SQL WHERE clause (without the WHERE keyword) specifying the rows to
     * update. Use "?" to mark places that should be substituted by values in selectionArgs.
     * @param selectionArgs An array of values that are mapped in order to each "?" in selection.
     * If no "?" are used, set this to NULL.
     *
     * @return int The number of rows updated.
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {

        // Decodes the content URI and choose which insert to use
        switch (sUriMatcher.match(uri)) {

            // A picture URL content URI
            case URL_DATE_QUERY:

                // Creats a new writeable database or retrieves a cached one
                SQLiteDatabase localSQLiteDatabase = mHelper.getWritableDatabase();

                // Updates the table
                int rows = localSQLiteDatabase.update(
                        DataProviderContract.DATE_TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);

                // If the update succeeded, notify a change and return the number of updated rows.
                if (0 != rows) {
                    getContext().getContentResolver().notifyChange(uri, null);
                    return rows;
                } else {

                    throw new SQLiteException("Update error:" + uri);
                }

            case IMAGE_URL_QUERY:

                throw new IllegalArgumentException("Update: Invalid URI: " + uri);
        }

        return -1;
    }
}
