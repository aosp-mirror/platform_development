/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.atsctvinput;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.tv.TvContract;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;

import com.example.android.atsctvinput.SampleTsStream.TsStream;
import com.example.android.atsctvinput.SectionParser.EITItem;
import com.example.android.atsctvinput.SectionParser.VCTItem;

import java.util.List;

/**
 * A sample TvInputService which plays ATSC TV stream.
 */
public class AtscTvInputService extends TvInputService {
    private static final String TAG = "AtscTvInputService";
    private static final int SEC_IN_MS = 1000;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
        // TODO: Uncomment or remove when a new API design is locked down.
        // setAvailable(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
    }

    @Override
    public TvInputService.Session onCreateSession(String inputId) {
        return new MyTvInputSessionImpl();
    }


    public TsStream getTsStreamForChannel(Uri channelUri) {
        String[] projection = { TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA };
        if (channelUri == null) {
            return null;
        }
        Cursor cursor = this.getContentResolver().query(
                channelUri, projection, null, null, null);
        if (cursor == null) {
            return null;
        }
        if (cursor.getCount() < 1) {
            cursor.close();
            return null;
        }
        cursor.moveToNext();
        TsStream stream = SampleTsStream.getTsStreamFromTuneInfo(cursor.getString(0));
        cursor.close();
        return stream;
    }

    private class MyTvInputSessionImpl extends TvInputService.Session {
        private MediaPlayer mPlayer;

        protected MyTvInputSessionImpl() {
            mPlayer = new MediaPlayer();
        }

        @Override
        public void onRelease() {
            if (mPlayer != null) {
                mPlayer.release();
                mPlayer = null;
            }
        }

        @Override
        public boolean onSetSurface(Surface surface) {
            Log.d(TAG, "onSetSurface(" + surface + ")");
            mPlayer.setSurface(surface);
            return true;
        }

        @Override
        public void onSetStreamVolume(float volume) {
            Log.d(TAG, "onSetStreamVolume(" + volume + ")");
            mPlayer.setVolume(volume, volume);
        }

        @Override
        public boolean onTune(Uri channelUri) {
            Log.d(TAG, "onTune(" + channelUri + ")");
            mPlayer.reset();
            TsStream stream = getTsStreamForChannel(channelUri);
            if (stream == null) {
                return false;
            }
            new ProgramUpdateTask().execute(stream, channelUri);
            AssetFileDescriptor afd = getResources().openRawResourceFd(stream.mResourceId);
            try {
                mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                        afd.getLength());
                mPlayer.prepare();
                afd.close();
            } catch (Exception e) {
                Log.e(TAG, "Failed to tune to(" + channelUri + ")");
                mPlayer.reset();
                return false;
            }
            mPlayer.start();
            return true;
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {
            Log.d(TAG, "onSetCaptionEnabled(" + enabled + ")");
        }
    }

    private class ProgramUpdateTask extends AsyncTask<Object, Void, Void> {
        @Override
        protected Void doInBackground(Object... objs) {
            TsStream stream = (TsStream) objs[0];
            Uri channelUri = (Uri) objs[1];
            Pair<VCTItem, List<EITItem>> result =
                    SampleTsStream.extractChannelInfo(AtscTvInputService.this, stream);
            if (result == null) {
                return null;
            }
            clearPrograms(channelUri);
            // The sample streams have passed timestamps. In order to show the metadata properly in
            // TV app, we offset the time here.
            long timeOffsetMs = Long.MIN_VALUE;
            long currentTimeMs = System.currentTimeMillis();
            for (EITItem i : result.second) {
                if (timeOffsetMs == Long.MIN_VALUE) {
                    timeOffsetMs = currentTimeMs - i.getStartTime() * SEC_IN_MS;
                }
                insertProgram(channelUri, i, timeOffsetMs);
            }
            return null;
        }

        private void clearPrograms(Uri channelUri) {
            Uri uri = TvContract.buildProgramsUriForChannel(channelUri);
            getContentResolver().delete(uri, null, null);
        }

        private Uri insertProgram(Uri channelUri, EITItem event, long timeOffsetMs) {
            Log.d(TAG, "insertProgram " + event.getTitleText());
            ContentValues values = new ContentValues();
            values.put(TvContract.Programs.COLUMN_CHANNEL_ID, ContentUris.parseId(channelUri));
            values.put(TvContract.Programs.COLUMN_TITLE, event.getTitleText());
            values.put(TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS, timeOffsetMs
                    + event.getStartTime() * SEC_IN_MS);
            values.put(TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS, timeOffsetMs
                    + (event.getStartTime() + event.getLengthInSecond()) * SEC_IN_MS);
            return getContentResolver().insert(
                    TvContract.Programs.CONTENT_URI, values);
        }
    }
}
