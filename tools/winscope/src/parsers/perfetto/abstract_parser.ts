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
import {Parser} from 'trace/parser';
import {ElapsedTimestamp, RealTimestamp, Timestamp, TimestampType} from 'trace/timestamp';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {WasmEngineProxy} from 'trace_processor/wasm_engine_proxy';

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

    this.realToElapsedTimeOffsetNs = await this.queryRealToElapsedTimeOffset();

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

  abstract getEntry(index: number, timestampType: TimestampType): Promise<T>;

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

  private async queryRealToElapsedTimeOffset(): Promise<bigint> {
    const elapsed = await this.queryLastClockSnapshot('BOOTTIME');
    const real = await this.queryLastClockSnapshot('REALTIME');
    return real - elapsed;
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
