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

  const node0Properties = new PropertyTreeBuilder()
    .setIsRoot(true)
    .setRootId('0')
    .setName('node0')
    .setChildren([
      {name: 'id', value: 0},
      {name: 'parentId', value: -1},
    ])
    .build();
  const node0Provider = new PropertiesProvider(
    node0Properties,
    async () => node0Properties,
    OperationChain.emptyChain<PropertyTreeNode>(),
    OperationChain.emptyChain<PropertyTreeNode>(),
    OperationChain.emptyChain<PropertyTreeNode>(),
  );

  const node1Properties = new PropertyTreeBuilder()
    .setIsRoot(false)
    .setRootId('1')
    .setName('node1')
    .setChildren([
      {name: 'id', value: 1},
      {name: 'parentId', value: 0},
    ])
    .build();
  const node1Provider = new PropertiesProvider(
    node1Properties,
    async () => node1Properties,
    OperationChain.emptyChain<PropertyTreeNode>(),
    OperationChain.emptyChain<PropertyTreeNode>(),
    OperationChain.emptyChain<PropertyTreeNode>(),
  );

  const node2Properties = new PropertyTreeBuilder()
    .setIsRoot(false)
    .setRootId('2')
    .setName('node2')
    .setChildren([
      {name: 'id', value: 2},
      {name: 'parentId', value: 1},
    ])
    .build();
  const node2Provider = new PropertiesProvider(
    node2Properties,
    async () => node2Properties,
    OperationChain.emptyChain<PropertyTreeNode>(),
    OperationChain.emptyChain<PropertyTreeNode>(),
    OperationChain.emptyChain<PropertyTreeNode>(),
  );

  const node3Properties = new PropertyTreeBuilder()
    .setIsRoot(false)
    .setRootId('3')
    .setName('node3')
    .setChildren([
      {name: 'id', value: 3},
      {name: 'parentId', value: 0},
    ])
    .build();
  const node3Provider = new PropertiesProvider(
    node3Properties,
    async () => node3Properties,
    OperationChain.emptyChain<PropertyTreeNode>(),
    OperationChain.emptyChain<PropertyTreeNode>(),
    OperationChain.emptyChain<PropertyTreeNode>(),
  );

  beforeEach(() => {
    jasmine.addCustomEqualityTester(TreeNodeUtils.treeNodeEqualityTester);
    builder = new HierarchyTreeBuilderVc();
  });

  it('builds hierarchy with one node', () => {
    const actualHierarchy = builder
      .setRoot(node0Provider)
      .setChildren([])
      .build();
    const expectedNode0 = new HierarchyTreeNode(
      '0 node0',
      'node0',
      node0Provider,
    );
    expect(actualHierarchy).toEqual(expectedNode0);
  });

  it('builds hierarchy with two nodes', () => {
    //    node0
    //     /
    // node1
    const actualHierarchy = builder
      .setRoot(node0Provider)
      .setChildren([node1Provider])
      .build();

    const expectedNode0 = new HierarchyTreeNode(
      '0 node0',
      'node0',
      node0Provider,
    );
    const expectedNode1 = new HierarchyTreeNode(
      '1.node1',
      'node1',
      node1Provider,
    );

    expectedNode0.addOrReplaceChild(expectedNode1);

    expect(actualHierarchy).toEqual(expectedNode0);
  });

  it('builds hierarchy with many nodes', () => {
    //      node0
    //      /  \
    //  node1  node3
    //   /
    // node2
    const actualHierarchy = builder
      .setRoot(node0Provider)
      .setChildren([node1Provider, node2Provider, node3Provider])
      .build();

    const expectedNode0 = new HierarchyTreeNode(
      '0 node0',
      'node0',
      node0Provider,
    );
    const expectedNode1 = new HierarchyTreeNode(
      '1.node1',
      'node1',
      node1Provider,
    );
    const expectedNode2 = new HierarchyTreeNode(
      '2.node2',
      'node2',
      node2Provider,
    );
    const expectedNode3 = new HierarchyTreeNode(
      '3.node3',
      'node3',
      node3Provider,
    );

    expectedNode0.addOrReplaceChild(expectedNode1);
    expectedNode0.addOrReplaceChild(expectedNode3);
    expectedNode1.addOrReplaceChild(expectedNode2);

    expect(actualHierarchy).toEqual(expectedNode0);
  });
});
