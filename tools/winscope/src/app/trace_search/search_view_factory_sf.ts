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

export class SearchViewFactorySf extends AbstractSearchViewFactory {
  override readonly traceType = TraceType.SURFACE_FLINGER;

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

    const layerSearchView = 'sf_layer_search';
    const sqlCreateViewSfLayerSearch = `
            CREATE PERFETTO VIEW ${layerSearchView}(
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
            INNER JOIN sf_layer_with_properties PREVIOUS ON PREVIOUS.state_id = CHANGE.previous_state_id AND PREVIOUS.layer_id = CURRENT.layer_id AND PREVIOUS.property = CURRENT.property;
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

    const entrySearchView = 'sf_entry_search';
    const sqlCreateViewSfEntrySearch = `
            CREATE PERFETTO VIEW ${entrySearchView} AS
              SELECT
                STATE.*,
                PREVIOUS.value as previous_value
              FROM sf_entry STATE
              INNER JOIN sf_state_changes STATE_CHANGES ON STATE_CHANGES.state_id = STATE.state_id
              LEFT JOIN sf_entry PREVIOUS ON PREVIOUS.state_id = STATE_CHANGES.previous_state_id
                                                     AND PREVIOUS.property = STATE.property;
          `;
    await this.traceProcessor.query(sqlCreateViewSfEntrySearch);

    return [layerSearchView, entrySearchView];
  }
}
