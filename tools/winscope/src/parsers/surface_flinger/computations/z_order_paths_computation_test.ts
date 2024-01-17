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
import {android} from 'protos/surfaceflinger/udc/static';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {Item} from 'trace/item';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {ZOrderPathsComputation} from './z_order_paths_computation';

describe('ZOrderPathsComputation', () => {
  let hierarchyRoot: HierarchyTreeNode;
  let computation: ZOrderPathsComputation;

  beforeEach(() => {
    hierarchyRoot = TreeNodeUtils.makeHierarchyNode({
      id: 'LayerTraceEntry',
      name: 'root',
    } as Item);
    computation = new ZOrderPathsComputation();
  });

  it('calculates zOrderPath', () => {
    const layer1 = TreeNodeUtils.makeHierarchyNode({
      id: 1,
      name: 'layer1',
      parent: -1,
      children: [2, 4],
      z: 0,
      zOrderRelativeOf: -1,
    } as android.surfaceflinger.ILayerProto);
    const layer2 = TreeNodeUtils.makeHierarchyNode({
      id: 2,
      name: 'layer2',
      parent: 1,
      children: [3],
      z: 1,
      zOrderRelativeOf: -1,
    } as android.surfaceflinger.ILayerProto);
    const layer3 = TreeNodeUtils.makeHierarchyNode({
      id: 3,
      name: 'layer3',
      parent: 2,
      children: [],
      z: 1,
      zOrderRelativeOf: -1,
    } as android.surfaceflinger.ILayerProto);
    const layer4 = TreeNodeUtils.makeHierarchyNode({
      id: 4,
      name: 'layer4',
      parent: 1,
      children: [],
      z: 2,
      zOrderRelativeOf: 1,
    } as android.surfaceflinger.ILayerProto);
    layer1.setZParent(hierarchyRoot);
    hierarchyRoot.addChild(layer1);

    layer2.setZParent(layer1);
    layer1.addChild(layer2);

    layer3.setZParent(layer2);
    layer2.addChild(layer3);

    layer4.setZParent(layer1);
    layer1.addChild(layer4);

    const rootWithZOrderPaths = computation.setRoot(hierarchyRoot).execute();
    const layer1WithPath = assertDefined(rootWithZOrderPaths.getChildById('1 layer1'));
    const layer2WithPath = assertDefined(layer1WithPath.getChildById('2 layer2'));
    const layer3WithPath = assertDefined(layer2WithPath.getChildById('3 layer3'));
    const layer4WithPath = assertDefined(layer1WithPath.getChildById('4 layer4'));

    expect(getZOrderPathArray(layer1WithPath.getEagerPropertyByName('zOrderPath'))).toEqual([0]);
    expect(getZOrderPathArray(layer2WithPath.getEagerPropertyByName('zOrderPath'))).toEqual([0, 1]);
    expect(getZOrderPathArray(layer3WithPath.getEagerPropertyByName('zOrderPath'))).toEqual([
      0, 1, 1,
    ]);
    expect(getZOrderPathArray(layer4WithPath.getEagerPropertyByName('zOrderPath'))).toEqual([0, 2]);
  });

  function getZOrderPathArray(property: PropertyTreeNode | undefined): number[] {
    if (!property) return [];
    return property.getAllChildren().map((child) => Number(child.getValue()));
  }
});
