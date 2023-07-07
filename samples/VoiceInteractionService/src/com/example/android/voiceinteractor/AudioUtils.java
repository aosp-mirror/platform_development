/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.example.android.voiceinteractor;

import android.media.AudioRecord;
import android.os.Trace;
import android.util.Log;

import java.time.Duration;
import java.util.Arrays;

public class AudioUtils {
    private static final String TAG = "Hotword-AudioUtils";
    private static final Duration AUDIO_RECORD_READ_DURATION = Duration.ofSeconds(1);

    static int read(AudioRecord record, int bytesPerSecond, float secondsToRead, byte[] buffer) {
        Log.i(TAG, "read(): bytesPerSecond=" + bytesPerSecond
                + ", secondsToRead=" + secondsToRead + ", bufferSize=" + buffer.length);
        int numBytes = 0;
        int nextSecondToSample = 0;
        while (true) {
            Trace.beginAsyncSection("AudioRecord.read", 0);
            int bytesRead = record.read(buffer, numBytes,
                    (int) (bytesPerSecond * AUDIO_RECORD_READ_DURATION.getSeconds()));
            Trace.endAsyncSection("AudioRecord.read", 0);
            Log.i(TAG, "AudioRecord.read offset=" + numBytes + ", size="
                    + (bytesPerSecond * AUDIO_RECORD_READ_DURATION.getSeconds()));
            numBytes += bytesRead;

            if (bytesRead <= 0) {
                Log.i(TAG, "Finished reading, last read()=" + bytesRead);
                break;
            }
            int curSecond = numBytes / bytesPerSecond;
            if (curSecond == nextSecondToSample
                    && numBytes > (bytesPerSecond * curSecond) + 10) {
                Log.i(TAG, "sample=" + Arrays.toString(
                        Arrays.copyOfRange(
                                buffer, bytesPerSecond * curSecond,
                                (bytesPerSecond * curSecond) + 10)));
                nextSecondToSample++;
            }
            if ((numBytes * 1.0 / bytesPerSecond) >= secondsToRead) {
                Log.i(TAG, "recorded enough. stopping. bytesRead=" + numBytes
                        + ", secondsRead=" + (numBytes * 1.0 / bytesPerSecond));
                break;
            }
        }
        return numBytes;
    }
}
