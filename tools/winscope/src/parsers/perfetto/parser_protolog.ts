/*
 * Copyright (C) 2024 The Android Open Source Project
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

import {assertDefined} from 'common/assert_utils';
import {ElapsedTimestamp, RealTimestamp, TimestampType} from 'common/time';
import {TimeUtils} from 'common/time_utils';
import {LogMessage} from 'trace/protolog';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {WasmEngineProxy} from 'trace_processor/wasm_engine_proxy';
import {AbstractParser} from './abstract_parser';

class PerfettoLogMessageTableRow {
  message = '<NO_MESSAGE>';
  tag = '<NO_TAG>';
  level = '<NO_LEVEL>';
  location = '<NO_LOC>';
  timestamp: bigint = 0n;

  constructor(timestamp: bigint, tag: string, level: string, message: string) {
    this.timestamp = timestamp ?? this.timestamp;
    this.tag = tag ?? this.tag;
    this.level = level ?? this.level;
    this.message = message ?? this.message;
  }
}

export class ParserProtolog extends AbstractParser<LogMessage> {
  constructor(traceFile: TraceFile, traceProcessor: WasmEngineProxy) {
    super(traceFile, traceProcessor);
  }

  override getTraceType(): TraceType {
    return TraceType.PROTO_LOG;
  }

  override async getEntry(index: number, timestampType: TimestampType): Promise<LogMessage> {
    const protologEntry = await this.queryProtoLogEntry(index);

    let time: string;
    let timestamp: bigint;
    const realToElapsedTimeOffsetNs = assertDefined(this.realToElapsedTimeOffsetNs);
    if (timestampType === TimestampType.REAL) {
      timestamp = protologEntry.timestamp + realToElapsedTimeOffsetNs;
      time = TimeUtils.format(new RealTimestamp(timestamp));
    } else {
      timestamp = protologEntry.timestamp;
      time = TimeUtils.format(new ElapsedTimestamp(timestamp));
    }

    return new LogMessage(
      protologEntry.message,
      time,
      protologEntry.tag,
      protologEntry.level,
      protologEntry.location,
      timestamp
    );
  }

  protected override getTableName(): string {
    return 'protolog';
  }

  private async queryProtoLogEntry(index: number): Promise<PerfettoLogMessageTableRow> {
    const sql = `
      SELECT
        ts, tag, level, message
      FROM
        protolog
      WHERE protolog.id = ${index};
    `;
    const result = await this.traceProcessor.query(sql).waitAllRows();

    if (result.numRows() !== 1) {
      throw new Error(
        `Expected exactly 1 protolog message with id ${index} but got ${result.numRows()}`
      );
    }

    const entry = result.iter({});

    return new PerfettoLogMessageTableRow(
      entry.get('ts') as bigint,
      entry.get('tag') as string,
      entry.get('level') as string,
      entry.get('message') as string
    );
  }
}
