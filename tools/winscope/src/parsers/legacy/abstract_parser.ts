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
import {ParserTimestampConverter} from 'common/timestamp_converter';
import {CoarseVersion} from 'trace/coarse_version';
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

export abstract class AbstractParser<T extends object = object>
  implements Parser<T>
{
  private timestamps: Timestamp[] | undefined;
  protected traceFile: TraceFile;
  protected decodedEntries: any[] = [];
  protected timestampConverter: ParserTimestampConverter;

  protected abstract getMagicNumber(): undefined | number[];
  protected abstract decodeTrace(trace: Uint8Array): any[];
  protected abstract getTimestamp(decodedEntry: any): Timestamp;
  protected abstract processDecodedEntry(index: number, decodedEntry: any): any;

  constructor(trace: TraceFile, timestampConverter: ParserTimestampConverter) {
    this.traceFile = trace;
    this.timestampConverter = timestampConverter;
  }

  async parse() {
    const traceBuffer = new Uint8Array(await this.traceFile.file.arrayBuffer());
    ParsingUtils.throwIfMagicNumberDoesNotMatch(
      traceBuffer,
      this.getMagicNumber(),
    );
    this.decodedEntries = this.decodeTrace(traceBuffer);
  }

  getDescriptors(): string[] {
    return [this.traceFile.getDescriptor()];
  }

  getLengthEntries(): number {
    return this.decodedEntries.length;
  }

  createTimestamps() {
    this.timestamps = this.decodeTimestamps();
  }

  getTimestamps(): undefined | Timestamp[] {
    return this.timestamps;
  }

  getCoarseVersion(): CoarseVersion {
    return CoarseVersion.LEGACY;
  }

  getEntry(index: AbsoluteEntryIndex): Promise<T> {
    const entry = this.processDecodedEntry(index, this.decodedEntries[index]);
    return Promise.resolve(entry);
  }

  customQuery<Q extends CustomQueryType>(
    type: Q,
    entriesRange: EntriesRange,
    param?: CustomQueryParamTypeMap[Q],
  ): Promise<CustomQueryParserResultTypeMap[Q]> {
    throw new Error('Not implemented');
  }

  private decodeTimestamps(): Timestamp[] {
    return this.decodedEntries.map((entry) => this.getTimestamp(entry));
  }

  abstract getTraceType(): TraceType;
  abstract getRealToBootTimeOffsetNs(): bigint | undefined;
  abstract getRealToMonotonicTimeOffsetNs(): bigint | undefined;
}
