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
export class TimeUtils {
  static nanosecondsToHuman(timestampNanos: number|bigint, hideNs = true): string {
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

  static humanToNanoseconds(timestampHuman: string): bigint {
    if (!TimeUtils.HUMAN_TIMESTAMP_REGEX.test(timestampHuman)) {
      throw Error("Invalid timestamp format");
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

  static units = [
    {nanosInUnit: 1, unit: "ns"},
    {nanosInUnit: 1000000, unit: "ms"},
    {nanosInUnit: 1000000 * 1000, unit: "s"},
    {nanosInUnit: 1000000 * 1000 * 60, unit: "m"},
    {nanosInUnit: 1000000 * 1000 * 60 * 60, unit: "h"},
    {nanosInUnit: 1000000 * 1000 * 60 * 60 * 24, unit: "d"},
  ];

  // (?=.) checks there is at least one character with a lookahead match
  static readonly HUMAN_TIMESTAMP_REGEX = /^(?=.)([0-9]+d)?([0-9]+h)?([0-9]+m)?([0-9]+s)?([0-9]+ms)?([0-9]+ns)?$/;
  static readonly NS_TIMESTAMP_REGEX = /^[0-9]+$/;
}
