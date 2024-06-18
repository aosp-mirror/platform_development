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
import {HierarchyTreeBuilderInputMethod} from './hierarchy_tree_builder_input_method';

describe('HierarchyTreeBuilderInputMethod', () => {
  let builder: HierarchyTreeBuilderInputMethod;
  let entry: PropertiesProvider;
  let entryPropertiesTree: PropertyTreeNode;

  beforeEach(() => {
    jasmine.addCustomEqualityTester(TreeNodeUtils.treeNodeEqualityTester);
    builder = new HierarchyTreeBuilderInputMethod();
    entryPropertiesTree = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('InputMethod')
      .setName('entry')
      .build();
    entry = new PropertiesProvider(
      entryPropertiesTree,
      async () => entryPropertiesTree,
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
    );
  });

  it('throws error if entry not set', () => {
    const noEntryError = new Error('root not set');
    expect(() => builder.setChildren([]).build()).toThrow(noEntryError);
  });

  it('throws error if children not set', () => {
    const noChildrenError = new Error('children not set');
    expect(() => builder.setRoot(entry).build()).toThrow(noChildrenError);
  });

  it('builds root with no children correctly', () => {
    const root = builder.setRoot(entry).setChildren([]).build();

    const expectedRoot = new HierarchyTreeNode(
      'InputMethod entry',
      'entry',
      new PropertiesProvider(
        entryPropertiesTree,
        async () => entryPropertiesTree,
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>(),
      ),
    );

    expect(root).toEqual(expectedRoot);
  });

  it('builds root with children correctly', () => {
    const childProps = new PropertyTreeBuilder()
      .setRootId('Service')
      .setName('child')
      .setIsRoot(true)
      .build();

    const childProvider = new PropertiesProvider(
      childProps,
      async () => childProps,
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
    );

    const root = builder.setRoot(entry).setChildren([childProvider]).build();

    const expectedRoot = new HierarchyTreeNode(
      'InputMethod entry',
      'entry',
      new PropertiesProvider(
        entryPropertiesTree,
        async () => entryPropertiesTree,
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>(),
        OperationChain.emptyChain<PropertyTreeNode>(),
      ),
    );
    expectedRoot.addOrReplaceChild(
      new HierarchyTreeNode('Service child', 'child', childProvider),
    );

    expect(root).toEqual(expectedRoot);
  });
});
