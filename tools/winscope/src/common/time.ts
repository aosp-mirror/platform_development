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

export const INVALID_TIME_NS = 0n;

export class TimeRange {
  constructor(readonly from: Timestamp, readonly to: Timestamp) {}

  containsTimestamp(ts: Timestamp): boolean {
    const min = this.from.getValueNs();
    const max = this.to.getValueNs();
    return ts.getValueNs() >= min && ts.getValueNs() <= max;
  }
}

export interface TimezoneInfo {
  timezone: string;
  locale: string;
}

export interface TimestampFormatter {
  format(timestamp: Timestamp): string;
}

export class Timestamp {
  private readonly utcValueNs: bigint;
  private readonly formatter: TimestampFormatter;

  constructor(valueNs: bigint, formatter: TimestampFormatter) {
    this.utcValueNs = valueNs;
    this.formatter = formatter;
  }

  getValueNs(): bigint {
    return this.utcValueNs;
  }

  valueOf(): bigint {
    return this.utcValueNs;
  }

  in(range: TimeRange): boolean {
    return (
      range.from.getValueNs() <= this.getValueNs() &&
      this.getValueNs() <= range.to.getValueNs()
    );
  }

  add(n: bigint): Timestamp {
    return new Timestamp(this.getValueNs() + n, this.formatter);
  }

  minus(n: bigint): Timestamp {
    return new Timestamp(this.getValueNs() - n, this.formatter);
  }

  times(n: bigint): Timestamp {
    return new Timestamp(this.getValueNs() * n, this.formatter);
  }

  div(n: bigint): Timestamp {
    return new Timestamp(this.getValueNs() / n, this.formatter);
  }

  format(): string {
    return this.formatter.format(this);
  }
}
