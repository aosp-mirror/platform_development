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

import {Parser} from 'trace/parser';
import {Timestamp, TimestampType} from 'trace/timestamp';
import {TraceType} from 'trace/trace_type';

export abstract class AbstractTracesParser<T> implements Parser<T> {
  private timestampsSet: boolean = false;
  private timestamps: Map<TimestampType, Timestamp[]> = new Map<TimestampType, Timestamp[]>();

  abstract parse(): Promise<void>;

  abstract getDescriptors(): string[];

  abstract getTraceType(): TraceType;

  abstract getEntry(index: number, timestampType: TimestampType): Promise<T>;

  abstract getLengthEntries(): number;

  getTimestamps(type: TimestampType): Timestamp[] | undefined {
    return this.timestamps.get(type);
  }

  protected async parseTimestamps() {
    for (const type of [TimestampType.ELAPSED, TimestampType.REAL]) {
      const timestamps: Timestamp[] = [];
      let areTimestampsValid = true;

      for (let index = 0; index < this.getLengthEntries(); index++) {
        const entry = await this.getEntry(index, type);
        const timestamp = this.getTimestamp(type, entry);
        if (timestamp === undefined) {
          areTimestampsValid = false;
          break;
        }
        timestamps.push(timestamp);
      }

      if (areTimestampsValid) {
        this.timestamps.set(type, timestamps);
      }
    }

    this.timestampsSet = true;
  }

  protected abstract getTimestamp(type: TimestampType, decodedEntry: any): undefined | Timestamp;
}
