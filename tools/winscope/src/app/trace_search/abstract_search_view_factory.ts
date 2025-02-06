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

import {TraceType} from 'trace/trace_type';
import {TraceProcessor} from 'trace_processor/trace_processor';

export abstract class AbstractSearchViewFactory {
  abstract readonly traceType: TraceType;

  constructor(protected traceProcessor: TraceProcessor) {}

  protected async createSqlTableWithDefaults(
    tableName: string,
  ): Promise<string> {
    const newTableName = `${tableName}_with_defaults`;
    const sql = `
      CREATE PERFETTO TABLE ${newTableName}(
        id INT,
        base64_proto_id INT,
        flat_key STRING,
        key STRING,
        int_value LONG,
        string_value STRING,
        real_value DOUBLE,
        display_value STRING
      ) AS
      SELECT
      id,
      base64_proto_id,
      flat_key,
      key,
      int_value,
      string_value,
      real_value,
      CASE
        WHEN int_value IS NOT NULL THEN cast_string!(int_value)
        WHEN string_value IS NOT NULL THEN string_value
        WHEN real_value IS NOT NULL THEN cast_string!(real_value)
        ELSE NULL END
      AS display_value
      FROM __intrinsic_winscope_proto_to_args_with_defaults('${tableName}');
    `;
    await this.traceProcessor.query(sql);
    return newTableName;
  }

  abstract createSearchViews(): Promise<string[]>;
}
