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
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
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
    hierarchyRoot = TreeNodeUtils.makeUiHierarchyNode({
      id: 'test',
      name: 'root',
    });
    operation = new AddChips();
  });

  it('adds GPU_CHIP', () => {
    const layer = TreeNodeUtils.makeUiHierarchyNode({
      id: 1,
      name: 'node',
      compositionType: LayerCompositionType.GPU,
    });
    hierarchyRoot.addChild(layer);
    layer.setZParent(hierarchyRoot);

    operation.apply(hierarchyRoot);
    expect(assertDefined(hierarchyRoot.getChildByName('node')).getChips()).toEqual([GPU_CHIP]);
  });

  it('adds HWC_CHIP', () => {
    const layerDevice = TreeNodeUtils.makeUiHierarchyNode({
      id: 1,
      name: 'node',
      compositionType: LayerCompositionType.HWC,
    });
    hierarchyRoot.addChild(layerDevice);
    layerDevice.setZParent(hierarchyRoot);

    operation.apply(hierarchyRoot);
    expect(assertDefined(hierarchyRoot.getChildByName('node')).getChips()).toEqual([HWC_CHIP]);
  });

  it('adds VISIBLE_CHIP', () => {
    const layer = TreeNodeUtils.makeUiHierarchyNode({
      id: 1,
      name: 'node',
      isVisible: true,
    });
    hierarchyRoot.addChild(layer);
    layer.setZParent(hierarchyRoot);

    operation.apply(hierarchyRoot);
    expect(assertDefined(hierarchyRoot.getChildByName('node')).getChips()).toEqual([VISIBLE_CHIP]);
  });

  it('adds DUPLICATE_CHIP', () => {
    const layer = TreeNodeUtils.makeUiHierarchyNode({
      id: 1,
      name: 'node',
      isDuplicate: true,
    });
    hierarchyRoot.addChild(layer);
    layer.setZParent(hierarchyRoot);

    operation.apply(hierarchyRoot);
    expect(assertDefined(hierarchyRoot.getChildByName('node')).getChips()).toEqual([
      DUPLICATE_CHIP,
    ]);
  });

  it('adds RELATIVE_Z_CHIP', () => {
    const layer = TreeNodeUtils.makeUiHierarchyNode({
      id: 1,
      name: 'node',
      zOrderRelativeOf: 2,
    });
    hierarchyRoot.addChild(layer);
    layer.setZParent(hierarchyRoot);

    operation.apply(hierarchyRoot);
    expect(assertDefined(hierarchyRoot.getChildByName('node')).getChips()).toEqual([
      RELATIVE_Z_CHIP,
    ]);
  });

  it('adds MISSING_Z_PARENT_CHIP', () => {
    const layer = TreeNodeUtils.makeUiHierarchyNode({
      id: 1,
      name: 'node',
      zOrderRelativeOf: 2,
      isMissingZParent: true,
    });
    hierarchyRoot.addChild(layer);
    layer.setZParent(hierarchyRoot);

    operation.apply(hierarchyRoot);
    expect(assertDefined(hierarchyRoot.getChildByName('node')).getChips()).toEqual([
      RELATIVE_Z_CHIP,
      MISSING_Z_PARENT_CHIP,
    ]);
  });

  it('adds RELATIVE_Z_PARENT_CHIP', () => {
    const childLayer = TreeNodeUtils.makeUiHierarchyNode({
      id: 1,
      name: 'node',
      zOrderRelativeOf: 2,
    });
    const parentLayer = TreeNodeUtils.makeUiHierarchyNode({
      id: 2,
      name: 'parentNode',
      zOrderRelativeOf: -1,
    });
    parentLayer.addChild(childLayer);
    childLayer.setZParent(parentLayer);

    hierarchyRoot.addChild(parentLayer);
    parentLayer.setZParent(hierarchyRoot);

    operation.apply(hierarchyRoot);
    const parentWithChips = assertDefined(hierarchyRoot.getChildByName('parentNode'));
    expect(parentWithChips.getChips()).toEqual([RELATIVE_Z_PARENT_CHIP]);
    expect(assertDefined(parentWithChips.getChildByName('node')).getChips()).toEqual([
      RELATIVE_Z_CHIP,
    ]);
  });
});
