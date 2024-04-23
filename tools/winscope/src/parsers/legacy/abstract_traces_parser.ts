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

import {Timestamp} from 'common/time';
import {ParserTimestampConverter} from 'common/timestamp_converter';
import {CoarseVersion} from 'trace/coarse_version';
import {
  CustomQueryParserResultTypeMap,
  CustomQueryType,
} from 'trace/custom_query';
import {AbsoluteEntryIndex, EntriesRange} from 'trace/index_types';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';

export abstract class AbstractTracesParser<T> implements Parser<T> {
  private timestamps: Timestamp[] | undefined;
  protected timestampConverter: ParserTimestampConverter;

  protected abstract getTimestamp(decodedEntry: any): Timestamp;

  constructor(timestampConverter: ParserTimestampConverter) {
    this.timestampConverter = timestampConverter;
  }

  getCoarseVersion(): CoarseVersion {
    return CoarseVersion.LEGACY;
  }

  customQuery<Q extends CustomQueryType>(
    type: Q,
    entriesRange: EntriesRange,
  ): Promise<CustomQueryParserResultTypeMap[Q]> {
    throw new Error('Not implemented');
  }

  getTimestamps(): Timestamp[] | undefined {
    return this.timestamps;
  }

  async createTimestamps() {
    this.timestamps = await this.decodeTimestamps();
  }

  private async decodeTimestamps() {
    const timestampsNs = [];
    for (let index = 0; index < this.getLengthEntries(); index++) {
      const entry = await this.getEntry(index);
      const timestamp = this.getTimestamp(entry);
      timestampsNs.push(timestamp);
    }
    return timestampsNs;
  }

  abstract parse(): Promise<void>;
  abstract getDescriptors(): string[];
  abstract getTraceType(): TraceType;
  abstract getEntry(index: AbsoluteEntryIndex): Promise<T>;
  abstract getLengthEntries(): number;
  abstract getRealToMonotonicTimeOffsetNs(): bigint | undefined;
  abstract getRealToBootTimeOffsetNs(): bigint | undefined;
}
