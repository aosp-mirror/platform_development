/*
* Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.common.assetprovider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;

import java.io.FileNotFoundException;
import java.io.IOException;

import static java.net.URLConnection.guessContentTypeFromName;

/**
 * Generic content provider, which makes any files available in this app's "assets" directory
 * available publicly.
 *
 * <p>To use, add the following to your AndroidManifest.xml:
 *
 * <code><pre>
 * <provider
 *   android:name=".AssetProvider"
 *   android:authorities="[YOUR CONTENT PROVIDER DOMAIN HERE]"
 *   android:exported="true"/>
 * </pre></code>
 */
public class AssetProvider extends ContentProvider {
    AssetManager mAssets;

    @Override
    public boolean onCreate() {
        Context ctx = getContext();
        if (ctx == null) {
            // Context not available. Give up.
            return false;
        }
        mAssets = ctx.getAssets();
        return true;
    }

    @Override
    public String getType(Uri uri){
        // Returns the MIME type for the selected URI, in conformance with the ContentProvider
        // interface. Looks up the file indicated by /res/assets/{uri.path}, and returns the MIME
        // type for that file as guessed by the URLConnection class.

        // Setup
        String path = uri.getPath();

        // Check if file exists
        if (!fileExists(path)) {
            return null;
        }

        // Determine MIME-type based on filename
        return guessContentTypeFromName(uri.toString());
    }


    @Override
    public AssetFileDescriptor openAssetFile (Uri uri, String mode)
            throws FileNotFoundException, SecurityException {
        // ContentProvider interface for opening a file descriptor by URI. This content provider
        // maps all URIs to the contents of the APK's assets folder, so a file handle to
        // /res/assets/{uri.path} will be returned.

        // Security check. This content provider only supports read-only access. (Also, the contents
        // of an APKs assets folder are immutable, so read-write access doesn't make sense here.)
        if (!"r".equals(mode)) {
            throw new SecurityException("Only read-only access is supported, mode must be [r]");
        }

        // Open asset from within APK and return file descriptor
        String path = uri.getPath();
        try {
            return mAssets.openFd(path);
        } catch (IOException e) {
            throw new FileNotFoundException();
        }
    }

    /**
     * Check if file exists inside APK assets.
     *
     * @param path Fully qualified path to file.
     * @return true if exists, false otherwise.
     */
    private boolean fileExists(String path) {
        try {
            // Check to see if file can be opened. If so, file exists.
            mAssets.openFd(path).close();
            return true;
        } catch (IOException e) {
            // Unable to open file descriptor for specified path; file doesn't exist.
            return false;
        }
    }

    // Required/unused ContentProvider methods below.
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        // Note: It might be worth implementing support for querying
        //       android.provider.OpenableColumns here in the future.
        throw new RuntimeException("Operation not supported");
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new RuntimeException("Operation not supported");
    }
}
