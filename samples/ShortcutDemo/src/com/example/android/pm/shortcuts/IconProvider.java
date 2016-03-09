/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.example.android.pm.shortcuts;

import android.content.ContentProvider;
import android.content.ContentProvider.PipeDataWriter;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class IconProvider extends ContentProvider implements PipeDataWriter<InputStream> {
    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection,
            String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        return 0;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new IllegalArgumentException("Only read is supported");
        }
        final int resId = Integer.parseInt(uri.getPath().substring(1) /* to remove the slash */);

        // Try to open an asset with the given name.
        try {
            return new ParcelFileDescriptor(openPipeHelper(uri, null, null,
                    getContext().getResources().openRawResource(resId), this));
        } catch (IOException e) {
            FileNotFoundException fnf = new FileNotFoundException("Unable to open " + uri);
            throw fnf;
        }
    }

    @Override
    public void writeDataToPipe(ParcelFileDescriptor output, Uri uri, String mimeType,
            Bundle opts, InputStream args) {
        // Transfer data from the asset to the pipe the client is reading.
        byte[] buffer = new byte[8192];
        int n;
        try (FileOutputStream fout = new FileOutputStream(output.getFileDescriptor())) {
            while ((n=args.read(buffer)) >= 0) {
                fout.write(buffer, 0, n);
            }
        } catch (IOException e) {
            Log.i("IconProvider", "Failed transferring", e);
        } finally {
            try {
                args.close();
            } catch (IOException e) {
            }
        }
    }
}
