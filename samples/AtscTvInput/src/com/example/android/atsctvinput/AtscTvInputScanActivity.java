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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import com.example.android.atsctvinput.SampleTsStream.TsStream;
import com.example.android.atsctvinput.SectionParser.EITItem;
import com.example.android.atsctvinput.SectionParser.VCTItem;
import com.example.atsctvinput.R;

import java.util.List;

/**
 * The scan/setup activity for ATSC TvInput app.
 */
public class AtscTvInputScanActivity extends Activity {
    private static final String TAG = "AtscTvInputScanActivity";
    private static final long FAKE_SCANTIME_PER_CHANNEL_MS = 1000;
    private ProgressDialog mProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getResources().getString(R.string.channel_scan_message));
        mProgressDialog.setCancelable(false);

        mProgressDialog.show();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                clearChannels();
                doAutoScan();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                mProgressDialog.hide();
                AtscTvInputScanActivity.this.setResult(Activity.RESULT_OK);
                AtscTvInputScanActivity.this.finish();
            }
        }.execute();
    }

    private void clearChannels() {
        String id = TvContract.buildInputId(new ComponentName(this, AtscTvInputService.class));
        Uri uri = TvContract.buildChannelsUriForInput(id);
        getContentResolver().delete(uri, null, null);
    }

    private void doAutoScan() {
        for (TsStream s : SampleTsStream.SAMPLES) {
            Pair<VCTItem, List<EITItem>> result = SampleTsStream.extractChannelInfo(this, s);
            if (result != null) {
                insertChannel(result.first, s);
                try {
                    Thread.sleep(FAKE_SCANTIME_PER_CHANNEL_MS);
                } catch (InterruptedException e) {
                    // Do nothing.
                }
            }
        }
    }

    public void insertChannel(VCTItem channel, TsStream stream) {
        Log.d(TAG, "Channel " + channel.getShortName() + " " + channel.getMajorChannelNumber()
                + "-" + channel.getMinorChannelNumber() + " is detected.");
        ContentValues values = new ContentValues();
        values.put(TvContract.Channels.COLUMN_DISPLAY_NUMBER,
                channel.getMajorChannelNumber() + "-" + channel.getMinorChannelNumber());
        values.put(TvContract.Channels.COLUMN_DISPLAY_NAME, channel.getShortName());
        values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA,
                SampleTsStream.getTuneInfo(stream));
        getContentResolver().insert(TvContract.Channels.CONTENT_URI, values);
    }
}
