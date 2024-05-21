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
import {Computation} from 'trace/tree_node/computation';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

class RectWmFactory {
  makeDisplayRect(display: HierarchyTreeNode, absoluteZ: number): TraceRect {
    const displayInfo = display.getEagerPropertyByName('displayInfo');
    const displayRectWidth =
      displayInfo?.getChildByName('logicalWidth')?.getValue() ?? 0;
    const displayRectHeight =
      displayInfo?.getChildByName('logicalHeight')?.getValue() ?? 0;

    return new TraceRectBuilder()
      .setX(0)
      .setY(0)
      .setWidth(displayRectWidth)
      .setHeight(displayRectHeight)
      .setId(display.id)
      .setName(`Display - ${display.name}`)
      .setCornerRadius(0)
      .setGroupId(
        assertDefined(display.getEagerPropertyByName('id')).getValue(),
      )
      .setIsVisible(false)
      .setIsDisplay(true)
      .setIsVirtual(false)
      .setDepth(absoluteZ)
      .build();
  }

  makeWindowStateRect(
    container: HierarchyTreeNode,
    absoluteZ: number,
  ): TraceRect | undefined {
    const displayId = container.getEagerPropertyByName('displayId')?.getValue();
    if (displayId === undefined) {
      return undefined;
    }

    const isVisible =
      container.getEagerPropertyByName('isComputedVisible')?.getValue() ??
      false;

    const alpha =
      container
        .getEagerPropertyByName('attributes')
        ?.getChildByName('alpha')
        ?.getValue() ?? 1;

    const frame = container
      .getEagerPropertyByName('windowFrames')
      ?.getChildByName('frame');
    if (frame === undefined || frame.getAllChildren().length === 0) {
      return undefined;
    }

    const rectLeft = assertDefined(frame.getChildByName('left')).getValue();
    const rectTop = assertDefined(frame.getChildByName('top')).getValue();
    const rectRight = assertDefined(frame.getChildByName('right')).getValue();
    const rectBottom = assertDefined(frame.getChildByName('bottom')).getValue();

    return new TraceRectBuilder()
      .setX(rectLeft)
      .setY(rectTop)
      .setWidth(rectRight - rectLeft)
      .setHeight(rectBottom - rectTop)
      .setId(container.id)
      .setName(container.name)
      .setCornerRadius(0)
      .setGroupId(displayId)
      .setIsVisible(isVisible)
      .setIsDisplay(false)
      .setIsVirtual(false)
      .setDepth(absoluteZ)
      .setOpacity(alpha)
      .build();
  }
}

export class RectsComputation implements Computation {
  private root: HierarchyTreeNode | undefined;
  private readonly rectsFactory = new RectWmFactory();

  setRoot(value: HierarchyTreeNode): this {
    this.root = value;
    return this;
  }

  executeInPlace(): void {
    if (!this.root) {
      throw Error('root not set');
    }

    this.root.getAllChildren().forEach((displayContent) => {
      const displayRect = this.rectsFactory.makeDisplayRect(displayContent, 0);
      displayContent.setRects([displayRect]);

      let absoluteZ = 1;
      displayContent.getAllChildren().forEach((child) => {
        child.forEachNodeDfs((container) => {
          if (!container.id.startsWith('WindowState ')) return;

          const rect = this.rectsFactory.makeWindowStateRect(
            container,
            absoluteZ,
          );
          if (!rect) {
            return;
          }
          container.setRects([rect]);
          absoluteZ++;
        });
      });
    });
  }
}
