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

import {assertDefined, assertTrue} from 'common/assert_utils';
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
import {WasmEngineProxy} from 'trace_processor/wasm_engine_proxy';

export abstract class AbstractParser<T> implements Parser<T> {
  protected traceProcessor: WasmEngineProxy;
  protected realToBootTimeOffsetNs?: bigint;
  protected timestampConverter: ParserTimestampConverter;
  private timestamps: Timestamp[] | undefined;
  private lengthEntries = 0;
  private traceFile: TraceFile;
  private elapsedTimestampsNs: Array<bigint> = [];

  protected abstract queryEntry(index: AbsoluteEntryIndex): Promise<any>;
  protected abstract getTableName(): string;

  constructor(
    traceFile: TraceFile,
    traceProcessor: WasmEngineProxy,
    timestampConverter: ParserTimestampConverter,
  ) {
    this.traceFile = traceFile;
    this.traceProcessor = traceProcessor;
    this.timestampConverter = timestampConverter;
  }

  async parse() {
    this.elapsedTimestampsNs = await this.queryElapsedTimestamps();
    this.lengthEntries = this.elapsedTimestampsNs.length;
    assertTrue(
      this.lengthEntries > 0,
      () =>
        `Trace processor tables don't contain entries of type ${this.getTraceType()}`,
    );

    let finalNonZeroNsIndex = -1;
    for (let i = this.elapsedTimestampsNs.length - 1; i > -1; i--) {
      if (this.elapsedTimestampsNs[i] !== 0n) {
        finalNonZeroNsIndex = i;
        break;
      }
    }
    this.realToBootTimeOffsetNs = await this.queryRealToElapsedTimeOffset(
      assertDefined(this.elapsedTimestampsNs.at(finalNonZeroNsIndex)),
    );

    if (this.lengthEntries > 0) {
      // Make sure there are trace entries that can be parsed
      await this.queryEntry(0);
    }
  }

  createTimestamps() {
    this.timestamps = this.elapsedTimestampsNs.map((ns) => {
      return this.timestampConverter.makeTimestampFromBootTimeNs(ns);
    });
  }

  getLengthEntries(): number {
    return this.lengthEntries;
  }

  getTimestamps(): Timestamp[] | undefined {
    return this.timestamps;
  }

  getCoarseVersion(): CoarseVersion {
    return CoarseVersion.LATEST;
  }

  customQuery<Q extends CustomQueryType>(
    type: Q,
    entriesRange: EntriesRange,
    param?: CustomQueryParamTypeMap[Q],
  ): Promise<CustomQueryParserResultTypeMap[Q]> {
    throw new Error('Not implemented');
  }

  getDescriptors(): string[] {
    return [this.traceFile.getDescriptor()];
  }

  getRealToMonotonicTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  getRealToBootTimeOffsetNs(): bigint | undefined {
    return this.realToBootTimeOffsetNs;
  }

  private async queryElapsedTimestamps(): Promise<Array<bigint>> {
    const sql = `SELECT ts FROM ${this.getTableName()} ORDER BY id;`;
    const result = await this.traceProcessor.query(sql).waitAllRows();
    const timestamps: Array<bigint> = [];
    for (const it = result.iter({}); it.valid(); it.next()) {
      timestamps.push(it.get('ts') as bigint);
    }
    return timestamps;
  }

  // Query the real-to-elapsed time offset at the specified time
  // (timestamp parameter).
  // The timestamp parameter must be a timestamp queried/provided by TP,
  // otherwise the TO_REALTIME() SQL function might return invalid values.
  private async queryRealToElapsedTimeOffset(
    elapsedTimestamp: bigint,
  ): Promise<bigint> {
    const sql = `
      SELECT TO_REALTIME(${elapsedTimestamp}) as realtime;
    `;

    const result = await this.traceProcessor.query(sql).waitAllRows();
    assertTrue(
      result.numRows() === 1,
      () => 'Failed to query realtime timestamp',
    );

    const real = result.iter({}).get('realtime') as bigint;
    return real - elapsedTimestamp;
  }

  abstract getEntry(index: AbsoluteEntryIndex): Promise<T>;
  abstract getTraceType(): TraceType;
}
