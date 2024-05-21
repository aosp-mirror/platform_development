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
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {UiRect} from 'viewers/components/rects/types2d';
import {UiRectBuilder} from 'viewers/components/rects/ui_rect_builder';
import {UI_RECT_FACTORY} from './ui_rect_factory';

describe('UI_RECT_FACTORY', () => {
  let hierarchyRoot: HierarchyTreeNode;
  let node1: HierarchyTreeNode;
  let node2: HierarchyTreeNode;

  beforeEach(() => {
    hierarchyRoot = new HierarchyTreeBuilder()
      .setId('TreeEntry')
      .setName('root')
      .setChildren([
        {id: 1, name: 'node1'},
        {id: 2, name: 'node2'},
      ])
      .build();
    node1 = assertDefined(hierarchyRoot.getChildByName('node1'));
    node2 = assertDefined(hierarchyRoot.getChildByName('node2'));
  });

  it('extracts rects from hierarchy tree', () => {
    buildRectAndSetToNode(node1, 0);
    buildRectAndSetToNode(node2, 1);

    const expectedUiRect1 = new UiRectBuilder()
      .setX(0)
      .setY(0)
      .setWidth(1)
      .setHeight(1)
      .setId('1 node1')
      .setLabel('node1')
      .setCornerRadius(0)
      .setGroupId(0)
      .setTransform(Transform.EMPTY.matrix)
      .setIsVisible(true)
      .setIsDisplay(false)
      .setIsClickable(true)
      .setIsVirtual(false)
      .setHasContent(false)
      .setDepth(0)
      .setOpacity(0.5)
      .build();

    const expectedUiRect2 = new UiRectBuilder()
      .setX(0)
      .setY(0)
      .setWidth(1)
      .setHeight(1)
      .setId('2 node2')
      .setLabel('node2')
      .setCornerRadius(0)
      .setGroupId(0)
      .setTransform(Transform.EMPTY.matrix)
      .setIsVisible(true)
      .setIsDisplay(false)
      .setIsClickable(true)
      .setIsVirtual(false)
      .setHasContent(false)
      .setDepth(1)
      .setOpacity(0.5)
      .build();

    const expectedRects: UiRect[] = [expectedUiRect1, expectedUiRect2];

    expect(UI_RECT_FACTORY.makeUiRects(hierarchyRoot)).toEqual(expectedRects);
  });

  it('handles depth order different to dfs order', () => {
    buildRectAndSetToNode(node1, 1);
    buildRectAndSetToNode(node2, 0);

    const expectedUiRect1 = new UiRectBuilder()
      .setX(0)
      .setY(0)
      .setWidth(1)
      .setHeight(1)
      .setId('1 node1')
      .setLabel('node1')
      .setCornerRadius(0)
      .setGroupId(0)
      .setTransform(Transform.EMPTY.matrix)
      .setIsVisible(true)
      .setIsDisplay(false)
      .setIsClickable(true)
      .setIsVirtual(false)
      .setHasContent(false)
      .setDepth(1)
      .setOpacity(0.5)
      .build();

    const expectedUiRect2 = new UiRectBuilder()
      .setX(0)
      .setY(0)
      .setWidth(1)
      .setHeight(1)
      .setId('2 node2')
      .setLabel('node2')
      .setCornerRadius(0)
      .setGroupId(0)
      .setTransform(Transform.EMPTY.matrix)
      .setIsVisible(true)
      .setIsDisplay(false)
      .setIsClickable(true)
      .setIsVirtual(false)
      .setHasContent(false)
      .setDepth(0)
      .setOpacity(0.5)
      .build();

    const expectedRects: UiRect[] = [expectedUiRect1, expectedUiRect2];
    expect(UI_RECT_FACTORY.makeUiRects(hierarchyRoot)).toEqual(expectedRects);
  });

  it('makes vc rects with trace type as groupId, content and empty label', () => {
    buildRectAndSetToNode(node1, 1);
    buildRectAndSetToNode(node2, 0);

    const expectedVcUiRect1 = new UiRectBuilder()
      .setX(0)
      .setY(0)
      .setWidth(1)
      .setHeight(1)
      .setId('1 node1')
      .setLabel('')
      .setCornerRadius(0)
      .setGroupId(TraceType.VIEW_CAPTURE_LAUNCHER_ACTIVITY)
      .setTransform(Transform.EMPTY.matrix)
      .setIsVisible(true)
      .setIsDisplay(false)
      .setIsClickable(true)
      .setIsVirtual(false)
      .setHasContent(true)
      .setDepth(1)
      .setOpacity(0.5)
      .build();

    const expectedVcUiRect2 = new UiRectBuilder()
      .setX(0)
      .setY(0)
      .setWidth(1)
      .setHeight(1)
      .setId('2 node2')
      .setLabel('')
      .setCornerRadius(0)
      .setGroupId(TraceType.VIEW_CAPTURE_LAUNCHER_ACTIVITY)
      .setTransform(Transform.EMPTY.matrix)
      .setIsVisible(true)
      .setIsDisplay(false)
      .setIsClickable(true)
      .setIsVirtual(false)
      .setHasContent(true)
      .setDepth(0)
      .setOpacity(0.5)
      .build();

    const expectedRects: UiRect[] = [expectedVcUiRect1, expectedVcUiRect2];
    expect(
      UI_RECT_FACTORY.makeVcUiRects(
        hierarchyRoot,
        TraceType.VIEW_CAPTURE_LAUNCHER_ACTIVITY,
      ),
    ).toEqual(expectedRects);
  });

  function buildRectAndSetToNode(node: HierarchyTreeNode, depth: number) {
    const rect = new TraceRectBuilder()
      .setX(0)
      .setY(0)
      .setWidth(1)
      .setHeight(1)
      .setId(node.id)
      .setName(node.name)
      .setCornerRadius(0)
      .setTransform(Transform.EMPTY.matrix)
      .setDepth(depth)
      .setGroupId(0)
      .setIsVisible(true)
      .setIsDisplay(false)
      .setIsVirtual(false)
      .setOpacity(0.5)
      .build();

    node.setRects([rect]);
  }
});
