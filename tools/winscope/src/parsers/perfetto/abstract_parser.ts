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
import {WasmEngineProxy} from 'trace_processor/wasm_engine_proxy';

export abstract class AbstractParser<T> implements Parser<T> {
  protected traceProcessor: WasmEngineProxy;
  protected realToElapsedTimeOffsetNs?: bigint;
  protected timestampFactory: TimestampFactory;
  private timestamps = new Map<TimestampType, Timestamp[]>();
  private lengthEntries = 0;
  private traceFile: TraceFile;

  constructor(
    traceFile: TraceFile,
    traceProcessor: WasmEngineProxy,
    timestampFactory: TimestampFactory,
  ) {
    this.traceFile = traceFile;
    this.traceProcessor = traceProcessor;
    this.timestampFactory = timestampFactory;
  }

  async parse() {
    const elapsedTimestamps = await this.queryElapsedTimestamps();
    this.lengthEntries = elapsedTimestamps.length;
    assertTrue(
      this.lengthEntries > 0,
      () =>
        `Trace processor tables don't contain entries of type ${this.getTraceType()}`,
    );

    this.realToElapsedTimeOffsetNs = await this.queryRealToElapsedTimeOffset(
      assertDefined(elapsedTimestamps.at(-1)),
    );

    this.timestamps.set(
      TimestampType.ELAPSED,
      elapsedTimestamps.map((value) =>
        this.timestampFactory.makeElapsedTimestamp(value),
      ),
    );

    this.timestamps.set(
      TimestampType.REAL,
      elapsedTimestamps.map((value) =>
        this.timestampFactory.makeRealTimestamp(
          value,
          assertDefined(this.realToElapsedTimeOffsetNs),
        ),
      ),
    );

    if (this.lengthEntries > 0) {
      // Make sure there are trace entries that can be parsed
      await this.getEntry(0, TimestampType.ELAPSED);
    }
  }

  abstract getTraceType(): TraceType;

  getLengthEntries(): number {
    return this.lengthEntries;
  }

  getTimestamps(type: TimestampType): Timestamp[] | undefined {
    return this.timestamps.get(type);
  }

  abstract getEntry(
    index: AbsoluteEntryIndex,
    timestampType: TimestampType,
  ): Promise<T>;

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

  protected abstract getTableName(): string;

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
}
