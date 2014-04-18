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

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.example.android.atsctvinput.PSIPParser.PSIPOutputListener;
import com.example.android.atsctvinput.SectionParser.EITItem;
import com.example.android.atsctvinput.SectionParser.VCTItem;
import com.example.atsctvinput.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class SampleTsStream {
    private static final String TAG = "SampleTsStream";
    /*
     * Sample MPEG2 transport streams which include ATSC PSIP data.
     * In order to play the stream with Android mediaplayer, each stream has exactly one program
     * and video and audio tracks are transcoded to MPEG4 and AAC respectively.
     */
    public static final TsStream[] SAMPLES = new TsStream[] {
        new TsStream(R.raw.freq_1_prog_1, 1, 1),
        new TsStream(R.raw.freq_2_prog_1029, 2, 1029),
    };

    private static final int READ_BUF_SIZE = 188;

    public static String getTuneInfo(TsStream stream) {
        return Long.toString(stream.mFrequency) + "," + Integer.toString(stream.mProgramNumber);
    }

    public static TsStream getTsStreamFromTuneInfo(String tuneInfo) {
        String values[] = tuneInfo.split(",");
        if (values.length != 2) {
            return null;
        }
        long freq = Long.parseLong(values[0]);
        int programNumber = Integer.parseInt(values[1]);
        for (TsStream s : SAMPLES) {
            if (s.mFrequency == freq && s.mProgramNumber == programNumber) {
                return s;
            }
        }
        return null;
    }

    public static Pair<VCTItem, List<EITItem>> extractChannelInfo(
            Context context, final TsStream stream) {
        final Object[] results = new Object[2];
        PSIPParser mPSIPParser = new PSIPParser(new PSIPOutputListener() {
            @Override
            public void onEITPidDetected(int pid) {
                // Do nothing;
            }

            @Override
            public void onEITItemParsed(VCTItem channel, List<EITItem> items) {
                if (channel.getProgramNumber() == stream.mProgramNumber) {
                    results[0] = channel;
                    results[1] = items;
                }
            }
        });
        InputStream in = context.getResources().openRawResource(stream.mResourceId);
        byte[] buf = new byte[READ_BUF_SIZE];
        try {
            while (results[0] == null && results[1] == null
                    && in.read(buf, 0, READ_BUF_SIZE) == READ_BUF_SIZE) {
                mPSIPParser.feedTSData(buf, 0, buf.length);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error while detecting channel from freq " + stream.mFrequency
                    + " program number " + stream.mProgramNumber);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                Log.e(TAG, "Error while closing input stream for " + stream.mFrequency);
            }
        }
        if (results[0] != null && results[1] != null) {
            return new Pair<VCTItem, List<EITItem>>(
                    (VCTItem) results[0], (List<EITItem>) results[1]);
        }
        return null;
    }

    public static class TsStream {
        public final int mResourceId;
        public final long mFrequency;
        public final int mProgramNumber;

        public TsStream(int resourceId, long frequency, int programNumber) {
            mResourceId = resourceId;
            mFrequency = frequency;
            mProgramNumber = programNumber;
        }
    }
}
