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

import {TraceRect} from 'trace/trace_rect';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {UiRect} from 'viewers/components/rects/types2d';
import {UiRectBuilder} from 'viewers/components/rects/ui_rect_builder';

export class PresenterSfUtils {
  static makeUiRects(
    hierarchyRoot: HierarchyTreeNode,
    viewCapturePackageNames: string[] = []
  ): UiRect[] {
    const traceRects = PresenterSfUtils.extractRects(hierarchyRoot);
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
        .setId(traceRect.id)
        .setDisplayId(traceRect.groupId)
        .setIsVirtual(traceRect.isVirtual)
        .setIsClickable(!traceRect.isDisplay)
        .setCornerRadius(traceRect.cornerRadius)
        .setHasContent(
          viewCapturePackageNames.includes(traceRect.name.substring(0, traceRect.name.indexOf('/')))
        )
        .build();
    });
  }

  private static extractRects(hierarchyRoot: HierarchyTreeNode): TraceRect[] {
    const rects: TraceRect[] = [];
    const displayRects: TraceRect[] = [];

    hierarchyRoot.forEachNodeDfs((node) => {
      if (node.id === 'LayerTraceEntry root') {
        const nodeRects = node.getRects();
        if (nodeRects) displayRects.push(...nodeRects);
      } else {
        const nodeRects = node.getRects();
        if (nodeRects) rects.push(...nodeRects);
      }
    });

    return rects.sort(PresenterSfUtils.compareLayerZ).concat(displayRects);
  }

  private static compareLayerZ(a: TraceRect, b: TraceRect): number {
    const zipLength = Math.min(a.zOrderPath.length, b.zOrderPath.length);
    for (let i = 0; i < zipLength; ++i) {
      const zOrderA = a.zOrderPath[i];
      const zOrderB = b.zOrderPath[i];
      if (zOrderA > zOrderB) return -1;
      if (zOrderA < zOrderB) return 1;
    }
    // When z-order is the same, the layer with larger ID is on top
    return a.id > b.id ? -1 : 1;
  }
}
