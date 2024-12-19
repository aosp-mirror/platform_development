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

import {assertDefined} from 'common/assert_utils';
import {Timestamp} from 'common/time/time';
import {ParserTimestampConverter} from 'common/time/timestamp_converter';
import {AddDefaults} from 'parsers/operations/add_defaults';
import {SetFormatters} from 'parsers/operations/set_formatters';
import {TranslateIntDef} from 'parsers/operations/translate_intdef';
import {RectsComputation} from 'parsers/view_capture/computations/rects_computation';
import {VisibilityComputation} from 'parsers/view_capture/computations/visibility_computation';
import {SetRootTransformProperties} from 'parsers/view_capture/operations/set_root_transform_properties';
import {com} from 'protos/viewcapture/udc/static';
import {CoarseVersion} from 'trace/coarse_version';
import {
  CustomQueryParserResultTypeMap,
  CustomQueryType,
  VisitableParserCustomQuery,
} from 'trace/custom_query';
import {EntriesRange} from 'trace/index_types';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {Operation} from 'trace/tree_node/operations/operation';
import {
  LazyPropertiesStrategyType,
  PropertiesProvider,
} from 'trace/tree_node/properties_provider';
import {PropertiesProviderBuilder} from 'trace/tree_node/properties_provider_builder';
import {PropertyTreeBuilderFromProto} from 'trace/tree_node/property_tree_builder_from_proto';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {DEFAULT_PROPERTY_TREE_NODE_FACTORY} from 'trace/tree_node/property_tree_node_factory';
import {HierarchyTreeBuilderVc} from './hierarchy_tree_builder_vc';
import {NodeField} from './tampered_protos';

export class ParserViewCaptureWindow implements Parser<HierarchyTreeNode> {
  private static readonly EAGER_PROPERTIES = [
    'classnameIndex',
    'hashcode',
    'id',
    'left',
    'top',
    'width',
    'height',
    'scaleX',
    'scaleY',
    'scrollX',
    'scrollY',
    'translationX',
    'translationY',
    'visibility',
    'alpha',
  ];
  private static readonly DENYLIST_PROPERTIES =
    ParserViewCaptureWindow.EAGER_PROPERTIES.concat(['children']); // some eager properties are overridden by calculated properties - avoid reloading them on lazy fetch

  private static readonly Operations = {
    SetFormattersNode: new SetFormatters(NodeField),
    TranslateIntDefNode: new TranslateIntDef(NodeField),
    AddDefaultsNodeEager: new AddDefaults(
      NodeField,
      ParserViewCaptureWindow.EAGER_PROPERTIES,
    ),
    AddDefaultsNodeLazy: new AddDefaults(
      NodeField,
      undefined,
      ParserViewCaptureWindow.DENYLIST_PROPERTIES,
    ),
    SetRootTransformProperties: new SetRootTransformProperties(),
  };

  private timestamps: Timestamp[] | undefined;

  constructor(
    private readonly descriptors: string[],
    private readonly frameData: com.android.app.viewcapture.data.IFrameData[],
    private readonly realToBootTimeOffsetNs: bigint,
    private readonly packageName: string,
    private readonly windowName: string,
    private readonly classNames: string[],
    private readonly timestampConverter: ParserTimestampConverter,
  ) {}

  parse() {
    throw new Error('Not implemented');
  }

  getTraceType(): TraceType {
    return TraceType.VIEW_CAPTURE;
  }

  getCoarseVersion(): CoarseVersion {
    return CoarseVersion.LEGACY;
  }

  getLengthEntries(): number {
    return this.frameData.length;
  }

  getRealToMonotonicTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  getRealToBootTimeOffsetNs(): bigint | undefined {
    return this.realToBootTimeOffsetNs;
  }

  createTimestamps() {
    this.timestamps = this.decodeTimestamps();
  }

  getTimestamps(): Timestamp[] | undefined {
    return this.timestamps;
  }

  getEntry(index: number): Promise<HierarchyTreeNode> {
    const tree = this.makeHierarchyTree(this.frameData[index]);
    return Promise.resolve(tree);
  }

  customQuery<Q extends CustomQueryType>(
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

  getDescriptors(): string[] {
    return [this.windowName, ...this.descriptors];
  }

  private decodeTimestamps(): Timestamp[] {
    return this.frameData.map((entry) =>
      this.timestampConverter.makeTimestampFromBootTimeNs(
        BigInt(assertDefined(entry.timestamp).toString()),
      ),
    );
  }

  private makeHierarchyTree(
    frameDataProto: com.android.app.viewcapture.data.IFrameData,
  ): HierarchyTreeNode {
    const nodes = this.makeNodePropertiesProviders(
      assertDefined(frameDataProto.node),
      true,
    );
    return new HierarchyTreeBuilderVc()
      .setRoot(nodes[0])
      .setChildren(nodes.slice(1))
      .setComputations([new VisibilityComputation(), new RectsComputation()])
      .build();
  }

  private makeNodePropertiesProviders(
    node: com.android.app.viewcapture.data.IViewNode,
    isRoot = false,
  ): PropertiesProvider[] {
    const eagerOperations: Array<Operation<PropertyTreeNode>> = [
      ParserViewCaptureWindow.Operations.AddDefaultsNodeEager,
    ];
    if (isRoot) {
      eagerOperations.push(
        ParserViewCaptureWindow.Operations.SetRootTransformProperties,
      );
    }

    const eagerProperties = this.makeEagerPropertiesTree(node);
    const lazyPropertiesStrategy = this.makeLazyPropertiesStrategy(node);

    const nodeProperties = new PropertiesProviderBuilder()
      .setEagerProperties(eagerProperties)
      .setLazyPropertiesStrategy(lazyPropertiesStrategy)
      .setCommonOperations([
        ParserViewCaptureWindow.Operations.SetFormattersNode,
        ParserViewCaptureWindow.Operations.TranslateIntDefNode,
      ])
      .setEagerOperations(eagerOperations)
      .setLazyOperations([
        ParserViewCaptureWindow.Operations.AddDefaultsNodeLazy,
      ])
      .build();

    const propertiesProviders: PropertiesProvider[] = [nodeProperties];

    node.children?.forEach(
      (childNode: com.android.app.viewcapture.data.IViewNode) => {
        propertiesProviders.push(
          ...this.makeNodePropertiesProviders(childNode),
        );
      },
    );

    return propertiesProviders;
  }

  private makeEagerPropertiesTree(
    node: com.android.app.viewcapture.data.IViewNode,
  ): PropertyTreeNode {
    const denyList: string[] = [];

    let obj = node;
    do {
      Object.getOwnPropertyNames(obj).forEach((it) => {
        if (!ParserViewCaptureWindow.EAGER_PROPERTIES.includes(it)) {
          denyList.push(it);
        }
      });
      obj = Object.getPrototypeOf(obj);
    } while (obj);

    const id = `${this.classNames[assertDefined(node.classnameIndex)]}@${
      node.hashcode
    }`;

    const nodeProperties = new PropertyTreeBuilderFromProto()
      .setData(node)
      .setRootId('ViewNode')
      .setRootName(id)
      .setDenyList(denyList)
      .build();

    nodeProperties.addOrReplaceChild(
      DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeCalculatedProperty(
        nodeProperties.id,
        'children',
        this.mapChildrenToHashcodes(node.children ?? []),
      ),
    );

    return nodeProperties;
  }

  private mapChildrenToHashcodes(
    children: com.android.app.viewcapture.data.IViewNode[],
  ): number[] {
    return children.map((child) => assertDefined(child.hashcode));
  }

  private makeLazyPropertiesStrategy(
    node: com.android.app.viewcapture.data.IViewNode,
  ): LazyPropertiesStrategyType {
    return async () => {
      const id = `${this.classNames[assertDefined(node.classnameIndex)]}@${
        node.hashcode
      }`;
      return new PropertyTreeBuilderFromProto()
        .setData(node)
        .setRootId('ViewNode')
        .setRootName(id)
        .setDenyList(ParserViewCaptureWindow.DENYLIST_PROPERTIES)
        .build();
    };
  }
}
