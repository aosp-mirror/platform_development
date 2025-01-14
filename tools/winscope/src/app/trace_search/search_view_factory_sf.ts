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
import {AbstractSearchViewFactory} from './abstract_search_view_factory';
import {SearchView} from './trace_search_initializer';

export class SearchViewFactorySf extends AbstractSearchViewFactory {
  override readonly traceType = TraceType.SURFACE_FLINGER;
  private static readonly LAYER_VIEW: SearchView = {
    name: 'sf_layer_search',
    dataType: 'SurfaceFlinger layer',
    spec: [
      {
        name: 'state_id',
        desc: 'Unique id of entry to which layer belongs',
      },
      {name: 'ts', desc: 'Timestamp of entry to which layer belongs'},
      {name: 'layer_id', desc: 'Layer id'},
      {name: 'parent_id', desc: 'Layer id of parent'},
      {name: 'layer_name', desc: 'Layer name'},
      {
        name: 'property',
        desc: 'Property name accounting for repeated fields',
      },
      {
        name: 'flat_property',
        desc: 'Property name not accounting for repeated fields',
      },
      {name: 'value', desc: 'Property value in string format'},
      {
        name: 'previous_value',
        desc: 'Property value from previous entry in string format',
      },
    ],
    examples: [
      {
        query: `SELECT ts, value, previous_value FROM sf_layer_search
WHERE layer_name='Taskbar#97'
AND property='color.a'
AND value!=previous_value`,
        desc: 'returns timestamp, current and previous values of alpha for Taskbar#97, for states where alpha changed from previous state',
      },
      {
        query: `SELECT ts, value, previous_value FROM sf_layer_search
WHERE layer_name LIKE 'Wallpaper%'
AND property='bounds.bottom'
AND cast_int!(value) <= 2400`,
        desc: 'returns timestamp, current and previous values of bottom bound for layers that start with "Wallpaper", for states where bottom bound <= 2400',
      },
    ],
  };
  private static readonly ROOT_VIEW: SearchView = {
    name: 'sf_hierarchy_root_search',
    dataType: 'SurfaceFlinger root',
    spec: [
      {
        name: 'state_id',
        desc: 'Unique id of entry',
      },
      {name: 'ts', desc: 'Timestamp of entry'},
      {
        name: 'property',
        desc: 'Property name accounting for repeated fields',
      },
      {
        name: 'flat_property',
        desc: 'Property name not accounting for repeated fields',
      },
      {name: 'value', desc: 'Property value in string format'},
      {
        name: 'previous_value',
        desc: 'Property value from previous entry in string format',
      },
    ],
    examples: [
      {
        query: `SELECT STATE.* FROM sf_hierarchy_root_search STATE_WITH_DISPLAY_ON
INNER JOIN sf_hierarchy_root_search STATE
ON STATE.state_id = STATE_WITH_DISPLAY_ON.state_id
AND STATE_WITH_DISPLAY_ON.flat_property='displays.layer_stack'
AND STATE_WITH_DISPLAY_ON.value!='4294967295'
AND STATE.property LIKE CONCAT(
  SUBSTRING(
      STATE_WITH_DISPLAY_ON.property,
      0,
      instr(STATE_WITH_DISPLAY_ON.property, ']')
  ),
  '%'
)`,
        desc: 'returns all properties for displays with valid layer stack from all states',
      },
    ],
  };

  static getPossibleSearchViews(): SearchView[] {
    return [SearchViewFactorySf.LAYER_VIEW, SearchViewFactorySf.ROOT_VIEW];
  }

  override async createSearchViews(): Promise<string[]> {
    const layerArgsTable = await this.createSqlTableWithDefaults(
      'surfaceflinger_layer',
    );
    const snapshotArgsTable = await this.createSqlTableWithDefaults(
      'surfaceflinger_layers_snapshot',
    );

    const sqlCreateTableSfStateChanges = `
            CREATE PERFETTO TABLE sf_state_changes AS
              SELECT
                STATE.id as state_id,
                (SELECT X.id
                  FROM surfaceflinger_layers_snapshot X
                WHERE X.ts < STATE.ts
                ORDER BY X.ts DESC
                LIMIT 1
                ) as previous_state_id
              FROM surfaceflinger_layers_snapshot STATE;
          `;
    await this.traceProcessor.query(sqlCreateTableSfStateChanges);

    const sqlCreateTableSfLayerIdentifier = `
            CREATE PERFETTO TABLE sf_layer_identifier AS
              SELECT
                LAYER.snapshot_id as state_id,
                LAYER_ID.int_value as layer_id,
                PARENT_PROPERTY.int_value as parent_id,
                NAME.string_value as layer_name,
                LAYER.base64_proto_id
              FROM surfaceflinger_layer LAYER
              INNER JOIN ${layerArgsTable} LAYER_ID ON LAYER_ID.base64_proto_id = LAYER.base64_proto_id AND LAYER_ID.key = 'id'
              INNER JOIN ${layerArgsTable} PARENT_PROPERTY ON PARENT_PROPERTY.base64_proto_id = LAYER.base64_proto_id AND PARENT_PROPERTY.key = 'parent'
              INNER JOIN ${layerArgsTable} NAME ON NAME.base64_proto_id = LAYER.base64_proto_id AND NAME.key = 'name';
          `;
    await this.traceProcessor.query(sqlCreateTableSfLayerIdentifier);

    const sqlCreateViewSfLayerWithProperties = `
            CREATE PERFETTO VIEW sf_layer_with_properties AS
              SELECT
                STATE.id as state_id,
                STATE.ts,
                LAYER.layer_id,
                LAYER.parent_id,
                LAYER.layer_name,
                PROPERTY.key as property,
                PROPERTY.flat_key as flat_property,
                PROPERTY.display_value as value
              FROM surfaceflinger_layers_snapshot STATE
              INNER JOIN sf_layer_identifier LAYER ON LAYER.state_id = STATE.id
              INNER JOIN ${layerArgsTable} PROPERTY ON PROPERTY.base64_proto_id = LAYER.base64_proto_id;
          `;
    await this.traceProcessor.query(sqlCreateViewSfLayerWithProperties);

    const sqlCreateViewSfLayerSearch = `
            CREATE PERFETTO VIEW ${SearchViewFactorySf.LAYER_VIEW.name}(
              state_id INT,
              ts INT,
              layer_id INT,
              parent_id INT,
              layer_name STRING,
              property STRING,
              flat_property STRING,
              value STRING,
              previous_value STRING
            ) AS
            SELECT
              CURRENT.state_id,
              CURRENT.ts,
              CURRENT.layer_id,
              CURRENT.parent_id,
              CURRENT.layer_name,
              CURRENT.property,
              CURRENT.flat_property,
              CURRENT.value,
              PREVIOUS.value as previous_value
            FROM sf_state_changes CHANGE
            INNER JOIN sf_layer_with_properties CURRENT ON CURRENT.state_id = CHANGE.state_id
            INNER JOIN sf_layer_with_properties PREVIOUS ON PREVIOUS.state_id = CHANGE.previous_state_id AND PREVIOUS.layer_id = CURRENT.layer_id AND PREVIOUS.property = CURRENT.property
            ORDER BY CURRENT.ts;
          `;
    await this.traceProcessor.query(sqlCreateViewSfLayerSearch);

    const sqlCreateViewSfEntry = `
            CREATE PERFETTO VIEW sf_entry AS
              SELECT
                STATE.id as state_id,
                STATE.ts,
                PROPERTY.key as property,
                PROPERTY.flat_key as flat_property,
                PROPERTY.display_value as value
              FROM surfaceflinger_layers_snapshot STATE
              INNER JOIN ${snapshotArgsTable} PROPERTY ON PROPERTY.base64_proto_id = STATE.base64_proto_id;
          `;
    await this.traceProcessor.query(sqlCreateViewSfEntry);

    const sqlCreateViewSfEntrySearch = `
            CREATE PERFETTO VIEW ${SearchViewFactorySf.ROOT_VIEW.name} AS
              SELECT
                STATE.*,
                PREVIOUS.value as previous_value
              FROM sf_entry STATE
              INNER JOIN sf_state_changes STATE_CHANGES ON STATE_CHANGES.state_id = STATE.state_id
              LEFT JOIN sf_entry PREVIOUS ON PREVIOUS.state_id = STATE_CHANGES.previous_state_id
                                                     AND PREVIOUS.property = STATE.property
            ORDER BY STATE.ts;
          `;
    await this.traceProcessor.query(sqlCreateViewSfEntrySearch);

    return [
      SearchViewFactorySf.LAYER_VIEW.name,
      SearchViewFactorySf.ROOT_VIEW.name,
    ];
  }
}
