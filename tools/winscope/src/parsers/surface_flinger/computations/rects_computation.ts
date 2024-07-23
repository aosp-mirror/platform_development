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
import {
  Transform,
  TransformUtils,
} from 'parsers/surface_flinger/transform_utils';
import {TraceRect} from 'trace/trace_rect';
import {TraceRectBuilder} from 'trace/trace_rect_builder';
import {Computation} from 'trace/tree_node/computation';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

function getDisplayWidthAndHeight(display: PropertyTreeNode): [number, number] {
  const displaySize = assertDefined(display.getChildByName('size'));
  const w = assertDefined(displaySize.getChildByName('w')?.getValue());
  const h = assertDefined(displaySize.getChildByName('h')?.getValue());
  const transformType =
    display.getChildByName('transform')?.getChildByName('type')?.getValue() ??
    0;
  const typeFlags = TransformUtils.getTypeFlags(transformType);
  const isRotated =
    typeFlags.includes('ROT_90') || typeFlags.includes('ROT_270');
  return [isRotated ? h : w, isRotated ? w : h];
}

// InputConfig constants defined in the platform:
//   frameworks/native/libs/input/android/os/InputConfig.aidl
export enum InputConfig {
  IS_WALLPAPER = 1 << 6,
  SPY = 1 << 14,
}

class RectSfFactory {
  static makeDisplayRects(displays: readonly PropertyTreeNode[]): TraceRect[] {
    const nameCounts = new Map<string, number>();
    return displays.map((display, index) => {
      const layerStackSpaceRect = assertDefined(
        display.getChildByName('layerStackSpaceRect'),
      );

      let displayRect = Rect.from(layerStackSpaceRect);
      const isEmptyLayerStackRect = displayRect.isEmpty();

      if (isEmptyLayerStackRect) {
        const [w, h] = getDisplayWidthAndHeight(display);
        displayRect = new Rect(0, 0, w, h);
      }

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
        .setDepth(index)
        .setIsSpy(false)
        .build();
    });
  }

  static makeLayerRect(
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
      .setDepth(absoluteZ)
      .setOpacity(opacity)
      .setIsSpy(false)
      .build();
  }

  static makeInputWindowRect(
    layer: HierarchyTreeNode,
    layerStack: number,
    absoluteZ: number,
    invalidBoundsFromDisplays: Rect[],
    display?: TraceRect,
  ): TraceRect {
    const name = assertDefined(layer.getEagerPropertyByName('name')).getValue();
    const inputWindowInfo = assertDefined(
      layer.getEagerPropertyByName('inputWindowInfo'),
    );
    let inputWindowRect = Rect.from(
      assertDefined(layer.getEagerPropertyByName('bounds')),
    );
    const inputConfig = assertDefined(
      inputWindowInfo.getChildByName('inputConfig'),
    ).getValue();

    const shouldCropToDisplay =
      (inputConfig & InputConfig.IS_WALLPAPER) !== 0 ||
      (invalidBoundsFromDisplays !== undefined &&
        invalidBoundsFromDisplays.some((invalid) =>
          inputWindowRect.isAlmostEqual(invalid, 0.01),
        ));
    if (shouldCropToDisplay && display !== undefined) {
      inputWindowRect = inputWindowRect.cropRect(display);
    }

    const isVisible =
      inputWindowInfo.getChildByName('visible')?.getValue() ??
      assertDefined(
        layer.getEagerPropertyByName('isComputedVisible'),
      ).getValue();

    return new TraceRectBuilder()
      .setX(inputWindowRect.x)
      .setY(inputWindowRect.y)
      .setWidth(inputWindowRect.w)
      .setHeight(inputWindowRect.h)
      .setId(`${assertDefined(layer.getEagerPropertyByName('id')).getValue()}`)
      .setName(name)
      .setCornerRadius(0)
      .setTransform(
        Transform.from(assertDefined(layer.getEagerPropertyByName('transform')))
          .matrix,
      )
      .setGroupId(layerStack)
      .setIsVisible(isVisible)
      .setIsDisplay(false)
      .setDepth(absoluteZ)
      .setIsSpy((inputConfig & InputConfig.SPY) !== 0)
      .build();
  }
}

export class RectsComputation implements Computation {
  private static readonly DEFAULT_INVALID_BOUNDS = new Rect(
    -50000,
    -50000,
    100000,
    100000,
  );

  private root?: HierarchyTreeNode;
  private displaysByLayerStack?: Map<number, TraceRect>;
  private invalidBoundsFromDisplays?: Rect[];

  setRoot(value: HierarchyTreeNode): this {
    this.root = value;
    return this;
  }

  // synced with getMaxDisplayBounds() in main/frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp
  private static getInvalidBoundsFromDisplays(
    displays: readonly PropertyTreeNode[],
  ): Rect[] {
    if (displays.length === 0) return [];
    const [maxX, maxY] = displays.reduce(
      (sizes, display) => {
        const [w, h] = getDisplayWidthAndHeight(display);
        return [Math.max(sizes[0], w), Math.max(sizes[1], h)];
      },
      [0, 0],
    );
    const [invalidX, invalidY] = [maxX * 10, maxY * 10];
    const invalidBounds = new Rect(
      -invalidX,
      -invalidY,
      invalidX * 2,
      invalidY * 2,
    );
    const rotatedInvalidBounds = new Rect(
      invalidBounds.y,
      invalidBounds.x,
      invalidBounds.h,
      invalidBounds.w,
    );
    return [invalidBounds, rotatedInvalidBounds];
  }

  executeInPlace(): void {
    this.processDisplays();
    this.processLayers(
      RectsComputation.hasLayerRect,
      RectSfFactory.makeLayerRect,
      true,
    );
    this.processLayers(
      RectsComputation.hasInputWindowRect,
      RectSfFactory.makeInputWindowRect,
      false,
    );
  }

  private processDisplays() {
    if (!this.root) {
      throw new Error('root not set in SF rects computation');
    }
    const displays =
      this.root.getEagerPropertyByName('displays')?.getAllChildren() ?? [];
    const displayRects = RectSfFactory.makeDisplayRects(displays);
    this.root.setRects(displayRects);

    this.displaysByLayerStack = new Map(
      displayRects.map((rect) => [rect.groupId, rect]),
    );

    this.invalidBoundsFromDisplays =
      RectsComputation.getInvalidBoundsFromDisplays(displays);
  }

  private processLayers(
    shouldIncludeLayer: (
      node: HierarchyTreeNode,
      invalidBoundsFromDisplays: Rect[],
    ) => boolean,
    makeTraceRect: (
      layer: HierarchyTreeNode,
      layerStack: number,
      absoluteZ: number,
      invalidBoundsFromDisplays: Rect[],
      display?: TraceRect,
    ) => TraceRect,
    isPrimaryRects: boolean,
  ) {
    const curAbsoluteZByLayerStack = new Map<number, number>();
    for (const layerStack of assertDefined(this.displaysByLayerStack).keys()) {
      curAbsoluteZByLayerStack.set(layerStack, 1);
    }

    const layersWithRects = assertDefined(this.root).filterDfs((node) =>
      shouldIncludeLayer(node, assertDefined(this.invalidBoundsFromDisplays)),
    );
    layersWithRects.sort(RectsComputation.compareLayerZ);

    for (let i = layersWithRects.length - 1; i > -1; i--) {
      const layer = layersWithRects[i];
      const layerStack = assertDefined(
        layer.getEagerPropertyByName('layerStack'),
      ).getValue();
      const absoluteZ = curAbsoluteZByLayerStack.get(layerStack) ?? 0;
      const rect = makeTraceRect(
        layer,
        layerStack,
        absoluteZ,
        assertDefined(this.invalidBoundsFromDisplays),
        this.displaysByLayerStack?.get(layerStack),
      );
      isPrimaryRects ? layer.setRects([rect]) : layer.setSecondaryRects([rect]);
      curAbsoluteZByLayerStack.set(layerStack, absoluteZ + 1);
    }
  }

  private static hasLayerRect(
    node: HierarchyTreeNode,
    invalidBoundsFromDisplays: Rect[],
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
      const screenBoundsRect = Rect.from(screenBounds);
      const isInvalidFromDisplays =
        invalidBoundsFromDisplays.length > 0 &&
        invalidBoundsFromDisplays.some((invalid) => {
          return screenBoundsRect.isAlmostEqual(invalid, 0.01);
        });
      return (
        !isInvalidFromDisplays &&
        !screenBoundsRect.isAlmostEqual(
          RectsComputation.DEFAULT_INVALID_BOUNDS,
          0.01,
        )
      );
    }

    return true;
  }

  private static hasInputWindowRect(node: HierarchyTreeNode): boolean {
    if (node.isRoot()) return false;
    const inputWindowInfo = node.getEagerPropertyByName('inputWindowInfo');
    return (
      inputWindowInfo !== undefined &&
      inputWindowInfo.getChildByName('inputConfig') !== undefined
    );
  }

  private static compareLayerZ(
    a: HierarchyTreeNode,
    b: HierarchyTreeNode,
  ): number {
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
