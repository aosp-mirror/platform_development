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
import {TraceRect} from 'trace/trace_rect';
import {TraceRectBuilder} from 'trace/trace_rect_builder';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {RectsComputation} from './rects_computation';

describe('ViewCapture RectsComputation', () => {
  let hierarchyRoot: HierarchyTreeNode;
  let computation: RectsComputation;

  beforeEach(() => {
    hierarchyRoot = new HierarchyTreeBuilder()
      .setId('ViewNode')
      .setName('test.package.name@123456789')
      .setProperties({
        scaleX: 1,
        scaleY: 1,
        scrollX: 0,
        scrollY: 0,
        left: 0,
        top: 0,
        width: 1,
        height: 1,
        translationX: 0,
        translationY: 0,
        isComputedVisible: true,
        alpha: 1,
      })
      .setChildren([
        {
          id: 'ViewNode',
          name: 'test.package.name@234567891',
          properties: {
            scaleX: 1,
            scaleY: 1,
            scrollX: 0,
            scrollY: 0,
            left: 0,
            top: 0,
            width: 2,
            height: 2,
            translationX: 0,
            translationY: 0,
            alpha: 1,
          },
          children: [
            {
              id: 'ViewNode',
              name: 'test.package.name@345678912',
              properties: {
                scaleX: 1,
                scaleY: 1,
                scrollX: 0,
                scrollY: 0,
                left: 0,
                top: 0,
                width: 3,
                height: 3,
                translationX: 0,
                translationY: 0,
                alpha: 1,
              },
              children: [],
            },
          ],
        },
      ])
      .build();

    computation = new RectsComputation();
  });

  it('makes rects', () => {
    const expectedRects: TraceRect[] = [
      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(1)
        .setHeight(1)
        .setId('ViewNode test.package.name@123456789')
        .setName('test.package.name@123456789')
        .setCornerRadius(0)
        .setGroupId(0)
        .setIsVisible(true)
        .setIsDisplay(false)
        .setDepth(0)
        .setOpacity(1)
        .setIsSpy(false)
        .build(),

      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(2)
        .setHeight(2)
        .setId('ViewNode test.package.name@234567891')
        .setName('test.package.name@234567891')
        .setCornerRadius(0)
        .setGroupId(0)
        .setIsVisible(false)
        .setIsDisplay(false)
        .setDepth(4)
        .setOpacity(1)
        .setIsSpy(false)
        .build(),

      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(3)
        .setHeight(3)
        .setId('ViewNode test.package.name@345678912')
        .setName('test.package.name@345678912')
        .setCornerRadius(0)
        .setGroupId(0)
        .setIsVisible(false)
        .setIsDisplay(false)
        .setDepth(8)
        .setOpacity(1)
        .setIsSpy(false)
        .build(),
    ];

    computation.setRoot(hierarchyRoot).executeInPlace();

    const rects: TraceRect[] = [];
    hierarchyRoot.forEachNodeDfs((node) => {
      rects.push(...assertDefined(node.getRects()));
    });

    expect(rects).toEqual(expectedRects);
  });
});
