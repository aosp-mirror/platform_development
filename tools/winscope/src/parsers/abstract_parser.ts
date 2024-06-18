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

import {Timestamp, TimestampType} from 'common/time';
import {TimestampFactory} from 'common/timestamp_factory';
import {
  CustomQueryParamTypeMap,
  CustomQueryParserResultTypeMap,
  CustomQueryType,
} from 'trace/custom_query';
import {AbsoluteEntryIndex, EntriesRange} from 'trace/index_types';
import {Parser} from 'trace/parser';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {ParsingUtils} from './parsing_utils';

abstract class AbstractParser<T extends object = object> implements Parser<T> {
  private timestamps = new Map<TimestampType, Timestamp[]>();
  protected traceFile: TraceFile;
  protected decodedEntries: any[] = [];
  protected timestampFactory: TimestampFactory;

  protected abstract getMagicNumber(): undefined | number[];
  protected abstract decodeTrace(trace: Uint8Array): any[];
  protected abstract getTimestamp(
    type: TimestampType,
    decodedEntry: any,
  ): undefined | Timestamp;
  protected abstract processDecodedEntry(
    index: number,
    timestampType: TimestampType,
    decodedEntry: any,
  ): any;

  constructor(trace: TraceFile, timestampFactory: TimestampFactory) {
    this.traceFile = trace;
    this.timestampFactory = timestampFactory;
  }

  async parse() {
    const traceBuffer = new Uint8Array(await this.traceFile.file.arrayBuffer());
    ParsingUtils.throwIfMagicNumberDoesNotMatch(
      traceBuffer,
      this.getMagicNumber(),
    );
    this.decodedEntries = this.decodeTrace(traceBuffer);
    this.timestamps = this.decodeTimestamps();
  }

  getDescriptors(): string[] {
    return [this.traceFile.getDescriptor()];
  }

  getLengthEntries(): number {
    return this.decodedEntries.length;
  }

  getTimestamps(type: TimestampType): undefined | Timestamp[] {
    return this.timestamps.get(type);
  }

  getEntry(
    index: AbsoluteEntryIndex,
    timestampType: TimestampType,
  ): Promise<T> {
    const entry = this.processDecodedEntry(
      index,
      timestampType,
      this.decodedEntries[index],
    );
    return Promise.resolve(entry);
  }

  customQuery<Q extends CustomQueryType>(
    type: Q,
    entriesRange: EntriesRange,
    param?: CustomQueryParamTypeMap[Q],
  ): Promise<CustomQueryParserResultTypeMap[Q]> {
    throw new Error('Not implemented');
  }

  private decodeTimestamps(): Map<TimestampType, Timestamp[]> {
    const timeStampMap = new Map<TimestampType, Timestamp[]>();
    for (const type of [TimestampType.ELAPSED, TimestampType.REAL]) {
      const timestamps: Timestamp[] = [];
      let areTimestampsValid = true;

      for (const entry of this.decodedEntries) {
        const timestamp = this.getTimestamp(type, entry);
        if (timestamp === undefined) {
          areTimestampsValid = false;
          break;
        }
        timestamps.push(timestamp);
      }

      if (areTimestampsValid) {
        timeStampMap.set(type, timestamps);
      }
    }
    return timeStampMap;
  }

  abstract getTraceType(): TraceType;
}

export {AbstractParser};
