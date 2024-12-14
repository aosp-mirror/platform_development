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
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {UiRect} from 'viewers/components/rects/ui_rect';
import {UiRectBuilder} from 'viewers/components/rects/ui_rect_builder';

class UiRectFactory {
  makeUiRects(
    hierarchyRoot: HierarchyTreeNode,
    viewCapturePackageNames: string[] = [],
  ): UiRect[] {
    const traceRects = this.extractRects(hierarchyRoot);
    return traceRects.map((traceRect) => {
      return new UiRectBuilder()
        .setX(traceRect.x)
        .setY(traceRect.y)
        .setWidth(traceRect.w)
        .setHeight(traceRect.h)
        .setLabel(traceRect.name)
        .setTransform(traceRect.transform)
        .setIsVisible(traceRect.isVisible)
        .setIsDisplay(traceRect.isDisplay)
        .setIsActiveDisplay(traceRect.isActiveDisplay)
        .setId(traceRect.id)
        .setGroupId(traceRect.groupId)
        .setIsClickable(!traceRect.isDisplay)
        .setCornerRadius(traceRect.cornerRadius)
        .setHasContent(
          viewCapturePackageNames.includes(
            traceRect.name.substring(0, traceRect.name.indexOf('/')),
          ),
        )
        .setDepth(traceRect.depth)
        .setOpacity(traceRect.opacity)
        .build();
    });
  }

  makeVcUiRects(hierarchyRoot: HierarchyTreeNode, groupId: number): UiRect[] {
    const traceRects = this.extractRects(hierarchyRoot).filter((traceRect) => {
      return traceRect.h > 0 && traceRect.w > 0;
    });
    return traceRects.map((traceRect) => {
      return new UiRectBuilder()
        .setX(traceRect.x)
        .setY(traceRect.y)
        .setWidth(traceRect.w)
        .setHeight(traceRect.h)
        .setLabel('')
        .setTransform(traceRect.transform)
        .setIsVisible(traceRect.isVisible)
        .setIsDisplay(traceRect.isDisplay)
        .setIsActiveDisplay(traceRect.isActiveDisplay)
        .setId(traceRect.id)
        .setGroupId(groupId)
        .setIsClickable(true)
        .setCornerRadius(traceRect.cornerRadius)
        .setHasContent(traceRect.isVisible)
        .setDepth(assertDefined(traceRect.depth))
        .setOpacity(traceRect.opacity)
        .build();
    });
  }

  makeInputRects(
    hierarchyRoot: HierarchyTreeNode,
    hasContent: (id: string) => boolean,
  ): UiRect[] {
    const traceRects = this.extractRects(hierarchyRoot, (node) =>
      // The root node contains the display rects, so use the primary rects for the displays.
      // For nodes, the input windows are the secondary rects.
      node.isRoot() ? node.getRects() : node.getSecondaryRects(),
    );
    return traceRects.map((traceRect) => {
      const opacity = traceRect.isDisplay ? 1 : traceRect.isSpy ? 0.25 : 0.9;
      return new UiRectBuilder()
        .setX(traceRect.x)
        .setY(traceRect.y)
        .setWidth(traceRect.w)
        .setHeight(traceRect.h)
        .setLabel(traceRect.name)
        .setTransform(traceRect.transform)
        .setIsVisible(traceRect.isVisible)
        .setIsDisplay(traceRect.isDisplay)
        .setIsActiveDisplay(traceRect.isActiveDisplay)
        .setId(traceRect.id)
        .setGroupId(traceRect.groupId)
        .setIsClickable(true)
        .setCornerRadius(traceRect.cornerRadius)
        .setHasContent(hasContent(traceRect.id))
        .setDepth(traceRect.depth)
        .setOpacity(opacity)
        .setFillRegion(traceRect.fillRegion)
        .build();
    });
  }

  private extractRects(
    hierarchyRoot: HierarchyTreeNode,
    extractNodeRects = (node: HierarchyTreeNode) => node.getRects(),
  ): TraceRect[] {
    const rects: TraceRect[] = [];

    hierarchyRoot.forEachNodeDfs((node) => {
      const nodeRects = extractNodeRects(node);
      if (nodeRects && nodeRects.length > 0) {
        rects.push(...nodeRects);
      }
    });

    return rects;
  }
}

export const UI_RECT_FACTORY = new UiRectFactory();
