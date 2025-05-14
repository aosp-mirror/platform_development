/*
 * Copyright (C) 2025 The Android Open Source Project
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
import {HierarchyTreeBuilderLog} from './hierarchy_tree_builder_log';

describe('HierarchyTreeBuilderLog', () => {
  let builder: HierarchyTreeBuilderLog;
  let root: PropertiesProvider;
  let rootPropertiesTree: PropertyTreeNode;

  beforeEach(() => {
    jasmine.addCustomEqualityTester(TreeNodeUtils.treeNodeEqualityTester);
    builder = new HierarchyTreeBuilderLog();
    rootPropertiesTree = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('LogTrace')
      .setName('root')
      .build();
    root = makePropertiesProvider(rootPropertiesTree);
  });

  it('throws error if root not set', () => {
    const noEntryError = new Error('root not set');
    expect(() => builder.setChildren([]).build()).toThrow(noEntryError);
  });

  it('throws error if containers not set', () => {
    const noChildrenError = new Error('children not set');
    expect(() => builder.setRoot(root).build()).toThrow(noChildrenError);
  });

  it('builds root with no children correctly', () => {
    const rootNode = builder.setRoot(root).setChildren([]).build();
    const expectedRoot = new HierarchyTreeNode(
      'LogTrace root',
      'root',
      makePropertiesProvider(rootPropertiesTree),
    );
    expect(rootNode).toEqual(expectedRoot);
  });

  it('builds root with children correctly', () => {
    const container1Props = new PropertyTreeBuilder()
      .setRootId('LogEntry1')
      .setName('log')
      .setIsRoot(true)
      .build();

    const container1Provider = makePropertiesProvider(container1Props);

    const container2Props = new PropertyTreeBuilder()
      .setRootId('LogEntry2')
      .setName('log')
      .setIsRoot(true)
      .build();

    const container2Provider = makePropertiesProvider(container2Props);

    const rootNode = builder
      .setRoot(root)
      .setChildren([container1Provider, container2Provider])
      .build();

    const expectedRoot = new HierarchyTreeNode(
      'LogTrace root',
      'root',
      makePropertiesProvider(rootPropertiesTree),
    );
    expectedRoot.addOrReplaceChild(
      new HierarchyTreeNode('LogEntry1 log', 'log', container1Provider),
    );
    expectedRoot.addOrReplaceChild(
      new HierarchyTreeNode('LogEntry2 log', 'log', container2Provider),
    );

    expect(rootNode).toEqual(expectedRoot);
  });

  function makePropertiesProvider(properties: PropertyTreeNode) {
    return new PropertiesProvider(
      properties,
      async () => properties,
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
    );
  }
});
