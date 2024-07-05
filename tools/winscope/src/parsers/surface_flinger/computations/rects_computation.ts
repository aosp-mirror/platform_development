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
import {Rect} from 'common/rect';
import {Transform} from 'parsers/surface_flinger/transform_utils';
import {TraceRect} from 'trace/trace_rect';
import {TraceRectBuilder} from 'trace/trace_rect_builder';
import {Computation} from 'trace/tree_node/computation';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

class RectSfFactory {
  makeDisplayRects(displays: readonly PropertyTreeNode[]): TraceRect[] {
    const nameCounts = new Map<string, number>();
    return displays.map((display, index) => {
      const layerStackSpaceRect = assertDefined(
        display.getChildByName('layerStackSpaceRect'),
      );
      const displayRect = Rect.from(layerStackSpaceRect);
      const layerStack = assertDefined(
        display.getChildByName('layerStack'),
      ).getValue();
      let displayName = display.getChildByName('name')?.getValue();
      if (!displayName) {
        displayName = 'Unknown Display';
      }
      const id = assertDefined(display.getChildByName('id')).getValue();

      const existingNameCount = nameCounts.get(displayName);
      if (existingNameCount !== undefined) {
        nameCounts.set(displayName, existingNameCount + 1);
        const qualifier = displayName === 'Unknown Display' ? '' : 'Mirror ';
        displayName += ` (${qualifier}${existingNameCount + 1})`;
      } else {
        nameCounts.set(displayName, 1);
      }

      return new TraceRectBuilder()
        .setX(displayRect.x)
        .setY(displayRect.y)
        .setWidth(displayRect.w)
        .setHeight(displayRect.h)
        .setId(`Display - ${id}`)
        .setName(displayName)
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setGroupId(layerStack)
        .setIsVisible(false)
        .setIsDisplay(true)
        .setIsVirtual(display.getChildByName('isVirtual')?.getValue() ?? false)
        .setDepth(index)
        .build();
    });
  }

  makeLayerRect(
    layer: HierarchyTreeNode,
    layerStack: number,
    absoluteZ: number,
  ): TraceRect {
    const isVisible = assertDefined(
      layer.getEagerPropertyByName('isComputedVisible'),
    ).getValue();

    const name = assertDefined(layer.getEagerPropertyByName('name')).getValue();
    const bounds = assertDefined(layer.getEagerPropertyByName('bounds'));
    const boundsRect = Rect.from(bounds);

    let opacity = layer
      .getEagerPropertyByName('color')
      ?.getChildByName('a')
      ?.getValue();
    if (isVisible && opacity === undefined) opacity = 0;

    return new TraceRectBuilder()
      .setX(boundsRect.x)
      .setY(boundsRect.y)
      .setWidth(boundsRect.w)
      .setHeight(boundsRect.h)
      .setId(
        `${assertDefined(
          layer.getEagerPropertyByName('id'),
        ).getValue()} ${name}`,
      )
      .setName(name)
      .setCornerRadius(
        layer.getEagerPropertyByName('cornerRadius')?.getValue() ?? 0,
      )
      .setTransform(
        Transform.from(assertDefined(layer.getEagerPropertyByName('transform')))
          .matrix,
      )
      .setGroupId(layerStack)
      .setIsVisible(isVisible)
      .setIsDisplay(false)
      .setIsVirtual(false)
      .setDepth(absoluteZ)
      .setOpacity(opacity)
      .build();
  }
}

export class RectsComputation implements Computation {
  private root: HierarchyTreeNode | undefined;
  private readonly rectsFactory = new RectSfFactory();

  setRoot(value: HierarchyTreeNode): this {
    this.root = value;
    return this;
  }

  getInvalidBoundsForDisplay(display: PropertyTreeNode): Rect {
    const displaySize = assertDefined(display.getChildByName('size'));
    const [invalidX, invalidY] = [
      assertDefined(displaySize.getChildByName('w')?.getValue()) * 10,
      assertDefined(displaySize.getChildByName('h')?.getValue()) * 10,
    ];
    return new Rect(-invalidX, -invalidY, invalidX * 2, invalidY * 2);
  }

  executeInPlace(): void {
    if (!this.root) {
      throw new Error('root not set in SF rects computation');
    }
    const groupIdToAbsoluteZ = new Map<number, number>();
    const invalidBoundsForDisplay = new Map<number, Rect>();

    const displays =
      this.root.getEagerPropertyByName('displays')?.getAllChildren() ?? [];
    const displayRects = this.rectsFactory.makeDisplayRects(displays);
    this.root.setRects(displayRects);

    displayRects.forEach((displayRect, i) => {
      groupIdToAbsoluteZ.set(displayRect.groupId, 1);
      invalidBoundsForDisplay.set(
        displayRect.groupId,
        this.getInvalidBoundsForDisplay(displays[i]),
      );
    });

    const layersWithRects = this.extractLayersWithRects(
      this.root,
      invalidBoundsForDisplay,
    );
    layersWithRects.sort(this.compareLayerZ);

    for (let i = layersWithRects.length - 1; i > -1; i--) {
      const layer = layersWithRects[i];
      const layerStack = assertDefined(
        layer.getEagerPropertyByName('layerStack'),
      ).getValue();
      const absoluteZ = groupIdToAbsoluteZ.get(layerStack) ?? 0;
      const rect = this.rectsFactory.makeLayerRect(
        layer,
        layerStack,
        absoluteZ,
      );
      layer.setRects([rect]);
      groupIdToAbsoluteZ.set(layerStack, absoluteZ + 1);
    }
  }

  private extractLayersWithRects(
    hierarchyRoot: HierarchyTreeNode,
    invalidBoundsForDisplay: Map<number, Rect>,
  ): HierarchyTreeNode[] {
    return hierarchyRoot.filterDfs((node) =>
      this.hasLayerRect(node, invalidBoundsForDisplay),
    );
  }

  private hasLayerRect(
    node: HierarchyTreeNode,
    invalidBoundsForDisplay: Map<number, Rect>,
  ): boolean {
    if (node.isRoot()) return false;

    const isVisible = node
      .getEagerPropertyByName('isComputedVisible')
      ?.getValue();
    if (isVisible === undefined) {
      throw new Error(
        'SF rects computation attempted before visibility computation',
      );
    }

    const screenBounds = node.getEagerPropertyByName('screenBounds');
    if (!screenBounds) return false;

    if (screenBounds && !isVisible) {
      const layerStack = assertDefined(
        node.getEagerPropertyByName('layerStack'),
      ).getValue();
      const invalidBounds = invalidBoundsForDisplay.get(layerStack);
      if (invalidBounds === undefined) return true;

      const screenBoundsRect = Rect.from(screenBounds);
      return !screenBoundsRect.isAlmostEqual(invalidBounds, 0.005);
    }

    return true;
  }

  private compareLayerZ(a: HierarchyTreeNode, b: HierarchyTreeNode): number {
    const aZOrderPath: number[] = assertDefined(
      a.getEagerPropertyByName('zOrderPath'),
    )
      .getAllChildren()
      .map((child) => child.getValue());
    const bZOrderPath: number[] = assertDefined(
      b.getEagerPropertyByName('zOrderPath'),
    )
      .getAllChildren()
      .map((child) => child.getValue());

    const zipLength = Math.min(aZOrderPath.length, bZOrderPath.length);
    for (let i = 0; i < zipLength; ++i) {
      const zOrderA = aZOrderPath[i];
      const zOrderB = bZOrderPath[i];
      if (zOrderA > zOrderB) return -1;
      if (zOrderA < zOrderB) return 1;
    }
    // When z-order is the same, the layer with larger ID is on top
    return a.id > b.id ? -1 : 1;
  }
}
