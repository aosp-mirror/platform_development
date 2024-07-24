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

import {IDENTITY_MATRIX, TransformMatrix} from 'common/geometry_types';
import {Size, UiRect} from 'viewers/components/rects/types2d';
import {
  Box3D,
  ColorType,
  Distance2D,
  Label3D,
  Point3D,
  Rect3D,
  Scene3D,
} from './types3d';

class Mapper3D {
  private static readonly CAMERA_ROTATION_FACTOR_INIT = 1;
  private static readonly Z_FIGHTING_EPSILON = 5;
  private static readonly Z_SPACING_FACTOR_INIT = 1;
  private static readonly Z_SPACING_MAX = 200;
  private static readonly LABEL_FIRST_Y_OFFSET = 100;
  private static readonly LABEL_TEXT_Y_SPACING = 200;
  private static readonly LABEL_CIRCLE_RADIUS = 15;
  private static readonly ZOOM_FACTOR_INIT = 1;
  private static readonly ZOOM_FACTOR_MIN = 0.1;
  private static readonly ZOOM_FACTOR_MAX = 8.5;
  private static readonly ZOOM_FACTOR_STEP = 0.2;

  private rects: UiRect[] = [];
  private highlightedRectId = '';
  private cameraRotationFactor = Mapper3D.CAMERA_ROTATION_FACTOR_INIT;
  private zSpacingFactor = Mapper3D.Z_SPACING_FACTOR_INIT;
  private zoomFactor = Mapper3D.ZOOM_FACTOR_INIT;
  private panScreenDistance = new Distance2D(0, 0);
  private showOnlyVisibleMode = false; // by default show all
  private currentGroupId = 0; // default stack id is usually 0

  setRects(rects: UiRect[]) {
    this.rects = rects;
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

  increaseZoomFactor(times = 1) {
    this.zoomFactor += Mapper3D.ZOOM_FACTOR_STEP * times;
    this.zoomFactor = Math.min(this.zoomFactor, Mapper3D.ZOOM_FACTOR_MAX);
  }

  decreaseZoomFactor(times = 1) {
    this.zoomFactor -= Mapper3D.ZOOM_FACTOR_STEP * times;
    this.zoomFactor = Math.max(this.zoomFactor, Mapper3D.ZOOM_FACTOR_MIN);
  }

  addPanScreenDistance(distance: Distance2D) {
    this.panScreenDistance.dx += distance.dx;
    this.panScreenDistance.dy += distance.dy;
  }

  resetCamera() {
    this.cameraRotationFactor = Mapper3D.CAMERA_ROTATION_FACTOR_INIT;
    this.zSpacingFactor = Mapper3D.Z_SPACING_FACTOR_INIT;
    this.zoomFactor = Mapper3D.ZOOM_FACTOR_INIT;
    this.panScreenDistance.dx = 0;
    this.panScreenDistance.dy = 0;
  }

  getShowOnlyVisibleMode(): boolean {
    return this.showOnlyVisibleMode;
  }

  setShowOnlyVisibleMode(enabled: boolean) {
    this.showOnlyVisibleMode = enabled;
  }

  getCurrentGroupId(): number {
    return this.currentGroupId;
  }

  setCurrentGroupId(id: number) {
    this.currentGroupId = id;
  }

  private compareDepth(a: UiRect, b: UiRect): number {
    return a.depth > b.depth ? -1 : 1;
  }

  computeScene(): Scene3D {
    const rects2d = this.selectRectsToDraw(this.rects);
    rects2d.sort(this.compareDepth);
    const rects3d = this.computeRects(rects2d);
    const labels3d = this.computeLabels(rects2d, rects3d);
    const boundingBox = this.computeBoundingBox(rects3d, labels3d);

    const scene: Scene3D = {
      boundingBox,
      camera: {
        rotationFactor: this.cameraRotationFactor,
        zoomFactor: this.zoomFactor,
        panScreenDistance: this.panScreenDistance,
      },
      rects: rects3d,
      labels: labels3d,
    };

    return scene;
  }

  private selectRectsToDraw(rects: UiRect[]): UiRect[] {
    rects = rects.filter((rect) => rect.groupId === this.currentGroupId);

    if (this.showOnlyVisibleMode) {
      rects = rects.filter((rect) => rect.isVisible || rect.isDisplay);
    }

    return rects;
  }

  private computeRects(rects2d: UiRect[]): Rect3D[] {
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
    const rects3d = rects2d.map((rect2d): Rect3D => {
      z =
        this.zSpacingFactor *
        (Mapper3D.Z_SPACING_MAX * rect2d.depth +
          computeAntiZFightingOffset(rect2d.depth));

      const darkFactor = rect2d.isVisible
        ? (visibleRectsTotal - visibleRectsSoFar++) / visibleRectsTotal
        : (nonVisibleRectsTotal - nonVisibleRectsSoFar++) /
          nonVisibleRectsTotal;

      const rect = {
        id: rect2d.id,
        topLeft: {
          x: rect2d.x,
          y: rect2d.y,
          z,
        },
        bottomRight: {
          x: rect2d.x + rect2d.w,
          y: rect2d.y + rect2d.h,
          z,
        },
        isOversized: false,
        cornerRadius: rect2d.cornerRadius,
        darkFactor,
        colorType: this.getColorType(rect2d),
        isClickable: rect2d.isClickable,
        transform: rect2d.transform ?? IDENTITY_MATRIX,
      };
      return this.cropOversizedRect(rect, maxDisplaySize);
    });

    return rects3d;
  }

  private getColorType(rect2d: UiRect): ColorType {
    let colorType: ColorType;
    if (this.highlightedRectId === rect2d.id && rect2d.isClickable) {
      colorType = ColorType.HIGHLIGHTED;
    } else if (rect2d.hasContent === true) {
      colorType = ColorType.HAS_CONTENT;
    } else if (rect2d.isVisible) {
      colorType = ColorType.VISIBLE;
    } else {
      colorType = ColorType.NOT_VISIBLE;
    }
    return colorType;
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

  private cropOversizedRect(rect3d: Rect3D, maxDisplaySize: Size): Rect3D {
    // Arbitrary max size for a rect (2x the maximum display)
    let maxDimension = Number.MAX_VALUE;
    if (maxDisplaySize.height > 0) {
      maxDimension = Math.max(maxDisplaySize.width, maxDisplaySize.height) * 2;
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

  private computeLabels(rects2d: UiRect[], rects3d: Rect3D[]): Label3D[] {
    const labels3d: Label3D[] = [];

    let labelY =
      Math.max(
        ...rects3d.map((rect) => {
          return this.matMultiply(rect.transform, rect.bottomRight).y;
        }),
      ) + Mapper3D.LABEL_FIRST_Y_OFFSET;

    rects2d.forEach((rect2d, index) => {
      if (!rect2d.label) {
        return;
      }

      const rect3d = rects3d[index];

      const bottomLeft: Point3D = {
        x: rect3d.topLeft.x,
        y: rect3d.topLeft.y,
        z: rect3d.topLeft.z,
      };
      const topRight: Point3D = {
        x: rect3d.bottomRight.x,
        y: rect3d.bottomRight.y,
        z: rect3d.bottomRight.z,
      };
      const lineStarts = [
        this.matMultiply(rect3d.transform, rect3d.topLeft),
        this.matMultiply(rect3d.transform, rect3d.bottomRight),
        this.matMultiply(rect3d.transform, bottomLeft),
        this.matMultiply(rect3d.transform, topRight),
      ];
      let maxIndex = 0;
      for (let i = 1; i < lineStarts.length; i++) {
        if (lineStarts[i].x > lineStarts[maxIndex].x) {
          maxIndex = i;
        }
      }
      const lineStart = lineStarts[maxIndex];
      lineStart.x += Mapper3D.LABEL_CIRCLE_RADIUS / 2;

      const lineEnd: Point3D = {
        x: lineStart.x,
        y: labelY,
        z: lineStart.z,
      };

      const isHighlighted =
        rect2d.isClickable && this.highlightedRectId === rect2d.id;

      const label3d: Label3D = {
        circle: {
          radius: Mapper3D.LABEL_CIRCLE_RADIUS,
          center: {
            x: lineStart.x,
            y: lineStart.y,
            z: lineStart.z + 0.5,
          },
        },
        linePoints: [lineStart, lineEnd],
        textCenter: lineEnd,
        text: rect2d.label,
        isHighlighted,
        rectId: rect2d.id,
      };
      labels3d.push(label3d);

      labelY += Mapper3D.LABEL_TEXT_Y_SPACING;
    });

    return labels3d;
  }

  private matMultiply(mat: TransformMatrix, point: Point3D): Point3D {
    return {
      x: mat.dsdx * point.x + mat.dsdy * point.y + mat.tx,
      y: mat.dtdx * point.x + mat.dtdy * point.y + mat.ty,
      z: point.z,
    };
  }

  private computeBoundingBox(rects: Rect3D[], labels: Label3D[]): Box3D {
    if (rects.length === 0) {
      return {
        width: 1,
        height: 1,
        depth: 1,
        center: {x: 0, y: 0, z: 0},
        diagonal: Math.sqrt(3),
      };
    }

    let minX = Number.MAX_VALUE;
    let maxX = Number.MIN_VALUE;
    let minY = Number.MAX_VALUE;
    let maxY = Number.MIN_VALUE;
    let minZ = Number.MAX_VALUE;
    let maxZ = Number.MIN_VALUE;

    const updateMinMaxCoordinates = (point: Point3D) => {
      minX = Math.min(minX, point.x);
      maxX = Math.max(maxX, point.x);
      minY = Math.min(minY, point.y);
      maxY = Math.max(maxY, point.y);
      minZ = Math.min(minZ, point.z);
      maxZ = Math.max(maxZ, point.z);
    };

    rects.forEach((rect) => {
      /*const topLeft: Point3D = {
        x: rect.center.x - rect.width / 2,
        y: rect.center.y + rect.height / 2,
        z: rect.center.z
      };
      const bottomRight: Point3D = {
        x: rect.center.x + rect.width / 2,
        y: rect.center.y - rect.height / 2,
        z: rect.center.z
      };*/
      updateMinMaxCoordinates(rect.topLeft);
      updateMinMaxCoordinates(rect.bottomRight);
    });

    labels.forEach((label) => {
      label.linePoints.forEach((point) => {
        updateMinMaxCoordinates(point);
      });
    });

    const center: Point3D = {
      x: (minX + maxX) / 2,
      y: (minY + maxY) / 2,
      z: (minZ + maxZ) / 2,
    };

    const width = maxX - minX;
    const height = maxY - minY;
    const depth = maxZ - minZ;

    return {
      width,
      height,
      depth,
      center,
      diagonal: Math.sqrt(width * width + height * height + depth * depth),
    };
  }
}

export {Mapper3D};
