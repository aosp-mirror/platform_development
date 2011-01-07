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

package com.example.android.xmladapters;

import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.TextView;

import java.io.InputStream;
import java.util.HashMap;

/**
 * This custom cursor binder is used by the adapter defined in res/xml to
 * bind contacts photos to their respective list item. This binder simply
 * queries a contact's photo based on the contact's id and sets the
 * photo as a compound drawable on the TextView used to display the contact's
 * name.
 */
public class ContactPhotoBinder extends Adapters.CursorBinder {
    private static final int PHOTO_SIZE_DIP = 54;
    
    private final Drawable mDefault;
    private final HashMap<Long, Drawable> mCache;
    private final Resources mResources;
    private final int mPhotoSize;

    public ContactPhotoBinder(Context context, Adapters.CursorTransformation transformation) {
        super(context, transformation);

        mResources = mContext.getResources();
        // Default picture used when a contact does not provide one
        mDefault = mResources.getDrawable(R.drawable.ic_contact_picture);
        // Cache used to avoid re-querying contacts photos every time
        mCache = new HashMap<Long, Drawable>();
        // Compute the size of the photo based on the display's density
        mPhotoSize = (int) (PHOTO_SIZE_DIP * mResources.getDisplayMetrics().density + 0.5f);
    }

    @Override
    public boolean bind(View view, Cursor cursor, int columnIndex) {
        final long id = cursor.getLong(columnIndex);
        
        // First check whether we have already cached the contact's photo
        Drawable d = mCache.get(id);
        
        if (d == null) {
            // If the photo wasn't in the cache, ask the contacts provider for
            // an input stream we can use to load the photo
            Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
            InputStream stream = ContactsContract.Contacts.openContactPhotoInputStream(
                    mContext.getContentResolver(), uri);
    
            // Creates the drawable for the contact's photo or use our fallback drawable
            if (stream != null) {
                // decoding the bitmap could be done in a worker thread too.
                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                d = new BitmapDrawable(mResources, bitmap);
            } else {
                d = mDefault;
            }

            d.setBounds(0, 0, mPhotoSize, mPhotoSize);
            ((TextView) view).setCompoundDrawables(d, null, null, null);

            // Remember the photo associated with this contact
            mCache.put(id, d);
        } else {
            ((TextView) view).setCompoundDrawables(d, null, null, null);
        }

        return true;
    }
}
