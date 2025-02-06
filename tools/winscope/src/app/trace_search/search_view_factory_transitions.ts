/*
 * Copyright (C) 2025 The Android Open Source Project
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
import {AbstractSearchViewFactory} from './abstract_search_view_factory';
import {SearchView} from './trace_search_initializer';

export class SearchViewFactoryTransitions extends AbstractSearchViewFactory {
  override readonly traceType = TraceType.TRANSITION;
  static readonly VIEW: SearchView = {
    name: 'transitions_search',
    dataType: 'Transitions',
    columns: [
      {
        name: 'ts',
        desc: 'Dispatch time - falls back to send time if available, else 0',
      },
      {
        name: 'transition_id',
        desc: 'Transition id',
      },
      {
        name: 'property',
        desc: 'Property name accounting for repeated fields',
      },
      {
        name: 'flat_property',
        desc: 'Property name not accounting for repeated fields',
      },
      {name: 'value', desc: 'Property value in string format'},
    ],
    examples: [
      {
        query: `SELECT
PROPS.ts,
PROPS.transition_id,
PROPS.property,
PROPS.value
FROM transitions_search HANDLER_MATCH
INNER JOIN transitions_search PROPS
ON HANDLER_MATCH.transition_id = PROPS.transition_id
WHERE HANDLER_MATCH.property = 'handler' AND HANDLER_MATCH.value LIKE "%DefaultMixedHandler"
ORDER BY PROPS.transition_id, PROPS.property`,
        desc: 'returns properties of transitions handled by DefaultMixedHandler',
      },
    ],
  };

  static getPossibleSearchViews(): SearchView[] {
    return [SearchViewFactoryTransitions.VIEW];
  }

  override async createSearchViews(): Promise<string[]> {
    const dataTable = await this.createSqlTableWithDefaults(
      '__intrinsic_window_manager_shell_transition_protos',
    );
    const sqlCreateTableWithTimestamps = `
      CREATE PERFETTO TABLE transition_protos_with_timestamps AS
        SELECT
          TRANS.ts,
          PROTOS.transition_id,
          PROTOS.base64_proto_id
        FROM __intrinsic_window_manager_shell_transition_protos PROTOS
        LEFT JOIN transitions_with_updated_ts TRANS
          ON TRANS.transition_id = PROTOS.transition_id;
    `;
    await this.traceProcessor.query(sqlCreateTableWithTimestamps);

    const sqlCreateViewTransitionsSearch = `
      CREATE PERFETTO VIEW ${SearchViewFactoryTransitions.VIEW.name} AS
        SELECT
          STATE.ts,
          STATE.transition_id,
          PROPERTY.key as property,
          PROPERTY.flat_key as flat_property,
          CASE
            WHEN (PROPERTY.key = 'handler' AND HANDLERS.handler_name IS NOT NULL) THEN HANDLERS.handler_name
            ELSE PROPERTY.display_value END
          AS value
        FROM transition_protos_with_timestamps STATE
        INNER JOIN ${dataTable} PROPERTY
          ON PROPERTY.base64_proto_id = STATE.base64_proto_id
        LEFT JOIN window_manager_shell_transition_handlers HANDLERS
          ON cast_string!(HANDLERS.handler_id) = PROPERTY.display_value
        ORDER BY STATE.transition_id, PROPERTY.key;
    `;
    await this.traceProcessor.query(sqlCreateViewTransitionsSearch);
    return [SearchViewFactoryTransitions.VIEW.name];
  }
}
