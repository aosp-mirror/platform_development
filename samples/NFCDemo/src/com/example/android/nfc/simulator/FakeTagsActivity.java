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
 * limitations under the License
 */
package com.example.android.nfc.simulator;

import android.app.ListActivity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Bytes;

import java.nio.charset.Charset;
import java.util.Locale;

/**
 * A activity that launches tags as if they had been scanned.
 */
public class FakeTagsActivity extends ListActivity {

    static final String TAG = "FakeTagsActivity";

    static final byte[] UID = new byte[] {0x05, 0x00, 0x03, 0x08};

    ArrayAdapter<TagDescription> mAdapter;

    public static NdefRecord newTextRecord(String text, Locale locale, boolean encodeInUtf8) {
        Preconditions.checkNotNull(text);
        Preconditions.checkNotNull(locale);
        final byte[] langBytes = locale.getLanguage().getBytes(Charsets.US_ASCII);
        final Charset utfEncoding = encodeInUtf8 ? Charsets.UTF_8 : Charset.forName("UTF-16");
        final byte[] textBytes = text.getBytes(utfEncoding);
        final int utfBit = encodeInUtf8 ? 0 : (1 << 7);
        final char status = (char) (utfBit + langBytes.length);
        final byte[] data = Bytes.concat(new byte[] {(byte) status}, langBytes, textBytes);
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], data);
    }

    public static NdefRecord newMimeRecord(String type, byte[] data) {
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(data);
        final byte[] typeBytes = type.getBytes(Charsets.US_ASCII);
        return new NdefRecord(NdefRecord.TNF_MIME_MEDIA, typeBytes, new byte[0], data);
    }

    static final class TagDescription {

        public String title;

        public NdefMessage[] msgs;

        public TagDescription(String title, byte[] bytes) {
            this.title = title;
            try {
                msgs = new NdefMessage[] {new NdefMessage(bytes)};
            } catch (final Exception e) {
                throw new RuntimeException("Failed to create tag description", e);
            }
        }

        @Override
        public String toString() {
            return title;
        }
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        final ArrayAdapter<TagDescription> adapter = new ArrayAdapter<TagDescription>(
            this, android.R.layout.simple_list_item_1, android.R.id.text1);
        adapter.add(
            new TagDescription("Broadcast NFC Text Tag", MockNdefMessages.ENGLISH_PLAIN_TEXT));
        adapter.add(new TagDescription(
            "Broadcast NFC SmartPoster URL & text", MockNdefMessages.SMART_POSTER_URL_AND_TEXT));
        adapter.add(new TagDescription(
            "Broadcast NFC SmartPoster URL", MockNdefMessages.SMART_POSTER_URL_NO_TEXT));
        setListAdapter(adapter);
        mAdapter = adapter;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        final TagDescription description = mAdapter.getItem(position);
        final Intent intent = new Intent(NfcAdapter.ACTION_TAG_DISCOVERED);
        intent.putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, description.msgs);
        startActivity(intent);
    }
}
