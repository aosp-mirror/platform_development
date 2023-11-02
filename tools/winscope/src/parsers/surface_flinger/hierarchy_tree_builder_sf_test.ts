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

import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {OperationChain} from 'trace/tree_node/operations/operation_chain';
import {PropertiesProvider} from 'trace/tree_node/properties_provider';
import {PropertySource, PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {HierarchyTreeBuilderSf} from './hierarchy_tree_builder_sf';
import {LayerFlag} from './layer_flag';

describe('HierarchyTreeBuilderSf', () => {
  let builder: HierarchyTreeBuilderSf;
  let entry: PropertiesProvider;

  beforeEach(() => {
    jasmine.addCustomEqualityTester(nodeEqualityTester);
    builder = new HierarchyTreeBuilderSf();
    const testPropertyNode = TreeNodeUtils.makePropertyNode('test node', 'test node', null);
    entry = new PropertiesProvider(
      testPropertyNode,
      async () => testPropertyNode,
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>()
    );
  });

  it('throws error if entry not set', () => {
    const noEntryError = new Error('entry not set');
    expect(() => builder.setLayers([]).build()).toThrow(noEntryError);
  });

  it('throws error if layers not set', () => {
    const noLayersError = new Error('layers not set');
    expect(() => builder.setEntry(entry).build()).toThrow(noLayersError);
  });

  it('builds root with no children correctly', () => {
    const root = builder.setEntry(entry).setLayers([]).build();

    const propertiesTree = new PropertyTreeNode(
      'LayerTraceEntry root',
      'root',
      PropertySource.PROTO,
      null
    );
    const expectedRoot = new HierarchyTreeNode(
      'LayerTraceEntry root',
      'root',
      new PropertiesProvider(
        propertiesTree,
        async () => propertiesTree,
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>()
      )
    );

    expect(root).toEqual(expectedRoot);
  });

  it('builds root with children correctly', () => {
    const layer1Props = new PropertyTreeNode('1 layer1', 'layer1', PropertySource.PROTO, null);
    layer1Props.addChild(new PropertyTreeNode('1 layer1.id', 'id', PropertySource.PROTO, 1));
    layer1Props.addChild(
      new PropertyTreeNode('1 layer1.name', 'name', PropertySource.PROTO, 'layer1')
    );
    layer1Props.addChild(
      new PropertyTreeNode('1 layer1.parent', 'parent', PropertySource.PROTO, -1)
    );
    layer1Props.addChild(
      new PropertyTreeNode('1 layer1.children', 'children', PropertySource.PROTO, [])
    );
    layer1Props.addChild(
      new PropertyTreeNode('1 layer1.flags', 'flags', PropertySource.PROTO, LayerFlag.HIDDEN)
    );
    const layer1Provider = new PropertiesProvider(
      layer1Props,
      async () => layer1Props,
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>()
    );

    const root = builder.setEntry(entry).setLayers([layer1Provider]).build();

    const propertiesTree = new PropertyTreeNode(
      'LayerTraceEntry root',
      'root',
      PropertySource.PROTO,
      null
    );
    const expectedRoot = new HierarchyTreeNode(
      'LayerTraceEntry root',
      'root',
      new PropertiesProvider(
        propertiesTree,
        async () => propertiesTree,
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>()
      )
    );
    expectedRoot.addChild(new HierarchyTreeNode('1 layer1', 'layer1', layer1Provider));

    expect(root).toEqual(expectedRoot);
  });

  it('builds root with nested children correctly', () => {
    const layer1Props = new PropertyTreeNode('1 layer1', 'layer1', PropertySource.PROTO, null);
    layer1Props.addChild(new PropertyTreeNode('1 layer1.id', 'id', PropertySource.PROTO, 1));
    layer1Props.addChild(
      new PropertyTreeNode('1 layer1.name', 'name', PropertySource.PROTO, 'layer1')
    );
    layer1Props.addChild(
      new PropertyTreeNode('1 layer1.parent', 'parent', PropertySource.PROTO, -1)
    );
    layer1Props.addChild(
      new PropertyTreeNode('1 layer1.children', 'children', PropertySource.PROTO, [2])
    );
    layer1Props.addChild(
      new PropertyTreeNode('1 layer1.flags', 'flags', PropertySource.PROTO, LayerFlag.HIDDEN)
    );
    const layer1Provider = new PropertiesProvider(
      layer1Props,
      async () => layer1Props,
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>()
    );

    const layer2Props = new PropertyTreeNode('2 layer2', 'layer2', PropertySource.PROTO, null);
    layer2Props.addChild(new PropertyTreeNode('2 layer2.id', 'id', PropertySource.PROTO, 2));
    layer2Props.addChild(
      new PropertyTreeNode('2 layer2.name', 'name', PropertySource.PROTO, 'layer2')
    );
    layer2Props.addChild(
      new PropertyTreeNode('2 layer2.parent', 'parent', PropertySource.PROTO, 1)
    );
    layer2Props.addChild(
      new PropertyTreeNode('2 layer2.children', 'children', PropertySource.PROTO, [])
    );
    layer2Props.addChild(
      new PropertyTreeNode('2 layer2.flags', 'flags', PropertySource.PROTO, LayerFlag.HIDDEN)
    );
    const layer2Provider = new PropertiesProvider(
      layer2Props,
      async () => layer2Props,
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>()
    );

    const root = builder.setEntry(entry).setLayers([layer1Provider, layer2Provider]).build();

    const propertiesTree = new PropertyTreeNode(
      'LayerTraceEntry root',
      'root',
      PropertySource.PROTO,
      null
    );
    const expectedRoot = new HierarchyTreeNode(
      'LayerTraceEntry root',
      'root',
      new PropertiesProvider(
        propertiesTree,
        async () => propertiesTree,
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>()
      )
    );
    const expectedRootLayer = new HierarchyTreeNode('1 layer1', 'layer1', layer1Provider);
    const expectedNestedLayer = new HierarchyTreeNode('2 layer2', 'layer2', layer2Provider);
    expectedRootLayer.addChild(expectedNestedLayer);
    expectedRoot.addChild(expectedRootLayer);

    expect(root).toEqual(expectedRoot);
  });

  it('builds root with duplicate id layers', () => {
    const layer1Props = new PropertyTreeNode('1 layer1', 'layer1', PropertySource.PROTO, null);
    layer1Props.addChild(new PropertyTreeNode('1 layer1.id', 'id', PropertySource.PROTO, 1));
    layer1Props.addChild(
      new PropertyTreeNode('1 layer1.name', 'name', PropertySource.PROTO, 'layer1')
    );
    layer1Props.addChild(
      new PropertyTreeNode('1 layer1.parent', 'parent', PropertySource.PROTO, -1)
    );
    layer1Props.addChild(
      new PropertyTreeNode('1 layer1.children', 'children', PropertySource.PROTO, [2])
    );
    layer1Props.addChild(
      new PropertyTreeNode('1 layer1.flags', 'flags', PropertySource.PROTO, LayerFlag.HIDDEN)
    );
    const layer1Provider = new PropertiesProvider(
      layer1Props,
      async () => layer1Props,
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>()
    );

    const layer2Props = new PropertyTreeNode('2 layer2', 'layer2', PropertySource.PROTO, null);
    layer2Props.addChild(new PropertyTreeNode('2 layer2.id', 'id', PropertySource.PROTO, 2));
    layer2Props.addChild(
      new PropertyTreeNode('2 layer2.name', 'name', PropertySource.PROTO, 'layer2')
    );
    layer2Props.addChild(
      new PropertyTreeNode('2 layer2.parent', 'parent', PropertySource.PROTO, 1)
    );
    layer2Props.addChild(
      new PropertyTreeNode('2 layer2.children', 'children', PropertySource.PROTO, [])
    );
    layer2Props.addChild(
      new PropertyTreeNode('2 layer2.flags', 'flags', PropertySource.PROTO, LayerFlag.HIDDEN)
    );
    const layer2Provider = new PropertiesProvider(
      layer2Props,
      async () => layer2Props,
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>()
    );

    const root = builder
      .setEntry(entry)
      .setLayers([layer1Provider, layer2Provider, layer2Provider])
      .build();

    const propertiesTree = new PropertyTreeNode(
      'LayerTraceEntry root',
      'root',
      PropertySource.PROTO,
      null
    );
    const expectedRoot = new HierarchyTreeNode(
      'LayerTraceEntry root',
      'root',
      new PropertiesProvider(
        propertiesTree,
        async () => propertiesTree,
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>()
      )
    );
    const expectedRootLayer = new HierarchyTreeNode('1 layer1', 'layer1', layer1Provider);
    const expectedNestedLayer = new HierarchyTreeNode('2 layer2', 'layer2', layer2Provider);
    const expectedDupNestedLayer = new HierarchyTreeNode(
      '2 layer2 duplicate(1)',
      'layer2 duplicate(1)',
      layer2Provider
    );
    expectedRootLayer.addChild(expectedNestedLayer);
    expectedRootLayer.addChild(expectedDupNestedLayer);
    expectedRoot.addChild(expectedRootLayer);

    expect(root).toEqual(expectedRoot);
  });

  function nodeEqualityTester(first: any, second: any): boolean | undefined {
    if (first instanceof HierarchyTreeNode && second instanceof HierarchyTreeNode) {
      return testHierarchyTreeNodes(first, second);
    }
    return undefined;
  }

  function testHierarchyTreeNodes(
    node: HierarchyTreeNode,
    expectedNode: HierarchyTreeNode
  ): boolean {
    if (node.id !== expectedNode.id) return false;
    if (node.name !== expectedNode.name) return false;

    for (const [index, child] of node.getAllChildren().entries()) {
      if (!testHierarchyTreeNodes(child, expectedNode.getAllChildren()[index])) return false;
    }
    return true;
  }
});
