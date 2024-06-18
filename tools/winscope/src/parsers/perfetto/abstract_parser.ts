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
import {TRACE_INFO} from 'trace/trace_info';
import {TraceType} from 'trace/trace_type';
import {WasmEngineProxy} from 'trace_processor/wasm_engine_proxy';

export abstract class AbstractParser<T> implements Parser<T> {
  protected traceProcessor: WasmEngineProxy;
  protected realToBootTimeOffsetNs?: bigint;
  protected timestampConverter: ParserTimestampConverter;
  protected entryIndexToRowIdMap: number[] = [];

  private lengthEntries = 0;
  private traceFile: TraceFile;
  private bootTimeTimestampsNs: Array<bigint> = [];
  private timestamps: Timestamp[] | undefined;

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
    const module = this.getStdLibModuleName();
    if (module) {
      await this.traceProcessor.query(`INCLUDE PERFETTO MODULE ${module};`);
    }

    this.entryIndexToRowIdMap = await this.buildEntryIndexToRowIdMap();
    const rowBootTimeTimestampsNs = await this.queryRowBootTimeTimestamps();
    this.bootTimeTimestampsNs = this.entryIndexToRowIdMap.map(
      (rowId) => rowBootTimeTimestampsNs[rowId],
    );
    this.lengthEntries = this.bootTimeTimestampsNs.length;
    assertTrue(
      this.lengthEntries > 0,
      () =>
        `Perfetto trace has no ${TRACE_INFO[this.getTraceType()].name} entries`,
    );

    let lastNonZeroTimestamp: bigint | undefined;
    for (let i = this.bootTimeTimestampsNs.length - 1; i >= 0; i--) {
      if (this.bootTimeTimestampsNs[i] !== 0n) {
        lastNonZeroTimestamp = this.bootTimeTimestampsNs[i];
        break;
      }
    }
    this.realToBootTimeOffsetNs = await this.queryRealToBootTimeOffset(
      assertDefined(lastNonZeroTimestamp),
    );
  }

  createTimestamps() {
    this.timestamps = this.bootTimeTimestampsNs.map((ns) => {
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

  protected async buildEntryIndexToRowIdMap(): Promise<AbsoluteEntryIndex[]> {
    const sqlRowIdAndTimestamp = `
     SELECT DISTINCT tbl.id AS id, tbl.ts
     FROM ${this.getTableName()} AS tbl
     ORDER BY tbl.ts;
   `;
    const result = await this.traceProcessor
      .query(sqlRowIdAndTimestamp)
      .waitAllRows();
    const entryIndexToRowId: AbsoluteEntryIndex[] = [];
    for (const it = result.iter({}); it.valid(); it.next()) {
      const rowId = Number(it.get('id') as bigint);
      entryIndexToRowId.push(rowId);
    }
    return entryIndexToRowId;
  }

  async queryRowBootTimeTimestamps(): Promise<Array<bigint>> {
    const sql = `SELECT ts FROM ${this.getTableName()} ORDER BY id;`;
    const result = await this.traceProcessor.query(sql).waitAllRows();
    const timestamps: Array<bigint> = [];
    for (const it = result.iter({}); it.valid(); it.next()) {
      timestamps.push(it.get('ts') as bigint);
    }
    return timestamps;
  }

  // Query the real-to-boot time offset at the specified time
  // (timestamp parameter).
  // The timestamp parameter must be a non-zero timestamp queried/provided by TP,
  // otherwise the TO_REALTIME() SQL function might return invalid values.
  private async queryRealToBootTimeOffset(bootTimeNs: bigint): Promise<bigint> {
    const sql = `
      SELECT TO_REALTIME(${bootTimeNs}) as realtime;
    `;

    const result = await this.traceProcessor.query(sql).waitAllRows();
    assertTrue(
      result.numRows() === 1,
      () => 'Failed to query realtime timestamp',
    );

    const real = result.iter({}).get('realtime') as bigint;
    return real - bootTimeNs;
  }

  protected getStdLibModuleName(): string | undefined {
    return undefined;
  }

  protected abstract getTableName(): string;
  abstract getEntry(index: AbsoluteEntryIndex): Promise<T>;
  abstract getTraceType(): TraceType;
}
