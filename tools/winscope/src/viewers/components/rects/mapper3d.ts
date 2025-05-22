/*
 * Copyright (C) 2022 The Android Open Source Project
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
import {Box3D} from 'common/geometry/box3d';
import {Distance} from 'common/geometry/distance';
import {Point3D} from 'common/geometry/point3d';
import {Rect3D} from 'common/geometry/rect3d';
import {Size} from 'common/geometry/size';
import {
  IDENTITY_MATRIX,
  TransformMatrix,
} from 'common/geometry/transform_matrix';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiRect} from 'viewers/components/rects/ui_rect';
import {ColorType} from './color_type';
import {RectLabel} from './rect_label';
import {Scene} from './scene';
import {ShadingMode} from './shading_mode';
import {UiRect3D} from './ui_rect3d';

class Mapper3D {
  private static readonly CAMERA_ROTATION_FACTOR_INIT = 1;
  private static readonly DISPLAY_CLUSTER_SPACING = 750;
  private static readonly LABEL_FIRST_Y_OFFSET = 100;
  private static readonly LABEL_Y_OFFSET = 500;
  private static readonly SINGLE_LABEL_Y_OFFSET = 100;
  private static readonly LABEL_CIRCLE_RADIUS = 15;
  private static readonly MAX_RENDERED_LABELS = 30;
  private static readonly Y_AXIS_ROTATION_FACTOR = 1.5;
  private static readonly Z_FIGHTING_EPSILON = 5;
  private static readonly ZOOM_FACTOR_INIT = 1;
  private static readonly ZOOM_FACTOR_MIN = 0.1;
  private static readonly ZOOM_FACTOR_MAX = 30;
  private static readonly ZOOM_FACTOR_STEP = 0.2;
  private static readonly Z_SPACING_FACTOR_INIT = 1;
  private static readonly Z_SPACING_MAX = 200;

  private rects: UiRect[] = [];
  private highlightedRectId = '';
  private cameraRotationFactor = Mapper3D.CAMERA_ROTATION_FACTOR_INIT;
  private zSpacingFactor = Mapper3D.Z_SPACING_FACTOR_INIT;
  private zoomFactor = Mapper3D.ZOOM_FACTOR_INIT;
  private panScreenDistance = new Distance(0, 0);
  private currentGroupIds = [0]; // default stack id is usually 0
  private shadingModeIndex = 0;
  private allowedShadingModes: ShadingMode[] = [ShadingMode.GRADIENT];
  private pinnedItems: UiHierarchyTreeNode[] = [];
  private previousBoundingBox: Box3D | undefined;

  setRects(rects: UiRect[]) {
    this.rects = rects;
  }

  setPinnedItems(value: UiHierarchyTreeNode[]) {
    this.pinnedItems = value;
  }

  setHighlightedRectId(id: string) {
    this.highlightedRectId = id;
  }

  getCameraRotationFactor(): number {
    return this.cameraRotationFactor;
  }

  setCameraRotationFactor(factor: number) {
    this.cameraRotationFactor = Math.min(Math.max(factor, 0), 1);
  }

  getZSpacingFactor(): number {
    return this.zSpacingFactor;
  }

  setZSpacingFactor(factor: number) {
    this.zSpacingFactor = Math.min(Math.max(factor, 0), 1);
  }

  increaseZoomFactor(ratio: number) {
    this.zoomFactor += Mapper3D.ZOOM_FACTOR_STEP * ratio;
    this.zoomFactor = Math.min(this.zoomFactor, Mapper3D.ZOOM_FACTOR_MAX);
  }

  decreaseZoomFactor(ratio: number) {
    this.zoomFactor -= Mapper3D.ZOOM_FACTOR_STEP * ratio;
    this.zoomFactor = Math.max(this.zoomFactor, Mapper3D.ZOOM_FACTOR_MIN);
  }

  addPanScreenDistance(distance: Distance) {
    this.panScreenDistance.dx += distance.dx;
    this.panScreenDistance.dy += distance.dy;
  }

  resetToOrthogonalState() {
    this.cameraRotationFactor = Mapper3D.CAMERA_ROTATION_FACTOR_INIT;
    this.zSpacingFactor = Mapper3D.Z_SPACING_FACTOR_INIT;
  }

  resetCamera() {
    this.resetToOrthogonalState();
    this.zoomFactor = Mapper3D.ZOOM_FACTOR_INIT;
    this.panScreenDistance.dx = 0;
    this.panScreenDistance.dy = 0;
  }

  getCurrentGroupIds(): number[] {
    return this.currentGroupIds;
  }

  setCurrentGroupIds(ids: number[]) {
    this.currentGroupIds = ids;
  }

  setAllowedShadingModes(modes: ShadingMode[]) {
    this.allowedShadingModes = modes;
  }

  setShadingMode(newMode: ShadingMode) {
    const newModeIndex = this.allowedShadingModes.findIndex(
      (m) => m === newMode,
    );
    if (newModeIndex !== -1) {
      this.shadingModeIndex = newModeIndex;
    }
  }

  getShadingMode(): ShadingMode {
    return this.allowedShadingModes[this.shadingModeIndex];
  }

  updateShadingMode() {
    this.shadingModeIndex =
      this.shadingModeIndex < this.allowedShadingModes.length - 1
        ? this.shadingModeIndex + 1
        : 0;
  }

  isWireFrame(): boolean {
    return (
      this.allowedShadingModes.at(this.shadingModeIndex) ===
      ShadingMode.WIRE_FRAME
    );
  }

  isShadedByGradient(): boolean {
    return (
      this.allowedShadingModes.at(this.shadingModeIndex) ===
      ShadingMode.GRADIENT
    );
  }

  isShadedByOpacity(): boolean {
    return (
      this.allowedShadingModes.at(this.shadingModeIndex) === ShadingMode.OPACITY
    );
  }

  computeScene(updateBoundingBox: boolean): Scene {
    const rects3d: UiRect3D[] = [];
    const labels3d: RectLabel[] = [];
    let clusterYOffset = 0;
    for (const groupId of this.currentGroupIds) {
      const rects2dForGroupId = this.selectRectsToDraw(this.rects, groupId);
      rects2dForGroupId.sort(this.compareDepth); // decreasing order of depth
      const rects3dForGroupId = this.computeRects(
        rects2dForGroupId,
        clusterYOffset,
      );
      const labels3dForGroupId = this.computeLabels(
        rects2dForGroupId,
        rects3dForGroupId,
      );
      rects3d.push(...rects3dForGroupId);
      labels3d.push(...labels3dForGroupId);

      const boundingBox = this.computeBoundingBox(
        rects3dForGroupId,
        labels3dForGroupId,
      );
      clusterYOffset += boundingBox.height + Mapper3D.DISPLAY_CLUSTER_SPACING;
    }

    const newBoundingBox = this.computeBoundingBox(rects3d, labels3d);
    if (!this.previousBoundingBox || updateBoundingBox) {
      this.previousBoundingBox = newBoundingBox;
    }

    const {angleX, angleY} = this.getCameraRotationAngles();

    const scene: Scene = {
      boundingBox: this.previousBoundingBox,
      camera: {
        rotationAngleX: angleX,
        rotationAngleY: angleY,
        zoomFactor: this.zoomFactor,
        panScreenDistance: this.panScreenDistance,
      },
      rects: rects3d,
      labels: labels3d,
      zDepth: newBoundingBox.depth,
    };
    return scene;
  }

  private getCameraRotationAngles(): {angleX: number; angleY: number} {
    const angleX = (this.cameraRotationFactor * Math.PI * 45) / 360;
    return {
      angleX,
      angleY: angleX * Mapper3D.Y_AXIS_ROTATION_FACTOR,
    };
  }

  private compareDepth(a: UiRect, b: UiRect): number {
    if (a.isDisplay && !b.isDisplay) return 1;
    if (!a.isDisplay && b.isDisplay) return -1;
    return b.depth - a.depth;
  }

  private selectRectsToDraw(rects: UiRect[], groupId: number): UiRect[] {
    return rects.filter((rect) => rect.groupId === groupId);
  }

  private computeRects(rects2d: UiRect[], clusterYOffset: number): UiRect3D[] {
    let visibleRectsSoFar = 0;
    let visibleRectsTotal = 0;
    let nonVisibleRectsSoFar = 0;
    let nonVisibleRectsTotal = 0;

    rects2d.forEach((rect) => {
      if (rect.isVisible) {
        ++visibleRectsTotal;
      } else {
        ++nonVisibleRectsTotal;
      }
    });

    const maxDisplaySize = this.getMaxDisplaySize(rects2d);

    const depthToCountOfRects = new Map<number, number>();
    const computeAntiZFightingOffset = (rectDepth: number) => {
      // Rendering overlapping rects with equal Z value causes Z-fighting (b/307951779).
      // Here we compute a Z-offset to be applied to the rect to guarantee that
      // eventually all rects will have unique Z-values.
      const countOfRectsAtSameDepth = depthToCountOfRects.get(rectDepth) ?? 0;
      const antiZFightingOffset =
        countOfRectsAtSameDepth * Mapper3D.Z_FIGHTING_EPSILON;
      depthToCountOfRects.set(rectDepth, countOfRectsAtSameDepth + 1);
      return antiZFightingOffset;
    };

    let z = 0;
    const rects3d = rects2d.map((rect2d, i): UiRect3D => {
      const j = rects2d.length - 1 - i; // rects sorted in decreasing order of depth; increment z by L - 1 - i
      z =
        this.zSpacingFactor *
        (Mapper3D.Z_SPACING_MAX * j + computeAntiZFightingOffset(j));

      let darkFactor = 0;
      if (rect2d.isVisible) {
        darkFactor = this.isShadedByOpacity()
          ? assertDefined(rect2d.opacity)
          : (visibleRectsTotal - visibleRectsSoFar++) / visibleRectsTotal;
      } else {
        darkFactor = this.isShadedByOpacity()
          ? 0.5
          : (nonVisibleRectsTotal - nonVisibleRectsSoFar++) /
            nonVisibleRectsTotal;
      }
      let fillRegion: Rect3D[] | undefined;
      if (rect2d.fillRegion) {
        fillRegion = rect2d.fillRegion.rects.map((r) => {
          return {
            topLeft: new Point3D(r.x, r.y, z),
            bottomRight: new Point3D(r.x + r.w, r.y + r.h, z),
          };
        });
      }
      const transform = rect2d.transform ?? IDENTITY_MATRIX;

      const rect: UiRect3D = {
        id: rect2d.id,
        topLeft: new Point3D(rect2d.x, rect2d.y, z),
        bottomRight: new Point3D(rect2d.x + rect2d.w, rect2d.y + rect2d.h, z),
        isOversized: false,
        cornerRadius: rect2d.cornerRadius,
        darkFactor,
        colorType: this.getColorType(rect2d),
        isClickable: rect2d.isClickable,
        transform: clusterYOffset ? transform.addTy(clusterYOffset) : transform,
        fillRegion,
        isPinned: this.pinnedItems.some((node) => node.id === rect2d.id),
        pointerLocationsInRect: rect2d.pointerLocationsInRect.map((p) => {
          return new Point3D(p.x, p.y, z);
        }),
        rayLocationsInScene: rect2d.rayLocationsInDisplay.map((p) => {
          return new Point3D(p.x, p.y, z);
        }),
      };
      return this.cropOversizedRect(rect, maxDisplaySize);
    });

    return rects3d;
  }

  private getColorType(rect2d: UiRect): ColorType {
    if (this.isHighlighted(rect2d)) {
      if (this.isShadedByOpacity()) {
        return ColorType.HIGHLIGHTED_WITH_OPACITY;
      }
      return ColorType.HIGHLIGHTED;
    }
    if (this.isWireFrame()) {
      return ColorType.EMPTY;
    }
    if (rect2d.hasContent === true) {
      if (this.isShadedByOpacity()) {
        return ColorType.HAS_CONTENT_AND_OPACITY;
      }
      return ColorType.HAS_CONTENT;
    }
    if (rect2d.isVisible) {
      if (this.isShadedByOpacity()) {
        return ColorType.VISIBLE_WITH_OPACITY;
      }
      return ColorType.VISIBLE;
    }
    return ColorType.NOT_VISIBLE;
  }

  private getMaxDisplaySize(rects2d: UiRect[]): Size {
    const displays = rects2d.filter((rect2d) => rect2d.isDisplay);

    let maxWidth = 0;
    let maxHeight = 0;
    if (displays.length > 0) {
      maxWidth = Math.max(
        ...displays.map((rect2d): number => Math.abs(rect2d.w)),
      );

      maxHeight = Math.max(
        ...displays.map((rect2d): number => Math.abs(rect2d.h)),
      );
    }
    return {
      width: maxWidth,
      height: maxHeight,
    };
  }

  private cropOversizedRect(rect3d: UiRect3D, maxDisplaySize: Size): UiRect3D {
    // Arbitrary max size for a rect (1.5x the maximum display)
    let maxDimension = Number.MAX_VALUE;
    if (maxDisplaySize.height > 0) {
      maxDimension =
        Math.max(maxDisplaySize.width, maxDisplaySize.height) * 1.5;
    }

    const height = Math.abs(rect3d.topLeft.y - rect3d.bottomRight.y);
    const width = Math.abs(rect3d.topLeft.x - rect3d.bottomRight.x);

    if (width > maxDimension) {
      rect3d.isOversized = true;
      (rect3d.topLeft.x = (maxDimension - maxDisplaySize.width / 2) * -1),
        (rect3d.bottomRight.x = maxDimension);
    }
    if (height > maxDimension) {
      rect3d.isOversized = true;
      rect3d.topLeft.y = (maxDimension - maxDisplaySize.height / 2) * -1;
      rect3d.bottomRight.y = maxDimension;
    }

    return rect3d;
  }

  private computeLabels(rects2d: UiRect[], rects3d: UiRect3D[]): RectLabel[] {
    const labels3d: RectLabel[] = [];

    // Consider coordinate system where x-y plane is computer screen, and z-axis
    // goes into screen. To give final image, y and x axes are rotated by yRot
    // and xRot, in that order. Axes origin is (x0, y0, z0).
    // yRot: [Math.cos(angleY)    0                    Math.sin(angleY)]
    //       [0                   1                    0               ]
    //       [-Math.sin(angleY)   0                    Math.cos(angleY)]

    // xRot: [1                   0                    0               ]
    //       [0                   Math.cos(angleX)    -Math.sin(angleX)]
    //       [0                   Math.sin(angleX)     Math.cos(angleX)]
    const {angleX, angleY} = this.getCameraRotationAngles();

    const bottomRightCorners = rects3d.map((rect) =>
      rect.transform.transformPoint3D(rect.bottomRight),
    );
    const x0 = Math.max(...bottomRightCorners.map((p) => p.x)); // max x in image (furthest right in 2D plane)
    const y0 = Math.max(...bottomRightCorners.map((p) => p.y)); // max y in image (furthest down in 2D plane)
    const z0 = rects3d[0]?.bottomRight.z; // depth of first rect

    const onlyHighlighted = this.onlyRenderSelectedLabel(rects2d);
    const firstLabelScaleFactor = Math.max(
      Math.min(this.zoomFactor, 1 + (8 - rects2d.length) * 0.05),
      0.25,
    );
    const fixedYOffset =
      y0 + Mapper3D.LABEL_FIRST_Y_OFFSET / firstLabelScaleFactor;
    const multiLabelYOffset = Mapper3D.LABEL_Y_OFFSET / this.zoomFactor;
    const singleLabelYOffset = Mapper3D.SINGLE_LABEL_Y_OFFSET / this.zoomFactor;

    rects2d.forEach((rect2d, index) => {
      if (!rect2d.label) {
        return;
      }
      const isHighlighted = this.isHighlighted(rect2d);
      if (onlyHighlighted && !isHighlighted) {
        return;
      }
      const lineStart = this.getLabelLineStart(rects3d[index]);

      // To accurately offset label in y-direction of original coordinate
      // system seen by user, we:
      // - define vector V from axes origin to rect's corner where label starts
      // - project V into rotated coordinate system
      // - apply y-coordinate of projection as offset yOffsetXYRot.
      const xDiff = x0 - lineStart.x;
      const yDiff = Math.max(0, lineStart.y - y0);
      const zDiff = lineStart.z - z0;
      const V = [xDiff, yDiff, zDiff];
      const zOffsetYRot = V[0] * -Math.sin(angleY) + V[2] * Math.cos(angleY);
      const yOffsetXYRot =
        V[1] * Math.cos(angleX) + zOffsetYRot * -Math.sin(angleX);

      const yOffsetIndex = onlyHighlighted
        ? singleLabelYOffset
        : multiLabelYOffset;
      const lineEndY = fixedYOffset + yOffsetXYRot + index * yOffsetIndex;

      lineStart.x += Mapper3D.LABEL_CIRCLE_RADIUS / 2;

      const lineEnd = new Point3D(lineStart.x, lineEndY, lineStart.z);

      const RectLabel: RectLabel = {
        circle: {
          radius: Mapper3D.LABEL_CIRCLE_RADIUS,
          center: new Point3D(lineStart.x, lineStart.y, lineStart.z + 0.5),
        },
        linePoints: [lineStart, lineEnd],
        textCenter: lineEnd,
        text: rect2d.label,
        isHighlighted,
        rectId: rect2d.id,
      };
      labels3d.push(RectLabel);
    });

    return labels3d;
  }

  private getLabelLineStart(rect3d: UiRect3D): Point3D {
    const bottomLeft = new Point3D(
      rect3d.topLeft.x,
      rect3d.bottomRight.y - rect3d.topLeft.y,
      rect3d.topLeft.z,
    );
    const topRight = new Point3D(
      rect3d.bottomRight.x,
      rect3d.topLeft.y,
      rect3d.bottomRight.z,
    );
    const lineStarts = [
      rect3d.transform.transformPoint3D(rect3d.topLeft),
      rect3d.transform.transformPoint3D(rect3d.bottomRight),
      rect3d.transform.transformPoint3D(bottomLeft),
      rect3d.transform.transformPoint3D(topRight),
    ];
    let rightmostCoordinateIndex = 0;
    for (let i = 1; i < lineStarts.length; i++) {
      if (lineStarts[i].x > lineStarts[rightmostCoordinateIndex].x) {
        rightmostCoordinateIndex = i;
      }
    }
    return lineStarts[rightmostCoordinateIndex];
  }

  private computeBoundingBox(rects: UiRect3D[], labels: RectLabel[]): Box3D {
    if (rects.length === 0) {
      return {
        width: 1,
        height: 1,
        depth: 1,
        center: new Point3D(0, 0, 0),
        diagonal: Math.sqrt(3),
      };
    }

    let minX = Number.MAX_VALUE;
    let maxX = Number.MIN_VALUE;
    let minY = Number.MAX_VALUE;
    let maxY = Number.MIN_VALUE;
    let minZ = Number.MAX_VALUE;
    let maxZ = Number.MIN_VALUE;

    const updateMinMaxCoordinates = (
      point: Point3D,
      transform?: TransformMatrix,
    ) => {
      const transformedPoint = transform?.transformPoint3D(point) ?? point;
      minX = Math.min(minX, transformedPoint.x);
      maxX = Math.max(maxX, transformedPoint.x);
      minY = Math.min(minY, transformedPoint.y);
      maxY = Math.max(maxY, transformedPoint.y);
      minZ = Math.min(minZ, transformedPoint.z);
      maxZ = Math.max(maxZ, transformedPoint.z);
    };

    rects.forEach((rect) => {
      updateMinMaxCoordinates(rect.topLeft, rect.transform);
      updateMinMaxCoordinates(rect.bottomRight, rect.transform);
    });

    // if multiple labels rendered, include first 10 in bounding box
    if (!this.onlyRenderSelectedLabel(rects)) {
      labels.slice(0, 10).forEach((label) => {
        label.linePoints.forEach((point) => {
          updateMinMaxCoordinates(point);
        });
      });
    }

    const center = new Point3D(
      (minX + maxX) / 2,
      (minY + maxY) / 2,
      (minZ + maxZ) / 2,
    );

    const width = (maxX - minX) * 1.1;
    const height = (maxY - minY) * 1.1;
    const depth = (maxZ - minZ) * 1.1;

    return {
      width,
      height,
      depth,
      center,
      diagonal: Math.sqrt(width * width + height * height + depth * depth),
    };
  }

  isHighlighted(rect: UiRect): boolean {
    return rect.isClickable && this.highlightedRectId === rect.id;
  }

  private onlyRenderSelectedLabel(rects: Array<UiRect | UiRect3D>): boolean {
    return (
      rects.length > Mapper3D.MAX_RENDERED_LABELS ||
      this.currentGroupIds.length > 1
    );
  }
}

export {Mapper3D};
