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
import android.util.Log;

import java.util.Arrays;

public class AudioUtils {
    private static String TAG = "Hotword-AudioUtils";

    static int read(AudioRecord record, int bytesPerSecond, float secondsToRead, byte[] buffer) {
        int numBytes = 0;
        int nextSecondToSample = 0;
        while (true) {
            int bytesRead = record.read(buffer, numBytes, numBytes + 1024);
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
            if (numBytes * 1.0 / bytesPerSecond >= secondsToRead) {
                Log.i(TAG, "recorded enough. stopping.");
                break;
            }
        }
        return numBytes;
    }
}
