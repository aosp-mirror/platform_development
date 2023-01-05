/*
 * Copyright (C) 2022 The Android Open Source Project
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
import {TreeUtils} from 'common/tree_utils';
import {TraceTreeNode} from 'trace/trace_tree_node';
import {TreeTransformer} from 'viewers/common/tree_transformer';
import {DiffType, HierarchyTreeNode} from 'viewers/common/ui_tree_utils';

describe('TreeTransformer', () => {
  let entry: TraceTreeNode;
  let selectedTree: HierarchyTreeNode;
  beforeAll(async () => {
    entry = {
      id: 3,
      name: 'Child1',
      stackId: 0,
      isVisible: true,
      kind: '3',
      stableId: '3 Child1',
      proto: {
        barrierLayer: [],
        id: 3,
        parent: 1,
        type: 'ContainerLayer',
      },
      parent: undefined,
      children: [
        {
          id: 2,
          name: 'Child2',
          stackId: 0,
          parent: undefined,
          children: [],
          kind: '2',
          stableId: '2 Child2',
          proto: {
            barrierLayer: [],
            id: 2,
            parent: 3,
            type: 'ContainerLayer',
          },
          isVisible: true,
        },
      ],
    };

    selectedTree = {
      id: 3,
      name: 'Child1',
      stackId: 0,
      isVisible: true,
      kind: '3',
      stableId: '3 Child1',
      showInFilteredView: true,
      skip: null,
      chips: [],
      children: [
        {
          id: 2,
          name: 'Child2',
          stackId: 0,
          children: [],
          kind: '2',
          stableId: '2 Child2',
          isVisible: true,
          showInFilteredView: true,
          chips: [],
        },
      ],
    };
  });

  it('creates ordinary properties tree without show diff enabled', () => {
    const expected = {
      kind: '',
      name: 'Child1',
      stableId: '3 Child1',
      children: [
        {
          kind: '',
          name: 'id: 3',
          stableId: '3 Child1.id',
          children: [],
          combined: true,
          propertyKey: 'id',
          propertyValue: '3',
        },
        {
          kind: '',
          name: 'type: ContainerLayer',
          stableId: '3 Child1.type',
          children: [],
          combined: true,
          propertyKey: 'type',
          propertyValue: 'ContainerLayer',
        },
      ],
      propertyKey: 'Child1',
      propertyValue: null,
    };

    const filter = TreeUtils.makeNodeFilter('');
    const transformer = new TreeTransformer(selectedTree, filter)
      .setOnlyProtoDump(true)
      .setProperties(entry);

    const transformedTree = transformer.transform();
    expect(transformedTree).toEqual(expected);
  });

  it('creates properties tree with show diff enabled, comparing to a null previous entry', () => {
    const expected = {
      kind: '',
      name: 'Child1',
      stableId: '3 Child1',
      children: [
        {
          kind: '',
          name: 'id: 3',
          diffType: DiffType.ADDED,
          stableId: '3 Child1.id',
          children: [],
          combined: true,
          propertyKey: 'id',
          propertyValue: '3',
        },
        {
          kind: '',
          name: 'type: ContainerLayer',
          diffType: DiffType.ADDED,
          stableId: '3 Child1.type',
          children: [],
          combined: true,
          propertyKey: 'type',
          propertyValue: 'ContainerLayer',
        },
      ],
      diffType: DiffType.NONE,
      propertyKey: 'Child1',
      propertyValue: null,
    };

    const filter = TreeUtils.makeNodeFilter('');
    const transformer = new TreeTransformer(selectedTree, filter)
      .setIsShowDiff(true)
      .setOnlyProtoDump(true)
      .setProperties(entry)
      .setDiffProperties(null);

    const transformedTree = transformer.transform();
    expect(transformedTree).toEqual(expected);
  });
});
