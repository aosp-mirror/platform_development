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

import { TimestampType } from "common/trace/timestamp";
import dateFormat, { masks } from "dateformat";

export class TimeUtils {
  static format(timestampType: TimestampType,
    elapsedTime: bigint, clockTimeOffset: bigint|undefined = undefined): string {
    switch (timestampType) {
      case TimestampType.ELAPSED: {
        return TimeUtils.nanosecondsToHumanElapsed(elapsedTime);
      }
      case TimestampType.REAL: {
        if (clockTimeOffset === undefined) {
          throw Error("clockTimeOffset required to format real timestamp");
        }
        return TimeUtils.nanosecondsToHumanReal(elapsedTime + clockTimeOffset);
      }
      default: {
        throw Error("Unhandled timestamp type");
      }
    }
  }

  static nanosecondsToHumanElapsed(timestampNanos: number|bigint, hideNs = true): string {
    timestampNanos = BigInt(timestampNanos);
    const units = TimeUtils.units;

    let leftNanos = timestampNanos;
    const parts = units.slice().reverse().map(({nanosInUnit, unit}) => {
      let amountOfUnit = BigInt(0);
      if (leftNanos >= nanosInUnit) {
        amountOfUnit = leftNanos / BigInt(nanosInUnit);
      }
      leftNanos = leftNanos % BigInt(nanosInUnit);
      return `${amountOfUnit}${unit}`;
    });

    if (hideNs) {
      parts.pop();
    }

    // Remove all 0ed units at start
    while (parts.length > 1 && parseInt(parts[0]) === 0) {
      parts.shift();
    }

    return parts.join("");
  }

  static nanosecondsToHumanReal(timestampNanos: number|bigint): string {
    timestampNanos = BigInt(timestampNanos);
    const ms = timestampNanos / 1000000n;
    const extraNanos = timestampNanos % 1000000n;
    const formattedTime = dateFormat(new Date(Number(ms)), "HH\"h\"MM\"m\"ss\"s\"l\"ms\"");
    const formattedDate = dateFormat(new Date(Number(ms)), "d mmm yyyy Z");

    return `${formattedTime}${extraNanos}ns, ${formattedDate}`;
  }

  static humanElapsedToNanoseconds(timestampHuman: string): bigint {
    if (!TimeUtils.HUMAN_ELAPSED_TIMESTAMP_REGEX.test(timestampHuman)) {
      throw Error("Invalid elapsed timestamp format");
    }

    const units = TimeUtils.units;

    const usedUnits = timestampHuman.split(/[0-9]+/).filter(it => it !== "");
    const usedValues = timestampHuman.split(/[a-z]+/).filter(it => it !== "").map(it => parseInt(it));

    let ns = BigInt(0);

    for (let i = 0; i < usedUnits.length; i++) {
      const unit = usedUnits[i];
      const value = usedValues[i];
      const unitData = units.find(it => it.unit == unit)!;
      ns += BigInt(unitData.nanosInUnit) * BigInt(value);
    }

    return ns;
  }

  static humanRealToNanoseconds(timestampHuman: string): bigint {
    if (!TimeUtils.HUMAN_REAL_TIMESTAMP_REGEX.test(timestampHuman)) {
      throw Error("Invalid real timestamp format");
    }

    const time = timestampHuman.split(",")[0];
    const date = timestampHuman.split(",")[1];

    let timeRest = time;
    const hours = parseInt(timeRest.split("h")[0]);
    timeRest = time.split("h")[1];
    const minutes = parseInt(timeRest.split("m")[0]);
    timeRest = time.split("m")[1];
    const seconds = parseInt(timeRest.split("s")[0]);
    timeRest = time.split("s")[1];
    const milliseconds = parseInt(timeRest.split("ms")[0]);
    timeRest = time.split("ms")[1];
    const nanoseconds = parseInt(timeRest);

    const dateMilliseconds = new Date(date).getTime();

    return BigInt(hours) * BigInt(TimeUtils.TO_NANO["h"]) +
      BigInt(minutes) * BigInt(TimeUtils.TO_NANO["m"]) +
      BigInt(seconds) * BigInt(TimeUtils.TO_NANO["s"]) +
      BigInt(milliseconds) * BigInt(TimeUtils.TO_NANO["ms"]) +
      BigInt(nanoseconds) * BigInt(TimeUtils.TO_NANO["ns"]) +
      BigInt(dateMilliseconds) * BigInt(TimeUtils.TO_NANO["ms"]);
  }

  static TO_NANO = {
    "ns": 1,
    "ms": 1000000,
    "s": 1000000 * 1000,
    "m": 1000000 * 1000 * 60,
    "h": 1000000 * 1000 * 60 * 60,
    "d": 1000000 * 1000 * 60 * 60 * 24
  };

  static units = [
    {nanosInUnit: TimeUtils.TO_NANO["ns"], unit: "ns"},
    {nanosInUnit: TimeUtils.TO_NANO["ms"], unit: "ms"},
    {nanosInUnit: TimeUtils.TO_NANO["s"], unit: "s"},
    {nanosInUnit: TimeUtils.TO_NANO["m"], unit: "m"},
    {nanosInUnit: TimeUtils.TO_NANO["h"], unit: "h"},
    {nanosInUnit: TimeUtils.TO_NANO["d"], unit: "d"},
  ];

  // (?=.) checks there is at least one character with a lookahead match
  static readonly HUMAN_ELAPSED_TIMESTAMP_REGEX = /^(?=.)([0-9]+d)?([0-9]+h)?([0-9]+m)?([0-9]+s)?([0-9]+ms)?([0-9]+ns)?$/;
  static readonly HUMAN_REAL_TIMESTAMP_REGEX = /^[0-9]([0-9])?h[0-9]([0-9])?m[0-9]([0-9])?s[0-9]([0-9])?([0-9])?ms[0-9]([0-9])?([0-9])?([0-9])?([0-9])?([0-9])?ns, [0-9]([0-9])? [A-Za-z][A-Za-z][A-Za-z] [0-9][0-9][0-9][0-9]( [A-Za-z][A-Za-z][A-Za-z])?$/;
  static readonly NS_TIMESTAMP_REGEX = /^\s*[0-9]+(\s?ns)?\s*$/;
}
