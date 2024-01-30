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

import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {OperationChain} from 'trace/tree_node/operations/operation_chain';
import {PropertiesProvider} from 'trace/tree_node/properties_provider';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {HierarchyTreeBuilderWm} from './hierarchy_tree_builder_wm';

describe('HierarchyTreeBuilderWm', () => {
  let builder: HierarchyTreeBuilderWm;
  let entry: PropertiesProvider;
  let entryPropertiesTree: PropertyTreeNode;

  beforeEach(() => {
    jasmine.addCustomEqualityTester(nodeEqualityTester);
    builder = new HierarchyTreeBuilderWm();
    entryPropertiesTree = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('WindowManagerState')
      .setName('root')
      .build();
    entry = new PropertiesProvider(
      entryPropertiesTree,
      async () => entryPropertiesTree,
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>()
    );
  });

  it('throws error if root not set', () => {
    const noEntryError = new Error('root not set');
    expect(() => builder.setChildren([]).build()).toThrow(noEntryError);
  });

  it('throws error if containers not set', () => {
    const noLayersError = new Error('children not set');
    expect(() => builder.setRoot(entry).build()).toThrow(noLayersError);
  });

  it('builds root with no children correctly', () => {
    const root = builder.setRoot(entry).setChildren([]).build();

    const expectedRoot = new HierarchyTreeNode(
      'WindowManagerState root',
      'root',
      new PropertiesProvider(
        entryPropertiesTree,
        async () => entryPropertiesTree,
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>()
      )
    );

    expect(root).toEqual(expectedRoot);
  });

  it('builds root with children correctly', () => {
    const container1Props = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('DisplayContent 1234567')
      .setName('container1')
      .setChildren([
        {name: 'id', value: 1},
        {name: 'name', value: 'container1'},
        {name: 'token', value: '1234567'},
        {name: 'children', value: []},
      ])
      .build();

    const container1Provider = new PropertiesProvider(
      container1Props,
      async () => container1Props,
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>()
    );

    const root = builder.setRoot(entry).setChildren([container1Provider]).build();

    const expectedRoot = new HierarchyTreeNode(
      'WindowManagerState root',
      'root',
      new PropertiesProvider(
        entryPropertiesTree,
        async () => entryPropertiesTree,
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>()
      )
    );
    expectedRoot.addOrReplaceChild(
      new HierarchyTreeNode('DisplayContent 1234567 container1', 'container1', container1Provider)
    );

    expect(root).toEqual(expectedRoot);
  });

  it('builds root with nested children correctly', () => {
    const container1Props = new PropertyTreeBuilder()
      .setRootId('DisplayContent 1234567')
      .setName('container1')
      .setIsRoot(true)
      .setChildren([
        {name: 'id', value: 1},
        {name: 'name', value: 'container1'},
        {name: 'token', value: '1234567'},
        {name: 'children', value: [7654321]},
      ])
      .build();

    const container1Provider = new PropertiesProvider(
      container1Props,
      async () => container1Props,
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>()
    );

    const container2Props = new PropertyTreeBuilder()
      .setRootId('DisplayArea 7654321')
      .setName('container2')
      .setIsRoot(true)
      .setChildren([
        {name: 'id', value: 2},
        {name: 'name', value: 'container2'},
        {name: 'token', value: '7654321'},
        {name: 'children', value: []},
      ])
      .build();

    const container2Provider = new PropertiesProvider(
      container2Props,
      async () => container2Props,
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>()
    );

    const root = builder
      .setRoot(entry)
      .setChildren([container1Provider, container2Provider])
      .build();

    const expectedRoot = new HierarchyTreeNode(
      'WindowManagerState root',
      'root',
      new PropertiesProvider(
        entryPropertiesTree,
        async () => entryPropertiesTree,
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>()
      )
    );
    const expectedRootLayer = new HierarchyTreeNode(
      'DisplayContent 1234567 container1',
      'container1',
      container1Provider
    );
    const expectedNestedLayer = new HierarchyTreeNode(
      'DisplayArea 7654321 container2',
      'container2',
      container2Provider
    );
    expectedRootLayer.addOrReplaceChild(expectedNestedLayer);
    expectedRoot.addOrReplaceChild(expectedRootLayer);

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
