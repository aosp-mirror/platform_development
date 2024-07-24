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

import {assertTrue} from 'common/assert_utils';
import {EntriesRange} from 'trace/trace';
import {WasmEngineProxy} from 'trace_processor/wasm_engine_proxy';
import {FakeProto, FakeProtoBuilder} from './fake_proto_builder';

export class Utils {
  static async queryEntry(
    traceProcessor: WasmEngineProxy,
    tableName: string,
    index: number,
  ): Promise<FakeProto> {
    const sql = `
      SELECT
          tbl.id AS trace_entry_id,
          args.key,
          args.value_type,
          args.int_value,
          args.string_value,
          args.real_value
      FROM ${tableName} AS tbl
      INNER JOIN args ON tbl.arg_set_id = args.arg_set_id
      WHERE trace_entry_id = ${index};
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
    entriesRange: EntriesRange,
  ): Promise<Array<bigint>> {
    const sql = `
      SELECT
        tbl.id as entry_index,
        args.key,
        args.value_type,
        args.int_value
      FROM ${tableName} AS tbl
      INNER JOIN args ON tbl.arg_set_id = args.arg_set_id
      WHERE
        entry_index BETWEEN ${entriesRange.start} AND ${entriesRange.end - 1}
        AND args.key = 'vsync_id'
        ORDER BY entry_index;
    `;

    const result = await traceProcessor.query(sql).waitAllRows();

    const values: Array<bigint> = [];
    for (const it = result.iter({}); it.valid(); it.next()) {
      const value = it.get('int_value') as bigint | undefined;
      const valueType = it.get('value_type') as string;
      assertTrue(
        valueType === 'uint' || valueType === 'int',
        () => 'expected vsyncid to have integer type',
      );
      values.push(value ?? -1n);
    }
    return values;
  }
}
