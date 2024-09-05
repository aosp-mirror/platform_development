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
import {Rect} from 'common/geometry/rect';
import {Region} from 'common/geometry/region';
import {Size} from 'common/geometry/size';
import {TransformMatrix} from 'common/geometry/transform_matrix';
import {
  Transform,
  TransformType,
} from 'parsers/surface_flinger/transform_utils';
import {GeometryFactory} from 'trace/geometry_factory';
import {TraceRect} from 'trace/trace_rect';
import {TraceRectBuilder} from 'trace/trace_rect_builder';
import {Computation} from 'trace/tree_node/computation';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {LayerExtractor} from './layer_extractor';

function getDisplaySize(display: PropertyTreeNode): Size {
  const displaySize = assertDefined(display.getChildByName('size'));
  const w = assertDefined(displaySize.getChildByName('w')?.getValue());
  const h = assertDefined(displaySize.getChildByName('h')?.getValue());
  const transformType =
    display.getChildByName('transform')?.getChildByName('type')?.getValue() ??
    0;
  const typeFlags = TransformType.getTypeFlags(transformType);
  const isRotated =
    typeFlags.includes('ROT_90') || typeFlags.includes('ROT_270');
  return {
    width: isRotated ? h : w,
    height: isRotated ? w : h,
  };
}

// InputConfig constants defined in the platform:
//   frameworks/native/libs/input/android/os/InputConfig.aidl
export enum InputConfig {
  NOT_TOUCHABLE = 1 << 3,
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

      let displayRect = GeometryFactory.makeRect(layerStackSpaceRect);
      const isEmptyLayerStackRect = displayRect.isEmpty();

      if (isEmptyLayerStackRect) {
        const size = getDisplaySize(display);
        displayRect = new Rect(0, 0, size.width, size.height);
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

      const isOn = display.getChildByName('isOn')?.getValue() ?? false;
      const isVirtual =
        display.getChildByName('isVirtual')?.getValue() ?? false;

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
        .setIsActiveDisplay(isOn && !isVirtual)
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
    const boundsRect = GeometryFactory.makeRect(bounds);

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
        assertDefined(layer.getEagerPropertyByName('cornerRadius')).getValue(),
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
    displayTransform?: TransformMatrix,
  ): TraceRect {
    const name = assertDefined(layer.getEagerPropertyByName('name')).getValue();
    const inputWindowInfo = assertDefined(
      layer.getEagerPropertyByName('inputWindowInfo'),
    );
    let inputWindowRect = GeometryFactory.makeRect(
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

    const layerTransform = Transform.from(
      assertDefined(layer.getEagerPropertyByName('transform')),
    ).matrix;

    let touchableRegion: Region | undefined;
    const isTouchable = (inputConfig & InputConfig.NOT_TOUCHABLE) === 0;
    const touchableRegionNode =
      inputWindowInfo.getChildByName('touchableRegion');

    if (!isTouchable) {
      touchableRegion = Region.createEmpty();
    } else if (touchableRegionNode !== undefined) {
      // The touchable region is given in the display space, not layer space.
      touchableRegion = GeometryFactory.makeRegion(touchableRegionNode);
      // First, transform the region into layer stack space.
      touchableRegion =
        displayTransform?.transformRegion(touchableRegion) ?? touchableRegion;
      // Second, transform the region into layer space.
      touchableRegion = layerTransform
        .inverse()
        .transformRegion(touchableRegion);
      if (shouldCropToDisplay && display !== undefined) {
        touchableRegion = new Region(
          touchableRegion.rects.map((rect) => {
            return rect.cropRect(display);
          }),
        );
      }
    }

    return new TraceRectBuilder()
      .setX(inputWindowRect.x)
      .setY(inputWindowRect.y)
      .setWidth(inputWindowRect.w)
      .setHeight(inputWindowRect.h)
      .setId(`${assertDefined(layer.getEagerPropertyByName('id')).getValue()}`)
      .setName(name)
      .setCornerRadius(0)
      .setTransform(layerTransform)
      .setGroupId(layerStack)
      .setIsVisible(isVisible)
      .setIsDisplay(false)
      .setDepth(absoluteZ)
      .setIsSpy((inputConfig & InputConfig.SPY) !== 0)
      .setFillRegion(touchableRegion)
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
  private displayTransformsByLayerStack?: Map<number, TransformMatrix>;
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

    // foldables expand rects to fill display space before all displays are available
    // make invalid bounds for each individual display, and for the rect of max dimensions

    const invalidBounds: Rect[] = [];

    const maxSize = displays.reduce(
      (size, display) => {
        const displaySize = getDisplaySize(display);
        invalidBounds.push(
          ...RectsComputation.makeInvalidBoundsFromSize(displaySize),
        );
        return {
          width: Math.max(size.width, displaySize.width),
          height: Math.max(size.height, displaySize.height),
        };
      },
      {width: 0, height: 0},
    );
    invalidBounds.push(...RectsComputation.makeInvalidBoundsFromSize(maxSize));

    return invalidBounds;
  }

  private static makeInvalidBoundsFromSize(size: Size): Rect[] {
    const [invalidX, invalidY] = [size.width * 10, size.height * 10];
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

    this.displayTransformsByLayerStack = new Map();
    displays.forEach((display) => {
      const layerStack = assertDefined(
        display.getChildByName('layerStack'),
      ).getValue();
      const matrix = RectsComputation.extractDisplayTransform(display);
      if (matrix) {
        assertDefined(this.displayTransformsByLayerStack).set(
          layerStack,
          matrix,
        );
      }
    });
  }

  private static extractDisplayTransform(
    display: PropertyTreeNode,
  ): TransformMatrix | undefined {
    const transformNode = display.getChildByName('transform');
    const layerStackSpaceRectNode = assertDefined(
      display.getChildByName('layerStackSpaceRect'),
    );
    if (!transformNode || !layerStackSpaceRectNode) {
      return undefined;
    }
    const transform = Transform.from(transformNode);
    let tx = transform.matrix.tx;
    let ty = transform.matrix.ty;
    const layerStackSpaceRect = GeometryFactory.makeRect(
      layerStackSpaceRectNode,
    );

    const typeFlags = TransformType.getTypeFlags(transform.type);
    if (typeFlags.includes('ROT_180')) {
      tx += layerStackSpaceRect.w;
      ty += layerStackSpaceRect.h;
    } else if (typeFlags.includes('ROT_270')) {
      tx += layerStackSpaceRect.w;
    } else if (typeFlags.includes('ROT_90')) {
      ty += layerStackSpaceRect.h;
    }
    return TransformMatrix.from({tx, ty}, transform.matrix);
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
      displayTransform?: TransformMatrix,
    ) => TraceRect,
    isPrimaryRects: boolean,
  ) {
    const curAbsoluteZByLayerStack = new Map<number, number>();
    for (const layerStack of assertDefined(this.displaysByLayerStack).keys()) {
      curAbsoluteZByLayerStack.set(layerStack, 1);
    }

    const layersWithRects = LayerExtractor.extractLayersSortedByZ(
      assertDefined(this.root),
    ).filter((node) =>
      shouldIncludeLayer(node, assertDefined(this.invalidBoundsFromDisplays)),
    );

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
        this.displayTransformsByLayerStack?.get(layerStack),
      );
      isPrimaryRects ? layer.setRects([rect]) : layer.setSecondaryRects([rect]);
      curAbsoluteZByLayerStack.set(layerStack, absoluteZ + 1);
    }
  }

  private static hasLayerRect(
    node: HierarchyTreeNode,
    invalidBoundsFromDisplays: Rect[],
  ): boolean {
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
      const screenBoundsRect = GeometryFactory.makeRect(screenBounds);
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
    const inputWindowInfo = node.getEagerPropertyByName('inputWindowInfo');
    return (
      inputWindowInfo !== undefined &&
      inputWindowInfo.getChildByName('inputConfig') !== undefined
    );
  }
}
