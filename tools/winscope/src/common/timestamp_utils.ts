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
import {TIME_UNITS} from './time_units';

export class TimestampUtils {
  // (?=.) checks there is at least one character with a lookahead match
  static readonly HUMAN_ELAPSED_TIMESTAMP_REGEX =
    /^(?=.)([0-9]+d)?([0-9]+h)?([0-9]+m)?([0-9]+s)?([0-9]+ms)?([0-9]+ns)?$/;
  static readonly HUMAN_REAL_TIMESTAMP_REGEX =
    /^[0-9]{4}-((0[13578]|1[02])-(0[1-9]|[12][0-9]|3[01])|(0[469]|11)-(0[1-9]|[12][0-9]|30)|(02)-(0[1-9]|[12][0-9]))T(0[0-9]|1[0-9]|2[0-3]):(0[0-9]|[1-5][0-9]):(0[0-9]|[1-5][0-9])(\.[0-9]{1,9})?Z?$/;
  static readonly NS_TIMESTAMP_REGEX = /^\s*[0-9]+(\s?ns)?\s*$/;

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

  static formatElapsedNs(timestampNanos: bigint, hideNs = false): string {
    let leftNanos = timestampNanos;
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

    if (hideNs) {
      parts.pop();
    }

    // Remove all 0ed units at start
    while (parts.length > 1 && parts[0].value === 0n) {
      parts.shift();
    }

    return parts.map((part) => `${part.value}${part.unit}`).join('');
  }
}
