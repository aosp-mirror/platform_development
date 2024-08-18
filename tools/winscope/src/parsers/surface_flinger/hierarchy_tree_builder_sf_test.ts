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

import {DuplicateLayerId} from 'messaging/user_warnings';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {UserNotifierChecker} from 'test/unit/user_notifier_checker';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {OperationChain} from 'trace/tree_node/operations/operation_chain';
import {PropertiesProvider} from 'trace/tree_node/properties_provider';
import {
  PropertySource,
  PropertyTreeNode,
} from 'trace/tree_node/property_tree_node';
import {HierarchyTreeBuilderSf} from './hierarchy_tree_builder_sf';
import {LayerFlag} from './layer_flag';

describe('HierarchyTreeBuilderSf', () => {
  let builder: HierarchyTreeBuilderSf;
  let entry: PropertiesProvider;
  let expectedRoot: HierarchyTreeNode;

  beforeEach(() => {
    jasmine.addCustomEqualityTester(TreeNodeUtils.treeNodeEqualityTester);
    builder = new HierarchyTreeBuilderSf();
    const propertiesTree = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('LayerTraceEntry')
      .setName('root')
      .build();
    entry = makePropertiesProvider(propertiesTree);
    expectedRoot = new HierarchyTreeNode('LayerTraceEntry root', 'root', entry);
  });

  it('throws error if entry not set', () => {
    const noEntryError = new Error('root not set');
    expect(() => builder.setChildren([]).build()).toThrow(noEntryError);
  });

  it('throws error if layers not set', () => {
    const noLayersError = new Error('children not set');
    expect(() => builder.setRoot(entry).build()).toThrow(noLayersError);
  });

  it('builds root with no children correctly', () => {
    const root = builder.setRoot(entry).setChildren([]).build();
    expect(root).toEqual(expectedRoot);
  });

  it('builds root with children correctly', () => {
    const layer1Props = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('1')
      .setName('layer1')
      .setChildren([
        {name: 'id', value: 1},
        {name: 'name', value: 'layer1'},
        {name: 'parent', value: -1},
        {name: 'children', value: []},
        {name: 'flags', value: LayerFlag.HIDDEN},
      ])
      .build();
    const layer1Provider = makePropertiesProvider(layer1Props);

    const root = builder.setRoot(entry).setChildren([layer1Provider]).build();

    expectedRoot.addOrReplaceChild(
      new HierarchyTreeNode('1 layer1', 'layer1', layer1Provider),
    );

    expect(root).toEqual(expectedRoot);
  });

  it('builds root with nested children correctly', () => {
    const layer1Props = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('1')
      .setName('layer1')
      .setChildren([
        {name: 'id', value: 1},
        {name: 'name', value: 'layer1'},
        {name: 'parent', value: -1},
        {
          name: 'children',
          value: undefined,
          children: [
            {
              name: '0',
              value: 2,
            },
          ],
        },
        {name: 'flags', value: LayerFlag.HIDDEN},
      ])
      .build();
    const layer1Provider = makePropertiesProvider(layer1Props);

    const layer2Props = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('2')
      .setName('layer2')
      .setChildren([
        {name: 'id', value: 2},
        {name: 'name', value: 'layer2'},
        {name: 'parent', value: 1},
        {name: 'children', value: []},
        {name: 'flags', value: LayerFlag.HIDDEN},
      ])
      .build();
    const layer2Provider = makePropertiesProvider(layer2Props);

    const root = builder
      .setRoot(entry)
      .setChildren([layer1Provider, layer2Provider])
      .build();

    const expectedRootLayer = new HierarchyTreeNode(
      '1 layer1',
      'layer1',
      layer1Provider,
    );
    const expectedNestedLayer = new HierarchyTreeNode(
      '2 layer2',
      'layer2',
      layer2Provider,
    );
    expectedRootLayer.addOrReplaceChild(expectedNestedLayer);
    expectedRoot.addOrReplaceChild(expectedRootLayer);

    expect(root).toEqual(expectedRoot);
  });

  it('builds root with duplicate id layers', () => {
    const userNotifierChecker = new UserNotifierChecker();
    const layer1Props = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('1')
      .setName('layer1')
      .setChildren([
        {name: 'id', value: 1},
        {name: 'name', value: 'layer1'},
        {name: 'parent', value: -1},
        {
          name: 'children',
          value: undefined,
          children: [
            {
              name: '0',
              value: 2,
            },
          ],
        },
        {name: 'flags', value: LayerFlag.HIDDEN},
      ])
      .build();
    const layer1Provider = makePropertiesProvider(layer1Props);

    const layer2Props = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('2')
      .setName('layer2')
      .setChildren([
        {name: 'id', value: 2},
        {name: 'name', value: 'layer2'},
        {name: 'parent', value: 1},
        {name: 'children', value: []},
        {name: 'flags', value: LayerFlag.HIDDEN},
      ])
      .build();
    const layer2Provider = makePropertiesProvider(layer2Props);

    const layer2DupProps = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('2')
      .setName('layer2 duplicate(1)')
      .setChildren([
        {name: 'id', value: 2},
        {name: 'name', value: 'layer2'},
        {name: 'parent', value: 1},
        {name: 'children', value: []},
        {name: 'flags', value: LayerFlag.HIDDEN},
      ])
      .build();
    const layer2DupProvider = makePropertiesProvider(layer2DupProps);

    const root = builder
      .setRoot(entry)
      .setChildren([layer1Provider, layer2Provider, layer2DupProvider])
      .build();

    const expectedRootLayer = new HierarchyTreeNode(
      '1 layer1',
      'layer1',
      layer1Provider,
    );
    const expectedNestedLayer = new HierarchyTreeNode(
      '2 layer2',
      'layer2',
      layer2Provider,
    );
    const expectedDupNestedLayer = new HierarchyTreeNode(
      '2 layer2 duplicate(1)',
      'layer2 duplicate(1)',
      layer2Provider,
    );
    expectedRootLayer.addOrReplaceChild(expectedNestedLayer);
    expectedRootLayer.addOrReplaceChild(expectedDupNestedLayer);
    expectedRoot.addOrReplaceChild(expectedRootLayer);

    expect(root).toEqual(expectedRoot);
    userNotifierChecker.expectAdded([new DuplicateLayerId('2')]);
  });

  it('builds root with default parent values correctly', () => {
    const layer1Props = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('1')
      .setName('layer1')
      .setChildren([
        {name: 'id', value: 1},
        {name: 'name', value: 'layer1'},
        {name: 'parent', value: -1},
        {
          name: 'children',
          value: undefined,
          children: [
            {
              name: '0',
              value: 2,
            },
          ],
        },
        {name: 'flags', value: LayerFlag.HIDDEN},
      ])
      .build();
    const layer1Provider = makePropertiesProvider(layer1Props);

    const layer2Props = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('2')
      .setName('layer2')
      .setChildren([
        {name: 'id', value: 2},
        {name: 'name', value: 'layer2'},
        {name: 'parent', value: 1, source: PropertySource.DEFAULT},
        {name: 'children', value: []},
        {name: 'flags', value: LayerFlag.HIDDEN},
      ])
      .build();
    const layer2Provider = makePropertiesProvider(layer2Props);

    const root = builder
      .setRoot(entry)
      .setChildren([layer1Provider, layer2Provider])
      .build();

    const expectedLayer1 = new HierarchyTreeNode(
      '1 layer1',
      'layer1',
      layer1Provider,
    );
    const expectedLayer2 = new HierarchyTreeNode(
      '2 layer2',
      'layer2',
      layer2Provider,
    );
    expectedRoot.addOrReplaceChild(expectedLayer1);
    expectedRoot.addOrReplaceChild(expectedLayer2);

    expect(root).toEqual(expectedRoot);
  });

  function makePropertiesProvider(node: PropertyTreeNode): PropertiesProvider {
    return new PropertiesProvider(
      node,
      async () => node,
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
    );
  }
});
