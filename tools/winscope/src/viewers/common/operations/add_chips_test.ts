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

import {assertDefined} from 'common/assert_utils';
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {LayerCompositionType} from 'trace/layer_composition_type';
import {
  DUPLICATE_CHIP,
  GPU_CHIP,
  HWC_CHIP,
  MISSING_Z_PARENT_CHIP,
  RELATIVE_Z_CHIP,
  RELATIVE_Z_PARENT_CHIP,
  VISIBLE_CHIP,
} from 'viewers/common/chip';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {AddChips} from './add_chips';

describe('AddChips', () => {
  let hierarchyRoot: UiHierarchyTreeNode;
  let operation: AddChips;

  beforeEach(() => {
    operation = new AddChips();
  });

  it('adds GPU_CHIP', () => {
    hierarchyRoot = UiHierarchyTreeNode.from(
      new HierarchyTreeBuilder()
        .setId('test')
        .setName('node')
        .setChildren([
          {
            id: 1,
            name: 'node',
            properties: {compositionType: LayerCompositionType.GPU},
          },
        ])
        .build(),
    );

    operation.apply(hierarchyRoot);
    expect(
      assertDefined(hierarchyRoot.getChildByName('node')).getChips(),
    ).toEqual([GPU_CHIP]);
  });

  it('adds HWC_CHIP', () => {
    hierarchyRoot = UiHierarchyTreeNode.from(
      new HierarchyTreeBuilder()
        .setId('test')
        .setName('node')
        .setChildren([
          {
            id: 1,
            name: 'node',
            properties: {compositionType: LayerCompositionType.HWC},
          },
        ])
        .build(),
    );

    operation.apply(hierarchyRoot);
    expect(
      assertDefined(hierarchyRoot.getChildByName('node')).getChips(),
    ).toEqual([HWC_CHIP]);
  });

  it('adds VISIBLE_CHIP', () => {
    hierarchyRoot = UiHierarchyTreeNode.from(
      new HierarchyTreeBuilder()
        .setId('test')
        .setName('node')
        .setChildren([
          {
            id: 1,
            name: 'node',
            properties: {isComputedVisible: true},
          },
        ])
        .build(),
    );

    operation.apply(hierarchyRoot);
    expect(
      assertDefined(hierarchyRoot.getChildByName('node')).getChips(),
    ).toEqual([VISIBLE_CHIP]);
  });

  it('adds DUPLICATE_CHIP', () => {
    hierarchyRoot = UiHierarchyTreeNode.from(
      new HierarchyTreeBuilder()
        .setId('test')
        .setName('node')
        .setChildren([
          {
            id: 1,
            name: 'node',
            properties: {isDuplicate: true},
          },
        ])
        .build(),
    );

    operation.apply(hierarchyRoot);
    expect(
      assertDefined(hierarchyRoot.getChildByName('node')).getChips(),
    ).toEqual([DUPLICATE_CHIP]);
  });

  it('adds RELATIVE_Z_CHIP', () => {
    hierarchyRoot = UiHierarchyTreeNode.from(
      new HierarchyTreeBuilder()
        .setId('test')
        .setName('node')
        .setChildren([
          {
            id: 1,
            name: 'node',
            properties: {zOrderRelativeOf: 2},
          },
        ])
        .build(),
    );

    operation.apply(hierarchyRoot);
    expect(
      assertDefined(hierarchyRoot.getChildByName('node')).getChips(),
    ).toEqual([RELATIVE_Z_CHIP]);
  });

  it('adds MISSING_Z_PARENT_CHIP', () => {
    hierarchyRoot = UiHierarchyTreeNode.from(
      new HierarchyTreeBuilder()
        .setId('test')
        .setName('node')
        .setChildren([
          {
            id: 1,
            name: 'node',
            properties: {zOrderRelativeOf: 2, isMissingZParent: true},
          },
        ])
        .build(),
    );

    operation.apply(hierarchyRoot);
    expect(
      assertDefined(hierarchyRoot.getChildByName('node')).getChips(),
    ).toEqual([RELATIVE_Z_CHIP, MISSING_Z_PARENT_CHIP]);
  });

  it('adds RELATIVE_Z_PARENT_CHIP', () => {
    hierarchyRoot = UiHierarchyTreeNode.from(
      new HierarchyTreeBuilder()
        .setId('test')
        .setName('node')
        .setChildren([
          {
            id: 2,
            name: 'parentNode',
            properties: {id: 2, zOrderRelativeOf: -1},
            children: [
              {
                id: 1,
                name: 'node',
                properties: {id: 1, zOrderRelativeOf: 2},
              },
            ],
          },
        ])
        .build(),
    );

    operation.apply(hierarchyRoot);
    const parentWithChips = assertDefined(
      hierarchyRoot.getChildByName('parentNode'),
    );
    expect(parentWithChips.getChips()).toEqual([RELATIVE_Z_PARENT_CHIP]);
    expect(
      assertDefined(parentWithChips.getChildByName('node')).getChips(),
    ).toEqual([RELATIVE_Z_CHIP]);
  });
});
