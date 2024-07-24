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
import {TraceRect} from 'trace/trace_rect';
import {TraceRectBuilder} from 'trace/trace_rect_builder';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

class RectVcFactory {
  private static DEPTH_MAGNIFICATION = 4;

  makeNodeRect(
    node: HierarchyTreeNode,
    leftShift: number,
    topShift: number,
    scaleX: number,
    scaleY: number,
    newScaleX: number,
    newScaleY: number,
    depth: number,
  ): TraceRect {
    const nodeLeft = assertDefined(
      node.getEagerPropertyByName('left'),
    ).getValue();
    const nodeTranslationX = assertDefined(
      node.getEagerPropertyByName('translationX'),
    ).getValue();
    const nodeWidth = assertDefined(
      node.getEagerPropertyByName('width'),
    ).getValue();

    const nodeTop = assertDefined(
      node.getEagerPropertyByName('top'),
    ).getValue();
    const nodeTranslationY = assertDefined(
      node.getEagerPropertyByName('translationY'),
    ).getValue();
    const nodeHeight = assertDefined(
      node.getEagerPropertyByName('height'),
    ).getValue();

    const rectLeft =
      leftShift +
      (nodeLeft + nodeTranslationX) * scaleX +
      (nodeWidth * (scaleX - newScaleX)) / 2;
    const rectTop =
      topShift +
      (nodeTop + nodeTranslationY) * scaleY +
      (nodeHeight * (scaleY - newScaleY)) / 2;

    const rect = new TraceRectBuilder()
      .setX(rectLeft)
      .setY(rectTop)
      .setWidth(nodeWidth * newScaleX)
      .setHeight(nodeHeight * newScaleY)
      .setId(node.id)
      .setName(node.name)
      .setCornerRadius(0)
      .setGroupId(0)
      .setIsVisible(
        node.getEagerPropertyByName('isComputedVisible')?.getValue() ?? false,
      )
      .setIsDisplay(false)
      .setIsVirtual(false)
      .setDepth(depth * RectVcFactory.DEPTH_MAGNIFICATION)
      .build();

    return rect;
  }
}
export const rectsFactory = new RectVcFactory();

export class RectsComputation {
  private readonly rectsFactory = new RectVcFactory();
  private root: HierarchyTreeNode | undefined;

  setRoot(value: HierarchyTreeNode): this {
    this.root = value;
    return this;
  }

  executeInPlace(): void {
    if (!this.root) {
      throw Error('root not set');
    }

    this.addRects(this.root, 0, 0, 1, 1, 0);
  }

  private addRects(
    node: HierarchyTreeNode,
    leftShift: number,
    topShift: number,
    scaleX: number,
    scaleY: number,
    depth: number,
  ) {
    const newScaleX =
      scaleX * assertDefined(node.getEagerPropertyByName('scaleX')).getValue();
    const newScaleY =
      scaleY * assertDefined(node.getEagerPropertyByName('scaleY')).getValue();

    const rect = this.rectsFactory.makeNodeRect(
      node,
      leftShift,
      topShift,
      scaleX,
      scaleY,
      newScaleX,
      newScaleY,
      depth,
    );
    node.setRects([rect]);

    node.getAllChildren().forEach((child) => {
      this.addRects(
        child,
        rect.x -
          assertDefined(node.getEagerPropertyByName('scrollX')).getValue(),
        rect.y -
          assertDefined(node.getEagerPropertyByName('scrollY')).getValue(),
        newScaleX,
        newScaleY,
        depth + 1,
      );
    });
  }
}
