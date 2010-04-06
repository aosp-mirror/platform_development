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

package com.android.development;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Random;

public class MediaScannerActivity extends Activity
{
    private TextView mTitle;
    private int mNumToInsert = 20;
    private int mArtists;
    private int mAlbums;
    private int mSongs;
    private ContentResolver mResolver;
    private Uri mAudioUri;
    ContentValues mValues[] = new ContentValues[10];
    Random mRandom = new Random();
    StringBuilder mBuilder = new StringBuilder();

    public MediaScannerActivity() {
    }

    /** Called when the activity is first created or resumed. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.media_scanner_activity);

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addDataScheme("file");
        registerReceiver(mReceiver, intentFilter);

        EditText t = (EditText) findViewById(R.id.numsongs);
        t.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                String text = s.toString();
                try {
                    mNumToInsert = Integer.valueOf(text);
                } catch (NumberFormatException ex) {
                    mNumToInsert = 20;
                }
                setInsertButtonText();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

        });
        mTitle = (TextView) findViewById(R.id.title);
        mResolver = getContentResolver();
        mAudioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        for (int i = 0; i < 10; i++) {
            mValues[i] = new ContentValues();
        }
        setInsertButtonText();
    }

    /** Called when the activity going into the background or being destroyed. */
    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        mInsertHandler.removeMessages(0);
        super.onDestroy();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {
                mTitle.setText("Media Scanner started scanning " + intent.getData().getPath());
            }
            else if (intent.getAction().equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                mTitle.setText("Media Scanner finished scanning " + intent.getData().getPath());
            }
        }
    };

    public void startScan(View v) {
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
                + Environment.getExternalStorageDirectory())));

        mTitle.setText("Sent ACTION_MEDIA_MOUNTED to trigger the Media Scanner.");
    }

    private void setInsertButtonText() {
        String label = getString(R.string.insertbutton, Integer.valueOf(mNumToInsert));
        Button b = (Button) findViewById(R.id.insertbutton);
        b.setText(label);
    }


    public void insertItems(View v) {
        if (mInsertHandler.hasMessages(0)) {
            mInsertHandler.removeMessages(0);
            setInsertButtonText();
        } else {
            mInsertHandler.sendEmptyMessage(0);
        }
    }

    Handler mInsertHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if (mNumToInsert-- > 0) {
                addAlbum();
                runOnUiThread(mDisplayUpdater);

                if (!isFinishing()) {
                    sendEmptyMessage(0);
                }
            }
        }
    };

    Runnable mDisplayUpdater = new Runnable() {
        public void run() {
            mTitle.setText("Added " + mArtists + " artists, " + mAlbums + " albums, "
                    + mSongs + " songs.");
        }
    };

    // Add one more album (with 10 songs) to the database. This will be a compilation album,
    // with one album artist for the album, and a separate artist for each song.
    private void addAlbum() {
        try {
            String albumArtist = "Various Artists";
            String albumName = getRandomWord(3);
            int baseYear = 1969 + mRandom.nextInt(30);
            for (int i = 0; i < 10; i++) {
                mValues[i].clear();
                String artist = getRandomName();
                final ContentValues map = mValues[i];
                map.put(MediaStore.MediaColumns.DATA,
                        "http://bogus/" + albumName + "/" + artist + "_" + i);
                map.put(MediaStore.MediaColumns.TITLE,
                        getRandomWord(4) + " " + getRandomWord(2) + " " + (i + 1));
                map.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3");

                map.put(Audio.Media.ARTIST, artist);
                map.put("album_artist", albumArtist);
                map.put(Audio.Media.ALBUM, albumName);
                map.put(Audio.Media.TRACK, i + 1);
                map.put(Audio.Media.DURATION, 4*60*1000);
                map.put(Audio.Media.IS_MUSIC, 1);
                map.put(Audio.Media.YEAR, baseYear + mRandom.nextInt(10));
            }
            mResolver.bulkInsert(mAudioUri, mValues);
            mSongs += 10;
            mAlbums++;
            mArtists += 11;
        } catch (SQLiteConstraintException ex) {
            Log.d("@@@@", "insert failed", ex);
        }
    }

    /**
     * Some code to generate random names. This just strings together random
     * syllables, and randomly inserts a modifier between the first
     * and last name.
     */
    private String[] elements = new String[] {
            "ab", "am",
            "bra", "bri",
            "ci", "co",
            "de", "di", "do",
            "fa", "fi",
            "ki",
            "la", "li",
            "ma", "me", "mi", "mo",
            "na", "ni",
            "pa",
            "ta", "ti",
            "vi", "vo"
    };

    private String getRandomWord(int len) {
        int max = elements.length;
        mBuilder.setLength(0);
        for (int i = 0; i < len; i++) {
            mBuilder.append(elements[mRandom.nextInt(max)]);
        }
        char c = mBuilder.charAt(0);
        c = Character.toUpperCase(c);
        mBuilder.setCharAt(0, c);
        return mBuilder.toString();
    }

    private String getRandomName() {
        boolean longfirst = mRandom.nextInt(5) < 3;
        String first = getRandomWord(longfirst ? 3 : 2);
        String last = getRandomWord(3);
        switch (mRandom.nextInt(6)) {
            case 1:
                if (!last.startsWith("Di")) {
                    last = "di " + last;
                }
                break;
            case 2:
                last = "van " + last;
                break;
            case 3:
                last = "de " + last;
                break;
        }
        return first + " " + last;
    }



}
