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

import {Transform} from 'parsers/surface_flinger/transform_utils';
import {android} from 'protos/surfaceflinger/udc/static';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {Item} from 'trace/item';
import {TraceRect} from 'trace/trace_rect';
import {TraceRectBuilder} from 'trace/trace_rect_builder';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {RectsComputation} from './rects_computation';

describe('RectsComputation', () => {
  let hierarchyRoot: HierarchyTreeNode;
  let computation: RectsComputation;

  beforeEach(() => {
    hierarchyRoot = TreeNodeUtils.makeHierarchyNode({id: 'LayerTraceEntry', name: 'root'} as Item);
    computation = new RectsComputation();
  });

  it('makes layer rects', () => {
    const layer1Node = TreeNodeUtils.makeHierarchyNode({
      id: 1,
      name: 'layer1',
      z: 0,
      zOrderRelativeOf: -1,
      cornerRadius: 0,
      layerStack: 0,
      bounds: {left: 0, top: 0, right: 1, bottom: 1},
      parent: -1,
      children: [2],
      zOrderPath: [0],
      isVisible: true,
      transform: Transform.EMPTY,
    } as android.surfaceflinger.ILayerProto);

    layer1Node.addChild(
      TreeNodeUtils.makeHierarchyNode({
        id: 2,
        name: 'layer2',
        parent: 1,
        children: [],
        z: 1,
        zOrderRelativeOf: -1,
        cornerRadius: 2,
        layerStack: 0,
        bounds: {left: 0, top: 0, right: 2, bottom: 2},
        zOrderPath: [0, 1],
        occludedBy: [1],
        isVisible: false,
        transform: Transform.EMPTY,
      } as android.surfaceflinger.ILayerProto)
    );

    hierarchyRoot.addChild(layer1Node);

    hierarchyRoot.addChild(
      TreeNodeUtils.makeHierarchyNode({
        id: 4,
        name: 'layerRelativeZ',
        parent: 1,
        children: [],
        z: 2,
        zOrderRelativeOf: 1,
        cornerRadius: 0,
        layerStack: 0,
        bounds: {left: 0, top: 0, right: 5, bottom: 5},
        zOrderPath: [0, 2],
        isVisible: true,
        transform: Transform.EMPTY,
      } as android.surfaceflinger.ILayerProto)
    );

    const expectedRects: TraceRect[] = [
      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(1)
        .setHeight(1)
        .setId('1 layer1')
        .setName('layer1')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setZOrderPath([0])
        .setGroupId(0)
        .setIsVisible(true)
        .setIsDisplay(false)
        .setIsVirtual(false)
        .build(),

      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(2)
        .setHeight(2)
        .setId('2 layer2')
        .setName('layer2')
        .setCornerRadius(2)
        .setTransform(Transform.EMPTY.matrix)
        .setZOrderPath([0, 1])
        .setGroupId(0)
        .setIsVisible(false)
        .setIsDisplay(false)
        .setIsVirtual(false)
        .build(),

      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(5)
        .setHeight(5)
        .setId('4 layerRelativeZ')
        .setName('layerRelativeZ')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setZOrderPath([0, 2])
        .setGroupId(0)
        .setIsVisible(true)
        .setIsDisplay(false)
        .setIsVirtual(false)
        .build(),
    ];

    computation.setRoot(hierarchyRoot).executeInPlace();

    const rects: TraceRect[] = [];
    hierarchyRoot.forEachNodeDfs((node) => {
      if (node.id === 'LayerTraceEntry root') {
        return;
      }
      const nodeRects = node.getRects();
      if (nodeRects) rects.push(...nodeRects);
    });

    expect(rects).toEqual(expectedRects);
  });

  it('makes display rects', () => {
    const displays = TreeNodeUtils.makePropertyNode('LayerTraceEntry root.displays', 'displays', [
      {
        id: 1,
        layerStack: 0,
        size: {w: 5, h: 5},
        layerStackSpaceRect: {left: 0, top: 0, bottom: 5, right: 5},
        transform: Transform.EMPTY,
        name: 'Test Display',
      },
    ]);
    hierarchyRoot.addEagerProperty(displays);

    const expectedDisplayRects = [
      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(5)
        .setHeight(5)
        .setId('Display - 1')
        .setName('Test Display')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setZOrderPath([])
        .setGroupId(0)
        .setIsVisible(false)
        .setIsDisplay(true)
        .setIsVirtual(false)
        .build(),
    ];

    computation.setRoot(hierarchyRoot).executeInPlace();
    expect(hierarchyRoot.getRects()).toEqual(expectedDisplayRects);
  });
});
