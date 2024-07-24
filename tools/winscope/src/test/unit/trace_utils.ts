/*
 * Copyright (C) 2023 The Android Open Source Project
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
import {AbsoluteFrameIndex, Trace} from 'trace/trace';

export class TraceUtils {
  static async extractEntries<T>(trace: Trace<T>): Promise<T[]> {
    const promises = trace.mapEntry(async (entry, index) => {
      return await entry.getValue();
    });
    return await Promise.all(promises);
  }

  static extractTimestamps<T>(trace: Trace<T>): Timestamp[] {
    const timestamps = new Array<Timestamp>();
    trace.forEachTimestamp((timestamp) => {
      timestamps.push(timestamp);
    });
    return timestamps;
  }

  static async extractFrames<T>(
    trace: Trace<T>,
  ): Promise<Map<AbsoluteFrameIndex, T[]>> {
    const frames = new Map<AbsoluteFrameIndex, T[]>();
    const promises = trace.mapFrame(async (frame, index) => {
      frames.set(index, await TraceUtils.extractEntries(frame));
    });
    await Promise.all(promises);
    return frames;
  }
}
