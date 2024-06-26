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
import {perfetto} from 'protos/windowmanager/latest/static';
import {com} from 'protos/windowmanager/udc/static';
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {TraceRect} from 'trace/trace_rect';
import {TraceRectBuilder} from 'trace/trace_rect_builder';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {RectsComputation} from './rects_computation';

type DisplayContentProto =
  | com.android.server.wm.IDisplayContentProto
  | perfetto.protos.IDisplayContentProto;
type WindowStateProto =
  | com.android.server.wm.IWindowStateProto
  | perfetto.protos.IWindowStateProto;

describe('RectsComputation', () => {
  let hierarchyRoot: HierarchyTreeNode;
  let computation: RectsComputation;
  let displayContent: HierarchyTreeNode;

  beforeEach(() => {
    hierarchyRoot = new HierarchyTreeBuilder()
      .setId('WindowManagerState')
      .setName('root')
      .setChildren([
        {
          id: 1,
          name: 'Test Display',
          properties: {
            id: 1,
            name: 'Test Display',
            displayInfo: {
              logicalWidth: 5,
              logicalHeight: 5,
            },
          } as DisplayContentProto,
        },
      ])
      .build();
    displayContent = assertDefined(
      hierarchyRoot.getChildByName('Test Display'),
    );
    computation = new RectsComputation();
  });

  it('makes display rects', () => {
    const expectedDisplayRects = [
      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(5)
        .setHeight(5)
        .setId('1 Test Display')
        .setName('Display - Test Display')
        .setCornerRadius(0)
        .setDepth(0)
        .setGroupId(1)
        .setIsVisible(false)
        .setIsDisplay(true)
        .setIsVirtual(false)
        .build(),
    ];

    computation.setRoot(hierarchyRoot).executeInPlace();
    expect(displayContent.getRects()).toEqual(expectedDisplayRects);
  });

  it('makes window state rects', () => {
    const state1Node = TreeNodeUtils.makeHierarchyNode({
      id: 'WindowState',
      name: 'state1',
      displayId: 1,
      isComputedVisible: true,
      windowFrames: {
        frame: {left: 0, top: 0, right: 1, bottom: 1},
      },
      attributes: {
        alpha: 0.5,
      },
    } as WindowStateProto);

    state1Node.addOrReplaceChild(
      TreeNodeUtils.makeHierarchyNode({
        id: 'WindowState',
        name: 'state2',
        displayId: 1,
        isComputedVisible: false,
        windowFrames: {
          frame: {left: 0, top: 0, right: 2, bottom: 2},
        },
        attributes: {
          alpha: 0.5,
        },
      } as WindowStateProto),
    );
    displayContent.addOrReplaceChild(state1Node);

    const expectedRects: TraceRect[] = [
      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(1)
        .setHeight(1)
        .setId('WindowState state1')
        .setName('state1')
        .setCornerRadius(0)
        .setDepth(1)
        .setGroupId(1)
        .setIsVisible(true)
        .setIsDisplay(false)
        .setIsVirtual(false)
        .setOpacity(0.5)
        .build(),

      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(2)
        .setHeight(2)
        .setId('WindowState state2')
        .setName('state2')
        .setCornerRadius(0)
        .setDepth(2)
        .setGroupId(1)
        .setIsVisible(false)
        .setIsDisplay(false)
        .setIsVirtual(false)
        .setOpacity(0.5)
        .build(),
    ];

    computation.setRoot(hierarchyRoot).executeInPlace();

    const rects: TraceRect[] = [];
    hierarchyRoot.forEachNodeDfs((node) => {
      const nodeRects = node.getRects();
      if (nodeRects && nodeRects.every((rect) => !rect.isDisplay)) {
        rects.push(...nodeRects);
      }
    });

    expect(rects).toEqual(expectedRects);
  });
});
