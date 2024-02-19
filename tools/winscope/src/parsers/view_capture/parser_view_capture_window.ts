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
import {Timestamp, TimestampType} from 'common/time';
import {TimestampFactory} from 'common/timestamp_factory';
import {AddDefaults} from 'parsers/operations/add_defaults';
import {SetFormatters} from 'parsers/operations/set_formatters';
import {TranslateIntDef} from 'parsers/operations/translate_intdef';
import {com} from 'protos/viewcapture/latest/static';
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
import {RectsComputation} from './computations/rects_computation';
import {VisibilityComputation} from './computations/visibility_computation';
import {HierarchyTreeBuilderVc} from './hierarchy_tree_builder_vc';
import {SetRootTransformProperties} from './operations/set_root_transform_properties';
import {NodeField} from './vc_tampered_protos';

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
  ];
  private static readonly DENYLIST_PROPERTIES = ['children'];

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
      ParserViewCaptureWindow.EAGER_PROPERTIES.concat(
        ParserViewCaptureWindow.DENYLIST_PROPERTIES,
      ),
    ),
    SetRootTransformProperties: new SetRootTransformProperties(),
  };

  private timestamps = new Map<TimestampType, Timestamp[]>();

  constructor(
    private readonly descriptors: string[],
    private readonly frameData: com.android.app.viewcapture.data.IFrameData[],
    private readonly traceType: TraceType,
    private readonly realToElapsedTimeOffsetNs: bigint,
    private readonly packageName: string,
    private readonly classNames: string[],
    private readonly timestampFactory: TimestampFactory,
  ) {
    /*
      TODO: Enable this once multiple ViewCapture Tabs becomes generic. Right now it doesn't matter since
        the title is dependent upon the view capture type.

      this.title = `${ParserViewCapture.shortenAndCapitalize(packageName)} - ${ParserViewCapture.shortenAndCapitalize(
        windowData.title
      )}`;
      */
    this.parse();
  }

  parse() {
    this.timestamps = this.decodeTimestamps();
  }

  getTraceType(): TraceType {
    return this.traceType;
  }

  getLengthEntries(): number {
    return this.frameData.length;
  }

  getTimestamps(type: TimestampType): Timestamp[] | undefined {
    return this.timestamps.get(type);
  }

  getEntry(index: number, _: TimestampType): Promise<HierarchyTreeNode> {
    const tree = this.makeHierarchyTree(this.frameData[index]);
    return Promise.resolve(tree);
  }

  customQuery<Q extends CustomQueryType>(
    type: Q,
    entriesRange: EntriesRange,
  ): Promise<CustomQueryParserResultTypeMap[Q]> {
    return new VisitableParserCustomQuery(type)
      .visit(CustomQueryType.VIEW_CAPTURE_PACKAGE_NAME, async () => {
        return Promise.resolve(this.packageName);
      })
      .getResult();
  }

  getDescriptors(): string[] {
    return this.descriptors;
  }

  private decodeTimestamps(): Map<TimestampType, Timestamp[]> {
    const timestampMap = new Map<TimestampType, Timestamp[]>();
    for (const type of [TimestampType.ELAPSED, TimestampType.REAL]) {
      const timestamps: Timestamp[] = [];
      let areTimestampsValid = true;

      for (const entry of this.frameData) {
        const timestampNs = BigInt(assertDefined(entry.timestamp).toString());

        let timestamp: Timestamp | undefined;
        if (this.timestampFactory.canMakeTimestampFromType(type, 0n)) {
          timestamp = this.timestampFactory.makeTimestampFromType(
            type,
            timestampNs,
            this.realToElapsedTimeOffsetNs,
          );
        }
        if (timestamp === undefined) {
          areTimestampsValid = false;
          break;
        }
        timestamps.push(timestamp);
      }

      if (areTimestampsValid) {
        timestampMap.set(type, timestamps);
      }
    }
    return timestampMap;
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
