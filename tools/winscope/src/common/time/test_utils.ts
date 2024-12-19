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

import {Timestamp, TimezoneInfo} from 'common/time/time';
import {TimestampConverter} from 'common/time/timestamp_converter';

class TimestampConverterTestUtils {
  readonly ASIA_TIMEZONE_INFO: TimezoneInfo = {
    timezone: 'Asia/Kolkata',
    locale: 'en-US',
  };
  readonly UTC_TIMEZONE_INFO: TimezoneInfo = {
    timezone: 'UTC',
    locale: 'en-US',
  };

  readonly TIMESTAMP_CONVERTER = new TimestampConverter(
    this.UTC_TIMEZONE_INFO,
    0n,
    0n,
  );

  readonly TIMESTAMP_CONVERTER_WITH_UTC_OFFSET = new TimestampConverter(
    this.ASIA_TIMEZONE_INFO,
    0n,
    0n,
  );

  private readonly TIMESTAMP_CONVERTER_NO_RTE_OFFSET = new TimestampConverter({
    timezone: 'UTC',
    locale: 'en-US',
  });

  constructor() {
    this.TIMESTAMP_CONVERTER_WITH_UTC_OFFSET.initializeUTCOffset(
      this.TIMESTAMP_CONVERTER_WITH_UTC_OFFSET.makeTimestampFromRealNs(0n),
    );
  }

  makeRealTimestamp(valueNs: bigint): Timestamp {
    return this.TIMESTAMP_CONVERTER.makeTimestampFromRealNs(valueNs);
  }

  makeRealTimestampWithUTCOffset(valueNs: bigint): Timestamp {
    return this.TIMESTAMP_CONVERTER_WITH_UTC_OFFSET.makeTimestampFromRealNs(
      valueNs,
    );
  }

  makeElapsedTimestamp(valueNs: bigint): Timestamp {
    return this.TIMESTAMP_CONVERTER_NO_RTE_OFFSET.makeTimestampFromMonotonicNs(
      valueNs,
    );
  }

  makeZeroTimestamp(): Timestamp {
    return this.TIMESTAMP_CONVERTER_NO_RTE_OFFSET.makeZeroTimestamp();
  }
}

function testTimestamps(
  timestamp: Timestamp,
  expectedTimestamp: Timestamp,
): boolean {
  if (timestamp.format() !== expectedTimestamp.format()) return false;
  if (timestamp.getValueNs() !== expectedTimestamp.getValueNs()) {
    return false;
  }
  return true;
}

export function timestampEqualityTester(
  first: any,
  second: any,
): boolean | undefined {
  if (first instanceof Timestamp && second instanceof Timestamp) {
    return testTimestamps(first, second);
  }
  return undefined;
}

export const TimestampConverterUtils = new TimestampConverterTestUtils();
