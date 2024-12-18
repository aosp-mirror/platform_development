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

import {Timestamp} from 'common/time/time';

export class TimestampUtils {
  // (?=.) checks there is at least one character with a lookahead match
  private static readonly REAL_TIME_ONLY_REGEX =
    /^(0[0-9]|1[0-9]|2[0-3]):(0[0-9]|[1-5][0-9]):(0[0-9]|[1-5][0-9])(\.[0-9]{1,9})?Z?$/;
  private static readonly REAL_DATE_TIME_REGEX =
    /^[0-9]{4}-((0[13578]|1[02])-(0[1-9]|[12][0-9]|3[01])|(0[469]|11)-(0[1-9]|[12][0-9]|30)|(02)-(0[1-9]|[12][0-9])),\s(0[0-9]|1[0-9]|2[0-3]):(0[0-9]|[1-5][0-9]):(0[0-9]|[1-5][0-9])(\.[0-9]{1,9})?Z?$/;
  private static readonly ISO_TIMESTAMP_REGEX =
    /^[0-9]{4}-((0[13578]|1[02])-(0[1-9]|[12][0-9]|3[01])|(0[469]|11)-(0[1-9]|[12][0-9]|30)|(02)-(0[1-9]|[12][0-9]))T(0[0-9]|1[0-9]|2[0-3]):(0[0-9]|[1-5][0-9]):(0[0-9]|[1-5][0-9])(\.[0-9]{1,9})?Z?$/;
  private static readonly ELAPSED_TIME_REGEX =
    /^(?=.)([0-9]+d)?([0-9]+h)?([0-9]+m)?([0-9]+s)?([0-9]+ms)?([0-9]+ns)?$/;
  private static readonly NS_TIME_REGEX = /^\s*[0-9]+(\s?ns)?\s*$/;

  static isNsFormat(timestampHuman: string): boolean {
    return TimestampUtils.NS_TIME_REGEX.test(timestampHuman);
  }

  static isHumanElapsedTimeFormat(timestampHuman: string): boolean {
    return TimestampUtils.ELAPSED_TIME_REGEX.test(timestampHuman);
  }

  static isRealTimeOnlyFormat(timestampHuman: string): boolean {
    return TimestampUtils.REAL_TIME_ONLY_REGEX.test(timestampHuman);
  }

  static isRealDateTimeFormat(timestampHuman: string): boolean {
    return TimestampUtils.REAL_DATE_TIME_REGEX.test(timestampHuman);
  }

  static isISOFormat(timestampHuman: string): boolean {
    return TimestampUtils.ISO_TIMESTAMP_REGEX.test(timestampHuman);
  }

  static isHumanRealTimestampFormat(timestampHuman: string): boolean {
    return (
      TimestampUtils.isISOFormat(timestampHuman) ||
      TimestampUtils.isRealDateTimeFormat(timestampHuman) ||
      TimestampUtils.isRealTimeOnlyFormat(timestampHuman)
    );
  }

  static extractDateFromHumanTimestamp(
    timestampHuman: string,
  ): string | undefined {
    if (
      !TimestampUtils.isRealDateTimeFormat(timestampHuman) &&
      !TimestampUtils.isISOFormat(timestampHuman)
    ) {
      return undefined;
    }
    return timestampHuman.slice(0, 10);
  }

  static extractTimeFromHumanTimestamp(
    timestampHuman: string,
  ): string | undefined {
    if (TimestampUtils.isRealDateTimeFormat(timestampHuman)) {
      return timestampHuman.slice(12);
    }
    if (TimestampUtils.isISOFormat(timestampHuman)) {
      return timestampHuman.slice(11);
    }
    if (TimestampUtils.isRealTimeOnlyFormat(timestampHuman)) {
      return timestampHuman;
    }
    return undefined;
  }

  static compareFn(a: Timestamp, b: Timestamp): number {
    return Number(a.getValueNs() - b.getValueNs());
  }

  static min(ts1: Timestamp, ts2: Timestamp): Timestamp {
    if (ts2.getValueNs() < ts1.getValueNs()) {
      return ts2;
    }

    return ts1;
  }

  static max(ts1: Timestamp, ts2: Timestamp): Timestamp {
    if (ts2.getValueNs() > ts1.getValueNs()) {
      return ts2;
    }

    return ts1;
  }
}
