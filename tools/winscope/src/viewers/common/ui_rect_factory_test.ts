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
import {Transform} from 'parsers/surface_flinger/transform_utils';
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {TraceRectBuilder} from 'trace/trace_rect_builder';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {UiRect} from 'viewers/components/rects/types2d';
import {UiRectBuilder} from 'viewers/components/rects/ui_rect_builder';
import {UI_RECT_FACTORY} from './ui_rect_factory';

describe('UI_RECT_FACTORY', () => {
  let hierarchyRoot: HierarchyTreeNode;
  let layer1Node: HierarchyTreeNode;
  let layer2Node: HierarchyTreeNode;

  beforeEach(() => {
    hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        {id: 1, name: 'layer1'},
        {id: 2, name: 'layer2'},
      ])
      .build();
    layer1Node = assertDefined(hierarchyRoot.getChildByName('layer1'));
    layer2Node = assertDefined(hierarchyRoot.getChildByName('layer2'));
  });

  it('extracts rects from hierarchy tree', () => {
    buildRectAndSetToLayerNode(layer1Node, 0);
    buildRectAndSetToLayerNode(layer2Node, 1);

    const expectedlayer1UiRect = new UiRectBuilder()
      .setX(0)
      .setY(0)
      .setWidth(1)
      .setHeight(1)
      .setId('1 layer1')
      .setLabel('layer1')
      .setCornerRadius(0)
      .setGroupId(0)
      .setTransform(Transform.EMPTY.matrix)
      .setIsVisible(true)
      .setIsDisplay(false)
      .setIsClickable(true)
      .setIsVirtual(false)
      .setHasContent(false)
      .setDepth(0)
      .build();

    const expectedLayer2UiRect = new UiRectBuilder()
      .setX(0)
      .setY(0)
      .setWidth(1)
      .setHeight(1)
      .setId('2 layer2')
      .setLabel('layer2')
      .setCornerRadius(0)
      .setGroupId(0)
      .setTransform(Transform.EMPTY.matrix)
      .setIsVisible(true)
      .setIsDisplay(false)
      .setIsClickable(true)
      .setIsVirtual(false)
      .setHasContent(false)
      .setDepth(1)
      .build();

    const expectedRects: UiRect[] = [
      expectedlayer1UiRect,
      expectedLayer2UiRect,
    ];

    expect(UI_RECT_FACTORY.makeUiRects(hierarchyRoot)).toEqual(expectedRects);
  });

  it('handles depth order different to dfs order', () => {
    buildRectAndSetToLayerNode(layer1Node, 1);
    buildRectAndSetToLayerNode(layer2Node, 0);

    const expectedlayer1UiRect = new UiRectBuilder()
      .setX(0)
      .setY(0)
      .setWidth(1)
      .setHeight(1)
      .setId('1 layer1')
      .setLabel('layer1')
      .setCornerRadius(0)
      .setGroupId(0)
      .setTransform(Transform.EMPTY.matrix)
      .setIsVisible(true)
      .setIsDisplay(false)
      .setIsClickable(true)
      .setIsVirtual(false)
      .setHasContent(false)
      .setDepth(1)
      .build();

    const expectedLayer2UiRect = new UiRectBuilder()
      .setX(0)
      .setY(0)
      .setWidth(1)
      .setHeight(1)
      .setId('2 layer2')
      .setLabel('layer2')
      .setCornerRadius(0)
      .setGroupId(0)
      .setTransform(Transform.EMPTY.matrix)
      .setIsVisible(true)
      .setIsDisplay(false)
      .setIsClickable(true)
      .setIsVirtual(false)
      .setHasContent(false)
      .setDepth(0)
      .build();

    const expectedRects: UiRect[] = [
      expectedlayer1UiRect,
      expectedLayer2UiRect,
    ];
    expect(UI_RECT_FACTORY.makeUiRects(hierarchyRoot)).toEqual(expectedRects);
  });

  function buildRectAndSetToLayerNode(
    layerNode: HierarchyTreeNode,
    depth: number,
  ) {
    const rect = new TraceRectBuilder()
      .setX(0)
      .setY(0)
      .setWidth(1)
      .setHeight(1)
      .setId(layerNode.id)
      .setName(layerNode.name)
      .setCornerRadius(0)
      .setTransform(Transform.EMPTY.matrix)
      .setDepth(depth)
      .setGroupId(0)
      .setIsVisible(true)
      .setIsDisplay(false)
      .setIsVirtual(false)
      .build();

    layerNode.setRects([rect]);
  }
});
