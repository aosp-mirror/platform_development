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
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {OperationChain} from 'trace/tree_node/operations/operation_chain';
import {PropertiesProvider} from 'trace/tree_node/properties_provider';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {HierarchyTreeBuilderVc} from './hierarchy_tree_builder_vc';

describe('HierarchyTreeBuilderVc', () => {
  let builder: HierarchyTreeBuilderVc;

  beforeEach(() => {
    jasmine.addCustomEqualityTester(TreeNodeUtils.treeNodeEqualityTester);
    builder = new HierarchyTreeBuilderVc();
  });

  it('throws error if entry not set', () => {
    const noEntryError = new Error('root not set');
    expect(() => builder.setChildren([]).build()).toThrow(noEntryError);
  });

  it('throws error if nodes not set', () => {
    const noNodesError = new Error('children not set');
    const entryPropertyTree = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('ViewNode')
      .setName('com.android.internal@123456789')
      .setChildren([
        {name: 'hashcode', value: 123456789},
        {name: 'children', value: []},
      ])
      .build();
    const entry = new PropertiesProvider(
      entryPropertyTree,
      async () => entryPropertyTree,
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
    );

    expect(() => builder.setRoot(entry).build()).toThrow(noNodesError);
  });

  it('builds root with no children correctly', () => {
    const entryPropertyTree = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('ViewNode')
      .setName('com.android.internal@123456789')
      .setChildren([
        {name: 'hashcode', value: 123456789},
        {name: 'children', value: []},
      ])
      .build();
    const entry = new PropertiesProvider(
      entryPropertyTree,
      async () => entryPropertyTree,
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
    );

    const root = builder.setRoot(entry).setChildren([]).build();

    const expectedRoot = new HierarchyTreeNode(
      'ViewNode com.android.internal@123456789',
      'com.android.internal@123456789',
      new PropertiesProvider(
        entryPropertyTree,
        async () => entryPropertyTree,
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>(),
      ),
    );

    expect(root).toEqual(expectedRoot);
  });

  it('builds root with children correctly', () => {
    const entryPropertyTree = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('ViewNode')
      .setName('com.android.internal@123456789')
      .setChildren([
        {name: 'hashcode', value: 123456789},
        {name: 'children', children: [{name: '0', value: 987654321}]},
      ])
      .build();
    const entry = new PropertiesProvider(
      entryPropertyTree,
      async () => entryPropertyTree,
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
    );

    const node1Props = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('1')
      .setName('node1')
      .setChildren([
        {name: 'hashcode', value: 987654321},
        {name: 'children', value: []},
      ])
      .build();
    const node1Provider = new PropertiesProvider(
      node1Props,
      async () => node1Props,
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
    );

    const root = builder.setRoot(entry).setChildren([node1Provider]).build();

    const expectedRoot = new HierarchyTreeNode(
      'ViewNode com.android.internal@123456789',
      'com.android.internal@123456789',
      new PropertiesProvider(
        entryPropertyTree,
        async () => entryPropertyTree,
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>(),
      ),
    );
    expectedRoot.addOrReplaceChild(
      new HierarchyTreeNode('1 node1', 'node1', node1Provider),
    );

    expect(root).toEqual(expectedRoot);
  });

  it('builds root with nested children correctly', () => {
    const entryPropertyTree = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('ViewNode')
      .setName('com.android.internal@123456789')
      .setChildren([
        {name: 'hashcode', value: 123456789},
        {name: 'children', children: [{name: '0', value: 987654321}]},
      ])
      .build();
    const entry = new PropertiesProvider(
      entryPropertyTree,
      async () => entryPropertyTree,
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
    );

    const node1Props = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('1')
      .setName('node1')
      .setChildren([
        {name: 'hashcode', value: 987654321},
        {name: 'children', children: [{name: '0', value: 464646464}]},
      ])
      .build();
    const node1Provider = new PropertiesProvider(
      node1Props,
      async () => node1Props,
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
    );

    const node2Props = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('2')
      .setName('node2')
      .setChildren([
        {name: 'hashcode', value: 464646464},
        {name: 'children', value: []},
      ])
      .build();
    const node2Provider = new PropertiesProvider(
      node2Props,
      async () => node2Props,
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
    );

    const root = builder
      .setRoot(entry)
      .setChildren([node1Provider, node2Provider])
      .build();

    const expectedRoot = new HierarchyTreeNode(
      'ViewNode com.android.internal@123456789',
      'com.android.internal@123456789',
      new PropertiesProvider(
        entryPropertyTree,
        async () => entryPropertyTree,
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>(),
      ),
    );
    const expectedRootNode = new HierarchyTreeNode(
      '1 node1',
      'node1',
      node1Provider,
    );
    const expectedNestedNode = new HierarchyTreeNode(
      '2 node2',
      'node2',
      node2Provider,
    );
    expectedRootNode.addOrReplaceChild(expectedNestedNode);
    expectedRoot.addOrReplaceChild(expectedRootNode);

    expect(root).toEqual(expectedRoot);
  });
});
