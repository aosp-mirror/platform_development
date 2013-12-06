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

package com.example.android.vault;

import static com.example.android.vault.VaultProvider.AUTHORITY;
import static com.example.android.vault.VaultProvider.DEFAULT_DOCUMENT_ID;

import android.content.ContentProviderClient;
import android.database.Cursor;
import android.provider.DocumentsContract.Document;
import android.test.AndroidTestCase;

import java.util.HashSet;

/**
 * Tests for {@link VaultProvider}.
 */
public class VaultProviderTest extends AndroidTestCase {

    private static final String MIME_TYPE_DEFAULT = "text/plain";

    private ContentProviderClient mClient;
    private VaultProvider mProvider;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mClient = getContext().getContentResolver().acquireContentProviderClient(AUTHORITY);
        mProvider = (VaultProvider) mClient.getLocalContentProvider();
        mProvider.wipeAllContents();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        mClient.release();
    }

    public void testDeleteDirectory() throws Exception {
        Cursor c;

        final String file = mProvider.createDocument(
                DEFAULT_DOCUMENT_ID, MIME_TYPE_DEFAULT, "file");
        final String dir = mProvider.createDocument(
                DEFAULT_DOCUMENT_ID, Document.MIME_TYPE_DIR, "dir");

        final String dirfile = mProvider.createDocument(
                dir, MIME_TYPE_DEFAULT, "dirfile");
        final String dirdir = mProvider.createDocument(
                dir, Document.MIME_TYPE_DIR, "dirdir");

        final String dirdirfile = mProvider.createDocument(
                dirdir, MIME_TYPE_DEFAULT, "dirdirfile");

        // verify everything is in place
        c = mProvider.queryChildDocuments(DEFAULT_DOCUMENT_ID, null, null);
        assertContains(c, "file", "dir");
        c = mProvider.queryChildDocuments(dir, null, null);
        assertContains(c, "dirfile", "dirdir");

        // should remove children and parent ref
        mProvider.deleteDocument(dir);

        c = mProvider.queryChildDocuments(DEFAULT_DOCUMENT_ID, null, null);
        assertContains(c, "file");

        mProvider.queryDocument(file, null);

        try { mProvider.queryDocument(dir, null); } catch (Exception expected) { }
        try { mProvider.queryDocument(dirfile, null); } catch (Exception expected) { }
        try { mProvider.queryDocument(dirdir, null); } catch (Exception expected) { }
        try { mProvider.queryDocument(dirdirfile, null); } catch (Exception expected) { }
    }

    private static void assertContains(Cursor c, String... docs) {
        final HashSet<String> set = new HashSet<String>();
        while (c.moveToNext()) {
            set.add(c.getString(c.getColumnIndex(Document.COLUMN_DISPLAY_NAME)));
        }

        for (String doc : docs) {
            assertTrue(doc, set.contains(doc));
        }
    }
}
