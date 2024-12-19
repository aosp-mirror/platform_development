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
import {ParserTimestampConverter} from 'common/time/timestamp_converter';
import {AddDefaults} from 'parsers/operations/add_defaults';
import {SetFormatters} from 'parsers/operations/set_formatters';
import {TranslateIntDef} from 'parsers/operations/translate_intdef';
import {AbstractParser} from 'parsers/perfetto/abstract_parser';
import {FakeProtoBuilder} from 'parsers/perfetto/fake_proto_builder';
import {FakeProtoTransformer} from 'parsers/perfetto/fake_proto_transformer';
import {Utils} from 'parsers/perfetto/utils';
import {DENYLIST_PROPERTIES} from 'parsers/surface_flinger/denylist_properties';
import {EAGER_PROPERTIES} from 'parsers/surface_flinger/eager_properties';
import {EntryHierarchyTreeFactory} from 'parsers/surface_flinger/entry_hierarchy_tree_factory';
import {TamperedMessageType} from 'parsers/tampered_message_type';
import root from 'protos/surfaceflinger/latest/json';
import {perfetto} from 'protos/surfaceflinger/latest/static';
import {
  CustomQueryParserResultTypeMap,
  CustomQueryType,
  VisitableParserCustomQuery,
} from 'trace/custom_query';
import {EntriesRange} from 'trace/trace';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {EnumFormatter, LAYER_ID_FORMATTER} from 'trace/tree_node/formatters';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {WasmEngineProxy} from 'trace_processor/wasm_engine_proxy';

export class ParserSurfaceFlinger extends AbstractParser<HierarchyTreeNode> {
  private static readonly CUSTOM_FORMATTERS = new Map([
    ['cropLayerId', LAYER_ID_FORMATTER],
    ['zOrderRelativeOf', LAYER_ID_FORMATTER],
    [
      'hwcCompositionType',
      new EnumFormatter(perfetto.protos.HwcCompositionType),
    ],
  ]);

  private static readonly LayersTraceFileProto = TamperedMessageType.tamper(
    root.lookupType('perfetto.protos.LayersTraceFileProto'),
  );
  private static readonly entryField =
    ParserSurfaceFlinger.LayersTraceFileProto.fields['entry'];
  private static readonly layerField = assertDefined(
    ParserSurfaceFlinger.entryField.tamperedMessageType?.fields['layers']
      .tamperedMessageType,
  ).fields['layers'];

  static readonly Operations = {
    SetFormattersLayer: new SetFormatters(
      ParserSurfaceFlinger.layerField,
      ParserSurfaceFlinger.CUSTOM_FORMATTERS,
    ),
    TranslateIntDefLayer: new TranslateIntDef(ParserSurfaceFlinger.layerField),
    AddDefaultsLayerEager: new AddDefaults(
      ParserSurfaceFlinger.layerField,
      EAGER_PROPERTIES,
    ),
    AddDefaultsLayerLazy: new AddDefaults(
      ParserSurfaceFlinger.layerField,
      undefined,
      EAGER_PROPERTIES.concat(DENYLIST_PROPERTIES),
    ),
    SetFormattersEntry: new SetFormatters(
      ParserSurfaceFlinger.entryField,
      ParserSurfaceFlinger.CUSTOM_FORMATTERS,
    ),
    TranslateIntDefEntry: new TranslateIntDef(ParserSurfaceFlinger.entryField),
    AddDefaultsEntryEager: new AddDefaults(ParserSurfaceFlinger.entryField, [
      'displays',
    ]),
    AddDefaultsEntryLazy: new AddDefaults(
      ParserSurfaceFlinger.entryField,
      undefined,
      DENYLIST_PROPERTIES,
    ),
  };

  private readonly factory = new EntryHierarchyTreeFactory();
  private layersSnapshotProtoTransformer: FakeProtoTransformer;
  private layerProtoTransformer: FakeProtoTransformer;

  constructor(
    traceFile: TraceFile,
    traceProcessor: WasmEngineProxy,
    timestampConverter: ParserTimestampConverter,
  ) {
    super(traceFile, traceProcessor, timestampConverter);
    this.layersSnapshotProtoTransformer = new FakeProtoTransformer(
      assertDefined(ParserSurfaceFlinger.entryField.tamperedMessageType),
    );
    this.layerProtoTransformer = new FakeProtoTransformer(
      assertDefined(ParserSurfaceFlinger.layerField.tamperedMessageType),
    );
  }

  override getTraceType(): TraceType {
    return TraceType.SURFACE_FLINGER;
  }

  override async getEntry(index: number): Promise<HierarchyTreeNode> {
    let snapshotProto = await Utils.queryEntry(
      this.traceProcessor,
      this.getTableName(),
      this.entryIndexToRowIdMap,
      index,
    );
    snapshotProto =
      this.layersSnapshotProtoTransformer.transform(snapshotProto);

    const layerProtos = (await this.querySnapshotLayers(index)).map(
      (layerProto) => this.layerProtoTransformer.transform(layerProto),
    );

    return this.factory.makeEntryHierarchyTree(
      snapshotProto,
      layerProtos,
      ParserSurfaceFlinger,
    );
  }

  override async customQuery<Q extends CustomQueryType>(
    type: Q,
    entriesRange: EntriesRange,
  ): Promise<CustomQueryParserResultTypeMap[Q]> {
    return new VisitableParserCustomQuery(type)
      .visit(CustomQueryType.VSYNCID, async () => {
        return Utils.queryVsyncId(
          this.traceProcessor,
          this.getTableName(),
          this.entryIndexToRowIdMap,
          entriesRange,
        );
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
        const queryResult = await this.traceProcessor.query(sql).waitAllRows();
        const result: CustomQueryParserResultTypeMap[CustomQueryType.SF_LAYERS_ID_AND_NAME] =
          [];
        for (const it = queryResult.iter({}); it.valid(); it.next()) {
          const idAndName = it.get('id_and_name') as string;
          const indexDelimiter = idAndName.indexOf(',');
          assertTrue(
            indexDelimiter > 0,
            () => `Unexpected value in query result: ${idAndName}`,
          );
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

  private async querySnapshotLayers(
    index: number,
  ): Promise<perfetto.protos.ILayerProto[]> {
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
      WHERE snapshot_id = ${this.entryIndexToRowIdMap[index]};
    `;
    const result = await this.traceProcessor.query(sql).waitAllRows();

    for (const it = result.iter({}); it.valid(); it.next()) {
      const builder = getBuilder(it.get('layer_id') as number);
      builder.addArg(
        it.get('key') as string,
        it.get('value_type') as string,
        it.get('int_value') as bigint | undefined,
        it.get('real_value') as number | undefined,
        it.get('string_value') as string | undefined,
      );
    }

    const layerProtos: perfetto.protos.ILayerProto[] = [];
    layerIdToBuilder.forEach((builder) => {
      layerProtos.push(builder.build());
    });

    return layerProtos;
  }
}
