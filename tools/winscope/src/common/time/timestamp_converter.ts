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

import {assertDefined, assertTrue} from 'common/assert_utils';
import {BigintMath} from 'common/bigint_math';
import {
  INVALID_TIME_NS,
  Timestamp,
  TimestampFormatter,
  TimestampFormatType,
  TimezoneInfo,
} from './time';
import {TimestampUtils} from './timestamp_utils';
import {TIME_UNITS, TIME_UNIT_TO_NANO} from './time_units';
import {UTCOffset} from './utc_offset';

// Pre-T traces do not provide real-to-boottime or real-to-monotonic offsets,so
// we group their timestamps under the "ELAPSED" umbrella term, and hope that
// the CPU was not suspended before the tracing session, causing them to diverge.
enum TimestampType {
  ELAPSED,
  REAL,
}

class RealTimestampFormatter implements TimestampFormatter {
  constructor(private utcOffset: UTCOffset) {}

  setUTCOffset(value: UTCOffset) {
    this.utcOffset = value;
  }

  format(timestamp: Timestamp, type: TimestampFormatType): string {
    const timestampNanos =
      timestamp.getValueNs() + (this.utcOffset.getValueNs() ?? 0n);
    const ms = BigintMath.divideAndRound(
      timestampNanos,
      BigInt(TIME_UNIT_TO_NANO.ms),
    );
    const formattedTimestamp = new Date(Number(ms))
      .toISOString()
      .replace('Z', '')
      .replace('T', ', ');
    if (type === TimestampFormatType.DROP_DATE) {
      return assertDefined(
        TimestampUtils.extractTimeFromHumanTimestamp(formattedTimestamp),
      );
    }
    return formattedTimestamp;
  }
}
const REAL_TIMESTAMP_FORMATTER_UTC = new RealTimestampFormatter(
  new UTCOffset(),
);

class ElapsedTimestampFormatter {
  format(timestamp: Timestamp): string {
    let leftNanos = timestamp.getValueNs();
    const parts: Array<{value: bigint; unit: string}> = TIME_UNITS.slice()
      .reverse()
      .map(({nanosInUnit, unit}) => {
        let amountOfUnit = BigInt(0);
        if (leftNanos >= nanosInUnit) {
          amountOfUnit = leftNanos / BigInt(nanosInUnit);
        }
        leftNanos = leftNanos % BigInt(nanosInUnit);
        return {value: amountOfUnit, unit};
      });

    // Remove all 0ed units at start
    while (parts.length > 1 && parts[0].value === 0n) {
      parts.shift();
    }

    return parts.map((part) => `${part.value}${part.unit}`).join('');
  }
}
const ELAPSED_TIMESTAMP_FORMATTER = new ElapsedTimestampFormatter();

export interface ParserTimestampConverter {
  makeTimestampFromRealNs(valueNs: bigint): Timestamp;
  makeTimestampFromMonotonicNs(valueNs: bigint): Timestamp;
  makeTimestampFromBootTimeNs(valueNs: bigint): Timestamp;
  makeZeroTimestamp(): Timestamp;
}

export interface ComponentTimestampConverter {
  makeTimestampFromHuman(timestampHuman: string): Timestamp;
  getUTCOffset(): string;
  makeTimestampFromNs(valueNs: bigint): Timestamp;
  validateHumanInput(timestampHuman: string): boolean;
}

export interface RemoteToolTimestampConverter {
  makeTimestampFromBootTimeNs(valueNs: bigint): Timestamp;
  makeTimestampFromRealNs(valueNs: bigint): Timestamp;
  tryGetBootTimeNs(timestamp: Timestamp): bigint | undefined;
  tryGetRealTimeNs(timestamp: Timestamp): bigint | undefined;
}

export class TimestampConverter
  implements
    ParserTimestampConverter,
    ComponentTimestampConverter,
    RemoteToolTimestampConverter
{
  private readonly utcOffset = new UTCOffset();
  private readonly realTimestampFormatter = new RealTimestampFormatter(
    this.utcOffset,
  );
  private createdTimestampType: TimestampType | undefined;

  constructor(
    private timezoneInfo: TimezoneInfo,
    private realToMonotonicTimeOffsetNs?: bigint,
    private realToBootTimeOffsetNs?: bigint,
  ) {}

  initializeUTCOffset(timestamp: Timestamp) {
    if (
      this.utcOffset.getValueNs() !== undefined ||
      !this.canMakeRealTimestamps()
    ) {
      return;
    }
    const utcValueNs = timestamp.getValueNs();
    const localNs =
      this.timezoneInfo.timezone !== 'UTC'
        ? this.addTimezoneOffset(this.timezoneInfo.timezone, utcValueNs)
        : utcValueNs;
    const utcOffsetNs = localNs - utcValueNs;
    this.utcOffset.initialize(utcOffsetNs);
  }

  setRealToMonotonicTimeOffsetNs(ns: bigint) {
    if (this.realToMonotonicTimeOffsetNs !== undefined) {
      return;
    }
    this.realToMonotonicTimeOffsetNs = ns;
  }

  setRealToBootTimeOffsetNs(ns: bigint) {
    if (this.realToBootTimeOffsetNs !== undefined) {
      return;
    }
    this.realToBootTimeOffsetNs = ns;
  }

  getUTCOffset(): string {
    return this.utcOffset.format();
  }

  makeTimestampFromMonotonicNs(valueNs: bigint): Timestamp {
    if (this.realToMonotonicTimeOffsetNs !== undefined) {
      return this.makeRealTimestamp(valueNs + this.realToMonotonicTimeOffsetNs);
    }
    return this.makeElapsedTimestamp(valueNs);
  }

  makeTimestampFromBootTimeNs(valueNs: bigint): Timestamp {
    if (this.realToBootTimeOffsetNs !== undefined) {
      return this.makeRealTimestamp(valueNs + this.realToBootTimeOffsetNs);
    }
    return this.makeElapsedTimestamp(valueNs);
  }

  makeTimestampFromRealNs(valueNs: bigint): Timestamp {
    return this.makeRealTimestamp(valueNs);
  }

  makeTimestampFromHuman(timestampHuman: string): Timestamp {
    if (TimestampUtils.isHumanElapsedTimeFormat(timestampHuman)) {
      return this.makeTimestampfromHumanElapsed(timestampHuman);
    }

    if (
      TimestampUtils.isISOFormat(timestampHuman) ||
      TimestampUtils.isRealDateTimeFormat(timestampHuman)
    ) {
      return this.makeTimestampFromHumanReal(timestampHuman);
    }

    throw new Error('Invalid timestamp format');
  }

  makeTimestampFromNs(valueNs: bigint): Timestamp {
    return new Timestamp(
      valueNs,
      this.canMakeRealTimestamps()
        ? this.realTimestampFormatter
        : ELAPSED_TIMESTAMP_FORMATTER,
    );
  }

  makeZeroTimestamp(): Timestamp {
    if (this.canMakeRealTimestamps()) {
      return new Timestamp(INVALID_TIME_NS, REAL_TIMESTAMP_FORMATTER_UTC);
    } else {
      return new Timestamp(INVALID_TIME_NS, ELAPSED_TIMESTAMP_FORMATTER);
    }
  }

  tryGetBootTimeNs(timestamp: Timestamp): bigint | undefined {
    if (
      this.createdTimestampType !== TimestampType.REAL ||
      this.realToBootTimeOffsetNs === undefined
    ) {
      return undefined;
    }
    return timestamp.getValueNs() - this.realToBootTimeOffsetNs;
  }

  tryGetRealTimeNs(timestamp: Timestamp): bigint | undefined {
    if (this.createdTimestampType !== TimestampType.REAL) {
      return undefined;
    }
    return timestamp.getValueNs();
  }

  validateHumanInput(timestampHuman: string, context = this): boolean {
    if (context.canMakeRealTimestamps()) {
      return TimestampUtils.isHumanRealTimestampFormat(timestampHuman);
    }
    return TimestampUtils.isHumanElapsedTimeFormat(timestampHuman);
  }

  clear() {
    this.createdTimestampType = undefined;
    this.realToBootTimeOffsetNs = undefined;
    this.realToMonotonicTimeOffsetNs = undefined;
    this.utcOffset.clear();
  }

  private canMakeRealTimestamps(): boolean {
    return this.createdTimestampType === TimestampType.REAL;
  }

  private makeRealTimestamp(valueNs: bigint): Timestamp {
    assertTrue(
      this.createdTimestampType === undefined ||
        this.createdTimestampType === TimestampType.REAL,
    );
    this.createdTimestampType = TimestampType.REAL;
    return new Timestamp(valueNs, this.realTimestampFormatter);
  }

  private makeElapsedTimestamp(valueNs: bigint): Timestamp {
    assertTrue(
      this.createdTimestampType === undefined ||
        this.createdTimestampType === TimestampType.ELAPSED,
    );
    this.createdTimestampType = TimestampType.ELAPSED;
    return new Timestamp(valueNs, ELAPSED_TIMESTAMP_FORMATTER);
  }

  private makeTimestampFromHumanReal(timestampHuman: string): Timestamp {
    // Remove trailing Z if present
    timestampHuman = timestampHuman.replace('Z', '');

    // Convert to ISO format if required
    if (TimestampUtils.isRealDateTimeFormat(timestampHuman)) {
      timestampHuman = timestampHuman.replace(', ', 'T');
    }

    // Date.parse only considers up to millisecond precision,
    // so only pass in YYYY-MM-DDThh:mm:ss
    let nanos = 0n;
    if (timestampHuman.includes('.')) {
      const [datetime, ns] = timestampHuman.split('.');
      nanos += BigInt(Math.floor(Number(ns.padEnd(9, '0'))));
      timestampHuman = datetime;
    }

    timestampHuman += this.utcOffset.format().slice(3);

    return this.makeTimestampFromRealNs(
      BigInt(Date.parse(timestampHuman)) * BigInt(TIME_UNIT_TO_NANO['ms']) +
        BigInt(nanos),
    );
  }

  private makeTimestampfromHumanElapsed(timestampHuman: string): Timestamp {
    const usedUnits = timestampHuman.split(/[0-9]+/).filter((it) => it !== '');
    const usedValues = timestampHuman
      .split(/[a-z]+/)
      .filter((it) => it !== '')
      .map((it) => Math.floor(Number(it)));

    let ns = BigInt(0);

    for (let i = 0; i < usedUnits.length; i++) {
      const unit = usedUnits[i];
      const value = usedValues[i];
      const unitData = assertDefined(TIME_UNITS.find((it) => it.unit === unit));
      ns += BigInt(unitData.nanosInUnit) * BigInt(value);
    }

    return this.makeElapsedTimestamp(ns);
  }

  private addTimezoneOffset(timezone: string, timestampNs: bigint): bigint {
    const utcDate = new Date(Number(timestampNs / 1000000n));
    const timezoneDateFormatted = utcDate.toLocaleString('en-US', {
      timeZone: timezone,
    });
    const timezoneDate = new Date(timezoneDateFormatted);

    let daysDiff = timezoneDate.getDay() - utcDate.getDay(); // day of the week
    if (daysDiff > 1) {
      // Saturday in timezone, Sunday in UTC
      daysDiff = -1;
    } else if (daysDiff < -1) {
      // Sunday in timezone, Saturday in UTC
      daysDiff = 1;
    }

    const hoursDiff =
      timezoneDate.getHours() - utcDate.getHours() + daysDiff * 24;
    const minutesDiff = timezoneDate.getMinutes() - utcDate.getMinutes();
    const localTimezoneOffsetMinutes = utcDate.getTimezoneOffset();

    return (
      timestampNs +
      BigInt(hoursDiff * 3.6e12) +
      BigInt(minutesDiff * 6e10) -
      BigInt(localTimezoneOffsetMinutes * 6e10)
    );
  }
}

export const UTC_TIMEZONE_INFO = {
  timezone: 'UTC',
  locale: 'en-US',
};
