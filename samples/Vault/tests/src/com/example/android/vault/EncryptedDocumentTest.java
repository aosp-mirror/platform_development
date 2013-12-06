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

import android.os.ParcelFileDescriptor;
import android.test.AndroidTestCase;
import android.test.MoreAsserts;
import android.test.suitebuilder.annotation.MediumTest;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Tests for {@link EncryptedDocument}.
 */
@MediumTest
public class EncryptedDocumentTest extends AndroidTestCase {

    private File mFile;

    private SecretKey mDataKey = new SecretKeySpec(new byte[] {
            0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
            0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01 }, "AES");

    private SecretKey mMacKey = new SecretKeySpec(new byte[] {
            0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
            0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
            0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
            0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02 }, "AES");

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mFile = new File(getContext().getFilesDir(), "meow");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        for (File f : getContext().getFilesDir().listFiles()) {
            f.delete();
        }
    }

    public void testEmptyFile() throws Exception {
        mFile.createNewFile();
        final EncryptedDocument doc = new EncryptedDocument(4, mFile, mDataKey, mMacKey);

        try {
            doc.readMetadata();
            fail("expected metadata to throw");
        } catch (IOException expected) {
        }

        try {
            final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createReliablePipe();
            doc.readContent(pipe[1]);
            fail("expected content to throw");
        } catch (IOException expected) {
        }
    }

    public void testNormalMetadataAndContents() throws Exception {
        final byte[] content = "KITTENS".getBytes(StandardCharsets.UTF_8);
        testMetadataAndContents(content);
    }

    public void testGiantMetadataAndContents() throws Exception {
        // try with content size of prime number >1MB
        final byte[] content = new byte[1298047];
        Arrays.fill(content, (byte) 0x42);
        testMetadataAndContents(content);
    }

    private void testMetadataAndContents(byte[] content) throws Exception {
        final EncryptedDocument doc = new EncryptedDocument(4, mFile, mDataKey, mMacKey);
        final byte[] beforeContent = content;

        final ParcelFileDescriptor[] beforePipe = ParcelFileDescriptor.createReliablePipe();
        new Thread() {
            @Override
            public void run() {
                final FileOutputStream os = new FileOutputStream(beforePipe[1].getFileDescriptor());
                try {
                    os.write(beforeContent);
                    beforePipe[1].close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();

        // fully write metadata and content
        final JSONObject before = new JSONObject();
        before.put("meow", "cake");
        doc.writeMetadataAndContent(before, beforePipe[0]);

        // now go back and verify we can read
        final JSONObject after = doc.readMetadata();
        assertEquals("cake", after.getString("meow"));

        final CountDownLatch latch = new CountDownLatch(1);
        final ParcelFileDescriptor[] afterPipe = ParcelFileDescriptor.createReliablePipe();
        final byte[] afterContent = new byte[beforeContent.length];
        new Thread() {
            @Override
            public void run() {
                final FileInputStream is = new FileInputStream(afterPipe[0].getFileDescriptor());
                try {
                    int i = 0;
                    while (i < afterContent.length) {
                        int n = is.read(afterContent, i, afterContent.length - i);
                        i += n;
                    }
                    afterPipe[0].close();
                    latch.countDown();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();

        doc.readContent(afterPipe[1]);
        latch.await(5, TimeUnit.SECONDS);

        MoreAsserts.assertEquals(beforeContent, afterContent);
    }

    public void testNormalMetadataOnly() throws Exception {
        final EncryptedDocument doc = new EncryptedDocument(4, mFile, mDataKey, mMacKey);

        // write only metadata
        final JSONObject before = new JSONObject();
        before.put("lol", "wut");
        doc.writeMetadataAndContent(before, null);

        // verify we can read
        final JSONObject after = doc.readMetadata();
        assertEquals("wut", after.getString("lol"));

        final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createReliablePipe();
        try {
            doc.readContent(pipe[1]);
            fail("found document content");
        } catch (IOException expected) {
        }
    }

    public void testCopiedFile() throws Exception {
        final EncryptedDocument doc1 = new EncryptedDocument(1, mFile, mDataKey, mMacKey);
        final EncryptedDocument doc4 = new EncryptedDocument(4, mFile, mDataKey, mMacKey);

        // write values for doc1 into file
        final JSONObject meta1 = new JSONObject();
        meta1.put("key1", "value1");
        doc1.writeMetadataAndContent(meta1, null);

        // now try reading as doc4, which should fail
        try {
            doc4.readMetadata();
            fail("somehow read without checking docid");
        } catch (DigestException expected) {
        }
    }

    public void testBitTwiddle() throws Exception {
        final EncryptedDocument doc = new EncryptedDocument(4, mFile, mDataKey, mMacKey);

        // write some metadata
        final JSONObject before = new JSONObject();
        before.put("twiddle", "twiddle");
        doc.writeMetadataAndContent(before, null);

        final RandomAccessFile f = new RandomAccessFile(mFile, "rw");
        f.seek(f.length() - 4);
        f.write(0x00);
        f.close();

        try {
            doc.readMetadata();
            fail("somehow passed hmac");
        } catch (DigestException expected) {
        }
    }

    public void testErrorAbortsWrite() throws Exception {
        final EncryptedDocument doc = new EncryptedDocument(4, mFile, mDataKey, mMacKey);

        // write initial metadata
        final JSONObject init = new JSONObject();
        init.put("color", "red");
        doc.writeMetadataAndContent(init, null);

        // try writing with a pipe that reports failure
        final byte[] content = "KITTENS".getBytes(StandardCharsets.UTF_8);
        final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createReliablePipe();
        new Thread() {
            @Override
            public void run() {
                final FileOutputStream os = new FileOutputStream(pipe[1].getFileDescriptor());
                try {
                    os.write(content);
                    pipe[1].closeWithError("ZOMG");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();

        final JSONObject second = new JSONObject();
        second.put("color", "blue");
        try {
            doc.writeMetadataAndContent(second, pipe[0]);
            fail("somehow wrote without error");
        } catch (IOException ignored) {
        }

        // verify that original metadata still in place
        final JSONObject after = doc.readMetadata();
        assertEquals("red", after.getString("color"));
    }
}
