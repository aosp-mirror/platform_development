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
import {AbsoluteEntryIndex, EntriesRange} from 'trace/trace';
import {WasmEngineProxy} from 'trace_processor/wasm_engine_proxy';
import {FakeProto, FakeProtoBuilder} from './fake_proto_builder';

export class Utils {
  static async queryEntry(
    traceProcessor: WasmEngineProxy,
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
    const result = await traceProcessor.query(sql).waitAllRows();

    const builder = new FakeProtoBuilder();
    for (const it = result.iter({}); it.valid(); it.next()) {
      builder.addArg(
        it.get('key') as string,
        it.get('value_type') as string,
        it.get('int_value') as bigint | undefined,
        it.get('real_value') as number | undefined,
        it.get('string_value') as string | undefined,
      );
    }
    return builder.build();
  }

  static async queryVsyncId(
    traceProcessor: WasmEngineProxy,
    tableName: string,
    entryIndexToRowIdMap: number[],
    entriesRange: EntriesRange,
    createVsyncIdQuery: (
      tableName: string,
      minRowId: number,
      maxRowId: number,
    ) => string = Utils.createDefaultVsyncIdQuery,
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
    const result = await traceProcessor.query(sql).waitAllRows();

    const vsyncIdOrderedByRow: Array<bigint> = [];
    let curRowId = BigInt(minRowId);
    for (const it = result.iter({}); it.valid(); it.next()) {
      const id = assertDefined(it.get('id') as bigint | undefined);
      while (curRowId < id) {
        // Handle missing table rows that don't have a vsync_id
        vsyncIdOrderedByRow.push(-1n);
        curRowId++;
      }
      assertTrue(
        curRowId === id,
        () => 'query for vsyncId contains duplicate rows with the same id',
      );
      const value = it.get('int_value') as bigint | undefined;
      const valueType = it.get('value_type') as string;
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

    assertTrue(
      vsyncIdOrderedByRow.length === numEntries,
      () => 'missing vsync_id value for one or more entries',
    );

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
  private static createDefaultVsyncIdQuery(
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
}
