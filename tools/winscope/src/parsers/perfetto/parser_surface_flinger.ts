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
import {TimestampFactory} from 'common/timestamp_factory';
import {AddDefaults} from 'parsers/operations/add_defaults';
import {SetFormatters} from 'parsers/operations/set_formatters';
import {TranslateIntDef} from 'parsers/operations/translate_intdef';
import {AbstractParser} from 'parsers/perfetto/abstract_parser';
import {FakeProtoBuilder} from 'parsers/perfetto/fake_proto_builder';
import {FakeProtoTransformer} from 'parsers/perfetto/fake_proto_transformer';
import {Utils} from 'parsers/perfetto/utils';
import {RectsComputation} from 'parsers/surface_flinger/computations/rects_computation';
import {VisibilityPropertiesComputation} from 'parsers/surface_flinger/computations/visibility_properties_computation';
import {ZOrderPathsComputation} from 'parsers/surface_flinger/computations/z_order_paths_computation';
import {HierarchyTreeBuilderSf} from 'parsers/surface_flinger/hierarchy_tree_builder_sf';
import {ParserSfUtils} from 'parsers/surface_flinger/parser_surface_flinger_utils';
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
import {PropertiesProvider} from 'trace/tree_node/properties_provider';
import {PropertiesProviderBuilder} from 'trace/tree_node/properties_provider_builder';
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

  private static readonly Operations = {
    SetFormattersLayer: new SetFormatters(
      ParserSurfaceFlinger.layerField,
      ParserSurfaceFlinger.CUSTOM_FORMATTERS,
    ),
    TranslateIntDefLayer: new TranslateIntDef(ParserSurfaceFlinger.layerField),
    AddDefaultsLayerEager: new AddDefaults(
      ParserSurfaceFlinger.layerField,
      ParserSfUtils.EAGER_PROPERTIES,
    ),
    AddDefaultsLayerLazy: new AddDefaults(
      ParserSurfaceFlinger.layerField,
      undefined,
      ParserSfUtils.EAGER_PROPERTIES.concat(ParserSfUtils.DENYLIST_PROPERTIES),
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
      ParserSfUtils.DENYLIST_PROPERTIES,
    ),
  };

  private layersSnapshotProtoTransformer: FakeProtoTransformer;
  private layerProtoTransformer: FakeProtoTransformer;

  constructor(
    traceFile: TraceFile,
    traceProcessor: WasmEngineProxy,
    timestampFactory: TimestampFactory,
  ) {
    super(traceFile, traceProcessor, timestampFactory);
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

  override async getEntry(
    index: number,
    timestampType: TimestampType,
  ): Promise<HierarchyTreeNode> {
    let snapshotProto = await Utils.queryEntry(
      this.traceProcessor,
      this.getTableName(),
      index,
    );
    snapshotProto =
      this.layersSnapshotProtoTransformer.transform(snapshotProto);
    const layerProtos = (await this.querySnapshotLayers(index)).map(
      (layerProto) => this.layerProtoTransformer.transform(layerProto),
    );

    return this.makeHierarchyTree(snapshotProto, layerProtos);
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
        const querResult = await this.traceProcessor.query(sql).waitAllRows();
        const result: CustomQueryParserResultTypeMap[CustomQueryType.SF_LAYERS_ID_AND_NAME] =
          [];
        for (const it = querResult.iter({}); it.valid(); it.next()) {
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

  private makeHierarchyTree(
    snapshotProto: perfetto.protos.ILayersSnapshotProto,
    layerProtos: perfetto.protos.ILayerProto[],
  ): HierarchyTreeNode {
    const excludesCompositionState =
      snapshotProto?.excludesCompositionState ?? false;
    const addExcludesCompositionState = excludesCompositionState
      ? ParserSfUtils.OPERATIONS.AddExcludesCompositionStateTrue
      : ParserSfUtils.OPERATIONS.AddExcludesCompositionStateFalse;

    const processed = new Map<number, number>();

    const layers: PropertiesProvider[] = layerProtos.map(
      (layer: perfetto.protos.ILayerProto) => {
        const duplicateCount = processed.get(assertDefined(layer.id)) ?? 0;
        processed.set(assertDefined(layer.id), duplicateCount + 1);
        const eagerProperties = ParserSfUtils.makeEagerPropertiesTree(
          layer,
          duplicateCount,
        );
        const lazyPropertiesStrategy =
          ParserSfUtils.makeLayerLazyPropertiesStrategy(layer, duplicateCount); //TODO: fetch these lazily instead

        const layerProps = new PropertiesProviderBuilder()
          .setEagerProperties(eagerProperties)
          .setLazyPropertiesStrategy(lazyPropertiesStrategy)
          .setCommonOperations([
            ParserSurfaceFlinger.Operations.SetFormattersLayer,
            ParserSurfaceFlinger.Operations.TranslateIntDefLayer,
          ])
          .setEagerOperations([
            ParserSurfaceFlinger.Operations.AddDefaultsLayerEager,
            ParserSfUtils.OPERATIONS.AddCompositionType,
            ParserSfUtils.OPERATIONS.UpdateTransforms,
            ParserSfUtils.OPERATIONS.AddVerboseFlags,
            addExcludesCompositionState,
          ])
          .setLazyOperations([
            ParserSurfaceFlinger.Operations.AddDefaultsLayerLazy,
          ])
          .build();
        return layerProps;
      },
    );

    const entry = new PropertiesProviderBuilder()
      .setEagerProperties(
        ParserSfUtils.makeEntryEagerPropertiesTree(snapshotProto),
      )
      .setLazyPropertiesStrategy(
        ParserSfUtils.makeEntryLazyPropertiesStrategy(snapshotProto),
      )
      .setCommonOperations([
        ParserSfUtils.OPERATIONS.AddDisplayProperties,
        ParserSurfaceFlinger.Operations.SetFormattersEntry,
        ParserSurfaceFlinger.Operations.TranslateIntDefEntry,
      ])
      .setEagerOperations([
        ParserSurfaceFlinger.Operations.AddDefaultsEntryEager,
      ])
      .setLazyOperations([
        ParserSurfaceFlinger.Operations.AddDefaultsEntryLazy,
        ParserSfUtils.OPERATIONS.AddDisplayProperties,
      ])
      .build();

    return new HierarchyTreeBuilderSf()
      .setRoot(entry)
      .setChildren(layers)
      .setComputations([
        new ZOrderPathsComputation(),
        new VisibilityPropertiesComputation(),
        new RectsComputation(),
      ])
      .build();
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
