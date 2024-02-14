/*
 * Copyright (C) 2024 The Android Open Source Project
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

import {Timestamp, TimestampType, TimezoneInfo} from './time';
import {TimeUtils} from './time_utils';

export class TimestampFactory {
  constructor(private timezoneInfo: TimezoneInfo = {timezone: 'UTC', locale: 'en-US'}) {}

  makeRealTimestamp(valueNs: bigint, realToElapsedTimeOffsetNs?: bigint): Timestamp {
    const valueWithRealtimeOffset = valueNs + (realToElapsedTimeOffsetNs ?? 0n);
    const localNs =
      this.timezoneInfo.timezone !== 'UTC'
        ? TimeUtils.addTimezoneOffset(this.timezoneInfo, valueWithRealtimeOffset)
        : valueWithRealtimeOffset;
    return new Timestamp(TimestampType.REAL, localNs);
  }

  makeElapsedTimestamp(valueNs: bigint): Timestamp {
    return new Timestamp(TimestampType.ELAPSED, valueNs);
  }

  canMakeTimestampFromType(type: TimestampType) {
    return type === TimestampType.ELAPSED || type === TimestampType.REAL;
  }

  makeTimestampFromType(
    type: TimestampType,
    valueNs: bigint,
    realToElapsedTimeOffsetNs?: bigint
  ): Timestamp {
    switch (type) {
      case TimestampType.REAL:
        if (realToElapsedTimeOffsetNs === undefined) {
          throw new Error("realToElapsedTimeOffsetNs can't be undefined to use real timestamp");
        }
        return this.makeRealTimestamp(valueNs, realToElapsedTimeOffsetNs);
      case TimestampType.ELAPSED:
        return this.makeElapsedTimestamp(valueNs);
      default:
        throw new Error('Unhandled timestamp type');
    }
  }
}

export const NO_TIMEZONE_OFFSET_FACTORY = new TimestampFactory();
