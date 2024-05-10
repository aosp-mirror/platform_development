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
import {TimestampType} from 'common/time';
import {LayerTraceEntry} from 'flickerlib/layers/LayerTraceEntry';
import {winscopeJson} from 'parsers/proto_types';
import {
  CustomQueryParserResultTypeMap,
  CustomQueryType,
  VisitableParserCustomQuery,
} from 'trace/custom_query';
import {EntriesRange} from 'trace/trace';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {WasmEngineProxy} from 'trace_processor/wasm_engine_proxy';
import {AbstractParser} from './abstract_parser';
import {FakeProto, FakeProtoBuilder} from './fake_proto_builder';
import {FakeProtoTransformer} from './fake_proto_transformer';
import {Utils} from './utils';

export class ParserSurfaceFlinger extends AbstractParser<LayerTraceEntry> {
  private layersSnapshotProtoTransformer = new FakeProtoTransformer(
    winscopeJson,
    'WinscopeTraceData',
    'layersSnapshot'
  );
  private layerProtoTransformer = new FakeProtoTransformer(winscopeJson, 'LayersProto', 'layers');

  constructor(traceFile: TraceFile, traceProcessor: WasmEngineProxy) {
    super(traceFile, traceProcessor);
  }

  override getTraceType(): TraceType {
    return TraceType.SURFACE_FLINGER;
  }

  override async getEntry(index: number, timestampType: TimestampType): Promise<LayerTraceEntry> {
    let snapshotProto = await Utils.queryEntry(this.traceProcessor, this.getTableName(), index);
    snapshotProto = this.layersSnapshotProtoTransformer.transform(snapshotProto);
    const layerProtos = (await this.querySnapshotLayers(index)).map((layerProto) =>
      this.layerProtoTransformer.transform(layerProto)
    );
    return LayerTraceEntry.fromProto(
      layerProtos,
      snapshotProto.displays,
      BigInt(snapshotProto.elapsedRealtimeNanos.toString()),
      snapshotProto.vsyncId,
      snapshotProto.hwcBlob,
      snapshotProto.where,
      this.realToElapsedTimeOffsetNs,
      timestampType === TimestampType.ELAPSED /*useElapsedTime*/,
      snapshotProto.excludesCompositionState ?? false
    );
  }

  override async customQuery<Q extends CustomQueryType>(
    type: Q,
    entriesRange: EntriesRange
  ): Promise<CustomQueryParserResultTypeMap[Q]> {
    return new VisitableParserCustomQuery(type)
      .visit(CustomQueryType.VSYNCID, async () => {
        return Utils.queryVsyncId(this.traceProcessor, this.getTableName(), entriesRange);
      })
      .visit(CustomQueryType.SF_LAYERS_ID_AND_NAME, async () => {
        const sql = `
        SELECT DISTINCT group_concat(value) AS id_and_name FROM (
          SELECT sfl.id AS id, args.key AS key, args.display_value AS value
          FROM surfaceflinger_layer AS sfl
          INNER JOIN args ON sfl.arg_set_id = args.arg_set_id
          WHERE (args.key = 'id' OR args.key = 'name')
          ORDER BY key
        )
        GROUP BY id;
      `;
        const querResult = await this.traceProcessor.query(sql).waitAllRows();
        const result: CustomQueryParserResultTypeMap[CustomQueryType.SF_LAYERS_ID_AND_NAME] = [];
        for (const it = querResult.iter({}); it.valid(); it.next()) {
          const idAndName = it.get('id_and_name') as string;
          const indexDelimiter = idAndName.indexOf(',');
          assertTrue(indexDelimiter > 0, () => `Unexpected value in query result: ${idAndName}`);
          const id = Number(idAndName.slice(0, indexDelimiter));
          const name = idAndName.slice(indexDelimiter + 1);
          result.push({id, name});
        }
        return result;
      })
      .getResult();
  }

  protected override getTableName(): string {
    return 'surfaceflinger_layers_snapshot';
  }

  private async querySnapshotLayers(index: number): Promise<FakeProto[]> {
    const layerIdToBuilder = new Map<number, FakeProtoBuilder>();
    const getBuilder = (layerId: number) => {
      if (!layerIdToBuilder.has(layerId)) {
        layerIdToBuilder.set(layerId, new FakeProtoBuilder());
      }
      return assertDefined(layerIdToBuilder.get(layerId));
    };

    const sql = `
      SELECT
          sfl.snapshot_id,
          sfl.id as layer_id,
          args.key,
          args.value_type,
          args.int_value,
          args.string_value,
          args.real_value
      FROM
          surfaceflinger_layer as sfl
          INNER JOIN args ON sfl.arg_set_id = args.arg_set_id
      WHERE snapshot_id = ${index};
    `;
    const result = await this.traceProcessor.query(sql).waitAllRows();

    for (const it = result.iter({}); it.valid(); it.next()) {
      const builder = getBuilder(it.get('layer_id') as number);
      builder.addArg(
        it.get('key') as string,
        it.get('value_type') as string,
        it.get('int_value') as bigint | undefined,
        it.get('real_value') as number | undefined,
        it.get('string_value') as string | undefined
      );
    }

    const layerProtos: FakeProto[] = [];
    layerIdToBuilder.forEach((builder) => {
      layerProtos.push(builder.build());
    });

    return layerProtos;
  }
}
