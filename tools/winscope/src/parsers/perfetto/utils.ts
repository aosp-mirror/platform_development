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

import {
  assertBigInt,
  assertBigIntOrUndefined,
  assertNumberOrUndefined,
  assertString,
  assertStringOrUndefined,
  assertTrue,
} from 'common/assert_utils';
import {UserNotifier} from 'common/user_notifier';
import {MissingVsyncId} from 'messaging/user_warnings';
import {AbsoluteEntryIndex, EntriesRange} from 'trace/trace';
import {TraceProcessor} from 'trace_processor/trace_processor';
import {FakeProto, FakeProtoBuilder} from './fake_proto_builder';

export async function queryArgs(
  traceProcessor: TraceProcessor,
  argSetId: number,
): Promise<FakeProto> {
  const sql = `
      SELECT
          key,
          value_type,
          int_value,
          string_value,
          real_value
      FROM args WHERE args.arg_set_id = ${argSetId};
    `;
  return getAndConvertArgsToProto(traceProcessor, sql);
}

export async function queryEntry(
  traceProcessor: TraceProcessor,
  tableName: string,
  entryIndexToRowIdMap: number[],
  entryIndex: AbsoluteEntryIndex,
): Promise<FakeProto> {
  const rowId = entryIndexToRowIdMap[entryIndex];
  const sql = `
      SELECT
          tbl.id,
          args.key,
          args.value_type,
          args.int_value,
          args.string_value,
          args.real_value
      FROM ${tableName} AS tbl
      INNER JOIN args ON tbl.arg_set_id = args.arg_set_id
      WHERE tbl.id = ${rowId};
    `;
  return getAndConvertArgsToProto(traceProcessor, sql);
}

async function getAndConvertArgsToProto(
  traceProcessor: TraceProcessor,
  sql: string,
): Promise<FakeProto> {
  const result = await traceProcessor.query(sql);
  const builder = new FakeProtoBuilder();
  for (const it = result.iter({}); it.valid(); it.next()) {
    builder.addArg(
      assertString(it.get('key')),
      assertString(it.get('value_type')),
      assertBigIntOrUndefined(it.get('int_value')),
      assertNumberOrUndefined(it.get('real_value')),
      assertStringOrUndefined(it.get('string_value')),
    );
  }
  return builder.build();
}

export async function queryVsyncId(
  traceProcessor: TraceProcessor,
  tableName: string,
  entryIndexToRowIdMap: number[],
  entriesRange: EntriesRange,
  createVsyncIdQuery: (
    tableName: string,
    minRowId: number,
    maxRowId: number,
  ) => string = createDefaultVsyncIdQuery,
): Promise<Array<bigint>> {
  let minRowId = Number.MAX_VALUE;
  let maxRowId = Number.MIN_VALUE;
  for (
    let entryIndex = entriesRange.start;
    entryIndex < entriesRange.end;
    ++entryIndex
  ) {
    const rowId = entryIndexToRowIdMap[entryIndex];
    minRowId = Math.min(minRowId, rowId);
    maxRowId = Math.max(maxRowId, rowId);
  }
  const numEntries = maxRowId - minRowId + 1;

  const sql = createVsyncIdQuery(tableName, minRowId, maxRowId);
  const result = await traceProcessor.query(sql);

  const vsyncIdOrderedByRow: Array<bigint> = [];
  let curRowId = BigInt(minRowId);
  for (const it = result.iter({}); it.valid(); it.next()) {
    const id = assertBigInt(it.get('id'));
    while (curRowId < id) {
      // Handle missing table rows that don't have a vsync_id
      vsyncIdOrderedByRow.push(-1n);
      curRowId++;
    }
    assertTrue(
      curRowId === id,
      () => 'query for vsyncId contains duplicate rows with the same id',
    );
    const value = assertBigIntOrUndefined(it.get('int_value'));
    const valueType = assertString(it.get('value_type'));
    assertTrue(
      valueType === 'uint' || valueType === 'int',
      () => 'expected vsync_id to have integer type',
    );
    vsyncIdOrderedByRow.push(value ?? -1n);
    curRowId++;
  }
  while (curRowId <= maxRowId) {
    // Handle missing table rows at the end of the trace
    vsyncIdOrderedByRow.push(-1n);
    curRowId++;
  }

  if (vsyncIdOrderedByRow.length !== numEntries) {
    UserNotifier.add(new MissingVsyncId(tableName));
  }

  const vsyncIdOrderedByEntry: Array<bigint> = [];
  for (
    let entryIndex = entriesRange.start;
    entryIndex < entriesRange.end;
    ++entryIndex
  ) {
    const rowId = entryIndexToRowIdMap[entryIndex];
    const vsyncId = vsyncIdOrderedByRow[rowId - minRowId];
    vsyncIdOrderedByEntry.push(vsyncId);
  }

  return vsyncIdOrderedByEntry;
}

// Creates a sql query for the vsync_id of the table rows that have
// an id in the range [minRowId, maxRowId]. The query may be created in a way
// where rows that don't have a vsync_id can be omitted from the query result.
function createDefaultVsyncIdQuery(
  tableName: string,
  minRowId: number,
  maxRowId: number,
): string {
  return `
      SELECT
        tbl.id AS id,
        args.key,
        args.value_type,
        args.int_value
      FROM ${tableName} AS tbl
      INNER JOIN args ON tbl.arg_set_id = args.arg_set_id
      WHERE
        tbl.id BETWEEN ${minRowId} AND ${maxRowId}
        AND args.key = 'vsync_id'
        ORDER BY tbl.id;
    `;
}

export async function getDistinctValues(
  traceProcessor: TraceProcessor,
  tableName: string,
  columns: string[],
): Promise<string[]> {
  const uniqueValueCol = 'unique_value';
  const sql =
    columns
      .map((col) => {
        return `SELECT DISTINCT ${col} AS ${uniqueValueCol} FROM ${tableName}`;
      })
      .join(' UNION ') + ` ORDER BY ${uniqueValueCol}`;

  const rows = await traceProcessor.query(sql);
  if (rows.numRows() === 0) {
    return [];
  }

  const options: string[] = [];
  for (const it = rows.iter({}); it.valid(); it.next()) {
    const val = it.get(uniqueValueCol);
    const option = val !== null && val !== undefined ? val.toString() : 'N/A';
    options.push(option);
  }
  return options;
}
