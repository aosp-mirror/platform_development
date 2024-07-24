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

import {Timestamp} from 'common/time';

class ScreenRecordingUtils {
  // Video time correction epsilon. Without correction, we could display the previous frame.
  // This correction was already present in the legacy Winscope.
  private static readonly EPSILON_SECONDS = 0.00001;

  static timestampToVideoTimeSeconds(
    firstTimestamp: Timestamp,
    currentTimestamp: Timestamp,
  ) {
    if (firstTimestamp.getType() !== currentTimestamp.getType()) {
      throw new Error('Attempted to use timestamps with different type');
    }
    const videoTimeSeconds =
      Number(currentTimestamp.getValueNs() - firstTimestamp.getValueNs()) /
        1000000000 +
      ScreenRecordingUtils.EPSILON_SECONDS;
    return videoTimeSeconds;
  }
}

export {ScreenRecordingUtils};
