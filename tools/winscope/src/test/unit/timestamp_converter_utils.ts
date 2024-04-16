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

import {Timestamp} from 'common/time';
import {TimestampConverter} from 'common/timestamp_converter';

export class TimestampConverterUtils {
  static readonly ASIA_TIMEZONE_INFO = {
    timezone: 'Asia/Kolkata',
    locale: 'en-US',
    utcOffsetMs: 19800000,
  };
  static readonly UTC_TIMEZONE_INFO = {
    timezone: 'UTC',
    locale: 'en-US',
    utcOffsetMs: 0,
  };

  static readonly TIMESTAMP_CONVERTER_WITH_UTC_OFFSET = new TimestampConverter(
    TimestampConverterUtils.ASIA_TIMEZONE_INFO,
    0n,
    0n,
  );

  static readonly TIMESTAMP_CONVERTER = new TimestampConverter(
    TimestampConverterUtils.UTC_TIMEZONE_INFO,
    0n,
    0n,
  );

  private static readonly TIMESTAMP_CONVERTER_NO_RTE_OFFSET =
    new TimestampConverter({
      timezone: 'UTC',
      locale: 'en-US',
      utcOffsetMs: 0,
    });

  static makeRealTimestamp(valueNs: bigint): Timestamp {
    return TimestampConverterUtils.TIMESTAMP_CONVERTER.makeTimestampFromRealNs(
      valueNs,
    );
  }

  static makeRealTimestampWithUTCOffset(valueNs: bigint): Timestamp {
    return TimestampConverterUtils.TIMESTAMP_CONVERTER_WITH_UTC_OFFSET.makeTimestampFromRealNs(
      valueNs,
    );
  }

  static makeElapsedTimestamp(valueNs: bigint): Timestamp {
    return TimestampConverterUtils.TIMESTAMP_CONVERTER_NO_RTE_OFFSET.makeTimestampFromMonotonicNs(
      valueNs,
    );
  }
}
