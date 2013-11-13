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

import static com.example.android.vault.VaultProvider.TAG;

import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.ProtocolException;
import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * Represents a single encrypted document stored on disk. Handles encryption,
 * decryption, and authentication of the document when requested.
 * <p>
 * Encrypted documents are stored on disk as a magic number, followed by an
 * encrypted metadata section, followed by an encrypted content section. The
 * content section always starts at a specific offset {@link #CONTENT_OFFSET} to
 * allow metadata updates without rewriting the entire file.
 * <p>
 * Each section is encrypted using AES-128 with a random IV, and authenticated
 * with SHA-256. Data encrypted and authenticated like this can be safely stored
 * on untrusted storage devices, as long as the keys are stored securely.
 * <p>
 * Not inherently thread safe.
 */
public class EncryptedDocument {

    /**
     * Magic number to identify file; "AVLT".
     */
    private static final int MAGIC_NUMBER = 0x41564c54;

    /**
     * Offset in file at which content section starts. Magic and metadata
     * section must fully fit before this offset.
     */
    private static final int CONTENT_OFFSET = 4096;

    private static final boolean DEBUG_METADATA = true;

    /** Key length for AES-128 */
    public static final int DATA_KEY_LENGTH = 16;
    /** Key length for SHA-256 */
    public static final int MAC_KEY_LENGTH = 32;

    private final SecureRandom mRandom;
    private final Cipher mCipher;
    private final Mac mMac;

    private final long mDocId;
    private final File mFile;
    private final SecretKey mDataKey;
    private final SecretKey mMacKey;

    /**
     * Create an encrypted document.
     *
     * @param docId the expected {@link Document#COLUMN_DOCUMENT_ID} to be
     *            validated when reading metadata.
     * @param file location on disk where the encrypted document is stored. May
     *            not exist yet.
     */
    public EncryptedDocument(long docId, File file, SecretKey dataKey, SecretKey macKey)
            throws GeneralSecurityException {
        mRandom = new SecureRandom();
        mCipher = Cipher.getInstance("AES/CTR/NoPadding");
        mMac = Mac.getInstance("HmacSHA256");

        if (dataKey.getEncoded().length != DATA_KEY_LENGTH) {
            throw new IllegalArgumentException("Expected data key length " + DATA_KEY_LENGTH);
        }
        if (macKey.getEncoded().length != MAC_KEY_LENGTH) {
            throw new IllegalArgumentException("Expected MAC key length " + MAC_KEY_LENGTH);
        }

        mDocId = docId;
        mFile = file;
        mDataKey = dataKey;
        mMacKey = macKey;
    }

    public File getFile() {
        return mFile;
    }

    @Override
    public String toString() {
        return mFile.getName();
    }

    /**
     * Decrypt and return parsed metadata section from this document.
     *
     * @throws DigestException if metadata fails MAC check, or if
     *             {@link Document#COLUMN_DOCUMENT_ID} recorded in metadata is
     *             unexpected.
     */
    public JSONObject readMetadata() throws IOException, GeneralSecurityException {
        final RandomAccessFile f = new RandomAccessFile(mFile, "r");
        try {
            assertMagic(f);

            // Only interested in metadata section
            final ByteArrayOutputStream metaOut = new ByteArrayOutputStream();
            readSection(f, metaOut);

            final String rawMeta = metaOut.toString(StandardCharsets.UTF_8.name());
            if (DEBUG_METADATA) {
                Log.d(TAG, "Found metadata for " + mDocId + ": " + rawMeta);
            }

            final JSONObject meta = new JSONObject(rawMeta);

            // Validate that metadata belongs to requested file
            if (meta.getLong(Document.COLUMN_DOCUMENT_ID) != mDocId) {
                throw new DigestException("Unexpected document ID");
            }

            return meta;

        } catch (JSONException e) {
            throw new IOException(e);
        } finally {
            f.close();
        }
    }

    /**
     * Decrypt and read content section of this document, writing it into the
     * given pipe.
     * <p>
     * Pipe is left open, so caller is responsible for calling
     * {@link ParcelFileDescriptor#close()} or
     * {@link ParcelFileDescriptor#closeWithError(String)}.
     *
     * @param contentOut write end of a pipe.
     * @throws DigestException if content fails MAC check. Some or all content
     *             may have already been written to the pipe when the MAC is
     *             validated.
     */
    public void readContent(ParcelFileDescriptor contentOut)
            throws IOException, GeneralSecurityException {
        final RandomAccessFile f = new RandomAccessFile(mFile, "r");
        try {
            assertMagic(f);

            if (f.length() <= CONTENT_OFFSET) {
                throw new IOException("Document has no content");
            }

            // Skip over metadata section
            f.seek(CONTENT_OFFSET);
            readSection(f, new FileOutputStream(contentOut.getFileDescriptor()));

        } finally {
            f.close();
        }
    }

    /**
     * Encrypt and write both the metadata and content sections of this
     * document, reading the content from the given pipe. Internally uses
     * {@link ParcelFileDescriptor#checkError()} to verify that content arrives
     * without errors. Writes to temporary file to keep atomic view of contents,
     * swapping into place only when write is successful.
     * <p>
     * Pipe is left open, so caller is responsible for calling
     * {@link ParcelFileDescriptor#close()} or
     * {@link ParcelFileDescriptor#closeWithError(String)}.
     *
     * @param contentIn read end of a pipe.
     */
    public void writeMetadataAndContent(JSONObject meta, ParcelFileDescriptor contentIn)
            throws IOException, GeneralSecurityException {
        // Write into temporary file to provide an atomic view of existing
        // contents during write, and also to recover from failed writes.
        final String tempName = mFile.getName() + ".tmp_" + Thread.currentThread().getId();
        final File tempFile = new File(mFile.getParentFile(), tempName);

        RandomAccessFile f = new RandomAccessFile(tempFile, "rw");
        try {
            // Truncate any existing data
            f.setLength(0);

            // Write content first to detect size
            if (contentIn != null) {
                f.seek(CONTENT_OFFSET);
                final int plainLength = writeSection(
                        f, new FileInputStream(contentIn.getFileDescriptor()));
                meta.put(Document.COLUMN_SIZE, plainLength);

                // Verify that remote side of pipe finished okay; if they
                // crashed or indicated an error then this throws and we
                // leave the original file intact and clean up temp below.
                contentIn.checkError();
            }

            meta.put(Document.COLUMN_DOCUMENT_ID, mDocId);
            meta.put(Document.COLUMN_LAST_MODIFIED, System.currentTimeMillis());

            // Rewind and write metadata section
            f.seek(0);
            f.writeInt(MAGIC_NUMBER);

            final ByteArrayInputStream metaIn = new ByteArrayInputStream(
                    meta.toString().getBytes(StandardCharsets.UTF_8));
            writeSection(f, metaIn);

            if (f.getFilePointer() > CONTENT_OFFSET) {
                throw new IOException("Metadata section was too large");
            }

            // Everything written fine, atomically swap new data into place.
            // fsync() before close would be overkill, since rename() is an
            // atomic barrier.
            f.close();
            tempFile.renameTo(mFile);

        } catch (JSONException e) {
            throw new IOException(e);
        } finally {
            // Regardless of what happens, always try cleaning up.
            f.close();
            tempFile.delete();
        }
    }

    /**
     * Read and decrypt the section starting at the current file offset.
     * Validates MAC of decrypted data, throwing if mismatch. When finished,
     * file offset is at the end of the entire section.
     */
    private void readSection(RandomAccessFile f, OutputStream out)
            throws IOException, GeneralSecurityException {
        final long start = f.getFilePointer();

        final Section section = new Section();
        section.read(f);

        final IvParameterSpec ivSpec = new IvParameterSpec(section.iv);
        mCipher.init(Cipher.DECRYPT_MODE, mDataKey, ivSpec);
        mMac.init(mMacKey);

        byte[] inbuf = new byte[8192];
        byte[] outbuf;
        int n;
        while ((n = f.read(inbuf, 0, (int) Math.min(section.length, inbuf.length))) != -1) {
            section.length -= n;
            mMac.update(inbuf, 0, n);
            outbuf = mCipher.update(inbuf, 0, n);
            if (outbuf != null) {
                out.write(outbuf);
            }
            if (section.length == 0) break;
        }

        section.assertMac(mMac.doFinal());

        outbuf = mCipher.doFinal();
        if (outbuf != null) {
            out.write(outbuf);
        }
    }

    /**
     * Encrypt and write the given stream as a full section. Writes section
     * header and encrypted data starting at the current file offset. When
     * finished, file offset is at the end of the entire section.
     */
    private int writeSection(RandomAccessFile f, InputStream in)
            throws IOException, GeneralSecurityException {
        final long start = f.getFilePointer();

        // Write header; we'll come back later to finalize details
        final Section section = new Section();
        section.write(f);

        final long dataStart = f.getFilePointer();

        mRandom.nextBytes(section.iv);

        final IvParameterSpec ivSpec = new IvParameterSpec(section.iv);
        mCipher.init(Cipher.ENCRYPT_MODE, mDataKey, ivSpec);
        mMac.init(mMacKey);

        int plainLength = 0;
        byte[] inbuf = new byte[8192];
        byte[] outbuf;
        int n;
        while ((n = in.read(inbuf)) != -1) {
            plainLength += n;
            outbuf = mCipher.update(inbuf, 0, n);
            if (outbuf != null) {
                mMac.update(outbuf);
                f.write(outbuf);
            }
        }

        outbuf = mCipher.doFinal();
        if (outbuf != null) {
            mMac.update(outbuf);
            f.write(outbuf);
        }

        section.setMac(mMac.doFinal());

        final long dataEnd = f.getFilePointer();
        section.length = dataEnd - dataStart;

        // Rewind and update header
        f.seek(start);
        section.write(f);
        f.seek(dataEnd);

        return plainLength;
    }

    /**
     * Header of a single file section.
     */
    private static class Section {
        long length;
        final byte[] iv = new byte[DATA_KEY_LENGTH];
        final byte[] mac = new byte[MAC_KEY_LENGTH];

        public void read(RandomAccessFile f) throws IOException {
            length = f.readLong();
            f.readFully(iv);
            f.readFully(mac);
        }

        public void write(RandomAccessFile f) throws IOException {
            f.writeLong(length);
            f.write(iv);
            f.write(mac);
        }

        public void setMac(byte[] mac) {
            if (mac.length != this.mac.length) {
                throw new IllegalArgumentException("Unexpected MAC length");
            }
            System.arraycopy(mac, 0, this.mac, 0, this.mac.length);
        }

        public void assertMac(byte[] mac) throws DigestException {
            if (mac.length != this.mac.length) {
                throw new IllegalArgumentException("Unexpected MAC length");
            }
            byte result = 0;
            for (int i = 0; i < mac.length; i++) {
                result |= mac[i] ^ this.mac[i];
            }
            if (result != 0) {
                throw new DigestException();
            }
        }
    }

    private static void assertMagic(RandomAccessFile f) throws IOException {
        final int magic = f.readInt();
        if (magic != MAGIC_NUMBER) {
            throw new ProtocolException("Bad magic number: " + Integer.toHexString(magic));
        }
    }
}
