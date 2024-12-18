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
import {Box3D} from 'common/geometry/box3d';
import {Distance} from 'common/geometry/distance';
import {Point3D} from 'common/geometry/point3d';
import {IDENTITY_MATRIX} from 'common/geometry/transform_matrix';
import {
  TransformType,
  TransformTypeFlags,
} from 'parsers/surface_flinger/transform_utils';
import * as THREE from 'three';
import {CSS2DObject} from 'three/examples/jsm/renderers/CSS2DRenderer';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {Camera} from './camera';
import {Canvas} from './canvas';
import {ColorType} from './color_type';
import {RectLabel} from './rect_label';
import {UiRect3D} from './ui_rect3d';

describe('Canvas', () => {
  const rectId = 'rect1';

  describe('updateViewPosition', () => {
    let canvasRects: HTMLCanvasElement;
    let canvasLabels: HTMLElement;
    let canvas: Canvas;
    let canvasWidthSpy: jasmine.Spy;
    let canvasHeightSpy: jasmine.Spy;
    let camera: Camera;
    let boundingBox: Box3D;
    let graphicsScene: THREE.Scene;
    let graphicsCamera: THREE.OrthographicCamera;

    beforeEach(() => {
      canvasRects = document.createElement('canvas');
      canvasWidthSpy = spyOnProperty(
        canvasRects,
        'clientWidth',
      ).and.returnValue(100);
      canvasHeightSpy = spyOnProperty(
        canvasRects,
        'clientHeight',
      ).and.returnValue(100);
      canvasLabels = document.createElement('canvas');
      canvas = new Canvas(canvasRects, canvasLabels);
      camera = makeCamera();
      boundingBox = makeBoundingBox();
      [graphicsScene, graphicsCamera] = canvas.renderView();
    });

    it('handles zero size canvas element', () => {
      const canvasRendererSetSizeSpy = spyOn(canvas.renderer, 'setSize');
      canvasWidthSpy.and.returnValue(0);
      canvas.updateViewPosition(camera, boundingBox);
      expect(canvasRendererSetSizeSpy).not.toHaveBeenCalled();

      canvasWidthSpy.and.returnValue(100);
      canvasHeightSpy.and.returnValue(0);
      canvas.updateViewPosition(camera, boundingBox);
      expect(canvasRendererSetSizeSpy).not.toHaveBeenCalled();
    });

    it('changes camera lrtb and maintains scene translated position based on canvas aspect ratio', () => {
      camera.panScreenDistance = new Distance(2, 2);

      canvas.updateViewPosition(camera, boundingBox);
      const [l, r, t, b] = [
        graphicsCamera.left,
        graphicsCamera.right,
        graphicsCamera.top,
        graphicsCamera.bottom,
      ];
      const prevPosition = graphicsScene.position.clone();

      canvasWidthSpy.and.returnValue(200);
      canvas.updateViewPosition(camera, boundingBox);

      expect(graphicsCamera.left).toBeLessThan(l);
      expect(graphicsCamera.right).toBeGreaterThan(r);
      expect(graphicsCamera.top).toEqual(t);
      expect(graphicsCamera.bottom).toEqual(b);
      expect(graphicsScene.position).toEqual(prevPosition);

      canvasWidthSpy.and.returnValue(100);
      canvasHeightSpy.and.returnValue(200);
      canvas.updateViewPosition(camera, boundingBox);

      expect(graphicsCamera.left).toEqual(l);
      expect(graphicsCamera.right).toEqual(r);
      expect(graphicsCamera.top).toBeGreaterThan(t);
      expect(graphicsCamera.bottom).toBeLessThan(b);
      expect(graphicsScene.position).toEqual(prevPosition);
    });

    it('changes scene translated position on change in pan screen distance', () => {
      canvas.updateViewPosition(camera, boundingBox);
      const prevPosition = graphicsScene.position.clone();
      const prevScale = graphicsScene.scale.clone();

      camera.panScreenDistance = new Distance(2, 2);
      canvas.updateViewPosition(camera, boundingBox);

      expect(graphicsScene.position.x).toBeGreaterThan(prevPosition.x);
      expect(graphicsScene.position.y).toBeLessThan(prevPosition.y);
      expect(graphicsScene.position.z).toEqual(prevPosition.z);
      expect(graphicsScene.scale).toEqual(prevScale);
    });

    it('changes scene scale and scene translated position on change in zoom factor', () => {
      canvas.updateViewPosition(camera, boundingBox);
      const prevPosition = graphicsScene.position.clone();
      const sceneScale = graphicsScene.scale.clone();

      camera.zoomFactor = 2;
      canvas.updateViewPosition(camera, boundingBox);

      expect(graphicsScene.scale).toEqual(sceneScale.multiplyScalar(2));
      expect(graphicsScene.position).toEqual(prevPosition.multiplyScalar(2));
    });

    it('changes camera position and scene translated x-position on change in rotation angle x', () => {
      canvas.updateViewPosition(camera, boundingBox);
      const prevScenePos = graphicsScene.position.clone();
      const prevCameraPos = graphicsCamera.position.clone();

      camera.rotationAngleX = 1.5;
      canvas.updateViewPosition(camera, boundingBox);

      expect(graphicsScene.position.x).toBeLessThan(prevScenePos.x);
      expect(graphicsScene.position.y).toEqual(prevScenePos.y);
      expect(graphicsScene.position.z).toEqual(prevScenePos.z);

      expect(graphicsCamera.position.x).toEqual(prevCameraPos.x);
      expect(graphicsCamera.position.y).toBeGreaterThan(prevCameraPos.y);
      expect(graphicsCamera.position.z).toBeLessThan(prevCameraPos.z);
    });

    it('changes camera position and scene translated y-position on change in rotation angle y', () => {
      canvas.updateViewPosition(camera, boundingBox);
      const prevScenePos = graphicsScene.position.clone();
      const prevCameraPos = graphicsCamera.position.clone();

      camera.rotationAngleY = 1.5;
      canvas.updateViewPosition(camera, boundingBox);

      expect(graphicsScene.position.x).toEqual(prevScenePos.x);
      expect(graphicsScene.position.y).toBeLessThan(prevScenePos.y);
      expect(graphicsScene.position.z).toEqual(prevScenePos.z);

      expect(graphicsCamera.position.x).toBeGreaterThan(prevCameraPos.x);
      expect(graphicsCamera.position.y).toEqual(prevCameraPos.y);
      expect(graphicsCamera.position.z).toBeLessThan(prevCameraPos.z);
    });

    it('changes scene scale and translated position on change in box diagonal', () => {
      canvas.updateViewPosition(camera, boundingBox);
      const prevPosition = graphicsScene.position.clone();
      const sceneScale = graphicsScene.scale.clone();

      boundingBox.diagonal = 2;
      canvas.updateViewPosition(camera, boundingBox);

      expect(graphicsScene.scale).toEqual(sceneScale.multiplyScalar(0.5));
      expect(graphicsScene.position).toEqual(prevPosition.multiplyScalar(0.5));
    });

    it('changes translated position on change in box depth', () => {
      camera.rotationAngleX = 1;
      camera.rotationAngleY = 1;
      canvas.updateViewPosition(camera, boundingBox);
      const prevPosition = graphicsScene.position.clone();
      const prevScale = graphicsScene.scale.clone();

      boundingBox.depth = 2;
      canvas.updateViewPosition(camera, boundingBox);
      expect(graphicsScene.position).toEqual(prevPosition.multiplyScalar(2));
      expect(graphicsScene.scale).toEqual(prevScale);
    });

    it('changes translated position on change in box center', () => {
      camera.rotationAngleX = 1;
      camera.rotationAngleY = 1;
      canvas.updateViewPosition(camera, boundingBox);
      const prevPosition = graphicsScene.position.clone();
      const prevScale = graphicsScene.scale.clone();

      boundingBox.center = new Point3D(3, 3, 3);
      canvas.updateViewPosition(camera, boundingBox);
      expect(graphicsScene.position).not.toEqual(prevPosition);
      expect(graphicsScene.scale).toEqual(prevScale);
    });

    it('robust to no labels canvas', () => {
      const canvas = new Canvas(canvasRects);
      canvas.updateViewPosition(makeCamera(), makeBoundingBox());
    });
  });

  describe('updateRects', () => {
    let canvas: Canvas;
    let isDarkMode: boolean;
    let graphicsScene: THREE.Scene;

    beforeEach(() => {
      isDarkMode = false;
      const canvasRects = document.createElement('canvas');
      const canvasLabels = document.createElement('canvas');
      canvas = new Canvas(canvasRects, canvasLabels, () => isDarkMode);
      graphicsScene = canvas.renderView()[0];
    });

    it('adds and removes rects', () => {
      const mapDeleteSpy = spyOn(Map.prototype, 'delete').and.callThrough();
      canvas.updateRects([]);
      expect(graphicsScene.getObjectByName(rectId)).toBeUndefined();
      canvas.updateRects([makeUiRect3D(rectId)]);
      expect(graphicsScene.getObjectByName(rectId)).toBeDefined();
      canvas.updateRects([]);
      expect(graphicsScene.getObjectByName(rectId)).toBeUndefined();
      expect(mapDeleteSpy).toHaveBeenCalledOnceWith(rectId);
    });

    it('updates existing rects instead of adding new rect', () => {
      const rect = makeUiRect3D(rectId);
      canvas.updateRects([rect]);
      const rectMesh = getRectMesh(rectId);
      expect(rectMesh.position.z).toEqual(0);

      const newRect = makeUiRect3D(rectId);
      newRect.topLeft = new Point3D(0, 0, 1);
      canvas.updateRects([newRect]);
      expect(rectMesh.position.z).toEqual(1);
      expect(getRectMesh('rect1')).toEqual(rectMesh);
    });

    it('makes rect with correct position and borders', () => {
      const rect = makeUiRect3D(rectId);
      rect.topLeft = new Point3D(1, 1, 5);
      rect.bottomRight = new Point3D(2, 2, 5);
      canvas.updateRects([rect]);
      const rectMesh = getRectMesh(rectId);
      expect(rectMesh.position.z).toEqual(5);
      checkBorderColor(rectId, Canvas.RECT_EDGE_COLOR_LIGHT_MODE);

      isDarkMode = true;
      canvas.updateRects([rect]);
      checkBorderColor(rectId, Canvas.RECT_EDGE_COLOR_DARK_MODE);
    });

    it('makes rect with correct fill material', () => {
      const rect = makeUiRect3D(rectId);
      canvas.updateRects([rect]);
      const rectMesh = getRectMesh(rectId);
      const defaultVisibleRectColor = new THREE.Color(
        200 / 255,
        232 / 255,
        183 / 255,
      );
      checkMaterialColorAndOpacity(
        rectMesh,
        defaultVisibleRectColor,
        Canvas.OPACITY_REGULAR,
      );

      const visibleWithOpacity = makeUiRect3D(rectId);
      visibleWithOpacity.colorType = ColorType.VISIBLE_WITH_OPACITY;
      canvas.updateRects([visibleWithOpacity]);
      const material = rectMesh.material as THREE.MeshBasicMaterial;
      expect(material.color).not.toEqual(defaultVisibleRectColor);
      expect(material.opacity).toEqual(1);

      const nonVisible = makeUiRect3D(rectId);
      nonVisible.colorType = ColorType.NOT_VISIBLE;
      canvas.updateRects([nonVisible]);
      checkMaterialColorAndOpacity(
        rectMesh,
        new THREE.Color(220 / 255, 220 / 255, 220 / 255),
        Canvas.OPACITY_REGULAR,
      );

      const highlighted = makeUiRect3D(rectId);
      highlighted.colorType = ColorType.HIGHLIGHTED;
      canvas.updateRects([highlighted]);
      checkMaterialColorAndOpacity(
        rectMesh,
        Canvas.RECT_COLOR_HIGHLIGHTED_LIGHT_MODE,
        Canvas.OPACITY_REGULAR,
      );
      isDarkMode = true;
      canvas.updateRects([highlighted]);
      checkMaterialColorAndOpacity(
        rectMesh,
        Canvas.RECT_COLOR_HIGHLIGHTED_DARK_MODE,
        Canvas.OPACITY_REGULAR,
      );

      const contentAndOpacity = makeUiRect3D(rectId);
      contentAndOpacity.colorType = ColorType.HAS_CONTENT_AND_OPACITY;
      canvas.updateRects([contentAndOpacity]);
      checkMaterialColorAndOpacity(rectMesh, Canvas.RECT_COLOR_HAS_CONTENT, 1);

      const content = makeUiRect3D(rectId);
      content.colorType = ColorType.HAS_CONTENT;
      canvas.updateRects([content]);
      checkMaterialColorAndOpacity(
        rectMesh,
        Canvas.RECT_COLOR_HAS_CONTENT,
        Canvas.OPACITY_REGULAR,
      );

      const oversized = makeUiRect3D(rectId);
      oversized.colorType = ColorType.HAS_CONTENT;
      oversized.isOversized = true;
      canvas.updateRects([oversized]);
      checkMaterialColorAndOpacity(
        rectMesh,
        Canvas.RECT_COLOR_HAS_CONTENT,
        Canvas.OPACITY_OVERSIZED,
      );

      const empty = makeUiRect3D(rectId);
      empty.colorType = ColorType.EMPTY;
      canvas.updateRects([empty]);
      expect(rectMesh.material).toEqual(Canvas.TRANSPARENT_MATERIAL);
    });

    it('makes rect with fill region', () => {
      const rect = makeUiRect3D(rectId);
      rect.fillRegion = [];
      rect.colorType = ColorType.HAS_CONTENT;
      canvas.updateRects([rect]);
      const rectMesh = getRectMesh(rectId);
      expect(rectMesh.material).toEqual(Canvas.TRANSPARENT_MATERIAL);

      const fillRegionMesh = getFillRegionMesh(rectId);
      expect(fillRegionMesh.position.z).toEqual(1);
      checkMaterialColorAndOpacity(
        fillRegionMesh,
        Canvas.RECT_COLOR_HAS_CONTENT,
        Canvas.OPACITY_REGULAR,
      );
    });

    it('makes rect with pinned borders', () => {
      const rect = makeUiRect3D(rectId);
      rect.topLeft = new Point3D(1, 1, 5);
      rect.bottomRight = new Point3D(2, 2, 5);
      rect.isPinned = true;

      const rect2 = makeUiRect3D('rect2');
      rect2.topLeft = new Point3D(1, 1, 5);
      rect2.bottomRight = new Point3D(2, 2, 5);
      rect2.isPinned = true;
      canvas.updateRects([rect, rect2]);

      checkBorderColor(rect.id, Canvas.RECT_EDGE_COLOR_PINNED);
      checkBorderColor(rect2.id, Canvas.RECT_EDGE_COLOR_PINNED_ALT);
    });

    it('handles changes in geometry', () => {
      const rect = makeUiRect3D(rectId);
      canvas.updateRects([rect]);
      const rectMesh = getRectMesh(rectId);
      let rectGeometryId = rectMesh.geometry.id;

      // no change
      canvas.updateRects([rect]);
      expect(rectMesh.geometry.id).toEqual(rectGeometryId);

      // geometry object replaced
      const roundRect = makeUiRect3D(rectId);
      roundRect.cornerRadius = 5;
      updateRectsAndCheckGeometryId(roundRect, rectMesh, rectGeometryId);
      rectGeometryId = rectMesh.geometry.id;

      const bottomRightChanged = makeUiRect3D(rectId);
      bottomRightChanged.cornerRadius = 5;
      bottomRightChanged.bottomRight = new Point3D(5, 5, 5);
      updateRectsAndCheckGeometryId(
        bottomRightChanged,
        rectMesh,
        rectGeometryId,
      );
      rectGeometryId = rectMesh.geometry.id;

      const topLeftChanged = makeUiRect3D(rectId);
      topLeftChanged.cornerRadius = 5;
      topLeftChanged.bottomRight = new Point3D(5, 5, 5);
      topLeftChanged.topLeft = new Point3D(0, 0, 5);
      updateRectsAndCheckGeometryId(topLeftChanged, rectMesh, rectGeometryId);
      rectGeometryId = rectMesh.geometry.id;

      const rotated = makeUiRect3D(rectId);
      rotated.cornerRadius = 5;
      rotated.bottomRight = new Point3D(5, 5, 5);
      rotated.topLeft = new Point3D(0, 0, 5);
      rotated.transform = TransformType.getDefaultTransform(
        TransformTypeFlags.ROT_90_VAL,
        2,
        2,
      ).matrix;
      const prevRotation = rectMesh.rotation.clone();
      canvas.updateRects([rotated]);
      expect(rectMesh.geometry.id).toEqual(rectGeometryId);
      expect(rectMesh.rotation.equals(prevRotation)).toBeFalse();
    });

    it('handles changes in fill region', () => {
      const noFillRegion = makeUiRect3D(rectId);
      canvas.updateRects([noFillRegion]);
      const rectMesh = getRectMesh(rectId);
      expect(rectMesh.getObjectByName(rectId + 'fillRegion')).toBeUndefined();
      expect(
        (rectMesh.material as THREE.MeshBasicMaterial).color.getHex(),
      ).toEqual(13166775);

      const emptyFillRegion = makeUiRect3D(rectId);
      emptyFillRegion.fillRegion = [];
      canvas.updateRects([emptyFillRegion]);
      const fillRegionMesh = getFillRegionMesh(rectId);
      expect(rectMesh.material).toEqual(Canvas.TRANSPARENT_MATERIAL);
      expect(
        (fillRegionMesh.material as THREE.MeshBasicMaterial).color.getHex(),
      ).toEqual(13166775);
      let fillRegionGeometryId = fillRegionMesh.geometry.id;

      const emptyFillRegionWithContent = makeUiRect3D(rectId);
      emptyFillRegionWithContent.fillRegion = [];
      emptyFillRegionWithContent.colorType = ColorType.HAS_CONTENT;
      canvas.updateRects([emptyFillRegionWithContent]);
      expect(rectMesh.material).toEqual(Canvas.TRANSPARENT_MATERIAL);
      checkMaterialColorAndOpacity(
        fillRegionMesh,
        Canvas.RECT_COLOR_HAS_CONTENT,
        Canvas.OPACITY_REGULAR,
      );
      let newGeometry = getFillRegionMesh(rectId).geometry;
      expect(newGeometry.id).toEqual(fillRegionGeometryId);

      const validFillRegion = makeUiRect3D(rectId);
      validFillRegion.fillRegion = [
        {
          topLeft: emptyFillRegion.topLeft,
          bottomRight: emptyFillRegion.bottomRight,
        },
      ];
      canvas.updateRects([validFillRegion]);
      newGeometry = getFillRegionMesh(rectId).geometry;
      expect(newGeometry.id).not.toEqual(fillRegionGeometryId);
      fillRegionGeometryId = newGeometry.id;

      const differentFillRegion = makeUiRect3D(rectId);
      differentFillRegion.fillRegion = [
        {
          topLeft: validFillRegion.fillRegion[0].topLeft,
          bottomRight: new Point3D(4, 4, 2),
        },
      ];
      canvas.updateRects([differentFillRegion]);
      newGeometry = getFillRegionMesh(rectId).geometry;
      expect(newGeometry.id).not.toEqual(fillRegionGeometryId);
      fillRegionGeometryId = newGeometry.id;

      canvas.updateRects([noFillRegion]);
      expect(
        getRectMesh(rectId).getObjectByName(rectId + 'fillRegion'),
      ).toBeUndefined();
    });

    it('handles change from normal to pinned borders', () => {
      const rect = makeUiRect3D(rectId);
      rect.topLeft = new Point3D(1, 1, 5);
      rect.bottomRight = new Point3D(2, 2, 5);
      canvas.updateRects([rect]);
      checkBorderColor(rect.id, Canvas.RECT_EDGE_COLOR_LIGHT_MODE);

      const pinnedRect = makeUiRect3D(rectId);
      pinnedRect.topLeft = new Point3D(1, 1, 5);
      pinnedRect.bottomRight = new Point3D(2, 2, 5);
      pinnedRect.isPinned = true;
      canvas.updateRects([pinnedRect]);
      checkBorderColor(rect.id, Canvas.RECT_EDGE_COLOR_PINNED);
    });

    function checkMaterialColorAndOpacity(
      mesh: THREE.Mesh,
      color: THREE.Color | number,
      opacity: number,
    ) {
      const material = mesh.material as THREE.MeshBasicMaterial;
      if (color instanceof THREE.Color) {
        expect(material.color).toEqual(color);
      } else {
        expect(material.color.getHex()).toEqual(color);
      }
      expect(material.opacity).toEqual(opacity);
    }

    function checkBorderColor(id: string, color: number) {
      const rectMesh = getRectMesh(id);

      const borderMesh = assertDefined(
        rectMesh.getObjectByName(id + 'border'),
      ) as THREE.Mesh;
      expect(
        (borderMesh.material as THREE.LineBasicMaterial).color.getHex(),
      ).toEqual(color);
    }

    function getRectMesh(id: string) {
      return assertDefined(graphicsScene.getObjectByName(id)) as THREE.Mesh;
    }

    function getFillRegionMesh(id: string) {
      const rectMesh = getRectMesh(id);
      return assertDefined(
        rectMesh.getObjectByName(id + 'fillRegion'),
      ) as THREE.Mesh;
    }

    function updateRectsAndCheckGeometryId(
      rect: UiRect3D,
      rectMesh: THREE.Mesh,
      prevId: number,
    ) {
      canvas.updateRects([rect]);
      expect(rectMesh.geometry.id).not.toEqual(prevId);
    }
  });

  describe('updateLabels', () => {
    let canvas: Canvas;
    let isDarkMode: boolean;
    let graphicsScene: THREE.Scene;

    beforeEach(() => {
      isDarkMode = false;
      const canvasRects = document.createElement('canvas');
      const canvasLabels = document.createElement('canvas');
      canvas = new Canvas(canvasRects, canvasLabels, () => isDarkMode);
      graphicsScene = canvas.renderView()[0];
    });

    it('adds and removes labels', () => {
      const mapDeleteSpy = spyOn(Map.prototype, 'delete').and.callThrough();
      canvas.updateLabels([]);
      expect(graphicsScene.getObjectByName(rectId + 'circle')).toBeUndefined();
      expect(graphicsScene.getObjectByName(rectId + 'line')).toBeUndefined();
      expect(graphicsScene.getObjectByName(rectId + 'text')).toBeUndefined();

      canvas.updateLabels([makeRectLabel(rectId)]);
      expect(graphicsScene.getObjectByName(rectId + 'circle')).toBeDefined();
      expect(graphicsScene.getObjectByName(rectId + 'line')).toBeDefined();
      expect(graphicsScene.getObjectByName(rectId + 'text')).toBeDefined();

      canvas.updateLabels([]);
      expect(graphicsScene.getObjectByName(rectId + 'circle')).toBeUndefined();
      expect(graphicsScene.getObjectByName(rectId + 'line')).toBeUndefined();
      expect(graphicsScene.getObjectByName(rectId + 'text')).toBeUndefined();
      expect(mapDeleteSpy).toHaveBeenCalledOnceWith(rectId);
    });

    it('updates existing labels instead of adding new labels', () => {
      const label = makeRectLabel(rectId);
      canvas.updateLabels([label]);
      const circleMesh = getCircleMesh(rectId);
      const geometryId = circleMesh.geometry.id;

      const newLabel = makeRectLabel(rectId);
      newLabel.circle.radius = 2;
      canvas.updateLabels([newLabel]);
      expect(getCircleMesh(rectId)).toEqual(circleMesh);
      expect(circleMesh.geometry.id).not.toEqual(geometryId);
    });

    it('makes label with correct circle and text geometry', () => {
      const label = makeRectLabel(rectId);
      canvas.updateLabels([label]);
      const circleMesh = getCircleMesh(rectId);
      expect(
        (circleMesh.geometry as THREE.CircleGeometry).parameters.radius,
      ).toEqual(label.circle.radius);
      checkVectorEqualToPoint(circleMesh.position, label.circle.center);
      const text = getText(rectId);
      checkVectorEqualToPoint(text.position, label.textCenter);
    });

    it('handles change in circle radius', () => {
      const label = makeRectLabel(rectId);
      canvas.updateLabels([label]);
      const circleMesh = getCircleMesh(rectId);

      const newLabel = makeRectLabel(rectId);
      newLabel.circle.radius = 2;
      canvas.updateLabels([newLabel]);
      expect(
        (circleMesh.geometry as THREE.CircleGeometry).parameters.radius,
      ).toEqual(2);
    });

    it('handles change in circle center', () => {
      const label = makeRectLabel(rectId);
      canvas.updateLabels([label]);
      const circleMesh = getCircleMesh(rectId);

      const newLabel = makeRectLabel(rectId);
      newLabel.circle.center = new Point3D(1, 1, 1);
      canvas.updateLabels([newLabel]);
      checkVectorEqualToPoint(circleMesh.position, newLabel.circle.center);
    });

    it('applies colors based on highlighted or dark mode state', () => {
      const label = makeRectLabel(rectId);
      canvas.updateLabels([label]);
      const circleMesh = getCircleMesh(rectId);
      const line = getLine(rectId);
      const text = getText(rectId);
      expect(
        (circleMesh.material as THREE.LineBasicMaterial).color.getHex(),
      ).toEqual(Canvas.LABEL_LINE_COLOR);
      expect((line.material as THREE.LineBasicMaterial).color.getHex()).toEqual(
        Canvas.LABEL_LINE_COLOR,
      );
      expect(text.element.style.color).toEqual('gray');

      const highlighted = makeRectLabel(rectId);
      highlighted.isHighlighted = true;
      canvas.updateLabels([highlighted]);
      expect(
        (circleMesh.material as THREE.LineBasicMaterial).color.getHex(),
      ).toEqual(Canvas.RECT_EDGE_COLOR_LIGHT_MODE);
      expect((line.material as THREE.LineBasicMaterial).color.getHex()).toEqual(
        Canvas.RECT_EDGE_COLOR_LIGHT_MODE,
      );
      expect(text.element.style.color).toEqual('');

      isDarkMode = true;
      canvas.updateLabels([highlighted]);
      expect(
        (circleMesh.material as THREE.LineBasicMaterial).color.getHex(),
      ).toEqual(Canvas.RECT_EDGE_COLOR_DARK_MODE);
      expect((line.material as THREE.LineBasicMaterial).color.getHex()).toEqual(
        Canvas.RECT_EDGE_COLOR_DARK_MODE,
      );
      expect(text.element.style.color).toEqual('');

      canvas.updateLabels([label]);
      expect(
        (circleMesh.material as THREE.LineBasicMaterial).color.getHex(),
      ).toEqual(Canvas.LABEL_LINE_COLOR);
      expect((line.material as THREE.LineBasicMaterial).color.getHex()).toEqual(
        Canvas.LABEL_LINE_COLOR,
      );
      expect(text.element.style.color).toEqual('gray');
    });

    it('handles change in line points', () => {
      const label = makeRectLabel(rectId);
      canvas.updateLabels([label]);
      const line = getLine(rectId);
      const geometryId = line.geometry.id;

      const newLabel = makeRectLabel(rectId);
      newLabel.linePoints = [new Point3D(1, 1, 1), new Point3D(1, 2, 1)];
      canvas.updateLabels([newLabel]);
      expect(line.geometry.id).not.toEqual(geometryId);
    });

    it('handles change in text center', () => {
      const label = makeRectLabel(rectId);
      canvas.updateLabels([label]);
      const text = getText(rectId);

      const newLabel = makeRectLabel(rectId);
      newLabel.textCenter = new Point3D(1, 15, 1);
      canvas.updateLabels([newLabel]);
      checkVectorEqualToPoint(text.position, newLabel.textCenter);
    });

    it('robust to no labels canvas', () => {
      const canvasRects = document.createElement('canvas');
      const canvas = new Canvas(canvasRects);
      canvas.updateLabels([]);
    });

    it('propagates highlighted item on text click', () => {
      const label = makeRectLabel(rectId);
      canvas.updateLabels([label]);
      const text = getText(rectId);

      let id: string | undefined;
      text.element.addEventListener(
        ViewerEvents.HighlightedIdChange,
        (event) => {
          id = (event as CustomEvent).detail.id;
        },
      );
      text.element.click();
      expect(id).toEqual(rectId);
    });

    function getCircleMesh(id: string): THREE.Mesh {
      return assertDefined(
        graphicsScene.getObjectByName(id + 'circle'),
      ) as THREE.Mesh;
    }

    function getLine(id: string): THREE.Line {
      return assertDefined(
        graphicsScene.getObjectByName(id + 'line'),
      ) as THREE.Line;
    }

    function getText(id: string): CSS2DObject {
      return assertDefined(
        graphicsScene.getObjectByName(id + 'text'),
      ) as CSS2DObject;
    }

    function checkVectorEqualToPoint(vector: THREE.Vector3, point: Point3D) {
      expect(
        vector.equals(new THREE.Vector3(point.x, point.y, point.z)),
      ).toBeTrue();
    }
  });

  describe('renderView', () => {
    let canvas: Canvas;
    let rectsCompileSpy: jasmine.Spy;
    let renderingSpies: jasmine.Spy[];

    beforeEach(() => {
      const canvasRects = document.createElement('canvas');
      const canvasLabels = document.createElement('canvas');
      canvas = new Canvas(canvasRects, canvasLabels);
      rectsCompileSpy = spyOn(assertDefined(canvas.renderer), 'compile');
      renderingSpies = [
        spyOn(assertDefined(canvas.renderer), 'setPixelRatio'),
        spyOn(assertDefined(canvas.renderer), 'render'),
        spyOn(assertDefined(canvas.labelRenderer), 'render'),
      ];
    });

    it('sets pixel ratio and renders rects and labels', () => {
      canvas.renderView();
      checkRenderSpiesCalled(1);
    });

    it('only compiles on first call', () => {
      canvas.renderView();
      expect(rectsCompileSpy).toHaveBeenCalledTimes(1);
      checkRenderSpiesCalled(1);

      canvas.renderView();
      expect(rectsCompileSpy).toHaveBeenCalledTimes(1);
      checkRenderSpiesCalled(2);
    });

    it('robust to no labels canvas', () => {
      const canvasRects = document.createElement('canvas');
      const canvas = new Canvas(canvasRects);
      canvas.renderView();
    });

    function checkRenderSpiesCalled(times: number) {
      renderingSpies.forEach((spy) => expect(spy).toHaveBeenCalledTimes(times));
    }
  });

  describe('getClickedRectId', () => {
    let canvas: Canvas;

    beforeEach(() => {
      const canvasRects = document.createElement('canvas');
      const canvasLabels = document.createElement('canvas');
      canvas = new Canvas(canvasRects, canvasLabels);
      canvas.updateViewPosition(makeCamera(), makeBoundingBox());
      canvas.renderView();
    });

    it('identifies clicked rect', () => {
      const rect = makeUiRect3D(rectId);
      rect.isClickable = true;
      canvas.updateRects([rect]);
      canvas.renderView();

      const id = canvas.getClickedRectId(0.1, 0.1, 0);
      expect(id).toEqual('rect1');
    });

    it('does not identify rect if not clickable', () => {
      const rect = makeUiRect3D(rectId);
      canvas.updateRects([rect]);
      expect(canvas.getClickedRectId(0.1, 0.1, 0)).toBeUndefined();
    });

    it('does not identify rect out of click area', () => {
      const rect = makeUiRect3D(rectId);
      rect.isClickable = true;
      canvas.updateRects([rect]);
      expect(canvas.getClickedRectId(2, 2, 0)).toBeUndefined();
    });
  });

  function makeCamera(): Camera {
    return {
      rotationAngleX: 0,
      rotationAngleY: 0,
      zoomFactor: 1,
      panScreenDistance: new Distance(0, 0),
    };
  }

  function makeBoundingBox(): Box3D {
    return {
      width: 1,
      height: 1,
      depth: 1,
      center: new Point3D(0, 0, 0),
      diagonal: 1,
    };
  }

  function makeUiRect3D(id: string): UiRect3D {
    return {
      id,
      topLeft: new Point3D(0, 0, 0),
      bottomRight: new Point3D(1, 1, 0),
      cornerRadius: 0,
      darkFactor: 1,
      colorType: ColorType.VISIBLE,
      isClickable: false,
      transform: IDENTITY_MATRIX,
      isOversized: false,
      fillRegion: undefined,
      isPinned: false,
    };
  }

  function makeRectLabel(id: string): RectLabel {
    return {
      circle: {radius: 1, center: new Point3D(0, 0, 0)},
      linePoints: [new Point3D(0, 0, 0), new Point3D(0, 1, 0)],
      textCenter: new Point3D(0, 12, 0),
      text: id,
      isHighlighted: false,
      rectId: id,
    };
  }
});
