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

import {assertDefined} from 'common/assert_utils';
import {ParserTimestampConverter} from 'common/timestamp_converter';
import {AddDefaults} from 'parsers/operations/add_defaults';
import {SetFormatters} from 'parsers/operations/set_formatters';
import {AbstractParser} from 'parsers/perfetto/abstract_parser';
import {FakeProtoTransformer} from 'parsers/perfetto/fake_proto_transformer';
import {Utils} from 'parsers/perfetto/utils';
import {TamperedMessageType} from 'parsers/tampered_message_type';
import {RectsComputation} from 'parsers/view_capture/computations/rects_computation';
import {VisibilityComputation} from 'parsers/view_capture/computations/visibility_computation';
import root from 'protos/viewcapture/latest/json';
import {perfetto} from 'protos/viewcapture/latest/static';
import {
  CustomQueryParserResultTypeMap,
  CustomQueryType,
  VisitableParserCustomQuery,
} from 'trace/custom_query';
import {EntriesRange} from 'trace/index_types';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertiesProvider} from 'trace/tree_node/properties_provider';
import {PropertiesProviderBuilder} from 'trace/tree_node/properties_provider_builder';
import {PropertyTreeBuilderFromProto} from 'trace/tree_node/property_tree_builder_from_proto';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {WasmEngineProxy} from 'trace_processor/wasm_engine_proxy';
import {HierarchyTreeBuilderVc} from './hierarchy_tree_builder_vc';

export class ParserViewCaptureWindow extends AbstractParser<HierarchyTreeNode> {
  private static readonly PROTO_WRAPPER_MESSAGE = TamperedMessageType.tamper(
    root.lookupType('perfetto.protos.Wrapper'),
  );
  private static readonly PROTO_VIEWCAPTURE_FIELD =
    ParserViewCaptureWindow.PROTO_WRAPPER_MESSAGE.fields['viewcapture'];
  private static readonly PROTO_VIEW_FIELD = assertDefined(
    ParserViewCaptureWindow.PROTO_VIEWCAPTURE_FIELD.tamperedMessageType?.fields[
      'views'
    ],
  );

  private static readonly PROPERTY_TREE_OPERATIONS = [
    new AddDefaults(ParserViewCaptureWindow.PROTO_VIEW_FIELD),
    new SetFormatters(ParserViewCaptureWindow.PROTO_VIEW_FIELD),
  ];

  private readonly packageName: string;
  private readonly windowName: string;
  private readonly protoTransformer: FakeProtoTransformer;

  constructor(
    traceFile: TraceFile,
    traceProcessor: WasmEngineProxy,
    timestampConverter: ParserTimestampConverter,
    packageName: string,
    windowName: string,
  ) {
    super(traceFile, traceProcessor, timestampConverter);

    this.packageName = packageName;
    this.windowName = windowName;

    this.protoTransformer = new FakeProtoTransformer(
      assertDefined(
        ParserViewCaptureWindow.PROTO_VIEWCAPTURE_FIELD.tamperedMessageType,
      ),
    );
  }

  override getTraceType(): TraceType {
    return TraceType.VIEW_CAPTURE;
  }

  override async getEntry(index: number): Promise<HierarchyTreeNode> {
    let entry = (await Utils.queryEntry(
      this.traceProcessor,
      this.getTableName(),
      this.entryIndexToRowIdMap,
      index,
    )) as perfetto.protos.IViewCapture;
    entry = this.protoTransformer.transform(entry);

    const views = this.makeViewPropertyProviders(entry);

    const rootView = assertDefined(
      views.find((view) => {
        const parentId = assertDefined(
          view.getEagerProperties().getChildByName('parentId'),
        ).getValue() as number;
        return parentId === -1;
      }),
    );

    const childrenViews = views.filter((view) => view !== rootView);

    return new HierarchyTreeBuilderVc()
      .setRoot(rootView)
      .setChildren(childrenViews)
      .setComputations([new VisibilityComputation(), new RectsComputation()])
      .build();
  }

  override customQuery<Q extends CustomQueryType>(
    type: Q,
    entriesRange: EntriesRange,
  ): Promise<CustomQueryParserResultTypeMap[Q]> {
    return new VisitableParserCustomQuery(type)
      .visit(CustomQueryType.VIEW_CAPTURE_METADATA, async () => {
        const metadata = {
          packageName: this.packageName,
          windowName: this.windowName,
        };
        return Promise.resolve(metadata);
      })
      .getResult();
  }

  protected override getStdLibModuleName(): string | undefined {
    return 'android.winscope.viewcapture';
  }

  protected override getTableName(): string {
    return 'android_viewcapture';
  }

  override async buildEntryIndexToRowIdMap(): Promise<number[]> {
    const sqlRowIdAndTimestamp = `
        SELECT vc.id as id, vc.ts as ts
        FROM ${this.getTableName()} AS vc
        JOIN args ON vc.arg_set_id = args.arg_set_id
        WHERE
          args.key = 'window_name' AND
          args.string_value = '${this.windowName}'
        ORDER BY vc.ts;
    `;
    const result = await this.traceProcessor
      .query(sqlRowIdAndTimestamp)
      .waitAllRows();
    const entryIndexToRowId: number[] = [];
    for (const it = result.iter({}); it.valid(); it.next()) {
      const rowId = Number(it.get('id') as bigint);
      entryIndexToRowId.push(rowId);
    }
    return entryIndexToRowId;
  }

  private makeViewPropertyProviders(
    entry: perfetto.protos.IViewCapture,
  ): PropertiesProvider[] {
    const providers = (entry.views ?? []).map((view) => {
      const allProperties = this.makeViewPropertyTree(view);
      const provider = new PropertiesProviderBuilder()
        .setEagerProperties(allProperties)
        .setCommonOperations(ParserViewCaptureWindow.PROPERTY_TREE_OPERATIONS)
        .build();

      return provider;
    });

    return providers;
  }

  private makeViewPropertyTree(
    view: perfetto.protos.ViewCapture.IView,
  ): PropertyTreeNode {
    const rootName = `${(view as any).className}@${view.hashcode}`;

    const nodeProperties = new PropertyTreeBuilderFromProto()
      .setData(view)
      .setRootId('root-view')
      .setRootName(rootName)
      .build();

    return nodeProperties;
  }
}
