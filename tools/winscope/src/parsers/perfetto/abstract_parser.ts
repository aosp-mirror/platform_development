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
import {StringUtils} from 'common/string_utils';
import {ElapsedTimestamp, RealTimestamp, Timestamp, TimestampType} from 'common/time';
import {AbsoluteEntryIndex, EntriesRange} from 'trace/index_types';
import {Parser} from 'trace/parser';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {WasmEngineProxy} from 'trace_processor/wasm_engine_proxy';
import {FakeProtoBuilder} from './fake_proto_builder';

export abstract class AbstractParser<T> implements Parser<T> {
  protected traceProcessor: WasmEngineProxy;
  protected realToElapsedTimeOffsetNs?: bigint;
  private timestamps = new Map<TimestampType, Timestamp[]>();
  private lengthEntries = 0;
  private traceFile: TraceFile;

  constructor(traceFile: TraceFile, traceProcessor: WasmEngineProxy) {
    this.traceFile = traceFile;
    this.traceProcessor = traceProcessor;
  }

  async parse() {
    const elapsedTimestamps = await this.queryElapsedTimestamps();
    this.lengthEntries = elapsedTimestamps.length;
    assertTrue(
      this.lengthEntries > 0,
      () => `Trace processor tables don't contain entries of type ${this.getTraceType()}`
    );

    this.realToElapsedTimeOffsetNs = await this.queryRealToElapsedTimeOffset(
      assertDefined(elapsedTimestamps.at(-1))
    );

    this.timestamps.set(
      TimestampType.ELAPSED,
      elapsedTimestamps.map((value) => new ElapsedTimestamp(value))
    );

    this.timestamps.set(
      TimestampType.REAL,
      elapsedTimestamps.map(
        (value) => new RealTimestamp(value + assertDefined(this.realToElapsedTimeOffsetNs))
      )
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

  abstract getEntry(index: AbsoluteEntryIndex, timestampType: TimestampType): Promise<T>;

  async getPartialProtos(entriesRange: EntriesRange, fieldPath: string): Promise<object[]> {
    const fieldPathSnakeCase = StringUtils.convertCamelToSnakeCase(fieldPath);
    const sql = `
      SELECT
        tbl.id as entry_index,
        args.key,
        args.value_type,
        args.int_value,
        args.string_value,
        args.real_value
      FROM ${this.getTableName()} AS tbl
      INNER JOIN args ON tbl.arg_set_id = args.arg_set_id
      WHERE
        entry_index BETWEEN ${entriesRange.start} AND ${entriesRange.end - 1}
        AND (args.key = '${fieldPathSnakeCase}' OR args.key LIKE '${fieldPathSnakeCase}.%')
        ORDER BY entry_index;
    `;
    const result = await this.traceProcessor.query(sql).waitAllRows();

    const entries: object[] = [];
    for (const it = result.iter({}); it.valid(); it.next()) {
      const builder = new FakeProtoBuilder();
      builder.addArg(
        it.get('key') as string,
        it.get('value_type') as string,
        it.get('int_value') as bigint | undefined,
        it.get('real_value') as number | undefined,
        it.get('string_value') as string | undefined
      );
      entries.push(builder.build());
    }
    return entries;
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
  private async queryRealToElapsedTimeOffset(elapsedTimestamp: bigint): Promise<bigint> {
    const sql = `
      SELECT TO_REALTIME(${elapsedTimestamp}) as realtime;
    `;

    const result = await this.traceProcessor.query(sql).waitAllRows();
    assertTrue(result.numRows() === 1, () => 'Failed to query realtime timestamp');

    const real = result.iter({}).get('realtime') as bigint;
    return real - elapsedTimestamp;
  }

  private async queryLastClockSnapshot(clockName: string): Promise<bigint> {
    const sql = `
      SELECT
          snapshot_id, clock_name, clock_value
      FROM clock_snapshot
      WHERE
          snapshot_id = ( SELECT MAX(snapshot_id) FROM clock_snapshot )
      AND clock_name = '${clockName}'`;

    const result = await this.traceProcessor.query(sql).waitAllRows();
    assertTrue(result.numRows() === 1, () => "Failed to query clock '${clockName}'");
    return result.iter({}).get('clock_value') as bigint;
  }
}
